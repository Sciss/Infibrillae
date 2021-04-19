package de.sciss.infibrillae.geom

// This is an adapted Scala translation of the RectangularShape Java class of OpenJDK
// as released under GNU GPL 2 -- see original file header below.
// So it can be used in Scala.js.

/*
 * Copyright (c) 1997, 2011, Oracle and/or its affiliates. All rights reserved.
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

import java.beans.Transient

/**
  * `RectangularShape` is the base class for a number of
  * `Shape` objects whose geometry is defined by a rectangular frame.
  * This class does not directly specify any specific geometry by
  * itself, but merely provides manipulation methods inherited by
  * a whole category of `Shape` objects.
  * The manipulation methods provided by this class can be used to
  * query and modify the rectangular frame, which provides a reference
  * for the subclasses to define their geometry.
  *
  * @author Jim Graham
  * @since 1.2
  */
abstract class RectangularShape protected()

/**
  * This is an abstract class that cannot be instantiated directly.
  *
  * @see Arc2D
  * @see Ellipse2D
  * @see Rectangle2D
  * @see RoundRectangle2D
  * @since 1.2
  */
  extends Shape with Cloneable {
  /**
    * Returns the X coordinate of the upper-left corner of
    * the framing rectangle in `double` precision.
    *
    * @return the X coordinate of the upper-left corner of
    *         the framing rectangle.
    * @since 1.2
    */
  def getX: Double

  /**
    * Returns the Y coordinate of the upper-left corner of
    * the framing rectangle in `double` precision.
    *
    * @return the Y coordinate of the upper-left corner of
    *         the framing rectangle.
    * @since 1.2
    */
  def getY: Double

  /**
    * Returns the width of the framing rectangle in
    * `double` precision.
    *
    * @return the width of the framing rectangle.
    * @since 1.2
    */
  def getWidth: Double

  /**
    * Returns the height of the framing rectangle
    * in `double` precision.
    *
    * @return the height of the framing rectangle.
    * @since 1.2
    */
  def getHeight: Double

  /**
    * Returns the smallest X coordinate of the framing
    * rectangle of the `Shape` in `double`
    * precision.
    *
    * @return the smallest X coordinate of the framing
    *         rectangle of the `Shape`.
    * @since 1.2
    */
  def getMinX: Double = getX

  /**
    * Returns the smallest Y coordinate of the framing
    * rectangle of the `Shape` in `double`
    * precision.
    *
    * @return the smallest Y coordinate of the framing
    *         rectangle of the `Shape`.
    * @since 1.2
    */
  def getMinY: Double = getY

  /**
    * Returns the largest X coordinate of the framing
    * rectangle of the `Shape` in `double`
    * precision.
    *
    * @return the largest X coordinate of the framing
    *         rectangle of the `Shape`.
    * @since 1.2
    */
  def getMaxX: Double = getX + getWidth

  /**
    * Returns the largest Y coordinate of the framing
    * rectangle of the `Shape` in `double`
    * precision.
    *
    * @return the largest Y coordinate of the framing
    *         rectangle of the `Shape`.
    * @since 1.2
    */
  def getMaxY: Double = getY + getHeight

  /**
    * Returns the X coordinate of the center of the framing
    * rectangle of the `Shape` in `double`
    * precision.
    *
    * @return the X coordinate of the center of the framing rectangle
    *         of the `Shape`.
    * @since 1.2
    */
  def getCenterX: Double = getX + getWidth / 2.0

  /**
    * Returns the Y coordinate of the center of the framing
    * rectangle of the `Shape` in `double`
    * precision.
    *
    * @return the Y coordinate of the center of the framing rectangle
    *         of the `Shape`.
    * @since 1.2
    */
  def getCenterY: Double = getY + getHeight / 2.0

  /**
    * Returns the framing `Rectangle2D`
    * that defines the overall shape of this object.
    *
    * @return a `Rectangle2D`, specified in
    *         `double` coordinates.
    * @see #setFrame(double, double, double, double)
    * @see #setFrame(Point2D, Dimension2D)
    * @see #setFrame(Rectangle2D)
    * @since 1.2
    */
  @Transient def getFrame = new Rectangle2D.Double(getX, getY, getWidth, getHeight)

  /**
    * Determines whether the `RectangularShape` is empty.
    * When the `RectangularShape` is empty, it encloses no
    * area.
    *
    * @return `true` if the `RectangularShape` is empty;
    *         `false` otherwise.
    * @since 1.2
    */
  def isEmpty: Boolean

  /**
    * Sets the location and size of the framing rectangle of this
    * `Shape` to the specified rectangular values.
    *
    * @param x the X coordinate of the upper-left corner of the
    *          specified rectangular shape
    * @param y the Y coordinate of the upper-left corner of the
    *          specified rectangular shape
    * @param w the width of the specified rectangular shape
    * @param h the height of the specified rectangular shape
    * @see #getFrame
    * @since 1.2
    */
  def setFrame(x: Double, y: Double, w: Double, h: Double): Unit

//  /**
//    * Sets the location and size of the framing rectangle of this
//    * `Shape` to the specified `Point2D` and
//    * `Dimension2D`, respectively.  The framing rectangle is used
//    * by the subclasses of `RectangularShape` to define
//    * their geometry.
//    *
//    * @param loc  the specified `Point2D`
//    * @param size the specified `Dimension2D`
//    * @see #getFrame
//    * @since 1.2
//    */
//  def setFrame(loc: Point2D, size: Dimension2D): Unit = {
//    setFrame(loc.getX, loc.getY, size.getWidth, size.getHeight)
//  }

  /**
    * Sets the framing rectangle of this `Shape` to
    * be the specified `Rectangle2D`.  The framing rectangle is
    * used by the subclasses of `RectangularShape` to define
    * their geometry.
    *
    * @param r the specified `Rectangle2D`
    * @see #getFrame
    * @since 1.2
    */
  def setFrame(r: Rectangle2D): Unit = {
    setFrame(r.getX, r.getY, r.getWidth, r.getHeight)
  }

  /**
    * Sets the diagonal of the framing rectangle of this `Shape`
    * based on the two specified coordinates.  The framing rectangle is
    * used by the subclasses of `RectangularShape` to define
    * their geometry.
    *
    * @param x1 the X coordinate of the start point of the specified diagonal
    * @param y1 the Y coordinate of the start point of the specified diagonal
    * @param x2 the X coordinate of the end point of the specified diagonal
    * @param y2 the Y coordinate of the end point of the specified diagonal
    * @since 1.2
    */
  def setFrameFromDiagonal(x1: Double, y1: Double, x2: Double, y2: Double): Unit = {
    var _x1 = x1
    var _y1 = y1
    var _x2 = x2
    var _y2 = y2
    if (_x2 < _x1) {
      val t = _x1
      _x1 = _x2
      _x2 = t
    }
    if (_y2 < _y1) {
      val t = _y1
      _y1 = _y2
      _y2 = t
    }
    setFrame(_x1, _y1, _x2 - _x1, _y2 - _y1)
  }

  /**
    * Sets the diagonal of the framing rectangle of this `Shape`
    * based on two specified `Point2D` objects.  The framing
    * rectangle is used by the subclasses of `RectangularShape`
    * to define their geometry.
    *
    * @param p1 the start `Point2D` of the specified diagonal
    * @param p2 the end `Point2D` of the specified diagonal
    * @since 1.2
    */
  def setFrameFromDiagonal(p1: Point2D, p2: Point2D): Unit = {
    setFrameFromDiagonal(p1.getX, p1.getY, p2.getX, p2.getY)
  }

  /**
    * Sets the framing rectangle of this `Shape`
    * based on the specified center point coordinates and corner point
    * coordinates.  The framing rectangle is used by the subclasses of
    * `RectangularShape` to define their geometry.
    *
    * @param centerX the X coordinate of the specified center point
    * @param centerY the Y coordinate of the specified center point
    * @param cornerX the X coordinate of the specified corner point
    * @param cornerY the Y coordinate of the specified corner point
    * @since 1.2
    */
  def setFrameFromCenter(centerX: Double, centerY: Double, cornerX: Double, cornerY: Double): Unit = {
    val halfW = Math.abs(cornerX - centerX)
    val halfH = Math.abs(cornerY - centerY)
    setFrame(centerX - halfW, centerY - halfH, halfW * 2.0, halfH * 2.0)
  }

  /**
    * Sets the framing rectangle of this `Shape` based on a
    * specified center `Point2D` and corner
    * `Point2D`.  The framing rectangle is used by the subclasses
    * of `RectangularShape` to define their geometry.
    *
    * @param center the specified center `Point2D`
    * @param corner the specified corner `Point2D`
    * @since 1.2
    */
  def setFrameFromCenter(center: Point2D, corner: Point2D): Unit =
    setFrameFromCenter(center.getX, center.getY, corner.getX, corner.getY)

  override def contains   (p: Point2D     ): Boolean = contains   (p.getX, p.getY)
  override def intersects (r: Rectangle2D ): Boolean = intersects (r.getX, r.getY, r.getWidth, r.getHeight)
  override def contains   (r: Rectangle2D ): Boolean = contains   (r.getX, r.getY, r.getWidth, r.getHeight)

//  override def getBounds: Rectangle = {
//    val width = getWidth
//    val height = getHeight
//    if (width < 0 || height < 0) return new Rectangle
//    val x = getX
//    val y = getY
//    val x1 = Math.floor(x)
//    val y1 = Math.floor(y)
//    val x2 = Math.ceil(x + width)
//    val y2 = Math.ceil(y + height)
//    new Rectangle(x1.toInt, y1.toInt, (x2 - x1).toInt, (y2 - y1).toInt)
//  }

  /**
    * Returns an iterator object that iterates along the
    * `Shape` object's boundary and provides access to a
    * flattened view of the outline of the `Shape`
    * object's geometry.
    * <p>
    * Only SEG_MOVETO, SEG_LINETO, and SEG_CLOSE point types will
    * be returned by the iterator.
    * <p>
    * The amount of subdivision of the curved segments is controlled
    * by the `flatness` parameter, which specifies the
    * maximum distance that any point on the unflattened transformed
    * curve can deviate from the returned flattened path segments.
    * An optional `AffineTransform` can
    * be specified so that the coordinates returned in the iteration are
    * transformed accordingly.
    *
    * @param at       an optional `AffineTransform` to be applied to the
    *                 coordinates as they are returned in the iteration,
    *                 or `null` if untransformed coordinates are desired.
    * @param flatness the maximum distance that the line segments used to
    *                 approximate the curved segments are allowed to deviate
    *                 from any point on the original curve
    * @return a `PathIterator` object that provides access to
    *         the `Shape` object's flattened geometry.
    * @since 1.2
    */
  override def getPathIterator(at: AffineTransform, flatness: Double): PathIterator =
    new FlatteningPathIterator(getPathIterator(at), flatness)

  /**
    * Creates a new object of the same class and with the same
    * contents as this object.
    *
    * @return a clone of this instance.
    * @throws OutOfMemoryError            if there is not enough memory.
    * @see java.lang.Cloneable
    * @since 1.2
    */
  override def clone: Any = try super.clone
  catch {
    case e: CloneNotSupportedException =>
      // this shouldn't happen, since we are Cloneable
      throw new InternalError(e)
  }
}
