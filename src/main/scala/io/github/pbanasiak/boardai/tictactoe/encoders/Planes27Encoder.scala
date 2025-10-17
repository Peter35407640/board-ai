package io.github.pbanasiak.boardai.tictactoe.encoders

import io.github.pbanasiak.boardai.nn.GameStateEncoder
import io.github.pbanasiak.boardai.{GameState, Player}
import io.github.pbanasiak.boardai.tictactoe.{TttBoard, TttMove}

final class Planes27Encoder extends GameStateEncoder[TttBoard, TttMove, Array[Double]] {

  override val nInput: Int = 27
  override val nOutput: Int = 9

  override def moveToIndex(move: TttMove): Int = move.r * 3 + move.c

  // Fixed perspective encoding: [X stones | O stones | X-to-move flag]
  override def encodeGameState(gameState: GameState[TttBoard, TttMove]): Array[Double] = {
    val isXToMove = gameState.playerToMove == Player.xPlayer

    val encodedPlanes: Seq[(Int, Double, Double, Double)] = for {
      row <- 0 until 3
      col <- 0 until 3
      cellValue = gameState.board.getInt(row, col)
      flatIndex = row * 3 + col
    } yield {
      val xPlane = if (cellValue == Player.xStone) 1.0 else 0.0
      val oPlane = if (cellValue == Player.oStone) 1.0 else 0.0
      val xToMovePlane = if (isXToMove) 1.0 else 0.0

      (flatIndex, xPlane, oPlane, xToMovePlane)
    }

    val result = Array.ofDim[Double](27)
    encodedPlanes.foreach { case (index, xPlane, oPlane, xToMoveFlag) =>
      result(index) = xPlane           // plane 0: X stones (always)
      result(index + 9) = oPlane       // plane 1: O stones (always)  
      result(index + 18) = xToMoveFlag // plane 2: X-to-move flag
    }

    result
  }
}