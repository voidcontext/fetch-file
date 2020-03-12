package vdx.fetchfile

import cats.effect._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.net.URL
import scala.io.Source
import java.security.MessageDigest

class DownloaderSpec extends AnyFlatSpec with Matchers {

  implicit val cs: ContextShift[IO] = IO.contextShift(scala.concurrent.ExecutionContext.global)

  "fetch" should "use the given backend to create the input stream" in {

    implicit val backend: Backend[IO] =
      (url: URL) =>
        Resource.make {
          IO.delay((new ByteArrayInputStream(url.toString.getBytes()), url.toString().getBytes().length))
        } {
          case (s, _) => IO.delay(s.close())
        }

    val downloader = Downloader[IO]


    (Blocker[IO].use { blocker =>
      val out = new ByteArrayOutputStream()
      for {
        _        <- downloader.fetch(
          new URL("http://example.com/test.file"),
          Resource.fromAutoCloseable(IO.delay(out)),
          blocker,
          1,
        )
        content  <- IO.delay(out.toString)
      } yield content
    }).unsafeRunSync() should be("http://example.com/test.file")
  }

  it should "download the file correctly with the provided backend" in {
    val downloader = Downloader[IO]

    val downloadedBytes = (Blocker[IO].use { blocker =>
      implicit val backend = HttpURLConnectionBackend[IO]
      val out = new ByteArrayOutputStream()
      for {
        _ <- downloader.fetch(
          new URL("http://localhost:8088/100MB.bin"),
          Resource.fromAutoCloseable(IO.delay(out)),
          blocker,
          1024 * 64,
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

