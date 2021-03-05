/*
 *  WebImage.scala
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

import org.scalajs.dom.html

class WebImage(peer: html.Image) extends Image[WebGraphics2D] {
  override def width  : Int = peer.width
  override def height : Int = peer.height

  override def draw(ctx: WebGraphics2D, x: Double, y: Double): Unit =
    ctx.peer.drawImage(peer, x, y)
}
