package tandu.activities

import org.scalatest.funsuite.AnyFunSuite

import scala.collection.mutable
import scala.util.Random

class MazeSpec extends AnyFunSuite:
  import Maze.*

  private def preset(id: String): SheetConfig = sheetPresets.find(_.presetId == id).get.config
  private val easy = preset("easy")

  test("presets have today's sizes on the short side; the long side fills the slot") {
    val _ = assert(preset("easy").dims == (13, 11))
    val _ = assert(preset("medium").dims == (15, 13))
    assert(preset("hard").dims == (29, 24))
  }

  test("two per page is wide") {
    assert(SheetConfig(10, Loops.Off, PerPage.Two, Style.Winding).dims == (10, 16))
  }

  test("cells stay at least 5.5 mm at every max size") {
    for pp <- PerPage.values do
      val (rows, cols) = SheetConfig(pp.maxSize, Loops.Off, pp, Style.Winding).dims
      val (w, h) = pp.slotMm
      val _ = assert(math.min(w / cols, h / rows) >= 5.5, s"perPage $pp")
  }

  test("codec round trip") {
    for c <- sheetPresets.map(_.config) :+ SheetConfig(7, Loops.Many, PerPage.Two, Style.Branchy) do
      val _ = assert(decodeConfig(encodeConfig(c)) == c)
    assert(encodeConfig(easy) == "s11.winding.few.pp4")
  }

  test("bad tokens are clamped or ignored") {
    val _ = assert(decodeConfig("s99.pp4.bogus").size == 16)
    val _ = assert(decodeConfig("pp3").perPage == PerPage.Four)
    val _ = assert(decodeConfig("s99999999999") == easy)
    val _ = assert(decodeConfig("s2.pp1").size == 5)
    // The page count is not part of the recipe.
    assert(decodeConfig("s11.few.pp4.p3") == easy)
  }

  test("adding pages keeps the earlier pages") {
    val one = sheetPages(easy, 1, seed = 42L)
    val three = sheetPages(easy, 3, seed = 42L)
    val _ = assert(three.head == one.head)
    val _ = assert(three.forall(_.mazes.size == 4))
    assert(three.distinct.size == 3)
  }

  // ---------- styles ----------

  private def cells(g: Grid): Seq[(Int, Int)] = for r <- 0 until g.rows; c <- 0 until g.cols yield (r, c)

  private def passages(g: Grid): Int =
    cells(g).map((r, c) => (if g.rightOpen(r, c) then 1 else 0) + (if g.downOpen(r, c) then 1 else 0)).sum

  private def degree(g: Grid, r: Int, c: Int): Int =
    List((r - 1, c), (r + 1, c), (r, c - 1), (r, c + 1)).count(n => g.connected((r, c), n))

  private def reachable(g: Grid): Int =
    val seen = mutable.Set((0, 0))
    val queue = mutable.Queue((0, 0))
    while queue.nonEmpty do
      val (r, c) = queue.dequeue()
      for n <- List((r - 1, c), (r + 1, c), (r, c - 1), (r, c + 1)) if g.connected((r, c), n) && seen.add(n) do
        queue.enqueue(n)
    seen.size

  private def deadEnds(style: Style): Int =
    (1 to 10).map(seed => generate(20, 20, 0.0, new Random(seed), style)).map(g => cells(g).count(degree(g, _, _) == 1)).sum

  test("every style makes a perfect maze: all cells reachable, one way between any two") {
    for style <- Style.values; seed <- 1 to 5 do
      val g = generate(12, 15, 0.0, new Random(seed), style)
      val _ = assert(reachable(g) == 12 * 15, s"$style seed $seed")
      val _ = assert(passages(g) == 12 * 15 - 1, s"$style seed $seed")
  }

  test("styles differ: winding has the fewest dead ends, branchy the most") {
    val (w, b, br) = (deadEnds(Style.Winding), deadEnds(Style.Balanced), deadEnds(Style.Branchy))
    val _ = assert(w < b, s"winding $w, balanced $b")
    assert(b < br, s"balanced $b, branchy $br")
  }

  test("presets get harder: Easy winding, Medium balanced, Hard branchy") {
    assert(List("easy", "medium", "hard").map(preset(_).style) == List(Style.Winding, Style.Balanced, Style.Branchy))
  }

  test("a token without a style keeps the style of the first preset") {
    val _ = assert(decodeConfig("s16.off.pp4").style == Style.Winding)
    assert(decodeConfig("s16.branchy.off.pp4").style == Style.Branchy)
  }

  // ---------- shapes ----------

  private def cfg(size: Int, perPage: PerPage, shape: Shape): SheetConfig =
    SheetConfig(size, Loops.Off, perPage, Style.Winding, Set(shape))

  /** The hex side length in mm, from the slot and the hex dims. */
  private def hexSide(c: SheetConfig): Double =
    val (rows, cols) = c.hexDims
    val (w, h) = c.perPage.slotMm
    math.min(w / (math.sqrt(3) * (cols + 0.5)), h / (1.5 * rows + 0.5))

  test("hex dims follow the slot") {
    val _ = assert(cfg(16, PerPage.Four, Shape.Hex).hexDims == (17, 12))
    val _ = assert(cfg(20, PerPage.Two, Shape.Hex).hexDims == (18, 26))
    assert(cfg(30, PerPage.One, Shape.Hex).hexDims == (36, 26))
  }

  test("circle rings from size") {
    val _ = assert(cfg(10, PerPage.Four, Shape.Circle).rings == 6)
    val _ = assert(cfg(30, PerPage.One, Shape.Circle).rings == 16)
    // 30 square cells: 43 (3 rings) is nearer by ratio than 19 (2 rings).
    assert(cfg(5, PerPage.Four, Shape.Circle).rings == 3)
  }

  test("every shape stays drawable at every max size") {
    for pp <- PerPage.values do
      val (w, h) = pp.slotMm
      val sq = cfg(pp.maxSize, pp, Shape.Square)
      val (rows, cols) = sq.dims
      val _ = assert(math.min(w / cols, h / rows) >= 5.5, s"square $pp")
      val ci = cfg(pp.maxSize, pp, Shape.Circle)
      val ring = math.min(w, h) / 2 / (ci.rings + 1)
      val _ = assert(ring >= 5.5, s"circle $pp")
      // The doorway between two rings is the inner arc of a cell.
      val layout = MazeGraph.CircleLayout(ci.rings)
      val doorway = (1 to ci.rings).map(i => 2 * math.Pi * i / layout.ringSizes(i)).min * ring
      val _ = assert(doorway >= 4.0, s"circle doorway $pp: $doorway")
      val side = hexSide(cfg(pp.maxSize, pp, Shape.Hex))
      val _ = assert(side >= 4.0, s"hex edge $pp: $side")
      val _ = assert(math.sqrt(3) * side >= 5.5, s"hex width $pp")
  }

  test("shapes codec") {
    val _ = assert(encodeConfig(easy) == "s11.winding.few.pp4")
    val sc = easy.withShape(Shape.Circle)
    val _ = assert(encodeConfig(sc).endsWith(".square-circle"))
    val _ = assert(decodeConfig(encodeConfig(sc)) == sc)
    val _ = assert(decodeConfig("s10.winding.few.pp4.circle-hex").shapes == Set(Shape.Circle, Shape.Hex))
    val _ = assert(decodeConfig("s10.square").shapes == Set(Shape.Square))
    val _ = assert(decodeConfig("s10.bogus-hex").shapes == Set(Shape.Square))
    // Only dashes: no shape names at all. The recipe must keep a shape.
    val _ = assert(decodeConfig("s10.-").shapes == Set(Shape.Square))
    assert(decodeConfig("s10.--").shapes == Set(Shape.Square))
  }

  test("the last shape cannot be removed") {
    assert(easy.withShape(Shape.Square) == easy)
  }

  test("pages go through the shapes in rounds") {
    val pages = sheetPages(easy.withAllShapes, 6, seed = 7L)
    for round <- pages.map(_.shape).grouped(3) do
      val _ = assert(round.toSet == Shape.values.toSet, pages.map(_.shape))
    for p <- pages; m <- p.mazes do
      val ok = p.shape match
        case Shape.Square => m.layout.isInstanceOf[MazeGraph.RectLayout]
        case Shape.Circle => m.layout.isInstanceOf[MazeGraph.CircleLayout]
        case Shape.Hex    => m.layout.isInstanceOf[MazeGraph.HexLayout]
      val _ = assert(ok, s"${p.shape}: ${m.layout}")
  }

  /** Cells of a maze of this shape: the layout's cell count. */
  private def cellsOf(c: SheetConfig, shape: Shape): Int =
    sheetPages(c.copy(shapes = Set(shape)), 1, seed = 1L).head.mazes.head.layout.cellCount

  test("circle and hex have about as many cells as square, as far as the paper allows") {
    for pp <- PerPage.values; size <- SheetConfig.MinSize to pp.maxSize do
      val c = cfg(size, pp, Shape.Square)
      val square = cellsOf(c, Shape.Square)
      for shape <- List(Shape.Circle, Shape.Hex) do
        val cells = cellsOf(c, shape)
        val atMax = cellsOf(cfg(pp.maxSize, pp, shape), shape)
        // Circle cell counts grow in large steps at small sizes (19, 43,
        // 67, …), so the band is wide.
        val _ = assert(cells <= 1.5 * square, s"$shape $pp size $size: $cells cells, square $square")
        val _ = assert(cells >= math.min(0.66 * square, atMax), s"$shape $pp size $size: $cells cells, square $square, max $atMax")
  }
