package de.sciss.infibrillae

import de.sciss.lucre.synth.Executor

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import scala.concurrent.Future

trait VisualPlatform {
  type Ctx = AWTGraphics2D

  def loadImage(name: String): Future[Image[Ctx]] = {
    val path  = s"assets/$name"
    import Executor.executionContext
    Future {
      val peer0 = ImageIO.read(new File(path))
      val peer = if (peer0.getType == BufferedImage.TYPE_INT_ARGB) peer0 else {
        val b = new BufferedImage(peer0.getWidth, peer0.getHeight, BufferedImage.TYPE_INT_ARGB)
        val g = b.createGraphics()
        g.drawImage(peer0, 0, 0, null)
        g.dispose()
        b
      }
      new AWTImage(peer)
    }
  }
}
