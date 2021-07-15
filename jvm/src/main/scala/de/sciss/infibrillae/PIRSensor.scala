/*
 *  PIRSensor.scala
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

import com.pi4j.io.gpio.event.{GpioPinDigitalStateChangeEvent, GpioPinListenerDigital}
import com.pi4j.io.gpio.{GpioFactory, RaspiPin, Pin => JPin}
import de.sciss.numbers.Implicits._
import org.rogach.scallop.{ScallopConf, ScallopOption => Opt}

import java.util.Locale
import scala.annotation.switch
import scala.swing.{BoxPanel, Button, MainFrame, Orientation, Swing}

// attempt with the passive infrared sensors
// (result: they don't work through the glass window)
object PIRSensor {
  case class Config(
                   pins: List[Int] = List(0, 1)
                   )
  def main(args: Array[String]): Unit = {
    Locale.setDefault(Locale.US)
    object p extends ScallopConf(args) {
      private val default = Config()

      val pins: Opt[List[Int]] = opt("pins", default = Some(default.pins),
        descr = s"GPIO pin numbers (wiring-pi scheme) (default: ${default.pins})."
      )
      verify()
      val config: Config = Config(
        pins = pins(),
      )
    }
    run(p.config)
  }

  private def mapPin(in: Int): JPin =
    (in.clip(0, 13): @switch) match {
      case  0 => RaspiPin.GPIO_00
      case  1 => RaspiPin.GPIO_01
      case  2 => RaspiPin.GPIO_02
      case  3 => RaspiPin.GPIO_03
      case  4 => RaspiPin.GPIO_04
      case  5 => RaspiPin.GPIO_05
      case  6 => RaspiPin.GPIO_06
      case  7 => RaspiPin.GPIO_07
      case  8 => RaspiPin.GPIO_08
      case  9 => RaspiPin.GPIO_09
      case 10 => RaspiPin.GPIO_10
      case 11 => RaspiPin.GPIO_11
      case 12 => RaspiPin.GPIO_12
      case 13 => RaspiPin.GPIO_13
      case 14 => RaspiPin.GPIO_14
      case 15 => RaspiPin.GPIO_15
      case 16 => RaspiPin.GPIO_16
      case 17 => RaspiPin.GPIO_17
      case 18 => RaspiPin.GPIO_18
      case 19 => RaspiPin.GPIO_19
      case 20 => RaspiPin.GPIO_20
      case 21 => RaspiPin.GPIO_21
      case 22 => RaspiPin.GPIO_22
      case 23 => RaspiPin.GPIO_23
      case 24 => RaspiPin.GPIO_24
      case 25 => RaspiPin.GPIO_25
      case 26 => RaspiPin.GPIO_26
      case 27 => RaspiPin.GPIO_27
      case 28 => RaspiPin.GPIO_28
      case 29 => RaspiPin.GPIO_29
      case 30 => RaspiPin.GPIO_30
      case 31 => RaspiPin.GPIO_31
    }

  def run(c: Config): Unit = {
    val pins      = c.pins.map(mapPin)
    val provider  = GpioFactory.getDefaultProvider
    val instance  = GpioFactory.getInstance()
    Swing.onEDT {
      val gg = pins.map { pin =>
        val b = new Button(pin.getName)
        b.focusable   = false
        b.foreground  = java.awt.Color.white
        val _in = instance.provisionDigitalInputPin(provider, pin) // , resistance)
//        if (deb0 >= 0) _in.setDebounce(deb0)
        def setState(state: Boolean): Unit =
          b.background = if (state) java.awt.Color.red else java.awt.Color.black

        val listener = new GpioPinListenerDigital {
          def handleGpioPinDigitalStateChangeEvent(e: GpioPinDigitalStateChangeEvent): Unit = {
            val now = e.getState.isHigh
            setState(now)
          }
        }
        setState(_in.getState.isHigh)
        _in.addListener(listener)
        b
      }
      val p = new BoxPanel(Orientation.Horizontal) {
        contents ++= gg
      }
      new MainFrame {
        contents = p
        pack().centerOnScreen()
        open()
      }
    }
  }
}
