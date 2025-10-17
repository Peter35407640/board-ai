package io.github.pbanasiak.boardai

import io.github.pbanasiak.boardai.Score
// no move on terminal state
case class ScoredMove[M](score:Score, move:M)
