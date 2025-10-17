package io.github.pbanasiak.boardai

import org.scalatest.funsuite.AnyFunSuite

class PlayerTest extends AnyFunSuite
{

  test("xPlayer") {
    val xP = Player.xPlayer

    assert(xP.otherPlayer == Player.oPlayer)
  }

  test("oPlayer") {
    val oP = Player.oPlayer

    assert(oP.otherPlayer == Player.xPlayer)
  }

  test("xPlayer stone") {
    val xP = Player.xPlayer

    assert(xP.getPlayerStone() == Player.xStone)
  }

  test("oPlayer stone") {
    val oP = Player.oPlayer

    assert(oP.getPlayerStone() == Player.oStone)
  }

}
