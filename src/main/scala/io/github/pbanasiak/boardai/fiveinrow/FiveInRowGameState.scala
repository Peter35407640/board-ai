package io.github.pbanasiak.boardai.fiveinrow

import io.github.pbanasiak.boardai.{GameResult, GameState, Player}

class FiveInRowGameState(game: FiveInRowGame,
  board: Board, playerToMove: Player,
  gameResult: Option[GameResult.Value]
) extends GameState[Board, Move](board, playerToMove, gameResult) {
  def applyMove(move: Move): FiveInRowGameState = {
    val nextBoard = game.makeMove(board, move, playerToMove)
    val nextPlayer = playerToMove.otherPlayer
    val isVictory = game.playerWin(nextBoard, playerToMove, move)
    if (isVictory) {
      val newGameResult = playerToMove match {
        case Player.xPlayer => GameResult.xWin
        case Player.oPlayer => GameResult.oWin
      }
      new FiveInRowGameState(game, nextBoard, nextPlayer, Some(newGameResult))
    } else if (game.validMoves(nextBoard).isEmpty) {
      new FiveInRowGameState(game, nextBoard, nextPlayer, Some(GameResult.draw))
    } else {
      new FiveInRowGameState(game, nextBoard, nextPlayer, None)
    }
  }

}
