/*
 *  AWTImage.scala
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

import java.awt
import java.awt.image.ImageObserver

class AWTImage(_peer: java.awt.image.BufferedImage) extends Image[AWTGraphics2D] with ImageObserver {
  override def width  : Int = _peer.getWidth
  override def height : Int = _peer.getHeight

  override def imageUpdate(img: awt.Image, infoFlags: Int, x: Int, y: Int, width: Int, height: Int): Boolean =
    true

  override def draw(ctx: AWTGraphics2D, x: Double, y: Double): Unit =
    ctx.peer.drawImage(_peer, x.toInt, y.toInt, this)
}
