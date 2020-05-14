package vdx.fetchfile

import cats.Applicative
import cats.effect.Concurrent
import cats.effect.concurrent.Ref
import cats.instances.string._
import cats.kernel.Semigroup
import cats.syntax.eq._
import cats.syntax.functor._
import cats.syntax.semigroup._
import fs2.{Pipe, RaiseThrowable, Stream}

object Pipes {

  /**
   * An fs2 Pipe that collects the SHA-256 sum of the stream as a
   * series of bytes. The result container can be anything that has an
   * Applicative and a Semigroup instance.
   *
   * @tparam F      The Concurrent effect of the Stream
   * @tparam G      The type of the contianer that holds the collected bytes
   * @param  shaRef The ref that holds the bytes of the SHA sum
   * @param  S      The implicit Semigroup instance of G[Byte]
   */
  def collectSHA256[F[_]: Concurrent, G[_]: Applicative](
    shaRef: Ref[F, G[Byte]]
  )(implicit S: Semigroup[G[Byte]]): Pipe[F, Byte, Byte] =
    _.observe(_.through(fs2.hash.sha256).evalTap(byte => shaRef.update(_ |+| Applicative[G].pure(byte))).void)

  /**
   * An fs2 Pipe that compares the stream's SHA-256 sum to the given `
   * @tparam F
   * @param  expectedSHA
   * @param  C
   */
  def ensureSHA256[F[_]: RaiseThrowable](expectedSHA: String)(implicit C: Stream.Compiler[F, F]): Pipe[F, Byte, Unit] =
    stream =>
      Stream
        .eval(
          stream.through(fs2.hash.sha256).compile.toVector
        )
        .map(_.map("%02x".format(_)).mkString)
        .flatMap { hash =>
          if (hash === expectedSHA.toLowerCase()) Stream.emit(()).covary[F]
          else Stream.raiseError(new Exception(s"Sha256 sum doesn't match (expected: $expectedSHA, got: $hash)"))
        }
}
