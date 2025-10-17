package io.github.pbanasiak.boardai.agent

import io.github.pbanasiak.boardai.{Game, GameState, Score, ScoredMove}

class FixedDepthAlphaBetaAgent[B, M](game: Game[B, M], fixedDepth: Int) 
  extends AlphaBetaAgent[B, M](game) {

  // Override the main negamax method to limit depth
  override def negamax(gameState: GameState[B, M], alpha: Double, beta: Double, n: Int = 0): (Score, Option[M]) = {
    // Check depth limit first
    if (n >= fixedDepth) {
      // At depth limit, use heuristic evaluation
      // For most games, neutral evaluation is reasonable
      return (Score(0.0), None)
    }
    
    // Otherwise, delegate to parent implementation
    super.negamax(gameState, alpha, beta, n)
  }
}
