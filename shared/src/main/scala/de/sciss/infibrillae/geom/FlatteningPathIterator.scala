package de.sciss.infibrillae.geom

// This is an adapted Scala translation of the FlatteningPathIterator Java class of OpenJDK
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

import PathIterator.{SEG_CLOSE, SEG_CUBICTO, SEG_LINETO, SEG_MOVETO, SEG_QUADTO}

import java.awt.geom.{CubicCurve2D, QuadCurve2D}

/**
  * The `FlatteningPathIterator` class returns a flattened view of
  * another `PathIterator` object.  Other `java.awt.Shape`
  * classes can use this class to provide flattening behavior for their paths
  * without having to perform the interpolation calculations themselves.
  *
  * @author Jim Graham
  */
object FlatteningPathIterator {
  private[geom] val GROW_SIZE = 24 // Multiple of cubic & quad curve size

}

/**
  * Constructs a new `FlatteningPathIterator` object
  * that flattens a path as it iterates over it.
  * The `limit` parameter allows you to control the
  * maximum number of recursive subdivisions that the iterator
  * can make before it assumes that the curve is flat enough
  * without measuring against the `flatness` parameter.
  * The flattened iteration therefore never generates more than
  * a maximum of `(2^limit)` line segments per curve.
  *
  * @param src      the original unflattened path being iterated over
  * @param flatness the maximum allowable distance between the
  *                 control points and the flattened curve
  * @param limit    the maximum number of recursive subdivisions
  *                 allowed for any curved segment
  * @throws IllegalArgumentException if
  *            `flatness` or `limit`
  *            is less than zero
  */
class FlatteningPathIterator(var src: PathIterator // The source iterator
                             , val flatness: Double, var limit: Int // Maximum number of recursion levels
                            ) extends PathIterator {

  if (flatness < 0.0) throw new IllegalArgumentException("flatness must be >= 0")
  if (limit < 0) throw new IllegalArgumentException("limit must be >= 0")

  private[geom] var squareflat  = flatness * flatness // Square of the flatness parameter
  private[geom] var levels      = new Array[Int](limit + 1) // The recursion level at which

  // prime the first path segment
  next(false)

  private[geom] var hold = new Array[Double](14) // The cache of interpolated coords

  private[geom] var curx = .0
  private[geom] var cury = .0 // The ending x,y of the last segment

  private[geom] var movx = .0
  private[geom] var movy = .0 // The x,y of the last move segment

  private[geom] var holdType = 0 // The type of the curve being held

  private[geom] var holdEnd = 0 // The index of the last curve segment

  private[geom] var holdIndex = 0 // The index of the curve segment

  private[geom] var levelIndex = 0 // The index of the entry in the

  private[geom] var done = false // True when iteration is done

  /**
    * Constructs a new `FlatteningPathIterator` object that
    * flattens a path as it iterates over it.  The iterator does not
    * subdivide any curve read from the source iterator to more than
    * 10 levels of subdivision which yields a maximum of 1024 line
    * segments per curve.
    *
    * @param src      the original unflattened path being iterated over
    * @param flatness the maximum allowable distance between the
    *                 control points and the flattened curve
    */
  def this(src: PathIterator, flatness: Double) =
    this(src, flatness, 10)

  /**
    * Returns the flatness of this iterator.
    *
    * @return the flatness of this `FlatteningPathIterator`.
    */
  def getFlatness: Double = Math.sqrt(squareflat)

  /**
    * Returns the recursion limit of this iterator.
    *
    * @return the recursion limit of this
    *         `FlatteningPathIterator`.
    */
  def getRecursionLimit: Int = limit

  /**
    * Returns the winding rule for determining the interior of the
    * path.
    *
    * @return the winding rule of the original unflattened path being
    *         iterated over.
    * @see PathIterator#WIND_EVEN_ODD
    * @see PathIterator#WIND_NON_ZERO
    */
  override def getWindingRule: Int = src.getWindingRule

  /**
    * Tests if the iteration is complete.
    *
    * @return `true` if all the segments have
    *         been read; `false` otherwise.
    */
  override def isDone: Boolean = done

  /*
       * Ensures that the hold array can hold up to (want) more values.
       * It is currently holding (hold.length - holdIndex) values.
       */
  private[geom] def ensureHoldCapacity(want: Int): Unit = {
    if (holdIndex - want < 0) {
      val have = hold.length - holdIndex
      val newsize = hold.length + FlatteningPathIterator.GROW_SIZE
      val newhold = new Array[Double](newsize)
      System.arraycopy(hold, holdIndex, newhold, holdIndex + FlatteningPathIterator.GROW_SIZE, have)
      hold = newhold
      holdIndex += FlatteningPathIterator.GROW_SIZE
      holdEnd += FlatteningPathIterator.GROW_SIZE
    }
  }

  /**
    * Moves the iterator to the next segment of the path forwards
    * along the primary direction of traversal as long as there are
    * more points in that direction.
    */
  override def next(): Unit = {
    next(true)
  }

  private def next(doNext: Boolean): Unit = {
    var level = 0
    if (holdIndex >= holdEnd) {
      if (doNext) src.next()
      if (src.isDone) {
        done = true
        return
      }
      holdType = src.currentSegment(hold)
      levelIndex = 0
      levels(0) = 0
    }
    holdType match {
      case SEG_MOVETO =>
      case SEG_LINETO =>
        curx = hold(0)
        cury = hold(1)
        if (holdType == SEG_MOVETO) {
          movx = curx
          movy = cury
        }
        holdIndex = 0
        holdEnd = 0

      case SEG_CLOSE =>
        curx = movx
        cury = movy
        holdIndex = 0
        holdEnd = 0

      case SEG_QUADTO =>
        if (holdIndex >= holdEnd) { // Move the coordinates to the end of the array.
          holdIndex = hold.length - 6
          holdEnd = hold.length - 2
          hold(holdIndex + 0) = curx
          hold(holdIndex + 1) = cury
          hold(holdIndex + 2) = hold(0)
          hold(holdIndex + 3) = hold(1)
          curx = hold(2)
          hold(holdIndex + 4) = curx
          cury = hold(3)
          hold(holdIndex + 5) = cury
        }
        level = levels(levelIndex)
        while (level < limit && !(QuadCurve2D.getFlatnessSq(hold, holdIndex) < squareflat)) {
          ensureHoldCapacity(4)
          QuadCurve2D.subdivide(hold, holdIndex, hold, holdIndex - 4, hold, holdIndex)
          holdIndex -= 4
          // Now that we have subdivided, we have constructed
          // two curves of one depth lower than the original
          // curve.  One of those curves is in the place of
          // the former curve and one of them is in the next
          // set of held coordinate slots.  We now set both
          // curves level values to the next higher level.
          level += 1
          levels(levelIndex) = level
          levelIndex += 1
          levels(levelIndex) = level
        }
        // This curve segment is flat enough, or it is too deep
        // in recursion levels to try to flatten any more.  The
        // two coordinates at holdIndex+4 and holdIndex+5 now
        // contain the endpoint of the curve which can be the
        // endpoint of an approximating line segment.
        holdIndex += 4
        levelIndex -= 1

      case SEG_CUBICTO =>
        if (holdIndex >= holdEnd) {
          holdIndex = hold.length - 8
          holdEnd = hold.length - 2
          hold(holdIndex + 0) = curx
          hold(holdIndex + 1) = cury
          hold(holdIndex + 2) = hold(0)
          hold(holdIndex + 3) = hold(1)
          hold(holdIndex + 4) = hold(2)
          hold(holdIndex + 5) = hold(3)
          curx = hold(4)
          hold(holdIndex + 6) = curx
          cury = hold(5)
          hold(holdIndex + 7) = cury
        }
        level = levels(levelIndex)
        while (level < limit && !(CubicCurve2D.getFlatnessSq(hold, holdIndex) < squareflat)) {
          ensureHoldCapacity(6)
          CubicCurve2D.subdivide(hold, holdIndex, hold, holdIndex - 6, hold, holdIndex)
          holdIndex -= 6
          level += 1
          levels(levelIndex) = level
          levelIndex += 1
          levels(levelIndex) = level
        }
        // two coordinates at holdIndex+6 and holdIndex+7 now
        holdIndex += 6
        levelIndex -= 1

    }
  }

  /**
    * Returns the coordinates and type of the current path segment in
    * the iteration.
    * The return value is the path segment type:
    * SEG_MOVETO, SEG_LINETO, or SEG_CLOSE.
    * A float array of length 6 must be passed in and can be used to
    * store the coordinates of the point(s).
    * Each point is stored as a pair of float x,y coordinates.
    * SEG_MOVETO and SEG_LINETO types return one point,
    * and SEG_CLOSE does not return any points.
    *
    * @param coords an array that holds the data returned from
    *               this method
    * @return the path segment type of the current path segment.
    * @throws NoSuchElementException if there
    *            are no more elements in the flattening path to be
    *            returned.
    * @see PathIterator#SEG_MOVETO
    * @see PathIterator#SEG_LINETO
    * @see PathIterator#SEG_CLOSE
    */
  override def currentSegment(coords: Array[Float]): Int = {
    if (isDone) throw new NoSuchElementException("flattening iterator out of bounds")
    var tpe = holdType
    if (tpe != SEG_CLOSE) {
      coords(0) = hold(holdIndex + 0).toFloat
      coords(1) = hold(holdIndex + 1).toFloat
      if (tpe != SEG_MOVETO) tpe = SEG_LINETO
    }
    tpe
  }

  /**
    * Returns the coordinates and type of the current path segment in
    * the iteration.
    * The return value is the path segment type:
    * SEG_MOVETO, SEG_LINETO, or SEG_CLOSE.
    * A double array of length 6 must be passed in and can be used to
    * store the coordinates of the point(s).
    * Each point is stored as a pair of double x,y coordinates.
    * SEG_MOVETO and SEG_LINETO types return one point,
    * and SEG_CLOSE does not return any points.
    *
    * @param coords an array that holds the data returned from
    *               this method
    * @return the path segment type of the current path segment.
    * @throws NoSuchElementException if there
    *            are no more elements in the flattening path to be
    *            returned.
    * @see PathIterator#SEG_MOVETO
    * @see PathIterator#SEG_LINETO
    * @see PathIterator#SEG_CLOSE
    */
  override def currentSegment(coords: Array[Double]): Int = {
    if (isDone) throw new NoSuchElementException("flattening iterator out of bounds")
    var tpe = holdType
    if (tpe != SEG_CLOSE) {
      coords(0) = hold(holdIndex + 0)
      coords(1) = hold(holdIndex + 1)
      if (tpe != SEG_MOVETO) tpe = SEG_LINETO
    }
    tpe
  }
}
