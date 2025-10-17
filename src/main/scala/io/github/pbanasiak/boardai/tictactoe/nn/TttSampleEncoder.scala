package io.github.pbanasiak.boardai.tictactoe.nn

import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.factory.Nd4j
import io.github.pbanasiak.boardai.fiveinrow.{Board, Move}
import io.github.pbanasiak.boardai.nn.{EncodedTrainingSample, FinalizedTrainingSample, GameStateEncoder, PendingTrainingSample}
import io.github.pbanasiak.boardai.tictactoe.{TttBoard, TttMove}
import io.github.pbanasiak.boardai.{GameResult, Player, SampleEncoder}

class TttSampleEncoder(
  encoder: GameStateEncoder[TttBoard, TttMove, Array[Double]]
) extends SampleEncoder[TttBoard, TttMove, Array[Double]] {

  /** Normalize visit counts into probabilities, zeroing illegal moves. */
  private def normalizeCounts(counts: Array[Int], legalMask: Array[Boolean]): Array[Double] = {
    val legalIdx: Array[Int] = legalMask.iterator.zipWithIndex.collect { case (true, i) => i }.toArray
    val legalSum = legalIdx.foldLeft(0)((acc, i) => acc + counts(i))

    if (legalSum == 0) {
      val p = if (legalIdx.nonEmpty) 1.0 / legalIdx.length else 0.0
      Array.tabulate(counts.length)(i => if (legalMask(i)) p else 0.0)
    } else {
      val denom = legalSum.toDouble
      Array.tabulate(counts.length)(i => if (legalMask(i)) counts(i) / denom else 0.0)
    }
  }

  override def encode(
    finalized: FinalizedTrainingSample[TttBoard, TttMove],
  ): EncodedTrainingSample[Array[Double]] = {


    val legalMask: Array[Boolean] = {
      val a = Array.fill(encoder.nOutput)(false)
      finalized.validMoves.foreach(m => a(encoder.moveToIndex(m)) = true)
      a
    }

    val visitCounts: Array[Int] = {
      val counts: Array[Int] = Array.fill(encoder.nOutput)(0)
      finalized.visitCounts.foreach { case (move, visitCount) =>
        counts(encoder.moveToIndex(move)) = visitCount
      }
      counts
    }

    val countsArray: Array[Double] = normalizeCounts(visitCounts, legalMask)

    val encodedState: Array[Double] = encoder.encodeGameState(finalized.gameState)
    EncodedTrainingSample[Array[Double]](
      gameState = encodedState,
      policyTarget = countsArray,
      playerToMove = finalized.gameState.playerToMove,
      gsValue = finalized.gsValue
    )
  }


}
