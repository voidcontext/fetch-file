package vdx.fetchfile

import cats.effect.{Blocker, Concurrent, Resource, Sync}
import cats.syntax.semigroupal._
import fs2.Pipe
import fs2.io.{readInputStream, writeOutputStream}

import java.io.OutputStream
import java.net.URL
import cats.effect.ContextShift

trait Downloader[F[_]] {
  def fetch(
    url: URL,
    out: Resource[F, OutputStream],
    ec: Blocker,
    chunkSize: Int,
    progress: Int => Pipe[F, Byte, Unit] = Progress.noop[F]
  )(implicit backend: Backend[F]): F[Unit]
}

object Downloader {

  def apply[F[_]: Concurrent: ContextShift]: Downloader[F] = new Downloader[F] {
    def fetch(
      url: URL,
      out: Resource[F, OutputStream],
      ec: Blocker,
      chunkSize: Int,
      progress: Int => Pipe[F, Byte, Unit] = Progress.noop
    )(implicit backend: Backend[F]): F[Unit] = {
      backend(url).product(out).use {
        case ((inStream, contentLength), outStream) =>
          readInputStream[F](Sync[F].delay(inStream), chunkSize, ec)
            .observe(progress(contentLength))
            .through(writeOutputStream[F](Sync[F].delay(outStream), ec))
            .compile
            .drain
      }
    }
  }
}
