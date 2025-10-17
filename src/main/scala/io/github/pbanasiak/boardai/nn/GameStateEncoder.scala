package io.github.pbanasiak.boardai.nn

import org.nd4j.linalg.api.ndarray.INDArray
import io.github.pbanasiak.boardai.GameState

// EncodedType for example Array[Double] or INDArray
trait GameStateEncoder[B,M, EncodedType] {
  def nInput: Int  // e.g. 27 for 3 planes x 3x3
  def nOutput: Int // e.g. 9 for 3x3 board
  def moveToIndex(move: M): Int
  def encodeGameState(gs: GameState[B, M]): EncodedType
}

