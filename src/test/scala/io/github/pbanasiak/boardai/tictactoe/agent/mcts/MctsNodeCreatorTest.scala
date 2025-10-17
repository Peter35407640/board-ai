package io.github.pbanasiak.boardai.tictactoe.agent.mcts

import org.scalactic.Tolerance.convertNumericToPlusOrMinusWrapper
import org.scalatest.funsuite.AnyFunSuite
import io.github.pbanasiak.boardai.mcts.{MctsNodeCreator, NodeCreator}
import io.github.pbanasiak.boardai.tictactoe.{TttBoard, TttGame, TttMove}
import io.github.pbanasiak.boardai.{GameState, Player, Score}


class MctsNodeCreatorTest extends AnyFunSuite {
  val tttGame = new TttGame()
  
  private def createEmptyBoard(): TttBoard = {
    TttBoard.zeros(3, 3)
  }

  private def createGameState(board: TttBoard = createEmptyBoard()): GameState[TttBoard, TttMove] = {
    new GameState(board, Player.xPlayer, None)
  }

  test("createNode should create root node with correct properties") {
    val nodeCreator = MctsNodeCreator(tttGame)
    val gameState = createGameState()

    val node = nodeCreator.createNode(gameState)

    // Basic properties
    assert(node.gameState === gameState)
    assert(node.priorScore === Score(-1.0))
    assert(node.parent.isEmpty)
    assert(node.lastMove.isEmpty)
    assert(node.totalVisitCount === 0)

    // Should have branches for all valid moves
    val validMoves = tttGame.validMoves(gameState)
    assert(node.moves.length === validMoves.length)
    assert(node.moves.toSet === validMoves.toSet)
  }

  test("createNode should create uniform priors for all valid moves") {
    val nodeCreator = MctsNodeCreator(tttGame)
    val gameState = createGameState()
    val validMoves = tttGame.validMoves(gameState)

    val node = nodeCreator.createNode(gameState)

    val expectedPrior = 1.0 / validMoves.size
    validMoves.foreach { move =>
      // GOOD - handles potential floating point precision issues
      assert(node.prior(move) === expectedPrior +- 1e-12)
    }
  }

  test("createNode should handle parent-child relationship") {
    val nodeCreator = MctsNodeCreator(tttGame)
    val parentGameState = createGameState()
    val parentNode = nodeCreator.createNode(parentGameState)

    val move = TttMove(0, 0)
    val childGameState =tttGame.applyMove( parentGameState,move)

    val childNode = nodeCreator.createNode(childGameState, Some(move), Some(parentNode))

    // Child properties
    assert(childNode.parent.isDefined)
    assert(childNode.parent.get === parentNode)
    assert(childNode.lastMove === Some(move))

    // Parent should have child registered
    assert(parentNode.hasChild(move))
    assert(parentNode.getChild(move) === Some(childNode))
  }

  test("createNode should handle empty valid moves") {
    val nodeCreator = MctsNodeCreator(tttGame)

    // Create a full board (no valid moves)
    val fullBoardArray = Array.fill(3, 3)(1)
    val fullBoard = TttBoard(fullBoardArray)
    val gameState = createGameState(fullBoard)

    val node = nodeCreator.createNode(gameState)

    assert(node.moves.isEmpty)
    assert(node.branches.isEmpty)
  }

  test("createNode should handle single valid move") {
    val nodeCreator = MctsNodeCreator(tttGame)

    // Create board with only one empty spot
    val nearFullArray = Array.fill(3, 3)(1)
    nearFullArray(2)(2) = 0 // One empty spot
    val nearFullBoard = TttBoard(nearFullArray)
    val gameState = createGameState(nearFullBoard)

    val node = nodeCreator.createNode(gameState)

    assert(node.moves.length === 1)
    assert(node.moves.head === TttMove(2, 2))
    assert(node.prior(TttMove(2, 2)) === 1.0) // 100% probability for single move
  }

  test("createNode without parent should not establish parent-child relationship") {
    val nodeCreator = MctsNodeCreator(tttGame)
    val gameState = createGameState()
    val move = TttMove(1, 1)

    val node = nodeCreator.createNode(gameState, Some(move), None)

    assert(node.parent.isEmpty)
    assert(node.lastMove === Some(move))
  }

  test("createNode without move should not establish parent-child relationship") {
    val nodeCreator = MctsNodeCreator(tttGame)
    val parentGameState = createGameState()
    val parentNode = nodeCreator.createNode(parentGameState)
    val childGameState = createGameState()

    val childNode = nodeCreator.createNode(childGameState, None, Some(parentNode))

    assert(childNode.parent === Some(parentNode))
    assert(childNode.lastMove.isEmpty)

    // Parent should not have the child registered for any move since no move was specified
    // We can verify this by checking that hasChild returns false for all valid moves
    val validMoves = tttGame.validMoves(parentGameState)
    validMoves.foreach { move =>
      assert(!parentNode.hasChild(move))
    }
  }

  test("createNode should maintain immutable uniform distribution") {
    val nodeCreator = MctsNodeCreator(tttGame)
    val gameState = createGameState()

    val node1 = nodeCreator.createNode(gameState)
    val node2 = nodeCreator.createNode(gameState)

    // Both nodes should have identical prior distributions
    val validMoves = tttGame.validMoves(gameState)
    validMoves.foreach { move =>
      assert(node1.prior(move) === node2.prior(move))
    }
  }

  test("all created nodes should have sum of priors equal to 1.0") {
    val nodeCreator = MctsNodeCreator(tttGame)

    // Test with various board configurations
    val configurations = List(
      createEmptyBoard(),
      createBoardWithOneMoveLeft(),
      createBoardWithTwoMovesLeft()
    )

    configurations.foreach { board =>
      val gameState = createGameState(board)
      val node = nodeCreator.createNode(gameState)

      if (node.moves.nonEmpty) {
        val totalPrior = node.moves.map(node.prior).sum
        assert(totalPrior === 1.0 +- 1e-12)
      }
    }
  }

  test("createNode should work with different players") {
    val nodeCreator = MctsNodeCreator(tttGame)

    val gameStateX = new GameState[TttBoard,TttMove](createEmptyBoard(), Player.xPlayer, None)
    val gameStateO = new GameState[TttBoard,TttMove](createEmptyBoard(), Player.oPlayer, None)

    val nodeX = nodeCreator.createNode(gameStateX)
    val nodeO = nodeCreator.createNode(gameStateO)

    // Both should have same structure but different game states
    assert(nodeX.moves.toSet === nodeO.moves.toSet)
    assert(nodeX.gameState.playerToMove !== nodeO.gameState.playerToMove)

    // Priors should be identical regardless of player
    nodeX.moves.foreach { move =>
      assert(nodeX.prior(move) === nodeO.prior(move) +- 1e-12)
    }
  }

  test("createNode should handle mid-game board states") {
    val nodeCreator = MctsNodeCreator(tttGame)

    // Create a mid-game board state
    val midGameArray = Array.fill(3, 3)(0)
    midGameArray(1)(1) = Player.xStone  // X in center
    midGameArray(0)(0) = Player.oStone  // O in corner
    midGameArray(2)(2) = Player.xStone  // X in opposite corner
    val midGameBoard = TttBoard(midGameArray)
    val gameState = createGameState(midGameBoard)

    val node = nodeCreator.createNode(gameState)
    val validMoves = tttGame.validMoves(gameState)

    assert(node.moves.length === validMoves.length)
    assert(node.moves.length === 6) // 9 - 3 occupied = 6

    val expectedPrior = 1.0 / 6.0
    validMoves.foreach { move =>
      assert(node.prior(move) === expectedPrior +- 1e-12)
    }
  }

  test("createNode should create independent node instances") {
    val nodeCreator = MctsNodeCreator(tttGame)
    val gameState = createGameState()

    val node1 = nodeCreator.createNode(gameState)
    val node2 = nodeCreator.createNode(gameState)

    // Nodes should be different instances
    assert(node1 ne node2) // Reference inequality

    // But have equivalent content
    assert(node1.gameState === node2.gameState)
    assert(node1.priorScore === node2.priorScore)
    assert(node1.moves.toSet === node2.moves.toSet)

    // Modifying one shouldn't affect the other
    node1.totalVisitCount = 5
    assert(node2.totalVisitCount === 0)
  }

  test("createNode should satisfy NodeCreator contract") {
    val nodeCreator = MctsNodeCreator(tttGame)
    val gameState = createGameState()

    // Test the trait contract
    assert(nodeCreator.isInstanceOf[NodeCreator[TttBoard, TttMove]])

    // Default parameters should work
    val node1 = nodeCreator.createNode(gameState)
    val node2 = nodeCreator.createNode(gameState, None)
    val node3 = nodeCreator.createNode(gameState, None, None)

    // All should be equivalent for root nodes
    assert(node1.parent === node2.parent)
    assert(node2.parent === node3.parent)
    assert(node1.lastMove === node2.lastMove)
    assert(node2.lastMove === node3.lastMove)
  }

  // Helper methods for board creation
  private def createBoardWithOneMoveLeft(): TttBoard = {
    val a = Array.fill(3, 3)(1)
    a(2)(2) = 0 // One empty spot
    TttBoard(a)
  }

  private def createBoardWithTwoMovesLeft(): TttBoard = {
    val a = Array.fill(3, 3)(1)
    a(0)(0) = 0
    a(2)(2) = 0
    TttBoard(a)
  }
}