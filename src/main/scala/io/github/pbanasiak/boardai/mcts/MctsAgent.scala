package io.github.pbanasiak.boardai.mcts

import io.github.pbanasiak.boardai.nn.PendingTrainingSample
import io.github.pbanasiak.boardai.{ExperienceCollector, Game, GameResult, GameState}

import scala.util.Random

class MctsAgent[B, M](
  game: Game[B, M],
  nodeCreator: NodeCreator[B, M],
  mcPlayer: MonteCarloPlayer[B, M],
  collector: Option[ExperienceCollector[B, M]],
  roundsPerMove: Int,
  val mctsCoeff: Double = 2.0,
  rand: Random,
  val selectRandomly: Boolean,
  debug: Boolean = false
) extends MctsAgentBase[B, M](game, nodeCreator, mcPlayer, roundsPerMove, rand, debug) {


  def selectBranch(node: MctsTreeNode[GameState[B, M], M]): M = {
    val totalCount = node.totalVisitCount

    // UCT (Upper Confidence bounds applied to Trees) formula for traditional MCTS
    def uctScore(move: M): Double = {
      val expVal = node.expectedValue(move) // Q(s,a) - average value
      val p = node.prior(move) // uniform prior for traditional MCTS
      val n = node.visitCount(move)

      val exploration = Math.sqrt(totalCount.doubleValue()) / (n + 1)
      val score = expVal + mctsCoeff * p * exploration
      score
    }

    if (node.moves.isEmpty) {
      throw new Exception(s"validMoves.isEmpty")
    }

    val movesWithScore = node.moves.map(m => (m, uctScore(m)))
    if (selectRandomly) {
      val topK = 2
      // shuffle guarantee to start randomly where all scores are the same
      val shuffled = Random.shuffle(movesWithScore)
      val sortedMoves = shuffled.sortBy(-_._2).take(topK)
      val idx = rand.nextInt(sortedMoves.length)
      sortedMoves(idx)._1
    } else {
      movesWithScore.maxBy(_._2)._1
    }
  }

  // pure mcts agent can be used to collect training data
  override protected def afterMoveSelection(
    root: MctsTreeNode[GameState[B, M], M],
    selectedMove: Option[M],
    gameResult: Option[GameResult.Value]
  ): Unit = {
    collector.foreach { col =>
      val visitCounts: Map[M, Int] = {
        root.branches.map { case (move, stats) =>
          move -> stats.visitCount
        }
      }

      /*
      gameResult can be known for the node if its a terminal state.
      terminal middle game results is not necessary the same as the very game result.
      because it might not be selected, actually it was avoided.
       */
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

