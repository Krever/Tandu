package tandu.activities

import org.scalatest.funsuite.AnyFunSuite

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
    assert(SheetConfig(10, Loops.Off, PerPage.Two).dims == (10, 16))
  }

  test("cells stay at least 5.5 mm at every max size") {
    for pp <- PerPage.values do
      val (rows, cols) = SheetConfig(pp.maxSize, Loops.Off, pp).dims
      val (w, h) = pp.slotMm
      val _ = assert(math.min(w / cols, h / rows) >= 5.5, s"perPage $pp")
  }

  test("codec round trip") {
    for c <- sheetPresets.map(_.config) :+ SheetConfig(7, Loops.Many, PerPage.Two) do
      val _ = assert(decodeConfig(encodeConfig(c)) == c)
    assert(encodeConfig(easy) == "s10.few.pp4")
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
