package tandu.activities

import com.raquo.laminar.api.L.*
import tandu.AppState
import tandu.i18n.Strings
import tandu.ui.{Components, Mode, ModeChooser, RulesCard}
import tandu.ui.Components.s

object Chess extends Activity:
  val id = "chess"
  def name(s: Strings): String = s.chess.name
  def description(s: Strings): String = s.chess.description
  val categories: Set[Category] = Set(Category.Tabletop)

  private val Size = 8

  enum Kind:
    case Pawn, Knight, Bishop, Rook, Queen, King
    def glyph: String = this match
      case Pawn   => "♟"
      case Knight => "♞"
      case Bishop => "♝"
      case Rook   => "♜"
      case Queen  => "♛"
      case King   => "♚"

  final case class Piece(owner: Player, kind: Kind)

  enum MoveKind:
    case Normal, DoubleStep, EnPassant, CastleKingSide, CastleQueenSide, Promotion

  private final case class Move(from: Int, to: Int, capture: Option[Int], kind: MoveKind)

  private final case class CastlingRights(
      p1KingSide: Boolean, p1QueenSide: Boolean,
      p2KingSide: Boolean, p2QueenSide: Boolean
  )
  private object CastlingRights:
    val initial: CastlingRights = CastlingRights(true, true, true, true)

  private enum Outcome:
    case Ongoing
    case Checkmate(winner: Player)
    case Stalemate

  private final case class State(
      board: Vector[Option[Piece]],
      turn: Player,
      selected: Option[Int],
      castling: CastlingRights,
      enPassant: Option[Int],
      lastMove: Option[(Int, Int)],
      outcome: Outcome
  )

  private final case class Hints(targets: Set[Int], captures: Set[Int])

  private def xy(i: Int): (Int, Int) = (i % Size, i / Size)
  private def idx(x: Int, y: Int): Int = y * Size + x
  private def inBounds(x: Int, y: Int): Boolean =
    x >= 0 && x < Size && y >= 0 && y < Size

  private val InitialBoard: Vector[Option[Piece]] =
    val backRank = Vector(Kind.Rook, Kind.Knight, Kind.Bishop, Kind.Queen,
                          Kind.King, Kind.Bishop, Kind.Knight, Kind.Rook)
    Vector.tabulate(Size * Size) { i =>
      val (x, y) = xy(i)
      y match
        case 0 => Some(Piece(Player.P2, backRank(x)))
        case 1 => Some(Piece(Player.P2, Kind.Pawn))
        case 6 => Some(Piece(Player.P1, Kind.Pawn))
        case 7 => Some(Piece(Player.P1, backRank(x)))
        case _ => None
    }

  private val initial: State =
    State(InitialBoard, Player.P1, None, CastlingRights.initial, None, None, Outcome.Ongoing)

  private val RookDirs   = List((1, 0), (-1, 0), (0, 1), (0, -1))
  private val BishopDirs = List((1, 1), (1, -1), (-1, 1), (-1, -1))
  private val QueenDirs  = RookDirs ::: BishopDirs
  private val KingDirs   = QueenDirs
  private val KnightHops = List(
    (1, 2), (2, 1), (2, -1), (1, -2),
    (-1, -2), (-2, -1), (-2, 1), (-1, 2)
  )

  private def pawnDir(p: Player): Int      = if p == Player.P1 then -1 else 1
  private def startRow(p: Player): Int     = if p == Player.P1 then 6 else 1
  private def promoteRow(p: Player): Int   = if p == Player.P1 then 0 else 7
  private def homeRank(p: Player): Int     = if p == Player.P1 then 7 else 0

  private def isAttackedBy(board: Vector[Option[Piece]], sq: Int, by: Player): Boolean =
    val (x, y) = xy(sq)
    def at(px: Int, py: Int): Option[Piece] =
      if inBounds(px, py) then board(idx(px, py)) else None
    def has(kinds: Set[Kind])(dx: Int, dy: Int): Boolean =
      at(x + dx, y + dy).exists(p => p.owner == by && kinds.contains(p.kind))

    val attackerPawnDy = -pawnDir(by)
    val pawnHit = List((-1, attackerPawnDy), (1, attackerPawnDy)).exists(has(Set(Kind.Pawn)))

    def ray(dirs: List[(Int, Int)], kinds: Set[Kind]): Boolean =
      dirs.exists { (dx, dy) =>
        def step(nx: Int, ny: Int): Boolean =
          if !inBounds(nx, ny) then false
          else board(idx(nx, ny)) match
            case None    => step(nx + dx, ny + dy)
            case Some(p) => p.owner == by && kinds.contains(p.kind)
        step(x + dx, y + dy)
      }

    pawnHit ||
      KnightHops.exists(has(Set(Kind.Knight))) ||
      KingDirs.exists(has(Set(Kind.King))) ||
      ray(RookDirs, Set(Kind.Rook, Kind.Queen)) ||
      ray(BishopDirs, Set(Kind.Bishop, Kind.Queen))

  private def findKing(board: Vector[Option[Piece]], owner: Player): Option[Int] =
    Option(board.indexWhere(_.exists(p => p.owner == owner && p.kind == Kind.King))).filter(_ >= 0)

  private def isInCheck(board: Vector[Option[Piece]], owner: Player): Boolean =
    findKing(board, owner).exists(k => isAttackedBy(board, k, owner.other))

  private def stepMoves(
      board: Vector[Option[Piece]], from: Int, owner: Player, dirs: List[(Int, Int)]
  ): List[Move] =
    val (x, y) = xy(from)
    dirs.flatMap { (dx, dy) =>
      val nx = x + dx; val ny = y + dy
      if !inBounds(nx, ny) then None
      else
        val to = idx(nx, ny)
        board(to) match
          case None                       => Some(Move(from, to, None, MoveKind.Normal))
          case Some(p) if p.owner != owner => Some(Move(from, to, Some(to), MoveKind.Normal))
          case _                          => None
    }

  private def slideMoves(
      board: Vector[Option[Piece]], from: Int, owner: Player, dirs: List[(Int, Int)]
  ): List[Move] =
    val (x, y) = xy(from)
    dirs.flatMap { (dx, dy) =>
      def step(nx: Int, ny: Int, acc: List[Move]): List[Move] =
        if !inBounds(nx, ny) then acc
        else
          val to = idx(nx, ny)
          board(to) match
            case None =>
              step(nx + dx, ny + dy, Move(from, to, None, MoveKind.Normal) :: acc)
            case Some(p) =>
              if p.owner != owner then Move(from, to, Some(to), MoveKind.Normal) :: acc
              else acc
      step(x + dx, y + dy, Nil)
    }

  private def pawnMoves(state: State, from: Int, owner: Player): List[Move] =
    val (x, y) = xy(from)
    val dy = pawnDir(owner)
    def promoted(ny: Int) = ny == promoteRow(owner)

    val pushes: List[Move] =
      val y1 = y + dy
      if !inBounds(x, y1) || state.board(idx(x, y1)).isDefined then Nil
      else
        val one = Move(from, idx(x, y1), None,
          if promoted(y1) then MoveKind.Promotion else MoveKind.Normal)
        val two =
          if y == startRow(owner) && state.board(idx(x, y + 2 * dy)).isEmpty then
            List(Move(from, idx(x, y + 2 * dy), None, MoveKind.DoubleStep))
          else Nil
        one :: two

    val captures: List[Move] = List(-1, 1).flatMap { cdx =>
      val cx = x + cdx; val cy = y + dy
      if !inBounds(cx, cy) then None
      else
        val ti = idx(cx, cy)
        state.board(ti) match
          case Some(p) if p.owner != owner =>
            Some(Move(from, ti, Some(ti),
              if promoted(cy) then MoveKind.Promotion else MoveKind.Normal))
          case None if state.enPassant.contains(ti) =>
            Some(Move(from, ti, Some(idx(cx, y)), MoveKind.EnPassant))
          case _ => None
    }

    pushes ::: captures

  private def castleMoves(state: State, owner: Player): List[Move] =
    val rank = homeRank(owner)
    val kingFrom = idx(4, rank)
    val (canK, canQ) = owner match
      case Player.P1 => (state.castling.p1KingSide, state.castling.p1QueenSide)
      case Player.P2 => (state.castling.p2KingSide, state.castling.p2QueenSide)
    if (!canK && !canQ) || isAttackedBy(state.board, kingFrom, owner.other) then Nil
    else
      val enemy = owner.other
      def empty(x: Int) = state.board(idx(x, rank)).isEmpty
      def safe(x: Int)  = !isAttackedBy(state.board, idx(x, rank), enemy)
      val ks =
        if canK && empty(5) && empty(6) && safe(5) && safe(6) then
          List(Move(kingFrom, idx(6, rank), None, MoveKind.CastleKingSide))
        else Nil
      val qs =
        if canQ && empty(1) && empty(2) && empty(3) && safe(2) && safe(3) then
          List(Move(kingFrom, idx(2, rank), None, MoveKind.CastleQueenSide))
        else Nil
      ks ::: qs

  private def pseudoMoves(state: State, from: Int): List[Move] =
    state.board(from) match
      case Some(p) if p.owner == state.turn =>
        p.kind match
          case Kind.Pawn   => pawnMoves(state, from, p.owner)
          case Kind.Knight => stepMoves(state.board, from, p.owner, KnightHops)
          case Kind.Bishop => slideMoves(state.board, from, p.owner, BishopDirs)
          case Kind.Rook   => slideMoves(state.board, from, p.owner, RookDirs)
          case Kind.Queen  => slideMoves(state.board, from, p.owner, QueenDirs)
          case Kind.King   =>
            stepMoves(state.board, from, p.owner, KingDirs) ::: castleMoves(state, p.owner)
      case _ => Nil

  private def boardAfter(
      board: Vector[Option[Piece]], m: Move, mover: Piece
  ): Vector[Option[Piece]] =
    val placed = if m.kind == MoveKind.Promotion then mover.copy(kind = Kind.Queen) else mover
    val b0 = board.updated(m.from, None).updated(m.to, Some(placed))
    val b1 = m.kind match
      case MoveKind.EnPassant => b0.updated(m.capture.get, None)
      case _                  => b0
    m.kind match
      case MoveKind.CastleKingSide =>
        val r = homeRank(mover.owner)
        b1.updated(idx(7, r), None).updated(idx(5, r), Some(Piece(mover.owner, Kind.Rook)))
      case MoveKind.CastleQueenSide =>
        val r = homeRank(mover.owner)
        b1.updated(idx(0, r), None).updated(idx(3, r), Some(Piece(mover.owner, Kind.Rook)))
      case _ => b1

  private def leavesOwnKingInCheck(state: State, m: Move): Boolean =
    val mover = state.board(m.from).get
    isInCheck(boardAfter(state.board, m, mover), mover.owner)

  private def legalMovesFrom(state: State, from: Int): List[Move] =
    pseudoMoves(state, from).filter(m => !leavesOwnKingInCheck(state, m))

  private val RookSquareRights: Map[Int, CastlingRights => CastlingRights] = Map(
    idx(0, 7) -> (_.copy(p1QueenSide = false)),
    idx(7, 7) -> (_.copy(p1KingSide  = false)),
    idx(0, 0) -> (_.copy(p2QueenSide = false)),
    idx(7, 0) -> (_.copy(p2KingSide  = false))
  )

  private def updateCastling(
      cr: CastlingRights, mover: Piece, m: Move
  ): CastlingRights =
    val afterKing =
      if mover.kind != Kind.King then cr
      else mover.owner match
        case Player.P1 => cr.copy(p1KingSide = false, p1QueenSide = false)
        case Player.P2 => cr.copy(p2KingSide = false, p2QueenSide = false)
    val afterFrom    = RookSquareRights.get(m.from).fold(afterKing)(_(afterKing))
    val afterCapture = m.capture.flatMap(RookSquareRights.get).fold(afterFrom)(_(afterFrom))
    afterCapture

  private def applyMove(state: State, m: Move): State =
    val mover = state.board(m.from).get
    val newBoard = boardAfter(state.board, m, mover)
    val newCastling = updateCastling(state.castling, mover, m)
    val newEnPassant =
      if m.kind == MoveKind.DoubleStep then
        val (x, ty) = xy(m.to)
        val (_, fy) = xy(m.from)
        Some(idx(x, (fy + ty) / 2))
      else None
    val nextState = State(
      board = newBoard,
      turn = state.turn.other,
      selected = None,
      castling = newCastling,
      enPassant = newEnPassant,
      lastMove = Some((m.from, m.to)),
      outcome = Outcome.Ongoing
    )
    val hasLegalMove = (0 until Size * Size).exists(i => legalMovesFrom(nextState, i).nonEmpty)
    val outcome =
      if hasLegalMove then Outcome.Ongoing
      else if isInCheck(newBoard, nextState.turn) then Outcome.Checkmate(state.turn)
      else Outcome.Stalemate
    nextState.copy(outcome = outcome)

  def render(): HtmlElement =
    ModeChooser.render(List(
      Mode(
        id = "in-app",
        label = _.mode.inApp,
        render = () => renderPlay()
      ),
      Mode(
        id = "board",
        label = _.mode.offline,
        materials = List(_.offline.materials.chessBoard),
        render = () => renderRules()
      ),
      Mode(
        id = "lichess",
        label = _.mode.lichess,
        render = () => renderLichess()
      )
    ))

  private val chessRulesSections: List[RulesCard.Section] = List(
    RulesCard.fromRules(_.offline.chess.rules),
    RulesCard.fromRules(_.offline.chess.pieces),
    RulesCard.fromRules(_.offline.chess.specials)
  )

  private def renderRules(): HtmlElement =
    div(
      cls := "stack-lg",
      RulesCard.render(chessRulesSections),
      div(
        cls := "chess-showcase",
        Kind.values.toList.map(pieceShowcase)
      )
    )

  private def showcaseState(kind: Kind): State =
    val owner  = Player.P1
    val demoSq = kind match
      case Kind.Pawn => idx(3, 6) // starting rank — shows push, double-step and captures
      case _         => idx(3, 4)
    val enemies: List[Int] = kind match
      case Kind.Pawn   => List(idx(2, 5), idx(4, 5))
      case Kind.Knight => List(idx(5, 5))
      case Kind.Bishop => List(idx(6, 1))
      case Kind.Rook   => List(idx(3, 1))
      case Kind.Queen  => List(idx(6, 1))
      case Kind.King   => List(idx(2, 5))

    var pieces = Map.empty[Int, Piece]
    pieces += demoSq -> Piece(owner, kind)
    enemies.foreach(i => pieces += i -> Piece(owner.other, Kind.Pawn))

    val board = Vector.tabulate(Size * Size)(pieces.get)
    State(
      board     = board,
      turn      = owner,
      selected  = Some(demoSq),
      castling  = CastlingRights(false, false, false, false),
      enPassant = None,
      lastMove  = None,
      outcome   = Outcome.Ongoing
    )

  private def pieceShowcase(kind: Kind): HtmlElement =
    val state = Val(showcaseState(kind))
    div(
      cls := "card stack",
      h3(cls := "h3 center", child.text <-- AppState.strings.map(_.offline.chess.pieces.lines(kind.ordinal))),
      boardView(state, _ => ())
    )

  private def renderLichess(): HtmlElement =
    div(
      cls := "stack-lg",
      RulesCard.render(chessRulesSections),
      div(
        cls := "center",
        a(
          cls := "btn btn--lg",
          href := "https://lichess.org",
          target := "_blank",
          child.text <-- s(_.offline.chess.lichessLabel)
        )
      )
    )

  private def applyTap(state: State, i: Int): State =
    if state.outcome != Outcome.Ongoing then state
    else
      state.board(i) match
        case Some(p) if p.owner == state.turn =>
          state.copy(selected = Some(i))
        case _ =>
          state.selected.flatMap(from => legalMovesFrom(state, from).find(_.to == i)) match
            case Some(m) => applyMove(state, m)
            case None    => state

  private def activePlayerOf(st: State): Option[Player] = st.outcome match
    case Outcome.Checkmate(w) => Some(w)
    case Outcome.Stalemate    => None
    case Outcome.Ongoing      => Some(st.turn)

  private def hintsOf(st: State): Hints = st.selected match
    case None       => Hints(Set.empty, Set.empty)
    case Some(from) =>
      val moves = legalMovesFrom(st, from)
      val (caps, tgts) = moves.partition(_.capture.isDefined)
      Hints(tgts.map(_.to).toSet, caps.map(_.to).toSet)

  private def checkedKingOf(st: State): Option[Int] = st.outcome match
    case Outcome.Checkmate(w) => findKing(st.board, w.other)
    case Outcome.Stalemate    => None
    case Outcome.Ongoing      => Option.when(isInCheck(st.board, st.turn))(findKing(st.board, st.turn)).flatten

  private def boardView(stateSig: Signal[State], onTap: Int => Unit): HtmlElement =
    val hints         = stateSig.map(hintsOf).distinct
    val checkedKingSq = stateSig.map(checkedKingOf).distinct
    div(
      cls := "board chess-board",
      styleAttr := s"grid-template-columns: repeat($Size, 1fr);",
      (0 until Size * Size).map { i =>
        val (x, y) = xy(i)
        val dark = (x + y) % 2 == 1
        val cellSig    = stateSig.map(_.board(i)).distinct
        val isSelected = stateSig.map(_.selected.contains(i)).distinct
        val isTarget   = hints.map(_.targets.contains(i)).distinct
        val isCapture  = hints.map(_.captures.contains(i)).distinct
        val isLast     = stateSig.map(_.lastMove.exists((f, t) => f == i || t == i)).distinct
        val isCheck    = checkedKingSq.map(_.contains(i)).distinct
        div(
          cls := "cell chess-cell cell--btn",
          cls("chess-cell--dark") := dark,
          cls("chess-cell--light") := !dark,
          cls("chess-cell--selected") <-- isSelected,
          cls("chess-cell--target")   <-- isTarget,
          cls("chess-cell--capture")  <-- isCapture,
          cls("chess-cell--last")     <-- isLast,
          cls("chess-cell--check")    <-- isCheck,
          child <-- cellSig.map {
            case None    => emptyNode
            case Some(p) =>
              div(
                cls := s"chess-piece chess-piece--p${p.owner.num}",
                p.kind.glyph
              )
          },
          onClick --> (_ => onTap(i))
        )
      }
    )

  private def renderPlay(): HtmlElement =
    val state = Var(initial)
    def reset(): Unit = state.set(initial)
    def onTap(i: Int): Unit = state.set(applyTap(state.now(), i))

    val activePlayer = state.signal.map(activePlayerOf).distinct

    val statusSignal: Signal[String] =
      state.signal.combineWith(AppState.strings).map { (st, str) =>
        st.outcome match
          case Outcome.Checkmate(w) => s"${w.labelKey(str)} — ${str.common.youWin}"
          case Outcome.Stalemate    => str.common.draw
          case Outcome.Ongoing      =>
            val base = s"${st.turn.labelKey(str)} — ${str.chess.turn}"
            if isInCheck(st.board, st.turn) then s"$base — ${str.chess.check}" else base
      }.distinct

    div(
      cls := "player-page stack-lg",
      cls("player-page--p1") <-- activePlayer.map(_.contains(Player.P1)),
      cls("player-page--p2") <-- activePlayer.map(_.contains(Player.P2)),
      div(
        cls := "center",
        div(cls := "player-badge", child.text <-- statusSignal)
      ),
      boardView(state.signal, onTap),
      div(
        cls := "row no-print",
        styleAttr := "justify-content: center;",
        Components.replayButton(s(_.common.playAgain), reset(), state.signal.map(_.outcome != Outcome.Ongoing))
      )
    )
