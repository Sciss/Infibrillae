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

import de.sciss.lucre.swing.View
import de.sciss.proc.LoadWorkspace.{T, fromBlob}
import de.sciss.proc.Workspace.Blob
import org.scalajs.dom.raw.XMLHttpRequest

import scala.concurrent.{Future, Promise}
import scala.scalajs.js.typedarray.{ArrayBuffer, Int8Array}
import scala.util.control.NonFatal

trait LoadWorkspacePlatform {
  def apply(url: String = "assets/workspace.mllt.bin"): Future[(Universe[T], View[T])] = {
    val oReq = new XMLHttpRequest
    oReq.open(method = "GET", url = url, async = true)
    oReq.responseType = "arraybuffer"
    val res = Promise[(Universe[T], View[T])]()

    oReq.onload = { _ =>
      oReq.response match {
        case ab: ArrayBuffer =>
          try {
            val bytes   = new Int8Array(ab).toArray
            val ws      = Blob.fromByteArray(bytes)
            val resVal  = fromBlob(ws)
            res.success(resVal)

          } catch {
            case NonFatal(ex) => res.failure(ex)
          }

        case other =>
          res.failure(new Exception(s"Expected an ArrayBuffer but got $other"))
      }
    }
    oReq.onerror = { _ =>
      res.failure(new Exception(s"XMLHttpRequest failed for '$url'"))
    }

    oReq.send(null)

    res.future
  }
}
