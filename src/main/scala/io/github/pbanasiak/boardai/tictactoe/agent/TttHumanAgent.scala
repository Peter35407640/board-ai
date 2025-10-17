package io.github.pbanasiak.boardai.tictactoe.agent

import java.util.Scanner
import io.github.pbanasiak.boardai.{Agent, GameState}
import io.github.pbanasiak.boardai.tictactoe.{TttBoard,  TttMove, TttGame}

import java.util.concurrent.SynchronousQueue

object TttHumanAgent extends Agent[TttBoard, TttMove] {

  val tttGame = new TttGame()
  private val scanner: Scanner = new Scanner(System.in)

  // Optional UI-backed queue for mouse input
  @volatile private var uiQueue: Option[SynchronousQueue[TttMove]] = None

  // Called by UI setup to enable/disable queue-based input
  def setUiQueue(q: Option[SynchronousQueue[TttMove]]): Unit = { uiQueue = q }

  override def selectMove(gameState: GameState[TttBoard,TttMove]): TttMove = {

    val validMoves =  tttGame.validMoves(gameState)
    if(validMoves.isEmpty) {
      throw new Exception(s"game is over")
    }

    uiQueue match {
      case Some(q) =>
        // Wait for a valid move via UI
        var m: TttMove = q.take()
        while(!validMoves.contains(m)) {
          // ignore invalid clicks and keep waiting
          m = q.take()
        }
        m
      case None =>
        // Console fallback
        var humanMove : Option[TttMove] = None
        while(humanMove.isEmpty || !validMoves.contains(humanMove.get)){
          println(s"enter your move, your stones: ${gameState.playerToMove.getPlayerStone()}")
          val line = scanner.nextLine()
          val a: Array[String] = line.split("[, ]")
          val row: Int = a(0).trim.toInt
          val col: Int = a(1).trim.toInt
          val move = TttMove(row, col)
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
