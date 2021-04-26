package de.sciss.infibrillae.geom

// This is an adapted Scala translation of the Rectangle2D Java class of OpenJDK
// as released under GNU GPL 2 -- see original file header below.
// So it can be used in Scala.js.

/*
 * Copyright (c) 1997, 2006, Oracle and/or its affiliates. All rights reserved.
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

import de.sciss.infibrillae.geom.impl.RectIterator

import java.io.Serializable
import scala.{Double => SDouble, Float => SFloat}

/**
  * The `Rectangle2D` class describes a rectangle
  * defined by a location `(x,y)` and dimension
  * `(w x h)`.
  * <p>
  * This class is only the abstract superclass for all objects that
  * store a 2D rectangle.
  * The actual storage representation of the coordinates is left to
  * the subclass.
  *
  * @author Jim Graham
  * @since 1.2
  */
object Rectangle2D {
  /**
    * The bitmask that indicates that a point lies to the left of
    * this `Rectangle2D`.
    *
    * @since 1.2
    */
  final val OUT_LEFT = 1
  /**
    * The bitmask that indicates that a point lies above
    * this `Rectangle2D`.
    *
    * @since 1.2
    */
  final val OUT_TOP = 2
  /**
    * The bitmask that indicates that a point lies to the right of
    * this `Rectangle2D`.
    *
    * @since 1.2
    */
  final val OUT_RIGHT = 4
  /**
    * The bitmask that indicates that a point lies below
    * this `Rectangle2D`.
    *
    * @since 1.2
    */
  final val OUT_BOTTOM = 8

  /**
    * Constructs and initializes a `Rectangle2D`
    * from the specified `float` coordinates.
    *
    * @param x the X coordinate of the upper-left corner
    *          of the newly constructed `Rectangle2D`
    * @param y the Y coordinate of the upper-left corner
    *          of the newly constructed `Rectangle2D`
    * @param width the width of the newly constructed
    *          `Rectangle2D`
    * @param height the height of the newly constructed
    *          `Rectangle2D`
    * @since 1.2
    */
  class Float(private var x: SFloat, private var y: SFloat, private var width: SFloat, private var height: SFloat)
    extends Rectangle2D with Serializable {

    def this() = this(0f, 0f, 0f, 0f)

    override def toString: String = s"Rectangle2D.Float($x, $y, $width, $height)"

    override def getX     : SDouble = x
    override def getY     : SDouble = y
    override def getWidth : SDouble = width
    override def getHeight: SDouble = height

    override def isEmpty: Boolean = (width <= 0.0f) || (height <= 0.0f)

    /**
      * Sets the location and size of this `Rectangle2D`
      * to the specified `float` values.
      *
      * @param x the X coordinate of the upper-left corner
      *          of this `Rectangle2D`
      * @param y the Y coordinate of the upper-left corner
      *          of this `Rectangle2D`
      * @param w the width of this `Rectangle2D`
      * @param h the height of this `Rectangle2D`
      * @since 1.2
      */
    def setRect(x: SFloat, y: SFloat, w: SFloat, h: SFloat): Unit = {
      this.x      = x
      this.y      = y
      this.width  = w
      this.height = h
    }

    override def setRect(x: SDouble, y: SDouble, w: SDouble, h: SDouble): Unit = {
      this.x      = x.toFloat
      this.y      = y.toFloat
      this.width  = w.toFloat
      this.height = h.toFloat
    }

    override def setRect(r: Rectangle2D): Unit = {
      this.x      = r.getX      .toFloat
      this.y      = r.getY      .toFloat
      this.width  = r.getWidth  .toFloat
      this.height = r.getHeight .toFloat
    }

    override def outcode(x: SDouble, y: SDouble): Int = {
      /*
                  * Note on casts to double below.  If the arithmetic of
                  * x+w or y+h is done in float, then some bits may be
                  * lost if the binary exponents of x/y and w/h are not
                  * similar.  By converting to double before the addition
                  * we force the addition to be carried out in double to
                  * avoid rounding error in the comparison.
                  *
                  * See bug 4320890 for problems that this inaccuracy causes.
                  */ var out = 0
      if (this.width <= 0) out |= OUT_LEFT | OUT_RIGHT
      else if (x < this.x) out |= OUT_LEFT
      else if (x > this.x + this.width) out |= OUT_RIGHT
      if (this.height <= 0) out |= OUT_TOP | OUT_BOTTOM
      else if (y < this.y) out |= OUT_TOP
      else if (y > this.y + this.height) out |= OUT_BOTTOM
      out
    }

    override def getBounds2D: Rectangle2D = new Rectangle2D.Float(x, y, width, height)

    override def createIntersection(r: Rectangle2D): Rectangle2D = {
      val dest = if (r.isInstanceOf[Rectangle2D.Float]) new Rectangle2D.Float else new Rectangle2D.Double
      Rectangle2D.intersect(this, r, dest)
      dest
    }

    override def createUnion(r: Rectangle2D): Rectangle2D = {
      val dest = if (r.isInstanceOf[Rectangle2D.Float]) new Rectangle2D.Float else new Rectangle2D.Double
      Rectangle2D.union(this, r, dest)
      dest
    }
  }

  /**
    * Constructs a new `Rectangle2D`, initialized to
    * location (0,&nbsp;0) and size (0,&nbsp;0).
    *
    * @since 1.2
    */
  class Double() extends Rectangle2D with Serializable {
    private var x       = .0
    private var y       = .0
    private var width   = .0
    private var height  = .0

    /**
      * Constructs and initializes a `Rectangle2D`
      * from the specified `double` coordinates.
      *
      * @param x the X coordinate of the upper-left corner
      *          of the newly constructed `Rectangle2D`
      * @param y the Y coordinate of the upper-left corner
      *          of the newly constructed `Rectangle2D`
      * @param w the width of the newly constructed
      *          `Rectangle2D`
      * @param h the height of the newly constructed
      *          `Rectangle2D`
      * @since 1.2
      */
    def this(x: SDouble, y: SDouble, w: SDouble, h: SDouble) {
      this()
      setRect(x, y, w, h)
    }

    override def toString: String = s"Rectangle2D.Double($x, $y, $width, $height)"

    override def getX     : SDouble = x
    override def getY     : SDouble = y
    override def getWidth : SDouble = width
    override def getHeight: SDouble = height

    override def isEmpty: Boolean = (width <= 0.0) || (height <= 0.0)

    override def setRect(x: SDouble, y: SDouble, w: SDouble, h: SDouble): Unit = {
      this.x      = x
      this.y      = y
      this.width  = w
      this.height = h
    }

    override def setRect(r: Rectangle2D): Unit = {
      this.x      = r.getX
      this.y      = r.getY
      this.width  = r.getWidth
      this.height = r.getHeight
    }

    override def outcode(x: SDouble, y: SDouble): Int = {
      var out = 0
      if (this.width <= 0) out |= OUT_LEFT | OUT_RIGHT
      else if (x < this.x) out |= OUT_LEFT
      else if (x > this.x + this.width) out |= OUT_RIGHT
      if (this.height <= 0) out |= OUT_TOP | OUT_BOTTOM
      else if (y < this.y) out |= OUT_TOP
      else if (y > this.y + this.height) out |= OUT_BOTTOM
      out
    }

    override def getBounds2D = new Rectangle2D.Double(x, y, width, height)

    override def createIntersection(r: Rectangle2D): Rectangle2D = {
      val dest = new Rectangle2D.Double
      Rectangle2D.intersect(this, r, dest)
      dest
    }

    override def createUnion(r: Rectangle2D): Rectangle2D = {
      val dest = new Rectangle2D.Double
      Rectangle2D.union(this, r, dest)
      dest
    }
  }

  /**
    * Intersects the pair of specified source `Rectangle2D`
    * objects and puts the result into the specified destination
    * `Rectangle2D` object.  One of the source rectangles
    * can also be the destination to avoid creating a third Rectangle2D
    * object, but in this case the original points of this source
    * rectangle will be overwritten by this method.
    *
    * @param src1 the first of a pair of `Rectangle2D`
    *             objects to be intersected with each other
    * @param src2 the second of a pair of `Rectangle2D`
    *             objects to be intersected with each other
    * @param dest the `Rectangle2D` that holds the
    *             results of the intersection of `src1` and
    *             `src2`
    * @since 1.2
    */
  def intersect(src1: Rectangle2D, src2: Rectangle2D, dest: Rectangle2D): Unit = {
    val x1 = Math.max(src1.getMinX, src2.getMinX)
    val y1 = Math.max(src1.getMinY, src2.getMinY)
    val x2 = Math.min(src1.getMaxX, src2.getMaxX)
    val y2 = Math.min(src1.getMaxY, src2.getMaxY)
    dest.setFrame(x1, y1, x2 - x1, y2 - y1)
  }

  /**
    * Unions the pair of source `Rectangle2D` objects
    * and puts the result into the specified destination
    * `Rectangle2D` object.  One of the source rectangles
    * can also be the destination to avoid creating a third Rectangle2D
    * object, but in this case the original points of this source
    * rectangle will be overwritten by this method.
    *
    * @param src1 the first of a pair of `Rectangle2D`
    *             objects to be combined with each other
    * @param src2 the second of a pair of `Rectangle2D`
    *             objects to be combined with each other
    * @param dest the `Rectangle2D` that holds the
    *             results of the union of `src1` and
    *             `src2`
    * @since 1.2
    */
  def union(src1: Rectangle2D, src2: Rectangle2D, dest: Rectangle2D): Unit = {
    val x1 = Math.min(src1.getMinX, src2.getMinX)
    val y1 = Math.min(src1.getMinY, src2.getMinY)
    val x2 = Math.max(src1.getMaxX, src2.getMaxX)
    val y2 = Math.max(src1.getMaxY, src2.getMaxY)
    dest.setFrameFromDiagonal(x1, y1, x2, y2)
  }
}

abstract class Rectangle2D protected()

/**
  * This is an abstract class that cannot be instantiated directly.
  * Type-specific implementation subclasses are available for
  * instantiation and provide a number of formats for storing
  * the information necessary to satisfy the various accessor
  * methods below.
  *
  * @see java.awt.geom.Rectangle2D.SFloat
  * @see java.awt.geom.Rectangle2D.SDouble
  * @see java.awt.Rectangle
  * @since 1.2
  */
  extends RectangularShape {
  /**
    * Sets the location and size of this `Rectangle2D`
    * to the specified `double` values.
    *
    * @param x the X coordinate of the upper-left corner
    *          of this `Rectangle2D`
    * @param y the Y coordinate of the upper-left corner
    *          of this `Rectangle2D`
    * @param w the width of this `Rectangle2D`
    * @param h the height of this `Rectangle2D`
    * @since 1.2
    */
  def setRect(x: SDouble, y: SDouble, w: SDouble, h: SDouble): Unit

  /**
    * Sets this `Rectangle2D` to be the same as the specified
    * `Rectangle2D`.
    *
    * @param r the specified `Rectangle2D`
    * @since 1.2
    */
  def setRect(r: Rectangle2D): Unit = {
    setRect(r.getX, r.getY, r.getWidth, r.getHeight)
  }

  /**
    * Tests if the specified line segment intersects the interior of this
    * `Rectangle2D`.
    *
    * @param x1 the X coordinate of the start point of the specified
    *           line segment
    * @param y1 the Y coordinate of the start point of the specified
    *           line segment
    * @param x2 the X coordinate of the end point of the specified
    *           line segment
    * @param y2 the Y coordinate of the end point of the specified
    *           line segment
    * @return `true` if the specified line segment intersects
    *         the interior of this `Rectangle2D`; `false`
    *         otherwise.
    * @since 1.2
    */
  def intersectsLine(x1: SDouble, y1: SDouble, x2: SDouble, y2: SDouble): Boolean = {
    var _x1 = x1
    var _y1 = y1
    var out1 = 0
    val out2 = outcode(x2, y2)
    if (out2 == 0) return true
    while ({
      out1 = outcode(_x1, _y1)
      out1 != 0
    }) {
      if ((out1 & out2) != 0) return false
      if ((out1 & (Rectangle2D.OUT_LEFT | Rectangle2D.OUT_RIGHT)) != 0) {
        var x = getX
        if ((out1 & Rectangle2D.OUT_RIGHT) != 0) x += getWidth
        _y1 = _y1 + (x - _x1) * (y2 - _y1) / (x2 - _x1)
        _x1 = x
      }
      else {
        var y = getY
        if ((out1 & Rectangle2D.OUT_BOTTOM) != 0) y += getHeight
        _x1 = _x1 + (y - _y1) * (x2 - _x1) / (y2 - _y1)
        _y1 = y
      }
    }
    true
  }

//  /**
//    * Tests if the specified line segment intersects the interior of this
//    * `Rectangle2D`.
//    *
//    * @param l the specified `Line2D` to test for intersection
//    *          with the interior of this `Rectangle2D`
//    * @return `true` if the specified `Line2D`
//    *         intersects the interior of this `Rectangle2D`;
//    *         `false` otherwise.
//    * @since 1.2
//    */
//  def intersectsLine(l: Line2D): Boolean = intersectsLine(l.getX1, l.getY1, l.getX2, l.getY2)

  /**
    * Determines where the specified coordinates lie with respect
    * to this `Rectangle2D`.
    * This method computes a binary OR of the appropriate mask values
    * indicating, for each side of this `Rectangle2D`,
    * whether or not the specified coordinates are on the same side
    * of the edge as the rest of this `Rectangle2D`.
    *
    * @param x the specified X coordinate
    * @param y the specified Y coordinate
    * @return the logical OR of all appropriate out codes.
    * @see #OUT_LEFT
    * @see #OUT_TOP
    * @see #OUT_RIGHT
    * @see #OUT_BOTTOM
    * @since 1.2
    */
  def outcode(x: SDouble, y: SDouble): Int

  /**
    * Determines where the specified `Point2D` lies with
    * respect to this `Rectangle2D`.
    * This method computes a binary OR of the appropriate mask values
    * indicating, for each side of this `Rectangle2D`,
    * whether or not the specified `Point2D` is on the same
    * side of the edge as the rest of this `Rectangle2D`.
    *
    * @param p the specified `Point2D`
    * @return the logical OR of all appropriate out codes.
    * @see #OUT_LEFT
    * @see #OUT_TOP
    * @see #OUT_RIGHT
    * @see #OUT_BOTTOM
    * @since 1.2
    */
  def outcode(p: Point2D): Int = outcode(p.getX, p.getY)

  /**
    * Sets the location and size of the outer bounds of this
    * `Rectangle2D` to the specified rectangular values.
    *
    * @param x the X coordinate of the upper-left corner
    *          of this `Rectangle2D`
    * @param y the Y coordinate of the upper-left corner
    *          of this `Rectangle2D`
    * @param w the width of this `Rectangle2D`
    * @param h the height of this `Rectangle2D`
    * @since 1.2
    */
  override def setFrame(x: SDouble, y: SDouble, w: SDouble, h: SDouble): Unit = {
    setRect(x, y, w, h)
  }

  /**
    * ``
    *
    * @since 1.2
    */
  override def getBounds2D: Rectangle2D = clone.asInstanceOf[Rectangle2D]

  override def contains(x: SDouble, y: SDouble): Boolean = {
    val x0 = getX
    val y0 = getY
    x >= x0 && y >= y0 && x < x0 + getWidth && y < y0 + getHeight
  }

  override def intersects(x: SDouble, y: SDouble, w: SDouble, h: SDouble): Boolean = {
    if (isEmpty || w <= 0 || h <= 0) return false
    val x0 = getX
    val y0 = getY
    x + w > x0 && y + h > y0 && x < x0 + getWidth && y < y0 + getHeight
  }

  override def contains(x: SDouble, y: SDouble, w: SDouble, h: SDouble): Boolean = {
    if (isEmpty || w <= 0 || h <= 0) return false
    val x0 = getX
    val y0 = getY
    x >= x0 && y >= y0 && (x + w) <= x0 + getWidth && (y + h) <= y0 + getHeight
  }

  /**
    * Returns a new `Rectangle2D` object representing the
    * intersection of this `Rectangle2D` with the specified
    * `Rectangle2D`.
    *
    * @param r the `Rectangle2D` to be intersected with
    *          this `Rectangle2D`
    * @return the largest `Rectangle2D` contained in both
    *         the specified `Rectangle2D` and in this
    *         `Rectangle2D`.
    * @since 1.2
    */
  def createIntersection(r: Rectangle2D): Rectangle2D

  /**
    * Returns a new `Rectangle2D` object representing the
    * union of this `Rectangle2D` with the specified
    * `Rectangle2D`.
    *
    * @param r the `Rectangle2D` to be combined with
    *          this `Rectangle2D`
    * @return the smallest `Rectangle2D` containing both
    *         the specified `Rectangle2D` and this
    *         `Rectangle2D`.
    * @since 1.2
    */
  def createUnion(r: Rectangle2D): Rectangle2D

  /**
    * Adds a point, specified by the double precision arguments
    * `newx` and `newy`, to this
    * `Rectangle2D`.  The resulting `Rectangle2D`
    * is the smallest `Rectangle2D` that
    * contains both the original `Rectangle2D` and the
    * specified point.
    * <p>
    * After adding a point, a call to `contains` with the
    * added point as an argument does not necessarily return
    * `true`. The `contains` method does not
    * return `true` for points on the right or bottom
    * edges of a rectangle. Therefore, if the added point falls on
    * the left or bottom edge of the enlarged rectangle,
    * `contains` returns `false` for that point.
    *
    * @param newx the X coordinate of the new point
    * @param newy the Y coordinate of the new point
    * @since 1.2
    */
  def add(newx: SDouble, newy: SDouble): Unit = {
    val x1 = Math.min(getMinX, newx)
    val x2 = Math.max(getMaxX, newx)
    val y1 = Math.min(getMinY, newy)
    val y2 = Math.max(getMaxY, newy)
    setRect(x1, y1, x2 - x1, y2 - y1)
  }

  /**
    * Adds the `Point2D` object `pt` to this
    * `Rectangle2D`.
    * The resulting `Rectangle2D` is the smallest
    * `Rectangle2D` that contains both the original
    * `Rectangle2D` and the specified `Point2D`.
    * <p>
    * After adding a point, a call to `contains` with the
    * added point as an argument does not necessarily return
    * `true`. The `contains`
    * method does not return `true` for points on the right
    * or bottom edges of a rectangle. Therefore, if the added point falls
    * on the left or bottom edge of the enlarged rectangle,
    * `contains` returns `false` for that point.
    *
    * @param pt the new `Point2D` to add to this
    *           `Rectangle2D`.
    * @since 1.2
    */
  def add(pt: Point2D): Unit = {
    add(pt.getX, pt.getY)
  }

  /**
    * Adds a `Rectangle2D` object to this
    * `Rectangle2D`.  The resulting `Rectangle2D`
    * is the union of the two `Rectangle2D` objects.
    *
    * @param r the `Rectangle2D` to add to this
    *          `Rectangle2D`.
    * @since 1.2
    */
  def add(r: Rectangle2D): Unit = {
    val x1 = Math.min(getMinX, r.getMinX)
    val x2 = Math.max(getMaxX, r.getMaxX)
    val y1 = Math.min(getMinY, r.getMinY)
    val y2 = Math.max(getMaxY, r.getMaxY)
    setRect(x1, y1, x2 - x1, y2 - y1)
  }

  /**
    * Returns an iteration object that defines the boundary of this
    * `Rectangle2D`.
    * The iterator for this class is multi-threaded safe, which means
    * that this `Rectangle2D` class guarantees that
    * modifications to the geometry of this `Rectangle2D`
    * object do not affect any iterations of that geometry that
    * are already in process.
    *
    * @param at an optional `AffineTransform` to be applied to
    *           the coordinates as they are returned in the iteration, or
    *           `null` if untransformed coordinates are desired
    * @return the `PathIterator` object that returns the
    *         geometry of the outline of this
    *         `Rectangle2D`, one segment at a time.
    * @since 1.2
    */
  override def getPathIterator(at: AffineTransform) = new RectIterator(this, at)

  /**
    * Returns an iteration object that defines the boundary of the
    * flattened `Rectangle2D`.  Since rectangles are already
    * flat, the `flatness` parameter is ignored.
    * The iterator for this class is multi-threaded safe, which means
    * that this `Rectangle2D` class guarantees that
    * modifications to the geometry of this `Rectangle2D`
    * object do not affect any iterations of that geometry that
    * are already in process.
    *
    * @param at       an optional `AffineTransform` to be applied to
    *                 the coordinates as they are returned in the iteration, or
    *                 `null` if untransformed coordinates are desired
    * @param flatness the maximum distance that the line segments used to
    *                 approximate the curved segments are allowed to deviate from any
    *                 point on the original curve.  Since rectangles are already flat,
    *                 the `flatness` parameter is ignored.
    * @return the `PathIterator` object that returns the
    *         geometry of the outline of this
    *         `Rectangle2D`, one segment at a time.
    * @since 1.2
    */
  override def getPathIterator(at: AffineTransform, flatness: SDouble): PathIterator =
    new RectIterator(this, at)

  /**
    * Returns the hashcode for this `Rectangle2D`.
    *
    * @return the hashcode for this `Rectangle2D`.
    * @since 1.2
    */
  override def hashCode: Int = {
    var bits = java.lang.Double.doubleToLongBits(getX)
    bits    += java.lang.Double.doubleToLongBits(getY) * 37
    bits    += java.lang.Double.doubleToLongBits(getWidth) * 43
    bits    += java.lang.Double.doubleToLongBits(getHeight) * 47
    bits.toInt ^ (bits >> 32).toInt
  }

  /**
    * Determines whether or not the specified `Object` is
    * equal to this `Rectangle2D`.  The specified
    * `Object` is equal to this `Rectangle2D`
    * if it is an instance of `Rectangle2D` and if its
    * location and size are the same as this `Rectangle2D`.
    *
    * @param obj an `Object` to be compared with this
    *            `Rectangle2D`.
    * @return `true` if `obj` is an instance
    *         of `Rectangle2D` and has
    *         the same values; `false` otherwise.
    * @since 1.2
    */
  override def equals(obj: Any): Boolean = {
    if (obj == this) return true
    obj match {
      case r2d: Rectangle2D =>
        (getX == r2d.getX) && (getY == r2d.getY) && (getWidth == r2d.getWidth) && (getHeight == r2d.getHeight)
      case _ =>
        false
    }
  }
}
