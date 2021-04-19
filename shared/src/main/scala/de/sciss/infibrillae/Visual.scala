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

import de.sciss.infibrillae.geom.{Path2D, Rectangle2D, Shape}
import de.sciss.lucre.synth.Executor.executionContext
import de.sciss.lucre.synth.Server
import de.sciss.numbers.Implicits._
import de.sciss.synth.UGenSource.Vec
import de.sciss.synth.message

import scala.concurrent.Future
import scala.math.{Pi, min}
import scala.util.control.NonFatal

object Visual extends VisualPlatform {
  private val trunkNameSq: Seq[(String, String)] = Seq(
    ("trunk11crop.jpg", "fibre4298crop1.jpg"),
    ("trunk13.jpg"    , "fibre4144crop1.jpg"),
    ("trunk15.jpg"    , "fibre4162crop1.jpg"),
  )

  private val speedSq = Seq(
    0.005,
    0.002,
    0.006,
  )

  private val poemSq: Seq[Vec[String]] = Seq(
    // penultimate is bridge to previous, ultimate is bridge to next
     Vector(
      "he", "strange", "creature", /*"light-skinned",*/ "tropics", "most", "others", "call",
      "frog", "indeed", "amphibians", "tribal", "translucid", "i", "curious", "teamed",
      "we", "worked", "there", "always", "together", "silent", "symbiosis", "no one",
      "knew", "unisonous", "alone", "parallel", "took", /*"long",*/ "understand", "condition",
      "when", "nervous", "nystagmus", "secretive", "sad", "almond", "eyes", "direction",
      "observant", "dancing",
      "light-skinned", "long",
    ),
    Vector(
      "dissolution", "transparency", "seem", "prerequisites", "gathering", "cells", "bend", "move",
      "connect", "into", "each other", "protuberances", "extremities", "touch", "slowly", "producing",
      "heat", "vapor", "sometimes", "synapsis", "encounters", /*"long",*/ "short", "fata morgana",
      "togethernesses", /*"invisible",*/ "they", "transformation", "happens", "un-earthed", "becoming",
      "long", "invisible",
    ),
    Vector(
      "mourning", "storm", "vibration", "malignity", "dark", "horrid", "shadows", "galloping",
      "other", "phantom", "limb", "urticaria", "body", "structure", "twinge", "vacuum",
      "disappearing", "being", "part", "transmuted", /*"skin",*/ "transformed", /*"invisible",*/
      "keloid", "how much", "work", "life", "conjunction", "absence", "something", "desired",
      "changes", "substance",
      "invisible", "skin",
    ),
  )

  private val polySq: Seq[Vec[(Float, Float)]] = Seq(
    Vector(
      (590.7143f,1116.7142f), (549.2857f,1080.9999f), (557.8571f,1040.2856f), (549.2857f,1001.71423f),
      (592.8572f,918.1428f), (630.71436f,837.4286f), (680.71436f,793.1429f), (707.14294f,713.1429f),
      (724.2858f,649.5715f), (757.14294f,628.8572f), (757.85724f,597.4285f), (834.28577f,511.0f),
      (893.5715f,474.57144f), (912.14294f,438.14288f), (1021.42865f,386.7143f), (1100.0001f,398.85715f),
      (1153.5715f,478.14288f), (1161.5918f,519.5715f), (1181.4287f,556.00006f), (1205.7144f,563.8572f),
      (1219.2858f,605.28577f), (1219.2858f,666.00006f), (1246.4287f,738.8572f), (1232.1431f,776.0f),
      (1260.7145f,804.5715f), (1297.8573f,890.28577f), (1285.7145f,911.00006f), (1290.0001f,943.8572f),
      (1300.0001f,1011.71436f), (1345.0001f,1163.143f), (1354.2858f,1227.4287f), (1380.0001f,1326.0001f),
      (1366.4287f,1406.7145f), (1404.2858f,1503.1431f), (1417.8572f,1586.0001f), (1425.0001f,1668.8572f),
      (1395.7144f,1744.5715f), (1370.0001f,1794.5715f), (1305.0001f,1821.7145f), (1190.0001f,1755.2859f),
      (1052.1431f,1686.7145f), (1016.4288f,1615.2859f), (845.7144f,1398.8573f), (786.4287f,1346.7145f),
      (680.0001f,1261.0001f), (680.0001f,1235.2859f), (609.2858f,1167.4288f),
    ),
    Vector(),
    Vector(
      (60.476192f,433.65207f), (68.03571f,393.58658f), (62.36607f,354.65506f), (65.0119f,311.56577f),
      (57.452377f,291.911f), (58.586304f,267.72052f), (75.973206f,234.08064f), (77.48511f,183.80981f),
      (116.03868f,144.87827f), (122.46427f,120.309814f), (157.61606f,88.18183f), (177.27081f,71.17291f),
      (256.26782f,56.053864f), (353.02972f,66.25922f), (408.59222f,93.47351f), (433.91663f,113.50625f),
      (466.80054f,124.84553f), (474.36005f,153.19376f), (488.72318f,167.93483f), (542.0178f,257.1372f),
      (557.5149f,285.8634f), (542.3958f,327.81876f), (528.78864f,387.539f), (514.04755f,413.24136f),
      (507.244f,447.6372f), (494.01483f,476.74136f), (473.9821f,490.3485f), (452.81543f,523.9884f),
      (437.3184f,531.9259f), (417.66364f,585.9765f), (365.50293f,640.4051f), (337.15472f,671.7771f),
      (300.49103f,685.3843f), (262.6934f,686.8962f), (208.26482f,638.8932f), (142.49695f,589.00037f),
      (100.541595f,530.79205f), (84.666595f,478.63132f)
    )
  )

  def apply(server: Server, canvas: Canvas[Ctx], idx: Int): Future[Visual[Ctx]] = {
    val (nameTrunk, nameFibre) = trunkNameSq(idx)
    val poem  = poemSq(idx)
    val speed = speedSq(idx)
    val poly  = polySq(idx)
    for {
      img1 <- loadImage(nameTrunk)
      img2 <- loadImage(nameFibre)
    } yield {
//      val t = osc.Browser.Transmitter(osc.Browser.Address(57110))
//      t.connect()
//      val canvas  = dom.document.getElementById("canvas").asInstanceOf[html.Canvas]
//      val ctx     = canvas.getContext("2d").asInstanceOf[CanvasRenderingContext2D]
      new Visual(img1, img2, server, canvas, speed = speed, poem = poem, poly = poly)
    }
  }


  private var _recording = false

  def recording: Boolean = _recording

  def recording_=(value: Boolean): Unit = {
    _recording = value
    if (value) {
      recFramesB.clear()
//      recTimeStart = System.currentTimeMillis()
    }
  }

  final class RecFrame(val time: Long, val trunkX: Double, val trunkY: Double)

//  private var recTimeStart  = 0L
  private val recFramesB    = Vector.newBuilder[RecFrame]

  def recFrames: Vector[RecFrame] = {
    val res = recFramesB.result()
    recFramesB.clear()
    res
  }

  def rec(trunkX: Double, trunkY: Double): Unit =
    if (recording) {
      recFramesB += new RecFrame(time = System.currentTimeMillis(), trunkX = trunkX, trunkY = trunkY)
    }
}
class Visual[Ctx <: Graphics2D] private(img1: Image[Ctx], img2: Image[Ctx], server: Server, canvas: Canvas[Ctx],
                                        speed: Double, poem: Vec[String], poly: Vec[(Float, Float)]) {
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
  private val polyColor: Color = Color.RGB4(0xF00)
  private var textColor: Color = Color.RGB4(0xCCC)
  private var palabra   = "in|fibrillae"
  private var palabraX  = 100.0
  private var palabraY  = 100.0

  private val polyShape: Shape = {
    val res = new Path2D.Double
    val ((x0, y0) +: tail) = poly
    res.moveTo(x0, y0)
    tail.foreach { case (x, y) =>
      res.lineTo(x, y)
    }
    res.closePath()
    res
  }

  def setComposite(name: String): Unit = {
    composite = Composite.parse(name)
    repaint()
  }

  def setText(s: String, x: Double, y: Double): Unit = {
    palabra   = s
    palabraX  = x
    palabraY  = y
//    repaint()
  }

  def setTextColor(name: String): Unit = {
    textColor = Color.parse(name)
//    repaint()
  }

  def setTrunkPos(x: Double, y: Double): Unit = {
    trunkX = x
    trunkY = y
  }

  def setTrunkTargetPos(x: Double, y: Double): Unit = {
    trunkTgtX = x
    trunkTgtY = y
    Visual.rec(trunkX, trunkY)
  }

  def setAnimTime(t: Double): Unit = {
    lastAnimTime = t
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
    paint(ctx, animTime)
    sendTrunkXY()
    repaint()
  }

  def paint(ctx: Ctx, animTime: Double): Unit = {
    val dt = min(100.0, animTime - lastAnimTime)
    lastAnimTime = animTime

    val wT  = dt * speed // 0.01
    val wS  = 1.0 - wT
    trunkX  = (trunkX * wS + trunkTgtX * wT).clip(trunkMinX, trunkMaxX)
    trunkY  = (trunkY * wS + trunkTgtY * wT).clip(trunkMinY, trunkMaxY)

    //  ctx.fillStyle = "green"
    //  ctx.fillRect(10, 10, 150, 100)
    ctx.composite = Composite.SourceOver
    val tx = trunkX - trunkMinX
    val ty = trunkY - trunkMinY
//    ctx.drawImage(img1, -tx, -ty)
    img1.draw(ctx, -tx, -ty)

    ctx.fillStyle = polyColor
    ctx.translate(-tx, -ty)
    ctx.fillShape(polyShape)
    ctx.translate(tx, ty)

    ctx.fillStyle = textColor // "#CCC"

//    ctx.fillText(palabra, palabraX, palabraY)
    ctx.fillText("disappearing" , 120.0, 122.0)
    ctx.fillText("vacuum"       , 110.0,  50.0)
    ctx.fillText("something"    , 70.0, 350.0)
    ctx.fillText("desired"      , 230.0, 280.0)

    ctx.composite = composite //  "color-burn"
//    ctx.drawImage(img2, 0.0, 0.0)
    img2.draw(ctx, 0.0, 0.0)
  }

  private var dragActive  = false

  var mouseEnabled = true

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
      if (!dragActive && mouseEnabled) {
//        val b = canvas.getBoundingClientRect
        val mx = e.x // e.clientX - b.left
        val my = e.y // e.clientY - b.top
        val dx = mx - canvasWH
        val dy = my - canvasHH
        val txT = (trunkX + dx).clip(trunkMinX, trunkMaxX)
        val tyT = (trunkY + dy).clip(trunkMinY, trunkMaxY)
        setTrunkTargetPos(txT, tyT)
      }
    }
  })

  canvas.requestAnimationFrame((ctx, _) => ctx.font = Font("Voltaire", 36))

//  ctx.font = "36px VoltaireRegular"

  repaint()
}
