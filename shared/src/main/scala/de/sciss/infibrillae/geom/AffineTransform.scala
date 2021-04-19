package de.sciss.infibrillae.geom

import de.sciss.infibrillae.geom.AffineTransform.{APPLY_IDENTITY, APPLY_SCALE, APPLY_SCALE_TRANSLATE, APPLY_SHEAR, APPLY_SHEAR_SCALE, APPLY_SHEAR_SCALE_TRANSLATE, APPLY_SHEAR_TRANSLATE, APPLY_TRANSLATE, HI_IDENTITY_APPLY_IDENTITY, HI_IDENTITY_APPLY_SCALE, HI_IDENTITY_APPLY_SCALE_TRANSLATE, HI_IDENTITY_APPLY_TRANSLATE}

import java.io.{ObjectInputStream, ObjectOutputStream}

// This is an adapted Scala translation of the AffineTransform Java class of OpenJDK
// as released under GNU GPL 2 -- see original file header below.
// So it can be used in Scala.js.

/*
 * Copyright (c) 1996, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/**
  * The `AffineTransform` class represents a 2D affine transform
  * that performs a linear mapping from 2D coordinates to other 2D
  * coordinates that preserves the "straightness" and
  * "parallelness" of lines.  Affine transformations can be constructed
  * using sequences of translations, scales, flips, rotations, and shears.
  * <p>
  * Such a coordinate transformation can be represented by a 3 row by
  * 3 column matrix with an implied last row of [ 0 0 1 ].  This matrix
  * transforms source coordinates `(x,y)` into
  * destination coordinates `(x',y')` by considering
  * them to be a column vector and multiplying the coordinate vector
  * by the matrix according to the following process:
  * <pre>
  * [ x']   [  m00  m01  m02  ] [ x ]   [ m00x + m01y + m02 ]
  * [ y'] = [  m10  m11  m12  ] [ y ] = [ m10x + m11y + m12 ]
  * [ 1 ]   [   0    0    1   ] [ 1 ]   [         1         ]
  * </pre>
  * <h3><a id="quadrantapproximation">Handling 90-Degree Rotations</a></h3>
  * <p>
  * In some variations of the `rotate` methods in the
  * `AffineTransform` class, a double-precision argument
  * specifies the angle of rotation in radians.
  * These methods have special handling for rotations of approximately
  * 90 degrees (including multiples such as 180, 270, and 360 degrees),
  * so that the common case of quadrant rotation is handled more
  * efficiently.
  * This special handling can cause angles very close to multiples of
  * 90 degrees to be treated as if they were exact multiples of
  * 90 degrees.
  * For small multiples of 90 degrees the range of angles treated
  * as a quadrant rotation is approximately 0.00000121 degrees wide.
  * This section explains why such special care is needed and how
  * it is implemented.
  * <p>
  * Since 90 degrees is represented as `PI/2` in radians,
  * and since PI is a transcendental (and therefore irrational) number,
  * it is not possible to exactly represent a multiple of 90 degrees as
  * an exact double precision value measured in radians.
  * As a result it is theoretically impossible to describe quadrant
  * rotations (90, 180, 270 or 360 degrees) using these values.
  * Double precision floating point values can get very close to
  * non-zero multiples of `PI/2` but never close enough
  * for the sine or cosine to be exactly 0.0, 1.0 or -1.0.
  * The implementations of `Math.sin()` and
  * `Math.cos()` correspondingly never return 0.0
  * for any case other than `Math.sin(0.0)`.
  * These same implementations do, however, return exactly 1.0 and
  * -1.0 for some range of numbers around each multiple of 90
  * degrees since the correct answer is so close to 1.0 or -1.0 that
  * the double precision significand cannot represent the difference
  * as accurately as it can for numbers that are near 0.0.
  * <p>
  * The net result of these issues is that if the
  * `Math.sin()` and `Math.cos()` methods
  * are used to directly generate the values for the matrix modifications
  * during these radian-based rotation operations then the resulting
  * transform is never strictly classifiable as a quadrant rotation
  * even for a simple case like `rotate(Math.PI/2.0)`,
  * due to minor variations in the matrix caused by the non-0.0 values
  * obtained for the sine and cosine.
  * If these transforms are not classified as quadrant rotations then
  * subsequent code which attempts to optimize further operations based
  * upon the type of the transform will be relegated to its most general
  * implementation.
  * <p>
  * Because quadrant rotations are fairly common,
  * this class should handle these cases reasonably quickly, both in
  * applying the rotations to the transform and in applying the resulting
  * transform to the coordinates.
  * To facilitate this optimal handling, the methods which take an angle
  * of rotation measured in radians attempt to detect angles that are
  * intended to be quadrant rotations and treat them as such.
  * These methods therefore treat an angle <em>theta</em> as a quadrant
  * rotation if either <code>Math.sin(<em>theta</em>)</code> or
  * <code>Math.cos(<em>theta</em>)</code> returns exactly 1.0 or -1.0.
  * As a rule of thumb, this property holds true for a range of
  * approximately 0.0000000211 radians (or 0.00000121 degrees) around
  * small multiples of `Math.PI/2.0`.
  *
  * @author Jim Graham
  * @since 1.2
  */
@SerialVersionUID(1330973210523860834L)
object AffineTransform {
  /*
     * This constant is only useful for the cached type field.
     * It indicates that the type has been decached and must be recalculated.
     */
  final private val TYPE_UNKNOWN = -1
  /**
    * This constant indicates that the transform defined by this object
    * is an identity transform.
    * An identity transform is one in which the output coordinates are
    * always the same as the input coordinates.
    * If this transform is anything other than the identity transform,
    * the type will either be the constant GENERAL_TRANSFORM or a
    * combination of the appropriate flag bits for the various coordinate
    * conversions that this transform performs.
    *
    * @see #TYPE_TRANSLATION
    * @see #TYPE_UNIFORM_SCALE
    * @see #TYPE_GENERAL_SCALE
    * @see #TYPE_FLIP
    * @see #TYPE_QUADRANT_ROTATION
    * @see #TYPE_GENERAL_ROTATION
    * @see #TYPE_GENERAL_TRANSFORM
    * @see #getType
    * @since 1.2
    */
  final val TYPE_IDENTITY = 0
  /**
    * This flag bit indicates that the transform defined by this object
    * performs a translation in addition to the conversions indicated
    * by other flag bits.
    * A translation moves the coordinates by a constant amount in x
    * and y without changing the length or angle of vectors.
    *
    * @see #TYPE_IDENTITY
    * @see #TYPE_UNIFORM_SCALE
    * @see #TYPE_GENERAL_SCALE
    * @see #TYPE_FLIP
    * @see #TYPE_QUADRANT_ROTATION
    * @see #TYPE_GENERAL_ROTATION
    * @see #TYPE_GENERAL_TRANSFORM
    * @see #getType
    * @since 1.2
    */
  final val TYPE_TRANSLATION = 1
  /**
    * This flag bit indicates that the transform defined by this object
    * performs a uniform scale in addition to the conversions indicated
    * by other flag bits.
    * A uniform scale multiplies the length of vectors by the same amount
    * in both the x and y directions without changing the angle between
    * vectors.
    * This flag bit is mutually exclusive with the TYPE_GENERAL_SCALE flag.
    *
    * @see #TYPE_IDENTITY
    * @see #TYPE_TRANSLATION
    * @see #TYPE_GENERAL_SCALE
    * @see #TYPE_FLIP
    * @see #TYPE_QUADRANT_ROTATION
    * @see #TYPE_GENERAL_ROTATION
    * @see #TYPE_GENERAL_TRANSFORM
    * @see #getType
    * @since 1.2
    */
  final val TYPE_UNIFORM_SCALE = 2
  /**
    * This flag bit indicates that the transform defined by this object
    * performs a general scale in addition to the conversions indicated
    * by other flag bits.
    * A general scale multiplies the length of vectors by different
    * amounts in the x and y directions without changing the angle
    * between perpendicular vectors.
    * This flag bit is mutually exclusive with the TYPE_UNIFORM_SCALE flag.
    *
    * @see #TYPE_IDENTITY
    * @see #TYPE_TRANSLATION
    * @see #TYPE_UNIFORM_SCALE
    * @see #TYPE_FLIP
    * @see #TYPE_QUADRANT_ROTATION
    * @see #TYPE_GENERAL_ROTATION
    * @see #TYPE_GENERAL_TRANSFORM
    * @see #getType
    * @since 1.2
    */
  final val TYPE_GENERAL_SCALE = 4
  /**
    * This constant is a bit mask for any of the scale flag bits.
    *
    * @see #TYPE_UNIFORM_SCALE
    * @see #TYPE_GENERAL_SCALE
    * @since 1.2
    */
  final val TYPE_MASK_SCALE = TYPE_UNIFORM_SCALE | TYPE_GENERAL_SCALE
  /**
    * This flag bit indicates that the transform defined by this object
    * performs a mirror image flip about some axis which changes the
    * normally right handed coordinate system into a left handed
    * system in addition to the conversions indicated by other flag bits.
    * A right handed coordinate system is one where the positive X
    * axis rotates counterclockwise to overlay the positive Y axis
    * similar to the direction that the fingers on your right hand
    * curl when you stare end on at your thumb.
    * A left handed coordinate system is one where the positive X
    * axis rotates clockwise to overlay the positive Y axis similar
    * to the direction that the fingers on your left hand curl.
    * There is no mathematical way to determine the angle of the
    * original flipping or mirroring transformation since all angles
    * of flip are identical given an appropriate adjusting rotation.
    *
    * @see #TYPE_IDENTITY
    * @see #TYPE_TRANSLATION
    * @see #TYPE_UNIFORM_SCALE
    * @see #TYPE_GENERAL_SCALE
    * @see #TYPE_QUADRANT_ROTATION
    * @see #TYPE_GENERAL_ROTATION
    * @see #TYPE_GENERAL_TRANSFORM
    * @see #getType
    * @since 1.2
    */
  final val TYPE_FLIP = 64
  /**
    * This flag bit indicates that the transform defined by this object
    * performs a quadrant rotation by some multiple of 90 degrees in
    * addition to the conversions indicated by other flag bits.
    * A rotation changes the angles of vectors by the same amount
    * regardless of the original direction of the vector and without
    * changing the length of the vector.
    * This flag bit is mutually exclusive with the TYPE_GENERAL_ROTATION flag.
    *
    * @see #TYPE_IDENTITY
    * @see #TYPE_TRANSLATION
    * @see #TYPE_UNIFORM_SCALE
    * @see #TYPE_GENERAL_SCALE
    * @see #TYPE_FLIP
    * @see #TYPE_GENERAL_ROTATION
    * @see #TYPE_GENERAL_TRANSFORM
    * @see #getType
    * @since 1.2
    */
  final val TYPE_QUADRANT_ROTATION = 8
  /**
    * This flag bit indicates that the transform defined by this object
    * performs a rotation by an arbitrary angle in addition to the
    * conversions indicated by other flag bits.
    * A rotation changes the angles of vectors by the same amount
    * regardless of the original direction of the vector and without
    * changing the length of the vector.
    * This flag bit is mutually exclusive with the
    * TYPE_QUADRANT_ROTATION flag.
    *
    * @see #TYPE_IDENTITY
    * @see #TYPE_TRANSLATION
    * @see #TYPE_UNIFORM_SCALE
    * @see #TYPE_GENERAL_SCALE
    * @see #TYPE_FLIP
    * @see #TYPE_QUADRANT_ROTATION
    * @see #TYPE_GENERAL_TRANSFORM
    * @see #getType
    * @since 1.2
    */
  final val TYPE_GENERAL_ROTATION = 16
  /**
    * This constant is a bit mask for any of the rotation flag bits.
    *
    * @see #TYPE_QUADRANT_ROTATION
    * @see #TYPE_GENERAL_ROTATION
    * @since 1.2
    */
  final val TYPE_MASK_ROTATION = TYPE_QUADRANT_ROTATION | TYPE_GENERAL_ROTATION
  /**
    * This constant indicates that the transform defined by this object
    * performs an arbitrary conversion of the input coordinates.
    * If this transform can be classified by any of the above constants,
    * the type will either be the constant TYPE_IDENTITY or a
    * combination of the appropriate flag bits for the various coordinate
    * conversions that this transform performs.
    *
    * @see #TYPE_IDENTITY
    * @see #TYPE_TRANSLATION
    * @see #TYPE_UNIFORM_SCALE
    * @see #TYPE_GENERAL_SCALE
    * @see #TYPE_FLIP
    * @see #TYPE_QUADRANT_ROTATION
    * @see #TYPE_GENERAL_ROTATION
    * @see #getType
    * @since 1.2
    */
  final val TYPE_GENERAL_TRANSFORM = 32
  /**
    * This constant is used for the internal state variable to indicate
    * that no calculations need to be performed and that the source
    * coordinates only need to be copied to their destinations to
    * complete the transformation equation of this transform.
    *
    * @see #APPLY_TRANSLATE
    * @see #APPLY_SCALE
    * @see #APPLY_SHEAR
    * @see #state
    */
  private[geom] final val APPLY_IDENTITY = 0
  /**
    * This constant is used for the internal state variable to indicate
    * that the translation components of the matrix (m02 and m12) need
    * to be added to complete the transformation equation of this transform.
    *
    * @see #APPLY_IDENTITY
    * @see #APPLY_SCALE
    * @see #APPLY_SHEAR
    * @see #state
    */
  private[geom] final val APPLY_TRANSLATE = 1
  /**
    * This constant is used for the internal state variable to indicate
    * that the scaling components of the matrix (m00 and m11) need
    * to be factored in to complete the transformation equation of
    * this transform.  If the APPLY_SHEAR bit is also set then it
    * indicates that the scaling components are not both 0.0.  If the
    * APPLY_SHEAR bit is not also set then it indicates that the
    * scaling components are not both 1.0.  If neither the APPLY_SHEAR
    * nor the APPLY_SCALE bits are set then the scaling components
    * are both 1.0, which means that the x and y components contribute
    * to the transformed coordinate, but they are not multiplied by
    * any scaling factor.
    *
    * @see #APPLY_IDENTITY
    * @see #APPLY_TRANSLATE
    * @see #APPLY_SHEAR
    * @see #state
    */
  private[geom] final val APPLY_SCALE = 2
  /**
    * This constant is used for the internal state variable to indicate
    * that the shearing components of the matrix (m01 and m10) need
    * to be factored in to complete the transformation equation of this
    * transform.  The presence of this bit in the state variable changes
    * the interpretation of the APPLY_SCALE bit as indicated in its
    * documentation.
    *
    * @see #APPLY_IDENTITY
    * @see #APPLY_TRANSLATE
    * @see #APPLY_SCALE
    * @see #state
    */
  private[geom] final val APPLY_SHEAR = 4
  /*
     * For methods which combine together the state of two separate
     * transforms and dispatch based upon the combination, these constants
     * specify how far to shift one of the states so that the two states
     * are mutually non-interfering and provide constants for testing the
     * bits of the shifted (HI) state.  The methods in this class use
     * the convention that the state of "this" transform is unshifted and
     * the state of the "other" or "argument" transform is shifted (HI).
     */
  private final val HI_SHIFT      = 3
  private final val HI_IDENTITY   = APPLY_IDENTITY  << HI_SHIFT
  private final val HI_TRANSLATE  = APPLY_TRANSLATE << HI_SHIFT
  private final val HI_SCALE      = APPLY_SCALE     << HI_SHIFT
  private final val HI_SHEAR      = APPLY_SHEAR     << HI_SHIFT

  private final val APPLY_SHEAR_SCALE_TRANSLATE = APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE
  private final val APPLY_SHEAR_SCALE           = APPLY_SHEAR | APPLY_SCALE
  private final val APPLY_SHEAR_TRANSLATE       = APPLY_SHEAR | APPLY_TRANSLATE
  private final val APPLY_SCALE_TRANSLATE       = APPLY_SCALE | APPLY_TRANSLATE

  private final val HI_IDENTITY_APPLY_IDENTITY          = HI_IDENTITY | APPLY_IDENTITY
  private final val HI_IDENTITY_APPLY_TRANSLATE         = HI_IDENTITY | APPLY_TRANSLATE
  private final val HI_IDENTITY_APPLY_SCALE             = HI_IDENTITY | APPLY_SCALE
  private final val HI_IDENTITY_APPLY_SCALE_TRANSLATE   = HI_IDENTITY | APPLY_SCALE | APPLY_TRANSLATE
  /**
    * Returns a transform representing a translation transformation.
    * The matrix representing the returned transform is:
    * <pre>
    * [   1    0    tx  ]
    * [   0    1    ty  ]
    * [   0    0    1   ]
    * </pre>
    *
    * @param tx the distance by which coordinates are translated in the
    *           X axis direction
    * @param ty the distance by which coordinates are translated in the
    *           Y axis direction
    * @return an `AffineTransform` object that represents a
    *         translation transformation, created with the specified vector.
    * @since 1.2
    */
  def getTranslateInstance(tx: Double, ty: Double): AffineTransform = {
    val Tx = new AffineTransform
    Tx.setToTranslation(tx, ty)
    Tx
  }

  /**
    * Returns a transform representing a rotation transformation.
    * The matrix representing the returned transform is:
    * <pre>
    * [   cos(theta)    -sin(theta)    0   ]
    * [   sin(theta)     cos(theta)    0   ]
    * [       0              0         1   ]
    * </pre>
    * Rotating by a positive angle theta rotates points on the positive
    * X axis toward the positive Y axis.
    * Note also the discussion of
    * <a href="#quadrantapproximation">Handling 90-Degree Rotations</a>
    * above.
    *
    * @param theta the angle of rotation measured in radians
    * @return an `AffineTransform` object that is a rotation
    *         transformation, created with the specified angle of rotation.
    * @since 1.2
    */
  def getRotateInstance(theta: Double): AffineTransform = {
    val Tx = new AffineTransform
    Tx.setToRotation(theta)
    Tx
  }

  /**
    * Returns a transform that rotates coordinates around an anchor point.
    * This operation is equivalent to translating the coordinates so
    * that the anchor point is at the origin (S1), then rotating them
    * about the new origin (S2), and finally translating so that the
    * intermediate origin is restored to the coordinates of the original
    * anchor point (S3).
    * <p>
    * This operation is equivalent to the following sequence of calls:
    * <pre>
    * AffineTransform Tx = new AffineTransform();
    * Tx.translate(anchorx, anchory);    // S3: final translation
    * Tx.rotate(theta);                  // S2: rotate around anchor
    * Tx.translate(-anchorx, -anchory);  // S1: translate anchor to origin
    * </pre>
    * The matrix representing the returned transform is:
    * <pre>
    * [   cos(theta)    -sin(theta)    x-x*cos+y*sin  ]
    * [   sin(theta)     cos(theta)    y-x*sin-y*cos  ]
    * [       0              0               1        ]
    * </pre>
    * Rotating by a positive angle theta rotates points on the positive
    * X axis toward the positive Y axis.
    * Note also the discussion of
    * <a href="#quadrantapproximation">Handling 90-Degree Rotations</a>
    * above.
    *
    * @param theta   the angle of rotation measured in radians
    * @param anchorx the X coordinate of the rotation anchor point
    * @param anchory the Y coordinate of the rotation anchor point
    * @return an `AffineTransform` object that rotates
    *         coordinates around the specified point by the specified angle of
    *         rotation.
    * @since 1.2
    */
  def getRotateInstance(theta: Double, anchorx: Double, anchory: Double): AffineTransform = {
    val Tx = new AffineTransform
    Tx.setToRotation(theta, anchorx, anchory)
    Tx
  }

  /**
    * Returns a transform that rotates coordinates according to
    * a rotation vector.
    * All coordinates rotate about the origin by the same amount.
    * The amount of rotation is such that coordinates along the former
    * positive X axis will subsequently align with the vector pointing
    * from the origin to the specified vector coordinates.
    * If both `vecx` and `vecy` are 0.0,
    * an identity transform is returned.
    * This operation is equivalent to calling:
    * <pre>
    * AffineTransform.getRotateInstance(Math.atan2(vecy, vecx));
    * </pre>
    *
    * @param vecx the X coordinate of the rotation vector
    * @param vecy the Y coordinate of the rotation vector
    * @return an `AffineTransform` object that rotates
    *         coordinates according to the specified rotation vector.
    * @since 1.6
    */
  def getRotateInstance(vecx: Double, vecy: Double): AffineTransform = {
    val Tx = new AffineTransform
    Tx.setToRotation(vecx, vecy)
    Tx
  }

  /**
    * Returns a transform that rotates coordinates around an anchor
    * point according to a rotation vector.
    * All coordinates rotate about the specified anchor coordinates
    * by the same amount.
    * The amount of rotation is such that coordinates along the former
    * positive X axis will subsequently align with the vector pointing
    * from the origin to the specified vector coordinates.
    * If both `vecx` and `vecy` are 0.0,
    * an identity transform is returned.
    * This operation is equivalent to calling:
    * <pre>
    * AffineTransform.getRotateInstance(Math.atan2(vecy, vecx),
    * anchorx, anchory);
    * </pre>
    *
    * @param vecx    the X coordinate of the rotation vector
    * @param vecy    the Y coordinate of the rotation vector
    * @param anchorx the X coordinate of the rotation anchor point
    * @param anchory the Y coordinate of the rotation anchor point
    * @return an `AffineTransform` object that rotates
    *         coordinates around the specified point according to the
    *         specified rotation vector.
    * @since 1.6
    */
  def getRotateInstance(vecx: Double, vecy: Double, anchorx: Double, anchory: Double): AffineTransform = {
    val Tx = new AffineTransform
    Tx.setToRotation(vecx, vecy, anchorx, anchory)
    Tx
  }

  /**
    * Returns a transform that rotates coordinates by the specified
    * number of quadrants.
    * This operation is equivalent to calling:
    * <pre>
    * AffineTransform.getRotateInstance(numquadrants * Math.PI / 2.0);
    * </pre>
    * Rotating by a positive number of quadrants rotates points on
    * the positive X axis toward the positive Y axis.
    *
    * @param numquadrants the number of 90 degree arcs to rotate by
    * @return an `AffineTransform` object that rotates
    *         coordinates by the specified number of quadrants.
    * @since 1.6
    */
  def getQuadrantRotateInstance(numquadrants: Int): AffineTransform = {
    val Tx = new AffineTransform
    Tx.setToQuadrantRotation(numquadrants)
    Tx
  }

  /**
    * Returns a transform that rotates coordinates by the specified
    * number of quadrants around the specified anchor point.
    * This operation is equivalent to calling:
    * <pre>
    * AffineTransform.getRotateInstance(numquadrants * Math.PI / 2.0,
    * anchorx, anchory);
    * </pre>
    * Rotating by a positive number of quadrants rotates points on
    * the positive X axis toward the positive Y axis.
    *
    * @param numquadrants the number of 90 degree arcs to rotate by
    * @param anchorx      the X coordinate of the rotation anchor point
    * @param anchory      the Y coordinate of the rotation anchor point
    * @return an `AffineTransform` object that rotates
    *         coordinates by the specified number of quadrants around the
    *         specified anchor point.
    * @since 1.6
    */
  def getQuadrantRotateInstance(numquadrants: Int, anchorx: Double, anchory: Double): AffineTransform = {
    val Tx = new AffineTransform
    Tx.setToQuadrantRotation(numquadrants, anchorx, anchory)
    Tx
  }

  /**
    * Returns a transform representing a scaling transformation.
    * The matrix representing the returned transform is:
    * <pre>
    * [   sx   0    0   ]
    * [   0    sy   0   ]
    * [   0    0    1   ]
    * </pre>
    *
    * @param sx the factor by which coordinates are scaled along the
    *           X axis direction
    * @param sy the factor by which coordinates are scaled along the
    *           Y axis direction
    * @return an `AffineTransform` object that scales
    *         coordinates by the specified factors.
    * @since 1.2
    */
  def getScaleInstance(sx: Double, sy: Double): AffineTransform = {
    val Tx = new AffineTransform
    Tx.setToScale(sx, sy)
    Tx
  }

  /**
    * Returns a transform representing a shearing transformation.
    * The matrix representing the returned transform is:
    * <pre>
    * [   1   shx   0   ]
    * [  shy   1    0   ]
    * [   0    0    1   ]
    * </pre>
    *
    * @param shx the multiplier by which coordinates are shifted in the
    *            direction of the positive X axis as a factor of their Y coordinate
    * @param shy the multiplier by which coordinates are shifted in the
    *            direction of the positive Y axis as a factor of their X coordinate
    * @return an `AffineTransform` object that shears
    *         coordinates by the specified multipliers.
    * @since 1.2
    */
  def getShearInstance(shx: Double, shy: Double): AffineTransform = {
    val Tx = new AffineTransform
    Tx.setToShear(shx, shy)
    Tx
  }

  // Utility methods to optimize rotate methods.
  // These tables translate the flags during predictable quadrant
  // rotations where the shear and scale values are swapped and negated.
  private val rot90conversion = Array(/* IDENTITY => */ APPLY_SHEAR, /* TRANSLATE (TR) => */ APPLY_SHEAR | APPLY_TRANSLATE, /* SCALE (SC) => */ APPLY_SHEAR, /* SC | TR => */ APPLY_SHEAR | APPLY_TRANSLATE, /* SHEAR (SH) => */ APPLY_SCALE, /* SH | TR => */ APPLY_SCALE | APPLY_TRANSLATE, /* SH | SC => */ APPLY_SHEAR | APPLY_SCALE, /* SH | SC | TR => */ APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE)

  // Round values to sane precision for printing
  // Note that Math.sin(Math.PI) has an error of about 10^-16
  private def _matround(matval: Double) = Math.rint(matval * 1E15) / 1E15
}

/**
  * Constructs a new `AffineTransform` representing the
  * Identity transformation.
  *
  * @since 1.2
  */
class AffineTransform()
  extends Cloneable with Serializable {

// m01 = m10 = m02 = m12 = 0.0;         /* Not needed. */
// state = APPLY_IDENTITY;              /* Not needed. */
// type = TYPE_IDENTITY;                /* Not needed. */

  /**
    * The X coordinate scaling element of the 3x3
    * affine transformation matrix.
    */
  private[geom] var m00 = 1.0
  /**
    * The Y coordinate shearing element of the 3x3
    * affine transformation matrix.
    */
  private[geom] var m10 = 0.0
  /**
    * The X coordinate shearing element of the 3x3
    * affine transformation matrix.
    */
  private[geom] var m01 = 0.0
  /**
    * The Y coordinate scaling element of the 3x3
    * affine transformation matrix.
    */
  private[geom] var m11 = 1.0
  /**
    * The X coordinate of the translation element of the
    * 3x3 affine transformation matrix.
    */
  private[geom] var m02 = 0.0
  /**
    * The Y coordinate of the translation element of the
    * 3x3 affine transformation matrix.
    */
  private[geom] var m12 = 0.0
  /**
    * This field keeps track of which components of the matrix need to
    * be applied when performing a transformation.
    *
    * @see #APPLY_IDENTITY
    * @see #APPLY_TRANSLATE
    * @see #APPLY_SCALE
    * @see #APPLY_SHEAR
    */
  private[geom] var state = 0
  /**
    * This field caches the current transformation type of the matrix.
    *
    * @see #TYPE_IDENTITY
    * @see #TYPE_TRANSLATION
    * @see #TYPE_UNIFORM_SCALE
    * @see #TYPE_GENERAL_SCALE
    * @see #TYPE_FLIP
    * @see #TYPE_QUADRANT_ROTATION
    * @see #TYPE_GENERAL_ROTATION
    * @see #TYPE_GENERAL_TRANSFORM
    * @see #TYPE_UNKNOWN
    * @see #getType
    */
  private var tpe = 0

  def this(m00: Double, m10: Double, m01: Double, m11: Double, m02: Double, m12: Double, state: Int) {
    this()
    this.m00 = m00
    this.m10 = m10
    this.m01 = m01
    this.m11 = m11
    this.m02 = m02
    this.m12 = m12
    this.state = state
    this.tpe = AffineTransform.TYPE_UNKNOWN
  }

  /**
    * Constructs a new `AffineTransform` that is a copy of
    * the specified `AffineTransform` object.
    *
    * @param Tx the `AffineTransform` object to copy
    * @since 1.2
    */
  def this(Tx: AffineTransform) {
    this()
    this.m00 = Tx.m00
    this.m10 = Tx.m10
    this.m01 = Tx.m01
    this.m11 = Tx.m11
    this.m02 = Tx.m02
    this.m12 = Tx.m12
    this.state = Tx.state
    this.tpe = Tx.tpe
  }

  /**
    * Constructs a new `AffineTransform` from 6 floating point
    * values representing the 6 specifiable entries of the 3x3
    * transformation matrix.
    *
    * @param m00 the X coordinate scaling element of the 3x3 matrix
    * @param m10 the Y coordinate shearing element of the 3x3 matrix
    * @param m01 the X coordinate shearing element of the 3x3 matrix
    * @param m11 the Y coordinate scaling element of the 3x3 matrix
    * @param m02 the X coordinate translation element of the 3x3 matrix
    * @param m12 the Y coordinate translation element of the 3x3 matrix
    * @since 1.2
    */
  def this(m00: Float, m10: Float, m01: Float, m11: Float, m02: Float, m12: Float) {
    this()
    this.m00 = m00
    this.m10 = m10
    this.m01 = m01
    this.m11 = m11
    this.m02 = m02
    this.m12 = m12
    updateState()
  }

  /**
    * Constructs a new `AffineTransform` from an array of
    * floating point values representing either the 4 non-translation
    * entries or the 6 specifiable entries of the 3x3 transformation
    * matrix.  The values are retrieved from the array as
    * {&nbsp;m00&nbsp;m10&nbsp;m01&nbsp;m11&nbsp;[m02&nbsp;m12]}.
    *
    * @param flatmatrix the float array containing the values to be set
    *                   in the new `AffineTransform` object. The length of the
    *                   array is assumed to be at least 4. If the length of the array is
    *                   less than 6, only the first 4 values are taken. If the length of
    *                   the array is greater than 6, the first 6 values are taken.
    * @since 1.2
    */
  def this(flatmatrix: Array[Float]) {
    this()
    m00 = flatmatrix(0)
    m10 = flatmatrix(1)
    m01 = flatmatrix(2)
    m11 = flatmatrix(3)
    if (flatmatrix.length > 5) {
      m02 = flatmatrix(4)
      m12 = flatmatrix(5)
    }
    updateState()
  }

  /**
    * Constructs a new `AffineTransform` from 6 double
    * precision values representing the 6 specifiable entries of the 3x3
    * transformation matrix.
    *
    * @param m00 the X coordinate scaling element of the 3x3 matrix
    * @param m10 the Y coordinate shearing element of the 3x3 matrix
    * @param m01 the X coordinate shearing element of the 3x3 matrix
    * @param m11 the Y coordinate scaling element of the 3x3 matrix
    * @param m02 the X coordinate translation element of the 3x3 matrix
    * @param m12 the Y coordinate translation element of the 3x3 matrix
    * @since 1.2
    */
  def this(m00: Double, m10: Double, m01: Double, m11: Double, m02: Double, m12: Double) {
    this()
    this.m00 = m00
    this.m10 = m10
    this.m01 = m01
    this.m11 = m11
    this.m02 = m02
    this.m12 = m12
    updateState()
  }

  /**
    * Constructs a new `AffineTransform` from an array of
    * double precision values representing either the 4 non-translation
    * entries or the 6 specifiable entries of the 3x3 transformation
    * matrix. The values are retrieved from the array as
    * {&nbsp;m00&nbsp;m10&nbsp;m01&nbsp;m11&nbsp;[m02&nbsp;m12]}.
    *
    * @param flatmatrix the double array containing the values to be set
    *                   in the new `AffineTransform` object. The length of the
    *                   array is assumed to be at least 4. If the length of the array is
    *                   less than 6, only the first 4 values are taken. If the length of
    *                   the array is greater than 6, the first 6 values are taken.
    * @since 1.2
    */
  def this(flatmatrix: Array[Double]) {
    this()
    m00 = flatmatrix(0)
    m10 = flatmatrix(1)
    m01 = flatmatrix(2)
    m11 = flatmatrix(3)
    if (flatmatrix.length > 5) {
      m02 = flatmatrix(4)
      m12 = flatmatrix(5)
    }
    updateState()
  }

  /**
    * Retrieves the flag bits describing the conversion properties of
    * this transform.
    * The return value is either one of the constants TYPE_IDENTITY
    * or TYPE_GENERAL_TRANSFORM, or a combination of the
    * appropriate flag bits.
    * A valid combination of flag bits is an exclusive OR operation
    * that can combine
    * the TYPE_TRANSLATION flag bit
    * in addition to either of the
    * TYPE_UNIFORM_SCALE or TYPE_GENERAL_SCALE flag bits
    * as well as either of the
    * TYPE_QUADRANT_ROTATION or TYPE_GENERAL_ROTATION flag bits.
    *
    * @return the OR combination of any of the indicated flags that
    *         apply to this transform
    * @see #TYPE_IDENTITY
    * @see #TYPE_TRANSLATION
    * @see #TYPE_UNIFORM_SCALE
    * @see #TYPE_GENERAL_SCALE
    * @see #TYPE_QUADRANT_ROTATION
    * @see #TYPE_GENERAL_ROTATION
    * @see #TYPE_GENERAL_TRANSFORM
    * @since 1.2
    */
  def getType: Int = {
    if (tpe == AffineTransform.TYPE_UNKNOWN) calculateType()
    tpe
  }

  /**
    * This is the utility function to calculate the flag bits when
    * they have not been cached.
    *
    * @see #getType
    */
  @SuppressWarnings(Array("fallthrough")) private def calculateType(): Unit = {
    var ret = AffineTransform.TYPE_IDENTITY
    var sgn0 = false
    var sgn1 = false
    var M0 = .0
    var M1 = .0
    var M2 = .0
    var M3 = .0
    updateState()

    def case1(): Unit = {
      M0 = m00
      M2 = m01
      M3 = m10
      M1 = m11
      if ((M0) * (M2) + (M3) * (M1) != 0) { // Transformed unit vectors are not perpendicular...
        ret = AffineTransform.TYPE_GENERAL_TRANSFORM
      } else {
        sgn0 = M0 >= 0.0
        sgn1 = M1 >= 0.0
        if (sgn0 == sgn1) { // sgn(M0) == sgn(M1) therefore sgn(M2) == -sgn(M3)
          // This is the "unflipped" (right-handed) state
          if (M0 != M1 || M2 != -M3) ret |= (AffineTransform.TYPE_GENERAL_ROTATION | AffineTransform.TYPE_GENERAL_SCALE)
          else if (M0 * M1 - M2 * M3 != 1.0) ret |= (AffineTransform.TYPE_GENERAL_ROTATION | AffineTransform.TYPE_UNIFORM_SCALE)
          else ret |= AffineTransform.TYPE_GENERAL_ROTATION
        }
        else { // sgn(M0) == -sgn(M1) therefore sgn(M2) == sgn(M3)
          // This is the "flipped" (left-handed) state
          if (M0 != -M1 || M2 != M3) ret |= (AffineTransform.TYPE_GENERAL_ROTATION | AffineTransform.TYPE_FLIP | AffineTransform.TYPE_GENERAL_SCALE)
          else if (M0 * M1 - M2 * M3 != 1.0) ret |= (AffineTransform.TYPE_GENERAL_ROTATION | AffineTransform.TYPE_FLIP | AffineTransform.TYPE_UNIFORM_SCALE)
          else ret |= (AffineTransform.TYPE_GENERAL_ROTATION | AffineTransform.TYPE_FLIP)
        }
      }
    }

    def case2(): Unit = {
      M0 = m01
      sgn0 = (M0) >= 0.0
      M1 = m10
      sgn1 = (M1) >= 0.0
      if (sgn0 != sgn1) { // Different signs - simple 90 degree rotation
        if (M0 != -M1) ret |= (AffineTransform.TYPE_QUADRANT_ROTATION | AffineTransform.TYPE_GENERAL_SCALE)
        else if (M0 != 1.0 && M0 != -1.0) ret |= (AffineTransform.TYPE_QUADRANT_ROTATION | AffineTransform.TYPE_UNIFORM_SCALE)
        else ret |= AffineTransform.TYPE_QUADRANT_ROTATION
      }
      else { // Same signs - 90 degree rotation plus an axis flip too
        if (M0 == M1) ret |= (AffineTransform.TYPE_QUADRANT_ROTATION | AffineTransform.TYPE_FLIP | AffineTransform.TYPE_UNIFORM_SCALE)
        else ret |= (AffineTransform.TYPE_QUADRANT_ROTATION | AffineTransform.TYPE_FLIP | AffineTransform.TYPE_GENERAL_SCALE)
      }
    }

    def case3(): Unit = {
      M0 = m00
      sgn0 = (M0) >= 0.0
      M1 = m11
      sgn1 = (M1) >= 0.0
      if (sgn0 == sgn1) if (sgn0) { // Both scaling factors non-negative - simple scale
        // Note: APPLY_SCALE implies M0, M1 are not both 1
        if (M0 == M1) ret |= AffineTransform.TYPE_UNIFORM_SCALE
        else ret |= AffineTransform.TYPE_GENERAL_SCALE
      }
      else { // Both scaling factors negative - 180 degree rotation
        if (M0 != M1) ret |= (AffineTransform.TYPE_QUADRANT_ROTATION | AffineTransform.TYPE_GENERAL_SCALE)
        else if (M0 != -1.0) ret |= (AffineTransform.TYPE_QUADRANT_ROTATION | AffineTransform.TYPE_UNIFORM_SCALE)
        else ret |= AffineTransform.TYPE_QUADRANT_ROTATION
      }
      else { // Scaling factor signs different - flip about some axis
        if (M0 == -M1) if (M0 == 1.0 || M0 == -1.0) ret |= AffineTransform.TYPE_FLIP
        else ret |= (AffineTransform.TYPE_FLIP | AffineTransform.TYPE_UNIFORM_SCALE)
        else ret |= (AffineTransform.TYPE_FLIP | AffineTransform.TYPE_GENERAL_SCALE)
      }
    }

    state match {
      case AffineTransform.APPLY_SHEAR_SCALE_TRANSLATE =>
        ret = AffineTransform.TYPE_TRANSLATION
        case1()
      case AffineTransform.APPLY_SHEAR_SCALE =>
        case1()
      case (AffineTransform.APPLY_SHEAR_TRANSLATE) =>
        ret = AffineTransform.TYPE_TRANSLATION
        case2()
      case (AffineTransform.APPLY_SHEAR) =>
        case2()
      case (AffineTransform.APPLY_SCALE_TRANSLATE) =>
        ret = AffineTransform.TYPE_TRANSLATION
        case3()
      case (AffineTransform.APPLY_SCALE) =>
        case3()
      case (AffineTransform.APPLY_TRANSLATE) =>
        ret = AffineTransform.TYPE_TRANSLATION
      case (AffineTransform.APPLY_IDENTITY) =>
        ()
    case _ =>
      stateError()

    }
    this.tpe = ret
  }

  /**
    * Returns the determinant of the matrix representation of the transform.
    * The determinant is useful both to determine if the transform can
    * be inverted and to get a single value representing the
    * combined X and Y scaling of the transform.
    * <p>
    * If the determinant is non-zero, then this transform is
    * invertible and the various methods that depend on the inverse
    * transform do not need to throw a
    * `NoninvertibleTransformException`.
    * If the determinant is zero then this transform can not be
    * inverted since the transform maps all input coordinates onto
    * a line or a point.
    * If the determinant is near enough to zero then inverse transform
    * operations might not carry enough precision to produce meaningful
    * results.
    * <p>
    * If this transform represents a uniform scale, as indicated by
    * the `getType` method then the determinant also
    * represents the square of the uniform scale factor by which all of
    * the points are expanded from or contracted towards the origin.
    * If this transform represents a non-uniform scale or more general
    * transform then the determinant is not likely to represent a
    * value useful for any purpose other than determining if inverse
    * transforms are possible.
    * <p>
    * Mathematically, the determinant is calculated using the formula:
    * <pre>
    * |  m00  m01  m02  |
    * |  m10  m11  m12  |  =  m00 * m11 - m01 * m10
    * |   0    0    1   |
    * </pre>
    *
    * @return the determinant of the matrix used to transform the
    *         coordinates.
    * @see #getType
    * @see #createInverse
    * @see #inverseTransform
    * @see #TYPE_UNIFORM_SCALE
    * @since 1.2
    */
  def getDeterminant: Double = state match {
    case APPLY_SHEAR_SCALE_TRANSLATE | APPLY_SHEAR_SCALE  => m00 * m11 - m01 * m10
    case APPLY_SHEAR_TRANSLATE | APPLY_SHEAR              => -(m01 * m10)
    case APPLY_SCALE_TRANSLATE | APPLY_SCALE              => m00 * m11
    case APPLY_TRANSLATE | APPLY_IDENTITY                 => 1.0
    case _ => stateError()
  }

  /**
    * Manually recalculates the state of the transform when the matrix
    * changes too much to predict the effects on the state.
    * The following table specifies what the various settings of the
    * state field say about the values of the corresponding matrix
    * element fields.
    * Note that the rules governing the SCALE fields are slightly
    * different depending on whether the SHEAR flag is also set.
    * <pre>
    * SCALE            SHEAR          TRANSLATE
    * m00/m11          m01/m10          m02/m12
    *
    * IDENTITY             1.0              0.0              0.0
    * TRANSLATE (TR)       1.0              0.0          not both 0.0
    * SCALE (SC)       not both 1.0         0.0              0.0
    * TR | SC          not both 1.0         0.0          not both 0.0
    * SHEAR (SH)           0.0          not both 0.0         0.0
    * TR | SH              0.0          not both 0.0     not both 0.0
    * SC | SH          not both 0.0     not both 0.0         0.0
    * TR | SC | SH     not both 0.0     not both 0.0     not both 0.0
    * </pre>
    */
  private[geom] def updateState(): Unit = {
    if (m01 == 0.0 && m10 == 0.0) if (m00 == 1.0 && m11 == 1.0) if (m02 == 0.0 && m12 == 0.0) {
      state = AffineTransform.APPLY_IDENTITY
      tpe = AffineTransform.TYPE_IDENTITY
    }
    else {
      state = AffineTransform.APPLY_TRANSLATE
      tpe = AffineTransform.TYPE_TRANSLATION
    }
    else if (m02 == 0.0 && m12 == 0.0) {
      state = AffineTransform.APPLY_SCALE
      tpe = AffineTransform.TYPE_UNKNOWN
    }
    else {
      state = AffineTransform.APPLY_SCALE | AffineTransform.APPLY_TRANSLATE
      tpe = AffineTransform.TYPE_UNKNOWN
    }
    else if (m00 == 0.0 && m11 == 0.0) if (m02 == 0.0 && m12 == 0.0) {
      state = AffineTransform.APPLY_SHEAR
      tpe = AffineTransform.TYPE_UNKNOWN
    }
    else {
      state = AffineTransform.APPLY_SHEAR | AffineTransform.APPLY_TRANSLATE
      tpe = AffineTransform.TYPE_UNKNOWN
    }
    else if (m02 == 0.0 && m12 == 0.0) {
      state = AffineTransform.APPLY_SHEAR | AffineTransform.APPLY_SCALE
      tpe = AffineTransform.TYPE_UNKNOWN
    }
    else {
      state = AffineTransform.APPLY_SHEAR | AffineTransform.APPLY_SCALE | AffineTransform.APPLY_TRANSLATE
      tpe = AffineTransform.TYPE_UNKNOWN
    }
  }

  /*
     * Convenience method used internally to throw exceptions when
     * a case was forgotten in a switch statement.
     */
  private def stateError(): Nothing = {
    throw new InternalError("missing case in transform state switch")
  }

  /**
    * Retrieves the 6 specifiable values in the 3x3 affine transformation
    * matrix and places them into an array of double precisions values.
    * The values are stored in the array as
    * {&nbsp;m00&nbsp;m10&nbsp;m01&nbsp;m11&nbsp;m02&nbsp;m12&nbsp;}.
    * An array of 4 doubles can also be specified, in which case only the
    * first four elements representing the non-transform
    * parts of the array are retrieved and the values are stored into
    * the array as {&nbsp;m00&nbsp;m10&nbsp;m01&nbsp;m11&nbsp;}
    *
    * @param flatmatrix the double array used to store the returned
    *                   values.
    * @see #getScaleX
    * @see #getScaleY
    * @see #getShearX
    * @see #getShearY
    * @see #getTranslateX
    * @see #getTranslateY
    * @since 1.2
    */
  def getMatrix(flatmatrix: Array[Double]): Unit = {
    flatmatrix(0) = m00
    flatmatrix(1) = m10
    flatmatrix(2) = m01
    flatmatrix(3) = m11
    if (flatmatrix.length > 5) {
      flatmatrix(4) = m02
      flatmatrix(5) = m12
    }
  }

  /**
    * Returns the `m00` element of the 3x3 affine transformation matrix.
    * This matrix factor determines how input X coordinates will affect output
    * X coordinates and is one element of the scale of the transform.
    * To measure the full amount by which X coordinates are stretched or
    * contracted by this transform, use the following code:
    * <pre>
    * Point2D p = new Point2D.Double(1, 0);
    * p = tx.deltaTransform(p, p);
    * double scaleX = p.distance(0, 0);
    * </pre>
    *
    * @return a double value that is `m00` element of the
    *         3x3 affine transformation matrix.
    * @see #getMatrix
    * @since 1.2
    */
  def getScaleX: Double = m00

  /**
    * Returns the `m11` element of the 3x3 affine transformation matrix.
    * This matrix factor determines how input Y coordinates will affect output
    * Y coordinates and is one element of the scale of the transform.
    * To measure the full amount by which Y coordinates are stretched or
    * contracted by this transform, use the following code:
    * <pre>
    * Point2D p = new Point2D.Double(0, 1);
    * p = tx.deltaTransform(p, p);
    * double scaleY = p.distance(0, 0);
    * </pre>
    *
    * @return a double value that is `m11` element of the
    *         3x3 affine transformation matrix.
    * @see #getMatrix
    * @since 1.2
    */
  def getScaleY: Double = m11

  /**
    * Returns the X coordinate shearing element (m01) of the 3x3
    * affine transformation matrix.
    *
    * @return a double value that is the X coordinate of the shearing
    *         element of the affine transformation matrix.
    * @see #getMatrix
    * @since 1.2
    */
  def getShearX: Double = m01

  /**
    * Returns the Y coordinate shearing element (m10) of the 3x3
    * affine transformation matrix.
    *
    * @return a double value that is the Y coordinate of the shearing
    *         element of the affine transformation matrix.
    * @see #getMatrix
    * @since 1.2
    */
  def getShearY: Double = m10

  /**
    * Returns the X coordinate of the translation element (m02) of the
    * 3x3 affine transformation matrix.
    *
    * @return a double value that is the X coordinate of the translation
    *         element of the affine transformation matrix.
    * @see #getMatrix
    * @since 1.2
    */
  def getTranslateX: Double = m02

  /**
    * Returns the Y coordinate of the translation element (m12) of the
    * 3x3 affine transformation matrix.
    *
    * @return a double value that is the Y coordinate of the translation
    *         element of the affine transformation matrix.
    * @see #getMatrix
    * @since 1.2
    */
  def getTranslateY: Double = m12

  /**
    * Concatenates this transform with a translation transformation.
    * This is equivalent to calling concatenate(T), where T is an
    * `AffineTransform` represented by the following matrix:
    * <pre>
    * [   1    0    tx  ]
    * [   0    1    ty  ]
    * [   0    0    1   ]
    * </pre>
    *
    * @param tx the distance by which coordinates are translated in the
    *           X axis direction
    * @param ty the distance by which coordinates are translated in the
    *           Y axis direction
    * @since 1.2
    */
  def translate(tx: Double, ty: Double): Unit = {
    state match {
      case (AffineTransform.APPLY_SHEAR_SCALE_TRANSLATE) =>
        m02 = tx * m00 + ty * m01 + m02
        m12 = tx * m10 + ty * m11 + m12
        if (m02 == 0.0 && m12 == 0.0) {
          state = AffineTransform.APPLY_SHEAR | AffineTransform.APPLY_SCALE
          if (tpe != AffineTransform.TYPE_UNKNOWN) tpe -= AffineTransform.TYPE_TRANSLATION
        }
      case (AffineTransform.APPLY_SHEAR_SCALE) =>
        m02 = tx * m00 + ty * m01
        m12 = tx * m10 + ty * m11
        if (m02 != 0.0 || m12 != 0.0) {
          state = AffineTransform.APPLY_SHEAR | AffineTransform.APPLY_SCALE | AffineTransform.APPLY_TRANSLATE
          tpe |= AffineTransform.TYPE_TRANSLATION
        }
      case (AffineTransform.APPLY_SHEAR_TRANSLATE) =>
        m02 = ty * m01 + m02
        m12 = tx * m10 + m12
        if (m02 == 0.0 && m12 == 0.0) {
          state = AffineTransform.APPLY_SHEAR
          if (tpe != AffineTransform.TYPE_UNKNOWN) tpe -= AffineTransform.TYPE_TRANSLATION
        }
      case (AffineTransform.APPLY_SHEAR) =>
        m02 = ty * m01
        m12 = tx * m10
        if (m02 != 0.0 || m12 != 0.0) {
          state = AffineTransform.APPLY_SHEAR | AffineTransform.APPLY_TRANSLATE
          tpe |= AffineTransform.TYPE_TRANSLATION
        }
      case (AffineTransform.APPLY_SCALE_TRANSLATE) =>
        m02 = tx * m00 + m02
        m12 = ty * m11 + m12
        if (m02 == 0.0 && m12 == 0.0) {
          state = AffineTransform.APPLY_SCALE
          if (tpe != AffineTransform.TYPE_UNKNOWN) tpe -= AffineTransform.TYPE_TRANSLATION
        }
      case (AffineTransform.APPLY_SCALE) =>
        m02 = tx * m00
        m12 = ty * m11
        if (m02 != 0.0 || m12 != 0.0) {
          state = AffineTransform.APPLY_SCALE | AffineTransform.APPLY_TRANSLATE
          tpe |= AffineTransform.TYPE_TRANSLATION
        }
      case (AffineTransform.APPLY_TRANSLATE) =>
        m02 = tx + m02
        m12 = ty + m12
        if (m02 == 0.0 && m12 == 0.0) {
          state = AffineTransform.APPLY_IDENTITY
          tpe = AffineTransform.TYPE_IDENTITY
        }
      case (AffineTransform.APPLY_IDENTITY) =>
        m02 = tx
        m12 = ty
        if (tx != 0.0 || ty != 0.0) {
          state = AffineTransform.APPLY_TRANSLATE
          tpe = AffineTransform.TYPE_TRANSLATION
        }

      case _ =>
        stateError()
    }
  }

  private def rotate90(): Unit = {
    var M0 = m00
    m00 = m01
    m01 = -M0
    M0 = m10
    m10 = m11
    m11 = -M0
    var state = AffineTransform.rot90conversion(this.state)
    if ((state & (AffineTransform.APPLY_SHEAR | AffineTransform.APPLY_SCALE)) == AffineTransform.APPLY_SCALE && m00 == 1.0 && m11 == 1.0) state -= AffineTransform.APPLY_SCALE
    this.state = state
    tpe = AffineTransform.TYPE_UNKNOWN
  }

  private def rotate180(): Unit = {
    m00 = -m00
    m11 = -m11
    val state = this.state
    if ((state & AffineTransform.APPLY_SHEAR) != 0) { // If there was a shear, then this rotation has no
      // effect on the state.
      m01 = -m01
      m10 = -m10
    }
    else { // No shear means the SCALE state may toggle when
      // m00 and m11 are negated.
      if (m00 == 1.0 && m11 == 1.0) this.state = state & ~AffineTransform.APPLY_SCALE
      else this.state = state | AffineTransform.APPLY_SCALE
    }
    tpe = AffineTransform.TYPE_UNKNOWN
  }

  private def rotate270(): Unit = {
    var M0 = m00
    m00 = -m01
    m01 = M0
    M0 = m10
    m10 = -m11
    m11 = M0
    var state = AffineTransform.rot90conversion(this.state)
    if ((state & (AffineTransform.APPLY_SHEAR | AffineTransform.APPLY_SCALE)) == AffineTransform.APPLY_SCALE && m00 == 1.0 && m11 == 1.0) state -= AffineTransform.APPLY_SCALE
    this.state = state
    tpe = AffineTransform.TYPE_UNKNOWN
  }

  /**
    * Concatenates this transform with a rotation transformation.
    * This is equivalent to calling concatenate(R), where R is an
    * `AffineTransform` represented by the following matrix:
    * <pre>
    * [   cos(theta)    -sin(theta)    0   ]
    * [   sin(theta)     cos(theta)    0   ]
    * [       0              0         1   ]
    * </pre>
    * Rotating by a positive angle theta rotates points on the positive
    * X axis toward the positive Y axis.
    * Note also the discussion of
    * <a href="#quadrantapproximation">Handling 90-Degree Rotations</a>
    * above.
    *
    * @param theta the angle of rotation measured in radians
    * @since 1.2
    */
  def rotate(theta: Double): Unit = {
    val sin = Math.sin(theta)
    if (sin == 1.0) rotate90()
    else if (sin == -1.0) rotate270()
    else {
      val cos = Math.cos(theta)
      if (cos == -1.0) rotate180()
      else if (cos != 1.0) {
        var M0 = .0
        var M1 = .0
        M0 = m00
        M1 = m01
        m00 = cos * M0 + sin * M1
        m01 = -sin * M0 + cos * M1
        M0 = m10
        M1 = m11
        m10 = cos * M0 + sin * M1
        m11 = -sin * M0 + cos * M1
        updateState()
      }
    }
  }

  /**
    * Concatenates this transform with a transform that rotates
    * coordinates around an anchor point.
    * This operation is equivalent to translating the coordinates so
    * that the anchor point is at the origin (S1), then rotating them
    * about the new origin (S2), and finally translating so that the
    * intermediate origin is restored to the coordinates of the original
    * anchor point (S3).
    * <p>
    * This operation is equivalent to the following sequence of calls:
    * <pre>
    * translate(anchorx, anchory);      // S3: final translation
    * rotate(theta);                    // S2: rotate around anchor
    * translate(-anchorx, -anchory);    // S1: translate anchor to origin
    * </pre>
    * Rotating by a positive angle theta rotates points on the positive
    * X axis toward the positive Y axis.
    * Note also the discussion of
    * <a href="#quadrantapproximation">Handling 90-Degree Rotations</a>
    * above.
    *
    * @param theta   the angle of rotation measured in radians
    * @param anchorx the X coordinate of the rotation anchor point
    * @param anchory the Y coordinate of the rotation anchor point
    * @since 1.2
    */
  def rotate(theta: Double, anchorx: Double, anchory: Double): Unit = { // REMIND: Simple for now - optimize later
    translate(anchorx, anchory)
    rotate(theta)
    translate(-anchorx, -anchory)
  }

  /**
    * Concatenates this transform with a transform that rotates
    * coordinates according to a rotation vector.
    * All coordinates rotate about the origin by the same amount.
    * The amount of rotation is such that coordinates along the former
    * positive X axis will subsequently align with the vector pointing
    * from the origin to the specified vector coordinates.
    * If both `vecx` and `vecy` are 0.0,
    * no additional rotation is added to this transform.
    * This operation is equivalent to calling:
    * <pre>
    * rotate(Math.atan2(vecy, vecx));
    * </pre>
    *
    * @param vecx the X coordinate of the rotation vector
    * @param vecy the Y coordinate of the rotation vector
    * @since 1.6
    */
  def rotate(vecx: Double, vecy: Double): Unit = {
    if (vecy == 0.0) {
      if (vecx < 0.0) rotate180()
      // If vecx > 0.0 - no rotation
      // If vecx == 0.0 - undefined rotation - treat as no rotation
    }
    else if (vecx == 0.0) if (vecy > 0.0) rotate90()
    else { // vecy must be < 0.0
      rotate270()
    }
    else {
      val len = Math.sqrt(vecx * vecx + vecy * vecy)
      val sin = vecy / len
      val cos = vecx / len
      var M0 = .0
      var M1 = .0
      M0 = m00
      M1 = m01
      m00 = cos * M0 + sin * M1
      m01 = -sin * M0 + cos * M1
      M0 = m10
      M1 = m11
      m10 = cos * M0 + sin * M1
      m11 = -sin * M0 + cos * M1
      updateState()
    }
  }

  /**
    * Concatenates this transform with a transform that rotates
    * coordinates around an anchor point according to a rotation
    * vector.
    * All coordinates rotate about the specified anchor coordinates
    * by the same amount.
    * The amount of rotation is such that coordinates along the former
    * positive X axis will subsequently align with the vector pointing
    * from the origin to the specified vector coordinates.
    * If both `vecx` and `vecy` are 0.0,
    * the transform is not modified in any way.
    * This method is equivalent to calling:
    * <pre>
    * rotate(Math.atan2(vecy, vecx), anchorx, anchory);
    * </pre>
    *
    * @param vecx    the X coordinate of the rotation vector
    * @param vecy    the Y coordinate of the rotation vector
    * @param anchorx the X coordinate of the rotation anchor point
    * @param anchory the Y coordinate of the rotation anchor point
    * @since 1.6
    */
  def rotate(vecx: Double, vecy: Double, anchorx: Double, anchory: Double): Unit = {
    translate(anchorx, anchory)
    rotate(vecx, vecy)
    translate(-anchorx, -anchory)
  }

  /**
    * Concatenates this transform with a transform that rotates
    * coordinates by the specified number of quadrants.
    * This is equivalent to calling:
    * <pre>
    * rotate(numquadrants * Math.PI / 2.0);
    * </pre>
    * Rotating by a positive number of quadrants rotates points on
    * the positive X axis toward the positive Y axis.
    *
    * @param numquadrants the number of 90 degree arcs to rotate by
    */
  def quadrantRotate(numquadrants: Int): Unit =
    numquadrants & 3 match {
      case 0 =>
      case 1 => rotate90()
      case 2 => rotate180()
      case 3 => rotate270()
    }

  /**
    * Concatenates this transform with a transform that rotates
    * coordinates by the specified number of quadrants around
    * the specified anchor point.
    * This method is equivalent to calling:
    * <pre>
    * rotate(numquadrants * Math.PI / 2.0, anchorx, anchory);
    * </pre>
    * Rotating by a positive number of quadrants rotates points on
    * the positive X axis toward the positive Y axis.
    *
    * @param numquadrants the number of 90 degree arcs to rotate by
    * @param anchorx      the X coordinate of the rotation anchor point
    * @param anchory      the Y coordinate of the rotation anchor point
    */
  def quadrantRotate(numquadrants: Int, anchorx: Double, anchory: Double): Unit = {
    numquadrants & 3 match {
      case 0 =>
        return
      case 1 =>
        m02 += anchorx * (m00 - m01) + anchory * (m01 + m00)
        m12 += anchorx * (m10 - m11) + anchory * (m11 + m10)
        rotate90()

      case 2 =>
        m02 += anchorx * (m00 + m00) + anchory * (m01 + m01)
        m12 += anchorx * (m10 + m10) + anchory * (m11 + m11)
        rotate180()

      case 3 =>
        m02 += anchorx * (m00 + m01) + anchory * (m01 - m00)
        m12 += anchorx * (m10 + m11) + anchory * (m11 - m10)
        rotate270()

    }
    if (m02 == 0.0 && m12 == 0.0) state &= ~AffineTransform.APPLY_TRANSLATE
    else state |= AffineTransform.APPLY_TRANSLATE
  }

  /**
    * Concatenates this transform with a scaling transformation.
    * This is equivalent to calling concatenate(S), where S is an
    * `AffineTransform` represented by the following matrix:
    * <pre>
    * [   sx   0    0   ]
    * [   0    sy   0   ]
    * [   0    0    1   ]
    * </pre>
    *
    * @param sx the factor by which coordinates are scaled along the
    *           X axis direction
    * @param sy the factor by which coordinates are scaled along the
    *           Y axis direction
    */
  def scale(sx: Double, sy: Double): Unit = {
    var state = this.state

    def case1(): Unit = {
      m01 *= sy
      m10 *= sx
      if (m01 == 0 && m10 == 0) {
        state &= AffineTransform.APPLY_TRANSLATE
        if (m00 == 1.0 && m11 == 1.0) this.tpe = if (state == AffineTransform.APPLY_IDENTITY) AffineTransform.TYPE_IDENTITY
        else AffineTransform.TYPE_TRANSLATION
        else {
          state |= AffineTransform.APPLY_SCALE
          this.tpe = AffineTransform.TYPE_UNKNOWN
        }
        this.state = state
      }
    }

    state match {
      case APPLY_SHEAR_SCALE_TRANSLATE | APPLY_SHEAR_SCALE =>
        m00 *= sx
        m11 *= sy
        case1()
      case APPLY_SHEAR_TRANSLATE | APPLY_SHEAR =>
        case1()

      case APPLY_SCALE_TRANSLATE | APPLY_SCALE =>
        m00 *= sx
        m11 *= sy
        if (m00 == 1.0 && m11 == 1.0) {
          state &= AffineTransform.APPLY_TRANSLATE
          this.state = state
          this.tpe = if (state == AffineTransform.APPLY_IDENTITY) AffineTransform.TYPE_IDENTITY
          else AffineTransform.TYPE_TRANSLATION
        }
        else this.tpe = AffineTransform.TYPE_UNKNOWN

      case APPLY_TRANSLATE | APPLY_IDENTITY =>
        m00 = sx
        m11 = sy
        if (sx != 1.0 || sy != 1.0) {
          this.state = state | AffineTransform.APPLY_SCALE
          this.tpe = AffineTransform.TYPE_UNKNOWN
        }

      case _ =>
        stateError()
    }
  }

  /**
    * Concatenates this transform with a shearing transformation.
    * This is equivalent to calling concatenate(SH), where SH is an
    * `AffineTransform` represented by the following matrix:
    * <pre>
    * [   1   shx   0   ]
    * [  shy   1    0   ]
    * [   0    0    1   ]
    * </pre>
    *
    * @param shx the multiplier by which coordinates are shifted in the
    *            direction of the positive X axis as a factor of their Y coordinate
    * @param shy the multiplier by which coordinates are shifted in the
    *            direction of the positive Y axis as a factor of their X coordinate
    * @since 1.2
    */
  def shear(shx: Double, shy: Double): Unit = {
    val state = this.state
    state match {
      case APPLY_SHEAR_SCALE_TRANSLATE | APPLY_SHEAR_SCALE =>
        var M0 = .0
        var M1 = .0
        M0 = m00
        M1 = m01
        m00 = M0 + M1 * shy
        m01 = M0 * shx + M1
        M0 = m10
        M1 = m11
        m10 = M0 + M1 * shy
        m11 = M0 * shx + M1
        updateState()

      case APPLY_SHEAR_TRANSLATE | APPLY_SHEAR =>
        m00 = m01 * shy
        m11 = m10 * shx
        if (m00 != 0.0 || m11 != 0.0) this.state = state | AffineTransform.APPLY_SCALE
        this.tpe = AffineTransform.TYPE_UNKNOWN

      case APPLY_SCALE_TRANSLATE | APPLY_SCALE =>
        m01 = m00 * shx
        m10 = m11 * shy
        if (m01 != 0.0 || m10 != 0.0) this.state = state | AffineTransform.APPLY_SHEAR
        this.tpe = AffineTransform.TYPE_UNKNOWN

      case APPLY_TRANSLATE | APPLY_IDENTITY =>
        m01 = shx
        m10 = shy
        if (m01 != 0.0 || m10 != 0.0) {
          this.state = state | AffineTransform.APPLY_SCALE | AffineTransform.APPLY_SHEAR
          this.tpe = AffineTransform.TYPE_UNKNOWN
        }

      case _ =>
        stateError()
    }
  }

  /**
    * Resets this transform to the Identity transform.
    *
    * @since 1.2
    */
  def setToIdentity(): Unit = {
    m00 = 1.0
    m11 = 1.0
    m10 = 0.0
    m01 = 0.0
    m02 = 0.0
    m12 = 0.0
    state = AffineTransform.APPLY_IDENTITY
    tpe   = AffineTransform.TYPE_IDENTITY
  }

  /**
    * Sets this transform to a translation transformation.
    * The matrix representing this transform becomes:
    * <pre>
    * [   1    0    tx  ]
    * [   0    1    ty  ]
    * [   0    0    1   ]
    * </pre>
    *
    * @param tx the distance by which coordinates are translated in the
    *           X axis direction
    * @param ty the distance by which coordinates are translated in the
    *           Y axis direction
    * @since 1.2
    */
  def setToTranslation(tx: Double, ty: Double): Unit = {
    m00 = 1.0
    m10 = 0.0
    m01 = 0.0
    m11 = 1.0
    m02 = tx
    m12 = ty
    if (tx != 0.0 || ty != 0.0) {
      state = AffineTransform.APPLY_TRANSLATE
      tpe   = AffineTransform.TYPE_TRANSLATION
    }
    else {
      state = AffineTransform.APPLY_IDENTITY
      tpe   = AffineTransform.TYPE_IDENTITY
    }
  }

  /**
    * Sets this transform to a rotation transformation.
    * The matrix representing this transform becomes:
    * <pre>
    * [   cos(theta)    -sin(theta)    0   ]
    * [   sin(theta)     cos(theta)    0   ]
    * [       0              0         1   ]
    * </pre>
    * Rotating by a positive angle theta rotates points on the positive
    * X axis toward the positive Y axis.
    * Note also the discussion of
    * <a href="#quadrantapproximation">Handling 90-Degree Rotations</a>
    * above.
    *
    * @param theta the angle of rotation measured in radians
    * @since 1.2
    */
  def setToRotation(theta: Double): Unit = {
    var sin = Math.sin(theta)
    var cos = .0
    if (sin == 1.0 || sin == -1.0) {
      cos   = 0.0
      state = AffineTransform.APPLY_SHEAR
      tpe   = AffineTransform.TYPE_QUADRANT_ROTATION
    }
    else {
      cos = Math.cos(theta)
      if (cos == -1.0) {
        sin   = 0.0
        state = AffineTransform.APPLY_SCALE
        tpe   = AffineTransform.TYPE_QUADRANT_ROTATION
      }
      else if (cos == 1.0) {
        sin   = 0.0
        state = AffineTransform.APPLY_IDENTITY
        tpe   = AffineTransform.TYPE_IDENTITY
      }
      else {
        state = AffineTransform.APPLY_SHEAR | AffineTransform.APPLY_SCALE
        tpe   = AffineTransform.TYPE_GENERAL_ROTATION
      }
    }
    m00 = cos
    m10 = sin
    m01 = -sin
    m11 = cos
    m02 = 0.0
    m12 = 0.0
  }

  /**
    * Sets this transform to a translated rotation transformation.
    * This operation is equivalent to translating the coordinates so
    * that the anchor point is at the origin (S1), then rotating them
    * about the new origin (S2), and finally translating so that the
    * intermediate origin is restored to the coordinates of the original
    * anchor point (S3).
    * <p>
    * This operation is equivalent to the following sequence of calls:
    * <pre>
    * setToTranslation(anchorx, anchory); // S3: final translation
    * rotate(theta);                      // S2: rotate around anchor
    * translate(-anchorx, -anchory);      // S1: translate anchor to origin
    * </pre>
    * The matrix representing this transform becomes:
    * <pre>
    * [   cos(theta)    -sin(theta)    x-x*cos+y*sin  ]
    * [   sin(theta)     cos(theta)    y-x*sin-y*cos  ]
    * [       0              0               1        ]
    * </pre>
    * Rotating by a positive angle theta rotates points on the positive
    * X axis toward the positive Y axis.
    * Note also the discussion of
    * <a href="#quadrantapproximation">Handling 90-Degree Rotations</a>
    * above.
    *
    * @param theta   the angle of rotation measured in radians
    * @param anchorx the X coordinate of the rotation anchor point
    * @param anchory the Y coordinate of the rotation anchor point
    * @since 1.2
    */
  def setToRotation(theta: Double, anchorx: Double, anchory: Double): Unit = {
    setToRotation(theta)
    val sin = m10
    val oneMinusCos = 1.0 - m00
    m02 = anchorx * oneMinusCos + anchory * sin
    m12 = anchory * oneMinusCos - anchorx * sin
    if (m02 != 0.0 || m12 != 0.0) {
      state |= AffineTransform.APPLY_TRANSLATE
      tpe |= AffineTransform.TYPE_TRANSLATION
    }
  }

  /**
    * Sets this transform to a rotation transformation that rotates
    * coordinates according to a rotation vector.
    * All coordinates rotate about the origin by the same amount.
    * The amount of rotation is such that coordinates along the former
    * positive X axis will subsequently align with the vector pointing
    * from the origin to the specified vector coordinates.
    * If both `vecx` and `vecy` are 0.0,
    * the transform is set to an identity transform.
    * This operation is equivalent to calling:
    * <pre>
    * setToRotation(Math.atan2(vecy, vecx));
    * </pre>
    *
    * @param vecx the X coordinate of the rotation vector
    * @param vecy the Y coordinate of the rotation vector
    * @since 1.6
    */
  def setToRotation(vecx: Double, vecy: Double): Unit = {
    var sin = .0
    var cos = .0
    if (vecy == 0) {
      sin = 0.0
      if (vecx < 0.0) {
        cos   = -1.0
        state = AffineTransform.APPLY_SCALE
        tpe   = AffineTransform.TYPE_QUADRANT_ROTATION
      }
      else {
        cos   = 1.0
        state = AffineTransform.APPLY_IDENTITY
        tpe   = AffineTransform.TYPE_IDENTITY
      }
    }
    else if (vecx == 0) {
      cos   = 0.0
      sin   = if (vecy > 0.0) 1.0 else -1.0
      state = AffineTransform.APPLY_SHEAR
      tpe   = AffineTransform.TYPE_QUADRANT_ROTATION
    }
    else {
      val len = Math.sqrt(vecx * vecx + vecy * vecy)
      cos   = vecx / len
      sin   = vecy / len
      state = AffineTransform.APPLY_SHEAR | AffineTransform.APPLY_SCALE
      tpe   = AffineTransform.TYPE_GENERAL_ROTATION
    }
    m00 = cos
    m10 = sin
    m01 = -sin
    m11 = cos
    m02 = 0.0
    m12 = 0.0
  }

  /**
    * Sets this transform to a rotation transformation that rotates
    * coordinates around an anchor point according to a rotation
    * vector.
    * All coordinates rotate about the specified anchor coordinates
    * by the same amount.
    * The amount of rotation is such that coordinates along the former
    * positive X axis will subsequently align with the vector pointing
    * from the origin to the specified vector coordinates.
    * If both `vecx` and `vecy` are 0.0,
    * the transform is set to an identity transform.
    * This operation is equivalent to calling:
    * <pre>
    * setToTranslation(Math.atan2(vecy, vecx), anchorx, anchory);
    * </pre>
    *
    * @param vecx    the X coordinate of the rotation vector
    * @param vecy    the Y coordinate of the rotation vector
    * @param anchorx the X coordinate of the rotation anchor point
    * @param anchory the Y coordinate of the rotation anchor point
    * @since 1.6
    */
  def setToRotation(vecx: Double, vecy: Double, anchorx: Double, anchory: Double): Unit = {
    setToRotation(vecx, vecy)
    val sin = m10
    val oneMinusCos = 1.0 - m00
    m02 = anchorx * oneMinusCos + anchory * sin
    m12 = anchory * oneMinusCos - anchorx * sin
    if (m02 != 0.0 || m12 != 0.0) {
      state |= AffineTransform.APPLY_TRANSLATE
      tpe   |= AffineTransform.TYPE_TRANSLATION
    }
  }

  /**
    * Sets this transform to a rotation transformation that rotates
    * coordinates by the specified number of quadrants.
    * This operation is equivalent to calling:
    * <pre>
    * setToRotation(numquadrants * Math.PI / 2.0);
    * </pre>
    * Rotating by a positive number of quadrants rotates points on
    * the positive X axis toward the positive Y axis.
    *
    * @param numquadrants the number of 90 degree arcs to rotate by
    * @since 1.6
    */
  def setToQuadrantRotation(numquadrants: Int): Unit =
    numquadrants & 3 match {
      case 0 =>
        m00 = 1.0
        m10 = 0.0
        m01 = 0.0
        m11 = 1.0
        m02 = 0.0
        m12 = 0.0
        state = AffineTransform.APPLY_IDENTITY
        tpe   = AffineTransform.TYPE_IDENTITY

      case 1 =>
        m00 = 0.0
        m10 = 1.0
        m01 = -1.0
        m11 = 0.0
        m02 = 0.0
        m12 = 0.0
        state = AffineTransform.APPLY_SHEAR
        tpe   = AffineTransform.TYPE_QUADRANT_ROTATION

      case 2 =>
        m00 = -1.0
        m10 = 0.0
        m01 = 0.0
        m11 = -1.0
        m02 = 0.0
        m12 = 0.0
        state = AffineTransform.APPLY_SCALE
        tpe   = AffineTransform.TYPE_QUADRANT_ROTATION

      case 3 =>
        m00 = 0.0
        m10 = -1.0
        m01 = 1.0
        m11 = 0.0
        m02 = 0.0
        m12 = 0.0
        state = AffineTransform.APPLY_SHEAR
        tpe   = AffineTransform.TYPE_QUADRANT_ROTATION
    }

  /**
    * Sets this transform to a translated rotation transformation
    * that rotates coordinates by the specified number of quadrants
    * around the specified anchor point.
    * This operation is equivalent to calling:
    * <pre>
    * setToRotation(numquadrants * Math.PI / 2.0, anchorx, anchory);
    * </pre>
    * Rotating by a positive number of quadrants rotates points on
    * the positive X axis toward the positive Y axis.
    *
    * @param numquadrants the number of 90 degree arcs to rotate by
    * @param anchorx      the X coordinate of the rotation anchor point
    * @param anchory      the Y coordinate of the rotation anchor point
    * @since 1.6
    */
  def setToQuadrantRotation(numquadrants: Int, anchorx: Double, anchory: Double): Unit =
    numquadrants & 3 match {
      case 0 =>
        m00 = 1.0
        m10 = 0.0
        m01 = 0.0
        m11 = 1.0
        m02 = 0.0
        m12 = 0.0
        state = AffineTransform.APPLY_IDENTITY
        tpe   = AffineTransform.TYPE_IDENTITY

      case 1 =>
        m00 = 0.0
        m10 = 1.0
        m01 = -1.0
        m11 = 0.0
        m02 = anchorx + anchory
        m12 = anchory - anchorx
        if (m02 == 0.0 && m12 == 0.0) {
          state = AffineTransform.APPLY_SHEAR
          tpe   = AffineTransform.TYPE_QUADRANT_ROTATION
        }
        else {
          state = AffineTransform.APPLY_SHEAR | AffineTransform.APPLY_TRANSLATE
          tpe   = AffineTransform.TYPE_QUADRANT_ROTATION | AffineTransform.TYPE_TRANSLATION
        }

      case 2 =>
        m00 = -1.0
        m10 = 0.0
        m01 = 0.0
        m11 = -1.0
        m02 = anchorx + anchorx
        m12 = anchory + anchory
        if (m02 == 0.0 && m12 == 0.0) {
          state = AffineTransform.APPLY_SCALE
          tpe   = AffineTransform.TYPE_QUADRANT_ROTATION
        }
        else {
          state = AffineTransform.APPLY_SCALE | AffineTransform.APPLY_TRANSLATE
          tpe   = AffineTransform.TYPE_QUADRANT_ROTATION | AffineTransform.TYPE_TRANSLATION
        }

      case 3 =>
        m00 = 0.0
        m10 = -1.0
        m01 = 1.0
        m11 = 0.0
        m02 = anchorx - anchory
        m12 = anchory + anchorx
        if (m02 == 0.0 && m12 == 0.0) {
          state = AffineTransform.APPLY_SHEAR
          tpe   = AffineTransform.TYPE_QUADRANT_ROTATION
        }
        else {
          state = AffineTransform.APPLY_SHEAR | AffineTransform.APPLY_TRANSLATE
          tpe   = AffineTransform.TYPE_QUADRANT_ROTATION | AffineTransform.TYPE_TRANSLATION
        }

    }

  /**
    * Sets this transform to a scaling transformation.
    * The matrix representing this transform becomes:
    * <pre>
    * [   sx   0    0   ]
    * [   0    sy   0   ]
    * [   0    0    1   ]
    * </pre>
    *
    * @param sx the factor by which coordinates are scaled along the
    *           X axis direction
    * @param sy the factor by which coordinates are scaled along the
    *           Y axis direction
    * @since 1.2
    */
  def setToScale(sx: Double, sy: Double): Unit = {
    m00 = sx
    m10 = 0.0
    m01 = 0.0
    m11 = sy
    m02 = 0.0
    m12 = 0.0
    if (sx != 1.0 || sy != 1.0) {
      state = AffineTransform.APPLY_SCALE
      tpe   = AffineTransform.TYPE_UNKNOWN
    }
    else {
      state = AffineTransform.APPLY_IDENTITY
      tpe   = AffineTransform.TYPE_IDENTITY
    }
  }

  /**
    * Sets this transform to a shearing transformation.
    * The matrix representing this transform becomes:
    * <pre>
    * [   1   shx   0   ]
    * [  shy   1    0   ]
    * [   0    0    1   ]
    * </pre>
    *
    * @param shx the multiplier by which coordinates are shifted in the
    *            direction of the positive X axis as a factor of their Y coordinate
    * @param shy the multiplier by which coordinates are shifted in the
    *            direction of the positive Y axis as a factor of their X coordinate
    * @since 1.2
    */
  def setToShear(shx: Double, shy: Double): Unit = {
    m00 = 1.0
    m01 = shx
    m10 = shy
    m11 = 1.0
    m02 = 0.0
    m12 = 0.0
    if (shx != 0.0 || shy != 0.0) {
      state = AffineTransform.APPLY_SHEAR | AffineTransform.APPLY_SCALE
      tpe   = AffineTransform.TYPE_UNKNOWN
    }
    else {
      state = AffineTransform.APPLY_IDENTITY
      tpe   = AffineTransform.TYPE_IDENTITY
    }
  }

  /**
    * Sets this transform to a copy of the transform in the specified
    * `AffineTransform` object.
    *
    * @param Tx the `AffineTransform` object from which to
    *           copy the transform
    * @since 1.2
    */
  def setTransform(Tx: AffineTransform): Unit = {
    this.m00 = Tx.m00
    this.m10 = Tx.m10
    this.m01 = Tx.m01
    this.m11 = Tx.m11
    this.m02 = Tx.m02
    this.m12 = Tx.m12
    this.state = Tx.state
    this.tpe = Tx.tpe
  }

  /**
    * Sets this transform to the matrix specified by the 6
    * double precision values.
    *
    * @param m00 the X coordinate scaling element of the 3x3 matrix
    * @param m10 the Y coordinate shearing element of the 3x3 matrix
    * @param m01 the X coordinate shearing element of the 3x3 matrix
    * @param m11 the Y coordinate scaling element of the 3x3 matrix
    * @param m02 the X coordinate translation element of the 3x3 matrix
    * @param m12 the Y coordinate translation element of the 3x3 matrix
    * @since 1.2
    */
  def setTransform(m00: Double, m10: Double, m01: Double, m11: Double, m02: Double, m12: Double): Unit = {
    this.m00 = m00
    this.m10 = m10
    this.m01 = m01
    this.m11 = m11
    this.m02 = m02
    this.m12 = m12
    updateState()
  }

  /**
    * Concatenates an `AffineTransform Tx` to
    * this `AffineTransform` Cx in the most commonly useful
    * way to provide a new user space
    * that is mapped to the former user space by `Tx`.
    * Cx is updated to perform the combined transformation.
    * Transforming a point p by the updated transform Cx' is
    * equivalent to first transforming p by `Tx` and then
    * transforming the result by the original transform Cx like this:
    * Cx'(p) = Cx(Tx(p))
    * In matrix notation, if this transform Cx is
    * represented by the matrix [this] and `Tx` is represented
    * by the matrix [Tx] then this method does the following:
    * <pre>
    * [this] = [this] x [Tx]
    * </pre>
    *
    * @param Tx the `AffineTransform` object to be
    *           concatenated with this `AffineTransform` object.
    * @see #preConcatenate
    * @since 1.2
    */
  @SuppressWarnings(Array("fallthrough")) def concatenate(Tx: AffineTransform): Unit = {
    ???
    var M0 = .0
    var M1 = .0
    var T00 = .0
    var T01 = .0
    var T10 = .0
    var T11 = .0
    var T02 = .0
    var T12 = .0
    val mystate = state
    val txstate = Tx.state
    ((txstate << AffineTransform.HI_SHIFT) | mystate) match {
      /* ---------- Tx == IDENTITY cases ---------- */
      case HI_IDENTITY_APPLY_IDENTITY =>
      case HI_IDENTITY_APPLY_TRANSLATE =>
      case HI_IDENTITY_APPLY_SCALE =>
      case HI_IDENTITY_APPLY_SCALE_TRANSLATE =>
      case (AffineTransform.HI_IDENTITY | AffineTransform.APPLY_SHEAR) =>
      case (AffineTransform.HI_IDENTITY | AffineTransform.APPLY_SHEAR | AffineTransform.APPLY_TRANSLATE) =>
      case (AffineTransform.HI_IDENTITY | AffineTransform.APPLY_SHEAR | AffineTransform.APPLY_SCALE) =>
      case (AffineTransform.HI_IDENTITY | AffineTransform.APPLY_SHEAR | AffineTransform.APPLY_SCALE | AffineTransform.APPLY_TRANSLATE) =>
        return
    /* ---------- this == IDENTITY cases ---------- */
    case (AffineTransform.HI_SHEAR | AffineTransform.HI_SCALE | AffineTransform.HI_TRANSLATE | AffineTransform.APPLY_IDENTITY) =>
      m01 = Tx.m01
      m10 = Tx.m10
    case (AffineTransform.HI_SCALE | AffineTransform.HI_TRANSLATE | AffineTransform.APPLY_IDENTITY) =>
      m00 = Tx.m00
      m11 = Tx.m11
    case (AffineTransform.HI_TRANSLATE | AffineTransform.APPLY_IDENTITY) =>
      m02 = Tx.m02
      m12 = Tx.m12
      state = txstate
      tpe = Tx.tpe
      return
    case (AffineTransform.HI_SHEAR | AffineTransform.HI_SCALE | AffineTransform.APPLY_IDENTITY) =>
      m01 = Tx.m01
      m10 = Tx.m10
    case (AffineTransform.HI_SCALE | AffineTransform.APPLY_IDENTITY) =>
      m00 = Tx.m00
      m11 = Tx.m11
      state = txstate
      tpe = Tx.tpe
      return
    case (AffineTransform.HI_SHEAR | AffineTransform.HI_TRANSLATE | AffineTransform.APPLY_IDENTITY) =>
      m02 = Tx.m02
      m12 = Tx.m12
    case (AffineTransform.HI_SHEAR | AffineTransform.APPLY_IDENTITY) =>
      m01 = Tx.m01
      m10 = Tx.m10
      m00 = 0.0
      m11 = 0.0
      state = txstate
      tpe = Tx.tpe
      return
    /* ---------- Tx == TRANSLATE cases ---------- */ case (AffineTransform.HI_TRANSLATE | AffineTransform.APPLY_SHEAR | AffineTransform.APPLY_SCALE | AffineTransform.APPLY_TRANSLATE) =>
    case (AffineTransform.HI_TRANSLATE | AffineTransform.APPLY_SHEAR | AffineTransform.APPLY_SCALE) =>
    case (AffineTransform.HI_TRANSLATE | AffineTransform.APPLY_SHEAR | AffineTransform.APPLY_TRANSLATE) =>
    case (AffineTransform.HI_TRANSLATE | AffineTransform.APPLY_SHEAR) =>
    case (AffineTransform.HI_TRANSLATE | AffineTransform.APPLY_SCALE | AffineTransform.APPLY_TRANSLATE) =>
    case (AffineTransform.HI_TRANSLATE | AffineTransform.APPLY_SCALE) =>
    case (AffineTransform.HI_TRANSLATE | AffineTransform.APPLY_TRANSLATE) =>
      translate(Tx.m02, Tx.m12)
      return
    /* ---------- Tx == SCALE cases ---------- */ case (AffineTransform.HI_SCALE | AffineTransform.APPLY_SHEAR | AffineTransform.APPLY_SCALE | AffineTransform.APPLY_TRANSLATE) =>
    case (AffineTransform.HI_SCALE | AffineTransform.APPLY_SHEAR | AffineTransform.APPLY_SCALE) =>
    case (AffineTransform.HI_SCALE | AffineTransform.APPLY_SHEAR | AffineTransform.APPLY_TRANSLATE) =>
    case (AffineTransform.HI_SCALE | AffineTransform.APPLY_SHEAR) =>
    case (AffineTransform.HI_SCALE | AffineTransform.APPLY_SCALE | AffineTransform.APPLY_TRANSLATE) =>
    case (AffineTransform.HI_SCALE | AffineTransform.APPLY_SCALE) =>
    case (AffineTransform.HI_SCALE | AffineTransform.APPLY_TRANSLATE) =>
      scale(Tx.m00, Tx.m11)
      return
    /* ---------- Tx == SHEAR cases ---------- */ case (AffineTransform.HI_SHEAR | AffineTransform.APPLY_SHEAR | AffineTransform.APPLY_SCALE | AffineTransform.APPLY_TRANSLATE) =>
    case (AffineTransform.HI_SHEAR | AffineTransform.APPLY_SHEAR | AffineTransform.APPLY_SCALE) =>
      T01 = Tx.m01
      T10 = Tx.m10
      M0 = m00
      m00 = m01 * T10
      m01 = M0 * T01
      M0 = m10
      m10 = m11 * T10
      m11 = M0 * T01
      tpe = AffineTransform.TYPE_UNKNOWN
      return
    case (AffineTransform.HI_SHEAR | AffineTransform.APPLY_SHEAR | AffineTransform.APPLY_TRANSLATE) =>
    case (AffineTransform.HI_SHEAR | AffineTransform.APPLY_SHEAR) =>
      m00 = m01 * Tx.m10
      m01 = 0.0
      m11 = m10 * Tx.m01
      m10 = 0.0
      state = mystate ^ (AffineTransform.APPLY_SHEAR | AffineTransform.APPLY_SCALE)
      tpe = AffineTransform.TYPE_UNKNOWN
      return
    case (AffineTransform.HI_SHEAR | AffineTransform.APPLY_SCALE | AffineTransform.APPLY_TRANSLATE) =>
    case (AffineTransform.HI_SHEAR | AffineTransform.APPLY_SCALE) =>
      m01 = m00 * Tx.m01
      m00 = 0.0
      m10 = m11 * Tx.m10
      m11 = 0.0
      state = mystate ^ (AffineTransform.APPLY_SHEAR | AffineTransform.APPLY_SCALE)
      tpe = AffineTransform.TYPE_UNKNOWN
      return
    case (AffineTransform.HI_SHEAR | AffineTransform.APPLY_TRANSLATE) =>
      m00 = 0.0
      m01 = Tx.m01
      m10 = Tx.m10
      m11 = 0.0
      state = AffineTransform.APPLY_TRANSLATE | AffineTransform.APPLY_SHEAR
      tpe = AffineTransform.TYPE_UNKNOWN
      return
    }
    // If Tx has more than one attribute, it is not worth optimizing
    // all of those cases...
    T00 = Tx.m00
    T01 = Tx.m01
    T02 = Tx.m02
    T10 = Tx.m10
    T11 = Tx.m11
    T12 = Tx.m12
    mystate match {
      case (AffineTransform.APPLY_SHEAR | AffineTransform.APPLY_SCALE) =>
        state = mystate | txstate
      case (AffineTransform.APPLY_SHEAR | AffineTransform.APPLY_SCALE | AffineTransform.APPLY_TRANSLATE) =>
        M0 = m00
        M1 = m01
        m00 = T00 * M0 + T10 * M1
        m01 = T01 * M0 + T11 * M1
        m02 += T02 * M0 + T12 * M1
        M0 = m10
        M1 = m11
        m10 = T00 * M0 + T10 * M1
        m11 = T01 * M0 + T11 * M1
        m12 += T02 * M0 + T12 * M1
        tpe = AffineTransform.TYPE_UNKNOWN
        return
      case (AffineTransform.APPLY_SHEAR | AffineTransform.APPLY_TRANSLATE) =>
      case (AffineTransform.APPLY_SHEAR) =>
        M0 = m01
        m00 = T10 * M0
        m01 = T11 * M0
        m02 += T12 * M0
        M0 = m10
        m10 = T00 * M0
        m11 = T01 * M0
        m12 += T02 * M0

      case (AffineTransform.APPLY_SCALE | AffineTransform.APPLY_TRANSLATE) =>
      case (AffineTransform.APPLY_SCALE) =>
        M0 = m00
        m00 = T00 * M0
        m01 = T01 * M0
        m02 += T02 * M0
        M0 = m11
        m10 = T10 * M0
        m11 = T11 * M0
        m12 += T12 * M0

      case (AffineTransform.APPLY_TRANSLATE) =>
        m00 = T00
        m01 = T01
        m02 += T02
        m10 = T10
        m11 = T11
        m12 += T12
        state = txstate | AffineTransform.APPLY_TRANSLATE
        tpe = AffineTransform.TYPE_UNKNOWN
        return

      case _ =>
        stateError()
    }
    updateState()
  }

  /**
    * Concatenates an `AffineTransform Tx` to
    * this `AffineTransform` Cx
    * in a less commonly used way such that `Tx` modifies the
    * coordinate transformation relative to the absolute pixel
    * space rather than relative to the existing user space.
    * Cx is updated to perform the combined transformation.
    * Transforming a point p by the updated transform Cx' is
    * equivalent to first transforming p by the original transform
    * Cx and then transforming the result by
    * `Tx` like this:
    * Cx'(p) = Tx(Cx(p))
    * In matrix notation, if this transform Cx
    * is represented by the matrix [this] and `Tx` is
    * represented by the matrix [Tx] then this method does the
    * following:
    * <pre>
    * [this] = [Tx] x [this]
    * </pre>
    *
    * @param Tx the `AffineTransform` object to be
    *           concatenated with this `AffineTransform` object.
    * @see #concatenate
    * @since 1.2
    */
  @SuppressWarnings(Array("fallthrough")) def preConcatenate(Tx: AffineTransform): Unit = {
    ???
    var M0 = .0
    var M1 = .0
    var T00 = .0
    var T01 = .0
    var T10 = .0
    var T11 = .0
    var T02 = .0
    var T12 = .0
    var mystate = state
    val txstate = Tx.state
    (txstate << AffineTransform.HI_SHIFT) | mystate match {
      case (AffineTransform.HI_IDENTITY | AffineTransform.APPLY_IDENTITY) =>
      case (AffineTransform.HI_IDENTITY | AffineTransform.APPLY_TRANSLATE) =>
      case (AffineTransform.HI_IDENTITY | AffineTransform.APPLY_SCALE) =>
      case (AffineTransform.HI_IDENTITY | AffineTransform.APPLY_SCALE | AffineTransform.APPLY_TRANSLATE) =>
      case (AffineTransform.HI_IDENTITY | AffineTransform.APPLY_SHEAR) =>
      case (AffineTransform.HI_IDENTITY | AffineTransform.APPLY_SHEAR | AffineTransform.APPLY_TRANSLATE) =>
      case (AffineTransform.HI_IDENTITY | AffineTransform.APPLY_SHEAR | AffineTransform.APPLY_SCALE) =>
      case (AffineTransform.HI_IDENTITY | AffineTransform.APPLY_SHEAR | AffineTransform.APPLY_SCALE | AffineTransform.APPLY_TRANSLATE) =>
        // Tx is IDENTITY...
        return
      case (AffineTransform.HI_TRANSLATE | AffineTransform.APPLY_IDENTITY) =>
      case (AffineTransform.HI_TRANSLATE | AffineTransform.APPLY_SCALE) =>
      case (AffineTransform.HI_TRANSLATE | AffineTransform.APPLY_SHEAR) =>
      case (AffineTransform.HI_TRANSLATE | AffineTransform.APPLY_SHEAR | AffineTransform.APPLY_SCALE) =>
        // Tx is TRANSLATE, this has no TRANSLATE
        m02 = Tx.m02
        m12 = Tx.m12
        state = mystate | AffineTransform.APPLY_TRANSLATE
        tpe |= AffineTransform.TYPE_TRANSLATION
        return
      case (AffineTransform.HI_TRANSLATE | AffineTransform.APPLY_TRANSLATE) =>
      case (AffineTransform.HI_TRANSLATE | AffineTransform.APPLY_SCALE | AffineTransform.APPLY_TRANSLATE) =>
      case (AffineTransform.HI_TRANSLATE | AffineTransform.APPLY_SHEAR | AffineTransform.APPLY_TRANSLATE) =>
      case (AffineTransform.HI_TRANSLATE | AffineTransform.APPLY_SHEAR | AffineTransform.APPLY_SCALE | AffineTransform.APPLY_TRANSLATE) =>
        // Tx is TRANSLATE, this has one too
        m02 = m02 + Tx.m02
        m12 = m12 + Tx.m12
        return
      case (AffineTransform.HI_SCALE | AffineTransform.APPLY_TRANSLATE) =>
      case (AffineTransform.HI_SCALE | AffineTransform.APPLY_IDENTITY) =>
        // Only these two existing states need a new state
        state = mystate | AffineTransform.APPLY_SCALE
      case (AffineTransform.HI_SCALE | AffineTransform.APPLY_SHEAR | AffineTransform.APPLY_SCALE | AffineTransform.APPLY_TRANSLATE) =>
      case (AffineTransform.HI_SCALE | AffineTransform.APPLY_SHEAR | AffineTransform.APPLY_SCALE) =>
      case (AffineTransform.HI_SCALE | AffineTransform.APPLY_SHEAR | AffineTransform.APPLY_TRANSLATE) =>
      case (AffineTransform.HI_SCALE | AffineTransform.APPLY_SHEAR) =>
      case (AffineTransform.HI_SCALE | AffineTransform.APPLY_SCALE | AffineTransform.APPLY_TRANSLATE) =>
      case (AffineTransform.HI_SCALE | AffineTransform.APPLY_SCALE) =>
        // Tx is SCALE, this is anything
        T00 = Tx.m00
        T11 = Tx.m11
        if ((mystate & AffineTransform.APPLY_SHEAR) != 0) {
          m01 = m01 * T00
          m10 = m10 * T11
          if ((mystate & AffineTransform.APPLY_SCALE) != 0) {
            m00 = m00 * T00
            m11 = m11 * T11
          }
        }
        else {
          m00 = m00 * T00
          m11 = m11 * T11
        }
        if ((mystate & AffineTransform.APPLY_TRANSLATE) != 0) {
          m02 = m02 * T00
          m12 = m12 * T11
        }
        tpe = AffineTransform.TYPE_UNKNOWN
        return
      case (AffineTransform.HI_SHEAR | AffineTransform.APPLY_SHEAR | AffineTransform.APPLY_TRANSLATE) =>
      case (AffineTransform.HI_SHEAR | AffineTransform.APPLY_SHEAR) =>
        mystate = mystate | AffineTransform.APPLY_SCALE
      case (AffineTransform.HI_SHEAR | AffineTransform.APPLY_TRANSLATE) =>
      case (AffineTransform.HI_SHEAR | AffineTransform.APPLY_IDENTITY) =>
      case (AffineTransform.HI_SHEAR | AffineTransform.APPLY_SCALE | AffineTransform.APPLY_TRANSLATE) =>
      case (AffineTransform.HI_SHEAR | AffineTransform.APPLY_SCALE) =>
        state = mystate ^ AffineTransform.APPLY_SHEAR
      case (AffineTransform.HI_SHEAR | AffineTransform.APPLY_SHEAR | AffineTransform.APPLY_SCALE | AffineTransform.APPLY_TRANSLATE) =>
      case (AffineTransform.HI_SHEAR | AffineTransform.APPLY_SHEAR | AffineTransform.APPLY_SCALE) =>
        // Tx is SHEAR, this is anything
        T01 = Tx.m01
        T10 = Tx.m10
        M0 = m00
        m00 = m10 * T01
        m10 = M0 * T10
        M0 = m01
        m01 = m11 * T01
        m11 = M0 * T10
        M0 = m02
        m02 = m12 * T01
        m12 = M0 * T10
        tpe = AffineTransform.TYPE_UNKNOWN
        return
    }
    T00 = Tx.m00
    T01 = Tx.m01
    T02 = Tx.m02
    T10 = Tx.m10
    T11 = Tx.m11
    T12 = Tx.m12
    mystate match {
      case (AffineTransform.APPLY_SHEAR | AffineTransform.APPLY_SCALE | AffineTransform.APPLY_TRANSLATE) =>
        M0 = m02
        M1 = m12
        T02 += M0 * T00 + M1 * T01
        T12 += M0 * T10 + M1 * T11
      case (AffineTransform.APPLY_SHEAR | AffineTransform.APPLY_SCALE) =>
        m02 = T02
        m12 = T12
        M0 = m00
        M1 = m10
        m00 = M0 * T00 + M1 * T01
        m10 = M0 * T10 + M1 * T11
        M0 = m01
        M1 = m11
        m01 = M0 * T00 + M1 * T01
        m11 = M0 * T10 + M1 * T11

      case (AffineTransform.APPLY_SHEAR | AffineTransform.APPLY_TRANSLATE) =>
        M0 = m02
        M1 = m12
        T02 += M0 * T00 + M1 * T01
        T12 += M0 * T10 + M1 * T11
      case (AffineTransform.APPLY_SHEAR) =>
        m02 = T02
        m12 = T12
        M0 = m10
        m00 = M0 * T01
        m10 = M0 * T11
        M0 = m01
        m01 = M0 * T00
        m11 = M0 * T10

      case (AffineTransform.APPLY_SCALE | AffineTransform.APPLY_TRANSLATE) =>
        M0 = m02
        M1 = m12
        T02 += M0 * T00 + M1 * T01
        T12 += M0 * T10 + M1 * T11
      case (AffineTransform.APPLY_SCALE) =>
        m02 = T02
        m12 = T12
        M0 = m00
        m00 = M0 * T00
        m10 = M0 * T10
        M0 = m11
        m01 = M0 * T01
        m11 = M0 * T11

      case (AffineTransform.APPLY_TRANSLATE) =>
        M0 = m02
        M1 = m12
        T02 += M0 * T00 + M1 * T01
        T12 += M0 * T10 + M1 * T11
      case (AffineTransform.APPLY_IDENTITY) =>
        m02 = T02
        m12 = T12
        m00 = T00
        m10 = T10
        m01 = T01
        m11 = T11
        state = mystate | txstate
        tpe = AffineTransform.TYPE_UNKNOWN
        return

      case _ =>
        stateError()
    }
    updateState()
  }

  /**
    * Returns an `AffineTransform` object representing the
    * inverse transformation.
    * The inverse transform Tx' of this transform Tx
    * maps coordinates transformed by Tx back
    * to their original coordinates.
    * In other words, Tx'(Tx(p)) = p = Tx(Tx'(p)).
    * <p>
    * If this transform maps all coordinates onto a point or a line
    * then it will not have an inverse, since coordinates that do
    * not lie on the destination point or line will not have an inverse
    * mapping.
    * The `getDeterminant` method can be used to determine if this
    * transform has no inverse, in which case an exception will be
    * thrown if the `createInverse` method is called.
    *
    * @return a new `AffineTransform` object representing the
    *         inverse transformation.
    * @see #getDeterminant
    * @throws NoninvertibleTransformException if the matrix cannot be inverted.
    */
  @throws[NoninvertibleTransformException]
  def createInverse: AffineTransform = {
    var det = .0
    state match {
      case APPLY_SHEAR_SCALE_TRANSLATE =>
        det = m00 * m11 - m01 * m10
        if (Math.abs(det) <= java.lang.Double.MIN_VALUE) throw new NoninvertibleTransformException("Determinant is " + det)
        new AffineTransform(m11 / det, -m10 / det, -m01 / det, m00 / det, (m01 * m12 - m11 * m02) / det, (m10 * m02 - m00 * m12) / det, AffineTransform.APPLY_SHEAR | AffineTransform.APPLY_SCALE | AffineTransform.APPLY_TRANSLATE)
      case APPLY_SHEAR_SCALE =>
        det = m00 * m11 - m01 * m10
        if (Math.abs(det) <= java.lang.Double.MIN_VALUE) throw new NoninvertibleTransformException("Determinant is " + det)
        new AffineTransform(m11 / det, -m10 / det, -m01 / det, m00 / det, 0.0, 0.0, AffineTransform.APPLY_SHEAR | AffineTransform.APPLY_SCALE)
      case APPLY_SHEAR_TRANSLATE =>
        if (m01 == 0.0 || m10 == 0.0) throw new NoninvertibleTransformException("Determinant is 0")
        new AffineTransform(0.0, 1.0 / m01, 1.0 / m10, 0.0, -m12 / m10, -m02 / m01, AffineTransform.APPLY_SHEAR | AffineTransform.APPLY_TRANSLATE)
      case APPLY_SHEAR =>
        if (m01 == 0.0 || m10 == 0.0) throw new NoninvertibleTransformException("Determinant is 0")
        new AffineTransform(0.0, 1.0 / m01, 1.0 / m10, 0.0, 0.0, 0.0, AffineTransform.APPLY_SHEAR)
      case APPLY_SCALE_TRANSLATE =>
        if (m00 == 0.0 || m11 == 0.0) throw new NoninvertibleTransformException("Determinant is 0")
        new AffineTransform(1.0 / m00, 0.0, 0.0, 1.0 / m11, -m02 / m00, -m12 / m11, AffineTransform.APPLY_SCALE | AffineTransform.APPLY_TRANSLATE)
      case APPLY_SCALE =>
        if (m00 == 0.0 || m11 == 0.0) throw new NoninvertibleTransformException("Determinant is 0")
        new AffineTransform(1.0 / m00, 0.0, 0.0, 1.0 / m11, 0.0, 0.0, AffineTransform.APPLY_SCALE)
      case APPLY_TRANSLATE =>
        new AffineTransform(1.0, 0.0, 0.0, 1.0, -m02, -m12, AffineTransform.APPLY_TRANSLATE)
      case APPLY_IDENTITY =>
        new AffineTransform
      case _ =>
        stateError()
    }
  }

  /**
    * Sets this transform to the inverse of itself.
    * The inverse transform Tx' of this transform Tx
    * maps coordinates transformed by Tx back
    * to their original coordinates.
    * In other words, Tx'(Tx(p)) = p = Tx(Tx'(p)).
    * <p>
    * If this transform maps all coordinates onto a point or a line
    * then it will not have an inverse, since coordinates that do
    * not lie on the destination point or line will not have an inverse
    * mapping.
    * The `getDeterminant` method can be used to determine if this
    * transform has no inverse, in which case an exception will be
    * thrown if the `invert` method is called.
    *
    * @see #getDeterminant
    * @throws NoninvertibleTransformException if the matrix cannot be inverted.
    */
  @throws[NoninvertibleTransformException]
  def invert(): Unit = {
    var M00 = .0
    var M01 = .0
    var M02 = .0
    var M10 = .0
    var M11 = .0
    var M12 = .0
    var det = .0
    state match {
      case APPLY_SHEAR_SCALE_TRANSLATE =>
        M00 = m00
        M01 = m01
        M02 = m02
        M10 = m10
        M11 = m11
        M12 = m12
        det = M00 * M11 - M01 * M10
        if (Math.abs(det) <= java.lang.Double.MIN_VALUE) throw new NoninvertibleTransformException("Determinant is " + det)
        m00 = M11 / det
        m10 = -M10 / det
        m01 = -M01 / det
        m11 = M00 / det
        m02 = (M01 * M12 - M11 * M02) / det
        m12 = (M10 * M02 - M00 * M12) / det

      case APPLY_SHEAR_SCALE =>
        M00 = m00
        M01 = m01
        M10 = m10
        M11 = m11
        det = M00 * M11 - M01 * M10
        if (Math.abs(det) <= java.lang.Double.MIN_VALUE) throw new NoninvertibleTransformException("Determinant is " + det)
        m00 = M11 / det
        m10 = -M10 / det
        m01 = -M01 / det
        m11 = M00 / det
      // m02 = 0.0;
      // m12 = 0.0;

      case APPLY_SHEAR_TRANSLATE =>
        M01 = m01
        M02 = m02
        M10 = m10
        M12 = m12
        if (M01 == 0.0 || M10 == 0.0) throw new NoninvertibleTransformException("Determinant is 0")
        // m00 = 0.0;
        m10 = 1.0 / M01
        m01 = 1.0 / M10
        // m11 = 0.0;
        m02 = -M12 / M10
        m12 = -M02 / M01

      case APPLY_SHEAR =>
        M01 = m01
        M10 = m10
        if (M01 == 0.0 || M10 == 0.0) throw new NoninvertibleTransformException("Determinant is 0")
        m10 = 1.0 / M01
        m01 = 1.0 / M10

      case APPLY_SCALE_TRANSLATE =>
        M00 = m00
        M02 = m02
        M11 = m11
        M12 = m12
        if (M00 == 0.0 || M11 == 0.0) throw new NoninvertibleTransformException("Determinant is 0")
        m00 = 1.0 / M00
        // m10 = 0.0;
        // m01 = 0.0;
        m11 = 1.0 / M11
        m02 = -M02 / M00
        m12 = -M12 / M11

      case APPLY_SCALE =>
        M00 = m00
        M11 = m11
        if (M00 == 0.0 || M11 == 0.0) throw new NoninvertibleTransformException("Determinant is 0")
        m00 = 1.0 / M00
        m11 = 1.0 / M11

      case APPLY_TRANSLATE =>
        // m00 = 1.0;
        // m11 = 1.0;
        m02 = -m02
        m12 = -m12

      case APPLY_IDENTITY =>

      case _ =>
        stateError()
    }
  }

  /**
    * Transforms the specified `ptSrc` and stores the result
    * in `ptDst`.
    * If `ptDst` is `null`, a new `Point2D`
    * object is allocated and then the result of the transformation is
    * stored in this object.
    * In either case, `ptDst`, which contains the
    * transformed point, is returned for convenience.
    * If `ptSrc` and `ptDst` are the same
    * object, the input point is correctly overwritten with
    * the transformed point.
    *
    * @param ptSrc the specified `Point2D` to be transformed
    * @param ptDst the specified `Point2D` that stores the
    *              result of transforming `ptSrc`
    * @return the `ptDst` after transforming
    *         `ptSrc` and storing the result in `ptDst`.
    * @since 1.2
    */
  def transform(ptSrc: Point2D, ptDst: Point2D): Point2D = {
    var _ptDst = ptDst
    if (_ptDst == null) if (ptSrc.isInstanceOf[Point2D.Double]) _ptDst = new Point2D.Double
    else _ptDst = new Point2D.Float
    // Copy source coords into local variables in case src == dst
    val x = ptSrc.getX
    val y = ptSrc.getY
    state match {
      case AffineTransform.APPLY_SHEAR_SCALE_TRANSLATE =>
        _ptDst.setLocation(x * m00 + y * m01 + m02, x * m10 + y * m11 + m12)
      case AffineTransform.APPLY_SHEAR_SCALE =>
        _ptDst.setLocation(x * m00 + y * m01, x * m10 + y * m11)
      case AffineTransform.APPLY_SHEAR_TRANSLATE =>
        _ptDst.setLocation(y * m01 + m02, x * m10 + m12)
      case AffineTransform.APPLY_SHEAR =>
        _ptDst.setLocation(y * m01, x * m10)
      case AffineTransform.APPLY_SCALE_TRANSLATE =>
        _ptDst.setLocation(x * m00 + m02, y * m11 + m12)
      case AffineTransform.APPLY_SCALE =>
        _ptDst.setLocation(x * m00, y * m11)
      case AffineTransform.APPLY_TRANSLATE =>
        _ptDst.setLocation(x + m02, y + m12)
      case AffineTransform.APPLY_IDENTITY =>
        _ptDst.setLocation(x, y)

      case _ =>
        stateError()
    }
    _ptDst
  }

  /**
    * Transforms an array of point objects by this transform.
    * If any element of the `ptDst` array is
    * `null`, a new `Point2D` object is allocated
    * and stored into that element before storing the results of the
    * transformation.
    * <p>
    * Note that this method does not take any precautions to
    * avoid problems caused by storing results into `Point2D`
    * objects that will be used as the source for calculations
    * further down the source array.
    * This method does guarantee that if a specified `Point2D`
    * object is both the source and destination for the same single point
    * transform operation then the results will not be stored until
    * the calculations are complete to avoid storing the results on
    * top of the operands.
    * If, however, the destination `Point2D` object for one
    * operation is the same object as the source `Point2D`
    * object for another operation further down the source array then
    * the original coordinates in that point are overwritten before
    * they can be converted.
    *
    * @param ptSrc  the array containing the source point objects
    * @param ptDst  the array into which the transform point objects are
    *               returned
    * @param srcOff the offset to the first point object to be
    *               transformed in the source array
    * @param dstOff the offset to the location of the first
    *               transformed point object that is stored in the destination array
    * @param numPts the number of point objects to be transformed
    * @since 1.2
    */
  def transform(ptSrc: Array[Point2D], srcOff: Int, ptDst: Array[Point2D], dstOff: Int, numPts: Int): Unit = {
    var _numPts = numPts
    var _srcOff = srcOff
    var _dstOff = dstOff
    val state = this.state
    while ( {
      {
        _numPts -= 1; _numPts
      } >= 0
    }) {
      val src = ptSrc({
        _srcOff += 1; _srcOff - 1
      })
      val x = src.getX
      val y = src.getY
      var dst = ptDst({
        _dstOff += 1; _dstOff - 1
      })
      if (dst == null) {
        if (src.isInstanceOf[Point2D.Double]) dst = new Point2D.Double
        else dst = new Point2D.Float
        ptDst(_dstOff - 1) = dst
      }
      state match {
        case APPLY_SHEAR_SCALE_TRANSLATE =>
          dst.setLocation(x * m00 + y * m01 + m02, x * m10 + y * m11 + m12)

        case APPLY_SHEAR_SCALE =>
          dst.setLocation(x * m00 + y * m01, x * m10 + y * m11)

        case APPLY_SHEAR_TRANSLATE =>
          dst.setLocation(y * m01 + m02, x * m10 + m12)

        case APPLY_SHEAR =>
          dst.setLocation(y * m01, x * m10)

        case APPLY_SCALE_TRANSLATE =>
          dst.setLocation(x * m00 + m02, y * m11 + m12)

        case APPLY_SCALE =>
          dst.setLocation(x * m00, y * m11)

        case APPLY_TRANSLATE =>
          dst.setLocation(x + m02, y + m12)

        case APPLY_IDENTITY =>
          dst.setLocation(x, y)

        case _ =>
          stateError()
      }
    }
  }

  /**
    * Transforms an array of floating point coordinates by this transform.
    * The two coordinate array sections can be exactly the same or
    * can be overlapping sections of the same array without affecting the
    * validity of the results.
    * This method ensures that no source coordinates are overwritten by a
    * previous operation before they can be transformed.
    * The coordinates are stored in the arrays starting at the specified
    * offset in the order `[x0, y0, x1, y1, ..., xn, yn]`.
    *
    * @param srcPts the array containing the source point coordinates.
    *               Each point is stored as a pair of x,&nbsp;y coordinates.
    * @param dstPts the array into which the transformed point coordinates
    *               are returned.  Each point is stored as a pair of x,&nbsp;y
    *               coordinates.
    * @param srcOff the offset to the first point to be transformed
    *               in the source array
    * @param dstOff the offset to the location of the first
    *               transformed point that is stored in the destination array
    * @param numPts the number of points to be transformed
    * @since 1.2
    */
  def transform(srcPts: Array[Float], srcOff: Int, dstPts: Array[Float], dstOff: Int, numPts: Int): Unit = {
    var _srcOff = srcOff
    var _dstOff = dstOff
    var _numPts = numPts
    var M00 = .0
    var M01 = .0
    var M02 = .0
    var M10 = .0
    var M11 = .0
    var M12 = .0 // For caching
    if ((dstPts eq srcPts) && _dstOff > _srcOff && _dstOff < _srcOff + _numPts * 2) { // If the arrays overlap partially with the destination higher
      // than the source and we transform the coordinates normally
      // we would overwrite some of the later source coordinates
      // with results of previous transformations.
      // To get around this we use arraycopy to copy the points
      // to their final destination with correct overwrite
      // handling and then transform them in place in the new
      // safer location.
      System.arraycopy(srcPts, _srcOff, dstPts, _dstOff, _numPts * 2)
      // srcPts = dstPts;         // They are known to be equal.
      _srcOff = _dstOff
    }
    state match {
      case APPLY_SHEAR_SCALE_TRANSLATE =>
        M00 = m00
        M01 = m01
        M02 = m02
        M10 = m10
        M11 = m11
        M12 = m12
        while ( {
          {
            _numPts -= 1; _numPts
          } >= 0
        }) {
          val x = srcPts({
            _srcOff += 1; _srcOff - 1
          })
          val y = srcPts({
            _srcOff += 1; _srcOff - 1
          })
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = (M00 * x + M01 * y + M02).toFloat
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = (M10 * x + M11 * y + M12).toFloat
        }
      case APPLY_SHEAR_SCALE =>
        M00 = m00
        M01 = m01
        M10 = m10
        M11 = m11
        while ( {
          {
            _numPts -= 1; _numPts
          } >= 0
        }) {
          val x = srcPts({
            _srcOff += 1; _srcOff - 1
          })
          val y = srcPts({
            _srcOff += 1; _srcOff - 1
          })
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = (M00 * x + M01 * y).toFloat
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = (M10 * x + M11 * y).toFloat
        }
      case APPLY_SHEAR_TRANSLATE =>
        M01 = m01
        M02 = m02
        M10 = m10
        M12 = m12
        while ( {
          {
            _numPts -= 1; _numPts
          } >= 0
        }) {
          val x = srcPts({
            _srcOff += 1; _srcOff - 1
          })
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = (M01 * srcPts({
            _srcOff += 1; _srcOff - 1
          }) + M02).toFloat
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = (M10 * x + M12).toFloat
        }
      case APPLY_SHEAR =>
        M01 = m01
        M10 = m10
        while ( {
          {
            _numPts -= 1; _numPts
          } >= 0
        }) {
          val x = srcPts({
            _srcOff += 1; _srcOff - 1
          })
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = (M01 * srcPts({
            _srcOff += 1; _srcOff - 1
          })).toFloat
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = (M10 * x).toFloat
        }
      case APPLY_SCALE_TRANSLATE =>
        M00 = m00
        M02 = m02
        M11 = m11
        M12 = m12
        while ( {
          {
            _numPts -= 1; _numPts
          } >= 0
        }) {
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = (M00 * srcPts({
            _srcOff += 1; _srcOff - 1
          }) + M02).toFloat
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = (M11 * srcPts({
            _srcOff += 1; _srcOff - 1
          }) + M12).toFloat
        }
      case APPLY_SCALE =>
        M00 = m00
        M11 = m11
        while ( {
          {
            _numPts -= 1; _numPts
          } >= 0
        }) {
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = (M00 * srcPts({
            _srcOff += 1; _srcOff - 1
          })).toFloat
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = (M11 * srcPts({
            _srcOff += 1; _srcOff - 1
          })).toFloat
        }
      case APPLY_TRANSLATE =>
        M02 = m02
        M12 = m12
        while ( {
          {
            _numPts -= 1; _numPts
          } >= 0
        }) {
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = (srcPts({
            _srcOff += 1; _srcOff - 1
          }) + M02).toFloat
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = (srcPts({
            _srcOff += 1; _srcOff - 1
          }) + M12).toFloat
        }
      case APPLY_IDENTITY =>
        if ((srcPts ne dstPts) || _srcOff != _dstOff) System.arraycopy(srcPts, _srcOff, dstPts, _dstOff, _numPts * 2)

      case _ =>
        stateError()
    }
  }

  /**
    * Transforms an array of double precision coordinates by this transform.
    * The two coordinate array sections can be exactly the same or
    * can be overlapping sections of the same array without affecting the
    * validity of the results.
    * This method ensures that no source coordinates are
    * overwritten by a previous operation before they can be transformed.
    * The coordinates are stored in the arrays starting at the indicated
    * offset in the order `[x0, y0, x1, y1, ..., xn, yn]`.
    *
    * @param srcPts the array containing the source point coordinates.
    *               Each point is stored as a pair of x,&nbsp;y coordinates.
    * @param dstPts the array into which the transformed point
    *               coordinates are returned.  Each point is stored as a pair of
    *               x,&nbsp;y coordinates.
    * @param srcOff the offset to the first point to be transformed
    *               in the source array
    * @param dstOff the offset to the location of the first
    *               transformed point that is stored in the destination array
    * @param numPts the number of point objects to be transformed
    * @since 1.2
    */
  def transform(srcPts: Array[Double], srcOff: Int, dstPts: Array[Double], dstOff: Int, numPts: Int): Unit = {
    var _srcOff = srcOff
    var _dstOff = dstOff
    var _numPts = numPts
    var M00 = .0
    var M01 = .0
    var M02 = .0
    var M10 = .0
    var M11 = .0
    var M12 = .0
    if ((dstPts eq srcPts) && _dstOff > _srcOff && _dstOff < _srcOff + _numPts * 2) {
      System.arraycopy(srcPts, _srcOff, dstPts, _dstOff, _numPts * 2)
      _srcOff = _dstOff
    }
    state match {
      case APPLY_SHEAR_SCALE_TRANSLATE =>
        M00 = m00
        M01 = m01
        M02 = m02
        M10 = m10
        M11 = m11
        M12 = m12
        while ( {
          {
            _numPts -= 1; _numPts
          } >= 0
        }) {
          val x = srcPts({
            _srcOff += 1; _srcOff - 1
          })
          val y = srcPts({
            _srcOff += 1; _srcOff - 1
          })
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = M00 * x + M01 * y + M02
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = M10 * x + M11 * y + M12
        }
      case APPLY_SHEAR_SCALE =>
        M00 = m00
        M01 = m01
        M10 = m10
        M11 = m11
        while ( {
          {
            _numPts -= 1; _numPts
          } >= 0
        }) {
          val x = srcPts({
            _srcOff += 1; _srcOff - 1
          })
          val y = srcPts({
            _srcOff += 1; _srcOff - 1
          })
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = M00 * x + M01 * y
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = M10 * x + M11 * y
        }
      case APPLY_SHEAR_TRANSLATE =>
        M01 = m01
        M02 = m02
        M10 = m10
        M12 = m12
        while ( {
          {
            _numPts -= 1; _numPts
          } >= 0
        }) {
          val x = srcPts({
            _srcOff += 1; _srcOff - 1
          })
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = M01 * srcPts({
            _srcOff += 1; _srcOff - 1
          }) + M02
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = M10 * x + M12
        }
      case APPLY_SHEAR =>
        M01 = m01
        M10 = m10
        while ( {
          {
            _numPts -= 1; _numPts
          } >= 0
        }) {
          val x = srcPts({
            _srcOff += 1; _srcOff - 1
          })
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = M01 * srcPts({
            _srcOff += 1; _srcOff - 1
          })
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = M10 * x
        }
      case APPLY_SCALE_TRANSLATE =>
        M00 = m00
        M02 = m02
        M11 = m11
        M12 = m12
        while ( {
          {
            _numPts -= 1; _numPts
          } >= 0
        }) {
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = M00 * srcPts({
            _srcOff += 1; _srcOff - 1
          }) + M02
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = M11 * srcPts({
            _srcOff += 1; _srcOff - 1
          }) + M12
        }
      case APPLY_SCALE =>
        M00 = m00
        M11 = m11
        while ( {
          {
            _numPts -= 1; _numPts
          } >= 0
        }) {
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = M00 * srcPts({
            _srcOff += 1; _srcOff - 1
          })
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = M11 * srcPts({
            _srcOff += 1; _srcOff - 1
          })
        }
      case APPLY_TRANSLATE =>
        M02 = m02
        M12 = m12
        while ( {
          {
            _numPts -= 1; _numPts
          } >= 0
        }) {
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = srcPts({
            _srcOff += 1; _srcOff - 1
          }) + M02
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = srcPts({
            _srcOff += 1; _srcOff - 1
          }) + M12
        }
      case APPLY_IDENTITY =>
        if ((srcPts ne dstPts) || _srcOff != _dstOff) System.arraycopy(srcPts, _srcOff, dstPts, _dstOff, _numPts * 2)

      case _ =>
        stateError()
    }
  }

  /**
    * Transforms an array of floating point coordinates by this transform
    * and stores the results into an array of doubles.
    * The coordinates are stored in the arrays starting at the specified
    * offset in the order `[x0, y0, x1, y1, ..., xn, yn]`.
    *
    * @param srcPts the array containing the source point coordinates.
    *               Each point is stored as a pair of x,&nbsp;y coordinates.
    * @param dstPts the array into which the transformed point coordinates
    *               are returned.  Each point is stored as a pair of x,&nbsp;y
    *               coordinates.
    * @param srcOff the offset to the first point to be transformed
    *               in the source array
    * @param dstOff the offset to the location of the first
    *               transformed point that is stored in the destination array
    * @param numPts the number of points to be transformed
    * @since 1.2
    */
  def transform(srcPts: Array[Float], srcOff: Int, dstPts: Array[Double], dstOff: Int, numPts: Int): Unit = {
    var _srcOff = srcOff
    var _dstOff = dstOff
    var _numPts = numPts
    var M00 = .0
    var M01 = .0
    var M02 = .0
    var M10 = .0
    var M11 = .0
    var M12 = .0
    state match {
      case APPLY_SHEAR_SCALE_TRANSLATE =>
        M00 = m00
        M01 = m01
        M02 = m02
        M10 = m10
        M11 = m11
        M12 = m12
        while ( {
          {
            _numPts -= 1; _numPts
          } >= 0
        }) {
          val x = srcPts({
            _srcOff += 1; _srcOff - 1
          })
          val y = srcPts({
            _srcOff += 1; _srcOff - 1
          })
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = M00 * x + M01 * y + M02
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = M10 * x + M11 * y + M12
        }
      case APPLY_SHEAR_SCALE =>
        M00 = m00
        M01 = m01
        M10 = m10
        M11 = m11
        while ( {
          {
            _numPts -= 1; _numPts
          } >= 0
        }) {
          val x = srcPts({
            _srcOff += 1; _srcOff - 1
          })
          val y = srcPts({
            _srcOff += 1; _srcOff - 1
          })
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = M00 * x + M01 * y
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = M10 * x + M11 * y
        }
      case APPLY_SHEAR_TRANSLATE =>
        M01 = m01
        M02 = m02
        M10 = m10
        M12 = m12
        while ( {
          {
            _numPts -= 1; _numPts
          } >= 0
        }) {
          val x = srcPts({
            _srcOff += 1; _srcOff - 1
          })
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = M01 * srcPts({
            _srcOff += 1; _srcOff - 1
          }) + M02
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = M10 * x + M12
        }
      case APPLY_SHEAR =>
        M01 = m01
        M10 = m10
        while ( {
          {
            _numPts -= 1; _numPts
          } >= 0
        }) {
          val x = srcPts({
            _srcOff += 1; _srcOff - 1
          })
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = M01 * srcPts({
            _srcOff += 1; _srcOff - 1
          })
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = M10 * x
        }
      case APPLY_SCALE_TRANSLATE =>
        M00 = m00
        M02 = m02
        M11 = m11
        M12 = m12
        while ( {
          {
            _numPts -= 1; _numPts
          } >= 0
        }) {
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = M00 * srcPts({
            _srcOff += 1; _srcOff - 1
          }) + M02
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = M11 * srcPts({
            _srcOff += 1; _srcOff - 1
          }) + M12
        }
      case APPLY_SCALE =>
        M00 = m00
        M11 = m11
        while ( {
          {
            _numPts -= 1; _numPts
          } >= 0
        }) {
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = M00 * srcPts({
            _srcOff += 1; _srcOff - 1
          })
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = M11 * srcPts({
            _srcOff += 1; _srcOff - 1
          })
        }
      case APPLY_TRANSLATE =>
        M02 = m02
        M12 = m12
        while ( {
          {
            _numPts -= 1; _numPts
          } >= 0
        }) {
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = srcPts({
            _srcOff += 1; _srcOff - 1
          }) + M02
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = srcPts({
            _srcOff += 1; _srcOff - 1
          }) + M12
        }
      case APPLY_IDENTITY =>
        while ( {
          {
            _numPts -= 1; _numPts
          } >= 0
        }) {
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = srcPts({
            _srcOff += 1; _srcOff - 1
          })
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = srcPts({
            _srcOff += 1; _srcOff - 1
          })
        }

      case _ =>
        stateError()
    }
  }

  /**
    * Transforms an array of double precision coordinates by this transform
    * and stores the results into an array of floats.
    * The coordinates are stored in the arrays starting at the specified
    * offset in the order `[x0, y0, x1, y1, ..., xn, yn]`.
    *
    * @param srcPts the array containing the source point coordinates.
    *               Each point is stored as a pair of x,&nbsp;y coordinates.
    * @param dstPts the array into which the transformed point
    *               coordinates are returned.  Each point is stored as a pair of
    *               x,&nbsp;y coordinates.
    * @param srcOff the offset to the first point to be transformed
    *               in the source array
    * @param dstOff the offset to the location of the first
    *               transformed point that is stored in the destination array
    * @param numPts the number of point objects to be transformed
    * @since 1.2
    */
  def transform(srcPts: Array[Double], srcOff: Int, dstPts: Array[Float], dstOff: Int, numPts: Int): Unit = {
    var _srcOff = srcOff
    var _dstOff = dstOff
    var _numPts = numPts
    var M00 = .0
    var M01 = .0
    var M02 = .0
    var M10 = .0
    var M11 = .0
    var M12 = .0
    state match {
      case APPLY_SHEAR_SCALE_TRANSLATE =>
        M00 = m00
        M01 = m01
        M02 = m02
        M10 = m10
        M11 = m11
        M12 = m12
        while ( {
          {
            _numPts -= 1; _numPts
          } >= 0
        }) {
          val x = srcPts({
            _srcOff += 1; _srcOff - 1
          })
          val y = srcPts({
            _srcOff += 1; _srcOff - 1
          })
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = (M00 * x + M01 * y + M02).toFloat
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = (M10 * x + M11 * y + M12).toFloat
        }
      case APPLY_SHEAR_SCALE =>
        M00 = m00
        M01 = m01
        M10 = m10
        M11 = m11
        while ( {
          {
            _numPts -= 1; _numPts
          } >= 0
        }) {
          val x = srcPts({
            _srcOff += 1; _srcOff - 1
          })
          val y = srcPts({
            _srcOff += 1; _srcOff - 1
          })
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = (M00 * x + M01 * y).toFloat
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = (M10 * x + M11 * y).toFloat
        }
      case APPLY_SHEAR_TRANSLATE =>
        M01 = m01
        M02 = m02
        M10 = m10
        M12 = m12
        while ( {
          {
            _numPts -= 1; _numPts
          } >= 0
        }) {
          val x = srcPts({
            _srcOff += 1; _srcOff - 1
          })
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = (M01 * srcPts({
            _srcOff += 1; _srcOff - 1
          }) + M02).toFloat
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = (M10 * x + M12).toFloat
        }
      case APPLY_SHEAR =>
        M01 = m01
        M10 = m10
        while ( {
          {
            _numPts -= 1; _numPts
          } >= 0
        }) {
          val x = srcPts({
            _srcOff += 1; _srcOff - 1
          })
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = (M01 * srcPts({
            _srcOff += 1; _srcOff - 1
          })).toFloat
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = (M10 * x).toFloat
        }
      case APPLY_SCALE_TRANSLATE =>
        M00 = m00
        M02 = m02
        M11 = m11
        M12 = m12
        while ( {
          {
            _numPts -= 1; _numPts
          } >= 0
        }) {
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = (M00 * srcPts({
            _srcOff += 1; _srcOff - 1
          }) + M02).toFloat
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = (M11 * srcPts({
            _srcOff += 1; _srcOff - 1
          }) + M12).toFloat
        }
      case APPLY_SCALE =>
        M00 = m00
        M11 = m11
        while ( {
          {
            _numPts -= 1; _numPts
          } >= 0
        }) {
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = (M00 * srcPts({
            _srcOff += 1; _srcOff - 1
          })).toFloat
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = (M11 * srcPts({
            _srcOff += 1; _srcOff - 1
          })).toFloat
        }
      case APPLY_TRANSLATE =>
        M02 = m02
        M12 = m12
        while ( {
          {
            _numPts -= 1; _numPts
          } >= 0
        }) {
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = (srcPts({
            _srcOff += 1; _srcOff - 1
          }) + M02).toFloat
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = (srcPts({
            _srcOff += 1; _srcOff - 1
          }) + M12).toFloat
        }
      case APPLY_IDENTITY =>
        while ( {
          {
            _numPts -= 1; _numPts
          } >= 0
        }) {
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = srcPts({
            _srcOff += 1; _srcOff - 1
          }).toFloat
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = srcPts({
            _srcOff += 1; _srcOff - 1
          }).toFloat
        }

      case _ =>
        stateError()
    }
  }

  /**
    * Inverse transforms the specified `ptSrc` and stores the
    * result in `ptDst`.
    * If `ptDst` is `null`, a new
    * `Point2D` object is allocated and then the result of the
    * transform is stored in this object.
    * In either case, `ptDst`, which contains the transformed
    * point, is returned for convenience.
    * If `ptSrc` and `ptDst` are the same
    * object, the input point is correctly overwritten with the
    * transformed point.
    *
    * @param ptSrc the point to be inverse transformed
    * @param ptDst the resulting transformed point
    * @return `ptDst`, which contains the result of the
    *         inverse transform.
    * @throws NoninvertibleTransformException  if the matrix cannot be
    *            inverted.
    */
  @throws[NoninvertibleTransformException]
  def inverseTransform(ptSrc: Point2D, ptDst: Point2D): Point2D = {
    val _ptDst = if (ptDst != null) ptDst else {
      if (ptSrc.isInstanceOf[Point2D.Double]) new Point2D.Double else new Point2D.Float
    }
    var x = ptSrc.getX
    var y = ptSrc.getY

    def case1(): Unit = {
      val det = m00 * m11 - m01 * m10
      if (Math.abs(det) <= java.lang.Double.MIN_VALUE) throw new NoninvertibleTransformException("Determinant is " + det)
      _ptDst.setLocation((x * m11 - y * m01) / det, (y * m00 - x * m10) / det)
    }

    def case2(): Unit = {
      if (m01 == 0.0 || m10 == 0.0) throw new NoninvertibleTransformException("Determinant is 0")
      _ptDst.setLocation(y / m10, x / m01)
    }

    def case3(): Unit = {
      if (m00 == 0.0 || m11 == 0.0) throw new NoninvertibleTransformException("Determinant is 0")
      _ptDst.setLocation(x / m00, y / m11)
    }

    state match {
      case APPLY_SHEAR_SCALE_TRANSLATE =>
        x -= m02
        y -= m12
        case1()
      case APPLY_SHEAR_SCALE =>
        case1()
      case APPLY_SHEAR_TRANSLATE =>
        x -= m02
        y -= m12
        case2()
      case APPLY_SHEAR =>
        case2()
      case APPLY_SCALE_TRANSLATE =>
        x -= m02
        y -= m12
        case3()
      case APPLY_SCALE =>
        case3()
      case APPLY_TRANSLATE =>
        _ptDst.setLocation(x - m02, y - m12)
      case APPLY_IDENTITY =>
        _ptDst.setLocation(x, y)

      case _ =>
        stateError()
    }
    _ptDst
  }

  /**
    * Inverse transforms an array of double precision coordinates by
    * this transform.
    * The two coordinate array sections can be exactly the same or
    * can be overlapping sections of the same array without affecting the
    * validity of the results.
    * This method ensures that no source coordinates are
    * overwritten by a previous operation before they can be transformed.
    * The coordinates are stored in the arrays starting at the specified
    * offset in the order `[x0, y0, x1, y1, ..., xn, yn]`.
    *
    * @param srcPts the array containing the source point coordinates.
    *               Each point is stored as a pair of x,&nbsp;y coordinates.
    * @param dstPts the array into which the transformed point
    *               coordinates are returned.  Each point is stored as a pair of
    *               x,&nbsp;y coordinates.
    * @param srcOff the offset to the first point to be transformed
    *               in the source array
    * @param dstOff the offset to the location of the first
    *               transformed point that is stored in the destination array
    * @param numPts the number of point objects to be transformed
    * @throws NoninvertibleTransformException  if the matrix cannot be
    *            inverted.
    */
  @throws[NoninvertibleTransformException]
  def inverseTransform(srcPts: Array[Double], srcOff: Int, dstPts: Array[Double], dstOff: Int, numPts: Int): Unit = {
    var _srcOff = srcOff
    var _dstOff = dstOff
    var _numPts = numPts
    var M00 = .0
    var M01 = .0
    var M02 = .0
    var M10 = .0
    var M11 = .0
    var M12 = .0
    var det = .0
    if ((dstPts eq srcPts) && _dstOff > _srcOff && _dstOff < _srcOff + _numPts * 2) {
      System.arraycopy(srcPts, _srcOff, dstPts, _dstOff, _numPts * 2)
      _srcOff = _dstOff
    }
    state match {
      case APPLY_SHEAR_SCALE_TRANSLATE =>
        M00 = m00
        M01 = m01
        M02 = m02
        M10 = m10
        M11 = m11
        M12 = m12
        det = M00 * M11 - M01 * M10
        if (Math.abs(det) <= java.lang.Double.MIN_VALUE) throw new NoninvertibleTransformException("Determinant is " + det)
        while ( {
          {
            _numPts -= 1; _numPts
          } >= 0
        }) {
          val x = srcPts({
            _srcOff += 1; _srcOff - 1
          }) - M02
          val y = srcPts({
            _srcOff += 1; _srcOff - 1
          }) - M12
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = (x * M11 - y * M01) / det
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = (y * M00 - x * M10) / det
        }
      case APPLY_SHEAR_SCALE =>
        M00 = m00
        M01 = m01
        M10 = m10
        M11 = m11
        det = M00 * M11 - M01 * M10
        if (Math.abs(det) <= java.lang.Double.MIN_VALUE) throw new NoninvertibleTransformException("Determinant is " + det)
        while ( {
          {
            _numPts -= 1; _numPts
          } >= 0
        }) {
          val x = srcPts({
            _srcOff += 1; _srcOff - 1
          })
          val y = srcPts({
            _srcOff += 1; _srcOff - 1
          })
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = (x * M11 - y * M01) / det
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = (y * M00 - x * M10) / det
        }
      case APPLY_SHEAR_TRANSLATE =>
        M01 = m01
        M02 = m02
        M10 = m10
        M12 = m12
        if (M01 == 0.0 || M10 == 0.0) throw new NoninvertibleTransformException("Determinant is 0")
        while ( {
          {
            _numPts -= 1; _numPts
          } >= 0
        }) {
          val x = srcPts({
            _srcOff += 1; _srcOff - 1
          }) - M02
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = (srcPts({
            _srcOff += 1; _srcOff - 1
          }) - M12) / M10
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = x / M01
        }
      case APPLY_SHEAR =>
        M01 = m01
        M10 = m10
        if (M01 == 0.0 || M10 == 0.0) throw new NoninvertibleTransformException("Determinant is 0")
        while ( {
          {
            _numPts -= 1; _numPts
          } >= 0
        }) {
          val x = srcPts({
            _srcOff += 1; _srcOff - 1
          })
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = srcPts({
            _srcOff += 1; _srcOff - 1
          }) / M10
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = x / M01
        }
      case APPLY_SCALE_TRANSLATE =>
        M00 = m00
        M02 = m02
        M11 = m11
        M12 = m12
        if (M00 == 0.0 || M11 == 0.0) throw new NoninvertibleTransformException("Determinant is 0")
        while ( {
          {
            _numPts -= 1; _numPts
          } >= 0
        }) {
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = (srcPts({
            _srcOff += 1; _srcOff - 1
          }) - M02) / M00
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = (srcPts({
            _srcOff += 1; _srcOff - 1
          }) - M12) / M11
        }
      case APPLY_SCALE =>
        M00 = m00
        M11 = m11
        if (M00 == 0.0 || M11 == 0.0) throw new NoninvertibleTransformException("Determinant is 0")
        while ( {
          {
            _numPts -= 1; _numPts
          } >= 0
        }) {
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = srcPts({
            _srcOff += 1; _srcOff - 1
          }) / M00
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = srcPts({
            _srcOff += 1; _srcOff - 1
          }) / M11
        }
      case APPLY_TRANSLATE =>
        M02 = m02
        M12 = m12
        while ( {
          {
            _numPts -= 1; _numPts
          } >= 0
        }) {
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = srcPts({
            _srcOff += 1; _srcOff - 1
          }) - M02
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = srcPts({
            _srcOff += 1; _srcOff - 1
          }) - M12
        }
      case APPLY_IDENTITY =>
        if ((srcPts ne dstPts) || _srcOff != _dstOff) System.arraycopy(srcPts, _srcOff, dstPts, _dstOff, _numPts * 2)

      case _ =>
        stateError()
    }
  }

  /**
    * Transforms the relative distance vector specified by
    * `ptSrc` and stores the result in `ptDst`.
    * A relative distance vector is transformed without applying the
    * translation components of the affine transformation matrix
    * using the following equations:
    * <pre>
    * [  x' ]   [  m00  m01 (m02) ] [  x  ]   [ m00x + m01y ]
    * [  y' ] = [  m10  m11 (m12) ] [  y  ] = [ m10x + m11y ]
    * [ (1) ]   [  (0)  (0) ( 1 ) ] [ (1) ]   [     (1)     ]
    * </pre>
    * If `ptDst` is `null`, a new
    * `Point2D` object is allocated and then the result of the
    * transform is stored in this object.
    * In either case, `ptDst`, which contains the
    * transformed point, is returned for convenience.
    * If `ptSrc` and `ptDst` are the same object,
    * the input point is correctly overwritten with the transformed
    * point.
    *
    * @param ptSrc the distance vector to be delta transformed
    * @param ptDst the resulting transformed distance vector
    * @return `ptDst`, which contains the result of the
    *         transformation.
    * @since 1.2
    */
  def deltaTransform(ptSrc: Point2D, ptDst: Point2D): Point2D = {
    val _ptDst = if (ptDst != null) ptDst else {
      if (ptSrc.isInstanceOf[Point2D.Double]) new Point2D.Double else new Point2D.Float
    }
    val x = ptSrc.getX
    val y = ptSrc.getY
    state match {
      case APPLY_SHEAR_SCALE_TRANSLATE =>
        _ptDst.setLocation(x * m00 + y * m01, x * m10 + y * m11)
      case APPLY_SHEAR_SCALE =>
        _ptDst.setLocation(x * m00 + y * m01, x * m10 + y * m11)
      case APPLY_SHEAR_TRANSLATE =>
        _ptDst.setLocation(y * m01, x * m10)
      case APPLY_SHEAR =>
        _ptDst.setLocation(y * m01, x * m10)
      case APPLY_SCALE_TRANSLATE =>
        _ptDst.setLocation(x * m00, y * m11)
      case APPLY_SCALE =>
        _ptDst.setLocation(x * m00, y * m11)
      case APPLY_TRANSLATE =>
        _ptDst.setLocation(x, y)
      case APPLY_IDENTITY =>
        _ptDst.setLocation(x, y)

      case _ =>
        stateError()
    }
    _ptDst
  }

  /**
    * Transforms an array of relative distance vectors by this
    * transform.
    * A relative distance vector is transformed without applying the
    * translation components of the affine transformation matrix
    * using the following equations:
    * <pre>
    * [  x' ]   [  m00  m01 (m02) ] [  x  ]   [ m00x + m01y ]
    * [  y' ] = [  m10  m11 (m12) ] [  y  ] = [ m10x + m11y ]
    * [ (1) ]   [  (0)  (0) ( 1 ) ] [ (1) ]   [     (1)     ]
    * </pre>
    * The two coordinate array sections can be exactly the same or
    * can be overlapping sections of the same array without affecting the
    * validity of the results.
    * This method ensures that no source coordinates are
    * overwritten by a previous operation before they can be transformed.
    * The coordinates are stored in the arrays starting at the indicated
    * offset in the order `[x0, y0, x1, y1, ..., xn, yn]`.
    *
    * @param srcPts the array containing the source distance vectors.
    *               Each vector is stored as a pair of relative x,&nbsp;y coordinates.
    * @param dstPts the array into which the transformed distance vectors
    *               are returned.  Each vector is stored as a pair of relative
    *               x,&nbsp;y coordinates.
    * @param srcOff the offset to the first vector to be transformed
    *               in the source array
    * @param dstOff the offset to the location of the first
    *               transformed vector that is stored in the destination array
    * @param numPts the number of vector coordinate pairs to be
    *               transformed
    * @since 1.2
    */
  def deltaTransform(srcPts: Array[Double], srcOff: Int, dstPts: Array[Double], dstOff: Int, numPts: Int): Unit = {
    var _srcOff = srcOff
    var _dstOff = dstOff
    var _numPts = numPts
    var M00 = .0
    var M01 = .0
    var M10 = .0
    var M11 = .0
    if ((dstPts eq srcPts) && _dstOff > _srcOff && _dstOff < _srcOff + _numPts * 2) {
      System.arraycopy(srcPts, _srcOff, dstPts, _dstOff, _numPts * 2)
      _srcOff = _dstOff
    }
    state match {
      case APPLY_SHEAR_SCALE_TRANSLATE | APPLY_SHEAR_SCALE =>
        M00 = m00
        M01 = m01
        M10 = m10
        M11 = m11
        while ( {
          {
            _numPts -= 1; _numPts
          } >= 0
        }) {
          val x = srcPts({
            _srcOff += 1; _srcOff - 1
          })
          val y = srcPts({
            _srcOff += 1; _srcOff - 1
          })
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = x * M00 + y * M01
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = x * M10 + y * M11
        }

      case APPLY_SHEAR_TRANSLATE | APPLY_SHEAR =>
        M01 = m01
        M10 = m10
        while ( {
          {
            _numPts -= 1; _numPts
          } >= 0
        }) {
          val x = srcPts({
            _srcOff += 1; _srcOff - 1
          })
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = srcPts({
            _srcOff += 1; _srcOff - 1
          }) * M01
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = x * M10
        }

      case APPLY_SCALE_TRANSLATE | APPLY_SCALE =>
        M00 = m00
        M11 = m11
        while ( {
          {
            _numPts -= 1; _numPts
          } >= 0
        }) {
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = srcPts({
            _srcOff += 1; _srcOff - 1
          }) * M00
          dstPts({
            _dstOff += 1; _dstOff - 1
          }) = srcPts({
            _srcOff += 1; _srcOff - 1
          }) * M11
        }

      case APPLY_TRANSLATE | APPLY_IDENTITY =>
        if ((srcPts ne dstPts) || _srcOff != _dstOff) System.arraycopy(srcPts, _srcOff, dstPts, _dstOff, _numPts * 2)

      case _ =>
        stateError()
    }
  }

  /**
    * Returns a new `Shape` object defined by the geometry of the
    * specified `Shape` after it has been transformed by
    * this transform.
    *
    * @param pSrc the specified `Shape` object to be
    *             transformed by this transform.
    * @return a new `Shape` object that defines the geometry
    *         of the transformed `Shape`, or null if `pSrc` is null.
    * @since 1.2
    */
  def createTransformedShape(pSrc: Shape): Shape = {
    if (pSrc == null) return null
    Path2D.Double(pSrc, this)
  }

  /**
    * Returns a `String` that represents the value of this
    * `Object`.
    *
    * @return a `String` representing the value of this
    *         `Object`.
    * @since 1.2
    */
  override def toString: String = "AffineTransform[[" +
    AffineTransform._matround(m00) + ", " + AffineTransform._matround(m01) + ", " +
    AffineTransform._matround(m02) + "], [" + AffineTransform._matround(m10) + ", " +
    AffineTransform._matround(m11) + ", " + AffineTransform._matround(m12) + "]]"

  /**
    * Returns `true` if this `AffineTransform` is
    * an identity transform.
    *
    * @return `true` if this `AffineTransform` is
    *         an identity transform; `false` otherwise.
    * @since 1.2
    */
  def isIdentity: Boolean = state == AffineTransform.APPLY_IDENTITY || (getType == AffineTransform.TYPE_IDENTITY)

  /**
    * Returns a copy of this `AffineTransform` object.
    *
    * @return an `Object` that is a copy of this
    *         `AffineTransform` object.
    * @since 1.2
    */
  override def clone: Any = try super.clone
  catch {
    case e: CloneNotSupportedException =>
      // this shouldn't happen, since we are Cloneable
      throw new InternalError(e)
  }

  /**
    * Returns the hashcode for this transform.
    *
    * @return a hash code for this transform.
    * @since 1.2
    */
  override def hashCode: Int = {
    var bits = java.lang.Double.doubleToLongBits(m00)
    bits = bits * 31 + java.lang.Double.doubleToLongBits(m01)
    bits = bits * 31 + java.lang.Double.doubleToLongBits(m02)
    bits = bits * 31 + java.lang.Double.doubleToLongBits(m10)
    bits = bits * 31 + java.lang.Double.doubleToLongBits(m11)
    bits = bits * 31 + java.lang.Double.doubleToLongBits(m12)
    (bits.toInt) ^ ((bits >> 32).toInt)
  }

  /**
    * Returns `true` if this `AffineTransform`
    * represents the same affine coordinate transform as the specified
    * argument.
    *
    * @param obj the `Object` to test for equality with this
    *            `AffineTransform`
    * @return `true` if `obj` equals this
    *         `AffineTransform` object; `false` otherwise.
    * @since 1.2
    */
  override def equals(obj: Any): Boolean = {
    if (!obj.isInstanceOf[AffineTransform]) return false
    val a = obj.asInstanceOf[AffineTransform]
    (m00 == a.m00) && (m01 == a.m01) && (m02 == a.m02) && (m10 == a.m10) && (m11 == a.m11) && (m12 == a.m12)
  }

  @throws[java.lang.ClassNotFoundException]
  @throws[java.io.IOException]
  private def writeObject(s: ObjectOutputStream): Unit = {
    s.defaultWriteObject()
  }

  @throws[java.lang.ClassNotFoundException]
  @throws[java.io.IOException]
  private def readObject(s: ObjectInputStream): Unit = {
    s.defaultReadObject()
    updateState()
  }
}
