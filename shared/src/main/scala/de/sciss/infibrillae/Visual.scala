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
import de.sciss.lucre.synth.Server
import de.sciss.numbers.Implicits._
import de.sciss.synth.message

import scala.concurrent.Future
import scala.math.{Pi, min}
import scala.util.control.NonFatal

object Visual extends VisualPlatform {
  def apply(server: Server, canvas: Canvas[Ctx]): Future[Visual[Ctx]] = {
    for {
      img1 <- loadImage("trunk11crop.jpg")
      img2 <- loadImage("fibre4298crop1.jpg")
    } yield {
//      val t = osc.Browser.Transmitter(osc.Browser.Address(57110))
//      t.connect()
//      val canvas  = dom.document.getElementById("canvas").asInstanceOf[html.Canvas]
//      val ctx     = canvas.getContext("2d").asInstanceOf[CanvasRenderingContext2D]
      new Visual(img1, img2, server, canvas)
    }
  }
}
class Visual[Ctx <: Graphics2D] private(img1: Image[Ctx], img2: Image[Ctx], server: Server,
                                        canvas: Canvas[Ctx]) {
  private val canvasWH  = canvas.width  /2
  private val canvasHH  = canvas.height /2
  private val trunkMinX = canvasWH
  private val trunkMinY = canvasHH
  private val trunkMaxX = img1.width  - canvasWH
  private val trunkMaxY = img1.height - canvasHH
  private var trunkX    = 570.0.clip(trunkMinX, trunkMaxX) // 0.0
  private var trunkY    = 180.0.clip(trunkMinY, trunkMaxY) // 0.0
  private var trunkTgtX = trunkX
  private var trunkTgtY = trunkY
  private var composite: Composite = Composite.ColorBurn
  private var textColor: Color = Color.RGB4(0xCCC)
  private var palabra   = "in|fibrillae"
  private var palabraX  = 100.0
  private var palabraY  = 100.0

  def setComposite(name: String): Unit = {
    composite = Composite.parse(name)
    repaint()
  }

  def setText(s: String, x: Double, y: Double): Unit = {
    palabra   = s
    palabraX  = x
    palabraY  = y
    repaint()
  }

  def setTextColor(name: String): Unit = {
    textColor = Color.parse(name)
    repaint()
  }

  def setTrunkXY(x: Double, y: Double): Unit = {
    trunkTgtX = x
    trunkTgtY = y
  }

  private var sentIdx = 0.0

  private def sendTrunkXY(): Unit = {
    val xr  = trunkX.linLin(trunkMinX, trunkMaxX, -1, 1)
    val yr  = trunkY.linLin(trunkMinY, trunkMaxY, -1, 1)
    val a   = math.atan2(yr, xr)
    val idx = a.linLin(-Pi, +Pi, 0.0, 4.0)
    if (sentIdx != idx) try {
      sentIdx = idx
//      if (!trns.isConnected) {
//        trns.connect()
//      }
//      trns ! message.ControlBusSet(10 -> idx)
      server.peer ! message.ControlBusSet(10 -> idx)
    } catch {
      case NonFatal(_) =>
    }

    // repaint()   // requestAnimationFrame?
  }

//  println(s"Visual it. 8. img1.width ${img1.width}, canvas.width ${canvas.width}")

  def repaint(): Unit =
    canvas.requestAnimationFrame(repaint) // dom.window.requestAnimationFrame(repaint)

  private var lastAnimTime = 0.0

  // animTime is in milliseconds
  def repaint(ctx: Ctx, animTime: Double): Unit = {
    val dt = min(100.0, animTime - lastAnimTime)
    lastAnimTime = animTime

    val wT  = dt * 0.005 // 0.01
    val wS  = 1.0 - wT
    trunkX  = (trunkX * wS + trunkTgtX * wT).clip(trunkMinX, trunkMaxX)
    trunkY  = (trunkY * wS + trunkTgtY * wT).clip(trunkMinY, trunkMaxY)
    sendTrunkXY()

    //  ctx.fillStyle = "green"
    //  ctx.fillRect(10, 10, 150, 100)
    ctx.composite = Composite.SourceOver
    val tx = trunkX - trunkMinX
    val ty = trunkY - trunkMinY
//    ctx.drawImage(img1, -tx, -ty)
    img1.draw(ctx, -tx, -ty)
    ctx.fillStyle = textColor // "#CCC"
    ctx.fillText(palabra, palabraX, palabraY)
    ctx.composite = composite //  "color-burn"
//    ctx.drawImage(img2, 0.0, 0.0)
    img2.draw(ctx, 0.0, 0.0)

    repaint()
  }

  private var dragActive  = false

  canvas.addMouseListener(new MouseListener {
    override def mouseDown(e: MouseEvent): Unit = {
      dragActive = true
      e.preventDefault()
    }

    override def mouseUp(e: MouseEvent): Unit = {
      if (dragActive) {
        dragActive = false
        e.preventDefault()
      }
    }

    override def mouseMove(e: MouseEvent): Unit = {
      e.preventDefault()
      if (!dragActive) {
//        val b = canvas.getBoundingClientRect
        val mx = e.x // e.clientX - b.left
        val my = e.y // e.clientY - b.top
        val dx = mx - canvasWH
        val dy = my - canvasHH
        val txT = (trunkX + dx).clip(trunkMinX, trunkMaxX)
        val tyT = (trunkY + dy).clip(trunkMinY, trunkMaxY)
        setTrunkXY(txT, tyT)
      }
    }
  })

  canvas.requestAnimationFrame((ctx, _) => ctx.font = Font("VoltaireRegular", 36))

//  ctx.font = "36px VoltaireRegular"

  repaint()
}
