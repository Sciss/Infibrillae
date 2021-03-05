package de.sciss.infibrillae

import de.sciss.lucre.synth.Executor

import java.io.File
import javax.imageio.ImageIO
import scala.concurrent.Future

trait VisualPlatform {
  type Ctx = AWTGraphics2D

  def loadImage(name: String): Future[Image[Ctx]] = {
    val path  = s"assets/$name"
    import Executor.executionContext
    Future {
      val peer = ImageIO.read(new File(path))
      new AWTImage(peer)
    }
  }
}
