package vdx.fetchfile

import cats.effect._
import fs2.Stream
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.io.ByteArrayOutputStream
import java.net.URL
import java.security.MessageDigest

import scala.io.Source
import vdx.fetchfile.Downloader.ContentLength


class DownloaderSpec extends AnyFlatSpec with Matchers {

  implicit val cs: ContextShift[IO] = IO.contextShift(scala.concurrent.ExecutionContext.global)

  "fetch" should "use the given client to create the stream representing the content" in {

    implicit val client: HttpClient[IO] =
      url => sink => {
        val bytes = url.toString.getBytes()

        sink(ContentLength(bytes.length.toLong), Stream.emits(bytes.toList).covary[IO])
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

  it should "download the file correctly via the HttpURLConnectionClient" in {

    val downloadedBytes = (Blocker[IO].use { blocker =>
      implicit val client = HttpURLConnectionClient[IO](blocker, 1024 * 8)
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
    val expectedShaSum = Source.fromFile("docker/static-files/100MB.bin.sha256").mkString.trim()

    val shaSum = MessageDigest.getInstance("SHA-256")
      .digest(downloadedBytes)
      .map("%02x".format(_))
      .mkString

    shaSum should be(expectedShaSum)
  }

  it should "be successful when the given shasum is correct" in {
    val expectedShaSum = Source.fromFile("docker/static-files/100MB.bin.sha256").mkString.trim()

    download(expectedShaSum).attempt.unsafeRunSync() should be(Right(()))
  }

  it should "be fail when the given shasum is incorrect" in {
    download("some-wrong-sha-sum").attempt.unsafeRunSync() should be(a[Left[_, _]])
  }

  def download(shaSum: String): IO[Unit] =
    Blocker[IO].use { blocker =>
      implicit val client = HttpURLConnectionClient[IO](blocker, 1024 * 8)
      val downloader = Downloader[IO](blocker)
      val out = new ByteArrayOutputStream()
      for {
        _ <- downloader.fetch(
          new URL("http://localhost:8088/100MB.bin"),
          Resource.fromAutoCloseable(IO.delay(out)),
          sha256Sum = Option(shaSum)
        )
        _ <- IO.delay(out.toByteArray())
      } yield ()
    }
}

