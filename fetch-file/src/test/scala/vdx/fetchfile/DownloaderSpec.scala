package vdx.fetchfile

import cats.effect._
import cats.effect.unsafe.implicits.global
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.io.Source

import java.io.ByteArrayOutputStream
import java.net.URL
import java.security.MessageDigest

class DownloaderSpec extends AnyFlatSpec with Matchers with TestHttpClient {

  "fetch" should "use the given client to create the stream representing the content" in {

    implicit val client: HttpClient[IO] = makeClient(_.toString().getBytes())

    val out = new ByteArrayOutputStream()
    val downloader = Downloader[IO]()

    (
      for {
        _ <- downloader.fetch(
          new URL("http://example.com/test.file"),
          Resource.fromAutoCloseable(IO.delay(out))
        )
        content <- IO.delay(out.toString)
      } yield content
    ).unsafeRunSync() should be("http://example.com/test.file")
  }

  it should "download the file correctly via the HttpURLConnectionClient" in {
    implicit val client = HttpURLConnectionClient[IO](1024 * 8)

    val downloader = Downloader[IO]()
    val out = new ByteArrayOutputStream()

    val downloadedBytes = (
      for {
        _ <- downloader.fetch(
          new URL("http://localhost:8088/100MB.bin"),
          Resource.fromAutoCloseable(IO.delay(out))
        )
        content <- IO.delay(out.toByteArray())
      } yield content
    ).unsafeRunSync()

    downloadedBytes.length should be(1024 * 1024 * 100)
    val expectedShaSum = Source.fromFile("docker/static-files/100MB.bin.sha256").mkString.trim()

    val shaSum = MessageDigest
      .getInstance("SHA-256")
      .digest(downloadedBytes)
      .map("%02x".format(_))
      .mkString

    shaSum should be(expectedShaSum)
  }
}
