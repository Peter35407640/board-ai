package io.github.pbanasiak.boardai.fiveinrow.nn

import io.github.pbanasiak.boardai.nn.PolicyValueModel
import org.deeplearning4j.nn.api.OptimizationAlgorithm
import org.deeplearning4j.nn.conf.layers.{DenseLayer, OutputLayer}
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
import scala.util.{Random, Try}

/**
 * MLP-based PolicyValueModel for Five-in-Row using DL4J library.
 * This implementation uses DL4J's MultiLayerNetwork with separate outputs for policy and value.
 */
final class FiveInRowMlpPolicyValueModel(
  val nInput: Int,
  val nOutput: Int,
  config: FiveInRowModelConfig
) extends PolicyValueModel[Array[Double]] {


  // Create policy network configuration
  private val policyConf: MultiLayerConfiguration = new NeuralNetConfiguration.Builder()
    .seed(config.initSeed)
    .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
    .updater(new Adam(config.learningRate))
    .weightInit(WeightInit.XAVIER)
    .list()
    .layer(0, new DenseLayer.Builder()
      .nIn(nInput)
      .nOut(config.hiddenSize)
      .activation(Activation.RELU)
      .build())
    .layer(1, new DenseLayer.Builder()
      .nIn(config.hiddenSize)
      .nOut(config.hiddenSize)
      .activation(Activation.RELU)
      .build())
    .layer(2, new OutputLayer.Builder(LossFunctions.LossFunction.MCXENT)
      .nIn(config.hiddenSize)
      .nOut(nOutput)
      .activation(Activation.SOFTMAX)
      .build())
    .build()

  // Create value network configuration
  private val valueConf: MultiLayerConfiguration = new NeuralNetConfiguration.Builder()
    .seed(config.initSeed)
    .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
    .updater(new Adam(config.learningRate))
    .weightInit(WeightInit.XAVIER)
    .list()
    .layer(0, new DenseLayer.Builder()
      .nIn(nInput)
      .nOut(config.hiddenSize)
      .activation(Activation.RELU)
      .build())
    .layer(1, new DenseLayer.Builder()
      .nIn(config.hiddenSize)
      .nOut(config.hiddenSize / 2)
      .activation(Activation.RELU)
      .build())
    .layer(2, new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
      .nIn(config.hiddenSize / 2)
      .nOut(1)
      .activation(Activation.TANH)
      .build())
    .build()

  // Initialize the networks
  private val policyNetwork = new MultiLayerNetwork(policyConf)
  private val valueNetwork = new MultiLayerNetwork(valueConf)

  // Initialize networks properly
  try {
    policyNetwork.init()
    valueNetwork.init()

    // Add listeners for training monitoring
    policyNetwork.setListeners(new ScoreIterationListener(100))
    valueNetwork.setListeners(new ScoreIterationListener(100))

    println("DL4J networks initialized successfully")
  } catch {
    case e: Exception =>
      println(s"Failed to initialize DL4J networks: ${e.getMessage}")
      throw e
  }

  override def policy(input: Array[Double]): Array[Double] = {
    try {
      val inputArray: INDArray = Nd4j.create(input).reshape(1, nInput)
      val output: INDArray = policyNetwork.output(inputArray)
      output.toDoubleVector
    } catch {
      case e: Exception =>
        println(s"Error in policy prediction: ${e.getMessage}")
        Array.fill(nOutput)(1.0 / nOutput) // Return uniform distribution on error
    }
  }

  override def value(input: Array[Double]): Double = {
    try {
      val inputArray: INDArray = Nd4j.create(input).reshape(1, nInput)
      val output: INDArray = valueNetwork.output(inputArray)
      output.getDouble(0L)
    } catch {
      case e: Exception =>
        println(s"Error in value prediction: ${e.getMessage}")
        0.0 // Return neutral value on error
    }
  }

  /**
   * Train the model on a batch of experiences using DL4J's built-in training methods.
   */
  def trainBatch(
    inputs: Array[Array[Double]],
    policyTargets: Array[Array[Double]],
    valueTargets: Array[Double],
    epochs: Int = 10
  ): Unit = {
    if (inputs.isEmpty) return

    require(
      inputs.length == policyTargets.length && inputs.length == valueTargets.length,
      "Input, policy target, and value target arrays must have same length"
    )

    try {
      // Prepare policy training data
      val policyInputData: INDArray = Nd4j.create(inputs)
      val policyTargetData: INDArray = Nd4j.create(policyTargets)
      val policyDataSet = new DataSet(policyInputData, policyTargetData)

      // Prepare value training data  
      val valueInputData: INDArray = Nd4j.create(inputs)
      val valueTargetData: INDArray = Nd4j.create(valueTargets.map(Array(_)))
      val valueDataSet = new DataSet(valueInputData, valueTargetData)

      println(s"Training DL4J networks on ${inputs.length} examples for $epochs epochs...")

      // Train policy network
      for (epoch <- 1 to epochs) {
        policyNetwork.fit(policyDataSet)
        if (epoch % (epochs / 2).max(1) == 0) {
          val policyScore = policyNetwork.score()
          println(f"Policy Network - Epoch $epoch: Score = ${policyScore}%.4f")
        }
      }

      // Train value network
      for (epoch <- 1 to epochs) {
        valueNetwork.fit(valueDataSet)
        if (epoch % (epochs / 2).max(1) == 0) {
          val valueScore = valueNetwork.score()
          println(f"Value Network - Epoch $epoch: Score = ${valueScore}%.4f")
        }
      }

      println(s"DL4J training completed successfully")

    } catch {
      case e: Exception =>
        println(s"DL4J training failed: ${e.getMessage}")
        e.printStackTrace()
    }
  }

  /**
   * Save model parameters to files using DL4J's built-in serialization
   */
  def save(basePath: String): Unit = {
    try {
      val modelsDir = new File("models")
      if (!modelsDir.exists()) modelsDir.mkdirs()

      val policyFile = new File(s"${basePath}_policy.zip")
      val valueFile = new File(s"${basePath}_value.zip")

      policyNetwork.save(policyFile)
      valueNetwork.save(valueFile)

      println(s"DL4J models saved to ${policyFile.getPath} and ${valueFile.getPath}")
    } catch {
      case e: Exception =>
        println(s"Model save failed: ${e.getMessage}")
        e.printStackTrace()
    }
  }
  /**
   * Save using the config's board-size-specific path
   */
  def save(): Unit = save(config.modelPathForSize)
  /**
   * Load model parameters from files using DL4J's built-in deserialization
   */
  def load(basePath: String): Boolean = {
    try {
      val policyFile = new File(s"${basePath}_policy.zip")
      val valueFile = new File(s"${basePath}_value.zip")

      if (!policyFile.exists() || !valueFile.exists()) {
        println(s"Model files not found: ${policyFile.getPath} or ${valueFile.getPath}")
        return false
      }

      val loadedPolicyNetwork = MultiLayerNetwork.load(policyFile, true)
      val loadedValueNetwork = MultiLayerNetwork.load(valueFile, true)

      println(s"DL4J models loaded from ${policyFile.getPath} and ${valueFile.getPath}")
      true
    } catch {
      case e: Exception =>
        println(s"Model load failed: ${e.getMessage}")
        e.printStackTrace()
        false
    }
  }

  /**
   * Load using the config's board-size-specific path
   */
  def load(): Boolean = load(config.modelPathForSize)


  /**
   * Get policy network for advanced operations
   */
  def getPolicyNetwork: MultiLayerNetwork = policyNetwork

  /**
   * Get value network for advanced operations  
   */
  def getValueNetwork: MultiLayerNetwork = valueNetwork
  def getConfig: FiveInRowModelConfig = config

}