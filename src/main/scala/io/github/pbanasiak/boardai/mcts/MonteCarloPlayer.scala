package io.github.pbanasiak.boardai.mcts

import io.github.pbanasiak.boardai.mcts.{MctsTreeNode, NodeCreator, NodeStatistics}
import io.github.pbanasiak.boardai.*

import scala.util.Random

case class MonteCarloPlayer[B, M](
  game: Game[B, M],
  nodeCreator: NodeCreator[B, M], rnd: Random) extends McPlayer[B, M] {
  /**
   * Plays stochastically to the end of the game to determine a winner.
   * Moves use the probability distribution determined by the model priors instead of being completely random.
   * need MctsTreeNode not just GameState because of Stochastic sampling needs priors
   *
   * @param node current state to play from
   * @return the value from the point of view of the current player
   */
  override def valueFromMCPlayout(node: MctsTreeNode[GameState[B, M], M]): MctsTreeNode[GameState[B, M], M] =
    if (node.gameState.isOver) {
      node
    } else {
      val move = selectMoveStochastically(node)
      val newState = game.applyMove(node.gameState, move)
      // The parent is None because the playout does not need to be part of the MCTree.
      val childNode = nodeCreator.createNode(newState, Some(move), None)
      valueFromMCPlayout(childNode)
    }

  /**
   * Stochastic selection favoring moves with higher scores.
   */
  private def selectMoveStochastically(node: MctsTreeNode[GameState[B, M], M]): M = {
    if (node.branches.size == 1) {
      node.branches.keys.head
    } else {
      val movesWithPriors: List[(M, NodeStatistics)] = node.branches.toList
      val priors = movesWithPriors.map(_._2.prior)
      val idx = new DistributionSampler(rnd).sampleIndex(priors)
      movesWithPriors(idx)._1

    }
  }
}
