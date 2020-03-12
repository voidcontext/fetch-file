package vdx.fetchfile
package examples

import cats.effect._
import cats.syntax.functor._
import java.net.URL
import java.io.ByteArrayOutputStream

object Main extends IOApp {
  def run(args: List[String]): IO[ExitCode] = {

    implicit val backend: Backend[IO] = HttpURLConnectionBackend[IO]

    Blocker[IO].use { blocker =>
      val out = new ByteArrayOutputStream()
      Downloader[IO].fetch(
        new URL("http://localhost:8088/100MB.bin"),
        Resource.fromAutoCloseable(IO.delay(out)),
        blocker,
        1024,
        Progress.consoleProgress
      ).as(ExitCode.Success)
    }
  }
}
