package de.sciss.infibrillae

trait Image[Ctx <: Graphics2D] {
  def width : Int
  def height: Int

  def draw(ctx: Ctx, x: Double, y: Double): Unit
}
