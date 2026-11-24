package tandu.workbook

import com.raquo.laminar.api.L.*
import org.scalajs.dom
import tandu.activities.{Battleships, DotToDot, GuideRobot, LetterTracing, MathPractice, Maze, SeekAndFind, SudokuSheet, WordBuilder, WordSearch}
import tandu.activities.SudokuLogic.Level
import tandu.i18n.{Lang, Strings}

import scala.scalajs.js
import scala.util.{Random, Try}

/** The workbook model: a recipe is a list of (page source, count) rows plus
  * book-level options (cover page, kid's name). Re-rendering the same recipe
  * always yields *fresh* puzzles — the regeneration loop is the point: the
  * book never runs out.
  */
object Workbook:

  /** One row of the composition list: print `count` pages of `sourceId`.
    * `params` holds the row's own settings for a source that has
    * [[Settings]] (an opaque token that only the source reads). None means
    * the source's default. */
  final case class Row(sourceId: String, count: Int, params: Option[String] = None)

  /** `name` and `coverEmoji` are the book's identity — they label it on the
    * books list and, when the cover page is on, land on the cover too. `id`
    * is the stable handle: the list is positional, but URLs, autosave and
    * share-dedup must survive a book's slot shifting (a deletion elsewhere,
    * an import), so they key off the id rather than the index. */
  final case class Recipe(
      id: String,
      rows: Vector[Row],
      cover: Boolean,
      name: String,
      coverEmoji: String
  )

  /** A fresh book id. 64 random bits, lowercase hex — unique enough for a
    * device-local list, and URL-safe with no escaping. */
  def freshId(): String = java.lang.Long.toHexString(Random.nextLong())

  /** Identity pictures offered alongside the name; one is drawn at random for
    * a fresh book. Kept modest in size on the printed cover — raster emoji
    * look bad rendered large. */
  val coverEmojis: Vector[String] =
    Vector("🦊", "🐰", "🦄", "🚀", "🐬", "🦖", "⭐", "🌈", "🐱", "⚽")

  def freshRecipe(rows: Vector[Row] = Vector.empty, name: String = ""): Recipe =
    Recipe(freshId(), rows, cover = true, name = name, coverEmoji = coverEmojis(Random.nextInt(coverEmojis.size)))

  // ---------- page sources ----------

  /** Everything a render needs besides the source itself. `pageIndex` counts
    * within one row, so sequenced sources (letter tracing) can hand out page
    * 1, page 2, … of their material. */
  final case class Ctx(lang: Lang, rng: Random, pageIndex: Int)

  /** Settings that a parent can change per row, edited with the source's
    * own form. The token is opaque to the workbook: it is saved, shared and
    * handed back to the source unchanged.
    *   - `summary`: the short label on the row line (for example "Easy").
    *   - `title`: the print-sheet title, which can depend on the settings.
    *   - `pages`: all page bodies of a row at once, so the source can plan
    *     across them (Seek & Find: themes in rounds).
    *   - `editor`: the form with a live preview; `token` is a two-way view
    *     of the row's settings. */
  final case class Settings(
      default: String,
      summary: String => Strings => String,
      title: String => Strings => String,
      pages: (String, Int, Random) => Seq[HtmlElement],
      editor: Var[String] => HtmlElement
  )

  /** One concrete page variant within a kind: the variant-chip label, the
    * print-sheet title, and a generator producing one A4 page body. */
  final case class Source(
      id: String,
      variantLabel: Strings => String,
      printTitle: Strings => String,
      render: Ctx => HtmlElement,
      settings: Option[Settings] = None
  ):
    /** The token a row uses: its own, or the default. */
    def paramsOf(row: Row): String = settings.fold("")(st => row.params.getOrElse(st.default))

    /** The print-sheet title for a row. */
    def titleOf(row: Row): Strings => String = settings.fold(printTitle)(_.title(paramsOf(row)))

  /** One kind of page — the unit a parent picks. A row stores a concrete
    * variant (Source) but is presented as its kind, with the variant
    * switchable in place: parents don't know which difficulty fits until
    * they've seen the preview. */
  final case class Group(
      id: String,
      glyph: String,
      tint: String,
      name: Strings => String,
      sources: List[Source]
  )

  /** The catalog, roughly youngest-first: pre-reader pages, then literacy- and
    * number-gated worksheets, then the 2-player extras. (Coloring pages are
    * deliberately absent: DotToDot.coloringSheetBody exists but the outlines
    * aren't good enough yet — revisit before surfacing.) */
  val groups: List[Group] = List(
    Group("letter-tracing", "✍️", "peach", _.letterTracing.name,
      LetterTracing.variants.map { v =>
        Source(s"letter-tracing-${v.id}", v.label, _.letterTracing.printTitle,
          ctx => LetterTracing.printSheetBody(v, ctx.lang, ctx.pageIndex))
      }),
    Group("dot-to-dot", "✏️", "sky", _.dotToDot.name, List(
      Source("dot-to-dot", _.dotToDot.name, _.offline.dotToDot.printTitle,
        ctx => DotToDot.printSheetBody(ctx.rng))
    )),
    // One source with its own form (the print page's form): find or count,
    // themes, crowd, layout. Rows keep their settings in `params`.
    Group("seek-and-find", "👀", "vermilion", _.seekAndFind.name, List(
      Source("seek-and-find", _.seekAndFind.name, _.offline.seekAndFind.printTitle,
        ctx => SeekAndFind.workbookPages(SeekAndFind.presetToken("easy"), 1, ctx.rng).head,
        settings = Some(Settings(
          default = SeekAndFind.presetToken("easy"),
          summary = SeekAndFind.tokenSummary,
          title = SeekAndFind.tokenTitle,
          pages = SeekAndFind.workbookPages,
          editor = SeekAndFind.settingsEditor
        )))
    )),
    // One source with its own form (the print page's form): size, symbols,
    // difficulty, puzzles per page, answers. Rows keep their settings in
    // `params`.
    Group("sudoku", "#", "plum", _.sudoku.name, List(
      Source("sudoku", _.sudoku.name, _.offline.sudoku.printTitle,
        ctx => SudokuSheet.workbookPages(SudokuSheet.presetToken("easy"), 1, ctx.rng).head,
        settings = Some(Settings(
          default = SudokuSheet.presetToken("easy"),
          summary = SudokuSheet.tokenSummary,
          title = _ => _.offline.sudoku.printTitle,
          pages = SudokuSheet.workbookPages,
          editor = SudokuSheet.settingsEditor
        )))
    )),
    // One source with its own form (the print page's form): size, loops,
    // mazes per page. Rows keep their settings in `params`.
    Group("maze", "🐭", "peach", _.maze.name, List(
      Source("maze", _.maze.name, _.offline.maze.printTitle,
        ctx => Maze.workbookPages(Maze.presetToken("easy"), 1, ctx.rng).head,
        settings = Some(Settings(
          default = Maze.presetToken("easy"),
          summary = Maze.tokenSummary,
          title = _ => _.offline.maze.printTitle,
          pages = Maze.workbookPages,
          editor = Maze.settingsEditor
        )))
    )),
    // The doc's "occasional variety page" — write-the-program robot sheets.
    Group("guide-robot", "🤖", "olive", _.guideRobot.name,
      GuideRobot.variants.map { v =>
        Source(s"guide-robot-${v.id}", v.nameKey, _.offline.guideRobot.printTitle,
          ctx => GuideRobot.printSheetBody(v, ctx.rng))
      }),
    Group("math", "➕", "mustard", _.mathPractice.name,
      MathPractice.levels.map { l =>
        Source(s"math-${l.id}", l.label, _.mathPractice.printTitle,
          _ => MathPractice.printSheetBody(l))
      }),
    Group("word-builder", "🔤", "teal", _.wordBuilder.name,
      WordBuilder.levels.map { l =>
        Source(s"word-builder-${l.id}", l.label, _.wordBuilder.printTitle,
          ctx => WordBuilder.printSheetBody(l, ctx.lang))
      }),
    Group("word-search", "🔎", "teal", _.wordSearch.name,
      WordSearch.variants.map { v =>
        Source(s"word-search-${v.id}", v.nameKey, _.offline.wordSearch.printTitle,
          ctx => WordSearch.printSheetBody(v, ctx.lang, ctx.rng))
      }),
    // 2-player extras for the shared/travel book.
    Group("tic-tac-toe-grids", "⭕", "olive", _.ticTacToe.name, List(
      Source("tic-tac-toe-grids", _.ticTacToe.classic.name, _.ticTacToe.name,
        _ => ticTacToeSheetBody()),
      Source("gomoku-grids", _.ticTacToe.gomoku.name, _.ticTacToe.name,
        _ => gomokuSheetBody())
    )),
    Group("battleships", "⊕", "vermilion", _.battleships.name, List(
      Source("battleships", _.battleships.name, _.offline.battleships.printTitle,
        _ => Battleships.printSheetBody())
    )),
    // Generic paper — drawing, free writing, own sums. No generator at all.
    Group("paper", "📄", "sky", _.workbook.paperName, List(
      Source("paper-blank", _.workbook.paperBlank, _.workbook.paperName, _ => paperSheetBody("blank")),
      Source("paper-lined", _.workbook.paperLined, _.workbook.paperName, _ => paperSheetBody("lined")),
      Source("paper-squared", _.workbook.paperSquared, _.workbook.paperName, _ => paperSheetBody("squared"))
    ))
  )

  /** Resolve a stored source id to its kind and variant. */
  def byId(id: String): Option[(Group, Source)] =
    groups.iterator.flatMap(g => g.sources.find(_.id == id).map(g -> _)).nextOption()

  /** A page of blank tic-tac-toe boards — canonical travel-book filler. Pure
    * markup, no generation. Games are seconds long, so the page packs twenty. */
  private def ticTacToeSheetBody(): HtmlElement =
    div(
      cls := "wbk-ttt-sheet",
      (0 until 20).map(_ =>
        div(cls := "wbk-ttt-board", (0 until 9).map(_ => div(cls := "wbk-ttt-cell")))
      )
    )

  /** Two blank 10×10 gomoku boards — five in a row, the bigger sibling the
    * in-app tic-tac-toe graduates to. */
  private def gomokuSheetBody(): HtmlElement =
    div(
      cls := "wbk-gomoku-sheet",
      (0 until 2).map(_ =>
        div(cls := "wbk-gomoku-board", (0 until 100).map(_ => div(cls := "wbk-gomoku-cell")))
      )
    )

  // Paper geometry in viewBox units ≙ mm: 10mm ruling, 5mm squares — the
  // school-notebook standards. SVG strokes, not CSS backgrounds, because
  // browsers skip backgrounds when printing by default.
  private val PaperW = 190
  private val PaperH = 235

  /** Public: the Paper tool prints the same sheets outside any book. */
  def paperSheetBody(kind: String): HtmlElement =
    div(
      cls := s"wbk-paper wbk-paper--$kind",
      kind match
        case "blank" => emptyNode
        case "lined" =>
          svg.svg(
            svg.cls := "wbk-paper-svg",
            svg.viewBox := s"0 0 $PaperW $PaperH",
            (1 until PaperH / 10).map(i => paperLine(0, i * 10, PaperW, i * 10))
          )
        case _ =>
          svg.svg(
            svg.cls := "wbk-paper-svg",
            svg.viewBox := s"0 0 $PaperW $PaperH",
            (0 to PaperH / 5).map(i => paperLine(0, i * 5, PaperW, i * 5)) ++
              (0 to PaperW / 5).map(i => paperLine(i * 5, 0, i * 5, PaperH))
          )
    )

  private def paperLine(x1: Int, y1: Int, x2: Int, y2: Int): SvgElement =
    svg.line(
      svg.cls := "wbk-paper-line",
      svg.x1 := x1.toString,
      svg.y1 := y1.toString,
      svg.x2 := x2.toString,
      svg.y2 := y2.toString
    )

  // ---------- presets ----------

  /** A built-in starting point: age bands and scenario packs share one chip
    * mechanism. Rows only — cover settings belong to the parent's recipe. */
  final case class Preset(id: String, name: Strings => String, rows: Vector[Row])

  /** A one-page Seek & Find row with the given settings token. */
  private def seekRow(token: String): Row = Row("seek-and-find", 1, Some(token))

  /** A one-page Sudoku row with the given settings token. */
  private def sudokuRow(token: String): Row = Row("sudoku", 1, Some(token))

  /** A one-page Maze row with the settings of a preset. */
  private def mazeRow(presetId: String): Row = Row("maze", 1, Some(Maze.presetToken(presetId)))

  val presets: List[Preset] = List(
    // Pre-readers: pictures, pens and counting; no word puzzles yet.
    Preset("age-4-5", _.workbook.age45, Vector(
      Row("letter-tracing-upper", 1),
      Row("dot-to-dot", 1),
      seekRow(SeekAndFind.presetToken("easy")),
      seekRow(SeekAndFind.countSheetToken),
      sudokuRow(SudokuSheet.presetToken("easy")),
      Row("math-easy", 1),
      mazeRow("easy")
    )),
    // Early readers: word builder and word search join in.
    Preset("age-6-7", _.workbook.age67, Vector(
      Row("dot-to-dot", 1),
      Row("letter-tracing-lower", 1),
      seekRow(SeekAndFind.presetToken("medium")),
      sudokuRow(SudokuSheet.presetToken("medium")),
      Row("word-builder-easy", 1),
      Row("word-search-easy", 1),
      Row("math-medium", 1),
      mazeRow("medium"),
      Row("guide-robot-easy", 1)
    )),
    // Confident readers: mid/hard everything.
    Preset("age-8", _.workbook.age8plus, Vector(
      sudokuRow(SudokuSheet.classicToken(Level.Medium)),
      sudokuRow(SudokuSheet.classicToken(Level.Hard)),
      mazeRow("hard"),
      Row("word-search-medium", 1),
      Row("word-search-hard", 1),
      Row("math-hard", 1),
      Row("word-builder-medium", 1),
      seekRow(SeekAndFind.presetToken("hard")),
      Row("guide-robot-medium", 1)
    )),
    // The whole book is understood as shared — game grids belong here.
    Preset("travel", _.workbook.travel, Vector(
      Row("tic-tac-toe-grids", 1),
      mazeRow("easy"),
      sudokuRow(SudokuSheet.presetToken("medium")),
      Row("dot-to-dot", 1),
      seekRow(SeekAndFind.presetToken("medium")),
      Row("word-search-easy", 1),
      Row("math-medium", 1)
    )),
    Preset("math-boost", _.workbook.mathBoost, Vector(
      Row("math-easy", 1),
      Row("math-medium", 1),
      Row("math-hard", 1),
      seekRow(SeekAndFind.countSheetToken),
      sudokuRow(SudokuSheet.presetToken("medium")),
      sudokuRow(SudokuSheet.classicToken(Level.Easy))
    )),
    // Doubles as "empty the book" via the replace-with-preset panel.
    Preset("empty", _.workbook.emptyPreset, Vector.empty)
  )

  // ---------- persistence codec ----------

  /** Hand-rolled JSON via the host's JSON object — no library needed for one
    * small shape. Unknown source ids are dropped on read so a stored book
    * survives catalog changes. */
  object Codec:
    private def rowsToJs(rows: Vector[Row]): js.Array[js.Dynamic] =
      js.Array(rows.map { r =>
        val d = js.Dynamic.literal(s = r.sourceId, c = r.count)
        r.params.foreach(p => d.updateDynamic("p")(p))
        d
      }*)

    /** Old source ids of Seek & Find, Maze and Sudoku, mapped to (new
      * source id, settings token of the same sheet). Books saved before
      * these kinds got their forms keep their pages. */
    private val legacyRows: Map[String, (String, String)] =
      val levels = List("easy", "medium", "hard")
      levels.map(p => s"seek-and-find-$p" -> ("seek-and-find", SeekAndFind.presetToken(p))).toMap +
        ("seek-and-find-count" -> ("seek-and-find", SeekAndFind.countSheetToken)) ++
        levels.map(p => s"maze-$p" -> ("maze", Maze.presetToken(p))) +
        ("sudoku-emoji4" -> ("sudoku", SudokuSheet.presetToken("easy"))) +
        ("sudoku-emoji6" -> ("sudoku", SudokuSheet.presetToken("medium"))) ++
        Level.values.map(l => s"sudoku-${l.toString.toLowerCase}" -> ("sudoku", SudokuSheet.classicToken(l)))

    private def migrate(row: Row): Row =
      legacyRows.get(row.sourceId).fold(row)((id, token) => row.copy(sourceId = id, params = Some(token)))

    private def recipeToJs(r: Recipe): js.Dynamic =
      js.Dynamic.literal(id = r.id, cover = r.cover, name = r.name, emoji = r.coverEmoji, rows = rowsToJs(r.rows))

    private def recipeFromJs(d: js.Dynamic): Recipe =
      val rows = d.rows.asInstanceOf[js.Array[js.Dynamic]].toVector
        .map(row => Row(row.s.asInstanceOf[String], row.c.asInstanceOf[Int], row.p.asInstanceOf[js.UndefOr[String]].toOption))
        .map(migrate)
        .filter(row => byId(row.sourceId).isDefined && row.count > 0)
      // Books stored before ids existed (or hand-edited) get one on read, so a
      // pre-id library keeps working — it's saved back with ids on next edit.
      val id = d.id.asInstanceOf[js.UndefOr[String]].toOption.filter(_.nonEmpty).getOrElse(freshId())
      Recipe(
        id,
        rows,
        cover = d.cover.asInstanceOf[Boolean],
        name = d.name.asInstanceOf[String],
        coverEmoji = d.emoji.asInstanceOf[String]
      )

    def booksToJson(books: Vector[Recipe]): String =
      js.JSON.stringify(js.Array(books.map(recipeToJs)*))

    def booksFromJson(json: String): Vector[Recipe] =
      Try {
        js.JSON.parse(json).asInstanceOf[js.Array[js.Dynamic]].toVector.map(recipeFromJs)
      }.getOrElse(Vector.empty)

    // A recipe as a URL-safe share payload: JSON → percent-encoding (keeps
    // btoa happy with emoji/diacritics) → base64url. Small enough that the
    // whole book travels in the link — no backend, works for "send grandma
    // a ready-to-print book".
    def recipeToShare(r: Recipe): String =
      val json = js.JSON.stringify(recipeToJs(r))
      dom.window.btoa(js.URIUtils.encodeURIComponent(json))
        .replace('+', '-').replace('/', '_').replace("=", "")

    def recipeFromShare(payload: String): Option[Recipe] =
      Try {
        val b64 = payload.replace('-', '+').replace('_', '/')
        val padded = b64 + "=" * ((4 - b64.length % 4) % 4)
        val json = js.URIUtils.decodeURIComponent(dom.window.atob(padded))
        recipeFromJs(js.JSON.parse(json).asInstanceOf[js.Dynamic])
      }.toOption
