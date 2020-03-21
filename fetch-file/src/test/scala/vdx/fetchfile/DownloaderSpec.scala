package vdx.fetchfile

import cats.effect._
import fs2.Stream
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.io.ByteArrayOutputStream
import java.net.URL
import java.security.MessageDigest

import scala.io.Source


class DownloaderSpec extends AnyFlatSpec with Matchers {

  implicit val cs: ContextShift[IO] = IO.contextShift(scala.concurrent.ExecutionContext.global)

  "fetch" should "use the given backend to create the input stream" in {

    implicit val backend: HttpBackend[IO] =
      (url: URL) => {
        val bytes = url.toString.getBytes()

        Resource.pure[IO, (Int, Stream[IO, Byte])](bytes.length -> Stream.emits(bytes.toList).covary[IO])
      }

    (Blocker[IO].use { blocker =>
      val out = new ByteArrayOutputStream()
      val downloader = Downloader[IO](blocker)

      for {
        _        <- downloader.fetch(
          new URL("http://example.com/test.file"),
          Resource.fromAutoCloseable(IO.delay(out)),
        )
        content  <- IO.delay(out.toString)
      } yield content
    }).unsafeRunSync() should be("http://example.com/test.file")
  }

  it should "download the file correctly through the HttpURLConnectionBackend" in {

    val downloadedBytes = (Blocker[IO].use { blocker =>
      implicit val backend = HttpURLConnectionBackend[IO](blocker, 1024 * 8)
      val downloader = Downloader[IO](blocker)
      val out = new ByteArrayOutputStream()
      for {
        _ <- downloader.fetch(
          new URL("http://localhost:8088/100MB.bin"),
          Resource.fromAutoCloseable(IO.delay(out)),
        )
        content <- IO.delay(out.toByteArray())
      } yield content
    }).unsafeRunSync()

    downloadedBytes.length should be(1024 * 1024 * 100)
    val expectedShaSum = Source.fromFile("docker/static-files/100MB.bin.shasum").mkString.trim()

    val shaSum = MessageDigest.getInstance("SHA-1")
      .digest(downloadedBytes)
      .map("%02x".format(_))
      .mkString

    shaSum should be(expectedShaSum)
  }
}

