# fetch-file [![Build Status](https://travis-ci.org/voidcontext/fetch-file.svg?branch=master)](https://travis-ci.org/voidcontext/fetch-file) [![Latest Version](https://img.shields.io/github/v/release/voidcontext/fetch-file)](https://github.com/voidcontext/fetch-file/releases)

Simple library to download a potentially large file over a potentially slow network, while the progress is printed to the stdout.

[![asciicast](https://asciinema.org/a/7kvI5otiStozvSx4UVgEEjCSA.svg)](https://asciinema.org/a/7kvI5otiStozvSx4UVgEEjCSA)

## Getting started

Currently only Scala 2.13.x is supported.

```scala
   libraryDependencies += "com.gaborpihaj" %% "fetchfile" % "0.2.0"
```

## Usage

This example downloads the content of the pre-generated test file into a file in `/tmp`. 

```scala
import cats.effect._
import cats.syntax.functor._
import vdx.fetchfile._

import java.net.URL
import java.io.{File, FileOutputStream}

object Main extends IOApp {
  def run(args: List[String]): IO[ExitCode] = {

    implicit val clock: MonotonicClock = MonotonicClock.system

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
```

See fulle example in [`examples`](https://github.com/voidcontext/fetch-file/tree/master/examples).


## Development

Some of the tests are relying on a HTTP webserver so that files can be downloaded through HTTP. The repository contains a predefined container for this. The test file needs to be generated before building the container.

```bash
  $ sh docker/static-files/pre_build.sh
  $ docker-compose up
  $ sbt test
```
