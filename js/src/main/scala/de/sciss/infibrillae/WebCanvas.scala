/*
 *  WebCanvas.scala
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

import org.scalajs.dom
import org.scalajs.dom.{CanvasRenderingContext2D, html}

class WebCanvas(_peer: html.Canvas) extends Canvas[WebGraphics2D] { self =>
  override def width  : Int = _peer.width
  override def height : Int = _peer.height

  def peer: html.Canvas = _peer

  private val ctx = {
    val ctxPeer = _peer.getContext("2d").asInstanceOf[CanvasRenderingContext2D]
    new WebGraphics2D(ctxPeer)
  }

  override def requestAnimationFrame(fun: (WebGraphics2D, Double) => Unit): Unit =
    dom.window.requestAnimationFrame { timeStamp =>
      fun(ctx, timeStamp)
    }

  override def addMouseListener(ml: MouseListener): Unit = {
    _peer.addEventListener[dom.raw.MouseEvent]("mousedown", { e =>
      ml.mouseDown(new WebMouseEvent(e, self))
    })
    _peer.addEventListener[dom.raw.MouseEvent]("mouseup", { e =>
      ml.mouseUp(new WebMouseEvent(e, self))
    })
    _peer.addEventListener[dom.raw.MouseEvent]("mousemove", { e =>
      ml.mouseMove(new WebMouseEvent(e, self))
    })
  }
}
