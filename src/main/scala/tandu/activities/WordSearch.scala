package tandu.activities

import com.raquo.laminar.api.L.*
import org.scalajs.dom
import tandu.{AppState, Page, Routing}
import tandu.i18n.{Lang, Strings}
import tandu.ui.{Components, Mode, ModeChooser, Printable, RulesCard}
import tandu.ui.Components.s
import tandu.ui.DomExt.*

import scala.collection.mutable
import scala.scalajs.js
import scala.util.Random

object WordSearch extends Activity:
  val id = "word-search"
  def name(s: Strings): String = s.wordSearch.name
  def description(s: Strings): String = s.wordSearch.description
  val minPlayers: Int = 1
  val maxPlayers: Int = 1
  val glyph: String = "🔎"
  val tint: String = "teal"

  // ---------- model ----------

  /** A search direction as a (row, col) unit step. The eight compass directions
    * are built from these; a variant exposes only the subset it allows. */
  final case class Dir(dr: Int, dc: Int)
  private val E  = Dir(0, 1)
  private val S  = Dir(1, 0)
  private val W  = Dir(0, -1)
  private val N  = Dir(-1, 0)
  private val SE = Dir(1, 1)
  private val SW = Dir(1, -1)
  private val NE = Dir(-1, 1)
  private val NW = Dir(-1, -1)

  /** A word as it sits in the grid, with the cells it occupies head-to-tail. */
  final case class Placement(word: String, cells: Vector[(Int, Int)])

  final case class Puzzle(size: Int, grid: Vector[Vector[Char]], placements: Vector[Placement]):
    def words: Vector[String] = placements.map(_.word)
    def letterAt(r: Int, c: Int): Char = grid(r)(c)

  // ---------- variants ----------

  final case class Variant(
      id: String,
      size: Int,
      wordCount: Int,
      maxLen: Int,
      dirs: Vector[Dir],
      perPage: Int,
      nameKey: Strings => String,
      descKey: Strings => String
  )

  val variants: List[Variant] = List(
    Variant("easy",   8,  6,  7,  Vector(E, S),                 4, _.wordSearch.easy.name,   _.wordSearch.easy.description),
    Variant("medium", 11, 8,  9,  Vector(E, S, SE, W, N),       4, _.wordSearch.medium.name, _.wordSearch.medium.description),
    Variant("hard",   13, 10, 11, Vector(E, S, W, N, SE, SW, NE, NW), 1, _.wordSearch.hard.name, _.wordSearch.hard.description)
  )

  // ---------- generator ----------

  /** Reduce a bank word to grid letters: drop spaces, hyphens and apostrophes
    * ("ice cream" → "ICECREAM", "arc-en-ciel" → "ARCENCIEL") and upper-case the
    * rest. Accented letters are kept as-is so the grid stays in the language's
    * own script. */
  def normalize(w: String): String = w.filter(_.isLetter).toUpperCase

  /** Build a puzzle for `v` from the current language's word bank. Words are
    * placed by randomized trial — overlapping shared letters is allowed — and
    * any cell left empty is filled with a letter sampled from the placed words,
    * so the filler stays in-script and never betrays the hidden words. */
  def generate(v: Variant, lang: Lang, rng: Random = new Random()): Puzzle =
    val grid = Array.fill(v.size, v.size)(' ')

    def fits(w: String, r0: Int, c0: Int, d: Dir): Option[Vector[(Int, Int)]] =
      val cells = (0 until w.length).map(i => (r0 + d.dr * i, c0 + d.dc * i)).toVector
      // Bounds first (short-circuits before the grid read), then a free or
      // matching letter — one pass over the cells.
      val ok = cells.zipWithIndex.forall { case ((r, c), i) =>
        r >= 0 && r < v.size && c >= 0 && c < v.size &&
          (grid(r)(c) == ' ' || grid(r)(c) == w.charAt(i))
      }
      Option.when(ok)(cells)

    def tryPlace(w: String): Option[Placement] =
      // Give every allowed direction a fair turn: shuffle the directions, then
      // try a handful of random start positions for each in order. Picking a
      // random direction per attempt instead would let across/down — which fit
      // far more easily — almost always win the first slot, so diagonals (and
      // backwards words) barely appeared even on the tiers that allow them.
      val dirs = rng.shuffle(v.dirs).iterator
      var placed: Option[Placement] = None
      while dirs.hasNext && placed.isEmpty do
        val d = dirs.next()
        var attempts = 0
        while attempts < 30 && placed.isEmpty do
          fits(w, rng.nextInt(v.size), rng.nextInt(v.size), d).foreach { cells =>
            cells.zipWithIndex.foreach { case ((r, c), i) => grid(r)(c) = w.charAt(i) }
            placed = Some(Placement(w, cells))
          }
          attempts += 1
      placed

    val pool = rng.shuffle(
      WordBank.forLang(lang).map(normalize).filter(w => w.length >= 3 && w.length <= v.maxLen).distinct
    )

    val placements = mutable.ArrayBuffer.empty[Placement]
    val it = pool.iterator
    while placements.size < v.wordCount && it.hasNext do
      tryPlace(it.next()).foreach(placements += _)

    val letters = placements.flatMap(_.word.toVector).toVector
    val fillPool = if letters.nonEmpty then letters else ('A' to 'Z').toVector
    for r <- 0 until v.size; c <- 0 until v.size do
      if grid(r)(c) == ' ' then grid(r)(c) = fillPool(rng.nextInt(fillPool.size))

    Puzzle(v.size, grid.iterator.map(_.toVector).toVector, placements.toVector)

  // ---------- play state ----------

  final case class Play(puzzle: Puzzle, found: Map[String, Vector[(Int, Int)]], selection: Vector[(Int, Int)]):
    def won: Boolean = found.size == puzzle.placements.size
    def foundCells: Set[(Int, Int)] = found.values.flatten.toSet

  private def newPlay(v: Variant, lang: Lang): Play =
    Play(generate(v, lang), Map.empty, Vector.empty)

  /** The straight run of cells from `a` to `b`. A sloppy drag that isn't exactly
    * horizontal, vertical or 45°-diagonal snaps to the dominant axis, so tracing
    * a word never demands pixel-perfect aim. */
  private def lineCells(a: (Int, Int), b: (Int, Int)): Vector[(Int, Int)] =
    val (r0, c0) = a
    val (r1, c1) = b
    val dr = r1 - r0
    val dc = c1 - c0
    val (sr, sc, len) =
      if dr == 0 && dc == 0 then (0, 0, 0)
      else if dr == 0 then (0, dc.sign, dc.abs)
      else if dc == 0 then (dr.sign, 0, dr.abs)
      else if dr.abs == dc.abs then (dr.sign, dc.sign, dr.abs)
      else if dr.abs > dc.abs then (dr.sign, 0, dr.abs)
      else (0, dc.sign, dc.abs)
    (0 to len).map(i => (r0 + sr * i, c0 + sc * i)).toVector

  /** Resolve a selection against the still-hidden words, accepting it traced in
    * either direction. Returns the play with the word marked found, or unchanged. */
  private def commit(p: Play): Play =
    if p.selection.length < 2 then p
    else
      val str = p.selection.map((r, c) => p.puzzle.letterAt(r, c)).mkString
      val rev = str.reverse
      p.puzzle.placements.find(pl => !p.found.contains(pl.word) && (pl.word == str || pl.word == rev)) match
        case Some(pl) => p.copy(found = p.found + (pl.word -> p.selection))
        case None     => p

  // ---------- UI ----------

  def render(): HtmlElement =
    ModeChooser.render(id, List(
      Mode(
        id = "in-app",
        label = _.mode.inApp,
        render = () => renderPlay()
      ),
      Mode(
        id = "print",
        label = _.mode.offline,
        materials = List(_.offline.materials.printer, _.offline.materials.paperPen),
        hint = Some(_.offline.wordSearch.sheetHint),
        render = () => renderOffline()
      )
    ))

  // Easy is the implicit default when the URL has no variant segment.
  private val DefaultVariant = variants(0)
  private val variantSignal: Signal[Variant] =
    Routing.router.currentPageSignal.map {
      case Page.Activity(`id`, _ :: vId :: _) =>
        variants.find(_.id == vId).getOrElse(DefaultVariant)
      case _ => DefaultVariant
    }.distinct

  private def renderPlay(): HtmlElement =
    div(
      cls := "word-search stack-lg",
      variantPill(),
      p(cls := "muted center no-print", child.text <-- s(_.wordSearch.instruction)),
      // Rebuild on either a variant change or a language switch — a fresh grid
      // is drawn from the new language's word bank.
      child <-- variantSignal.combineWith(AppState.lang.signal).map((v, lang) => playForVariant(v, lang))
    )

  private def playForVariant(v: Variant, lang: Lang): HtmlElement =
    val state = Var(newPlay(v, lang))

    // The drag in progress: a plain closure var holds its anchor cell; only the
    // selection (in `state`) drives rendering.
    var dragging = false
    var dragRect: dom.DOMRect = null
    var anchor: (Int, Int) = (0, 0)

    def cellAt(rect: dom.DOMRect, clientX: Double, clientY: Double): (Int, Int) =
      val fx = (clientX - rect.left) / rect.width
      val fy = (clientY - rect.top) / rect.height
      val c = math.max(0, math.min(v.size - 1, (fx * v.size).toInt))
      val r = math.max(0, math.min(v.size - 1, (fy * v.size).toInt))
      (r, c)

    def newGame(): Unit = state.set(newPlay(v, lang))

    // Sets, so the 169 per-cell membership tests below are O(1) and only refire
    // when the set actually changes.
    val foundCellsSig  = state.signal.map(_.foundCells).distinct
    val selectionSig   = state.signal.map(_.selection.toSet).distinct

    div(
      cls := "stack-lg",
      div(
        cls := "word-search-grid",
        styleAttr := s"--ws-size: ${v.size}",
        onPointerDown --> { ev =>
          if !state.now().won then
            dragging = true
            val el = ev.currentTarget.asInstanceOf[dom.Element]
            el.setPointerCapture(ev.pointerId)
            dragRect = el.getBoundingClientRect()
            anchor = cellAt(dragRect, ev.clientX, ev.clientY)
            state.update(_.copy(selection = Vector(anchor)))
        },
        onPointerMove --> { ev =>
          if dragging then
            // Pointermove fires ~60×/s; skip the state churn until the traced
            // line actually changes (it only does when the cursor crosses a
            // cell boundary).
            val next = lineCells(anchor, cellAt(dragRect, ev.clientX, ev.clientY))
            if next != state.now().selection then state.update(_.copy(selection = next))
        },
        onPointerUp --> { _ =>
          if dragging then
            dragging = false
            state.update(p => commit(p).copy(selection = Vector.empty))
        },
        onPointerCancel --> { _ =>
          dragging = false
          state.update(_.copy(selection = Vector.empty))
        },
        (0 until v.size).flatMap { r =>
          (0 until v.size).map { c =>
            div(
              cls := "word-search-cell",
              cls("is-selected") <-- selectionSig.map(_.contains((r, c))),
              cls("is-found")    <-- foundCellsSig.map(_.contains((r, c))),
              // Bound to the signal (not a captured letter) so "New puzzle"
              // redraws the grid in place; `distinct` keeps a selection change
              // from rewriting every cell's unchanged text.
              child.text <-- state.signal.map(_.puzzle.letterAt(r, c).toString).distinct
            )
          }
        }
      ),
      wordList(state.signal),
      child <-- state.signal.map(_.won).distinct.map {
        case true  => wonView(newGame)
        case false => controls(newGame)
      }
    )

  private def wordList(stateSig: Signal[Play]): HtmlElement =
    // The set of solved words; the only thing that changes during play. The word
    // list itself is rebuilt only when the puzzle changes (words signal), and
    // each item flips its own `is-found` class — no per-move DOM teardown.
    val foundSig = stateSig.map(_.found.keySet).distinct
    div(
      cls := "word-search-words",
      h3(
        cls := "word-search-words__title",
        child.text <-- stateSig.combineWith(AppState.strings).map { (p, str) =>
          s"${str.wordSearch.foundLabel}: ${p.found.size}/${p.puzzle.placements.size}"
        }.distinct
      ),
      ul(
        cls := "word-search-list",
        children <-- stateSig.map(_.puzzle.words).distinct.map { words =>
          words.map { w =>
            li(
              cls := "word-search-list__item",
              cls("is-found") <-- foundSig.map(_.contains(w)),
              w
            )
          }
        }
      )
    )

  private def controls(newGame: () => Unit): HtmlElement =
    div(
      cls := "row word-search-controls",
      styleAttr := "justify-content: center; flex-wrap: wrap;",
      Components.ghost(s(_.wordSearch.newGame), newGame())
    )

  private def wonView(newGame: () => Unit): HtmlElement =
    div(
      cls := "handoff card",
      div(cls := "handoff__title", child.text <-- s(_.wordSearch.won)),
      button(
        cls := "btn btn--lg",
        child.text <-- s(_.wordSearch.newGame),
        onClick --> (_ => newGame())
      )
    )

  private def variantPill(): HtmlElement =
    div(
      cls := "center no-print",
      div(
        cls := "pill-toggle",
        variants.map { v =>
          button(
            cls := "pill-btn",
            cls("is-active") <-- variantSignal.map(_.id == v.id),
            child.text <-- AppState.strings.map(v.nameKey),
            onClick --> (_ => Routing.go(Page.Activity(id, List("in-app", v.id))))
          )
        }
      )
    )

  // ---------- print ----------

  /** One printed page's worth of puzzles (the variant's usual per-page batch),
    * as a bare body for composed documents like workbooks. */
  def printSheetBody(v: Variant, lang: Lang, rng: Random = new Random()): HtmlElement =
    div(
      cls := "word-search-print-sheet",
      cls("word-search-print-sheet--single") := v.perPage == 1,
      List.fill(v.perPage)(printablePuzzle(v, generate(v, lang, rng)))
    )

  private def renderOffline(): HtmlElement =
    val puzzles: Var[List[(Variant, Puzzle)]] = Var(Nil)

    def printBatch(v: Variant): Unit =
      val lang = AppState.lang.now()
      puzzles.set(List.fill(v.perPage)(v -> generate(v, lang)))
      val _ = js.timers.setTimeout(50)(Printable.print())

    div(
      cls := "stack-lg",
      div(
        cls := "no-print",
        RulesCard.render(List(RulesCard.fromRules(_.offline.wordSearch.rules)))
      ),
      div(
        cls := "no-print stack-lg word-search-print-actions",
        variants.map { v =>
          button(
            cls := "btn btn--lg btn--block",
            child.text <-- AppState.strings.map(str => s"${str.printable.print} — ${v.nameKey(str)}"),
            onClick --> (_ => printBatch(v))
          )
        }
      ),
      div(
        cls := "print-only",
        Printable.render(
          title = _.offline.wordSearch.printTitle,
          body = div(
            cls := "word-search-print-sheet",
            // Hard prints one big board per page; the rest tile two-up.
            cls("word-search-print-sheet--single") <-- puzzles.signal.map(_.headOption.exists(_._1.perPage == 1)),
            children <-- puzzles.signal.map(_.map((v, p) => printablePuzzle(v, p)))
          )
        )
      )
    )

  private def printablePuzzle(v: Variant, p: Puzzle): HtmlElement =
    div(
      cls := s"word-search-print-board word-search-print-board--${v.id}",
      div(
        cls := "word-search-print-grid",
        styleAttr := s"--ws-size: ${v.size}",
        (0 until v.size).flatMap { r =>
          (0 until v.size).map { c =>
            div(cls := "word-search-print-cell", p.letterAt(r, c).toString)
          }
        }
      ),
      div(
        cls := "word-search-print-words",
        h4(
          cls := "word-search-print-words__title",
          child.text <-- AppState.strings.map(_.offline.wordSearch.wordsLabel)
        ),
        ul(
          cls := "word-search-print-list",
          p.words.map(w => li(w))
        )
      )
    )
