package tandu.activities

import com.raquo.laminar.api.L.*
import tandu.AppState
import tandu.i18n.Strings
import tandu.ui.Components
import tandu.ui.Components.s

object Checkers extends Activity:
  val id = "checkers"
  def name(s: Strings): String = s.checkers.name
  def description(s: Strings): String = s.checkers.description
  val categories: Set[Category] = Set(Category.Tabletop)

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
    val state = Var(initial)

    def tap(i: Int): Unit =
      val cur = state.now()
      if cur.winner.isDefined then ()
      else
        cur.board(i) match
          case Some(p) if p.owner == cur.turn && !cur.selection.exists(_.locked) =>
            state.set(cur.copy(selection = Some(Selection(i, locked = false))))
          case _ =>
            for
              sel <- cur.selection
              m   <- legalMovesFor(cur, sel.at).find(_.to == i)
            do state.set(applyMove(cur, m))

    def reset(): Unit = state.set(initial)

    val activePlayer = state.signal.map(st => st.winner.orElse(Some(st.turn)))

    val statusSignal: Signal[String] =
      state.signal.combineWith(AppState.strings).map { (st, str) =>
        st.winner match
          case Some(w) => s"${w.labelKey(str)} — ${str.common.youWin}"
          case None    => s"${st.turn.labelKey(str)} — ${str.checkers.turn}"
      }

    val hints: Signal[Hints] = state.signal.map { st =>
      if st.winner.isDefined then Hints(Set.empty, Set.empty)
      else
        val targets = st.selection.map(sel => legalMovesFor(st, sel.at).map(_.to).toSet).getOrElse(Set.empty)
        val capturing =
          if st.selection.exists(_.locked) then Set.empty
          else
            val caps = allMoves(st.board, st.turn).filter(_.isCapture)
            if caps.isEmpty then Set.empty else caps.map(_.from).toSet
        Hints(targets, capturing)
    }.distinct

    div(
      cls := "player-page stack-lg",
      cls("player-page--p1") <-- activePlayer.map(_.contains(Player.P1)).distinct,
      cls("player-page--p2") <-- activePlayer.map(_.contains(Player.P2)).distinct,
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
          val cellSig = state.signal.map(_.board(i)).distinct
          val isSelected = state.signal.map(_.selection.exists(_.at == i)).distinct
          val isTarget = hints.map(_.targets.contains(i)).distinct
          val canCapture = hints.map(_.capturing.contains(i)).distinct
          div(
            cls := "cell ck-cell",
            cls("ck-cell--dark") := dark,
            cls("ck-cell--light") := !dark,
            cls("cell--btn") := dark,
            cls("ck-cell--selected") <-- isSelected,
            cls("ck-cell--target") <-- isTarget,
            cls("ck-cell--hint") <-- canCapture,
            child <-- cellSig.map {
              case None => emptyNode
              case Some(p) =>
                div(
                  cls := s"ck-piece ck-piece--p${p.owner.num}",
                  cls("ck-piece--king") := p.isKing
                )
            },
            onClick --> (_ => if dark then tap(i))
          )
        }
      ),
      div(
        cls := "row no-print",
        styleAttr := "justify-content: center;",
        Components.replayButton(s(_.common.playAgain), reset(), state.signal.map(_.winner.isDefined))
      )
    )
