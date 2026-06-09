package tandu.activities

import com.raquo.laminar.api.L.*
import tandu.{AppState, Kind}
import tandu.activities.freezedance.*
import tandu.i18n.Strings
import tandu.ui.Components.s

import scala.scalajs.js
import scala.util.Random

/** Freeze Dance — the floor's only hands-free game. Music plays, everyone
  * dances; at a random moment it cuts dead and the dancers must freeze, then it
  * starts again. The mechanic is source-agnostic: the engine only calls
  * `play()` / `pause()` on whichever [[freezedance.DanceSource]] is selected.
  * Two sources: the built-in randomised synth, and "Songs" — one-tap suggested
  * YouTube hits, a pasted link, or a device file, all in one panel. */
object FreezeDance extends Activity:
  val id = "freeze-dance"
  def name(s: Strings): String = s.freezeDance.name
  def description(s: Strings): String = s.freezeDance.description
  val minPlayers: Int = 1
  val maxPlayers: Int = 8
  override val kind: Kind = Kind.Move
  val glyph: String = "🕺"
  val tint: String = "plum"

  /** Built-in CC0 loops: drop files in `public/music/` and list them here. They
    * appear as one-tap cards in the Songs panel alongside the YouTube picks, are
    * excluded from the PWA precache (see `vite.config.js`) and runtime-cached for
    * offline once played. Empty by default. */
  val tracks: List[Track] = Nil

  /** Curated, kid-appropriate dance songs offered as one-tap cards — the
    * low-friction path, since most people don't keep music files around.
    *
    * Deliberately **continuous** upbeat tracks, NOT "freeze dance" songs: this
    * game already provides the freeze by cutting the music at random, so a song
    * with its own freezes baked in would fight the engine. The two royalty-free
    * instrumentals are the safest picks (no lyrics, no cues, no ads). IDs may rot
    * (videos go private/removed); the paste-a-link box is the fallback. */
  private val songSuggestions: List[Suggestion] = List(
    Suggestion("Baby Shark Dance", "Pinkfong", Pick.Yt("XqZsoesa55w")),
    Suggestion("I Like to Move It", "Reel 2 Real", Pick.Yt("vuo8kD5zF5I")),
    Suggestion("Happy", "Pharrell Williams", Pick.Yt("ZbZSe6N_BXs")),
    Suggestion("Dance Party · instrumental", "AShamaluevMusic", Pick.Yt("EAtbLyv6Ixo"))
  )

  private enum Phase:
    case Idle, Dancing, Frozen

  private enum Source(val labelKey: Strings => String):
    case Synth extends Source(_.freezeDance.srcSynth)
    case Songs extends Source(_.freezeDance.srcSongs)

  // Random dance / freeze spans, in ms. Dance long enough to get moving; freeze
  // long enough to wobble, short enough not to get bored.
  private val DanceMin  = 5000
  private val DanceMax  = 13000
  private val FreezeMin = 2500
  private val FreezeMax = 4500

  def render(): HtmlElement =
    val rng   = new Random()
    val synth = SynthSource(rng)
    // Bundled CC0 loops (if any) join the YouTube picks as one-tap cards.
    val bundled  = tracks.map(t => Suggestion(t.title, "Built-in", Pick.Audio(s"/music/${t.file}")))
    val external = ExternalSource(songSuggestions ::: bundled)

    val available: List[Source] = List(Source.Synth, Source.Songs)

    def sourceFor(k: Source): DanceSource = k match
      case Source.Synth => synth
      case Source.Songs => external

    val selected = Var(Source.Synth)
    val phase    = Var(Phase.Idle)

    var timer: Option[js.timers.SetTimeoutHandle] = None
    def clearTimer(): Unit =
      timer.foreach(js.timers.clearTimeout)
      timer = None
    def randMs(lo: Int, hi: Int): Int = lo + rng.nextInt(hi - lo)
    def current: DanceSource = sourceFor(selected.now())

    def scheduleFreeze(): Unit =
      clearTimer()
      timer = Some(js.timers.setTimeout(randMs(DanceMin, DanceMax))(doFreeze()))
    def doFreeze(): Unit =
      current.pause()
      phase.set(Phase.Frozen)
      clearTimer()
      timer = Some(js.timers.setTimeout(randMs(FreezeMin, FreezeMax))(doResume()))
    def doResume(): Unit =
      phase.set(Phase.Dancing)
      current.play()
      scheduleFreeze()

    def start(): Unit =
      // Phase first so the stage (and the YouTube iframe) is visible before
      // play() loads the embed — a hidden iframe won't autoplay with sound.
      phase.set(Phase.Dancing)
      current.play()
      scheduleFreeze()
    def stop(): Unit =
      clearTimer()
      current.pause()
      current.reset()
      phase.set(Phase.Idle)
    def switchTo(k: Source): Unit =
      if selected.now() != k then
        stop()
        selected.set(k)

    // Start is enabled only once the *currently selected* source has a track —
    // flatMapSwitch re-subscribes to whichever source's `ready` is in play.
    val readySig: Signal[Boolean] = selected.signal.flatMapSwitch(k => sourceFor(k).ready)
    val idle: Signal[Boolean]     = phase.signal.map(_ == Phase.Idle)
    // Show the active source's video (if it has one) in the stage — asked of the
    // source via the abstraction, not a concrete instance.
    val showVideo: Signal[Boolean] = selected.signal.flatMapSwitch(k => sourceFor(k).isVideo)

    // Setup: tabs + the chosen source's panel. Hidden (not unmounted, so audio
    // keeps playing) once the game starts, leaving a clean stage + Stop.
    val setup =
      div(
        cls := "freeze-setup no-print",
        hidden <-- idle.map(!_),
        p(cls := "freeze-instruction muted center", child.text <-- s(_.freezeDance.instruction)),
        div(
          cls := "center",
          div(
            cls := "pill-toggle",
            available.map { k =>
              button(
                cls := "pill-btn",
                cls("is-active") <-- selected.signal.map(_ == k),
                child.text <-- AppState.strings.map(k.labelKey),
                onClick --> (_ => switchTo(k))
              )
            }
          )
        ),
        div(cls := "freeze-source-panel", child <-- selected.signal.map(k => sourceFor(k).controls))
      )

    // Stage: the focal play surface, shown only while dancing/frozen. A YouTube
    // pick plays here (pausing freezes the frame); otherwise the emoji animates.
    val stage =
      div(
        cls := "freeze-stage",
        hidden <-- idle,
        cls("is-dancing") <-- phase.signal.map(_ == Phase.Dancing),
        cls("is-frozen")  <-- phase.signal.map(_ == Phase.Frozen),
        cls("has-video")  <-- showVideo,
        div(
          cls := "freeze-video",
          hidden <-- showVideo.map(!_),
          child <-- selected.signal.map(k => sourceFor(k).videoNode.getOrElse(emptyNode)),
          p(cls := "freeze-hint muted center freeze-yt-note", child.text <-- s(_.freezeDance.ytAdsNote))
        ),
        div(cls := "freeze-stage__emoji", hidden <-- showVideo,
          child.text <-- phase.signal.map { case Phase.Frozen => "🧊"; case _ => "🕺" }),
        div(
          cls := "freeze-stage__cue",
          child.text <-- phase.signal.combineWith(AppState.strings).map { (p, str) =>
            p match
              case Phase.Frozen => str.freezeDance.freezeCue
              case _            => str.freezeDance.danceCue
          }
        )
      )

    div(
      cls := "freeze stack-lg",
      onUnmountCallback { _ =>
        clearTimer()
        List(synth, external).foreach(_.dispose())
      },
      setup,
      stage,
      div(
        cls := "freeze-actions center no-print",
        child <-- idle.map {
          case true =>
            button(
              cls := "btn btn--hero btn--block",
              disabled <-- readySig.map(!_),
              child.text <-- s(_.freezeDance.start),
              onClick --> (_ => start())
            )
          case false =>
            button(
              cls := "btn btn--block",
              child.text <-- s(_.freezeDance.stop),
              onClick --> (_ => stop())
            )
        }
      )
    )
