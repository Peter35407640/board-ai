package io.github.pbanasiak.boardai.fiveinrow.nn

import io.github.pbanasiak.boardai.nn.PolicyValueModel
import org.deeplearning4j.nn.api.OptimizationAlgorithm
import org.deeplearning4j.nn.conf.layers.{ConvolutionLayer, DenseLayer, GlobalPoolingLayer, OutputLayer, PoolingType, SubsamplingLayer}
import org.deeplearning4j.nn.conf.{MultiLayerConfiguration, NeuralNetConfiguration}
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.deeplearning4j.nn.weights.WeightInit
import org.deeplearning4j.optimize.listeners.ScoreIterationListener
import org.nd4j.linalg.activations.Activation
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.dataset.DataSet
import org.nd4j.linalg.factory.Nd4j
import org.nd4j.linalg.learning.config.Adam
import org.nd4j.linalg.lossfunctions.LossFunctions

import java.io.File

/**
 * Simplified CNN-based PolicyValueModel using separate networks for policy and value.
 * This approach avoids the shared trunk issues that were causing dead value heads.
 */
final class FiveInRowCnnPolicyValueModel(
  val boardSize: Int,
  val nOutput: Int,
  config: FiveInRowModelConfig
) extends PolicyValueModel[INDArray] {

  private def randomSeed(): Long = java.lang.System.currentTimeMillis() + scala.util.Random.nextLong()


  // Separate policy network - more complex since it needs to distinguish between many moves
  private val policyConf: MultiLayerConfiguration = new NeuralNetConfiguration.Builder()
    .seed(randomSeed())
    .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
    .updater(new Adam(config.learningRate))
    .weightInit(WeightInit.XAVIER)
    .list()
    .layer(0, new ConvolutionLayer.Builder(3, 3)
      .nIn(3).nOut(32).stride(1, 1).padding(0, 0).activation(Activation.LEAKYRELU).build())
    .layer(1, new DenseLayer.Builder()
      .nOut(64).activation(Activation.LEAKYRELU).build())
    .layer(2, new OutputLayer.Builder(LossFunctions.LossFunction.MCXENT)
      .nOut(nOutput).activation(Activation.SOFTMAX).build())
    .setInputType(org.deeplearning4j.nn.conf.inputs.InputType.convolutional(boardSize, boardSize, 3))
    .build()

  private val valueConf: MultiLayerConfiguration = new NeuralNetConfiguration.Builder()
    .seed(randomSeed())
    .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
    .updater(new Adam(config.learningRate))
    .weightInit(WeightInit.XAVIER)
    .list()
    .layer(0, new ConvolutionLayer.Builder(3, 3)
      // 3x3 kernel, 3^3 combinations=9 *3 planes = 27 , * 4 directions = 108 , round to 128
      .nIn(3).nOut(128).stride(1, 1).padding(0, 0).activation(Activation.LEAKYRELU).build())
    .layer(1, new DenseLayer.Builder()
      .nOut(64).activation(Activation.LEAKYRELU).build())
    .layer(2, new DenseLayer.Builder()
      .nOut(32).activation(Activation.LEAKYRELU).build())
    .layer(3, new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
      .nOut(1).activation(Activation.TANH).build())
    .setInputType(org.deeplearning4j.nn.conf.inputs.InputType.convolutional(boardSize, boardSize, 3))
    .build()

  private val policyNet = new MultiLayerNetwork(policyConf)
   val valueNet = new MultiLayerNetwork(valueConf)

  try {
    policyNet.init()
    valueNet.init()

    policyNet.setListeners(new ScoreIterationListener(500))
    valueNet.setListeners(new ScoreIterationListener(500))

    val policyParamCount = policyNet.numParams()
    val valueParamCount = valueNet.numParams()

    println("=== Separate CNN Networks Initialized ===")
    println(f"Policy network parameters: ${policyParamCount}%,d")
    println(f"Value network parameters: ${valueParamCount}%,d")
    println(f"Total parameters: ${policyParamCount + valueParamCount}%,d")

    // DEBUG: Get the actual parameter arrays and check them
    if (valueParamCount > 0) {
      val valueParamArray = valueNet.params() // This returns INDArray
      val firstFewValues = (0 until Math.min(10, valueParamArray.length().toInt)).map(i => valueParamArray.getDouble(i.toLong))
      println(s"Value network first 10 parameters: ${firstFewValues.mkString(", ")}")
      println(s"Value network parameter mean: ${valueParamArray.meanNumber().doubleValue()}")
      println(s"Value network parameter std: ${valueParamArray.stdNumber().doubleValue()}")
    } else {
      println("ERROR: Value network has 0 parameters!")
    }

  } catch {
    case e: Exception =>
      println(s"Failed to initialize separate CNN networks: ${e.getMessage}")
      e.printStackTrace()
      throw e
  }

  private def validateInputShape(input: INDArray, operation: String): Unit = {
    val shape = input.shape()
    val expectedShape = Array(1L, 3L, boardSize.toLong, boardSize.toLong)

    require(shape.length == 4,
      s"$operation input must be 4D, got ${shape.length}D with shape [${shape.mkString(", ")}]")

    require(java.util.Arrays.equals(shape, expectedShape),
      s"$operation input shape [${shape.mkString(", ")}] doesn't match expected [${expectedShape.mkString(", ")}]")
  }

  def policy(input: INDArray): Array[Double] = {
    validateInputShape(input, "policy")
    //TODO perhaps crash instead of continue
    try {
      val output = policyNet.output(input)
      output.toDoubleVector
    } catch {
      case e: Exception =>
        println(s"Error in policy prediction: ${e.getMessage}")
        Array.fill(nOutput)(1.0 / nOutput)
    }
  }

  def value(input: INDArray): Double = {
    validateInputShape(input, "value")

    //TODO perhaps crash instead of continue
    try {
      val output = valueNet.output(input)
      output.getDouble(0L)
    } catch {
      case e: Exception =>
        println(s"Error in value prediction: ${e.getMessage}")
        0.0
    }
  }

  // Update training method for value network
  def trainBatch(
    inputs: Array[INDArray],
    policyTargets: Array[INDArray],
    valueTargets: Array[Double],
    epochs: Int = 10
  ): Unit = {
    if (inputs.isEmpty) return

    try {
      // shape: [batchSize, 3, boardSize, boardSize]
      val policyInputData: INDArray = Nd4j.concat(0, inputs *)
      val policyInputShape = policyInputData.shape()

      // Stack policy targets into batch: (nSamples, boardSize * boardSize)

      // Transform Array[INDArray] where each has shape [49]
      // into single INDArray with shape [batchSize, 49]
      val policyTargetData: INDArray = Nd4j.stack(0, policyTargets *)
      val policyTargetShape = policyTargetData.shape()
      val policyDataSet = new DataSet(policyInputData, policyTargetData)

      // Same concatenation for value network
      val valueInputData: INDArray = Nd4j.concat(0, inputs *)
      val valueInputShape = valueInputData.shape()
      val valueTargetData: INDArray = Nd4j.create(valueTargets).reshape(valueTargets.length, 1)
      val valueTargetShape = valueTargetData.shape()
      val valueDataSet = new DataSet(valueInputData, valueTargetData)
      //TODO add shape(s) assertion for policyInputData,policyTargetData,valueInputData,valueTargetData

      // DEBUG: Check the shapes of the input and target data
      val batchSize = inputs.length.toLong
      val expectedInputShape = Array(batchSize, 3L, boardSize.toLong, boardSize.toLong)
      val expectedPolicyTargetShape = Array(batchSize, nOutput.toLong)
      val expectedValueTargetShape = Array(batchSize, 1L)

      require(java.util.Arrays.equals(policyInputData.shape(), expectedInputShape),
        s"policyInputData shape [${policyInputData.shape().mkString(", ")}] doesn't match expected [${expectedInputShape.mkString(", ")}]")

      require(java.util.Arrays.equals(policyTargetData.shape(), expectedPolicyTargetShape),
        s"policyTargetData shape [${policyTargetData.shape().mkString(", ")}] doesn't match expected [${expectedPolicyTargetShape.mkString(", ")}]")

      require(java.util.Arrays.equals(valueInputData.shape(), expectedInputShape),
        s"valueInputData shape [${valueInputData.shape().mkString(", ")}] doesn't match expected [${expectedInputShape.mkString(", ")}]")

      require(java.util.Arrays.equals(valueTargetData.shape(), expectedValueTargetShape),
        s"valueTargetData shape [${valueTargetData.shape().mkString(", ")}] doesn't match expected [${expectedValueTargetShape.mkString(", ")}]")

      println(s"Training CNN networks on ${inputs.length} examples for $epochs epochs...")

      for (epoch <- 1 to epochs) {
        policyNet.fit(policyDataSet)
        valueNet.fit(valueDataSet)

        if (epoch % math.max(1, epochs / 3) == 0) {
          val policyScore = policyNet.score()
          val valueScore = valueNet.score()
          println(f"Epoch $epoch: Policy = ${policyScore}%.4f, Value = ${valueScore}%.4f")
        }
      }

      println("CNN training completed successfully")

    } catch {
      case e: Exception =>
        println(s"CNN training failed: ${e.getMessage}")
        e.printStackTrace()
    }
  }

  def save(basePath: String): Unit = {
    try {
      val modelsDir = new File("models")
      if (!modelsDir.exists()) modelsDir.mkdirs()

      val policyFile = new File(s"${basePath}_policy.zip")
      val valueFile = new File(s"${basePath}_value.zip")

      policyNet.save(policyFile)
      valueNet.save(valueFile)

      println(s"Policy network saved to ${policyFile.getPath}")
      println(s"Value network saved to ${valueFile.getPath}")
    } catch {
      case e: Exception =>
        println(s"Model save failed: ${e.getMessage}")
        e.printStackTrace()
    }
  }

  def save(): Unit = save(config.modelPathForSize)

  def load(basePath: String): Boolean = {
    try {
      val policyFile = new File(s"${basePath}_policy.zip")
      val valueFile = new File(s"${basePath}_value.zip")

      if (!policyFile.exists() || !valueFile.exists()) {
        println(s"Model files not found: ${policyFile.getPath} or ${valueFile.getPath}")
        return false
      }

      // Option 1: Replace the entire networks instead of just copying parameters
      val loadedPolicyNet = MultiLayerNetwork.load(policyFile, true)
      val loadedValueNet = MultiLayerNetwork.load(valueFile, true)

      // Clear the old networks and replace them entirely
      this.policyNet.setParams(loadedPolicyNet.params())
      this.policyNet.setUpdater(loadedPolicyNet.getUpdater())

      this.valueNet.setParams(loadedValueNet.params())
      this.valueNet.setUpdater(loadedValueNet.getUpdater())

      // Ensure listeners are set
      policyNet.setListeners(new ScoreIterationListener(500))
      valueNet.setListeners(new ScoreIterationListener(500))

      println(s"Models loaded from ${policyFile.getPath} and ${valueFile.getPath}")
      true
    } catch {
      case e: Exception =>
        println(s"Model load failed: ${e.getMessage}")
        e.printStackTrace()
        false
    }
  }

  def load(): Boolean = load(config.modelPathForSize)

  def getConfig: FiveInRowModelConfig = config

  def printArchitectureSummary(): Unit = {
    println("\n=== Policy Network Architecture ===")
    println(policyNet.summary())
    println("\n=== Value Network Architecture ===")
    println(valueNet.summary())
  }
}