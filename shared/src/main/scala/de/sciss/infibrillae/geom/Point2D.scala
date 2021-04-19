package de.sciss.infibrillae.geom

// This is an adapted Scala translation of the Point2D Java class of OpenJDK
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

import java.io.Serializable
import scala.{Double => SDouble, Float => SFloat}

/**
  * The `Point2D` class defines a point representing a location
  * in `(x,y)` coordinate space.
  * <p>
  * This class is only the abstract superclass for all objects that
  * store a 2D coordinate.
  * The actual storage representation of the coordinates is left to
  * the subclass.
  *
  * @author Jim Graham
  * @since 1.2
  */
object Point2D {
  /**
    * Constructs and initializes a `Point2D` with
    * the specified coordinates.
    *
    * @param x the X coordinate of the newly
    *          constructed `Point2D`
    * @param y the Y coordinate of the newly
    *          constructed `Point2D`
    * @since 1.2
    */
  class Float(private var x: SFloat, private var y: SFloat) 
    extends Point2D with Serializable {
    
    def this() = this(0.0f, 0.0f)

    override def getX: SDouble = x
    override def getY: SDouble = y

    override def setLocation(x: SDouble, y: SDouble): Unit = {
      this.x = x.toFloat
      this.y = y.toFloat
    }

    /**
      * Sets the location of this `Point2D` to the
      * specified `float` coordinates.
      *
      * @param x the new X coordinate of this `Point2D`
      * @param y the new Y coordinate of this `Point2D`
      * @since 1.2
      */
    def setLocation(x: SFloat, y: SFloat): Unit = {
      this.x = x
      this.y = y
    }

    /**
      * Returns a `String` that represents the value
      * of this `Point2D`.
      *
      * @return a string representation of this `Point2D`.
      * @since 1.2
      */
    override def toString: String = "Point2D.SFloat[" + x + ", " + y + "]"
  }

  /**
    * Constructs and initializes a `Point2D` with the
    * specified coordinates.
    *
    * @param x the X coordinate of the newly
    *          constructed `Point2D`
    * @param y the Y coordinate of the newly
    *          constructed `Point2D`
    * @since 1.2
    */
  class Double(private var x: SDouble, private var y: SDouble)
    extends Point2D with Serializable {

    def this() = this(0.0, 0.0)

    override def getX: SDouble = x

    override def getY: SDouble = y

    override def setLocation(x: SDouble, y: SDouble): Unit = {
      this.x = x
      this.y = y
    }

    override def toString: String = "Point2D.SDouble[" + x + ", " + y + "]"
  }

  /**
    * Returns the square of the distance between two points.
    *
    * @param x1 the X coordinate of the first specified point
    * @param y1 the Y coordinate of the first specified point
    * @param x2 the X coordinate of the second specified point
    * @param y2 the Y coordinate of the second specified point
    * @return the square of the distance between the two
    *         sets of specified coordinates.
    * @since 1.2
    */
  def distanceSq(x1: SDouble, y1: SDouble, x2: SDouble, y2: SDouble): SDouble = {
    var _x1 = x1
    var _y1 = y1
    _x1 -= x2
    _y1 -= y2
    _x1 * _x1 + _y1 * _y1
  }

  /**
    * Returns the distance between two points.
    *
    * @param x1 the X coordinate of the first specified point
    * @param y1 the Y coordinate of the first specified point
    * @param x2 the X coordinate of the second specified point
    * @param y2 the Y coordinate of the second specified point
    * @return the distance between the two sets of specified
    *         coordinates.
    * @since 1.2
    */
  def distance(x1: SDouble, y1: SDouble, x2: SDouble, y2: SDouble): SDouble = {
    var _x1 = x1
    var _y1 = y1
    _x1 -= x2
    _y1 -= y2
    Math.sqrt(_x1 * _x1 + _y1 * _y1)
  }
}

/**
  * This is an abstract class that cannot be instantiated directly.
  * Type-specific implementation subclasses are available for
  * instantiation and provide a number of formats for storing
  * the information necessary to satisfy the various accessor
  * methods below.
  *
  * @see java.awt.geom.Point2D.SFloat
  * @see java.awt.geom.Point2D.SDouble
  * @see java.awt.Point
  * @since 1.2
  */
abstract class Point2D protected() extends Cloneable {
  /**
    * Returns the X coordinate of this `Point2D` in
    * `double` precision.
    *
    * @return the X coordinate of this `Point2D`.
    * @since 1.2
    */
  def getX: SDouble

  /**
    * Returns the Y coordinate of this `Point2D` in
    * `double` precision.
    *
    * @return the Y coordinate of this `Point2D`.
    * @since 1.2
    */
  def getY: SDouble

  /**
    * Sets the location of this `Point2D` to the
    * specified `double` coordinates.
    *
    * @param x the new X coordinate of this `Point2D`
    * @param y the new Y coordinate of this `Point2D`
    * @since 1.2
    */
  def setLocation(x: SDouble, y: SDouble): Unit

  /**
    * Sets the location of this `Point2D` to the same
    * coordinates as the specified `Point2D` object.
    *
    * @param p the specified `Point2D` to which to set
    *          this `Point2D`
    * @since 1.2
    */
  def setLocation(p: Point2D): Unit =
    setLocation(p.getX, p.getY)

  /**
    * Returns the square of the distance from this
    * `Point2D` to a specified point.
    *
    * @param px the X coordinate of the specified point to be measured
    *           against this `Point2D`
    * @param py the Y coordinate of the specified point to be measured
    *           against this `Point2D`
    * @return the square of the distance between this
    *         `Point2D` and the specified point.
    * @since 1.2
    */
  def distanceSq(px: SDouble, py: SDouble): SDouble = {
    var _px = px
    var _py = py
    _px -= getX
    _py -= getY
    _px * _px + _py * _py
  }

  /**
    * Returns the square of the distance from this
    * `Point2D` to a specified `Point2D`.
    *
    * @param pt the specified point to be measured
    *           against this `Point2D`
    * @return the square of the distance between this
    *         `Point2D` to a specified `Point2D`.
    * @since 1.2
    */
  def distanceSq(pt: Point2D): SDouble = {
    val px = pt.getX - this.getX
    val py = pt.getY - this.getY
    px * px + py * py
  }

  /**
    * Returns the distance from this `Point2D` to
    * a specified point.
    *
    * @param px the X coordinate of the specified point to be measured
    *           against this `Point2D`
    * @param py the Y coordinate of the specified point to be measured
    *           against this `Point2D`
    * @return the distance between this `Point2D`
    *         and a specified point.
    * @since 1.2
    */
  def distance(px: SDouble, py: SDouble): SDouble = {
    var _px = px
    var _py = py
    _px -= getX
    _py -= getY
    Math.sqrt(_px * _px + _py * _py)
  }

  /**
    * Returns the distance from this `Point2D` to a
    * specified `Point2D`.
    *
    * @param pt the specified point to be measured
    *           against this `Point2D`
    * @return the distance between this `Point2D` and
    *         the specified `Point2D`.
    * @since 1.2
    */
  def distance(pt: Point2D): SDouble = {
    val px = pt.getX - this.getX
    val py = pt.getY - this.getY
    Math.sqrt(px * px + py * py)
  }

  /**
    * Creates a new object of the same class and with the
    * same contents as this object.
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

  /**
    * Returns the hashcode for this `Point2D`.
    *
    * @return a hash code for this `Point2D`.
    */
  override def hashCode: Int = {
    var bits = java.lang.Double.doubleToLongBits(getX)
    bits ^= java.lang.Double.doubleToLongBits(getY) * 31
    bits.toInt ^ (bits >> 32).toInt
  }

  /**
    * Determines whether or not two points are equal. Two instances of
    * `Point2D` are equal if the values of their
    * `x` and `y` member fields, representing
    * their position in the coordinate space, are the same.
    *
    * @param obj an object to be compared with this `Point2D`
    * @return `true` if the object to be compared is
    *         an instance of `Point2D` and has
    *         the same values; `false` otherwise.
    */
  override def equals(obj: Any): Boolean =
    obj match {
      case p2d: Point2D =>
        (getX == p2d.getX) && (getY == p2d.getY)
      case _ =>
        super.equals(obj)
    }
}
