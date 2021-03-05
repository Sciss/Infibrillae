/*
 *  Image.scala
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

trait Image[Ctx <: Graphics2D] {
  def width : Int
  def height: Int

  def draw(ctx: Ctx, x: Double, y: Double): Unit
}
