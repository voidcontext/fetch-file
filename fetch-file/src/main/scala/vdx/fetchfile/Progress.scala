package vdx.fetchfile

import cats.effect.Sync
import fs2._
import vdx.fetchfile.Downloader.ContentLength

import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

object Progress {

  /**
   * A progress tracker that does nothing
   */
  def noop[F[_]]: ContentLength => Pipe[F, Byte, Unit] = _ => _.map(_ => ())

  /**
   * A console based progress tracker which needs control character support as it refreshes the status in place.
   */
  def consoleProgress[F[_]: Sync](implicit clock: MonotonicClock): ContentLength => Pipe[F, Byte, Unit] =
    custom[F] { (downloadedBytes, contentLength, elapsedTime, downloadSpeed) =>
      println(
        s"\u001b[1A\u001b[100D\u001b[0KDownloaded ${bytesToString(downloadedBytes)} of " +
          s"${bytesToString(contentLength.value)} | " +
          s"${bytesToString(downloadSpeed)}/s | " +
          s"Time: ${millisToString(elapsedTime)}"
      )
    }

  /**
   * Helps building custom progress trackers
   */
  def custom[F[_]: Sync](
    f: (Long, ContentLength, Long, Long) => Unit,
    chunkLimit: Option[Int] = None
  )(implicit clock: MonotonicClock): ContentLength => Pipe[F, Byte, Unit] =
    contentLength => {
      s =>
        Stream.eval(Sync[F].delay(clock.nanoTime())).flatMap { startTime =>
          val downloadedBytes = new AtomicLong(0)

          chunkLimit
            .map(s.chunkLimit(_))
            .getOrElse(s.chunks)
            .map { chunk =>
              val down = downloadedBytes.addAndGet(chunk.size.toLong)
              val elapsedTime = (clock.nanoTime() - startTime) / 1000000
              val speed = (down * 1000) / Math.max(elapsedTime, 1)

              f(down, contentLength, elapsedTime, speed)
            }
        }
    }

  def millisToString(millis: Long): String =
    if (millis > 1000) s"${millis.toFloat / 1000} s"
    else s"${millis} ms"

  // https://stackoverflow.com/questions/45885151/bytes-in-human-readable-format-with-idiomatic-scala
  def bytesToString(size: Long): String = {
    val TB = 1L << 40
    val GB = 1L << 30
    val MB = 1L << 20
    val KB = 1L << 10

    val (value, unit) = {
      if (size >= 2 * TB) {
        (size.toDouble / TB, "TB")
      } else if (size >= 2 * GB) {
        (size.toDouble / GB, "GB")
      } else if (size >= 2 * MB) {
        (size.toDouble / MB, "MB")
      } else if (size >= 2 * KB) {
        (size.toDouble / KB, "KB")
      } else {
        (size.toDouble, "B")
      }
    }
    "%.1f %s".formatLocal(Locale.US, value, unit)
  }
}
