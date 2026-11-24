package tandu.activities

import org.scalatest.funsuite.AnyFunSuite

import scala.util.Random

/** Statistical difficulty of the printed Sudoku puzzles (see
  * [[SudokuLogic]]). The report test prints a table; the other tests keep
  * every level at its level. */
class SudokuDifficultySpec extends AnyFunSuite:
  import SudokuLogic.*

  private val seeds = 1 to 20

  /** Puzzles of one size and level, made once for all tests. */
  private val puzzles = scala.collection.mutable.Map.empty[(GridSize, Level), Vector[Puzzle]]
  private def of(size: GridSize, level: Level): Vector[Puzzle] =
    puzzles.getOrElseUpdate((size, level), seeds.map(s => generate(size, level, new Random(s))).toVector)

  private def needs(size: GridSize, level: Level, p: Puzzle): Boolean =
    level.below.forall(b => !rate(size, p.givens, b).solved)

  private def mean(xs: Seq[Int]): Double = xs.sum.toDouble / xs.size

  // The report only prints a table (no assertion). Change `ignore` to
  // `test` to see the table after a change to the generator.
  ignore("report: givens, need-rate and steps by size and level") {
    val lines =
      for size <- GridSize.values.toList; level <- Level.values.toList yield
        val ps = of(size, level)
        val ratings = ps.map(p => rate(size, p.givens, level))
        val need = ps.count(needs(size, level, _))
        f"${size.toString}%-3s ${level.toString}%-6s  givens ${mean(ps.map(_.givens.count(_ != 0)))}%5.1f  " +
          f"needs level $need%2d/${ps.size}  advanced steps ${mean(ratings.map(_.advancedSteps))}%5.1f  " +
          f"steps ${mean(ratings.map(_.steps.values.sum))}%5.1f  search ${ratings.map(_.meanSearch).sum / ratings.size}%5.2f"
    println(lines.mkString("\n", "\n", "\n"))
    succeed
  }

  test("9×9 Medium and Hard puzzles need their level in at least 90% of cases") {
    for level <- List(Level.Medium, Level.Hard) do
      val n = of(GridSize.G9, level).count(needs(GridSize.G9, level, _))
      val _ = assert(n >= 18, s"$level: $n / ${seeds.size}")
  }

  test("9×9: Easy < Medium < Hard in mean givens (fewer) and in mean advanced steps (more)") {
    val List(e, m, h) = Level.values.toList.map { level =>
      val ps = of(GridSize.G9, level)
      (mean(ps.map(_.givens.count(_ != 0))), mean(ps.map(p => rate(GridSize.G9, p.givens, level).advancedSteps)))
    }: @unchecked
    val _ = assert(e._1 > m._1 && m._1 > h._1, s"givens: $e, $m, $h")
    assert(e._2 < m._2 && m._2 < h._2, s"advanced steps: $e, $m, $h")
  }

  test("every size: Easy > Medium > Hard in mean givens; Easy < Medium < Hard in search effort") {
    for size <- GridSize.values do
      val List(e, m, h) = Level.values.toList.map { level =>
        val ps = of(size, level)
        (mean(ps.map(_.givens.count(_ != 0))), ps.map(p => rate(size, p.givens, level).meanSearch).sum / ps.size)
      }: @unchecked
      val _ = assert(e._1 > m._1 && m._1 > h._1, s"$size givens: $e, $m, $h")
      val _ = assert(e._2 < m._2 && m._2 < h._2, s"$size search: $e, $m, $h")
  }

  test("6×6 Hard is never an Easy puzzle: it needs at least Medium") {
    for p <- of(GridSize.G6, Level.Hard) do
      val _ = assert(!rate(GridSize.G6, p.givens, Level.Easy).solved)
  }

  test("small grids at Hard: valid in bounded time") {
    for size <- List(GridSize.G4, GridSize.G6); s <- 1 to 10 do
      val p = generate(size, Level.Hard, new Random(s))
      val _ = assert(rate(size, p.givens, Level.Hard).solved, s"$size $s")
  }

  test("one Hard 9×9 page (4 puzzles) takes at most about 300 ms") {
    val _ = generate(GridSize.G9, Level.Hard, new Random(0)) // warm up
    val ms = (1 to 3).map { s =>
      val t = System.nanoTime()
      (1 to 4).foreach(i => generate(GridSize.G9, Level.Hard, new Random(s * 10 + i)))
      (System.nanoTime() - t) / 1e6
    }.sum / 3
    info(f"one Hard 9×9 page: $ms%.0f ms")
    assert(ms <= 300, f"one Hard 9×9 page: $ms%.0f ms")
  }

  // Where a level is hard to need (6×6 Medium), a puzzle can use every
  // try. The answer page makes all pages at once; it must not freeze the
  // preview for long.
  test("one 4×4 or 6×6 page (6 puzzles) takes at most about 60 ms, at every level") {
    for size <- List(GridSize.G4, GridSize.G6); level <- Level.values do
      val _ = generate(size, level, new Random(0)) // warm up
      val ms = (1 to 3).map { s =>
        val t = System.nanoTime()
        (1 to 6).foreach(i => generate(size, level, new Random(s * 10 + i)))
        (System.nanoTime() - t) / 1e6
      }.sum / 3
      info(f"one $size $level page: $ms%.0f ms")
      val _ = assert(ms <= 60, f"one $size $level page: $ms%.0f ms")
  }
