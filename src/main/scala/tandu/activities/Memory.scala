package tandu.activities

import com.raquo.laminar.api.L.*
import tandu.{AppState, Page, Routing}
import tandu.i18n.Strings
import tandu.ui.{Components, Mode, ModeChooser, Printable, RulesCard}
import tandu.ui.Components.s

import scala.scalajs.js
import scala.util.Random

object Memory extends Activity:
  val id = "memory"
  def name(s: Strings): String = s.memory.name
  def description(s: Strings): String = s.memory.description
  val minPlayers: Int = 1
  val maxPlayers: Int = Int.MaxValue
  val handsFree: Boolean = false
  val glyph: String = "◉"
  val tint: String = "teal"

  private val Emojis: Vector[String] = Vector(
    "🐶", "🐱", "🦊", "🐻", "🐼", "🐯", "🦁", "🐮",
    "🐷", "🐸", "🐵", "🐔", "🐧", "🐦", "🦉", "🦄",
    "🐝", "🐞", "🦋", "🐢", "🐠", "🐬", "🐳", "🦀",
    "🌸", "🌻", "🌳", "🌵", "🍎", "🍌", "🍉", "🍓",
    "🍒", "🍕", "🍔", "🍩", "🍦", "🎂", "⚽", "🏀",
    "🚗", "🚂", "🚀", "✈️", "🎈", "🎁", "⭐", "🌈"
  )

  final case class Variant(
      id: String,
      pairs: Int,
      cols: Int,
      nameKey: Strings => String,
      descKey: Strings => String
  )

  val variants: List[Variant] = List(
    Variant("easy",   6,  3, _.memory.easy.name,   _.memory.easy.description),
    Variant("medium", 8,  4, _.memory.medium.name, _.memory.medium.description),
    Variant("hard",  12,  4, _.memory.hard.name,   _.memory.hard.description)
  )

  final case class CardState(emoji: String, faceUp: Boolean, matched: Boolean)

  enum Phase:
    case Idle
    case OneFlipped(idx: Int)
    case Mismatch(a: Int, b: Int)

  final case class State(
      cards: Vector[CardState],
      phase: Phase,
      turn: Player,
      scores: Map[Player, Int]
  ):
    def finished: Boolean = cards.forall(_.matched)
    def winner: Option[Player] =
      if !finished then None
      else
        val p1 = scores(Player.P1)
        val p2 = scores(Player.P2)
        if p1 == p2 then None
        else if p1 > p2 then Some(Player.P1)
        else Some(Player.P2)

  private def dealCards(pairs: Int): Vector[CardState] =
    val chosen = Random.shuffle(Emojis.toList).take(pairs)
    val deck = chosen.flatMap(e => List(e, e))
    Random.shuffle(deck).toVector.map(CardState(_, faceUp = false, matched = false))

  private def initial(v: Variant): State =
    State(
      cards  = dealCards(v.pairs),
      phase  = Phase.Idle,
      turn   = Player.P1,
      scores = Map(Player.P1 -> 0, Player.P2 -> 0)
    )

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
        materials = List(_.offline.materials.printer, _.offline.materials.scissors, _.offline.materials.laminatorOptional),
        hint = Some(_.offline.memory.cutHint),
        render = () => renderOffline()
      )
    ))

  private def renderOffline(): HtmlElement =
    div(
      cls := "stack-lg",
      div(
        cls := "no-print",
        RulesCard.render(List(RulesCard.fromRules(_.offline.memory.rules)))
      ),
      Printable.printButton(),
      div(cls := "print-only", printableSheet())
    )

  private def printableSheet(): HtmlElement =
    // Pairs sit next to each other on the sheet; the parent shuffles after cutting.
    val pairs = 24
    val deck  = Emojis.take(pairs).flatMap(e => List(e, e))
    Printable.render(
      title = _.offline.memory.printTitle,
      body = div(
        cls := "mem-print-sheet",
        deck.map(emoji => div(cls := "mem-print-card", emoji))
      )
    )

  private def renderPlay(): HtmlElement =
    ModeChooser.render(
      activityId = id,
      prefix = List("in-app"),
      heading = _.memory.chooseVariant,
      modes = variants.map { v =>
        Mode(
          id = v.id,
          label = v.nameKey,
          description = Some(v.descKey),
          testIdPrefix = "variant",
          render = () => gameView(v)
        )
      }
    )

  private val MismatchRevealMs = 900

  private def gameView(v: Variant): HtmlElement =
    val state = Var(initial(v))
    var pendingFlipBack: Option[js.timers.SetTimeoutHandle] = None

    def scheduleFlipBack(a: Int, b: Int): Unit =
      pendingFlipBack = Some(js.timers.setTimeout(MismatchRevealMs) {
        pendingFlipBack = None
        val cur = state.now()
        cur.phase match
          case Phase.Mismatch(`a`, `b`) =>
            val cleared = cur.cards
              .updated(a, cur.cards(a).copy(faceUp = false))
              .updated(b, cur.cards(b).copy(faceUp = false))
            state.set(cur.copy(cards = cleared, phase = Phase.Idle, turn = cur.turn.other))
          case _ => ()
      })

    def tap(i: Int): Unit =
      val cur = state.now()
      val c = cur.cards(i)
      if cur.finished || c.faceUp || c.matched then return
      cur.phase match
        case Phase.Mismatch(_, _) => ()
        case Phase.Idle =>
          val newCards = cur.cards.updated(i, c.copy(faceUp = true))
          state.set(cur.copy(cards = newCards, phase = Phase.OneFlipped(i)))
        case Phase.OneFlipped(prev) =>
          val newCards = cur.cards.updated(i, c.copy(faceUp = true))
          val prevCard = newCards(prev)
          if prevCard.emoji == c.emoji then
            val matched = newCards
              .updated(prev, prevCard.copy(matched = true))
              .updated(i, newCards(i).copy(matched = true))
            val newScores = cur.scores.updated(cur.turn, cur.scores(cur.turn) + 1)
            state.set(cur.copy(cards = matched, phase = Phase.Idle, scores = newScores))
          else
            state.set(cur.copy(cards = newCards, phase = Phase.Mismatch(prev, i)))
            scheduleFlipBack(prev, i)

    def reset(): Unit = state.set(initial(v))

    val activeMark = state.signal.map(st => if st.finished then None else Some(st.turn))

    val statusSignal: Signal[String] =
      state.signal.combineWith(AppState.strings).map { (st, str) =>
        st.winner match
          case Some(w) => s"${w.labelKey(str)} — ${str.memory.wins}"
          case None if st.finished => str.common.draw
          case None => s"${st.turn.labelKey(str)} — ${str.memory.turn}"
      }

    div(
      cls := "player-page stack-lg",
      onUnmountCallback(_ => pendingFlipBack.foreach(js.timers.clearTimeout)),
      cls("player-page--p1") <-- activeMark.map(_.contains(Player.P1)),
      cls("player-page--p2") <-- activeMark.map(_.contains(Player.P2)),
      div(
        cls := "center",
        div(cls := "player-badge", child.text <-- statusSignal)
      ),
      div(
        cls := "row",
        styleAttr := "justify-content: center; gap: var(--space-5);",
        scoreView(Player.P1, state.signal),
        scoreView(Player.P2, state.signal)
      ),
      div(
        cls := "board mem-board",
        styleAttr := s"grid-template-columns: repeat(${v.cols}, 1fr);",
        (0 until v.pairs * 2).map { i =>
          val cardSig = state.signal.map(_.cards(i)).distinct
          div(
            cls := "cell cell--btn mem-card",
            cls("mem-card--up")      <-- cardSig.map(c => c.faceUp || c.matched),
            cls("mem-card--matched") <-- cardSig.map(_.matched),
            child.text <-- cardSig.map(c => if c.faceUp || c.matched then c.emoji else ""),
            onClick --> (_ => tap(i))
          )
        }
      ),
      div(
        cls := "row no-print",
        styleAttr := "justify-content: center;",
        Components.ghost(s(_.memory.changeVariant), Routing.go(Page.Activity(id, List("in-app")))),
        Components.replayButton(s(_.common.playAgain), reset(), state.signal.map(_.finished))
      )
    )

  private def scoreView(p: Player, stateSig: Signal[State]): HtmlElement =
    val isTurn = stateSig.map(st => !st.finished && st.turn == p)
    div(
      cls := s"mem-score mem-score--p${p.num}",
      cls("is-active") <-- isTurn,
      div(cls := "mem-score__label", child.text <-- s(p.labelKey)),
      div(cls := "mem-score__value", child.text <-- stateSig.map(_.scores(p).toString))
    )
