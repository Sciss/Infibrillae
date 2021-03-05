package de.sciss.infibrillae

trait Canvas[Ctx] {
  def width : Int
  def height: Int

  def requestAnimationFrame(fun: (Ctx, Double) => Unit): Unit

  def addMouseListener(ml: MouseListener): Unit
}
