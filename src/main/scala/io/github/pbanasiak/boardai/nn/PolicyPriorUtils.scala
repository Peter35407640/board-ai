package io.github.pbanasiak.boardai.nn

/**
 * Utilities for turning a raw policy vector into valid-move priors.
 * Kept separate from NodeCreator for easy unit testing.
 */
object PolicyPriorUtils {

  /**
   * Extracts and clamps policy scores for valid moves from a neural network policy output.
   *
   * Takes raw policy scores (which can be any real numbers) and filters them to only include
   * valid moves, while ensuring all values are non-negative. Invalid indices return 0.0,
   * and NaN/Infinite values are clamped to 0.0. Negative values are clamped to 0.0.
   *
   * Note: This does NOT normalize the scores to probabilities (sum to 1.0). The returned
   * values are non-negative scores that may sum to any value > 0. Use `renormalize` or 
   * `maskAndRenormalize` for proper probability distributions.
   *
   * @param policyScores Raw policy scores from neural network output (can be any real values)
   * @param validMoves   Sequence of valid moves for the current game state
   * @param moveToIndex  Function to convert a move to its corresponding array index
   * @return Sequence of (move, clampedScore) pairs where scores are non-negative
   */
  def mask[Move](
    policyScores: Array[Double],
    validMoves: Seq[Move],
    moveToIndex: Move => Int
  ): Seq[(Move, Double)] = {
    validMoves.map { move =>
      val moveIndex = moveToIndex(move)
      val probability =
        if (moveIndex >= 0 && moveIndex < policyScores.length) {
          val rawScore = policyScores(moveIndex)
          if (rawScore.isNaN || rawScore.isInfinite) 0.0 else math.max(rawScore, 0.0)
        } else 0.0
      move -> probability
    }
  }

  /** Renormalize probabilities; if degenerate, fall back to uniform over provided moves. */
  def renormalize[Move](pairs: Seq[(Move, Double)]): Map[Move, Double] = {
    if (pairs.isEmpty) Map.empty
    else {
      val sum = pairs.map(_._2).sum
      if (sum > 0.0 && !sum.isNaN && !sum.isInfinite)
        pairs.map { case (m, p) => m -> (p / sum) }.toMap
      else {
        val u = 1.0 / pairs.size
        pairs.map { case (m, _) => m -> u }.toMap
      }
    }
  }

  /** Convenience: mask then renormalize. */
  def maskAndRenormalize[Move](
    policy: Array[Double],
    validMoves: Seq[Move],
    moveToIndex: Move => Int
  ): Map[Move, Double] = renormalize(mask(policy, validMoves, moveToIndex))
}
