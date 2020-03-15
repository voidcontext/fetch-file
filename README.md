# fetch-file

Simple library to download a potentially large file over a potentially slow network, while the progress is printed to the stdout.

[![asciicast](https://asciinema.org/a/7kvI5otiStozvSx4UVgEEjCSA.svg)](https://asciinema.org/a/7kvI5otiStozvSx4UVgEEjCSA)

## Usage

This example downloads the content of the pre-generated test file into a file in `/tmp`. 

```scala
import cats.effect.{Clock => _, _}
import cats.syntax.functor._
import vdx.fetchfile._

import java.net.URL
import java.io.{File, FileOutputStream}

object Main extends IOApp {
  def run(args: List[String]): IO[ExitCode] = {

    implicit val backend: Backend[IO] = HttpURLConnectionBackend[IO]
    implicit val clock: Clock = Clock.system

    val outFile = new File("/tmp/100MB.bin")

    Blocker[IO].use { blocker =>
      Downloader[IO](blocker, 1024 * 8, Progress.consoleProgress[IO])
        .fetch(
          new URL("http://localhost:8088/100MB.bin"),
          Resource.fromAutoCloseable(IO.delay(new FileOutputStream(outFile)))
        )
        .as(ExitCode.Success)
    }
  }
}
```

See fulle example in [`examples`](https://github.com/voidcontext/fetch-file/tree/master/examples).


## Development

Some of the tests are relying on a HTTP webserver so that files can be downloaded through HTTP. The repository contains a predefined container for this. The test file needs to be generated before building the container.

```bash
  $ sh docker/static-files/pre_build.sh
  $ docker-compose up
  $ sbt test
```
