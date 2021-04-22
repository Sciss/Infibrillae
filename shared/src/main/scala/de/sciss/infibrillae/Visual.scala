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

import de.sciss.infibrillae.geom.{Area, Path2D, Rectangle2D, Shape}
import de.sciss.lucre.synth.Executor.executionContext
import de.sciss.lucre.synth.Server
import de.sciss.numbers.Implicits._
import de.sciss.synth.UGenSource.Vec
import de.sciss.synth.message

import scala.concurrent.Future
import scala.math.{Pi, min}
import scala.util.control.NonFatal

object Visual extends VisualPlatform {
  val font: Font = Font("Voltaire", 36)

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

  val poemSq: Seq[Vec[String]] = Seq(
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

  val poemBoxesSq: Seq[Vec[IRect2D]] = Seq(
    Vector(
      IRect2D(2,-28,30,29), IRect2D(1,-25,98,32), IRect2D(1,-25,106,26), IRect2D(1,-27,89,34),
      IRect2D(2,-25,65,26), IRect2D(1,-28,83,29), IRect2D(1,-28,41,29), IRect2D(1,-29,51,36),
      IRect2D(2,-28,87,29), IRect2D(1,-28,156,35), IRect2D(1,-28,68,29), IRect2D(1,-28,129,29),
      IRect2D(2,-27,5,27), IRect2D(1,-27,95,28), IRect2D(1,-28,97,29), IRect2D(0,-19,33,21),
      IRect2D(0,-28,93,30), IRect2D(1,-28,68,29), IRect2D(1,-28,83,35), IRect2D(1,-28,114,35),
      IRect2D(1,-28,72,29), IRect2D(1,-28,131,35), IRect2D(2,-19,88,20), IRect2D(2,-28,66,30),
      IRect2D(2,-27,136,28), IRect2D(1,-28,71,29), IRect2D(2,-28,93,35), IRect2D(1,-28,60,29),
      IRect2D(2,-28,148,29), IRect2D(1,-28,125,29), IRect2D(0,-28,67,30), IRect2D(2,-19,103,21),
      IRect2D(2,-25,143,32), IRect2D(1,-27,115,29), IRect2D(1,-28,42,29), IRect2D(1,-28,99,29),
      IRect2D(1,-19,56,26), IRect2D(1,-28,116,29), IRect2D(1,-28,130,30), IRect2D(1,-28,103,35),
      IRect2D(2,-28,178,35), IRect2D(2,-28,55,35)
    ),
    Vector(
      IRect2D(1,-28,145,29), IRect2D(1,-25,171,32), IRect2D(1,-19,66,20), IRect2D(2,-27,170,34),
      IRect2D(1,-28,127,35), IRect2D(1,-28,55,29), IRect2D(2,-28,63,29), IRect2D(2,-19,71,21),
      IRect2D(1,-25,103,26), IRect2D(2,-27,52,28), IRect2D(1,-28,138,29), IRect2D(2,-28,187,35),
      IRect2D(1,-27,148,28), IRect2D(1,-28,72,29), IRect2D(1,-28,79,35), IRect2D(2,-28,131,35),
      IRect2D(2,-28,56,29), IRect2D(0,-20,75,27), IRect2D(1,-27,145,28), IRect2D(1,-27,111,34),
      IRect2D(1,-25,146,26), IRect2D(1,-28,68,29), IRect2D(1,-29,174,36), IRect2D(1,-28,200,35),
      IRect2D(1,-28,58,35), IRect2D(1,-29,199,30), IRect2D(2,-28,110,35), IRect2D(2,-28,146,29),
      IRect2D(2,-28,128,35), IRect2D(2,-28,55,35), IRect2D(2,-28,110,30)
    ),
    Vector(
      IRect2D(2,-27,129,34), IRect2D(1,-25,75,26), IRect2D(0,-28,119,30), IRect2D(2,-28,127,35),
      IRect2D(1,-28,58,29), IRect2D(2,-28,79,29), IRect2D(1,-28,110,30), IRect2D(1,-28,122,35),
      IRect2D(1,-28,71,29), IRect2D(2,-28,118,35), IRect2D(2,-28,57,29), IRect2D(2,-27,107,28),
      IRect2D(2,-28,64,35), IRect2D(1,-25,117,26), IRect2D(1,-27,87,34), IRect2D(0,-19,101,21),
      IRect2D(1,-28,169,35), IRect2D(2,-28,71,35), IRect2D(2,-25,51,32), IRect2D(1,-28,152,29),
      IRect2D(1,-29,162,30), IRect2D(2,-28,78,29), IRect2D(2,-28,130,30), IRect2D(0,-28,63,30),
      IRect2D(2,-29,39,30), IRect2D(1,-27,157,34), IRect2D(1,-28,104,29), IRect2D(1,-28,141,35),
      IRect2D(1,-28,94,29), IRect2D(1,-28,107,35), IRect2D(1,-28,130,29), IRect2D(2,-28,110,30),
      IRect2D(1,-28,52,29)
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
    val poem      = poemSq(idx)
    val poemBoxes = poemBoxesSq(idx)
    val speed     = speedSq(idx)
    val poly      = polySq(idx)
    for {
      img1 <- loadImage(nameTrunk)
      img2 <- loadImage(nameFibre)
    } yield {
//      val t = osc.Browser.Transmitter(osc.Browser.Address(57110))
//      t.connect()
//      val canvas  = dom.document.getElementById("canvas").asInstanceOf[html.Canvas]
//      val ctx     = canvas.getContext("2d").asInstanceOf[CanvasRenderingContext2D]
      new Visual(img1, img2, server, canvas, speed = speed, poem = poem, poemBoxes = poemBoxes, poly = poly)
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
                                        speed: Double, poem: Vec[String], poemBoxes: Vec[IRect2D],
                                        poly: Vec[(Float, Float)]) {
  private val canvasW   = canvas.width
  private val canvasWH  = canvasW / 2
  private val canvasH   = canvas.height
  private val canvasHH  = canvasH / 2
  private val trunkMinX = canvasWH
  private val trunkMinY = canvasHH
  private val trunkMaxX = img1.width  - canvasWH
  private val trunkMaxY = img1.height - canvasHH
  private var trunkX    = 570.0.clip(trunkMinX, trunkMaxX) // 0.0
  private var trunkY    = 180.0.clip(trunkMinY, trunkMaxY) // 0.0
  private var trunkTgtX = trunkX
  private var trunkTgtY = trunkY
  private var composite: Composite = Composite.ColorBurn
  private val polyColor1: Color = Color.RGB4(0xF00)
  private val polyColor2: Color = Color.RGB4(0xFF0)
  private var textColor: Color  = Color.RGB4(0xCCC)
  private var palabra   = "in|fibrillae"
  private var palabraX  = 100.0
  private var palabraY  = 100.0
  private var mouseX    = -1
  private var mouseY    = -1

  private var placed = Option.empty[(String, Double, Double)]

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
    canvas.repaint(repaint) // dom.window.requestAnimationFrame(repaint)

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

//    val inside    = polyShape.contains(mouseX + tx, mouseY + ty)

//    val inside    = polyShape.contains(mouseX + tx - 20, mouseY + ty - 20, 40.0, 40.0)
//    ctx.fillStyle = if (inside) polyColor2 else polyColor1
//    ctx.translate(-tx, -ty)
//    ctx.fillShape(polyShape)
//    ctx.translate(tx, ty)

//    ctx.fillText(palabra, palabraX, palabraY)

    //    for (pi <- 0 until 4) {
//      val sx = 120.0
//      val sy = 50 + pi * 50
//      ctx.fillStyle = polyColor2
//      val ri = poemBoxes(pi)
//      val r = new Rectangle2D.Double(ri.x + sx, ri.y + sy, ri.width, ri.height)
//      ctx.fillShape(r)
//      ctx.fillStyle = textColor
//      ctx.fillText(poem(pi), sx, sy)
//    }

    if (placed.isEmpty) {
      val a = new Area(polyShape)
      a.intersect(new Area(new Rectangle2D.Double(tx, ty, canvasW, canvasH)))
      val pi = util.Random.nextInt(poem.size)
      val pb = poemBoxes(pi)
      val rx = util.Random.nextInt(canvasW - pb.width ) + tx
      val ry = util.Random.nextInt(canvasH - pb.height) + ty
      if (a.contains(new Rectangle2D.Double(rx, ry, pb.width, pb.height))) {
        placed = Some(poem(pi), rx - pb.x, ry - pb.y)
      }
    }

    placed.foreach { case (s, sx, sy) =>
      ctx.fillStyle = textColor
      ctx.fillText(s, sx - tx, sy - ty)
    }

//    ctx.fillText("disappearing" , 120.0, 122.0)
//    ctx.fillText("vacuum"       , 110.0,  50.0)
//    ctx.fillText("something"    ,  70.0, 350.0)
//    ctx.fillText("desired"      , 230.0, 280.0)

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
        mouseX = mx
        mouseY = my
        val dx = mx - canvasWH
        val dy = my - canvasHH
        val txT = (trunkX + dx).clip(trunkMinX, trunkMaxX)
        val tyT = (trunkY + dy).clip(trunkMinY, trunkMaxY)
        setTrunkTargetPos(txT, tyT)
      }
    }
  })

  canvas.repaint((ctx, _) => ctx.font = Visual.font)

//  ctx.font = "36px VoltaireRegular"

  repaint()
}
