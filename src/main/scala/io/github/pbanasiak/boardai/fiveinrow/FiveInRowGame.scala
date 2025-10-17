package io.github.pbanasiak.boardai.fiveinrow

import io.github.pbanasiak.boardai.{Game, GameResult, GameState, Player, Score}
import io.github.pbanasiak.boardai.fiveinrow.FiveInRowGame.Direction

case class GameConfig(boardSize: Int, numInRow: Int)
case class Move(r: Int, c: Int)


object FiveInRowGame {
  case class Direction(r: Int, c: Int)

  val winScore: Score = Score(100)
  val drawScore: Score = Score(0)

  def gameResultToScore(gameResult: GameResult.Value): Score = {
    gameResult match {
      case GameResult.draw => drawScore
      case GameResult.xWin => winScore
      case GameResult.oWin => winScore
    }
  }
}

class FiveInRowGame(val gameConfig: GameConfig) extends Game[Board, Move] {


  override val winScore: Score = FiveInRowGame.winScore
  override val drawScore: Score =FiveInRowGame.drawScore


  val allMoves: Seq[Move] = for {
    r <- 0 until gameConfig.boardSize
    c <- 0 until gameConfig.boardSize
  } yield Move(r, c)


  def initialPosition(): Board = {
    Board.zeros(gameConfig.boardSize, gameConfig.boardSize)
  }

  def initialState(): GameState[Board, Move] = {
    new GameState(initialPosition(), Player.xPlayer, None)
  }

  def makeMove(board: Board, move: Move, player: Player): Board = {
    assert(move.r >= 0)
    assert(move.c >= 0)
    assert(move.r < gameConfig.boardSize)
    assert(move.c < gameConfig.boardSize)

    val square = board.getInt(move.r, move.c)
    assert(square == 0, s"square not empty $move")

    // Use pure Scala Board.setInt instead of matrix operations
    val stone: Int = player.getPlayerStone()
    board.setInt(move.r, move.c, stone)
  }

  def validMoves(board: Board): Seq[Move] = {
    allMoves.filter(m => board.getInt(m.r, m.c) == 0)
  }

  override def validMoves(gameState: GameState[Board, Move]): Seq[Move] = {
    validMoves(gameState.board)
  }

  override def applyMove(gameState: GameState[Board, Move], move: Move): GameState[Board, Move] = {
    val board = gameState.board
    val playerToMove = gameState.playerToMove

    val nextBoard: Board = makeMove(board, move, playerToMove)
    val nextPlayer = playerToMove.otherPlayer

    val isVictory = playerWin(nextBoard, playerToMove, move)
    if (isVictory) {
      val newGameResult = playerToMove match {
        case Player.xPlayer => GameResult.xWin
        case Player.oPlayer => GameResult.oWin
      }
      new GameState(nextBoard, nextPlayer, Some(newGameResult))
    } else if (validMoves(nextBoard).isEmpty) {
      new GameState(nextBoard, nextPlayer, Some(GameResult.draw))
    } else {
      new GameState(nextBoard, nextPlayer, None)
    }
  }


  val directions = Seq(
    Direction(1, 0),
    Direction(0, 1),
    Direction(1, 1),
    Direction(1, -1),
  )

  def isOnBoard(move: Move) = {
    move.r >= 0 && move.r < gameConfig.boardSize && move.c >= 0 && move.c < gameConfig.boardSize
  }

  def isDirectionWinning(board: Board, stone: Int, lastMove: Move, direction: Direction): Boolean = {

    var m = lastMove

    def plusDirection(move: Move, dir: Direction) = {
      Move(move.r + dir.r, move.c + dir.c)
    }

    def minusDirection(move: Move, dir: Direction) = {
      Move(move.r - dir.r, move.c - dir.c)
    }

    var count1 = 0
    while (count1 < gameConfig.numInRow && isOnBoard(m) && board.getInt(m.r, m.c) == stone) {
      count1 += 1
      m = plusDirection(m, direction)
    }

    // should not happen, the moved square does not contain our stone
    if (count1 < 1) return false

    // win
    if (count1 >= gameConfig.numInRow) return true

    // check opposite direction, skip lastMove position
    m = minusDirection(lastMove, direction)
    var count2 = 0
    while ((count1 + count2) < gameConfig.numInRow && isOnBoard(m) && board.getInt(m.r, m.c) == stone) {
      count2 += 1
      m = minusDirection(m, direction)
    }

    val consecutiveNum = count1 + count2
    consecutiveNum >= gameConfig.numInRow
  }

  def playerWin(board: Board, stone: Int, lastMove: Move) = {

    val dirWin: Option[Direction] = directions.find { d => isDirectionWinning(board, stone, lastMove, d) }
    dirWin.isDefined
  }

  def playerWin(board: Board, player: Player, lastMove: Move): Boolean = {

    player match {
      case Player.oPlayer => playerWin(board, Player.oStone, lastMove)
      case Player.xPlayer => playerWin(board, Player.xStone, lastMove)
    }
  }

}
