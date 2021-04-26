package de.sciss.infibrillae.geom

// This is an adapted Scala translation of the Ellipse2D Java class of OpenJDK
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

import java.io.Serializable
import scala.{Float => SFloat, Double => SDouble}

/**
  * The `Ellipse2D` class describes an ellipse that is defined
  * by a framing rectangle.
  * <p>
  * This class is only the abstract superclass for all objects which
  * store a 2D ellipse.
  * The actual storage representation of the coordinates is left to
  * the subclass.
  *
  * @author Jim Graham
  * @since 1.2
  */
object Ellipse2D {
  /**
    * The `SFloat` class defines an ellipse specified
    * in `float` precision.
    */
  class Float() extends Ellipse2D with Serializable {
    /**
      * The X coordinate of the upper-left corner of the
      * framing rectangle of this `Ellipse2D`.
      */
    private var x = 0.0f
    /**
      * The Y coordinate of the upper-left corner of the
      * framing rectangle of this `Ellipse2D`.
      */
    private var y = 0.0f
    /**
      * The overall width of this `Ellipse2D`.
      */
    private var width = 0.0f
    /**
      * The overall height of this `Ellipse2D`.
      */
    private var height = 0.0f

    /**
      * Constructs and initializes an `Ellipse2D` from the
      * specified coordinates.
      *
      * @param x the X coordinate of the upper-left corner
      *          of the framing rectangle
      * @param y the Y coordinate of the upper-left corner
      *          of the framing rectangle
      * @param w the width of the framing rectangle
      * @param h the height of the framing rectangle
      * @since 1.2
      */
    def this(x: SFloat, y: SFloat, w: SFloat, h: SFloat) {
      this()
      setFrame(x, y, w, h)
    }

    override def toString: String = s"Ellipse2D.Float($x, $y, $width, $height)"

    override def getX     : SDouble = x.toDouble
    override def getY     : SDouble = y.toDouble
    override def getWidth : SDouble = width.toDouble
    override def getHeight: SDouble = height.toDouble

    override def isEmpty: Boolean = width <= 0.0f || height <= 0.0f

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
      * @since 1.2
      */
    def setFrame(x: SFloat, y: SFloat, w: SFloat, h: SFloat): Unit = {
      this.x      = x
      this.y      = y
      this.width  = w
      this.height = h
    }

    override def setFrame(x: SDouble, y: SDouble, w: SDouble, h: SDouble): Unit = {
      this.x      = x.toFloat
      this.y      = y.toFloat
      this.width  = w.toFloat
      this.height = h.toFloat
    }

    override def getBounds2D = new Rectangle2D.Float(x, y, width, height)
  }

  /**
    * The `SDouble` class defines an ellipse specified
    * in `double` precision.
    */
  class Double() extends Ellipse2D with Serializable {
    private var x       = 0.0
    private var y       = 0.0
    private var width   = 0.0
    /**
      * The overall height of the `Ellipse2D`.
      */
    private var height  = 0.0

    /**
      * Constructs and initializes an `Ellipse2D` from the
      * specified coordinates.
      *
      * @param x the X coordinate of the upper-left corner
      *          of the framing rectangle
      * @param y the Y coordinate of the upper-left corner
      *          of the framing rectangle
      * @param w the width of the framing rectangle
      * @param h the height of the framing rectangle
      * @since 1.2
      */
    def this(x: SDouble, y: SDouble, w: SDouble, h: SDouble) {
      this()
      setFrame(x, y, w, h)
    }

    override def toString: String = s"Ellipse2D.Double($x, $y, $width, $height)"

    override def getX     : SDouble = x
    override def getY     : SDouble = y
    override def getWidth : SDouble = width
    override def getHeight: SDouble = height

    override def isEmpty: Boolean = width <= 0.0 || height <= 0.0

    override def setFrame(x: SDouble, y: SDouble, w: SDouble, h: SDouble): Unit = {
      this.x      = x
      this.y      = y
      this.width  = w
      this.height = h
    }

    override def getBounds2D = new Rectangle2D.Double(x, y, width, height)
  }
}

/**
  * This is an abstract class that cannot be instantiated directly.
  * Type-specific implementation subclasses are available for
  * instantiation and provide a number of formats for storing
  * the information necessary to satisfy the various accessor
  * methods below.
  *
  * @see java.awt.geom.Ellipse2D.SFloat
  * @see java.awt.geom.Ellipse2D.Double
  * @since 1.2
  */
abstract class Ellipse2D protected() extends RectangularShape {
  override def contains(x: SDouble, y: SDouble): Boolean = { // Normalize the coordinates compared to the ellipse
    // having a center at 0,0 and a radius of 0.5.
    val ellW = getWidth
    if (ellW <= 0.0) return false
    val normX = (x - getX) / ellW - 0.5
    val ellH = getHeight
    if (ellH <= 0.0) return false
    val normy = (y - getY) / ellH - 0.5
    (normX * normX + normy * normy) < 0.25
  }

  override def intersects(x: SDouble, y: SDouble, w: SDouble, h: SDouble): Boolean = {
    if (w <= 0.0 || h <= 0.0) return false
    // Normalize the rectangular coordinates compared to the ellipse
    val ellW = getWidth
    if (ellW <= 0.0) return false
    val normX0 = (x - getX) / ellW - 0.5
    val normX1 = normX0 + w / ellW
    val ellH = getHeight
    if (ellH <= 0.0) return false
    val normy0 = (y - getY) / ellH - 0.5
    val normy1 = normy0 + h / ellH
    // find nearest x (left edge, right edge, 0.0)
    // find nearest y (top edge, bottom edge, 0.0)
    // if nearest x,y is inside circle of radius 0.5, then intersects
    var nearX = 0.0
    var nearY = 0.0
    if (normX0 > 0.0) { // center to left of X extents
      nearX = normX0
    }
    else if (normX1 < 0.0) { // center to right of X extents
      nearX = normX1
    }
    else nearX = 0.0
    if (normy0 > 0.0) { // center above Y extents
      nearY = normy0
    }
    else if (normy1 < 0.0) { // center below Y extents
      nearY = normy1
    }
    else nearY = 0.0
    (nearX * nearX + nearY * nearY) < 0.25
  }

  override def contains(x: SDouble, y: SDouble, w: SDouble, h: SDouble): Boolean =
    contains(x, y) && contains(x + w, y) && contains(x, y + h) && contains(x + w, y + h)

  /**
    * Returns an iteration object that defines the boundary of this
    * `Ellipse2D`.
    * The iterator for this class is multi-threaded safe, which means
    * that this `Ellipse2D` class guarantees that
    * modifications to the geometry of this `Ellipse2D`
    * object do not affect any iterations of that geometry that
    * are already in process.
    *
    * @param at an optional `AffineTransform` to be applied to
    *           the coordinates as they are returned in the iteration, or
    *           `null` if untransformed coordinates are desired
    * @return the `PathIterator` object that returns the
    *         geometry of the outline of this `Ellipse2D`,
    *         one segment at a time.
    * @since 1.2
    */
  override def getPathIterator(at: AffineTransform) = new EllipseIterator(this, at)

  /**
    * Returns the hashcode for this `Ellipse2D`.
    *
    * @return the hashcode for this `Ellipse2D`.
    * @since 1.6
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
    * equal to this `Ellipse2D`.  The specified
    * `Object` is equal to this `Ellipse2D`
    * if it is an instance of `Ellipse2D` and if its
    * location and size are the same as this `Ellipse2D`.
    *
    * @param obj an `Object` to be compared with this
    *            `Ellipse2D`.
    * @return `true` if `obj` is an instance
    *         of `Ellipse2D` and has the same values;
    *         `false` otherwise.
    * @since 1.6
    */
  override def equals(obj: Any): Boolean = {
    if (obj.asInstanceOf[AnyRef] eq this) return true
    obj match {
      case e2d: Ellipse2D =>
        return (getX == e2d.getX) && (getY == e2d.getY) && (getWidth == e2d.getWidth) && (getHeight == e2d.getHeight)
      case _ =>
    }
    false
  }
}
