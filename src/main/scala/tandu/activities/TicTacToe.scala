package tandu.activities

import com.raquo.laminar.api.L.*
import tandu.AppState
import tandu.i18n.Strings
import tandu.ui.Components
import tandu.ui.Components.s

object TicTacToe extends Activity:
  val id = "tic-tac-toe"
  def name(s: Strings): String = s.tttName
  def description(s: Strings): String = s.tttDesc
  val categories: Set[Category] = Set(Category.Tabletop)

  enum Mark:
    case X, O
    def next: Mark = this match { case X => O; case O => X }
    def label: String = this match { case X => "X"; case O => "O" }
    def num: Int = this match { case X => 1; case O => 2 }

  final case class Variant(
      id: String,
      size: Int,
      winLen: Int,
      dense: Boolean,
      nameKey: Strings => String,
      descKey: Strings => String
  )

  val variants: List[Variant] = List(
    Variant("classic", 3, 3, dense = false, _.tttClassicName, _.tttClassicDesc),
    Variant("gomoku", 10, 5, dense = true, _.tttGomokuName, _.tttGomokuDesc)
  )

  private case class CellView(mark: Option[Mark], isWin: Boolean)

  private case class State(
      board: Vector[Option[Mark]],
      turn: Mark,
      winner: Option[Mark],
      winLine: Set[Int]
  ):
    def isDraw: Boolean = winner.isEmpty && board.forall(_.isDefined)
    def finished: Boolean = winner.isDefined || isDraw
    def activeMark: Option[Mark] = winner.orElse(Option.when(!isDraw)(turn))

  private object State:
    def empty(size: Int): State =
      State(Vector.fill(size * size)(None), Mark.X, None, Set.empty)

  private val Dirs: List[(Int, Int)] = List((1, 0), (0, 1), (1, 1), (1, -1))

  private def checkWin(
      board: Vector[Option[Mark]],
      size: Int,
      winLen: Int
  ): (Option[Mark], Set[Int]) =
    def at(x: Int, y: Int): Option[Mark] = board(y * size + x)
    val winning =
      for
        y <- 0 until size
        x <- 0 until size
        (dx, dy) <- Dirs
        ex = x + dx * (winLen - 1)
        ey = y + dy * (winLen - 1)
        if ex >= 0 && ex < size && ey >= 0 && ey < size
        cells = (0 until winLen).map(i => (x + dx * i, y + dy * i))
        marks = cells.map((cx, cy) => at(cx, cy))
        if marks.head.isDefined && marks.forall(_ == marks.head)
      yield (marks.head, cells.map((cx, cy) => cy * size + cx).toSet)
    winning.headOption.getOrElse((None, Set.empty))

  def render(): HtmlElement =
    val variant: Var[Option[Variant]] = Var(None)
    div(
      cls := "stack-lg",
      child <-- variant.signal.map {
        case None    => chooserView(v => variant.set(Some(v)))
        case Some(v) => gameView(v, () => variant.set(None))
      }
    )

  private def chooserView(onPick: Variant => Unit): HtmlElement =
    sectionTag(
      cls := "stack",
      h2(cls := "h2", child.text <-- s(_.tttChooseVariant)),
      div(
        cls := "stack",
        variants.map { v =>
          Components.tile(
            AppState.strings.map(v.nameKey),
            AppState.strings.map(v.descKey),
            onPick(v)
          )
        }
      )
    )

  private def gameView(v: Variant, onBack: () => Unit): HtmlElement =
    val state = Var(State.empty(v.size))

    def play(i: Int): Unit =
      val cur = state.now()
      if cur.winner.isDefined || cur.board(i).isDefined then ()
      else
        val nextBoard = cur.board.updated(i, Some(cur.turn))
        val (winner, line) = checkWin(nextBoard, v.size, v.winLen)
        state.set(State(nextBoard, cur.turn.next, winner, line))

    def reset(): Unit = state.set(State.empty(v.size))

    val labelSignal: Signal[String] =
      state.signal.combineWith(AppState.strings).map { (st, str) =>
        st.winner match
          case Some(w) => s"${str.player} ${w.num} — ${str.tttWins}"
          case None if st.isDraw => str.draw
          case None => s"${str.player} ${st.turn.num} — ${str.tttTurn}"
      }
    val indicatorSignal: Signal[String] =
      state.signal.map(_.activeMark.map(_.label).getOrElse(""))
    val activeMark = state.signal.map(_.activeMark)
    val finished = state.signal.map(_.finished)

    val cellViews: Signal[Vector[CellView]] =
      state.signal.map { st =>
        Vector.tabulate(v.size * v.size)(i => CellView(st.board(i), st.winLine.contains(i)))
      }

    div(
      cls := "player-page stack-lg",
      cls("player-page--p1") <-- activeMark.map(_.contains(Mark.X)),
      cls("player-page--p2") <-- activeMark.map(_.contains(Mark.O)),
      div(
        cls := "center",
        div(
          cls := "player-badge",
          span(child.text <-- labelSignal),
          span(child.text <-- indicatorSignal)
        )
      ),
      div(
        cls := "board ttt-board",
        cls("board--dense") := v.dense,
        styleAttr := s"grid-template-columns: repeat(${v.size}, 1fr);",
        (0 until v.size * v.size).map: i =>
          val cv = cellViews.map(_(i))
          div(
            cls := "cell cell--btn",
            cls("cell--x") <-- cv.map(_.mark.contains(Mark.X)),
            cls("cell--o") <-- cv.map(_.mark.contains(Mark.O)),
            cls("cell--win") <-- cv.map(_.isWin),
            child.text <-- cv.map(_.mark.map(_.label).getOrElse("")),
            onClick --> (_ => play(i))
          )
      ),
      div(
        cls := "row no-print",
        styleAttr := "justify-content: center;",
        Components.ghost(s(_.tttChangeVariant), onBack()),
        button(
          cls := "btn btn--player",
          child.text <-- s(_.playAgain),
          disabled <-- finished.map(!_),
          onClick --> (_ => reset())
        )
      )
    )
