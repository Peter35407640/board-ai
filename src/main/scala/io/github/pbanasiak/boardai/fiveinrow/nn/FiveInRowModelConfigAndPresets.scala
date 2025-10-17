package io.github.pbanasiak.boardai.fiveinrow.nn

/*  For 15x15 or larger boards, consider:
FiveInRowModelConfig(
  hiddenSize = 256,           // Scale with board size
  learningRate = 0.001,       // Lower for stability
  trainingEpochs = 20         // More epochs for larger networks
)*/


/** Model-only hyperparameters and IO settings for Five-in-Row. */
case class FiveInRowModelConfig(
  boardSize: Int,

  hiddenSize: Int = 128,
  learningRate: Double = 0.01,
  trainingEpochs: Int = 10,
  initSeed: Long = 42,

) {
  def modelPathForSize: String = s"models/fiveinrow_${boardSize}x${boardSize}"

}

object FiveInRowPresets {
  def configForBoardSize(boardSize: Int): (FiveInRowModelConfig, FiveInRowTrainingConfig) = {
    val hiddenSize = boardSize match {
      case 7 => 128
      case 10 => 256
      case 15 => 512
      case _ => math.max(128, boardSize * boardSize * 2)
    }


    val model = FiveInRowModelConfig(
      boardSize = boardSize,
      hiddenSize = hiddenSize,
      learningRate = 0.001,
      trainingEpochs = 5,
      initSeed = 42,
    )

    val training = FiveInRowTrainingConfig(
      gamesPerIteration = 10,

      // MCTS parameters for training
      mctsRoundsPerMove = 20000,
      puctCoeff = 8.0,
      mctsSelectRandomly = true,
      
      evalMctsRounds = 400,
      evalPuctCoeff = 2.0,
      evalMctsRandomSelection = false,
      evaluateEveryNIterations = 5,
      trainingSeed = 42,
      evaluationSeed = 123
    )

    (model, training)
  }
}
