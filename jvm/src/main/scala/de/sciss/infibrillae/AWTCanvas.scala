package de.sciss.infibrillae

import java.awt.{Graphics, event}
import java.awt.event.MouseAdapter
import javax.swing.{JComponent, Timer}

class AWTCanvas extends Canvas[AWTGraphics2D] {
  private var animFun: (AWTGraphics2D, Double) => Unit = (_, _) => ()
  private var animStartTime = 0L

  private object _peer extends JComponent {
    override def paintComponent(g: Graphics): Unit = {
      val g2  = g.asInstanceOf[java.awt.Graphics2D]
      val g2w = new AWTGraphics2D(g2)
      val now = System.currentTimeMillis()
      val dt  = now - animStartTime
      animFun(g2w, dt.toDouble)
    }
  }

  def peer: JComponent = _peer

  override def width  : Int = _peer.getWidth
  override def height : Int = _peer.getHeight

  private val timer = new Timer(1000/30, { _ =>
    _peer.repaint()
    _peer.getToolkit.sync()
  })

  override def requestAnimationFrame(fun: (AWTGraphics2D, Double) => Unit): Unit = {
    animFun = fun
    if (!timer.isRunning) {
      animStartTime = System.currentTimeMillis()
      timer.restart()
    }
  }

  override def addMouseListener(ml: MouseListener): Unit =
    _peer.addMouseListener(new MouseAdapter {
      override def mousePressed(e: event.MouseEvent): Unit =
        ml.mouseDown(new AWTMouseEvent(e))

      override def mouseReleased(e: event.MouseEvent): Unit =
        ml.mouseUp(new AWTMouseEvent(e))

      override def mouseDragged(e: event.MouseEvent): Unit =
        ml.mouseMove(new AWTMouseEvent(e))

      override def mouseMoved(e: event.MouseEvent): Unit =
        ml.mouseMove(new AWTMouseEvent(e))
    })
}
