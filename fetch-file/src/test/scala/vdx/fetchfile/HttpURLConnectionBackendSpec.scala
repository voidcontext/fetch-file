package vdx.fetchfile

import cats.effect._
import cats.effect.Sync.catsWriterTSync
import cats.instances.vector._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import cats.data.WriterT
import java.net.URL
import scala.concurrent.ExecutionContext

class HttpURLConnectionBackendSpec extends AnyFlatSpec with Matchers {
  //  val contextShiftIO = ContextShift[IO]

  "HttpURLConnectionBackend" should "evaluate the connection on the given blocking context" in {
    type TestIO[A] = WriterT[IO, Vector[String], A]

    implicit val syncTestIO: Sync[TestIO] = catsWriterTSync[IO, Vector[String]]

    implicit val contextShiftTestIO: ContextShift[TestIO] = new ContextShift[TestIO] {

      override def shift: TestIO[Unit] = WriterT.tell(Vector("ContextShift.shift"))

      override def evalOn[A](ec: ExecutionContext)(fa: TestIO[A]): TestIO[A] =
        WriterT.tell[IO, Vector[String]](Vector(s"ContextShift.evalOn")).flatMap[A](_ => fa)
    }

    Blocker[IO].use[IO, Unit] { blocker =>
      val backend = HttpURLConnectionBackend[TestIO](blocker, 1024 * 8)

      val (logs, _) = backend(new URL("http://localhost:8088/100MB.bin")).use[TestIO, Unit](_ => WriterT.value(())).run.unsafeRunSync()

      logs should be(Vector("ContextShift.evalOn"))

      IO.unit
    }.unsafeRunSync()

  }
}
