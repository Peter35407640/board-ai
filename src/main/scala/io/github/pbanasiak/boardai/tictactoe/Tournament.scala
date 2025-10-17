package io.github.pbanasiak.boardai.tictactoe

import io.github.pbanasiak.boardai
import io.github.pbanasiak.boardai.TournamentResult
import io.github.pbanasiak.boardai.agent.RandomAgent
import io.github.pbanasiak.boardai.mcts.*
import io.github.pbanasiak.boardai.nn.GameStateEncoder
import io.github.pbanasiak.boardai.tictactoe.agent.*
import io.github.pbanasiak.boardai.tictactoe.encoders.Planes27Encoder
import io.github.pbanasiak.boardai.tictactoe.nn.{TttPolicyValueModel, TttPresets}
import io.github.pbanasiak.boardai.tictactoe.ui.SwingTicTacToeUI

import scala.util.Random

/**
 * for automatically test various agents
 */
object Tournament extends App {

  //  val rnd = new Random(java.lang.System.currentTimeMillis())
  val rnd = new Random(123);
  val game = new TttGame()

  val preset = TttPresets.Quick
  val modelConfig = preset.model
  val nSimulations = 300

  val nGames = 50
  // available agents
  val randomAgent = new RandomAgent(game, rnd)
  val perfectAgent = new TttAgentWithCache(false)
  val nodeCreator = MctsNodeCreator(game)

  val humanAgent = TttHumanAgent

  val mcPlayer = MonteCarloPlayer(game, nodeCreator, rnd)
  val mctsAgent = new MctsAgent(game, nodeCreator, mcPlayer, collector = None, nSimulations, 2.0, rnd, selectRandomly = true)
  val mctsNnAgent: MctsNnAgent[TttBoard, TttMove] = {

    val encoder: GameStateEncoder[TttBoard, TttMove, Array[Double]] = new Planes27Encoder()


    val model = new TttPolicyValueModel(
      nInput = encoder.nInput,
      nOutput = encoder.nOutput,
      config = modelConfig
    )

    // Try to load pre-trained model using the new load method
    val modelLoaded = model.load(Some("models/tictactoe_quick"))
    if (modelLoaded) {
      println(s"✅ Loaded pre-trained model from ${modelConfig.modelBasePath}_policy.zip and ${modelConfig.modelBasePath}_value.zip")
    } else {
      println(s"⚠️ Using randomly initialized model - no trained model found at ${modelConfig.modelBasePath}")
      println("   Run ModelTrainer first to create a trained model")
    }

    val nodeCreator = new MctsNnNodeCreator(game, encoder, model)
    val collector = None // new ExperienceCollector()
    new MctsNnAgent(game, nodeCreator, mcPlayer, collector,
      roundsPerMove = nSimulations,
      puctCoeff = 1.0,
      rand = rnd,
      selectRandomly = false)
  }


  val uiPrinter = Some(new SwingTicTacToeUI())
  val t = new boardai.Tournament[TttBoard, TttMove](game.initialState())

  // Example 1: AI vs AI
  val result1: TournamentResult = t.playSingleGameTournament(game, mctsNnAgent, mctsAgent, nGames, 9, uiPrinter)
  val result2: TournamentResult = t.playSingleGameTournament(game, mctsAgent, mctsNnAgent, nGames, 9, uiPrinter)


  // Example 2: Interactive game with UI if HumanAgent is used

  //    val interactive: TournamentResult = t.playSingleGameTournament(game, humanAgent, perfectAgent, 1, 9, uiPrinter)
  //    println(s"Interactive game result: $interactive")
}
