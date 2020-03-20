package vdx

import fs2.Stream

import java.net.URL

package object fetchfile {
  type HttpBackend[F[_]] = URL => F[(Int, Stream[F, Byte])] // What about the content length?
}
