package io.github.pbanasiak.boardai.tictactoe.nn

/** Model-only hyperparameters and IO settings. */
case class TttModelConfig(
  hiddenSize: Int = 128,
  learningRate: Double = 0.01,
  valueLossWeight: Double = 0.5,
  trainingEpochs: Int = 10, // Move this here from TttTrainingConfig
  modelBasePath: String = "models/tictactoe_trained",
  initSeed: Long = 42 // seed for model initialization
)


object TttPresets {

  object Quick {
    val model: TttModelConfig = TttModelConfig(
      hiddenSize = 64,
      learningRate = 0.001,
      valueLossWeight = 0.3,
      trainingEpochs = 12,
      modelBasePath = "models/tictactoe_quick",
      initSeed = 42
    )
    val training: TttTrainingConfig = TttTrainingConfig(
      iterations = 10,
      gamesPerIteration = 50,
      mctsRoundsPerMove = 100,
      puctCoeff = 2.0,
      mctsSelectRandomly = true,
      evalMctsRounds = 10,
      evalPuctCoeff = 2.0,
      evalMctsRandomSelection = false,
      evaluateEveryNIterations = 5,
      trainingSeed = 42,
      evaluationSeed = 123
    )
  }

  object Debug {
    val model: TttModelConfig = TttModelConfig(
      hiddenSize = 32,
      learningRate = 0.01,
      valueLossWeight = 0.5,
      trainingEpochs = 2,
      modelBasePath = "models/tictactoe_debug",
      initSeed = 42
    )
    val training: TttTrainingConfig = TttTrainingConfig(
      iterations = 5,
      gamesPerIteration = 5,
      mctsRoundsPerMove = 20,
      puctCoeff = 2.0,
      mctsSelectRandomly = true,
      evalMctsRounds = 10,
      evalPuctCoeff = 2.0,
      evalMctsRandomSelection = false,
      evaluateEveryNIterations = 2,
      trainingSeed = 42,
      evaluationSeed = 123
    )
  }
}
