package io.github.pbanasiak.boardai

sealed trait Player {
  def otherPlayer: Player

  // player color: white/black
  def getPlayerStone(): Int = {
    this match {
      case Player.oPlayer => Player.oStone
      case Player.xPlayer => Player.xStone
    }
  }
}

object Player {

  // always start
  case object xPlayer extends Player {
    override def otherPlayer: Player = oPlayer
  }

  // always play second
  case object oPlayer extends Player {
    override def otherPlayer: Player = xPlayer
  }

  // x always start game
  val xStone = 1
  val oStone = 2
}
