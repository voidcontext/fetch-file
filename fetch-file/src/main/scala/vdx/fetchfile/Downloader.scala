package vdx.fetchfile

import cats.effect._
import cats.syntax.eq._
import cats.instances.string._
import cats.syntax.functor._
import fs2.{Pipe, Stream}
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
  def fetch(url: URL, out: Resource[F, OutputStream], sha256Sum: Option[String] = None): F[Unit]
}

object Downloader {
  final case class ContentLength(value: Long) extends AnyVal

  /**
   * Creates a `Downloader` that is going to run all blocking operations on the given ExecutionContext.
   */
  def apply[F[_]: Concurrent: ContextShift](
    ec: Blocker,
    progress: ContentLength => Pipe[F, Byte, Unit] = Progress.noop[F],
  )(implicit client: HttpClient[F]): Downloader[F] = new Downloader[F] {
    def fetch(url: URL, out: Resource[F, OutputStream], sha256Sum: Option[String] = None): F[Unit] =
      out.use { outStream =>
        client(url) { (contentLength, body) =>
          body.observe(progress(contentLength))
            // The writeOutputStream pipe returns Unit so it is safe to write the final output using observe
            .observe(writeOutputStream[F](Concurrent[F].delay(outStream), ec))
            .through(maybeCompareSHA(sha256Sum))
            .compile
            .drain
        }
      }

    def maybeCompareSHA(sha256: Option[String]): Pipe[F, Byte, Unit] =
      stream =>
        sha256.map[Stream[F, Unit]] { expectedSHA =>
          Stream.eval(
            // We'll compute the sh256 hash of the downloaded file
            stream.through(fs2.hash.sha256)
              .compile
              .toVector
          ).flatMap { hashBytes =>
            val hash = hashBytes.map("%02x".format(_)).mkString
            if (hash === expectedSHA.toLowerCase()) Stream.emit(()).covary[F]
            else Stream.raiseError(new Exception(s"Sha256 sum doesn't match (expected: $expectedSHA, got: $hash)"))
          }
        }
        .getOrElse(stream.void)
  }
}
