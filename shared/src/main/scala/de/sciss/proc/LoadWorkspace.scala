/*
 *  LoadWorkspace.scala
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

import de.sciss.lucre.edit.UndoManager
import de.sciss.lucre.swing.View
import de.sciss.lucre.{Cursor, expr}
import de.sciss.proc.Implicits._
import de.sciss.proc.Workspace.Blob

object LoadWorkspace extends LoadWorkspacePlatform {
  type S = Durable
  type T = Durable.Txn

  def fromBlob(ws: Blob, elem: String = "start-web"): (Universe[T], View[T]) = {
    println("Workspace meta data:")
    ws.meta.foreach(println)
    implicit val cursor: Cursor[T] = ws.cursor
    implicit val undo: UndoManager[T] = UndoManager()
    val resOpt = cursor.step { implicit tx =>
      val fRoot = ws.root
      //              fRoot.iterator.foreach { child =>
      //                println(s"CHILD: ${child.name}")
      //              }
      fRoot.$[Widget](elem).map { w =>
        implicit val u: Universe[T] = Universe.dummy[T]
        val wH = tx.newHandle(w)
        implicit val ctx: expr.Context[T] = ExprContext(selfH = Some(wH))
        val gW = w.graph().value
        val _view = gW.expand[T]
        _view.initControl()
        (u, _view)
      }
    }
    resOpt.getOrElse(sys.error(s"No element '$elem' found"))
  }
}
