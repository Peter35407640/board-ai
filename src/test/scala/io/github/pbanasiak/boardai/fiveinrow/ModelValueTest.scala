package io.github.pbanasiak.boardai.fiveinrow.test

import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.factory.Nd4j
import org.scalatest.funsuite.AnyFunSuite
import io.github.pbanasiak.boardai.fiveinrow.*
import io.github.pbanasiak.boardai.fiveinrow.encoders.Planes3FiveInRowEncode
import io.github.pbanasiak.boardai.fiveinrow.nn.{FiveInRowCnnPolicyValueModel, FiveInRowPresets}
import io.github.pbanasiak.boardai.{GameResult, GameState, Player}

import scala.util.Try

class ModelValueTest extends AnyFunSuite {

  val boardSize = 7
  val gameConfig = GameConfig(boardSize, 5)
  val encoder = new Planes3FiveInRowEncode(gameConfig)
  val modelBase = "models/fiveinrow_7x7_cnn_iter_90"

  // Shared model instance (lazy to avoid loading if tests are skipped)
  lazy val model: Option[FiveInRowCnnPolicyValueModel] = {
    val (modelConfig, _) = FiveInRowPresets.configForBoardSize(boardSize)
    val m = new FiveInRowCnnPolicyValueModel(boardSize = boardSize, nOutput = encoder.nOutput, config = modelConfig)
    if (Try(m.load(modelBase)).getOrElse(false)) Some(m) else None
  }

  lazy val freshModel: FiveInRowCnnPolicyValueModel = {
    val (modelConfig, _) = FiveInRowPresets.configForBoardSize(boardSize)
    val m = new FiveInRowCnnPolicyValueModel(boardSize = boardSize, nOutput = encoder.nOutput, config = modelConfig)
    m
  }


  test("model loads successfully") {
    assert(model.isDefined, s"Failed to load model from $modelBase")
  }

  test("terminal X-win position should have positive X-perspective value") {
    assume(model.isDefined, "Model not loaded")
    val m = model.get

    // X has five in a row at row=2, cols 0..4
    val xWinBoard = Array(
      Array(0, 0, 0, 0, 0, 0, 0),
      Array(0, 0, 0, 0, 0, 0, 0),
      Array(0, 1, 1, 1, 1, 0, 0), // X wins
      Array(0, 0, 0, 0, 0, 0, 0),
      Array(0, 0, 0, 0, 0, 0, 0),
      Array(0, 0, 0, 0, 0, 0, 0),
      Array(0, 0, 0, 0, 0, 0, 0),
    )
    val board = new Board(xWinBoard)
    val terminalState: GameState[Board, Move] = new GameState(board, Player.oPlayer, None)
    val encTerminal = encoder.encodeGameState(terminalState)
    val valTerminal = m.value(encTerminal)

    println(s"Terminal 4 X-win value (X-perspective) = $valTerminal")

    // X-win should have positive X-perspective value (close to +1)
    assert(valTerminal > 0.5, s"Expected positive X-perspective value for X-win, got $valTerminal")
  }

  test("terminal o-win position should have negative X-perspective value") {
    assume(model.isDefined, "Model not loaded")
    val m = model.get

    // X has five in a row at row=2, cols 0..4
    val oWinBoard = Array(
      Array(0, 0, 0, 0, 0, 0, 0),
      Array(0, 0, 0, 0, 0, 0, 0),
      Array(0, 2, 2, 2, 2, 0, 0), // X lost
      Array(0, 0, 0, 0, 0, 0, 0),
      Array(0, 0, 0, 0, 0, 0, 0),
      Array(0, 0, 0, 0, 0, 0, 0),
      Array(0, 0, 0, 0, 0, 0, 0),
    )
    val board = new Board(oWinBoard)
    val terminalState: GameState[Board, Move] = new GameState(board, Player.xPlayer, None)
    val encTerminal = encoder.encodeGameState(terminalState)
    val valTerminal = m.value(encTerminal)

    println(s"Terminal 4 o-win value (X-perspective) = $valTerminal")

    // X-win should have positive X-perspective value (close to +1)
    assert(valTerminal < -0.5, s"Expected positive X-perspective value for X-win, got $valTerminal")
  }


  test("one-move-finish-for-X position should have positive X-perspective value") {
    assume(model.isDefined, "Model not loaded")
    val m = model.get

    // X has four in a row with gaps: . X X X X . (O to move but X wins next)
    val x4Board = Array(
      Array(0, 0, 0, 0, 0, 0, 0),
      Array(0, 1, 1, 1, 1, 0, 0), // X cannot be blocked by O
      Array(0, 0, 0, 0, 0, 0, 0),
      Array(0, 0, 0, 0, 0, 0, 0),
      Array(0, 0, 0, 0, 0, 0, 0),
      Array(0, 0, 0, 0, 0, 0, 0),
      Array(0, 0, 0, 0, 0, 0, 0),
    )
    val board4x = new Board(x4Board)
    val gs = new GameState[Board, Move](board4x, Player.oPlayer, None)
    val encOneMove = encoder.encodeGameState(gs)
    val valOneMove = m.value(encOneMove)

    println(s"One-move-finish-for-X value (X-perspective) = $valOneMove")

    // X is about to win, so X-perspective value should be strongly positive
    assert(valOneMove > 0.5, s"Expected positive X-perspective value for X-about-to-win, got $valOneMove")
  }


  test("policy distribution analysis for blocking scenario") {
    assume(model.isDefined, "Model not loaded")
    val m = model.get

    val xWinBoard = Array(
      Array(0, 0, 0, 0, 0, 0, 0),
      Array(0, 0, 0, 0, 0, 0, 0),
      Array(0, 1, 1, 1, 0, 0, 0), // o must block
      Array(0, 0, 0, 0, 0, 0, 0),
      Array(0, 0, 0, 0, 0, 0, 0),
      Array(0, 0, 0, 0, 0, 0, 0),
      Array(0, 0, 0, 0, 0, 0, 0),
    )
    val board = new Board(xWinBoard)
    val gs: GameState[Board, Move] = new GameState(board, Player.oPlayer, None)

    val encoded = encoder.encodeGameState(gs)
    val policyOut = m.policy(encoded)
    val valueX = m.value(encoded)

    // Policy statistics
    val mean = policyOut.sum / policyOut.length
    val min = policyOut.min
    val max = policyOut.max
    val std = math.sqrt(policyOut.map(p => (p - mean) * (p - mean)).sum / policyOut.length)
    val argmax = policyOut.zipWithIndex.maxBy(_._1)._2

    println(s"Policy stats: mean=${f"$mean%.6f"}, min=${f"$min%.6f"}, max=${f"$max%.6f"}, std=${f"$std%.6f"}, argmaxIdx=$argmax")
    println(s"Blocking scenario value (X-perspective) = $valueX")

    // Expected blocking moves at (3,0) and (3,5)
    val expectedBlockingMoves = Seq((2, 0), (2, 4))
    val expectedIndices = expectedBlockingMoves.map { case (r, c) => encoder.moveToIndex(Move(r, c)) }
    val blockingProbs = expectedIndices.map(policyOut(_))

    println(s"Blocking probabilities: ${blockingProbs.zip(expectedBlockingMoves).map { case (p, coord) => s"$coord: ${f"$p%.4f"}" }.mkString(", ")}")

    // Policy should not be completely uniform (std should be > some threshold)
    val uniformStd = 0.0 // perfectly uniform would have 0 std after normalization
    assert(std > uniformStd, s"Policy appears too uniform, std=$std")
    assert(valueX > 0.0, s"Expected value for blocking scenario to be > 0.5, got $valueX. Board: \n$board")

    // Assert that the move with the highest policy probability is one of the blocking moves
    assert(expectedIndices.contains(argmax), s"Expected argmax ($argmax) to be one of the blocking moves ($expectedIndices). Board: \n$board")

    // Assert that the sum of probabilities for the blocking moves is high
    val blockingProbsSum = blockingProbs.sum
    assert(blockingProbsSum > 0.5, s"Expected sum of blocking probabilities > 0.5, got $blockingProbsSum. Board: \n$board")
  }


  test("debug modelValue 4x rows") {
    assume(model.isDefined, "Model not loaded")
    val m = model.get
    //    val m = freshModel

    // Test X-win position
    val xWinGameStates = (0 until 7).map { winningRow =>
      val boardArray = Array.tabulate(7, 7) { (row, col) =>
        if (row == winningRow && col >= 1 && col <= 4) 1 else 0
      }
      val board = new Board(boardArray)
      new GameState[Board, Move](board, Player.oPlayer, None)
    }

    val oWinGameStates = (0 until 7).map { winningRow =>
      val boardArray = Array.tabulate(7, 7) { (row, col) =>
        if (row == winningRow && col >= 1 && col <= 4) 2 else 0
      }
      val board = new Board(boardArray)
      new GameState[Board, Move](board, Player.xPlayer, None)
    }


    xWinGameStates.zipWithIndex.foreach { case (state, j) =>
      val encoded = encoder.encodeGameState(state)
      val eVal = m.value(encoded)

      println(f"X-win $j value: ${eVal}%.4f")
    }

    println
    oWinGameStates.zipWithIndex.foreach { case (state, j) =>
      val encoded = encoder.encodeGameState(state)
      val eVal = m.value(encoded)

      println(f"O-win $j value: ${eVal}%.4f")
    }

  }

  test("debug network output raw") {
    val m = freshModel

    // Test with a simple 1-element array
    val array: INDArray = Nd4j.create(encoder.nInput) // example shape
    val simpleInput = array.assign(0.5) // All 0.5s
    println(s"Simple input length: ${simpleInput.length}")
    println(s"Expected nInput: ${encoder.nInput}")

    // Test the value network directly if we can access it
    try {
      val value1 = m.value(simpleInput)
      println(s"Simple input (all 0.5) value: $value1")

      // Try different constant inputs
      val zeroInput = Nd4j.create(encoder.nInput).assign(0.0)
      val oneInput = Nd4j.create(encoder.nInput).assign(1.0)
      val randomInput = Nd4j.create(encoder.nInput).assign(scala.util.Random.nextDouble())

      println(s"All-zero input value: ${m.value(zeroInput)}")
      println(s"All-one input value: ${m.value(oneInput)}")
      println(s"Random input value: ${m.value(randomInput)}")

      // Test if we can access the raw network to see what's happening
      if (m.isInstanceOf[FiveInRowCnnPolicyValueModel]) {
        println("Can access the model - checking if it's actually initialized properly")
      }

    } catch {
      case e: Exception =>
        println(s"Error during debug test: ${e.getMessage}")
        e.printStackTrace()
    }
  }

  test("inspect loaded model parameters iter_5") {
    val (modelConfig, _) = FiveInRowPresets.configForBoardSize(boardSize)
    val m = new FiveInRowCnnPolicyValueModel(boardSize = boardSize, nOutput = encoder.nOutput, config = modelConfig)

    println("=== Before Loading ===")
    val beforeParams = m.valueNet.params()
    println(s"Value network param mean (before): ${beforeParams.meanNumber().doubleValue()}")
    println(s"Value network param std (before): ${beforeParams.stdNumber().doubleValue()}")

    val loaded = m.load("models/fiveinrow_7x7_cnn_iter_5")

    if (loaded) {
      println("\n=== After Loading iter_5 ===")
      val afterParams = m.valueNet.params()
      val firstTen = (0 until Math.min(10, afterParams.length().toInt)).map(i => afterParams.getDouble(i.toLong))

      println(s"Loaded successfully: $loaded")
      println(s"Value network param count: ${afterParams.length()}")
      println(s"Value network param mean: ${afterParams.meanNumber().doubleValue()}")
      println(s"Value network param std: ${afterParams.stdNumber().doubleValue()}")
      println(s"First 10 params: ${firstTen.mkString(", ")}")

      // Check if all parameters are the same (sign of initialization problem)
      val uniqueValues = (0 until Math.min(100, afterParams.length().toInt))
        .map(i => afterParams.getDouble(i.toLong))
        .distinct
        .size
      println(s"Unique values in first 100 params: $uniqueValues")

      // Test the model
      val emptyBoard = Array.tabulate(7, 7)((_, _) => 0)
      val emptyState = new GameState[Board, Move](new Board(emptyBoard), Player.xPlayer, None)
      val emptyEncoded = encoder.encodeGameState(emptyState)

      val xWinBoard = Array.tabulate(7, 7) { (row, col) =>
        if (row == 2 && col >= 1 && col <= 4) 1 else 0
      }
      val xWinState = new GameState[Board, Move](new Board(xWinBoard), Player.oPlayer, None)
      val xWinEncoded = encoder.encodeGameState(xWinState)

      println(s"\nEmpty board value: ${m.value(emptyEncoded)}")
      println(s"X-win board value: ${m.value(xWinEncoded)}")

      // Check the actual network output (before TANH)
      import scala.jdk.CollectionConverters._
      val feedForwardList = m.valueNet.feedForward(xWinEncoded, false).asScala.toList
      val rawOutput = feedForwardList.last
      println(s"Raw output (before TANH): ${rawOutput.getDouble(0L)}")

    } else {
      println("Failed to load model")
    }
  }

  
}