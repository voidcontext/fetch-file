
package vdx.fetchfile

import cats.effect._
import fs2.io.{readInputStream}

import java.net.{HttpURLConnection, URL}
import java.io.InputStream
import vdx.fetchfile.Downloader.ContentLength


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
  def apply[F[_]: Sync: ContextShift](blocker: Blocker, chunkSize: Int): HttpClient[F] =
    url => sink =>
     makeConnectionResource(url, blocker)
       .map {
         case (contentLength, inputStream) =>
           ContentLength(contentLength.toLong) -> readInputStream(Sync[F].delay(inputStream), chunkSize, blocker)
       }
       .use(sink.tupled)


  private[this] def makeConnectionResource[F[_]: Sync: ContextShift](
    url: URL,
    blocker: Blocker
  ): Resource[F, (Int, InputStream)] =
    Resource.make(makeConnection(url, blocker)) { case (_, inStream) => Sync[F].delay(inStream.close())}

  private[this] def makeConnection[F[_]: Sync: ContextShift](url: URL, blocker: Blocker): F[(Int, InputStream)] =
    ContextShift[F].blockOn(blocker)(
      Sync[F].delay {
        val connection = url.openConnection().asInstanceOf[HttpURLConnection]
        connection.setRequestProperty(
          "User-Agent",
          "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11"
        )
        connection.connect()
        connection.getContentLength -> connection.getInputStream()
      }
    )
}
