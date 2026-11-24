package tandu.activities

import org.scalatest.funsuite.AnyFunSuite

class SudokuSheetSpec extends AnyFunSuite:
  import SudokuLogic.*
  import SudokuSheet.*

  private def preset(id: String): SheetConfig = sheetPresets.find(_.presetId == id).get.config

  test("codec round trip, presets and others") {
    for c <- sheetPresets.map(_.config) :+ SheetConfig(GridSize.G6, Symbols.Digits, Level.Hard, PerPage.Two, answers = true) do
      val _ = assert(decodeConfig(encodeConfig(c)) == c)
  }

  test("tokens") {
    val _ = assert(encodeConfig(preset("hard")) == "g9.digits.hard.pp4")
    val _ = assert(encodeConfig(preset("easy").copy(answers = true)) == "g4.shapes.easy.pp6.ans")
    val _ = assert(classicToken(Level.Medium) == "g9.digits.medium.pp6")
    assert(presetToken("medium") == "g6.shapes.medium.pp6" && presetToken("bogus") == presetToken("easy"))
  }

  test("bad tokens keep the first preset's values; 9×9 is always digits") {
    val easy = preset("easy")
    val _ = assert(decodeConfig("") == easy)
    val _ = assert(decodeConfig("g5.pp3.bogus") == easy)
    val _ = assert(decodeConfig("g9.shapes").symbols == Symbols.Digits)
    val _ = assert(decodeConfig("hard.hard").level == Level.Hard)
    // The page count is not part of the recipe.
    assert(decodeConfig("g4.shapes.easy.pp6.p3") == easy)
  }

  test("withSize: 9×9 sets digits; back to 4×4 keeps digits") {
    val nine = preset("easy").withSize(GridSize.G9)
    val _ = assert(nine.symbols == Symbols.Digits)
    assert(nine.withSize(GridSize.G4).symbols == Symbols.Digits)
  }

  test("adding pages keeps the earlier pages; each page has perPage puzzles") {
    val c = preset("medium")
    val one = sheetPages(c, 1, 42L)
    val three = sheetPages(c, 3, 42L)
    val _ = assert(three.head == one.head && three.size == 3)
    val _ = assert(three.distinct.size == 3)
    assert(three.forall { case SheetPage.Puzzles(ps) => ps.size == 6; case _ => false })
  }

  test("puzzles are at the recipe's size and level") {
    val c = preset("hard")
    val SheetPage.Puzzles(ps) = sheetPages(c, 1, 5L).head: @unchecked
    for p <- ps do
      val _ = assert(p.givens.size == 81)
      val _ = assert(rate(GridSize.G9, p.givens, Level.Hard).solved)
  }

  test("answers: extra pages at the end, labelled page·puzzle, matching the puzzles") {
    val c = SheetConfig(GridSize.G4, Symbols.Shapes, Level.Easy, PerPage.Six, answers = true)
    val ps = sheetPages(c, 10, 7L)
    val _ = assert(ps.size == 12) // 60 puzzles, 56 per 4×4 answer page
    val puzzles = ps.take(10).zipWithIndex.flatMap {
      case (SheetPage.Puzzles(xs), k) => xs.zipWithIndex.map((p, j) => (s"${k + 1}·${j + 1}", p))
      case _                          => Nil
    }
    val answers = ps.drop(10).flatMap { case SheetPage.Answers(es) => es; case _ => Nil }
    val _ = assert(answers == puzzles)
    // Answers off: no answer pages.
    assert(sheetPages(c.copy(answers = false), 10, 7L).size == 10)
  }

  test("answers per page by grid size") {
    assert(GridSize.values.toList.map(answersPerPage) == List(56, 36, 25))
  }

  test("reading one page makes only that page, once") {
    val made = scala.collection.mutable.ListBuffer.empty[Int]
    val pages = lazyPages(10, i => { made += i; SheetPage.Puzzles(Vector.empty) })
    val _ = assert(pages.size == 10 && made.isEmpty)
    val _ = pages(3)
    val _ = pages(3)
    assert(made.toList == List(3))
  }

  test("summary: preset name, else size and level") {
    val en = tandu.i18n.Strings.en
    val _ = assert(tokenSummary(presetToken("hard"))(en) == "Hard")
    val _ = assert(tokenSummary(classicToken(Level.Hard))(en) == "9×9 · Hard")
    assert(tokenSummary(presetToken("easy").replace("pp6", "pp2"))(en) == "4×4 · Easy")
  }
