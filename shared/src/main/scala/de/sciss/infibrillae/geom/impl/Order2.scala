package de.sciss.infibrillae.geom.impl

// This is an adapted Scala translation of the Order2 Java class of OpenJDK
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

import de.sciss.infibrillae.geom.impl.Curve.{DECREASING, INCREASING, round}
import de.sciss.infibrillae.geom.{PathIterator, Rectangle2D}

import scala.collection.mutable

object Order2 {
  def insert(curves: mutable.Growable[Curve], tmp: Array[Double], x0: Double, y0: Double, cx0: Double, cy0: Double,
             x1: Double, y1: Double, direction: Int): Unit = {
    val numParams = getHorizontalParams(y0, cy0, y1, tmp)
    if (numParams == 0) { // We are using addInstance here to avoid inserting horisontal
      // segments
      addInstance(curves, x0, y0, cx0, cy0, x1, y1, direction)
      return
    }
    // assert(numparams == 1);
    val t = tmp(0)
    tmp(0) = x0
    tmp(1) = y0
    tmp(2) = cx0
    tmp(3) = cy0
    tmp(4) = x1
    tmp(5) = y1
    split(tmp, 0, t)
    val i0 = if (direction == INCREASING) 0
    else 4
    val i1 = 4 - i0
    addInstance(curves, tmp(i0), tmp(i0 + 1), tmp(i0 + 2), tmp(i0 + 3), tmp(i0 + 4), tmp(i0 + 5), direction)
    addInstance(curves, tmp(i1), tmp(i1 + 1), tmp(i1 + 2), tmp(i1 + 3), tmp(i1 + 4), tmp(i1 + 5), direction)
  }

  def addInstance(curves: mutable.Growable[Curve], x0: Double, y0: Double, cx0: Double, cy0: Double,
                  x1: Double, y1: Double, direction: Int): Unit = {
    if      (y0 > y1) curves.addOne(new Order2(x1, y1, cx0, cy0, x0, y0, -direction))
    else if (y1 > y0) curves.addOne(new Order2(x0, y0, cx0, cy0, x1, y1, direction))
  }

  /*
       * Return the count of the number of horizontal sections of the
       * specified quadratic Bezier curve.  Put the parameters for the
       * horizontal sections into the specified {@code ret} array.
       * <p>
       * If we examine the parametric equation in t, we have:
       *     Py(t) = C0*(1-t)^2 + 2*CP*t*(1-t) + C1*t^2
       *           = C0 - 2*C0*t + C0*t^2 + 2*CP*t - 2*CP*t^2 + C1*t^2
       *           = C0 + (2*CP - 2*C0)*t + (C0 - 2*CP + C1)*t^2
       *     Py(t) = (C0 - 2*CP + C1)*t^2 + (2*CP - 2*C0)*t + (C0)
       * If we take the derivative, we get:
       *     Py(t) = At^2 + Bt + C
       *     dPy(t) = 2At + B = 0
       *     2*(C0 - 2*CP + C1)t + 2*(CP - C0) = 0
       *     2*(C0 - 2*CP + C1)t = 2*(C0 - CP)
       *     t = 2*(C0 - CP) / 2*(C0 - 2*CP + C1)
       *     t = (C0 - CP) / (C0 - CP + C1 - CP)
       * Note that this method will return 0 if the equation is a line,
       * which is either always horizontal or never horizontal.
       * Completely horizontal curves need to be eliminated by other
       * means outside of this method.
       */
  def getHorizontalParams(c0: Double, cp: Double, c1: Double, ret: Array[Double]): Int = {
    var _c0 = c0
    var _c1 = c1
    if (_c0 <= cp && cp <= _c1) return 0
    _c0 -= cp
    _c1 -= cp
    val denom = _c0 + _c1
    // If denom == 0 then cp == (c0+c1)/2 and we have a line.
    if (denom == 0) return 0
    val t = _c0 / denom
    // No splits at t==0 and t==1
    if (t <= 0 || t >= 1) return 0
    ret(0) = t
    1
  }

  /*
       * Split the quadratic Bezier stored at coords[pos...pos+5] representing
       * the paramtric range [0..1] into two subcurves representing the
       * parametric subranges [0..t] and [t..1].  Store the results back
       * into the array at coords[pos...pos+5] and coords[pos+4...pos+9].
       */ def split(coords: Array[Double], pos: Int, t: Double): Unit = {
    var x0 = .0
    var y0 = .0
    var cx = .0
    var cy = .0
    var x1 = .0
    var y1 = .0
    x1 = coords(pos + 4)
    coords(pos + 8) = x1
    y1 = coords(pos + 5)
    coords(pos + 9) = y1
    cx = coords(pos + 2)
    cy = coords(pos + 3)
    x1 = cx + (x1 - cx) * t
    y1 = cy + (y1 - cy) * t
    x0 = coords(pos + 0)
    y0 = coords(pos + 1)
    x0 = x0 + (cx - x0) * t
    y0 = y0 + (cy - y0) * t
    cx = x0 + (x1 - x0) * t
    cy = y0 + (y1 - y0) * t
    coords(pos + 2) = x0
    coords(pos + 3) = y0
    coords(pos + 4) = cx
    coords(pos + 5) = cy
    coords(pos + 6) = x1
    coords(pos + 7) = y1
  }

  def TforY(y: Double, ycoeff0: Double, ycoeff1: Double, ycoeff2: Double): Double = { // The caller should have already eliminated y values
    var _ycoeff0 = ycoeff0
    // outside of the y0 to y1 range.
    _ycoeff0 -= y
    if (ycoeff2 == 0.0) { // The quadratic parabola has degenerated to a line.
      // ycoeff1 should not be 0.0 since we have already eliminated
      // totally horizontal lines, but if it is, then we will generate
      // infinity here for the root, which will not be in the [0,1]
      // range so we will pass to the failure code below.
      val root = -_ycoeff0 / ycoeff1
      if (root >= 0 && root <= 1) return root
    }
    else { // From Numerical Recipes, 5.6, Quadratic and Cubic Equations
      var d = ycoeff1 * ycoeff1 - 4.0 * ycoeff2 * _ycoeff0
      // If d < 0.0, then there are no roots
      if (d >= 0.0) {
        d = Math.sqrt(d)
        // For accuracy, calculate one root using:
        //     (-ycoeff1 +/- d) / 2ycoeff2
        // and the other using:
        //     2ycoeff0 / (-ycoeff1 +/- d)
        // Choose the sign of the +/- so that ycoeff1+d
        // gets larger in magnitude
        if (ycoeff1 < 0.0) d = -d
        val q = (ycoeff1 + d) / -2.0
        // We already tested ycoeff2 for being 0 above
        var root = q / ycoeff2
        if (root >= 0 && root <= 1) return root
        if (q != 0.0) {
          root = _ycoeff0 / q
          if (root >= 0 && root <= 1) return root
        }
      }
    }
    /* We failed to find a root in [0,1].  What could have gone wrong?
             * First, remember that these curves are constructed to be monotonic
             * in Y and totally horizontal curves have already been eliminated.
             * Now keep in mind that the Y coefficients of the polynomial form
             * of the curve are calculated from the Y coordinates which define
             * our curve.  They should theoretically define the same curve,
             * but they can be off by a couple of bits of precision after the
             * math is done and so can represent a slightly modified curve.
             * This is normally not an issue except when we have solutions near
             * the endpoints.  Since the answers we get from solving the polynomial
             * may be off by a few bits that means that they could lie just a
             * few bits of precision outside the [0,1] range.
             *
             * Another problem could be that while the parametric curve defined
             * by the Y coordinates has a local minima or maxima at or just
             * outside of the endpoints, the polynomial form might express
             * that same min/max just inside of and just shy of the Y coordinate
             * of that endpoint.  In that case, if we solve for a Y coordinate
             * at or near that endpoint, we may be solving for a Y coordinate
             * that is below that minima or above that maxima and we would find
             * no solutions at all.
             *
             * In either case, we can assume that y is so near one of the
             * endpoints that we can just collapse it onto the nearest endpoint
             * without losing more than a couple of bits of precision.
             */
    // First calculate the midpoint between y0 and y1 and choose to
    // return either 0.0 or 1.0 depending on whether y is above
    // or below the midpoint...
    // Note that we subtracted y from ycoeff0 above so both y0 and y1
    // will be "relative to y" so we are really just looking at where
    // zero falls with respect to the "relative midpoint" here.
    val y0 = _ycoeff0
    val y1 = _ycoeff0 + ycoeff1 + ycoeff2
    if (0 < (y0 + y1) / 2) 0.0
    else 1.0
  }
}

final class Order2(x0: Double, y0: Double, cx0: Double, cy0: Double,
                   x1: Double, y1: Double, direction: Int)
  extends Curve(direction) { // REMIND: Better accuracy in the root finding methods would

  //  ensure that cy0 is in range.  As it stands, it is never
  //  more than "1 mantissa bit" out of range...
  private val _cy0    = if (cy0 < y0) y0 else if (cy0 > y1) y1 else cy0
  private val xmin    = Math.min(Math.min(x0, x1), cx0)
  private val xmax    = Math.max(Math.max(x0, x1), cx0)
  private val xcoeff0 = x0
  private val xcoeff1 = cx0 + cx0 - x0 - x0
  private val xcoeff2 = x0 - cx0 - cx0 + x1
  private val ycoeff0 = y0
  private val ycoeff1 = _cy0 + _cy0 - y0 - y0
  private val ycoeff2 = y0 - _cy0 - _cy0 + y1

  override def getOrder = 2

  override def getXTop: Double = x0
  override def getYTop: Double = y0

  override def getXBot: Double = x1
  override def getYBot: Double = y1

  override def getXMin: Double = xmin
  override def getXMax: Double = xmax

  override def getX0: Double = if (direction == INCREASING) x0 else x1
  override def getY0: Double = if (direction == INCREASING) y0 else y1

  def getCX0: Double = cx0
  def getCY0: Double = _cy0

  override def getX1: Double = if (direction == DECREASING) x0 else x1
  override def getY1: Double = if (direction == DECREASING) y0 else y1

  override def XforY(y: Double): Double = {
    if (y <= y0) return x0
    if (y >= y1) return x1
    XforT(TforY(y))
  }

  override def TforY(y: Double): Double = {
    if (y <= y0) return 0
    if (y >= y1) return 1
    Order2.TforY(y, ycoeff0, ycoeff1, ycoeff2)
  }

  override def XforT(t: Double): Double = (xcoeff2 * t + xcoeff1) * t + xcoeff0

  override def YforT(t: Double): Double = (ycoeff2 * t + ycoeff1) * t + ycoeff0

  override def dXforT(t: Double, deriv: Int): Double = deriv match {
    case 0 =>
      (xcoeff2 * t + xcoeff1) * t + xcoeff0
    case 1 =>
      2 * xcoeff2 * t + xcoeff1
    case 2 =>
      2 * xcoeff2
    case _ =>
      0
  }

  override def dYforT(t: Double, deriv: Int): Double = deriv match {
    case 0 =>
      (ycoeff2 * t + ycoeff1) * t + ycoeff0
    case 1 =>
      2 * ycoeff2 * t + ycoeff1
    case 2 =>
      2 * ycoeff2
    case _ =>
      0
  }

  override def nextVertical(t0: Double, t1: Double): Double = {
    val t = -xcoeff1 / (2 * xcoeff2)
    if (t > t0 && t < t1) return t
    t1
  }

  override def enlarge(r: Rectangle2D): Unit = {
    r.add(x0, y0)
    val t = -xcoeff1 / (2 * xcoeff2)
    if (t > 0 && t < 1) r.add(XforT(t), YforT(t))
    r.add(x1, y1)
  }

  override def getSubCurve(ystart: Double, yend: Double, dir: Int): Curve = {
    var t0 = .0
    var t1 = .0
    if (ystart <= y0) {
      if (yend >= y1) return getWithDirection(dir)
      t0 = 0
    }
    else t0 = Order2.TforY(ystart, ycoeff0, ycoeff1, ycoeff2)
    if (yend >= y1) t1 = 1
    else t1 = Order2.TforY(yend, ycoeff0, ycoeff1, ycoeff2)
    val eqn = new Array[Double](10)
    eqn(0) = x0
    eqn(1) = y0
    eqn(2) = cx0
    eqn(3) = _cy0
    eqn(4) = x1
    eqn(5) = y1
    if (t1 < 1) Order2.split(eqn, 0, t1)
    var i = 0
    if (t0 <= 0) i = 0
    else {
      Order2.split(eqn, 0, t0 / t1)
      i = 4
    }
    new Order2(eqn(i + 0), ystart, eqn(i + 2), eqn(i + 3), eqn(i + 4), yend, dir)
  }

  override def getReversedCurve = new Order2(x0, y0, cx0, _cy0, x1, y1, -direction)

  override def getSegment(coords: Array[Double]): Int = {
    coords(0) = cx0
    coords(1) = _cy0
    if (direction == INCREASING) {
      coords(2) = x1
      coords(3) = y1
    } else {
      coords(2) = x0
      coords(3) = y0
    }
    PathIterator.SEG_QUADTO
  }

  override def controlPointString: String = "(" + round(cx0) + ", " + round(_cy0) + "), "
}
