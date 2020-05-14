package vdx.fetchfile

import cats.effect.ContextShift
import cats.effect.IO
import cats.effect.concurrent.Ref
import cats.instances.list._
import fs2.Stream
import org.scalacheck.Arbitrary
import org.scalacheck.Gen
import org.scalacheck.Prop
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.Checkers
import org.typelevel.claimant.Claim

import scala.concurrent.ExecutionContext

import java.security.MessageDigest

@SuppressWarnings(Array("scalafix:DisableSyntax.=="))
class PipesSpec extends AnyWordSpec with Matchers with TestHttpClient with Checkers {
  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  val testText = "This is a test text"
  val expectedShaSum = "c62968ebcd6b7c706a8ac082db862682fa8be407106cb7eaa050c713a4e969d7"

  val byteArray: Gen[Array[Byte]] = Gen.listOf(Arbitrary.arbByte.arbitrary).map(_.toArray)

  "ensureSHA256" should {
    "do nothing when the given shaSum is correct" in {
      check(
        Prop.forAllNoShrink(byteArray) { bytes =>
          Stream
            .emits(bytes)
            .covary[IO]
            .through(Pipes.ensureSHA256(calcHash("SHA-256", bytes)))
            .compile
            .drain
            .attempt
            .unsafeRunSync()
            .isRight
        }
      )
    }

    "fail the stream when the given shasum is not correct" in {
      check(
        Prop.forAllNoShrink(byteArray) { bytes =>
          Stream
            .emits(bytes)
            .covary[IO]
            .through(Pipes.ensureSHA256("some-wrong-sha"))
            .compile
            .drain
            .attempt
            .unsafeRunSync()
            .isLeft
        }
      )
    }
  }

  "collectSHA256" should {
    "collect the shasum without modifying the stream" in {
      check(
        Prop.forAllNoShrink(byteArray) { bytes =>
          (for {
            ref       <- Ref.of[IO, List[Byte]](List.empty)
            result    <- Stream.emits(bytes).covary[IO].through(Pipes.collectSHA256(ref)).compile.toList
            collected <- ref.get
          } yield {
            Claim(
              result == bytes.toList &&
                collected.map("%02x".format(_)).mkString == calcHash("SHA-256", bytes)
            )
          }).unsafeRunSync()
        }
      )
    }
  }

  def calcHash(algo: String, bytes: Array[Byte]): String =
    MessageDigest
      .getInstance(algo)
      .digest(bytes)
      .map("%02x".format(_))
      .mkString
}
