package io.github.pbanasiak.boardai.fiveinrow.nn

import io.github.pbanasiak.boardai.GameState
import io.github.pbanasiak.boardai.fiveinrow.{Board, Move}
import io.github.pbanasiak.boardai.nn.{FinalizedTrainingSample, PendingTrainingSample}

object TrainingSampleRotator {
  def rotatePendingTrainingSample(
    sample: FinalizedTrainingSample[Board, Move],
    boardSize:Int
  ): Seq[FinalizedTrainingSample[Board, Move]] = {

    val originalBoard = sample.gameState.board

    // Function to rotate a move by given degrees
    def rotateMove(move: Move, degrees: Int): Move = degrees match {
      case 90  => Move(move.c, boardSize - 1 - move.r)           // 90° clockwise
      case 180 => Move(boardSize - 1 - move.r, boardSize - 1 - move.c) // 180°
      case 270 => Move(boardSize - 1 - move.c, move.r)           // 270° clockwise (90° counter-clockwise)
      case _   => move
    }

    // Function to rotate a board by given degrees
    def rotateBoard(board: Board, degrees: Int): Board = {
      val rotatedCells = Array.ofDim[Int](boardSize, boardSize)

      for (r <- 0 until boardSize; c <- 0 until boardSize) {
        val (newR, newC) = degrees match {
          case 90  => (c, boardSize - 1 - r)
          case 180 => (boardSize - 1 - r, boardSize - 1 - c)
          case 270 => (boardSize - 1 - c, r)
          case _   => (r, c)
        }
        rotatedCells(newR)(newC) = board.getInt(r, c)
      }

      new Board(rotatedCells)
    }

    // Generate rotated samples for 90°, 180°, and 270°
    Seq(90, 180, 270).map { degrees =>
      val rotatedBoard: Board = rotateBoard(originalBoard, degrees)
      val rotatedValidMoves = sample.validMoves.map(move => rotateMove(move, degrees))
      val rotatedVisitCounts = sample.visitCounts.map { case (move, count) =>
        rotateMove(move, degrees) -> count
      }
      val rotatedMoveChosen = sample.moveChosen.map(move => rotateMove(move, degrees))

      FinalizedTrainingSample[Board, Move](
        gameState = new GameState(rotatedBoard, sample.gameState.playerToMove, sample.gameState.gameResult),
        validMoves = rotatedValidMoves,
        visitCounts = rotatedVisitCounts,
        moveChosen = rotatedMoveChosen,
        gsValue = sample.gsValue // unchanged
      )
    }
  }
}
