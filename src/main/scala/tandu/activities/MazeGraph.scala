package tandu.activities

import scala.collection.mutable
import scala.util.Random

/** A maze as a graph: cells `0 until cellCount` and the neighbours of each
  * cell. A [[Layout]] gives the shape (square grid, hexagons, rings); the
  * same [[carve]] makes the maze for every shape. */
object MazeGraph:

  /** A passage between `a` and `b`, stored as (smaller cell, larger cell). */
  private def key(a: Int, b: Int): (Int, Int) = if a < b then (a, b) else (b, a)

  /** The path length from `from` to each cell through open passages; -1
    * where there is no path. */
  private[activities] def distances(layout: Layout, from: Int, open: (Int, Int) => Boolean): Array[Int] =
    val dist = Array.fill(layout.cellCount)(-1)
    dist(from) = 0
    val queue = mutable.Queue(from)
    while queue.nonEmpty do
      val cell = queue.dequeue()
      for next <- layout.neighbours(cell) if dist(next) < 0 && open(cell, next) do
        dist(next) = dist(cell) + 1
        queue.enqueue(next)
    dist

  /** The cells of one maze shape. `neighbours` must be symmetric: if `b`
    * is a neighbour of `a`, then `a` is a neighbour of `b`. */
  sealed trait Layout:
    def cellCount: Int
    def neighbours(cell: Int): Vector[Int]
    def start: Int
    def goal: Int
    /** The cells that can be the start. When there is more than one,
      * [[carve]] takes the one with the longest path to the goal. */
    def startChoices: Vector[Int] = Vector(start)
    /** When true, [[carve]] gives the goal exactly one passage. */
    def goalHasOneDoor: Boolean = false

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

    /** The neighbour across edge `k` of the hexagon: 0 upper right,
      * 1 right, 2 lower right, 3 lower left, 4 left, 5 upper left. The
      * rows above and below touch columns c-1 and c (even row), or c and
      * c+1 (odd row). */
    def neighbourIn(cell: Int, k: Int): Option[Int] =
      val (r, c) = (cell / cols, cell % cols)
      val d = if r % 2 == 0 then -1 else 0
      val (nr, nc) = k match
        case 0 => (r - 1, c + d + 1)
        case 1 => (r, c + 1)
        case 2 => (r + 1, c + d + 1)
        case 3 => (r + 1, c + d)
        case 4 => (r, c - 1)
        case _ => (r - 1, c + d)
      Option.when(nr >= 0 && nr < rows && nc >= 0 && nc < cols)(nr * cols + nc)

    private val table: Vector[Vector[Int]] = Vector.tabulate(cellCount)(cell => (0 until 6).flatMap(neighbourIn(cell, _)).toVector)
    def neighbours(cell: Int): Vector[Int] = table(cell)

  /** Rings of cells around one centre cell. Ring `i` spans the radius
    * `i .. i + 1` (in ring widths); ring 0 is the centre cell (the goal).
    * A ring has two times the cells of the ring inside it when its cells
    * would otherwise be wider than 1.5 ring widths (at the inner side), so
    * all cells keep about the same size, from 0.75 to 1.5 ring widths. More
    * and smaller cells give a harder maze in the same circle. The start is
    * an outer-ring cell (see `startChoices`); the drawing turns the circle
    * so that it is at the bottom. Angles are in radians, clockwise from the
    * right (y points down). */
  final case class CircleLayout(rings: Int) extends Layout:
    val ringSizes: Vector[Int] = CircleLayout.ringSizes(rings)
    private val firstCell: Vector[Int] = ringSizes.scanLeft(0)(_ + _)
    val cellCount: Int = firstCell.last
    private val ringOfCell: Array[Int] = Array.tabulate(cellCount)(cell => firstCell.lastIndexWhere(_ <= cell))
    def cellAt(ring: Int, i: Int): Int = firstCell(ring) + i
    def ringOf(cell: Int): Int = ringOfCell(cell)
    def indexInRing(cell: Int): Int = cell - firstCell(ringOf(cell))
    /** The cell on the next ring in whose arc contains this cell's arc. */
    def inward(cell: Int): Int =
      val ring = ringOf(cell)
      cellAt(ring - 1, indexInRing(cell) * ringSizes(ring - 1) / ringSizes(ring))
    val goal: Int = 0
    /** The root of the carving; the real start is one of `startChoices`. */
    val start: Int = cellAt(rings, 0)
    // The centre is near every outer cell, so a fixed entrance gives a
    // short path, and a centre with many doors can be reached from many
    // sides. Therefore the entrance is the outer cell farthest from the centre,
    // and the centre has one door.
    override val startChoices: Vector[Int] = (0 until ringSizes(rings)).map(cellAt(rings, _)).toVector
    override val goalHasOneDoor: Boolean = true

    /** The angles (from, to) of a cell's arc; the centre cell has the full
      * circle. */
    def arc(cell: Int): (Double, Double) =
      val step = 2 * math.Pi / ringSizes(ringOf(cell))
      val i = indexInRing(cell)
      (i * step, (i + 1) * step)

    private val table: Vector[Vector[Int]] = Vector.tabulate(cellCount) { cell =>
      val ring = ringOf(cell)
      val i = indexInRing(cell)
      val n = ringSizes(ring)
      val in = if ring == 0 then Vector.empty else Vector(inward(cell))
      val outward =
        if ring == rings then Vector.empty
        else
          val ratio = ringSizes(ring + 1) / n
          (i * ratio until (i + 1) * ratio).map(cellAt(ring + 1, _)).toVector
      val beside =
        if ring == 0 then Vector.empty
        else Vector(cellAt(ring, (i + n - 1) % n), cellAt(ring, (i + 1) % n)).distinct
      in ++ beside ++ outward
    }
    def neighbours(cell: Int): Vector[Int] = table(cell)

  object CircleLayout:
    /** The cell count of each ring, 0 to `rings` (see [[CircleLayout]]).
      * Cheap: it does not make the neighbour table. */
    def ringSizes(rings: Int): Vector[Int] =
      (2 to rings).foldLeft(Vector(1, 6)) { (acc, i) =>
        val prev = acc.last
        acc :+ (if 2 * math.Pi * i / prev > 1.5 then prev * 2 else prev)
      }.take(rings + 1)

  /** The carved maze: the open passages between neighbour cells. Each pair
    * is stored as (smaller cell, larger cell). */
  final case class Carved(layout: Layout, passages: Set[(Int, Int)], start: Int):
    def open(a: Int, b: Int): Boolean = passages.contains(key(a, b))
    def degree(cell: Int): Int = layout.neighbours(cell).count(open(cell, _))
    def distancesFrom(cell: Int): Array[Int] = distances(layout, cell, open)

  /** Carve a maze in the given style (see [[Maze.Style]]): a spanning tree
    * of the layout, with exactly one path between any two cells.
    *
    * `braid` (0..1) then opens an extra wall at that fraction of the dead
    * ends, which makes loops. The start and the goal never get an extra
    * passage. */
  def carve(layout: Layout, style: Maze.Style, braid: Double, rng: Random): Carved =
    val n = layout.cellCount
    val goal = layout.goal
    val visited = Array.fill(n)(false)
    val passages = mutable.Set.empty[(Int, Int)]
    def link(a: Int, b: Int): Unit = passages += key(a, b)
    def linked(a: Int, b: Int): Boolean = passages.contains(key(a, b))
    val root = layout.start
    // A goal with one door is left out of the carving and gets its door at
    // the end.
    val skip = if layout.goalHasOneDoor then goal else -1
    def neighbours(cell: Int): Vector[Int] =
      if skip < 0 then layout.neighbours(cell) else layout.neighbours(cell).filter(_ != skip)

    // Recursive backtracker. An explicit stack, so a big maze cannot blow
    // the call stack.
    def winding(): Unit =
      val stack = mutable.Stack(root)
      visited(root) = true
      while stack.nonEmpty do
        val unvisited = neighbours(stack.top).filterNot(visited)
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
      for first <- rng.shuffle((0 until n).toVector) if !visited(first) && first != skip do
        var cur = first
        while !visited(cur) do
          val ns = neighbours(cur)
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
        for next <- neighbours(cell) if !visited(next) && !inFrontier(next) do
          inFrontier(next) = true
          frontier += next
      add(root)
      while frontier.nonEmpty do
        val i = rng.nextInt(frontier.size)
        val cell = frontier(i)
        frontier(i) = frontier.last
        frontier.dropRightInPlace(1)
        val inMaze = neighbours(cell).filter(visited)
        link(cell, inMaze(rng.nextInt(inMaze.size)))
        add(cell)

    style match
      case Maze.Style.Winding  => winding()
      case Maze.Style.Balanced => balanced()
      case Maze.Style.Branchy  => branchy()

    if skip >= 0 then
      val doors = layout.neighbours(goal)
      link(goal, doors(rng.nextInt(doors.size)))

    // The start: of the choices, the one with the longest path to the goal.
    val start =
      if layout.startChoices.size == 1 then layout.startChoices.head
      else
        val dist = distances(layout, goal, linked)
        layout.startChoices.maxBy(dist(_))

    if braid > 0 then
      def degree(cell: Int): Int = layout.neighbours(cell).count(linked(cell, _))
      def kept(cell: Int): Boolean = cell == start || cell == goal
      for cell <- 0 until n do
        if degree(cell) == 1 && !kept(cell) && rng.nextDouble() < braid then
          val closed = layout.neighbours(cell).filterNot(next => linked(cell, next) || kept(next))
          if closed.nonEmpty then link(cell, closed(rng.nextInt(closed.size)))

    Carved(layout, passages.toSet, start)
