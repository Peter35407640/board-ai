package io.github.pbanasiak.boardai

import scala.collection.mutable.ListBuffer
import scala.util.Random

class DistributionSampler(rnd: Random) {

  def sampleIndex(distribution: List[Double]): Int = {
    assert(distribution.nonEmpty)
    assert(distribution.forall(p => p >= 0))
    val almostZero = 1e-7

    val sum: Double = distribution.sum
    if (sum <= almostZero) {
      0
    } else {

      var prev = 0.0
      val aBuffer = ListBuffer[(Double, Double)]()
      distribution.foreach { d =>
        val next = prev + d
        aBuffer.addOne((prev, next))
        prev = next
      }

      val r: Double = rnd.nextDouble() * sum
      val found: Int = aBuffer.indexWhere { case (a, b) => a <= r && r < b }

      if (found < 0) {
        0
      } else found

    }

  }
}
