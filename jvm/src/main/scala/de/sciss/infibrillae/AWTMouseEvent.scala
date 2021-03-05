package de.sciss.infibrillae

class AWTMouseEvent(_peer: java.awt.event.MouseEvent) extends MouseEvent {
  override def x: Int = _peer.getX
  override def y: Int = _peer.getY

  override def preventDefault(): Unit = ()
}
