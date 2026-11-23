package tandu.activities

import org.scalatest.funsuite.AnyFunSuite

import scala.collection.mutable
import scala.util.Random

class MazeSpec extends AnyFunSuite:
  import Maze.*

  private def preset(id: String): SheetConfig = sheetPresets.find(_.presetId == id).get.config
  private val easy = preset("easy")

  test("presets have today's sizes on the short side; the long side fills the slot") {
    val _ = assert(preset("easy").dims == (12, 10))
    val _ = assert(preset("medium").dims == (19, 16))
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
    assert(encodeConfig(easy) == "s10.winding.few.pp4")
  }

  test("bad tokens are clamped or ignored") {
    val _ = assert(decodeConfig("s99.pp4.bogus").size == 16)
    val _ = assert(decodeConfig("pp3").perPage == PerPage.Four)
    val _ = assert(decodeConfig("s99999999999") == easy)
    val _ = assert(decodeConfig("s2.pp1").size == 5)
    // The page count is not part of the recipe.
    assert(decodeConfig("s10.few.pp4.p3") == easy)
  }

  test("adding pages keeps the earlier pages") {
    val one = sheetGrids(easy, 1, seed = 42L)
    val three = sheetGrids(easy, 3, seed = 42L)
    val _ = assert(three.head == one.head)
    val _ = assert(three.forall(_.size == 4))
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
