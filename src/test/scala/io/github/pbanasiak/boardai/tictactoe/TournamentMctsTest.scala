package io.github.pbanasiak.boardai.tictactoe

import org.scalatest.funsuite.AnyFunSuiteLike
import io.github.pbanasiak.boardai.{Tournament, TournamentResult}
import io.github.pbanasiak.boardai.mcts.{MctsAgent, MctsNodeCreator, MonteCarloPlayer}
import io.github.pbanasiak.boardai.tictactoe.Tournament.{mctsAgent, nGames, perfectAgent, t}
import io.github.pbanasiak.boardai.tictactoe.agent.TttAgentWithCache

import scala.util.Random

class TournamentMctsTest extends AnyFunSuiteLike {
  val tttGame = new TttGame()
  val nodeCreator = MctsNodeCreator(tttGame)

  test("test mctsAgent vs perfectAgent") {
    val rndA = new Random(123);
    val nSimulations = 2000
    val nGames = 100


    // its final test to make sure it works
    val perfectAgent = new TttAgentWithCache(false)
    val mcPlayer = MonteCarloPlayer[TttBoard, TttMove](tttGame, nodeCreator, rndA)
    val mctsAgent = new MctsAgent(tttGame, nodeCreator, mcPlayer, None, nSimulations, 1.5, rndA, selectRandomly = false)

    val t = new Tournament[TttBoard, TttMove](tttGame.initialState())
    val result1: TournamentResult = t.playSingleGameTournament(tttGame, mctsAgent, perfectAgent, nGames, 9, None)
    val result2: TournamentResult = t.playSingleGameTournament(tttGame, perfectAgent, mctsAgent, nGames, 9, None)

    println(f"TournamentResult result ${result1} ${result2}")
    assert(result1.draw > 99)
    assert(result2.draw > 99)
  }
}
