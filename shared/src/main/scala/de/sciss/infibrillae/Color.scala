/*
 *  Color.scala
 *  (in|fibrillae)
 *
 *  Copyright (c) 2020-2021 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU Affero General Public License v3+
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.infibrillae

object Color {
  def parse(s: String): Color = ???

  /** 4-bit value 0xRGB */
  final case class RGB4(value: Int) extends Color {
    private lazy val _cssString = {
      val h = ((value & 0xFFF) | 0x1000).toHexString
      s"#${h.substring(1)}"
    }

    override def cssString: String = _cssString
  }

  final case class ARGB8(value: Int) extends Color {
    private lazy val _cssString = {
      val rgba = (value << 8) | (value >>> 24)
      val h = (rgba & 0xFFFFFFFFL | 0x100000000L).toHexString
      s"#${h.substring(1)}"
    }

    def replaceAlpha(a: Int): ARGB8 = copy((value & 0x00FFFFFF) | (a << 24))

    override def cssString: String = _cssString
  }
}
sealed trait Color {
  def cssString: String
}
