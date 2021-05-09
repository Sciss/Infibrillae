/*
 *  WebKeyEvent.scala
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

import org.scalajs.dom

class WebKeyEvent(peer: dom.raw.KeyboardEvent /*, canvas: WebCanvas*/) extends KeyEvent {

  override def isAltDown    : Boolean = peer.altKey
  override def isShiftDown  : Boolean = peer.shiftKey
  override def isControlDown: Boolean = peer.ctrlKey

  override def key: String = {
    val s = peer.key
    if (s.length != 1) s else s.charAt(0) match {
      case c if c >= 'a' && c <= 'z' => (c - 32).toChar.toString  // make upper case
      case _ => s
    }
  }

  override def preventDefault(): Unit =
    peer.preventDefault()
}
