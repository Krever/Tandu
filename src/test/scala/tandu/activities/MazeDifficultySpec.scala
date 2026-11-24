package tandu.activities

import org.scalatest.funsuite.AnyFunSuite

/** Statistical difficulty of the printed mazes (see [[MazeDifficulty]]).
  * The report test prints a table; the other tests keep the shapes in
  * step with each other. */
class MazeDifficultySpec extends AnyFunSuite:
  import Maze.*

  private def cfg(size: Int, perPage: PerPage, style: Style, shape: Shape): SheetConfig =
    SheetConfig(size, Loops.Off, perPage, style, Set(shape))

  // The two reports only print a table (no assertion) and are the slowest
  // part of the suite, so they are off. Change `ignore` to `test` to see
  // the table after a change to the generator.
  ignore("report: difficulty by shape, style and size") {
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

  ignore("report: difficulty of the presets, for each shape") {
    val lines =
      for
        preset <- sheetPresets
        shape <- Shape.values.toList
      yield
        val s = MazeDifficulty.measure(preset.config.copy(shapes = Set(shape)), mazes = 40, seed = 1L)
        f"${preset.presetId}%-6s ${shape.toString}%-6s  cells ${s.cells}%6.0f  solution ${s.solution}%5.1f  decisions ${s.decisions}%5.1f  effort ${s.effort}%7.1f  easiest ${s.easiest}%7.1f"
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

  /** Stats of each preset for each shape, measured once for all tests. */
  private lazy val presetTable: Map[(String, Shape), MazeDifficulty.Stats] =
    (for p <- sheetPresets; shape <- Shape.values yield
      (p.presetId, shape) -> MazeDifficulty.measure(p.config.copy(shapes = Set(shape)), mazes = 40, seed = 4L)).toMap

  private def presetStats(id: String, shape: Shape): MazeDifficulty.Stats = presetTable((id, shape))

  test("presets get harder: Easy < Medium < Hard in effort and decisions, for every shape") {
    for shape <- Shape.values do
      val List(e, m, h) = List("easy", "medium", "hard").map(presetStats(_, shape)): @unchecked
      val _ = assert(e.effort < m.effort && m.effort < h.effort, s"$shape effort: ${e.effort}, ${m.effort}, ${h.effort}")
      val _ = assert(e.decisions < m.decisions && m.decisions < h.decisions,
        s"$shape decisions: ${e.decisions}, ${m.decisions}, ${h.decisions}")
  }

  /** Decisions are the difficulty. Effort is not in this test: it follows
    * the amount of maze on the page (Hard has one big maze per page), which
    * is work, not difficulty. The order test above keeps effort growing. */
  test("the preset steps are even: Easy to Medium is about the same step as Medium to Hard") {
    for shape <- Shape.values do
      val List(e, m, h) = List("easy", "medium", "hard").map(presetStats(_, shape)): @unchecked
      val (up1, up2) = (m.decisions / e.decisions, h.decisions / m.decisions)
      val _ = assert(math.max(up1, up2) / math.min(up1, up2) <= 1.5, f"$shape decisions x$up1%.2f then x$up2%.2f")
  }

  test("at Easy and Hard, circle and hex are about as hard as square") {
    for id <- List("easy", "hard") do
      val square = presetStats(id, Shape.Square)
      for shape <- List(Shape.Circle, Shape.Hex) do
        val s = presetStats(id, shape)
        val _ = assert(s.effort >= 0.8 * square.effort, f"$id $shape effort ${s.effort}%.1f, square ${square.effort}%.1f")
  }
