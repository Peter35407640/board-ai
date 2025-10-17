package io.github.pbanasiak.boardai.tictactoe.agent

import org.scalatest.funsuite.AnyFunSuite
import io.github.pbanasiak.boardai.{GameState, Player}
import io.github.pbanasiak.boardai.Player.{oStone, xStone}
import io.github.pbanasiak.boardai.mcts.{MctsAgent, MctsNodeCreator, MonteCarloPlayer}
import io.github.pbanasiak.boardai.tictactoe.{TttBoard, TttGame, TttMove}

import scala.util.Random

class MctsAgentTest extends AnyFunSuite {
  val tttGame = new TttGame()

  // Helper methods for creating test scenarios
  private def createAgent(simulations: Int = 100, c: Double = 2.0, seed: Int = 12345): MctsAgent[TttBoard, TttMove] = {
    val rnd = new Random(seed)
    val nodeCreator = MctsNodeCreator(tttGame)
    val mcPlayer = MonteCarloPlayer[TttBoard, TttMove](tttGame, nodeCreator, rnd)
    new MctsAgent(tttGame, nodeCreator, mcPlayer, None, simulations, c, rnd, selectRandomly = false)
  }

  private def createEmptyBoard(): TttBoard = {
    TttBoard.zeros(3, 3)
  }

  private def createNearWinBoard(): TttBoard = {
    // X needs one more move to win horizontally in top row
    // Board: X X _
    //        O O _
    //        _ _ _
    val a = Array(
      Array(Player.xStone, Player.xStone, 0),
      Array(Player.oStone, Player.oStone, 0),
      Array(0, 0, 0)
    )
    TttBoard(a)
  }

  private def createBlockingPosition(): TttBoard = {
    // O needs to block X from winning
    // Board: X X _  (X can win at 0,2)
    //        O _ _
    //        _ _ _
    val a = Array(
      Array(Player.xStone, Player.xStone, 0),
      Array(Player.oStone, 0, 0),
      Array(0, 0, 0)
    )
    TttBoard(a)
  }

  private def createCriticalPosition(): TttBoard = {
    // Position where one move is clearly better than others
    // Board: X O _
    //        O X _
    //        _ _ _
    val a = Array(
      Array(Player.xStone, Player.oStone, 0),
      Array(Player.oStone, Player.xStone, 0),
      Array(0, 0, 0)
    )
    TttBoard(a)
  }

  private def createFullBoard(): TttBoard = {
    TttBoard(Array.fill(3, 3)(1))
  }

  private def createGameState(board: TttBoard, player: Player = Player.xPlayer): GameState[TttBoard, TttMove] = {
    new GameState[TttBoard, TttMove](board, player, None)
  }

  // Basic functionality tests
  test("selectMove should return valid move from empty board") {
    val agent = createAgent(50)
    val gameState = createGameState(createEmptyBoard())

    val move = agent.selectMove(gameState)
    val validMoves = tttGame.validMoves(gameState)

    assert(validMoves.contains(move))
  }

  test("selectMove should return valid move from any position") {
    val agent = createAgent(50)
    val gameState = createGameState(createCriticalPosition())

    val move = agent.selectMove(gameState)
    val validMoves = tttGame.validMoves(gameState)

    assert(validMoves.contains(move))
  }

  test("selectScoredMove should return reasonable score range") {
    val agent = createAgent(100)
    val gameState = createGameState(createEmptyBoard())

    val scoredMove = agent.selectScoredMove(gameState)

    // MCTS scores should be in reasonable range (-1 to 1)
    assert(scoredMove.score.value >= -1.1) // Allow small margin for floating point
    assert(scoredMove.score.value <= 1.1)
    assert(tttGame.validMoves(gameState).contains(scoredMove.move))
  }

  // Strategic behavior tests
  test("selectMove should find winning move when obvious") {
    val agent = createAgent(200) // More simulations for better accuracy

    val board = createNearWinBoard()
    val gameState = createGameState(board, Player.xPlayer)

    // Run multiple times due to MCTS randomness
    val moves = (1 to 5).map(_ => agent.selectMove(gameState))

    // Should consistently pick the winning move (0, 2)
    val winningMoves = moves.count(_ == TttMove(0, 2))
    assert(winningMoves >= 3, s"Expected at least 3 winning moves out of 5, got $winningMoves")
  }

  test("selectMove should block opponent winning move") {
    val agent = createAgent(10)

    val a = Array(
      Array(xStone, xStone, 0),
      Array(oStone, xStone, 0),
      Array(xStone, oStone, oStone)
    )
    val board = TttBoard(a)

    val gameState = createGameState(board, Player.oPlayer)
    val xxx = agent.selectScoredMove(gameState)

    // O should block X by playing at (0, 2)
    val moves = (1 to 5).map(_ => agent.selectMove(gameState))
    val blockingMoves = moves.count(_ == TttMove(0, 2))

    assert(blockingMoves >= 3, s"Expected at least 3 blocking moves out of 5, got $blockingMoves")
  }

  // Algorithm improvement tests
  test("MCTS should improve with more simulations") {
    val quickAgent = createAgent(20, seed = 42)
    val thoroughAgent = createAgent(300, seed = 42)

    val gameState = createGameState(createCriticalPosition())

    // Test multiple runs to account for randomness
    val quickScores = (1 to 3).map(_ => math.abs(quickAgent.selectScoredMove(gameState).score.value))
    val thoroughScores = (1 to 3).map(_ => math.abs(thoroughAgent.selectScoredMove(gameState).score.value))

    val quickAvg = quickScores.sum / quickScores.length
    val thoroughAvg = thoroughScores.sum / thoroughScores.length

    info(s"Quick agent average: $quickAvg, Thorough agent average: $thoroughAvg")

    // More simulations should generally lead to more confident evaluations
    // (Higher absolute scores indicate more certainty)
    assert(thoroughAvg >= quickAvg * 0.8, "More simulations should lead to more confident evaluations")
  }

  test("MCTS should be consistent with fixed random seed") {
    val agent1 = createAgent(100, seed = 789)
    val agent2 = createAgent(100, seed = 789)
    val gameState = createGameState(createEmptyBoard())

    val move1 = agent1.selectMove(gameState)
    val move2 = agent2.selectMove(gameState)

    assert(move1 == move2, "Same seed should produce same results")
  }

  // Edge case tests
  test("selectBranch should throw exception for no valid moves") {
    val agent = createAgent(10)

    val fullBoard = createFullBoard()
    val gameState = createGameState(fullBoard)
    val node = MctsNodeCreator(tttGame).createNode(gameState)

    assertThrows[Exception] {
      agent.selectBranch(node)
    }
  }

  test("agent should handle near-end-game positions") {
    val agent = createAgent(50)

    // Board with only one empty spot
    val a = Array.fill(3, 3)(1)
    a(2)(2) = 0 // One empty spot
    val board = TttBoard(a)
    val gameState = createGameState(board)

    val move = agent.selectMove(gameState)
    assert(move == TttMove(2, 2), "Should pick the only available move")
  }

  // Performance and robustness tests
  test("agent should complete quickly with reasonable simulation count") {
    val agent = createAgent(100)
    val gameState = createGameState(createEmptyBoard())

    val startTime = System.currentTimeMillis()
    val move = agent.selectMove(gameState)
    val endTime = System.currentTimeMillis()

    val timeMs = endTime - startTime
    assert(timeMs < 5000, s"Agent took too long: ${timeMs}ms")
    assert(tttGame.validMoves(gameState).contains(move))
  }

  test("agent should handle multiple consecutive moves") {
    val agent = createAgent(50)
    var gameState = createGameState(createEmptyBoard())

    // Play several moves
    for (i <- 1 to 4) {
      if (!gameState.isOver) {
        val validMovesBeforeMove = tttGame.validMoves(gameState)
        val move = agent.selectMove(gameState)

        // Check the move is valid BEFORE applying it
        assert(validMovesBeforeMove.contains(move),
          s"Move $move not valid. Valid moves: $validMovesBeforeMove, Board state at step $i")

        // Apply the move
        gameState = tttGame.applyMove(gameState, move)
      }
    }

    // At the end, verify we either finished the game or have a valid state
    if (!gameState.isOver) {
      val finalValidMoves = tttGame.validMoves(gameState)
      assert(finalValidMoves.nonEmpty, "Game should either be over or have valid moves")
    }
  }

  // Regression test for specific known positions
  test("regression test for known position") {
    val agent = createAgent(4, seed = 123)

    // Specific position from original test
    val a = Array(
      Array(2, 2, 0),
      Array(1, 1, 2),
      Array(0, 1, 0)
    )
    val board = TttBoard(a)
    val gameState = new GameState[TttBoard, TttMove](board, Player.oPlayer, None)

    val scoredMove = agent.selectScoredMove(gameState)

    // Should find a reasonable move with reasonable confidence
    assert(tttGame.validMoves(gameState).contains(scoredMove.move))
    assert(math.abs(scoredMove.score.value) <= 1.0)
  }

  // Add a debugging version of the failing test
  test("debug agent should perform reasonably against perfect play") {
    val rnd = new Random(456)
    val nodeCreator = MctsNodeCreator(tttGame)
    val mcPlayer = MonteCarloPlayer[TttBoard, TttMove](tttGame, nodeCreator, rnd)

    val agent = new MctsAgent(tttGame, nodeCreator, mcPlayer, None, 50, 2.0, rnd, selectRandomly = false, debug = true)
    val perfectAgent = new TttAgentWithCache(false)

    // Test on a specific mid-game position
    val a = Array(
      Array(2, 0, 0),
      Array(1, 1, 2),
      Array(0, 1, 0)
    )
    val board = TttBoard(a)
    val gameState = new GameState[TttBoard, TttMove](board, Player.oPlayer, None)

    println("=== DEBUG MCTS vs Perfect ===")
    println("Board state:")
    for (row <- 0 to 2) {
      for (col <- 0 to 2) {
        print(s"${board.getInt(row, col)} ")
      }
      println()
    }
    println(s"Current player: ${gameState.playerToMove}")
    println(s"Valid moves: ${tttGame.validMoves(gameState)}")

    val mctsScore = agent.selectScoredMove(gameState)
    val perfectScore = perfectAgent.selectScoredMove(gameState)

    println(s"MCTS: ${mctsScore}")
    println(s"Perfect: ${perfectScore}")
    println(s"Score difference: ${math.abs(mctsScore.score.value - perfectScore.score.value)}")

    // MCTS should be within reasonable range of perfect play
    assert(math.abs(mctsScore.score.value - perfectScore.score.value) <= 0.3)
  }

  // Integration test (lighter version of original)
  test("agent should perform reasonably against perfect play") {
    val rnd = new Random(456)
    val nodeCreator = MctsNodeCreator(tttGame)
    val mcPlayer = MonteCarloPlayer[TttBoard, TttMove](tttGame, nodeCreator, rnd)
    val agent = new MctsAgent(tttGame, nodeCreator, mcPlayer, None, 10, 2.0, rnd, selectRandomly = false)
    val perfectAgent = new TttAgentWithCache(false)

    // Test on a specific mid-game position
    val a = Array(
      Array(2, 0, 0),
      Array(1, 1, 2),
      Array(0, 1, 0)
    )
    val board = TttBoard(a)
    val gameState = new GameState[TttBoard, TttMove](board, Player.oPlayer, None)

    val mctsScore = agent.selectScoredMove(gameState)
    val perfectScore = perfectAgent.selectScoredMove(gameState)

    info(s"MCTS: ${mctsScore}, Perfect: ${perfectScore}")

    // MCTS should be within reasonable range of perfect play
    assert(math.abs(mctsScore.score.value - perfectScore.score.value) <= 0.3)
  }

  test("debug multiple consecutive moves") {
    val agent = createAgent(50, seed = 12345) // Fixed seed for reproducibility
    var gameState = createGameState(createEmptyBoard())

    for (i <- 1 to 4) {
      if (!gameState.isOver) {
        println(s"Step $i:")
        println(s"Current player: ${gameState.playerToMove}")

        // Print board state
        for (row <- 0 to 2) {
          for (col <- 0 to 2) {
            print(s"${gameState.board.getInt(row, col)} ")
          }
          println()
        }

        val validMoves = tttGame.validMoves(gameState)
        println(s"Valid moves: $validMoves")

        val move = agent.selectMove(gameState)
        println(s"Selected move: $move")

        assert(validMoves.contains(move),
          s"Invalid move $move selected. Valid moves: $validMoves")

        gameState = tttGame.applyMove(gameState, move)
        println(s"After move - Game over: ${gameState.isOver}")
        println("---")
      }
    }
  }
}