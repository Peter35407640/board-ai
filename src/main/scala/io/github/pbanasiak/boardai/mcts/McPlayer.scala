package io.github.pbanasiak.boardai.mcts

import io.github.pbanasiak.boardai.GameState

trait McPlayer[B,M] {
  def valueFromMCPlayout(node: MctsTreeNode[GameState[B, M], M]): MctsTreeNode[GameState[B, M], M]
}
