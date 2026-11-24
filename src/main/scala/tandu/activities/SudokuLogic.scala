package tandu.activities

import scala.util.Random

/** Printed Sudoku: a rating solver and a generator that makes puzzles at a
  * level. The level of a puzzle is the set of solving techniques that it
  * needs, not the number of givens.
  *
  * A grid is row-major: cell `r * n + c`. In a puzzle, 0 is an empty cell;
  * the symbols are 1..n. */
object SudokuLogic:

  /** The grid size and the box shape (rows × columns of a box). */
  enum GridSize(val n: Int, val boxRows: Int, val boxCols: Int):
    case G4 extends GridSize(4, 2, 2)
    case G6 extends GridSize(6, 2, 3)
    case G9 extends GridSize(9, 3, 3)

  /** The solving techniques of the rating solver, from easy to hard. */
  enum Technique:
    /** A cell with only one candidate. */
    case NakedSingle
    /** A symbol with only one place left in a row, column or box. */
    case HiddenSingle
    /** Two cells of a unit with the same two candidates: these two symbols
      * go out of the other cells of the unit. */
    case NakedPair
    /** Pointing: the places of a symbol in a box are all in one row or
      * column, so the symbol goes out of the rest of that line. Box-line:
      * the places of a symbol in a line are all in one box, so the symbol
      * goes out of the rest of that box. */
    case LockedCandidates

  import Technique.*

  enum Level(val techniques: Set[Technique]):
    case Easy extends Level(Set(NakedSingle))
    case Medium extends Level(Set(NakedSingle, HiddenSingle))
    case Hard extends Level(Set(NakedSingle, HiddenSingle, NakedPair, LockedCandidates))

    /** The level below this one; Easy has none. */
    def below: Option[Level] = this match
      case Easy   => None
      case Medium => Some(Easy)
      case Hard   => Some(Medium)

  /** The result of the rating solver.
    *   - `solved`: the level's techniques filled every cell.
    *   - `grid`: the grid as far as the solver came.
    *   - `steps`: how many times each technique made progress. */
  final case class Rating(solved: Boolean, grid: Vector[Int], steps: Map[Technique, Int]):
    /** All steps that are not naked singles. */
    def advancedSteps: Int = steps.iterator.collect { case (t, k) if t != NakedSingle => k }.sum

  /** A puzzle and its solution (row-major; 0 is an empty cell in `givens`). */
  final case class Puzzle(givens: Vector[Int], solution: Vector[Int])

  // ---------- geometry ----------

  /** The units (rows, columns, boxes) and the peers of each cell. */
  private final class Geometry(size: GridSize):
    val n: Int = size.n
    val cells: Int = n * n
    def boxOf(cell: Int): Int =
      val (r, c) = (cell / n, cell % n)
      (r / size.boxRows) * (n / size.boxCols) + c / size.boxCols
    val rows: Array[Array[Int]] = Array.tabulate(n)(r => Array.tabulate(n)(c => r * n + c))
    val cols: Array[Array[Int]] = Array.tabulate(n)(c => Array.tabulate(n)(r => r * n + c))
    val boxes: Array[Array[Int]] = Array.tabulate(n)(b => (0 until cells).filter(boxOf(_) == b).toArray)
    val lines: Array[Array[Int]] = rows ++ cols
    val units: Array[Array[Int]] = lines ++ boxes
    val peers: Array[Array[Int]] = Array.tabulate(cells) { cell =>
      (0 until cells).filter { o =>
        o != cell && (o / n == cell / n || o % n == cell % n || boxOf(o) == boxOf(cell))
      }.toArray
    }
    val full: Int = ((1 << (n + 1)) - 1) & ~1

  private lazy val geometries: Map[GridSize, Geometry] = GridSize.values.map(s => s -> Geometry(s)).toMap

  // ---------- rating solver ----------

  /** The solver state: the grid and the candidates of each cell, as a bit
    * mask (bit v for symbol v; 0 for a filled cell). */
  private final class Solver(g: Geometry, givens: IndexedSeq[Int]):
    val grid: Array[Int] = new Array[Int](g.cells)
    val cand: Array[Int] = Array.fill(g.cells)(g.full)
    for i <- 0 until g.cells if givens(i) != 0 do place(i, givens(i))

    def place(cell: Int, v: Int): Unit =
      grid(cell) = v
      cand(cell) = 0
      val bit = ~(1 << v)
      val ps = g.peers(cell)
      var i = 0
      while i < ps.length do
        cand(ps(i)) &= bit
        i += 1

    /** Remove the bits of `mask` from the cells of `unit` for which `keep`
      * is false. True when a candidate was removed. */
    private def eliminate(unit: Array[Int], mask: Int, keep: Int => Boolean): Boolean =
      var changed = false
      for cell <- unit if grid(cell) == 0 && !keep(cell) && (cand(cell) & mask) != 0 do
        cand(cell) &= ~mask
        changed = true
      changed

    private def value(mask: Int): Int = Integer.numberOfTrailingZeros(mask)

    def nakedSingle(): Boolean =
      var cell = 0
      while cell < g.cells do
        if grid(cell) == 0 && Integer.bitCount(cand(cell)) == 1 then
          place(cell, value(cand(cell)))
          return true
        cell += 1
      false

    def hiddenSingle(): Boolean =
      g.units.exists { unit =>
        (1 to g.n).exists { v =>
          val bit = 1 << v
          val ps = unit.filter(cell => grid(cell) == 0 && (cand(cell) & bit) != 0)
          ps.length == 1 && { place(ps(0), v); true }
        }
      }

    def nakedPair(): Boolean =
      g.units.exists { unit =>
        val two = unit.filter(cell => grid(cell) == 0 && Integer.bitCount(cand(cell)) == 2)
        two.indices.exists { i =>
          (i + 1 until two.length).exists { j =>
            val (a, b) = (two(i), two(j))
            cand(a) == cand(b) && eliminate(unit, cand(a), cell => cell == a || cell == b)
          }
        }
      }

    def lockedCandidates(): Boolean =
      def places(unit: Array[Int], bit: Int): Array[Int] = unit.filter(cell => grid(cell) == 0 && (cand(cell) & bit) != 0)
      (1 to g.n).exists { v =>
        val bit = 1 << v
        // Pointing: a box's places in one row or one column.
        val pointing = g.boxes.exists { box =>
          val ps = places(box, bit)
          ps.nonEmpty && {
            val inBox = box.toSet
            (ps.forall(_ / g.n == ps(0) / g.n) && eliminate(g.rows(ps(0) / g.n), bit, inBox)) ||
            (ps.forall(_ % g.n == ps(0) % g.n) && eliminate(g.cols(ps(0) % g.n), bit, inBox))
          }
        }
        // Box-line: a line's places in one box.
        pointing || g.lines.exists { line =>
          val ps = places(line, bit)
          ps.nonEmpty && ps.forall(g.boxOf(_) == g.boxOf(ps(0))) && eliminate(g.boxes(g.boxOf(ps(0))), bit, line.toSet)
        }
      }

    def apply(t: Technique): Boolean = t match
      case NakedSingle      => nakedSingle()
      case HiddenSingle     => hiddenSingle()
      case NakedPair        => nakedPair()
      case LockedCandidates => lockedCandidates()

    /** Apply the level's techniques until the grid is full or no technique
      * makes progress. Each step uses the easiest technique that works.
      * `count(t)` is called once for each step. */
    def run(level: Level, count: Technique => Unit): Boolean =
      val order = Technique.values.filter(level.techniques)
      var result: Option[Boolean] = None
      while result.isEmpty do
        if !grid.contains(0) then result = Some(true)
        else if (0 until g.cells).exists(cell => grid(cell) == 0 && cand(cell) == 0) then result = Some(false)
        else order.find(apply) match
          case Some(t) => count(t)
          case None    => result = Some(false)
      result.get

  def rate(size: GridSize, givens: IndexedSeq[Int], level: Level): Rating =
    val solver = Solver(geometries(size), givens)
    val steps = scala.collection.mutable.Map.empty[Technique, Int]
    val solved = solver.run(level, t => steps(t) = steps.getOrElse(t, 0) + 1)
    Rating(solved, solver.grid.toVector, steps.toMap)

  private def solvable(g: Geometry, givens: Array[Int], level: Level): Boolean =
    Solver(g, givens.toIndexedSeq).run(level, _ => ())

  // ---------- generator ----------

  /** A random full grid, by randomized backtracking. */
  def fill(size: GridSize, rng: Random): Vector[Int] =
    val g = geometries(size)
    val grid = new Array[Int](g.cells)
    def fits(cell: Int, v: Int): Boolean = !g.peers(cell).exists(grid(_) == v)
    def go(cell: Int): Boolean =
      cell == g.cells || rng.shuffle((1 to g.n).toList).exists { v =>
        fits(cell, v) && {
          grid(cell) = v
          go(cell + 1) || { grid(cell) = 0; false }
        }
      }
    val _ = go(0)
    grid.toVector

  /** The number of puzzles that [[generate]] tries before it keeps one that
    * does not need its level. */
  val MaxTries = 20

  /** A puzzle that is solvable at `level`. From a full random grid, givens
    * are removed in random order; a removal stays only when the puzzle is
    * still solvable at the level. The puzzle must also need the level (not
    * be solvable at the level below). If no try in [[MaxTries]] needs the
    * level, the last try is kept: it is never harder than asked. A puzzle
    * that logic solves has exactly one solution. */
  def generate(size: GridSize, level: Level, rng: Random): Puzzle =
    val g = geometries(size)
    def once(): Puzzle =
      val solution = fill(size, rng)
      val givens = solution.toArray
      for pos <- rng.shuffle((0 until g.cells).toVector) do
        val saved = givens(pos)
        givens(pos) = 0
        if !solvable(g, givens, level) then givens(pos) = saved
      Puzzle(givens.toVector, solution)
    def needsLevel(p: Puzzle): Boolean = level.below.forall(b => !solvable(g, p.givens.toArray, b))
    val tries = Iterator.continually(once()).take(MaxTries)
    var last = tries.next()
    while !needsLevel(last) && tries.hasNext do last = tries.next()
    last
