/*
 *  Visual.scala
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

import de.sciss.lucre.synth.Executor.executionContext
import de.sciss.numbers.Implicits._
import org.scalajs.dom
import org.scalajs.dom.{MouseEvent, html}
import org.scalajs.dom.raw.{CanvasRenderingContext2D, Event}

import scala.concurrent.{Future, Promise}

object Visual {
  def loadImage(name: String): Future[html.Image] = {
    val img = dom.document.createElement("img").asInstanceOf[html.Image]
    val path = s"assets/$name"
    val pr  = Promise[html.Image]()
    img.addEventListener("load", { _: Event =>
      pr.success(img)
    })
    img.addEventListener("error", { _: Event =>
      pr.failure(new Exception(s"Could not read image '$path'"))
    })
    img.src = path
    pr.future
  }

  def apply(): Future[Visual] = {
    for {
      img1 <- loadImage("trunk11crop.jpg")
      img2 <- loadImage("fibre4298crop1.jpg")
    } yield {
      new Visual(img1, img2)
    }
  }
}
class Visual private(img1: html.Image, img2: html.Image) {
  private val canvas    = dom.document.getElementById("canvas").asInstanceOf[html.Canvas]
  private val ctx       = canvas.getContext("2d").asInstanceOf[CanvasRenderingContext2D]
  private val trunkMinX = canvas.width  - img1.width
  private val trunkMinY = canvas.height - img1.height
  private var trunkX    = -570.0.clip(trunkMinX, 0.0) // 0.0
  private var trunkY    = -180.0.clip(trunkMinY, 0.0) // 0.0
  private var composite = "color-burn"
  private var textColor = "#CCC"

  def setComposite(name: String): Unit = {
    composite = name
    repaint()
  }

  def setTextColor(name: String): Unit = {
    textColor = name
    repaint()
  }

  println(s"Visual it. 7. img1.width ${img1.width}, canvas.width ${canvas.width} trunkMinX $trunkMinX, trunkMinY $trunkMinY")

  def repaint(): Unit = {
    //  ctx.fillStyle = "green"
    //  ctx.fillRect(10, 10, 150, 100)
    ctx.globalCompositeOperation = "source-over"
    val tx = if (dragActive) (trunkX + dragEndX - dragStartX).clip(trunkMinX, 0.0) else trunkX
    val ty = if (dragActive) (trunkY + dragEndY - dragStartY).clip(trunkMinY, 0.0) else trunkY
    ctx.drawImage(img1, tx, ty)
    ctx.fillStyle = textColor // "#CCC"
    ctx.fillText("in|fibrillae", 100.0, 100.0)
    ctx.globalCompositeOperation = composite //  "color-burn"
    ctx.drawImage(img2, 0.0, 0.0)
  }

  private var dragStartX  = 0.0
  private var dragStartY  = 0.0
  private var dragEndX    = 0.0
  private var dragEndY    = 0.0
  private var dragActive  = false

  canvas.addEventListener[MouseEvent]("mousedown", { e =>
    val b = canvas.getBoundingClientRect
    val mx = e.clientX - b.left
    val my = e.clientY - b.top
    println(s"down $mx, $my") // , ${e.pageX}, ${e.pageY}")
    dragStartX  = mx
    dragStartY  = my
    dragActive  = true
    e.preventDefault()
  })
  canvas.addEventListener[MouseEvent]("mousemove", { e =>
    // println(s"move") //  ${e.clientX}, ${e.clientY}, ${e.pageX}, ${e.pageY}")
    if (dragActive) {
      val b = canvas.getBoundingClientRect
      val mx = e.clientX - b.left
      val my = e.clientY - b.top
      e.preventDefault()
      dragEndX = mx
      dragEndY = my
      repaint()
    }
  })
  canvas.ownerDocument.addEventListener[MouseEvent]("mouseup", { e =>
    // println("up")
    if (dragActive) {
      trunkX = (trunkX + dragEndX - dragStartX).clip(trunkMinX, 0.0)
      trunkY = (trunkY + dragEndY - dragStartY).clip(trunkMinY, 0.0)
      println(s"trunkX $trunkX, trunkY $trunkY")
      dragActive = false
      e.preventDefault()
    }
  })

  ctx.font = "36px VoltaireRegular"

  repaint()
}
