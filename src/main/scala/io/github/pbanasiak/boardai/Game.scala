package io.github.pbanasiak.boardai

trait Game[B, M] {
  def validMoves(gameState: GameState[B, M]): Seq[M]
  def applyMove(gameState: GameState[B, M], move: M): GameState[B, M]

  def initialState(): GameState[B, M]

  // Optional: might also include other game-specific operations
  val winScore: Score
  val drawScore: Score
}
