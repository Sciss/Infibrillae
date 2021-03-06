/*
 *  LDRSensor.scala
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

import com.pi4j.io.i2c.{I2CBus, I2CFactory}
import de.sciss.numbers.Implicits._
import org.rogach.scallop.{ScallopConf, ScallopOption => Opt}

import java.util.Locale
import scala.swing.{Component, Dimension, MainFrame, Swing}

/** Testing the light-dependent resistor through the ADS1115 converter.
  * This can be used to find sampling rate, gain and thresholds.
  */
object LDRSensor {
  implicit class BinaryString(private val s: String) extends AnyVal {
    def binary: Int = Integer.parseInt(s.filter(_ != '_'), 2)
  }

  case class Config(
                     maxDiff: Int     = 500,
                     period : Int     = 250,
                     thresh : Int     = 200,
                     view   : Boolean = true,
                     verbose: Boolean = false,
                     trig: () => Unit = () => ()
                   )

  def main(args: Array[String]): Unit = { // Create I2C bus
    Locale.setDefault(Locale.US)
    object p extends ScallopConf(args) {
      private val default = Config()

      val maxDiff: Opt[Int] = opt("max-diff", default = Some(default.maxDiff),
        descr = s"Maximum sample difference (default: ${default.maxDiff}).",
        validate = x => x > 0
      )
      val period: Opt[Int] = opt("period", default = Some(default.period),
        descr = s"Period between samples in milliseconds (default: ${default.period}).",
        validate = x => x > 0
      )
      val thresh: Opt[Int] = opt("thresh", default = Some(default.thresh),
        descr = s"Threshold for activation (default: ${default.thresh}).",
        validate = x => x > 0
      )
      val noView: Opt[Boolean] = opt("no-view", default = Some(!default.view),
        descr = "Do not open visual view.",
      )
      verify()
      val config: Config = Config(
        maxDiff = maxDiff(),
        period  = period(),
        thresh  = thresh(),
        view    = !noView(),
      )
    }
    run(p.config)
  }

  def run(c: Config): Unit = {
    val bus = I2CFactory.getInstance(I2CBus.BUS_1)
    // Get I2C device, ADS1115 I2C address is 0x48(72)
    val device = bus.getDevice(0x48)
    // 0x84 = binary 1 000 010 0
    // bit 15: Operational Status. 1 = Start a single conversion.
    // bits 14-12: Input multiplexer configuration. 000 = default
    // bits 11-9: Programmable gain amplifier configuration. 000 = +- 6.144V, 001 = +- 4.096V,
    //    010 = +- 2.048V (default), 011 = +- 1.024V, 100 = +- 0.512V, 101 or 110 or 111 = +- 0.256V
    // bit 8: Device operating mode. 0 = continuous, 1 = single shot or power-down state
    //
    // 0x83 = binary 100 00011.
    // bits 7-5: Data rate. 000 = 8 Hz, 001 = 16 Hz, 010 = 32 Hz, 011 = 64 Hz, 100 = 128 Hz (default),
    //    101 = 250 Hz, 110 = 475 Hz, 111 = 860 Hz
    // bit 4: Comparator mode. 0 = traditional, 1 = windowed
    // bit 3: Comparator polarity of the ALERT/RDY pin. 0 = active low, 1 = active high
    // bit 2: Latching comparator. 0 = non-latching, 1 = latching
    // bits 1-0: Comparator queue and disable. 00 = Assert after one conversion, 01 = Assert after two conversions,
    //    10 = Assert after four conversions, 11 = Disable comparator (default)
    // AINP = AIN0 and AINN = AIN1, +/- 2.048V, Continuous conversion mode, 128 Hz
    //        byte[] config = { (byte) 0b1_000_010_0, (byte) 0b100_00011 };
    // +/- 1.024V, Continuous conversion mode, 8 Hz
    val adcConf = Array("1_000_011_0".binary.toByte, "000_00011".binary.toByte)
    // Select configuration register
    device.write(0x01, adcConf, 0, 2)
    Thread.sleep(500)

    val HIST_SIZE = 128
    var HIST_IDX  = 0
    val history = new Array[Int](HIST_SIZE)

    lazy val view: Component =
      new Component {
        private val scaleX  = 4
        private val width   = HIST_SIZE * scaleX
        private val height  = 480
        preferredSize = new Dimension(width, height)
        opaque = true

        override protected def paintComponent(g: swing.Graphics2D): Unit = {
          super.paintComponent(g)
          g.setColor(java.awt.Color.black)
          g.fillRect(0, 0, peer.getWidth, peer.getHeight)
          g.setColor(java.awt.Color.red)
          val dyT = c.thresh.linLin(0, c.maxDiff.toFloat, 0, height.toFloat).toInt.clip(0, height)
          g.fillRect(0, height - dyT - 2, width, 4)
          var i = 1
          var x1    = history(0)
          var dif   = 0
          var x     = 0
          while (i < HIST_SIZE) {
            val x0  = history(i)
            dif     = Math.abs(x0 - x1)
            x1      = x0
            val dy  = dif.linLin(0, c.maxDiff.toFloat, 0, height.toFloat).toInt.clip(0, height)
            val colr = if (dif > c.thresh) java.awt.Color.red else java.awt.Color.white
            g.setColor(colr)
            g.fillRect(x, height - dy, scaleX - 1, dy)
            x += scaleX
            i += 1
          }
        }
      }

    if (c.view) Swing.onEDT {
      new MainFrame {
        title = "LDR"
        contents = view
        pack().centerOnScreen()
        open()
      }
    }

    val data = new Array[Byte](2)

    def poll(): Int = {
      // Read 2 bytes of data
      // raw_adc msb, raw_adc lsb
      device.read(0x00, data, 0, 2)
      // Convert the data
      val raw_adc0 = ((data(0) & 0xFF) << 8) + (data(1) & 0xFF)
      val raw_adc = if (raw_adc0 > 0x7FFF) {
        raw_adc0 - 0x7FFF
      } else {
        raw_adc0 + 0x8000
      }
      raw_adc
    }

    var prev = poll()
    Thread.sleep(c.period)

    while (true) {
      val next = poll()
      if (c.verbose) System.out.printf("AIN0: %d \n", next)

      if (Math.abs(next - prev) > c.thresh) c.trig()
      prev = next

      if (c.view) {
        history(HIST_IDX) = next
        HIST_IDX += 1
        if (HIST_IDX == HIST_SIZE) HIST_IDX = 0
        view.repaint()
      }

      Thread.sleep(c.period)
    }
  }
}
