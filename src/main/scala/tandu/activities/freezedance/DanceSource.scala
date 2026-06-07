package tandu.activities.freezedance

import com.raquo.laminar.api.L.*
import org.scalajs.dom
import tandu.AppState
import tandu.ui.Components.{banner, s}

import scala.scalajs.js
import scala.util.Random

/** One way to feed music to the Freeze Dance engine. The engine only ever needs
  * two verbs — [[play]] to dance, [[pause]] to freeze — plus a [[ready]] gate and
  * a [[controls]] panel where the source sets itself up (and parks its hidden
  * `<audio>` / `<iframe>` element). [[dispose]] releases resources on unmount.
  *
  * Adding a new backend (e.g. Spotify Web Playback SDK) means writing one more
  * implementation and listing it in `FreezeDance.Source` — nothing else changes. */
/** A short row of links to free music sources, shown on the Upload and Link
  * panels so a parent without a track to hand can find one. These are discovery
  * pointers only — we don't bundle or rehost anything from them, so each site's
  * own terms are between the parent and the site. Site names aren't translated. */
object MusicLinks:
  private val sites: List[(String, String)] = List(
    "Pixabay"     -> "https://pixabay.com/music/",
    "FreeToUse"   -> "https://freetouse.com/music",
    "Chosic"      -> "https://www.chosic.com/free-music/children/",
    "OpenGameArt" -> "https://opengameart.org/content/cc0-music-0"
  )

  def render(): HtmlElement =
    val links = sites.zipWithIndex.flatMap { case ((label, url), i) =>
      val sep = if i > 0 then Seq(span(cls := "freeze-links__sep", " · ")) else Seq.empty
      sep :+ a(href := url, target := "_blank", rel := "noopener noreferrer", label)
    }
    p(
      cls := "freeze-hint muted center freeze-links",
      span(child.text <-- s(_.freezeDance.freeMusicLabel)),
      span(" "),
      links
    )

trait DanceSource:
  /** Begin or resume playback. */
  def play(): Unit
  /** Freeze: a temporary pause that [[play]] resumes from. */
  def pause(): Unit
  /** Full stop back to idle — distinct from a freeze. Lets a source forget any
    * "already started" state so the next [[play]] is a clean (re)start. */
  def reset(): Unit = ()
  def dispose(): Unit

  /** True once a track is selected and playable — gates the Start button. */
  def ready: Signal[Boolean]

  /** The source's own setup UI. Returns a stable element (built once) so it can
    * be mounted/unmounted as the picker switches without losing its media node. */
  def controls: HtmlElement

  /** A visual the engine mounts in the play stage while this source plays (e.g.
    * a YouTube embed — pausing it freezes the frame). None → the engine shows
    * its own emoji stage instead. */
  def videoNode: Option[HtmlElement] = None

  /** Whether [[videoNode]] should be shown right now (vs. the emoji stage). */
  def isVideo: Signal[Boolean] = Val(false)

// ---------------------------------------------------------------------------
// Synth — procedural, zero-bundle, offline. Always available, no setup.
// ---------------------------------------------------------------------------

/** A small generative dance track built live in the Web Audio graph: synth
  * drums, a bassline, a chord arpeggio and a melody. It's fully auto-generated,
  * so every run is a different tune — and it **re-rolls on every unpause**, so
  * the dancers get a fresh key, tempo, chord progression and groove each time
  * the music returns after a freeze. What keeps it musical rather than random
  * noise: a tight **lookahead scheduler** (a 25ms timer that queues steps against
  * the audio clock), and a generator that only ever draws diatonic chords and
  * scale tones. Costs no bundle weight, needs no licence, works offline; the
  * freeze snaps the master gain to silence in a few milliseconds. */
final class SynthSource(rng: Random) extends DanceSource:
  import SynthSource.*

  private var ctx: dom.AudioContext = null
  private var master: dom.GainNode = null
  private var noise: dom.AudioBuffer = null
  private var sched: Option[js.timers.SetIntervalHandle] = None

  private var step         = 0             // global sixteenth-note counter
  private var nextNoteTime = 0.0           // audio-clock time of the next step
  private var track: Arrangement = genTrack(rng) // the current randomised arrangement
  private var lead: Array[Int] = genLead()  // 64 steps; midi note or REST

  private val Lookahead = 0.10             // schedule this far ahead, seconds
  private val TickMs    = 25               // scheduler wake interval

  /** Draw a brand-new arrangement (key, tempo, progression, grooves) and a
    * fresh melody for it. Called on every play() — i.e. on each unpause. */
  private def reroll(): Unit =
    track = genTrack(rng)
    lead  = genLead()

  private def ensure(): Unit =
    if ctx == null then
      ctx = new dom.AudioContext()
      // master -> soft limiter -> out, so stacked voices glue and never clip.
      val comp = ctx.createDynamicsCompressor()
      comp.threshold.value = -10
      comp.ratio.value     = 6
      comp.attack.value    = 0.003
      comp.release.value   = 0.18
      master = ctx.createGain()
      master.gain.value = 0.0001
      master.connect(comp)
      comp.connect(ctx.destination)
      noise = makeNoise(ctx, rng)

  // ---- voice primitives (throwaway nodes; they stop and GC themselves) ----

  private def osc(kind: String, freq: Double): dom.OscillatorNode =
    val o = ctx.createOscillator(); o.`type` = kind; o.frequency.value = freq; o
  private def filter(kind: String, freq: Double, q: Double): dom.BiquadFilterNode =
    val f = ctx.createBiquadFilter(); f.`type` = kind; f.frequency.value = freq; f.Q.value = q; f
  private def noiseSrc(): dom.AudioBufferSourceNode =
    val n = ctx.createBufferSource(); n.buffer = noise; n

  /** A percussive amp envelope: fast attack, exponential decay to silence. */
  private def env(t: Double, peak: Double, dur: Double, attack: Double): dom.GainNode =
    val g = ctx.createGain()
    g.gain.setValueAtTime(0.0001, t)
    g.gain.exponentialRampToValueAtTime(peak, t + attack)
    g.gain.exponentialRampToValueAtTime(0.0001, t + dur)
    g

  // ---- drums ----

  private def kick(t: Double): Unit =
    val o = osc("sine", 150)
    o.frequency.setValueAtTime(150, t)
    o.frequency.exponentialRampToValueAtTime(48, t + 0.11)
    val g = env(t, 0.95, 0.30, 0.002)
    o.connect(g); g.connect(master)
    o.start(t); o.stop(t + 0.33)

  private def snare(t: Double): Unit =
    val n = noiseSrc(); val hp = filter("highpass", 1700, 0.8); val g = env(t, 0.4, 0.18, 0.001)
    n.connect(hp); hp.connect(g); g.connect(master)
    n.start(t); n.stop(t + 0.2)
    val o = osc("triangle", 190); val bg = env(t, 0.22, 0.11, 0.001) // a little body
    o.connect(bg); bg.connect(master)
    o.start(t); o.stop(t + 0.13)

  private def hat(t: Double, open: Boolean): Unit =
    val dur = if open then 0.18 else 0.035
    val n = noiseSrc(); val hp = filter("highpass", 8000, 0.9)
    val g = env(t, if open then 0.20 else 0.14, dur, 0.001)
    n.connect(hp); hp.connect(g); g.connect(master)
    n.start(t); n.stop(t + dur + 0.02)

  // ---- pitched voices ----

  private def bass(t: Double, midi: Int): Unit =
    val o = osc("sawtooth", hz(midi)); val lp = filter("lowpass", 240, 7)
    lp.frequency.setValueAtTime(720, t)               // a little filter pluck
    lp.frequency.exponentialRampToValueAtTime(220, t + 0.12)
    val g = env(t, 0.5, track.stepDur * 1.7, 0.006)
    o.connect(lp); lp.connect(g); g.connect(master)
    o.start(t); o.stop(t + track.stepDur * 1.8)

  private def arp(t: Double, midi: Int): Unit =
    val o = osc("square", hz(midi)); val lp = filter("lowpass", 2600, 0.7)
    val g = env(t, 0.10, 0.18, 0.004)
    o.connect(lp); lp.connect(g); g.connect(master)
    o.start(t); o.stop(t + 0.2)

  private def melody(t: Double, midi: Int): Unit =
    val o1 = osc("triangle", hz(midi))
    val o2 = osc("triangle", hz(midi)); o2.detune.value = 8 // shimmer
    val end = t + track.stepDur * 2.0
    val g = env(t, 0.17, track.stepDur * 1.9, 0.012)
    o1.connect(g); o2.connect(g); g.connect(master)
    o1.start(t); o2.start(t); o1.stop(end); o2.stop(end)

  // ---- sequencer ----

  /** A 4-bar (64-step) melody over the current progression: a rhythm per bar,
    * chord tones on the strong beats, scale steps in between — always consonant
    * with the chord under it. */
  private def genLead(): Array[Int] =
    val out   = Array.fill(64)(REST)
    val scale = track.leadScale
    var idx = 0
    var bar = 0
    while bar < 4 do
      val motif = Motifs(rng.nextInt(Motifs.length))
      val tones = track.prog(bar).lead
      for onset <- motif do
        val pitch =
          if onset % 4 == 0 then
            val p = tones(rng.nextInt(tones.length)); idx = nearestIdx(scale, p); p
          else
            idx = math.max(0, math.min(scale.length - 1, idx + (rng.nextInt(3) - 1)))
            scale(idx)
        out(bar * 16 + onset) = pitch
      bar += 1
    out

  /** Lay down everything the current `track` fires on global sixteenth-step `s`
    * at audio time `t`. */
  private def scheduleStep(s: Int, t: Double): Unit =
    val bar   = (s / 16) % 4
    val i     = s % 16
    val chord = track.prog(bar)

    if track.kickSteps.contains(i) then kick(t)
    if track.snareSteps.contains(i) then snare(t)
    if i % track.hatStep == 0 then hat(t, open = false)
    if track.openHats.contains(i) then hat(t, open = true)
    if bar == 3 && i >= 12 && i % 2 == 1 then snare(t)          // fill into the turnaround

    track.bassMode match
      case 0 =>                                                 // disco octave offbeats
        if i % 4 == 2 then bass(t, chord.bass + (if (i / 4) % 2 == 0 then 0 else 12))
        else if i == 0 then bass(t, chord.bass)
      case 1 =>                                                 // root on the beats
        if i % 4 == 0 then bass(t, chord.bass)
      case _ =>                                                 // driving eighths
        if i % 2 == 0 then bass(t, chord.bass)

    if i % track.arpStep == 0 then                              // arpeggiate the chord
      val n   = chord.arp.length
      val seq = i / track.arpStep
      val idx = track.arpDir match
        case 0 => seq % n                                       // up
        case 1 => (n - 1) - (seq % n)                           // down
        case _ => val p = seq % (2 * n - 2); if p < n then p else (2 * n - 2) - p // up-down
      arp(t, chord.arp(idx))

    val note = lead(s % lead.length)
    if note != REST then melody(t, note)

  private def schedulerTick(): Unit =
    if ctx != null then
      while nextNoteTime < ctx.currentTime + Lookahead do
        scheduleStep(step, nextNoteTime)
        step += 1
        nextNoteTime += track.stepDur
        if step % lead.length == 0 then lead = genLead()        // fresh melody each loop

  def play(): Unit =
    ensure()
    reroll()                                  // a different tune on every (re)start
    val now = ctx.currentTime
    master.gain.cancelScheduledValues(now)
    master.gain.setTargetAtTime(0.8, now, 0.012)
    step = 0                                  // start the new arrangement at bar 0
    nextNoteTime = now + 0.06
    if sched.isEmpty then
      sched = Some(js.timers.setInterval(TickMs)(schedulerTick()))

  def pause(): Unit =
    sched.foreach(js.timers.clearInterval)
    sched = None
    if master != null then
      val now = ctx.currentTime
      master.gain.cancelScheduledValues(now)
      master.gain.setTargetAtTime(0.0001, now, 0.006) // snap to silence
    end if

  def dispose(): Unit =
    pause()
    if ctx != null then
      try { val _ = ctx.asInstanceOf[js.Dynamic].close() }
      catch case _: Throwable => ()
    ctx = null
    master = null

  def ready: Signal[Boolean] = Val(true)

  val controls: HtmlElement =
    p(cls := "freeze-hint muted center", child.text <-- s(_.freezeDance.synthHint))

object SynthSource:
  private val REST = Int.MinValue

  private def hz(n: Int): Double = 440.0 * math.pow(2.0, (n - 69) / 12.0)

  /** A chord in the loop: a bass root, mid-octave arpeggio tones, and the
    * lead-octave notes a melody may land on for that bar. */
  private final case class Chord(bass: Int, arp: Vector[Int], lead: Vector[Int])

  /** A complete randomised arrangement — everything `play()` needs to render a
    * tune. Re-rolled on every unpause. */
  private final case class Arrangement(
      stepDur: Double,          // seconds per sixteenth (i.e. the tempo)
      prog: Vector[Chord],      // 4 chords, realised in this track's key
      leadScale: Vector[Int],   // scale tones the melody walks, in the lead octave
      kickSteps: Set[Int],
      snareSteps: Set[Int],
      hatStep: Int,             // closed hat every Nth sixteenth (1 = 16ths, 2 = 8ths)
      openHats: Set[Int],
      bassMode: Int,            // 0 octave-offbeat, 1 on-beats, 2 driving eighths
      arpStep: Int,             // arp note every Nth sixteenth
      arpDir: Int               // 0 up, 1 down, 2 up-down
  )

  // Major scale as semitone offsets; scaleNote extends it across octaves so we
  // can stack diatonic thirds past the top of the scale.
  private val Major = Array(0, 2, 4, 5, 7, 9, 11)
  private def scaleNote(deg: Int): Int =
    Major(((deg % 7) + 7) % 7) + 12 * Math.floorDiv(deg, 7)
  private def triad(deg: Int): Vector[Int] = Vector(deg, deg + 2, deg + 4).map(scaleNote)

  // Chord progressions as scale degrees (0 = I … 5 = vi). All diatonic, all happy.
  private val Progressions: Vector[Vector[Int]] = Vector(
    Vector(0, 4, 5, 3), // I  V  vi IV
    Vector(5, 3, 0, 4), // vi IV I  V
    Vector(0, 5, 3, 4), // I  vi IV V
    Vector(0, 3, 4, 3), // I  IV V  IV
    Vector(0, 3, 0, 4), // I  IV I  V
    Vector(5, 3, 4, 4), // vi IV V  V
    Vector(0, 4, 3, 4)  // I  V  IV V
  )

  private val KickPatterns: Vector[Set[Int]] = Vector(
    Set(0, 4, 8, 12),           // four-on-the-floor
    Set(0, 4, 8, 12, 14),       // + a pickup
    Set(0, 4, 6, 8, 12),
    Set(0, 4, 8, 10, 12),
    Set(0, 3, 6, 8, 11, 14)     // syncopated, but still anchored on 0/8
  )
  private val SnarePatterns: Vector[Set[Int]] = Vector(
    Set(4, 12), Set(4, 12), Set(4, 12, 15) // backbeat-dominant
  )
  private val OpenHatChoices: Vector[Set[Int]] = Vector(
    Set.empty, Set(14), Set(6, 14), Set(2, 6, 10, 14)
  )

  // Sixteenth-step onset masks; one is drawn per bar to shape a melodic phrase.
  private val Motifs: Vector[Vector[Int]] = Vector(
    Vector(0, 3, 6, 8, 11, 14),
    Vector(0, 2, 4, 8, 10, 12),
    Vector(0, 4, 7, 8, 12, 15),
    Vector(2, 6, 8, 10, 14)
  )

  /** Draw a fresh arrangement: a random key, tempo and progression, plus random
    * drum / bass / arp grooves. Realised so every note stays diatonic. */
  private def genTrack(rng: Random): Arrangement =
    val key  = -2 + rng.nextInt(7)                 // transpose -2..+4 semitones
    val degs = Progressions(rng.nextInt(Progressions.length))
    val prog = degs.map { d =>
      val t = triad(d)
      Chord(
        bass = 36 + key + scaleNote(d),
        arp  = t.map(o => 60 + key + o),
        lead = t.map(o => 72 + key + o)
      )
    }
    val leadScale = Vector(0, 2, 4, 5, 7, 9, 11, 12, 14).map(72 + key + _)
    val bpm = 112 + rng.nextInt(26)                // 112..137 BPM
    Arrangement(
      stepDur   = 60.0 / bpm / 4.0,
      prog      = prog,
      leadScale = leadScale,
      kickSteps = KickPatterns(rng.nextInt(KickPatterns.length)),
      snareSteps = SnarePatterns(rng.nextInt(SnarePatterns.length)),
      hatStep   = if rng.nextBoolean() then 1 else 2,
      openHats  = OpenHatChoices(rng.nextInt(OpenHatChoices.length)),
      bassMode  = rng.nextInt(3),
      arpStep   = if rng.nextInt(3) == 0 then 1 else 2, // mostly 8ths
      arpDir    = rng.nextInt(3)
    )

  private def nearestIdx(scale: Vector[Int], midi: Int): Int =
    var best = 0; var bestD = Int.MaxValue; var i = 0
    while i < scale.length do
      val d = math.abs(scale(i) - midi)
      if d < bestD then { bestD = d; best = i }
      i += 1
    best

  private def makeNoise(ctx: dom.AudioContext, rng: Random): dom.AudioBuffer =
    val len  = ctx.sampleRate.toInt // one second of white noise, reused by every hat/snare
    val buf  = ctx.createBuffer(1, len, ctx.sampleRate.toInt)
    val data = buf.getChannelData(0)
    var i = 0
    while i < len do
      data(i) = (rng.nextDouble() * 2.0 - 1.0).toFloat
      i += 1
    buf

// ---------------------------------------------------------------------------
// Audio-element backbone — shared by Files, Upload and the direct-link branch.
// ---------------------------------------------------------------------------

/** Base for any source that plays through a plain `<audio>` element: looping,
  * cross-origin playback works without CORS, and pause/resume are instant and
  * reliable — the sturdiest backend for the freeze cut. */
abstract class AudioDanceSource extends DanceSource:
  protected val srcVar = Var(Option.empty[String])
  protected val errored = Var(false)

  protected val audioEl = audioTag(cls := "freeze-audio")
  // Loop so short clips (and the synth-sized CC0 loops) keep the floor moving;
  // the engine, not the track end, decides when to stop. Error flag drives the
  // "couldn't play that" banner.
  locally {
    val el = audioEl.ref
    el.loop = true
    el.addEventListener("error", (_: dom.Event) => errored.set(true))
  }

  private def media: dom.html.Audio = audioEl.ref

  protected def setSrc(url: String): Unit =
    errored.set(false)
    srcVar.set(Some(url))
    media.src = url

  protected def clearSrc(): Unit =
    srcVar.set(None)
    try media.removeAttribute("src") catch case _: Throwable => ()

  def play(): Unit =
    try { val _ = media.play() }
    catch case _: Throwable => ()

  def pause(): Unit =
    try media.pause() catch case _: Throwable => ()

  def dispose(): Unit = pause()

  def ready: Signal[Boolean] =
    srcVar.signal.combineWith(errored.signal).map((src, err) => src.isDefined && !err)

// ---------------------------------------------------------------------------
// Suggested songs — one-tap presets that load straight into the Songs source.
// ---------------------------------------------------------------------------

/** What a one-tap suggestion plays. */
enum Pick:
  case Yt(id: String)        // a YouTube video
  case Audio(url: String)    // a direct audio URL (e.g. a bundled CC0 loop)

/** A song offered as a one-tap card. `by` is the creator/channel. Titles and
  * names show as-is (song titles aren't translated). */
final case class Suggestion(title: String, by: String, pick: Pick):
  /** Stable key used to mark the chosen card. */
  def key: String = pick match
    case Pick.Yt(id)     => s"yt:$id"
    case Pick.Audio(url) => url

/** A bundled CC0 loop in `public/music/`, surfaced as an audio suggestion. */
final case class Track(file: String, title: String)

object Youtube:
  // Pulls the 11-char video id out of the common YouTube URL shapes.
  private val YtId =
    """(?:youtu\.be/|youtube(?:-nocookie)?\.com/(?:watch\?(?:.*&)?v=|embed/|shorts/|v/|live/))([\w-]{11})""".r
  def id(raw: String): Option[String] = YtId.findFirstMatchIn(raw).map(_.group(1))

// ---------------------------------------------------------------------------
// Songs — the one "bring a real song" mode: tap a suggestion, paste a link, or
// pick a file. YouTube plays in a cookie-less embed (driven over postMessage);
// everything else plays through the shared <audio> element.
// ---------------------------------------------------------------------------

final class ExternalSource(suggestions: List[Suggestion]) extends AudioDanceSource:
  private enum Mode:
    case Empty, Audio, YouTube
  private val mode   = Var(Mode.Empty)
  private val chosen = Var(Option.empty[String]) // key of the active pick, for highlight
  private var objectUrl: Option[String] = None
  private var ytId: Option[String] = None        // the chosen video, loaded lazily on play
  private var ytLoaded = false                   // is the embed currently loaded & playing?

  private val ytFrame =
    iframe(cls := "freeze-yt", hidden <-- mode.signal.map(_ != Mode.YouTube))
  // `allow` isn't a named Laminar key — set it directly so autoplay/fullscreen work.
  ytFrame.ref.setAttribute("allow", "autoplay; encrypted-media; fullscreen")

  private def embedUrl(id: String, autoplay: Boolean): String =
    val origin = dom.window.location.origin
    val ap     = if autoplay then 1 else 0
    s"https://www.youtube-nocookie.com/embed/$id?enablejsapi=1&autoplay=$ap&playsinline=1&rel=0&modestbranding=1&origin=$origin"

  // Drive the embed via the IFrame API's postMessage protocol — no external script.
  private def ytCmd(func: String): Unit =
    val w = ytFrame.ref.contentWindow
    if w != null then
      val msg = js.JSON.stringify(
        js.Dynamic.literal("event" -> "command", "func" -> func, "args" -> js.Array[js.Any]())
      )
      val _ = w.asInstanceOf[js.Dynamic].postMessage(msg, "*")

  // Selecting a card just records the pick — the embed is (re)loaded on play(),
  // with autoplay, so the very first beat is reliable rather than a postMessage
  // race against a not-yet-ready player.
  private def playYt(id: String, key: String): Unit =
    clearSrc(); errored.set(false)
    ytId = Some(id); ytLoaded = false
    try ytFrame.ref.removeAttribute("src") catch case _: Throwable => ()
    mode.set(Mode.YouTube); chosen.set(Some(key))

  private def playAudio(url: String, key: String): Unit =
    ytId = None; ytLoaded = false
    try ytFrame.ref.removeAttribute("src") catch case _: Throwable => ()
    setSrc(url); mode.set(Mode.Audio); chosen.set(Some(key))

  private def choose(sg: Suggestion): Unit = sg.pick match
    case Pick.Yt(id)     => playYt(id, sg.key)
    case Pick.Audio(url) => playAudio(url, sg.key)

  private def loadLink(raw: String): Unit =
    val t = raw.trim
    if t.nonEmpty then
      Youtube.id(t) match
        case Some(id)                     => playYt(id, s"yt:$id")
        case None if t.startsWith("http") => playAudio(t, t)
        case None                         => errored.set(true)

  private def loadFile(file: dom.File): Unit =
    objectUrl.foreach(dom.URL.revokeObjectURL)
    val url = dom.URL.createObjectURL(file)
    objectUrl = Some(url)
    playAudio(url, "file")

  override def play(): Unit =
    if mode.now() == Mode.YouTube then
      // First start (or restart after Stop): load the embed with autoplay — the
      // load is gesture-driven (Start click), so it plays with sound. Later
      // resumes hit an already-ready player, so postMessage is reliable.
      if !ytLoaded then
        ytId.foreach(id => ytFrame.ref.src = embedUrl(id, autoplay = true))
        ytLoaded = true
      else ytCmd("playVideo")
    else super.play()

  override def pause(): Unit =
    if mode.now() == Mode.YouTube then ytCmd("pauseVideo") else super.pause()

  override def reset(): Unit =
    // Full stop: forget the loaded embed so the next Start reloads from the top,
    // and rewind an audio track for a clean replay.
    ytLoaded = false
    try audioEl.ref.currentTime = 0 catch case _: Throwable => ()

  override def dispose(): Unit =
    super.dispose()
    objectUrl.foreach(dom.URL.revokeObjectURL); objectUrl = None
    try ytFrame.ref.removeAttribute("src") catch case _: Throwable => ()

  override def ready: Signal[Boolean] =
    mode.signal.combineWith(srcVar.signal, errored.signal).map { (m, src, err) =>
      !err && (m == Mode.YouTube || (m == Mode.Audio && src.isDefined))
    }

  // The engine mounts the embed in the play stage (so the video and the Stop
  // button stay together); it's shown only while a YouTube pick is active.
  override val videoNode: Option[HtmlElement] = Some(ytFrame)
  override val isVideo: Signal[Boolean] = mode.signal.map(_ == Mode.YouTube)

  // ---- UI ----

  private def songCard(sg: Suggestion): HtmlElement =
    button(
      cls := "freeze-song-card",
      cls("is-active") <-- chosen.signal.map(_.contains(sg.key)),
      span(cls := "freeze-song-card__glyph", sg.pick match { case Pick.Yt(_) => "▶"; case _ => "♪" }),
      div(
        cls := "freeze-song-card__text",
        div(cls := "freeze-song-card__title", sg.title),
        div(cls := "freeze-song-card__by", sg.by)
      ),
      onClick --> (_ => choose(sg))
    )

  // "Bring your own" is the rare case, so it's tucked behind one toggle to keep
  // the panel calm — suggestions are what most people want.
  private val addOpen = Var(false)

  private def addOwnBlock: HtmlElement =
    val text     = Var("")
    val fileName = Var("")
    div(
      cls := "freeze-add__body stack",
      span(cls := "freeze-add__label", child.text <-- s(_.freezeDance.pasteLabel)),
      div(
        cls := "row freeze-link__row",
        input(
          tpe := "url",
          cls := "freeze-link__input",
          placeholder <-- s(_.freezeDance.linkPlaceholder),
          controlled(value <-- text.signal, onInput.mapToValue --> text),
          onKeyDown.filter(_.key == "Enter") --> (_ => loadLink(text.now()))
        ),
        button(cls := "btn", child.text <-- s(_.freezeDance.linkLoad), onClick --> (_ => loadLink(text.now())))
      ),
      child <-- errored.signal.map {
        case true  => banner("warn", s(_.freezeDance.linkInvalid))
        case false => emptyNode
      },
      span(cls := "freeze-add__label", child.text <-- s(_.freezeDance.uploadLabel)),
      div(
        cls := "center",
        label(
          cls := "btn btn--ghost freeze-file",
          child.text <-- fileName.signal.combineWith(AppState.strings).map { (n, str) =>
            if n.nonEmpty then n else str.freezeDance.uploadButton
          },
          input(
            tpe := "file",
            accept := "audio/*",
            cls := "freeze-file__input",
            onChange --> { ev =>
              val files = ev.target.asInstanceOf[dom.html.Input].files
              if files != null && files.length > 0 then
                val f = files.item(0)
                if f != null then { fileName.set(f.name); loadFile(f) }
            }
          )
        )
      ),
      MusicLinks.render()
    )

  val controls: HtmlElement =
    div(
      cls := "stack freeze-source",
      p(cls := "freeze-hint muted center", child.text <-- s(_.freezeDance.songsHint)),
      div(cls := "freeze-suggestions", suggestions.map(songCard)),
      div(
        cls := "freeze-add",
        button(
          cls := "freeze-add__toggle",
          cls("is-open") <-- addOpen.signal,
          span(cls := "freeze-add__chev", "›"),
          span(child.text <-- s(_.freezeDance.addOwn)),
          onClick --> (_ => addOpen.update(!_))
        ),
        child <-- addOpen.signal.map(if _ then addOwnBlock else emptyNode)
      ),
      audioEl
    )
