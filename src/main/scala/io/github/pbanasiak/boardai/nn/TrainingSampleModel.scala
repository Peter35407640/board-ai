package io.github.pbanasiak.boardai.nn

import io.github.pbanasiak.boardai.{GameResult, GameState, Player}

/** Marker for training samples used across games. */
sealed trait TrainingSample

// Training samples operate in game domain.
final case class PendingTrainingSample[B, M](
  gameState: GameState[B,M],      // Current state of the game board
  validMoves: List[M],            // Available legal moves from this state
  visitCounts: Map[M, Int],       // MCTS exploration counts for each move
  moveChosen: Option[M],          // The move that was actually selected and played
  gameResult: Option[GameResult.Value],
) extends TrainingSample

final case class FinalizedTrainingSample[B, M](
  gameState: GameState[B,M],      // Current state of the game board
  validMoves: List[M],            // Available legal moves from this state
  visitCounts: Map[M, Int],       // MCTS exploration counts for each move
  moveChosen: Option[M],          // The move that was actually selected and played
  gsValue: Double, // the position (game state) value always from xPerspective
) extends TrainingSample


// Encoded samples operate in NN domain, part of Training
final case class EncodedTrainingSample[EncodedType](
  // encoded game state, shape (1, 3, boardSize, boardSize)
  gameState: EncodedType,      

  // normalized distribution over legal moves, shape (1, outputSize)
  // where outputSize = boardSize * boardSize
  policyTarget: EncodedType,   
  
  playerToMove: Player,          // perspective used for value label derivation elsewhere
  gsValue: Double   // the position (game state) value always from xPerspective
) extends TrainingSample
