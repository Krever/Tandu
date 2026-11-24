package tandu.activities

import tandu.ui.SheetBuilder

import scala.util.Random

/** The printed Sudoku sheet: the recipe, its URL codec and the pages. The
  * level of the puzzles comes from [[SudokuLogic]]. */
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
    SheetBuilder.Preset("easy", _.sudoku.easy.name, SheetConfig(GridSize.G4, Symbols.Shapes, Level.Easy, PerPage.Six, answers = false)),
    SheetBuilder.Preset("medium", _.sudoku.medium.name, SheetConfig(GridSize.G6, Symbols.Shapes, Level.Medium, PerPage.Six, answers = false)),
    SheetBuilder.Preset("hard", _.sudoku.hard.name, SheetConfig(GridSize.G9, Symbols.Digits, Level.Hard, PerPage.Four, answers = false))
  )

  /** The settings token of a preset (the first preset for an unknown id),
    * for workbook rows. */
  def presetToken(presetId: String): String =
    encodeConfig(sheetPresets.find(_.presetId == presetId).getOrElse(sheetPresets.head).config)

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
