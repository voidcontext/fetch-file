# fetch-file [![Build Status](https://travis-ci.org/voidcontext/fetch-file.svg?branch=master)](https://travis-ci.org/voidcontext/fetch-file) [![Latest Version](https://img.shields.io/github/v/release/voidcontext/fetch-file)](https://github.com/voidcontext/fetch-file/releases)

Simple library to download a potentially large file over a potentially slow network, while the progress is printed to the stdout.

[![asciicast](https://asciinema.org/a/7kvI5otiStozvSx4UVgEEjCSA.svg)](https://asciinema.org/a/7kvI5otiStozvSx4UVgEEjCSA)

## Getting started

Currently only Scala 2.13.x is supported.

```scala
   libraryDependencies += "com.gaborpihaj" %% "fetchfile" % "0.4.0"
```

## Usage

This example downloads the content of the pre-generated test file into a file in `/tmp`.

```scala
import cats.effect._
import cats.instances.list._
import fs2.Pipe
import fs2.compression.Compression
import vdx.fetchfile.Pipes._
import vdx.fetchfile._

import scala.io.Source

import java.io.{File, FileOutputStream}
import java.net.URL

object Main extends IOApp {
  def run(args: List[String]): IO[ExitCode] = {

    implicit val clock: MonotonicClock = MonotonicClock.system()

    val shaSum = Source.fromFile("docker/static-files/100MB.bin.sha256").mkString.trim()
    val gzippedeShaSum = Source.fromFile("docker/static-files/100MB.bin.gz.sha256").mkString.trim()

    val outFile = new File("/tmp/100MB.bin")

    implicit val backend: HttpClient[IO] = HttpURLConnectionClient[IO](1024 * 16)

    val downloader = Downloader[IO](Progress.consoleProgress[IO])

    val gunzip: Pipe[IO, Byte, Byte] =
      Compression[IO].gunzip(32 * 1024).andThen(_.flatMap(_.content))

    for {
      ref <- Ref.of[IO, List[Byte]](List.empty)
      _ <- downloader.fetch(
        new URL("http://localhost:8088/100MB.bin.gz"),
        Resource.fromAutoCloseable(IO.delay(new FileOutputStream(outFile))),
        pipes = List(
          collectSHA256(ref), // Collect SHA before gunzip
          gunzip // Unzip compressed stream
        ),
        last = ensureSHA256(shaSum) // Ensure gunzipped SHA is correct
      )
      collecetedSha <- ref.get.map(_.map("%02x".format(_)).mkString)
    } yield {
      println(s"Collected SHA: $collecetedSha, expected gzipped SHA: $gzippedeShaSum")
      ExitCode.Success
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
