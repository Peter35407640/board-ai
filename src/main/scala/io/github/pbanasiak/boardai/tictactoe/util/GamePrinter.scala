package io.github.pbanasiak.boardai.tictactoe.util

import io.github.pbanasiak.boardai
import io.github.pbanasiak.boardai.{GameState, Player}
import io.github.pbanasiak.boardai.tictactoe.{TttBoard, TttMove}

class GamePrinter() extends boardai.GamePrinter[TttBoard, TttMove] {
  def printDraw(board: TttBoard, playerToMove: Player): Unit = {
    println(s"${playerToMove} Draw with stones: ${playerToMove.getPlayerStone()}")
    println(s"${board}\n")
  }

  def printVictory(board: TttBoard, playerToMove: Player): Unit = {
    println()
    println(s"${playerToMove} Victory with stones: ${playerToMove.getPlayerStone()}")
    println(s"${board}\n")
  }

  def print(gameState: GameState[TttBoard, TttMove]): Unit = {
    println(s"to move: ${gameState.playerToMove} with stones: ${gameState.playerToMove.getPlayerStone()}")
    println(s"${gameState.board}\n")
  }
}
