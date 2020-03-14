package vdx.fetchfile

/**
 * Representation of a clock.
 * In case of cats.effect.Clock[F], the evaluation of IO makes the stream slightly slower.
 * To avoid this performance impact, but still keep the progress tracker testable we introduce our own clock interface.
 */
trait Clock {
  def nanoTime(): Long
}

object Clock {
  /**
   * Simple clock implementation based on System.nanoTime()
   */
  def system(): Clock = new Clock {
    def nanoTime(): Long = System.nanoTime()
  }
}

