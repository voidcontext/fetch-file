package vdx.fetchfile

import cats.effect.IO
import fs2.Stream
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatestplus.scalacheck.Checkers

import cats.effect.ContextShift
import org.scalacheck.{Gen, Prop}
import scala.concurrent.ExecutionContext
import vdx.fetchfile.Downloader.ContentLength

class ProgressSpec extends AnyFlatSpec with Checkers {

  val byte = Gen.choose(1, 255).map(_.toByte)
  val bytes = Gen.containerOf[List, Byte](byte)

  "custom" should "create a custom progress tracker that calls the given function after each chunk" in {
    check(
      Prop.forAll(bytes) { bs =>

        implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
        var elapsedTime: Long = 0
        implicit val clock: MonotonicClock = new MonotonicClock {
          def nanoTime(): Long = {
            elapsedTime += 1000000
            elapsedTime
          }

        }

        var chunksOK = true

        val progress = (downloadedBytes: Long, contentLength: ContentLength, elapsedTime: Long, downloadSpeed: Long) => {
          chunksOK = chunksOK &&
            contentLength.value == bs.length.toLong &&
            elapsedTime == downloadedBytes &&
            downloadSpeed == (downloadedBytes * 1000) / elapsedTime
        }

        val pipe = Progress.custom[IO](progress, Some(1))

        Stream.emits[IO, Byte](bs)
          .observe(pipe(ContentLength(bs.length.toLong)))
          .compile
          .drain
          .unsafeRunSync()

        chunksOK
      }
    )
  }
}
