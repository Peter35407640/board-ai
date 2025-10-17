package io.github.pbanasiak.boardai.tictactoe.agent.mcts

import org.scalatest.funsuite.AnyFunSuite
import io.github.pbanasiak.boardai.tictactoe.{TttBoard, TttGame, TttMove}
import io.github.pbanasiak.boardai.tictactoe.encoders.Planes27Encoder
import io.github.pbanasiak.boardai.tictactoe.nn.{TttPolicyValueModel, TttPresets}
import io.github.pbanasiak.boardai.{GameState, Player}

class ModelValueOrientationTest extends AnyFunSuite {

  private def swapStones(a: Array[Array[Int]]): Array[Array[Int]] =
    a.map(_.map {
      case Player.xStone => Player.oStone
      case Player.oStone => Player.xStone
      case v             => v
    })

  test("model value shows approximate antisymmetry only with stone-swap + side-swap") {
    val encoder = new Planes27Encoder()
    val model = new TttPolicyValueModel(encoder.nInput, encoder.nOutput, TttPresets.Quick.model)
    model.load()

    val boardArr = Array(
      Array(Player.xStone, Player.xStone, 0),
      Array(0, 0, 0),
      Array(0, 0, 0)
    )
    val boardX = TttBoard(boardArr)

    // Original: X to move
    val gsX = new GameState[TttBoard, TttMove](boardX, Player.xPlayer, None)
    val vRawX = model.value(encoder.encodeGameState(gsX))
    val vCurX = vRawX // interpret as current-player later if needed

    // Symmetric counterpart: swap stones and flip side to move (O to move)
    val boardO = TttBoard(swapStones(boardArr))
    val gsO = new GameState[TttBoard, TttMove](boardO, Player.oPlayer, None)
    val vRawO = model.value(encoder.encodeGameState(gsO))

    // If raw values are X-perspective, then current-player perspective is:
    // - for X to move: vCurX = vRawX
    // - for O to move: vCurO = -vRawO (because raw is “good for X”)
    val vCurO = -vRawO

    // Now approximate antisymmetry is meaningful
    val delta = math.abs(vCurX - (-vCurO))
    assert(delta < 0.5, s"Expected approximate antisymmetry with stone-swap+side-swap. vCurX=$vCurX vCurO=$vCurO rawX=$vRawX rawO=$vRawO delta=$delta")
  }
}