package vdx

import cats.effect.Resource

import java.net.URL
import java.io.InputStream

package object fetchfile {

  type Backend[F[_]] = URL => Resource[F, InputStream]
}
