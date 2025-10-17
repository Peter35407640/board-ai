package io.github.pbanasiak.boardai.nn

/**
 * Dummy model that returns uniform policy and zero value.
 * Useful for wiring and testing MCTS+NN pipelines before adding real backends.
 */
final class UniformPolicyZeroValueModel(outputSize: Int) extends PolicyValueModel[Array[Double]] {

  private val uniform: Array[Double] = {
    val probability = 1.0 / outputSize.toDouble
    Array.fill(outputSize)(probability)
  }

  override def policy(input: Array[Double]): Array[Double] = uniform

  override def value(input: Array[Double]): Double = 0.0
}
