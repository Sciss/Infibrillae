package de.sciss.infibrillae

final case class Font(family: String, sizePx: Int) {
  def cssString: String = s"${sizePx}px $family"
}
