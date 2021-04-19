/*
 *  WebGraphics2D.scala
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

import de.sciss.infibrillae.geom.{PathIterator, Shape}
import org.scalajs.dom.raw.CanvasRenderingContext2D

import scala.annotation.switch

class WebGraphics2D(_peer: CanvasRenderingContext2D) extends Graphics2D {
  private var _composite: Composite = Composite.SourceOver

  override def composite: Composite = _composite
  override def composite_=(value: Composite): Unit = {
    _composite = value
    _peer.globalCompositeOperation = value.name
  }

  override def translate(tx: Double, ty: Double): Unit =
    _peer.translate(tx, ty)

  private var _font: Font = Font("SansSerif", 12)

  override def font: Font = _font
  override def font_=(value: Font): Unit = {
    _font = value
    _peer.font = value.cssString
  }

  private var _fillStyle: Color = Color.RGB4(0)

  override def fillStyle: Color = _fillStyle
  override def fillStyle_=(value: Color): Unit = {
    _fillStyle = value
    peer.fillStyle = value.cssString
  }

  def peer: CanvasRenderingContext2D = _peer

  override def fillText(s: String, x: Double, y: Double): Unit =
    _peer.fillText(s, x, y)

  private val pathCoords = new Array[Double](6)

  override def fillShape(s: Shape): Unit = {
    val it = s.getPathIterator(null)
    if (it.isDone) return
    val c = pathCoords
    _peer.beginPath()
    while (!it.isDone) {
      it.next()
      (it.currentSegment(c): @switch) match {
        case PathIterator.SEG_MOVETO =>
          _peer.moveTo(c(0), c(1))
        case PathIterator.SEG_LINETO =>
          _peer.lineTo(c(0), c(1))
        case PathIterator.SEG_QUADTO =>
          _peer.quadraticCurveTo(c(2), c(3), c(0), c(1))
        case PathIterator.SEG_CUBICTO =>
          _peer.bezierCurveTo(c(2), c(3), c(4), c(5), c(0), c(1))
        case PathIterator.SEG_CLOSE =>
          _peer.closePath()
      }
    }
    _peer.fill()
  }
}
