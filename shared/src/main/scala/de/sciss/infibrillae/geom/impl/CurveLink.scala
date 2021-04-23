package de.sciss.infibrillae.geom.impl

// This is an adapted Scala translation of the CurveLink Java class of OpenJDK
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

final class CurveLink(private var curve: Curve, private var yTop: Double,
                      private var yBot: Double, private var eTag: Int) {
  if (yTop < curve.getYTop || yBot > curve.getYBot)
    throw new InternalError("bad curvelink [" + yTop + "=>" + yBot + "] for " + curve)

  private/*[geom]*/ var next: CurveLink = null

  def absorb(link: CurveLink): Boolean = absorb(link.curve, link.yTop, link.yBot, link.eTag)

  def absorb(curve: Curve, yStart: Double, yEnd: Double, eTag: Int): Boolean = {
    if ((this.curve ne curve) || this.eTag != eTag || yBot < yStart || yTop > yEnd) return false
    if (yStart < curve.getYTop || yEnd > curve.getYBot) {
      throw new InternalError("bad curvelink [" + yStart + "=>" + yEnd + "] for " + curve)
    }
    this.yTop = Math.min(yTop, yStart)
    this.yBot = Math.max(yBot, yEnd)
    true
  }

  def isEmpty: Boolean = yTop == yBot

  def getCurve: Curve = curve

  def getSubCurve: Curve = {
    if (yTop == curve.getYTop && yBot == curve.getYBot) return curve.getWithDirection(eTag)
    curve.getSubCurve(yTop, yBot, eTag)
  }

  def getMoveto = new Order0(getXTop, getYTop)

  def getXTop: Double = curve.XforY(yTop)

  def getYTop: Double = yTop

  def getXBot: Double = curve.XforY(yBot)

  def getYBot: Double = yBot

  def getX: Double = curve.XforY(yTop)

  def getEdgeTag: Int = eTag

  def setNext(link: CurveLink): Unit =
    this.next = link

  def getNext: CurveLink = next
}
