package de.sciss.infibrillae

trait Graphics2D {
  var composite: Composite

  var font: Font

  var fillStyle: Color

  def fillText(s: String, x: Double, y: Double): Unit
}
