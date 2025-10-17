package io.github.pbanasiak.boardai.nn

import org.scalactic.Tolerance.convertNumericToPlusOrMinusWrapper
import org.scalatest.funsuite.AnyFunSuiteLike

class PolicyPriorUtilsTest extends AnyFunSuiteLike {

  // Generic move type for testing - just needs an index
  case class TestMove(index: Int)

  private def moveToIndex(move: TestMove): Int = move.index
  private val tolerance = 1e-12

  test("mask extracts probabilities for valid moves using moveToIndex") {
    val policy = Array(0.10, 0.20, 0.30, 0.00, 0.15, 0.05, 0.02, 0.08, 0.10)
    val validMoves = Seq(TestMove(0), TestMove(5), TestMove(7)) // indices 0, 5, 7

    val masked = PolicyPriorUtils.mask(policy, validMoves, moveToIndex)
    val map = masked.toMap

    assert(map(TestMove(0)) === 0.10 +- tolerance)
    assert(map(TestMove(5)) === 0.05 +- tolerance)
    assert(map(TestMove(7)) === 0.08 +- tolerance)
    assert(masked.size == validMoves.size)
  }

  test("mask clamps negatives and NaN/Inf to zero") {
    val policy = Array.fill(9)(0.0)
    policy(0) = -0.5
    policy(1) = Double.NaN
    policy(2) = Double.PositiveInfinity
    policy(3) = 0.4

    val validMoves = Seq(TestMove(0), TestMove(1), TestMove(2), TestMove(3))
    val masked = PolicyPriorUtils.mask(policy, validMoves, moveToIndex).toMap

    assert(masked(TestMove(0)) == 0.0)
    assert(masked(TestMove(1)) == 0.0)
    assert(masked(TestMove(2)) == 0.0)
    assert(masked(TestMove(3)) == 0.4)
  }

  test("mask handles out-of-bounds indices") {
    val policy = Array(0.1, 0.2, 0.3)
    val validMoves = Seq(TestMove(1), TestMove(5), TestMove(-1)) // 5 and -1 are out of bounds

    val masked = PolicyPriorUtils.mask(policy, validMoves, moveToIndex).toMap

    assert(masked(TestMove(1)) === 0.2 +- tolerance)
    assert(masked(TestMove(5)) === 0.0) // out of bounds
    assert(masked(TestMove(-1)) === 0.0) // negative index
  }

  test("renormalize divides by sum when sum equals 1.0") {
    val pairs = Seq(
      TestMove(0) -> 0.2,
      TestMove(1) -> 0.3,
      TestMove(2) -> 0.5
    )
    val norm = PolicyPriorUtils.renormalize(pairs)

    assert(norm.values.sum === 1.0 +- tolerance)
    assert(norm(TestMove(0)) === 0.2 +- tolerance)
    assert(norm(TestMove(1)) === 0.3 +- tolerance)
    assert(norm(TestMove(2)) === 0.5 +- tolerance)
  }

  test("renormalize scales down when sum > 1.0") {
    val pairs = Seq(
      TestMove(0) -> 1.5,
      TestMove(1) -> 2.0,
      TestMove(2) -> 1.5
    ) // sum = 5.0
    val norm = PolicyPriorUtils.renormalize(pairs)

    assert(norm.values.sum === 1.0 +- tolerance)
    assert(norm(TestMove(0)) === 0.3 +- tolerance) // 1.5/5.0
    assert(norm(TestMove(1)) === 0.4 +- tolerance) // 2.0/5.0
    assert(norm(TestMove(2)) === 0.3 +- tolerance) // 1.5/5.0
  }

  test("renormalize scales up when sum < 1.0") {
    val pairs = Seq(
      TestMove(0) -> 0.2,
      TestMove(1) -> 0.3,
      TestMove(2) -> 0.3
    ) // sum = 0.8
    val norm = PolicyPriorUtils.renormalize(pairs)

    assert(norm.values.sum === 1.0 +- tolerance)
    assert(norm(TestMove(0)) === 0.25 +- tolerance) // 0.2/0.8
    assert(norm(TestMove(1)) === 0.375 +- tolerance) // 0.3/0.8
    assert(norm(TestMove(2)) === 0.375 +- tolerance) // 0.3/0.8
  }

  test("renormalize falls back to uniform when sum <= 0 or NaN/Inf") {
    val pairsZero = Seq(TestMove(0) -> 0.0, TestMove(1) -> 0.0)
    val normZero = PolicyPriorUtils.renormalize(pairsZero)
    normZero.values.foreach(p => assert(p === 0.5 +- tolerance))

    val pairsNaN = Seq(TestMove(0) -> Double.NaN, TestMove(1) -> 0.0, TestMove(2) -> 0.0)
    val normNaN = PolicyPriorUtils.renormalize(pairsNaN)
    val expectedUniform = 1.0 / 3.0
    normNaN.values.foreach(p => assert(p === expectedUniform +- tolerance))

    val pairsInf = Seq(TestMove(0) -> Double.PositiveInfinity, TestMove(1) -> 0.0)
    val normInf = PolicyPriorUtils.renormalize(pairsInf)
    normInf.values.foreach(p => assert(p === 0.5 +- tolerance))

    val empty = PolicyPriorUtils.renormalize(Seq.empty)
    assert(empty.isEmpty)
  }

  test("maskAndRenormalize masks to valid moves then renormalizes") {
    val policy = Array(0.10, 0.20, 0.30, 0.00, 0.15, 0.05, 0.02, 0.08, 0.10)
    val validMoves = Seq(TestMove(0), TestMove(2), TestMove(4)) // indices 0, 2, 4

    val priors = PolicyPriorUtils.maskAndRenormalize(policy, validMoves, moveToIndex)

    assert(priors.values.sum === 1.0 +- tolerance)

    // Proportions should match original ratios among valid indices: 0.10 : 0.30 : 0.15
    val total = 0.10 + 0.30 + 0.15
    assert(priors(TestMove(0)) === 0.10 / total +- tolerance)
    assert(priors(TestMove(2)) === 0.30 / total +- tolerance)
    assert(priors(TestMove(4)) === 0.15 / total +- tolerance)
  }
}