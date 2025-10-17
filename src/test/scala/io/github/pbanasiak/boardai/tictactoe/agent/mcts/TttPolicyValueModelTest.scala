package io.github.pbanasiak.boardai.tictactoe.nn

import org.scalatest.funsuite.AnyFunSuite
import io.github.pbanasiak.boardai.tictactoe.{TttBoard, TttGame, TttMove}
import io.github.pbanasiak.boardai.tictactoe.encoders.Planes27Encoder
import io.github.pbanasiak.boardai.{GameState, Player}

class TttPolicyValueModelTest extends AnyFunSuite {

  val game = new TttGame()
  val encoder = new Planes27Encoder()
  private val config = TttPresets.Quick.model // Use debug config for fast tests

  val model = new TttPolicyValueModel(
    nInput = encoder.nInput,
    nOutput = encoder.nOutput,
    config = config
  )

  private val success = model.load(Some("models/tictactoe_quick")) // Use config's default path (models/tictactoe_quick)
  println(s"Model loaded from '${config.modelBasePath}': $success")

  test("debug - compare pure NN vs MCTS+NN performance") {
    val trainedModel = new TttPolicyValueModel(
      nInput = encoder.nInput,
      nOutput = encoder.nOutput,
      config = config
    )

    val loaded = trainedModel.load()
    assert(loaded, "Model should load successfully")
    println(s"Model loaded: $loaded")

    // Test 1: Pure NN policy (no MCTS)
    val winningBoard = Array(
      Array(Player.xStone, Player.xStone, 0),
      Array(Player.oStone, Player.oStone, 0),
      Array(0, 0, 0)
    )
    val board = TttBoard(winningBoard)
    val gameState = new GameState[TttBoard, TttMove](board, Player.xPlayer, None)
    val encoded = encoder.encodeGameState(gameState)
    val policy = trainedModel.policy(encoded)

    println(s"Pure NN policy - Position 2 prob: ${policy(2)}")

    // Test 2: Check if model actually changed during training
    val randomModel = new TttPolicyValueModel(
      nInput = encoder.nInput,
      nOutput = encoder.nOutput,
      config = config
    )

    val randomPolicy = randomModel.policy(encoded)
    println(s"Random model - Position 2 prob: ${randomPolicy(2)}")

    val difference = math.abs(policy(2) - randomPolicy(2))
    println(s"Difference: $difference")

    if (difference < 0.01) {
      println("❌ PROBLEM: Trained model is identical to random model!")
      println("   The save/load mechanism is broken or training didn't actually update weights")
    } else {
      println("✅ Models are different - but trained model might still be undertrained")
    }
  }

  test("policy works for simple winning position") {
    // X needs one more move to win horizontally in top row
    // Board: X X _
    //        O O _
    //        _ _ _
    val a = Array(
      Array(Player.xStone, Player.xStone, 0),
      Array(Player.oStone, Player.oStone, 0),
      Array(0, 0, 0)
    )
    val board = TttBoard(a)
    val gameState = new GameState[TttBoard, TttMove](board, Player.xPlayer, None)

    println(s"Board state (X to move, about to win at (0,2)):")
    println(gameState.board.toString)
    println(s"Current player: ${gameState.playerToMove}")

    // Encode the game state
    val encoded = encoder.encodeGameState(gameState)
    println(s"Encoded input length: ${encoded.length}")

    // Get policy prediction
    val policyOutput = model.policy(encoded)
    println(s"Policy output length: ${policyOutput.length}")
    println(s"Policy values: ${policyOutput.zipWithIndex.map { case (p, i) => f"$i:$p%.3f" }.mkString(", ")}")

    // Check that policy output has correct length
    assert(policyOutput.length == encoder.nOutput)

    // Check that all values are non-negative (valid probabilities)
    policyOutput.foreach(p => assert(p >= 0.0))

    // Check that probabilities sum to approximately 1.0
    val sum = policyOutput.sum
    println(s"Policy sum: $sum")
    assert(math.abs(sum - 1.0) < 0.001)

    // Show which moves are legal
    val legalMoves = game.validMoves(gameState)
    println(s"Legal moves: ${legalMoves.mkString(", ")}")

    // For this position, X should win by playing position 2 (top-right)
    println(s"Probability for winning move (position 2): ${policyOutput(2)}")
  }

  test("value prediction works") {
    // Empty board
    val a = Array(
      Array(0, 0, 0),
      Array(0, 0, 0),
      Array(0, 0, 0)
    )
    val board = TttBoard(a)
    val gameState = new GameState[TttBoard, TttMove](board, Player.xPlayer, None)

    println(s"Initial board:")
    println(gameState.board.toString)

    // Encode the game state
    val encoded = encoder.encodeGameState(gameState)

    // Get value prediction
    val valueOutput = model.value(encoded)
    println(s"Value prediction for initial state: $valueOutput")

    // Value should be between -1 and 1
    assert(valueOutput >= -1.1)
    assert(valueOutput <= 1.1)
  }


  test("value prediction for winning position X about to win") {
    val winningBoard = Array(
      Array(1, 1, 0),  // X, X, empty - winning opportunity
      Array(2, 0, 0),
      Array(0, 0, 2)
    )
    val board = TttBoard(winningBoard)

    // Model always returns values from X's perspective (no conversion needed)

    // X to move (X is about to win, should be positive from X's perspective)
    val gsX = new GameState[TttBoard, TttMove](board, Player.xPlayer, None)
    val encX = encoder.encodeGameState(gsX)
    val valueX = model.value(encX)

    // O to move (X is still about to win, should still be positive from X's perspective)
    val gsO = new GameState[TttBoard, TttMove](board, Player.oPlayer, None)
    val encO = encoder.encodeGameState(gsO)
    val valueO = model.value(encO)

    println(s"X to move: value=$valueX (X's perspective)")
    println(s"O to move: value=$valueO (X's perspective)")

    // Both should be positive since X is advantaged in this position
    assert(valueX > 0.0, s"Expected positive value for X-advantageous position; got $valueX")
    assert(valueO > 0.0, s"Expected positive value for X-advantageous position; got $valueO")

    // The values might be slightly different due to the turn information, but both positive
    println(s"✅ Both evaluations are positive as expected for X-winning position")
  }

  test("value prediction perspective analysis") {
    // Test a clearly winning position for X
    val xWinningBoard = Array(
      Array(1, 1, 1),  // X wins horizontally
      Array(0, 0, 0),
      Array(0, 0, 0)
    )
    val xWinBoard = TttBoard(xWinningBoard)
    val xWinState = new GameState[TttBoard, TttMove](xWinBoard, Player.xPlayer, None)

    val xWinEncoded = encoder.encodeGameState(xWinState)
    val xWinValue = model.value(xWinEncoded)
    println(s"X already won - Value: $xWinValue")

    // Test a clearly losing position for current player
    val oLosingBoard = Array(
      Array(1, 1, 1),  // X wins, but O to move (game over)
      Array(0, 0, 0),
      Array(0, 0, 0)
    )
    val oLoseBoard = TttBoard(oLosingBoard)
    val oLoseState = new GameState[TttBoard, TttMove](oLoseBoard, Player.oPlayer, None)

    val oLoseEncoded = encoder.encodeGameState(oLoseState)
    val oLoseValue = model.value(oLoseEncoded)
    println(s"X already won, O to move - Value: $oLoseValue")

    // Test empty board from both perspectives
    val emptyBoard = Array(
      Array(0, 0, 0),
      Array(0, 0, 0),
      Array(0, 0, 0)
    )
    val emptyBoardObj = TttBoard(emptyBoard)

    val xStartState = new GameState[TttBoard, TttMove](emptyBoardObj, Player.xPlayer, None)
    val xStartEncoded = encoder.encodeGameState(xStartState)
    val xStartValue = model.value(xStartEncoded)

    val oStartState = new GameState[TttBoard, TttMove](emptyBoardObj, Player.oPlayer, None)
    val oStartEncoded = encoder.encodeGameState(oStartState)
    val oStartValue = model.value(oStartEncoded)

    println(s"Empty board - X to move: $xStartValue")
    println(s"Empty board - O to move: $oStartValue")

    println("\n=== Analysis ===")
    if (math.abs(xStartValue + oStartValue) < 0.1) {
      println("✓ Model appears to evaluate from current player's perspective (values are opposite)")
    } else if (math.abs(xStartValue - oStartValue) < 0.1) {
      println("✓ Model appears to evaluate from a fixed perspective (values are similar)")
    } else {
      println("? Model perspective is unclear from empty board test")
    }
  }

  test("handles different board positions") {
    val testCases = List(
      (Array(
        Array(0, 0, 0),
        Array(0, 0, 0),
        Array(0, 0, 0)
      ), Player.xPlayer, "empty board"),

      (Array(
        Array(0, 0, 0),
        Array(0, Player.xStone, 0),
        Array(0, 0, 0)
      ), Player.oPlayer, "X plays center"),

      (Array(
        Array(Player.oStone, 0, 0),
        Array(0, Player.xStone, 0),
        Array(0, 0, 0)
      ), Player.xPlayer, "X center, O top-left"),

      (Array(
        Array(Player.oStone, 0, 0),
        Array(0, Player.xStone, 0),
        Array(0, 0, Player.xStone)
      ), Player.oPlayer, "X center, O top-left, X bottom-right")
    )

    for ((boardArray, currentPlayer, description) <- testCases) {
      println(s"\n--- Test: $description ---")

      val board = TttBoard(boardArray)
      val gameState = new GameState[TttBoard, TttMove](board, currentPlayer, None)

      println(gameState.board.toString)
      println(s"Current player: ${gameState.playerToMove}")

      val encoded = encoder.encodeGameState(gameState)
      val policyOutput = model.policy(encoded)
      val valueOutput = model.value(encoded)

      println(s"Policy sum: ${policyOutput.sum}")
      println(s"Value: $valueOutput")

      // Basic sanity checks
      assert(policyOutput.length == encoder.nOutput)
      assert(math.abs(policyOutput.sum - 1.0) < 0.001)
      policyOutput.foreach(p => assert(p >= 0.0))
      assert(valueOutput >= -1.1)
      assert(valueOutput <= 1.1)

      // Show legal moves and their probabilities
      val legalMoves = game.validMoves(gameState)
      println(s"Legal moves: ${legalMoves.mkString(", ")}")
      legalMoves.foreach { move =>
        val moveIndex = encoder.moveToIndex(move)
        println(s"  Move $move (index $moveIndex): probability = ${policyOutput(moveIndex)}")
      }
    }
  }

  test("shows policy distribution for blocking position") {
    // O needs to block X from winning
    // Board: X X _  (X can win at (0,2))
    //        O _ _
    //        _ _ _
    val a = Array(
      Array(Player.xStone, Player.xStone, 0),
      Array(Player.oStone, 0, 0),
      Array(0, 0, 0)
    )
    val board = TttBoard(a)
    val gameState = new GameState[TttBoard, TttMove](board, Player.oPlayer, None)

    println(s"\n--- Testing blocking position ---")
    println(s"Board (O to move, must block X at (0,2)):")
    println(gameState.board.toString)

    val encoded = encoder.encodeGameState(gameState)
    val policyOutput = model.policy(encoded)

    println(s"Policy distribution:")
    for (i <- policyOutput.indices) {
      if (policyOutput(i) > 0.001) { // only show non-negligible probabilities
        println(f"  Position $i: ${policyOutput(i)}%.4f")
      }
    }

    val legalMoves = game.validMoves(gameState)
    println(s"Legal moves: ${legalMoves.mkString(", ")}")

    // Position 2 should block X's win
    println(f"Probability for blocking move (position 2): ${policyOutput(2)}%.4f")

    // Basic sanity checks
    assert(policyOutput.length == encoder.nOutput)
    assert(math.abs(policyOutput.sum - 1.0) < 0.001)
    policyOutput.foreach(p => assert(p >= 0.0))
  }

  test("model configuration is accessible") {
    println(s"Model config: ${model.getConfig}")
    assert(model.getConfig == config)
  }

  test("model can be saved and loaded") {
    // Create a temporary model path for testing
    val testConfig = config.copy(modelBasePath = "models/test_model")
    val testModel = new TttPolicyValueModel(
      nInput = encoder.nInput,
      nOutput = encoder.nOutput,
      config = testConfig
    )

    // Test saving
    testModel.save()
    println(s"Test model saved to ${testConfig.modelBasePath}")

    // Test loading
    val loadedModel = new TttPolicyValueModel(
      nInput = encoder.nInput,
      nOutput = encoder.nOutput,
      config = testConfig
    )

    val loadSuccess = loadedModel.load()
    assert(loadSuccess, "Model should load successfully after saving")

    // Compare outputs to ensure they match
    val testInput = Array.fill(encoder.nInput)(0.1)
    val originalPolicy = testModel.policy(testInput)
    val loadedPolicy = loadedModel.policy(testInput)

    // Policies should be very close (allowing for small numerical differences)
    for (i <- originalPolicy.indices) {
      assert(math.abs(originalPolicy(i) - loadedPolicy(i)) < 0.001,
        s"Policy outputs should match after save/load at index $i")
    }

    val originalValue = testModel.value(testInput)
    val loadedValue = loadedModel.value(testInput)
    assert(math.abs(originalValue - loadedValue) < 0.001,
      "Value outputs should match after save/load")

    println("✅ Save/load test passed")
  }
}