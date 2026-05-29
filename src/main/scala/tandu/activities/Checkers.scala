package tandu.activities

import com.raquo.laminar.api.L.*
import tandu.AppState
import tandu.i18n.Strings
import tandu.ui.{Components, Mode, ModeChooser, RulesCard}
import tandu.ui.Components.s

import scala.scalajs.js

object Checkers extends Activity:
  val id = "checkers"
  def name(s: Strings): String = s.checkers.name
  def description(s: Strings): String = s.checkers.description
  val minPlayers: Int = 2
  val maxPlayers: Int = 2
  val handsFree: Boolean = false

  private val Size = 8

  enum Piece(val owner: Player):
    case Man(p: Player)  extends Piece(p)
    case King(p: Player) extends Piece(p)
    def isKing: Boolean = this match
      case King(_) => true
      case _       => false
    def promoted: Piece = this match
      case Man(o) => King(o)
      case k      => k

  private final case class Move(from: Int, to: Int, captured: Option[Int]):
    def isCapture: Boolean = captured.isDefined

  private final case class Selection(at: Int, locked: Boolean)

  private final case class State(
      board: Vector[Option[Piece]],
      turn: Player,
      selection: Option[Selection],
      winner: Option[Player]
  )

  private final case class Hints(targets: Set[Int], capturing: Set[Int])

  private def xy(i: Int): (Int, Int) = (i % Size, i / Size)
  private def idx(x: Int, y: Int): Int = y * Size + x
  private def inBounds(x: Int, y: Int): Boolean =
    x >= 0 && x < Size && y >= 0 && y < Size

  private val InitialBoard: Vector[Option[Piece]] =
    Vector.tabulate(Size * Size) { i =>
      val (x, y) = xy(i)
      if (x + y) % 2 != 1 then None
      else if y < 3 then Some(Piece.Man(Player.P2))
      else if y >= 5 then Some(Piece.Man(Player.P1))
      else None
    }

  private val initial: State = State(InitialBoard, Player.P1, None, None)

  private def directions(p: Piece): List[(Int, Int)] = p match
    case Piece.Man(Player.P1) => List((-1, -1), (1, -1))
    case Piece.Man(Player.P2) => List((-1, 1), (1, 1))
    case Piece.King(_)        => List((-1, -1), (1, -1), (-1, 1), (1, 1))

  private def movesFrom(board: Vector[Option[Piece]], from: Int): List[Move] =
    board(from) match
      case None => Nil
      case Some(p) =>
        val (x, y) = xy(from)
        val dirs = directions(p)
        val captures = dirs.flatMap { (dx, dy) =>
          val mx = x + dx; val my = y + dy
          val lx = x + 2 * dx; val ly = y + 2 * dy
          if !inBounds(lx, ly) then None
          else
            board(idx(mx, my)) match
              case Some(mp) if mp.owner != p.owner && board(idx(lx, ly)).isEmpty =>
                Some(Move(from, idx(lx, ly), Some(idx(mx, my))))
              case _ => None
        }
        if captures.nonEmpty then captures
        else
          dirs.flatMap { (dx, dy) =>
            val nx = x + dx; val ny = y + dy
            if inBounds(nx, ny) && board(idx(nx, ny)).isEmpty then
              Some(Move(from, idx(nx, ny), None))
            else None
          }

  private def allMoves(board: Vector[Option[Piece]], player: Player): List[Move] =
    val any = board.zipWithIndex.collect {
      case (Some(p), i) if p.owner == player => movesFrom(board, i)
    }.flatten.toList
    val captures = any.filter(_.isCapture)
    if captures.nonEmpty then captures else any

  private def legalMovesFor(state: State, from: Int): List[Move] =
    state.selection match
      case Some(sel) if sel.locked && sel.at == from =>
        movesFrom(state.board, from).filter(_.isCapture)
      case Some(sel) if sel.locked => Nil
      case _ =>
        allMoves(state.board, state.turn).filter(_.from == from)

  private def applyTap(state: State, i: Int): State =
    if state.winner.isDefined then state
    else
      state.board(i) match
        case Some(p) if p.owner == state.turn && !state.selection.exists(_.locked) =>
          state.copy(selection = Some(Selection(i, locked = false)))
        case _ =>
          state.selection.flatMap(sel => legalMovesFor(state, sel.at).find(_.to == i)) match
            case Some(m) => applyMove(state, m)
            case None    => state

  private def applyMove(state: State, m: Move): State =
    val piece = state.board(m.from).get
    val (_, ty) = xy(m.to)
    val promote = (piece, ty) match
      case (Piece.Man(Player.P1), 0)                  => true
      case (Piece.Man(Player.P2), y) if y == Size - 1 => true
      case _                                          => false
    val finalPiece = if promote then piece.promoted else piece
    val moved = state.board.updated(m.from, None).updated(m.to, Some(finalPiece))
    val newBoard = m.captured.fold(moved)(c => moved.updated(c, None))

    val continuations =
      if m.isCapture && !promote then movesFrom(newBoard, m.to).filter(_.isCapture)
      else Nil

    if continuations.nonEmpty then
      state.copy(board = newBoard, selection = Some(Selection(m.to, locked = true)))
    else
      val nextTurn = state.turn.other
      val winner = Option.when(allMoves(newBoard, nextTurn).isEmpty)(state.turn)
      state.copy(board = newBoard, turn = nextTurn, selection = None, winner = winner)

  def render(): HtmlElement =
    ModeChooser.render(id, List(
      Mode(
        id = "in-app",
        label = _.mode.inApp,
        render = () => renderPlay()
      ),
      Mode(
        id = "board",
        label = _.mode.offline,
        materials = List(_.offline.materials.checkersBoard),
        render = () => renderRules()
      ),
      Mode(
        id = "lichess",
        label = _.mode.lichess,
        render = () => renderLichess()
      )
    ))

  private val checkersRulesSections: List[RulesCard.Section] =
    List(RulesCard.fromRules(_.offline.checkers.rules))

  // Replays a real player flow: each entry is a cell tap (select source, then destination).
  private val DemoTaps: Vector[Int] = Vector(
    idx(4, 5), idx(3, 4),                    // P1 advances
    idx(1, 2), idx(0, 3),                    // P2 advances
    idx(2, 5), idx(1, 4),                    // P1 advances
    idx(0, 3), idx(2, 5),                    // P2 captures
    idx(3, 6), idx(1, 4)                     // P1 captures back
  )
  private val DemoTapMs     = 1000
  private val DemoRestartMs = 2500

  private def renderRules(): HtmlElement =
    val example = Var(initial)
    var step = 0
    var handle: Option[js.timers.SetTimeoutHandle] = None

    def schedule(delay: Int): Unit =
      handle = Some(js.timers.setTimeout(delay) {
        if step >= DemoTaps.length then
          example.set(initial)
          step = 0
          schedule(DemoTapMs)
        else
          example.set(applyTap(example.now(), DemoTaps(step)))
          step += 1
          schedule(if step >= DemoTaps.length then DemoRestartMs else DemoTapMs)
      })

    schedule(DemoTapMs)

    div(
      cls := "stack-lg",
      onUnmountCallback(_ => handle.foreach(js.timers.clearTimeout)),
      styleAttr <-- activePlayerSig(example.signal).map(playerColorVar),
      RulesCard.render(checkersRulesSections),
      gameView(example.signal, _ => ())
    )

  private def activePlayerSig(stateSig: Signal[State]): Signal[Option[Player]] =
    stateSig.map(st => st.winner.orElse(Some(st.turn))).distinct

  private def playerColorVar(p: Option[Player]): String = p match
    case Some(Player.P1) => "--player-color: var(--color-p1);"
    case Some(Player.P2) => "--player-color: var(--color-p2);"
    case None            => ""

  private def renderLichess(): HtmlElement =
    div(
      cls := "stack-lg",
      RulesCard.render(checkersRulesSections),
      div(
        cls := "center",
        a(
          cls := "btn btn--lg",
          href := "https://lidraughts.org",
          target := "_blank",
          child.text <-- s(_.offline.checkers.lichessLabel)
        )
      )
    )

  private def renderPlay(): HtmlElement =
    val state = Var(initial)
    def reset(): Unit = state.set(initial)
    def onTap(i: Int): Unit = state.set(applyTap(state.now(), i))
    val active = activePlayerSig(state.signal)

    div(
      cls := "player-page stack-lg",
      cls("player-page--p1") <-- active.map(_.contains(Player.P1)),
      cls("player-page--p2") <-- active.map(_.contains(Player.P2)),
      gameView(state.signal, onTap),
      div(
        cls := "row no-print",
        styleAttr := "justify-content: center;",
        Components.replayButton(s(_.common.playAgain), reset(), state.signal.map(_.winner.isDefined))
      )
    )

  private def hintsOf(st: State): Hints =
    if st.winner.isDefined then Hints(Set.empty, Set.empty)
    else
      val targets = st.selection.map(sel => legalMovesFor(st, sel.at).map(_.to).toSet).getOrElse(Set.empty)
      val capturing =
        if st.selection.exists(_.locked) then Set.empty
        else
          val caps = allMoves(st.board, st.turn).filter(_.isCapture)
          if caps.isEmpty then Set.empty else caps.map(_.from).toSet
      Hints(targets, capturing)

  private def gameView(stateSig: Signal[State], onTap: Int => Unit): HtmlElement =
    val statusSignal: Signal[String] =
      stateSig.combineWith(AppState.strings).map { (st, str) =>
        st.winner match
          case Some(w) => s"${w.labelKey(str)} — ${str.common.youWin}"
          case None    => s"${st.turn.labelKey(str)} — ${str.checkers.turn}"
      }

    val hints: Signal[Hints] = stateSig.map(hintsOf).distinct

    div(
      cls := "stack",
      div(
        cls := "center",
        div(cls := "player-badge", child.text <-- statusSignal)
      ),
      div(
        cls := "board ck-board",
        styleAttr := s"grid-template-columns: repeat($Size, 1fr);",
        (0 until Size * Size).map { i =>
          val (x, y) = xy(i)
          val dark = (x + y) % 2 == 1
          val cellSig    = stateSig.map(_.board(i)).distinct
          val isSelected = stateSig.map(_.selection.exists(_.at == i)).distinct
          val isTarget   = hints.map(_.targets.contains(i)).distinct
          val canCapture = hints.map(_.capturing.contains(i)).distinct
          div(
            cls := "cell ck-cell",
            cls("ck-cell--dark") := dark,
            cls("ck-cell--light") := !dark,
            cls("cell--btn") := dark,
            cls("ck-cell--selected") <-- isSelected,
            cls("ck-cell--target")   <-- isTarget,
            cls("ck-cell--hint")     <-- canCapture,
            child <-- cellSig.map {
              case None    => emptyNode
              case Some(p) =>
                div(
                  cls := s"ck-piece ck-piece--p${p.owner.num}",
                  cls("ck-piece--king") := p.isKing
                )
            },
            onClick --> (_ => if dark then onTap(i))
          )
        }
      )
    )
