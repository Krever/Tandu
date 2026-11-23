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
          .takeWhile((c, r) => c >= 0 && c < Cols && r >= 0 && r < Rows && board(idx(c, r)).contains(p))
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
    def winsFor(p: Player)(c: Int): Boolean =
      drop(board, c, p).exists((b, at) => winThrough(b, at).isDefined)

    legal.find(winsFor(me)).getOrElse {
      legal.find(winsFor(me.other)).filter(_ => rnd.nextDouble() < 0.85).getOrElse {
        // A move is unsafe if it lets the opponent win by dropping on top.
        val safe = legal.filterNot { c =>
          drop(board, c, me).exists { (b, _) =>
            drop(b, c, me.other).exists((b2, at2) => winThrough(b2, at2).isDefined)
          }
        }
        val pool = if safe.nonEmpty then safe else legal
        pool.maxBy(c => score(board, c, me) + rnd.nextDouble() * 2)
      }
    }

  /** Heuristic value of dropping into `col`: centre preference plus how many
    * open four-windows through the landing cell already hold our discs. */
  private def score(board: Board, col: Int, me: Player): Double =
    landingRow(board, col).fold(Double.MinValue) { row =>
      val centre = 3 - math.abs(col - Cols / 2)
      val windows =
        for
          (dx, dy) <- Dirs
          offset   <- 0 until WinLen
          cells = (0 until WinLen).map(i => (col + dx * (i - offset), row + dy * (i - offset)))
          if cells.forall((c, r) => c >= 0 && c < Cols && r >= 0 && r < Rows)
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

  enum Opponent:
    case Friend, Computer

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
      modes = List(
        Mode(
          id = "friend",
          label = _.fourInARow.friend.name,
          description = Some(_.fourInARow.friend.description),
          testIdPrefix = "opponent",
          render = () => gameView(Opponent.Friend)
        ),
        Mode(
          id = "computer",
          label = _.fourInARow.computer.name,
          description = Some(_.fourInARow.computer.description),
          testIdPrefix = "opponent",
          render = () => gameView(Opponent.Computer)
        )
      )
    )

  private final case class State(
      board: Board,
      turn: Player,
      winner: Option[Player],
      winLine: Set[Int]
  ):
    def isDraw: Boolean = winner.isEmpty && legalCols(board).isEmpty
    def finished: Boolean = winner.isDefined || isDraw
    def activePlayer: Option[Player] = winner.orElse(Option.when(!isDraw)(turn))

  private object State:
    def initial: State = State(emptyBoard, Player.P1, None, Set.empty)

  private val ComputerDelayMs = 550.0

  private def gameView(opponent: Opponent): HtmlElement =
    val vsComputer = opponent == Opponent.Computer
    val computer   = Player.P2
    val state      = Var(State.initial)
    val hoverCol   = Var(Option.empty[Int])
    // Bumped on reset so a computer move scheduled for the old game is dropped.
    var generation = 0

    def computerToMove(st: State): Boolean = vsComputer && !st.finished && st.turn == computer

    def place(col: Int): Unit =
      val cur = state.now()
      if !cur.finished then
        drop(cur.board, col, cur.turn).foreach { (board, at) =>
          val line = winThrough(board, at)
          val next = State(board, cur.turn.other, line.map(_ => cur.turn), line.getOrElse(Set.empty))
          state.set(next)
          if computerToMove(next) then
            val gen = generation
            val _ = js.timers.setTimeout(ComputerDelayMs) {
              val now = state.now()
              if gen == generation && computerToMove(now) then place(computerMove(now.board, computer))
            }
        }

    def tap(col: Int): Unit =
      if !computerToMove(state.now()) then place(col)

    def reset(): Unit =
      generation += 1
      state.set(State.initial)

    def nameOf(p: Player, str: Strings): String =
      if !vsComputer then p.labelKey(str)
      else if p == computer then str.fourInARow.computerName
      else str.fourInARow.you

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

    // The landing cell of the hovered column, shown as a faint preview disc.
    val preview: Signal[Option[Int]] =
      state.signal.combineWith(hoverCol.signal).map { (st, hc) =>
        if st.finished || computerToMove(st) then None
        else hc.flatMap(c => landingRow(st.board, c).map(r => idx(c, r)))
      }

    def discClass(p: Player): String = s"c4-disc c4-disc--p${p.num}"

    div(
      cls := "player-page stack-lg",
      cls("player-page--p1") <-- active.map(_.contains(Player.P1)),
      cls("player-page--p2") <-- active.map(_.contains(Player.P2)),
      div(
        cls := "center",
        div(
          cls := "player-badge",
          dataAttr("testid") := "c4-status",
          span(child.text <-- labelSignal),
          child.maybe <-- active.map(_.map(p => span(cls := s"c4-badge-disc c4-badge-disc--p${p.num}")))
        )
      ),
      div(
        cls := "board c4-board",
        cls("is-locked") <-- state.signal.map(st => st.finished || computerToMove(st)),
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
              cls("c4-cell--preview-p1") <-- preview.combineWith(state.signal).map((pv, st) => pv.contains(i) && st.turn == Player.P1),
              cls("c4-cell--preview-p2") <-- preview.combineWith(state.signal).map((pv, st) => pv.contains(i) && st.turn == Player.P2),
              // A fresh element per placed disc, so the drop animation plays once
              // on arrival; --c4-fall is how many rows it falls through.
              child.maybe <-- mark.map(_.map(p =>
                span(cls := discClass(p), styleAttr := s"--c4-fall: ${r + 1};")
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
