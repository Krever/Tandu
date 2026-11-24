package tandu.activities

import org.scalatest.funsuite.AnyFunSuite

import scala.util.Random

class SudokuLogicSpec extends AnyFunSuite:
  import SudokuLogic.*

  /** A 9×9 grid from rows of text; `.` is an empty cell. */
  private def grid(rows: String*): Vector[Int] =
    rows.mkString.map(ch => if ch == '.' then 0 else ch.asDigit).toVector

  /** True when the puzzle has exactly one solution (the in-app counter). */
  private def unique(size: GridSize, givens: Vector[Int]): Boolean =
    val v = Sudoku.Variant("t", size.n, size.boxRows, size.boxCols, 0, emoji = false, _ => "", _ => "")
    val arr = Array.tabulate(size.n, size.n)((r, c) => givens(r * size.n + c))
    Sudoku.countSolutions(v, arr, 2) == 1

  test("a full grid with one hole is Easy: one naked single") {
    val full = fill(GridSize.G9, new Random(1))
    val r = rate(GridSize.G9, full.updated(40, 0), Level.Easy)
    assert(r.solved && r.grid == full && r.steps == Map(Technique.NakedSingle -> 1))
  }

  test("a hidden single that is not a naked single needs Medium") {
    // 5 at (1,4), (2,7), (4,1), (7,2): in the top-left box, 5 fits only at (0,0).
    val g = Vector.fill(81)(0).updated(1 * 9 + 4, 5).updated(2 * 9 + 7, 5).updated(4 * 9 + 1, 5).updated(7 * 9 + 2, 5)
    val _ = assert(rate(GridSize.G9, g, Level.Easy).grid(0) == 0)
    val m = rate(GridSize.G9, g, Level.Medium)
    val _ = assert(m.grid(0) == 5)
    assert(m.steps.getOrElse(Technique.HiddenSingle, 0) >= 1 && !m.solved)
  }

  test("a puzzle that needs guessing is not solvable at Hard") {
    // AI Escargot (Arto Inkala): one solution, needs trial and error.
    val escargot = grid("1....7.9.", ".3..2...8", "..96..5..", "..53..9..", ".1..8...2", "6....4...", "3......1.", ".4......7", "..7...3..")
    val _ = assert(escargot.size == 81)
    val _ = assert(unique(GridSize.G9, escargot))
    assert(!rate(GridSize.G9, escargot, Level.Hard).solved)
  }

  test("an empty grid is not solvable at any level, and the solver stops") {
    for size <- GridSize.values; level <- Level.values do
      val _ = assert(!rate(size, Vector.fill(size.n * size.n)(0), level).solved)
  }

  test("generated puzzles: solvable at their level, the solver finds the solution, one solution") {
    for size <- GridSize.values; level <- Level.values; seed <- 1 to 5 do
      val p = generate(size, level, new Random(seed))
      val r = rate(size, p.givens, level)
      val _ = assert(r.solved && r.grid == p.solution, s"$size $level $seed")
      val _ = assert(p.givens.indices.forall(i => p.givens(i) == 0 || p.givens(i) == p.solution(i)))
      val _ = assert(unique(size, p.givens), s"$size $level $seed")
  }

  test("the hardest reachable level: small grids fall back") {
    val _ = assert(Level.values.toList.map(effective(GridSize.G4, _)) == List(Level.Easy, Level.Easy, Level.Easy))
    val _ = assert(Level.values.toList.map(effective(GridSize.G6, _)) == List(Level.Easy, Level.Medium, Level.Medium))
    assert(Level.values.toList.map(effective(GridSize.G9, _)) == Level.values.toList)
  }

  test("digging stops at the givens target, or goes on only until the puzzle needs its level") {
    for size <- GridSize.values; level <- Level.values; seed <- 1 to 3 do
      val p = generate(size, level, new Random(seed))
      val givens = p.givens.count(_ != 0)
      val below = effective(size, level).below
      // Below the target only when needed: one given more was not enough.
      val _ = assert(givens >= givensTarget(size, level) || below.exists(b => !rate(size, p.givens, b).solved),
        s"$size $level $seed: $givens givens")
      // Easy has no level below, so it stops at the target.
      if below.isEmpty && givensTarget(size, level) > 0 then
        val _ = assert(givens == givensTarget(size, level), s"$size $level $seed: $givens givens")
  }

  test("the generator is deterministic for a seed") {
    assert(generate(GridSize.G6, Level.Medium, new Random(9)) == generate(GridSize.G6, Level.Medium, new Random(9)))
  }

  // ---------- Hard techniques, on hand-made grids ----------

  /** A 9×9 grid with the given cells: ((row, column), symbol). */
  private def cells(gs: ((Int, Int), Int)*): Vector[Int] =
    gs.foldLeft(Vector.fill(81)(0)) { case (g, ((r, c), v)) => g.updated(r * 9 + c, v) }

  private def has(mask: Int, v: Int): Boolean = (mask & (1 << v)) != 0

  /** True when symbol v is placed in the cell or is still a candidate. */
  private def possible(size: GridSize, givens: Vector[Int], level: Level, cell: Int, v: Int): Boolean =
    rate(size, givens, level).grid(cell) == v || has(candidates(size, givens, level)(cell), v)

  test("naked pair: (0,0) and (0,1) can only be 1 or 2, so 1 and 2 go out of the rest of row 0") {
    // Box 0 rows 1-2 hold 3..8; 9 in columns 0 and 1 further down.
    val g = cells((1, 0) -> 3, (1, 1) -> 4, (1, 2) -> 5, (2, 0) -> 6, (2, 1) -> 7, (2, 2) -> 8, (5, 0) -> 9, (6, 1) -> 9)
    val medium = candidates(GridSize.G9, g, Level.Medium)
    val _ = assert((3 until 9).forall(c => has(medium(c), 1) && has(medium(c), 2)))
    val hard = rate(GridSize.G9, g, Level.Hard)
    val _ = assert(hard.steps.getOrElse(Technique.NakedPair, 0) >= 1)
    val after = candidates(GridSize.G9, g, Level.Hard)
    val _ = assert((3 until 9).forall(c => !has(after(c), 1) && !has(after(c), 2) && hard.grid(c) != 1 && hard.grid(c) != 2))
    // The pair keeps its two candidates.
    assert(List(0, 1).forall(c => has(after(c), 1) && has(after(c), 2)))
  }

  test("pointing: in box 0, 5 can only be in row 0, so 5 goes out of the rest of row 0") {
    val g = cells((1, 0) -> 1, (1, 1) -> 2, (1, 2) -> 3, (2, 0) -> 4, (2, 1) -> 6, (2, 2) -> 7)
    val _ = assert(rate(GridSize.G9, g, Level.Medium).steps.isEmpty)
    val _ = assert(rate(GridSize.G9, g, Level.Hard).steps.getOrElse(Technique.LockedCandidates, 0) >= 1)
    val _ = assert((3 until 9).forall(c => !possible(GridSize.G9, g, Level.Hard, c, 5)))
    // The three places in the box stay candidates.
    val after = candidates(GridSize.G9, g, Level.Hard)
    assert((0 until 3).forall(c => has(after(c), 5)))
  }

  test("box-line: in column 0, 5 can only be in box 0, so 5 goes out of the rest of box 0") {
    val g = cells((3, 0) -> 1, (4, 0) -> 2, (5, 0) -> 3, (6, 0) -> 4, (7, 0) -> 6, (8, 0) -> 7)
    val _ = assert(rate(GridSize.G9, g, Level.Medium).steps.isEmpty)
    val _ = assert(rate(GridSize.G9, g, Level.Hard).steps.getOrElse(Technique.LockedCandidates, 0) >= 1)
    val _ = assert((0 until 3).forall(r => (1 until 3).forall(c => !possible(GridSize.G9, g, Level.Hard, r * 9 + c, 5))))
    // The three places in column 0 stay candidates.
    val after = candidates(GridSize.G9, g, Level.Hard)
    assert((0 until 3).forall(r => has(after(r * 9), 5)))
  }
