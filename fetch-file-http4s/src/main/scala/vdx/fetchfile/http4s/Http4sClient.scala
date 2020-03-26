package vdx.fetchfile
package http4s

import cats.MonadError
import org.http4s.client.Client
import org.http4s.headers.`Content-Length`
import org.http4s.{Headers, Status}
import vdx.fetchfile.Downloader.ContentLength


object Http4sClient {
  def apply[F[_]: MonadError[*[_], Throwable]](client: Client[F]): HttpClient[F] =
    url => sink =>
      client.get(url.toString()) {
        case Status.Successful(r) => sink(ContentLength(contentLength(r.headers)), r.body)
        case _ => MonadError[F, Throwable].raiseError(new HttpClientException("HTTP call failed."))
      }

  private[this] def contentLength(headers: Headers): Long =
    headers.get(`Content-Length`).map(_.length).getOrElse(0L)
}
