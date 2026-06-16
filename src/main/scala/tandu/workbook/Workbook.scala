package tandu.workbook

import com.raquo.laminar.api.L.*
import org.scalajs.dom
import tandu.activities.{Battleships, DotToDot, GuideRobot, LetterTracing, MathPractice, Maze, SeekAndFind, Sudoku, WordBuilder, WordSearch}
import tandu.i18n.{Lang, Strings}

import scala.scalajs.js
import scala.util.{Random, Try}

/** The workbook model: a recipe is a list of (page source, count) rows plus
  * book-level options (cover page, kid's name). Re-rendering the same recipe
  * always yields *fresh* puzzles — the regeneration loop is the point: the
  * book never runs out.
  */
object Workbook:

  /** One row of the composition list: print `count` pages of `sourceId`. */
  final case class Row(sourceId: String, count: Int)

  /** `name` and `coverEmoji` are the book's identity — they label it on the
    * books list and, when the cover page is on, land on the cover too. */
  final case class Recipe(
      rows: Vector[Row],
      cover: Boolean,
      name: String,
      coverEmoji: String
  )

  /** Identity pictures offered alongside the name; one is drawn at random for
    * a fresh book. Kept modest in size on the printed cover — raster emoji
    * look bad rendered large. */
  val coverEmojis: Vector[String] =
    Vector("🦊", "🐰", "🦄", "🚀", "🐬", "🦖", "⭐", "🌈", "🐱", "⚽")

  def freshRecipe(rows: Vector[Row] = Vector.empty, name: String = ""): Recipe =
    Recipe(rows, cover = true, name = name, coverEmoji = coverEmojis(Random.nextInt(coverEmojis.size)))

  // ---------- page sources ----------

  /** Everything a render needs besides the source itself. `pageIndex` counts
    * within one row, so sequenced sources (letter tracing) can hand out page
    * 1, page 2, … of their material. */
  final case class Ctx(lang: Lang, rng: Random, pageIndex: Int)

  /** One concrete page variant within a kind: the variant-chip label, the
    * print-sheet title, and a generator producing one A4 page body. */
  final case class Source(
      id: String,
      variantLabel: Strings => String,
      printTitle: Strings => String,
      render: Ctx => HtmlElement
  )

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
    // The counting sheet rides along as a fourth "variant" — it's the same
    // board, scored by writing counts instead of circling.
    Group("seek-and-find", "👀", "vermilion", _.seekAndFind.name,
      SeekAndFind.printLevelIds.map { lvl =>
        Source(s"seek-and-find-$lvl", levelName(lvl), _.offline.seekAndFind.printTitle,
          ctx => SeekAndFind.printSeekBody(lvl, ctx.rng))
      } :+ Source("seek-and-find-count", _.offline.seekAndFind.countSheetName,
        _.offline.seekAndFind.countPrintTitle, ctx => SeekAndFind.printCountBody(ctx.rng))),
    Group("sudoku", "#", "plum", _.sudoku.name,
      Sudoku.variants.map { v =>
        Source(s"sudoku-${v.id}", v.nameKey, _.offline.sudoku.printTitle,
          _ => Sudoku.printSheetBody(v))
      }),
    Group("maze", "🐭", "peach", _.maze.name,
      Maze.variants.map { v =>
        Source(s"maze-${v.id}", v.nameKey, _.offline.maze.printTitle,
          ctx => Maze.printSheetBody(v, ctx.rng))
      }),
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

  private def levelName(lvl: String): Strings => String = lvl match
    case "easy"   => _.seekAndFind.easy
    case "medium" => _.seekAndFind.medium
    case _        => _.seekAndFind.hard

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

  val presets: List[Preset] = List(
    // Pre-readers: pictures, pens and counting; no word puzzles yet.
    Preset("age-4-5", _.workbook.age45, Vector(
      Row("letter-tracing-upper", 1),
      Row("dot-to-dot", 1),
      Row("seek-and-find-easy", 1),
      Row("seek-and-find-count", 1),
      Row("sudoku-emoji4", 1),
      Row("math-easy", 1),
      Row("maze-easy", 1)
    )),
    // Early readers: word builder and word search join in.
    Preset("age-6-7", _.workbook.age67, Vector(
      Row("dot-to-dot", 1),
      Row("letter-tracing-lower", 1),
      Row("seek-and-find-medium", 1),
      Row("sudoku-emoji6", 1),
      Row("word-builder-easy", 1),
      Row("word-search-easy", 1),
      Row("math-medium", 1),
      Row("maze-medium", 1),
      Row("guide-robot-easy", 1)
    )),
    // Confident readers: mid/hard everything.
    Preset("age-8", _.workbook.age8plus, Vector(
      Row("sudoku-medium", 1),
      Row("sudoku-hard", 1),
      Row("maze-hard", 1),
      Row("word-search-medium", 1),
      Row("word-search-hard", 1),
      Row("math-hard", 1),
      Row("word-builder-medium", 1),
      Row("seek-and-find-hard", 1),
      Row("guide-robot-medium", 1)
    )),
    // The whole book is understood as shared — game grids belong here.
    Preset("travel", _.workbook.travel, Vector(
      Row("tic-tac-toe-grids", 1),
      Row("maze-easy", 1),
      Row("sudoku-emoji6", 1),
      Row("dot-to-dot", 1),
      Row("seek-and-find-medium", 1),
      Row("word-search-easy", 1),
      Row("math-medium", 1)
    )),
    Preset("math-boost", _.workbook.mathBoost, Vector(
      Row("math-easy", 1),
      Row("math-medium", 1),
      Row("math-hard", 1),
      Row("seek-and-find-count", 1),
      Row("sudoku-emoji6", 1),
      Row("sudoku-easy", 1)
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
      js.Array(rows.map(r => js.Dynamic.literal(s = r.sourceId, c = r.count))*)

    private def recipeToJs(r: Recipe): js.Dynamic =
      js.Dynamic.literal(cover = r.cover, name = r.name, emoji = r.coverEmoji, rows = rowsToJs(r.rows))

    private def recipeFromJs(d: js.Dynamic): Recipe =
      val rows = d.rows.asInstanceOf[js.Array[js.Dynamic]].toVector
        .map(row => Row(row.s.asInstanceOf[String], row.c.asInstanceOf[Int]))
        .filter(row => byId(row.sourceId).isDefined && row.count > 0)
      Recipe(
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
