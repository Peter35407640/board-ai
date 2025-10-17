package io.github.pbanasiak.boardai.fiveinrow.util

import io.github.pbanasiak.boardai
import io.github.pbanasiak.boardai.{GameState, Player}
import io.github.pbanasiak.boardai.fiveinrow.{Board, Move}

class GamePrinter() extends boardai.GamePrinter[Board, Move] {
  def printDraw(board: Board, playerToMove: Player): Unit = {
    println(s"${playerToMove} Draw with stones: ${playerToMove.getPlayerStone()}")
    println(s"${board}\n")
  }

  def printVictory(board: Board, playerToMove: Player): Unit = {
    println()
    println(s"${playerToMove} Victory with stones: ${playerToMove.getPlayerStone()}")
    println(s"${board}\n")
  }

  def print(gameState: GameState[Board, Move]): Unit = {
    println(s"to move: ${gameState.playerToMove} with stones: ${gameState.playerToMove.getPlayerStone()}")
    println(s"${gameState.board}\n")
  }
}
