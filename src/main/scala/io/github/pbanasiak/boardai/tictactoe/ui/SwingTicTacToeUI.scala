package io.github.pbanasiak.boardai.tictactoe.ui

import io.github.pbanasiak.boardai.{GamePrinter, GameState, Player}
import io.github.pbanasiak.boardai.tictactoe
import io.github.pbanasiak.boardai.tictactoe.{TttBoard, TttMove, TttGame}

import java.awt.{BasicStroke, Color, Dimension, Font, Graphics, Graphics2D}
import java.awt.event.{MouseAdapter, MouseEvent}
import javax.swing.{JFrame, JLabel, JPanel, SwingUtilities, WindowConstants}
import java.util.concurrent.SynchronousQueue

/**
 * Simple Swing-based UI for Tic-Tac-Toe that
 * - displays the board and the player to move
 * - allows a human to select moves with a mouse click
 *
 * It also serves as a GamePrinter so it can be plugged into the Tournament.
 */
class SwingTicTacToeUI() extends GamePrinter[TttBoard, TttMove] {
  import SwingTicTacToeUI._

  val tttGame = TttGame()
  // ensure move input is routed through HumanAgent-compatible queue
  HumanUiInput.enable()

  private val frame = new JFrame("TicTacToe")
  private val status = new JLabel("Ready")
  private val boardPanel = new BoardPanel()

  frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
  frame.setLayout(new java.awt.BorderLayout())
  frame.add(status, java.awt.BorderLayout.NORTH)
  frame.add(boardPanel, java.awt.BorderLayout.CENTER)
  frame.pack()
  frame.setLocationRelativeTo(null)
  frame.setVisible(true)

  @volatile private var lastBoard: TttBoard = tttGame.initialPosition()
  @volatile private var lastPlayer: Player = Player.xPlayer

  override def printDraw(board: TttBoard, playerToMove: Player): Unit = updateUi(board, s"Draw. Last to move: $playerToMove")

  override def printVictory(board: TttBoard, playerToMove: Player): Unit = updateUi(board, s"${playerToMove} won!")

  override def print(gameState: GameState[TttBoard, TttMove]): Unit = updateUi(gameState.board, s"To move: ${gameState.playerToMove}")

  private def updateUi(board: TttBoard, text: String): Unit = {
    lastBoard = board
    status.setText(text)
    boardPanel.repaint()
  }

  private class BoardPanel extends JPanel {
    setPreferredSize(new Dimension(360, 360))
    setBackground(Color.WHITE)

    addMouseListener(new MouseAdapter {
      override def mouseClicked(e: MouseEvent): Unit = {
        val (r, c) = coordsToCell(e.getX, e.getY)
        if (r >= 0 && r < 3 && c >= 0 && c < 3) {
          // Only accept clicks on empty cells
          if (lastBoard.getInt(r, c) == 0) {
            HumanUiInput.offer(TttMove(r, c))
          }
        }
      }
    })

    override def paintComponent(g: Graphics): Unit = {
      super.paintComponent(g)
      val g2 = g.asInstanceOf[Graphics2D]
      g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON)

      val w = getWidth
      val h = getHeight
      val cellW = w / 3
      val cellH = h / 3

      // grid
      g2.setColor(Color.BLACK)
      g2.setStroke(new BasicStroke(2f))
      for (i <- 1 until 3) {
        g2.drawLine(i * cellW, 0, i * cellW, h)
        g2.drawLine(0, i * cellH, w, i * cellH)
      }

      // pieces
      val fontSize = Math.min(cellW, cellH) * 3 / 4
      g2.setFont(g2.getFont.deriveFont(Font.BOLD, fontSize.toFloat))
      for (r <- 0 until 3; c <- 0 until 3) {
        val v = lastBoard.getInt(r, c)
        if (v != 0) {
          val s = if (v == Player.xStone) "X" else "O"
          val x = c * cellW
          val y = r * cellH
          val fm = g2.getFontMetrics
          val textW = fm.stringWidth(s)
          val textH = fm.getAscent
          val tx = x + (cellW - textW) / 2
          val ty = y + (cellH + textH) / 2 - fm.getDescent
          g2.drawString(s, tx, ty)
        }
      }
    }

    private def coordsToCell(x: Int, y: Int): (Int, Int) = {
      val cellW = getWidth / 3
      val cellH = getHeight / 3
      val c = x / cellW
      val r = y / cellH
      (r, c)
    }
  }
}

object SwingTicTacToeUI {
  /**
   * A small bridge to deliver mouse-selected moves to the HumanAgent via a blocking queue.
   */
  object HumanUiInput {
    private var queueOpt: Option[SynchronousQueue[TttMove]] = None

    def enable(): Unit = synchronized {
      if (queueOpt.isEmpty) queueOpt = Some(new SynchronousQueue[TttMove]())
      tictactoe.agent.TttHumanAgent.setUiQueue(queueOpt)
    }

    def offer(m: TttMove): Unit = queueOpt.foreach(_.offer(m))

    // For testing or cleanup
    def disable(): Unit = synchronized { queueOpt = None; tictactoe.agent.TttHumanAgent.setUiQueue(None) }
  }
}
