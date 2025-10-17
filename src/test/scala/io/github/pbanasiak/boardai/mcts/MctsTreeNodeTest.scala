package io.github.pbanasiak.boardai.mcts

import org.scalactic.Tolerance.convertNumericToPlusOrMinusWrapper
import org.scalatest.funsuite.AnyFunSuite
import io.github.pbanasiak.boardai.Score

class MctsTreeNodeTest extends AnyFunSuite {

  test("expectedValue returns node winProbability converted to score for unvisited move") {
    val priorScore = Score(0.7)
    val priors = Map(1 -> 0.5, 2 -> 0.5)
    val node = new MctsTreeNode[String, Int]("state", priorScore, priors, None, None)

    assert(node.expectedValue(1) === priorScore)
    assert(node.expectedValue(2) === priorScore)
  }

  test("expectedValue returns empirical average for visited move") {
    val priorScore = Score(0.2)
    val priors = Map(1 -> 0.5, 2 -> 0.5)
    val node = new MctsTreeNode[String, Int]("state", priorScore, priors, None, None)

    // record two visits with values 0.2 and 0.4 -> average 0.3
    node.recordVisit(1, Score(0.2))
    node.recordVisit(1, Score(0.4))

    val expectedAvg = (0.2 + 0.4) / 2.0
    assert(node.visitCount(1) == 2)
    assert(node.expectedValue(1) === expectedAvg +- 1e-12)
  }
}