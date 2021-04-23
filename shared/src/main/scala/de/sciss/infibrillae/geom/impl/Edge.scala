package de.sciss.infibrillae.geom.impl

// This is an adapted Scala translation of the Edge Java class of OpenJDK
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

object Edge {
  private[geom] final val INIT_PARTS = 4
  private[geom] final val GROW_PARTS = 10
}

final class Edge(private val curve: Curve, cTag: Int, private var eTag: Int) {
  private var activeY     = 0.0
  private var equivalence = 0

  def this(c: Curve, cTag: Int) =
    this(c, cTag, AreaOp.ETAG_IGNORE)

  def getCurve: Curve = curve

  def getCurveTag: Int = cTag

  def getEdgeTag: Int = eTag

  def setEdgeTag(eTag: Int): Unit =
    this.eTag = eTag

  def getEquivalence: Int = equivalence

  def setEquivalence(eq: Int): Unit =
    equivalence = eq

  private var lastEdge: Edge = null
  private var lastResult = 0
  private var lastLimit = .0

  def compareTo(other: Edge, yrange: Array[Double]): Int = {
    if ((other eq lastEdge) && yrange(0) < lastLimit) {
      if (yrange(1) > lastLimit) yrange(1) = lastLimit
      return lastResult
    }
    if ((this eq other.lastEdge) && yrange(0) < other.lastLimit) {
      if (yrange(1) > other.lastLimit) yrange(1) = other.lastLimit
      return 0 - other.lastResult
    }
    //long start = System.currentTimeMillis();
    val ret = curve.compareTo(other.curve, yrange)
    //long end = System.currentTimeMillis();
    /*
            System.out.println("compare: "+
                               ((System.identityHashCode(this) <
                                 System.identityHashCode(other))
                                ? this+" to "+other
                                : other+" to "+this)+
                               " == "+ret+" at "+yrange[1]+
                               " in "+(end-start)+"ms");
             */
    lastEdge    = other
    lastLimit   = yrange(1)
    lastResult  = ret
    ret
  }

  def record(yend: Double, etag: Int): Unit = {
    this.activeY  = yend
    this.eTag     = etag
  }

  def isActiveFor(y: Double, etag: Int): Boolean = this.eTag == etag && this.activeY >= y

  override def toString: String = "Edge[" + curve + ", " +
    (if (cTag == AreaOp.CTAG_LEFT) "L" else "R") + ", " +
    (if (eTag == AreaOp.ETAG_ENTER) "I" else { if (eTag == AreaOp.ETAG_EXIT) "O" else "N" }) + "]"
}
