package io.github.pbanasiak.boardai.agent

import io.github.pbanasiak.boardai.*

class AlphaBetaAgent[B, M](game: Game[B, M]) extends Agent[B, M] {

  // prefer quicker wins: tiny per-ply decay applied to terminal win scores.
  // This keeps Score normalized in [-1, 1] while giving a small preference
  // for faster wins over slower ones. We assume searches won't exceed
  // maxSearchDepth plies; adjust if you ever expect deeper searches.
  val maxSearchDepth: Int = 1000
  val winDecayPerPly: Double = 1.0 / (10 * maxSearchDepth)

  def negamax(gameState: GameState[B, M], alpha: Double, beta: Double, n: Int = 0): (Score, Option[M]) = {
    assert(n <= maxSearchDepth, s"maxSearchDepth exceeded")

    var currentAlpha = alpha

    // Terminal state check
    if (gameState.isOver) {
      val result = gameState.gameResult.get
      val score = result match {
        case GameResult.draw => game.drawScore
        // +1 is to match the logic of AlphaBetaAgent.negamax, here is one level deeper
        case _ => -Score(game.winScore.value - n * winDecayPerPly)
      }
      return (score, None)
    }

    val validMoves = game.validMoves(gameState)
    if (validMoves.isEmpty) {
      throw new Exception("No valid moves")
    }

    var bestScore = Double.NegativeInfinity
    var bestMove = validMoves.head // Always have at least one move here

    validMoves.foreach { move =>
      val nextGameState = game.applyMove(gameState, move)
      val (opponentScore, opponentOptionalMove) = negamax(nextGameState, -beta, -currentAlpha, n + 1)
      val score: Score = -opponentScore

      if (score.value > bestScore) {
        bestScore = score.value
        bestMove = move
      }

      currentAlpha = math.max(currentAlpha, score.value)
      if (currentAlpha >= beta) {
        // Alpha-beta cutoff
        return (Score(bestScore), Some(bestMove))
      }
    }

    (Score(bestScore), Some(bestMove))
  }

  def negamax(gameState: GameState[B, M]): (Score, Option[M]) = {
    negamax(gameState, Double.NegativeInfinity, Double.PositiveInfinity, 0)
  }

  def bestMove(gameState: GameState[B, M]): ScoredMove[M] = {
    val (bestScore, optionalMove) = negamax(gameState)
    optionalMove match {
      case Some(m) => ScoredMove(bestScore, m)
      case None => throw new Exception("bestMove must not be called on terminal game state")
    }
  }

  override def selectMove(gameState: GameState[B, M]): M = {
    val bestScoredMove = bestMove(gameState)
    bestScoredMove.move
  }
}