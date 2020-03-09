package vdx

import cats.effect.{Resource, Sync}

import java.io.InputStream
import java.net.{HttpURLConnection, URL}

package object fetchfile {

  type Backend[F[_]] = URL => Resource[F, InputStream]

  object Backend {
    implicit def httpUrlConnectionBackend[F[_]: Sync]: Backend[F] =
      url => Resource.fromAutoCloseable {
        Sync[F].delay {
          val connection = url.openConnection().asInstanceOf[HttpURLConnection]
          connection.setRequestProperty(
            "User-Agent",
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11"
          )
          connection.connect()
          connection.getInputStream()
        }
      }

  }
}
