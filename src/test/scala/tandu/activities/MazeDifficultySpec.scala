package tandu.activities

import org.scalatest.funsuite.AnyFunSuite

/** Statistical difficulty of the printed mazes (see [[MazeDifficulty]]).
  * The report test prints a table; the other tests keep the shapes in
  * step with each other. */
class MazeDifficultySpec extends AnyFunSuite:
  import Maze.*

  private def cfg(size: Int, perPage: PerPage, style: Style, shape: Shape): SheetConfig =
    SheetConfig(size, Loops.Off, perPage, style, Set(shape))

  test("report: difficulty by shape, style and size") {
    val lines =
      for
        (size, pp) <- List((10, PerPage.Four), (16, PerPage.Four), (24, PerPage.One))
        style <- Style.values.toList
        shape <- Shape.values.toList
      yield
        val s = MazeDifficulty.measure(cfg(size, pp, style, shape), mazes = 40, seed = 1L)
        f"size $size%2d ${pp.toString}%-4s ${style.toString}%-8s ${shape.toString}%-6s  cells ${s.cells}%6.0f  solution ${s.solution}%5.1f  decisions ${s.decisions}%5.1f  effort ${s.effort}%7.1f  easiest ${s.easiest}%7.1f  detour ${s.detour}%5.2f"
    println(lines.mkString("\n", "\n", "\n"))
    succeed
  }

  test("a circle maze is not much easier than a hex maze of the same size") {
    for (size, pp) <- List((16, PerPage.Four), (24, PerPage.One)); style <- Style.values do
      val circle = MazeDifficulty.measure(cfg(size, pp, style, Shape.Circle), mazes = 40, seed = 2L)
      val hex = MazeDifficulty.measure(cfg(size, pp, style, Shape.Hex), mazes = 40, seed = 2L)
      val _ = assert(circle.effort >= 0.75 * hex.effort,
        f"size $size $style: circle effort ${circle.effort}%.1f, hex ${hex.effort}%.1f")
      // Also from the goal: the centre must not be an easy way in.
      val _ = assert(circle.easiest >= 0.75 * hex.easiest,
        f"size $size $style: circle easiest ${circle.easiest}%.1f, hex ${hex.easiest}%.1f")
  }

  test("the centre of a circle maze has one door") {
    for style <- Style.values; loops <- Loops.values do
      val c = SheetConfig(16, loops, PerPage.Four, style, Set(Shape.Circle))
      for page <- Maze.sheetPages(c, 5, seed = 3L); m <- page.mazes do
        val _ = assert(m.degree(m.layout.goal) == 1, s"$style $loops")
  }
