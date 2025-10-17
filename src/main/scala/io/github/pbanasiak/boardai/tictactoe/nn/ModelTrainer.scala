package io.github.pbanasiak.boardai.tictactoe.nn

import io.github.pbanasiak.boardai.agent.RandomAgent
import io.github.pbanasiak.boardai.mcts.{MctsNnAgent, MctsNnNodeCreator, MonteCarloPlayer}
import io.github.pbanasiak.boardai.nn.{EncodedTrainingSample, NPlyStartStateGenerator}
import io.github.pbanasiak.boardai.tictactoe.encoders.Planes27Encoder
import io.github.pbanasiak.boardai.tictactoe.{TttBoard, TttGame, TttMove}
import io.github.pbanasiak.boardai.{Agent, ExperienceCollector, GameResult, GameState, Player, Tournament}

import scala.util.Random

/**
 * Simple, clean trainer that:
 *  - self-plays NN vs NN and collects PendingTrainingSample
 *  - finalizes all samples at game end with the true result
 *  - trains the model on finalized samples
 *  - evaluates periodically
 */
object ModelTrainer {

  def trainModel(game: TttGame,
    model: TttPolicyValueModel,
    trainingConfig: TttTrainingConfig): Unit = {

    val encoder = new Planes27Encoder()
    val sampleEncoder = new TttSampleEncoder(encoder)
    println(s"Starting training for ${trainingConfig.iterations} iterations...")
    println(s"Using config: $trainingConfig")

    for (iter <- 1 to trainingConfig.iterations) {
      println(s"\n=== Training Iteration $iter ===")

      val collector = new ExperienceCollector[TttBoard, TttMove]()

      val nodeCreator = new MctsNnNodeCreator(game, encoder, model)

      // Exploit agent: default rounds/puct from config
      val exploit = new MctsNnAgent[TttBoard, TttMove](
        game,
        nodeCreator = nodeCreator,
        mcPlayer = new MonteCarloPlayer(game, nodeCreator, new Random(iter * trainingConfig.trainingSeed + 11)),
        collector = Some(collector),
        roundsPerMove = trainingConfig.mctsRoundsPerMove,
        puctCoeff = trainingConfig.puctCoeff,
        rand = new Random(iter * trainingConfig.trainingSeed + 12),
        selectRandomly = trainingConfig.mctsSelectRandomly,
        debug = false
      )

      // Explore agent: fewer rounds, forced random top-k
      val exploreRounds = math.max(10, trainingConfig.mctsRoundsPerMove / 2)
      val explore = new MctsNnAgent[TttBoard, TttMove](
        game,
        nodeCreator = nodeCreator,
        mcPlayer = new MonteCarloPlayer(game, nodeCreator, new Random(iter * trainingConfig.trainingSeed + 21)),
        collector = Some(collector),
        roundsPerMove = exploreRounds,
        puctCoeff = trainingConfig.puctCoeff,
        rand = new Random(iter * trainingConfig.trainingSeed + 22),
        selectRandomly = true,
        debug = false
      )

      // Clear any previous finalized data (safety)
      collector.getFinalizedSamplesAndClear()

      val rnd = new Random(iter * 1234567L)
      val gamesThisIter = trainingConfig.gamesPerIteration

      val twoPlyPool: Seq[GameState[TttBoard, TttMove]] = {
        val all = NPlyStartStateGenerator.twoPlyFromInitial(game)
        // cap and shuffle for runtime balance
        rnd.shuffle(all.toList).take(math.min(all.size, math.max(50, gamesThisIter)))
      }

      // Helper: play from an arbitrary starting state
      def playFrom(gs0: GameState[TttBoard, TttMove], a: Agent[TttBoard, TttMove], b: Agent[TttBoard, TttMove]): GameResult.Value = {
        var gs = gs0
        while (!gs.isOver) {
          val current = if (gs.playerToMove == Player.xPlayer) a else b
          val mv = current.selectMove(gs)
          gs = game.applyMove(gs, mv)
        }
        gs.gameResult.get
      }


      // Self-play, randomize who starts; use the same collector for both agents (all samples in one finalized pool)
      for (_ <- 1 to gamesThisIter) {
        val (a, b) = if (rnd.nextBoolean()) (explore, exploit) else (exploit, explore)

        // Choose a start state: 50% from 2-ply pool, 50% from initial (randomized side to move)
        val usePool = rnd.nextBoolean() && twoPlyPool.nonEmpty
        val startState =
          if (usePool) twoPlyPool(rnd.nextInt(twoPlyPool.size))
          else game.initialState()


        // Optional side swap only when starting from initial
        val result =
          if (usePool) {
            collector.beginEpisode()
            playFrom(startState, a, b)
          } else {
            val swapStart = rnd.nextBoolean()
            collector.beginEpisode()
            if (!swapStart) playFrom(startState, a, b) else playFrom(startState, b, a)
          }
        collector.finalizePendingWith(result) // finalize all pending to finalized with ground-truth result
      }

      // Train on this iteration batch
      val finalized = collector.getFinalizedSamplesAndClear()
      if (finalized.nonEmpty) {
        val encoded: Array[EncodedTrainingSample[Array[Double]]] = finalized.map(ps => sampleEncoder.encode(ps))
        
        val inputs = encoded.map(_.gameState)
        val policyTargets = encoded.map(_.policyTarget)
        val valueTargets = finalized.map(s => s.gsValue)

        model.trainBatch(inputs, policyTargets, valueTargets, Some(model.getConfig.trainingEpochs))
        println(s"Trained model on ${finalized.length} samples for ${model.getConfig.trainingEpochs} epochs")
      } else {
        println("No finalized samples this iteration.")
      }

      // Periodic evaluation
      if (iter % trainingConfig.evaluateEveryNIterations == 0) {
        evaluateModel(game, model, encoder, iter, trainingConfig)
        saveModelWithIteration(model, iter)
      }
    }

    println("\n=== Training Complete ===")
  }

  private def saveModelWithIteration(model: TttPolicyValueModel, iteration: Int): Unit = {
    try {
      val baseConfig = model.getConfig
      val iterationPath = s"${baseConfig.modelBasePath}_iter_$iteration"

      model.save(Some(iterationPath))
      println(s"Model checkpoint saved at iteration $iteration")
    } catch {
      case e: Exception =>
        println(s"Failed to save model checkpoint at iteration $iteration: ${e.getMessage}")
        e.printStackTrace()
    }
  }

  private def evaluateModel(game: TttGame,
    model: TttPolicyValueModel,
    encoder: Planes27Encoder,
    iteration: Int,
    config: TttTrainingConfig): Unit = {

    println(s"\n--- Evaluation at iteration $iteration ---")

    val tournament = new Tournament[TttBoard, TttMove](game.initialState())
    val nodeCreator = new MctsNnNodeCreator(game, encoder, model)
    val nnAgent = new MctsNnAgent[TttBoard, TttMove](
      game,
      nodeCreator = nodeCreator,
      mcPlayer = new MonteCarloPlayer(game, nodeCreator, new Random(config.evaluationSeed)),
      collector = None,
      roundsPerMove = config.evalMctsRounds,
      puctCoeff = config.puctCoeff,
      rand = new Random(config.evaluationSeed),
      selectRandomly = config.evalMctsRandomSelection,
      debug = false
    )

    val randomAgent = new RandomAgent(game, new Random(123))
    val result = tournament.playSingleGameTournament(game, nnAgent, randomAgent, 20, 9, None)
    val total = result.wins + result.loss + result.draw
    val wr = if (total > 0) result.wins.toDouble / total * 100 else 0.0
    println(f"vs Random: wins=${result.wins}, loss=${result.loss}, draw=${result.draw}, winRate=$wr%.2f%%")
  }

  def main(args: Array[String]): Unit = {
    val game = new TttGame()
    val encoder = new Planes27Encoder()
    val presets = TttPresets.Quick

    val model = new TttPolicyValueModel(
      nInput = encoder.nInput,
      nOutput = encoder.nOutput,
      config = presets.model
    )

    println(model.summary())
    trainModel(game, model, presets.training)
    model.save()
    println(s"Model saved to ${presets.model.modelBasePath}_policy.zip and ${presets.model.modelBasePath}_value.zip")
  }
}