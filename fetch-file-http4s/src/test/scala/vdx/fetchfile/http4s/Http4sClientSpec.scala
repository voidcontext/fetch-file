package vdx.fetchfile
package http4s

import cats.effect._
import cats.syntax.semigroupal._
import org.http4s.client.blaze.BlazeClientBuilder
import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.ExecutionContext.global
import scala.io.Source

import java.io.ByteArrayOutputStream
import java.net.URL
import java.security.MessageDigest

class Http4sClientSpec extends AnyFlatSpec with Matchers with EitherValues {

  implicit val cs: ContextShift[IO] = IO.contextShift(global)

  "Http4sClient" should "download the file correctly via the http4s client" in {

    val downloadedBytes = (Blocker[IO]
      .product(BlazeClientBuilder[IO](global).resource)
      .use {
        case (blocker, client) =>
          implicit val backend = Http4sClient[IO](client)
          val downloader = Downloader[IO](blocker)
          val out = new ByteArrayOutputStream()
          for {
            _ <- downloader.fetch(
              new URL("http://localhost:8088/100MB.bin"),
              Resource.fromAutoCloseable(IO.delay(out))
            )
            content <- IO.delay(out.toByteArray())
          } yield content
      })
      .unsafeRunSync()

    downloadedBytes.length should be(1024 * 1024 * 100)
    val expectedShaSum = Source.fromFile("docker/static-files/100MB.bin.sha256").mkString.trim()

    val shaSum = MessageDigest
      .getInstance("SHA-256")
      .digest(downloadedBytes)
      .map("%02x".format(_))
      .mkString

    shaSum should be(expectedShaSum)
  }

  it should "raise an error when the HTTP request is not successful" in {
    val attempt = (Blocker[IO]
      .product(BlazeClientBuilder[IO](global).resource)
      .use {
        case (blocker, client) =>
          implicit val backend = Http4sClient[IO](client)

          val downloader = Downloader[IO](blocker)
          val out = new ByteArrayOutputStream()

          downloader.fetch(
            new URL("http://localhost:8088/nonexsitent"),
            Resource.fromAutoCloseable(IO.delay(out))
          )
      })
      .attempt
      .unsafeRunSync()

    attempt.left.value should be(an[HttpClientException])

  }

}
