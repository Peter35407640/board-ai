package io.github.pbanasiak.boardai.mcts

import io.github.pbanasiak.boardai.mcts.{MctsTreeNode, NodeCreator, MonteCarloPlayer}
import io.github.pbanasiak.boardai.{Agent,GameResult, Game, GameState, Score, ScoredMove}

import scala.annotation.tailrec
import scala.util.Random

abstract class MctsAgentBase[B, M](
  protected val game: Game[B, M],
  protected val nodeCreator: NodeCreator[B, M],
  protected val mcPlayer: McPlayer[B, M],
  val roundsPerMove: Int,
  protected val rand: Random,
  protected val debug: Boolean = false
) extends Agent[B, M] {

  override def selectMove(gameState: GameState[B, M]): M = {
    val scoredMove = selectScoredMove(gameState)
    if (debug) println(s"bestScoredMove: ${scoredMove.score} ${scoredMove.move}")
    scoredMove.move
  }

  // Check if any immediate child is a terminal win for the current player
  protected def findImmediateWinningMove(node: MctsTreeNode[GameState[B, M], M]): Option[(M, GameState[B, M])] = {
    val currentPlayer = node.gameState.playerToMove
    node.moves.iterator
      .map(m => m -> game.applyMove(node.gameState, m))
      .find { case (_, nextState) => nextState.isOver && nextState.isVictoryOf(currentPlayer) }
  }

  @tailrec
  final def updateAncestors(mctsTreeNode: MctsTreeNode[GameState[B, M], M], score: Score): Unit = {
    if (mctsTreeNode.parent.isDefined && mctsTreeNode.lastMove.isDefined) {
      mctsTreeNode.parent.get.recordVisit(mctsTreeNode.lastMove.get, score)
      updateAncestors(mctsTreeNode.parent.get, -score)
    }
  }

  @tailrec
  final def addLeafNode(node: MctsTreeNode[GameState[B, M], M]): MctsTreeNode[GameState[B, M], M] = {
    if (node.gameState.isOver) {
      node
    } else {
      // Immediate-win cutoff: if any child is terminal win, take it
      findImmediateWinningMove(node) match {
        case Some((winMove, _terminalState)) =>
          node.getChild(winMove) match {
            case Some(childNode) => childNode

            case None =>
              val newState: GameState[B, M] = game.applyMove(node.gameState, winMove)
              val childNode = nodeCreator.createNode(newState, Some(winMove), Some(node))
              childNode
          }
        case None =>
          val nextMove: M = selectBranch(node)
          node.getChild(nextMove) match {
            case None =>
              val newState: GameState[B, M] = game.applyMove(node.gameState, nextMove)
              val childNode = nodeCreator.createNode(newState, Some(nextMove), Some(node))
              childNode
            case Some(childNode) =>
              addLeafNode(childNode)
          }
      }
    }
  }

  def singlePlayout(root: MctsTreeNode[GameState[B, M], M]): Unit = {
    val newLeafNode = addLeafNode(root)

    // not necessary terminal , mcPlayer might stop at some depth and return scores
    val playOutNode: MctsTreeNode[GameState[B, M], M] = newLeafNode.gameState.gameResult match {
      case Some(gResult) => newLeafNode
      case None => mcPlayer.valueFromMCPlayout(newLeafNode)
    }

    newLeafNode.totalVisitCount += 1

    val leadingToLeafPlayer =
      newLeafNode.parent.map(_.gameState.playerToMove).getOrElse(throw Exception("parent is None"))

    // value from perspective of the player leading to the leaf node
    val mctsValue: Score = if(playOutNode.gameState.gameResult.isDefined) {
      // Terminal case: convert game result to value from leadingToLeafPlayer's perspective
      playOutNode.gameState.toMctValue(leadingToLeafPlayer)
    } else {
      val rawPriorScore: Score = playOutNode.priorScore
      // rawPriorScore is from perspective of playOutNode.gameState.playerToMove
      // Convert to perspective of leadingToLeafPlayer
      val score = if(playOutNode.gameState.playerToMove == leadingToLeafPlayer) { rawPriorScore}  else { -rawPriorScore}
      score
    }
    updateAncestors(newLeafNode, mctsValue)
  }

  def selectScoredMove(gameState: GameState[B, M]): ScoredMove[M] = {
    val root: MctsTreeNode[GameState[B, M], M] = nodeCreator.createNode(gameState)

    findImmediateWinningMove(root) match {
      case Some((winMove, terminalState)) =>
        // Use the provided terminal state (no recomputation/guessing)
        afterMoveSelection(root, selectedMove = Some(winMove), gameResult = Some(terminalState.gameResult.get))
        return ScoredMove(Score(1.0), winMove)
      case None => ()
    }

    for (_ <- 0 until roundsPerMove) {
      singlePlayout(root)
    }

    val selected = selectMostVisitedMove(root)
    val expectedValue: Double = root.expectedValue(selected)

    // Hook for subclasses to perform additional actions (like recording visit counts)
    afterMoveSelection(root,  selectedMove = Some(selected), gameResult = None)

    ScoredMove(Score(expectedValue), selected)
  }

  def selectMostVisitedMove(root: MctsTreeNode[GameState[B, M], M]): M = {
    assert(root.branches.nonEmpty)

    val movesWithVisitCounts: Seq[(M, Int)] =
      root.branches.toSeq.map { case (m, b) => (m, b.visitCount) }
    val maxVisits = movesWithVisitCounts.map(_._2).max

    val maxVisitMoves = movesWithVisitCounts.collect { case (m, v) if v == maxVisits => m }
    maxVisitMoves.head
  }

  // Abstract method that subclasses must implement
  protected def selectBranch(node: MctsTreeNode[GameState[B, M], M]): M

  // Hook method for subclasses to override if needed
  protected def afterMoveSelection(
    root: MctsTreeNode[GameState[B, M], M],
    selectedMove: Option[M],
    gameResult: Option[GameResult.Value]
  ): Unit = ()
}
