package vdx.fetchfile

import cats.effect.Sync
import fs2._

import scala.concurrent.duration.{Duration, NANOSECONDS}

import java.util.concurrent.atomic.AtomicLong
import java.util.Locale

object Progress {
  def noop[F[_]]: Int => Pipe[F, Byte, Unit] = _ => _.map(_ => ())

  def consoleProgress[F[_]: Sync]: Int => Pipe[F, Byte, Unit] =
    custom[F] { (downloadedBytes, contentLength, _, downloadSpeed) =>
      println(
        s"\u001b[1A\u001b[100D\u001b[0KDownloaded ${bytesToString(downloadedBytes)} of" +
          s" ${bytesToString(contentLength.toLong)} | ${bytesToString(downloadSpeed)}/s"
      )
    }

  def custom[F[_]: Sync](f: (Long, Int, Duration, Long) => Unit): Int => Pipe[F, Byte, Unit] =
    contentLength => { s =>
      Stream.eval(Sync[F].delay(System.nanoTime()))
        .flatMap { startTime =>
          val downloadedBytes = new AtomicLong(0)

          s.chunks.map { chunk =>
            val down = downloadedBytes.addAndGet(chunk.size.toLong)
            val elapsedTime = Duration(System.nanoTime() - startTime, NANOSECONDS)
            val speed = (down * 1000) / Math.max(elapsedTime.toMillis, 1)

            f(down, contentLength, elapsedTime, speed)


          }
        }
    }

  // https://stackoverflow.com/questions/45885151/bytes-in-human-readable-format-with-idiomatic-scala
  def bytesToString(size: Long): String = {
    val TB = 1L << 40
    val GB = 1L << 30
    val MB = 1L << 20
    val KB = 1L << 10

    val (value, unit) = {
      if (size >= 2 * TB) {
        (size.asInstanceOf[Double] / TB, "TB")
      } else if (size >= 2 * GB) {
        (size.asInstanceOf[Double] / GB, "GB")
      } else if (size >= 2 * MB) {
        (size.asInstanceOf[Double] / MB, "MB")
      } else if (size >= 2 * KB) {
        (size.asInstanceOf[Double] / KB, "KB")
      } else {
        (size.asInstanceOf[Double], "B")
      }
    }
    "%.1f %s".formatLocal(Locale.US, value, unit)
  }
}
