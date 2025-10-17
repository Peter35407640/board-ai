package io.github.pbanasiak.boardai.fiveinrow.agent

import java.util.Scanner

import io.github.pbanasiak.boardai.{Agent, GameState}
import io.github.pbanasiak.boardai.fiveinrow._
import io.github.pbanasiak.boardai.fiveinrow.ui.SwingUI

class HumanAgent(game:FiveInRowGame, ui: Option[SwingUI] = None) extends Agent[Board, Move] {

  private val scanner: Scanner = new Scanner(System.in)

  override def selectMove(gameState: GameState[Board,Move]): Move = {

    val validMoves =  game.validMoves(gameState.board)
    if(validMoves.isEmpty) {
      throw new Exception(s"game is over")
    }

    // If UI is provided, wait for mouse click
    ui match {
      case Some(u) =>
        var move: Move = u.awaitHumanMove()
        while(!validMoves.contains(move)) {
          // keep waiting for another click if invalid
          move = u.awaitHumanMove()
        }
        move
      case None =>
        var humanMove : Option[Move] = None
        while(humanMove.isEmpty || !validMoves.contains(humanMove.get)){
          println(s"enter your move, your stones: ${gameState.playerToMove.getPlayerStone()}")
          val line = scanner.nextLine()
          val a: Array[String] = line.split("[, ]")
          val row: Int = a(0).trim.toInt
          val col: Int = a(1).trim.toInt
          val move = Move(row, col)
          if(validMoves.contains(move)){
            println(s"valid move selected $move")
          } else {
            println(s"invalid move, try again")
          }
          humanMove = Some(move)
        }
        humanMove.get
    }
  }
}
