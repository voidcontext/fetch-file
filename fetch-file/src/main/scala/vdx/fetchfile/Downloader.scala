package vdx.fetchfile

import cats.Foldable
import cats.effect._
import cats.instances.list._
import cats.syntax.foldable._
import cats.syntax.functor._
import fs2.Pipe
import fs2.io.writeOutputStream

import java.io.OutputStream
import java.net.URL

/**
 * Given a url and an OutputStream the `fetch` function populates the stream with the content of the HTTP resource.
 * Considering the intended functionality of this interface, the outputsream is usually a FileOutputStream
 *
 * Example:
 * {{{
 *   fetch(
 *     new URL("http://example.com/path/to/large.file"),
 *     Resource.fromAutoClosable(new FileOutputStream(new File("/path/to/destination.file")))
 *   )
 * }}}
 */
trait Downloader[F[_]] {

  /**
   * Fetches the given URL and popuplates the given output stream.
   */
  def fetch(url: URL, out: Resource[F, OutputStream]): F[Unit] =
    fetch[List](url, out, List.empty, streamUnit)

  def fetch[G[_]: Foldable](url: URL, out: Resource[F, OutputStream], pipes: G[Pipe[F, Byte, Byte]]): F[Unit] =
    fetch[G](url, out, pipes, streamUnit)

  def fetch(url: URL, out: Resource[F, OutputStream], last: Pipe[F, Byte, Unit]): F[Unit] =
    fetch[List](url, out, List.empty, last)

  def fetch[G[_]: Foldable](
    url: URL,
    out: Resource[F, OutputStream],
    pipes: G[Pipe[F, Byte, Byte]],
    last: Pipe[F, Byte, Unit]
  ): F[Unit]

  private def streamUnit[A]: Pipe[F, A, Unit] =
    _.void
}

object Downloader {
  final case class ContentLength(value: Long) extends AnyVal

  /**
   * Creates a `Downloader` that is going to run all blocking operations on the given ExecutionContext.
   */
  def apply[F[_]: Async](
    progress: ContentLength => Pipe[F, Byte, Nothing] = Progress.noop[F]
  )(implicit client: HttpClient[F]): Downloader[F] = new Downloader[F] {
    def fetch[G[_]: Foldable](
      url: URL,
      out: Resource[F, OutputStream],
      pipes: G[Pipe[F, Byte, Byte]],
      last: Pipe[F, Byte, Unit]
    ): F[Unit] =
      out.use { outStream =>
        client(url) { (contentLength, body) =>
          pipes
            .foldLeft(body.observe(progress(contentLength)))(_ through _)
            .observe(writeOutputStream[F](Async[F].delay(outStream)))
            .through(last)
            .compile
            .drain
        }
      }
  }
}
