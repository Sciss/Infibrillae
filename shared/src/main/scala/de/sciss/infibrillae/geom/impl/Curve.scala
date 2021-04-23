package de.sciss.infibrillae.geom.impl

// This is an adapted Scala translation of the Curve Java class of OpenJDK
// as released under GNU GPL 2 -- see original file header below.
// So it can be used in Scala.js.

/*
 * Copyright (c) 1998, 2006, Oracle and/or its affiliates. All rights reserved.
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

import de.sciss.infibrillae.geom.{IllegalPathStateException, PathIterator, Rectangle2D, impl}

import scala.collection.mutable

object Curve {
  final val INCREASING = 1
  final val DECREASING = -1

  def insertMove(curves: mutable.Growable[Curve], x: Double, y: Double): Unit = {
    curves += new impl.Order0(x, y)
  }

  def insertLine(curves: mutable.Growable[Curve], x0: Double, y0: Double, x1: Double, y1: Double): Unit = {
    if      (y0 < y1) curves += new Order1(x0, y0, x1, y1, INCREASING)
    else if (y0 > y1) curves += new Order1(x1, y1, x0, y0, DECREASING)
    else {
      // Do not add horizontal lines
    }
  }

  def insertQuad(curves: mutable.Growable[Curve], x0: Double, y0: Double, coords: Array[Double]): Unit = {
    val y1 = coords(3)
    if (y0 > y1) Order2.insert(curves, coords, coords(2), y1, coords(0), coords(1), x0, y0, DECREASING)
    else if (y0 == y1 && y0 == coords(1)) ()
    else Order2.insert(curves, coords, x0, y0, coords(0), coords(1), coords(2), y1, INCREASING)
  }

  def insertCubic(curves: mutable.Growable[Curve], x0: Double, y0: Double, coords: Array[Double]): Unit = {
    val y1 = coords(5)
    if (y0 > y1) Order3.insert(curves, coords, coords(4), y1, coords(2), coords(3), coords(0), coords(1), x0, y0, DECREASING)
    else if (y0 == y1 && y0 == coords(1) && y0 == coords(3)) ()
    else Order3.insert(curves, coords, x0, y0, coords(0), coords(1), coords(2), coords(3), coords(4), y1, INCREASING)
  }

  /**
    * Calculates the number of times the given path
    * crosses the ray extending to the right from (px,py).
    * If the point lies on a part of the path,
    * then no crossings are counted for that intersection.
    * +1 is added for each crossing where the Y coordinate is increasing
    * -1 is added for each crossing where the Y coordinate is decreasing
    * The return value is the sum of all crossings for every segment in
    * the path.
    * The path must start with a SEG_MOVETO, otherwise an exception is
    * thrown.
    * The caller must check p[xy] for NaN values.
    * The caller may also reject infinite p[xy] values as well.
    */
  def pointCrossingsForPath(pi: PathIterator, px: Double, py: Double): Int = {
    if (pi.isDone) return 0
    val coords = new Array[Double](6)
    if (pi.currentSegment(coords) != PathIterator.SEG_MOVETO)
      throw new IllegalPathStateException("missing initial moveto " + "in path definition")
    pi.next()
    var movX = coords(0)
    var movY = coords(1)
    var curX = movX
    var curY = movY
    var endX = 0.0
    var endY = 0.0
    var crossings = 0
    while (!pi.isDone) {
      pi.currentSegment(coords) match {
        case PathIterator.SEG_MOVETO =>
          if (curY != movY) crossings += pointCrossingsForLine(px, py, curX, curY, movX, movY)
          curX = coords(0)
          movX = curX
          curY = coords(1)
          movY = curY

        case PathIterator.SEG_LINETO =>
          endX = coords(0)
          endY = coords(1)
          crossings += pointCrossingsForLine(px, py, curX, curY, endX, endY)
          curX = endX
          curY = endY

        case PathIterator.SEG_QUADTO =>
          endX = coords(2)
          endY = coords(3)
          crossings += pointCrossingsForQuad(px, py, curX, curY, coords(0), coords(1), endX, endY, 0)
          curX = endX
          curY = endY

        case PathIterator.SEG_CUBICTO =>
          endX = coords(4)
          endY = coords(5)
          crossings += pointCrossingsForCubic(px, py, curX, curY, coords(0), coords(1), coords(2), coords(3), endX, endY, 0)
          curX = endX
          curY = endY

        case PathIterator.SEG_CLOSE =>
          if (curY != movY) crossings += pointCrossingsForLine(px, py, curX, curY, movX, movY)
          curX = movX
          curY = movY

      }
      pi.next()
    }
    if (curY != movY) crossings += pointCrossingsForLine(px, py, curX, curY, movX, movY)
    crossings
  }

  /**
    * Calculates the number of times the line from (x0,y0) to (x1,y1)
    * crosses the ray extending to the right from (px,py).
    * If the point lies on the line, then no crossings are recorded.
    * +1 is returned for a crossing where the Y coordinate is increasing
    * -1 is returned for a crossing where the Y coordinate is decreasing
    */
  def pointCrossingsForLine(px: Double, py: Double, x0: Double, y0: Double, x1: Double, y1: Double): Int = {
    if (py < y0 && py < y1) return 0
    if (py >= y0 && py >= y1) return 0
    // assert(y0 != y1);
    if (px >= x0 && px >= x1) return 0
    if (px < x0 && px < x1) return if (y0 < y1) 1
    else -1
    val xIntercept = x0 + (py - y0) * (x1 - x0) / (y1 - y0)
    if (px >= xIntercept) return 0
    if (y0 < y1) 1
    else -1
  }

  /**
    * Calculates the number of times the quad from (x0,y0) to (x1,y1)
    * crosses the ray extending to the right from (px,py).
    * If the point lies on a part of the curve,
    * then no crossings are counted for that intersection.
    * the level parameter should be 0 at the top-level call and will count
    * up for each recursion level to prevent infinite recursion
    * +1 is added for each crossing where the Y coordinate is increasing
    * -1 is added for each crossing where the Y coordinate is decreasing
    */
  def pointCrossingsForQuad(px: Double, py: Double, x0: Double, y0: Double, xc: Double, yc: Double,
                            x1: Double, y1: Double, level: Int): Int = {
    var _xc = xc
    var _yc = yc
    if (py < y0 && py < _yc && py < y1) return 0
    if (py >= y0 && py >= _yc && py >= y1) return 0
    // Note y0 could equal y1...
    if (px >= x0 && px >= _xc && px >= x1) return 0
    if (px < x0 && px < _xc && px < x1) {
      if (py >= y0) if (py < y1) return 1
      else { // py < y0
        if (py >= y1) return -1
      }
      // py outside of y01 range, and/or y0==y1
      return 0
    }
    // double precision only has 52 bits of mantissa
    if (level > 52) return pointCrossingsForLine(px, py, x0, y0, x1, y1)
    val x0c = (x0 + _xc) / 2
    val y0c = (y0 + _yc) / 2
    val xc1 = (_xc + x1) / 2
    val yc1 = (_yc + y1) / 2
    _xc = (x0c + xc1) / 2
    _yc = (y0c + yc1) / 2
    if (java.lang.Double.isNaN(_xc) || java.lang.Double.isNaN(_yc)) { // [xy]c are NaN if any of [xy]0c or [xy]c1 are NaN
      // [xy]0c or [xy]c1 are NaN if any of [xy][0c1] are NaN
      // These values are also NaN if opposing infinities are added
      return 0
    }
    pointCrossingsForQuad  (px, py, x0, y0, x0c, y0c, _xc, _yc, level + 1) +
      pointCrossingsForQuad(px, py, _xc, _yc, xc1, yc1, x1, y1, level + 1)
  }

  /**
    * Calculates the number of times the cubic from (x0,y0) to (x1,y1)
    * crosses the ray extending to the right from (px,py).
    * If the point lies on a part of the curve,
    * then no crossings are counted for that intersection.
    * the level parameter should be 0 at the top-level call and will count
    * up for each recursion level to prevent infinite recursion
    * +1 is added for each crossing where the Y coordinate is increasing
    * -1 is added for each crossing where the Y coordinate is decreasing
    */
  def pointCrossingsForCubic(px: Double, py: Double, x0: Double, y0: Double, xc0: Double, yc0: Double,
                             xc1: Double, yc1: Double, x1: Double, y1: Double, level: Int): Int = {
    var _xc0 = xc0
    var _yc0 = yc0
    var _xc1 = xc1
    var _yc1 = yc1
    if (py < y0 && py < _yc0 && py < _yc1 && py < y1) return 0
    if (py >= y0 && py >= _yc0 && py >= _yc1 && py >= y1) return 0
    // Note y0 could equal yc0...
    if (px >= x0 && px >= _xc0 && px >= _xc1 && px >= x1) return 0
    if (px < x0 && px < _xc0 && px < _xc1 && px < x1) {
      if (py >= y0) if (py < y1) return 1
      else if (py >= y1) return -1
      // py outside of y01 range, and/or y0==yc0
      return 0
    }
    if (level > 52) return pointCrossingsForLine(px, py, x0, y0, x1, y1)
    var xmid = (_xc0 + _xc1) / 2
    var ymid = (_yc0 + _yc1) / 2
    _xc0 = (x0 + _xc0) / 2
    _yc0 = (y0 + _yc0) / 2
    _xc1 = (_xc1 + x1) / 2
    _yc1 = (_yc1 + y1) / 2
    val xc0m = (_xc0 + xmid) / 2
    val yc0m = (_yc0 + ymid) / 2
    val xmc1 = (xmid + _xc1) / 2
    val ymc1 = (ymid + _yc1) / 2
    xmid = (xc0m + xmc1) / 2
    ymid = (yc0m + ymc1) / 2
    if (java.lang.Double.isNaN(xmid) || java.lang.Double.isNaN(ymid)) { // [xy]mid are NaN if any of [xy]c0m or [xy]mc1 are NaN
      // [xy]c0m or [xy]mc1 are NaN if any of [xy][c][01] are NaN
      return 0
    }
    pointCrossingsForCubic(px, py, x0, y0, _xc0, _yc0, xc0m, yc0m, xmid, ymid, level + 1) + pointCrossingsForCubic(px, py, xmid, ymid, xmc1, ymc1, _xc1, _yc1, x1, y1, level + 1)
  }

  /**
    * The rectangle intersection test counts the number of times
    * that the path crosses through the shadow that the rectangle
    * projects to the right towards (x => +INFINITY).
    *
    * During processing of the path it actually counts every time
    * the path crosses either or both of the top and bottom edges
    * of that shadow.  If the path enters from the top, the count
    * is incremented.  If it then exits back through the top, the
    * same way it came in, the count is decremented and there is
    * no impact on the winding count.  If, instead, the path exits
    * out the bottom, then the count is incremented again and a
    * full pass through the shadow is indicated by the winding count
    * having been incremented by 2.
    *
    * Thus, the winding count that it accumulates is actually double
    * the real winding count.  Since the path is continuous, the
    * final answer should be a multiple of 2, otherwise there is a
    * logic error somewhere.
    *
    * If the path ever has a direct hit on the rectangle, then a
    * special value is returned.  This special value terminates
    * all ongoing accumulation on up through the call chain and
    * ends up getting returned to the calling function which can
    * then produce an answer directly.  For intersection tests,
    * the answer is always "true" if the path intersects the
    * rectangle.  For containment tests, the answer is always
    * "false" if the path intersects the rectangle.  Thus, no
    * further processing is ever needed if an intersection occurs.
    */
  final val RECT_INTERSECTS = 0x80000000

  /**
    * Accumulate the number of times the path crosses the shadow
    * extending to the right of the rectangle.  See the comment
    * for the RECT_INTERSECTS constant for more complete details.
    * The return value is the sum of all crossings for both the
    * top and bottom of the shadow for every segment in the path,
    * or the special value RECT_INTERSECTS if the path ever enters
    * the interior of the rectangle.
    * The path must start with a SEG_MOVETO, otherwise an exception is
    * thrown.
    * The caller must check r[xy]{min,max} for NaN values.
    */
  def rectCrossingsForPath(pi: PathIterator, rxMin: Double, ryMin: Double, rxMax: Double, ryMax: Double): Int = {
    if (rxMax <= rxMin || ryMax <= ryMin) return 0
    if (pi.isDone) return 0
    val coords = new Array[Double](6)
    if (pi.currentSegment(coords) != PathIterator.SEG_MOVETO) throw new IllegalPathStateException("missing initial moveto " + "in path definition")
    pi.next()
    var curx = .0
    var cury = .0
    var movx = .0
    var movy = .0
    var endx = .0
    var endy = .0
    movx = coords(0)
    curx = movx
    movy = coords(1)
    cury = movy
    var crossings = 0
    while ( {
      crossings != RECT_INTERSECTS && !pi.isDone
    }) {
      pi.currentSegment(coords) match {
        case PathIterator.SEG_MOVETO =>
          if (curx != movx || cury != movy) crossings = rectCrossingsForLine(crossings, rxMin, ryMin, rxMax, ryMax, curx, cury, movx, movy)
          // Count should always be a multiple of 2 here.
          // assert((crossings & 1) != 0);
          curx = coords(0)
          movx = curx
          cury = coords(1)
          movy = cury

        case PathIterator.SEG_LINETO =>
          endx = coords(0)
          endy = coords(1)
          crossings = rectCrossingsForLine(crossings, rxMin, ryMin, rxMax, ryMax, curx, cury, endx, endy)
          curx = endx
          cury = endy

        case PathIterator.SEG_QUADTO =>
          endx = coords(2)
          endy = coords(3)
          crossings = rectCrossingsForQuad(crossings, rxMin, ryMin, rxMax, ryMax, curx, cury, coords(0), coords(1), endx, endy, 0)
          curx = endx
          cury = endy

        case PathIterator.SEG_CUBICTO =>
          endx = coords(4)
          endy = coords(5)
          crossings = rectCrossingsForCubic(crossings, rxMin, ryMin, rxMax, ryMax, curx, cury, coords(0), coords(1), coords(2), coords(3), endx, endy, 0)
          curx = endx
          cury = endy

        case PathIterator.SEG_CLOSE =>
          if (curx != movx || cury != movy) crossings = rectCrossingsForLine(crossings, rxMin, ryMin, rxMax, ryMax, curx, cury, movx, movy)
          curx = movx
          cury = movy

      }
      pi.next()
    }
    if (crossings != RECT_INTERSECTS && (curx != movx || cury != movy)) crossings = rectCrossingsForLine(crossings, rxMin, ryMin, rxMax, ryMax, curx, cury, movx, movy)
    crossings
  }

  /**
    * Accumulate the number of times the line crosses the shadow
    * extending to the right of the rectangle.  See the comment
    * for the RECT_INTERSECTS constant for more complete details.
    */
  def rectCrossingsForLine(crossings: Int, rxMin: Double, ryMin: Double, rxMax: Double, ryMax: Double,
                           x0: Double, y0: Double, x1: Double, y1: Double): Int = {
    var _crossings = crossings
    if (y0 >= ryMax && y1 >= ryMax) return _crossings
    if (y0 <= ryMin && y1 <= ryMin) return _crossings
    if (x0 <= rxMin && x1 <= rxMin) return _crossings
    if (x0 >= rxMax && x1 >= rxMax) { // Line is entirely to the right of the rect
      // and the vertical ranges of the two overlap by a non-empty amount
      // Thus, this line segment is partially in the "right-shadow"
      // Path may have done a complete crossing
      // Or path may have entered or exited the right-shadow
      if (y0 < y1) { // y-increasing line segment...
        // We know that y0 < rymax and y1 > rymin
        if (y0 <= ryMin) _crossings += 1
        if (y1 >= ryMax) _crossings += 1
      }
      else if (y1 < y0) { // y-decreasing line segment...
        // We know that y1 < rymax and y0 > rymin
        if (y1 <= ryMin) _crossings -= 1
        if (y0 >= ryMax) _crossings -= 1
      }
      return _crossings
    }
    // Remaining case:
    // Both x and y ranges overlap by a non-empty amount
    // First do trivial INTERSECTS rejection of the cases
    // where one of the endpoints is inside the rectangle.
    if ((x0 > rxMin && x0 < rxMax && y0 > ryMin && y0 < ryMax) || (x1 > rxMin && x1 < rxMax && y1 > ryMin && y1 < ryMax)) return RECT_INTERSECTS
    // Otherwise calculate the y intercepts and see where
    // they fall with respect to the rectangle
    var xi0 = x0
    if (y0 < ryMin) xi0 += ((ryMin - y0) * (x1 - x0) / (y1 - y0))
    else if (y0 > ryMax) xi0 += ((ryMax - y0) * (x1 - x0) / (y1 - y0))
    var xi1 = x1
    if (y1 < ryMin) xi1 += ((ryMin - y1) * (x0 - x1) / (y0 - y1))
    else if (y1 > ryMax) xi1 += ((ryMax - y1) * (x0 - x1) / (y0 - y1))
    if (xi0 <= rxMin && xi1 <= rxMin) return _crossings
    if (xi0 >= rxMax && xi1 >= rxMax) {
      if (y0 < y1) {
        if (y0 <= ryMin) _crossings += 1
        if (y1 >= ryMax) _crossings += 1
      }
      else if (y1 < y0) {
        if (y1 <= ryMin) _crossings -= 1
        if (y0 >= ryMax) _crossings -= 1
      }
      return _crossings
    }
    RECT_INTERSECTS
  }

  /**
    * Accumulate the number of times the quad crosses the shadow
    * extending to the right of the rectangle.  See the comment
    * for the RECT_INTERSECTS constant for more complete details.
    */
  def rectCrossingsForQuad(crossings: Int, rxMin: Double, ryMin: Double, rxMax: Double, ryMax: Double,
                           x0: Double, y0: Double, xc: Double, yc: Double, x1: Double, y1: Double, level: Int): Int = {
    var _crossings = crossings
    var _xc = xc
    var _yc = yc
    if (y0 >= ryMax && _yc >= ryMax && y1 >= ryMax) return _crossings
    if (y0 <= ryMin && _yc <= ryMin && y1 <= ryMin) return _crossings
    if (x0 <= rxMin && _xc <= rxMin && x1 <= rxMin) return _crossings
    if (x0 >= rxMax && _xc >= rxMax && x1 >= rxMax) { // Quad is entirely to the right of the rect
      // and the vertical range of the 3 Y coordinates of the quad
      // overlaps the vertical range of the rect by a non-empty amount
      // We now judge the crossings solely based on the line segment
      // connecting the endpoints of the quad.
      // Note that we may have 0, 1, or 2 crossings as the control
      // point may be causing the Y range intersection while the
      // two endpoints are entirely above or below.
      if (y0 < y1) {
        if (y0 <= ryMin && y1 > ryMin) _crossings += 1
        if (y0 < ryMax && y1 >= ryMax) _crossings += 1
      }
      else if (y1 < y0) {
        if (y1 <= ryMin && y0 > ryMin) _crossings -= 1
        if (y1 < ryMax && y0 >= ryMax) _crossings -= 1
      }
      return _crossings
    }
    // The intersection of ranges is more complicated
    if ((x0 < rxMax && x0 > rxMin && y0 < ryMax && y0 > ryMin) || (x1 < rxMax && x1 > rxMin && y1 < ryMax && y1 > ryMin)) return RECT_INTERSECTS
    // Otherwise, subdivide and look for one of the cases above.
    if (level > 52) return rectCrossingsForLine(_crossings, rxMin, ryMin, rxMax, ryMax, x0, y0, x1, y1)
    val x0c = (x0 + _xc) / 2
    val y0c = (y0 + _yc) / 2
    val xc1 = (_xc + x1) / 2
    val yc1 = (_yc + y1) / 2
    _xc = (x0c + xc1) / 2
    _yc = (y0c + yc1) / 2
    if (java.lang.Double.isNaN(_xc) || java.lang.Double.isNaN(_yc)) return 0
    _crossings = rectCrossingsForQuad(_crossings, rxMin, ryMin, rxMax, ryMax, x0, y0, x0c, y0c, _xc, _yc, level + 1)
    if (_crossings != RECT_INTERSECTS) _crossings = rectCrossingsForQuad(_crossings, rxMin, ryMin, rxMax, ryMax, _xc, _yc, xc1, yc1, x1, y1, level + 1)
    _crossings
  }

  /**
    * Accumulate the number of times the cubic crosses the shadow
    * extending to the right of the rectangle.  See the comment
    * for the RECT_INTERSECTS constant for more complete details.
    */
  def rectCrossingsForCubic(crossings: Int, rxMin: Double, ryMin: Double, rxMax: Double, ryMax: Double,
                            x0: Double, y0: Double, xc0: Double, yc0: Double, xc1: Double, yc1: Double,
                            x1: Double, y1: Double, level: Int): Int = {
    var _crossings = crossings
    var _xc0 = xc0
    var _yc0 = yc0
    var _xc1 = xc1
    var _yc1 = yc1
    if (y0 >= ryMax && _yc0 >= ryMax && _yc1 >= ryMax && y1 >= ryMax) return _crossings
    if (y0 <= ryMin && _yc0 <= ryMin && _yc1 <= ryMin && y1 <= ryMin) return _crossings
    if (x0 <= rxMin && _xc0 <= rxMin && _xc1 <= rxMin && x1 <= rxMin) return _crossings
    if (x0 >= rxMax && _xc0 >= rxMax && _xc1 >= rxMax && x1 >= rxMax) { // Cubic is entirely to the right of the rect
      // and the vertical range of the 4 Y coordinates of the cubic
      // connecting the endpoints of the cubic.
      // points may be causing the Y range intersection while the
      if (y0 < y1) {
        if (y0 <= ryMin && y1 > ryMin) _crossings += 1
        if (y0 < ryMax && y1 >= ryMax) _crossings += 1
      }
      else if (y1 < y0) {
        if (y1 <= ryMin && y0 > ryMin) _crossings -= 1
        if (y1 < ryMax && y0 >= ryMax) _crossings -= 1
      }
      return _crossings
    }
    if ((x0 > rxMin && x0 < rxMax && y0 > ryMin && y0 < ryMax) || (x1 > rxMin && x1 < rxMax && y1 > ryMin && y1 < ryMax)) return RECT_INTERSECTS
    if (level > 52) return rectCrossingsForLine(_crossings, rxMin, ryMin, rxMax, ryMax, x0, y0, x1, y1)
    var xMid = (_xc0 + _xc1) / 2
    var yMid = (_yc0 + _yc1) / 2
    _xc0 = (x0 + _xc0) / 2
    _yc0 = (y0 + _yc0) / 2
    _xc1 = (_xc1 + x1) / 2
    _yc1 = (_yc1 + y1) / 2
    val xc0m = (_xc0 + xMid) / 2
    val yc0m = (_yc0 + yMid) / 2
    val xmc1 = (xMid + _xc1) / 2
    val ymc1 = (yMid + _yc1) / 2
    xMid = (xc0m + xmc1) / 2
    yMid = (yc0m + ymc1) / 2
    if (java.lang.Double.isNaN(xMid) || java.lang.Double.isNaN(yMid)) return 0
    _crossings = rectCrossingsForCubic(_crossings, rxMin, ryMin, rxMax, ryMax, x0, y0, _xc0, _yc0, xc0m, yc0m, xMid, yMid, level + 1)
    if (_crossings != RECT_INTERSECTS) _crossings = rectCrossingsForCubic(_crossings, rxMin, ryMin, rxMax, ryMax, xMid, yMid, xmc1, ymc1, _xc1, _yc1, x1, y1, level + 1)
    _crossings
  }

  def round(v: Double): Double = { //return Math.rint(v*10)/10;
    v
  }

  def orderOf(x1: Double, x2: Double): Int = {
    if (x1 < x2) return -1
    if (x1 > x2) return 1
    0
  }

  def signedDiffBits(y1: Double, y2: Double): Long = java.lang.Double.doubleToLongBits(y1) - java.lang.Double.doubleToLongBits(y2)

  def diffBits(y1: Double, y2: Double): Long = Math.abs(java.lang.Double.doubleToLongBits(y1) - java.lang.Double.doubleToLongBits(y2))

  def prev(v: Double): Double = java.lang.Double.longBitsToDouble(java.lang.Double.doubleToLongBits(v) - 1)

  def next(v: Double): Double = java.lang.Double.longBitsToDouble(java.lang.Double.doubleToLongBits(v) + 1)

  final val T_MIN = 1E-3
}

abstract class Curve(private val direction: Int) {
  final def getDirection: Int = direction

  final def getWithDirection(direction: Int): Curve = if (this.direction == direction) this
  else getReversedCurve

  override def toString: String =
    "Curve[" + getOrder + ", " + ("(" + Curve.round(getX0) + ", " + Curve.round(getY0) + "), ") +
      controlPointString + ("(" + Curve.round(getX1) + ", " + Curve.round(getY1) + "), ") +
      (if (direction == Curve.INCREASING) "D" else "U") + "]"

  def controlPointString: String = ""

  def getOrder: Int

  def getXTop: Double

  def getYTop: Double

  def getXBot: Double

  def getYBot: Double

  def getXMin: Double

  def getXMax: Double

  def getX0: Double

  def getY0: Double

  def getX1: Double

  def getY1: Double

  def XforY(y: Double): Double

  def TforY(y: Double): Double

  def XforT(t: Double): Double

  def YforT(t: Double): Double

  def dXforT(t: Double, deriv: Int): Double

  def dYforT(t: Double, deriv: Int): Double

  def nextVertical(t0: Double, t1: Double): Double

  def crossingsFor(x: Double, y: Double): Int =
    if ((y >= getYTop && y < getYBot) && (x < getXMax && (x < getXMin || x < XforY(y)))) 1 else 0

  def accumulateCrossings(c: Crossings): Boolean = {
    val xhi = c.getXHi
    if (getXMin >= xhi) return false
    val xlo = c.getXLo
    val ylo = c.getYLo
    val yhi = c.getYHi
    val y0 = getYTop
    val y1 = getYBot
    var tstart = .0
    var ystart = .0
    var tend = .0
    var yend = .0
    if (y0 < ylo) {
      if (y1 <= ylo) return false
      ystart = ylo
      tstart = TforY(ylo)
    }
    else {
      if (y0 >= yhi) return false
      ystart = y0
      tstart = 0
    }
    if (y1 > yhi) {
      yend = yhi
      tend = TforY(yhi)
    }
    else {
      yend = y1
      tend = 1
    }
    var hitLo = false
    var hitHi = false
    while (true) {
      val x = XforT(tstart)
      if (x < xhi) {
        if (hitHi || x > xlo) return true
        hitLo = true
      }
      else {
        if (hitLo) return true
        hitHi = true
      }
      if (tstart >= tend) {
//        break //todo: break is not supported
        if (hitLo) c.record(ystart, yend, direction)
        return false
      }
      tstart = nextVertical(tstart, tend)
    }
//    if (hitLo) c.record(ystart, yend, direction)
//    false
    throw new Exception("Never here")
  }

  def enlarge(r: Rectangle2D): Unit

  def getSubCurve(ystart: Double, yend: Double): Curve = getSubCurve(ystart, yend, direction)

  def getReversedCurve: Curve

  def getSubCurve(ystart: Double, yend: Double, dir: Int): Curve

  def compareTo(that: Curve, yrange: Array[Double]): Int = {
    /*
           System.out.println(this+".compareTo("+that+")");
           System.out.println("target range = "+yrange[0]+"=>"+yrange[1]);
           */
    val y0 = yrange(0)
    var y1 = yrange(1)
    y1 = Math.min(Math.min(y1, this.getYBot), that.getYBot)
    if (y1 <= yrange(0)) {
      System.err.println("this == " + this)
      System.err.println("that == " + that)
      System.out.println("target range = " + yrange(0) + "=>" + yrange(1))
      throw new InternalError("backstepping from " + yrange(0) + " to " + y1)
    }
    yrange(1) = y1
    if (this.getXMax <= that.getXMin) {
      if (this.getXMin == that.getXMax) return 0
      return -1
    }
    if (this.getXMin >= that.getXMax) return 1
    // Parameter s for thi(s) curve and t for tha(t) curve
    // [st]0 = parameters for top of current section of interest
    // [st]1 = parameters for bottom of valid range
    // [st]h = parameters for hypothesis point
    // [d][xy]s = valuations of thi(s) curve at sh
    // [d][xy]t = valuations of tha(t) curve at th
    var s0 = this.TforY(y0)
    var ys0 = this.YforT(s0)
    if (ys0 < y0) {
      s0 = refineTforY(s0, ys0, y0)
      ys0 = this.YforT(s0)
    }
    var s1 = this.TforY(y1)
    if (this.YforT(s1) < y0) {
      s1 = refineTforY(s1, this.YforT(s1), y0)
      //System.out.println("s1 problem!");
    }
    var t0 = that.TforY(y0)
    var yt0 = that.YforT(t0)
    if (yt0 < y0) {
      t0 = that.refineTforY(t0, yt0, y0)
      yt0 = that.YforT(t0)
    }
    var t1 = that.TforY(y1)
    if (that.YforT(t1) < y0) {
      t1 = that.refineTforY(t1, that.YforT(t1), y0)
      //System.out.println("t1 problem!");
    }
    var xs0 = this.XforT(s0)
    var xt0 = that.XforT(t0)
    val scale = Math.max(Math.abs(y0), Math.abs(y1))
    val ymin = Math.max(scale * 1E-14, 1E-300)
    if (fairlyClose(xs0, xt0)) {
      var bump = ymin
      val maxbump = Math.min(ymin * 1E13, (y1 - y0) * .1)
      var y = y0 + bump
      var break1 = false
      while (!break1 && y <= y1) {
        if (fairlyClose(this.XforY(y), that.XforY(y))) {
          bump *= 2
          if (bump > maxbump) bump = maxbump
          y += bump
        } else {
          y -= bump
          var break2 = false
          while (!break2) {
            bump /= 2
            val newy = y + bump
            if (newy <= y) {
              break2 = true
              //todo: break is not supported
            } else {
              if (fairlyClose(this.XforY(newy), that.XforY(newy))) y = newy
            }
          }
          break1 = true //todo: break is not supported
        }
      }
      if (y > y0) {
        if (y < y1) yrange(1) = y
        return 0
      }
    }
    //double ymin = y1 * 1E-14;
    if (ymin <= 0) System.out.println("ymin = " + ymin)
    /*
            System.out.println("s range = "+s0+" to "+s1);
            System.out.println("t range = "+t0+" to "+t1);
            */
    var break3 = false
    while (!break3 && (s0 < s1 && t0 < t1)) {
      val sh = this.nextVertical(s0, s1)
      val xsh = this.XforT(sh)
      val ysh = this.YforT(sh)
      val th = that.nextVertical(t0, t1)
      val xth = that.XforT(th)
      val yth = that.YforT(th)
      /*
                  System.out.println("sh = "+sh);
                  System.out.println("th = "+th);
                  */
      try {
        if (findIntersect(that, yrange, ymin, 0, 0, s0, xs0, ys0, sh, xsh, ysh, t0, xt0, yt0, th, xth, yth)) {
          break3 = true //todo: break is not supported
        }
      }
      catch {
        case t: Throwable =>
          System.err.println("Error: " + t)
          System.err.println("y range was " + yrange(0) + "=>" + yrange(1))
          System.err.println("s y range is " + ys0 + "=>" + ysh)
          System.err.println("t y range is " + yt0 + "=>" + yth)
          System.err.println("ymin is " + ymin)
          return 0
      }
      if (!break3) {
        if (ysh < yth) {
          if (ysh > yrange(0)) {
            if (ysh < yrange(1)) yrange(1) = ysh
            break3 = true //todo: break is not supported
          } else {
            s0 = sh
            xs0 = xsh
            ys0 = ysh
          }

        } else {
          if (yth > yrange(0)) {
            if (yth < yrange(1)) yrange(1) = yth
            break3 = true //todo: break is not supported
          } else {
            t0 = th
            xt0 = xth
            yt0 = yth
          }
        }
      }
    }
    val ymid = (yrange(0) + yrange(1)) / 2
    /*
            System.out.println("final this["+s0+", "+sh+", "+s1+"]");
            System.out.println("final    y["+ys0+", "+ysh+"]");
            System.out.println("final that["+t0+", "+th+", "+t1+"]");
            System.out.println("final    y["+yt0+", "+yth+"]");
            System.out.println("final order = "+orderof(this.XforY(ymid),
                                                        that.XforY(ymid)));
            System.out.println("final range = "+yrange[0]+"=>"+yrange[1]);
            *//*
            System.out.println("final sx = "+this.XforY(ymid));
            System.out.println("final tx = "+that.XforY(ymid));
            System.out.println("final order = "+orderof(this.XforY(ymid),
                                                        that.XforY(ymid)));
            */
    Curve.orderOf(this.XforY(ymid), that.XforY(ymid))
  }

  def findIntersect(that: Curve, yrange: Array[Double], ymin: Double, slevel: Int, tlevel: Int,
                    s0: Double, xs0: Double, ys0: Double, s1: Double, xs1: Double, ys1: Double,
                    t0: Double, xt0: Double, yt0: Double, t1: Double, xt1: Double, yt1: Double): Boolean = {
    /*
           String pad = "        ";
           pad = pad+pad+pad+pad+pad;
           pad = pad+pad;
           System.out.println("----------------------------------------------");
           System.out.println(pad.substring(0, slevel)+ys0);
           System.out.println(pad.substring(0, slevel)+ys1);
           System.out.println(pad.substring(0, slevel)+(s1-s0));
           System.out.println("-------");
           System.out.println(pad.substring(0, tlevel)+yt0);
           System.out.println(pad.substring(0, tlevel)+yt1);
           System.out.println(pad.substring(0, tlevel)+(t1-t0));
           */
    if (ys0 > yt1 || yt0 > ys1) return false
    if (Math.min(xs0, xs1) > Math.max(xt0, xt1) || Math.max(xs0, xs1) < Math.min(xt0, xt1)) return false
    // Bounding boxes intersect - back off the larger of
    // the two subcurves by half until they stop intersecting
    // (or until they get small enough to switch to a more
    //  intensive algorithm).
    if (s1 - s0 > Curve.T_MIN) {
      val s = (s0 + s1) / 2
      val xs = this.XforT(s)
      val ys = this.YforT(s)
      if (s == s0 || s == s1) {
        System.out.println("s0 = " + s0)
        System.out.println("s1 = " + s1)
        throw new InternalError("no s progress!")
      }
      if (t1 - t0 > Curve.T_MIN) {
        val t = (t0 + t1) / 2
        val xt = that.XforT(t)
        val yt = that.YforT(t)
        if (t == t0 || t == t1) {
          System.out.println("t0 = " + t0)
          System.out.println("t1 = " + t1)
          throw new InternalError("no t progress!")
        }
        if (ys >= yt0 && yt >= ys0) if (findIntersect(that, yrange, ymin, slevel + 1, tlevel + 1, s0, xs0, ys0, s, xs, ys, t0, xt0, yt0, t, xt, yt)) return true
        if (ys >= yt) if (findIntersect(that, yrange, ymin, slevel + 1, tlevel + 1, s0, xs0, ys0, s, xs, ys, t, xt, yt, t1, xt1, yt1)) return true
        if (yt >= ys) if (findIntersect(that, yrange, ymin, slevel + 1, tlevel + 1, s, xs, ys, s1, xs1, ys1, t0, xt0, yt0, t, xt, yt)) return true
        if (ys1 >= yt && yt1 >= ys) if (findIntersect(that, yrange, ymin, slevel + 1, tlevel + 1, s, xs, ys, s1, xs1, ys1, t, xt, yt, t1, xt1, yt1)) return true
      }
      else {
        if (ys >= yt0) if (findIntersect(that, yrange, ymin, slevel + 1, tlevel, s0, xs0, ys0, s, xs, ys, t0, xt0, yt0, t1, xt1, yt1)) return true
        if (yt1 >= ys) if (findIntersect(that, yrange, ymin, slevel + 1, tlevel, s, xs, ys, s1, xs1, ys1, t0, xt0, yt0, t1, xt1, yt1)) return true
      }
    }
    else if (t1 - t0 > Curve.T_MIN) {
      val t = (t0 + t1) / 2
      val xt = that.XforT(t)
      val yt = that.YforT(t)
      if (t == t0 || t == t1) {
        System.out.println("t0 = " + t0)
        System.out.println("t1 = " + t1)
        throw new InternalError("no t progress!")
      }
      if (yt >= ys0) if (findIntersect(that, yrange, ymin, slevel, tlevel + 1, s0, xs0, ys0, s1, xs1, ys1, t0, xt0, yt0, t, xt, yt)) return true
      if (ys1 >= yt) if (findIntersect(that, yrange, ymin, slevel, tlevel + 1, s0, xs0, ys0, s1, xs1, ys1, t, xt, yt, t1, xt1, yt1)) return true
    }
    else { // No more subdivisions
      val xlk = xs1 - xs0
      val ylk = ys1 - ys0
      val xnm = xt1 - xt0
      val ynm = yt1 - yt0
      val xmk = xt0 - xs0
      val ymk = yt0 - ys0
      val det = xnm * ylk - ynm * xlk
      if (det != 0) {
        val detinv = 1 / det
        var s = (xnm * ymk - ynm * xmk) * detinv
        var t = (xlk * ymk - ylk * xmk) * detinv
        if (s >= 0 && s <= 1 && t >= 0 && t <= 1) {
          s = s0 + s * (s1 - s0)
          t = t0 + t * (t1 - t0)
          if (s < 0 || s > 1 || t < 0 || t > 1) System.out.println("Uh oh!")
          val y = (this.YforT(s) + that.YforT(t)) / 2
          if (y <= yrange(1) && y > yrange(0)) {
            yrange(1) = y
            return true
          }
        }
      }
      //System.out.println("Testing lines!");
    }
    false
  }

  def refineTforY(t0: Double, yt0: Double, y0: Double): Double = {
    var _t0 = t0
    var _yt0 = yt0
    var t1 = 1d
    while (true) {
      val th = (_t0 + t1) / 2
      if (th == _t0 || th == t1) return t1
      val y = YforT(th)
      if (y < y0) {
        _t0 = th
        _yt0 = y
      }
      else if (y > y0) t1 = th
      else return t1
    }

    throw new Exception("Never here")
  }

  def fairlyClose(v1: Double, v2: Double): Boolean =
    Math.abs(v1 - v2) < Math.max(Math.abs(v1), Math.abs(v2)) * 1E-10

  def getSegment(coords: Array[Double]): Int
}
