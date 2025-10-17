package io.github.pbanasiak.boardai

// Value types for semantic clarity
opaque type Score = Double

object Score {
  def apply(value: Double): Score = value

  extension (s: Score) {
    def value: Double = s
    // def toProbability: WinProbability is Game Dependent Chess <> TicTacToe
    def unary_- : Score = Score(-s.value)
    def +(other: Score): Score = Score((s.value + other.value).max(-1.0).min(1.0))
  }

}

opaque type WinProbability = Double

object WinProbability {
  def apply(value: Double): WinProbability = {
    require(value >= 0.0 && value <= 1.0, s"WinProbability must be in [0,1], got $value")
    value
  }

  extension (p: WinProbability) {
    def value: Double = p
  }
}
