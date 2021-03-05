package de.sciss.infibrillae

import org.scalajs.dom.html

class WebImage(peer: html.Image) extends Image[WebGraphics2D] {
  override def width  : Int = peer.width
  override def height : Int = peer.height

  override def draw(ctx: WebGraphics2D, x: Double, y: Double): Unit =
    ctx.peer.drawImage(peer, x, y)
}
