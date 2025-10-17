package io.github.pbanasiak.boardai

/**
 *
 * @param initialState
 * @tparam B board type
 * @tparam M move type
 */
class Tournament[B, M](initialState: GameState[B, M]) {

  def playSingleGameTournament(
    game: Game[B, M],
    agentA: Agent[B, M], agentB: Agent[B, M],
    nGames: Int, maxEpisodeLength: Int,
    gamePrinter: Option[GamePrinter[B, M]],
  ): TournamentResult = {

    val winLossCollector = new WinLossCollector()
    for (i <- 1 to nGames) {
      val g1 = playGame(game, agentA, agentB, maxEpisodeLength, gamePrinter)
      winLossCollector.add(g1)
    }
    val result = winLossCollector.results()
    println(f"${agentA.getClass.getSimpleName} vs ${agentB.getClass.getSimpleName} TournamentResult result ${result}")
    result
  }

  def play2GamesTournament(
    game: Game[B, M],
    agentA: Agent[B, M], agentB: Agent[B, M],
    nGames: Int, maxEpisodeLength: Int, gamePrinter: Option[GamePrinter[B, M]]): TournamentResult = {

    val winLossCollector = new WinLossCollector()
    for (i <- 1 to nGames) {
      val (g1, g2) = play2Games(game, agentA, agentB, maxEpisodeLength, gamePrinter)
      winLossCollector.add(g1)
      winLossCollector.add(g2)
    }
    winLossCollector.results()
  }


  // each agent start single game
  def play2Games(
    game: Game[B, M],
    agentA: Agent[B, M], agentB: Agent[B, M],
    maxEpisodeLength: Int,
    gamePrinter: Option[GamePrinter[B, M]],
  ): (Int, Int) = {

    val result1 = playGame(game, agentA, agentB, maxEpisodeLength, gamePrinter)
    val result2 = playGame(game, agentB, agentA, maxEpisodeLength, gamePrinter)

    // results are from agentA perspective
    (result1, -result2)
  }

  // returns game score from agentA perspective
  def playGame(game: Game[B, M],
    agentA: Agent[B, M], agentB: Agent[B, M],
    maxEpisodeLength: Int,
    gamePrinter: Option[GamePrinter[B, M]],
  ): Int = {

    var state: GameState[B, M] = initialState

    def chooseAgent(player: Player) = {
      if (player == Player.xPlayer) agentA
      else agentB
    }

    var iters = 0

    while (iters < maxEpisodeLength) {
      gamePrinter.map(gp => gp.print(state))

      iters = iters + 1
      val agent = chooseAgent(state.playerToMove)
      val m: M = agent.selectMove(state)
      val nextState = game.applyMove(state, m)

      // if current move lead to victory
      if (nextState.isOver) {
        // player who just made the move
        val isVictory = nextState.isVictoryOf(state.playerToMove)

        if (isVictory) {
          gamePrinter.map(gp => gp.printVictory(nextState.board, state.playerToMove))
          val score = nextState.gameResult match {
            case Some(GameResult.xWin) => 1
            case Some(GameResult.oWin) => -1
            case _ => 0
          }
          return score
        }

        // no victory must be draw
        gamePrinter.map(gp => gp.printDraw(nextState.board, state.playerToMove))
        return 0
      }

      state = nextState
    }

    // too many iters result in draw
    0
  }

}
