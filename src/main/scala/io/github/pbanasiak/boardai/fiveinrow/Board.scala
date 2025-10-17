package io.github.pbanasiak.boardai.fiveinrow

class Board(private val cells: Array[Array[Int]]):

  def getInt(r: Int, c: Int): Int = cells(r)(c)

  def setInt(r: Int, c: Int, value: Int): Board =
    val newCells = cells.map(_.clone())
    newCells(r)(c) = value
    Board(newCells)

  // Immutable access to internal state
  def getCells: Array[Array[Int]] = cells.map(_.clone())

  // Making it compatible with old getMatrix() calls
  def getMatrix(): Array[Array[Int]] = getCells

  def toList: List[List[Int]] = cells.map(_.toList).toList
  def toFlatList: List[Int] = cells.flatten.toList

  def isEmpty: Boolean = toFlatList.forall(_ == 0)
  def isFull: Boolean = toFlatList.forall(_ != 0)

  // Get board dimensions
  def rows: Int = cells.length
  def cols: Int = if cells.length > 0 then cells(0).length else 0

  override def toString: String =
    cells.map(_.mkString(" ")).mkString("\n")

  // Pure Scala approach - convert to List for comparison
  override def equals(obj: Any): Boolean = obj match
    case other: Board => this.toList == other.toList
    case _ => false

  override def hashCode(): Int = toList.hashCode()

object Board:
  def apply(cells: Array[Array[Int]]): Board = new Board(cells.map(_.clone()))

  def zeros(rows: Int, cols: Int): Board =
    new Board(Array.fill(rows, cols)(0))

  def fromFlatArray(flat: Array[Int], rows: Int, cols: Int): Board =
    val cells = Array.ofDim[Int](rows, cols)
    for i <- flat.indices do
      cells(i / cols)(i % cols) = flat(i)
    new Board(cells)

  def fromString(boardStr: String): Board =
    val lines = boardStr.trim.split("\n")
    val cells = lines.map(_.trim.split("\\s+").map(_.toInt))
    new Board(cells)

  // Compatibility method for old ND4J array creation
  def fromDoubleArray(doubleArray: Array[Array[Double]]): Board =
    val intArray = doubleArray.map(_.map(_.toInt))
    new Board(intArray)

// Scala 3 extension methods
extension (board: Board)
  def updated(r: Int, c: Int, value: Int): Board = board.setInt(r, c, value)
  def apply(r: Int, c: Int): Int = board.getInt(r, c)