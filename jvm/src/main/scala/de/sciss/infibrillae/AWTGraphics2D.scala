/*
 *  AWTGraphics2D.scala
 *  (in|fibrillae)
 *
 *  Copyright (c) 2020-2021 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU Affero General Public License v3+
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.infibrillae

import com.jhlabs.composite.ColorBurnComposite
import de.sciss.infibrillae.geom.{AffineTransform, PathIterator, Shape}

import java.awt.Rectangle

class AWTGraphics2D(_peer: java.awt.Graphics2D) extends Graphics2D {
  private var _composite: Composite = Composite.SourceOver

  override def composite: Composite = _composite
  override def composite_=(value: Composite): Unit = {
    _composite = value
    val c = value match {
      case Composite.SourceOver => java.awt.AlphaComposite.SrcOver
      case Composite.ColorBurn  => new ColorBurnComposite(1f)
    }
    _peer.setComposite(c)
  }

  override def translate(tx: Double, ty: Double): Unit =
    _peer.translate(tx, ty)

  private var _font: Font = Font("SansSerif", 12)

  override def font: Font = _font
  override def font_=(value: Font): Unit = {
    _font = value
    val f = new java.awt.Font(value.family, java.awt.Font.PLAIN, value.sizePx)
    _peer.setFont(f)
  }

  private var _fillStyle: Color = Color.RGB4(0)

  override def fillStyle: Color = _fillStyle
  override def fillStyle_=(value: Color): Unit = {
    _fillStyle = value
    value match {
      case Color.RGB4(rgb4) =>
        val r4    = (rgb4 & 0xF00) >> 8
        val g4    = (rgb4 & 0x0F0) >> 4
        val b4    =  rgb4 & 0x00F
        val r8    = (r4 << 4) | r4
        val g8    = (g4 << 4) | g4
        val b8    = (b4 << 4) | b4
        val rgb8  = (r8 << 16) | (g8 << 8) | b8
        val c  = new java.awt.Color(rgb8)
        peer.setColor(c)
    }
  }

  def peer: java.awt.Graphics2D = _peer

  override def fillText(s: String, x: Double, y: Double): Unit =
    _peer.drawString(s, x.toFloat, y.toFloat)

  private final class WrapPathIterator(peer: PathIterator) extends java.awt.geom.PathIterator {
    override def getWindingRule: Int = peer.getWindingRule

    override def isDone: Boolean = peer.isDone

    override def next(): Unit = peer.next()

    override def currentSegment(coords: Array[Float ]): Int = peer.currentSegment(coords)
    override def currentSegment(coords: Array[Double]): Int = peer.currentSegment(coords)
  }

  private object WrapShape extends java.awt.Shape {
    var current: Shape = null
    private val atPeer    = new AffineTransform()
    private val atMatrix  = new Array[Double](6)

    override def getBounds: Rectangle = ???

    override def getBounds2D: java.awt.geom.Rectangle2D = ???

    override def contains(x: Double, y: Double): Boolean =
      current.contains(x, y)

    override def contains(p: java.awt.geom.Point2D): Boolean = ???

    override def intersects(x: Double, y: Double, w: Double, h: Double): Boolean =
      current.intersects(x, y, w, h)

    override def intersects(r: java.awt.geom.Rectangle2D): Boolean = ???

    override def contains(x: Double, y: Double, w: Double, h: Double): Boolean =
      current.contains(x, y, w, h)

    override def contains(r: java.awt.geom.Rectangle2D): Boolean = ???

    private def wrapTransform(at: java.awt.geom.AffineTransform): AffineTransform =
      if (at == null) null else {
        val m = atMatrix
        at.getMatrix(m)
        val res = atPeer
        res.setTransform(m(0),m(1),m(2),m(3),m(4),m(5))
        res
      }

    override def getPathIterator(at: java.awt.geom.AffineTransform): java.awt.geom.PathIterator = {
      val atP = wrapTransform(at)
      new WrapPathIterator(current.getPathIterator(atP))
    }

    override def getPathIterator(at: java.awt.geom.AffineTransform, flatness: Double): java.awt.geom.PathIterator = {
      val atP = wrapTransform(at)
      new WrapPathIterator(current.getPathIterator(atP, flatness))
    }
  }

  override def fillShape(s: Shape): Unit = {
    WrapShape.current = s
    _peer.fill(WrapShape)
  }
}
