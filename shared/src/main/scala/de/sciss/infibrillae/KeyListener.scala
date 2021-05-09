package de.sciss.infibrillae

trait KeyListener {
  def keyDown (e: KeyEvent): Unit
  def keyUp   (e: KeyEvent): Unit
//  def keyTyped(e: KeyEvent): Unit
}
