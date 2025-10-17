package io.github.pbanasiak.boardai.mcts

// scala

import io.github.pbanasiak.boardai.nn.GameStateEncoder
import io.github.pbanasiak.boardai.{ExperienceCollector, Game, GameState}

class NoShortCircuitMctsNnAgent[B, M](
  game: Game[B, M],
  nodeCreator: NodeCreator[B, M],
  mcPlayer: MonteCarloPlayer[B, M],
  collector: Option[ExperienceCollector[B, M]],
  roundsPerMove: Int,
  puctCoeff: Double,
  rand: scala.util.Random,
  selectRandomly: Boolean,
  debug: Boolean = false
) extends MctsNnAgent[B, M](game, nodeCreator, mcPlayer, collector, roundsPerMove, puctCoeff, rand, selectRandomly, debug) {

  // Disable the short-circuit path for this evaluation agent
  override protected def findImmediateWinningMove(
    node: MctsTreeNode[GameState[B, M], M]
  ): Option[(M, GameState[B, M])] = None
}
