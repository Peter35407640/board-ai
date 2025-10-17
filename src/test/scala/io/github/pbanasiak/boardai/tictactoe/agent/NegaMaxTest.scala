package io.github.pbanasiak.boardai.tictactoe.agent

import org.scalatest.funsuite.AnyFunSuite
import io.github.pbanasiak.boardai.agent.NegaMax
import io.github.pbanasiak.boardai.tictactoe.{TttBoard, TttMove, TttGame}
import io.github.pbanasiak.boardai.{GameState, Player, Score, ScoredMove}

class NegaMaxTest extends AnyFunSuite {
  val tttGame = new TttGame()

  test("22 wins") {
    val boardArray = Array(
      Array(1, 0, 0),
      Array(0, 1, 0),
      Array(0, 0, 0)
    )
    val board: TttBoard = TttBoard(boardArray)
    val gameState = new GameState[TttBoard, TttMove](board, Player.xPlayer, None)

    val testObj = new NegaMax(tttGame)
    val bestMove = testObj.bestMove(gameState, 0)

    assert(bestMove.move ==  TttMove(2, 2))
    assert(bestMove.score.value > 0.9)
  }

  test("22 block the opponent") {
    val boardArray = Array(
      Array(2, 0, 0),
      Array(0, 2, 0),
      Array(1, 0, 0)
    )
    val board: TttBoard = TttBoard(boardArray)
    val gameState = new GameState[TttBoard, TttMove](board, Player.xPlayer, None)

    val testObj = new NegaMax(tttGame)
    val bestMove = testObj.bestMove(gameState, 0)

    assert(bestMove == ScoredMove(Score(0), TttMove(2, 2)))
  }

  test("immediate win - horizontal") {
    val boardArray = Array(
      Array(1, 1, 0), // X has two in a row, can win at (0,2)
      Array(2, 0, 0),
      Array(0, 2, 0)
    )
    val board: TttBoard = TttBoard(boardArray)
    val gameState = new GameState[TttBoard, TttMove](board, Player.xPlayer, None)

    val testObj = new NegaMax(tttGame)
    val bestMove = testObj.bestMove(gameState, 0)

    // Move(2, 2) is also winning but not immediate
    assert(bestMove.move ==  TttMove(0, 2))
    assert(bestMove.score.value == 0.9999)
  }

  test("immediate win - vertical") {
    val boardArray = Array(
      Array(1, 2, 0),
      Array(1, 0, 2), // X can win vertically at (2,0)
      Array(0, 0, 0)
    )
    val board: TttBoard = TttBoard(boardArray)
    val gameState = new GameState[TttBoard, TttMove](board, Player.xPlayer, None)

    val testObj = new NegaMax(tttGame)
    val bestMove = testObj.bestMove(gameState, 0)

    // (1.0, Move(2, 2))  is also winning but not immediate
    assert(bestMove.move == TttMove(2, 0))
    assert(bestMove.score.value == 0.9999)
  }

  test("immediate win - diagonal") {
    val boardArray = Array(
      Array(1, 2, 0),
      Array(0, 1, 2), // X can win diagonally at (2,2)
      Array(0, 0, 0)
    )
    val board: TttBoard = TttBoard(boardArray)
    val gameState = new GameState[TttBoard, TttMove](board, Player.xPlayer, None)

    val testObj = new NegaMax(tttGame)
    val bestMove = testObj.bestMove(gameState, 0)

    assert(bestMove.move ==  TttMove(2, 2))
    assert(bestMove.score.value == 0.9999)
  }

  test("x must block and win the game - horizontal") {
    val boardArray = Array(
      Array(2, 2, 0), // O has two in a row, X must block at (0,2) and win
      Array(1, 0, 0),
      Array(0, 1, 0)
    )
    val board: TttBoard = TttBoard(boardArray)
    val gameState = new GameState[TttBoard, TttMove](board, Player.xPlayer, None)

    val testObj = new NegaMax(tttGame)
    val bestMove = testObj.bestMove(gameState, 0)

    assert(bestMove.move == TttMove(0, 2))
    // Should be x win with perfect play after blocking
    assert(bestMove.score.value > 0)
  }

  test("must block opponent win - vertical") {
    val boardArray = Array(
      Array(2, 1, 0),
      Array(2, 0, 1), // O threatens to win vertically at (2,0)
      Array(0, 0, 0)
    )
    val board: TttBoard = TttBoard(boardArray)
    val gameState = new GameState[TttBoard, TttMove](board, Player.xPlayer, None)

    val testObj = new NegaMax(tttGame)
    val bestMove = testObj.bestMove(gameState, 0)

    assert(bestMove.move == TttMove(2, 0))
    assert(bestMove.score.value >= 0.9)
  }

  test("forced draw scenario") {
    val boardArray = Array(
      Array(2, 1, 2),
      Array(1, 2, 1),
      Array(0, 1, 2)
    )
    val board: TttBoard = TttBoard(boardArray)
    val gameState = new GameState[TttBoard, TttMove](board, Player.xPlayer, None)

    val testObj = new NegaMax(tttGame)
    val bestMove = testObj.bestMove(gameState, 0)

    // X will lose regardless, so score should be -1
    assert(bestMove.score == Score(0))
    assert(bestMove.move == TttMove(2, 0))
  }

  test("fork opportunity - X can create two winning threats") {
    val boardArray = Array(
      Array(1, 0, 0),
      Array(0, 2, 0),
      Array(0, 0, 1)
    )
    val board: TttBoard = TttBoard(boardArray)
    val gameState = new GameState[TttBoard, TttMove](board, Player.xPlayer, None)

    val testObj = new NegaMax(tttGame)
    val bestMove = testObj.bestMove(gameState, 0)

    // Either (0,2) or (2,0) creates a fork
    assert(bestMove.move == TttMove(0, 2) || bestMove.move == TttMove(2, 0))
    assert(bestMove.score.value > 0) // X should win
  }

  test("prevent opponent fork") {
    val boardArray = Array(
      Array(2, 0, 0),
      Array(0, 1, 0),
      Array(0, 0, 2) // O at (2,2) - O threatens fork
    )
    val board: TttBoard = TttBoard(boardArray)
    val gameState = new GameState[TttBoard, TttMove](board, Player.xPlayer, None)

    val testObj = new NegaMax(tttGame)
    val bestMove = testObj.bestMove(gameState, 0)

    // X must prevent O's fork by blocking at (0,2) or (2,0)
    assert(bestMove.move == TttMove(2, 1)) // there are more
    assert(bestMove.score == Score(0.0)) // Should be draw with perfect play
  }

  test("corner vs edge opening response") {
    val boardArray = Array(
      Array(1, 0, 0), // X takes corner
      Array(0, 0, 0),
      Array(0, 0, 0)
    )
    val board: TttBoard = TttBoard(boardArray)
    val gameState = new GameState[TttBoard, TttMove](board, Player.oPlayer, None)

    val testObj = new NegaMax(tttGame)
    val bestMove = testObj.bestMove(gameState, 0)

    // O should take center against corner opening
    assert(bestMove.move == TttMove(1, 1))
    assert(bestMove.score == Score(0.0))
  }

  test("edge opening - X plays edge first") {
    val boardArray = Array(
      Array(0, 1, 0), // X takes edge
      Array(0, 0, 0),
      Array(0, 0, 0)
    )
    val board: TttBoard = TttBoard(boardArray)
    val gameState = new GameState[TttBoard, TttMove](board, Player.oPlayer, None)

    val testObj = new NegaMax(tttGame)
    val bestMove = testObj.bestMove(gameState, 0)

    assert(bestMove.move == TttMove(2, 1)) // there are more moves
    assert(bestMove.score == Score(0.0))
  }

  test("near endgame - few moves left") {
    val boardArray = Array(
      Array(1, 2, 1),
      Array(2, 1, 2),
      Array(2, 0, 2) // Only one move left at (2,1)
    )
    val board: TttBoard = TttBoard(boardArray)
    val gameState = new GameState[TttBoard, TttMove](board, Player.xPlayer, None)

    val testObj = new NegaMax(tttGame)
    val bestMove = testObj.bestMove(gameState, 0)

    assert(bestMove.move == TttMove(2, 1))
    // This should result in a draw
    assert(bestMove.score == Score(0))
  }

  test("O player perspective - immediate win") {
    val boardArray = Array(
      Array(2, 2, 0), // O can win at (0,2)
      Array(1, 0, 1),
      Array(0, 1, 0)
    )
    val board: TttBoard = TttBoard(boardArray)
    val gameState = new GameState[TttBoard, TttMove](board, Player.oPlayer, None)

    val testObj = new NegaMax(tttGame)
    val bestMove = testObj.bestMove(gameState, 0)

    assert(bestMove.move == TttMove(0, 2))
    assert(bestMove.score.value == 0.9999)
  }

  test("O player perspective - must block X") {
    val boardArray = Array(
      Array(1, 1, 0), // X threatens win at (0,2)
      Array(2, 0, 0),
      Array(0, 2, 0)
    )
    val board: TttBoard = TttBoard(boardArray)
    val gameState = new GameState[TttBoard, TttMove](board, Player.oPlayer, None)

    val testObj = new NegaMax(tttGame)
    val bestMove = testObj.bestMove(gameState, 0)

    // oPlayer perspective should block and win
    assert(bestMove.move == TttMove(0, 2))
    assert(bestMove.score.value > 0)
  }

  test("symmetric positions should have consistent evaluation") {
    // Test rotational symmetry
    val boardArray1 = Array(
      Array(1, 0, 0),
      Array(0, 2, 0),
      Array(0, 0, 0)
    )
    val boardArray2 = Array(
      Array(0, 0, 1),
      Array(0, 2, 0),
      Array(0, 0, 0)
    )

    val board1: TttBoard = TttBoard(boardArray1)
    val board2: TttBoard = TttBoard(boardArray2)
    val gameState1 = new GameState[TttBoard, TttMove](board1, Player.xPlayer, None)
    val gameState2 = new GameState[TttBoard, TttMove](board2, Player.xPlayer, None)

    val testObj = new NegaMax(tttGame)
    val bestMove1 = testObj.bestMove(gameState1, 0)
    val bestMove2 = testObj.bestMove(gameState2, 0)

    // Scores should be identical for symmetric positions
    assert(bestMove1.score == bestMove2.score)
  }

  test("performance on deeper game tree") {
    // Test with 3 moves played (6 moves remaining)
    val boardArray = Array(
      Array(1, 0, 0),
      Array(0, 2, 0),
      Array(0, 0, 1)
    )
    val board: TttBoard = TttBoard(boardArray)
    val gameState = new GameState[TttBoard, TttMove](board, Player.oPlayer, None)

    val testObj = new NegaMax(tttGame)
    val startTime = System.nanoTime()
    val bestMove = testObj.bestMove(gameState, 0)
    val endTime = System.nanoTime()
    val duration = (endTime - startTime) / 1000000.0 // Convert to milliseconds

    // Should complete in reasonable time (adjust threshold as needed)
    assert(duration < 1000.0, s"Algorithm took too long: ${duration}ms")
    assert(bestMove.score == Score(0.0)) // Should be draw with perfect play
  }
}