package tandu.activities

import com.raquo.laminar.api.L.*
import org.scalajs.dom
import tandu.AppState
import tandu.i18n.Strings
import tandu.ui.{Components, PaintSurface}
import tandu.ui.Components.s

/** Draw & Guess — pass-and-play pictionary. One child (the artist) peeks at a
  * secret word and draws it on the shared screen; everyone else shouts
  * guesses. The device's job is to deal secret words fairly and keep them
  * hidden: the word only shows on explicit, artist-only screens (hand-off →
  * preview) and behind a hold-to-peek button while drawing. Guess judging is
  * verbal — the kids decide when it's been guessed, the app just celebrates.
  */
object DrawAndGuess extends Activity:
  val id = "draw-and-guess"
  def name(s: Strings): String = s.drawAndGuess.name
  def description(s: Strings): String = s.drawAndGuess.description
  val minPlayers: Int = 2
  val maxPlayers: Int = Int.MaxValue
  val glyph: String = "🎨"
  val tint: String = "rose"

  private enum Phase:
    case Handoff, Preview, Drawing, Reveal

  def render(): HtmlElement =
    val roller = new Roller[String]
    val word   = Var("")
    val phase  = Var(Phase.Handoff)

    def showWord(): Unit =
      word.set(roller.next(DrawAndGuessBank.wordsFor(AppState.lang.now())))
      phase.set(Phase.Preview)

    // Drawing and Reveal share one DOM subtree so the finished drawing stays
    // on screen at the reveal. Collapsing them to one stage (plus .distinct)
    // keeps `child <--` from rebuilding the canvas on that transition, while
    // each new round still mounts a fresh, blank surface.
    val stage = phase.signal
      .map {
        case Phase.Handoff => Phase.Handoff
        case Phase.Preview => Phase.Preview
        case _             => Phase.Drawing
      }
      .distinct

    div(
      cls := "draw-guess stack",
      child <-- stage.map {
        case Phase.Handoff => handoffPanel(showWord)
        case Phase.Preview => previewPanel(word, () => phase.set(Phase.Drawing))
        case _             => drawPanel(word, phase)
      }
    )

  /** Pass the device to this round's artist; nobody else may look yet. */
  private def handoffPanel(showWord: () => Unit): HtmlElement =
    Components.card(
      p(cls := "center", child.text <-- s(_.drawAndGuess.handoff)),
      button(
        cls := "btn btn--hero btn--block",
        child.text <-- s(_.drawAndGuess.showWord),
        onClick --> (_ => showWord())
      )
    )

  /** Artist-only screen: memorize the word before the audience watches. */
  private def previewPanel(word: Var[String], startDrawing: () => Unit): HtmlElement =
    Components.card(
      p(cls := "muted center", child.text <-- s(_.drawAndGuess.yourWordIs)),
      div(cls := "draw-guess__word", child.text <-- word.signal),
      p(cls := "muted center", child.text <-- s(_.drawAndGuess.secretHint)),
      button(
        cls := "btn btn--hero btn--block",
        child.text <-- s(_.drawAndGuess.startDrawing),
        onClick --> (_ => startDrawing())
      )
    )

  private def drawPanel(word: Var[String], phase: Var[Phase]): HtmlElement =
    val revealed = phase.signal.map(_ == Phase.Reveal)
    div(
      cls := "stack",
      child <-- revealed.map {
        case false => drawingControls(word, phase)
        case true  => revealBanner(word, phase)
      },
      PaintSurface.render(),
      // The "nobody will ever get this" exit, tucked under the canvas where it
      // doesn't compete with the happy path.
      button(
        cls := "btn btn--ghost btn--block",
        hidden <-- revealed,
        child.text <-- s(_.drawAndGuess.giveUp),
        onClick --> (_ => phase.set(Phase.Reveal))
      )
    )

  private def drawingControls(word: Var[String], phase: Var[Phase]): HtmlElement =
    // Hold-to-peek: the word shows only while the button is pressed, so a
    // guesser glancing at the screen never catches it parked there.
    val peeking = Var(false)
    div(
      cls := "row",
      button(
        cls := "btn btn--ghost draw-guess__peek",
        cls("is-peeking") <-- peeking.signal,
        onPointerDown --> (_ => peeking.set(true)),
        onPointerUp --> (_ => peeking.set(false)),
        onPointerLeave --> (_ => peeking.set(false)),
        child.text <-- peeking.signal
          .combineWith(word.signal)
          .combineWith(AppState.strings)
          .map((p, w, str) => if p then w else str.drawAndGuess.peek)
      ),
      button(
        cls := "btn draw-guess__guessed",
        child.text <-- s(_.drawAndGuess.guessed),
        onClick --> { _ =>
          cheer()
          phase.set(Phase.Reveal)
        }
      )
    )

  private def revealBanner(word: Var[String], phase: Var[Phase]): HtmlElement =
    div(
      cls := "stack",
      p(cls := "muted center", child.text <-- s(_.drawAndGuess.theWordWas)),
      div(cls := "draw-guess__word", child.text <-- word.signal),
      button(
        cls := "btn btn--hero btn--block",
        child.text <-- s(_.drawAndGuess.nextRound),
        onClick --> (_ => phase.set(Phase.Handoff))
      )
    )

  /** Three quick ascending notes — a tiny fanfare for a correct guess. Its own
    * throwaway AudioContext, mirroring the Timer's beep. */
  private def cheer(): Unit =
    try
      val ctx = new dom.AudioContext()
      val t0  = ctx.currentTime
      List(523.25, 659.25, 783.99).zipWithIndex.foreach { (freq, i) =>
        val osc  = ctx.createOscillator()
        val gain = ctx.createGain()
        osc.`type` = "triangle"
        osc.frequency.value = freq
        osc.connect(gain)
        gain.connect(ctx.destination)
        val t = t0 + i * 0.12
        gain.gain.value = 0.0001
        gain.gain.exponentialRampToValueAtTime(0.25, t + 0.02)
        gain.gain.exponentialRampToValueAtTime(0.0001, t + 0.35)
        osc.start(t)
        osc.stop(t + 0.4)
      }
    catch case _: Throwable => ()
