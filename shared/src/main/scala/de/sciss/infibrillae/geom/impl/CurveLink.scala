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

final class CurveLink(private var curve: Curve, private var ytop: Double,
                      private var ybot: Double, private var etag: Int) {
  if (ytop < curve.getYTop || ybot > curve.getYBot)
    throw new InternalError("bad curvelink [" + ytop + "=>" + ybot + "] for " + curve)

  private/*[geom]*/ var next: CurveLink = null

  def absorb(link: CurveLink): Boolean = absorb(link.curve, link.ytop, link.ybot, link.etag)

  def absorb(curve: Curve, ystart: Double, yend: Double, etag: Int): Boolean = {
    if ((this.curve ne curve) || this.etag != etag || ybot < ystart || ytop > yend) return false
    if (ystart < curve.getYTop || yend > curve.getYBot) {
      throw new InternalError("bad curvelink [" + ystart + "=>" + yend + "] for " + curve)
    }
    this.ytop = Math.min(ytop, ystart)
    this.ybot = Math.max(ybot, yend)
    true
  }

  def isEmpty: Boolean = ytop == ybot

  def getCurve: Curve = curve

  def getSubCurve: Curve = {
    if (ytop == curve.getYTop && ybot == curve.getYBot) return curve.getWithDirection(etag)
    curve.getSubCurve(ytop, ybot, etag)
  }

  def getMoveto = new Order0(getXTop, getYTop)

  def getXTop: Double = curve.XforY(ytop)

  def getYTop: Double = ytop

  def getXBot: Double = curve.XforY(ybot)

  def getYBot: Double = ybot

  def getX: Double = curve.XforY(ytop)

  def getEdgeTag: Int = etag

  def setNext(link: CurveLink): Unit =
    this.next = link

  def getNext: CurveLink = next
}
