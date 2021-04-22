package de.sciss.infibrillae.geom.impl

// This is an adapted Scala translation of the Order1 Java class of OpenJDK
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

import de.sciss.infibrillae.geom.{PathIterator, Rectangle2D}
import de.sciss.infibrillae.geom.impl.Curve.{DECREASING, INCREASING, orderof}

final class Order1(protected val x0: Double, protected val y0: Double,
                   protected val x1: Double, protected val y1: Double, direction: Int)
  extends Curve(direction) {

  private val xmin = if (x0 < x1) x0 else x1
  private val xmax = if (x0 < x1) x1 else x0

  override def getOrder = 1

  override def getXTop: Double = x0
  override def getYTop: Double = y0

  override def getXBot: Double = x1
  override def getYBot: Double = y1

  override def getXMin: Double = xmin
  override def getXMax: Double = xmax

  override def getX0: Double = if (direction == INCREASING) x0 else x1
  override def getY0: Double = if (direction == INCREASING) y0 else y1

  override def getX1: Double = if (direction == DECREASING) x0 else x1
  override def getY1: Double = if (direction == DECREASING) y0 else y1

  override def XforY(y: Double): Double = {
    if (x0 == x1 || y <= y0) return x0
    if (y >= y1) return x1
    // assert(y0 != y1); /* No horizontal lines... */
    x0 + (y - y0) * (x1 - x0) / (y1 - y0)
  }

  override def TforY(y: Double): Double = {
    if (y <= y0) return 0
    if (y >= y1) return 1
    (y - y0) / (y1 - y0)
  }

  override def XforT(t: Double): Double = x0 + t * (x1 - x0)

  override def YforT(t: Double): Double = y0 + t * (y1 - y0)

  override def dXforT(t: Double, deriv: Int): Double = deriv match {
    case 0 =>
      x0 + t * (x1 - x0)
    case 1 =>
      x1 - x0
    case _ =>
      0
  }

  override def dYforT(t: Double, deriv: Int): Double = deriv match {
    case 0 =>
      y0 + t * (y1 - y0)
    case 1 =>
      y1 - y0
    case _ =>
      0
  }

  override def nextVertical(t0: Double, t1: Double): Double = t1

  override def accumulateCrossings(c: Crossings): Boolean = {
    val xlo = c.getXLo
    val ylo = c.getYLo
    val xhi = c.getXHi
    val yhi = c.getYHi
    if (xmin >= xhi) return false
    var xstart = .0
    var ystart = .0
    var xend = .0
    var yend = .0
    if (y0 < ylo) {
      if (y1 <= ylo) return false
      ystart = ylo
      xstart = XforY(ylo)
    }
    else {
      if (y0 >= yhi) return false
      ystart = y0
      xstart = x0
    }
    if (y1 > yhi) {
      yend = yhi
      xend = XforY(yhi)
    }
    else {
      yend = y1
      xend = x1
    }
    if (xstart >= xhi && xend >= xhi) return false
    if (xstart > xlo || xend > xlo) return true
    c.record(ystart, yend, direction)
    false
  }

  override def enlarge(r: Rectangle2D): Unit = {
    r.add(x0, y0)
    r.add(x1, y1)
  }

  override def getSubCurve(ystart: Double, yend: Double, dir: Int): Curve = {
    if (ystart == y0 && yend == y1) return getWithDirection(dir)
    if (x0 == x1) return new Order1(x0, ystart, x1, yend, dir)
    val num = x0 - x1
    val denom = y0 - y1
    val xstart = x0 + (ystart - y0) * num / denom
    val xend = x0 + (yend - y0) * num / denom
    new Order1(xstart, ystart, xend, yend, dir)
  }

  override def getReversedCurve = new Order1(x0, y0, x1, y1, -direction)

  override def compareTo(other: Curve, yrange: Array[Double]): Int = {
    if (!other.isInstanceOf[Order1]) return super.compareTo(other, yrange)
    val c1 = other.asInstanceOf[Order1]
    if (yrange(1) <= yrange(0)) throw new InternalError("yrange already screwed up...")
    yrange(1) = Math.min(Math.min(yrange(1), y1), c1.y1)
    if (yrange(1) <= yrange(0)) throw new InternalError("backstepping from " + yrange(0) + " to " + yrange(1))
    if (xmax <= c1.xmin) return if (xmin == c1.xmax) 0
    else -1
    if (xmin >= c1.xmax) return 1
    /*
             * If "this" is curve A and "other" is curve B, then...
             * xA(y) = x0A + (y - y0A) (x1A - x0A) / (y1A - y0A)
             * xB(y) = x0B + (y - y0B) (x1B - x0B) / (y1B - y0B)
             * xA(y) == xB(y)
             * x0A + (y - y0A) (x1A - x0A) / (y1A - y0A)
             *    == x0B + (y - y0B) (x1B - x0B) / (y1B - y0B)
             * 0 == x0A (y1A - y0A) (y1B - y0B) + (y - y0A) (x1A - x0A) (y1B - y0B)
             *    - x0B (y1A - y0A) (y1B - y0B) - (y - y0B) (x1B - x0B) (y1A - y0A)
             * 0 == (x0A - x0B) (y1A - y0A) (y1B - y0B)
             *    + (y - y0A) (x1A - x0A) (y1B - y0B)
             *    - (y - y0B) (x1B - x0B) (y1A - y0A)
             * If (dxA == x1A - x0A), etc...
             * 0 == (x0A - x0B) * dyA * dyB
             *    + (y - y0A) * dxA * dyB
             *    - (y - y0B) * dxB * dyA
             * 0 == (x0A - x0B) * dyA * dyB
             *    + y * dxA * dyB - y0A * dxA * dyB
             *    - y * dxB * dyA + y0B * dxB * dyA
             * 0 == (x0A - x0B) * dyA * dyB
             *    + y * dxA * dyB - y * dxB * dyA
             *    - y0A * dxA * dyB + y0B * dxB * dyA
             * 0 == (x0A - x0B) * dyA * dyB
             *    + y * (dxA * dyB - dxB * dyA)
             *    - y0A * dxA * dyB + y0B * dxB * dyA
             * y == ((x0A - x0B) * dyA * dyB
             *       - y0A * dxA * dyB + y0B * dxB * dyA)
             *    / (-(dxA * dyB - dxB * dyA))
             * y == ((x0A - x0B) * dyA * dyB
             *       - y0A * dxA * dyB + y0B * dxB * dyA)
             *    / (dxB * dyA - dxA * dyB)
             */
    val dxa = x1 - x0
    val dya = y1 - y0
    val dxb = c1.x1 - c1.x0
    val dyb = c1.y1 - c1.y0
    val denom = dxb * dya - dxa * dyb
    var y = .0
    if (denom != 0) {
      val num = (x0 - c1.x0) * dya * dyb - y0 * dxa * dyb + c1.y0 * dxb * dya
      y = num / denom
      if (y <= yrange(0)) { // intersection is above us
        // Use bottom-most common y for comparison
        y = Math.min(y1, c1.y1)
      }
      else { // intersection is below the top of our range
        if (y < yrange(1)) { // If intersection is in our range, adjust valid range
          yrange(1) = y
        }
        // Use top-most common y for comparison
        y = Math.max(y0, c1.y0)
      }
    }
    else { // lines are parallel, choose any common y for comparison
      // Note - prefer an endpoint for speed of calculating the X
      // (see shortcuts in Order1.XforY())
      y = Math.max(y0, c1.y0)
    }
    orderof(XforY(y), c1.XforY(y))
  }

  override def getSegment(coords: Array[Double]): Int = {
    if (direction == INCREASING) {
      coords(0) = x1
      coords(1) = y1
    } else {
      coords(0) = x0
      coords(1) = y0
    }
    PathIterator.SEG_LINETO
  }
}
