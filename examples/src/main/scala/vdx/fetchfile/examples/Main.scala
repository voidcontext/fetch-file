package vdx.fetchfile.examples

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
