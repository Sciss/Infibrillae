package de.sciss.infibrillae

object Composite {
  def parse(s: String): Composite = ???

  final case object SourceOver extends Composite {
    final val name = "source-over"
  }

  final case object ColorBurn extends Composite {
    final val name = "color-burn"
  }
}
sealed trait Composite {
  def name: String
}
