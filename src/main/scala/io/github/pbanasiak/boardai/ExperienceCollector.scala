package io.github.pbanasiak.boardai


import io.github.pbanasiak.boardai.nn.{FinalizedTrainingSample, PendingTrainingSample, TrainingSample}

import scala.collection.mutable.{ArrayBuffer, ListBuffer}

// experience collector operates on game domain, not encoded.
final class ExperienceCollector[B, M] {

  private val pending = ArrayBuffer[PendingTrainingSample[B, M]]()

  // TODO finalized <> encoded, encoding belong to training, not collector
  private val finalized = ArrayBuffer[FinalizedTrainingSample[B, M]]()

  def beginEpisode(): Unit = pending.clear()

  // Accepts either PendingTrainingSample or FinalizedTrainingSample
  def collect(sample: PendingTrainingSample[B, M]): Unit = {
    pending += sample
  }

  // set game result for all pending samples without result.
  def finalizePendingWith(result: GameResult.Value): Unit = {
    if (pending.nonEmpty) {
      finalized ++= pending.iterator.map { ps =>
        ps.gameResult match {
          case Some(gr) =>
            FinalizedTrainingSample(
              gameState = ps.gameState,
              validMoves = ps.validMoves,
              visitCounts = ps.visitCounts,
              moveChosen = ps.moveChosen,
              gsValue = GameResult.toXValue(gr)
            )
          case None => //ps.copy(gameResult = Some(result))
            FinalizedTrainingSample(
              gameState = ps.gameState,
              validMoves = ps.validMoves,
              visitCounts = ps.visitCounts,
              moveChosen = ps.moveChosen,
              gsValue = GameResult.toXValue(result)
            )

        }
      }
      pending.clear()
    }
  }

  def finalizePendingWithDiscount(result: GameResult.Value, discountFactor: Double = 0.98): Unit = {
    if (pending.nonEmpty) {
      val baseValue: Double = GameResult.toXValue(result)
      val reversedPending = pending.reverse // Last move gets full value

      finalized ++= reversedPending.zipWithIndex.map { case (ps, stepsFromEnd) =>
        ps.gameResult match {
          case Some(gr) =>
          // Already has terminal value
            FinalizedTrainingSample(
              gameState = ps.gameState,
              validMoves = ps.validMoves,
              visitCounts = ps.visitCounts,
              moveChosen = ps.moveChosen,
              gsValue = GameResult.toXValue(gr),
            )

          case None =>
            val discountedValue: Double = baseValue * math.pow(discountFactor, stepsFromEnd)
            FinalizedTrainingSample(
              gameState = ps.gameState,
              validMoves = ps.validMoves,
              visitCounts = ps.visitCounts,
              moveChosen = ps.moveChosen,
              gsValue = discountedValue,
            )
        }
      }
      pending.clear()
    }
  }

  def getFinalizedSamples(): Array[FinalizedTrainingSample[B, M]] = {
    val arr = finalized.toArray
    arr
  }

  def getFinalizedSamplesAndClear(): Array[FinalizedTrainingSample[B, M]] = {
    val arr = finalized.toArray
    finalized.clear()
    arr
  }

  /** How many finalized samples are available. */
  def size: Int = finalized.size

}