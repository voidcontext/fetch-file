package vdx.fetchfile

import cats.effect._
import fs2.Pipe
import fs2.io.writeOutputStream

import java.io.OutputStream
import java.net.URL

trait Downloader[F[_]] {
  def fetch(url: URL, out: Resource[F, OutputStream]): F[Unit]
}

object Downloader {

  def apply[F[_]: Concurrent: ContextShift](
    ec: Blocker,
    progress: Int => Pipe[F, Byte, Unit] = Progress.noop[F]
  )(implicit backend: HttpBackend[F]): Downloader[F] = new Downloader[F] {
    def fetch(url: URL, out: Resource[F, OutputStream]): F[Unit] =
      (
        for {
          outStream <- out
          lengthAndInputStream <- backend(url)
        } yield (outStream, lengthAndInputStream)
      ).use {
        case (outStream, (contentLength, inputStream)) =>
           inputStream.observe(progress(contentLength))
            .through(writeOutputStream[F](Concurrent[F].delay(outStream), ec))
            .compile
            .drain
      }
  }
}
