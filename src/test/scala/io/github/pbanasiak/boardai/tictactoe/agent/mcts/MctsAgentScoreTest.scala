package io.github.pbanasiak.boardai.tictactoe.agent

import org.scalatest.funsuite.AnyFunSuite
import io.github.pbanasiak.boardai.mcts.{MctsAgent, MctsNodeCreator, MonteCarloPlayer}
import io.github.pbanasiak.boardai.tictactoe.{TttBoard, TttGame, TttMove}
import io.github.pbanasiak.boardai.{GameState, Player}

import scala.util.Random

class MctsAgentScoreTest extends AnyFunSuite {
  val tttGame = new TttGame()

  private def createAgent(simulations: Int = 500, seed: Int = 42, talk: Boolean = false): MctsAgent[TttBoard, TttMove] = {
    val rnd = new Random(seed)
    val nodeCreator = MctsNodeCreator(tttGame)
    val mcPlayer = MonteCarloPlayer[TttBoard, TttMove](tttGame, nodeCreator, rnd)
    new MctsAgent(tttGame, MctsNodeCreator(tttGame), mcPlayer, None, simulations, 2.0, new Random(seed), talk)
  }


  private def createGameState(board: TttBoard, player: Player): GameState[TttBoard, TttMove] = {
    new GameState[TttBoard, TttMove](board, player, None)
  }

  test("Debug MCTS blocking position with detailed output") {
    val agent = createAgent(50, seed = 456, talk = true) // Enable debug output

    // O must block X from winning at (0,2)
    val blockingBoard = Array(
      Array(Player.xStone, Player.xStone, 0), // X X _
      Array(Player.oStone, 0, 0), // O _ _
      Array(0, 0, 0) // _ _ _
    )
    val board = TttBoard(blockingBoard)
    val gameState = createGameState(board, Player.oPlayer)

    println("=== DEBUG BLOCKING POSITION ===")
    println("Board:")
    println(board.toString)
    println(s"Player to move: ${gameState.playerToMove}")
    println("X can win at (0,2) if O doesn't block")

    val scoredMove = agent.selectScoredMove(gameState)
    println(s"MCTS selected: ${scoredMove.move} with score ${scoredMove.score}")

    // Test multiple runs to see consistency
    val moves = (1 to 5).map { i =>
      val testAgent = createAgent(1000, seed = 456 + i)
      val result = testAgent.selectMove(gameState)
      println(s"Run $i: selected $result")
      result
    }

    val blockingMoves = moves.count(_ == TttMove(0, 2))
    println(s"Blocking moves: $blockingMoves out of 5")

    // Should find the blocking move most of the time
    assert(scoredMove.move == TttMove(0, 2) || blockingMoves >= 3,
      s"Expected blocking move (0,2) or at least 3/5 consistent blocking, got ${scoredMove.move} with $blockingMoves/5 blocking")
  }


  test("MCTS should correctly evaluate losing positions") {
    val agent = createAgent(500, seed = 789)

    // X is about to lose - O can win in multiple ways
    val losingBoard = Array(
      Array(Player.oStone, Player.oStone, 0), // O O _
      Array(Player.xStone, 0, 0), // X _ _
      Array(0, 0, Player.oStone) // _ _ O
    )
    val board = TttBoard(losingBoard)
    val gameState = createGameState(board, Player.xPlayer)

    val scoredMove = agent.selectScoredMove(gameState)

    // Score should be negative (bad for X)
    assert(scoredMove.score.value < 0.0,
      s"Expected negative score for losing position, got ${scoredMove.score}")
  }

  test("MCTS score consistency across multiple runs") {
    // Test that scores are consistent with fixed seeds
    val agent1 = createAgent(200, seed = 999)
    val agent2 = createAgent(200, seed = 999)

    val testBoard = Array(
      Array(Player.xStone, 0, 0), // X _ _
      Array(0, Player.oStone, 0), // _ O _
      Array(0, 0, 0) // _ _ _
    )
    val board = TttBoard(testBoard)
    val gameState = createGameState(board, Player.xPlayer)

    val score1 = agent1.selectScoredMove(gameState)
    val score2 = agent2.selectScoredMove(gameState)

    assert(score1.move == score2.move, "Same seed should produce same moves")
    assert(math.abs(score1.score.value - score2.score.value) < 0.01,
      "Same seed should produce similar scores")
  }

  test("MCTS should handle draw positions correctly") {
    val agent = createAgent(300, seed = 111)

    // Position likely to lead to draw
    val drawBoard = Array(
      Array(Player.xStone, Player.oStone, Player.xStone), // X O X
      Array(Player.oStone, Player.oStone, Player.xStone), // O O X
      Array(Player.oStone, Player.xStone, 0) // O X _
    )
    val board = TttBoard(drawBoard)
    val gameState = createGameState(board, Player.oPlayer)

    val scoredMove = agent.selectScoredMove(gameState)

    // Should make the only legal move
    assert(scoredMove.move == TttMove(2, 2), "Should make the only legal move")

    // Score should be close to 0 (draw)
    assert(math.abs(scoredMove.score.value) < 0.2,
      s"Expected score close to 0 for draw position, got ${scoredMove.score}")
  }

  test("MCTS score negation in backpropagation") {
    val agent = createAgent(100, seed = 222)

    // Create a position where we can trace score propagation
    val testBoard = Array(
      Array(Player.xStone, Player.xStone, 0), // X X _
      Array(Player.oStone, 0, 0), // O _ _
      Array(0, 0, 0) // _ _ _
    )
    val board = TttBoard(testBoard)

    // Test from X's perspective (should see winning opportunity)
    val xGameState = createGameState(board, Player.xPlayer)
    val xScoredMove = agent.selectScoredMove(gameState = xGameState)
    assert(xScoredMove.score.value > 0.5, "X should see winning opportunity")

    // Test from O's perspective (should see need to block)
    val oGameState = createGameState(board, Player.oPlayer)
    val oScoredMove = agent.selectScoredMove(gameState = oGameState)

    // Both should want to play at (0,2) but for different reasons
    assert(xScoredMove.move == TttMove(0, 2), "X should want to win at (0,2)")
    assert(oScoredMove.move == TttMove(0, 2), "O should want to block at (0,2)")
  }

  test("MCTS should prefer winning over blocking when both are possible") {
    val agent = createAgent(500, seed = 333)

    // X can win at (0,2) OR block O from winning at (1,2)
    val complexBoard = Array(
      Array(Player.xStone, Player.xStone, 0), // X X _  <- X can win here
      Array(Player.oStone, Player.oStone, 0), // O O _  <- O can win here
      Array(0, 0, 0) // _ _ _
    )
    val board = TttBoard(complexBoard)
    val gameState = createGameState(board, Player.xPlayer)

    val scoredMove = agent.selectScoredMove(gameState)

    // Should prefer winning to blocking
    assert(scoredMove.move == TttMove(0, 2),
      s"Should prefer winning move (0,2) over blocking, got ${scoredMove.move}")

    // Should have very high confidence
    assert(scoredMove.score.value > 0.8,
      s"Should have high confidence in winning move, got ${scoredMove.score}")
  }

  test("Score values should be within valid range") {
    val agent = createAgent(200, seed = 444)

    val testPositions = List(
      // Empty board
      Array(
        Array(0, 0, 0),
        Array(0, 0, 0),
        Array(0, 0, 0)
      ),
      // Mid-game
      Array(
        Array(Player.xStone, 0, Player.oStone),
        Array(0, Player.xStone, 0),
        Array(Player.oStone, 0, 0)
      )
    )

    testPositions.foreach { boardArray =>
      val board = TttBoard(boardArray)
      val gameState = createGameState(board, Player.xPlayer)
      val scoredMove = agent.selectScoredMove(gameState)

      assert(scoredMove.score.value >= -1.0 && scoredMove.score.value <= 1.0,
        s"Score ${scoredMove.score} outside valid range [-1,1]")
    }
  }
}
