val mmToPx = 96 / 25.4

// parse a "m .... z" svg path
def parseSvgPolygon(s: String): Seq[(Float, Float)] = {
  val s0 = s.trim.split(' ')
  require (s0.length > 2 && s0.head == "m" && s0.last == "z")
  val (xs, ys) = s0.tail.init.map { tup =>
    val Array(xs, ys) = tup.split(",")
    ((xs.toDouble * mmToPx).toFloat, (ys.toDouble * mmToPx).toFloat)
  } .unzip
  xs.toSeq.integrate zip ys.toSeq.integrate
}

val p11 = "m 156.29315,295.46399 -10.96131,-9.44941 2.26786,-10.77232 -2.26786,-10.20535 11.52828,-22.11161 10.01637,-21.35565 13.22916,-11.71726 6.99256,-21.16667 4.53572,-16.81994 8.69345,-5.48065 0.18899,-8.31549 20.22172,-22.86755 15.68601,-9.63839 4.9137,-9.63839 28.91518,-13.60715 20.78869,3.2128 14.1741,20.97768 2.12203,10.96131 5.24852,9.63839 6.42559,2.07887 3.59076,10.96131 0.0,16.06399 7.18156,19.27678 -3.77975,9.82738 7.55951,7.55953 9.82737,22.67857 -3.21278,5.48065 1.13392,8.69345 2.64583,17.95387 11.90625,40.06548 2.45684,17.00893 6.80358,26.08035 -3.59077,21.35566 10.01636,25.5134 3.59077,21.9226 1.88989,21.92261 -7.74851,20.03274 -6.80357,13.22917 -17.19792,7.18156 -30.42708,-17.5759 -36.47469,-18.14285 -9.44941,-18.89882 -45.16817,-57.2634 -15.68601,-13.79611 -28.15923,-22.67858 0.0,-6.80357 -18.70982,-17.95386 z"
val p15 = "m 60.476191,433.65208 7.559524,-40.06548 -5.669642,-38.93153 2.645833,-43.0893 -7.559524,-19.65476 1.133927,-24.19048 17.386906,-33.63988 1.511903,-50.27083 38.553572,-38.93155 6.42559,-24.56845 35.15179,-32.127982 19.65476,-17.008919 78.99702,-15.119051 96.76191,10.20535 55.5625,27.214292 25.32441,20.03274 32.88392,11.33928 7.55952,28.34822 14.36312,14.74107 53.29462,89.20238 15.49704,28.7262 -15.11906,41.95535 -13.60715,59.72024 -14.74107,25.70237 -6.80355,34.39583 -13.22917,29.10417 -20.03274,13.60715 -21.16667,33.63989 -15.49704,7.9375 -19.65476,54.05059 -52.1607,54.42858 -28.34822,31.37201 -36.66368,13.60715 -37.79763,1.51191 -54.42857,-48.00298 -65.76786,-49.89285 -41.95535,-58.20833 -15.875003,-52.16073 z"

val pts = parseSvgPolygon(p11)
val ptsS = pts.grouped(4).map { g => g.map { case (x, y) => s"(${x}f,${y}f)" }.mkString("  ", ", ", ",") }.mkString("Vector(\n", "\n", "\n)")
println(ptsS)
