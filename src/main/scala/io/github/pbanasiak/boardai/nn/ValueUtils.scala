package io.github.pbanasiak.boardai.nn

import io.github.pbanasiak.boardai.Player

object ValueUtils {
  /** Convert X-perspective value to current player perspective */
  def xPerspectiveToCurrentPlayer(xValue: Double, currentPlayer: Player): Double = {
    currentPlayer match {
      case Player.xPlayer => xValue      // X wants positive values
      case Player.oPlayer => -xValue     // O wants negative of X's values
    }
  }

  /** Convert current player perspective back to X perspective */
  def currentPlayerToXPerspective(currentValue: Double, currentPlayer: Player): Double = {
    currentPlayer match {
      case Player.xPlayer => currentValue
      case Player.oPlayer => -currentValue
    }
  }
}