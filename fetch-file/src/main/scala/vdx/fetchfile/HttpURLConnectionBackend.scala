package vdx.fetchfile

import cats.effect.{Resource, Sync}
import java.net.HttpURLConnection

object HttpURLConnectionBackend {
  def apply[F[_]: Sync]: Backend[F] =
    url => Resource.make {
      Sync[F].delay {
        val connection = url.openConnection().asInstanceOf[HttpURLConnection]
        connection.setRequestProperty(
          "User-Agent",
          "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11"
        )
        connection.connect()
        connection.getInputStream -> connection.getContentLength
      }
    } {
      case (inStream, _) => Sync[F].delay(inStream.close())
    }
}
