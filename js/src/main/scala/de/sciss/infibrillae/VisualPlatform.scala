/*
 *  VisualPlatform.scala
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
import org.scalajs.dom.html
import org.scalajs.dom.raw.Event

import scala.concurrent.{Future, Promise}

trait VisualPlatform {
  type Ctx = WebGraphics2D

  def rec(animTime: Double, trunkX: Double, trunkY: Double): Unit = ()

  def loadImage(name: String): Future[Image[Ctx]] = {
    val peer  = dom.document.createElement("img").asInstanceOf[html.Image]
    val path  = s"assets/$name"
    val pr    = Promise[Image[Ctx]]()
    peer.addEventListener("load", { _: Event =>
      val img = new WebImage(peer)
      pr.success(img)
    })
    peer.addEventListener("error", { _: Event =>
      pr.failure(new Exception(s"Could not read image '$path'"))
    })
    peer.src = path
    pr.future
  }
}
