val root = scala.xml.XML.loadFile("/data/projects/infibrillae/trunks/trunk11crop-sensors.svg")
val circles = (root \\ "circle")
require (circles.size == 8)

case class Circle(cx: Int, cy: Int, r: Int, hue: Int) {
  def toSensor = Sensor(cx = cx, cy = cy, r = r)
}

case class Sensor(cx: Int, cy: Int, r: Int)

val mmToPx = 96 / 25.4

val xs = circles.toVector.map { elem =>
  val style = (elem \ "@style").text
  val cx    = ((elem \ "@cx").text.toDouble * mmToPx + 0.5).toInt
  val cy    = ((elem \ "@cy").text.toDouble * mmToPx + 0.5).toInt
  val r     = ((elem \ "@r" ).text.toDouble * mmToPx + 0.5).toInt
  val i     = style.indexOf("stroke:#") + "stroke:#".length
  val j     = style.indexOf(";", i)
  val rgbS  = style.substring(i, j)
  val rgb   = java.lang.Integer.parseInt(rgbS, 16)
  val c     = new java.awt.Color(rgb)
  val Array(hue, _, _) = java.awt.Color.RGBtoHSB(c.getRed, c.getGreen, c.getBlue, null)
  Circle(cx, cy, r, (hue * 360).toInt)
}

val sensors = xs.sortBy(_.hue).map(_.toSensor)
sensors.grouped(4).map(_.mkString(", ")).toVector.mkString("Vector(\n  ", ",\n  ", ",\n)")
