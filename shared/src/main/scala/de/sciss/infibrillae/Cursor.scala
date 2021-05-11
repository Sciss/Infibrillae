/*
 *  Canvas.scala
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

object Cursor {
  case object Default   extends Cursor { val name = "default"   }
  case object CrossHair extends Cursor { val name = "crosshair" }
  case object Hidden    extends Cursor { val name = "none"      }
}
sealed trait Cursor {
  def name: String
}
