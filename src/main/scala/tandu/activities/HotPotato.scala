package tandu.activities

import com.raquo.laminar.api.L.*
import org.scalajs.dom
import tandu.{AppState, Kind}
import tandu.activities.freezedance.SynthSource
import tandu.i18n.Strings
import tandu.ui.Components.s

import scala.scalajs.js
import scala.util.Random

/** Hot Potato — the device's whole job is an **unpredictable, impartial stop**.
  * Music plays while the kids pass an object around; at a moment nobody can
  * anticipate (and nobody can be accused of rigging) the music cuts and a buzzer
  * sounds — whoever's holding the potato is caught. Deliberately tiny: no
  * visible countdown (that would telegraph the cut) and no player tracking (the
  * kids manage who's out themselves). Reuses [[SynthSource]] for an offline,
  * zero-bundle music bed; the buzzer is the only bespoke sound. */
object HotPotato extends Activity:
  val id = "hot-potato"
  def name(s: Strings): String = s.hotPotato.name
  def description(s: Strings): String = s.hotPotato.description
  val minPlayers: Int = 3
  val maxPlayers: Int = Int.MaxValue
  val handsFree: Boolean = true
  override val kind: Kind = Kind.Move
  val glyph: String = "🥔"
  val tint: String = "vermilion"

  private enum Phase:
    case Idle, Passing, Caught

  // Random span before the cut, in ms. Wide enough that the stop can't be
  // counted out; short enough that a round stays tense.
  private val MinMs = 7000
  private val MaxMs = 22000

  def render(): HtmlElement =
    val rng   = new Random()
    val synth = SynthSource(rng)
    val phase = Var(Phase.Idle)

    var timer: Option[js.timers.SetTimeoutHandle] = None
    def clearTimer(): Unit =
      timer.foreach(js.timers.clearTimeout)
      timer = None

    def start(): Unit =
      phase.set(Phase.Passing)
      synth.play()
      clearTimer()
      timer = Some(js.timers.setTimeout(MinMs + rng.nextInt(MaxMs - MinMs))(catchIt()))
    def catchIt(): Unit =
      clearTimer()
      synth.pause()
      buzz()
      phase.set(Phase.Caught)
    def stop(): Unit =
      clearTimer()
      synth.pause()
      phase.set(Phase.Idle)

    val idle: Signal[Boolean] = phase.signal.map(_ == Phase.Idle)

    div(
      cls := "potato stack-lg",
      onUnmountCallback { _ =>
        clearTimer()
        synth.dispose()
      },
      // Instruction stands in for the setup panel; gone once a round is running.
      p(
        cls := "muted center potato-instruction",
        hidden <-- idle.map(!_),
        child.text <-- s(_.hotPotato.instruction)
      ),
      div(
        cls := "potato-stage",
        hidden <-- idle,
        cls("is-passing") <-- phase.signal.map(_ == Phase.Passing),
        cls("is-caught")  <-- phase.signal.map(_ == Phase.Caught),
        div(
          cls := "potato-stage__emoji",
          child.text <-- phase.signal.map { case Phase.Caught => "💥"; case _ => "🥔" }
        ),
        div(
          cls := "potato-stage__cue",
          child.text <-- phase.signal.combineWith(AppState.strings).map { (p, str) =>
            p match
              case Phase.Caught => str.hotPotato.caughtCue
              case _            => str.hotPotato.passCue
          }
        )
      ),
      div(
        cls := "potato-actions center",
        child <-- phase.signal.map {
          case Phase.Idle =>
            button(
              cls := "btn btn--hero btn--block",
              child.text <-- s(_.hotPotato.start),
              onClick --> (_ => start())
            )
          case Phase.Passing =>
            button(
              cls := "btn btn--ghost btn--block",
              child.text <-- s(_.hotPotato.stop),
              onClick --> (_ => stop())
            )
          case Phase.Caught =>
            button(
              cls := "btn btn--hero btn--block",
              child.text <-- s(_.hotPotato.again),
              onClick --> (_ => start())
            )
        }
      )
    )

  /** A short, harsh "you're caught" buzzer — a sawtooth tone that drops in pitch.
    * Its own throwaway AudioContext, mirroring the Timer's beep. */
  private def buzz(): Unit =
    try
      val ctx  = new dom.AudioContext()
      val osc  = ctx.createOscillator()
      val gain = ctx.createGain()
      osc.`type` = "sawtooth"
      osc.connect(gain)
      gain.connect(ctx.destination)
      val t = ctx.currentTime
      osc.frequency.setValueAtTime(180, t)
      osc.frequency.exponentialRampToValueAtTime(80, t + 0.55)
      gain.gain.value = 0.0001
      gain.gain.exponentialRampToValueAtTime(0.35, t + 0.02)
      gain.gain.setValueAtTime(0.35, t + 0.5)
      gain.gain.exponentialRampToValueAtTime(0.0001, t + 0.72)
      osc.start(t)
      osc.stop(t + 0.75)
    catch case _: Throwable => ()
