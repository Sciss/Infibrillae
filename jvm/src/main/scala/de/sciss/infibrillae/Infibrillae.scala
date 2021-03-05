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
import de.sciss.infibrillae.Visual.RecFrame
import de.sciss.lucre.synth.Executor
import de.sciss.proc.{AuralSystem, Durable, LoadWorkspace, SoundProcesses, Universe, Widget}

import java.awt.{BorderLayout, Cursor, EventQueue}
import java.text.SimpleDateFormat
import java.util.{Date, Locale}
import javax.swing.{BorderFactory, JComponent, JFrame, JPanel, WindowConstants}
import scala.swing.event.ButtonClicked
import scala.swing.{Dimension, FlowPanel, ToggleButton}
import scala.util.{Failure, Success}

object Infibrillae {
  def main(args: Array[String]): Unit =
    run()

  type S = Durable
  type T = Durable.Txn

  private var universeOpt = Option.empty[Universe[T]]

  private var visualOpt = Option.empty[Visual[AWTGraphics2D]]

  def run(): Unit = {

    SoundProcesses.init()
    Widget        .init()

    val fut = LoadWorkspace()

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
          canvasPeer.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR))
          val ggRecord = new ToggleButton("Record") { toggle =>
            reactions += {
              case _: ButtonClicked =>
                if (selected) {
                  Visual.recording = true
                } else {
                  val frames = Visual.recFrames
                  Visual.recording = false
                  frames.headOption match {
                    case None => println("Recording is empty!")

                    case Some(frame0) =>
                      val fmt = new SimpleDateFormat("'Record start: 'HH'h'mm'm'ss.SSS's'", Locale.US)
                      println(fmt.format(new Date(frame0.time)))
                      canvas.manualMode = true
                      visualOpt.foreach { vis =>
                        val dirOut = userHome / "Documents" / "infib_rec"
                        dirOut.mkdirs()
                        vis.setTrunkPos(frame0.trunkX, frame0.trunkY)
                        vis.setAnimTime(-0.1)
                        toggle.enabled = false
                        val fps = 25.0

                        def invoke(rem: Vector[RecFrame], frameIdx: Int): Unit =
                          rem match {
                            case frame +: tail =>
                              val outTime = (frameIdx / fps) * 1000
                              val inTime  = (frame.time - frame0.time).toDouble
                              if (inTime < outTime) {
                                invoke(tail, frameIdx = frameIdx)
                              } else {
                                vis.setTrunkTargetPos(frame.trunkX, frame.trunkY)
                                canvas.manualTime = outTime
                                canvas.requestAnimationFrame { (g2, time) =>
                                  vis.paint(g2, time)
                                  val fOut = dirOut / f"frame-${frameIdx + 1}%04d.png"
                                  canvas.saveImage(fOut)
                                  invoke(tail, frameIdx = frameIdx + 1)
                                }
                                canvas.peer.repaint()
                              }

                            case _ =>
                              canvas.manualMode = false
                              toggle.enabled    = true
                        }

                        invoke(frames, frameIdx = 0)
                      }
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
                  Visual(server, canvas).onComplete {
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
        }

      case Failure(ex) =>
        ex.printStackTrace()
    }

    println("End of main.")
  }
}
