/*
 *  AWTCanvas.scala
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

import java.awt.event.{KeyAdapter, MouseAdapter}
import java.awt.image.BufferedImage
import java.awt.{Graphics, RenderingHints, event}
import javax.imageio.ImageIO
import javax.swing.{JComponent, Timer}

object AWTCanvas {
  private lazy val hiddenCursor: java.awt.Cursor =
    java.awt.Toolkit.getDefaultToolkit.createCustomCursor(
      new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB), new java.awt.Point(0, 0), "hidden")
}
class AWTCanvas extends Canvas[AWTGraphics2D] {
  private var animFunArr    = new Array[(AWTGraphics2D, Double) => Unit](8)
  private var animFunNum    = 0
  private var animStartTime = 0L
  private var _manualMode   = false

  var manualTime = 0.0

  def manualMode: Boolean = _manualMode
  def manualMode_=(value: Boolean): Unit = if (_manualMode != value) {
    _manualMode = value
    if (value) {
      timer.stop()
    } else {
      startTimer()
    }
  }

  def saveImage(f: File): Unit = {
    val fmt = f.extL
    ImageIO.write(_peer.image, fmt, f)
  }

  private object _peer extends JComponent {
    private var buf: BufferedImage = null
    private var g2w: AWTGraphics2D = null

    def image: BufferedImage = buf

//    setDoubleBuffered(true)

    override def paintComponent(g: Graphics): Unit = {
      if (buf == null || buf.getWidth != getWidth || buf.getHeight != getHeight) {
        if (buf != null) buf.flush()
        buf = new BufferedImage(getWidth, getHeight, BufferedImage.TYPE_INT_ARGB)
        val gi = buf.createGraphics()
        gi.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g2w = if (g2w == null) new AWTGraphics2D(gi) else g2w.newPeer(gi)
      }

//      val g2  = g.asInstanceOf[java.awt.Graphics2D]
//      val g2w = new AWTGraphics2D(g2)
      val dt  = if (_manualMode) manualTime else (System.currentTimeMillis() - animStartTime).toDouble
      val arr = animFunArr
      var i = 0
      val n = animFunNum
      animFunNum = 0
      while (i < n) {
        val f = arr(i)
        arr(i) = null
        f(g2w, dt)
        i += 1
      }
      g.drawImage(buf, 0, 0, this)
    }
  }

//  private val g2  = _peer.getGraphics.asInstanceOf[java.awt.Graphics2D]
//  private val g2w = new AWTGraphics2D(g2)

  def peer: JComponent = _peer

  override def width  : Int = _peer.getWidth
  override def height : Int = _peer.getHeight

  private val fps = 60

  private val timer = new Timer(1000/fps, { _ =>
    _peer.repaint()
    _peer.getToolkit.sync()
  })

  override def repaint(fun: (AWTGraphics2D, Double) => Unit): Unit = {
    if (animFunArr.length == animFunNum) {
      val a = new Array[(AWTGraphics2D, Double) => Unit](animFunNum << 1)
      System.arraycopy(animFunArr, 0, a, 0, animFunNum)
      animFunArr = a
    }
    animFunArr(animFunNum) = fun
    animFunNum += 1
    if (!timer.isRunning && !_manualMode) {
      startTimer()
    }
  }

  private def startTimer(): Unit = {
    animStartTime = System.currentTimeMillis()
    timer.restart()
  }

  override def addMouseListener(ml: MouseListener): Unit = {
    val ma = new MouseAdapter {
      override def mousePressed(e: event.MouseEvent): Unit =
        ml.mouseDown(new AWTMouseEvent(e))

      override def mouseReleased(e: event.MouseEvent): Unit =
        ml.mouseUp(new AWTMouseEvent(e))

      override def mouseDragged(e: event.MouseEvent): Unit =
        ml.mouseMove(new AWTMouseEvent(e))

      override def mouseMoved(e: event.MouseEvent): Unit =
        ml.mouseMove(new AWTMouseEvent(e))
    }
    _peer.addMouseListener      (ma)
    _peer.addMouseMotionListener(ma)
  }

  override def addKeyListener(kl: KeyListener): Unit = {
    val ka = new KeyAdapter {
      override def keyPressed(e: event.KeyEvent): Unit =
        kl.keyDown(new AWTKeyEvent(e))

      override def keyReleased(e: event.KeyEvent): Unit =
        kl.keyUp(new AWTKeyEvent(e))
    }
    _peer.addKeyListener(ka)
  }

  override def requestFocus(): Unit = _peer.requestFocus()

  private var _cursor: Cursor = Cursor.Default

  override def cursor: Cursor = _cursor
  override def cursor_=(value: Cursor): Unit = if (_cursor != value) {
    _cursor = value
    val awtC = value match {
      case Cursor.Default   => java.awt.Cursor.getDefaultCursor
      case Cursor.CrossHair => java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.CROSSHAIR_CURSOR)
      case Cursor.Hidden    => AWTCanvas.hiddenCursor
    }
    _peer.setCursor(awtC)
  }}
