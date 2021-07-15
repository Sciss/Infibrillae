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

import de.sciss.infibrillae.Visual.{NumSensors, Sensor, Word}
import de.sciss.infibrillae.geom.{Area, Ellipse2D, Path2D, Rectangle2D, Shape}
import de.sciss.lucre.synth.Executor.executionContext
import de.sciss.numbers.Implicits._
import de.sciss.osc
import de.sciss.synth.UGenSource.Vec

import java.util
import scala.concurrent.Future
import scala.math.{Pi, atan2, max, min}
import scala.util.Random
import scala.util.control.NonFatal

object Visual extends VisualPlatform {
  val font: Font = Font("Voltaire", 36)

  final val NumSpaces = 3 // eventually 6

  private val trunkNameSq: Seq[(String, String)] = Seq(
    ("trunk11crop.jpg", "fibre4298crop1.jpg"),
    ("trunk13.jpg"    , "fibre4144crop1.jpg"),
    ("trunk15.jpg"    , "fibre4162crop1.jpg"),
  )

  private val speedSq: Seq[Double] = Seq(
    0.005,
    0.002,
    0.006,
  )

  private val sensorAtkSq: Seq[Float] = Seq(
    0.2f,
    0.3f,
    0.25f,
  )

  private val sensorRlsSq: Seq[Float] = Seq(
    0.07f,
    0.06f,
    0.05f,
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

  private val polySq: Seq[List[Vec[(Float, Float)]]] = Seq(
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
    ) :: Nil,
    Vector(
      (290.92392f,1657.3878f), (341.43155f,1583.6467f), (365.6752f,1481.6213f), (403.05084f,1444.2456f),
      (458.60925f,1253.3268f), (479.82242f,1204.8395f), (480.83258f,1136.149f), (505.07623f,1045.2354f),
      (525.2793f,901.7937f), (569.726f,814.9206f), (599.02045f,757.3419f), (601.0408f,704.81396f),
      (637.40625f,634.10333f), (764.6855f,526.017f), (857.6195f,569.4536f), (854.58905f,692.6922f),
      (825.2946f,736.1288f), (856.6094f,819.97144f), (839.43677f,871.4892f), (863.6804f,949.271f),
      (911.1576f,1035.134f), (926.3099f,1205.8497f), (1003.0815f,1332.1189f), (1004.0916f,1383.6367f),
      (1040.457f,1426.0631f), (1081.8734f,1408.8905f), (1191.98f,1530.1088f), (1258.65f,1531.1189f),
      (1290.975f,1577.5859f), (1284.914f,1641.2256f), (1223.2947f,1774.5657f), (1105.1068f,1846.2865f),
      (1061.6702f,1915.987f), (1065.7108f,1979.6267f), (991.9697f,2087.713f), (922.26917f,2249.3374f),
      (820.74884f,2251.3577f), (789.1815f,2324.0886f), (702.0559f,2321.058f), (695.995f,2175.5962f),
      (633.36554f,2059.4287f), (416.18274f,1822.0428f),
    ) :: Nil,
    Vector(
      (228.57143f,1639.0f), (257.14285f,1487.5714f), (235.71428f,1340.4286f), (245.71428f,1177.5714f),
      (217.14285f,1103.2856f), (221.42856f,1011.85706f), (287.14285f,884.71423f), (292.85712f,694.71423f),
      (438.5714f,547.57135f), (462.85712f,454.71423f), (595.7143f,333.28564f), (670.0f,268.99997f),
      (968.5714f,211.8571f), (1334.2856f,250.4285f), (1544.2856f,353.28568f), (1640.0f,428.99997f),
      (1764.2856f,471.8571f), (1792.857f,578.99994f), (1847.1428f,634.71423f), (2048.5713f,971.85706f),
      (2107.1428f,1080.4285f), (2050.0f,1238.9999f), (1998.5714f,1464.7141f), (1942.8572f,1561.8569f),
      (1917.143f,1691.8569f), (1867.143f,1801.8569f), (1791.4287f,1853.2855f), (1711.4287f,1980.4285f),
      (1652.8572f,2010.4285f), (1578.5715f,2214.714f), (1381.4287f,2420.4285f), (1274.2859f,2538.9998f),
      (1135.7145f,2590.4285f), (992.8573f,2596.1428f), (787.143f,2414.7144f), (538.57153f,2226.143f),
      (380.00012f,2006.1431f), (320.00012f,1809.0001f)
    ) ::
    Vector(
      (624.2857f,1441.857f), (587.1428f,1216.1428f), (619.99994f,1154.7142f), (640.71423f,1006.14276f),
      (688.57135f,1011.1428f), (717.85706f,815.42847f), (675.71423f,767.5713f), (799.99994f,637.5713f),
      (877.1428f,598.9999f), (960.71423f,625.42847f), (1087.1428f,513.28564f), (1154.2856f,306.14282f),
      (1209.9999f,318.99994f), (1203.5713f,456.85712f), (1286.4285f,567.57135f), (1332.857f,598.2857f),
      (1457.1428f,877.5714f), (1515.7142f,881.8571f), (1535.0f,943.2857f), (1470.0f,990.4285f),
      (1478.5715f,1155.4285f), (1521.4286f,1293.2856f), (1525.7144f,1389.7142f), (1455.7142f,1417.5714f),
      (1301.4286f,1713.2856f), (1234.2856f,1843.2856f), (997.1428f,1931.857f), (898.5714f,1947.5713f),
      (879.2857f,1881.857f), (801.0714f,1753.2856f), (737.1428f,1737.5713f), (678.5714f,1684.7142f),
      (622.8571f,1536.1428f), (594.2857f,1465.4285f),
    ) :: Nil
  )

  // defined by a circle region
  case class Sensor(cx: Int, cy: Int, r: Int)

  final val NumSensors = 8

  private val sensorsSq: Seq[Vec[Sensor]] = Seq(
    Vector(
      Sensor(765,869,200), Sensor(1162,963,200), Sensor(1268,1663,200), Sensor(945,1335,200),
      Sensor(346,593,324), Sensor(1592,745,324), Sensor(1598,2030,324), Sensor(609,1690,324),
    ),
    Vector(
      Sensor(673,879,200), Sensor(887,1259,200), Sensor(1082,1845,200), Sensor(482,1662,200),
      Sensor(204,571,403), Sensor(1316,988,354), Sensor(1453,2182,351), Sensor(-29,1728,380),
    ),
    Vector(
      Sensor(508,636,200), Sensor(1822,896,200), Sensor(1599,1893,200), Sensor(516,1928,200),
      Sensor(173,308,403), Sensor(2216,656,354), Sensor(1988,2196,351), Sensor(153,2302,380),
    ),
  )

  def apply(client: osc.Transmitter.Directed, canvas: Canvas[Ctx]): Future[Visual[Ctx]] = {
    val futImages: Seq[Future[Image[Ctx]]] = trunkNameSq.flatMap { case (nameTrunk, nameFibre) =>
      loadImage(nameTrunk) :: loadImage(nameFibre) :: Nil
    }
    Future.sequence(futImages).map { imgSq =>
      val imgTupSq = imgSq.grouped(2).map { case Seq(imgTrunk, imgFibre) => (imgTrunk, imgFibre) } .toArray
      new Visual(
        imgTupSq  = imgTupSq,
        client    = client,
        canvas    = canvas,
        minWords  = 6,
        maxWords  = 10,
        verbose   = false,
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

    var visible = false
    var inside  = false

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

  private final val AUTOMOTIVE_TIMEOUT = 30000L // milliseconds
}
class Visual[Ctx <: Graphics2D] private(
                                         imgTupSq : Array[(Image[Ctx], Image[Ctx])],
                                         client   : osc.Transmitter.Directed,
                                         canvas   : Canvas[Ctx],
                                         minWords : Int,
                                         maxWords : Int,
                                         verbose  : Boolean,
                                       ) {
  private var spaceIdx  = Random.nextInt(Visual.NumSpaces) // 0

  private val canvasW   = canvas.width
  private val canvasWH  = canvasW / 2
  private val canvasH   = canvas.height
  private val canvasHH  = canvasH / 2
  private val trunkMinX = canvasWH
  private val trunkMinY = canvasHH

  private var imgTrunk    : Image[Ctx]    = _
  private var imgFibre    : Image[Ctx]    = _
  private var words       : Array[String] = _
  private var poemBoxes   : Array[IRect2D]= _
  private var speed       : Double        = _
  private var polyList    : List[Vec[(Float, Float)]] = _
  private var polyShape   : Shape         = _
  private var sensors     : Array[Sensor] = _
  private var poem        : Array[Word]   = _
  private var sensorShapes: Array[Shape]  = _
  private var trunkMaxX   : Int           = _
  private var trunkMaxY   : Int           = _
  private var trunkX    = 570.0
  private var trunkY    = 180.0
  private var trunkTgtX   : Double        = _
  private var trunkTgtY   : Double        = _
  private val sensorData  : Array[Float]  = new Array(Visual.NumSensors)
  private var sensorAtk   : Float         = _
  private var sensorRls   : Float         = _

  private def spaceIdxUpdated(): Unit = {
    val (_imgTrunk, _imgFibre) = imgTupSq(spaceIdx)
    imgTrunk    = _imgTrunk
    imgFibre    = _imgFibre
    words       = Visual.poemSq     (spaceIdx).toArray
    poemBoxes   = Visual.poemBoxesSq(spaceIdx).toArray
    speed       = Visual.speedSq    (spaceIdx)
    polyList    = Visual.polySq     (spaceIdx)
    sensors     = Visual.sensorsSq  (spaceIdx).toArray
    sensorAtk   = Visual.sensorAtkSq(spaceIdx)
    sensorRls   = Visual.sensorRlsSq(spaceIdx)
    poem        = mkPoem()

    sensorShapes  = sensors.map { s =>
      val res = new Ellipse2D.Double(s.cx - s.r, s.cy - s.r, s.r * 2, s.r * 2)
      //    println(res)
      res
    }

    util.Arrays.fill(sensorData, 0.0f)

    trunkMaxX = imgTrunk.width  - canvasWH
    trunkMaxY = imgTrunk.height - canvasHH

    trunkX    = trunkX.clip(trunkMinX, trunkMaxX)
    trunkY    = trunkY.clip(trunkMinY, trunkMaxY)
    trunkTgtX = trunkX
    trunkTgtY = trunkY

    polyShape = mkPolyShape()

    sendOSC(osc.Message("/trans", spaceIdx))
  }

  spaceIdxUpdated()

  def activateVolume(): Unit =
    sendOSC(osc.Message("/unmute"))

  private def mkPoem() =
    words.iterator.zip(poemBoxes).zipWithIndex.map { case ((s, pb), pi) =>
      val bridge = if (pi == words.length - 2) -1 else if (pi == words.length - 1) +1 else 0
      new Word(s, new Rectangle2D.Double(pb.x, pb.y, pb.width, pb.height), bridge = bridge)
    } .toArray

  private var composite: Composite = Composite.ColorBurn
  private var mouseX    = -1.0
  private var mouseY    = -1.0

  private var numPlaced     = 0
  private var placeTime     = 0.0
  private var placeOp       = 0   // 0 nada, 1 insert, 2 remove, -1 bridge prepare, -2 bridge
  private var placeNextIdx  = 0

  private var mouseTimeOut    = Long.MaxValue
  private var automotive      = false
  private var automotiveDx    = 0.0
  private var automotiveDy    = 0.0
  private var automotiveRest  = Long.MaxValue

  private def mkPoly(pts: Vec[(Float, Float)]): Shape = {
    val res = new Path2D.Double
    var first = true
    pts.foreach { case (x, y) =>
      if (first) {
        res.moveTo(x, y)
        first = false
      } else {
        res.lineTo(x, y)
      }
    }
    res.closePath()
    res
  }

  private def mkPolyShape(): Shape = {
    polyList match {
      case single :: Nil => mkPoly(single)
      case outer :: inner :: Nil =>
        val sOut  = mkPoly(outer)
        val sIn   = mkPoly(inner)
        val res   = new Area(sOut)
        res.subtract(new Area(sIn))
        res
    }
  }

  private final val DEBUG = false
//  private final val TEST  = true

  private val polyColor1: Color = Color.ARGB8(0x20FF0000)
  private val polyColor2: Color = Color.ARGB8(0x20800080) // Color.RGB4(0xFF0)
  private val mouseColor: Color = Color.ARGB8(0x80800080) // Color.RGB4(0xFF0)

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

  private var sentPanIdx = 0.0

  private def sendOSC(m: osc.Message): Unit =
    try {
      if (client != null) client ! m
    } catch {
      case NonFatal(ex) =>
        ex.printStackTrace()
    }

  private def sendTrunkXY(): Unit = {
    val xr  = trunkX.linLin(trunkMinX, trunkMaxX, -1, 1)
    val yr  = trunkY.linLin(trunkMinY, trunkMaxY, -1, 1)
    val a   = atan2(yr, xr)
    val idx = a.linLin(-Pi, +Pi, 0.0, 4.0)
    if (sentPanIdx != idx) try {
      sentPanIdx = idx
      sendOSC(osc.Message("/pan", idx.toFloat))

    } catch {
      case NonFatal(ex) =>
        ex.printStackTrace()
    }

    // repaint()   // requestAnimationFrame?
  }

  private def sendSensors(): Unit =
    sendOSC(osc.Message("/sense", sensorData: _*))

  //  println(s"Visual it. 8. img1.width ${img1.width}, canvas.width ${canvas.width}")

  def repaint(): Unit =
    canvas.repaint(repaint) // dom.window.requestAnimationFrame(repaint)

  private var lastAnimTime = 0.0

  // animTime is in milliseconds
  def repaint(ctx: Ctx, animTime: Double): Unit = {
    paint(ctx, animTime)
    sendTrunkXY()
    sendSensors()
    repaint()
  }

  private val rTest1 = new Rectangle2D.Double
  private val rTest2 = new Rectangle2D.Double

  def paint(ctx: Ctx, animTime: Double): Unit = {
    val animDt    = min(100.0, animTime - lastAnimTime)
    lastAnimTime  = animTime
    val wT        = animDt * speed // 0.01
    val wS        = 1.0 - wT
    // println(s"wT $wT")

    val now = System.currentTimeMillis()
    if (automotive) {
      if (now > automotiveRest) {
        // mouseActive()
        mouseTimeOut  = now + Random.between(20, 30) * 1000L
        automotive    = false
        canvas.cursor = Cursor.CrossHair

      } else {
        if (placeOp >= 0) {
          // brownian motion
          //      val biasX     = if (trunkX < imgTrunk.width /2) 0.5 else -0.5
          //      val biasY     = if (trunkY < imgTrunk.height/2) 0.5 else -0.5
          val biasX     = 0.5 - (trunkX / imgTrunk.width )
          val biasY     = 0.5 - (trunkY / imgTrunk.height)
          automotiveDx  = (automotiveDx + Random.between(-8.0 + biasX, 8.0 + biasX) * wT).clip(-150.0, +150.0)
          automotiveDy  = (automotiveDy + Random.between(-8.0 + biasY, 8.0 + biasY) * wT).clip(-150.0, +150.0)
          val dx        = automotiveDx // * wT
          val dy        = automotiveDy // * wT
          trunkTgtX     = (trunkX + dx).clip(trunkMinX, trunkMaxX)
          trunkTgtY     = (trunkY + dy).clip(trunkMinY, trunkMaxY)
          // mouse drift towards canvas center
          if (mouseX != canvasWH) mouseX = mouseX * wS + canvasWH * wT
          if (mouseY != canvasHH) mouseY = mouseY * wS + canvasHH * wT
        }
      }

    } else {
      if (now > mouseTimeOut) {
        automotive      = true
        automotiveRest  = now + Random.between(60, 120) * 1000L
        automotiveDx    = 0.0
        automotiveDy    = 0.0
        canvas.cursor   = Cursor.Hidden
      }
    }

    val trunkX1   = (trunkX * wS + trunkTgtX * wT).clip(trunkMinX, trunkMaxX)
    val trunkY1   = (trunkY * wS + trunkTgtY * wT).clip(trunkMinY, trunkMaxY)
    val dx  = trunkX1 - trunkX
    val dy  = trunkY1 - trunkY
    trunkX  = trunkX1
    trunkY  = trunkY1

    val tx = trunkX - trunkMinX
    val ty = trunkY - trunkMinY
    // if (!TEST) {
      ctx.composite = Composite.SourceOver
      imgTrunk.draw(ctx, -tx, -ty)
    // }

    // update sensors
    if (mouseX >= 0) {
      val mxAbs = mouseX + tx
      val myAbs = mouseY + ty
      for (si <- 0 until NumSensors) {
        val hit   = sensorShapes(si).contains(mxAbs, myAbs)
        val vOld  = sensorData(si)
        val vNew  = if (hit) min(1.0f, vOld + sensorAtk * wT) else max(0.0f, vOld - sensorRls * wT)
        sensorData(si) = vNew.toFloat
      }
    }

    if (DEBUG) {
      ctx.translate(-tx, -ty)
      ctx.fillStyle = polyColor1
      sensorShapes.foreach { sh =>
        ctx.fillShape(sh)
      }
      ctx.fillStyle = polyColor2
      ctx.fillShape(polyShape)

      ctx.translate(tx, ty)

      ctx.fillStyle = mouseColor
      if (mouseX >= 0 && mouseY >= 0) {
        rTest1.setRect(mouseX - 8, mouseY - 2, 16, 4)
        ctx.fillShape(rTest1)
        rTest1.setRect(mouseX - 2, mouseY - 8, 4, 16)
        ctx.fillShape(rTest1)
      }

      for (si <- sensorData.indices) {
        val v = sensorData(si)
        val h = v * 30
        rTest1.setRect(si * 30 + 10, 40 - h, 20, h)
        ctx.fillShape(rTest1)
      }
    }

    var pi = 0
    while (pi < numPlaced) {
      val p   = poem(pi)
      val pb  = p.r // poemBoxes(placedIdx)
      val pT  = animDt * 0.005            // actually we should use power / logarithmic scale
      val fr  = 1.0 - (animDt * 0.001)
      val pS  = 1.0 - pT
      p.visible = {
        val px0 = p.x - tx + pb.getX
        (px0 < canvasW) && (px0 + pb.getWidth > 0.0) && {
          val py0 = p.y - ty + pb.getY
          (py0 < canvasH) && (py0 + pb.getHeight > 0.0)
        }
      }
      if (p.visible) {
        p.inside = {
          val px0 = p.x - tx + pb.getX
          (px0 >= 0.0) && (px0 + pb.getWidth <= canvasW) && {
            val py0 = p.y - ty + pb.getY
            (py0 >= 0.0) && (py0 + pb.getHeight <= canvasH)
          }
        }
        if (p.inside) {
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
      }
      val col = p.color(animTime)
      if (p.shouldRemove) {
        numPlaced -= 1
        poem(pi) = poem(numPlaced)
        poem(numPlaced) = p
        pi -= 1  // "repeat" index
        if (verbose) println(s"remove. p = $p, numPlaced = $numPlaced")
        if (verbose) println(poem.take(numPlaced).map(_.s).mkString("after remove: ", ", ", ""))

      } else if (p.visible) {
        // if (!TEST) {
          ctx.fillStyle = col
          ctx.fillText(p.s, p.x - tx, p.y - ty)
        // }

      } else {
        p.vx *= pS
        p.vy *= pS
        p.inside = false
      }

      pi += 1
    }

    if (placeOp <= 0 && placeTime < animTime) {
      if (placeOp == 0) {
        placeOp = if (numPlaced < minWords) 1 else if (numPlaced == maxWords) 2 else Random.nextInt(2) + 1
        // note: should be larger than fade-time, otherwise we need additional logic
        placeTime = animTime + Random.between(6000.0, 24000.0)
        if (placeOp == 1) {
          placeNextIdx = Random.nextInt(poem.length - numPlaced) + numPlaced

        } else if (placeOp == 2) {
          val pi  = Random.nextInt(numPlaced)
          val fdt = Random.between(1.8, 4.8)
          poem(pi).fadeOut(animTime, dur = fdt)
          placeOp = 0
        }
      } else if (placeOp == -1) {
        val lastPoemIdx = numPlaced - 1
        if (poem(lastPoemIdx).inside) {
          var pi = 0
          while (pi < lastPoemIdx) {
            val fdt = Random.between(1.8, 4.8)
            poem(pi).fadeOut(animTime, dur = fdt)
            pi += 1
          }
          placeTime = animTime + Random.between(12000.0, 16000.0)
          placeOp = -2
          sendOSC(osc.Message("/dwell", 0))
        }
      } else if (placeOp == -2) {
        val p = poem(numPlaced - 1)
        if (verbose) println(s"numPlaced $numPlaced - word ${p.s} - bridge ${p.bridge}")
        spaceIdx  = (spaceIdx + p.bridge).wrap(0, Visual.NumSpaces - 1)
        spaceIdxUpdated()

        // now we find the counter word
        val counterBridge = -p.bridge
        var pi = 0
        while (pi < poem.length) {
          val pC = poem(pi)
          if (pC.bridge == counterBridge) {
            pC.x   = p.x + (p.r.getWidth  - pC.r.getWidth )/2
            pC.y   = p.y + (p.r.getHeight - pC.r.getHeight)/2
            pC.vx  = 0.0
            pC.vy  = 0.0
            val fdt = Random.between(1.8 + 5.0, 4.8 + 5.0)
            placeTime = animTime + Random.between(10000.0, 20000.0)
            pC.fadeOut(animTime, dur = fdt)
            // now swap with end to avoid duplicate choices
            poem(pi)  = poem(0)
            poem(0)   = pC
            if (verbose) println(s"insert counter bridge. pC = $pC")
            numPlaced = 1

            // now let's try to move the canvas so that the new region is in the interior
            val pb    = pC.r
            val pxOff = pC.x - tx
            val pyOff = pC.y - ty
            val polyB = polyShape.getBounds2D
            val minTx = max(0, polyB.getX - pxOff)
            val minTy = max(0, polyB.getY - pyOff)
            val maxTx = min(imgTrunk.width  - canvasW, minTx + polyB.getWidth - pb.getWidth )
            val maxTy = min(imgTrunk.height - canvasH, minTy + polyB.getHeight - pb.getHeight)
            // println(f"pC.x ${pC.x}%1.1f, pC.y ${pC.y}%1.1f, tx $tx%1.1f, ty $ty%1.1f, polyX $polyX%1.1f, polyY $polyY%1.1f, polyW $polyW%1.1f, polyH $polyH%1.1f, minTX $minTX%1.1f, maxTX $maxTX%1.1f, minTY $minTY%1.1f, maxTY $maxTY%1.1f")
            var attempts = 20
            while (attempts > 0) {
              val txNew = Random.between(minTx, maxTx)
              val tyNew = Random.between(minTy, maxTy)
              rTest1.setRect(pxOff + txNew, pyOff + tyNew, pb.getWidth, pb.getHeight)
              if (polyShape.contains(rTest1)) {
                val dtx   = (txNew + canvasWH) - trunkX
                val dty   = (tyNew + canvasHH) - trunkY
                if (verbose) println(f"Found good spot at $txNew%1.1f, $tyNew%1.1f after ${20 - attempts} attempts. dtx $dtx%1.1f, dty $dty%1.1f")
                trunkX   += dtx
                trunkY   += dty
                trunkTgtX = trunkX
                trunkTgtY = trunkY
                pC.x     += dtx
                pC.y     += dty
                attempts  = 0  // "break"
              } else {
                attempts -= 1
              }
            }

            pi = poem.length  // "break"
          } else {
            pi += 1
          }
        }
        placeTime = animTime + Random.between(6000.0, 24000.0)
        placeOp   = 0
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
        val fdt = Random.between(1.8, 4.8)
        p.fadeIn(animTime, dur = fdt)
        // now swap with end to avoid duplicate choices
        poem(pi)        = poem(numPlaced)
        poem(numPlaced) = p
        if (verbose) println(s"insert. p = $p, numPlaced = $numPlaced")
        numPlaced += 1
        if (verbose) println(poem.take(numPlaced).map(_.s).mkString("after insert: ", ", ", ""))
        // "ralentir"
//        automotiveDx = 0.0
//        automotiveDy = 0.0
        automotiveDx *= 0.5
        automotiveDy *= 0.5
        if (p.bridge == 0) {
          placeOp = 0
        } else {
          placeOp = -1
        }
      }
    }

    // if (!TEST) {
      ctx.composite = composite //  "color-burn"
      imgFibre.draw(ctx, 0.0, 0.0)
    // }
  }

  private var dragActive  = false

  var mouseEnabled = true

  canvas.addKeyListener(new KeyListener {
    override def keyDown(e: KeyEvent): Unit = {
      // println(s"key = ${e.key}")
      if (e.key == "Enter" && placeOp == 0 && (numPlaced < maxWords)) {
        var pi = numPlaced
        while (pi < poem.length) {
          if (poem(pi).bridge == 1) {
            placeOp = 1
            placeNextIdx = pi
            pi = poem.length  // "break"
          } else {
            pi += 1
          }
        }
      }
    }

    override def keyUp(e: KeyEvent): Unit = ()
  })

  private def mouseActive(): Unit = {
    mouseTimeOut = System.currentTimeMillis() + Visual.AUTOMOTIVE_TIMEOUT
    if (automotive) {
      automotive    = false
      canvas.cursor = Cursor.CrossHair
    }
  }

  canvas.addMouseListener(new MouseListener {
    override def mouseDown(e: MouseEvent): Unit = {
      dragActive = true
      e.preventDefault()
      mouseActive()
      canvas.requestFocus()
    }

    override def mouseUp(e: MouseEvent): Unit = {
      if (dragActive) {
        dragActive = false
        e.preventDefault()
        mouseActive()
      }
    }

    override def mouseMove(e: MouseEvent): Unit = {
      e.preventDefault()
      mouseActive()
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

  canvas.cursor = Cursor.CrossHair
  canvas.requestFocus()
  mouseActive() // set time-out
  canvas.repaint((ctx, _) => ctx.font = Visual.font)

//  ctx.font = "36px VoltaireRegular"

  repaint()
}
