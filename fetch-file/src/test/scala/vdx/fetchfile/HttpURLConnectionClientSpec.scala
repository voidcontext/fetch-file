package vdx.fetchfile

import cats.data.WriterT
import cats.effect.Sync.catsWriterTSync
import cats.effect._
import cats.instances.vector._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.ExecutionContext

import java.io.InputStream
import java.net.URL

class HttpURLConnectionClientSpec extends AnyFlatSpec with Matchers {
  val contextShiftIO = IO.contextShift(ExecutionContext.global)

  "HttpURLConnectionClient" should "evaluate the connection on the given blocking context" in {
    type TestIO[A] = WriterT[IO, Vector[String], A]

    implicit val syncTestIO: Sync[TestIO] = catsWriterTSync[IO, Vector[String]]

    implicit val contextShiftTestIO: ContextShift[TestIO] = new ContextShift[TestIO] {

      override def shift: TestIO[Unit] = WriterT.liftF(contextShiftIO.shift)

      override def evalOn[A](ec: ExecutionContext)(fa: TestIO[A]): TestIO[A] =
        WriterT(
          for {
            l <- fa.written
            r <- contextShiftIO.evalOn(ec)(fa.value.map(v => logWhenConnectionHasMade(v) -> v))
          } yield (l ++ r._1, r._2)
        )

      private[this] def logWhenConnectionHasMade[A](a: A): Vector[String] =
        a match {
          case (_: Int, _:InputStream) => Vector(s"evalOn: ${Thread.currentThread().getName()}")
          case _ => Vector.empty
        }
    }

    Blocker[IO].use[IO, Unit] { blocker =>
      val client = HttpURLConnectionClient[TestIO](blocker, 1024 * 8)
      val url = new URL("http://localhost:8088/100MB.bin")

      val (logs, _) = client(url)((_, _) => WriterT.value(())).run.unsafeRunSync()

      logs should have(size(1))

      logs(0) should startWith("evalOn: cats-effect-blocker-")

      IO.unit
    }.unsafeRunSync()

  }
}
