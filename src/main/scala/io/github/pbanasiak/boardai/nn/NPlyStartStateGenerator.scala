package io.github.pbanasiak.boardai.nn

import io.github.pbanasiak.boardai.{Game, GameState, Player}

import scala.util.Random

/**
 * Generic N-ply start-state generator.
 *
 * Expands seeds breadth-first up to `plyDepth` plies (half-moves), collecting non-terminal states.
 * Useful for generating diverse mid-game starting positions (e.g., 2-ply: all X moves, then all O replies).
 *
 * Deduplication: by (board, playerToMove) using case-class/equals of the board type B.
 * If your board type B provides structural equals/hashCode (like TicTacToe board does), this is safe.
 */
object NPlyStartStateGenerator {

  /**
   * Expand from a set of seed states for exactly `plyDepth` plies, collecting resulting non-terminal states.
   *
   * @param game        game rules
   * @param seeds       starting states (usually 1 = initial, or a small pool observed in self-play)
   * @param plyDepth    number of plies (half-moves) to expand; e.g., 2 = X move then O reply
   * @param maxPerSeed  optionally cap how many states to collect per seed (after full expansion)
   * @param rng         random for sampling/truncation when capping
   * @tparam B          board type
   * @tparam M          move type
   * @return            collected non-terminal states at exactly `plyDepth` plies from the seeds
   */
  def expandNPly[B, M](
    game: Game[B, M],
    seeds: Seq[GameState[B, M]],
    plyDepth: Int,
    maxPerSeed: Option[Int] = None,
    rng: Random = new Random()
  ): Seq[GameState[B, M]] = {
    require(plyDepth >= 1, s"plyDepth must be >= 1, got $plyDepth")

    seeds.flatMap { seed =>
      val collected = expandOneSeedNPly(game, seed, plyDepth)
      maxPerSeed match {
        case Some(k) if collected.size > k =>
          rng.shuffle(collected.toList).take(k)
        case _ =>
          collected
      }
    }
  }

  /**
   * Convenience: two-ply expansion (current side moves, then opponent moves).
   */
  def expandTwoPly[B, M](
    game: Game[B, M],
    seed: GameState[B, M]
  ): Seq[GameState[B, M]] = {
    expandOneSeedNPly(game, seed, plyDepth = 2)
  }

  /**
   * Convenience: from the initial state, generate all 2-ply states (all X moves, then all O replies).
   */
  def twoPlyFromInitial[B, M](game: Game[B, M]): Seq[GameState[B, M]] = {
    expandTwoPly(game, game.initialState())
  }

  // ---------- internals ----------

  private final case class Key[B](board: B, player: Player)

  private def expandOneSeedNPly[B, M](
    game: Game[B, M],
    seed: GameState[B, M],
    plyDepth: Int
  ): Seq[GameState[B, M]] = {
    import scala.collection.mutable

    var frontier: Vector[GameState[B, M]] = Vector(seed)
    var depth = 0

    if (seed.isOver) {
      return Vector.empty
    }

    while (depth < plyDepth) {
      val next = mutable.ArrayBuffer[GameState[B, M]]()
      val seen = mutable.HashSet[Key[B]]()

      frontier.foreach { gs =>
        if (!gs.isOver) {
          val moves = game.validMoves(gs)
          moves.foreach { m =>
            val ns = game.applyMove(gs, m)
            if (!ns.isOver) {
              val k = Key(ns.board, ns.playerToMove)
              if (!seen.contains(k)) {
                seen += k
                next += ns
              }
            }
          }
        }
      }

      frontier = next.toVector
      depth += 1
    }

    frontier
  }
}