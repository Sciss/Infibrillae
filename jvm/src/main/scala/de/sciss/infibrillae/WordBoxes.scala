package de.sciss.infibrillae

import java.awt.image.BufferedImage

object WordBoxes {
  def main(args: Array[String]): Unit = {
    run()
  }

  def run(): Unit = {
    val img = new BufferedImage(6, 6, BufferedImage.TYPE_INT_ARGB)
    val g   = img.createGraphics()
    val f   = new java.awt.Font(Visual.font.family, java.awt.Font.PLAIN, Visual.font.sizePx)
    g.setFont(f)
    val frc = g.getFontRenderContext
    Visual.poemSq.zipWithIndex.foreach { case (poem, pi) =>
//      println(s"// poem ${pi + 1}")
      val rs = poem.map { word =>
        val gv = f.createGlyphVector(frc, word)
        val b  = gv.getVisualBounds.getBounds
        val r  = IRect2D(b.x, b.y, b.width, b.height)
        r
      }
      val s = rs.grouped(4).map(_.mkString(", ")).mkString("Vector(\n  ", ",\n  ", "\n),")
      println(s)
    }
    g.dispose()
  }
}
