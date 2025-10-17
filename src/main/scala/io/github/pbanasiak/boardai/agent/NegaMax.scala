package io.github.pbanasiak.boardai.agent

import io.github.pbanasiak.boardai.{Agent, Game, GameResult, GameState, Score, ScoredMove}

/**
 * this agent is slow but the logic is simple
 * use it to cross check AlphaBetaAgent logic.
 */
class NegaMax[B, M](game: Game[B, M]) extends Agent[B, M] {

  // prefer quicker wins: tiny per-ply decay applied to terminal win scores.
  // This keeps Score normalized in [-1, 1] while giving a small preference
  // for faster wins over slower ones. We assume searches won't exceed
  // maxSearchDepth plies; adjust if you ever expect deeper searches.
  val maxSearchDepth: Int = 1000
  val winDecayPerPly: Double = 1.0 / (10 * maxSearchDepth)

  def bestMove(gameState: GameState[B, M], n: Int): ScoredMove[M] = {
    assert(n <= maxSearchDepth, s"maxSearchDepth exceeded")
    assert(!gameState.isOver, s"game is already over")

    val validMoves = game.validMoves(gameState)
    assert(validMoves.nonEmpty, s"game is already over")

    val scoredNextMoves: Seq[ScoredMove[M]] = validMoves.map { m =>


      val nextGameState = game.applyMove(gameState, m)
      // if terminal state, current player win, or draw
      if (nextGameState.isOver) {
        // TODO can be draw or win for current player
        val s = nextGameState.gameResult.get match {
          case GameResult.draw => game.drawScore
          // prefer win on shallow depth
          case _ => Score(game.winScore.value - (n + 1) * winDecayPerPly)
        }
        ScoredMove(s, m)
      } else {
        val score = bestMove(nextGameState, n + 1)

        // from my perspective
        val scoreForCurrentPlayer = -score.score
        ScoredMove(scoreForCurrentPlayer, m)
      }
    } // validMoves.map

    val bestScoredMove: ScoredMove[M] = scoredNextMoves.sortBy(_.score.value).reverse.head

    bestScoredMove
  }

  override def selectMove(gameState: GameState[B, M]): M = {

    val bestScoredMove: ScoredMove[M] = bestMove(gameState, 0)
    bestScoredMove.move
  }
}
