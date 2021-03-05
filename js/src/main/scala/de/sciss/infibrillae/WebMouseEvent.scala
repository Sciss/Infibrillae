package de.sciss.infibrillae

import org.scalajs.dom

class WebMouseEvent(peer: dom.raw.MouseEvent, canvas: WebCanvas) extends MouseEvent {
  private lazy val b = canvas.peer.getBoundingClientRect()

  override def x: Int = (peer.clientX - b.left).toInt
  override def y: Int = (peer.clientY - b.top ).toInt

  override def preventDefault(): Unit =
    peer.preventDefault()
}
