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
import de.sciss.osc
import de.sciss.synth.message
import org.scalajs.dom
import org.scalajs.dom.{MouseEvent, html}
import org.scalajs.dom.raw.{CanvasRenderingContext2D, Event}

import scala.concurrent.{Future, Promise}
import scala.math.Pi
import scala.util.control.NonFatal

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
      val t = osc.Browser.Transmitter(osc.Browser.Address(57110))
//      t.connect()
      new Visual(img1, img2, t)
    }
  }
}
class Visual private(img1: html.Image, img2: html.Image, trns: osc.Browser.Transmitter.Directed) {
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

  def setTrunkXY(x: Double, y: Double): Unit = {
    trunkX  = x
    trunkY  = y
    val xr  = x.linLin(trunkMinX, 0.0, -1, 1)
    val yr  = y.linLin(trunkMinY, 0.0, -1, 1)
    val a   = math.atan2(yr, xr)
    val idx = a.linLin(-Pi, +Pi, 0.0, 4.0)
    try {
      if (!trns.isConnected) {
        trns.connect()
      }
      trns ! message.ControlBusSet(10 -> idx)
    } catch {
      case NonFatal(_) =>
    }

    repaint()   // requestAnimationFrame?
  }

  println(s"Visual it. 8. img1.width ${img1.width}, canvas.width ${canvas.width} trunkMinX $trunkMinX, trunkMinY $trunkMinY")

  def repaint(): Unit = {
    //  ctx.fillStyle = "green"
    //  ctx.fillRect(10, 10, 150, 100)
    ctx.globalCompositeOperation = "source-over"
    val tx = /*if (dragActive) (trunkX + dragEndX - dragStartX).clip(trunkMinX, 0.0) else*/ trunkX
    val ty = /*if (dragActive) (trunkY + dragEndY - dragStartY).clip(trunkMinY, 0.0) else*/ trunkY
    ctx.drawImage(img1, tx, ty)
    ctx.fillStyle = textColor // "#CCC"
    ctx.fillText("in|fibrillae", 100.0, 100.0)
    ctx.globalCompositeOperation = composite //  "color-burn"
    ctx.drawImage(img2, 0.0, 0.0)
  }

  private var dragTrunkX  = 0.0
  private var dragTrunkY  = 0.0
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
    dragTrunkX  = trunkX
    dragTrunkY  = trunkY
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
      val tx   = (dragTrunkX + dragEndX - dragStartX).clip(trunkMinX, 0.0)
      val ty   = (dragTrunkY + dragEndY - dragStartY).clip(trunkMinY, 0.0)
      setTrunkXY(tx, ty)
    }
  })
  canvas.ownerDocument.addEventListener[MouseEvent]("mouseup", { e =>
    // println("up")
    if (dragActive) {
      dragActive = false
      e.preventDefault()
    }
  })

  ctx.font = "36px VoltaireRegular"

  repaint()
}
