
# BoardAI

A clean, strongly-typed Scala framework for training AI agents to play board games, inspired by the AlphaGo/AlphaZero family of algorithms.

## Overview

BoardAI provides a generic, type-safe foundation for implementing board game AI agents. The framework separates game rules from AI strategies, making it easy to implement new games and experiment with different AI approaches.

## Core Concepts

The framework is built around a few key abstractions:

### Type Parameters

- **`B` (Board)**: Represents the board position
- **`M` (Move)**: Represents a valid move in the game
- **`EncodedType`**: Neural network input format (e.g., `Array[Double]`, `INDArray`)

### Core Traits

#### `Game[B, M]`

Represents the game rules and mechanics:

```scala
trait Game[B, M] {
  def validMoves(gameState: GameState[B, M]): Seq[M]
  def applyMove(gameState: GameState[B, M], move: M): GameState[B, M]
  def initialState(): GameState[B, M]
  val winScore: Score
  val drawScore: Score
}
```


#### `Agent[B, M]`

An AI agent that can select the best move given a game state:

```scala
trait Agent[B, M] {
  def selectMove(gameState: GameState[B, M]): M
}
```


#### `GameState[B, M]`

Represents the complete state of a game at a given point. Terminal (finished game) gameState has the `gameResult` defined.


```scala
class GameState[B, M](
  val board: B,
  val playerToMove: Player,
  val gameResult: Option[GameResult]
)
```

### Neural Network Integration

#### `GameStateEncoder[B, M, EncodedType]`

Converts game states into neural network inputs. The `EncodedType` parameter separates the game domain from the neural network implementation, allowing the same game logic to work with different neural network frameworks (e.g., `Array[Double]` for simple networks, `INDArray` for DL4J). This abstraction ensures that game-specific code doesn't depend on any particular neural network library.

#### `PolicyValueModel[EncodedType]`

Represents a neural network model that evaluates game positions. It takes an encoded game state (of type `EncodedType`) and returns two outputs:
- **Policy**: A probability distribution over possible moves
- **Value**: An estimate of the position's value for the current player

This model is used by MCTS agents to guide tree search, combining learned intuition with simulation-based search.

#### `NodeCreator[B, M]`

A factory interface for creating MCTS tree nodes. It decouples node creation from the search algorithm, allowing different node implementations (e.g., with or without neural network caching) while keeping the MCTS logic consistent. Works in conjunction with `MctsTreeNode` to build the search tree.

#### `MctsTreeNode[B, M]`

Represents a single node in the Monte Carlo Tree Search tree. Each node tracks:
- The game state at this position
- Visit counts and value estimates
- Links to parent and child nodes
- Prior policy probabilities (when using neural network guidance)

Nodes are created by `NodeCreator` and form the tree structure that MCTS explores to find the best move.

**Relationship**: These components work together in the neural network-guided MCTS flow: `GameStateEncoder` converts game states to `EncodedType`, `PolicyValueModel` evaluates these encodings to guide search, `NodeCreator` builds the tree structure with `MctsTreeNode` instances that store both game information and neural network predictions.

## AI Strategies

### Classical Search

- **NegaMax**: Simple minimax implementation with negamax formulation
- **AlphaBetaAgent**: Optimized alpha-beta pruning for faster tree search
- **FixedDepthAlphaBetaAgent**: Depth-limited alpha-beta search
- **RandomAgent**: Baseline agent that selects random valid moves

### Monte Carlo Tree Search (MCTS)

- **MctsAgent**: Pure MCTS implementation with UCB1 selection
- **MctsNnAgent**: MCTS enhanced with neural network policy and value guidance (AlphaZero-style)

## Implemented Games

- **Tic-Tac-Toe**: Classic 3×3 game
- **Five-in-a-Row (Gomoku)**: Configurable board size

## Features

- **Type Safety**: Strongly typed design ensures compile-time guarantees
- **Generic Framework**: Easy to add new games by implementing the `Game[B, M]` trait
- **Multiple AI Strategies**: Compare classical search vs. MCTS approaches
- **Neural Network Integration**: Support for deep learning-based agents (DL4J)
- **Tournament System**: Built-in tournament functionality to compare agents
- **Pre-trained Models**: Includes trained models for Tic-Tac-Toe and Gomoku (see `models/` directory)

## Pre-trained Models

The `models/` directory contains pre-trained neural networks for both Tic-Tac-Toe and Five-in-a-Row (Gomoku):

- **Tic-Tac-Toe models**: `tictactoe_quick_policy.zip` and `tictactoe_quick_value.zip` - Work reasonably well for the simple game
- **Gomoku models**: `fiveinrow_7x7_cnn_iter_*.zip` - Multiple iterations of training attempts on 7×7 boards
- **Test models**: `test_model_*.zip` - Used for unit testing

**Note**: The Gomoku models show the challenges of training board game AI with limited compute resources. They demonstrate partial learning but aren't production-ready. Feel free to use them as starting points for your own experiments!

## Project Structure

```
src/main/scala/io/github/pbanasiak/boardai/
├── agent/              # AI agent implementations
├── mcts/               # Monte Carlo Tree Search components
├── nn/                 # Neural network integration
├── tictactoe/          # Tic-Tac-Toe game implementation
├── fiveinrow/          # Five-in-a-Row (Gomoku) implementation
├── Game.scala          # Core game trait
├── Agent.scala         # Core agent trait
├── GameState.scala     # Game state representation
├── GameResult.scala    # Game outcome enumeration
└── ...                 # Utilities and helpers
```


## Getting Started

### Prerequisites

- Scala 3.x
- SBT (Scala Build Tool)
- JDK 17+

### Running Tournaments

To run AI vs AI tournaments, use the following commands:

```shell script
sbt -J-Xmx2G "runMain io.github.pbanasiak.boardai.tictactoe.Tournament"
```
Or for Five-in-a-Row (Gomoku):

```shell script
sbt -J-Xmx2G "runMain io.github.pbanasiak.boardai.fiveinrow.Tournament"
```
To play against yourself change agent to humanAgent.

### Running Tests

```shell script
sbt -J-Xmx2G test
```
Some model-related tests will fail because I failed to train the model well enough.

## Example Usage

```scala
// Create a game instance
val game = new TicTacToeGame()

// Create agents
val agent1 = new AlphaBetaAgent(game)
val agent2 = new MctsAgent(game, numSimulations = 1000)

val t = new boardai.Tournament[TttBoard, TttMove](game.initialState())

// Play a game
t.playSingleGameTournament(game, agent1, agent2, nGames = 20, None)
```


## Design Philosophy

1. **Separation of Concerns**: Game rules are completely separate from AI logic
2. **Composability**: Agents and games can be mixed and matched
3. **Type Safety**: Generic types prevent mixing incompatible games and agents
4. **Immutability**: Game states are immutable, making the system easier to reason about
5. **Extensibility**: Easy to add new games and agents without modifying core framework

## Roadmap

- [ ] Additional classic games (Chess, Go, etc.)
- [ ] Self-play training pipeline
- [ ] Enhanced neural network architectures
- [ ] Distributed MCTS
- [ ] Performance optimizations

## License

MIT License

Copyright (c) 2025 Piotr Banasiak

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

## Project Status

This is an experimental project exploring board game AI implementations. 
The framework successfully works for simpler games like Tic-Tac-Toe, though training models for more complex games like Gomoku remains a challenge.

The main value of this project is the clean, type-safe foundation it provides - feel free to use it as a starting point for your own experiments!

## Contributing

This is a personal learning project, but if you find it useful or interesting, feel free to:
- Fork it and experiment
- Fix issues if you find bugs
- Share what you build with it!
- Have fun

No formal contribution process - just have fun with it. 🎲

## Acknowledgments

After watching the AlphaGo documentary on Netflix, I thought, "Wow, I can try this myself!" — and that's how this project was born.

I underestimated how challenging it is to design and train a neural network. Apart from AI and neural network courses on Coursera, I had no prior hands-on experience. With limited hardware (RTX 3060), I have to admit I failed to train a well-performing neural network for Gomoku.

Nevertheless, I hope someone might find this project useful — whether as a learning resource, a starting point for their own experiments, or simply as proof that you don't need a PhD to try implementing these algorithms yourself!
