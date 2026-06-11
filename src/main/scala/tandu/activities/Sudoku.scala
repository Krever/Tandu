package tandu.activities

import com.raquo.laminar.api.L.*
import tandu.{AppState, Page, Routing}
import tandu.i18n.Strings
import tandu.ui.{Components, Mode, ModeChooser, Printable, RulesCard}
import tandu.ui.Components.s

import scala.scalajs.js
import scala.util.Random

object Sudoku extends Activity:
  val id = "sudoku"
  def name(s: Strings): String = s.sudoku.name
  def description(s: Strings): String = s.sudoku.description
  val minPlayers: Int = 1
  val maxPlayers: Int = 1
  val glyph: String = "#"
  val tint: String = "plum"

  // ---------- model ----------

  /** 0 represents an empty cell. */
  type Grid = Vector[Vector[Int]]

  final case class Cell(value: Int, fixed: Boolean, pencil: Set[Int]):
    def isEmpty: Boolean = value == 0

  type Board = Vector[Vector[Cell]]

  final case class State(
      board: Board,
      solution: Grid,
      symbols: Vector[String],
      selected: Option[(Int, Int)],
      pencilMode: Boolean,
      history: List[Board]
  ):
    def isWon: Boolean =
      board.zipWithIndex.forall { (row, r) =>
        row.zipWithIndex.forall { (c, ci) => c.value == solution(r)(ci) }
      }

  final case class Variant(
      id: String,
      size: Int,
      boxRows: Int,
      boxCols: Int,
      clues: Int,
      emoji: Boolean,
      nameKey: Strings => String,
      descKey: Strings => String
  )

  val variants: List[Variant] = List(
    Variant("emoji4", 4, 2, 2, 8,  emoji = true,  _.sudoku.emoji4.name, _.sudoku.emoji4.description),
    Variant("emoji6", 6, 2, 3, 18, emoji = true,  _.sudoku.emoji6.name, _.sudoku.emoji6.description),
    Variant("easy",   9, 3, 3, 40, emoji = false, _.sudoku.easy.name,   _.sudoku.easy.description),
    Variant("medium", 9, 3, 3, 32, emoji = false, _.sudoku.medium.name, _.sudoku.medium.description),
    Variant("hard",   9, 3, 3, 26, emoji = false, _.sudoku.hard.name,   _.sudoku.hard.description)
  )

  /** Symbol sets for the kid-friendly emoji variants; one is drawn per game. */
  private val emojiThemes: Vector[Vector[String]] = Vector(
    Vector("🐶", "🐱", "🐰", "🦊", "🐻", "🐼"),
    Vector("🍎", "🍌", "🍇", "🍓", "🍊", "🥝"),
    Vector("🚗", "🚌", "🚒", "🚜", "🚓", "🚲"),
    Vector("🐟", "🐙", "🦀", "🐬", "🐢", "🦈"),
    Vector("🌞", "🌙", "⭐", "🌈", "🍄", "🌸")
  )

  // Every theme must cover the largest emoji variant.
  assert(emojiThemes.forall(_.length >= variants.filter(_.emoji).map(_.size).max))

  private def symbolsFor(v: Variant, rng: Random): Vector[String] =
    if v.emoji then emojiThemes(rng.nextInt(emojiThemes.size)).take(v.size)
    else (1 to v.size).map(_.toString).toVector

  // ---------- generator ----------

  private def boxOf(v: Variant, r: Int, c: Int): Int =
    (r / v.boxRows) * (v.size / v.boxCols) + (c / v.boxCols)

  private def isSafe(v: Variant, grid: Array[Array[Int]], r: Int, c: Int, n: Int): Boolean =
    var i = 0
    while i < v.size do
      if grid(r)(i) == n then return false
      if grid(i)(c) == n then return false
      i += 1
    val br = (r / v.boxRows) * v.boxRows
    val bc = (c / v.boxCols) * v.boxCols
    var rr = br
    while rr < br + v.boxRows do
      var cc = bc
      while cc < bc + v.boxCols do
        if grid(rr)(cc) == n then return false
        cc += 1
      rr += 1
    true

  /** Fill in a complete random solution by randomized backtracking. */
  private def fillSolution(v: Variant, rng: Random): Array[Array[Int]] =
    val grid = Array.fill(v.size, v.size)(0)
    def go(idx: Int): Boolean =
      if idx == v.size * v.size then true
      else
        val r = idx / v.size
        val c = idx % v.size
        val candidates = rng.shuffle((1 to v.size).toList)
        candidates.exists { n =>
          if isSafe(v, grid, r, c, n) then
            grid(r)(c) = n
            if go(idx + 1) then true
            else { grid(r)(c) = 0; false }
          else false
        }
    val _ = go(0)
    grid

  /** Count solutions of `grid` up to `cap` (used to verify uniqueness). */
  private def countSolutions(v: Variant, grid: Array[Array[Int]], cap: Int): Int =
    var count = 0
    def go(): Boolean =
      // Find first empty
      var r = -1; var c = -1
      var rr = 0
      var found = false
      while rr < v.size && !found do
        var cc = 0
        while cc < v.size && !found do
          if grid(rr)(cc) == 0 then { r = rr; c = cc; found = true }
          cc += 1
        rr += 1
      if !found then
        count += 1
        return count >= cap
      var n = 1
      while n <= v.size do
        if isSafe(v, grid, r, c, n) then
          grid(r)(c) = n
          if go() then { grid(r)(c) = 0; return true }
          grid(r)(c) = 0
        n += 1
      false
    val _ = go()
    count

  /** Produce a (puzzle, solution) pair where the puzzle has roughly
    * `v.clues` filled cells and a unique solution. */
  def generate(v: Variant, rng: Random = new Random()): (Grid, Grid) =
    val solved = fillSolution(v, rng)
    val puzzle = solved.map(_.clone)
    val positions = rng.shuffle((0 until v.size * v.size).toList)
    var clues = v.size * v.size
    for pos <- positions if clues > v.clues do
      val r = pos / v.size
      val c = pos % v.size
      val saved = puzzle(r)(c)
      puzzle(r)(c) = 0
      val n = countSolutions(v, puzzle, 2)
      if n != 1 then
        puzzle(r)(c) = saved
      else
        clues -= 1
    val toVec = (a: Array[Array[Int]]) =>
      a.iterator.map(row => row.toVector).toVector
    (toVec(puzzle), toVec(solved))

  private def newState(v: Variant): State =
    val rng = new Random()
    val (puzzle, solution) = generate(v, rng)
    val board = puzzle.map(_.map(n => Cell(value = n, fixed = n != 0, pencil = Set.empty)))
    State(
      board = board,
      solution = solution,
      symbols = symbolsFor(v, rng),
      selected = None,
      pencilMode = false,
      history = Nil
    )

  /** Does any *other* cell in (r, c)'s row, column or box hold `n`? */
  private def peerHas(v: Variant, board: Board, r: Int, c: Int, n: Int): Boolean =
    val br = (r / v.boxRows) * v.boxRows
    val bc = (c / v.boxCols) * v.boxCols
    (0 until v.size).exists(i => i != c && board(r)(i).value == n) ||
    (0 until v.size).exists(i => i != r && board(i)(c).value == n) ||
    (br until br + v.boxRows).exists(rr =>
      (bc until bc + v.boxCols).exists(cc => (rr, cc) != (r, c) && board(rr)(cc).value == n)
    )

  private def conflictsAt(v: Variant, board: Board, r: Int, c: Int): Boolean =
    val value = board(r)(c).value
    value != 0 && peerHas(v, board, r, c, value)

  /** Compute all coordinates that participate in any conflict on the current board.
    * Doing this once per board change avoids re-scanning rows/cols/boxes for every cell on every state tick. */
  private def conflictSet(v: Variant, board: Board): Set[(Int, Int)] =
    val out = scala.collection.mutable.Set.empty[(Int, Int)]
    var r = 0
    while r < v.size do
      var c = 0
      while c < v.size do
        if conflictsAt(v, board, r, c) then out += ((r, c))
        c += 1
      r += 1
    out.toSet

  // ---------- UI ----------

  private def gridTemplate(size: Int): String =
    s"grid-template-columns: repeat($size, 1fr); grid-template-rows: repeat($size, 1fr);"

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
        hint = Some(_.offline.sudoku.sheetHint),
        render = () => renderOffline()
      )
    ))

  private final case class PrintPuzzle(variant: Variant, grid: Grid, symbols: Vector[String])

  private def renderOffline(): HtmlElement =
    val puzzles: Var[List[PrintPuzzle]] = Var(Nil)

    def printBatch(v: Variant): Unit =
      puzzles.set(List.fill(6) {
        val rng = new Random()
        PrintPuzzle(v, generate(v, rng)._1, symbolsFor(v, rng))
      })
      val _ = js.timers.setTimeout(50)(Printable.print())

    div(
      cls := "stack-lg",
      div(
        cls := "no-print",
        RulesCard.render(List(RulesCard.fromRules(_.offline.sudoku.rules)))
      ),
      div(
        cls := "no-print stack-lg sudoku-print-actions",
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
          title = _.offline.sudoku.printTitle,
          body = div(
            cls := "sudoku-print-sheet",
            children <-- puzzles.signal.map(_.map(printablePuzzle))
          )
        )
      )
    )

  private def printablePuzzle(p: PrintPuzzle): HtmlElement =
    val v = p.variant
    div(
      cls := "sudoku-print-puzzle",
      when(v.emoji)(div(cls := "sudoku-print-legend", p.symbols.mkString(" "))),
      div(
        cls := "sudoku-print-board",
        cls(s"sudoku-print-board--${v.size}") := v.size != 9,
        styleAttr := gridTemplate(v.size),
        (0 until v.size).flatMap { r =>
          (0 until v.size).map { c =>
            val value = p.grid(r)(c)
            div(
              cls := "sudoku-print-cell",
              cls("sudoku-print-cell--boxl") := c > 0 && c % v.boxCols == 0,
              cls("sudoku-print-cell--boxt") := r > 0 && r % v.boxRows == 0,
              if value == 0 then "" else p.symbols(value - 1)
            )
          }
        }
      )
    )

  // The "medium" variant is the implicit default when the URL has no variant
  // segment after the in-app mode.
  private val DefaultVariant = variants.find(_.id == "medium").get
  // Lazy so that headless tests can use the generator without a DOM/router.
  private lazy val variantSignal: Signal[Variant] =
    Routing.router.currentPageSignal.map {
      case Page.Activity(`id`, _ :: vId :: _) =>
        variants.find(_.id == vId).getOrElse(DefaultVariant)
      case _ => DefaultVariant
    }.distinct

  private def renderPlay(): HtmlElement =
    div(
      cls := "sudoku stack-lg",
      variantPill(),
      child <-- variantSignal.map(playForVariant)
    )

  private def playForVariant(v: Variant): HtmlElement =
    val state = Var(newState(v))
    // Opt-in solving aid: grey out pad symbols already present in the selected
    // cell's row/column/box. Off by default — with it on the game gets easy.
    val assist = Var(false)

    def pushHistory(): Unit =
      state.update(st => st.copy(history = st.board :: st.history.take(99)))

    def selectCell(r: Int, c: Int): Unit =
      state.update(st => st.copy(selected = Some((r, c))))

    def enterNumber(n: Int): Unit =
      val st = state.now()
      st.selected match
        case None => ()
        case Some((r, c)) =>
          val cell = st.board(r)(c)
          if cell.fixed then ()
          else
            pushHistory()
            val updated =
              if st.pencilMode then
                if n == 0 then cell.copy(pencil = Set.empty)
                else if cell.pencil.contains(n) then cell.copy(pencil = cell.pencil - n)
                else cell.copy(value = 0, pencil = cell.pencil + n)
              else
                if cell.value == n then cell.copy(value = 0)
                else cell.copy(value = n, pencil = Set.empty)
            val newBoard = st.board.updated(r, st.board(r).updated(c, updated))
            state.update(_.copy(board = newBoard))

    def undo(): Unit =
      val st = state.now()
      st.history match
        case prev :: rest => state.set(st.copy(board = prev, history = rest))
        case Nil => ()

    def togglePencil(): Unit =
      state.update(st => st.copy(pencilMode = !st.pencilMode))

    def reset(): Unit = state.set(newState(v))

    div(
      cls := "stack-lg",
      child <-- state.signal.map(_.isWon).distinct.map {
        case true  => wonView(reset)
        case false => playView(v, state, assist.signal, selectCell, enterNumber)
      },
      div(
        cls := "row sudoku-controls",
        styleAttr := "justify-content: center; flex-wrap: wrap;",
        // Pencil marks are a grown-up solving aid; the emoji variants stay tap-only.
        when(!v.emoji)(
          Components.ghost(
            s(_.sudoku.pencil),
            togglePencil(),
            isDisabled = state.signal.map(_.isWon)
          )
        ),
        button(
          cls := "btn btn--ghost sudoku-assist",
          cls("is-active") <-- assist.signal,
          child.text <-- s(_.sudoku.assist),
          disabled <-- state.signal.map(_.isWon),
          onClick --> (_ => assist.update(!_))
        ),
        Components.ghost(
          s(_.sudoku.undo),
          undo(),
          isDisabled = state.signal.map(st => st.history.isEmpty || st.isWon)
        ),
        Components.ghost(s(_.sudoku.newGame), reset())
      )
    )

  private def variantPill(): HtmlElement =
    div(
      cls := "center no-print",
      div(
        cls := "pill-toggle sudoku-variants",
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

  private def wonView(newGame: () => Unit): HtmlElement =
    div(
      cls := "handoff card",
      div(cls := "handoff__title", child.text <-- s(_.common.youWin)),
      button(
        cls := "btn btn--lg",
        child.text <-- s(_.sudoku.newGame),
        onClick --> (_ => newGame())
      )
    )

  private def playView(
      v: Variant,
      state: Var[State],
      assist: Signal[Boolean],
      onSelect: (Int, Int) => Unit,
      onEnter: Int => Unit
  ): HtmlElement =
    div(
      cls := "stack-lg",
      boardView(v, state.signal, onSelect),
      numberPad(v, state.signal, assist, onEnter)
    )

  private def boardView(
      v: Variant,
      stateSig: Signal[State],
      onSelect: (Int, Int) => Unit
  ): HtmlElement =
    val selSig = stateSig.map(_.selected).distinct
    val selValSig = stateSig.map { st =>
      st.selected.map((sr, sc) => st.board(sr)(sc).value).filter(_ != 0)
    }.distinct
    val symbolsSig = stateSig.map(_.symbols).distinct
    val conflictsSig = stateSig.map(_.board).distinct.map(conflictSet(v, _))
    div(
      cls := "sudoku-board",
      cls(s"sudoku-board--${v.size}") := v.size != 9,
      styleAttr := gridTemplate(v.size),
      (0 until v.size).flatMap { r =>
        (0 until v.size).map { c =>
          val cellSig = stateSig.map(_.board(r)(c)).distinct
          val isSelected = selSig.map(_.contains((r, c)))
          val sameUnit = selSig.map {
            case Some((sr, sc)) => sr == r || sc == c || boxOf(v, sr, sc) == boxOf(v, r, c)
            case None           => false
          }
          val sameValue = selValSig.combineWith(cellSig).map { (sv, cell) =>
            sv.contains(cell.value) && cell.value != 0
          }
          val conflict = conflictsSig.map(_.contains((r, c)))
          div(
            cls := "sudoku-cell",
            cls("sudoku-cell--boxl") := c > 0 && c % v.boxCols == 0,
            cls("sudoku-cell--boxt") := r > 0 && r % v.boxRows == 0,
            cls("is-selected") <-- isSelected,
            cls("is-peer")      <-- sameUnit,
            cls("is-same")      <-- sameValue,
            cls("is-fixed")     <-- cellSig.map(_.fixed),
            cls("is-conflict")  <-- conflict,
            onClick --> (_ => onSelect(r, c)),
            child <-- cellSig.combineWith(symbolsSig).map { (cell, symbols) =>
              if cell.value != 0 then div(cls := "sudoku-cell__value", symbols(cell.value - 1))
              else if cell.pencil.nonEmpty then
                div(
                  cls := "sudoku-pencil",
                  (1 to v.size).map { n =>
                    div(
                      cls := "sudoku-pencil__digit",
                      if cell.pencil.contains(n) then symbols(n - 1) else ""
                    )
                  }
                )
              else emptyNode
            }
          )
        }
      }
    )

  private def numberPad(
      v: Variant,
      stateSig: Signal[State],
      assist: Signal[Boolean],
      onEnter: Int => Unit
  ): HtmlElement =
    val symbolsSig = stateSig.map(_.symbols).distinct
    // The set of pad entries disabled for the selected cell (0 = erase), scanned
    // once per state tick rather than per button (same idea as conflictSet).
    // A given clue can't be changed at all. On other cells — only with the
    // opt-in helper on — entering n is blocked when it would duplicate n in the
    // cell's row/column/box; taps that merely clear n from the cell (value or
    // pencil mark) stay enabled.
    val blockedSig: Signal[Set[Int]] = stateSig.combineWith(assist).map { (st, assistOn) =>
      st.selected.fold(Set.empty[Int]) { (r, c) =>
        val cell = st.board(r)(c)
        if cell.fixed then (0 to v.size).toSet
        else if !assistOn then Set.empty
        else
          (1 to v.size).filter { n =>
            val togglesOff =
              if st.pencilMode then cell.pencil.contains(n) else cell.value == n
            !togglesOff && peerHas(v, st.board, r, c, n)
          }.toSet
      }
    }.distinct
    div(
      cls := "sudoku-pad",
      cls("sudoku-pad--emoji") := v.emoji,
      when(v.size != 9)(styleAttr := s"grid-template-columns: repeat(${v.size + 1}, 1fr);"),
      (1 to v.size).map { n =>
        button(
          cls := "sudoku-pad__btn",
          cls("is-pencil") <-- stateSig.map(_.pencilMode),
          disabled <-- blockedSig.map(_.contains(n)).distinct,
          child.text <-- symbolsSig.map(_(n - 1)),
          onClick --> (_ => onEnter(n))
        )
      },
      button(
        cls := "sudoku-pad__btn sudoku-pad__btn--erase",
        "⌫",
        disabled <-- blockedSig.map(_.contains(0)).distinct,
        onClick --> (_ => onEnter(0))
      )
    )
