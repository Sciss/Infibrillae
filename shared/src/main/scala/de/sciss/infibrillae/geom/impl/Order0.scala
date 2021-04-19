package de.sciss.infibrillae.geom.impl

// This is an adapted Scala translation of the Order0 Java class of OpenJDK
// as released under GNU GPL 2 -- see original file header below.
// So it can be used in Scala.js.

/*
 * Copyright (c) 1998, Oracle and/or its affiliates. All rights reserved.
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
import de.sciss.infibrillae.geom.impl.Curve.INCREASING
import sun.awt.geom.Crossings

final class Order0(x: Double, y: Double) extends Curve(INCREASING) {
  override def getOrder = 0

  override def getXTop: Double = x
  override def getYTop: Double = y

  override def getXBot: Double = x
  override def getYBot: Double = y

  override def getXMin: Double = x
  override def getXMax: Double = x

  override def getX0: Double = x
  override def getY0: Double = y

  override def getX1: Double = x
  override def getY1: Double = y

  override def XforY(y: Double): Double = y

  override def TforY(y: Double) = 0

  override def XforT(t: Double): Double = x

  override def YforT(t: Double): Double = y

  override def dXforT(t: Double, deriv: Int) = 0

  override def dYforT(t: Double, deriv: Int) = 0

  override def nextVertical(t0: Double, t1: Double): Double = t1

  override def crossingsFor(x: Double, y: Double) = 0

  override def accumulateCrossings(c: Crossings): Boolean = x > c.getXLo && x < c.getXHi && y > c.getYLo && y < c.getYHi

  override def enlarge(r: Rectangle2D): Unit = {
    r.add(x, y)
  }

  override def getSubCurve(ystart: Double, yend: Double, dir: Int): Curve = this

  override def getReversedCurve: Curve = this

  override def getSegment(coords: Array[Double]): Int = {
    coords(0) = x
    coords(1) = y
    PathIterator.SEG_MOVETO
  }
}
