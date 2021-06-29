package de.sciss.infibrillae.geom

// This is an adapted Scala translation of the QuadCurve2D Java class of OpenJDK
// as released under GNU GPL 2 -- see original file header below.
// So it can be used in Scala.js.

/*
 * Copyright (c) 1997, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.geom.Line2D
import java.io.Serializable

import scala.{Float => SFloat, Double => SDouble}

/**
  * The `QuadCurve2D` class defines a quadratic parametric curve
  * segment in `(x,y)` coordinate space.
  * <p>
  * This class is only the abstract superclass for all objects that
  * store a 2D quadratic curve segment.
  * The actual storage representation of the coordinates is left to
  * the subclass.
  *
  * @author Jim Graham
  * @since 1.2
  */
object QuadCurve2D {
  /**
    * A quadratic parametric curve segment specified with
    * `float` coordinates.
    */
  class Float() extends QuadCurve2D with Serializable {
    /**
      * The X coordinate of the start point of the quadratic curve
      * segment.
      */
    private var x1 = 0.0f
    /**
      * The Y coordinate of the start point of the quadratic curve
      * segment.
      */
    private var y1 = 0.0f
    /**
      * The X coordinate of the control point of the quadratic curve
      * segment.
      */
    private var ctrlx = 0.0f
    /**
      * The Y coordinate of the control point of the quadratic curve
      * segment.
      */
    private var ctrly = 0.0f
    /**
      * The X coordinate of the end point of the quadratic curve
      * segment.
      */
    private var x2 = 0.0f
    /**
      * The Y coordinate of the end point of the quadratic curve
      * segment.
      */
    private var y2 = 0.0f

    /**
      * Constructs and initializes a `QuadCurve2D` from the
      * specified `float` coordinates.
      *
      * @param x1    the X coordinate of the start point
      * @param y1    the Y coordinate of the start point
      * @param ctrlx the X coordinate of the control point
      * @param ctrly the Y coordinate of the control point
      * @param x2    the X coordinate of the end point
      * @param y2    the Y coordinate of the end point
      * @since 1.2
      */
    def this(x1: SFloat, y1: SFloat, ctrlx: SFloat, ctrly: SFloat, x2: SFloat, y2: SFloat) = {
      this()
      setCurve(x1, y1, ctrlx, ctrly, x2, y2)
    }

    /**
      * ``
      *
      * @since 1.2
      */
    override def getX1: SDouble = x1.toDouble

    override def getY1: SDouble = y1.toDouble

    override def getP1 = new Point2D.Float(x1, y1)

    override def getCtrlX: SDouble = ctrlx.toDouble

    override def getCtrlY: SDouble = ctrly.toDouble

    override def getCtrlPt = new Point2D.Float(ctrlx, ctrly)

    override def getX2: SDouble = x2.toDouble

    override def getY2: SDouble = y2.toDouble

    override def getP2 = new Point2D.Float(x2, y2)

    override def setCurve(x1: SDouble, y1: SDouble, ctrlx: SDouble, ctrly: SDouble, x2: SDouble, y2: SDouble): Unit = {
      this.x1     = x1.toFloat
      this.y1     = y1.toFloat
      this.ctrlx  = ctrlx.toFloat
      this.ctrly  = ctrly.toFloat
      this.x2     = x2.toFloat
      this.y2     = y2.toFloat
    }

    /**
      * Sets the location of the end points and control point of this curve
      * to the specified `float` coordinates.
      *
      * @param x1    the X coordinate of the start point
      * @param y1    the Y coordinate of the start point
      * @param ctrlx the X coordinate of the control point
      * @param ctrly the Y coordinate of the control point
      * @param x2    the X coordinate of the end point
      * @param y2    the Y coordinate of the end point
      * @since 1.2
      */
    def setCurve(x1: SFloat, y1: SFloat, ctrlx: SFloat, ctrly: SFloat, x2: SFloat, y2: SFloat): Unit = {
      this.x1     = x1
      this.y1     = y1
      this.ctrlx  = ctrlx
      this.ctrly  = ctrly
      this.x2     = x2
      this.y2     = y2
    }

    override def getBounds2D: Rectangle2D = {
      val left    = Math.min(Math.min(x1, x2), ctrlx)
      val top     = Math.min(Math.min(y1, y2), ctrly)
      val right   = Math.max(Math.max(x1, x2), ctrlx)
      val bottom  = Math.max(Math.max(y1, y2), ctrly)
      new Rectangle2D.Float(left, top, right - left, bottom - top)
    }
  }

  /**
    * A quadratic parametric curve segment specified with
    * `double` coordinates.
    */
  class Double() extends QuadCurve2D with Serializable {
    private var x1    = 0.0
    private var y1    = 0.0
    private var ctrlx = 0.0
    private var ctrly = 0.0
    private var x2    = 0.0
    private var y2    = 0.0

    /**
      * Constructs and initializes a `QuadCurve2D` from the
      * specified `double` coordinates.
      *
      * @param x1    the X coordinate of the start point
      * @param y1    the Y coordinate of the start point
      * @param ctrlx the X coordinate of the control point
      * @param ctrly the Y coordinate of the control point
      * @param x2    the X coordinate of the end point
      * @param y2    the Y coordinate of the end point
      * @since 1.2
      */
    def this(x1: SDouble, y1: SDouble, ctrlx: SDouble, ctrly: SDouble, x2: SDouble, y2: SDouble) = {
      this()
      setCurve(x1, y1, ctrlx, ctrly, x2, y2)
    }

    override def getX1: SDouble = x1

    override def getY1: SDouble = y1

    override def getP1 = new Point2D.Double(x1, y1)

    override def getCtrlX: SDouble = ctrlx

    override def getCtrlY: SDouble = ctrly

    override def getCtrlPt = new Point2D.Double(ctrlx, ctrly)

    override def getX2: SDouble = x2

    override def getY2: SDouble = y2

    override def getP2 = new Point2D.Double(x2, y2)

    override def setCurve(x1: SDouble, y1: SDouble, ctrlx: SDouble, ctrly: SDouble, x2: SDouble, y2: SDouble): Unit = {
      this.x1     = x1
      this.y1     = y1
      this.ctrlx  = ctrlx
      this.ctrly  = ctrly
      this.x2     = x2
      this.y2     = y2
    }

    override def getBounds2D: Rectangle2D = {
      val left    = Math.min(Math.min(x1, x2), ctrlx)
      val top     = Math.min(Math.min(y1, y2), ctrly)
      val right   = Math.max(Math.max(x1, x2), ctrlx)
      val bottom  = Math.max(Math.max(y1, y2), ctrly)
      new Rectangle2D.Double(left, top, right - left, bottom - top)
    }
  }

  /**
    * Returns the square of the flatness, or maximum distance of a
    * control point from the line connecting the end points, of the
    * quadratic curve specified by the indicated control points.
    *
    * @param x1    the X coordinate of the start point
    * @param y1    the Y coordinate of the start point
    * @param ctrlx the X coordinate of the control point
    * @param ctrly the Y coordinate of the control point
    * @param x2    the X coordinate of the end point
    * @param y2    the Y coordinate of the end point
    * @return the square of the flatness of the quadratic curve
    *         defined by the specified coordinates.
    * @since 1.2
    */
  def getFlatnessSq(x1: SDouble, y1: SDouble, ctrlx: SDouble, ctrly: SDouble, x2: SDouble, y2: SDouble): SDouble =
    Line2D.ptSegDistSq(x1, y1, x2, y2, ctrlx, ctrly)

  /**
    * Returns the flatness, or maximum distance of a
    * control point from the line connecting the end points, of the
    * quadratic curve specified by the indicated control points.
    *
    * @param x1    the X coordinate of the start point
    * @param y1    the Y coordinate of the start point
    * @param ctrlx the X coordinate of the control point
    * @param ctrly the Y coordinate of the control point
    * @param x2    the X coordinate of the end point
    * @param y2    the Y coordinate of the end point
    * @return the flatness of the quadratic curve defined by the
    *         specified coordinates.
    * @since 1.2
    */
  def getFlatness(x1: SDouble, y1: SDouble, ctrlx: SDouble, ctrly: SDouble, x2: SDouble, y2: SDouble): SDouble =
    Line2D.ptSegDist(x1, y1, x2, y2, ctrlx, ctrly)

  /**
    * Returns the square of the flatness, or maximum distance of a
    * control point from the line connecting the end points, of the
    * quadratic curve specified by the control points stored in the
    * indicated array at the indicated index.
    *
    * @param coords an array containing coordinate values
    * @param offset the index into `coords` from which to
    *               to start getting the values from the array
    * @return the flatness of the quadratic curve that is defined by the
    *         values in the specified array at the specified index.
    * @since 1.2
    */
  def getFlatnessSq(coords: Array[SDouble], offset: Int): SDouble =
    Line2D.ptSegDistSq(coords(offset + 0), coords(offset + 1), coords(offset + 4),
      coords(offset + 5), coords(offset + 2), coords(offset + 3))

  /**
    * Returns the flatness, or maximum distance of a
    * control point from the line connecting the end points, of the
    * quadratic curve specified by the control points stored in the
    * indicated array at the indicated index.
    *
    * @param coords an array containing coordinate values
    * @param offset the index into `coords` from which to
    *               start getting the coordinate values
    * @return the flatness of a quadratic curve defined by the
    *         specified array at the specified offset.
    * @since 1.2
    */
  def getFlatness(coords: Array[SDouble], offset: Int): SDouble =
    Line2D.ptSegDist(coords(offset + 0), coords(offset + 1), coords(offset + 4),
      coords(offset + 5), coords(offset + 2), coords(offset + 3))

  /**
    * Subdivides the quadratic curve specified by the `src`
    * parameter and stores the resulting two subdivided curves into the
    * `left` and `right` curve parameters.
    * Either or both of the `left` and `right`
    * objects can be the same as the `src` object or
    * `null`.
    *
    * @param src   the quadratic curve to be subdivided
    * @param left  the `QuadCurve2D` object for storing the
    *              left or first half of the subdivided curve
    * @param right the `QuadCurve2D` object for storing the
    *              right or second half of the subdivided curve
    * @since 1.2
    */
  def subdivide(src: QuadCurve2D, left: QuadCurve2D, right: QuadCurve2D): Unit = {
    val x1      = src.getX1
    val y1      = src.getY1
    var ctrlx   = src.getCtrlX
    var ctrly   = src.getCtrlY
    val x2      = src.getX2
    val y2      = src.getY2
    val ctrlx1  = (x1 + ctrlx) / 2.0
    val ctrly1  = (y1 + ctrly) / 2.0
    val ctrlx2  = (x2 + ctrlx) / 2.0
    val ctrly2  = (y2 + ctrly) / 2.0
    ctrlx       = (ctrlx1 + ctrlx2) / 2.0
    ctrly       = (ctrly1 + ctrly2) / 2.0
    if (left  != null) left .setCurve(x1, y1, ctrlx1, ctrly1, ctrlx, ctrly)
    if (right != null) right.setCurve(ctrlx, ctrly, ctrlx2, ctrly2, x2, y2)
  }

  /**
    * Subdivides the quadratic curve specified by the coordinates
    * stored in the `src` array at indices
    * `srcoff` through `srcoff`&nbsp;+&nbsp;5
    * and stores the resulting two subdivided curves into the two
    * result arrays at the corresponding indices.
    * Either or both of the `left` and `right`
    * arrays can be `null` or a reference to the same array
    * and offset as the `src` array.
    * Note that the last point in the first subdivided curve is the
    * same as the first point in the second subdivided curve.  Thus,
    * it is possible to pass the same array for `left` and
    * `right` and to use offsets such that
    * `rightoff` equals `leftoff` + 4 in order
    * to avoid allocating extra storage for this common point.
    *
    * @param src      the array holding the coordinates for the source curve
    * @param srcoff   the offset into the array of the beginning of the
    *                 the 6 source coordinates
    * @param left     the array for storing the coordinates for the first
    *                 half of the subdivided curve
    * @param leftoff  the offset into the array of the beginning of the
    *                 the 6 left coordinates
    * @param right    the array for storing the coordinates for the second
    *                 half of the subdivided curve
    * @param rightoff the offset into the array of the beginning of the
    *                 the 6 right coordinates
    * @since 1.2
    */
  def subdivide(src: Array[SDouble], srcoff: Int, left: Array[SDouble], leftoff: Int,
                right: Array[SDouble], rightoff: Int): Unit = {
    var x1 = src(srcoff + 0)
    var y1 = src(srcoff + 1)
    var ctrlx = src(srcoff + 2)
    var ctrly = src(srcoff + 3)
    var x2 = src(srcoff + 4)
    var y2 = src(srcoff + 5)
    if (left != null) {
      left(leftoff + 0) = x1
      left(leftoff + 1) = y1
    }
    if (right != null) {
      right(rightoff + 4) = x2
      right(rightoff + 5) = y2
    }
    x1 = (x1 + ctrlx) / 2.0
    y1 = (y1 + ctrly) / 2.0
    x2 = (x2 + ctrlx) / 2.0
    y2 = (y2 + ctrly) / 2.0
    ctrlx = (x1 + x2) / 2.0
    ctrly = (y1 + y2) / 2.0
    if (left != null) {
      left(leftoff + 2) = x1
      left(leftoff + 3) = y1
      left(leftoff + 4) = ctrlx
      left(leftoff + 5) = ctrly
    }
    if (right != null) {
      right(rightoff + 0) = ctrlx
      right(rightoff + 1) = ctrly
      right(rightoff + 2) = x2
      right(rightoff + 3) = y2
    }
  }

  /**
    * Solves the quadratic whose coefficients are in the `eqn`
    * array and places the non-complex roots back into the same array,
    * returning the number of roots.  The quadratic solved is represented
    * by the equation:
    *
    * {{{
    * eqn = {C, B, A};
    * ax^2 + bx + c = 0
    * }}}
    *
    * A return value of `-1` is used to distinguish a constant
    * equation, which might be always 0 or never 0, from an equation that
    * has no zeroes.
    * @param eqn the array that contains the quadratic coefficients
    * @return the number of roots, or `-1` if the equation is
    *         a constant
    * @since 1.2
    */
  def solveQuadratic(eqn: Array[SDouble]): Int = solveQuadratic(eqn, eqn)

  /**
    * Solves the quadratic whose coefficients are in the `eqn`
    * array and places the non-complex roots into the `res`
    * array, returning the number of roots.
    * The quadratic solved is represented by the equation:
    *
    * {{{
    * eqn = {C, B, A};
    * ax^2 + bx + c = 0
    * }}}
    *
    * A return value of `-1` is used to distinguish a constant
    * equation, which might be always 0 or never 0, from an equation that
    * has no zeroes.
    * @param eqn the specified array of coefficients to use to solve
    *            the quadratic equation
    * @param res the array that contains the non-complex roots
    *            resulting from the solution of the quadratic equation
    * @return the number of roots, or `-1` if the equation is
    *         a constant.
    * @since 1.3
    */
  def solveQuadratic(eqn: Array[SDouble], res: Array[SDouble]): Int = {
    val a = eqn(2)
    val b = eqn(1)
    val c = eqn(0)
    var roots = 0
    if (a == 0.0) { // The quadratic parabola has degenerated to a line.
      if (b == 0.0) { // The line has degenerated to a constant.
        return -1
      }
      res({
        roots += 1; roots - 1
      }) = -c / b
    }
    else { // From Numerical Recipes, 5.6, Quadratic and Cubic Equations
      var d = b * b - 4.0 * a * c
      if (d < 0.0) { // If d < 0.0, then there are no roots
        return 0
      }
      d = Math.sqrt(d)
      // For accuracy, calculate one root using:
      //     (-b +/- d) / 2a
      // and the other using:
      //     2c / (-b +/- d)
      // Choose the sign of the +/- so that b+d gets larger in magnitude
      if (b < 0.0) d = -d
      val q = (b + d) / -2.0
      // We already tested a for being 0 above
      res({
        roots += 1; roots - 1
      }) = q / a
      if (q != 0.0) res({
        roots += 1; roots - 1
      }) = c / q
    }
    roots
  }

  /**
    * Fill an array with the coefficients of the parametric equation
    * in t, ready for solving against val with solveQuadratic.
    * We currently have:
    *
    * {{{
    * val = Py(t) = C1*(1-t)^2 + 2*CP*t*(1-t) + C2*t^2
    * = C1 - 2*C1*t + C1*t^2 + 2*CP*t - 2*CP*t^2 + C2*t^2
    * = C1 + (2*CP - 2*C1)*t + (C1 - 2*CP + C2)*t^2
    * 0 = (C1 - val) + (2*CP - 2*C1)*t + (C1 - 2*CP + C2)*t^2
    * 0 = C + Bt + At^2
    * C = C1 - val
    * B = 2*CP - 2*C1
    * A = C1 - 2*CP + C2
    * }}}
    */
  private def fillEqn(eqn: Array[SDouble], `val`: SDouble, c1: SDouble, cp: SDouble, c2: SDouble): Unit = {
    eqn(0) = c1 - `val`
    eqn(1) = cp + cp - c1 - c1
    eqn(2) = c1 - cp - cp + c2
  }

  /**
    * Evaluate the t values in the first num slots of the vals[] array
    * and place the evaluated values back into the same array.  Only
    * evaluate t values that are within the range &lt;0, 1&gt;, including
    * the 0 and 1 ends of the range iff the include0 or include1
    * booleans are true.  If an "inflection" equation is handed in,
    * then any points which represent a point of inflection for that
    * quadratic equation are also ignored.
    */
  private def evalQuadratic(vals: Array[SDouble], num: Int, include0: Boolean, include1: Boolean,
                            inflect: Array[SDouble], c1: SDouble, ctrl: SDouble, c2: SDouble) = {
    var j = 0
    for (i <- 0 until num) {
      val t = vals(i)
      if ((if (include0) t >= 0
      else t > 0) && (if (include1) t <= 1
      else t < 1) && (inflect == null || inflect(1) + 2 * inflect(2) * t != 0)) {
        val u = 1 - t
        vals({
          j += 1; j - 1
        }) = c1 * u * u + 2 * ctrl * t * u + c2 * t * t
      }
    }
    j
  }

  private final val BELOW     = -2
  private final val LOWEDGE   = -1
  private final val INSIDE    = 0
  private final val HIGHEDGE  = 1
  private final val ABOVE     = 2

  /**
    * Determine where coord lies with respect to the range from
    * low to high.  It is assumed that low &lt;= high.  The return
    * value is one of the 5 values BELOW, LOWEDGE, INSIDE, HIGHEDGE,
    * or ABOVE.
    */
  private def getTag(coord: SDouble, low: SDouble, high: SDouble): Int = {
    if (coord <= low ) return if (coord < low ) BELOW else LOWEDGE
    if (coord >= high) return if (coord > high) ABOVE else HIGHEDGE
    INSIDE
  }

  /**
    * Determine if the pttag represents a coordinate that is already
    * in its test range, or is on the border with either of the two
    * opttags representing another coordinate that is "towards the
    * inside" of that test range.  In other words, are either of the
    * two "opt" points "drawing the pt inward"?
    */
  private def inwards(pttag: Int, opt1tag: Int, opt2tag: Int): Boolean =
    pttag match {
      case BELOW | ABOVE  => false
      case LOWEDGE        => opt1tag >= INSIDE || opt2tag >= INSIDE
      case INSIDE         => true
      case HIGHEDGE       => opt1tag <= INSIDE || opt2tag <= INSIDE
      case _              => false
    }
}

/**
  * This is an abstract class that cannot be instantiated directly.
  * Type-specific implementation subclasses are available for
  * instantiation and provide a number of formats for storing
  * the information necessary to satisfy the various accessor
  * methods below.
  *
  * @see java.awt.geom.QuadCurve2D.Float
  * @see java.awt.geom.QuadCurve2D.Double
  */
abstract class QuadCurve2D protected() extends Shape with Cloneable {
  /**
    * Returns the X coordinate of the start point in
    * `double` in precision.
    *
    * @return the X coordinate of the start point.
    * @since 1.2
    */
  def getX1: SDouble

  /**
    * Returns the Y coordinate of the start point in
    * `double` precision.
    *
    * @return the Y coordinate of the start point.
    * @since 1.2
    */
  def getY1: SDouble

  /**
    * Returns the start point.
    *
    * @return a `Point2D` that is the start point of this
    *         `QuadCurve2D`.
    * @since 1.2
    */
  def getP1: Point2D

  /**
    * Returns the X coordinate of the control point in
    * `double` precision.
    *
    * @return X coordinate the control point
    * @since 1.2
    */
  def getCtrlX: SDouble

  /**
    * Returns the Y coordinate of the control point in
    * `double` precision.
    *
    * @return the Y coordinate of the control point.
    * @since 1.2
    */
  def getCtrlY: SDouble

  /**
    * Returns the control point.
    *
    * @return a `Point2D` that is the control point of this
    *         `Point2D`.
    * @since 1.2
    */
  def getCtrlPt: Point2D

  /**
    * Returns the X coordinate of the end point in
    * `double` precision.
    *
    * @return the x coordinate of the end point.
    * @since 1.2
    */
  def getX2: SDouble

  /**
    * Returns the Y coordinate of the end point in
    * `double` precision.
    *
    * @return the Y coordinate of the end point.
    * @since 1.2
    */
  def getY2: SDouble

  /**
    * Returns the end point.
    *
    * @return a `Point` object that is the end point
    *         of this `Point2D`.
    * @since 1.2
    */
  def getP2: Point2D

  /**
    * Sets the location of the end points and control point of this curve
    * to the specified `double` coordinates.
    *
    * @param x1    the X coordinate of the start point
    * @param y1    the Y coordinate of the start point
    * @param ctrlx the X coordinate of the control point
    * @param ctrly the Y coordinate of the control point
    * @param x2    the X coordinate of the end point
    * @param y2    the Y coordinate of the end point
    * @since 1.2
    */
  def setCurve(x1: SDouble, y1: SDouble, ctrlx: SDouble, ctrly: SDouble, x2: SDouble, y2: SDouble): Unit

  /**
    * Sets the location of the end points and control points of this
    * `QuadCurve2D` to the `double` coordinates at
    * the specified offset in the specified array.
    *
    * @param coords the array containing coordinate values
    * @param offset the index into the array from which to start
    *               getting the coordinate values and assigning them to this
    *               `QuadCurve2D`
    * @since 1.2
    */
  def setCurve(coords: Array[SDouble], offset: Int): Unit = {
    setCurve(coords(offset + 0), coords(offset + 1), coords(offset + 2),
      coords(offset + 3), coords(offset + 4), coords(offset + 5))
  }

  /**
    * Sets the location of the end points and control point of this
    * `QuadCurve2D` to the specified `Point2D`
    * coordinates.
    *
    * @param p1 the start point
    * @param cp the control point
    * @param p2 the end point
    * @since 1.2
    */
  def setCurve(p1: Point2D, cp: Point2D, p2: Point2D): Unit =
    setCurve(p1.getX, p1.getY, cp.getX, cp.getY, p2.getX, p2.getY)

  /**
    * Sets the location of the end points and control points of this
    * `QuadCurve2D` to the coordinates of the
    * `Point2D` objects at the specified offset in
    * the specified array.
    *
    * @param pts    an array containing `Point2D` that define
    *               coordinate values
    * @param offset the index into `pts` from which to start
    *               getting the coordinate values and assigning them to this
    *               `QuadCurve2D`
    * @since 1.2
    */
  def setCurve(pts: Array[Point2D], offset: Int): Unit =
    setCurve(pts(offset + 0).getX, pts(offset + 0).getY, pts(offset + 1).getX,
      pts(offset + 1).getY, pts(offset + 2).getX, pts(offset + 2).getY)

  /**
    * Sets the location of the end points and control point of this
    * `QuadCurve2D` to the same as those in the specified
    * `QuadCurve2D`.
    *
    * @param c the specified `QuadCurve2D`
    * @since 1.2
    */
  def setCurve(c: QuadCurve2D): Unit =
    setCurve(c.getX1, c.getY1, c.getCtrlX, c.getCtrlY, c.getX2, c.getY2)

  /**
    * Returns the square of the flatness, or maximum distance of a
    * control point from the line connecting the end points, of this
    * `QuadCurve2D`.
    *
    * @return the square of the flatness of this
    *         `QuadCurve2D`.
    * @since 1.2
    */
  def getFlatnessSq: SDouble = Line2D.ptSegDistSq(getX1, getY1, getX2, getY2, getCtrlX, getCtrlY)

  /**
    * Returns the flatness, or maximum distance of a
    * control point from the line connecting the end points, of this
    * `QuadCurve2D`.
    *
    * @return the flatness of this `QuadCurve2D`.
    * @since 1.2
    */
  def getFlatness: SDouble = Line2D.ptSegDist(getX1, getY1, getX2, getY2, getCtrlX, getCtrlY)

  /**
    * Subdivides this `QuadCurve2D` and stores the resulting
    * two subdivided curves into the `left` and
    * `right` curve parameters.
    * Either or both of the `left` and `right`
    * objects can be the same as this `QuadCurve2D` or
    * `null`.
    *
    * @param left  the `QuadCurve2D` object for storing the
    *              left or first half of the subdivided curve
    * @param right the `QuadCurve2D` object for storing the
    *              right or second half of the subdivided curve
    * @since 1.2
    */
  def subdivide(left: QuadCurve2D, right: QuadCurve2D): Unit =
    QuadCurve2D.subdivide(this, left, right)

  /**
    * ``
    *
    * @since 1.2
    */
  override def contains(x: SDouble, y: SDouble): Boolean = {
    val x1 = getX1
    val y1 = getY1
    val xc = getCtrlX
    val yc = getCtrlY
    val x2 = getX2
    val y2 = getY2
    /*
             * We have a convex shape bounded by quad curve Pc(t)
             * and ine Pl(t).
             *
             *     P1 = (x1, y1) - start point of curve
             *     P2 = (x2, y2) - end point of curve
             *     Pc = (xc, yc) - control point
             *
             *     Pq(t) = P1*(1 - t)^2 + 2*Pc*t*(1 - t) + P2*t^2 =
             *           = (P1 - 2*Pc + P2)*t^2 + 2*(Pc - P1)*t + P1
             *     Pl(t) = P1*(1 - t) + P2*t
             *     t = [0:1]
             *
             *     P = (x, y) - point of interest
             *
             * Let's look at second derivative of quad curve equation:
             *
             *     Pq''(t) = 2 * (P1 - 2 * Pc + P2) = Pq''
             *     It's constant vector.
             *
             * Let's draw a line through P to be parallel to this
             * vector and find the intersection of the quad curve
             * and the line.
             *
             * Pq(t) is point of intersection if system of equations
             * below has the solution.
             *
             *     L(s) = P + Pq''*s == Pq(t)
             *     Pq''*s + (P - Pq(t)) == 0
             *
             *     | xq''*s + (x - xq(t)) == 0
             *     | yq''*s + (y - yq(t)) == 0
             *
             * This system has the solution if rank of its matrix equals to 1.
             * That is, determinant of the matrix should be zero.
             *
             *     (y - yq(t))*xq'' == (x - xq(t))*yq''
             *
             * Let's solve this equation with 't' variable.
             * Also let kx = x1 - 2*xc + x2
             *          ky = y1 - 2*yc + y2
             *
             *     t0q = (1/2)*((x - x1)*ky - (y - y1)*kx) /
             *                 ((xc - x1)*ky - (yc - y1)*kx)
             *
             * Let's do the same for our line Pl(t):
             *
             *     t0l = ((x - x1)*ky - (y - y1)*kx) /
             *           ((x2 - x1)*ky - (y2 - y1)*kx)
             *
             * It's easy to check that t0q == t0l. This fact means
             * we can compute t0 only one time.
             *
             * In case t0 < 0 or t0 > 1, we have an intersections outside
             * of shape bounds. So, P is definitely out of shape.
             *
             * In case t0 is inside [0:1], we should calculate Pq(t0)
             * and Pl(t0). We have three points for now, and all of them
             * lie on one line. So, we just need to detect, is our point
             * of interest between points of intersections or not.
             *
             * If the denominator in the t0q and t0l equations is
             * zero, then the points must be collinear and so the
             * curve is degenerate and encloses no area.  Thus the
             * result is false.
             */
    val kx = x1 - 2 * xc + x2
    val ky = y1 - 2 * yc + y2
    val dx = x - x1
    val dy = y - y1
    val dxl = x2 - x1
    val dyl = y2 - y1
    val t0 = (dx * ky - dy * kx) / (dxl * ky - dyl * kx)
    if (t0 < 0 || t0 > 1 || t0 != t0) return false
    val xb = kx * t0 * t0 + 2 * (xc - x1) * t0 + x1
    val yb = ky * t0 * t0 + 2 * (yc - y1) * t0 + y1
    val xl = dxl * t0 + x1
    val yl = dyl * t0 + y1
    (x >= xb && x < xl) || (x >= xl && x < xb) || (y >= yb && y < yl) || (y >= yl && y < yb)
  }

  override def contains(p: Point2D): Boolean = contains(p.getX, p.getY)

  override def intersects(x: SDouble, y: SDouble, w: SDouble, h: SDouble): Boolean = { // Trivially reject non-existant rectangles
    if (w <= 0 || h <= 0) return false
    // Trivially accept if either endpoint is inside the rectangle
    // (not on its border since it may end there and not go inside)
    // Record where they lie with respect to the rectangle.
    //     -1 => left, 0 => inside, 1 => right
    val x1 = getX1
    val y1 = getY1
    val x1tag = QuadCurve2D.getTag(x1, x, x + w)
    val y1tag = QuadCurve2D.getTag(y1, y, y + h)
    if (x1tag == QuadCurve2D.INSIDE && y1tag == QuadCurve2D.INSIDE) return true
    val x2 = getX2
    val y2 = getY2
    val x2tag = QuadCurve2D.getTag(x2, x, x + w)
    val y2tag = QuadCurve2D.getTag(y2, y, y + h)
    if (x2tag == QuadCurve2D.INSIDE && y2tag == QuadCurve2D.INSIDE) return true
    val ctrlx = getCtrlX
    val ctrly = getCtrlY
    val ctrlxtag = QuadCurve2D.getTag(ctrlx, x, x + w)
    val ctrlytag = QuadCurve2D.getTag(ctrly, y, y + h)
    // Trivially reject if all points are entirely to one side of
    // the rectangle.
    if (x1tag < QuadCurve2D.INSIDE && x2tag < QuadCurve2D.INSIDE && ctrlxtag < QuadCurve2D.INSIDE) return false // All points left
    if (y1tag < QuadCurve2D.INSIDE && y2tag < QuadCurve2D.INSIDE && ctrlytag < QuadCurve2D.INSIDE) return false // All points above
    if (x1tag > QuadCurve2D.INSIDE && x2tag > QuadCurve2D.INSIDE && ctrlxtag > QuadCurve2D.INSIDE) return false // All points right
    if (y1tag > QuadCurve2D.INSIDE && y2tag > QuadCurve2D.INSIDE && ctrlytag > QuadCurve2D.INSIDE) return false // All points below
    // Test for endpoints on the edge where either the segment
    // or the curve is headed "inwards" from them
    // Note: These tests are a superset of the fast endpoint tests
    //       above and thus repeat those tests, but take more time
    //       and cover more cases
    if (QuadCurve2D.inwards(x1tag, x2tag, ctrlxtag) && QuadCurve2D.inwards(y1tag, y2tag, ctrlytag)) { // First endpoint on border with either edge moving inside
      return true
    }
    if (QuadCurve2D.inwards(x2tag, x1tag, ctrlxtag) && QuadCurve2D.inwards(y2tag, y1tag, ctrlytag)) { // Second endpoint on border with either edge moving inside
      return true
    }
    // Trivially accept if endpoints span directly across the rectangle
    val xoverlap = x1tag * x2tag <= 0
    val yoverlap = y1tag * y2tag <= 0
    if (x1tag == QuadCurve2D.INSIDE && x2tag == QuadCurve2D.INSIDE && yoverlap) return true
    if (y1tag == QuadCurve2D.INSIDE && y2tag == QuadCurve2D.INSIDE && xoverlap) return true
    // We now know that both endpoints are outside the rectangle
    // but the 3 points are not all on one side of the rectangle.
    // Therefore the curve cannot be contained inside the rectangle,
    // but the rectangle might be contained inside the curve, or
    // the curve might intersect the boundary of the rectangle.
    val eqn = new Array[SDouble](3)
    val res = new Array[SDouble](3)
    if (!yoverlap) { // Both Y coordinates for the closing segment are above or
      // below the rectangle which means that we can only intersect
      // if the curve crosses the top (or bottom) of the rectangle
      // in more than one place and if those crossing locations
      // span the horizontal range of the rectangle.
      QuadCurve2D.fillEqn(eqn, if (y1tag < QuadCurve2D.INSIDE) y
      else y + h, y1, ctrly, y2)
      return QuadCurve2D.solveQuadratic(eqn, res) == 2 && QuadCurve2D.evalQuadratic(res, 2, include0 = true, include1 = true, null, x1, ctrlx, x2) == 2 && QuadCurve2D.getTag(res(0), x, x + w) * QuadCurve2D.getTag(res(1), x, x + w) <= 0
    }
    // Y ranges overlap.  Now we examine the X ranges
    if (!xoverlap) { // Both X coordinates for the closing segment are left of
      // or right of the rectangle which means that we can only
      // intersect if the curve crosses the left (or right) edge
      // of the rectangle in more than one place and if those
      // crossing locations span the vertical range of the rectangle.
      QuadCurve2D.fillEqn(eqn, if (x1tag < QuadCurve2D.INSIDE) x
      else x + w, x1, ctrlx, x2)
      return QuadCurve2D.solveQuadratic(eqn, res) == 2 && QuadCurve2D.evalQuadratic(res, 2, include0 = true, include1 = true, null, y1, ctrly, y2) == 2 && QuadCurve2D.getTag(res(0), y, y + h) * QuadCurve2D.getTag(res(1), y, y + h) <= 0
    }
    // The X and Y ranges of the endpoints overlap the X and Y
    // ranges of the rectangle, now find out how the endpoint
    // line segment intersects the Y range of the rectangle
    val dx = x2 - x1
    val dy = y2 - y1
    val k = y2 * x1 - x2 * y1
    var c1tag = 0
    var c2tag = 0
    if (y1tag == QuadCurve2D.INSIDE) c1tag = x1tag
    else c1tag = QuadCurve2D.getTag((k + dx * (if (y1tag < QuadCurve2D.INSIDE) y
    else y + h)) / dy, x, x + w)
    if (y2tag == QuadCurve2D.INSIDE) c2tag = x2tag
    else c2tag = QuadCurve2D.getTag((k + dx * (if (y2tag < QuadCurve2D.INSIDE) y
    else y + h)) / dy, x, x + w)
    // If the part of the line segment that intersects the Y range
    // of the rectangle crosses it horizontally - trivially accept
    if (c1tag * c2tag <= 0) return true
    // Now we know that both the X and Y ranges intersect and that
    // the endpoint line segment does not directly cross the rectangle.
    //
    // We can almost treat this case like one of the cases above
    // where both endpoints are to one side, except that we will
    // only get one intersection of the curve with the vertical
    // side of the rectangle.  This is because the endpoint segment
    // accounts for the other intersection.
    // (Remember there is overlap in both the X and Y ranges which
    //  means that the segment must cross at least one vertical edge
    //  of the rectangle - in particular, the "near vertical side" -
    //  leaving only one intersection for the curve.)
    // Now we calculate the y tags of the two intersections on the
    // "near vertical side" of the rectangle.  We will have one with
    // the endpoint segment, and one with the curve.  If those two
    // vertical intersections overlap the Y range of the rectangle,
    // we have an intersection.  Otherwise, we don't.
    // c1tag = vertical intersection class of the endpoint segment
    // Choose the y tag of the endpoint that was not on the same
    // side of the rectangle as the subsegment calculated above.
    // Note that we can "steal" the existing Y tag of that endpoint
    // since it will be provably the same as the vertical intersection.
    c1tag = if (c1tag * x1tag <= 0) y1tag else y2tag
    // c2tag = vertical intersection class of the curve
    // We have to calculate this one the straightforward way.
    // Note that the c2tag can still tell us which vertical edge
    // to test against.
    QuadCurve2D.fillEqn(eqn, if (c2tag < QuadCurve2D.INSIDE) x else x + w, x1, ctrlx, x2)
    val num = QuadCurve2D.solveQuadratic(eqn, res)
    // Note: We should be able to assert(num == 2); since the
    // X range "crosses" (not touches) the vertical boundary,
    // but we pass num to evalQuadratic for completeness.
    QuadCurve2D.evalQuadratic(res, num, include0 = true, include1 = true, null, y1, ctrly, y2)
    // Note: We can assert(num evals == 1); since one of the
    // 2 crossings will be out of the [0,1] range.
    c2tag = QuadCurve2D.getTag(res(0), y, y + h)
    // Finally, we have an intersection if the two crossings
    // overlap the Y range of the rectangle.
    c1tag * c2tag <= 0
  }

  override def intersects(r: Rectangle2D): Boolean = intersects(r.getX, r.getY, r.getWidth, r.getHeight)

  override def contains(x: SDouble, y: SDouble, w: SDouble, h: SDouble): Boolean = {
    if (w <= 0 || h <= 0) return false
    // Assertion: Quadratic curves closed by connecting their
    // endpoints are always convex.
    contains(x, y) && contains(x + w, y) && contains(x + w, y + h) && contains(x, y + h)
  }

  override def contains(r: Rectangle2D): Boolean = contains(r.getX, r.getY, r.getWidth, r.getHeight)

//  override def getBounds: Rectangle = getBounds2D.getBounds

  /**
    * Returns an iteration object that defines the boundary of the
    * shape of this `QuadCurve2D`.
    * The iterator for this class is not multi-threaded safe,
    * which means that this `QuadCurve2D` class does not
    * guarantee that modifications to the geometry of this
    * `QuadCurve2D` object do not affect any iterations of
    * that geometry that are already in process.
    *
    * @param at an optional `AffineTransform` to apply to the
    *           shape boundary
    * @return a `PathIterator` object that defines the boundary
    *         of the shape.
    * @since 1.2
    */
  override def getPathIterator(at: AffineTransform) = new QuadIterator(this, at)

  /**
    * Returns an iteration object that defines the boundary of the
    * flattened shape of this `QuadCurve2D`.
    * The iterator for this class is not multi-threaded safe,
    * which means that this `QuadCurve2D` class does not
    * guarantee that modifications to the geometry of this
    * `QuadCurve2D` object do not affect any iterations of
    * that geometry that are already in process.
    *
    * @param at       an optional `AffineTransform` to apply
    *                 to the boundary of the shape
    * @param flatness the maximum distance that the control points for a
    *                 subdivided curve can be with respect to a line connecting
    *                 the end points of this curve before this curve is
    *                 replaced by a straight line connecting the end points.
    * @return a `PathIterator` object that defines the
    *         flattened boundary of the shape.
    * @since 1.2
    */
  override def getPathIterator(at: AffineTransform, flatness: SDouble) =
    new FlatteningPathIterator(getPathIterator(at), flatness)

  /**
    * Creates a new object of the same class and with the same contents
    * as this object.
    *
    * @return a clone of this instance.
    * @throws OutOfMemoryError            if there is not enough memory.
    * @see java.lang.Cloneable
    */
  override def clone: Any =
    try {
      super.clone
    } catch {
      case e: CloneNotSupportedException =>
        // this shouldn't happen, since we are Cloneable
        throw new InternalError(e)
    }
}
