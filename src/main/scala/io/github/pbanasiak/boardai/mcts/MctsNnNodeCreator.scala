package io.github.pbanasiak.boardai.mcts

import org.nd4j.linalg.api.ndarray.INDArray
import io.github.pbanasiak.boardai.{Game, GameState, Player, Score}
import io.github.pbanasiak.boardai.mcts.{MctsTreeNode, NodeCreator}
import io.github.pbanasiak.boardai.nn.{GameStateEncoder, PolicyPriorUtils, PolicyValueModel, ValueUtils}

/**
 * NodeCreator that uses a policy–value model to initialize:
 *  - priors: from model.policy(state)
 *  - value:  from model.value(state)
 *
 * Priors are masked to valid moves and renormalized. If the model returns
 * degenerate priors (sum <= 0 or NaN), uniform priors are used as fallback.
 */
class MctsNnNodeCreator[B,M, EncodedType](
  game: Game[B, M],
  
  encoder: GameStateEncoder[B,M, EncodedType],
  policyValueModel: PolicyValueModel[EncodedType]
) extends NodeCreator[B, M] {

  override def createNode(gameState: GameState[B, M], move: Option[M], parent: Option[MctsTreeNode[GameState[B, M], M]]): MctsTreeNode[GameState[B, M], M] = {
    val validMoves: Seq[M] = game.validMoves(gameState)
    val input: EncodedType = encoder.encodeGameState(gameState)

    // Use ValueUtils instead of inline conversion
    val rawXValue: Double = policyValueModel.value(input)
    val valueForCurrentPlayer: Double = ValueUtils.xPerspectiveToCurrentPlayer(rawXValue, gameState.playerToMove)

    val rawPolicy: Array[Double] = policyValueModel.policy(input)
    val movePriors: Map[M, Double] = PolicyPriorUtils.maskAndRenormalize(rawPolicy, validMoves, encoder.moveToIndex)

    val newNode = new MctsTreeNode(gameState, Score(valueForCurrentPlayer), movePriors, parent, move)

    for {
      parentNode <- parent
      moveToParent <- move
    } parentNode.addChild(moveToParent, newNode)
    newNode
  }

}
