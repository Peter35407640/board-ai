package io.github.pbanasiak.boardai.tictactoe.nn

/**
 * Training/runtime settings (self-play, MCTS, eval, seeds).
 */
case class TttTrainingConfig(
  iterations: Int = 100,
  gamesPerIteration: Int = 50,

  // MCTS parameters for training
  mctsRoundsPerMove: Int = 100,
  puctCoeff: Double = 2.0,
  mctsSelectRandomly: Boolean = true,

  // MCTS parameters for evaluation
  evalMctsRounds: Int = 200,
  evalPuctCoeff  : Double = 2.0,
  evalMctsRandomSelection: Boolean = false,

  // Evaluation frequency
  evaluateEveryNIterations: Int = 10,

  // Random seeds
  trainingSeed: Long = 42,
  evaluationSeed: Long = 123
)
