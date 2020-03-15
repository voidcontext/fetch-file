package vdx.fetchfile
package examples

import cats.effect.{Blocker, IO, IOApp, ExitCode, Resource}
import cats.syntax.functor._
import java.net.URL
import java.io.ByteArrayOutputStream

object Main extends IOApp {
  def run(args: List[String]): IO[ExitCode] = {

    implicit val backend: Backend[IO] = HttpURLConnectionBackend[IO]
    implicit val clock: Clock = Clock.system

    Blocker[IO].use { blocker =>
      val out = new ByteArrayOutputStream()
      Downloader[IO](blocker, 1024 * 8, Progress.consoleProgress[IO])
        .fetch(
          new URL("http://localhost:8088/100MB.bin"),
          Resource.fromAutoCloseable(IO.delay(out)),

        )
        .as(ExitCode.Success)
    }
  }
}
