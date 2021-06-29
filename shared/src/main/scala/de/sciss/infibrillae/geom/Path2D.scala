package de.sciss.infibrillae.geom

// This is an adapted Scala translation of the Path2D Java class of OpenJDK
// as released under GNU GPL 2 -- see original file header below.
// So it can be used in Scala.js.

/*
 * Copyright (c) 2006, 2017, Oracle and/or its affiliates. All rights reserved.
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

import de.sciss.infibrillae.geom.Path2D.Iterator.curvecoords
import de.sciss.infibrillae.geom.impl.Curve

import java.io.{InvalidObjectException, ObjectInputStream, ObjectOutputStream, Serializable, StreamCorruptedException}
import java.util
import scala.{Float => SFloat, Double => SDouble}

/**
  * The `Path2D` class provides a simple, yet flexible
  * shape which represents an arbitrary geometric path.
  * It can fully represent any path which can be iterated by the
  * `PathIterator` interface including all of its segment
  * types and winding rules and it implements all of the
  * basic hit testing methods of the `Shape` interface.
  * <p>
  * Use `Path2D.SFloat` when dealing with data that can be represented
  * and used with floating point precision.  Use `Path2D.SDouble`
  * for data that requires the accuracy or range of double precision.
  * <p>
  * `Path2D` provides exactly those facilities required for
  * basic construction and management of a geometric path and
  * implementation of the above interfaces with little added
  * interpretation.
  * If it is useful to manipulate the interiors of closed
  * geometric shapes beyond simple hit testing then the
  * `Area` class provides additional capabilities
  * specifically targeted at closed figures.
  * While both classes nominally implement the `Shape`
  * interface, they differ in purpose and together they provide
  * two useful views of a geometric shape where `Path2D`
  * deals primarily with a trajectory formed by path segments
  * and `Area` deals more with interpretation and manipulation
  * of enclosed regions of 2D geometric space.
  * <p>
  * The `PathIterator` interface has more detailed descriptions
  * of the types of segments that make up a path and the winding rules
  * that control how to determine which regions are inside or outside
  * the path.
  *
  * @author Jim Graham
  * @since 1.6
  */
object Path2D {
  /**
    * An even-odd winding rule for determining the interior of
    * a path.
    *
    * @see PathIterator#WIND_EVEN_ODD
    * @since 1.6
    */
  final val WIND_EVEN_ODD = PathIterator.WIND_EVEN_ODD

  /**
    * A non-zero winding rule for determining the interior of a
    * path.
    *
    * @see PathIterator#WIND_NON_ZERO
    * @since 1.6
    */
  final val WIND_NON_ZERO = PathIterator.WIND_NON_ZERO

  // For code simplicity, copy these constants to our namespace
  // and cast them to byte constants for easy storage.
  private final val SEG_MOVETO  = PathIterator.SEG_MOVETO .toByte
  private final val SEG_LINETO  = PathIterator.SEG_LINETO .toByte
  private final val SEG_QUADTO  = PathIterator.SEG_QUADTO .toByte
  private final val SEG_CUBICTO = PathIterator.SEG_CUBICTO.toByte
  private final val SEG_CLOSE   = PathIterator.SEG_CLOSE  .toByte

  private[geom] val INIT_SIZE         = 20
  private[geom] val EXPAND_MAX        = 500
  private[geom] val EXPAND_MAX_COORDS = EXPAND_MAX * 2
  private[geom] val EXPAND_MIN        = 10 // ensure > 6 (cubics)

  private[geom] def expandPointTypes(oldPointTypes: Array[Byte], needed: Int): Array[Byte] = {
    val oldSize = oldPointTypes.length
    val newSizeMin = oldSize + needed
    if (newSizeMin < oldSize) { // hard overflow failure - we can't even accommodate
      // new items without overflowing
      throw new ArrayIndexOutOfBoundsException("pointTypes exceeds maximum capacity !")
    }
    // growth algorithm computation
    var grow = oldSize
    if (grow > EXPAND_MAX) grow = Math.max(EXPAND_MAX, oldSize >> 3) // 1/8th min
    else if (grow < EXPAND_MIN) grow = EXPAND_MIN
    assert(grow > 0)
    var newSize = oldSize + grow
    if (newSize < newSizeMin) { // overflow in growth algorithm computation
      newSize = Integer.MAX_VALUE
    }
    while (true) {
      try // try allocating the larger array
        return util.Arrays.copyOf(oldPointTypes, newSize)
      catch {
        case oome: OutOfMemoryError =>
          if (newSize == newSizeMin) throw oome
      }
      newSize = newSizeMin + (newSize - newSizeMin) / 2
    }

    throw new Exception("Never here")
  }

  /**
    * The `Float` class defines a geometric path with
    * coordinates stored in single precision floating point.
    *
    * @since 1.6
    */
  @SerialVersionUID(6990832515060788886L)
  object Float {
    /**
      * Constructs a new single precision `Path2D` object
      * from an arbitrary `Shape` object, transformed by an
      * `AffineTransform` object.
      * All of the initial geometry and the winding rule for this path are
      * taken from the specified `Shape` object and transformed
      * by the specified `AffineTransform` object.
      *
      * @param s  the specified `Shape` object
      * @param at the specified `AffineTransform` object
      * @since 1.6
      */
    def apply(s: Shape, at: AffineTransform): Float = {
      val res = new Float
      res.init(s, at)
      res
    }

    /**
      * Constructs a new single precision `Path2D` object
      * from an arbitrary `Shape` object.
      * All of the initial geometry and the winding rule for this path are
      * taken from the specified `Shape` object.
      *
      * @param s the specified `Shape` object
      * @since 1.6
      */
    def apply(s: Shape): Float = this(s, null)

    private[geom] def expandCoords(oldCoords: Array[SFloat], needed: Int): Array[SFloat] = {
      val oldSize = oldCoords.length
      val newSizeMin = oldSize + needed
      if (newSizeMin < oldSize) throw new ArrayIndexOutOfBoundsException("coords exceeds maximum capacity !")
      var grow = oldSize
      if (grow > EXPAND_MAX_COORDS) grow = Math.max(EXPAND_MAX_COORDS, oldSize >> 3)
      else if (grow < EXPAND_MIN) grow = EXPAND_MIN
      assert(grow > needed)
      var newSize = oldSize + grow
      if (newSize < newSizeMin) newSize = Integer.MAX_VALUE
      while (true) {
        try return util.Arrays.copyOf(oldCoords, newSize)
        catch {
          case oome: OutOfMemoryError =>
            if (newSize == newSizeMin) throw oome
        }
        newSize = newSizeMin + (newSize - newSizeMin) / 2
      }

      throw new Exception("Never here")
    }

    private[geom] class CopyIterator private[geom](val p2df: Path2D.Float) extends Path2D.Iterator(p2df) {
      private[geom] var floatCoords = p2df.floatCoords

      override def currentSegment(coords: Array[SFloat]): Int = {
        val tpe = path.pointTypes(typeIdx)
        val numCoords = curvecoords(tpe)
        if (numCoords > 0) System.arraycopy(floatCoords, pointIdx, coords, 0, numCoords)
        tpe
      }

      override def currentSegment(coords: Array[SDouble]): Int = {
        val tpe = path.pointTypes(typeIdx)
        val numCoords = curvecoords(tpe)
        if (numCoords > 0) for (i <- 0 until numCoords) {
          coords(i) = floatCoords(pointIdx + i)
        }
        tpe
      }
    }

    private[geom] class TxIterator private[geom](val p2df: Path2D.Float, var affine: AffineTransform)
      extends Path2D.Iterator(p2df) {

      private[geom] var floatCoords = p2df.floatCoords

      override def currentSegment(coords: Array[SFloat]): Int = {
        val tpe = path.pointTypes(typeIdx)
        val numCoords = curvecoords(tpe)
        if (numCoords > 0) affine.transform(floatCoords, pointIdx, coords, 0, numCoords / 2)
        tpe
      }

      override def currentSegment(coords: Array[SDouble]): Int = {
        val tpe = path.pointTypes(typeIdx)
        val numCoords = curvecoords(tpe)
        if (numCoords > 0) affine.transform(floatCoords, pointIdx, coords, 0, numCoords / 2)
        tpe
      }
    }
  }

  /**
    * Constructs a new empty single precision `Path2D` object
    * with the specified winding rule and the specified initial
    * capacity to store path segments.
    * This number is an initial guess as to how many path segments
    * will be added to the path, but the storage is expanded as
    * needed to store whatever path segments are added.
    *
    * @param rule            the winding rule
    * @param initialCapacity the estimate for the number of path segments
    *                        in the path
    * @see #WIND_EVEN_ODD
    * @see #WIND_NON_ZERO
    * @since 1.6
    */
  class Float(rule: Int, initialCapacity: Int) extends Path2D(rule, initialCapacity) with Serializable {
    private[geom] var floatCoords = new Array[SFloat](initialCapacity * 2)

    /**
      * Constructs a new empty single precision `Path2D` object
      * with a default winding rule of `#`.
      *
      * @since 1.6
      */
    def this() = this (WIND_NON_ZERO, INIT_SIZE)

    private[Float] def init(s: Shape, at: AffineTransform): Unit = {
      s match {
        case p2d: Path2D =>
          setWindingRule(p2d.windingRule)
          this.numTypes     = p2d.numTypes
          // trim arrays:
          this.pointTypes   = util.Arrays.copyOf(p2d.pointTypes, p2d.numTypes)
          this.numCoords    = p2d.numCoords
          this.floatCoords  = p2d.cloneCoordsSFloat(at)
        case _ =>
          val pi            = s.getPathIterator(at)
          setWindingRule(pi.getWindingRule)
          this.pointTypes   = new Array[Byte](INIT_SIZE)
          this.floatCoords  = new Array[SFloat](INIT_SIZE * 2)
          append(pi, connect = false)
      }
    }

    /**
      * Constructs a new empty single precision `Path2D` object
      * with the specified winding rule to control operations that
      * require the interior of the path to be defined.
      *
      * @param rule the winding rule
      * @see #WIND_EVEN_ODD
      * @see #WIND_NON_ZERO
      * @since 1.6
      */
    def this(rule: Int) = this(rule, INIT_SIZE)

    override final def trimToSize(): Unit = { // trim arrays:
      if (numTypes  < pointTypes .length) this.pointTypes  = util.Arrays.copyOf(pointTypes , numTypes)
      if (numCoords < floatCoords.length) this.floatCoords = util.Arrays.copyOf(floatCoords, numCoords)
    }

    override private[geom] def cloneCoordsSFloat(at: AffineTransform): Array[SFloat] =
      if (at == null) util.Arrays.copyOf(floatCoords, numCoords)
      else {
        val ret = new Array[SFloat](numCoords)
        at.transform(floatCoords, 0, ret, 0, numCoords / 2)
        ret
      }

    override private[geom] def cloneCoordsSDouble(at: AffineTransform): Array[SDouble] = {
      val ret = new Array[SDouble](numCoords)
      if (at == null) for (i <- 0 until numCoords) {
        ret(i) = floatCoords(i)
      }
      else at.transform(floatCoords, 0, ret, 0, numCoords / 2)
      ret
    }

    override private[geom] def append(x: SFloat, y: SFloat): Unit = {
      floatCoords({
        numCoords += 1; numCoords - 1
      }) = x
      floatCoords({
        numCoords += 1; numCoords - 1
      }) = y
    }

    override private[geom] def append(x: SDouble, y: SDouble): Unit = {
      floatCoords({
        numCoords += 1; numCoords - 1
      }) = x.toFloat
      floatCoords({
        numCoords += 1; numCoords - 1
      }) = y.toFloat
    }

    override private[geom] def getPoint(coordindex: Int): Point2D =
      new Point2D.Float(floatCoords(coordindex), floatCoords(coordindex + 1))

    override private[geom] def needRoom(needMove: Boolean, newCoords: Int): Unit = {
      if ((numTypes == 0) && needMove) throw new IllegalPathStateException("missing initial moveto " + "in path definition")
      if (numTypes >= pointTypes.length) pointTypes = expandPointTypes(pointTypes, 1)
      if (numCoords > (floatCoords.length - newCoords)) floatCoords = Float.expandCoords(floatCoords, newCoords)
    }

    /**
      * ``
      *
      * @since 1.6
      */
    override final def moveTo(x: SDouble, y: SDouble): Unit = {
      if (numTypes > 0 && pointTypes(numTypes - 1) == SEG_MOVETO) {
        floatCoords(numCoords - 2) = x.toFloat
        floatCoords(numCoords - 1) = y.toFloat
      }
      else {
        needRoom(needMove = false, newCoords = 2)
        pointTypes({
          numTypes += 1; numTypes - 1
        }) = SEG_MOVETO
        floatCoords({
          numCoords += 1; numCoords - 1
        }) = x.toFloat
        floatCoords({
          numCoords += 1; numCoords - 1
        }) = y.toFloat
      }
    }

    /**
      * Adds a point to the path by moving to the specified
      * coordinates specified in float precision.
      * <p>
      * This method provides a single precision variant of
      * the double precision `moveTo()` method on the
      * base `Path2D` class.
      *
      * @param x the specified X coordinate
      * @param y the specified Y coordinate
      * @see Path2D#moveTo
      * @since 1.6
      */
    final def moveTo(x: SFloat, y: SFloat): Unit = {
      if (numTypes > 0 && pointTypes(numTypes - 1) == SEG_MOVETO) {
        floatCoords(numCoords - 2) = x
        floatCoords(numCoords - 1) = y
      }
      else {
        needRoom(needMove = false, newCoords = 2)
        pointTypes({
          numTypes += 1; numTypes - 1
        }) = SEG_MOVETO
        floatCoords({
          numCoords += 1; numCoords - 1
        }) = x
        floatCoords({
          numCoords += 1; numCoords - 1
        }) = y
      }
    }

    override final def lineTo(x: SDouble, y: SDouble): Unit = {
      needRoom(needMove = true, newCoords = 2)
      pointTypes({
        numTypes += 1; numTypes - 1
      }) = SEG_LINETO
      floatCoords({
        numCoords += 1; numCoords - 1
      }) = x.toFloat
      floatCoords({
        numCoords += 1; numCoords - 1
      }) = y.toFloat
    }

    /**
      * Adds a point to the path by drawing a straight line from the
      * current coordinates to the new specified coordinates
      * specified in float precision.
      * <p>
      * This method provides a single precision variant of
      * the double precision `lineTo()` method on the
      * base `Path2D` class.
      *
      * @param x the specified X coordinate
      * @param y the specified Y coordinate
      * @see Path2D#lineTo
      * @since 1.6
      */
    final def lineTo(x: SFloat, y: SFloat): Unit = {
      needRoom(needMove = true, newCoords = 2)
      pointTypes({
        numTypes += 1; numTypes - 1
      }) = SEG_LINETO
      floatCoords({
        numCoords += 1; numCoords - 1
      }) = x
      floatCoords({
        numCoords += 1; numCoords - 1
      }) = y
    }

    override final def quadTo(x1: SDouble, y1: SDouble, x2: SDouble, y2: SDouble): Unit = {
      needRoom(needMove = true, newCoords = 4)
      pointTypes({
        numTypes += 1; numTypes - 1
      }) = SEG_QUADTO
      floatCoords({
        numCoords += 1; numCoords - 1
      }) = x1.toFloat
      floatCoords({
        numCoords += 1; numCoords - 1
      }) = y1.toFloat
      floatCoords({
        numCoords += 1; numCoords - 1
      }) = x2.toFloat
      floatCoords({
        numCoords += 1; numCoords - 1
      }) = y2.toFloat
    }

    /**
      * Adds a curved segment, defined by two new points, to the path by
      * drawing a Quadratic curve that intersects both the current
      * coordinates and the specified coordinates `(x2,y2)`,
      * using the specified point `(x1,y1)` as a quadratic
      * parametric control point.
      * All coordinates are specified in float precision.
      * <p>
      * This method provides a single precision variant of
      * the double precision `quadTo()` method on the
      * base `Path2D` class.
      *
      * @param x1 the X coordinate of the quadratic control point
      * @param y1 the Y coordinate of the quadratic control point
      * @param x2 the X coordinate of the final end point
      * @param y2 the Y coordinate of the final end point
      * @see Path2D#quadTo
      * @since 1.6
      */
    final def quadTo(x1: SFloat, y1: SFloat, x2: SFloat, y2: SFloat): Unit = {
      needRoom(needMove = true, newCoords = 4)
      pointTypes({
        numTypes += 1; numTypes - 1
      }) = SEG_QUADTO
      floatCoords({
        numCoords += 1; numCoords - 1
      }) = x1
      floatCoords({
        numCoords += 1; numCoords - 1
      }) = y1
      floatCoords({
        numCoords += 1; numCoords - 1
      }) = x2
      floatCoords({
        numCoords += 1; numCoords - 1
      }) = y2
    }

    override final def curveTo(x1: SDouble, y1: SDouble, x2: SDouble, y2: SDouble, x3: SDouble, y3: SDouble): Unit = {
      needRoom(needMove = true, newCoords = 6)
      pointTypes({
        numTypes += 1; numTypes - 1
      }) = SEG_CUBICTO
      floatCoords({
        numCoords += 1; numCoords - 1
      }) = x1.toFloat
      floatCoords({
        numCoords += 1; numCoords - 1
      }) = y1.toFloat
      floatCoords({
        numCoords += 1; numCoords - 1
      }) = x2.toFloat
      floatCoords({
        numCoords += 1; numCoords - 1
      }) = y2.toFloat
      floatCoords({
        numCoords += 1; numCoords - 1
      }) = x3.toFloat
      floatCoords({
        numCoords += 1; numCoords - 1
      }) = y3.toFloat
    }

    /**
      * Adds a curved segment, defined by three new points, to the path by
      * drawing a B&eacute;zier curve that intersects both the current
      * coordinates and the specified coordinates `(x3,y3)`,
      * using the specified points `(x1,y1)` and `(x2,y2)` as
      * B&eacute;zier control points.
      * All coordinates are specified in float precision.
      * <p>
      * This method provides a single precision variant of
      * the double precision `curveTo()` method on the
      * base `Path2D` class.
      *
      * @param x1 the X coordinate of the first B&eacute;zier control point
      * @param y1 the Y coordinate of the first B&eacute;zier control point
      * @param x2 the X coordinate of the second B&eacute;zier control point
      * @param y2 the Y coordinate of the second B&eacute;zier control point
      * @param x3 the X coordinate of the final end point
      * @param y3 the Y coordinate of the final end point
      * @see Path2D#curveTo
      * @since 1.6
      */
    final def curveTo(x1: SFloat, y1: SFloat, x2: SFloat, y2: SFloat, x3: SFloat, y3: SFloat): Unit = {
      needRoom(needMove = true, newCoords = 6)
      pointTypes({
        numTypes += 1; numTypes - 1
      }) = SEG_CUBICTO
      floatCoords({
        numCoords += 1; numCoords - 1
      }) = x1
      floatCoords({
        numCoords += 1; numCoords - 1
      }) = y1
      floatCoords({
        numCoords += 1; numCoords - 1
      }) = x2
      floatCoords({
        numCoords += 1; numCoords - 1
      }) = y2
      floatCoords({
        numCoords += 1; numCoords - 1
      }) = x3
      floatCoords({
        numCoords += 1; numCoords - 1
      }) = y3
    }

    override private[geom] def pointCrossings(px: SDouble, py: SDouble): Int = {
      if (numTypes == 0) return 0
      var movx = 0.0
      var movy = 0.0
      var curx = 0.0
      var cury = 0.0
      var endx = 0.0
      var endy = 0.0
      val coords = floatCoords
      movx = coords(0)
      curx = movx
      movy = coords(1)
      cury = movy
      var crossings = 0
      var ci = 2
      for (i <- 1 until numTypes) {
        pointTypes(i) match {
          case PathIterator.SEG_MOVETO =>
            if (cury != movy) crossings += Curve.pointCrossingsForLine(px, py, curx, cury, movx, movy)
            curx = coords({
              ci += 1; ci - 1
            })
            movx = curx
            cury = coords({
              ci += 1; ci - 1
            })
            movy = cury

          case PathIterator.SEG_LINETO =>
            endx = coords({
              ci += 1; ci - 1
            })
            endy = coords({
              ci += 1; ci - 1
            })
            crossings += Curve.pointCrossingsForLine(px, py, curx, cury, endx, endy)
            curx = endx
            cury = endy

          case PathIterator.SEG_QUADTO =>
            val xc = coords({
              ci += 1; ci - 1
            })
            val yc = coords({
              ci += 1; ci - 1
            })
            endx = coords({
              ci += 1; ci - 1
            })
            endy = coords({
              ci += 1; ci - 1
            })
            crossings += Curve.pointCrossingsForQuad(px, py, curx, cury, xc, yc, endx, endy, 0)
            curx = endx
            cury = endy

          case PathIterator.SEG_CUBICTO =>
            val xc0 = coords({
              ci += 1; ci - 1
            })
            val yc0 = coords({
              ci += 1; ci - 1
            })
            val xc1 = coords({
              ci += 1; ci - 1
            })
            val yc1 = coords({
              ci += 1; ci - 1
            })
            endx = coords({
              ci += 1; ci - 1
            })
            endy = coords({
              ci += 1; ci - 1
            })
            crossings += Curve.pointCrossingsForCubic(px, py, curx, cury, xc0, yc0, xc1, yc1, endx, endy, 0)
            curx = endx
            cury = endy

          case PathIterator.SEG_CLOSE =>
            if (cury != movy) crossings += Curve.pointCrossingsForLine(px, py, curx, cury, movx, movy)
            curx = movx
            cury = movy

        }
      }
      if (cury != movy) crossings += Curve.pointCrossingsForLine(px, py, curx, cury, movx, movy)
      crossings
    }

    override private[geom] def rectCrossings(rxmin: SDouble, rymin: SDouble, rxmax: SDouble, rymax: SDouble): Int = {
      if (numTypes == 0) return 0
      val coords = floatCoords
      var curx = 0.0
      var cury = 0.0
      var movx = 0.0
      var movy = 0.0
      var endx = 0.0
      var endy = 0.0
      curx = coords(0)
      movx = curx
      cury = coords(1)
      movy = cury
      var crossings = 0
      var ci = 2
      var i = 1
      while ( {
        crossings != Curve.RECT_INTERSECTS && i < numTypes
      }) {
        pointTypes(i) match {
          case PathIterator.SEG_MOVETO =>
            if (curx != movx || cury != movy) crossings = Curve.rectCrossingsForLine(crossings, rxmin, rymin, rxmax, rymax, curx, cury, movx, movy)
            // Count should always be a multiple of 2 here.
            // assert((crossings & 1) != 0);
            curx = coords({
              ci += 1; ci - 1
            })
            movx = curx
            cury = coords({
              ci += 1; ci - 1
            })
            movy = cury

          case PathIterator.SEG_LINETO =>
            endx = coords({
              ci += 1; ci - 1
            })
            endy = coords({
              ci += 1; ci - 1
            })
            crossings = Curve.rectCrossingsForLine(crossings, rxmin, rymin, rxmax, rymax, curx, cury, endx, endy)
            curx = endx
            cury = endy

          case PathIterator.SEG_QUADTO =>
            val xc = coords({
              ci += 1; ci - 1
            })
            val yc = coords({
              ci += 1; ci - 1
            })
            endx = coords({
              ci += 1; ci - 1
            })
            endy = coords({
              ci += 1; ci - 1
            })
            crossings = Curve.rectCrossingsForQuad(crossings, rxmin, rymin, rxmax, rymax, curx, cury, xc, yc, endx, endy, 0)
            curx = endx
            cury = endy

          case PathIterator.SEG_CUBICTO =>
            val xc0 = coords({
              ci += 1; ci - 1
            })
            val yc0 = coords({
              ci += 1; ci - 1
            })
            val xc1 = coords({
              ci += 1; ci - 1
            })
            val yc1 = coords({
              ci += 1; ci - 1
            })
            endx = coords({
              ci += 1; ci - 1
            })
            endy = coords({
              ci += 1; ci - 1
            })
            crossings = Curve.rectCrossingsForCubic(crossings, rxmin, rymin, rxmax, rymax, curx, cury, xc0, yc0, xc1, yc1, endx, endy, 0)
            curx = endx
            cury = endy

          case PathIterator.SEG_CLOSE =>
            if (curx != movx || cury != movy) crossings = Curve.rectCrossingsForLine(crossings, rxmin, rymin, rxmax, rymax, curx, cury, movx, movy)
            curx = movx
            cury = movy

        }

        i += 1
      }
      if (crossings != Curve.RECT_INTERSECTS && (curx != movx || cury != movy)) crossings = Curve.rectCrossingsForLine(crossings, rxmin, rymin, rxmax, rymax, curx, cury, movx, movy)
      crossings
    }

    override final def append(pi: PathIterator, connect: Boolean): Unit = {
      var _connect = connect
      val coords = new Array[SFloat](6)
      while (!pi.isDone) {
        pi.currentSegment(coords) match {
          case SEG_MOVETO =>
            if (!_connect || numTypes < 1 || numCoords < 1) {
              moveTo(coords(0), coords(1))
              return // break //todo: break is not supported

            }
            if (pointTypes(numTypes - 1) != SEG_CLOSE && floatCoords(numCoords - 2) == coords(0) && floatCoords(numCoords - 1) == coords(1)) { // Collapse out initial moveto/lineto
              return // break //todo: break is not supported

            }
            lineTo(coords(0), coords(1))

          case SEG_LINETO =>
            lineTo(coords(0), coords(1))

          case SEG_QUADTO =>
            quadTo(coords(0), coords(1), coords(2), coords(3))

          case SEG_CUBICTO =>
            curveTo(coords(0), coords(1), coords(2), coords(3), coords(4), coords(5))

          case SEG_CLOSE =>
            closePath()

        }
        pi.next()
        _connect = false
      }
    }

    override final def transform(at: AffineTransform): Unit = {
      at.transform(floatCoords, 0, floatCoords, 0, numCoords / 2)
    }

    override final def getBounds2D: Rectangle2D = {
      var x1 = 0.0f
      var y1 = 0.0f
      var x2 = 0.0f
      var y2 = 0.0f
      var i = numCoords
      if (i > 0) {
        y1 = floatCoords({
          i -= 1; i
        })
        y2 = y1
        x1 = floatCoords({
          i -= 1; i
        })
        x2 = x1
        while ( {
          i > 0
        }) {
          val y = floatCoords({
            i -= 1; i
          })
          val x = floatCoords({
            i -= 1; i
          })
          if (x < x1) x1 = x
          if (y < y1) y1 = y
          if (x > x2) x2 = x
          if (y > y2) y2 = y
        }
      }
      else {
        x1 = 0.0f
        y1 = 0.0f
        x2 = 0.0f
        y2 = 0.0f
      }
      new Rectangle2D.Float(x1, y1, x2 - x1, y2 - y1)
    }

    /**
      * ``
      * <p>
      * The iterator for this class is not multi-threaded safe,
      * which means that the `Path2D` class does not
      * guarantee that modifications to the geometry of this
      * `Path2D` object do not affect any iterations of
      * that geometry that are already in process.
      *
      * @since 1.6
      */
    override final def getPathIterator(at: AffineTransform): PathIterator = if (at == null) new Float.CopyIterator(this)
    else new Float.TxIterator(this, at)

    /**
      * Creates a new object of the same class as this object.
      *
      * @return a clone of this instance.
      * throws OutOfMemoryError    if there is not enough memory.
      * @see java.lang.Cloneable
      * @since 1.6
      */
    override final def clone: Any = Path2D.Float(this)

    /**
      * Writes the default serializable fields to the
      * `ObjectOutputStream` followed by an explicit
      * serialization of the path segments stored in this
      * path.
      */
    @throws[java.io.IOException]
    private def writeObject(s: ObjectOutputStream): Unit = {
      super.writeObject(s, isdbl = false)
    }

    /**
      * Reads the default serializable fields from the
      * `ObjectInputStream` followed by an explicit
      * serialization of the path segments stored in this
      * path.
      * <p>
      * There are no default serializable fields as of 1.6.
      * <p>
      * The serial data for this object is described in the
      * writeObject method.
      *
      * @since 1.6
      */
    @throws[java.lang.ClassNotFoundException]
    @throws[java.io.IOException]
    private def readObject(s: ObjectInputStream): Unit = {
      super.readObject(s, storedbl = false)
    }
  }

  /**
    * The `Double` class defines a geometric path with
    * coordinates stored in double precision floating point.
    */
  object Double {

    /**
      * Constructs a new double precision `Path2D` object
      * from an arbitrary `Shape` object, transformed by an
      * `AffineTransform` object.
      * All of the initial geometry and the winding rule for this path are
      * taken from the specified `Shape` object and transformed
      * by the specified `AffineTransform` object.
      *
      * @param s  the specified `Shape` object
      * @param at the specified `AffineTransform` object
      * @since 1.6
      */
    def apply(s: Shape, at: AffineTransform): Double = {
      val res = new Double
      res.init(s, at)
      res
    }

    /**
      * Constructs a new double precision `Path2D` object
      * from an arbitrary `Shape` object.
      * All of the initial geometry and the winding rule for this path are
      * taken from the specified `Shape` object.
      *
      * @param s the specified `Shape` object
      * @since 1.6
      */
    def apply(s: Shape): Double = this(s, null)

    private[geom] def expandCoords(oldCoords: Array[SDouble], needed: Int): Array[SDouble] = {
      val oldSize = oldCoords.length
      val newSizeMin = oldSize + needed
      if (newSizeMin < oldSize) throw new ArrayIndexOutOfBoundsException("coords exceeds maximum capacity !")
      var grow = oldSize
      if (grow > EXPAND_MAX_COORDS) grow = Math.max(EXPAND_MAX_COORDS, oldSize >> 3)
      else if (grow < EXPAND_MIN) grow = EXPAND_MIN
      assert(grow > needed)
      var newSize = oldSize + grow
      if (newSize < newSizeMin) newSize = Integer.MAX_VALUE
      while (true) {
        try return util.Arrays.copyOf(oldCoords, newSize)
        catch {
          case oome: OutOfMemoryError =>
            if (newSize == newSizeMin) throw oome
        }
        newSize = newSizeMin + (newSize - newSizeMin) / 2
      }

      throw new Exception("Never here")
    }

    private[geom] class CopyIterator private[geom](val p2dd: Path2D.Double) extends Path2D.Iterator(p2dd) {
      private[geom] val doubleCoords = p2dd.doubleCoords

      override def currentSegment(coords: Array[SFloat]): Int = {
        val tpe = path.pointTypes(typeIdx)
        val numCoords = curvecoords(tpe)
        if (numCoords > 0) for (i <- 0 until numCoords) {
          coords(i) = doubleCoords(pointIdx + i).toFloat
        }
        tpe
      }

      override def currentSegment(coords: Array[SDouble]): Int = {
        val tpe = path.pointTypes(typeIdx)
        val numCoords = curvecoords(tpe)
        if (numCoords > 0) System.arraycopy(doubleCoords, pointIdx, coords, 0, numCoords)
        tpe
      }
    }

    private[geom] class TxIterator private[geom](val p2dd: Path2D.Double, var affine: AffineTransform) extends Path2D.Iterator(p2dd) {
      private[geom] val doubleCoords = p2dd.doubleCoords

      override def currentSegment(coords: Array[SFloat]): Int = {
        val tpe = path.pointTypes(typeIdx)
        val numCoords = curvecoords(tpe)
        if (numCoords > 0) affine.transform(doubleCoords, pointIdx, coords, 0, numCoords / 2)
        tpe
      }

      override def currentSegment(coords: Array[SDouble]): Int = {
        val tpe = path.pointTypes(typeIdx)
        val numCoords = curvecoords(tpe)
        if (numCoords > 0) affine.transform(doubleCoords, pointIdx, coords, 0, numCoords / 2)
        tpe
      }
    }
  }

  /**
    * Constructs a new empty double precision `Path2D` object
    * with the specified winding rule and the specified initial
    * capacity to store path segments.
    * This number is an initial guess as to how many path segments
    * are in the path, but the storage is expanded as needed to store
    * whatever path segments are added to this path.
    *
    * @param rule            the winding rule
    * @param initialCapacity the estimate for the number of path segments
    *                        in the path
    * @see #WIND_EVEN_ODD
    * @see #WIND_NON_ZERO
    * @since 1.6
    */
  class Double(rule: Int, initialCapacity: Int) extends Path2D(rule, initialCapacity) with Serializable {
    private[geom] var doubleCoords = new Array[SDouble](initialCapacity * 2)

    /**
      * Constructs a new empty double precision `Path2D` object
      * with a default winding rule of `#`.
      *
      * @since 1.6
      */
    def this() = this(WIND_NON_ZERO, INIT_SIZE)

    /**
      * Constructs a new empty double precision `Path2D` object
      * with the specified winding rule to control operations that
      * require the interior of the path to be defined.
      *
      * @param rule the winding rule
      * @see #WIND_EVEN_ODD
      * @see #WIND_NON_ZERO
      * @since 1.6
      */
    def this(rule: Int) = this(rule, INIT_SIZE)

    /*private[Double]*/ def init(s: Shape, at: AffineTransform): Unit = {
      s match {
        case p2d: Path2D =>
          setWindingRule(p2d.windingRule)
          this.numTypes     = p2d.numTypes
          // trim arrays:
          this.pointTypes   = util.Arrays.copyOf(p2d.pointTypes, p2d.numTypes)
          this.numCoords    = p2d.numCoords
          this.doubleCoords = p2d.cloneCoordsSDouble(at)
        case _ =>
          val pi            = s.getPathIterator(at)
          setWindingRule(pi.getWindingRule)
          this.pointTypes   = new Array[Byte](INIT_SIZE)
          this.doubleCoords = new Array[SDouble](INIT_SIZE * 2)
          append(pi, connect = false)
      }
    }

    override final def trimToSize(): Unit = {
      if (numTypes  < pointTypes  .length) this.pointTypes   = util.Arrays.copyOf(pointTypes  , numTypes)
      if (numCoords < doubleCoords.length) this.doubleCoords = util.Arrays.copyOf(doubleCoords, numCoords)
    }

    override private[geom] def cloneCoordsSFloat(at: AffineTransform): Array[SFloat] = {
      val ret = new Array[SFloat](numCoords)
      if (at == null) for (i <- 0 until numCoords) {
        ret(i) = doubleCoords(i).toFloat
      }
      else at.transform(doubleCoords, 0, ret, 0, numCoords / 2)
      ret
    }

    override private[geom] def cloneCoordsSDouble(at: AffineTransform): Array[SDouble] = {
      if (at == null) util.Arrays.copyOf(doubleCoords, numCoords)
      else {
        val ret = new Array[SDouble](numCoords)
        at.transform(doubleCoords, 0, ret, 0, numCoords / 2)
        ret
      }
    }

    override private[geom] def append(x: SFloat, y: SFloat): Unit = {
      doubleCoords({
        numCoords += 1; numCoords - 1
      }) = x
      doubleCoords({
        numCoords += 1; numCoords - 1
      }) = y
    }

    override private[geom] def append(x: SDouble, y: SDouble): Unit = {
      doubleCoords({
        numCoords += 1; numCoords - 1
      }) = x
      doubleCoords({
        numCoords += 1; numCoords - 1
      }) = y
    }

    override private[geom] def getPoint(coordindex: Int) =
      new Point2D.Double(doubleCoords(coordindex), doubleCoords(coordindex + 1))

    override private[geom] def needRoom(needMove: Boolean, newCoords: Int): Unit = {
      if ((numTypes == 0) && needMove) throw new IllegalPathStateException("missing initial moveto " + "in path definition")
      if (numTypes >= pointTypes.length) pointTypes = expandPointTypes(pointTypes, 1)
      if (numCoords > (doubleCoords.length - newCoords)) doubleCoords = Double.expandCoords(doubleCoords, newCoords)
    }

    override final def moveTo(x: SDouble, y: SDouble): Unit = {
      if (numTypes > 0 && pointTypes(numTypes - 1) == SEG_MOVETO) {
        doubleCoords(numCoords - 2) = x
        doubleCoords(numCoords - 1) = y
      }
      else {
        needRoom(needMove = false, newCoords = 2)
        pointTypes({
          numTypes += 1; numTypes - 1
        }) = SEG_MOVETO
        doubleCoords({
          numCoords += 1; numCoords - 1
        }) = x
        doubleCoords({
          numCoords += 1; numCoords - 1
        }) = y
      }
    }

    override final def lineTo(x: SDouble, y: SDouble): Unit = {
      needRoom(needMove = true, newCoords = 2)
      pointTypes({
        numTypes += 1; numTypes - 1
      }) = SEG_LINETO
      doubleCoords({
        numCoords += 1; numCoords - 1
      }) = x
      doubleCoords({
        numCoords += 1; numCoords - 1
      }) = y
    }

    override final def quadTo(x1: SDouble, y1: SDouble, x2: SDouble, y2: SDouble): Unit = {
      needRoom(needMove = true, newCoords = 4)
      pointTypes({
        numTypes += 1; numTypes - 1
      }) = SEG_QUADTO
      doubleCoords({
        numCoords += 1; numCoords - 1
      }) = x1
      doubleCoords({
        numCoords += 1; numCoords - 1
      }) = y1
      doubleCoords({
        numCoords += 1; numCoords - 1
      }) = x2
      doubleCoords({
        numCoords += 1; numCoords - 1
      }) = y2
    }

    override final def curveTo(x1: SDouble, y1: SDouble, x2: SDouble, y2: SDouble, x3: SDouble, y3: SDouble): Unit = {
      needRoom(needMove = true, newCoords = 6)
      pointTypes({
        numTypes += 1; numTypes - 1
      }) = SEG_CUBICTO
      doubleCoords({
        numCoords += 1; numCoords - 1
      }) = x1
      doubleCoords({
        numCoords += 1; numCoords - 1
      }) = y1
      doubleCoords({
        numCoords += 1; numCoords - 1
      }) = x2
      doubleCoords({
        numCoords += 1; numCoords - 1
      }) = y2
      doubleCoords({
        numCoords += 1; numCoords - 1
      }) = x3
      doubleCoords({
        numCoords += 1; numCoords - 1
      }) = y3
    }

    override private[geom] def pointCrossings(px: SDouble, py: SDouble): Int = {
      if (numTypes == 0) return 0
      var movx = 0.0
      var movy = 0.0
      var curx = 0.0
      var cury = 0.0
      var endx = 0.0
      var endy = 0.0
      val coords = doubleCoords
      curx = coords(0)
      movx = curx
      cury = coords(1)
      movy = cury
      var crossings = 0
      var ci = 2
      for (i <- 1 until numTypes) {
        pointTypes(i) match {
          case PathIterator.SEG_MOVETO =>
            if (cury != movy) crossings += Curve.pointCrossingsForLine(px, py, curx, cury, movx, movy)
            curx = coords({
              ci += 1; ci - 1
            })
            movx = curx

            cury = coords({
              ci += 1; ci - 1
            })
            movy = cury

          case PathIterator.SEG_LINETO =>
            endx = coords({
              ci += 1; ci - 1
            })
            endy = coords({
              ci += 1; ci - 1
            })
            crossings += Curve.pointCrossingsForLine(px, py, curx, cury, endx, endy)
            curx = endx
            cury = endy

          case PathIterator.SEG_QUADTO =>
            val xc = coords({
              ci += 1; ci - 1
            })
            val yc = coords({
              ci += 1; ci - 1
            })
            endx = coords({
              ci += 1; ci - 1
            })
            endy = coords({
              ci += 1; ci - 1
            })
            crossings += Curve.pointCrossingsForQuad(px, py, curx, cury, xc, yc, endx, endy, 0)
            curx = endx
            cury = endy

          case PathIterator.SEG_CUBICTO =>
            val xc0 = coords({
              ci += 1; ci - 1
            })
            val yc0 = coords({
              ci += 1; ci - 1
            })
            val xc1 = coords({
              ci += 1; ci - 1
            })
            val yc1 = coords({
              ci += 1; ci - 1
            })
            endx = coords({
              ci += 1; ci - 1
            })
            endy = coords({
              ci += 1; ci - 1
            })
            crossings += Curve.pointCrossingsForCubic(px, py, curx, cury, xc0, yc0, xc1, yc1, endx, endy, 0)
            curx = endx
            cury = endy

          case PathIterator.SEG_CLOSE =>
            if (cury != movy) crossings += Curve.pointCrossingsForLine(px, py, curx, cury, movx, movy)
            curx = movx
            cury = movy

        }
      }
      if (cury != movy) crossings += Curve.pointCrossingsForLine(px, py, curx, cury, movx, movy)
      crossings
    }

    override private[geom] def rectCrossings(rxmin: SDouble, rymin: SDouble, rxmax: SDouble, rymax: SDouble): Int = {
      if (numTypes == 0) return 0
      val coords = doubleCoords
      var curx = 0.0
      var cury = 0.0
      var movx = 0.0
      var movy = 0.0
      var endx = 0.0
      var endy = 0.0
      curx = coords(0)
      movx = curx
      cury = coords(1)
      movy = cury
      var crossings = 0
      var ci = 2
      var i = 1
      while ( {
        crossings != Curve.RECT_INTERSECTS && i < numTypes
      }) {
        pointTypes(i) match {
          case PathIterator.SEG_MOVETO =>
            if (curx != movx || cury != movy) crossings = Curve.rectCrossingsForLine(crossings, rxmin, rymin, rxmax, rymax, curx, cury, movx, movy)
            curx = coords({
              ci += 1; ci - 1
            })
            movx = curx
            cury = coords({
              ci += 1; ci - 1
            })
            movy = cury

          case PathIterator.SEG_LINETO =>
            endx = coords({
              ci += 1; ci - 1
            })
            endy = coords({
              ci += 1; ci - 1
            })
            crossings = Curve.rectCrossingsForLine(crossings, rxmin, rymin, rxmax, rymax, curx, cury, endx, endy)
            curx = endx
            cury = endy

          case PathIterator.SEG_QUADTO =>
            val xc = coords({
              ci += 1; ci - 1
            })
            val yc = coords({
              ci += 1; ci - 1
            })
            endx = coords({
              ci += 1; ci - 1
            })
            endy = coords({
              ci += 1; ci - 1
            })
            crossings = Curve.rectCrossingsForQuad(crossings, rxmin, rymin, rxmax, rymax, curx, cury, xc, yc, endx, endy, 0)
            curx = endx
            cury = endy

          case PathIterator.SEG_CUBICTO =>
            val xc0 = coords({
              ci += 1; ci - 1
            })
            val yc0 = coords({
              ci += 1; ci - 1
            })
            val xc1 = coords({
              ci += 1; ci - 1
            })
            val yc1 = coords({
              ci += 1; ci - 1
            })
            endx = coords({
              ci += 1; ci - 1
            })
            endy = coords({
              ci += 1; ci - 1
            })
            crossings = Curve.rectCrossingsForCubic(crossings, rxmin, rymin, rxmax, rymax, curx, cury, xc0, yc0, xc1, yc1, endx, endy, 0)
            curx = endx
            cury = endy

          case PathIterator.SEG_CLOSE =>
            if (curx != movx || cury != movy) crossings = Curve.rectCrossingsForLine(crossings, rxmin, rymin, rxmax, rymax, curx, cury, movx, movy)
            curx = movx
            cury = movy

        }

        i += 1
      }
      if (crossings != Curve.RECT_INTERSECTS && (curx != movx || cury != movy)) crossings = Curve.rectCrossingsForLine(crossings, rxmin, rymin, rxmax, rymax, curx, cury, movx, movy)
      crossings
    }

    override final def append(pi: PathIterator, connect: Boolean): Unit = {
      var _connect = connect
      val coords = new Array[SDouble](6)
      while (!pi.isDone) {
        pi.currentSegment(coords) match {
          case SEG_MOVETO =>
            if (!_connect || numTypes < 1 || numCoords < 1) {
              moveTo(coords(0), coords(1))
              return // break //todo: break is not supported

            }
            if (pointTypes(numTypes - 1) != SEG_CLOSE && doubleCoords(numCoords - 2) == coords(0) && doubleCoords(numCoords - 1) == coords(1)) {
              return // break //todo: break is not supported
            }
            lineTo(coords(0), coords(1))

          case SEG_LINETO =>
            lineTo(coords(0), coords(1))

          case SEG_QUADTO =>
            quadTo(coords(0), coords(1), coords(2), coords(3))

          case SEG_CUBICTO =>
            curveTo(coords(0), coords(1), coords(2), coords(3), coords(4), coords(5))

          case SEG_CLOSE =>
            closePath()

        }
        pi.next()
        _connect = false
      }
    }

    override final def transform(at: AffineTransform): Unit = {
      at.transform(doubleCoords, 0, doubleCoords, 0, numCoords / 2)
    }

    override final def getBounds2D: Rectangle2D = {
      var x1 = 0.0
      var y1 = 0.0
      var x2 = 0.0
      var y2 = 0.0
      var i = numCoords
      if (i > 0) {
        y1 = doubleCoords({
          i -= 1; i
        })
        y2 = y1
        x1 = doubleCoords({
          i -= 1; i
        })
        x2 = x1
        while ( {
          i > 0
        }) {
          val y = doubleCoords({
            i -= 1; i
          })
          val x = doubleCoords({
            i -= 1; i
          })
          if (x < x1) x1 = x
          if (y < y1) y1 = y
          if (x > x2) x2 = x
          if (y > y2) y2 = y
        }
      }
      else {
        x1 = 0.0
        y1 = 0.0
        x2 = 0.0
        y2 = 0.0
      }
      new Rectangle2D.Double(x1, y1, x2 - x1, y2 - y1)
    }

    /**
      * ``
      * <p>
      * The iterator for this class is not multi-threaded safe,
      * which means that the `Path2D` class does not
      * guarantee that modifications to the geometry of this
      * `Path2D` object do not affect any iterations of
      * that geometry that are already in process.
      *
      * @param at an `AffineTransform`
      * @return a new `PathIterator` that iterates along the boundary
      *         of this `Shape` and provides access to the geometry
      *         of this `Shape`'s outline
      * @since 1.6
      */
    override final def getPathIterator(at: AffineTransform): PathIterator = if (at == null) new Double.CopyIterator(this)
    else new Double.TxIterator(this, at)

    override final def clone: Any = Path2D.Double(this)

    /**
      * Writes the default serializable fields to the
      * `ObjectOutputStream` followed by an explicit
      * serialization of the path segments stored in this
      * path.
      */
    @throws[java.io.IOException]
    private def writeObject(s: ObjectOutputStream): Unit = {
      super.writeObject(s, isdbl = true)
    }

    @throws[java.lang.ClassNotFoundException]
    @throws[java.io.IOException]
    private def readObject(s: ObjectInputStream): Unit = {
      super.readObject(s, storedbl = true)
    }
  }

  /**
    * Tests if the specified coordinates are inside the closed
    * boundary of the specified `PathIterator`.
    * <p>
    * This method provides a basic facility for implementors of
    * the `Shape` interface to implement support for the
    * `Shape` method.
    *
    * @param pi the specified `PathIterator`
    * @param x  the specified X coordinate
    * @param y  the specified Y coordinate
    * @return `true` if the specified coordinates are inside the
    *         specified `PathIterator`; `false` otherwise
    * @since 1.6
    */
  def contains(pi: PathIterator, x: SDouble, y: SDouble): Boolean = if (x * 0.0 + y * 0.0 == 0.0) {
    /* N * 0.0 is 0.0 only if N is finite.
                * Here we know that both x and y are finite.
                */
    val mask = if (pi.getWindingRule == WIND_NON_ZERO) -1 else 1
    val cross = Curve.pointCrossingsForPath(pi, x, y)
    (cross & mask) != 0
  }
  else {
    /* Either x or y was infinite or NaN.
                * A NaN always produces a negative response to any test
                * and Infinity values cannot be "inside" any path so
                * they should return false as well.
                */ false
  }

  /**
    * Tests if the specified `Point2D` is inside the closed
    * boundary of the specified `PathIterator`.
    * <p>
    * This method provides a basic facility for implementors of
    * the `Shape` interface to implement support for the
    * `Shape` method.
    *
    * @param pi the specified `PathIterator`
    * @param p  the specified `Point2D`
    * @return `true` if the specified coordinates are inside the
    *         specified `PathIterator`; `false` otherwise
    * @since 1.6
    */
  def contains(pi: PathIterator, p: Point2D): Boolean = contains(pi, p.getX, p.getY)

  /**
    * Tests if the specified rectangular area is entirely inside the
    * closed boundary of the specified `PathIterator`.
    * <p>
    * This method provides a basic facility for implementors of
    * the `Shape` interface to implement support for the
    * `Shape` method.
    * <p>
    * This method object may conservatively return false in
    * cases where the specified rectangular area intersects a
    * segment of the path, but that segment does not represent a
    * boundary between the interior and exterior of the path.
    * Such segments could lie entirely within the interior of the
    * path if they are part of a path with a `#`
    * winding rule or if the segments are retraced in the reverse
    * direction such that the two sets of segments cancel each
    * other out without any exterior area falling between them.
    * To determine whether segments represent true boundaries of
    * the interior of the path would require extensive calculations
    * involving all of the segments of the path and the winding
    * rule and are thus beyond the scope of this implementation.
    *
    * @param pi the specified `PathIterator`
    * @param x  the specified X coordinate
    * @param y  the specified Y coordinate
    * @param w  the width of the specified rectangular area
    * @param h  the height of the specified rectangular area
    * @return `true` if the specified `PathIterator` contains
    *         the specified rectangular area; `false` otherwise.
    * @since 1.6
    */
  def contains(pi: PathIterator, x: SDouble, y: SDouble, w: SDouble, h: SDouble): Boolean = {
    if (java.lang.Double.isNaN(x + w) || java.lang.Double.isNaN(y + h)) {
      /* [xy]+[wh] is NaN if any of those values are NaN,
                  * or if adding the two together would produce NaN
                  * by virtue of adding opposing Infinite values.
                  * Since we need to add them below, their sum must
                  * not be NaN.
                  * We return false because NaN always produces a
                  * negative response to tests
                  */ return false
    }
    if (w <= 0 || h <= 0) return false
    val mask = if (pi.getWindingRule == WIND_NON_ZERO) -1
    else 2
    val crossings = Curve.rectCrossingsForPath(pi, x, y, x + w, y + h)
    crossings != Curve.RECT_INTERSECTS && (crossings & mask) != 0
  }

  /**
    * Tests if the specified `Rectangle2D` is entirely inside the
    * closed boundary of the specified `PathIterator`.
    * <p>
    * This method provides a basic facility for implementors of
    * the `Shape` interface to implement support for the
    * `Shape` method.
    * <p>
    * This method object may conservatively return false in
    * cases where the specified rectangular area intersects a
    * segment of the path, but that segment does not represent a
    * boundary between the interior and exterior of the path.
    * Such segments could lie entirely within the interior of the
    * path if they are part of a path with a `#`
    * winding rule or if the segments are retraced in the reverse
    * direction such that the two sets of segments cancel each
    * other out without any exterior area falling between them.
    * To determine whether segments represent true boundaries of
    * the interior of the path would require extensive calculations
    * involving all of the segments of the path and the winding
    * rule and are thus beyond the scope of this implementation.
    *
    * @param pi the specified `PathIterator`
    * @param r  a specified `Rectangle2D`
    * @return `true` if the specified `PathIterator` contains
    *         the specified `Rectangle2D`; `false` otherwise.
    * @since 1.6
    */
  def contains(pi: PathIterator, r: Rectangle2D): Boolean = contains(pi, r.getX, r.getY, r.getWidth, r.getHeight)

  /**
    * Tests if the interior of the specified `PathIterator`
    * intersects the interior of a specified set of rectangular
    * coordinates.
    * <p>
    * This method provides a basic facility for implementors of
    * the `Shape` interface to implement support for the
    * `Shape` method.
    * <p>
    * This method object may conservatively return true in
    * cases where the specified rectangular area intersects a
    * segment of the path, but that segment does not represent a
    * boundary between the interior and exterior of the path.
    * Such a case may occur if some set of segments of the
    * path are retraced in the reverse direction such that the
    * two sets of segments cancel each other out without any
    * interior area between them.
    * To determine whether segments represent true boundaries of
    * the interior of the path would require extensive calculations
    * involving all of the segments of the path and the winding
    * rule and are thus beyond the scope of this implementation.
    *
    * @param pi the specified `PathIterator`
    * @param x  the specified X coordinate
    * @param y  the specified Y coordinate
    * @param w  the width of the specified rectangular coordinates
    * @param h  the height of the specified rectangular coordinates
    * @return `true` if the specified `PathIterator` and
    *         the interior of the specified set of rectangular
    *         coordinates intersect each other; `false` otherwise.
    * @since 1.6
    */
  def intersects(pi: PathIterator, x: SDouble, y: SDouble, w: SDouble, h: SDouble): Boolean = {
    if (java.lang.Double.isNaN(x + w) || java.lang.Double.isNaN(y + h)) return false
    if (w <= 0 || h <= 0) return false
    val mask = if (pi.getWindingRule == WIND_NON_ZERO) -1
    else 2
    val crossings = Curve.rectCrossingsForPath(pi, x, y, x + w, y + h)
    crossings == Curve.RECT_INTERSECTS || (crossings & mask) != 0
  }

  /**
    * Tests if the interior of the specified `PathIterator`
    * intersects the interior of a specified `Rectangle2D`.
    * <p>
    * This method provides a basic facility for implementors of
    * the `Shape` interface to implement support for the
    * `Shape` method.
    * <p>
    * This method object may conservatively return true in
    * cases where the specified rectangular area intersects a
    * segment of the path, but that segment does not represent a
    * boundary between the interior and exterior of the path.
    * Such a case may occur if some set of segments of the
    * path are retraced in the reverse direction such that the
    * two sets of segments cancel each other out without any
    * interior area between them.
    * To determine whether segments represent true boundaries of
    * the interior of the path would require extensive calculations
    * involving all of the segments of the path and the winding
    * rule and are thus beyond the scope of this implementation.
    *
    * @param pi the specified `PathIterator`
    * @param r  the specified `Rectangle2D`
    * @return `true` if the specified `PathIterator` and
    *         the interior of the specified `Rectangle2D`
    *         intersect each other; `false` otherwise.
    * @since 1.6
    */
  def intersects(pi: PathIterator, r: Rectangle2D): Boolean = intersects(pi, r.getX, r.getY, r.getWidth, r.getHeight)

  /*
       * Support fields and methods for serializing the subclasses.
       */
  private final val SERIAL_STORAGE_FLT_ARRAY  = 0x30
  private final val SERIAL_STORAGE_DBL_ARRAY  = 0x31
  private final val SERIAL_SEG_FLT_MOVETO     = 0x40
  private final val SERIAL_SEG_FLT_LINETO     = 0x41
  private final val SERIAL_SEG_FLT_QUADTO     = 0x42
  private final val SERIAL_SEG_FLT_CUBICTO    = 0x43
  private final val SERIAL_SEG_DBL_MOVETO     = 0x50
  private final val SERIAL_SEG_DBL_LINETO     = 0x51
  private final val SERIAL_SEG_DBL_QUADTO     = 0x52
  private final val SERIAL_SEG_DBL_CUBICTO    = 0x53
  private final val SERIAL_SEG_CLOSE          = 0x60
  private final val SERIAL_PATH_END           = 0x61

  private[geom] object Iterator {
    private[geom] val curvecoords = Array(2, 2, 4, 6, 0)
  }

  abstract private[geom] class Iterator private[geom](var path: Path2D) extends PathIterator {
    private[geom] var typeIdx = 0
    private[geom] var pointIdx = 0

    override def getWindingRule: Int = path.getWindingRule

    override def isDone: Boolean = typeIdx >= path.numTypes

    override def next(): Unit = {
      val tpe = path.pointTypes({
        typeIdx += 1; typeIdx - 1
      })
      pointIdx += Iterator.curvecoords(tpe)
    }
  }
}

/**
  * Constructs a new empty `Path2D` object.
  * It is assumed that the package sibling subclass that is
  * defaulting to this constructor will fill in all values.
  *
  * @since 1.6
  */
/* private protected */
abstract class Path2D private[geom]() extends Shape with Cloneable {
  private[geom] var pointTypes: Array[Byte] = null
  private[geom] var numTypes    = 0
  private[geom] var numCoords   = 0
  private[geom] var windingRule = 0

  /**
    * Constructs a new `Path2D` object from the given
    * specified initial values.
    * This method is only intended for internal use and should
    * not be made public if the other constructors for this class
    * are ever exposed.
    *
    * @param rule         the winding rule
    * @param initialTypes the size to make the initial array to
    *                     store the path segment types
    * @since 1.6
    */
  def this(rule: Int, initialTypes: Int) = {
    this()
    setWindingRule(rule)
    this.pointTypes = new Array[Byte](initialTypes)
  }

  private[geom] def cloneCoordsSFloat(at: AffineTransform): Array[SFloat]

  private[geom] def cloneCoordsSDouble(at: AffineTransform): Array[SDouble]

  private[geom] def append(x: SFloat, y: SFloat): Unit

  private[geom] def append(x: SDouble, y: SDouble): Unit

  private[geom] def getPoint(coordindex: Int): Point2D

  private[geom] def needRoom(needMove: Boolean, newCoords: Int): Unit

  private[geom] def pointCrossings(px: SDouble, py: SDouble): Int

  private[geom] def rectCrossings(rxmin: SDouble, rymin: SDouble, rxmax: SDouble, rymax: SDouble): Int

  /**
    * Adds a point to the path by moving to the specified
    * coordinates specified in double precision.
    *
    * @param x the specified X coordinate
    * @param y the specified Y coordinate
    * @since 1.6
    */
  def moveTo(x: SDouble, y: SDouble): Unit

  /**
    * Adds a point to the path by drawing a straight line from the
    * current coordinates to the new specified coordinates
    * specified in double precision.
    *
    * @param x the specified X coordinate
    * @param y the specified Y coordinate
    * @since 1.6
    */
  def lineTo(x: SDouble, y: SDouble): Unit

  /**
    * Adds a curved segment, defined by two new points, to the path by
    * drawing a Quadratic curve that intersects both the current
    * coordinates and the specified coordinates `(x2,y2)`,
    * using the specified point `(x1,y1)` as a quadratic
    * parametric control point.
    * All coordinates are specified in double precision.
    *
    * @param x1 the X coordinate of the quadratic control point
    * @param y1 the Y coordinate of the quadratic control point
    * @param x2 the X coordinate of the final end point
    * @param y2 the Y coordinate of the final end point
    * @since 1.6
    */
  def quadTo(x1: SDouble, y1: SDouble, x2: SDouble, y2: SDouble): Unit

  /**
    * Adds a curved segment, defined by three new points, to the path by
    * drawing a B&eacute;zier curve that intersects both the current
    * coordinates and the specified coordinates `(x3,y3)`,
    * using the specified points `(x1,y1)` and `(x2,y2)` as
    * B&eacute;zier control points.
    * All coordinates are specified in double precision.
    *
    * @param x1 the X coordinate of the first B&eacute;zier control point
    * @param y1 the Y coordinate of the first B&eacute;zier control point
    * @param x2 the X coordinate of the second B&eacute;zier control point
    * @param y2 the Y coordinate of the second B&eacute;zier control point
    * @param x3 the X coordinate of the final end point
    * @param y3 the Y coordinate of the final end point
    * @since 1.6
    */
  def curveTo(x1: SDouble, y1: SDouble, x2: SDouble, y2: SDouble, x3: SDouble, y3: SDouble): Unit

  /**
    * Closes the current subpath by drawing a straight line back to
    * the coordinates of the last `moveTo`.  If the path is already
    * closed then this method has no effect.
    *
    * @since 1.6
    */
  final def closePath(): Unit = {
    if (numTypes == 0 || pointTypes(numTypes - 1) != Path2D.SEG_CLOSE) {
      needRoom(needMove = true, newCoords = 0)
      pointTypes({
        numTypes += 1; numTypes - 1
      }) = Path2D.SEG_CLOSE
    }
  }

  /**
    * Appends the geometry of the specified `Shape` object to the
    * path, possibly connecting the new geometry to the existing path
    * segments with a line segment.
    * If the `connect` parameter is `true` and the
    * path is not empty then any initial `moveTo` in the
    * geometry of the appended `Shape`
    * is turned into a `lineTo` segment.
    * If the destination coordinates of such a connecting `lineTo`
    * segment match the ending coordinates of a currently open
    * subpath then the segment is omitted as superfluous.
    * The winding rule of the specified `Shape` is ignored
    * and the appended geometry is governed by the winding
    * rule specified for this path.
    *
    * @param s       the `Shape` whose geometry is appended
    *                to this path
    * @param connect a boolean to control whether or not to turn an initial
    *                `moveTo` segment into a `lineTo` segment
    *                to connect the new geometry to the existing path
    * @since 1.6
    */
  final def append(s: Shape, connect: Boolean): Unit = {
    append(s.getPathIterator(null), connect)
  }

  /**
    * Appends the geometry of the specified
    * `PathIterator` object
    * to the path, possibly connecting the new geometry to the existing
    * path segments with a line segment.
    * If the `connect` parameter is `true` and the
    * path is not empty then any initial `moveTo` in the
    * geometry of the appended `Shape` is turned into a
    * `lineTo` segment.
    * If the destination coordinates of such a connecting `lineTo`
    * segment match the ending coordinates of a currently open
    * subpath then the segment is omitted as superfluous.
    * The winding rule of the specified `Shape` is ignored
    * and the appended geometry is governed by the winding
    * rule specified for this path.
    *
    * @param pi      the `PathIterator` whose geometry is appended to
    *                this path
    * @param connect a boolean to control whether or not to turn an initial
    *                `moveTo` segment into a `lineTo` segment
    *                to connect the new geometry to the existing path
    * @since 1.6
    */
  def append(pi: PathIterator, connect: Boolean): Unit

  /**
    * Returns the fill style winding rule.
    *
    * @return an integer representing the current winding rule.
    * @see #WIND_EVEN_ODD
    * @see #WIND_NON_ZERO
    * @see #setWindingRule
    * @since 1.6
    */
  final def getWindingRule: Int = windingRule

  /**
    * Sets the winding rule for this path to the specified value.
    *
    * @param rule an integer representing the specified
    *             winding rule
    * @throws IllegalArgumentException if
    *            `rule` is not either
    *            `#` or
    *            `#`
    * @see #getWindingRule
    * @since 1.6
    */
  final def setWindingRule(rule: Int): Unit = {
    if (rule != Path2D.WIND_EVEN_ODD && rule != Path2D.WIND_NON_ZERO) throw new IllegalArgumentException("winding rule must be " + "WIND_EVEN_ODD or " + "WIND_NON_ZERO")
    windingRule = rule
  }

  /**
    * Returns the coordinates most recently added to the end of the path
    * as a `Point2D` object.
    *
    * @return a `Point2D` object containing the ending coordinates of
    *         the path or `null` if there are no points in the path.
    * @since 1.6
    */
  final def getCurrentPoint: Point2D = {
    var index = numCoords
    if (numTypes < 1 || index < 1) return null
    if (pointTypes(numTypes - 1) == Path2D.SEG_CLOSE) {
      // loop //todo: labels are not supported
      var i = numTypes - 2
      while (i > 0) {
        pointTypes(i) match {
          case Path2D.SEG_MOVETO =>
            i = 1 // break loop // todo: label break is not supported

          case Path2D.SEG_LINETO =>
            index -= 2

          case Path2D.SEG_QUADTO =>
            index -= 4

          case Path2D.SEG_CUBICTO =>
            index -= 6

          case Path2D.SEG_CLOSE =>
        }
        i -= 1
      }
    }
    getPoint(index - 2)
  }

  /**
    * Resets the path to empty.  The append position is set back to the
    * beginning of the path and all coordinates and point types are
    * forgotten.
    *
    * @since 1.6
    */
  final def reset(): Unit = {
    numTypes  = 0
    numCoords = 0
  }

  /**
    * Transforms the geometry of this path using the specified
    * `AffineTransform`.
    * The geometry is transformed in place, which permanently changes the
    * boundary defined by this object.
    *
    * @param at the `AffineTransform` used to transform the area
    * @since 1.6
    */
  def transform(at: AffineTransform): Unit

  /**
    * Returns a new `Shape` representing a transformed version
    * of this `Path2D`.
    * Note that the exact type and coordinate precision of the return
    * value is not specified for this method.
    * The method will return a Shape that contains no less precision
    * for the transformed geometry than this `Path2D` currently
    * maintains, but it may contain no more precision either.
    * If the tradeoff of precision vs. storage size in the result is
    * important then the convenience constructors in the
    * `Path2D.SFloat`
    * and
    * `Path2D.SDouble`
    * subclasses should be used to make the choice explicit.
    *
    * @param at the `AffineTransform` used to transform a
    *           new `Shape`.
    * @return a new `Shape`, transformed with the specified
    *         `AffineTransform`.
    * @since 1.6
    */
  final def createTransformedShape(at: AffineTransform): Shape = {
    val p2d = clone.asInstanceOf[Path2D]
    if (at != null) p2d.transform(at)
    p2d
  }

//  override final def getBounds: Rectangle = getBounds2D.getBounds

  override final def contains(x: SDouble, y: SDouble): Boolean = if (x * 0.0 + y * 0.0 == 0.0) {
    if (numTypes < 2) return false
    val mask = if (windingRule == Path2D.WIND_NON_ZERO) -1
    else 1
    (pointCrossings(x, y) & mask) != 0
  }
  else false

  override final def contains(p: Point2D): Boolean = contains(p.getX, p.getY)

  /**
    * ``
    * <p>
    * This method object may conservatively return false in
    * cases where the specified rectangular area intersects a
    * segment of the path, but that segment does not represent a
    * boundary between the interior and exterior of the path.
    * Such segments could lie entirely within the interior of the
    * path if they are part of a path with a `#`
    * winding rule or if the segments are retraced in the reverse
    * direction such that the two sets of segments cancel each
    * other out without any exterior area falling between them.
    * To determine whether segments represent true boundaries of
    * the interior of the path would require extensive calculations
    * involving all of the segments of the path and the winding
    * rule and are thus beyond the scope of this implementation.
    *
    * @since 1.6
    */
  override final def contains(x: SDouble, y: SDouble, w: SDouble, h: SDouble): Boolean = {
    if (java.lang.Double.isNaN(x + w) || java.lang.Double.isNaN(y + h)) return false
    if (w <= 0 || h <= 0) return false
    val mask = if (windingRule == Path2D.WIND_NON_ZERO) -1
    else 2
    val crossings = rectCrossings(x, y, x + w, y + h)
    crossings != Curve.RECT_INTERSECTS && (crossings & mask) != 0
  }

  override final def contains(r: Rectangle2D): Boolean = contains(r.getX, r.getY, r.getWidth, r.getHeight)

  /**
    * ``
    * <p>
    * This method object may conservatively return true in
    * cases where the specified rectangular area intersects a
    * segment of the path, but that segment does not represent a
    * boundary between the interior and exterior of the path.
    * Such a case may occur if some set of segments of the
    * path are retraced in the reverse direction such that the
    * two sets of segments cancel each other out without any
    * interior area between them.
    * To determine whether segments represent true boundaries of
    * the interior of the path would require extensive calculations
    * involving all of the segments of the path and the winding
    * rule and are thus beyond the scope of this implementation.
    *
    * @since 1.6
    */
  override final def intersects(x: SDouble, y: SDouble, w: SDouble, h: SDouble): Boolean = {
    if (java.lang.Double.isNaN(x + w) || java.lang.Double.isNaN(y + h)) return false
    if (w <= 0 || h <= 0) return false
    val mask = if (windingRule == Path2D.WIND_NON_ZERO) -1
    else 2
    val crossings = rectCrossings(x, y, x + w, y + h)
    crossings == Curve.RECT_INTERSECTS || (crossings & mask) != 0
  }

  override final def intersects(r: Rectangle2D): Boolean = intersects(r.getX, r.getY, r.getWidth, r.getHeight)

  /**
    * ``
    * <p>
    * The iterator for this class is not multi-threaded safe,
    * which means that this `Path2D` class does not
    * guarantee that modifications to the geometry of this
    * `Path2D` object do not affect any iterations of
    * that geometry that are already in process.
    *
    * @since 1.6
    */
  override final def getPathIterator(at: AffineTransform, flatness: SDouble) =
    new FlatteningPathIterator(getPathIterator(at), flatness)

//  /**
//    * Creates a new object of the same class as this object.
//    *
//    * @return a clone of this instance.
//    * @exception OutOfMemoryError            if there is not enough memory.
//    * @see java.lang.Cloneable
//    * @since 1.6
//    */
//  override def clone: Any

  /**
    * Trims the capacity of this Path2D instance to its current
    * size. An application can use this operation to minimize the
    * storage of a path.
    *
    * @since 10
    */
  def trimToSize(): Unit

  @throws[java.io.IOException]
  final private[geom] def writeObject(s: ObjectOutputStream, isdbl: Boolean): Unit = {
    s.defaultWriteObject()
    var fCoords: Array[SFloat  ] = null
    var dCoords: Array[SDouble ] = null
    if (isdbl) {
      dCoords = this.asInstanceOf[Path2D.Double].doubleCoords
      fCoords = null
    }
    else {
      fCoords = this.asInstanceOf[Path2D.Float].floatCoords
      dCoords = null
    }
    val numTypes = this.numTypes
    s.writeByte(if (isdbl) Path2D.SERIAL_STORAGE_DBL_ARRAY
    else Path2D.SERIAL_STORAGE_FLT_ARRAY)
    s.writeInt(numTypes)
    s.writeInt(numCoords)
    s.writeByte(windingRule.toByte)
    var cindex = 0
    for (i <- 0 until numTypes) {
      var npoints = 0
      var serialtype = 0
      pointTypes(i) match {
        case Path2D.SEG_MOVETO =>
          npoints = 1
          serialtype = if (isdbl) Path2D.SERIAL_SEG_DBL_MOVETO
          else Path2D.SERIAL_SEG_FLT_MOVETO

        case Path2D.SEG_LINETO =>
          npoints = 1
          serialtype = if (isdbl) Path2D.SERIAL_SEG_DBL_LINETO
          else Path2D.SERIAL_SEG_FLT_LINETO

        case Path2D.SEG_QUADTO =>
          npoints = 2
          serialtype = if (isdbl) Path2D.SERIAL_SEG_DBL_QUADTO
          else Path2D.SERIAL_SEG_FLT_QUADTO

        case Path2D.SEG_CUBICTO =>
          npoints = 3
          serialtype = if (isdbl) Path2D.SERIAL_SEG_DBL_CUBICTO
          else Path2D.SERIAL_SEG_FLT_CUBICTO

        case Path2D.SEG_CLOSE =>
          npoints = 0
          serialtype = Path2D.SERIAL_SEG_CLOSE

        case _ =>
          // Should never happen
          throw new InternalError("unrecognized path type")
      }
      s.writeByte(serialtype)
      while ( {
        {
          npoints -= 1; npoints
        } >= 0
      }) if (isdbl) {
        s.writeDouble(dCoords({
          cindex += 1; cindex - 1
        }))
        s.writeDouble(dCoords({
          cindex += 1; cindex - 1
        }))
      }
      else {
        s.writeFloat(fCoords({
          cindex += 1; cindex - 1
        }))
        s.writeFloat(fCoords({
          cindex += 1; cindex - 1
        }))
      }
    }
    s.writeByte(Path2D.SERIAL_PATH_END)
  }

  @throws[java.lang.ClassNotFoundException]
  @throws[java.io.IOException]
  final private[geom] def readObject(s: ObjectInputStream, storedbl: Boolean): Unit = {
    s.defaultReadObject()
    // The subclass calls this method with the storage type that
    // they want us to use (storedbl) so we ignore the storage
    // method hint from the stream.
    s.readByte
    val nT = s.readInt
    var nC = s.readInt
    try setWindingRule(s.readByte)
    catch {
      case iae: IllegalArgumentException =>
        throw new InvalidObjectException(iae.getMessage)
    }
    // Accept the size from the stream only if it is less than INIT_SIZE
    // otherwise the size will be based on the real data in the stream
    pointTypes = new Array[Byte](if (nT < 0 || nT > Path2D.INIT_SIZE) Path2D.INIT_SIZE
    else nT)
    val initX2 = Path2D.INIT_SIZE * 2
    if (nC < 0 || nC > initX2) nC = initX2
    if (storedbl) this.asInstanceOf[Path2D.Double].doubleCoords = new Array[SDouble](nC)
    else this.asInstanceOf[Path2D.Float].floatCoords = new Array[SFloat](nC)

    def checkPathEnd(): Unit =
      if (nT >= 0 && s.readByte != Path2D.SERIAL_PATH_END) throw new StreamCorruptedException("missing PATH_END")

    var i = 0
    while ( {
      nT < 0 || i < nT
    }) {
      var isdbl = false
      var npoints = 0
      var segtype = 0.toByte
      val serialtype = s.readByte
      serialtype match {
        case Path2D.SERIAL_SEG_FLT_MOVETO =>
          isdbl = false
          npoints = 1
          segtype = Path2D.SEG_MOVETO

        case Path2D.SERIAL_SEG_FLT_LINETO =>
          isdbl = false
          npoints = 1
          segtype = Path2D.SEG_LINETO

        case Path2D.SERIAL_SEG_FLT_QUADTO =>
          isdbl = false
          npoints = 2
          segtype = Path2D.SEG_QUADTO

        case Path2D.SERIAL_SEG_FLT_CUBICTO =>
          isdbl = false
          npoints = 3
          segtype = Path2D.SEG_CUBICTO

        case Path2D.SERIAL_SEG_DBL_MOVETO =>
          isdbl = true
          npoints = 1
          segtype = Path2D.SEG_MOVETO

        case Path2D.SERIAL_SEG_DBL_LINETO =>
          isdbl = true
          npoints = 1
          segtype = Path2D.SEG_LINETO

        case Path2D.SERIAL_SEG_DBL_QUADTO =>
          isdbl = true
          npoints = 2
          segtype = Path2D.SEG_QUADTO

        case Path2D.SERIAL_SEG_DBL_CUBICTO =>
          isdbl = true
          npoints = 3
          segtype = Path2D.SEG_CUBICTO

        case Path2D.SERIAL_SEG_CLOSE =>
          isdbl = false
          npoints = 0
          segtype = Path2D.SEG_CLOSE

        case Path2D.SERIAL_PATH_END =>
          if (nT < 0) {
            checkPathEnd()
            return
          }
          throw new StreamCorruptedException("unexpected PATH_END")

        case _ =>
          throw new StreamCorruptedException("unrecognized path type")
      }
      needRoom(segtype != Path2D.SEG_MOVETO, npoints * 2)
      if (isdbl) while ( {
        {
          npoints -= 1; npoints
        } >= 0
      }) append(s.readDouble(), s.readDouble())
      else while ( {
        {
          npoints -= 1; npoints
        } >= 0
      }) append(s.readFloat(), s.readFloat())
      pointTypes({
        numTypes += 1; numTypes - 1
      }) = segtype

      i += 1
    }

    checkPathEnd()
  }
}
