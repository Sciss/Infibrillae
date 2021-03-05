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

import org.scalajs.dom.raw.CanvasRenderingContext2D

class WebGraphics2D(_peer: CanvasRenderingContext2D) extends Graphics2D {
  private var _composite: Composite = Composite.SourceOver

  override def composite: Composite = _composite
  override def composite_=(value: Composite): Unit = {
    _composite = value
    _peer.globalCompositeOperation = value.name
  }

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
}
