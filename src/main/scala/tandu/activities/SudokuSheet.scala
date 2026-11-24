package tandu.activities

import com.raquo.laminar.api.L.*
import tandu.i18n.Strings
import tandu.ui.SheetBuilder
import tandu.ui.Components.s

import scala.util.Random

/** The printed Sudoku sheet: the recipe, its URL codec, the pages, the
  * sheet body and the form of the sheet builder. The level of the puzzles
  * comes from [[SudokuLogic]]. */
object SudokuSheet:
  import SudokuLogic.*

  // ---------- recipe ----------

  enum Symbols:
    case Shapes, Digits

  enum PerPage(val n: Int):
    case One extends PerPage(1)
    case Two extends PerPage(2)
    case Four extends PerPage(4)
    case Six extends PerPage(6)

  /** The recipe of a sheet. 9×9 is digits only. When `answers` is on, the
    * sheet ends with pages of the solved grids. */
  final case class SheetConfig(size: GridSize, symbols: Symbols, level: Level, perPage: PerPage, answers: Boolean)

  extension (c: SheetConfig)
    /** A new grid size. 9×9 sets digits; the other sizes keep the symbols. */
    def withSize(g: GridSize): SheetConfig =
      c.copy(size = g, symbols = if g == GridSize.G9 then Symbols.Digits else c.symbols)

  val sheetPresets: List[SheetBuilder.Preset[SheetConfig]] = List(
    SheetBuilder.Preset("easy", form(_.levelEasy), SheetConfig(GridSize.G4, Symbols.Shapes, Level.Easy, PerPage.Six, answers = false)),
    SheetBuilder.Preset("medium", form(_.levelMedium), SheetConfig(GridSize.G6, Symbols.Shapes, Level.Medium, PerPage.Six, answers = false)),
    SheetBuilder.Preset("hard", form(_.levelHard), SheetConfig(GridSize.G9, Symbols.Digits, Level.Hard, PerPage.Four, answers = false))
  )

  /** A label of the sheet form. */
  private def form(f: Strings.SudokuForm => String): Strings => String = str => f(str.offline.sudoku.form)

  /** The settings token of a preset (the first preset for an unknown id),
    * for workbook rows. */
  def presetToken(presetId: String): String = SheetBuilder.presetToken(sheetSpec, presetId)

  // ---------- workbook ----------

  /** All page bodies of a workbook row; the answer pages follow when the
    * row has answers on. */
  def workbookPages(token: String, n: Int, rng: Random): Seq[HtmlElement] =
    SheetBuilder.workbookPages(sheetSpec, token, n, rng)

  /** The editor of a workbook row: the print page's form and preview. */
  def settingsEditor(token: Var[String]): HtmlElement = SheetBuilder.embedded(sheetSpec, token)

  /** The workbook row label: the preset name, otherwise the size and the
    * level (for example "9×9 · Hard"). */
  def tokenSummary(token: String): Strings => String =
    val c = decodeConfig(token)
    SheetBuilder.presetName(sheetSpec, c).getOrElse { str =>
      val level = c.level match
        case Level.Easy   => str.offline.sudoku.form.levelEasy
        case Level.Medium => str.offline.sudoku.form.levelMedium
        case Level.Hard   => str.offline.sudoku.form.levelHard
      s"${c.size.n}×${c.size.n} · $level"
    }

  /** The token of the old 9×9 print sheets: digits, six per page. */
  def classicToken(level: Level): String =
    encodeConfig(SheetConfig(GridSize.G9, Symbols.Digits, level, PerPage.Six, answers = false))

  // ---------- codec ----------

  /** The recipe as one URL path segment, for example `g9.digits.hard.pp4`,
    * with `.ans` at the end when answers are on. */
  def encodeConfig(c: SheetConfig): String =
    val ans = if c.answers then ".ans" else ""
    s"g${c.size.n}.${c.symbols.toString.toLowerCase}.${c.level.toString.toLowerCase}.pp${c.perPage.n}$ans"

  private val SizeToken = "g(\\d+)".r
  private val PerPageToken = "pp(\\d+)".r

  /** The recipe from a URL path segment. The order of tokens does not
    * matter. A missing or bad token keeps the value of the first preset.
    * Without `ans`, answers are off. 9×9 always gets digits. Therefore an
    * edited or old link always gives a valid recipe. */
  def decodeConfig(token: String): SheetConfig =
    val start = sheetPresets.head.config.copy(answers = false)
    val raw = token.split('.').foldLeft(start) { (c, t) =>
      SheetBuilder.named(Symbols.values, t).map(v => c.copy(symbols = v))
        .orElse(SheetBuilder.named(Level.values, t).map(v => c.copy(level = v)))
        .getOrElse(t match
          case "ans"           => c.copy(answers = true)
          case SizeToken(n)    => GridSize.values.find(g => n.toIntOption.contains(g.n)).fold(c)(g => c.copy(size = g))
          case PerPageToken(n) => PerPage.values.find(p => n.toIntOption.contains(p.n)).fold(c)(p => c.copy(perPage = p))
          case _               => c
        )
    }
    raw.withSize(raw.size)

  // ---------- pages ----------

  /** A page of the sheet: puzzles, or the solved grids of the puzzles,
    * each labelled "page·puzzle" (both from 1). */
  enum SheetPage:
    case Puzzles(puzzles: Vector[Puzzle])
    case Answers(entries: Vector[(String, Puzzle)])

  /** Answers are small: more of them fit on a page than puzzles. */
  def answersPerPage(size: GridSize): Int = size match
    case GridSize.G4 => 56
    case GridSize.G6 => 36
    case GridSize.G9 => 25

  /** Pages that are made when they are read, once. A Hard 9×9 page takes a
    * noticeable time to make, and the preview shows one page only.
    * Equality is by reference: element equality would make every page. */
  private final class LazyPages(n: Int, make: Int => SheetPage) extends IndexedSeq[SheetPage]:
    private val cache = new Array[SheetPage](n)
    def length: Int = n
    def apply(i: Int): SheetPage =
      if cache(i) == null then cache(i) = make(i)
      cache(i)
    override def equals(o: Any): Boolean = o.asInstanceOf[AnyRef] eq this
    override def hashCode(): Int = System.identityHashCode(this)

  private[activities] def lazyPages(n: Int, make: Int => SheetPage): IndexedSeq[SheetPage] = LazyPages(n, make)

  /** `n` puzzle pages from one seed, then the answer pages when answers
    * are on. Puzzle page k has its own seed, so it does not change when
    * pages are added, and it can be made without the other pages. */
  def sheetPages(c: SheetConfig, n: Int, seed: Long): IndexedSeq[SheetPage] =
    val puzzlePages = lazyPages(n, k =>
      val rng = new Random(seed + 1_000_003L * k)
      SheetPage.Puzzles(Vector.fill(c.perPage.n)(generate(c.size, c.level, rng))))
    val perAnswerPage = answersPerPage(c.size)
    val answerPages = if c.answers then (n * c.perPage.n + perAnswerPage - 1) / perAnswerPage else 0
    def entries: IndexedSeq[(String, Puzzle)] =
      (0 until n).flatMap { k =>
        puzzlePages(k) match
          case SheetPage.Puzzles(ps) => ps.zipWithIndex.map((p, j) => (s"${k + 1}·${j + 1}", p))
          case SheetPage.Answers(_)  => Nil
      }
    lazyPages(n + answerPages, i =>
      if i < n then puzzlePages(i)
      else SheetPage.Answers(entries.slice((i - n) * perAnswerPage, (i - n + 1) * perAnswerPage).toVector))

  // ---------- sheet builder ----------

  lazy val sheetSpec: SheetBuilder.Spec[SheetConfig, SheetPage] = SheetBuilder.Spec(
    testIdPrefix = "sudoku",
    presets = sheetPresets,
    encode = encodeConfig,
    decode = decodeConfig,
    title = _ => _.offline.sudoku.printTitle,
    pagesOf = sheetPages,
    body = sheetBody,
    form = sheetForm,
    newLabel = _.sudoku.newGame
  )

  /** Shapes for paper: a child cannot draw an emoji into a cell. Ordered
    * easiest first: the 4×4 takes circle, triangle, square and plus; the
    * 6×6 adds an X and a diamond. */
  private val PrintShapes: Vector[String] = Vector("●", "▲", "■", "✚", "✕", "◆")

  private def symbolsOf(c: SheetConfig): Vector[String] = c.symbols match
    case Symbols.Shapes => PrintShapes.take(c.size.n)
    case Symbols.Digits => (1 to c.size.n).map(_.toString).toVector

  /** The board size in mm. Small grids leave room for the legend. */
  private def boardMm(c: SheetConfig): Int =
    val nine = c.size == GridSize.G9
    c.perPage match
      case PerPage.Six  => if nine then 75 else 65
      case PerPage.Four => if nine then 88 else 80
      case PerPage.Two  => if nine then 105 else 95
      case PerPage.One  => if nine then 170 else 150

  /** The board size in mm of one answer, and the answers in a row. */
  private def answerLayout(size: GridSize): (Int, Int) = size match
    case GridSize.G4 => (21, 8)
    case GridSize.G6 => (28, 6)
    case GridSize.G9 => (34, 5)

  /** One board. `givens`, when set, marks the given cells (answers show
    * them bold and the rest light). */
  private def board(size: GridSize, grid: Vector[Int], symbols: Vector[String], mm: Int, givens: Option[Vector[Int]]): HtmlElement =
    val n = size.n
    div(
      cls := "sudoku-print-board",
      cls(s"sudoku-print-board--$n") := n != 9,
      styleAttr := s"width: ${mm}mm; height: ${mm}mm; font-size: ${"%.2f".format(0.6 * mm / n)}mm; " +
        s"grid-template-columns: repeat($n, 1fr); grid-template-rows: repeat($n, 1fr);",
      grid.indices.map { i =>
        val (r, c) = (i / n, i % n)
        div(
          cls := "sudoku-print-cell",
          cls("sudoku-print-cell--boxl") := c > 0 && c % size.boxCols == 0,
          cls("sudoku-print-cell--boxt") := r > 0 && r % size.boxRows == 0,
          cls("sudoku-print-cell--given") := givens.exists(_(i) != 0),
          if grid(i) == 0 then "" else symbols(grid(i) - 1)
        )
      }
    )

  def sheetBody(c: SheetConfig, page: SheetPage): HtmlElement =
    val symbols = symbolsOf(c)
    page match
      case SheetPage.Puzzles(ps) =>
        div(
          cls := s"sudoku-print-sheet sudoku-print-sheet--pp${c.perPage.n}",
          ps.map { p =>
            div(
              cls := "sudoku-print-puzzle",
              when(c.symbols == Symbols.Shapes)(div(cls := "sudoku-print-legend", symbols.mkString(" "))),
              board(c.size, p.givens, symbols, boardMm(c), None)
            )
          }
        )
      case SheetPage.Answers(entries) =>
        val (mm, perRow) = answerLayout(c.size)
        div(
          cls := "sudoku-answers",
          h3(cls := "sudoku-answers__title", child.text <-- s(form(_.answersTitle))),
          div(
            cls := "sudoku-answers__grid",
            styleAttr := s"grid-template-columns: repeat($perRow, auto);",
            entries.map { (label, p) =>
              div(
                cls := "sudoku-answer",
                div(cls := "sudoku-answer__label", label),
                board(c.size, p.solution, symbols, mm, Some(p.givens))
              )
            }
          )
        )

  /** The form rows: size, symbols, difficulty, puzzles per page, answers.
    * Shapes are disabled for 9×9. */
  private def sheetForm(config: Var[SheetConfig]): HtmlElement =
    val c = config.signal
    SheetBuilder.form(
      SheetBuilder.formRow(form(_.size), SheetBuilder.pills(
        "sudoku-size", c.map(_.size),
        GridSize.values.toList.map(g => (g, g.n.toString, Val(s"${g.n}×${g.n}"))),
        g => config.update(_.withSize(g)))),
      SheetBuilder.formRow(form(_.symbols), SheetBuilder.pills(
        "sudoku-symbols", c.map(_.symbols),
        List((Symbols.Shapes, "shapes", s(form(_.symbolsShapes))), (Symbols.Digits, "digits", s(form(_.symbolsDigits)))),
        v => config.update(k => k.copy(symbols = v).withSize(k.size)),
        isDisabled = v => c.map(k => v == Symbols.Shapes && k.size == GridSize.G9))),
      SheetBuilder.formRow(form(_.level), SheetBuilder.pills(
        "sudoku-level", c.map(_.level),
        List((Level.Easy, form(_.levelEasy)), (Level.Medium, form(_.levelMedium)), (Level.Hard, form(_.levelHard)))
          .map((l, label) => (l, l.toString.toLowerCase, s(label))),
        l => config.update(_.copy(level = l)))),
      SheetBuilder.formRow(form(_.perPage), SheetBuilder.pills(
        "sudoku-per-page", c.map(_.perPage),
        PerPage.values.toList.map(p => (p, p.n.toString, Val(p.n.toString))),
        p => config.update(_.copy(perPage = p)))),
      SheetBuilder.flag(config, "sudoku-answers", form(_.answers), _.answers, k => k.copy(answers = !k.answers))
    )
