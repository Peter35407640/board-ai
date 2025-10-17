package io.github.pbanasiak.boardai.mcts

final case class NodeStatistics(prior: Double, visitCount: Int = 0, totalValue: Double = 0)
