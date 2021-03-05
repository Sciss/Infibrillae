package de.sciss.infibrillae

trait MouseListener {
  def mouseDown (e: MouseEvent): Unit
  def mouseUp   (e: MouseEvent): Unit
  def mouseMove (e: MouseEvent): Unit
}
