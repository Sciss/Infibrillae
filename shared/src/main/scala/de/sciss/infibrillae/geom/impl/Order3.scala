package de.sciss.infibrillae.geom.impl

// This is an adapted Scala translation of the Order3 Java class of OpenJDK
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

import de.sciss.infibrillae.geom.impl.Curve.{DECREASING, INCREASING, diffBits, next, prev, round}
import de.sciss.infibrillae.geom.{PathIterator, QuadCurve2D, Rectangle2D}

import scala.collection.mutable

object Order3 {
  def insert(curves: mutable.Growable[Curve], tmp: Array[Double], x0: Double, y0: Double,
             cx0: Double, cy0: Double, cx1: Double, cy1: Double, x1: Double, y1: Double, direction: Int): Unit = {
    var numParams = getHorizontalParams(y0, cy0, cy1, y1, tmp)
    if (numParams == 0) { // We are using addInstance here to avoid inserting horisontal
      // segments
      addInstance(curves, x0, y0, cx0, cy0, cx1, cy1, x1, y1, direction)
      return
    }
    // Store coordinates for splitting at tmp[3..10]
    tmp(3) = x0
    tmp(4) = y0
    tmp(5) = cx0
    tmp(6) = cy0
    tmp(7) = cx1
    tmp(8) = cy1
    tmp(9) = x1
    tmp(10) = y1
    var t = tmp(0)
    if (numParams > 1 && t > tmp(1)) { // Perform a "2 element sort"...
      tmp(0) = tmp(1)
      tmp(1) = t
      t = tmp(0)
    }
    split(tmp, 3, t)
    if (numParams > 1) { // Recalculate tmp[1] relative to the range [tmp[0]...1]
      t = (tmp(1) - t) / (1 - t)
      split(tmp, 9, t)
    }
    var index = 3
    if (direction == DECREASING) index += numParams * 6
    while ( {
      numParams >= 0
    }) {
      addInstance(curves, tmp(index + 0), tmp(index + 1), tmp(index + 2), tmp(index + 3),
        tmp(index + 4), tmp(index + 5), tmp(index + 6), tmp(index + 7), direction)
      numParams -= 1
      if (direction == INCREASING) index += 6
      else index -= 6
    }
  }

  def addInstance(curves: mutable.Growable[Curve], x0: Double, y0: Double, cx0: Double, cy0: Double,
                  cx1: Double, cy1: Double, x1: Double, y1: Double, direction: Int): Unit = {
    if      (y0 > y1) curves.addOne(new Order3(x1, y1, cx1, cy1, cx0, cy0, x0, y0, -direction))
    else if (y1 > y0) curves.addOne(new Order3(x0, y0, cx0, cy0, cx1, cy1, x1, y1,  direction))
  }

  /*
       * Return the count of the number of horizontal sections of the
       * specified cubic Bezier curve.  Put the parameters for the
       * horizontal sections into the specified {@code ret} array.
       * <p>
       * If we examine the parametric equation in t, we have:
       *   Py(t) = C0(1-t)^3 + 3CP0 t(1-t)^2 + 3CP1 t^2(1-t) + C1 t^3
       *         = C0 - 3C0t + 3C0t^2 - C0t^3 +
       *           3CP0t - 6CP0t^2 + 3CP0t^3 +
       *           3CP1t^2 - 3CP1t^3 +
       *           C1t^3
       *   Py(t) = (C1 - 3CP1 + 3CP0 - C0) t^3 +
       *           (3C0 - 6CP0 + 3CP1) t^2 +
       *           (3CP0 - 3C0) t +
       *           (C0)
       * If we take the derivative, we get:
       *   Py(t) = Dt^3 + At^2 + Bt + C
       *   dPy(t) = 3Dt^2 + 2At + B = 0
       *        0 = 3*(C1 - 3*CP1 + 3*CP0 - C0)t^2
       *          + 2*(3*CP1 - 6*CP0 + 3*C0)t
       *          + (3*CP0 - 3*C0)
       *        0 = 3*(C1 - 3*CP1 + 3*CP0 - C0)t^2
       *          + 3*2*(CP1 - 2*CP0 + C0)t
       *          + 3*(CP0 - C0)
       *        0 = (C1 - CP1 - CP1 - CP1 + CP0 + CP0 + CP0 - C0)t^2
       *          + 2*(CP1 - CP0 - CP0 + C0)t
       *          + (CP0 - C0)
       *        0 = (C1 - CP1 + CP0 - CP1 + CP0 - CP1 + CP0 - C0)t^2
       *          + 2*(CP1 - CP0 - CP0 + C0)t
       *          + (CP0 - C0)
       *        0 = ((C1 - CP1) - (CP1 - CP0) - (CP1 - CP0) + (CP0 - C0))t^2
       *          + 2*((CP1 - CP0) - (CP0 - C0))t
       *          + (CP0 - C0)
       * Note that this method will return 0 if the equation is a line,
       * which is either always horizontal or never horizontal.
       * Completely horizontal curves need to be eliminated by other
       * means outside of this method.
       */
  def getHorizontalParams(c0: Double, cp0: Double, cp1: Double, c1: Double, ret: Array[Double]): Int = {
    var _c1 = c1
    var _cp0 = cp0
    var _cp1 = cp1
    if (c0 <= _cp0 && _cp0 <= _cp1 && _cp1 <= _c1) return 0
    _c1 -= _cp1
    _cp1 -= _cp0
    _cp0 -= c0
    ret(0) = _cp0
    ret(1) = (_cp1 - _cp0) * 2
    ret(2) = _c1 - _cp1 - _cp1 + _cp0
    val numRoots = QuadCurve2D.solveQuadratic(ret, ret)
    var j = 0
    for (i <- 0 until numRoots) {
      val t = ret(i)
      // No splits at t==0 and t==1
      if (t > 0 && t < 1) {
        if (j < i) ret(j) = t
        j += 1
      }
    }
    j
  }

  /*
       * Split the cubic Bezier stored at coords[pos...pos+7] representing
       * the parametric range [0..1] into two subcurves representing the
       * parametric subranges [0..t] and [t..1].  Store the results back
       * into the array at coords[pos...pos+7] and coords[pos+6...pos+13].
       */
  def split(coords: Array[Double], pos: Int, t: Double): Unit = {
    var x0  = 0.0
    var y0  = 0.0
    var cx0 = 0.0
    var cy0 = 0.0
    var cx1 = 0.0
    var cy1 = 0.0
    var x1  = 0.0
    var y1  = 0.0
    x1 = coords(pos + 6)
    coords(pos + 12) = x1
    y1 = coords(pos + 7)
    coords(pos + 13) = y1
    cx1 = coords(pos + 4)
    cy1 = coords(pos + 5)
    x1 = cx1 + (x1 - cx1) * t
    y1 = cy1 + (y1 - cy1) * t
    x0 = coords(pos + 0)
    y0 = coords(pos + 1)
    cx0 = coords(pos + 2)
    cy0 = coords(pos + 3)
    x0 = x0 + (cx0 - x0) * t
    y0 = y0 + (cy0 - y0) * t
    cx0 = cx0 + (cx1 - cx0) * t
    cy0 = cy0 + (cy1 - cy0) * t
    cx1 = cx0 + (x1 - cx0) * t
    cy1 = cy0 + (y1 - cy0) * t
    cx0 = x0 + (cx0 - x0) * t
    cy0 = y0 + (cy0 - y0) * t
    coords(pos + 2) = x0
    coords(pos + 3) = y0
    coords(pos + 4) = cx0
    coords(pos + 5) = cy0
    coords(pos + 6) = cx0 + (cx1 - cx0) * t
    coords(pos + 7) = cy0 + (cy1 - cy0) * t
    coords(pos + 8) = cx1
    coords(pos + 9) = cy1
    coords(pos + 10) = x1
    coords(pos + 11) = y1
  }
}

final class Order3(x0: Double, y0: Double, cx0: Double, cy0: Double, cx1: Double, cy1: Double, x1: Double, y1: Double,
                   direction: Int) extends Curve(direction) { // REMIND: Better accuracy in the root finding methods would

  //  ensure that cys are in range.  As it stands, they are never
  //  more than "1 mantissa bit" out of range...
  private val _cy0 = if (cy0 < y0) y0 else cy0
  private val _cy1 = if (cy1 > y1) y1 else cy1

  private val xMin = Math.min(Math.min(x0, x1), Math.min(cx0, cx1))
  private val xMax = Math.max(Math.max(x0, x1), Math.max(cx0, cx1))

  private val xCoeff0 = x0
  private val xCoeff1 = (cx0 - x0) * 3.0
  private val xCoeff2 = (cx1 - cx0 - cx0 + x0) * 3.0
  private val xCoeff3 = x1 - (cx1 - cx0) * 3.0 - x0
  private val yCoeff0 = y0
  private val yCoeff1 = (_cy0 - y0) * 3.0
  private val yCoeff2 = (_cy1 - _cy0 - _cy0 + y0) * 3.0
  private val yCoeff3 = y1 - (_cy1 - _cy0) * 3.0 - y0

  private var YforT1  = y0
  private var YforT2  = y0
  private var YforT3  = y0

  private var TforY1 = .0
  private var TforY2 = .0
  private var TforY3 = .0

  override def getOrder = 3

  override def getXTop: Double = x0
  override def getYTop: Double = y0

  override def getXBot: Double = x1
  override def getYBot: Double = y1

  override def getXMin: Double = xMin
  override def getXMax: Double = xMax

  override def getX0: Double = if (direction == INCREASING) x0 else x1
  override def getY0: Double = if (direction == INCREASING) y0 else y1

  def getCX0: Double = if (direction == INCREASING) cx0 else cx1
  def getCY0: Double = if (direction == INCREASING) _cy0 else _cy1

  def getCX1: Double = if (direction == DECREASING) cx0 else cx1
  def getCY1: Double = if (direction == DECREASING) _cy0 else _cy1

  override def getX1: Double = if (direction == DECREASING) x0 else x1
  override def getY1: Double = if (direction == DECREASING) y0 else y1

  /*
       * Solve the cubic whose coefficients are in the a,b,c,d fields and
       * return the first root in the range [0, 1].
       * The cubic solved is represented by the equation:
       *     x^3 + (ycoeff2)x^2 + (ycoeff1)x + (ycoeff0) = y
       * @return the first valid root (in the range [0, 1])
       */
  override def TforY(y: Double): Double = {
    if (y <= y0) return 0
    if (y >= y1) return 1
    if (y == YforT1) return TforY1
    if (y == YforT2) return TforY2
    if (y == YforT3) return TforY3
    // From Numerical Recipes, 5.6, Quadratic and Cubic Equations
    if (yCoeff3 == 0.0) { // The cubic degenerated to quadratic (or line or ...).
      return Order2.TforY(y, yCoeff0, yCoeff1, yCoeff2)
    }
    val a = yCoeff2 / yCoeff3
    val b = yCoeff1 / yCoeff3
    val c = (yCoeff0 - y) / yCoeff3
//    val roots = 0
    var Q = (a * a - 3.0 * b) / 9.0
    var R = (2.0 * a * a * a - 9.0 * a * b + 27.0 * c) / 54.0
    val R2 = R * R
    val Q3 = Q * Q * Q
    val a_3 = a / 3.0
    var t = .0
    if (R2 < Q3) {
      val theta = Math.acos(R / Math.sqrt(Q3))
      Q = -2.0 * Math.sqrt(Q)
      t = refine(a, b, c, y, Q * Math.cos(theta / 3.0) - a_3)
      if (t < 0) t = refine(a, b, c, y, Q * Math.cos((theta + Math.PI * 2.0) / 3.0) - a_3)
      if (t < 0) t = refine(a, b, c, y, Q * Math.cos((theta - Math.PI * 2.0) / 3.0) - a_3)
    }
    else {
      val neg = R < 0.0
      val S = Math.sqrt(R2 - Q3)
      if (neg) R = -R
      var A = Math.pow(R + S, 1.0 / 3.0)
      if (!neg) A = -A
      val B = if (A == 0.0) 0.0
      else Q / A
      t = refine(a, b, c, y, (A + B) - a_3)
    }
    if (t < 0) { //throw new InternalError("bad t");
      var t0 = 0d
      var t1 = 1d
      var break1 = false
      while (!break1) {
        t = (t0 + t1) / 2
        if (t == t0 || t == t1) {
          break1 = true //todo: break is not supported
        } else {
          val yt = YforT(t)
          if (yt < y) t0 = t
          else if (yt > y) t1 = t
          else {
            break1 = true //todo: break is not supported
          }
        }
      }
    }
    if (t >= 0) {
      TforY3 = TforY2
      YforT3 = YforT2
      TforY2 = TforY1
      YforT2 = YforT1
      TforY1 = t
      YforT1 = y
    }
    t
  }

  def refine(a: Double, b: Double, c: Double, target: Double, t: Double): Double = {
    var _t = t
    if (_t < -0.1 || _t > 1.1) return -1
    var y = YforT(_t)
    var t0 = 0.0
    var t1 = 0.0
    if (y < target) {
      t0 = _t
      t1 = 1
    } else {
      t0 = 0
      t1 = _t
    }
    val origT     = _t
    val origY     = y
    var useSlope  = true
    var break1    = false
    while (!break1 && (y != target)) {
      var continue1 = false
      if (!useSlope) {
        val t2 = (t0 + t1) / 2
        if (t2 == t0 || t2 == t1) {
          break1 = true //todo: break is not supported
        } else {
          _t = t2
        }
      } else {
        val slope = dYforT(_t, 1)
        if (slope == 0) {
          useSlope = false
          continue1 = true //todo: continue is not supported
        } else {
          val t2 = _t + ((target - y) / slope)
          if (t2 == _t || t2 <= t0 || t2 >= t1) {
            useSlope = false
            continue1 = true //todo: continue is not supported
          } else {
            _t = t2
          }
        }
      }
      if (!break1 && !continue1) {
        y = YforT(_t)
        if (y < target) t0 = _t
        else if (y > target) t1 = _t
        else {
          break1 = true //todo: break is not supported
        }
      }
    }

    val verbose = false
    if (false && _t >= 0 && _t <= 1) {
      y = YforT(_t)
      val tdiff = diffBits(_t, origT)
      val ydiff = diffBits(y, origY)
      val yerr  = diffBits(y, target)
      if (yerr > 0 || (verbose && tdiff > 0)) {
        System.out.println("target was y = " + target)
        System.out.println("original was y = " + origY + ", t = " + origT)
        System.out.println("final was y = " + y + ", t = " + _t)
        System.out.println("t diff is " + tdiff)
        System.out.println("y diff is " + ydiff)
        System.out.println("y error is " + yerr)
        val tlow = prev(_t)
        val ylow = YforT(tlow)
        val thi = next(_t)
        val yhi = YforT(thi)
        if (Math.abs(target - ylow) < Math.abs(target - y) || Math.abs(target - yhi) < Math.abs(target - y))
          System.out.println("adjacent y's = [" + ylow + ", " + yhi + "]")
      }
    }
    if (_t > 1) -1 else _t
  }

  override def XforY(y: Double): Double = {
    if (y <= y0) return x0
    if (y >= y1) return x1
    XforT(TforY(y))
  }

  override def XforT(t: Double): Double = (((xCoeff3 * t) + xCoeff2) * t + xCoeff1) * t + xCoeff0
  override def YforT(t: Double): Double = (((yCoeff3 * t) + yCoeff2) * t + yCoeff1) * t + yCoeff0

  override def dXforT(t: Double, deriv: Int): Double = deriv match {
    case 0 =>
      (((xCoeff3 * t) + xCoeff2) * t + xCoeff1) * t + xCoeff0
    case 1 =>
      ((3 * xCoeff3 * t) + 2 * xCoeff2) * t + xCoeff1
    case 2 =>
      (6 * xCoeff3 * t) + 2 * xCoeff2
    case 3 =>
      6 * xCoeff3
    case _ =>
      0
  }

  override def dYforT(t: Double, deriv: Int): Double = deriv match {
    case 0 =>
      (((yCoeff3 * t) + yCoeff2) * t + yCoeff1) * t + yCoeff0
    case 1 =>
      ((3 * yCoeff3 * t) + 2 * yCoeff2) * t + yCoeff1
    case 2 =>
      (6 * yCoeff3 * t) + 2 * yCoeff2
    case 3 =>
      6 * yCoeff3
    case _ =>
      0
  }

  override def nextVertical(t0: Double, t1: Double): Double = {
    var _t1 = t1
    val eqn = Array(xCoeff1, 2 * xCoeff2, 3 * xCoeff3)
    val numroots = QuadCurve2D.solveQuadratic(eqn, eqn)
    for (i <- 0 until numroots) {
      if (eqn(i) > t0 && eqn(i) < _t1) _t1 = eqn(i)
    }
    _t1
  }

  override def enlarge(r: Rectangle2D): Unit = {
    r.add(x0, y0)
    val eqn       = Array(xCoeff1, 2 * xCoeff2, 3 * xCoeff3)
    val numRoots  = QuadCurve2D.solveQuadratic(eqn, eqn)
    for (i <- 0 until numRoots) {
      val t = eqn(i)
      if (t > 0 && t < 1) r.add(XforT(t), YforT(t))
    }
    r.add(x1, y1)
  }

  override def getSubCurve(yStart: Double, yEnd: Double, dir: Int): Curve = {
    if (yStart <= y0 && yEnd >= y1) return getWithDirection(dir)
    val eqn = new Array[Double](14)
    var t0 = 0.0
    var t1 = 0.0
    t0 = TforY(yStart)
    t1 = TforY(yEnd)
    eqn(0) = x0
    eqn(1) = y0
    eqn(2) = cx0
    eqn(3) = _cy0
    eqn(4) = cx1
    eqn(5) = _cy1
    eqn(6) = x1
    eqn(7) = y1
    if (t0 > t1) {
      /* This happens in only rare cases where ystart is
                  * very near yend and solving for the yend root ends
                  * up stepping slightly lower in t than solving for
                  * the ystart root.
                  * Ideally we might want to skip this tiny little
                  * segment and just fudge the surrounding coordinates
                  * to bridge the gap left behind, but there is no way
                  * to do that from here.  Higher levels could
                  * potentially eliminate these tiny "fixup" segments,
                  * but not without a lot of extra work on the code that
                  * coalesces chains of curves into subpaths.  The
                  * simplest solution for now is to just reorder the t
                  * values and chop out a miniscule curve piece.
                  */
      val t = t0
      t0 = t1
      t1 = t
    }
    if (t1 < 1) Order3.split(eqn, 0, t1)
    var i = 0
    if (t0 <= 0) i = 0
    else {
      Order3.split(eqn, 0, t0 / t1)
      i = 6
    }
    new Order3(eqn(i + 0), yStart, eqn(i + 2), eqn(i + 3), eqn(i + 4), eqn(i + 5), eqn(i + 6), yEnd, dir)
  }

  override def getReversedCurve = new Order3(x0, y0, cx0, _cy0, cx1, _cy1, x1, y1, -direction)

  override def getSegment(coords: Array[Double]): Int = {
    if (direction == INCREASING) {
      coords(0) = cx0
      coords(1) = _cy0
      coords(2) = cx1
      coords(3) = _cy1
      coords(4) = x1
      coords(5) = y1
    } else {
      coords(0) = cx1
      coords(1) = _cy1
      coords(2) = cx0
      coords(3) = _cy0
      coords(4) = x0
      coords(5) = y0
    }
    PathIterator.SEG_CUBICTO
  }

  override def controlPointString: String = ("(" + round(getCX0) + ", " + round(getCY0) + "), ") +
    ("(" + round(getCX1) + ", " + round(getCY1) + "), ")
}
