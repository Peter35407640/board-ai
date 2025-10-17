package io.github.pbanasiak.boardai

trait Agent[B, M] {
  def selectMove(gameState:GameState[B, M]):M
}
