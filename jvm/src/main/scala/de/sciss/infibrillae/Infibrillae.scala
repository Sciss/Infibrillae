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
import de.sciss.numbers.Implicits._
import de.sciss.osc
import de.sciss.proc.{AuralSystem, Durable, LoadWorkspace, SoundProcesses, Universe, Widget}
import org.rogach.scallop.{ScallopConf, ScallopOption => Opt}

import java.awt
import java.awt.event.{ActionEvent, InputEvent, KeyEvent}
import java.awt.image.BufferedImage
import java.awt.{BorderLayout, Cursor, EventQueue, GraphicsEnvironment, Toolkit}
import java.net.InetSocketAddress
import java.text.SimpleDateFormat
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.{Date, Locale, Timer, TimerTask}
import javax.swing.{AbstractAction, BorderFactory, JComponent, JFrame, JPanel, KeyStroke, SwingUtilities, WindowConstants}
import scala.concurrent.Future
import scala.swing.event.ButtonClicked
import scala.swing.{Dimension, FlowPanel, ToggleButton}
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

object Infibrillae {
  case class Config(
                   verbose      : Boolean = false,
                   oscPort      : Int     = 57120,
                   doubleSize   : Boolean = false,
                   fullScreen   : Boolean = false,
                   viewShiftX   : Int     = 0,
                   viewShiftY   : Int     = 0,
                   shutdownHour : Int     = 21,
                   initDelay    : Int     = 120,
                   )

  def main(args: Array[String]): Unit = {
    Locale.setDefault(Locale.US)
    object p extends ScallopConf(args) {
      private val default = Config()

      val verbose: Opt[Boolean] = opt("verbose", short = 'V', default = Some(false),
        descr = "Verbose printing."
      )
      val oscPort: Opt[Int] = opt("osc-port", default = Some(default.oscPort),
        descr = s"OSC port of audio system, or 0 to disable (default: ${default.oscPort})."
      )
      val doubleSize: Opt[Boolean] = opt("double-size", default = Some(default.doubleSize),
        descr = "Draw pixels at zoom level 200%"
      )
      val fullScreen: Opt[Boolean] = opt("full-screen", default = Some(default.fullScreen),
        descr = "Make the window full-screen"
      )
      val viewShiftX: Opt[Int] = opt("view-shift-x", default = Some(default.viewShiftX),
        descr = s"Horizontal image shift in pixels (default: ${default.viewShiftX})."
      )
      val viewShiftY: Opt[Int] = opt("view-shift-y", default = Some(default.viewShiftY),
        descr = s"Vertical image shift in pixels (default: ${default.viewShiftY})."
      )
      val shutdownHour: Opt[Int] = opt("shutdown",
        descr = s"Hour of Pi shutdown (or 0 to avoid shutdown) (default: ${default.shutdownHour})",
        default = Some(default.shutdownHour),
        validate = x => x >= 0 && x <= 24
      )
      val initDelay: Opt[Int] = opt("init-delay", default = Some(default.initDelay),
        descr = s"Initial delay in seconds (to make sure date-time is synced) (default: ${default.initDelay})."
      )
      verify()
      val config: Config = Config(
        verbose       = verbose(),
        oscPort       = oscPort(),
        doubleSize    = doubleSize(),
        fullScreen    = fullScreen(),
        viewShiftX    = viewShiftX(),
        viewShiftY    = viewShiftY(),
        shutdownHour  = shutdownHour(),
        initDelay     = initDelay(),
      )
    }
    run(p.config)
  }

  type S = Durable
  type T = Durable.Txn

  private var universeOpt = Option.empty[Universe[T]]

  private var visualOpt = Option.empty[Visual[AWTGraphics2D]]

  final def name          : String = "in|fibrillae (window)"
  final def nameAndVersion: String = s"$name $fullVersion"

  private def buildInfString(key: String): String = try {
    val clazz = Class.forName("de.sciss.infibrillae.BuildInfo")
    val m     = clazz.getMethod(key)
    m.invoke(null).toString
  } catch {
    case NonFatal(_) => "?"
  }

  final def version       : String = buildInfString("version")
  final def builtAt       : String = buildInfString("builtAtString")
  final def fullVersion   : String = s"v$version, built $builtAt"

  private def mkView(c: Config): Unit = {
    println("Workspace loaded.")
    val canvas: AWTCanvas = if (!c.doubleSize) new AWTCanvas else new AWTCanvas {
      override protected def drawContents(img: BufferedImage, target: awt.Graphics2D): Unit =
        target.drawImage(img, 0, 0, 800, 800, null)
    }
    val canvasPeer: JComponent = canvas.peer
    val extent  = if (c.doubleSize) 800 else 400
    val dim     = new Dimension(extent, extent)
    canvasPeer.setPreferredSize (dim)
    canvasPeer.setMaximumSize   (dim)
    canvasPeer.setOpaque(true)
    canvasPeer.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR))
    val p  = new JPanel(new BorderLayout())
    val sd = GraphicsEnvironment.getLocalGraphicsEnvironment.getDefaultScreenDevice
    val gc = sd.getDefaultConfiguration
    val border = if (c.fullScreen) {
      val sb = gc.getBounds
      val spaceH    = sb.width - extent
      val padLeft   = (spaceH / 2 + c.viewShiftX).clip(0, spaceH)
      val padRight  = spaceH - padLeft
      val spaceV    = sb.height - extent
      val padTop    = (spaceV / 2 + c.viewShiftY).clip(0, spaceV)
      val padBottom = spaceV - padTop
      BorderFactory.createMatteBorder(padTop, padLeft, padBottom, padRight, java.awt.Color.BLACK)
    } else {
      BorderFactory.createEmptyBorder(40, 40, 40, 40)
    }
    p.setBorder(border)
    p.add(canvasPeer, BorderLayout.CENTER )

    val fr = new JFrame(gc)
    if (c.fullScreen) {
      fr.setUndecorated(true)
    } else {
      fr.setTitle("in|fibrillae")
    }

    fr.setContentPane(p)
    fr.pack()
    if (!c.fullScreen) fr.setLocationRelativeTo(null)
    fr.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
    fr.setVisible(true)

    if (c.fullScreen) toggleFullScreen(fr)

    // XXX TODO: doesn't work:
    //    installFullScreenKey(fr, canvasPeer)

    import Executor.executionContext

    val t: osc.UDP.Transmitter.Directed = if (c.oscPort <= 0) null else {
      val res = osc.UDP.Transmitter(new InetSocketAddress("127.0.0.1", c.oscPort))
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

  def shutdown(): Unit = {
    import sys.process._
    Seq("sudo", "shutdown", "now").!
  }

  private def log(what: => String): Unit = println(what)

  def run(c: Config): Unit = {
    println(nameAndVersion)

    val initDelayMS = math.max(0, c.initDelay) * 1000L
    if (initDelayMS > 0) {
      println(s"Waiting for ${c.initDelay} seconds.")
      Thread.sleep(initDelayMS)
    }

    val odt       = OffsetDateTime.now()
    val date      = Date.from(odt.toInstant)
    println(s"The date and time: $date")

    EventQueue.invokeLater(() => mkView(c))

    lazy val timer = new Timer

    if (c.shutdownHour > 0) {
      val odtSD0  = odt.withHour(c.shutdownHour % 24).truncatedTo(ChronoUnit.HOURS)
      val odtSD   = if (odtSD0.isAfter(odt)) odtSD0 else {
        println("WARNING: Shutdown hour lies on next day. Shutting down in two hours instead!")
        odt.plus(2, ChronoUnit.HOURS)
      }
      val dateSD  = Date.from(odtSD.toInstant)
      timer.schedule(new TimerTask {
        override def run(): Unit = {
          log("About to shut down...")
          Thread.sleep(8000)
          // writeLock.synchronized {
            shutdown()
          // }
        }
      }, dateSD)
      log(s"Shutdown scheduled for $dateSD")
    }
  }

  def toggleFullScreen(frame: javax.swing.JFrame): Unit = {
    val gc = frame /*.peer*/.getGraphicsConfiguration
    val sd = gc.getDevice
    val w  = SwingUtilities.getWindowAncestor(frame /*.peer*/.getRootPane)
    sd.setFullScreenWindow(if (sd.getFullScreenWindow == w) null else w)
  }

  def installFullScreenKey(frame: javax.swing.JFrame, display: javax.swing.JComponent): Unit = {
    val iMap    = display/*.peer*/.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
    val aMap    = display/*.peer*/.getActionMap
    val fsName  = "fullscreen"
    iMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, Toolkit.getDefaultToolkit.getMenuShortcutKeyMask |
      InputEvent.SHIFT_MASK), fsName)
    aMap.put(fsName, new AbstractAction(fsName) {
      def actionPerformed(e: ActionEvent): Unit = {
        println("toggleFullScreen")
        toggleFullScreen(frame)
      }
    })
  }

  def runBoot(c: Config): Unit = {

    SoundProcesses.init()
    Widget        .init()

    val fut: Future[(Universe[T], View[T])] = LoadWorkspace() // s"assets/workspace-${trunkIds(SPACE_IDX)}.mllt.bin")

    import Executor.executionContext

    fut.onComplete {
      case Success((universe: Universe[T], view)) =>
        universeOpt = Some(universe)
        EventQueue.invokeLater { () =>
          println("Workspace loaded.")
          val canvas: AWTCanvas = if (!c.doubleSize) new AWTCanvas else new AWTCanvas {
            override protected def drawContents(img: BufferedImage, target: awt.Graphics2D): Unit =
              target.drawImage(img, 0, 0, 800, 800, null)
          }
          val canvasPeer: JComponent = canvas.peer
          val extent = if (c.doubleSize) 800 else 400
          canvasPeer.setPreferredSize(new Dimension(extent, extent))
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
