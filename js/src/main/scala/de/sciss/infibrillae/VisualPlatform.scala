package de.sciss.infibrillae

import org.scalajs.dom
import org.scalajs.dom.html
import org.scalajs.dom.raw.Event

import scala.concurrent.{Future, Promise}

trait VisualPlatform {
  type Ctx = WebGraphics2D

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
