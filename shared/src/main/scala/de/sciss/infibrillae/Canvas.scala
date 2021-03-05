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

trait Canvas[Ctx] {
  def width : Int
  def height: Int

  def requestAnimationFrame(fun: (Ctx, Double) => Unit): Unit

  def addMouseListener(ml: MouseListener): Unit
}
