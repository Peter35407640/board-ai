package io.github.pbanasiak.boardai.mcts

import org.nd4j.linalg.api.ndarray.INDArray
import io.github.pbanasiak.boardai.mcts.{MctsAgentBase, MctsTreeNode, MonteCarloPlayer, NodeCreator}
import io.github.pbanasiak.boardai.*
import io.github.pbanasiak.boardai.nn.{GameStateEncoder, PendingTrainingSample}

import scala.util.Random

class MctsNnAgent[B, M](
  game: Game[B, M],
  nodeCreator: NodeCreator[B, M],
  mcPlayer: McPlayer[B, M],
  collector: Option[ExperienceCollector[B, M]],
  roundsPerMove: Int,
  val puctCoeff: Double = 2.0,
  rand: Random,
  val selectRandomly: Boolean,
  debug: Boolean = false
) extends MctsAgentBase[B, M](game, nodeCreator, mcPlayer, roundsPerMove, rand, debug) {

  // Store last search tree visit counts for debugging
  @volatile private var lastVisitCounts: Option[Map[M, Int]] = None

  // ### **0.5 - 0.8**  - Moderate randomness, still favors good moves
  val temperature: Double = 0.8

  /**
   * Get visit counts from the last MCTS search.
   * Useful for debugging and visualization.
   */
  def getLastSearchVisits(): Option[Map[M, Int]] = lastVisitCounts

  def selectBranch(node: MctsTreeNode[GameState[B, M], M]): M = {
    val totalCount = node.totalVisitCount

    def puctScore(move: M): Double = {
      val expVal = node.expectedValue(move)
      val p = node.prior(move)
      val n = node.visitCount(move)

      val exploration = Math.sqrt(totalCount + 1) / (n + 1)
      val score = expVal + puctCoeff * p * exploration
      score
    }

    if (node.moves.isEmpty) {
      throw new Exception(s"validMoves.isEmpty")
    }

    val movesWithScore: Seq[(M, Double)] = node.moves.map(m => (m, puctScore(m)))

    if (selectRandomly && temperature > 0.0) {
      val scores = movesWithScore.map(_._2)
      val adjustedScores = scores.map(_ / temperature)
      val maxScore = adjustedScores.max
      val expScores = adjustedScores.map(s => Math.exp(s - maxScore))
      val probabilities = {
        val sum = expScores.sum
        expScores.map(_ / sum)
      }

      val randomValue = rand.nextDouble()
      val selectedIndex = probabilities
        .scanLeft(0.0)(_ + _)
        .tail
        .indexWhere(_ >= randomValue)

      if (selectedIndex >= 0) {
        movesWithScore(selectedIndex)._1
      } else {
        movesWithScore.last._1
      }
    } else {
      movesWithScore.maxBy(_._2)._1
    }
  }

  /* ------------------------------------------------------------------ *
 *  Collect experience right after the search has selected the move.  *
 * ------------------------------------------------------------------ */


  override protected def afterMoveSelection(
    root: MctsTreeNode[GameState[B, M], M],
    selectedMove: Option[M],
    gameResult: Option[GameResult.Value]
  ): Unit = {
    // Store visit counts for debugging
    val visitCounts: Map[M, Int] = {
      root.branches.map { case (move, stats) =>
        move -> stats.visitCount
      }
    }
    lastVisitCounts = Some(visitCounts)

    // Collect experience for training
    collector.foreach { col =>
      val pending = PendingTrainingSample(
        gameState = root.gameState,
        validMoves = root.moves,
        visitCounts = visitCounts,
        moveChosen = selectedMove,
        gameResult = gameResult,
      )
      col.collect(pending)
    }
  }

}