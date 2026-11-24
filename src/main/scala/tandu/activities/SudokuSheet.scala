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

  val AllSymbols: Set[Symbols] = Symbols.values.toSet

  enum PerPage(val n: Int):
    case One extends PerPage(1)
    case Two extends PerPage(2)
    case Four extends PerPage(4)
    case Six extends PerPage(6)

  /** The recipe of a sheet. `symbols` holds one or both kinds; with both,
    * pages alternate (one kind per page). 9×9 is digits only. When
    * `answers` is on, the sheet ends with pages of the solved grids. */
  final case class SheetConfig(size: GridSize, symbols: Set[Symbols], level: Level, perPage: PerPage, answers: Boolean)

  extension (c: SheetConfig)
    /** A new grid size. 9×9 sets digits; the other sizes keep the symbols. */
    def withSize(g: GridSize): SheetConfig =
      c.copy(size = g, symbols = if g == GridSize.G9 then Set(Symbols.Digits) else c.symbols)

    /** Turn one kind of symbols on or off. The last kind stays on, and 9×9
      * stays digits. */
    def withSymbol(k: Symbols): SheetConfig =
      val next = if c.symbols(k) then c.symbols - k else c.symbols + k
      if next.isEmpty then c else c.copy(symbols = next).withSize(c.size)

  val sheetPresets: List[SheetBuilder.Preset[SheetConfig]] = List(
    SheetBuilder.Preset("easy", form(_.levelEasy), SheetConfig(GridSize.G4, Set(Symbols.Shapes), Level.Easy, PerPage.Six, answers = false)),
    SheetBuilder.Preset("medium", form(_.levelMedium), SheetConfig(GridSize.G6, Set(Symbols.Shapes), Level.Medium, PerPage.Six, answers = false)),
    SheetBuilder.Preset("hard", form(_.levelHard), SheetConfig(GridSize.G9, Set(Symbols.Digits), Level.Hard, PerPage.Four, answers = false))
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
    encodeConfig(SheetConfig(GridSize.G9, Set(Symbols.Digits), level, PerPage.Six, answers = false))

  // ---------- codec ----------

  /** The recipe as one URL path segment, for example `g9.digits.hard.pp4`
    * or `g4.shapes-digits.easy.pp6`, with `.ans` at the end when answers
    * are on. */
  def encodeConfig(c: SheetConfig): String =
    val ans = if c.answers then ".ans" else ""
    val symbols = Symbols.values.filter(c.symbols).map(_.toString.toLowerCase).mkString("-")
    s"g${c.size.n}.$symbols.${c.level.toString.toLowerCase}.pp${c.perPage.n}$ans"

  private val SizeToken = "g(\\d+)".r
  private val PerPageToken = "pp(\\d+)".r

  /** The recipe from a URL path segment. The order of tokens does not
    * matter. A missing or bad token keeps the value of the first preset.
    * Without `ans`, answers are off. 9×9 always gets digits. Therefore an
    * edited or old link always gives a valid recipe. */
  def decodeConfig(token: String): SheetConfig =
    val start = sheetPresets.head.config.copy(answers = false)
    val raw = token.split('.').foldLeft(start) { (c, t) =>
      SheetBuilder.named(Level.values, t).map(v => c.copy(level = v))
        .getOrElse(t match
          case "ans"           => c.copy(answers = true)
          case SizeToken(n)    => GridSize.values.find(g => n.toIntOption.contains(g.n)).fold(c)(g => c.copy(size = g))
          case PerPageToken(n) => PerPage.values.find(p => n.toIntOption.contains(p.n)).fold(c)(p => c.copy(perPage = p))
          case _ =>
            // Symbol kinds joined by '-'. A token of only dashes has no
            // parts; it must not give a recipe with no symbols.
            val parts = t.split('-').toList.map(SheetBuilder.named(Symbols.values, _))
            if parts.nonEmpty && parts.forall(_.isDefined) then c.copy(symbols = parts.flatten.toSet) else c
        )
    }
    raw.withSize(raw.size)

  // ---------- pages ----------

  /** The solved grid of one puzzle, labelled "page·puzzle" (both from 1),
    * in the symbols of its page. */
  final case class AnswerEntry(label: String, symbols: Symbols, puzzle: Puzzle)

  /** A page of the sheet: puzzles in one kind of symbols, or answers. */
  enum SheetPage:
    case Puzzles(symbols: Symbols, puzzles: Vector[Puzzle])
    case Answers(entries: Vector[AnswerEntry])

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
    * pages are added, and it can be made without the other pages. With
    * both kinds of symbols, the pages go through them in shuffled rounds;
    * the rounds are made for [[SheetBuilder.MaxPages]] pages, so they also
    * do not change when pages are added. */
  def sheetPages(c: SheetConfig, n: Int, seed: Long): IndexedSeq[SheetPage] =
    val kinds = SheetBuilder.rounds(Symbols.values.toVector.filter(c.symbols), math.max(n, SheetBuilder.MaxPages), new Random(seed))
    val puzzlePages = lazyPages(n, k =>
      val rng = new Random(seed + 1_000_003L * k)
      SheetPage.Puzzles(kinds(k), Vector.fill(c.perPage.n)(generate(c.size, c.level, rng))))
    val perAnswerPage = answersPerPage(c.size)
    val answerPages = if c.answers then (n * c.perPage.n + perAnswerPage - 1) / perAnswerPage else 0
    def entries: IndexedSeq[AnswerEntry] =
      (0 until n).flatMap { k =>
        puzzlePages(k) match
          case SheetPage.Puzzles(kind, ps) => ps.zipWithIndex.map((p, j) => AnswerEntry(s"${k + 1}·${j + 1}", kind, p))
          case SheetPage.Answers(_)        => Nil
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

  private def symbolsOf(size: GridSize, kind: Symbols): Vector[String] = kind match
    case Symbols.Shapes => PrintShapes.take(size.n)
    case Symbols.Digits => (1 to size.n).map(_.toString).toVector

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
    page match
      case SheetPage.Puzzles(kind, ps) =>
        val symbols = symbolsOf(c.size, kind)
        div(
          cls := s"sudoku-print-sheet sudoku-print-sheet--pp${c.perPage.n}",
          ps.map { p =>
            div(
              cls := "sudoku-print-puzzle",
              when(kind == Symbols.Shapes)(div(cls := "sudoku-print-legend", symbols.mkString(" "))),
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
            entries.map { e =>
              div(
                cls := "sudoku-answer",
                div(cls := "sudoku-answer__label", e.label),
                board(c.size, e.puzzle.solution, symbolsOf(c.size, e.symbols), mm, Some(e.puzzle.givens))
              )
            }
          )
        )

  /** The form rows: size, symbols, difficulty, puzzles per page, answers.
    * Symbols are chips (one or both kinds); Shapes is disabled for 9×9. */
  private def sheetForm(config: Var[SheetConfig]): HtmlElement =
    val c = config.signal
    SheetBuilder.form(
      SheetBuilder.formRow(form(_.size), SheetBuilder.pills(
        "sudoku-size", c.map(_.size),
        GridSize.values.toList.map(g => (g, g.n.toString, Val(s"${g.n}×${g.n}"))),
        g => config.update(_.withSize(g)))),
      SheetBuilder.formRow(form(_.symbols), div(
        cls := "sb-chips",
        List((Symbols.Shapes, "shapes", form(_.symbolsShapes)), (Symbols.Digits, "digits", form(_.symbolsDigits))).map { (kind, key, label) =>
          SheetBuilder.chip(s"sudoku-symbols-$key", c.map(_.symbols(kind)), s(label), () => config.update(_.withSymbol(kind)),
            isDisabled = c.map(k => kind == Symbols.Shapes && k.size == GridSize.G9))
        }
      )),
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
