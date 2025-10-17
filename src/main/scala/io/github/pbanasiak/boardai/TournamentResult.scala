package io.github.pbanasiak.boardai

case class TournamentResult(wins: Int, loss: Int, draw: Int)

class WinLossCollector() {

  private var wins = 0
  private var lost = 0
  private var draws = 0

  // TODO revisit that
  def add(score: Int) = {
    score match {
      case 1 => wins += 1
      case -1 => lost += 1
      case 0 => draws += 1
      case any => throw new Exception(s"boom unexpected score $score")
    }
  }

  def results() = TournamentResult(wins, lost, draws)
}
