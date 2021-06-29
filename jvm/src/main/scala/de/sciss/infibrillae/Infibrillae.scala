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

import de.sciss.file._
import de.sciss.lucre.swing.View
import de.sciss.lucre.synth.Executor
import de.sciss.numbers.Implicits.doubleNumberWrapper
import de.sciss.osc
import de.sciss.proc.{AuralSystem, Durable, LoadWorkspace, SoundProcesses, Universe, Widget}

import java.awt.{BorderLayout, Cursor, EventQueue}
import java.net.InetSocketAddress
import java.text.SimpleDateFormat
import java.util.{Date, Locale}
import javax.swing.{BorderFactory, JComponent, JFrame, JPanel, WindowConstants}
import scala.concurrent.Future
import scala.swing.event.ButtonClicked
import scala.swing.{Dimension, FlowPanel, ToggleButton}
import scala.util.{Failure, Success}

object Infibrillae {
  def main(args: Array[String]): Unit = {
    val noOSC = args.contains("--no-osc")
    Locale.setDefault(Locale.US)
    runConnect(noOSC = noOSC)
  }

  type S = Durable
  type T = Durable.Txn

  private var universeOpt = Option.empty[Universe[T]]

  private var visualOpt = Option.empty[Visual[AWTGraphics2D]]

  def runConnect(noOSC: Boolean): Unit = {
    EventQueue.invokeLater { () =>
      println("Workspace loaded.")
      val canvas = new AWTCanvas
      val canvasPeer: JComponent = canvas.peer
      canvasPeer.setPreferredSize(new Dimension(400, 400))
      canvasPeer.setOpaque(true)
      canvasPeer.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR))
      val p  = new JPanel(new BorderLayout())
      val p1 = new JPanel(new BorderLayout())
      p1.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40))
      p1.add(canvasPeer         , BorderLayout.CENTER )
      p .add(p1                 , BorderLayout.CENTER )
      val fr = new JFrame("in|fibrillae")
      fr.setContentPane(p)
      fr.pack()
      fr.setLocationRelativeTo(null)
      fr.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      fr.setVisible(true)

      import Executor.executionContext

      val t: osc.UDP.Transmitter.Directed = if (noOSC) null else {
        val res = osc.UDP.Transmitter(new InetSocketAddress("127.0.0.1", 57120))
        res.connect()
        res
      }
      Visual(t /*server*/, canvas /*, idx = SPACE_IDX*/).onComplete {
        case Success(v) =>
          println("Visual ready.")
          visualOpt = Some(v)

        case Failure(ex) =>
          ex.printStackTrace()
      }
    }
  }

  def runBoot(): Unit = {

    SoundProcesses.init()
    Widget        .init()

    val fut: Future[(Universe[T], View[T])] = LoadWorkspace() // s"assets/workspace-${trunkIds(SPACE_IDX)}.mllt.bin")

    import Executor.executionContext

    fut.onComplete {
      case Success((universe: Universe[T], view)) =>
        universeOpt = Some(universe)
        EventQueue.invokeLater { () =>
          println("Workspace loaded.")
          val canvas = new AWTCanvas
          val canvasPeer: JComponent = canvas.peer
          canvasPeer.setPreferredSize(new Dimension(400, 400))
          canvasPeer.setOpaque(true)
//          canvasPeer.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR))
//          canvasPeer.setCursor(Toolkit.getDefaultToolkit.createCustomCursor(
//            new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB), new Point(0, 0), "hidden"))
          val ggRecord = new ToggleButton("Record") { toggle =>
            reactions += {
              case _: ButtonClicked =>
                if (selected) {
                  Visual.recording = true
                } else {
                  val frames = Visual.recFrames
                  Visual.recording = false
                  (frames.headOption, frames.lastOption) match {
                    case (Some(frameFirst), Some(frameLast)) =>
                      val fmt = new SimpleDateFormat("HH'h'mm'm'ss.SSS's'", Locale.US)
                      println(s"Record Start: ${fmt.format(new Date(frameFirst.time))}")
                      println(s"Record Stop : ${fmt.format(new Date(frameLast.time))}")
                      visualOpt.foreach { vis =>
                        val dirOut = userHome / "Documents" / "infib_rec"
                        dirOut.mkdirs()
                        vis.setTrunkPos(frameFirst.trunkX, frameFirst.trunkY)
                        vis.setAnimTime(-0.1)
                        toggle.enabled    = false
                        canvas.manualMode = true
                        vis.mouseEnabled  = false

                        val fps       = 25.0
                        val durMs     = frameLast.time - frameFirst.time
                        val numFrames = math.max(1, math.round(durMs * 0.001 * fps).toInt)

                        def invoke(frameIdx: Int): Unit =
                          if (frameIdx < numFrames) {
                            val outTime = (frameIdx / fps) * 1000
                            val frameI10 = frames.indexWhere { f =>
                              val inTime = (f.time - frameFirst.time).toDouble
                              inTime > outTime
                            }
                            val frameI1 = if (frameI10 >= 0) frameI10 else numFrames - 1
                            val frameI2 = frameI1 - 1
                            val frame1  = frames(frameI1)
                            val frame2  = frames(frameI2)
                            val frameT1 = (frame1.time - frameFirst.time).toDouble
                            val frameT2 = (frame2.time - frameFirst.time).toDouble
                            val trunkX  = outTime.linLin(frameT1, frameT2, frame1.trunkX, frame2.trunkX)
                            val trunkY  = outTime.linLin(frameT1, frameT2, frame1.trunkY, frame2.trunkY)
                            vis.setTrunkTargetPos(trunkX, trunkY)
                            canvas.manualTime = outTime
                            canvas.repaint { (g2, time) =>
                              vis.paint(g2, time)
                              val fOut = dirOut / f"frame-${frameIdx + 1}%05d.png"
                              canvas.saveImage(fOut)
                              invoke(frameIdx = frameIdx + 1)
                            }
                            canvas.peer.repaint()

                          } else {
                            println(s"Wrote $frameIdx out of ${frames.size} frames.")
                            canvas.manualMode = false
                            toggle.enabled    = true
                            vis.mouseEnabled  = true
                          }

                        invoke(frameIdx = 0)
                      }

                    case _ => println("Recording is empty!")
                  }
                }
            }
          }
          val p  = new JPanel(new BorderLayout())
          val p1 = new JPanel(new BorderLayout())
          val p2 = new FlowPanel(ggRecord)
          p1.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40))
          p .add(view.component.peer, BorderLayout.SOUTH  )
          p1.add(canvasPeer         , BorderLayout.CENTER )
          p .add(p1                 , BorderLayout.CENTER )
          p .add(p2.peer            , BorderLayout.NORTH  )
          val fr = new JFrame("in|fibrillae")
          fr.setContentPane(p)
          fr.pack()
          fr.setLocationRelativeTo(null)
          fr.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
          fr.setVisible(true)

          universe.cursor.step { implicit tx =>
            universe.auralSystem.reactNow { implicit tx => {
              case AuralSystem.Running(server) =>
                tx.afterCommit {
                  Visual(??? /*server*/, canvas /*, idx = SPACE_IDX*/).onComplete {
                    case Success(v) =>
                      println("Visual ready.")
//                      val Palabra(txt, txtX, txtY) = palabras(SPACE_IDX)
//                      v.setText(txt, txtX, txtY)
                      visualOpt = Some(v)

                    case Failure(ex) =>
                      ex.printStackTrace()
                  }
                }
              case _ =>
            }}
          }
        }

      case Failure(ex) =>
        ex.printStackTrace()
    }

    println("End of main.")
  }
}
