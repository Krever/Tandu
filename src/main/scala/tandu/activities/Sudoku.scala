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
  val handsFree: Boolean = false

  // ---------- model ----------

  /** 0 represents an empty cell. */
  type Grid = Vector[Vector[Int]]

  final case class Cell(value: Int, fixed: Boolean, pencil: Set[Int]):
    def isEmpty: Boolean = value == 0

  type Board = Vector[Vector[Cell]]

  final case class State(
      board: Board,
      solution: Grid,
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
      clues: Int,
      nameKey: Strings => String,
      descKey: Strings => String
  )

  val variants: List[Variant] = List(
    Variant("easy",   40, _.sudoku.easy.name,   _.sudoku.easy.description),
    Variant("medium", 32, _.sudoku.medium.name, _.sudoku.medium.description),
    Variant("hard",   26, _.sudoku.hard.name,   _.sudoku.hard.description)
  )

  // ---------- generator ----------

  private def boxOf(r: Int, c: Int): Int = (r / 3) * 3 + (c / 3)

  private def isSafe(grid: Array[Array[Int]], r: Int, c: Int, v: Int): Boolean =
    var i = 0
    while i < 9 do
      if grid(r)(i) == v then return false
      if grid(i)(c) == v then return false
      i += 1
    val br = (r / 3) * 3
    val bc = (c / 3) * 3
    var rr = br
    while rr < br + 3 do
      var cc = bc
      while cc < bc + 3 do
        if grid(rr)(cc) == v then return false
        cc += 1
      rr += 1
    true

  /** Fill in a complete random solution by randomized backtracking. */
  private def fillSolution(rng: Random): Array[Array[Int]] =
    val grid = Array.fill(9, 9)(0)
    def go(idx: Int): Boolean =
      if idx == 81 then true
      else
        val r = idx / 9
        val c = idx % 9
        val candidates = rng.shuffle((1 to 9).toList)
        candidates.exists { v =>
          if isSafe(grid, r, c, v) then
            grid(r)(c) = v
            if go(idx + 1) then true
            else { grid(r)(c) = 0; false }
          else false
        }
    val _ = go(0)
    grid

  /** Count solutions of `grid` up to `cap` (used to verify uniqueness). */
  private def countSolutions(grid: Array[Array[Int]], cap: Int): Int =
    var count = 0
    def go(): Boolean =
      // Find first empty
      var r = -1; var c = -1
      var rr = 0
      var found = false
      while rr < 9 && !found do
        var cc = 0
        while cc < 9 && !found do
          if grid(rr)(cc) == 0 then { r = rr; c = cc; found = true }
          cc += 1
        rr += 1
      if !found then
        count += 1
        return count >= cap
      var v = 1
      while v <= 9 do
        if isSafe(grid, r, c, v) then
          grid(r)(c) = v
          if go() then { grid(r)(c) = 0; return true }
          grid(r)(c) = 0
        v += 1
      false
    val _ = go()
    count

  /** Produce a (puzzle, solution) pair where the puzzle has roughly
    * `targetClues` filled cells and a unique solution. */
  def generate(targetClues: Int, rng: Random = new Random()): (Grid, Grid) =
    val solved = fillSolution(rng)
    val puzzle = solved.map(_.clone)
    val positions = rng.shuffle((0 until 81).toList)
    var clues = 81
    for pos <- positions if clues > targetClues do
      val r = pos / 9
      val c = pos % 9
      val saved = puzzle(r)(c)
      puzzle(r)(c) = 0
      val n = countSolutions(puzzle, 2)
      if n != 1 then
        puzzle(r)(c) = saved
      else
        clues -= 1
    val toVec = (a: Array[Array[Int]]) =>
      a.iterator.map(row => row.toVector).toVector
    (toVec(puzzle), toVec(solved))

  private def newState(v: Variant): State =
    val (puzzle, solution) = generate(v.clues)
    val board = puzzle.map(_.map(n => Cell(value = n, fixed = n != 0, pencil = Set.empty)))
    State(board = board, solution = solution, selected = None, pencilMode = false, history = Nil)

  private def conflictsAt(board: Board, r: Int, c: Int): Boolean =
    val v = board(r)(c).value
    if v == 0 then false
    else
      val br = (r / 3) * 3
      val bc = (c / 3) * 3
      (0 until 9).exists(i => i != c && board(r)(i).value == v) ||
      (0 until 9).exists(i => i != r && board(i)(c).value == v) ||
      (br until br + 3).exists(rr =>
        (bc until bc + 3).exists(cc => (rr, cc) != (r, c) && board(rr)(cc).value == v)
      )

  /** Compute all coordinates that participate in any conflict on the current board.
    * Doing this once per board change avoids re-scanning rows/cols/boxes for every cell on every state tick. */
  private def conflictSet(board: Board): Set[(Int, Int)] =
    val out = scala.collection.mutable.Set.empty[(Int, Int)]
    var r = 0
    while r < 9 do
      var c = 0
      while c < 9 do
        if conflictsAt(board, r, c) then out += ((r, c))
        c += 1
      r += 1
    out.toSet

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
        hint = Some(_.offline.sudoku.sheetHint),
        render = () => renderOffline()
      )
    ))

  private def renderOffline(): HtmlElement =
    val puzzles: Var[List[Grid]] = Var(Nil)

    def printBatch(v: Variant): Unit =
      puzzles.set(List.fill(6)(generate(v.clues)._1))
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

  private def printablePuzzle(grid: Grid): HtmlElement =
    div(
      cls := "sudoku-print-board",
      (0 until 9).flatMap { r =>
        (0 until 9).map { c =>
          val v = grid(r)(c)
          div(
            cls := "sudoku-print-cell",
            cls(s"sudoku-print-cell--r$r") := true,
            cls(s"sudoku-print-cell--c$c") := true,
            if v == 0 then "" else v.toString
          )
        }
      }
    )

  // Second variant ("medium") is the implicit default when the URL has no
  // variant segment after the in-app mode.
  private val DefaultVariant = variants(1)
  private val variantSignal: Signal[Variant] =
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
        case false => playView(state, selectCell, enterNumber)
      },
      div(
        cls := "row sudoku-controls",
        styleAttr := "justify-content: center; flex-wrap: wrap;",
        Components.ghost(
          s(_.sudoku.pencil),
          togglePencil(),
          isDisabled = state.signal.map(_.isWon)
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
      state: Var[State],
      onSelect: (Int, Int) => Unit,
      onEnter: Int => Unit
  ): HtmlElement =
    div(
      cls := "stack-lg",
      boardView(state.signal, onSelect),
      numberPad(state.signal, onEnter)
    )

  private def boardView(
      stateSig: Signal[State],
      onSelect: (Int, Int) => Unit
  ): HtmlElement =
    val selSig = stateSig.map(_.selected).distinct
    val selValSig = stateSig.map { st =>
      st.selected.map((sr, sc) => st.board(sr)(sc).value).filter(_ != 0)
    }.distinct
    val conflictsSig = stateSig.map(_.board).distinct.map(conflictSet)
    div(
      cls := "sudoku-board",
      (0 until 9).flatMap { r =>
        (0 until 9).map { c =>
          val cellSig = stateSig.map(_.board(r)(c)).distinct
          val isSelected = selSig.map(_.contains((r, c)))
          val sameUnit = selSig.map {
            case Some((sr, sc)) => sr == r || sc == c || boxOf(sr, sc) == boxOf(r, c)
            case None           => false
          }
          val sameValue = selValSig.combineWith(cellSig).map { (sv, cell) =>
            sv.contains(cell.value) && cell.value != 0
          }
          val conflict = conflictsSig.map(_.contains((r, c)))
          div(
            cls := "sudoku-cell",
            cls(s"sudoku-cell--r$r") := true,
            cls(s"sudoku-cell--c$c") := true,
            cls("is-selected") <-- isSelected,
            cls("is-peer")      <-- sameUnit,
            cls("is-same")      <-- sameValue,
            cls("is-fixed")     <-- cellSig.map(_.fixed),
            cls("is-conflict")  <-- conflict,
            onClick --> (_ => onSelect(r, c)),
            child <-- cellSig.map { cell =>
              if cell.value != 0 then div(cls := "sudoku-cell__value", cell.value.toString)
              else if cell.pencil.nonEmpty then
                div(
                  cls := "sudoku-pencil",
                  (1 to 9).map { n =>
                    div(
                      cls := "sudoku-pencil__digit",
                      if cell.pencil.contains(n) then n.toString else ""
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
      stateSig: Signal[State],
      onEnter: Int => Unit
  ): HtmlElement =
    div(
      cls := "sudoku-pad",
      (1 to 9).map { n =>
        button(
          cls := "sudoku-pad__btn",
          cls("is-pencil") <-- stateSig.map(_.pencilMode),
          n.toString,
          onClick --> (_ => onEnter(n))
        )
      },
      button(
        cls := "sudoku-pad__btn sudoku-pad__btn--erase",
        "⌫",
        onClick --> (_ => onEnter(0))
      )
    )

