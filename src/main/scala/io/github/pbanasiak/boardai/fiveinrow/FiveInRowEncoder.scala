package io.github.pbanasiak.boardai.fiveinrow

import org.nd4j.linalg.api.ndarray.INDArray
import io.github.pbanasiak.boardai.GameState
import io.github.pbanasiak.boardai.nn.GameStateEncoder

trait FiveInRowEncoder extends GameStateEncoder[Board, Move, INDArray] {
  // Any FiveInRow-specific methods can go here
  // The basic interface is inherited from GameStateEncoder
}
