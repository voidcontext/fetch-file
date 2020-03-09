package vdx.fetchfile

import cats.effect._
import fs2._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.io.{ByteArrayInputStream}
import java.net.URL
import java.io.ByteArrayOutputStream

class DownloaderSpec extends AnyFlatSpec with Matchers {
  implicit val cs: ContextShift[IO] = IO.contextShift(scala.concurrent.ExecutionContext.global)

  "fetch" should "use the given backend to create the input stream" in {

    implicit val backend: Backend[IO] =
      (url: URL) => Resource.make(IO.delay(new ByteArrayInputStream(url.toString.getBytes())))(s => IO.delay(s.close()))

    val downloader = Downloader[IO]


    (Blocker[IO].use { blocker =>
      val out = new ByteArrayOutputStream()
      for {
        _        <- downloader.fetch(
          new URL("http://example.com/test.file"),
          Resource.fromAutoCloseable(IO.delay(out)),
          blocker,
          1,
          (s: Stream[IO, Byte]) => s.map(_ => ())
        )
        content  <- IO.delay(out.toString)
      } yield content
    }).unsafeRunSync() should be("http://example.com/test.file")
  }
}

