package io.github.pbanasiak.boardai.fiveinrow.encoders

import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.factory.Nd4j
import io.github.pbanasiak.boardai.{GameState, Player}
import io.github.pbanasiak.boardai.fiveinrow.{Board, FiveInRowEncoder, GameConfig, Move}

class Planes3FiveInRowEncode(gameConfig: GameConfig) extends FiveInRowEncoder {

  override val nInput: Int = gameConfig.boardSize * gameConfig.boardSize * 3  // 3 planes: current, opponent, player-to-move
  override val nOutput: Int = gameConfig.boardSize * gameConfig.boardSize    // one output per board position

  override def moveToIndex(move: Move): Int = {
    move.r * gameConfig.boardSize + move.c
  }

  override def encodeGameState(gs: GameState[Board, Move]): INDArray = {
    val isXToMove = gs.playerToMove == Player.xPlayer
    val boardSize = gameConfig.boardSize

    // Create INDArray with shape (1, 3, boardSize, boardSize)
    val result = Nd4j.create(1, 3, boardSize, boardSize)

    for (row <- 0 until boardSize) {
      for (col <- 0 until boardSize) {
        val cell = gs.board.getInt(row, col)

        // Plane 0: X stones
        result.putScalar(Array(0, 0, row, col), if (cell == Player.xStone) 1.0 else 0.0)

        // Plane 1: O stones  
        result.putScalar(Array(0, 1, row, col), if (cell == Player.oStone) 1.0 else 0.0)

        // Plane 2: X-to-move indicator
        result.putScalar(Array(0, 2, row, col), if (isXToMove) 1.0 else 0.0)
      }
    }

    result
  }
}
