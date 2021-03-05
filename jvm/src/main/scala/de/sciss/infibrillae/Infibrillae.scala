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

import de.sciss.lucre.synth.Executor
import de.sciss.proc.{AuralSystem, Durable, LoadWorkspace, SoundProcesses, Universe, Widget}

import java.awt.{BorderLayout, Cursor, EventQueue}
import javax.swing.{BorderFactory, JComponent, JFrame, JPanel, WindowConstants}
import scala.swing.Dimension
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
          val p = new JPanel(new BorderLayout())
          val p1 = new JPanel(new BorderLayout())
          p1.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40))
          p.add(view.component.peer , BorderLayout.SOUTH)
          p1.add(canvasPeer          , BorderLayout.CENTER)
          p.add(p1          , BorderLayout.CENTER)
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
