package io.github.pbanasiak.boardai.tictactoe

import io.github.pbanasiak.boardai.{Game, GameResult, GameState, Player, Score}

case class TttMove(r: Int, c: Int)

class TttGame extends Game[TttBoard, TttMove] {

  val winScore: Score = Score(1)
  val drawScore: Score = Score(0)


  val allMoves: Seq[TttMove] = for {
    r <- 0 until 3
    c <- 0 until 3
  } yield TttMove(r, c)


  def initialPosition(): TttBoard = TttBoard.zeros(3, 3)

  def initialState(): GameState[TttBoard, TttMove] = new GameState(initialPosition(), Player.xPlayer, None)

  def makeMove(board: TttBoard, move: TttMove, player: Player): TttBoard = {
    assert(move.r >= 0)
    assert(move.c >= 0)
    assert(move.r < 3)
    assert(move.c < 3)

    val square = board.getInt(move.r, move.c)
    assert(square == 0, s"square not empty $move")

    val stone: Int = player.getPlayerStone()
    board.setInt(move.r, move.c, stone)
  }

  override def validMoves(gameState: GameState[TttBoard,TttMove]): List[TttMove] = {
    val board = gameState.board
    allMoves.filter(m => board.getInt(m.r, m.c) == 0).toList
  }
  
  def validMoves(board: TttBoard, playerToMove: Player): Seq[TttMove] = {
    allMoves.filter(m => board.getInt(m.r, m.c) == 0)
  }

  override def applyMove(gameState: GameState[TttBoard,TttMove], move: TttMove): GameState[TttBoard, TttMove] = {
    
    val board = gameState.board
    val playerToMove = gameState.playerToMove
    
    val nextBoard: TttBoard = makeMove(board, move, playerToMove)
    val nextPlayer = playerToMove.otherPlayer

    val isVictory = playerWin(nextBoard, playerToMove, move)
    if (isVictory) {
      val newGameResult = playerToMove match {
        case Player.xPlayer => GameResult.xWin
        case Player.oPlayer => GameResult.oWin
      }
      new GameState(nextBoard, nextPlayer, Some(newGameResult))
    } else if (validMoves(nextBoard, nextPlayer).isEmpty) {
      new GameState(nextBoard, nextPlayer, Some(GameResult.draw))
    } else {
      new GameState(nextBoard, nextPlayer, None)
    }
  }
  
  /**
   * @param lastMove is not used in TictacToe, but it is used in other games
   *                 to quickly check if the player has won.
   */
  def playerWin(board: TttBoard, stone: Int, lastMove:TttMove) = {

    // TODO only check rows, cols, diagonals related to lastMove
    def isRowWinning(r: Int, stone: Int) = {
      board.getInt(r, 0) == stone &&
        board.getInt(r, 1) == stone &&
        board.getInt(r, 2) == stone
    }

    def isColWinning(c: Int, stone: Int) = {
      board.getInt(0, c) == stone &&
        board.getInt(1, c) == stone &&
        board.getInt(2, c) == stone
    }

    def isDiagLeftUpperWinning(stone: Int) = {
      board.getInt(0, 0) == stone &&
        board.getInt(1, 1) == stone &&
        board.getInt(2, 2) == stone
    }

    def isDiagRightUpperWinning(stone: Int) = {
      board.getInt(0, 2) == stone &&
        board.getInt(1, 1) == stone &&
        board.getInt(2, 0) == stone
    }

    val anyRowWin: Boolean = (0 until 3).exists(r => isRowWinning(r, stone))
    val anyColWin: Boolean = (0 until 3).exists(r => isColWinning(r, stone))
    val anyDiagWin: Boolean = isDiagLeftUpperWinning(stone) || isDiagRightUpperWinning(stone)

    anyRowWin || anyColWin || anyDiagWin
  }

  def playerWin(board: TttBoard, player: Player, lastMove:TttMove): Boolean = {

    player match {
      case Player.oPlayer => playerWin(board, Player.oStone, lastMove)
      case Player.xPlayer => playerWin(board, Player.xStone, lastMove)
    }
  }

}
