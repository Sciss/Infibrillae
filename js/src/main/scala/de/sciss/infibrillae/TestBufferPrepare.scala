/*
 *  TestBufferPrepare.scala
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

import de.sciss.asyncfile.AsyncFile
import de.sciss.asyncfile.Ops.URIOps
import de.sciss.audiofile.{AudioFileSpec, AudioFileType, SampleFormat}
import de.sciss.lucre.edit.UndoManager
import de.sciss.lucre.store.InMemoryDB
import de.sciss.lucre.swing.View
import de.sciss.lucre.synth.Executor.executionContext
import de.sciss.lucre.{expr, swing, Artifact => LArtifact, ArtifactLocation => LArtifactLocation}
import de.sciss.proc.AuralSystem.{Running, Stopped}
import de.sciss.proc.{Durable, ExprContext, Universe}
import de.sciss.synth.{SynthGraph, ugen}
import de.sciss.{proc, synth}
import org.scalajs.dom.raw.XMLHttpRequest

import java.net.URI
import java.nio.ByteBuffer
import scala.concurrent.{Future, Promise}
import scala.scalajs.js.typedarray.{ArrayBuffer, Int8Array}
import scala.util.control.NonFatal

object TestBufferPrepare {
  type S = Durable
  type T = Durable.Txn

  val FILE_NAME_REMOTE  = "HS_MergedCutFrom3'35Rsmp-Trns-BleachI-453.aif"
  val FILE_NAME_LOCAL   = "foo.aif"
  val FILE_FRAMES       = 2023731L
  val FILE_CHANNELS     = 2
  val rootURI           = new URI("idb", "/", null)

  def apply(): Future[(Universe[T], View[T])] = {
    val oReq  = new XMLHttpRequest
    val url   = s"assets/$FILE_NAME_REMOTE"
    oReq.open(method = "GET", url = url, async = true)
    oReq.responseType = "arraybuffer"
    val res = Promise[(Universe[T], View[T])]()

    oReq.onload = { _ =>
//      println(s"onload; status ${oReq.status}")
      oReq.response match {
        case ab: ArrayBuffer if oReq.status == 200 =>
          try {
            val bytes = new Int8Array(ab).toArray
            println(s"ArrayBuffer byteLength is ${ab.byteLength}")
            val futLoad = for {
              af  <- AsyncFile.openWrite(rootURI / FILE_NAME_LOCAL)
              _   <- af.write(ByteBuffer.wrap(bytes))
              _   <- af.close()
              tup <- createWorkspace()
            } yield tup
            res.completeWith(futLoad)

          } catch {
            case NonFatal(ex) => res.failure(ex)
          }

        case _ if oReq.status != 200 =>
          res.failure(new Exception(s"Download failed with status ${oReq.status}"))

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

  private def createWorkspace(): Future[(Universe[T], View[T])] = {
    implicit val cursor: S = Durable(InMemoryDB())

    lazy val gProcSave = SynthGraph {
      import synth.proc.graph._
      import ugen._

      val pitch = LFSaw.kr(0.14)
        .mulAdd(24, LFSaw.kr(List(8, 7.23))
          .mulAdd(2, 80))
      val osc   = SinOsc.ar(pitch.midiCps) * 0.1
      val verb  = CombN.ar(osc, 0.2, 0.2, 4)

      // val SR    = 48000.0
      val b     = Buffer.Empty(/*SR * 20*/ FILE_FRAMES, numChannels = FILE_CHANNELS)
      val sig   = Seq.tabulate(FILE_CHANNELS)(verb.out)
      val rb    = RecordBuf.ar(sig, buf = b, loop = 0)
      val rDone = Done.kr(rb)
      val wb    = Action.WriteBuf(rDone, "out", buf = b, fileType = 0, sampleFormat = 1)

      DC.kr(0).poll(rDone , "REC DONE"  )
      DC.kr(0).poll(wb    , "WRITE DONE")

      StopSelf(wb)

      Out.ar(0, verb)
    }

    lazy val gProcLoad = SynthGraph {
      import synth.proc.graph._
      import ugen._

      val b   = Buffer("in")
      val sig = PlayBuf.ar(numChannels = FILE_CHANNELS, buf = b)
      Out.ar(0, sig)
    }

    lazy val gW0 = swing.Graph {
      import expr.graph._
      import swing.graph._

      val rProcLoad = Runner("proc-load")
      val rProcSave = Runner("proc-save")

      val as              = AuralSystem()
      val booted          = as.isRunning
//      val bootedT         = booted.toTrig

      val actBoot = as.runWith(
        AuralSystem.InputBusChannels  -> 0,
        AuralSystem.OutputBusChannels -> 2,
        AuralSystem.MemorySize -> 32768,
      )

      val ggBoot = Button("Boot")
      ggBoot.clicked ---> actBoot
      ggBoot.enabled = !booted

      def mkStartStop(r: Runner): (Widget, Widget) = {
        val ggStart = Button("Play")
        val ggStop  = Button("Stop")
        ggStart .clicked ---> r.run
        ggStop  .clicked ---> r.stop
        ggStart.enabled = booted && r.isIdle
        ggStop.enabled  = r.isBusy
        (ggStart, ggStop)
      }

      val (ggStartLoadProc, ggStopLoadProc) = mkStartStop(rProcLoad)
      val (ggStartSaveProc, ggStopSaveProc) = mkStartStop(rProcSave)

//      rProcLoad.state.changed ---> PrintLn(Const("STATE = ") ++ rProcLoad.state.toStr)

      FlowPanel(ggBoot,
        Label("Load:"), ggStartLoadProc, ggStopLoadProc,
        Label("Save:"), ggStartSaveProc, ggStopSaveProc)
    }

    val gW = gW0

    implicit val undo: UndoManager[T] = UndoManager()

    //    import Workspace.Implicits._

    val (universe, view) = cursor.step { implicit tx =>
      implicit val u: Universe[T] = Universe.dummy[T]

      val pLoad = proc.Proc[T]()
      pLoad.graph() = gProcLoad
      val pSave = proc.Proc[T]()
      pSave.graph() = gProcSave

      val loc     = LArtifactLocation.newConst[T](rootURI)
      val art     = LArtifact[T](loc, LArtifact.Child(FILE_NAME_LOCAL /*"Einleitung_NC_T061.wav"*/))
      val spec    = AudioFileSpec(AudioFileType.AIFF, SampleFormat.Int24,
        numChannels = FILE_CHANNELS, sampleRate = 48000.0, numFrames = FILE_FRAMES /*7541957L*/)
      val cue     = proc.AudioCue.Obj[T](art, spec, 0L, 1.0)

      val w = proc.Widget[T]()
      w.graph() = gW

      val pLoadAttr = pLoad.attr
      val pSaveAttr = pSave.attr
      val wAttr     = w.attr
      pLoadAttr .put("in"       , cue  )
      pSaveAttr .put("out"      , art  )
      wAttr     .put("proc-load", pLoad)
      wAttr     .put("proc-save", pSave)

      val wH = tx.newHandle(w)
      implicit val ctx: expr.Context[T] = ExprContext(selfH = Some(wH))

      val _view = gW.expand[T]
      _view.initControl()

      u.auralSystem.reactNow { implicit tx => {
        case Running(_) => println("auralStarted")
        case Stopped    => println("auralStopped")
        case _          =>
      }}

      (u, _view)
    }

    Future.successful((universe, view))
  }
}
