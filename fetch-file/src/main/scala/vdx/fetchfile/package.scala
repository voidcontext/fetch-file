package vdx

import fs2.Stream

import java.net.URL

package object fetchfile {

  /**
   * Sink represents a function that takes the content length and the content as a stream and consumes it
   */
  type Sink[F[_]] = (Downloader.ContentLength, Stream[F, Byte]) => F[Unit]

  /**
   * A HttpClient is a function from a URL and Sink function
   * the content length and the content itself as an fs2 Stream
   */
  type HttpClient[F[_]] = URL => Sink[F] => F[Unit]
}
