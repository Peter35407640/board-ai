package io.github.pbanasiak.boardai.mcts

import io.github.pbanasiak.boardai.{Game, GameState, Score}

case class MctsNodeCreator[B, M](game: Game[B, M]) extends NodeCreator[B, M] {

  def createNode(
    gameState: GameState[B, M],
    move: Option[M] = None,
    parent: Option[MctsTreeNode[GameState[B, M], M]] = None): MctsTreeNode[GameState[B, M], M] = {

    val validMoves = game.validMoves(gameState)

    // For plain MCTS: no initial value estimation, uniform priors
    val pessimisticScore = Score(-1) // dont trust un-explored nodes

    val uniformPrior = if (validMoves.nonEmpty) 1.0 / validMoves.size else 0.0

    val movePriors = validMoves.map(_ -> uniformPrior).toMap

    val newNode = new MctsTreeNode(gameState, pessimisticScore, movePriors, parent, move)

    // Establish a parent-child relationship
    for {
      parentNode <- parent
      moveToParent <- move
    } parentNode.addChild(moveToParent, newNode)
    newNode
  }

}