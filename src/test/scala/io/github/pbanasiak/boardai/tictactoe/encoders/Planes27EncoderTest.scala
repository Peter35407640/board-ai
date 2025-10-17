package io.github.pbanasiak.boardai.tictactoe.encoders

import org.scalatest.funsuite.AnyFunSuiteLike
import io.github.pbanasiak.boardai.tictactoe.{TttBoard, TttMove}
import io.github.pbanasiak.boardai.{GameState, Player}

class Planes27EncoderTest extends AnyFunSuiteLike {

  private val encoder = new Planes27Encoder()

  private def stateFrom(boardArray: Array[Array[Int]], player: Player): GameState[TttBoard, TttMove] = {
    val board = TttBoard(boardArray)
    new GameState[TttBoard, TttMove](board, player, None)
  }

  private def idx(r: Int, c: Int): Int = r * 3 + c

  test("encoder shapes: nInput=27, nOutput=9") {
    assert(encoder.nInput == 27)
    assert(encoder.nOutput == 9)
  }

  test("moveToIndex is row-major order") {
    assert(encoder.moveToIndex(TttMove(0, 0)) == 0)
    assert(encoder.moveToIndex(TttMove(0, 2)) == 2)
    assert(encoder.moveToIndex(TttMove(1, 0)) == 3)
    assert(encoder.moveToIndex(TttMove(2, 2)) == 8)
  }

  test("encodeGameState for X to move: plane0=X stones, plane1=O stones, plane2=all ones") {
    // X at (0,0) and (1,1), O at (0,2)
    val a = Array(
      Array(Player.xStone, 0, Player.oStone),
      Array(0, Player.xStone, 0),
      Array(0, 0, 0)
    )
    val gs = stateFrom(a, Player.xPlayer)

    val v = encoder.encodeGameState(gs)
    assert(v.length == 27)

    // plane offsets
    val p0 = 0 // X stones (always)
    val p1 = 9 // O stones (always)
    val p2 = 18 // X-to-move flag

    // Plane 0 (X stones at (0,0) and (1,1))
    assert(v(p0 + idx(0, 0)) == 1.0)
    assert(v(p0 + idx(1, 1)) == 1.0)
    assert(v(p0 + idx(0, 2)) == 0.0) // O stone should not appear here

    // Plane 1 (O stones at (0,2))
    assert(v(p1 + idx(0, 2)) == 1.0)
    assert(v(p1 + idx(0, 0)) == 0.0)
    assert(v(p1 + idx(1, 1)) == 0.0)

    // Plane 2 (X to move => all ones)
    (0 until 3).foreach(r => (0 until 3).foreach { c =>
      assert(v(p2 + idx(r, c)) == 1.0)
    })
  }

  test("encodeGameState for O to move: plane0=X stones, plane1=O stones, plane2=all zeros") {
    // X at (0,0) and (1,1), O at (0,2)
    val a = Array(
      Array(Player.xStone, 0, Player.oStone),
      Array(0, Player.xStone, 0),
      Array(0, 0, 0)
    )
    val gs = stateFrom(a, Player.oPlayer)

    val v = encoder.encodeGameState(gs)
    assert(v.length == 27)

    // plane offsets - FIXED COMMENTS
    val p0 = 0  // X stones (always)
    val p1 = 9  // O stones (always)
    val p2 = 18 // X-to-move flag

    // Plane 0 (X stones at (0,0) and (1,1)) - FIXED TEST
    assert(v(p0 + idx(0, 0)) == 1.0)
    assert(v(p0 + idx(1, 1)) == 1.0)
    assert(v(p0 + idx(0, 2)) == 0.0)

    // Plane 1 (O stones at (0,2)) - FIXED TEST
    assert(v(p1 + idx(0, 2)) == 1.0)
    assert(v(p1 + idx(0, 0)) == 0.0)
    assert(v(p1 + idx(1, 1)) == 0.0)

    // Plane 2 (O to move => all zeros)
    (0 until 3).foreach(r => (0 until 3).foreach { c =>
      assert(v(p2 + idx(r, c)) == 0.0)
    })
  }

  test("encodeGameState contains only 0/1 values") {
    val a = Array(
      Array(Player.xStone, Player.oStone, 0),
      Array(0, 0, 0),
      Array(0, 0, 0)
    )
    val gsX = stateFrom(a, Player.xPlayer)
    val gsO = stateFrom(a, Player.oPlayer)

    val vX = encoder.encodeGameState(gsX)
    val vO = encoder.encodeGameState(gsO)

    assert(vX.forall(x => x == 0.0 || x == 1.0))
    assert(vO.forall(x => x == 0.0 || x == 1.0))
  }

  test("encoder encodes player-to-move consistently with GameState") {
    import io.github.pbanasiak.boardai.tictactoe.{TttBoard, TttMove}

    val empty = Array(
      Array(0, 0, 0),
      Array(0, 0, 0),
      Array(0, 0, 0)
    )
    val board = TttBoard(empty)

    val gsX = new GameState[TttBoard, TttMove](board, Player.xPlayer, None)
    val encX = encoder.encodeGameState(gsX)
    assert(encX(18) == 1.0, s"Index 18 should indicate X-to-move=1.0 for X, got ${encX(18)}")

    val gsO = new GameState[TttBoard, TttMove](board, Player.oPlayer, None)
    val encO = encoder.encodeGameState(gsO)
    assert(encO(18) == 0.0, s"Index 18 should indicate X-to-move=0.0 for O, got ${encO(18)}")
  }
}