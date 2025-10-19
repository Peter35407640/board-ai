package io.github.pbanasiak.boardai.fiveinrow

import org.nd4j.linalg.api.ndarray.INDArray
import io.github.pbanasiak.boardai.{GPUChecker, RandomOrderGame, Tournament, TournamentResult}
import io.github.pbanasiak.boardai.agent.{AlphaBetaAgent, FixedDepthAlphaBetaAgent, RandomAgent}
import io.github.pbanasiak.boardai.fiveinrow.agent.HumanAgent
import io.github.pbanasiak.boardai.fiveinrow.encoders.Planes3FiveInRowEncode
import io.github.pbanasiak.boardai.fiveinrow.nn.{FiveInRowCnnPolicyValueModel, FiveInRowMlpPolicyValueModel, FiveInRowPresets, FiveInRowTrainingConfig}
import io.github.pbanasiak.boardai.fiveinrow.ui.SwingUI
import io.github.pbanasiak.boardai.mcts.*

import scala.util.Random


/**
 * for automatically test various agents
 */
object Tournament extends App {
  GPUChecker.verifyGPUUsage()
  GPUChecker.benchmarkMatrixMultiplication()
  GPUChecker.checkGPUMemory()

  private val gameConfig = GameConfig(7, 5)
  private val game = new FiveInRowGame(gameConfig)
  private val rnd = new Random(java.lang.System.currentTimeMillis())

  private val (modelConfig, trainingConfig) = FiveInRowPresets.configForBoardSize(gameConfig.boardSize)
  private val encoder = new Planes3FiveInRowEncode(gameConfig)
  private val mctsRounds = 10000 // determines thinking time
  private val nGames = 10
  private val mcDepth = 1
  println(s"mctsRounds $mctsRounds depth $mcDepth")

  // "models/fiveinrow_7x7
  private def mkModel(baseName: String): FiveInRowCnnPolicyValueModel = {
    val model = new FiveInRowCnnPolicyValueModel(
      boardSize = gameConfig.boardSize,
      nOutput = encoder.nOutput,
      config = modelConfig,
    )

    // Try to load the trained model
    val modelLoaded = model.load(baseName)
    if (modelLoaded) {
      println(s"Loaded trained ${modelConfig.modelPathForSize} model")
    } else {
      println("==Using randomly initialized model - no trained model found")
    }

    model
  }

  private def mkNnAgent(baseName: String): MctsNnAgent[Board, Move] = {
    val model = mkModel(baseName)
    val nodeCreator = new MctsNnNodeCreator[Board, Move, INDArray](game, encoder, model)
    val mcNnPlayer = MonteCarloNnPlayer(game, nodeCreator, encoder, model, depthLimit = mcDepth)

    new MctsNnAgent(game, nodeCreator, mcNnPlayer, collector = None,
      roundsPerMove = mctsRounds,
      puctCoeff = 4.0, rand = rnd, selectRandomly = false)
  }

  private def mkPlainMctsAgent() = {
    val nodeCreator = MctsNodeCreator(game)
    val mcPlayer = MonteCarloPlayer(game, nodeCreator, rnd)
    new MctsAgent(game, nodeCreator, mcPlayer, collector = None, mctsRounds, 2.0, rnd,
      selectRandomly = false)
  }

  val maxEpisodeLength = gameConfig.boardSize * gameConfig.boardSize
  val nodeCreator = MctsNodeCreator(game)

  // UI
  val ui = Some(new SwingUI(game))

  // agents to choose from
  val humanAgent = new HumanAgent(game, ui)
  val mctsNnAgent = mkNnAgent("models/fiveinrow_7x7_cnn_iter_90")
  val mctsNnAgentPrev = mkNnAgent("models/fiveinrow_7x7_cnn_iter_5")
  val mctsAgent = mkPlainMctsAgent()
  val abAgent = new FixedDepthAlphaBetaAgent(RandomOrderGame(game), fixedDepth = 6)
  val randomAgent = new RandomAgent(game, rnd)

  // Create debug context for model visualization
  val model = mkModel("models/fiveinrow_7x7_cnn_iter_90")
  val t = new Tournament[Board, Move](game.initialState())


  // comment auto play section below if you want human play
  val result1: TournamentResult = t.playSingleGameTournament(
    game, mctsNnAgent, mctsAgent,
    nGames, maxEpisodeLength, ui)
  println(f"TournamentResult result ${result1} }")

  val result2: TournamentResult = t.playSingleGameTournament(
    game, mctsAgent, mctsNnAgent,
    nGames, maxEpisodeLength, ui)
  println(f"TournamentResult result ${result2} }")
  // END of auto play section

  // uncomment for iterative human play
  //    val interactive: TournamentResult = t.playSingleGameTournament(
  //      game, humanAgent, mctsNnAgent, 1, maxEpisodeLength, ui)
  //    println(s"Interactive game result: $interactive")

}
