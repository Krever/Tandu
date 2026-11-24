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

  test("the generator is deterministic for a seed") {
    assert(generate(GridSize.G6, Level.Medium, new Random(9)) == generate(GridSize.G6, Level.Medium, new Random(9)))
  }
