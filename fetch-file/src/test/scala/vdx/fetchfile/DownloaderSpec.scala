package vdx.fetchfile

import cats.effect._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.io.Source

import java.io.ByteArrayOutputStream
import java.net.URL
import java.security.MessageDigest

class DownloaderSpec extends AnyFlatSpec with Matchers with TestHttpClient {

  implicit val cs: ContextShift[IO] = IO.contextShift(scala.concurrent.ExecutionContext.global)

  "fetch" should "use the given client to create the stream representing the content" in {

    implicit val client: HttpClient[IO] = makeClient(_.toString().getBytes())

    (Blocker[IO].use { blocker =>
      val out = new ByteArrayOutputStream()
      val downloader = Downloader[IO](blocker)

      for {
        _ <- downloader.fetch(
          new URL("http://example.com/test.file"),
          Resource.fromAutoCloseable(IO.delay(out))
        )
        content <- IO.delay(out.toString)
      } yield content
    }).unsafeRunSync() should be("http://example.com/test.file")
  }

  it should "download the file correctly via the HttpURLConnectionClient" in {

    val downloadedBytes = (Blocker[IO].use { blocker =>
      implicit val client = HttpURLConnectionClient[IO](blocker, 1024 * 8)
      val downloader = Downloader[IO](blocker)
      val out = new ByteArrayOutputStream()
      for {
        _ <- downloader.fetch(
          new URL("http://localhost:8088/100MB.bin"),
          Resource.fromAutoCloseable(IO.delay(out))
        )
        content <- IO.delay(out.toByteArray())
      } yield content
    }).unsafeRunSync()

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
