package io.github.pbanasiak.boardai.tictactoe

import io.github.pbanasiak.boardai.{GameState, Player}
import org.scalatest.funsuite.AnyFunSuite

class TttGameTest extends AnyFunSuite {
  val game = new TttGame()

  test("testMakeMove") {
    val board: TttBoard = game.initialPosition()
    val newBoard = game.makeMove(board, TttMove(0, 0), Player.oPlayer)

    assert(newBoard.getInt(1, 1) == 0)
    assert(newBoard.getInt(0, 0) == Player.oStone)
  }


  test("testInitialPosition") {

    val board: TttBoard = game.initialPosition()
    assert(board.getInt(1, 1) == 0)
  }

  test("testValidMoves start") {

    val initialState = game.initialState()
    val validMoves = game.validMoves(initialState)

    assert(validMoves.size == 9)
  }

  test("testValidMoves xxx") {
    val boardArray = Array(
      Array(1, 1, 2),
      Array(0, 0, 0),
      Array(0, 0, 0)
    )
    val board = TttBoard(boardArray)
    val state = new GameState[TttBoard,TttMove](board, Player.xPlayer, None)
    val validMoves = game.validMoves(state)

    assert(validMoves.size == 6)
  }

  test("testValidMoves empty") {
    val boardArray = Array.fill(3, 3)(1)
    val board = TttBoard(boardArray)
    val state = new GameState[TttBoard,TttMove](board, Player.xPlayer, None)
    val validMoves = game.validMoves(state)

    assert(validMoves.isEmpty)
  }

  test("testPlayerWin oPlayer row1") {
    val boardArray = Array(
      Array(0, 0, 0),
      Array(Player.oStone, Player.oStone, Player.oStone),
      Array(0, 0, 0)
    )
    val board = TttBoard(boardArray)

    assert(game.playerWin(board, Player.oPlayer, TttMove(1, 2)))
  }

  test("testPlayerWin xPlayer diag") {
    val boardArray = Array(
      Array(Player.xStone, 0, 0),
      Array(0, Player.xStone, 0),
      Array(0, 0, Player.xStone)
    )
    val board = TttBoard(boardArray)

    assert(game.playerWin(board, Player.xPlayer, TttMove(1, 1)))
  }

}
