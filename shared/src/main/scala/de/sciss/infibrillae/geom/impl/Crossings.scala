package de.sciss.infibrillae.geom.impl

// This is an adapted Scala translation of the Crossings Java class of OpenJDK
// as released under GNU GPL 2 -- see original file header below.
// So it can be used in Scala.js.

/*
 * Copyright (c) 1998, 2014, Oracle and/or its affiliates. All rights reserved.
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

import de.sciss.infibrillae.geom.PathIterator

import scala.collection.{mutable, Seq => CSeq}

object Crossings {
  val debug = false

  def findCrossings(curves: CSeq[Curve], xlo: Double, ylo: Double, xhi: Double, yhi: Double): Crossings = {
    val cross = new Crossings.EvenOdd(xlo, ylo, xhi, yhi)
    if (curves.exists(_.accumulateCrossings(cross))) return null
    if (debug) cross.print()
    cross
  }

  def findCrossings(pi: PathIterator, xlo: Double, ylo: Double, xhi: Double, yhi: Double): Crossings = {
    val cross = if (pi.getWindingRule == PathIterator.WIND_EVEN_ODD)
      new Crossings.EvenOdd(xlo, ylo, xhi, yhi)
    else
      new Crossings.NonZero(xlo, ylo, xhi, yhi)
    // coords array is big enough for holding:
    //     coordinates returned from currentSegment (6)
    //     OR
    //         two subdivided quadratic curves (2+4+4=10)
    //         AND
    //             0-1 horizontal splitting parameters
    //             OR
    //             2 parametric equation derivative coefficients
    //         three subdivided cubic curves (2+6+6+6=20)
    //             0-2 horizontal splitting parameters
    //             3 parametric equation derivative coefficients
    val coords = new Array[Double](23)
    var movx = 0.0
    var movy = 0.0
    var curx = 0.0
    var cury = 0.0
    var newx = .0
    var newy = .0
    while (!pi.isDone) {
      val tpe = pi.currentSegment(coords)
      tpe match {
        case PathIterator.SEG_MOVETO =>
          if (movy != cury && cross.accumulateLine(curx, cury, movx, movy)) return null
          curx = coords(0)
          movx = curx
          cury = coords(1)
          movy = cury

        case PathIterator.SEG_LINETO =>
          newx = coords(0)
          newy = coords(1)
          if (cross.accumulateLine(curx, cury, newx, newy)) return null
          curx = newx
          cury = newy

        case PathIterator.SEG_QUADTO =>
          newx = coords(2)
          newy = coords(3)
          if (cross.accumulateQuad(curx, cury, coords)) return null
          curx = newx
          cury = newy

        case PathIterator.SEG_CUBICTO =>
          newx = coords(4)
          newy = coords(5)
          if (cross.accumulateCubic(curx, cury, coords)) return null
          curx = newx
          cury = newy

        case PathIterator.SEG_CLOSE =>
          if (movy != cury && cross.accumulateLine(curx, cury, movx, movy)) return null
          curx = movx
          cury = movy

      }
      pi.next()
    }
    if (movy != cury) if (cross.accumulateLine(curx, cury, movx, movy)) return null
    if (debug) cross.print()
    cross
  }

  final class EvenOdd(private val xlo: Double, private val ylo: Double,
                      private val xhi: Double, private val yhi: Double)
    extends Crossings(xlo, ylo, xhi, yhi) {

    override def covers(ystart: Double, yend: Double): Boolean =
      limit == 2 && yranges(0) <= ystart && yranges(1) >= yend

    override def record(ystart: Double, yend: Double, direction: Int): Unit = {
      if (ystart >= yend) return
      var _ystart = ystart
      var _yend   = yend
      var from = 0
      // Quickly jump over all pairs that are completely "above"
      while (from < limit && _ystart > yranges(from + 1)) from += 2
      var to = from
      var break1 = false
      while (!break1 && (from < limit)) {
        val yrlo = yranges(from)
        from += 1
        val yrhi = yranges(from)
        from += 1
        if (_yend < yrlo) { // Quickly handle insertion of the new range
          yranges(to) = _ystart
          to += 1
          yranges(to) = _yend
          to += 1
          _ystart = yrlo
          _yend = yrhi
//          continue //todo: continue is not supported
        } else {
          // The ranges overlap - sort, collapse, insert, iterate
          var yll = .0
          var ylh = .0
          var yhl = .0
          var yhh = .0
          if (_ystart < yrlo) {
            yll = _ystart
            ylh = yrlo
          }
          else {
            yll = yrlo
            ylh = _ystart
          }
          if (_yend < yrhi) {
            yhl = _yend
            yhh = yrhi
          }
          else {
            yhl = yrhi
            yhh = _yend
          }
          if (ylh == yhl) {
            _ystart = yll
            _yend   = yhh
          }
          else {
            if (ylh > yhl) {
              _ystart = yhl
              yhl = ylh
              ylh = _ystart
            }
            if (yll != ylh) {
              yranges(to) = yll
              to += 1
              yranges(to) = ylh
              to += 1
            }
            _ystart = yhl
            _yend = yhh
          }
          if (_ystart >= _yend) {
//            break //todo: break is not supported
            break1 = true
          }
        }
      }

      if (to < from && from < limit) System.arraycopy(yranges, from, yranges, to, limit - from)
      to += (limit - from)
      if (_ystart < _yend) {
        if (to >= yranges.length) {
          val newranges = new Array[Double](to + 10)
          System.arraycopy(yranges, 0, newranges, 0, to)
          yranges = newranges
        }
        yranges(to) = _ystart
        to += 1
        yranges(to) = _yend
        to += 1
      }
      limit = to
    }
  }

  final class NonZero(private val xlo: Double, private val ylo: Double,
                      private val xhi: Double, private val yhi: Double)
    extends Crossings(xlo, ylo, xhi, yhi) {

    private var crosscounts = new Array[Int](yranges.length / 2)

    override def covers(ystart: Double, yend: Double): Boolean = {
      var _ystart = ystart
      var i = 0
      while (i < limit) {
        val ylo = yranges(i)
        i += 1
        val yhi = yranges(i)
        i += 1
        if (_ystart >= yhi) {
//          continue //todo: continue is not supported
        }  else {
          if (_ystart < ylo) return false
          if (yend <= yhi) return true
          _ystart = yhi
        }
      }
      _ystart >= yend
    }

    def remove(cur: Int): Unit = {
      limit -= 2
      val rem = limit - cur
      if (rem > 0) {
        System.arraycopy(yranges, cur + 2, yranges, cur, rem)
        System.arraycopy(crosscounts, cur / 2 + 1, crosscounts, cur / 2, rem / 2)
      }
    }

    def insert(cur: Int, lo: Double, hi: Double, dir: Int): Unit = {
      val rem = limit - cur
      val oldranges = yranges
      val oldcounts = crosscounts
      if (limit >= yranges.length) {
        yranges = new Array[Double](limit + 10)
        System.arraycopy(oldranges, 0, yranges, 0, cur)
        crosscounts = new Array[Int]((limit + 10) / 2)
        System.arraycopy(oldcounts, 0, crosscounts, 0, cur / 2)
      }
      if (rem > 0) {
        System.arraycopy(oldranges, cur, yranges, cur + 2, rem)
        System.arraycopy(oldcounts, cur / 2, crosscounts, cur / 2 + 1, rem / 2)
      }
      yranges(cur + 0) = lo
      yranges(cur + 1) = hi
      crosscounts(cur / 2) = dir
      limit += 2
    }

    override def record(ystart: Double, yend: Double, direction: Int): Unit = {
      if (ystart >= yend) return
      var _ystart = ystart
      var cur = 0
      while (cur < limit && _ystart > yranges(cur + 1)) cur += 2
      if (cur < limit) {
        var rdir = crosscounts(cur / 2)
        var yrlo = yranges(cur + 0)
        var yrhi = yranges(cur + 1)
        if (yrhi == _ystart && rdir == direction) { // Remove the range from the list and collapse it
          // into the range being inserted.  Note that the
          // new combined range may overlap the following range
          // so we must not simply combine the ranges in place
          // unless we are at the last range.
          if (cur + 2 == limit) {
            yranges(cur + 1) = yend
            return
          }
          remove(cur)
          _ystart = yrlo
          rdir = crosscounts(cur / 2)
          yrlo = yranges(cur + 0)
          yrhi = yranges(cur + 1)
        }
        if (yend < yrlo) { // Just insert the new range at the current location
          insert(cur, _ystart, yend, direction)
          return
        }
        if (yend == yrlo && rdir == direction) { // Just prepend the new range to the current one
          yranges(cur) = _ystart
          return
        }
        // The ranges must overlap - (yend > yrlo && yrhi > ystart)
        if (_ystart < yrlo) {
          insert(cur, _ystart, yrlo, direction)
          cur += 2
          _ystart = yrlo
        }
        else if (yrlo < _ystart) {
          insert(cur, yrlo, _ystart, rdir)
          cur += 2
          yrlo = _ystart
        }
        // assert(yrlo == ystart);
        val newdir = rdir + direction
        val newend = Math.min(yend, yrhi)
        if (newdir == 0) remove(cur)
        else {
          crosscounts(cur / 2) = newdir
          yranges(cur) = _ystart
          cur += 1
          yranges(cur) = newend
          cur += 1
        }
        yrlo   = newend
        _ystart = yrlo
        if (yrlo < yrhi) insert(cur, yrlo, yrhi, rdir)
      }
      if (_ystart < yend) insert(cur, _ystart, yend, direction)
    }
  }
}

abstract class Crossings(private var xlo: Double, private var ylo: Double,
                         private var xhi: Double, private var yhi: Double) {
  protected  /*private*//*[geom]*/ var limit = 0
  protected /*private*//*[geom]*/ var yranges = new Array[Double](10)

  final def getXLo: Double = xlo

  final def getYLo: Double = ylo

  final def getXHi: Double = xhi

  final def getYHi: Double = yhi

  def record(ystart: Double, yend: Double, direction: Int): Unit

  def print(): Unit = {
    System.out.println("Crossings [")
    System.out.println("  bounds = [" + ylo + ", " + yhi + "]")
    var i = 0
    while ( {
      i < limit
    }) {
      System.out.println("  [" + yranges(i) + ", " + yranges(i + 1) + "]")

      i += 2
    }
    System.out.println("]")
  }

  final def isEmpty: Boolean = limit == 0

  def covers(ystart: Double, yend: Double): Boolean

  def accumulateLine(x0: Double, y0: Double, x1: Double, y1: Double): Boolean = if (y0 <= y1) accumulateLine(x0, y0, x1, y1, 1)
  else accumulateLine(x1, y1, x0, y0, -1)

  def accumulateLine(x0: Double, y0: Double, x1: Double, y1: Double, direction: Int): Boolean = {
    if (yhi <= y0 || ylo >= y1) return false
    if (x0 >= xhi && x1 >= xhi) return false
    if (y0 == y1) return x0 >= xlo || x1 >= xlo
    var xstart = .0
    var ystart = .0
    var xend = .0
    var yend = .0
    val dx = x1 - x0
    val dy = y1 - y0
    if (y0 < ylo) {
      xstart = x0 + (ylo - y0) * dx / dy
      ystart = ylo
    }
    else {
      xstart = x0
      ystart = y0
    }
    if (yhi < y1) {
      xend = x0 + (yhi - y0) * dx / dy
      yend = yhi
    }
    else {
      xend = x1
      yend = y1
    }
    if (xstart >= xhi && xend >= xhi) return false
    if (xstart > xlo || xend > xlo) return true
    record(ystart, yend, direction)
    false
  }

  private val tmp = mutable.Buffer.empty[Curve]

  def accumulateQuad(x0: Double, y0: Double, coords: Array[Double]): Boolean = {
    if (y0 < ylo && coords(1) < ylo && coords(3) < ylo) return false
    if (y0 > yhi && coords(1) > yhi && coords(3) > yhi) return false
    if (x0 > xhi && coords(0) > xhi && coords(2) > xhi) return false
    if (x0 < xlo && coords(0) < xlo && coords(2) < xlo) {
      if (y0 < coords(3)) record(Math.max(y0, ylo), Math.min(coords(3), yhi), 1)
      else if (y0 > coords(3)) record(Math.max(coords(3), ylo), Math.min(y0, yhi), -1)
      return false
    }
    Curve.insertQuad(tmp, x0, y0, coords)
    if (tmp.exists(_.accumulateCrossings(this))) return true
    tmp.clear()
    false
  }

  def accumulateCubic(x0: Double, y0: Double, coords: Array[Double]): Boolean = {
    if (y0 < ylo && coords(1) < ylo && coords(3) < ylo && coords(5) < ylo) return false
    if (y0 > yhi && coords(1) > yhi && coords(3) > yhi && coords(5) > yhi) return false
    if (x0 > xhi && coords(0) > xhi && coords(2) > xhi && coords(4) > xhi) return false
    if (x0 < xlo && coords(0) < xlo && coords(2) < xlo && coords(4) < xlo) {
      if (y0 <= coords(5)) record(Math.max(y0, ylo), Math.min(coords(5), yhi), 1)
      else record(Math.max(coords(5), ylo), Math.min(y0, yhi), -1)
      return false
    }
    Curve.insertCubic(tmp, x0, y0, coords)
    if (tmp.exists(_.accumulateCrossings(this))) return true
    tmp.clear()
    false
  }
}
