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

  /** The recipe of a sheet. `symbols` holds one or both kinds; with both,
    * the puzzles alternate (on a page, and from page to page). 9×9 is
    * digits only. When `answers` is on, the sheet ends with pages of the
    * solved grids. */
  final case class SheetConfig(size: GridSize, symbols: Set[Symbols], level: Level, perPage: PerPage, answers: Boolean)

  /** 9×9 is for children who know the digits, and its board has no room
    * for a legend: digits only, without a legend. */
  private def digitsOnly(size: GridSize): Boolean = size == GridSize.G9

  extension (c: SheetConfig)
    /** A new grid size. 9×9 sets digits; the other sizes keep the symbols. */
    def withSize(g: GridSize): SheetConfig =
      c.copy(size = g, symbols = if digitsOnly(g) then Set(Symbols.Digits) else c.symbols)

    /** Turn one kind of symbols on or off. The last kind stays on, and 9×9
      * stays digits. */
    def withSymbol(k: Symbols): SheetConfig = c.copy(symbols = SheetBuilder.toggle(c.symbols, k)).withSize(c.size)

  /** The name of a level (the same names as the in-app Sudoku). */
  private def levelLabel(l: Level): Strings => String = l match
    case Level.Easy   => _.sudoku.easy.name
    case Level.Medium => _.sudoku.medium.name
    case Level.Hard   => _.sudoku.hard.name

  val sheetPresets: List[SheetBuilder.Preset[SheetConfig]] = List(
    SheetBuilder.Preset("easy", levelLabel(Level.Easy), SheetConfig(GridSize.G4, Set(Symbols.Shapes), Level.Easy, PerPage.Six, answers = false)),
    SheetBuilder.Preset("medium", levelLabel(Level.Medium), SheetConfig(GridSize.G6, Set(Symbols.Shapes), Level.Medium, PerPage.Six, answers = false)),
    SheetBuilder.Preset("hard", levelLabel(Level.Hard), SheetConfig(GridSize.G9, Set(Symbols.Digits), Level.Hard, PerPage.Four, answers = false))
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
    SheetBuilder.presetName(sheetSpec, c).getOrElse(str => s"${c.size.n}×${c.size.n} · ${levelLabel(c.level)(str)}")

  /** The token of the old 9×9 print sheets: digits, six per page. */
  def classicToken(level: Level): String =
    encodeConfig(SheetConfig(GridSize.G9, Set(Symbols.Digits), level, PerPage.Six, answers = false))

  /** The old workbook source ids (one per variant) and the token of the
    * same sheet. The ids are literal: saved books keep them. */
  lazy val legacyTokens: Map[String, String] = Map(
    "sudoku-emoji4" -> presetToken("easy"),
    "sudoku-emoji6" -> presetToken("medium"),
    "sudoku-easy"   -> classicToken(Level.Easy),
    "sudoku-medium" -> classicToken(Level.Medium),
    "sudoku-hard"   -> classicToken(Level.Hard)
  )

  // ---------- codec ----------

  /** The recipe as one URL path segment, for example `g9.digits.hard.pp4`
    * or `g4.shapes-digits.easy.pp6`, with `.ans` at the end when answers
    * are on. */
  def encodeConfig(c: SheetConfig): String =
    val ans = if c.answers then ".ans" else ""
    s"g${c.size.n}.${SheetBuilder.encodeSet(Symbols.values, c.symbols)}.${c.level.toString.toLowerCase}.pp${c.perPage.n}$ans"

  private val SizeToken = "g(\\d+)".r
  private val PerPageToken = "pp(\\d+)".r

  /** The recipe from a URL path segment. The order of tokens does not
    * matter. A missing or bad token keeps the value of the first preset.
    * Without `ans`, answers are off. 9×9 always gets digits. Therefore an
    * edited or old link always gives a valid recipe. */
  def decodeConfig(token: String): SheetConfig =
    val raw = token.split('.').foldLeft(sheetPresets.head.config) { (c, t) =>
      SheetBuilder.named(Level.values, t).map(v => c.copy(level = v))
        .getOrElse(t match
          case "ans"           => c.copy(answers = true)
          case SizeToken(n)    => GridSize.values.find(g => n.toIntOption.contains(g.n)).fold(c)(g => c.copy(size = g))
          case PerPageToken(n) => PerPage.values.find(p => n.toIntOption.contains(p.n)).fold(c)(p => c.copy(perPage = p))
          case _               => SheetBuilder.namedSet(Symbols.values, t).fold(c)(v => c.copy(symbols = v))
        )
    }
    raw.withSize(raw.size)

  // ---------- pages ----------

  /** One puzzle of a page and the kind of symbols that it is printed in. */
  final case class SheetPuzzle(symbols: Symbols, puzzle: Puzzle)

  /** The solved grid of one puzzle, labelled "page·puzzle" (both from 1). */
  final case class AnswerEntry(label: String, puzzle: SheetPuzzle)

  /** A page of the sheet: puzzles, or answers. */
  enum SheetPage:
    case Puzzles(puzzles: Vector[SheetPuzzle])
    case Answers(entries: Vector[AnswerEntry])

  /** The answer page: the board size in mm, and the boards in a row and in
    * a column. Answers are small: more of them fit on a page than puzzles. */
  private def answerLayout(size: GridSize): (Int, Int, Int) = size match
    case GridSize.G4 => (21, 8, 7)
    case GridSize.G6 => (28, 6, 6)
    case GridSize.G9 => (34, 5, 5)

  def answersPerPage(size: GridSize): Int =
    val (_, cols, rows) = answerLayout(size)
    cols * rows

  /** `n` puzzle pages from one seed, then the answer pages when answers
    * are on. Puzzle page k has its own seed, so it does not change when
    * pages are added, and it can be made without the other pages. An
    * answer page makes only the puzzle pages that it shows. With both
    * kinds of symbols, the puzzles go through them in shuffled rounds
    * (with two kinds: they alternate), on a page and from page to page.
    * The rounds are made for [[SheetBuilder.MaxPages]] pages, so they also
    * do not change when pages are added. */
  def sheetPages(c: SheetConfig, n: Int, seed: Long): IndexedSeq[SheetPage] =
    val per = c.perPage.n
    val kinds = SheetBuilder.rounds(Symbols.values.toVector.filter(c.symbols), math.max(n, SheetBuilder.MaxPages) * per, new Random(seed))
    val puzzlePages = SheetBuilder.lazyPages(n) { k =>
      val rng = new Random(seed + 1_000_003L * k)
      Vector.tabulate(per)(j => SheetPuzzle(kinds(k * per + j), generate(c.size, c.level, rng)))
    }
    val perAnswerPage = answersPerPage(c.size)
    val answerPages = if c.answers then (n * per + perAnswerPage - 1) / perAnswerPage else 0
    def answers(a: Int): Vector[AnswerEntry] =
      (a * perAnswerPage until math.min((a + 1) * perAnswerPage, n * per)).map { i =>
        val (k, j) = (i / per, i % per)
        AnswerEntry(s"${k + 1}·${j + 1}", puzzlePages(k)(j))
      }.toVector
    SheetBuilder.lazyPages(n + answerPages)(i =>
      if i < n then SheetPage.Puzzles(puzzlePages(i)) else SheetPage.Answers(answers(i - n)))

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

  /** The board size in mm. A board with a legend leaves room for it. */
  private def boardMm(c: SheetConfig): Int =
    val legend = !digitsOnly(c.size)
    c.perPage match
      case PerPage.Six  => if legend then 65 else 75
      case PerPage.Four => if legend then 80 else 88
      case PerPage.Two  => if legend then 95 else 105
      case PerPage.One  => if legend then 150 else 170

  /** One board. `givens`, when set, marks the given cells (answers show
    * them bold and the rest light). */
  private def board(size: GridSize, grid: Vector[Int], symbols: Vector[String], mm: Int, givens: Option[Vector[Int]]): HtmlElement =
    val n = size.n
    div(
      cls := "sudoku-print-board",
      cls(s"sudoku-print-board--$n") := n != 9,
      styleAttr := s"width: ${mm}mm; height: ${mm}mm; font-size: ${"%.2f".format(0.6 * mm / n)}mm; " + Sudoku.gridTemplate(n),
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
      case SheetPage.Puzzles(ps) =>
        div(
          cls := s"sudoku-print-sheet sudoku-print-sheet--pp${c.perPage.n}",
          ps.map { p =>
            val symbols = symbolsOf(c.size, p.symbols)
            div(
              cls := "sudoku-print-puzzle",
              // The symbols to use, above each small board: shapes and
              // digits alike (a young child may not know every digit).
              when(!digitsOnly(c.size))(div(cls := "sudoku-print-legend", symbols.mkString(" "))),
              board(c.size, p.puzzle.givens, symbols, boardMm(c), None)
            )
          }
        )
      case SheetPage.Answers(entries) =>
        val (mm, cols, _) = answerLayout(c.size)
        div(
          cls := "sudoku-answers",
          h3(cls := "sudoku-answers__title", child.text <-- s(form(_.answers))),
          div(
            cls := "sudoku-answers__grid",
            styleAttr := s"grid-template-columns: repeat($cols, auto);",
            entries.map { e =>
              val p = e.puzzle
              div(
                cls := "sudoku-answer",
                div(cls := "sudoku-answer__label", e.label),
                board(c.size, p.puzzle.solution, symbolsOf(c.size, p.symbols), mm, Some(p.puzzle.givens))
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
            isDisabled = c.map(k => kind == Symbols.Shapes && digitsOnly(k.size)))
        }
      )),
      SheetBuilder.formRow(form(_.level), SheetBuilder.pills(
        "sudoku-level", c.map(_.level),
        Level.values.toList.map(l => (l, l.toString.toLowerCase, s(levelLabel(l)))),
        l => config.update(_.copy(level = l)))),
      SheetBuilder.formRow(form(_.perPage), SheetBuilder.pills(
        "sudoku-per-page", c.map(_.perPage),
        PerPage.values.toList.map(p => (p, p.n.toString, Val(p.n.toString))),
        p => config.update(_.copy(perPage = p)))),
      SheetBuilder.flag(config, "sudoku-answers", form(_.answers), _.answers, k => k.copy(answers = !k.answers))
    )
