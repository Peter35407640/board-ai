package io.github.pbanasiak.boardai.tictactoe

class TttBoard(private val cells: Array[Array[Int]]):

  def getInt(r: Int, c: Int): Int = cells(r)(c)

  def setInt(r: Int, c: Int, value: Int): TttBoard =
    val newCells = cells.map(_.clone())
    newCells(r)(c) = value
    TttBoard(newCells)

  // Immutable access to internal state
  def getCells: Array[Array[Int]] = cells.map(_.clone())

  def toList: List[List[Int]] = cells.map(_.toList).toList
  def toFlatList: List[Int] = cells.flatten.toList

  def isEmpty: Boolean = toFlatList.forall(_ == 0)
  def isFull: Boolean = toFlatList.forall(_ != 0)

  override def toString: String =
    cells.map(_.mkString(" ")).mkString("\n")

  // Pure Scala approach - no Java imports needed
  override def equals(obj: Any): Boolean = obj match
    case other: TttBoard => this.toList == other.toList
    case _ => false

  override def hashCode(): Int = toList.hashCode()

object TttBoard:
  def apply(cells: Array[Array[Int]]): TttBoard = new TttBoard(cells.map(_.clone()))

  def zeros(rows: Int, cols: Int): TttBoard =
    new TttBoard(Array.fill(rows, cols)(0))

  def fromFlatArray(flat: Array[Int], rows: Int, cols: Int): TttBoard =
    val cells = Array.ofDim[Int](rows, cols)
    for i <- flat.indices do
      cells(i / cols)(i % cols) = flat(i)
    new TttBoard(cells)

  def fromString(boardStr: String): TttBoard =
    val lines = boardStr.trim.split("\n")
    val cells = lines.map(_.trim.split("\\s+").map(_.toInt))
    new TttBoard(cells)

// Scala 3 extension methods
extension (board: TttBoard)
  def updated(r: Int, c: Int, value: Int): TttBoard = board.setInt(r, c, value)
  def apply(r: Int, c: Int): Int = board.getInt(r, c)