package de.sciss.infibrillae.geom.impl

// This is an adapted Scala translation of the AreaOp Java class of OpenJDK
// as released under GNU GPL 2 -- see original file header below.
// So it can be used in Scala.js.

/*
 * Copyright (c) 1998, 2003, Oracle and/or its affiliates. All rights reserved.
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

import java.util
import java.util.Comparator

object AreaOp {
  abstract class CAGOp extends AreaOp {
    private[geom] var inLeft    = false
    private[geom] var inRight   = false
    private[geom] var inResult  = false

    override def newRow(): Unit = {
      inLeft    = false
      inRight   = false
      inResult  = false
    }

    override def classify(e: Edge): Int = {
      if (e.getCurveTag == CTAG_LEFT) inLeft = !inLeft
      else inRight = !inRight
      val newClass = newClassification(inLeft, inRight)
      if (inResult == newClass) return ETAG_IGNORE
      inResult = newClass
      if (newClass) ETAG_ENTER else ETAG_EXIT
    }

    override def getState: Int = if (inResult) RSTAG_INSIDE else RSTAG_OUTSIDE

    def newClassification(inLeft: Boolean, inRight: Boolean): Boolean
  }

  class AddOp extends AreaOp.CAGOp {
    override def newClassification(inLeft: Boolean, inRight: Boolean): Boolean = inLeft || inRight
  }

  class SubOp extends AreaOp.CAGOp {
    override def newClassification(inLeft: Boolean, inRight: Boolean): Boolean = inLeft && !inRight
  }

  class IntOp extends AreaOp.CAGOp {
    override def newClassification(inLeft: Boolean, inRight: Boolean): Boolean = inLeft && inRight
  }

  class XorOp extends AreaOp.CAGOp {
    override def newClassification(inLeft: Boolean, inRight: Boolean): Boolean = inLeft != inRight
  }

  class NZWindOp extends AreaOp {
    private var count = 0

    override def newRow(): Unit =
      count = 0

    override def classify(e: Edge): Int = { // Note: the right curves should be an empty set with this op...
      // assert(e.getCurveTag() == CTAG_LEFT);
      var newCount = count
      val `type` = if (newCount == 0) ETAG_ENTER
      else ETAG_IGNORE
      newCount += e.getCurve.getDirection
      count = newCount
      if (newCount == 0) ETAG_EXIT
      else `type`
    }

    override def getState: Int = if (count == 0) RSTAG_OUTSIDE else RSTAG_INSIDE
  }

  class EOWindOp extends AreaOp {
    private var inside = false

    override def newRow(): Unit =
      inside = false

    override def classify(e: Edge): Int = {
      val newInside = !inside
      inside = newInside
      if (newInside) ETAG_ENTER else ETAG_EXIT
    }

    override def getState: Int = if (inside) RSTAG_INSIDE else RSTAG_OUTSIDE
  }

  /* Constants to tag the left and right curves in the edge list */
  final val CTAG_LEFT     = 0
  final val CTAG_RIGHT    = 1
  /* Constants to classify edges */
  final val ETAG_IGNORE   = 0
  final val ETAG_ENTER    = 1
  final val ETAG_EXIT     = -1
  /* Constants used to classify result state */
  final val RSTAG_INSIDE  = 1
  final val RSTAG_OUTSIDE = -1

  private def addEdges(edges: util.Vector[Edge], curves: util.Vector[Curve], curvetag: Int): Unit = {
    val enum_ : util.Enumeration[Curve] = curves.elements
    while (enum_.hasMoreElements) {
      val c = enum_.nextElement
      if (c.getOrder > 0) edges.add(new Edge(c, curvetag))
    }
  }

  private val YXTopComparator = new Comparator[Edge]() {
    override def compare(o1: Edge, o2: Edge): Int = {
      val c1 = o1.getCurve
      val c2 = o2.getCurve
      var v1 = .0
      var v2 = .0
      v1 = c1.getYTop
      v2 = c2.getYTop
      if (v1 == v2) {
        v1 = c1.getXTop
        v2 = c2.getXTop
        if (v1 == v2) return 0
      }
      if (v1 < v2) return -1
      1
    }
  }

  def finalizeSubCurves(subcurves: util.Vector[CurveLink], chains: util.Vector[ChainEnd]): Unit = {
    val numchains = chains.size
    if (numchains == 0) return
    if ((numchains & 1) != 0) throw new InternalError("Odd number of chains!")
    val endlist = new Array[ChainEnd](numchains)
    chains.toArray(endlist)
    var i = 1
    while (i < numchains) {
      val open      = endlist(i - 1)
      val close     = endlist(i)
      val subcurve  = open.linkTo(close)
      if (subcurve != null) subcurves.add(subcurve)

      i += 2
    }
    chains.clear()
  }

  private val EmptyLinkList   = new Array[CurveLink](2)
  private val EmptyChainList  = new Array[ChainEnd](2)

  def resolveLinks(subcurves: util.Vector[CurveLink], chains: util.Vector[ChainEnd],
                   links: util.Vector[CurveLink]): Unit = {
    val numlinks = links.size
    val linklist = if (numlinks == 0) EmptyLinkList
    else {
      if ((numlinks & 1) != 0) throw new InternalError("Odd number of new curves!")
      val res = new Array[CurveLink](numlinks + 2)
      links.toArray(res)
      res
    }
    val numchains = chains.size
    val endlist = if (numchains == 0) EmptyChainList
    else {
      if ((numchains & 1) != 0) throw new InternalError("Odd number of chains!")
      val res = new Array[ChainEnd](numchains + 2)
      chains.toArray(res)
      res
    }
    var curchain  = 0
    var curlink   = 0
    chains.clear()
    var chain     = endlist(0)
    var nextchain = endlist(1)
    var link      = linklist(0)
    var nextlink  = linklist(1)
    while (chain != null || link != null) {
      /*
                  * Strategy 1:
                  * Connect chains or links if they are the only things left...
                  */
      var connectchains = link  == null
      var connectlinks  = chain == null
      if (!connectchains && !connectlinks) { // assert(link != null && chain != null);
        /*
                         * Strategy 2:
                         * Connect chains or links if they close off an open area...
                         */
        connectchains = (curchain & 1) == 0 && chain.getX == nextchain.getX
        connectlinks  = (curlink  & 1) == 0 && link .getX == nextlink .getX
        if (!connectchains && !connectlinks) {
          /*
                              * Strategy 3:
                              * Connect chains or links if their successor is
                              * between them and their potential connectee...
                              */
          val cx = chain.getX
          val lx = link.getX
          connectchains = nextchain != null && cx < lx && obstructs(nextchain .getX, lx, curchain)
          connectlinks  = nextlink  != null && lx < cx && obstructs(nextlink  .getX, cx, curlink)
        }
      }
      if (connectchains) {
        val subcurve = chain.linkTo(nextchain)
        if (subcurve != null) subcurves.add(subcurve)
        curchain += 2
        chain = endlist(curchain)
        nextchain = endlist(curchain + 1)
      }
      if (connectlinks) {
        val openend   = new ChainEnd(link, null)
        val closeend  = new ChainEnd(nextlink, openend)
        openend.setOtherEnd(closeend)
        chains.add(openend)
        chains.add(closeend)
        curlink += 2
        link = linklist(curlink)
        nextlink = linklist(curlink + 1)
      }
      if (!connectchains && !connectlinks) { // assert(link != null);
        // assert(chain != null);
        // assert(chain.getEtag() == link.getEtag());
        chain.addLink(link)
        chains.add(chain)
        curchain += 1
        chain = nextchain
        nextchain = endlist(curchain + 1)
        curlink += 1
        link = nextlink
        nextlink = linklist(curlink + 1)
      }
    }
    if ((chains.size & 1) != 0) System.out.println("Odd number of chains!")
  }

  /*
       * Does the position of the next edge at v1 "obstruct" the
       * connectivity between current edge and the potential
       * partner edge which is positioned at v2?
       *
       * Phase tells us whether we are testing for a transition
       * into or out of the interior part of the resulting area.
       *
       * Require 4-connected continuity if this edge and the partner
       * edge are both "entering into" type edges
       * Allow 8-connected continuity for "exiting from" type edges
       */
  def obstructs(v1: Double, v2: Double, phase: Int): Boolean =
    if ((phase & 1) == 0) v1 <= v2 else v1 < v2
}

abstract class AreaOp private() {
  def newRow(): Unit

  def classify(e: Edge): Int

  def getState: Int

  def calculate(left: util.Vector[Curve], right: util.Vector[Curve]): util.Vector[Curve] = {
    val edges = new util.Vector[Edge]
    AreaOp.addEdges(edges, left , AreaOp.CTAG_LEFT  )
    AreaOp.addEdges(edges, right, AreaOp.CTAG_RIGHT )
    val curves = pruneEdges(edges)
    if (false) {
      System.out.println("result: ")
      val numcurves = curves.size
      val curvelist = curves.toArray(new Array[Curve](numcurves))
      for (i <- 0 until numcurves) {
        System.out.println("curvelist[" + i + "] = " + curvelist(i))
      }
    }
    curves
  }

  private def pruneEdges(edges: util.Vector[Edge]): util.Vector[Curve] = {
    val numedges = edges.size
    if (numedges < 2) { // empty vector is expected with less than 2 edges
      return new util.Vector[Curve]
    }
    val edgelist = edges.toArray(new Array[Edge](numedges))
    util.Arrays.sort(edgelist, AreaOp.YXTopComparator)
    if (false) {
      System.out.println("pruning: ")
      for (i <- 0 until numedges) {
        System.out.println("edgelist[" + i + "] = " + edgelist(i))
      }
    }
    var e: Edge = null
    var left = 0
    var right = 0
    var cur = 0
    var next = 0
    val yrange    = new Array[Double](2)
    val subcurves = new util.Vector[CurveLink]
    val chains    = new util.Vector[ChainEnd]
    val links     = new util.Vector[CurveLink]
    // Active edges are between left (inclusive) and right (exclusive)
    var break1 = false
    while (!break1 && (left < numedges)) {
      var y = yrange(0)
      // Prune active edges that fall off the top of the active y range
      next = right - 1
      cur = next
      while (cur >= left) {
        e = edgelist(cur)
        if (e.getCurve.getYBot > y) {
          if (next > cur) edgelist(next) = e
          next -= 1
        }

        cur -= 1
      }
      left = next + 1
      // Grab a new "top of Y range" if the active edges are empty
      if (left >= right) {
        if (right >= numedges) {
//          break //todo: break is not supported
          break1 = true
        } else {
          y = edgelist(right).getCurve.getYTop
          if (y > yrange(0)) AreaOp.finalizeSubCurves(subcurves, chains)
          yrange(0) = y
        }
      }
      if (!break1) {
        // Incorporate new active edges that enter the active y range
        var break2 = false
        while (!break2 && (right < numedges)) {
          e = edgelist(right)
          if (e.getCurve.getYTop > y) {
//            break  //todo: break is not supported
            break2 = true
          } else {
            right += 1
          }
        }
        // Sort the current active edges by their X values and
        // determine the maximum valid Y range where the X ordering
        // is correct
        yrange(1) = edgelist(left).getCurve.getYBot
        if (right < numedges) {
          y = edgelist(right).getCurve.getYTop
          if (yrange(1) > y) yrange(1) = y
        }
        if (false) {
          System.out.println("current line: y = [" + yrange(0) + ", " + yrange(1) + "]")
          cur = left
          while (cur < right) {
            System.out.println("  " + edgelist(cur))

            cur += 1
          }
        }
        // Note: We could start at left+1, but we need to make
        // sure that edgelist[left] has its equivalence set to 0.
        var nexteq = 1
        cur = left
        while (cur < right) {
          e = edgelist(cur)
          e.setEquivalence(0)
          next = cur
          var break3 = false
          while (!break3 && (next > left)) {
            val prevedge = edgelist(next - 1)
            val ordering = e.compareTo(prevedge, yrange)
            if (yrange(1) <= yrange(0)) throw new InternalError("backstepping to " + yrange(1) + " from " + yrange(0))
            if (ordering >= 0) {
              if (ordering == 0) { // If the curves are equal, mark them to be
                // deleted later if they cancel each other
                // out so that we avoid having extraneous
                // curve segments.
                var eq = prevedge.getEquivalence
                if (eq == 0) {
                  eq = {
                    nexteq += 1; nexteq - 1
                  }
                  prevedge.setEquivalence(eq)
                }
                e.setEquivalence(eq)
              }
//              break //todo: break is not supported
              break3 = true
            } else {
              edgelist(next) = prevedge

              next -= 1
            }
          }

          edgelist(next) = e

          cur += 1
        }
        if (false) {
          System.out.println("current sorted line: y = [" + yrange(0) + ", " + yrange(1) + "]")
          cur = left
          while (cur < right) {
            System.out.println("  " + edgelist(cur))

            cur += 1
          }
        }
        // Now prune the active edge list.
        // For each edge in the list, determine its classification
        // (entering shape, exiting shape, ignore - no change) and
        // record the current Y range and its classification in the
        // Edge object for use later in constructing the new outline.
        newRow()
        val ystart = yrange(0)
        val yend = yrange(1)
        cur = left
        while (cur < right) {
          e = edgelist(cur)
          var etag = 0
          val eq = e.getEquivalence
          if (eq != 0) { // Find one of the segments in the "equal" range
            // with the right transition state and prefer an
            // edge that was either active up until ystart
            // or the edge that extends the furthest downward
            // (i.e. has the most potential for continuation)
            val origstate = getState
            etag = if (origstate == AreaOp.RSTAG_INSIDE) AreaOp.ETAG_EXIT else AreaOp.ETAG_ENTER
            var activematch: Edge = null
            var longestmatch      = e
            var furthesty         = yend
            do { // Note: classify() must be called
              // on every edge we consume here.
              classify(e)
              if (activematch == null && e.isActiveFor(ystart, etag)) activematch = e
              y = e.getCurve.getYBot
              if (y > furthesty) {
                longestmatch = e
                furthesty = y
              }
            } while ({
              cur += 1
              cur < right && { e = edgelist(cur); e }.getEquivalence == eq
            })
            cur -= 1
            if (getState == origstate) etag = AreaOp.ETAG_IGNORE
            else {
              e = if (activematch != null) activematch else longestmatch
            }
          }
          else etag = classify(e)
          if (etag != AreaOp.ETAG_IGNORE) {
            e.record(yend, etag)
            links.add(new CurveLink(e.getCurve, ystart, yend, etag))
          }

          cur += 1
        }
        // assert(getState() == AreaOp.RSTAG_OUTSIDE);
        if (getState != AreaOp.RSTAG_OUTSIDE) {
          System.out.println("Still inside at end of active edge list!")
          System.out.println("num curves = " + (right - left))
          System.out.println("num links = " + links.size)
          System.out.println("y top = " + yrange(0))
          if (right < numedges) {
            System.out.println("y top of next curve = " + edgelist(right).getCurve.getYTop)
          } else {
            System.out.println("no more curves")
          }
          cur = left
          while (cur < right) {
            e = edgelist(cur)
            System.out.println(e)
            val eq = e.getEquivalence
            if (eq != 0) System.out.println("  was equal to " + eq + "...")

            cur += 1
          }
        }
        if (false) {
          System.out.println("new links:")
          for (i <- 0 until links.size) {
            val link = links.elementAt(i)
            System.out.println("  " + link.getSubCurve)
          }
        }
        AreaOp.resolveLinks(subcurves, chains, links)
        links.clear()
        // Finally capture the bottom of the valid Y range as the top
        // of the next Y range.
        yrange(0) = yend
      }
    }
    AreaOp.finalizeSubCurves(subcurves, chains)
    val ret = new util.Vector[Curve]
    val enum_ : util.Enumeration[CurveLink] = subcurves.elements
    while (enum_.hasMoreElements) {
      var link = enum_.nextElement
      ret.add(link.getMoveto)
      var nextlink = link
      while ( {
        nextlink = nextlink.getNext
        nextlink != null
      }) if (!link.absorb(nextlink)) {
        ret.add(link.getSubCurve)
        link = nextlink
      }
      ret.add(link.getSubCurve)
    }
    ret
  }
}
