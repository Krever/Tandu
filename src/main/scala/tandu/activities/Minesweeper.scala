package tandu.activities

import com.raquo.laminar.api.L.*
import tandu.{AppState, Page, Routing}
import tandu.i18n.Strings
import tandu.ui.{Mode, ModeChooser}
import tandu.ui.Components.s

import scala.collection.mutable
import scala.util.Random

object Minesweeper extends Activity:
  val id = "minesweeper"
  def name(s: Strings): String = s.minesweeper.name
  def description(s: Strings): String = s.minesweeper.description
  val minPlayers: Int = 1
  val maxPlayers: Int = 1
  val glyph: String = "✺"
  val tint: String = "vermilion"

  final case class Cell(
      mine: Boolean,
      revealed: Boolean,
      flagged: Boolean,
      adjacent: Int
  )

  enum Status:
    case Playing, Won, Lost

  enum TapMode:
    case Reveal, Flag

  final case class Board(rows: Int, cols: Int, mines: Int, cells: Vector[Cell]):
    def idx(r: Int, c: Int): Int = r * cols + c
    def cell(r: Int, c: Int): Cell = cells(idx(r, c))
    def inBounds(r: Int, c: Int): Boolean = r >= 0 && r < rows && c >= 0 && c < cols
    def neighbors(r: Int, c: Int): Seq[(Int, Int)] =
      for
        dr <- -1 to 1
        dc <- -1 to 1
        if !(dr == 0 && dc == 0)
        nr = r + dr
        nc = c + dc
        if inBounds(nr, nc)
      yield (nr, nc)
    def hasMines: Boolean = cells.exists(_.mine)

  final case class State(
      board: Board,
      status: Status,
      flagsLeft: Int
  )

  final case class Variant(
      id: String,
      rows: Int,
      cols: Int,
      mines: Int,
      nameKey: Strings => String,
      descKey: Strings => String
  )

  val variants: List[Variant] = List(
    Variant("easy",   9,  9,  10, _.minesweeper.easy.name,   _.minesweeper.easy.description),
    Variant("medium", 12, 12, 25, _.minesweeper.medium.name, _.minesweeper.medium.description),
    Variant("hard",   16, 12, 40, _.minesweeper.hard.name,   _.minesweeper.hard.description)
  )

  private def emptyBoard(rows: Int, cols: Int, mines: Int): Board =
    Board(rows, cols, mines, Vector.fill(rows * cols)(Cell(mine = false, revealed = false, flagged = false, adjacent = 0)))

  /** Place mines avoiding the first-tap cell and its 8 neighbours. */
  private def withMines(board: Board, firstR: Int, firstC: Int, rng: Random): Board =
    val avoid: Set[(Int, Int)] =
      (board.neighbors(firstR, firstC) :+ (firstR, firstC)).toSet
    val candidates =
      (for r <- 0 until board.rows; c <- 0 until board.cols if !avoid.contains((r, c))
      yield (r, c)).toIndexedSeq
    val picked = rng.shuffle(candidates).take(board.mines).toSet
    val newCells = (0 until board.rows * board.cols).map { i =>
      val r = i / board.cols
      val c = i % board.cols
      val isMine = picked.contains((r, c))
      val adj =
        if isMine then 0
        else board.neighbors(r, c).count((nr, nc) => picked.contains((nr, nc)))
      Cell(mine = isMine, revealed = false, flagged = false, adjacent = adj)
    }.toVector
    board.copy(cells = newCells)

  private def initial(v: Variant): State =
    State(
      board = emptyBoard(v.rows, v.cols, v.mines),
      status = Status.Playing,
      flagsLeft = v.mines
    )

  /** Reveal a cell, cascading through 0-adjacent neighbours. */
  private def reveal(board: Board, startR: Int, startC: Int): Board =
    val cells = board.cells.toArray
    val queue = mutable.Queue.empty[(Int, Int)]
    queue.enqueue((startR, startC))
    while queue.nonEmpty do
      val (r, c) = queue.dequeue()
      val i = board.idx(r, c)
      val cell = cells(i)
      if !cell.revealed && !cell.flagged then
        cells(i) = cell.copy(revealed = true)
        if !cell.mine && cell.adjacent == 0 then
          board.neighbors(r, c).foreach { (nr, nc) =>
            val nc2 = cells(board.idx(nr, nc))
            if !nc2.revealed && !nc2.flagged then queue.enqueue((nr, nc))
          }
    board.copy(cells = cells.toVector)

  private def revealAllMines(board: Board): Board =
    val newCells = board.cells.map(c => if c.mine then c.copy(revealed = true) else c)
    board.copy(cells = newCells)

  private def allSafeRevealed(board: Board): Boolean =
    board.cells.forall(c => c.mine || c.revealed)

  private def tap(state: State, r: Int, c: Int, rng: Random = new Random()): State =
    if state.status != Status.Playing then state
    else
      val boardWithMines =
        if state.board.hasMines then state.board
        else withMines(state.board, r, c, rng)
      val cell = boardWithMines.cell(r, c)
      if cell.flagged || cell.revealed then
        state.copy(board = boardWithMines)
      else if cell.mine then
        state.copy(
          board = revealAllMines(boardWithMines.copy(
            cells = boardWithMines.cells.updated(boardWithMines.idx(r, c), cell.copy(revealed = true))
          )),
          status = Status.Lost
        )
      else
        val revealed = reveal(boardWithMines, r, c)
        val won = allSafeRevealed(revealed)
        state.copy(
          board = if won then revealAllMines(revealed) else revealed,
          status = if won then Status.Won else Status.Playing
        )

  private def toggleFlag(state: State, r: Int, c: Int): State =
    if state.status != Status.Playing then state
    else
      val cell = state.board.cell(r, c)
      if cell.revealed then state
      else
        val flagged = !cell.flagged
        if flagged && state.flagsLeft <= 0 then state
        else
          val newCells = state.board.cells.updated(state.board.idx(r, c), cell.copy(flagged = flagged))
          state.copy(
            board = state.board.copy(cells = newCells),
            flagsLeft = state.flagsLeft + (if flagged then -1 else 1)
          )

  // ---------- UI ----------

  def render(): HtmlElement =
    ModeChooser.render(id, List(
      Mode(
        id = "in-app",
        label = _.mode.inApp,
        render = () => renderPlay()
      )
    ))

  // First variant is the implicit default when the URL has no variant segment.
  private val variantSignal: Signal[Variant] =
    Routing.router.currentPageSignal.map {
      case Page.Activity(`id`, path) =>
        path.headOption.flatMap(s => variants.find(_.id == s)).getOrElse(variants(0))
      case _ => variants(0)
    }.distinct

  private def renderPlay(): HtmlElement =
    div(
      cls := "ms stack-lg",
      variantPill(),
      child <-- variantSignal.map(playForVariant)
    )

  private def playForVariant(v: Variant): HtmlElement =
    val state   = Var(initial(v))
    val tapMode = Var(TapMode.Reveal)

    def onCellTap(r: Int, c: Int): Unit =
      tapMode.now() match
        case TapMode.Reveal => state.update(tap(_, r, c))
        case TapMode.Flag   => state.update(toggleFlag(_, r, c))

    def onCellFlag(r: Int, c: Int): Unit =
      state.update(toggleFlag(_, r, c))

    def reset(): Unit = state.set(initial(v))

    div(
      cls := "stack-lg",
      div(
        cls := "ms-status",
        div(
          cls := "ms-flags",
          span(cls := "ms-flags__icon", "🚩"),
          span(child.text <-- state.signal.map(_.flagsLeft.toString))
        ),
        div(
          cls := "ms-banner",
          child.text <-- state.signal.combineWith(AppState.strings).map { (st, str) =>
            st.status match
              case Status.Playing => str.minesweeper.playing
              case Status.Won     => str.minesweeper.youWon
              case Status.Lost    => str.minesweeper.youLost
          }
        ),
        button(
          cls := "btn btn--ghost",
          child.text <-- s(_.minesweeper.newGame),
          onClick --> (_ => reset())
        )
      ),
      tapModePill(tapMode),
      boardView(v, state.signal, onCellTap, onCellFlag)
    )

  private def tapModePill(tapMode: Var[TapMode]): HtmlElement =
    div(
      cls := "center no-print",
      div(
        cls := "pill-toggle ms-mode",
        button(
          cls := "pill-btn",
          cls("is-active") <-- tapMode.signal.map(_ == TapMode.Reveal),
          span(cls := "ms-mode__icon", "⛏"),
          span(child.text <-- s(_.minesweeper.revealMode)),
          onClick --> (_ => tapMode.set(TapMode.Reveal))
        ),
        button(
          cls := "pill-btn",
          cls("is-active") <-- tapMode.signal.map(_ == TapMode.Flag),
          span(cls := "ms-mode__icon", "🚩"),
          span(child.text <-- s(_.minesweeper.flagMode)),
          onClick --> (_ => tapMode.set(TapMode.Flag))
        )
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
            onClick --> (_ => Routing.go(Page.Activity(id, List(v.id))))
          )
        }
      )
    )

  private def boardView(
      v: Variant,
      stateSig: Signal[State],
      onCellTap: (Int, Int) => Unit,
      onCellFlag: (Int, Int) => Unit
  ): HtmlElement =
    div(
      cls := "ms-board",
      styleAttr := s"grid-template-columns: repeat(${v.cols}, 1fr);",
      (0 until v.rows).flatMap { r =>
        (0 until v.cols).map { c =>
          val cellSig = stateSig.map(_.board.cell(r, c)).distinct
          div(
            cls := "ms-cell",
            cls("ms-cell--revealed") <-- cellSig.map(_.revealed),
            cls("ms-cell--mine")     <-- cellSig.map(c => c.revealed && c.mine),
            cls("ms-cell--flagged")  <-- cellSig.map(c => c.flagged && !c.revealed),
            child <-- cellSig.map(cellContent),
            onClick --> (_ => onCellTap(r, c)),
            onContextMenu.preventDefault --> (_ => onCellFlag(r, c))
          )
        }
      }
    )

  private def cellContent(cell: Cell): HtmlElement =
    if !cell.revealed then
      if cell.flagged then div(cls := "ms-cell__face", "🚩")
      else div(cls := "ms-cell__face")
    else if cell.mine then div(cls := "ms-cell__face", "💣")
    else if cell.adjacent == 0 then div(cls := "ms-cell__face")
    else
      div(
        cls := s"ms-cell__face ms-cell__face--n${cell.adjacent}",
        cell.adjacent.toString
      )
