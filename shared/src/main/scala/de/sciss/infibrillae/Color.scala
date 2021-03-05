package de.sciss.infibrillae

object Color {
  def parse(s: String): Color = ???

  final case class RGB8(value: Int) extends Color {
    private lazy val _cssString = {
      val h = ((value & 0xFFF) | 0x1000).toHexString
      s"#${h.substring(h.length - 3)}"
    }

    override def cssString: String = _cssString
  }
}
sealed trait Color {
  def cssString: String
}
