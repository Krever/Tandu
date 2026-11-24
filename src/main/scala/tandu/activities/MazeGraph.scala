package tandu.activities

import scala.collection.mutable
import scala.util.Random

/** A maze as a graph: cells `0 until cellCount` and the neighbours of each
  * cell. A [[Layout]] gives the shape (square grid, hexagons, rings); the
  * same [[carve]] makes the maze for every shape. */
object MazeGraph:

  /** The cells of one maze shape. `neighbours` must be symmetric: if `b`
    * is a neighbour of `a`, then `a` is a neighbour of `b`. */
  sealed trait Layout:
    def cellCount: Int
    def neighbours(cell: Int): Vector[Int]
    def start: Int
    def goal: Int

  /** A square grid. Cell `r * cols + c`; up to 4 neighbours. Start is the
    * top-left cell, goal the bottom-right cell. */
  final case class RectLayout(rows: Int, cols: Int) extends Layout:
    val cellCount: Int = rows * cols
    val start: Int = 0
    val goal: Int = cellCount - 1
    private val table: Vector[Vector[Int]] = Vector.tabulate(cellCount) { cell =>
      val (r, c) = (cell / cols, cell % cols)
      Vector((r - 1, c), (r + 1, c), (r, c - 1), (r, c + 1))
        .collect { case (nr, nc) if nr >= 0 && nr < rows && nc >= 0 && nc < cols => nr * cols + nc }
    }
    def neighbours(cell: Int): Vector[Int] = table(cell)

  /** Hexagons with a point at the top, in rows; odd rows are shifted half
    * a cell to the right. Cell `r * cols + c`; up to 6 neighbours. Start is
    * the top-left cell, goal the bottom-right cell. */
  final case class HexLayout(rows: Int, cols: Int) extends Layout:
    val cellCount: Int = rows * cols
    val start: Int = 0
    val goal: Int = cellCount - 1
    private val table: Vector[Vector[Int]] = Vector.tabulate(cellCount) { cell =>
      val (r, c) = (cell / cols, cell % cols)
      // The rows above and below touch columns c-1 and c (even row), or
      // c and c+1 (odd row).
      val d = if r % 2 == 0 then -1 else 0
      Vector((r, c - 1), (r, c + 1), (r - 1, c + d), (r - 1, c + d + 1), (r + 1, c + d), (r + 1, c + d + 1))
        .collect { case (nr, nc) if nr >= 0 && nr < rows && nc >= 0 && nc < cols => nr * cols + nc }
    }
    def neighbours(cell: Int): Vector[Int] = table(cell)

  /** Rings of cells around one centre cell. Ring `i` spans the radius
    * `i .. i + 1` (in ring widths); ring 0 is the centre cell (the goal).
    * A ring has two times the cells of the ring inside it when its cells
    * would otherwise be wider than 2 ring widths, so all cells keep about
    * the same size. The start is the outer-ring cell at the bottom; the
    * angles are turned so that this cell is centred at the bottom.
    * Angles are in radians, clockwise from the right (y points down). */
  final case class CircleLayout(rings: Int) extends Layout:
    val ringSizes: Vector[Int] =
      (2 to rings).foldLeft(Vector(1, 6)) { (acc, i) =>
        val prev = acc.last
        acc :+ (if 2 * math.Pi * i / prev > 2 then prev * 2 else prev)
      }.take(rings + 1)
    private val firstCell: Vector[Int] = ringSizes.scanLeft(0)(_ + _)
    val cellCount: Int = firstCell.last
    def cellAt(ring: Int, i: Int): Int = firstCell(ring) + i
    def ringOf(cell: Int): Int = firstCell.lastIndexWhere(_ <= cell)
    def indexInRing(cell: Int): Int = cell - firstCell(ringOf(cell))
    val goal: Int = 0
    val start: Int = cellAt(rings, ringSizes(rings) / 4)
    /** Turns the circle so that the start cell is centred at the bottom. */
    private val offset: Double = math.Pi / 2 - (indexInRing(start) + 0.5) * 2 * math.Pi / ringSizes(rings)

    /** The angles (from, to) of a cell's arc; the centre cell has the full
      * circle. */
    def arc(cell: Int): (Double, Double) =
      val ring = ringOf(cell)
      val step = 2 * math.Pi / ringSizes(ring)
      val i = indexInRing(cell)
      (offset + i * step, offset + (i + 1) * step)

    private val table: Vector[Vector[Int]] = Vector.tabulate(cellCount) { cell =>
      val ring = ringOf(cell)
      val i = indexInRing(cell)
      val n = ringSizes(ring)
      val inward =
        if ring == 0 then Vector.empty
        else Vector(cellAt(ring - 1, i * ringSizes(ring - 1) / n))
      val outward =
        if ring == rings then Vector.empty
        else
          val ratio = ringSizes(ring + 1) / n
          (i * ratio until (i + 1) * ratio).map(cellAt(ring + 1, _)).toVector
      val beside =
        if ring == 0 then Vector.empty
        else Vector(cellAt(ring, (i + n - 1) % n), cellAt(ring, (i + 1) % n)).distinct
      inward ++ beside ++ outward
    }
    def neighbours(cell: Int): Vector[Int] = table(cell)

  /** The carved maze: the open passages between neighbour cells. Each pair
    * is stored as (smaller cell, larger cell). */
  final case class Carved(layout: Layout, passages: Set[(Int, Int)]):
    def open(a: Int, b: Int): Boolean = passages.contains(if a < b then (a, b) else (b, a))
    def degree(cell: Int): Int = layout.neighbours(cell).count(open(cell, _))

  /** Carve a maze in the given style (see [[Maze.Style]]): a spanning tree
    * of the layout, with exactly one path between any two cells.
    *
    * `braid` (0..1) then opens an extra wall at that fraction of the dead
    * ends, which makes loops. The start and the goal are never opened
    * more. */
  def carve(layout: Layout, style: Maze.Style, braid: Double, rng: Random): Carved =
    val n = layout.cellCount
    val visited = Array.fill(n)(false)
    val passages = mutable.Set.empty[(Int, Int)]
    def link(a: Int, b: Int): Unit = passages += (if a < b then (a, b) else (b, a))
    def linked(a: Int, b: Int): Boolean = passages.contains(if a < b then (a, b) else (b, a))
    val root = layout.start

    // Recursive backtracker. An explicit stack, so a big maze cannot blow
    // the call stack.
    def winding(): Unit =
      val stack = mutable.Stack(root)
      visited(root) = true
      while stack.nonEmpty do
        val unvisited = layout.neighbours(stack.top).filterNot(visited)
        if unvisited.isEmpty then { val _ = stack.pop() }
        else
          val next = unvisited(rng.nextInt(unvisited.size))
          link(stack.top, next)
          visited(next) = true
          stack.push(next)

    // Wilson's algorithm. From each cell that is not in the maze, walk at
    // random until the walk touches the maze. When the walk crosses itself,
    // the loop is erased: each cell keeps only its last exit. Then carve
    // the walk into the maze.
    def balanced(): Unit =
      val exit = new Array[Int](n)
      visited(root) = true
      for first <- rng.shuffle((0 until n).toVector) if !visited(first) do
        var cur = first
        while !visited(cur) do
          val ns = layout.neighbours(cur)
          exit(cur) = ns(rng.nextInt(ns.size))
          cur = exit(cur)
        cur = first
        while !visited(cur) do
          visited(cur) = true
          link(cur, exit(cur))
          cur = exit(cur)

    // Prim's algorithm with a random frontier. Grow the maze from one cell:
    // take a random cell next to the maze and connect it to a random maze
    // neighbour.
    def branchy(): Unit =
      val frontier = mutable.ArrayBuffer.empty[Int]
      val inFrontier = Array.fill(n)(false)
      def add(cell: Int): Unit =
        visited(cell) = true
        for next <- layout.neighbours(cell) if !visited(next) && !inFrontier(next) do
          inFrontier(next) = true
          frontier += next
      add(root)
      while frontier.nonEmpty do
        val i = rng.nextInt(frontier.size)
        val cell = frontier(i)
        frontier(i) = frontier.last
        frontier.dropRightInPlace(1)
        val inMaze = layout.neighbours(cell).filter(visited)
        link(cell, inMaze(rng.nextInt(inMaze.size)))
        add(cell)

    style match
      case Maze.Style.Winding  => winding()
      case Maze.Style.Balanced => balanced()
      case Maze.Style.Branchy  => branchy()

    if braid > 0 then
      def degree(cell: Int): Int = layout.neighbours(cell).count(linked(cell, _))
      for cell <- 0 until n do
        if degree(cell) == 1 && cell != layout.start && cell != layout.goal && rng.nextDouble() < braid then
          val closed = layout.neighbours(cell).filterNot(linked(cell, _))
          if closed.nonEmpty then link(cell, closed(rng.nextInt(closed.size)))

    Carved(layout, passages.toSet)
