package io.github.pbanasiak.boardai

import io.github.pbanasiak.boardai.nn.{EncodedTrainingSample, FinalizedTrainingSample, GameStateEncoder}

trait SampleEncoder[B,M, EncodedType] {
  
  def encode(
    sample: FinalizedTrainingSample[B, M],
  ): EncodedTrainingSample[EncodedType]
}
