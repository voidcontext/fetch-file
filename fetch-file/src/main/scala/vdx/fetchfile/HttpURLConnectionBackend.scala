
package vdx.fetchfile

import cats.effect._
import fs2.io.{readInputStream}
import fs2.Stream

import java.net.HttpURLConnection

object HttpURLConnectionBackend {
  def apply[F[_]: Sync: ContextShift: Bracket[*[_], Throwable]](blocker: Blocker, chunkSize: Int): HttpBackend[F] =
    url =>
      Resource.make {
        Sync[F].delay {
          val connection = url.openConnection().asInstanceOf[HttpURLConnection]
          connection.setRequestProperty(
            "User-Agent",
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11"
          )
          connection.connect()
          connection.getContentLength -> connection.getInputStream
        }
      } {
        case (_, inStream) => Sync[F].delay(inStream.close())
      } use[F, (Int, Stream[F, Byte])] {
        case (contentLength, inStream) => Sync[F].pure(contentLength -> readInputStream(Sync[F].delay(inStream), chunkSize, blocker))
      }
}
