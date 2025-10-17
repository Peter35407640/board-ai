package io.github.pbanasiak.boardai.tictactoe.agent

import io.github.pbanasiak.boardai.agent.AlphaBetaAgent
import io.github.pbanasiak.boardai.tictactoe.*
import io.github.pbanasiak.boardai.{Agent, GameState, Player, Score, ScoredMove}

import scala.collection.mutable

/**
 * Perfect play agent with memoization cache.
 * TicTacToe space is small enough that all positions can be cached.
 */
class TttAgentWithCache(talk: Boolean) extends Agent[TttBoard, TttMove] {

  private val tttGame = new TttGame()

  case class Key(board: List[Int], playerToMove: Player)

  def toKey(gameState: GameState[TttBoard, TttMove]): Key = {
    // More robust key generation
    val boardState = for {
      r <- 0 until 3
      c <- 0 until 3
    } yield gameState.board.getInt(r, c)

    Key(boardState.toList, gameState.playerToMove)
  }

  private val perfectAgent = new AlphaBetaAgent(tttGame)
  private val cache = mutable.HashMap[Key, ScoredMove[TttMove]]()

  def selectScoredMove(gameState: GameState[TttBoard, TttMove]): ScoredMove[TttMove] = {
    val key = toKey(gameState)
    cache.get(key) match {
      case Some(cachedResult) =>
        if (talk) println(s"Cache hit: ${cachedResult.score} ${cachedResult.move}")
        cachedResult

      case None =>
        val validMoves = tttGame.validMoves(gameState)
        require(validMoves.nonEmpty, "Cannot select move from terminal game state")

        val (score, bestMove): (Score, Option[TttMove]) = perfectAgent.negamax(gameState)
        val bestScoredMove = ScoredMove(score, bestMove.get)
        cache.put(key, ScoredMove(score, bestMove.get))
        if (talk) println(s"Cache miss, computed: ${score} ${bestMove.get}")
        bestScoredMove
    }
  }

  override def selectMove(gameState: GameState[TttBoard, TttMove]): TttMove = {
    val scoredMove = selectScoredMove(gameState)
    scoredMove.move
  }

  // Useful for debugging and statistics
  def cacheStats: (Int, Double) = {
    val size = cache.size
    val maxPossiblePositions = 3 * 3 * 9 * 8 * 7 * 6 * 5 * 4 * 3 * 2 * 1 // Very rough upper bound
    val coverage = size.toDouble / 5478 * 100 // Actual number of reachable positions
    (size, coverage)
  }
}