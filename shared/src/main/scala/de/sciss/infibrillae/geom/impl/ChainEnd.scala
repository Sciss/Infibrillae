package de.sciss.infibrillae.geom.impl

// This is an adapted Scala translation of the ChainEnd Java class of OpenJDK
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

final class ChainEnd(first: CurveLink, private var partner: ChainEnd) {
  private var head = first
  private var tail = first
  private var etag = first.getEdgeTag

  def getChain: CurveLink = head

  def setOtherEnd(partner: ChainEnd): Unit = {
    this.partner = partner
  }

  def getPartner: ChainEnd = partner

  /*
       * Returns head of a complete chain to be added to subcurves
       * or null if the links did not complete such a chain.
       */
  def linkTo(that: ChainEnd): CurveLink = {
    if (etag == AreaOp.ETAG_IGNORE || that.etag == AreaOp.ETAG_IGNORE) throw new InternalError("ChainEnd linked more than once!")
    if (etag == that.etag) throw new InternalError("Linking chains of the same type!")
    var enter : ChainEnd = null
    var exit  : ChainEnd = null
    // assert(partner.etag != that.partner.etag);
    if (etag == AreaOp.ETAG_ENTER) {
      enter = this
      exit = that
    }
    else {
      enter = that
      exit = this
    }
    // Now make sure these ChainEnds are not linked to any others...
    etag = AreaOp.ETAG_IGNORE
    that.etag = AreaOp.ETAG_IGNORE
    // Now link everything up...
    enter.tail.setNext(exit.head)
    enter.tail = exit.tail
    if (partner eq that) { // Curve has closed on itself...
      return enter.head
    }
    // Link this chain into one end of the chain formed by the partners
    val otherenter = exit.partner
    val otherexit = enter.partner
    otherenter.partner = otherexit
    otherexit.partner = otherenter
    if (enter.head.getYTop < otherenter.head.getYTop) {
      enter.tail.setNext(otherenter.head)
      otherenter.head = enter.head
    }
    else {
      otherexit.tail.setNext(enter.head)
      otherexit.tail = enter.tail
    }
    null
  }

  def addLink(newlink: CurveLink): Unit = {
    if (etag == AreaOp.ETAG_ENTER) {
      tail.setNext(newlink)
      tail = newlink
    }
    else {
      newlink.setNext(head)
      head = newlink
    }
  }

  def getX: Double = if (etag == AreaOp.ETAG_ENTER) tail.getXBot else head.getXBot
}
