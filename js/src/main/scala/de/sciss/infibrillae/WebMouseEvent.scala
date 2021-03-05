/*
 *  WebMouseEvent.scala
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

class WebMouseEvent(peer: dom.raw.MouseEvent, canvas: WebCanvas) extends MouseEvent {
  private lazy val b = canvas.peer.getBoundingClientRect()

  override def x: Int = (peer.clientX - b.left).toInt
  override def y: Int = (peer.clientY - b.top ).toInt

  override def preventDefault(): Unit =
    peer.preventDefault()
}
