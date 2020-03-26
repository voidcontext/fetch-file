package vdx.fetchfile

import cats.effect._
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
  def fetch(url: URL, out: Resource[F, OutputStream]): F[Unit]
}

object Downloader {
  final case class ContentLength(value: Long) extends AnyVal

  /**
   * Creates a `Downloader` that is going to run all blocking operations on the given ExecutionContext.
   */
  def apply[F[_]: Concurrent: ContextShift](
    ec: Blocker,
    progress: ContentLength => Pipe[F, Byte, Unit] = Progress.noop[F]
  )(implicit client: HttpClient[F]): Downloader[F] = new Downloader[F] {
    def fetch(url: URL, out: Resource[F, OutputStream]): F[Unit] =
      out.use { outStream =>
        client(url) { (contentLength, body) =>
          body.observe(progress(contentLength))
            .through(writeOutputStream[F](Concurrent[F].delay(outStream), ec))
            .compile
            .drain

        }
      }
  }
}
