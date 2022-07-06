package vdx.fetchfile

import cats.effect._
import fs2.io.{readInputStream}
import vdx.fetchfile.Downloader.ContentLength

import java.io.InputStream
import java.net.{HttpURLConnection, URL}

/**
 * A HttpClient implementation based on java.net.HttpURLConnection.
 */
object HttpURLConnectionClient {

  /**
   * Creates a HttpClient instance that is using java.net.HttpUrlConnection to make a HTTP request.
   *
   * The evaluation of the blocking HTTP call and the fs2.Stream creation from the java InputStream
   * is using the given execution context wrapped in Blocker.
   */
  def apply[F[_]: Sync](chunkSize: Int): HttpClient[F] =
    url =>
      sink =>
        makeConnectionResource(url).map {
          case (contentLength, inputStream) =>
            ContentLength(contentLength.toLong) -> readInputStream(Sync[F].delay(inputStream), chunkSize)
        }.use(sink.tupled)

  private[this] def makeConnectionResource[F[_]: Sync](
    url: URL
  ): Resource[F, (Int, InputStream)] =
    Resource.make(makeConnection(url)) { case (_, inStream) => Sync[F].delay(inStream.close()) }

  @SuppressWarnings(Array("scalafix:DisableSyntax.asInstanceOf"))
  private[this] def makeConnection[F[_]: Sync](url: URL): F[(Int, InputStream)] =
    Sync[F].delay {
      val connection = url.openConnection().asInstanceOf[HttpURLConnection]
      connection.setRequestProperty(
        "User-Agent",
        "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11"
      )
      connection.connect()
      connection.getContentLength -> connection.getInputStream()
    }
}
