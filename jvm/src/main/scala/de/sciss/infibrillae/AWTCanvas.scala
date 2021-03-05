package de.sciss.infibrillae

import java.awt.event.MouseAdapter
import java.awt.image.BufferedImage
import java.awt.{Graphics, RenderingHints, event}
import javax.swing.{JComponent, Timer}

class AWTCanvas extends Canvas[AWTGraphics2D] {
  private var animFunArr    = new Array[(AWTGraphics2D, Double) => Unit](8)
  private var animFunNum    = 0
  private var animStartTime = 0L

  private object _peer extends JComponent {
    private var buf: BufferedImage = null
    private var g2w: AWTGraphics2D = null

//    setDoubleBuffered(true)

    override def paintComponent(g: Graphics): Unit = {
      if (buf == null || buf.getWidth != getWidth || buf.getHeight != getHeight) {
        if (buf != null) buf.flush()
        buf = new BufferedImage(getWidth, getHeight, BufferedImage.TYPE_INT_ARGB)
        val gi = buf.createGraphics()
        gi.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g2w = new AWTGraphics2D(gi)
      }

//      val g2  = g.asInstanceOf[java.awt.Graphics2D]
//      val g2w = new AWTGraphics2D(g2)
      val now = System.currentTimeMillis()
      val dt  = now - animStartTime
      val arr = animFunArr
      var i = 0
      val n = animFunNum
      animFunNum = 0
      while (i < n) {
        val f = arr(i)
        arr(i) = null
        f(g2w, dt.toDouble)
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

  override def requestAnimationFrame(fun: (AWTGraphics2D, Double) => Unit): Unit = {
    if (animFunArr.length == animFunNum) {
      val a = new Array[(AWTGraphics2D, Double) => Unit](animFunNum << 1)
      System.arraycopy(animFunArr, 0, a, 0, animFunNum)
      animFunArr = a
    }
    animFunArr(animFunNum) = fun
    animFunNum += 1
    if (!timer.isRunning) {
      animStartTime = System.currentTimeMillis()
      timer.restart()
    }
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
}
