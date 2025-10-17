package io.github.pbanasiak.boardai

object GameResult extends Enumeration {
  val draw, xWin, oWin = Value

  // Map result to scalar from X’s perspective
  def toXValue(result: GameResult.Value): Double = result match {
    case GameResult.xWin => 1.0
    case GameResult.oWin => -1.0
    case GameResult.draw => 0.0
  }
  
}