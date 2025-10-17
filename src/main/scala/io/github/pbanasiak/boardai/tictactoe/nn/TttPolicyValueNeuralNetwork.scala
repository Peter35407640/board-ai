
package io.github.pbanasiak.boardai.tictactoe.nn

import org.deeplearning4j.nn.conf.layers.{DenseLayer, OutputLayer}
import org.deeplearning4j.nn.conf.{MultiLayerConfiguration, NeuralNetConfiguration}
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.deeplearning4j.nn.weights.WeightInit
import org.nd4j.linalg.activations.Activation
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.dataset.DataSet
import org.nd4j.linalg.factory.Nd4j
import org.nd4j.linalg.learning.config.Adam
import org.nd4j.linalg.lossfunctions.LossFunctions
import io.github.pbanasiak.boardai.nn.PolicyValueModel

import java.io.File
import scala.util.Try

/**
 * TicTacToe Policy-Value neural network model using DeepLearning4J.
 * Uses a multi-layer perceptron with shared layers and two heads for policy and value prediction.
 */
final class TttPolicyValueModel(
  val nInput: Int,
  val nOutput: Int,
  config: TttModelConfig,
) extends PolicyValueModel[Array[Double]] {

  // Build the neural network architecture
  private val network: MultiLayerNetwork = buildNetwork()

  private def buildNetwork(): MultiLayerNetwork = {
    val conf: MultiLayerConfiguration = new NeuralNetConfiguration.Builder()
      .seed(config.initSeed)
      .weightInit(WeightInit.XAVIER)
      .updater(new Adam(config.learningRate))
      .list()
      // Shared hidden layers
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
      // Policy head - outputs probability distribution over moves
      .layer(2, new OutputLayer.Builder()
        .nIn(config.hiddenSize)
        .nOut(nOutput)
        .activation(Activation.SOFTMAX)
        .lossFunction(LossFunctions.LossFunction.MCXENT) // Multi-class cross-entropy
        .build())
      .build()

    val net = new MultiLayerNetwork(conf)
    net.init()
    net
  }

  // For value prediction, we need a separate small network since DL4J doesn't easily support multi-head
  private val valueNetwork: MultiLayerNetwork = buildValueNetwork()

  private def buildValueNetwork(): MultiLayerNetwork = {
    val conf: MultiLayerConfiguration = new NeuralNetConfiguration.Builder()
      .seed(config.initSeed)
      .weightInit(WeightInit.XAVIER)
      .updater(new Adam(config.learningRate))
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
      // Value head - single output for position evaluation
      .layer(2, new OutputLayer.Builder()
        .nIn(config.hiddenSize / 2)
        .nOut(1)
        .activation(Activation.TANH) // Output in [-1, 1]
        .lossFunction(LossFunctions.LossFunction.MSE) // Mean squared error
        .build())
      .build()

    val net = new MultiLayerNetwork(conf)
    net.init()
    net
  }

  override def policy(input: Array[Double]): Array[Double] = {
    val inputArray: INDArray = Nd4j.create(Array(input))
    val output: INDArray = network.output(inputArray)
    
    // Convert INDArray back to Scala Array[Double]
    val result = new Array[Double](nOutput)
    for (i <- 0 until nOutput) {
      result(i) = output.getDouble(0L, i)
    }
    result
  }

  override def value(input: Array[Double]): Double = {
    val inputArray: INDArray = Nd4j.create(Array(input))
    val output: INDArray = valueNetwork.output(inputArray)
    output.getDouble(0L, 0)
  }

  /**
   * Train the model on a batch of experiences.
   * @param inputs game state encodings
   * @param policyTargets target policy distributions (MCTS visit counts)
   * @param valueTargets target values from game outcomes
   * @param epochs number of training epochs
   */
  def trainBatch(inputs: Array[Array[Double]],
                policyTargets: Array[Array[Double]], 
                valueTargets: Array[Double],
                epochs: Option[Int] = None): Unit = {

    if (inputs.isEmpty) {
      println("No training data provided")
      return
    }

    require(inputs.length == policyTargets.length && inputs.length == valueTargets.length,
      "Input, policy target, and value target arrays must have same length")

    val actualEpochs = epochs.getOrElse(config.trainingEpochs)
    
    try {
      // Prepare data for policy network
      val policyInputs: INDArray = Nd4j.create(inputs)
      val policyOutputs: INDArray = Nd4j.create(policyTargets)
      val policyDataSet = new DataSet(policyInputs, policyOutputs)

      // Prepare data for value network  
      val valueInputs: INDArray = Nd4j.create(inputs)
      val valueOutputs: INDArray = Nd4j.create(valueTargets.map(Array(_)))
      val valueDataSet = new DataSet(valueInputs, valueOutputs)

      // Train both networks
      for (_ <- 0 until actualEpochs) {
        network.fit(policyDataSet)
        valueNetwork.fit(valueDataSet)
      }

      println(s"Trained model on ${inputs.length} examples for $actualEpochs epochs")

    } catch {
      case e: Exception =>
        println(s"Training failed: ${e.getMessage}")
        e.printStackTrace()
    }
  }

  /**
   * Save both networks to disk
   */
  def save(basePath: Option[String] = None): Unit = {
    val path = basePath.getOrElse(config.modelBasePath)
    
    try {
      // Create models directory if it doesn't exist
      val modelsDir = new File("models")
      if (!modelsDir.exists()) {
        modelsDir.mkdirs()
      }

      // Save both networks
      network.save(new File(s"${path}_policy.zip"))
      valueNetwork.save(new File(s"${path}_value.zip"))
      
      println(s"Model saved to ${path}_policy.zip and ${path}_value.zip")
    } catch {
      case e: Exception =>
        println(s"Model save failed: ${e.getMessage}")
        e.printStackTrace()
    }
  }

  /**
   * Load both networks from disk
   */
  def load(basePath: Option[String] = None): Boolean = {
    val path = basePath.getOrElse(config.modelBasePath)
    
    try {
      val policyFile = new File(s"${path}_policy.zip")
      val valueFile = new File(s"${path}_value.zip")

      if (!policyFile.exists() || !valueFile.exists()) {
        println(s"Model files not found: ${path}_policy.zip or ${path}_value.zip")
        return false
      }

      val loadedPolicyNet = MultiLayerNetwork.load(policyFile, false)
      val loadedValueNet = MultiLayerNetwork.load(valueFile, false)

      // Replace current networks with loaded ones
      this.network.setParams(loadedPolicyNet.params())
      this.valueNetwork.setParams(loadedValueNet.params())

      println(s"Model loaded from ${path}_policy.zip and ${path}_value.zip")
      true
    } catch {
      case e: Exception =>
        println(s"Model load failed: ${e.getMessage}")
        e.printStackTrace()
        false
    }
  }


  def getConfig: TttModelConfig = config

  /**
   * Get summary of the networks for debugging
   */
  def summary(): String = {
    s"""TttPolicyValueModel Summary:
       |Policy Network: ${network.summary()}
       |Value Network: ${valueNetwork.summary()}
       |Config: $config
       |""".stripMargin
  }
}
