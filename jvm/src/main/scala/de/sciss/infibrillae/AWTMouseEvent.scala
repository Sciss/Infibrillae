/*
 *  AWTMouseEvent.scala
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

class AWTMouseEvent(_peer: java.awt.event.MouseEvent) extends MouseEvent {
  override def x: Int = _peer.getX
  override def y: Int = _peer.getY

  override def preventDefault(): Unit = ()
}
