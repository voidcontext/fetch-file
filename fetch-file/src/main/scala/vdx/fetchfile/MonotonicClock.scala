package vdx.fetchfile

/**
 * Representation of a clock.
 * In case of cats.effect.Clock[F], the evaluation of IO makes the stream slightly slower.
 * To avoid this performance impact, but still keep the progress tracker testable we introduce our own clock interface.
 */
trait MonotonicClock {
  def nanoTime(): Long
}

object MonotonicClock {
  /**
   * Simple clock implementation based on System.nanoTime()
   */
  def system(): MonotonicClock = new MonotonicClock {
    def nanoTime(): Long = System.nanoTime()
  }
}

