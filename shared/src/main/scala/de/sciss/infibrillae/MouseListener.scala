/*
 *  MouseListener.scala
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

trait MouseListener {
  def mouseDown (e: MouseEvent): Unit
  def mouseUp   (e: MouseEvent): Unit
  def mouseMove (e: MouseEvent): Unit
}
