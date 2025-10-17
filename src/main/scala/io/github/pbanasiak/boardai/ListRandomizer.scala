package io.github.pbanasiak.boardai

object ListRandomizer {
  val rand = new scala.util.Random(java.lang.System.currentTimeMillis())

  def getRandomElement[T](list: Seq[T]): T = {
    val n = rand.nextInt(list.size)
    list(n)
  }
}
