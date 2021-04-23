package de.sciss.infibrillae.geom

// This is an adapted Scala translation of the Area Java class of OpenJDK
// as released under GNU GPL 2 -- see original file header below.
// So it can be used in Scala.js.

/*
 * Copyright (c) 1998, 2014, Oracle and/or its affiliates. All rights reserved.
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

import de.sciss.infibrillae.geom.Area.pathToCurves
import de.sciss.infibrillae.geom.PathIterator.{SEG_CLOSE, SEG_CUBICTO, SEG_LINETO, SEG_QUADTO, WIND_NON_ZERO}
import de.sciss.infibrillae.geom.impl.{AreaOp, Crossings, Curve}

import java.util.NoSuchElementException
import scala.collection.mutable
import scala.collection.{Seq => CSeq}

/**
  * An `Area` object stores and manipulates a
  * resolution-independent description of an enclosed area of
  * 2-dimensional space.
  * `Area` objects can be transformed and can perform
  * various Constructive Area Geometry (CAG) operations when combined
  * with other `Area` objects.
  * The CAG operations include area
  * `#`, `#`,
  * `#`, and `#`.
  * See the linked method documentation for examples of the various
  * operations.
  * <p>
  * The `Area` class implements the `Shape`
  * interface and provides full support for all of its hit-testing
  * and path iteration facilities, but an `Area` is more
  * specific than a generalized path in a number of ways:
  * <ul>
  * <li>Only closed paths and sub-paths are stored.
  * `Area` objects constructed from unclosed paths
  * are implicitly closed during construction as if those paths
  * had been filled by the `Graphics2D.fill` method.
  * <li>The interiors of the individual stored sub-paths are all
  * non-empty and non-overlapping.  Paths are decomposed during
  * construction into separate component non-overlapping parts,
  * empty pieces of the path are discarded, and then these
  * non-empty and non-overlapping properties are maintained
  * through all subsequent CAG operations.  Outlines of different
  * component sub-paths may touch each other, as long as they
  * do not cross so that their enclosed areas overlap.
  * <li>The geometry of the path describing the outline of the
  * `Area` resembles the path from which it was
  * constructed only in that it describes the same enclosed
  * 2-dimensional area, but may use entirely different types
  * and ordering of the path segments to do so.
  * </ul>
  * Interesting issues which are not always obvious when using
  * the `Area` include:
  * <ul>
  * <li>Creating an `Area` from an unclosed (open)
  * `Shape` results in a closed outline in the
  * `Area` object.
  * <li>Creating an `Area` from a `Shape`
  * which encloses no area (even when "closed") produces an
  * empty `Area`.  A common example of this issue
  * is that producing an `Area` from a line will
  * be empty since the line encloses no area.  An empty
  * `Area` will iterate no geometry in its
  * `PathIterator` objects.
  * <li>A self-intersecting `Shape` may be split into
  * two (or more) sub-paths each enclosing one of the
  * non-intersecting portions of the original path.
  * <li>An `Area` may take more path segments to
  * describe the same geometry even when the original
  * outline is simple and obvious.  The analysis that the
  * `Area` class must perform on the path may
  * not reflect the same concepts of "simple and obvious"
  * as a human being perceives.
  * </ul>
  *
  * @since 1.2
  */
object Area {
  private val EmptyCurves = Vector.empty[Curve] // new java.util.Vector[Curve]

  private def pathToCurves(pi: PathIterator): CSeq[Curve] = {
    val curves = mutable.ArrayBuffer.empty[Curve]
    val windingRule = pi.getWindingRule
    // coords array is big enough for holding:
    //     coordinates returned from currentSegment (6)
    //     OR
    //         two subdivided quadratic curves (2+4+4=10)
    //         AND
    //             0-1 horizontal splitting parameters
    //             OR
    //             2 parametric equation derivative coefficients
    //         three subdivided cubic curves (2+6+6+6=20)
    //             0-2 horizontal splitting parameters
    //             3 parametric equation derivative coefficients
    val coords = new Array[Double](23)
    var movx = 0.0
    var movy = 0.0
    var curx = 0.0
    var cury = 0.0
    var newx = .0
    var newy = .0
    while (!pi.isDone) {
      pi.currentSegment(coords) match {
        case PathIterator.SEG_MOVETO =>
          Curve.insertLine(curves, curx, cury, movx, movy)
          movx = coords(0)
          curx = movx
          movy = coords(1)
          cury = movy
          Curve.insertMove(curves, movx, movy)

        case PathIterator.SEG_LINETO =>
          newx = coords(0)
          newy = coords(1)
          Curve.insertLine(curves, curx, cury, newx, newy)
          curx = newx
          cury = newy

        case PathIterator.SEG_QUADTO =>
          newx = coords(2)
          newy = coords(3)
          Curve.insertQuad(curves, curx, cury, coords)
          curx = newx
          cury = newy

        case PathIterator.SEG_CUBICTO =>
          newx = coords(4)
          newy = coords(5)
          Curve.insertCubic(curves, curx, cury, coords)
          curx = newx
          cury = newy

        case PathIterator.SEG_CLOSE =>
          Curve.insertLine(curves, curx, cury, movx, movy)
          curx = movx
          cury = movy

      }
      pi.next()
    }
    Curve.insertLine(curves, curx, cury, movx, movy)
    val operator = if (windingRule == PathIterator.WIND_EVEN_ODD) new AreaOp.EOWindOp else new AreaOp.NZWindOp
    operator.calculate(curves, EmptyCurves)
  }
}

class Area private (private var curves: CSeq[Curve]) extends Shape with Cloneable {

  /**
    * Default constructor which creates an empty area.
    */
  def this() = this(Area.EmptyCurves)

  /**
    * The `Area` class creates an area geometry from the
    * specified `Shape` object.  The geometry is explicitly
    * closed, if the `Shape` is not already closed.  The
    * fill rule (even-odd or winding) specified by the geometry of the
    * `Shape` is used to determine the resulting enclosed area.
    *
    * @param s the `Shape` from which the area is constructed
    * @throws NullPointerException if `s` is null
    * @since 1.2
    */
  def this(s: Shape) =
    this(s match {
      case area: Area => area.curves
      case _          => pathToCurves(s.getPathIterator(null))
    })

  /**
    * Adds the shape of the specified `Area` to the
    * shape of this `Area`.
    * The resulting shape of this `Area` will include
    * the union of both shapes, or all areas that were contained
    * in either this or the specified `Area`.
    * <pre>
    * // Example:
    * Area a1 = new Area([triangle 0,0 =&gt; 8,0 =&gt; 0,8]);
    * Area a2 = new Area([triangle 0,0 =&gt; 8,0 =&gt; 8,8]);
    * a1.add(a2);
    *
    * a1(before)     +         a2         =     a1(after)
    *
    * ################     ################     ################
    * ##############         ##############     ################
    * ############             ############     ################
    * ##########                 ##########     ################
    * ########                     ########     ################
    * ######                         ######     ######    ######
    * ####                             ####     ####        ####
    * ##                                 ##     ##            ##
    * </pre>
    *
    * @param rhs the `Area` to be added to the
    *            current shape
    * @throws NullPointerException if `rhs` is null
    * @since 1.2
    */
  def add(rhs: Area): Unit = {
    curves = new AreaOp.AddOp().calculate(this.curves, rhs.curves)
    invalidateBounds()
  }

  /**
    * Subtracts the shape of the specified `Area` from the
    * shape of this `Area`.
    * The resulting shape of this `Area` will include
    * areas that were contained only in this `Area`
    * and not in the specified `Area`.
    * <pre>
    * // Example:
    * Area a1 = new Area([triangle 0,0 =&gt; 8,0 =&gt; 0,8]);
    * Area a2 = new Area([triangle 0,0 =&gt; 8,0 =&gt; 8,8]);
    * a1.subtract(a2);
    *
    * a1(before)     -         a2         =     a1(after)
    *
    * ################     ################
    * ##############         ##############     ##
    * ############             ############     ####
    * ##########                 ##########     ######
    * ########                     ########     ########
    * ######                         ######     ######
    * ####                             ####     ####
    * ##                                 ##     ##
    * </pre>
    *
    * @param rhs the `Area` to be subtracted from the
    *            current shape
    * @throws NullPointerException if `rhs` is null
    * @since 1.2
    */
  def subtract(rhs: Area): Unit = {
    curves = new AreaOp.SubOp().calculate(this.curves, rhs.curves)
    invalidateBounds()
  }

  /**
    * Sets the shape of this `Area` to the intersection of
    * its current shape and the shape of the specified `Area`.
    * The resulting shape of this `Area` will include
    * only areas that were contained in both this `Area`
    * and also in the specified `Area`.
    * <pre>
    * // Example:
    * Area a1 = new Area([triangle 0,0 =&gt; 8,0 =&gt; 0,8]);
    * Area a2 = new Area([triangle 0,0 =&gt; 8,0 =&gt; 8,8]);
    * a1.intersect(a2);
    *
    * a1(before)   intersect     a2         =     a1(after)
    *
    * ################     ################     ################
    * ##############         ##############       ############
    * ############             ############         ########
    * ##########                 ##########           ####
    * ########                     ########
    * ######                         ######
    * ####                             ####
    * ##                                 ##
    * </pre>
    *
    * @param rhs the `Area` to be intersected with this
    *            `Area`
    * @throws NullPointerException if `rhs` is null
    * @since 1.2
    */
  def intersect(rhs: Area): Unit = {
    curves = new AreaOp.IntOp().calculate(this.curves, rhs.curves)
    invalidateBounds()
  }

  /**
    * Sets the shape of this `Area` to be the combined area
    * of its current shape and the shape of the specified `Area`,
    * minus their intersection.
    * The resulting shape of this `Area` will include
    * only areas that were contained in either this `Area`
    * or in the specified `Area`, but not in both.
    * <pre>
    * // Example:
    * Area a1 = new Area([triangle 0,0 =&gt; 8,0 =&gt; 0,8]);
    * Area a2 = new Area([triangle 0,0 =&gt; 8,0 =&gt; 8,8]);
    * a1.exclusiveOr(a2);
    *
    * a1(before)    xor        a2         =     a1(after)
    *
    * ################     ################
    * ##############         ##############     ##            ##
    * ############             ############     ####        ####
    * ##########                 ##########     ######    ######
    * ########                     ########     ################
    * ######                         ######     ######    ######
    * ####                             ####     ####        ####
    * ##                                 ##     ##            ##
    * </pre>
    *
    * @param rhs the `Area` to be exclusive ORed with this
    *            `Area`.
    * @throws NullPointerException if `rhs` is null
    * @since 1.2
    */
  def exclusiveOr(rhs: Area): Unit = {
    curves = new AreaOp.XorOp().calculate(this.curves, rhs.curves)
    invalidateBounds()
  }

  /**
    * Removes all of the geometry from this `Area` and
    * restores it to an empty area.
    *
    * @since 1.2
    */
  def reset(): Unit = {
    curves = Vector.empty
    invalidateBounds()
  }

  /**
    * Tests whether this `Area` object encloses any area.
    *
    * @return `true` if this `Area` object
    *         represents an empty area; `false` otherwise.
    * @since 1.2
    */
  def isEmpty: Boolean = curves.isEmpty

  /**
    * Tests whether this `Area` consists entirely of
    * straight edged polygonal geometry.
    *
    * @return `true` if the geometry of this
    *         `Area` consists entirely of line segments;
    *         `false` otherwise.
    * @since 1.2
    */
  def isPolygonal: Boolean =
    curves.forall(_.getOrder <= 1)

  /**
    * Tests whether this `Area` is rectangular in shape.
    *
    * @return `true` if the geometry of this
    *         `Area` is rectangular in shape; `false`
    *         otherwise.
    * @since 1.2
    */
  def isRectangular: Boolean = {
    val size = curves.size
    if (size == 0) return true
    if (size > 3) return false
    val c1 = curves(1)
    val c2 = curves(2)
    if (c1.getOrder != 1 || c2.getOrder != 1) return false
    if (c1.getXTop != c1.getXBot || c2.getXTop != c2.getXBot) return false
    if (c1.getYTop != c2.getYTop || c1.getYBot != c2.getYBot) { // One might be able to prove that this is impossible...
      return false
    }
    true
  }

  /**
    * Tests whether this `Area` is comprised of a single
    * closed subpath.  This method returns `true` if the
    * path contains 0 or 1 subpaths, or `false` if the path
    * contains more than 1 subpath.  The subpaths are counted by the
    * number of `PathIterator`  segments
    * that appear in the path.
    *
    * @return `true` if the `Area` is comprised
    *         of a single basic geometry; `false` otherwise.
    * @since 1.2
    */
  def isSingular: Boolean = {
    if (curves.size < 3) return true
    curves.forall(_.getOrder != 0)
  }

  private var cachedBounds: Rectangle2D.Double = null

  private def invalidateBounds(): Unit =
    cachedBounds = null

  private def getCachedBounds: Rectangle2D = {
    if (cachedBounds != null) return cachedBounds
    val r = new Rectangle2D.Double
    if (curves.nonEmpty) {
      val it = curves.iterator
      val c = it.next()
      // First point is always an order 0 curve (moveto)
      r.setRect(c.getX0, c.getY0, 0, 0)
      while (it.hasNext) {
        it.next().enlarge(r)
      }
    }
    cachedBounds = r
    r
  }

  /**
    * Returns a high precision bounding `Rectangle2D` that
    * completely encloses this `Area`.
    * <p>
    * The Area class will attempt to return the tightest bounding
    * box possible for the Shape.  The bounding box will not be
    * padded to include the control points of curves in the outline
    * of the Shape, but should tightly fit the actual geometry of
    * the outline itself.
    *
    * @return the bounding `Rectangle2D` for the
    *         `Area`.
    * @since 1.2
    */
  override def getBounds2D: Rectangle2D = getCachedBounds.getBounds2D

//  /**
//    * Returns a bounding `Rectangle` that completely encloses
//    * this `Area`.
//    * <p>
//    * The Area class will attempt to return the tightest bounding
//    * box possible for the Shape.  The bounding box will not be
//    * padded to include the control points of curves in the outline
//    * of the Shape, but should tightly fit the actual geometry of
//    * the outline itself.  Since the returned object represents
//    * the bounding box with integers, the bounding box can only be
//    * as tight as the nearest integer coordinates that encompass
//    * the geometry of the Shape.
//    *
//    * @return the bounding `Rectangle` for the
//    *         `Area`.
//    * @since 1.2
//    */
//  override def getBounds: Rectangle = getCachedBounds.getBounds

  /**
    * Returns an exact copy of this `Area` object.
    *
    * @return Created clone object
    * @since 1.2
    */
  override def clone = new Area(this)

  /**
    * Tests whether the geometries of the two `Area` objects
    * are equal.
    * This method will return false if the argument is null.
    *
    * @param other the `Area` to be compared to this
    *              `Area`
    * @return `true` if the two geometries are equal;
    *         `false` otherwise.
    * @since 1.2
    */
  def equals(other: Area): Boolean = { // REMIND: A *much* simpler operation should be possible...
    // Should be able to do a curve-wise comparison since all Areas
    // should evaluate their curves in the same top-down order.
    if (other eq this) return true
    if (other == null) return false
    val c = new AreaOp.XorOp().calculate(this.curves, other.curves)
    c.isEmpty
  }

  /**
    * Transforms the geometry of this `Area` using the specified
    * `AffineTransform`.  The geometry is transformed in place, which
    * permanently changes the enclosed area defined by this object.
    *
    * @param t the transformation used to transform the area
    * @throws NullPointerException if `t` is null
    * @since 1.2
    */
  def transform(t: AffineTransform): Unit = {
    if (t == null) throw new NullPointerException("transform must not be null")
    // REMIND: A simpler operation can be performed for some types
    // of transform.
    curves = Area.pathToCurves(getPathIterator(t))
    invalidateBounds()
  }

  /**
    * Creates a new `Area` object that contains the same
    * geometry as this `Area` transformed by the specified
    * `AffineTransform`.  This `Area` object
    * is unchanged.
    *
    * @param t the specified `AffineTransform` used to transform
    *          the new `Area`
    * @throws NullPointerException if `t` is null
    * @return a new `Area` object representing the transformed
    *         geometry.
    * @since 1.2
    */
  def createTransformedArea(t: AffineTransform): Area = {
    val a = new Area(this)
    a.transform(t)
    a
  }

  /**
    * ``
    *
    * @since 1.2
    */
  override def contains(x: Double, y: Double): Boolean = {
    if (!getCachedBounds.contains(x, y)) return false
    val crossings = curves.foldLeft(0)((acc, c) => acc + c.crossingsFor(x, y))
    (crossings & 1) == 1
  }

  override def contains(p: Point2D): Boolean = contains(p.getX, p.getY)

  override def contains(x: Double, y: Double, w: Double, h: Double): Boolean = {
    if (w < 0 || h < 0) return false
    if (!getCachedBounds.contains(x, y, w, h)) return false
    val c = Crossings.findCrossings(curves, x, y, x + w, y + h)
    c != null && c.covers(y, y + h)
  }

  override def contains(r: Rectangle2D): Boolean = contains(r.getX, r.getY, r.getWidth, r.getHeight)

  override def intersects(x: Double, y: Double, w: Double, h: Double): Boolean = {
    if (w < 0 || h < 0) return false
    if (!getCachedBounds.intersects(x, y, w, h)) return false
    val c = Crossings.findCrossings(curves, x, y, x + w, y + h)
    c == null || !c.isEmpty
  }

  override def intersects(r: Rectangle2D): Boolean = intersects(r.getX, r.getY, r.getWidth, r.getHeight)

  /**
    * Creates a `PathIterator` for the outline of this
    * `Area` object.  This `Area` object is unchanged.
    *
    * @param at an optional `AffineTransform` to be applied to
    *           the coordinates as they are returned in the iteration, or
    *           `null` if untransformed coordinates are desired
    * @return the `PathIterator` object that returns the
    *         geometry of the outline of this `Area`, one
    *         segment at a time.
    * @since 1.2
    */
  override def getPathIterator(at: AffineTransform) =
    new AreaIterator(curves, at)

  /**
    * Creates a `PathIterator` for the flattened outline of
    * this `Area` object.  Only uncurved path segments
    * represented by the SEG_MOVETO, SEG_LINETO, and SEG_CLOSE point
    * types are returned by the iterator.  This `Area`
    * object is unchanged.
    *
    * @param at       an optional `AffineTransform` to be
    *                 applied to the coordinates as they are returned in the
    *                 iteration, or `null` if untransformed coordinates
    *                 are desired
    * @param flatness the maximum amount that the control points
    *                 for a given curve can vary from colinear before a subdivided
    *                 curve is replaced by a straight line connecting the end points
    * @return the `PathIterator` object that returns the
    *         geometry of the outline of this `Area`, one segment
    *         at a time.
    * @since 1.2
    */
  override def getPathIterator(at: AffineTransform, flatness: Double) = new FlatteningPathIterator(getPathIterator(at), flatness)
}

class AreaIterator(private var curves: CSeq[Curve], private var transform: AffineTransform)
  extends PathIterator {

//  private var index     = 0

  private var prevCurve: Curve = null
  private var thisCurve: Curve = null
  private val peer = curves.iterator

  if (curves.nonEmpty) thisCurve = peer.next() // curves.head

  override def getWindingRule: Int = { // REMIND: Which is better, EVEN_ODD or NON_ZERO?
    //         The paths calculated could be classified either way.
    //return WIND_EVEN_ODD;
    WIND_NON_ZERO
  }

  override def isDone: Boolean = prevCurve == null && thisCurve == null

  override def next(): Unit = {
    if (prevCurve != null) prevCurve = null
    else {
      prevCurve = thisCurve
//      index += 1
      if (peer.hasNext) {
        thisCurve = peer.next()
        if (thisCurve.getOrder != 0 && prevCurve.getX1 == thisCurve.getX0 && prevCurve.getY1 == thisCurve.getY0) prevCurve = null
      }
      else thisCurve = null
    }
  }

  override def currentSegment(coords: Array[Float]): Int = {
    val dCoords = new Array[Double](6)
    val segType = currentSegment(dCoords)
    val numPoints =
      if      (segType == SEG_CLOSE   ) 0
      else if (segType == SEG_QUADTO  ) 2
      else if (segType == SEG_CUBICTO ) 3
      else 1

    for (i <- 0 until numPoints * 2) {
      coords(i) = dCoords(i).toFloat
    }
    segType
  }

  override def currentSegment(coords: Array[Double]): Int = {
    var segType   = 0
    var numPoints = 0
    if (prevCurve != null) { // Need to finish off junction between curves
      if (thisCurve == null || thisCurve.getOrder == 0) return SEG_CLOSE
      coords(0) = thisCurve.getX0
      coords(1) = thisCurve.getY0
      segType = SEG_LINETO
      numPoints = 1
    }
    else if (thisCurve == null) throw new NoSuchElementException("area iterator out of bounds")
    else {
      segType = thisCurve.getSegment(coords)
      numPoints = thisCurve.getOrder
      if (numPoints == 0) numPoints = 1
    }
    if (transform != null) transform.transform(coords, 0, coords, 0, numPoints)
    segType
  }
}
