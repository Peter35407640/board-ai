package io.github.pbanasiak.boardai.fiveinrow.ui

import io.github.pbanasiak.boardai.fiveinrow.{Board, FiveInRowGame, Move}
import io.github.pbanasiak.boardai.{GamePrinter, GameState, Player}

import java.awt.event.{MouseAdapter, MouseEvent}
import java.awt.*
import javax.swing.{JFrame, JPanel, SwingUtilities}
import scala.concurrent.Promise

/**
 * Minimal Swing UI to visualize the board and allow mouse input for HumanAgent.
 * Usage:
 *   val ui = new SwingUI(game)
 *   pass Some(ui) into Tournament.play* as gamePrinter
 *   And construct HumanAgent(game, Some(ui)) so it can wait for clicks.
 */
class SwingUI(game: FiveInRowGame) extends GamePrinter[Board, Move] {

  private val boardSize = game.gameConfig.boardSize

  private val frame = new JFrame("Five in Row")
  private val panel = new BoardPanel
  private val statusLabel = new javax.swing.JLabel("Ready")

  @volatile private var currentState: Option[GameState[Board, Move]] = None
  @volatile private var lastMove: Option[Move] = None

  // Promise used by HumanAgent to await next click
  @volatile private var nextClickPromise: Option[Promise[Move]] = None

  init()

  private def init(): Unit = {
    panel.setPreferredSize(new Dimension(600, 600))
    frame.setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE)
    frame.getContentPane.add(panel, java.awt.BorderLayout.CENTER)
    frame.getContentPane.add(statusLabel, java.awt.BorderLayout.SOUTH)
    frame.pack()
    frame.setLocationRelativeTo(null)
    frame.setVisible(true)
  }

  // GamePrinter interface
  override def printDraw(board: Board, playerToMove: Player): Unit = {
    // Ensure final board position is displayed on draw
    panel.setBoard(board)
    panel.setInfo(s"Draw. Last to move: $playerToMove")
    setStatus(s"Last game: DRAW")
  }

  override def printVictory(board: Board, playerToMove: Player): Unit = {
    // Ensure the last (winning) move is shown
    panel.setBoard(board)
    panel.setInfo(s"$playerToMove wins!")
    setStatus(s"Last game: $playerToMove WINS")
  }

  override def print(gameState: GameState[Board, Move]): Unit = {
    // Track the last move by comparing with previous state
    currentState match {
      case Some(prevState) =>
        // Find the difference between current and previous board to identify last move
        var foundMove: Option[Move] = None
        for (r <- 0 until boardSize; c <- 0 until boardSize if foundMove.isEmpty) {
          if (prevState.board.getInt(r, c) != gameState.board.getInt(r, c)) {
            foundMove = Some(Move(r, c))
          }
        }
        lastMove = foundMove
      case None =>
        lastMove = None
    }

    currentState = Some(gameState)
    val who = gameState.playerToMove
    panel.setInfo(s"To move: $who (stone ${who.getPlayerStone()})")
    panel.setBoard(gameState.board)
    panel.setLastMove(lastMove)
  }

  /** Provide a fresh Promise that will be completed on next valid click. */
  def awaitHumanMove(): Move = {
    val p = Promise[Move]()
    nextClickPromise = Some(p)
    // block current thread until completed
    scala.concurrent.Await.result(p.future, scala.concurrent.duration.Duration.Inf)
  }

  private def setStatus(text: String): Unit =
    SwingUtilities.invokeLater(() => statusLabel.setText(text))

  private class BoardPanel extends JPanel {
    private var info: String = ""
    private var board: Option[Board] = None
    private var lastMove: Option[Move] = None

    setBackground(Color.WHITE)

    addMouseListener(new MouseAdapter {
      override def mouseClicked(e: MouseEvent): Unit = {
        val size = getSize
        val cell = math.min(size.width, size.height) / boardSize.toDouble
        val x = e.getX
        val y = e.getY
        val col = math.floor(x / cell).toInt
        val row = math.floor(y / cell).toInt
        if (row >= 0 && row < boardSize && col >= 0 && col < boardSize) {
          // Validate move against current state board emptiness
          currentState.foreach { gs =>
            if (gs.board.getInt(row, col) == 0) {
              nextClickPromise.foreach { pr =>
                if (!pr.isCompleted) pr.trySuccess(Move(row, col))
              }
            }
          }
        }
      }
    })

    def setInfo(s: String): Unit = SwingUtilities.invokeLater(() => { info = s; repaint() })

    def setBoard(b: Board): Unit = SwingUtilities.invokeLater(() => { board = Some(b); repaint() })

    def setLastMove(move: Option[Move]): Unit = SwingUtilities.invokeLater(() => { lastMove = move; repaint() })



    override def paintComponent(g: Graphics): Unit = {
      super.paintComponent(g)
      val g2 = g.asInstanceOf[Graphics2D]
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      val w = getWidth
      val h = getHeight
      val size = math.min(w, h)
      val cell = size / boardSize.toDouble

      // draw grid
      g2.setColor(Color.LIGHT_GRAY)
      g2.setStroke(new BasicStroke(1f))
      for (i <- 0 to boardSize) {
        val p = (i * cell).toInt
        g2.drawLine(0, p, (boardSize * cell).toInt, p)
        g2.drawLine(p, 0, p, (boardSize * cell).toInt)
      }

      // draw stones
      board.foreach { b =>
        for (r <- 0 until boardSize; c <- 0 until boardSize) {
          val v = b.getInt(r, c)
          if (v != 0) {
            val cx = (c * cell).toInt
            val cy = (r * cell).toInt
            val pad = (cell * 0.1).toInt
            val d = (cell - 2 * pad).toInt

            // Highlight last move with colored background
            lastMove.foreach { move =>
              if (move.r == r && move.c == c) {
                g2.setColor(new Color(255, 255, 0, 100)) // Semi-transparent yellow
                g2.fillRect(cx, cy, cell.toInt, cell.toInt)
                g2.setColor(new Color(255, 215, 0)) // Golden border
                g2.setStroke(new BasicStroke(3f))
                g2.drawRect(cx + 1, cy + 1, cell.toInt - 2, cell.toInt - 2)
                g2.setStroke(new BasicStroke(1f)) // Reset stroke
              }
            }

            if (v == Player.xStone) g2.setColor(Color.BLACK) else g2.setColor(Color.WHITE)
            g2.fillOval(cx + pad, cy + pad, d, d)
            g2.setColor(Color.DARK_GRAY)
            g2.drawOval(cx + pad, cy + pad, d, d)
          }
        }
      }

      // info text
      g2.setColor(Color.BLACK)
      g2.drawString(info, 10, (boardSize * cell + 15).toInt min (h - 5))
    }

    override def getPreferredSize: Dimension = new Dimension(600, 620)
  }
}