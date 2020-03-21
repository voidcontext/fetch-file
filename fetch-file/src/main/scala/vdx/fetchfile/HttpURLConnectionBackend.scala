
package vdx.fetchfile

import cats.effect._
import fs2.io.{readInputStream}

import java.net.{HttpURLConnection, URL}
import java.io.InputStream


object HttpURLConnectionBackend {
  def apply[F[_]: Sync: ContextShift: Bracket[*[_], Throwable]](blocker: Blocker, chunkSize: Int): HttpBackend[F] =
    url =>
  for {
    connResource              <- makeConnectionResource(url)
    (contentLength, inStream) = connResource
    fs2Stream                 <- Resource.liftF(Sync[F].delay(readInputStream(Sync[F].delay(inStream), chunkSize, blocker)))
  } yield contentLength -> fs2Stream


  private[this] def makeConnectionResource[F[_]: Sync](url: URL): Resource[F, (Int, InputStream)] =
    Resource.make(makeConnection(url)) { case (_, inStream) => Sync[F].delay(inStream.close())}

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
