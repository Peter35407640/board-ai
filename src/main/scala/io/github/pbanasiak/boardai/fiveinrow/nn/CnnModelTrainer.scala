
package io.github.pbanasiak.boardai.fiveinrow.nn

import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.factory.Nd4j
import io.github.pbanasiak.boardai.agent.{FixedDepthAlphaBetaAgent, RandomAgent}
import io.github.pbanasiak.boardai.fiveinrow.{Board, FiveInRowGame, FiveInRowSampleEncoder, GameConfig, Move}
import io.github.pbanasiak.boardai.fiveinrow.encoders.Planes3FiveInRowEncode
import io.github.pbanasiak.boardai.mcts.{MctsAgent, MctsNnAgent, MctsNnNodeCreator, MctsNodeCreator, MonteCarloNnPlayer, MonteCarloPlayer}
import io.github.pbanasiak.boardai.nn.{EncodedTrainingSample, FinalizedTrainingSample, NPlyStartStateGenerator, PendingTrainingSample}
import io.github.pbanasiak.boardai.{Agent, ExperienceCollector, GameResult, GameState, Player, RandomOrderGame, SampleEncoder, Tournament}

import scala.util.Random

/**
 * CNN-specific Five-in-Row trainer
 */
object CnnModelTrainer {
  val evalDepthLimit = 1

  private def currentDepth(iter: Int) = 0


  /**
   * Generate simple tactical puzzles: positions where one player must block or lose
   */
  private def generateBlockingPuzzles(gameConfig: GameConfig): Seq[GameState[Board, Move]] = {
    val puzzles = scala.collection.mutable.ArrayBuffer[GameState[Board, Move]]()
    val boardSize = gameConfig.boardSize

    // Three-in-a-row horizontally (must block both ends)
    for (row <- 1 until boardSize - 1; col <- 1 until boardSize - 4) {
      val board = Array.fill(boardSize, boardSize)(0)
      // X has three in a row: . X X X .
      board(row)(col + 1) = Player.xStone
      board(row)(col + 2) = Player.xStone
      board(row)(col + 3) = Player.xStone
      // O must block at col or col+4
      puzzles += new GameState(new Board(board), Player.oPlayer, None)
    }

    // Three-in-a-row vertically
    for (row <- 1 until boardSize - 4; col <- 1 until boardSize - 1) {
      val board = Array.fill(boardSize, boardSize)(0)
      board(row + 1)(col) = Player.xStone
      board(row + 2)(col) = Player.xStone
      board(row + 3)(col) = Player.xStone
      puzzles += new GameState(new Board(board), Player.oPlayer, None)
    }

    // Four-in-a-row (immediate loss if not blocked)
    for (row <- 1 until boardSize - 1; col <- 0 until boardSize - 5) {
      val board = Array.fill(boardSize, boardSize)(0)
      // X has four in a row: X X X X .
      board(row)(col) = Player.xStone
      board(row)(col + 1) = Player.xStone
      board(row)(col + 2) = Player.xStone
      board(row)(col + 3) = Player.xStone
      // O MUST block at col+4 or lose immediately
      puzzles += new GameState(new Board(board), Player.oPlayer, None)
    }

    puzzles.toSeq
  }

  private def trainOnPuzzle(
    puzzle: GameState[Board, Move],
    game: FiveInRowGame,
    model: FiveInRowCnnPolicyValueModel,
    encoder: Planes3FiveInRowEncode,
    sampleEncoder: SampleEncoder[Board, Move, INDArray],
    gameConfig: GameConfig
  ): Unit = {
    val validMoves = game.validMoves(puzzle)

    // Find which moves are blocking moves
    val blockingMoves = validMoves.filter { move =>
      val nextState = game.applyMove(puzzle, move)
      // Check if this blocks a threat by seeing if X can win immediately after
      val xCanWinAfter = game.validMoves(nextState).exists { xMove =>
        val afterX = game.applyMove(nextState, xMove)
        afterX.isOver && afterX.isVictoryOf(Player.xPlayer)
      }
      !xCanWinAfter // Good move = X can't win immediately
    }

    if (blockingMoves.nonEmpty) {
      val totalBlockingMoves = blockingMoves.size
      val boardSize = gameConfig.boardSize
      val fullPolicySize = boardSize * boardSize
      val targetPolicy = Array.fill(fullPolicySize)(0.0)

      // Less extreme probabilities
      val blockingProb = 0.7 / totalBlockingMoves  // Reduced from 0.8
      val nonBlockingProb = if (validMoves.size > totalBlockingMoves) {
        0.3 / (validMoves.size - totalBlockingMoves)
      } else {
        0.0
      }

      validMoves.foreach { move =>
        val index = move.r * boardSize + move.c
        if (blockingMoves.contains(move)) {
          targetPolicy(index) = blockingProb
        } else {
          targetPolicy(index) = nonBlockingProb
        }
      }

      val encoded = encoder.encodeGameState(puzzle)
      val policyTarget = Nd4j.create(targetPolicy)
      val valueTarget = -0.3  // Changed from -0.9 to -0.3 (much less extreme!)

      try {
        model.trainBatch(
          Array(encoded),
          Array(policyTarget),
          Array(valueTarget),
          epochs = 1  // Changed from 5 to 1
        )
      } catch {
        case e: IllegalArgumentException =>
          println(s"CNN training failed: ${e.getMessage}")
          e.printStackTrace()
      }
    }
  }

  private def mkExploitAgent(game: FiveInRowGame,
    model: FiveInRowCnnPolicyValueModel,
    trainingConfig: FiveInRowTrainingConfig,
    gameConfig: GameConfig,
    iter: Int,
    collector: ExperienceCollector[Board, Move],
  ): Agent[Board, Move] = {

    val encoder = new Planes3FiveInRowEncode(gameConfig)
    val nodeNnCreator = new MctsNnNodeCreator(game, encoder, model)


    val exploit = new MctsNnAgent[Board, Move](
      game,
      nodeCreator = nodeNnCreator,
      mcPlayer = new MonteCarloNnPlayer(game, nodeNnCreator, encoder, model, currentDepth(iter)),
      collector = Some(collector),
      roundsPerMove = trainingConfig.mctsRoundsPerMove,
      puctCoeff = trainingConfig.puctCoeff,
      rand = new Random(),
      selectRandomly = true,
      debug = false
    )

    exploit
  }

  private def mkExploreAgent(game: FiveInRowGame,
    model: FiveInRowCnnPolicyValueModel,
    trainingConfig: FiveInRowTrainingConfig,
    gameConfig: GameConfig,
    iter: Int,
    collector: ExperienceCollector[Board, Move],
  ): Agent[Board, Move] = {

    val encoder = new Planes3FiveInRowEncode(gameConfig)
    val exploreNodeCreator = new MctsNnNodeCreator(game, encoder, model)

    // Explore agent uses fewer sims to maintain diversity (about half of exploit)

    val explorePuctCoeff = trainingConfig.puctCoeff * 1.5
    val explore = new MctsNnAgent[Board, Move](
      game,
      nodeCreator = exploreNodeCreator,
      mcPlayer = new MonteCarloNnPlayer(game, exploreNodeCreator, encoder, model, currentDepth(iter)),
      collector = Some(collector),
      roundsPerMove = trainingConfig.mctsRoundsPerMove,
      puctCoeff = explorePuctCoeff,
      rand = new Random(),
      selectRandomly = true,
      debug = false
    )
    explore
  }

  private def mkPlainAgent(game: FiveInRowGame,
    trainingConfig: FiveInRowTrainingConfig,
    gameConfig: GameConfig,
    iter: Int,
    collector: ExperienceCollector[Board, Move],
  ): Agent[Board, Move] = {

    val nodeCreator = new MctsNodeCreator(game)
    val rnd = new Random(iter * trainingConfig.trainingSeed + 22)

    // Explore agent uses fewer sims to maintain diversity (about half of exploit)
    val exploreRounds = math.max(20, trainingConfig.mctsRoundsPerMove * 10)

    val plainMctsAgent = new MctsAgent[Board, Move](
      game,
      nodeCreator = nodeCreator,
      mcPlayer = new MonteCarloPlayer(game, nodeCreator, rnd),
      collector = Some(collector),
      roundsPerMove = exploreRounds,
      mctsCoeff = 2.0,
      rand = new Random(),
      selectRandomly = true,
      debug = false
    )
    plainMctsAgent
  }

  private def encodeDataAndTrainBatch(
    shuffled: Array[FinalizedTrainingSample[Board, Move]],
    sampleEncoder: SampleEncoder[Board, Move, INDArray],
    model: FiveInRowCnnPolicyValueModel
  ): Boolean = {
    val encoded: Array[EncodedTrainingSample[INDArray]] = shuffled.map(ps => sampleEncoder.encode(ps))

    val inputs: Array[INDArray] = encoded.map(_.gameState)
    val policyTargets: Array[INDArray] = encoded.map(_.policyTarget)
    val valueTargets: Array[Double] = shuffled.map(fs => fs.gsValue)

    // Before model.trainBatch() call:
    val targetStats = valueTargets.groupBy(identity).map { case (x, y) => (x, y.length) }
    println(s"Value target distribution: $targetStats")

    // Add these debug checks:
    val hasNanTargets = valueTargets.exists(_.isNaN)
    val hasInfTargets = valueTargets.exists(_.isInfinite)
    println(s"Value targets contain NaN: $hasNanTargets")
    println(s"Value targets contain Inf: $hasInfTargets")

    // Check input data
    val inputsHaveNan = inputs.exists(_.isNaN.any())
    val inputsHaveInf = inputs.exists(_.isInfinite.any())
    println(s"Inputs contain NaN: $inputsHaveNan")
    println(s"Inputs contain Inf: $inputsHaveInf")

    // Check policy targets
    val policyHasNan = policyTargets.exists(_.isNaN().any())
    val policyHasInf = policyTargets.exists(_.isInfinite().any())
    println(s"Policy targets contain NaN: $policyHasNan")
    println(s"Policy targets contain Inf: $policyHasInf")

    if (hasNanTargets || hasInfTargets || inputsHaveNan || inputsHaveInf || policyHasNan || policyHasInf) {
      println("STOPPING TRAINING - Invalid training data detected!")
      return false
    }

    model.trainBatch(inputs, policyTargets, valueTargets, model.getConfig.trainingEpochs)

    // Check model state after training
    val testInput = inputs.head
    val testValue = model.value(testInput)
    val testPolicy = model.policy(testInput)

    if (testValue.isNaN || testValue.isInfinite || Math.abs(testValue) > 1.5) {
      println(s"CRITICAL: Model output became NaN/Inf after training! Test value: $testValue")
      println("Stopping training to prevent model collapse!")
      return false
    }

    if (testPolicy.exists(p => p.isNaN || p.isInfinite)) {
      println(s"CRITICAL: Policy output became NaN/Inf after training!")
      println("Model weights have exploded. Training stopped.")
      return false
    }

    println(s"Trained CNN model on ${shuffled.length} samples for ${model.getConfig.trainingEpochs} epochs")
    println(s"Post-training test value: $testValue (should be finite)")

    true
  }

  def trainModel(game: FiveInRowGame,
    model: FiveInRowCnnPolicyValueModel,
    trainingConfig: FiveInRowTrainingConfig,
    gameConfig: GameConfig): Unit = {

    println(s"Starting Five-in-Row CNN training...")

    val encoder = new Planes3FiveInRowEncode(gameConfig)
    val sampleEncoder = new FiveInRowSampleEncoder(encoder)

    // Load the latest model if available
    val startIteration = loadLatestModel(model, model.getConfig.modelPathForSize)

    // Generate tactical puzzles but DON'T use them in early iterations
    val tacticalPuzzles = generateBlockingPuzzles(gameConfig)
    println(s"Generated ${tacticalPuzzles.size} tactical puzzles (will use after iteration 20)")

    var iter = 1
    while (true) {
      println(s"\n=== CNN Training Iteration $iter ===")

      // *** REMOVED Phase 1: No tactical puzzle training in early iterations! ***
      // The model needs to learn from self-play first

      // Phase 2: Self-play training (the ONLY training for early iterations)
      println("Self-play training...")
      val collector = new ExperienceCollector[Board, Move]()
      val exploit = mkExploitAgent(game, model, trainingConfig, gameConfig, iter, collector)
      val explore = mkExploreAgent(game, model, trainingConfig, gameConfig, iter, collector)

      collector.getFinalizedSamplesAndClear()
      val rnd = new Random()

      for (gameNo <- 1 to trainingConfig.gamesPerIteration) {
        collector.beginEpisode()
        val (first, second) = if (rnd.nextBoolean()) (exploit, explore) else (explore, exploit)

        // Alternate who goes first: even games X starts, odd games O starts
        val initialState: GameState[Board, Move] = if (gameNo % 2 == 0) {
          game.initialState()  // X starts (normal)
        } else {
          // O starts: create initial state with O to move
          new GameState(game.initialPosition(), Player.oPlayer, None)
        }

        val result = playFrom(initialState, first, second, gameConfig, collector)
        collector.finalizePendingWithDiscount(result)
      }

      val finalized = collector.getFinalizedSamplesAndClear()
      // Count game results
      val xWins = finalized.count(_.gsValue > 0.5)  // X-perspective value > 0.5 means X won
      val oWins = finalized.count(_.gsValue < -0.5) // X-perspective value < -0.5 means O won
      val draws = finalized.count(s => Math.abs(s.gsValue) <= 0.5) // Near zero means draw

      println(s"Iteration $iter game results (approximate): X≈$xWins, O≈$oWins, Draws≈$draws")



      if (finalized.nonEmpty) {
        val rotations = finalized.flatMap(fs =>
          TrainingSampleRotator.rotatePendingTrainingSample(fs, gameConfig.boardSize))
        val shuffled = Random.shuffle((finalized ++ rotations).toSeq).toArray

        val success = encodeDataAndTrainBatch(shuffled, sampleEncoder, model)

        if (!success) {
          println("CRITICAL: Training failed or model collapsed. Stopping.")
          return
        }
      }

      // OPTIONAL: Add tactical puzzles ONLY after model has learned basics (after iteration 20)
      if (iter > 20 && iter % 5 == 0) {
        println("Adding tactical puzzle training...")
        val puzzleSubset = scala.util.Random.shuffle(tacticalPuzzles.toList).take(3)
        puzzleSubset.foreach { puzzle =>
          trainOnPuzzle(puzzle, game, model, encoder, sampleEncoder, gameConfig)
        }
      }

      // Save periodically
      if (iter % 5 == 0) {
        saveModelWithIteration(model, iter)

        // Test the model
        val emptyBoard = Array.fill(gameConfig.boardSize, gameConfig.boardSize)(0)
        val emptyState = new GameState[Board, Move](new Board(emptyBoard), Player.xPlayer, None)
        val emptyEncoded = encoder.encodeGameState(emptyState)
        val emptyValue = model.value(emptyEncoded)

        println(s"\n=== Model Check at Iteration $iter ===")
        println(f"Empty board value: $emptyValue%.4f (should be near 0.0)")

        // Test on a simple X-win
        val xWinBoard = Array.tabulate(gameConfig.boardSize, gameConfig.boardSize) { (row, col) =>
          if (row == 2 && col >= 1 && col <= 4) 1 else 0
        }
        val xWinState = new GameState[Board, Move](new Board(xWinBoard), Player.oPlayer, None)
        val xWinEncoded = encoder.encodeGameState(xWinState)
        val xWinValue = model.value(xWinEncoded)
        println(f"X-win position value: $xWinValue%.4f (should be positive)")

        // CRITICAL: Check for model collapse
        if (math.abs(emptyValue) > 0.95 || math.abs(xWinValue) > 0.99) {
          println(s"WARNING: Model may be collapsing (extreme values detected)")
        }
      }

      iter += 1
    }
  }


  private def playFrom(
    gs0: GameState[Board, Move],
    a: Agent[Board, Move],
    b: Agent[Board, Move],
    gameConfig: GameConfig,
    collector: ExperienceCollector[Board, Move],
  ): GameResult.Value = {
    var moveNum = 0
    val game = new FiveInRowGame(gameConfig)
    var gs = gs0
    while (!gs.isOver) {
      val current = if (gs.playerToMove == Player.xPlayer) a else b
      val mv = current.selectMove(gs)
      moveNum += 1
      gs = game.applyMove(gs, mv)
      println(s"Game moveNum: $moveNum, player: ${gs.playerToMove}")
    }

    // Force-add the terminal position to training data
    val terminalResult = gs.gameResult.get
    collector.collect(PendingTrainingSample(gs, List.empty, Map.empty, None, Some(terminalResult)))

    println(s"Game result: ${terminalResult}")
    terminalResult
  }

  private def saveModelWithIteration(model: FiveInRowCnnPolicyValueModel, iteration: Int): Unit = {
    try {
      val baseConfig = model.getConfig
      val iterationPath = s"${baseConfig.modelPathForSize}_cnn_iter_$iteration"

      model.save(iterationPath)
      println(s"CNN model checkpoint saved at iteration $iteration")
    } catch {
      case e: Exception =>
        println(s"Failed to save CNN model checkpoint at iteration $iteration: ${e.getMessage}")
        e.printStackTrace()
    }
  }

  private def evaluateModel(game: FiveInRowGame,
    model: FiveInRowCnnPolicyValueModel,
    encoder: Planes3FiveInRowEncode,
    iteration: Int,
    config: FiveInRowTrainingConfig,
    gameConfig: GameConfig): Unit = {

    println(s"\n--- CNN Evaluation at iteration $iteration ---")

    val tournament = new Tournament[Board, Move](game.initialState())
    val nodeCreator = new MctsNnNodeCreator(game, encoder, model)
    val nnAgent = new MctsNnAgent[Board, Move](
      game,
      nodeCreator = nodeCreator,
      mcPlayer = new MonteCarloNnPlayer(game, nodeCreator, encoder, model, evalDepthLimit),
      collector = None,
      roundsPerMove = config.evalMctsRounds,
      puctCoeff = config.puctCoeff,
      rand = new Random(),
      selectRandomly = config.evalMctsRandomSelection,
      debug = false
    )

    val randomAgent = new RandomAgent(game, new Random(123))
    val maxEpisodeLength = gameConfig.boardSize * gameConfig.boardSize
    val result = tournament.playSingleGameTournament(game, nnAgent, randomAgent, 10, maxEpisodeLength, None)
    val total = result.wins + result.loss + result.draw
    val wr = if (total > 0) result.wins.toDouble / total * 100 else 0.0
    println(f"CNN vs Random: wins=${result.wins}, loss=${result.loss}, draw=${result.draw}, winRate=$wr%.2f%%")
  }

  private def loadLatestModel(model: FiveInRowCnnPolicyValueModel, modelBasePath: String): Int = {
    FileHelper.findLatestModelIteration(modelBasePath) match {
      case Some(latestIteration) =>
        val latestModelPath = s"${modelBasePath}_cnn_iter_$latestIteration"
        val loaded = model.load(latestModelPath)
        if (loaded) {
          println(s"Loaded model from iteration $latestIteration: $latestModelPath")
          latestIteration
        } else {
          println(s"Failed to load model from $latestModelPath, starting from iteration 1")
          1
        }
      case None =>
        println(s"No existing model found with pattern ${modelBasePath}_cnn_iter_*, starting from iteration 1")
        1
    }
  }

  def main(args: Array[String]): Unit = {
    val boardSize = if (args.nonEmpty) args(0).toInt else 7 // Default to 10x10
    val gameConfig = GameConfig(boardSize, 5)
    val game = new FiveInRowGame(gameConfig)
    val encoder = new Planes3FiveInRowEncode(gameConfig)
    val (modelConfig, trainingConfig) = FiveInRowPresets.configForBoardSize(boardSize)

    val model = new FiveInRowCnnPolicyValueModel(
      boardSize = boardSize,
      nOutput = encoder.nOutput,
      config = modelConfig
    )

    println(s"Five-in-Row CNN Model Summary for ${boardSize}x${boardSize}:")
    println(s"Board input: ${boardSize}x${boardSize}, Output size: ${encoder.nOutput}")
    println(s"Hidden size: ${modelConfig.hiddenSize}")

    trainModel(game, model, trainingConfig, gameConfig)

    println(s"CNN model saved to ${modelConfig.modelPathForSize}_cnn_policy.zip and ${modelConfig.modelPathForSize}_cnn_value.zip")
  }
}
