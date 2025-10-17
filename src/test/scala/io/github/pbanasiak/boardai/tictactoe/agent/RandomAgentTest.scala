package io.github.pbanasiak.boardai.agent

import org.scalatest.funsuite.AnyFunSuite
import io.github.pbanasiak.boardai.{GameResult, GameState, Player}
import io.github.pbanasiak.boardai.tictactoe.*

import scala.util.Random

class RandomAgentTest extends AnyFunSuite {
  val rnd = new Random(1)
  val tttGame = new TttGame()

  test("board full throw exception") {

    // Fill the board with ones, then set some cells to 2 (O)
    val boardArray = Array.fill(3, 3)(1)
    boardArray(0)(0) = 2
    boardArray(1)(2) = 2
    boardArray(2)(1) = 2

    val board: TttBoard = new TttBoard(boardArray)
    val gameState = new GameState[TttBoard,TttMove](board, Player.xPlayer, Some(GameResult.xWin))

    val testObj = new RandomAgent(tttGame,rnd)

    val thrown = intercept[Exception] {
      val move = testObj.selectMove(gameState)
    }

    assert(thrown.getMessage.contains("game terminated already"))
  }

  test("column 2 free") {

    // Column 2 is free; other columns partially filled
    val boardArray = Array(
      Array(1, 1, 0),
      Array(2, 2, 0),
      Array(1, 1, 0)
    )
    val board: TttBoard = TttBoard(boardArray)

    val gameState = new GameState[TttBoard,TttMove](board, Player.xPlayer, None)

    val testObj = new RandomAgent(tttGame,rnd)

    val move = testObj.selectMove(gameState)

    val possibleMoves = Set(
      TttMove(0, 2),
      TttMove(1, 2),
      TttMove(2, 2),
    )

    assert(possibleMoves.contains(move))
  }
}
