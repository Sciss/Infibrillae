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
import scala.collection.mutable
import scala.collection.{Seq => CSeq}

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

  private def addEdges(edges: mutable.Growable[Edge], curves: CSeq[Curve], curveTag: Int): Unit =
    curves.foreach { c =>
      if (c.getOrder > 0) edges += new Edge(c, curveTag)
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

  private def finalizeSubCurves(subCurves: mutable.Growable[CurveLink], chains: mutable.Buffer[ChainEnd]): Unit = {
    val numChains = chains.size
    if (numChains == 0) return
    if ((numChains & 1) != 0) throw new InternalError("Odd number of chains!")
//    val endList = new Array[ChainEnd](numChains)
    val endList = chains.toArray // (endList)
    var i = 1
    while (i < numChains) {
      val open      = endList(i - 1)
      val close     = endList(i)
      val subCurve  = open.linkTo(close)
      if (subCurve != null) subCurves.addOne(subCurve)

      i += 2
    }
    chains.clear()
  }

  private val EmptyLinkList   = new Array[CurveLink](2)
  private val EmptyChainList  = new Array[ChainEnd](2)

  private def resolveLinks(subCurves: mutable.Growable[CurveLink], chains: mutable.Buffer[ChainEnd],
                           links: CSeq[CurveLink]): Unit = {
    val numLinks = links.size
    val linkList = if (numLinks == 0) EmptyLinkList
    else {
      if ((numLinks & 1) != 0) throw new InternalError("Odd number of new curves!")
//      val res = new Array[CurveLink](numLinks + 2)
      val resB = Array.newBuilder[CurveLink]
      resB.sizeHint(numLinks + 2)
      resB.addAll(links)
      resB.addOne(null)
      resB.addOne(null)
//      links.toArray(res)
      val res = resB.result()
      res
    }
    val numChains = chains.size
    val endList = if (numChains == 0) EmptyChainList
    else {
      if ((numChains & 1) != 0) throw new InternalError("Odd number of chains!")
//      val res = new Array[ChainEnd](numChains + 2)
      val resB = Array.newBuilder[ChainEnd]
      resB.sizeHint(numChains + 2)
      resB.addAll(chains)
      resB.addOne(null)
      resB.addOne(null)
      val res = resB.result()
      res
    }
    var curChain  = 0
    var curLink   = 0
    chains.clear()
    var chain     = endList(0)
    var nextChain = endList(1)
    var link      = linkList(0)
    var nextLink  = linkList(1)
    while (chain != null || link != null) {
      /*
                  * Strategy 1:
                  * Connect chains or links if they are the only things left...
                  */
      var connectChains = link  == null
      var connectLinks  = chain == null
      if (!connectChains && !connectLinks) { // assert(link != null && chain != null);
        /*
                         * Strategy 2:
                         * Connect chains or links if they close off an open area...
                         */
        connectChains = (curChain & 1) == 0 && chain.getX == nextChain.getX
        connectLinks  = (curLink  & 1) == 0 && link .getX == nextLink .getX
        if (!connectChains && !connectLinks) {
          /*
                              * Strategy 3:
                              * Connect chains or links if their successor is
                              * between them and their potential connectee...
                              */
          val cx = chain.getX
          val lx = link.getX
          connectChains = nextChain != null && cx < lx && obstructs(nextChain .getX, lx, curChain)
          connectLinks  = nextLink  != null && lx < cx && obstructs(nextLink  .getX, cx, curLink)
        }
      }
      if (connectChains) {
        val subCurve = chain.linkTo(nextChain)
        if (subCurve != null) subCurves.addOne(subCurve)
        curChain += 2
        chain     = endList(curChain)
        nextChain = endList(curChain + 1)
      }
      if (connectLinks) {
        val openEnd   = new ChainEnd(link, null)
        val closeEnd  = new ChainEnd(nextLink, openEnd)
        openEnd.setOtherEnd(closeEnd)
        chains.addOne(openEnd)
        chains.addOne(closeEnd)
        curLink += 2
        link      = linkList(curLink)
        nextLink  = linkList(curLink + 1)
      }
      if (!connectChains && !connectLinks) { // assert(link != null);
        // assert(chain != null);
        // assert(chain.getEtag() == link.getEtag());
        chain.addLink(link)
        chains.addOne(chain)
        curChain += 1
        chain     = nextChain
        nextChain = endList(curChain + 1)
        curLink += 1
        link      = nextLink
        nextLink  = linkList(curLink + 1)
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

  def calculate(left: CSeq[Curve], right: CSeq[Curve]): CSeq[Curve] = {
    val edgesB = Vector.newBuilder[Edge]
    AreaOp.addEdges(edgesB, left , AreaOp.CTAG_LEFT  )
    AreaOp.addEdges(edgesB, right, AreaOp.CTAG_RIGHT )
    val edges   = edgesB.result()
    val curves  = pruneEdges(edges)
    if (false) {
      System.out.println("result: ")
      val numCurves = curves.size
      val curveList = curves.toArray // (new Array[Curve](numCurves))
      for (i <- 0 until numCurves) {
        System.out.println("curvelist[" + i + "] = " + curveList(i))
      }
    }
    curves
  }

  private def pruneEdges(edges: CSeq[Edge]): CSeq[Curve] = {
    val numEdges = edges.size
    if (numEdges < 2) { // empty vector is expected with less than 2 edges
      return Vector.empty
    }
    val edgeList = edges.toArray // (new Array[Edge](numEdges))
    util.Arrays.sort(edgeList, AreaOp.YXTopComparator)
    if (false) {
      System.out.println("pruning: ")
      for (i <- 0 until numEdges) {
        System.out.println("edgelist[" + i + "] = " + edgeList(i))
      }
    }
    var e: Edge = null
    var left  = 0
    var right = 0
    var cur   = 0
    var next  = 0
    val yRange    = new Array[Double](2)
    val subCurves = mutable.Buffer.empty[CurveLink]
    val chains    = mutable.Buffer.empty[ChainEnd]
    val links     = mutable.Buffer.empty[CurveLink]
    // Active edges are between left (inclusive) and right (exclusive)
    var break1 = false
    while (!break1 && (left < numEdges)) {
      var y = yRange(0)
      // Prune active edges that fall off the top of the active y range
      next = right - 1
      cur = next
      while (cur >= left) {
        e = edgeList(cur)
        if (e.getCurve.getYBot > y) {
          if (next > cur) edgeList(next) = e
          next -= 1
        }

        cur -= 1
      }
      left = next + 1
      // Grab a new "top of Y range" if the active edges are empty
      if (left >= right) {
        if (right >= numEdges) {
//          break //todo: break is not supported
          break1 = true
        } else {
          y = edgeList(right).getCurve.getYTop
          if (y > yRange(0)) AreaOp.finalizeSubCurves(subCurves, chains)
          yRange(0) = y
        }
      }
      if (!break1) {
        // Incorporate new active edges that enter the active y range
        var break2 = false
        while (!break2 && (right < numEdges)) {
          e = edgeList(right)
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
        yRange(1) = edgeList(left).getCurve.getYBot
        if (right < numEdges) {
          y = edgeList(right).getCurve.getYTop
          if (yRange(1) > y) yRange(1) = y
        }
        if (false) {
          System.out.println("current line: y = [" + yRange(0) + ", " + yRange(1) + "]")
          cur = left
          while (cur < right) {
            System.out.println("  " + edgeList(cur))

            cur += 1
          }
        }
        // Note: We could start at left+1, but we need to make
        // sure that edgeList[left] has its equivalence set to 0.
        var nextEq = 1
        cur = left
        while (cur < right) {
          e = edgeList(cur)
          e.setEquivalence(0)
          next = cur
          var break3 = false
          while (!break3 && (next > left)) {
            val prevEdge = edgeList(next - 1)
            val ordering = e.compareTo(prevEdge, yRange)
            if (yRange(1) <= yRange(0)) throw new InternalError("backstepping to " + yRange(1) + " from " + yRange(0))
            if (ordering >= 0) {
              if (ordering == 0) { // If the curves are equal, mark them to be
                // deleted later if they cancel each other
                // out so that we avoid having extraneous
                // curve segments.
                var eq = prevEdge.getEquivalence
                if (eq == 0) {
                  eq = {
                    nextEq += 1; nextEq - 1
                  }
                  prevEdge.setEquivalence(eq)
                }
                e.setEquivalence(eq)
              }
//              break //todo: break is not supported
              break3 = true
            } else {
              edgeList(next) = prevEdge

              next -= 1
            }
          }

          edgeList(next) = e

          cur += 1
        }
        if (false) {
          System.out.println("current sorted line: y = [" + yRange(0) + ", " + yRange(1) + "]")
          cur = left
          while (cur < right) {
            System.out.println("  " + edgeList(cur))

            cur += 1
          }
        }
        // Now prune the active edge list.
        // For each edge in the list, determine its classification
        // (entering shape, exiting shape, ignore - no change) and
        // record the current Y range and its classification in the
        // Edge object for use later in constructing the new outline.
        newRow()
        val yStart  = yRange(0)
        val yEnd    = yRange(1)
        cur = left
        while (cur < right) {
          e = edgeList(cur)
          var eTag = 0
          val eq = e.getEquivalence
          if (eq != 0) { // Find one of the segments in the "equal" range
            // with the right transition state and prefer an
            // edge that was either active up until ystart
            // or the edge that extends the furthest downward
            // (i.e. has the most potential for continuation)
            val origState = getState
            eTag = if (origState == AreaOp.RSTAG_INSIDE) AreaOp.ETAG_EXIT else AreaOp.ETAG_ENTER
            var activeMatch: Edge = null
            var longestMatch      = e
            var furthestY         = yEnd
            do { // Note: classify() must be called
              // on every edge we consume here.
              classify(e)
              if (activeMatch == null && e.isActiveFor(yStart, eTag)) activeMatch = e
              y = e.getCurve.getYBot
              if (y > furthestY) {
                longestMatch = e
                furthestY = y
              }
            } while ({
              cur += 1
              cur < right && { e = edgeList(cur); e }.getEquivalence == eq
            })
            cur -= 1
            if (getState == origState) eTag = AreaOp.ETAG_IGNORE
            else {
              e = if (activeMatch != null) activeMatch else longestMatch
            }
          }
          else eTag = classify(e)
          if (eTag != AreaOp.ETAG_IGNORE) {
            e.record(yEnd, eTag)
            links.addOne(new CurveLink(e.getCurve, yStart, yEnd, eTag))
          }

          cur += 1
        }
        // assert(getState() == AreaOp.RSTAG_OUTSIDE);
        if (getState != AreaOp.RSTAG_OUTSIDE) {
          System.out.println("Still inside at end of active edge list!")
          System.out.println("num curves = " + (right - left))
          System.out.println("num links = " + links.size)
          System.out.println("y top = " + yRange(0))
          if (right < numEdges) {
            System.out.println("y top of next curve = " + edgeList(right).getCurve.getYTop)
          } else {
            System.out.println("no more curves")
          }
          cur = left
          while (cur < right) {
            e = edgeList(cur)
            System.out.println(e)
            val eq = e.getEquivalence
            if (eq != 0) System.out.println("  was equal to " + eq + "...")

            cur += 1
          }
        }
        if (false) {
          System.out.println("new links:")
          links.foreach { link =>
            System.out.println("  " + link.getSubCurve)
          }
        }
        AreaOp.resolveLinks(subCurves, chains, links)
        links.clear()
        // Finally capture the bottom of the valid Y range as the top
        // of the next Y range.
        yRange(0) = yEnd
      }
    }
    AreaOp.finalizeSubCurves(subCurves, chains)
    val resB = Vector.newBuilder[Curve]
    subCurves.foreach { link =>
      resB.addOne(link.getMoveto)
      var nextLink = link
      var currLink = link
      while ({
        nextLink = nextLink.getNext
        nextLink != null
      }) if (!currLink.absorb(nextLink)) {
        resB.addOne(currLink.getSubCurve)
        currLink = nextLink
      }
      resB.addOne(currLink.getSubCurve)
    }
    resB.result()
  }
}
