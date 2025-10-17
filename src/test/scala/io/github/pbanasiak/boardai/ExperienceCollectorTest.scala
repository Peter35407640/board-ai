package io.github.pbanasiak.boardai

import org.scalatest.funsuite.AnyFunSuite
import io.github.pbanasiak.boardai.nn.{FinalizedTrainingSample, PendingTrainingSample}
import io.github.pbanasiak.boardai.tictactoe.{TttBoard, TttMove}

class ExperienceCollectorTest extends AnyFunSuite {

  private val nOut = 9

//  private def encodedState(xToMove: Boolean): Array[Double] = {
//    // 27-length encoding; plane-2 (indices 18..26) marks X-to-move with ones
//    val a = Array.fill(27)(0.0)
//    a(18) = if (xToMove) 1.0 else 0.0
//    a
//  }

  private def pendingSample(xToMove: Boolean): PendingTrainingSample[TttBoard, TttMove] = {
    val validMoves = List(TttMove(0, 0), TttMove(1, 1), TttMove(2, 2))
    val visitCounts:Map[TttMove,Int] = validMoves.map(m => m-> 1).toMap
    val who = if (xToMove) Player.xPlayer else Player.oPlayer
    val gs = GameState[TttBoard, TttMove](TttBoard(Array.fill(3, 3)(0)), who, None)
    
    PendingTrainingSample(
      gameState = gs,
      validMoves = validMoves,
      visitCounts = visitCounts,
      moveChosen = Some( TttMove(0, 0)),
      gameResult = None,
    )
  }

  test("finalizePendingWith assigns +1 X-perspective for X win") {
    val col = new ExperienceCollector[TttBoard, TttMove]()

    col.beginEpisode()
    col.collect(pendingSample(xToMove = true)) // X to move
    col.collect(pendingSample(xToMove = false)) // O to move

    col.finalizePendingWith(GameResult.xWin) // X wins

    val data: Array[FinalizedTrainingSample[TttBoard, TttMove]] = col.getFinalizedSamplesAndClear()
    assert(data.length == 2)

    // X-perspective numeric target derived from gameResult
    data.foreach { s =>
      val v = s.gsValue
      assert(v == 1.0, s"Expected +1.0 for X win, got $v")
    }
  }

  test("finalizePendingWith assigns 0.0 X-perspective for draw") {
    val col = new ExperienceCollector[TttBoard, TttMove]()

    col.beginEpisode()
    col.collect(pendingSample(xToMove = true))
    col.collect(pendingSample(xToMove = false))

    col.finalizePendingWith(GameResult.draw) // draw

    val data = col.getFinalizedSamplesAndClear()
    assert(data.nonEmpty)
    data.foreach { s =>
      val v = s.gsValue
      assert(v == 0.0, s"Expected 0.0 for draw, got $v")
    }
  }


}