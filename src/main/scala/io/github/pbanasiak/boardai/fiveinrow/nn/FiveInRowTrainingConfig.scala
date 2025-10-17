package io.github.pbanasiak.boardai.fiveinrow.nn

/**
 * Training/runtime settings for Five-in-Row (self-play, MCTS, eval, seeds).
 */
case class FiveInRowTrainingConfig(
  gamesPerIteration: Int = 20,

  // MCTS parameters for training
  mctsRoundsPerMove: Int = 200,
  puctCoeff: Double = 2.0,
  mctsSelectRandomly: Boolean = true,

  // MCTS parameters for evaluation
  evalMctsRounds: Int = 400,
  evalPuctCoeff: Double = 2.0,
  evalMctsRandomSelection: Boolean = false,

  // Evaluation frequency
  evaluateEveryNIterations: Int = 1,

  // Random seeds
  trainingSeed: Long = 42,
  evaluationSeed: Long = 123
)
