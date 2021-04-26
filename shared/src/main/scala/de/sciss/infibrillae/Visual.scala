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

import de.sciss.infibrillae.Visual.{Sensor, Word}
import de.sciss.infibrillae.geom.{Area, Ellipse2D, Path2D, Rectangle2D, Shape}
import de.sciss.lucre.synth.Executor.executionContext
import de.sciss.numbers.Implicits._
import de.sciss.osc
import de.sciss.synth.UGenSource.Vec

import scala.concurrent.Future
import scala.math.{Pi, atan2, min}
import scala.util.Random
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

  // defined by a circle region
  case class Sensor(cx: Int, cy: Int, r: Int)

  private val sensorsSq: Seq[Vec[Sensor]] = Seq(
    Vector(
      Sensor(765,869,200), Sensor(1162,963,200), Sensor(1268,1663,200), Sensor(945,1335,200),
      Sensor(346,593,324), Sensor(1592,745,324), Sensor(1598,2030,324), Sensor(609,1690,324),
    ),
  )

  def apply(client: osc.Transmitter.Directed, canvas: Canvas[Ctx], idx: Int): Future[Visual[Ctx]] = {
    val (nameTrunk, nameFibre) = trunkNameSq(idx)
    val words     = poemSq(idx)
    val poemBoxes = poemBoxesSq(idx)
    val speed     = speedSq(idx)
    val poly      = polySq(idx)
    val sensors   = sensorsSq(idx)
    for {
      imgTrunk <- loadImage(nameTrunk)
      imgFibre <- loadImage(nameFibre)
    } yield {
//      val t = osc.Browser.Transmitter(osc.Browser.Address(57110))
//      t.connect()
//      val canvas  = dom.document.getElementById("canvas").asInstanceOf[html.Canvas]
//      val ctx     = canvas.getContext("2d").asInstanceOf[CanvasRenderingContext2D]
      val poem = words.iterator.zip(poemBoxes).zipWithIndex.map { case ((s, pb), pi) =>
        val bridge = if (pi == words.size - 2) -1 else if (pi == words.size - 1) +1 else 0
        new Word(s, new Rectangle2D.Double(pb.x, pb.y, pb.width, pb.height), bridge = bridge)
      } .toArray
      new Visual(
        imgTrunk  = imgTrunk,
        imgFibre  = imgFibre,
        client    = client,
        canvas    = canvas,
        speed     = speed,
        poem      = poem,
        poly      = poly.toArray,
        minWords  = 6,
        maxWords  = 10,
        sensors   = sensors.toArray,
        verbose = false,
      )
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

  val textColor   : Color         = Color.RGB4(0xCCC)
  val textColorT  : Color.ARGB8   = Color.ARGB8(0x00CCCCCC)
  val textColorB  : Color         = Color.RGB4(0xEAA)
  val textColorBT : Color.ARGB8   = Color.ARGB8(0x00EEAAAA)

  /**
    * @param s        the word string
    * @param r        the word bounds at origin; must not be modified
    * @param bridge   -1 back, +1 forth, 0 none
    */
  final class Word(val s: String, val r: Rectangle2D, val bridge: Int) {
    override def toString: String = f"Word($s, $r, $bridge), x = $x%1.1f, y = $y%1.1f, fadeState = $fadeState"

    var x : Double  = 0.0
    var y : Double  = 0.0
    var vx: Double  = 0.0
    var vy: Double  = 0.0

    private var fadeState : Int     = 0
    private var fadeStart : Double  = 0.0
    private var fadeStop  : Double  = 0.0

    private val textColor : Color         = if (bridge == 0) Visual.textColor   else Visual.textColorB
    private val textColorT: Color.ARGB8   = if (bridge == 0) Visual.textColorT  else Visual.textColorBT

    def color(timeStamp: Double): Color = fadeState match {
      case 0 => textColor
      case 1 => // in
        if (timeStamp > fadeStop) {
          fadeState = 0
          textColor
        } else {
          val a = timeStamp.linLin(fadeStart, fadeStop, 0, 255).toInt
          textColorT.replaceAlpha(a)
        }
      case 2 => // out
        if (timeStamp > fadeStop) {
          fadeState = 3
          textColorT
        } else {
          val a = timeStamp.linLin(fadeStart, fadeStop, 255, 0).toInt
          textColorT.replaceAlpha(a)
        }
      case 3 => textColorT
    }

    def shouldRemove: Boolean = fadeState == 3

    def fadeIn(timeStamp: Double, dur: Double = 2.0): Unit = {
      fadeState = 1
      fadeStart = timeStamp
      fadeStop  = timeStamp + dur * 1000
    }

    def fadeOut(timeStamp: Double, dur: Double = 2.0): Unit = {
      fadeState = 2
      fadeStart = timeStamp
      fadeStop  = timeStamp + dur * 1000
    }

    def halt(): Unit = {
      vx = 0.0
      vy = 0.0
    }
  }
}
class Visual[Ctx <: Graphics2D] private(
                                         imgTrunk : Image[Ctx],
                                         imgFibre : Image[Ctx],
                                         client   : osc.Transmitter.Directed,
                                         canvas   : Canvas[Ctx],
                                         speed    : Double,
                                         poem     : Array[Word],
                                         poly     : Array[(Float, Float)],
                                         minWords : Int,
                                         maxWords : Int,
                                         sensors  : Array[Sensor],
                                         verbose  : Boolean,
                                       ) {
  private val canvasW   = canvas.width
  private val canvasWH  = canvasW / 2
  private val canvasH   = canvas.height
  private val canvasHH  = canvasH / 2
  private val trunkMinX = canvasWH
  private val trunkMinY = canvasHH
  private val trunkMaxX = imgTrunk.width  - canvasWH
  private val trunkMaxY = imgTrunk.height - canvasHH
  private var trunkX    = 570.0.clip(trunkMinX, trunkMaxX) // 0.0
  private var trunkY    = 180.0.clip(trunkMinY, trunkMaxY) // 0.0
  private var trunkTgtX = trunkX
  private var trunkTgtY = trunkY
  private var composite: Composite = Composite.ColorBurn
//  private val polyColor1: Color = Color.ARGB8(0x20FF0000)
//  private val polyColor2: Color = Color.RGB4(0xFF0)
  private var mouseX    = -1
  private var mouseY    = -1

  private var numPlaced     = 0
  private var placeTime     = 0.0
  private var placeOp       = 0   // 0 nada, 1 insert, 2 remove, -1 bridge prepare, -2 bridge
  private var placeNextIdx  = 0

  private val sensorShapes  = sensors.map { s =>
    val res = new Ellipse2D.Double(s.cx - s.r, s.cy - s.r, s.r * 2, s.r * 2)
//    println(res)
    res
  }

  private val polyShape: Shape = {
    val res = new Path2D.Double
    var i = 0
    while (i < poly.length) {
      val (x, y) = poly(i)
      if (i == 0) res.moveTo(x, y)
      else        res.lineTo(x, y)
      i += 1
    }
    res.closePath()
    res
  }

  def setComposite(name: String): Unit = {
    composite = Composite.parse(name)
    repaint()
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
    val a   = atan2(yr, xr)
    val idx = a.linLin(-Pi, +Pi, 0.0, 4.0)
    if (sentIdx != idx) try {
      sentIdx = idx
//      if (!trns.isConnected) {
//        trns.connect()
//      }
//      trns ! message.ControlBusSet(10 -> idx)
      client /*server.peer*/ ! osc.Message("/pan", idx.toFloat) // message.ControlBusSet(10 -> idx)

    } catch {
      case NonFatal(ex) =>
        ex.printStackTrace()
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

  private val rTest1 = new Rectangle2D.Double
  private val rTest2 = new Rectangle2D.Double

  def paint(ctx: Ctx, animTime: Double): Unit = {
    val animDt = min(100.0, animTime - lastAnimTime)
    lastAnimTime = animTime

    val wT  = animDt * speed // 0.01
    val wS  = 1.0 - wT
    val trunkX1  = (trunkX * wS + trunkTgtX * wT).clip(trunkMinX, trunkMaxX)
    val trunkY1  = (trunkY * wS + trunkTgtY * wT).clip(trunkMinY, trunkMaxY)
    val dx  = trunkX1 - trunkX
    val dy  = trunkY1 - trunkY
    trunkX  = trunkX1
    trunkY  = trunkY1

    ctx.composite = Composite.SourceOver
    val tx = trunkX - trunkMinX
    val ty = trunkY - trunkMinY
    imgTrunk.draw(ctx, -tx, -ty)

//    ctx.translate(-tx, -ty)
//    ctx.fillStyle = polyColor1
//    sensorShapes.foreach { sh =>
//      ctx.fillShape(sh)
//    }
//    ctx.translate(tx, ty)

    var pi = 0
    while (pi < numPlaced) {
      val p   = poem(pi)
      val pb  = p.r // poemBoxes(placedIdx)
      val pT  = animDt * 0.005            // actually we should use power / logarithmic scale
      val fr  = 1.0 - (animDt * 0.001)
      val pS  = 1.0 - pT
      val vis = {
        val px0 = p.x - tx + pb.getX
        (px0 < canvasW) && (px0 + pb.getWidth > 0.0) && {
          val py0 = p.y - ty + pb.getY
          (py0 < canvasH) && (py0 + pb.getHeight > 0.0)
        }
      }
      if (vis) {
        val inside = {
          val px0 = p.x - tx + pb.getX
          (px0 >= 0.0) && (px0 + pb.getWidth <= canvasW) && {
            val py0 = p.y - ty + pb.getY
            (py0 >= 0.0) && (py0 + pb.getHeight <= canvasH)
          }
        }
        if (inside) {
          p.vx = (p.vx * pS + dx * pT) * fr
          p.vy = (p.vy * pS + dy * pT) * fr
        } else {
          p.vx *= pS
          p.vy *= pS
        }
        val sx   = p.x + p.vx
        val sy   = p.y + p.vy

        rTest1.setRect(sx + pb.getX, sy + pb.getY, pb.getWidth, pb.getHeight)
        var collides = false
        var pj = 0 // placedIdx + 1
        while (!collides && pj < numPlaced) {
          if (pj != pi) {
            val q   = poem(pj)
            val qb  = q.r
            if (rTest1.intersects(q.x + qb.getX, q.y + qb.getY, qb.getWidth, qb.getHeight)) {
              collides = true
            }
          }
          pj += 1
        }

        if (!collides && polyShape.contains(rTest1 /*pb.getX + sx, pb.getY + sy, pb.getWidth, pb.getHeight*/)) {
          p.x = sx
          p.y = sy
        } else {
          p.vx = 0.0
          p.vy = 0.0
        }
        ctx.fillStyle = p.color(animTime)
        if (p.shouldRemove) {
          numPlaced -= 1
          poem(pi) = poem(numPlaced)
          poem(numPlaced) = p
          pi -= 1  // "repeat" index
          if (verbose) println(s"remove. p = $p, numPlaced = $numPlaced")
          if (verbose) println(poem.take(numPlaced).map(_.s).mkString("after remove: ", ", ", ""))

        } else {
          ctx.fillText(p.s, p.x - tx, p.y - ty)
        }

      } else {
        p.vx *= pS
        p.vy *= pS
      }

      pi += 1
    }

    if (placeOp <= 0 && placeTime < animTime) {
      if (placeOp == 0) {
        placeOp = if (numPlaced < minWords) 1 else if (numPlaced == maxWords) 2 else Random.nextInt(2) + 1
        // note: should be larger than fade-time, otherwise we need additional logic
        placeTime = animTime + Random.nextDouble().linLin(0.0, 1.0, 6000.0, 24000.0)
        if (placeOp == 1) {
          placeNextIdx = Random.nextInt(poem.length - numPlaced) + numPlaced

        } else if (placeOp == 2) {
          val pi  = Random.nextInt(numPlaced)
          val fdt = Random.nextDouble().linLin(0.0, 1.0, 1.8, 4.8)
          poem(pi).fadeOut(animTime, dur = fdt)
          placeOp = 0
        }
      } else if (placeOp == -1) { // XXX TODO: and is visible!
        var pi = 0
        while (pi < numPlaced - 1) {
          val fdt = Random.nextDouble().linLin(0.0, 1.0, 1.8, 4.8)
          poem(pi).fadeOut(animTime, dur = fdt)
          pi += 1
        }
        placeTime = animTime + Random.nextDouble().linLin(0.0, 1.0, 12000.0, 24000.0)
        placeOp = -2
      }
    }

    if (placeOp == 1) {
      // keep trying with the same index, so we do not favour short words
      val pi  = placeNextIdx // Random.nextInt(poem.length - numPlaced)
      val p   = poem(pi)
      val pb  = p.r
      val rx  = Random.nextInt(canvasW - pb.getWidth .toInt) + tx
      val ry  = Random.nextInt(canvasH - pb.getHeight.toInt) + ty
      rTest1.setRect(rx /*+ pb.x*/, ry /*+ pb.y*/, pb.getWidth, pb.getHeight)
      var collides = false
      var i = 0
      while (!collides && i < numPlaced) {
        val q   = poem(i)
        val qb  = q.r
        if (rTest1.intersects(q.x + qb.getX, q.y + qb.getY, qb.getWidth, qb.getHeight)) {
          collides = true
        }
        i += 1
      }
      if (!collides && {
        val a = new Area(polyShape)
        rTest2.setRect(tx, ty, canvasW, canvasH)
        a.intersect(new Area(rTest2))
        a.contains(rTest1)
      }) {
        p.x   = rx - pb.getX
        p.y   = ry - pb.getY
        p.vx  = 0.0
        p.vy  = 0.0
        val fdt = Random.nextDouble().linLin(0.0, 1.0, 1.8, 4.8)
        p.fadeIn(animTime, dur = fdt)
        // now swap with end to avoid duplicate choices
        poem(pi)        = poem(numPlaced)
        poem(numPlaced) = p
        if (verbose) println(s"insert. p = $p, numPlaced = $numPlaced")
        numPlaced += 1
        if (verbose) println(poem.take(numPlaced).map(_.s).mkString("after insert: ", ", ", ""))
        if (p.bridge == 0) {
          placeOp = 0
        } else {
          placeOp = -1
        }
      }
    }

    ctx.composite = composite //  "color-burn"
//    ctx.drawImage(img2, 0.0, 0.0)
    imgFibre.draw(ctx, 0.0, 0.0)
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
