package io.github.pbanasiak.boardai.tictactoe.agent

import org.scalatest.funsuite.AnyFunSuite
import io.github.pbanasiak.boardai.agent.{AlphaBetaAgent, NegaMax}
import io.github.pbanasiak.boardai.{GameResult, GameState, Player, Score, ScoredMove}
import io.github.pbanasiak.boardai.tictactoe.{TttBoard, TttGame, TttMove}

class AlphaBetaAgentTest extends AnyFunSuite {
  val tttGame = new TttGame()

  test("22 wins") {

    val a = Array(
      Array(1, 0, 0),
      Array(0, 1, 0),
      Array(0, 0, 0)
    )

    val board: TttBoard = TttBoard(a)
    val gameState = new GameState[TttBoard,TttMove](board, Player.xPlayer, None)

    val testObj = new AlphaBetaAgent(tttGame)
    val bestMove = testObj.bestMove(gameState)

    assert(bestMove.move == TttMove(2, 2))
    assert(bestMove.score.value > 0.9)
  }

  test("22 block the opponent") {

    val a = Array(
      Array(2, 0, 0),
      Array(0, 2, 0),
      Array(1, 0, 0)
    )

    val board: TttBoard = TttBoard(a)
    val gameState = new GameState[TttBoard,TttMove](board, Player.xPlayer, None)

    val testObj = new AlphaBetaAgent(tttGame)
    val (bestScore, bestMove) = testObj.negamax(gameState, Double.NegativeInfinity, Double.PositiveInfinity)

    assert(bestScore == Score(0))
    assert(bestMove.contains(TttMove(2, 2)))
  }

  test("cross test vs MiniMax") {

    val alphaBetaAgent = new AlphaBetaAgent(tttGame)
    val miniMaxAgent = new NegaMax(tttGame)

    val a = Array(
      Array(2, 1, 0),
      Array(0, 0, 0),
      Array(0, 0, 0)
    )
    val board: TttBoard = TttBoard(a)
    val gameState = new GameState[TttBoard,TttMove](board, Player.xPlayer, None)
    val moves: Seq[TttMove] = tttGame.validMoves(gameState)

    moves.foreach { m =>
      println(s"testing m: $m")
      val nextBoard = tttGame.makeMove(board, m, Player.xPlayer)
      val nextState = new GameState[TttBoard,TttMove](nextBoard, Player.oPlayer, None)

      // chosen moves don't need to match but scores must
      val (abScore, abMove) = alphaBetaAgent.negamax(nextState, Double.NegativeInfinity, Double.PositiveInfinity)
      val mmBest: ScoredMove[TttMove] = miniMaxAgent.bestMove(nextState, 0)

      assert(abScore == mmBest.score, s"nextBoard ${nextBoard} ")
    }

  }

  // NEW TESTS START HERE

  test("empty board - first move should be strategic") {
    val board = TttBoard.zeros(3, 3)
    val gameState = new GameState[TttBoard,TttMove](board, Player.xPlayer, None)

    val agent = new AlphaBetaAgent(tttGame)
    val bestMove = agent.selectMove(gameState)

    // First move on empty board should result in a draw score when both players play optimally
    val (score, move) = agent.negamax(gameState)
    assert(score.value == 0.0, "Empty board should lead to draw with optimal play")

    // Move should be valid
    assert(bestMove.r >= 0 && bestMove.r <= 2 && bestMove.c >= 0 && bestMove.c <= 2)
    assert(board.getInt(bestMove.r, bestMove.c) == 0, "Selected move should be on empty square")
  }

  test("immediate win opportunity - X player") {
    // X can win by playing at (0,2)
    val a = Array(
      Array(1, 1, 0), // X X _
      Array(2, 2, 1), // O O X
      Array(0, 0, 0) // _ _ _
    )
    val board = TttBoard(a)
    val gameState = new GameState[TttBoard,TttMove](board, Player.xPlayer, None)

    val agent = new AlphaBetaAgent(tttGame)
    val bestMove = agent.selectMove(gameState)
    val (score, move) = agent.negamax(gameState)

    assert(bestMove == TttMove(0, 2), "Should choose winning move")
    assert(score.value >= 0.9, "Winning move should have score of ~ 1.0")
  }

  test("immediate win opportunity - O player") {
    // O can win by playing at (2,1)
    val a = Array(
      Array(2, 0, 1), // O _ X
      Array(2, 1, 0), // O X _
      Array(0, 0, 0) // _ _ _
    )
    val board = TttBoard(a)
    val gameState = new GameState[TttBoard,TttMove](board, Player.oPlayer, None)

    val agent = new AlphaBetaAgent(tttGame)
    val bestMove = agent.selectMove(gameState)
    val (score, move) = agent.negamax(gameState)

    assert(bestMove == TttMove(2, 0), "Should choose winning move")
    assert(score.value > 0.9, "Winning move should have score of almost 1.0")
  }

  test("must block opponent's winning move") {
    // X is about to win on diagonal, O must block at (2,2)
    val a = Array(
      Array(1, 0, 2), // X _ O
      Array(0, 1, 0), // _ X _
      Array(2, 0, 0) // O _ _
    )
    val board = TttBoard(a)
    val gameState = new GameState[TttBoard,TttMove](board, Player.oPlayer, None)

    val agent = new AlphaBetaAgent(tttGame)
    val bestMove = agent.selectMove(gameState)

    assert(bestMove == TttMove(2, 2), "Should block opponent's winning move")
  }

  test("fork opportunity - create two winning threats") {
    val boardArray = Array(
      Array(1, 0, 0),
      Array(0, 2, 0),
      Array(0, 0, 1)
    )
    val board: TttBoard = TttBoard(boardArray)
    val gameState = new GameState[TttBoard,TttMove](board, Player.xPlayer, None)

    val agent = new AlphaBetaAgent(tttGame)
    val bestMove = agent.bestMove(gameState)

    assert(bestMove.move == TttMove(0, 2) || bestMove.move == TttMove(2, 0))
    assert(bestMove.score.value > 0) // X should win
  }

  test("defend against fork") {
    // O needs to prevent X from creating a fork
    val a = Array(
      Array(1, 0, 0), // X _ _
      Array(0, 2, 0), // _ O _
      Array(0, 0, 1) // _ _ X
    )
    val board = TttBoard(a)
    val gameState = new GameState[TttBoard,TttMove](board, Player.oPlayer, None)

    val agent = new AlphaBetaAgent(tttGame)
    val bestMove = agent.selectMove(gameState)
    val (score, nmMove) = agent.negamax(gameState)

    // Should result in draw if both play optimally
    assert(score.value == 0.0, "Should lead to draw")
  }

  test("game already won - X wins") {
    val a = Array(
      Array(1, 1, 1), // X X X - X wins
      Array(2, 2, 0), // O O _
      Array(0, 0, 0) // _ _ _
    )
    val board = TttBoard(a)
    val gameState = new GameState[TttBoard,TttMove](board, Player.oPlayer, Some(GameResult.xWin))

    val agent = new AlphaBetaAgent(tttGame)
    val (score, nmMove) = agent.negamax(gameState)

    // Score should be -1 for the losing player (negamax perspective)
    assert(score.value < -0.9, "Terminal winning state should return approx -100 for losing player")
  }

  test("game already won - O wins") {
    val a = Array(
      Array(2, 0, 1), // O _ X
      Array(2, 1, 0), // O X _
      Array(2, 0, 0) // O _ _ - O wins
    )
    val board = TttBoard(a)
    val gameState = new GameState[TttBoard,TttMove](board, Player.xPlayer, Some(GameResult.oWin))

    val agent = new AlphaBetaAgent(tttGame)
    val (score, nmMove) = agent.negamax(gameState)

    // Score should be -1 for the losing player (negamax perspective)
    assert(score.value < -0.9, "Terminal winning state should return -1 for losing player")
  }

  test("game ends in draw") {
    val a = Array(
      Array(1, 2, 1), // X O X
      Array(2, 2, 1), // O O X
      Array(2, 1, 2) // O X O - draw
    )
    val board = TttBoard(a)
    val gameState = new GameState[TttBoard,TttMove](board, Player.xPlayer, Some(GameResult.draw))

    val agent = new AlphaBetaAgent(tttGame)
    val (score, nmMove) = agent.negamax(gameState)

    assert(score.value == 0.0, "Terminal draw state should return 0")
  }

  test("one move left - forced win") {
    val a = Array(
      Array(1, 2, 1), // X O X
      Array(2, 1, 2), // O X O
      Array(2, 0, 1) // O _ X
    )
    val board = TttBoard(a)
    val gameState = new GameState[TttBoard,TttMove](board, Player.xPlayer, None)

    val agent = new AlphaBetaAgent(tttGame)
    val bestMove = agent.selectMove(gameState)
    val (score, nmMove) = agent.negamax(gameState)

    assert(bestMove == TttMove(2, 1), "Only valid move")
    assert(score.value >= 0.9, "Should result in win for X (main diagonal)")
  }


  test("narrow alpha-beta window") {
    val a = Array(
      Array(1, 0, 0), // X _ _
      Array(0, 0, 0), // _ _ _
      Array(0, 0, 0) // _ _ _
    )
    val board = TttBoard(a)
    val gameState = new GameState[TttBoard,TttMove](board, Player.oPlayer, None)

    val agent = new AlphaBetaAgent(tttGame)

    // Test with very narrow window that should trigger cutoffs
    val (narrowResult, nMove) = agent.negamax(gameState, -0.1, 0.1)
    val (fullResult, fMove) = agent.negamax(gameState, Double.NegativeInfinity, Double.PositiveInfinity)

    // Scores should be the same
    assert(narrowResult == fullResult, "Narrow alpha-beta window should not affect correctness")
  }

  test("center preference in early game") {
    val a = Array(
      Array(1, 0, 0), // X _ _
      Array(0, 0, 0), // _ _ _
      Array(0, 0, 0) // _ _ _
    )
    val board = TttBoard(a)
    val gameState = new GameState[TttBoard,TttMove](board, Player.oPlayer, None)

    val agent = new AlphaBetaAgent(tttGame)
    val bestMove = agent.selectMove(gameState)

    // Center is strategically best after corner
    assert(bestMove == TttMove(1, 1), "Should prefer center after opponent takes corner")
  }

  test("corner response to center") {
    val a = Array(
      Array(0, 0, 0), // _ _ _
      Array(0, 1, 0), // _ X _
      Array(0, 0, 0) // _ _ _
    )
    val board = TttBoard(a)
    val gameState = new GameState[TttBoard,TttMove](board, Player.oPlayer, None)

    val agent = new AlphaBetaAgent(tttGame)
    val bestMove = agent.selectMove(gameState)

    // Should pick a corner
    val corners = Seq(TttMove(0, 0), TttMove(0, 2), TttMove(2, 0), TttMove(2, 2))
    assert(corners.contains(bestMove), "Should respond to center with corner")
  }

  test("multiple winning moves - any should work") {
    val a = Array(
      Array(1, 1, 0), // X X _
      Array(1, 2, 2), // X O O
      Array(0, 0, 0) // _ _ _
    )
    val board = TttBoard(a)
    val gameState = new GameState[TttBoard,TttMove](board, Player.xPlayer, None)

    val agent = new AlphaBetaAgent(tttGame)
    val (score, nmMove) = agent.negamax(gameState)

    assert(score.value >= 0.9, "Should find winning move")

    // There are multiple winning moves: (0,2) and (2,0) both win
    val winningMoves = Seq(TttMove(0, 2), TttMove(2, 0))
    val selectedMove = agent.selectMove(gameState)
    assert(winningMoves.contains(selectedMove), "Should select one of the winning moves")
  }

  test("selectMove method consistency") {
    val a = Array(
      Array(1, 2, 0), // X O _
      Array(0, 0, 0), // _ _ _
      Array(0, 0, 0) // _ _ _
    )
    val board = TttBoard(a)
    val gameState = new GameState[TttBoard,TttMove](board, Player.xPlayer, None)

    val agent = new AlphaBetaAgent(tttGame)
    val moveFromSelectMove = agent.selectMove(gameState)
    val moveFromNegamax = agent.negamax(gameState)._2.get

    assert(moveFromSelectMove == moveFromNegamax, "selectMove should return same move as negamax")
  }

}