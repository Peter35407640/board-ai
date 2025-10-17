package io.github.pbanasiak.boardai.tictactoe

import org.scalatest.funsuite.AnyFunSuiteLike
import io.github.pbanasiak.boardai.{Tournament, TournamentResult}
import io.github.pbanasiak.boardai.mcts.{MctsNnAgent, MctsNnNodeCreator, MctsNodeCreator, MonteCarloPlayer}
import io.github.pbanasiak.boardai.nn.GameStateEncoder
import io.github.pbanasiak.boardai.tictactoe.Tournament.{game, mctsAgent, nGames, perfectAgent, t}
import io.github.pbanasiak.boardai.tictactoe.agent.TttAgentWithCache
import io.github.pbanasiak.boardai.tictactoe.encoders.Planes27Encoder
import io.github.pbanasiak.boardai.tictactoe.nn.{TttPolicyValueModel, TttPresets}

import scala.util.Random

class TournamentMctsNnTest extends AnyFunSuiteLike {

  val tttGame = new TttGame()
  test("test mctsAgent vs perfectAgent") {
    val rnd = new Random(123);
    val nSimulations = 100 // really low number to make sure NN works
    val nGames = 50


    // its final test to make sure it works
    val perfectAgent = new TttAgentWithCache(false)
    val nodeCreator = MctsNodeCreator(tttGame)
    val mcPlayer = MonteCarloPlayer[TttBoard, TttMove](tttGame, nodeCreator, rnd)
    val mctsNnAgent: MctsNnAgent[TttBoard, TttMove] = {
      val encoder = new Planes27Encoder()
      val model = new TttPolicyValueModel(
        nInput = encoder.nInput,
        nOutput = encoder.nOutput,
        config = TttPresets.Quick.model
      )

      // Try to load pre-trained model, if it fails, use initialized model
      val loadSuccess = model.load()
      println(s"Model loaded from '${model.getConfig.modelBasePath}': $loadSuccess")

      val nodeCreator = new MctsNnNodeCreator(tttGame, encoder, model)
      val collector = None // new ExperienceCollector()
      new MctsNnAgent(tttGame, nodeCreator, mcPlayer,  collector, roundsPerMove = nSimulations,
        puctCoeff = 2.0, rand = rnd,
        selectRandomly = false, debug = false)
    }
    val t = new Tournament[TttBoard, TttMove](tttGame.initialState())
    val result1: TournamentResult = t.playSingleGameTournament(tttGame, mctsNnAgent, mctsAgent, nGames, 9, None)
    val result2: TournamentResult = t.playSingleGameTournament(tttGame, mctsAgent, mctsNnAgent, nGames, 9, None)

    // expect at least 50% not lost
    assert(result1.draw + result1.wins> 25)
    assert(result2.draw+result2.loss > 25)
  }
}
