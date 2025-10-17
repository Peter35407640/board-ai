package io.github.pbanasiak.boardai.tictactoe.agent.mcts

import org.scalatest.funsuite.AnyFunSuite
import io.github.pbanasiak.boardai.mcts.{MctsAgent, MctsNodeCreator, MonteCarloPlayer}
import io.github.pbanasiak.boardai.tictactoe.{TttBoard, TttMove, TttGame}
import io.github.pbanasiak.boardai.{GameState, Player}

import scala.util.Random

class MctsAgentDebugTest extends AnyFunSuite {
  val tttGame = new TttGame()

  test("Debug MCTS score calculation step by step") {
    val rnd = new Random(123)
    val nodeCreator = MctsNodeCreator(tttGame)
    val mcPlayer = MonteCarloPlayer(tttGame, nodeCreator, rnd)
    val agent = new MctsAgent(tttGame, nodeCreator, mcPlayer, None, 100, 2.0, rnd, selectRandomly = false, debug = true)

    // X can win immediately at (0,2)
    val winningBoard = Array(
      Array(Player.xStone, Player.xStone, 0), // X X _
      Array(Player.oStone, 0, 0), // O _ _
      Array(0, 0, 0) // _ _ _
    )
    val board = TttBoard(winningBoard)
    val gameState = new GameState[TttBoard, TttMove](board, Player.xPlayer, None)

    println("=== DEBUGGING WINNING POSITION ===")
    println("Board:")
    println(board.toString)
    println(s"Player to move: ${gameState.playerToMove}")
    println(s"Game over: ${gameState.isOver}")

    // Test MonteCarloPlayer directly on a winning terminal position
    val winningMove = TttMove(0, 2)
    val afterWinState = tttGame.applyMove(gameState, winningMove)
    println(s"\nAfter winning move ${winningMove}:")
    println(s"Player to move: ${afterWinState.playerToMove}")
    println(s"Game over: ${afterWinState.isOver}")
    println(s"Game result: ${afterWinState.gameResult}")
    //    if (afterWinState.score.isDefined) {
    //      println(s"Score from ${afterWinState.playerToMove}'s perspective: ${afterWinState.score.get}")
    //    }

    // Test MonteCarloPlayer on terminal position
    val terminalNode = nodeCreator.createNode(afterWinState)
    val mcScore = mcPlayer.valueFromMCPlayout(terminalNode)
    println(s"MC score from ${afterWinState.playerToMove}'s perspective: $mcScore")

    // Test MCTS agent
    val scoredMove = agent.selectScoredMove(gameState)
    println(s"\nMCTS result: ${scoredMove.move} with score ${scoredMove.score}")

    // Manual verification: If X plays (0,2), X should win
    assert(scoredMove.move == TttMove(0, 2), s"Expected winning move (0,2), got ${scoredMove.move}")
  }


  test("Debug root node scoring after MCTS") {
    val rnd = new Random(789)
    val nodeCreator = MctsNodeCreator(tttGame)
    val mcPlayer = MonteCarloPlayer(tttGame, nodeCreator, rnd)
    val agent = new MctsAgent(tttGame, nodeCreator, mcPlayer, None, 1000, 2.0, new Random(789), selectRandomly = false)

    val winningBoard = Array(
      Array(Player.xStone, Player.xStone, 0),
      Array(Player.oStone, 0, 0),
      Array(0, 0, 0)
    )
    val board = TttBoard(winningBoard)
    val gameState = new GameState[TttBoard, TttMove](board, Player.xPlayer, None)

    // Create root node and run MCTS
    val root = nodeCreator.createNode(gameState)

    for (i <- 0 until 1000) {
      agent.singlePlayout(root)
    }

    println("=== ROOT NODE ANALYSIS AFTER 1000 PLAYOUTS ===")
    println(s"Root total visit count: ${root.totalVisitCount}")

    root.branches.toSeq.sortBy(-_._2.visitCount).foreach { case (move, stats) =>
      val expectedValue = root.expectedValue(move)
      println(s"Move $move: visits=${stats.visitCount}, totalValue=${stats.totalValue}, avgValue=${expectedValue}")
    }

    val bestMove = agent.selectMostVisitedMove(root)
    val bestScore = root.expectedValue(bestMove)
    println(s"Best move: $bestMove with score: $bestScore")

    assert(bestMove == TttMove(0, 2), s"Expected best move (0,2), got $bestMove")
    assert(bestScore > 0.0, s"Expected positive score for best move, got ${bestScore}")
  }

  test("Test single playout in isolation") {
    val rnd = new Random(999)
    val nodeCreator = MctsNodeCreator(tttGame)
    val mcPlayer = MonteCarloPlayer(tttGame, nodeCreator, rnd)
    val agent = new MctsAgent(tttGame, nodeCreator, mcPlayer, None, 1, 2.0, rnd, selectRandomly = false)

    val winningBoard = Array(
      Array(Player.xStone, Player.xStone, 0),
      Array(Player.oStone, 0, 0),
      Array(0, 0, 0)
    )
    val board = TttBoard(winningBoard)
    val gameState = new GameState[TttBoard, TttMove](board, Player.xPlayer, None)

    val root = nodeCreator.createNode(gameState)

    println("=== SINGLE PLAYOUT DEBUG ===")
    println("Before playout:")
    println(s"Root visit count: ${root.totalVisitCount}")
    root.branches.foreach { case (move, stats) =>
      println(s"Move $move: visits=${stats.visitCount}, totalValue=${stats.totalValue}")
    }

    // Run one playout
    agent.singlePlayout(root)

    println("\nAfter one playout:")
    println(s"Root visit count: ${root.totalVisitCount}")
    root.branches.foreach { case (move, stats) =>
      println(s"Move $move: visits=${stats.visitCount}, totalValue=${stats.totalValue}")
    }

    // The visited branch should have a score recorded
    val visitedBranch = root.branches.find(_._2.visitCount > 0)
    assert(visitedBranch.isDefined, "At least one branch should have been visited")

    val (visitedMove, visitedStats) = visitedBranch.get
    println(s"Visited move $visitedMove got totalValue=${visitedStats.totalValue}")
  }

  test("Debug losing position perspective processing") {
    val rnd = new Random(789)
    val nodeCreator = MctsNodeCreator(tttGame)
    val mcPlayer = MonteCarloPlayer(tttGame, nodeCreator, rnd)
    val agent = new MctsAgent(tttGame, nodeCreator, mcPlayer, None, 100, 2.0, rnd, selectRandomly = false)

    // X is in a losing position - O has multiple threats
    val losingBoard = Array(
      Array(Player.oStone, Player.oStone, 0), // O O _  <- O can win at (0,2)
      Array(Player.xStone, 0, 0), // X _ _  <- O can also win at (1,1) creating fork
      Array(0, 0, Player.oStone) // _ _ O
    )
    val board = TttBoard(losingBoard)
    val gameState = new GameState[TttBoard, TttMove](board, Player.xPlayer, None)

    println("=== DEBUG LOSING POSITION ===")
    println("Board:")
    println(board.toString)
    println(s"Player to move: ${gameState.playerToMove}")
    println("Available moves for X: " + gameState.board.getCells.zipWithIndex.flatMap { case (row, r) =>
      row.zipWithIndex.collect { case (0, c) => TttMove(r, c) }
    }.toList)

    // Test what happens when X plays each possible move
    val possibleMoves = List(TttMove(0, 2), TttMove(1, 1), TttMove(1, 2), TttMove(2, 0), TttMove(2, 1))

    possibleMoves.foreach { xMove =>
      val stateAfterX = tttGame.applyMove(gameState, xMove)
      println(s"\nAfter X plays $xMove:")
      println(s"Player to move: ${stateAfterX.playerToMove}")
      println(s"Game over: ${stateAfterX.isOver}")

      if (!stateAfterX.isOver) {
        // Check if O has immediate winning moves
        val oWinningMoves = tttGame.validMoves(stateAfterX).filter { oMove =>
          val finalState = tttGame.applyMove(stateAfterX, oMove)
          finalState.isOver && finalState.isVictoryOf(Player.oPlayer)
        }
        println(s"O's immediate winning moves: $oWinningMoves")

        // Test immediate win detection
        val oNode = nodeCreator.createNode(stateAfterX)
        val detectedWin = agent.selectBranch(oNode)
        println(s"Agent selected for O: $detectedWin")

        if (oWinningMoves.nonEmpty) {
          val finalState = tttGame.applyMove(stateAfterX, detectedWin)
          println(s"Final result: ${finalState.gameResult}")
          //          if (finalState.score.isDefined) {
          //            println(s"Score from ${finalState.playerToMove}'s perspective: ${finalState.score.get}")
          //          }
        }
      }
    }

    // Run MCTS on the losing position
    println("\n=== RUNNING MCTS ON LOSING POSITION ===")
    val root = nodeCreator.createNode(gameState)

    // Run a few playouts and analyze results
    for (i <- 0 until 10) {
      agent.singlePlayout(root)
      if (i < 3) {
        println(s"After playout $i:")
        root.branches.foreach { case (move, stats) =>
          val avgScore = if (stats.visitCount > 0) stats.totalValue / stats.visitCount else 0.0
          println(s"  Move $move: visits=${stats.visitCount}, totalValue=${stats.totalValue}, avg=$avgScore")
        }
      }
    }

    val finalResult = agent.selectScoredMove(gameState)
    println(s"\nFinal MCTS result: move=${finalResult.move}, score=${finalResult.score}")

    // The score should be negative since X is in a losing position
    assert(finalResult.score.value < 0.0,
      s"Expected negative score for X's losing position, got ${finalResult.score}")
  }

  test("Debug MCTS score calculation step by step2") {
    val rnd = new Random(123)
    val nodeCreator = MctsNodeCreator(tttGame)
    val mcPlayer = MonteCarloPlayer(tttGame, nodeCreator, rnd)
    val agent = new MctsAgent(tttGame, nodeCreator, mcPlayer, None, 100, 2.0, rnd, selectRandomly = false)

    // Losing position for X
    val losingBoard = Array(
      Array(Player.oStone, Player.oStone, 0), // O O _
      Array(Player.xStone, 0, 0), // X _ _
      Array(0, 0, Player.oStone) // _ _ O
    )
    val board = TttBoard(losingBoard)
    val gameState = new GameState[TttBoard, TttMove](board, Player.xPlayer, None)

    println("=== DEBUG LOSING POSITION ===")
    println("Board:")
    println(board.toString)
    println(s"Player to move: ${gameState.playerToMove}")
    println(s"Available moves for X: ${tttGame.validMoves(gameState)}")

    // Test each move manually to verify they all lose
    tttGame.validMoves(gameState).foreach { move =>
      val afterMove = tttGame.applyMove(gameState, move)
      println(s"\nAfter X plays $move:")
      println(s"Player to move: ${afterMove.playerToMove}")
      println(s"Game over: ${afterMove.isOver}")

      if (!afterMove.isOver) {
        // Find O's immediate winning moves
        val oWinningMoves = tttGame.validMoves(afterMove).filter { oMove =>
          val afterOMove = tttGame.applyMove(afterMove, oMove)
          afterOMove.isOver && afterOMove.isVictoryOf(Player.oPlayer)
        }
        println(s"O's immediate winning moves: $oWinningMoves")

        // Simulate O playing optimally
        if (oWinningMoves.nonEmpty) {
          val oMove = oWinningMoves.head
          println(s"Agent selected for O: $oMove")
          val finalState = tttGame.applyMove(afterMove, oMove)
          println(s"Final result: ${finalState.gameResult}")
          //          println(s"Score from xPlayer's perspective: ${finalState.score}")
        }
      }
    }

    println("\n=== RUNNING MCTS ON LOSING POSITION ===")
    val root = nodeCreator.createNode(gameState)

    // Run a few playouts and print state after each
    for (i <- 0 until 5) {
      agent.singlePlayout(root)
      println(s"After playout $i:")
      root.moves.foreach { move =>
        val stats = root.branches(move)
        val expectedValue = root.expectedValue(move)
        println(s"  Move $move: visits=${stats.visitCount}, totalValue=${stats.totalValue}, avg=${expectedValue}")
      }
    }

    val selected = agent.selectMostVisitedMove(root)
    val finalScore = root.expectedValue(selected)
    println(s"\nFinal MCTS result: move=$selected, score=${finalScore}")

    // This should be negative for a losing position
    assert(finalScore < 0.0, s"Expected negative score for X's losing position, got ${finalScore}")
  }
}