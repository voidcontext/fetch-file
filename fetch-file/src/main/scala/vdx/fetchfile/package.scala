package vdx

import cats.effect.Resource
import fs2.Stream

import java.net.URL

package object fetchfile {

  type HttpBackend[F[_]] = URL => Resource[F, (Int, Stream[F, Byte])]
}
