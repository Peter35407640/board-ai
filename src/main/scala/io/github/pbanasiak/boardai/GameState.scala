package io.github.pbanasiak.boardai

import io.github.pbanasiak.boardai.Player.xPlayer

class GameState[B, M](
  val board: B, val playerToMove: Player,
  val gameResult: Option[GameResult.Value]
) {

  def isOver: Boolean = gameResult.isDefined

  // Score is in [-1,1]
  def toMctValue(currentPlayer: Player): Score = {
    gameResult match {
      case Some(gResult) => gResult match {
        case GameResult.draw => Score(0)
        case GameResult.xWin => if (currentPlayer == xPlayer) Score(1) else Score(-1)
        case GameResult.oWin => if (currentPlayer == xPlayer) Score(-1) else Score(1)
      }

      case None =>
        assert(false, "call to toMctValue on non-terminal state")
        Score(0)
    }
  }

  // TODO in case X made last leading to victory move,
  // the victory is not neccessary X depends on game rules
  def isVictoryOf(player: Player): Boolean = {
    gameResult match {
      case None => false
      case Some(gResult) => gResult match {
        case GameResult.draw => false
        case GameResult.xWin => (player == Player.xPlayer)
        case GameResult.oWin => (player == Player.oPlayer)
      }
    }
  }


}
