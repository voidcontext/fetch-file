package vdx.fetchfile

import cats.effect.IO
import fs2.Stream
import vdx.fetchfile.Downloader.ContentLength

import java.net.URL

trait TestHttpClient {
  def makeClient(getContent: URL => Array[Byte]): HttpClient[IO] =
    url =>
      sink => {
        val bytes = getContent(url)

        sink(ContentLength(bytes.length.toLong), Stream.emits(bytes.toList).covary[IO])
      }
}
