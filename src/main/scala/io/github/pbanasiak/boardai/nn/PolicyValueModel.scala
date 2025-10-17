package io.github.pbanasiak.boardai.nn

import org.nd4j.linalg.api.ndarray.INDArray

/**
 * Minimal interface to decouple agents/search from any NN backend.
 *
 * Implementations return:
 *  - policy: action probabilities (length = output size, e.g., 9 for 3x3)
 *  - value: scalar evaluation of the position from the current player's perspective in [-1, 1]
 *
 * The input is assumed to be the encoder’s output (e.g., 27-length Planes27Encoder vector).
 *  EncodedType for example Array[Double] or INDArray
 */
trait PolicyValueModel[EncodedType] {
  def policy(input: EncodedType): Array[Double]
  def value(input: EncodedType): Double
}