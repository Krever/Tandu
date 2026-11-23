package tandu.activities

import com.raquo.laminar.api.L.*
import tandu.{AppState, Page, Routing}
import tandu.i18n.Strings
import tandu.ui.{Components, Mode, ModeChooser, RulesCard}
import tandu.ui.Components.s

import scala.scalajs.js
import scala.util.Random

/** Drop discs into a 7×6 grid; first to line up four wins. Played pass-and-play
  * between two people, or solo against a deliberately beatable computer. */
object FourInARow extends Activity:
  val id = "four-in-a-row"
  def name(s: Strings): String = s.fourInARow.name
  def description(s: Strings): String = s.fourInARow.description
  // Solo counts: the computer opponent makes it a one-player game too.
  val minPlayers: Int = 1
  val maxPlayers: Int = 2
  val glyph: String = "🔴"
  val tint: String = "plum"

  val Cols = 7
  val Rows = 6
  val WinLen = 4

  /** Row 0 is the top of the board; index = row * Cols + col. */
  type Board = Vector[Option[Player]]

  def emptyBoard: Board = Vector.fill(Cols * Rows)(None)

  def idx(col: Int, row: Int): Int = row * Cols + col

  private def inBounds(col: Int, row: Int): Boolean =
    col >= 0 && col < Cols && row >= 0 && row < Rows

  /** The row a disc dropped into `col` lands on, if the column has room. */
  def landingRow(board: Board, col: Int): Option[Int] =
    (Rows - 1 to 0 by -1).find(r => board(idx(col, r)).isEmpty)

  def legalCols(board: Board): List[Int] =
    (0 until Cols).filter(c => board(idx(c, 0)).isEmpty).toList

  def drop(board: Board, col: Int, p: Player): Option[(Board, Int)] =
    landingRow(board, col).map(r => (board.updated(idx(col, r), Some(p)), idx(col, r)))

  private val Dirs: List[(Int, Int)] = List((1, 0), (0, 1), (1, 1), (1, -1))

  /** The winning cells through `at`, if the disc there completes a line. Only
    * the last-placed disc can create a new line, so that's all we check. */
  def winThrough(board: Board, at: Int): Option[Set[Int]] =
    board(at).flatMap { p =>
      val (c0, r0) = (at % Cols, at / Cols)
      def run(dx: Int, dy: Int): List[Int] =
        Iterator
          .iterate((c0 + dx, r0 + dy))((c, r) => (c + dx, r + dy))
          .takeWhile((c, r) => inBounds(c, r) && board(idx(c, r)).contains(p))
          .map((c, r) => idx(c, r))
          .toList
      Dirs.iterator
        .map((dx, dy) => at :: run(dx, dy) ::: run(-dx, -dy))
        .find(_.size >= WinLen)
        .map(_.toSet)
    }

  // ---- Computer opponent ---------------------------------------------------

  /** Picks a column for `me`. Takes a win when it sees one, usually blocks an
    * immediate threat, avoids handing over a win, and otherwise plays toward
    * the centre and its own lines. The occasional missed block keeps it
    * beatable for kids. */
  def computerMove(board: Board, me: Player, rnd: Random = Random): Int =
    val legal = legalCols(board)
    def winsFor(b: Board, p: Player)(c: Int): Boolean =
      drop(b, c, p).exists((b2, at) => winThrough(b2, at).isDefined)

    legal
      .find(winsFor(board, me))
      .orElse(legal.find(winsFor(board, me.other)).filter(_ => rnd.nextDouble() < 0.85))
      .getOrElse {
        // A move is unsafe if it lets the opponent win by dropping on top.
        val safe = legal.filterNot(c => drop(board, c, me).exists((b, _) => winsFor(b, me.other)(c)))
        val pool = if safe.nonEmpty then safe else legal
        pool.maxBy(c => score(board, c, me) + rnd.nextDouble() * 2)
      }

  /** Heuristic value of dropping into `col`: centre preference plus how many
    * open four-windows through the landing cell already hold our discs. */
  private def score(board: Board, col: Int, me: Player): Double =
    landingRow(board, col).fold(Double.MinValue) { row =>
      val centre = Cols / 2 - math.abs(col - Cols / 2)
      val windows =
        for
          (dx, dy) <- Dirs
          offset   <- 0 until WinLen
          cells = (0 until WinLen).map(i => (col + dx * (i - offset), row + dy * (i - offset)))
          if cells.forall(inBounds)
          discs = cells.map((c, r) => board(idx(c, r)))
          if !discs.exists(_.contains(me.other))
        yield discs.count(_.contains(me)) match
          case 3 => 6.0
          case 2 => 2.0
          case 1 => 0.5
          case _ => 0.0
      centre + windows.sum
    }

  // ---- UI ------------------------------------------------------------------

  enum Opponent(val id: String, val text: Strings => Strings.TttVariant):
    case Friend   extends Opponent("friend", _.fourInARow.friend)
    case Computer extends Opponent("computer", _.fourInARow.computer)

  def render(): HtmlElement =
    ModeChooser.render(id, List(
      Mode(
        id = "in-app",
        label = _.mode.inApp,
        render = () => renderPlay()
      ),
      Mode(
        id = "paper",
        label = _.mode.offline,
        materials = List(_.offline.materials.paperPen),
        render = () => RulesCard.render(List(RulesCard.fromRules(_.offline.fourInARow.rules)))
      )
    ))

  private def renderPlay(): HtmlElement =
    ModeChooser.render(
      activityId = id,
      prefix = List("in-app"),
      heading = _.fourInARow.chooseOpponent,
      modes = Opponent.values.toList.map(o =>
        Mode(
          id = o.id,
          label = o.text(_).name,
          description = Some(o.text(_).description),
          testIdPrefix = "opponent",
          render = () => gameView(o)
        )
      )
    )

  private final case class State(
      board: Board,
      turn: Player,
      winner: Option[Player],
      winLine: Set[Int]
  ):
    lazy val isDraw: Boolean   = winner.isEmpty && legalCols(board).isEmpty
    lazy val finished: Boolean = winner.isDefined || isDraw
    def activePlayer: Option[Player] = winner.orElse(Option.when(!isDraw)(turn))

  private object State:
    def initial: State = State(emptyBoard, Player.P1, None, Set.empty)

  private val ComputerDelayMs = 550.0

  private def gameView(opponent: Opponent): HtmlElement =
    val vsComputer = opponent == Opponent.Computer
    val computer   = Player.P2
    val state      = Var(State.initial)
    val hoverCol   = Var(Option.empty[Int])
    // The pending computer move; cancelled on reset and on unmount.
    var pending: Option[js.timers.SetTimeoutHandle] = None
    def cancelPending(): Unit =
      pending.foreach(js.timers.clearTimeout)
      pending = None

    def computerToMove(st: State): Boolean = vsComputer && !st.finished && st.turn == computer
    def locked(st: State): Boolean         = st.finished || computerToMove(st)

    def place(col: Int): Unit =
      val cur = state.now()
      if !cur.finished then
        drop(cur.board, col, cur.turn).foreach { (board, at) =>
          val line = winThrough(board, at)
          val next = State(board, cur.turn.other, line.map(_ => cur.turn), line.getOrElse(Set.empty))
          state.set(next)
          if computerToMove(next) then
            pending = Some(js.timers.setTimeout(ComputerDelayMs) {
              pending = None
              val now = state.now()
              if computerToMove(now) then place(computerMove(now.board, computer))
            })
        }

    def tap(col: Int): Unit =
      if !computerToMove(state.now()) then place(col)

    def reset(): Unit =
      cancelPending()
      state.set(State.initial)

    // Labels never name the computer: its win and its turn have their own texts.
    def nameOf(p: Player, str: Strings): String =
      if vsComputer then str.fourInARow.you else p.labelKey(str)

    val labelSignal: Signal[String] =
      state.signal.combineWith(AppState.strings).map { (st, str) =>
        st.winner match
          case Some(w) if vsComputer =>
            if w == computer then str.fourInARow.computerWins else str.common.youWin
          case Some(w)              => s"${nameOf(w, str)} — ${str.fourInARow.wins}"
          case None if st.isDraw    => str.common.draw
          case None if computerToMove(st) => str.fourInARow.thinking
          case None                 => s"${nameOf(st.turn, str)} — ${str.fourInARow.turn}"
      }
    val active   = state.signal.map(_.activePlayer)
    val finished = state.signal.map(_.finished)
    val isLocked = state.signal.map(locked).distinct

    // The landing cell of the hovered column, shown as a faint preview disc.
    // It shows only while the game runs, so its colour is the active player's
    // (--player-color, from the page wrapper).
    val preview: Signal[Option[Int]] =
      state.signal.combineWith(hoverCol.signal.distinct).map { (st, hc) =>
        if locked(st) then None
        else hc.flatMap(c => landingRow(st.board, c).map(r => idx(c, r)))
      }.distinct

    div(
      cls := "player-page stack-lg",
      onUnmountCallback(_ => cancelPending()),
      cls("player-page--p1") <-- active.map(_.contains(Player.P1)),
      cls("player-page--p2") <-- active.map(_.contains(Player.P2)),
      div(
        cls := "center",
        div(
          cls := "player-badge",
          dataAttr("testid") := "c4-status",
          span(child.text <-- labelSignal),
          child.maybe <-- active.map(_.map(_ => span(cls := "c4-badge-disc")))
        )
      ),
      div(
        cls := "board c4-board",
        cls("is-locked") <-- isLocked,
        styleAttr := s"grid-template-columns: repeat($Cols, 1fr);",
        onMouseLeave --> (_ => hoverCol.set(None)),
        (0 until Rows).flatMap { r =>
          (0 until Cols).map { c =>
            val i    = idx(c, r)
            val mark = state.signal.map(_.board(i)).distinct
            div(
              cls := "cell cell--btn c4-cell",
              dataAttr("col") := c.toString,
              cls("cell--win") <-- state.signal.map(_.winLine.contains(i)).distinct,
              cls("c4-cell--preview") <-- preview.map(_.contains(i)).distinct,
              // A fresh element per placed disc, so the drop animation plays once
              // on arrival; --c4-fall is how many rows it falls through.
              child.maybe <-- mark.map(_.map(p =>
                span(cls := s"c4-disc c4-disc--p${p.num}", styleAttr := s"--c4-fall: ${r + 1};")
              )),
              onMouseEnter --> (_ => hoverCol.set(Some(c))),
              onClick --> (_ => tap(c))
            )
          }
        }
      ),
      div(
        cls := "row no-print",
        styleAttr := "justify-content: center;",
        Components.ghost(s(_.fourInARow.changeOpponent), Routing.go(Page.Activity(id, List("in-app")))),
        Components.replayButton(s(_.common.playAgain), reset(), finished)
      )
    )
