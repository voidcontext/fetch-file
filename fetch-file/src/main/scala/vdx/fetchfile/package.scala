package vdx

import cats.effect.Resource
import fs2.Stream

import java.net.URL

package object fetchfile {
  /**
   * A HttpBackend is a function from URL to Resource that's a tuple of
   * the content length and the content itself as an fs2 Stream
   */
  type HttpBackend[F[_]] = URL => Resource[F, (Int, Stream[F, Byte])]
}
