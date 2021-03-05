/*
 *  LoadWorkspacePlatform.scala
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

package de.sciss.proc

import de.sciss.asyncfile.AsyncFile
import de.sciss.file.File
import de.sciss.lucre.swing.View
import de.sciss.lucre.synth.Executor
import de.sciss.proc.LoadWorkspace.{T, fromBlob}
import de.sciss.proc.Workspace.Blob

import java.nio.ByteBuffer
import scala.concurrent.Future

trait LoadWorkspacePlatform {
  def apply(url: String = "assets/workspace.mllt.bin"): Future[(Universe[T], View[T])] = {
    import Executor.executionContext
    AsyncFile.openRead(new File(url).toURI).flatMap { ch =>
      val bytes = new Array[Byte](ch.size.toInt)
      val bb    = ByteBuffer.wrap(bytes)
      ch.read(bb).map { _ =>
        val ws = Blob.fromByteArray(bytes)
        fromBlob(ws)
      } .andThen {
        case _ => ch.close()
      }
    }
  }
}
