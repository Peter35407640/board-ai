// language: scala
package io.github.pbanasiak.boardai

import scala.util.Random

/**
 * Wrapper Game that delegates all operations to an underlying game instance
 * but returns validMoves in random order. Useful when you want nondeterministic
 * move ordering without changing existing Game implementations.
 *
 * Example:
 *   val base: Game[Board, Move] = new FiveInRowGame(config)
 *   val randGame = new RandomOrderGame(base)
 *   // use randGame wherever a Game is expected
 */
class RandomOrderGame[B, M](underlying: Game[B, M], seed: Long = System.currentTimeMillis()) extends Game[B, M] {
  private val rnd = new Random(seed)

  // Delegate other members
  override val winScore: Score = underlying.winScore
  override val drawScore: Score = underlying.drawScore

  override def validMoves(gameState: GameState[B, M]): Seq[M] = {
    val moves = underlying.validMoves(gameState)
    rnd.shuffle(moves)
  }

  override def applyMove(gameState: GameState[B, M], move: M): GameState[B, M] =
    underlying.applyMove(gameState, move)

  override def initialState(): GameState[B, M] = underlying.initialState()
}
