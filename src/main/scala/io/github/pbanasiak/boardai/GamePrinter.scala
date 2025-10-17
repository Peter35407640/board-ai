package io.github.pbanasiak.boardai

trait GamePrinter[B,M] {
  def printDraw(board: B, playerToMove: Player): Unit

  def printVictory(board: B, playerToMove: Player): Unit

  def print(gameState: GameState[B, M]): Unit
}
