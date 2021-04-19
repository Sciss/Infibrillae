/*
 *  Graphics2D.scala
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

import de.sciss.infibrillae.geom.Shape

trait Graphics2D {
  var composite: Composite

  var font: Font

  var fillStyle: Color

  def fillText(s: String, x: Double, y: Double): Unit

  def fillShape(s: Shape): Unit

  def translate(tx: Double, ty: Double): Unit
}
