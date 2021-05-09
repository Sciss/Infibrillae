/*
 *  AWTKeyEvent.scala
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

class AWTKeyEvent(_peer: java.awt.event.KeyEvent) extends KeyEvent {

  override def isAltDown    : Boolean = _peer.isAltDown
  override def isShiftDown  : Boolean = _peer.isShiftDown
  override def isControlDown: Boolean = _peer.isControlDown

  override def key: String =
    java.awt.event.KeyEvent.getKeyText(_peer.getKeyCode)  // XXX TODO --- implement directly not to have localization

  override def preventDefault(): Unit = ()
}
