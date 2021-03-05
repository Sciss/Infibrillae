package de.sciss.infibrillae

trait MouseEvent {
  def x: Int
  def y: Int

  def preventDefault(): Unit
}
