package io.github.pbanasiak.boardai.mcts

import io.github.pbanasiak.boardai.Score
import io.github.pbanasiak.boardai.mcts.MctsTreeNode

object MctsTreeNode {
}

/**
 * generic
 *
 * @param gameState
 * @param winProbability
 * @param priors
 * @param parent
 * @param lastMove
 * @tparam GS
 * @tparam M
 */
class MctsTreeNode[GS, M](
  val gameState: GS,
  val priorScore: Score,
  priors: Map[M, Double],
  val parent: Option[MctsTreeNode[GS, M]],
  val lastMove: Option[M]
) {
  var totalVisitCount: Int = 0
  private var children: Map[M, MctsTreeNode[GS, M]] = Map()

  /**
   * valid moves -> BranchStats.
   * This is the expansion phase of the MCTS algorithm.
   * populated in class constructor
   */
  var branches: Map[M, NodeStatistics] = {
    priors.map { case (move, prior) => move -> NodeStatistics(prior) }
  }

  def moves: List[M] = branches.keys.toList

  def addChild(move: M, childNode: MctsTreeNode[GS, M]): Unit = children += (move -> childNode)

  def getChild(move: M): Option[MctsTreeNode[GS, M]] = children.get(move)

  def hasChild(move: M): Boolean = children.contains(move)

  def recordVisit(move: M, value: Score): Unit = {
    totalVisitCount += 1
    branches.get(move) match {
      case None => throw new Exception(s"move $move not in branch")
      case Some(b) =>
        val updatedBranch = NodeStatistics(b.prior, b.visitCount + 1, b.totalValue + value.value)
        branches += (move -> updatedBranch)
    }
  }

  /**
   *
   * @param move
   * @return -1, 0, +1 range value from current player's perspective
   */
  def expectedValue(move: M): Double = {
    val branch = branches(move)
    branch.visitCount match {
      // using prior value
      case 0 => this.priorScore.value

      case _ => branch.totalValue / branch.visitCount.toDouble
    }
  }

  def prior(move: M): Double = branches(move).prior

  def visitCount(move: M): Int = branches(move).visitCount

}