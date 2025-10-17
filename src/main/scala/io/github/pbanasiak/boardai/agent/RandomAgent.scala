package io.github.pbanasiak.boardai.agent

import io.github.pbanasiak.boardai.{Agent, Game, GameState}

import scala.util.Random

class RandomAgent[B, M](game: Game[B, M], rnd: Random) extends Agent[B, M] {

  override def selectMove(gameState: GameState[B, M]): M = {
    val isTerminated = gameState.isOver
    if (isTerminated) throw new Exception(s"game terminated already gameState.board: ${gameState.board}")
    val validMoves = game.validMoves(gameState)
    val shuffled: Seq[M] = rnd.shuffle(validMoves)

    shuffled.head
  }

}
