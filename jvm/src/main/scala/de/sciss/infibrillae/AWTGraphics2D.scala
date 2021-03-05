package de.sciss.infibrillae

import com.jhlabs.composite.ColorBurnComposite

class AWTGraphics2D(_peer: java.awt.Graphics2D) extends Graphics2D {
  private var _composite: Composite = Composite.SourceOver

  override def composite: Composite = _composite
  override def composite_=(value: Composite): Unit = {
    _composite = value
    val c = value match {
      case Composite.SourceOver => java.awt.AlphaComposite.SrcOver
      case Composite.ColorBurn  => new ColorBurnComposite(1f)
    }
    _peer.setComposite(c)
  }

  private var _font: Font = Font("SansSerif", 12)

  override def font: Font = _font
  override def font_=(value: Font): Unit = {
    _font = value
    val f = new java.awt.Font(value.family, java.awt.Font.PLAIN, value.sizePx)
    _peer.setFont(f)
  }

  private var _fillStyle: Color = Color.RGB4(0)

  override def fillStyle: Color = _fillStyle
  override def fillStyle_=(value: Color): Unit = {
    _fillStyle = value
    value match {
      case Color.RGB4(rgb4) =>
        val r4    = (rgb4 & 0xF00) >> 8
        val g4    = (rgb4 & 0x0F0) >> 4
        val b4    =  rgb4 & 0x00F
        val r8    = (r4 << 4) | r4
        val g8    = (g4 << 4) | g4
        val b8    = (b4 << 4) | b4
        val rgb8  = (r8 << 16) | (g8 << 8) | b8
        val c  = new java.awt.Color(rgb8)
        peer.setColor(c)
    }
  }

  def peer: java.awt.Graphics2D = _peer

  override def fillText(s: String, x: Double, y: Double): Unit =
    _peer.drawString(s, x.toFloat, y.toFloat)
}
