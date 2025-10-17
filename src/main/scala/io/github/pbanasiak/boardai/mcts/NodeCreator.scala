package io.github.pbanasiak.boardai.mcts

import io.github.pbanasiak.boardai.GameState

/**
 * Factory trait for creating MCTS tree nodes.
 * Implementations can customize node creation logic, such as how priors are calculated
 * or how neural network evaluations are integrated.
 */
trait NodeCreator[B, M] {
  /**
   * Creates a new MCTS tree node
   * @param gameState The game state this node represents
   * @param move The move that led to this state (None for root node)
   * @param parent The parent node (None for root node)
   * @return A new MctsTreeNode
   */
  def createNode(
    gameState: GameState[B, M],
    move: Option[M] = None,
    parent: Option[MctsTreeNode[GameState[B, M], M]] = None
  ): MctsTreeNode[GameState[B, M], M]
}