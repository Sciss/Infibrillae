/*
 *  Infibrillae.scala
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

import com.raquo.laminar.api.L.{documentEvents, render, unsafeWindowOwner}
import de.sciss.asyncfile.AsyncFile
import de.sciss.audiofile.AudioFile
import de.sciss.log.Level
import de.sciss.lucre.swing.View
import de.sciss.lucre.synth.Executor
import de.sciss.proc.{AuralSystem, Durable, LoadWorkspace, SoundProcesses, Universe, Widget}
import de.sciss.synth.{Server => SServer}
import de.sciss.{osc, synth}
import org.scalajs.dom
import org.scalajs.dom.html

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}
import scala.util.{Failure, Success}

@JSExportTopLevel("Infibrillae")
object Infibrillae {
  def main(args: Array[String]): Unit =
    runGUI()

  type S = Durable
  type T = Durable.Txn

  def runGUI(): Unit = {
    println("Infibrillae initialized.")
    documentEvents.onDomContentLoaded.foreach { _ =>
      run()
    } (unsafeWindowOwner)
  }

  private var universeOpt = Option.empty[Universe[T]]

  @JSExport
  def startAural(): Unit = {
    println("startAural() called")
    universeOpt.fold[Unit] {
      println("SoundProcesses is not initialized yet.")
    } { u =>
      u.cursor.step { implicit tx =>
        val sCfg = synth.Server.Config()
        val cCfg = synth.Client.Config()
        sCfg.inputBusChannels   = 0
        sCfg.outputBusChannels  = 2
        sCfg.transport          = osc.Browser
//        u.auralSystem.start(sCfg, cCfg, connect = true)
        u.auralSystem.connect(sCfg, cCfg)
      }
    }
  }

  private var visualOpt = Option.empty[Visual[WebGraphics2D]]

  @JSExportTopLevel("setComposite")
  def setComposite(code: String): Unit = {
    visualOpt.foreach(_.setComposite(code))
  }

//  @JSExportTopLevel("setTextColor")
//  def setTextColor(code: String): Unit = {
//    visualOpt.foreach(_.setTextColor(code))
//  }

//  @JSExportTopLevel("setText")
//  def setText(s: String, x: Double, y: Double): Unit = {
//    visualOpt.foreach(_.setText(s, x, y))
//  }

  @JSExportTopLevel("dumpOSC")
  def dumpOSC(code: Int = 1): Unit =
    SServer.default.dumpOSC(osc.Dump(code))

  @JSExportTopLevel("dumpTree")
  def dumpTree(): Unit = {
    import de.sciss.synth.Ops._
    SServer.default.dumpTree(controls = true)
  }

  @JSExportTopLevel("cmdPeriod")
  def cmdPeriod(): Unit = {
    import de.sciss.synth.Ops._
    SServer.default.freeAll()
  }

  @JSExportTopLevel("serverCounts")
  def serverCounts(): Unit =
    println(SServer.default.counts)

  @JSExportTopLevel("sendOSC")
  def sendOSC(cmd: String, args: js.Any*): Unit =
    SServer.default.!(osc.Message(cmd, args: _*))

  @JSExport
  def run(): Unit = {

//    FScape        .init()
    SoundProcesses.init()
    Widget        .init()

    AsyncFile.log.level       = Level.Info  // Debug
    AudioFile.log.level       = Level.Info  // Debug
//    fscape.Log.stream.level   = Level.Off   // Level.Info // Debug
//    fscape.Log.control.level  = Level.Off   // Level.Info // Debug

    AsyncFile.log.out         = Console.out
    AudioFile.log.out         = Console.out
//    fscape.Log.stream.out     = Console.out
//    fscape.Log.control.out    = Console.out

    SoundProcesses.logAural.level = Level.Info  // Debug

    val container: dom.Element = dom.document.getElementById("piece")

    val fut: Future[(Universe[T], View[T])] = LoadWorkspace() // s"assets/workspace-${trunkIds(SPACE_IDX)}.mllt.bin")
//    val fut = TestBufferPrepare()

    import Executor.executionContext

    fut.onComplete {
      case Success((universe: Universe[T], view)) =>
        universeOpt = Some(universe)
        container.removeChild(container.firstChild)
        /*val root: RootNode =*/ render(container, view.component)
        val canvasPeer  = dom.document.getElementById("canvas").asInstanceOf[html.Canvas]
        val canvas      = new WebCanvas(canvasPeer)
        universe.cursor.step { implicit tx =>
          universe.auralSystem.reactNow { implicit tx => {
            case AuralSystem.Running(_ /*server*/) =>
              tx.afterCommit {
                val t = osc.Browser.Transmitter(osc.Browser.Address(57120))
                t.connect()
                Visual(t, canvas /*, idx = SPACE_IDX*/).onComplete {
                  case Success(v) =>
                    println("Visual ready.")
                    visualOpt = Some(v)

                  case Failure(ex) =>
                    ex.printStackTrace()
                }
              }
            case _ =>
          }}
        }

      case Failure(ex) =>
        ex.printStackTrace()
    }

    println("End of main.")
  }
}
