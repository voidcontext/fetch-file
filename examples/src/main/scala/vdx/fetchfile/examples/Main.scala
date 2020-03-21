package vdx.fetchfile.examples

import cats.effect.{Clock => _, _}
import cats.syntax.functor._
import vdx.fetchfile._

import java.net.URL
import java.io.{File, FileOutputStream}

object Main extends IOApp {
  def run(args: List[String]): IO[ExitCode] = {

    implicit val clock: Clock = Clock.system

    val outFile = new File("/tmp/100MB.bin")

    Blocker[IO].use { blocker =>
      implicit val backend: HttpBackend[IO] = HttpURLConnectionBackend[IO](blocker, 1024 * 16)

      Downloader[IO](blocker, Progress.consoleProgress[IO])
        .fetch(
          new URL("http://localhost:8088/100MB.bin"),
          Resource.fromAutoCloseable(IO.delay(new FileOutputStream(outFile)))
        )
        .as(ExitCode.Success)
    }
  }
}
