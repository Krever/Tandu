package tandu.activities

import com.raquo.laminar.api.L.*
import com.raquo.laminar.api.L.svg as S
import tandu.{AppState, Kind}
import tandu.i18n.Strings
import tandu.ui.{Components, Mode, ModeChooser}
import tandu.ui.Components.s

import scala.scalajs.js
import scala.util.Random

/** Digital clock notation. Persisted globally (see [[tandu.AppState.clockFormat]])
  * so the choice carries across activities and sessions. */
enum ClockFormat:
  case H12, H24

object ClockFormat:
  def fromString(s: String): Option[ClockFormat] = values.find(_.toString == s)

/** Telling-the-time practice. Two exercises:
  *   - Match: translate between analog and digital, to the quarter hour.
  *   - Part of day: name the part of the day shown by a digital clock.
  *
  * The digital-format selector (12h/24h) is shared and persistent, so the same
  * notation is used everywhere the app shows a time. */
object Clock extends Activity:
  val id = "clock"
  def name(s: Strings): String        = s.clock.name
  def description(s: Strings): String = s.clock.description
  val minPlayers: Int                 = 1
  val maxPlayers: Int                 = 1
  val glyph: String = "🕐"
  val tint: String = "sky"
  override val kind: Kind             = Kind.Learn

  // ---------- model ----------

  /** A wall-clock time, minutes constrained to the quarter hour. */
  final case class ClockTime(hour: Int, minute: Int):
    /** Analog signature: a 12-hour clock face cannot distinguish 1:00 from
      * 13:00, so two times with the same face are indistinguishable on a dial.
      * Used to keep answer choices unambiguous against an analog stem. */
    def face: (Int, Int) = (hour % 12, minute)

  enum DayPart(val emoji: String, val label: Strings => String):
    case Morning   extends DayPart("🌅", _.clock.morning)
    case Afternoon extends DayPart("☀️", _.clock.afternoon)
    case Evening   extends DayPart("🌆", _.clock.evening)
    case Night     extends DayPart("🌙", _.clock.night)

  /** Which notation is shown as the stem (the other is the answer). */
  enum Direction:
    case AnalogToDigital, DigitalToAnalog

  enum Task:
    case Match(direction: Direction, target: ClockTime, options: Vector[ClockTime], correctIdx: Int)
    case PartOfDay(target: ClockTime, options: Vector[DayPart], correctIdx: Int)

  // ---------- generation ----------

  private val quarters = Vector(0, 15, 30, 45)

  private def randTime(): ClockTime = ClockTime(Random.nextInt(24), quarters(Random.nextInt(4)))

  private def partOf(hour: Int): DayPart =
    if hour < 5 then DayPart.Night         // 00:00–04:59 wraps into night
    else if hour < 12 then DayPart.Morning
    else if hour < 17 then DayPart.Afternoon
    else if hour < 21 then DayPart.Evening
    else DayPart.Night                     // 21:00–23:59

  /** Nudge a time to a plausible-but-wrong neighbour: shift the hour by a few
    * hours, or move to a different quarter. */
  private def perturb(t: ClockTime): ClockTime =
    if Random.nextBoolean() then
      val delta = 1 + Random.nextInt(3)
      val signed = if Random.nextBoolean() then delta else -delta
      ClockTime(((t.hour + signed) % 24 + 24) % 24, t.minute)
    else
      val others = quarters.filterNot(_ == t.minute)
      ClockTime(t.hour, others(Random.nextInt(others.size)))

  /** Four choices including `correct`, every one with a distinct clock face so
    * exactly one matches whatever analog dial is shown. */
  private def matchOptions(correct: ClockTime): (Vector[ClockTime], Int) =
    val seen = scala.collection.mutable.Set(correct.face)
    val opts = scala.collection.mutable.ListBuffer(correct)
    var tries = 0
    while opts.size < 4 && tries < 100 do
      val cand = perturb(correct)
      if !seen(cand.face) then seen += cand.face; opts += cand
      tries += 1
    while opts.size < 4 do
      val cand = randTime()
      if !seen(cand.face) then seen += cand.face; opts += cand
    val shuffled = Random.shuffle(opts.toVector)
    (shuffled, shuffled.indexOf(correct))

  /** A fresh random time whose `key` differs from the previous task's when
    * possible, so consecutive questions don't repeat the same answer. */
  private def pickTarget[K](avoid: Option[Task], key: ClockTime => K): ClockTime =
    val avoidKey = avoid.map(t => key(target(t)))
    var t = randTime()
    var tries = 0
    while avoidKey.contains(key(t)) && tries < 20 do
      t = randTime(); tries += 1
    t

  private def buildMatch(avoid: Option[Task]): Task =
    val tgt            = pickTarget(avoid, _.face)
    val (options, idx) = matchOptions(tgt)
    val direction      = if Random.nextBoolean() then Direction.AnalogToDigital else Direction.DigitalToAnalog
    Task.Match(direction, tgt, options, idx)

  private def buildPartOfDay(avoid: Option[Task]): Task =
    val tgt     = pickTarget(avoid, t => partOf(t.hour))
    val options = Random.shuffle(DayPart.values.toVector)
    Task.PartOfDay(tgt, options, options.indexOf(partOf(tgt.hour)))

  // ---------- task helpers ----------

  private def target(task: Task): ClockTime = task match
    case Task.Match(_, t, _, _)  => t
    case Task.PartOfDay(t, _, _) => t

  private def correctIdx(task: Task): Int = task match
    case Task.Match(_, _, _, i)  => i
    case Task.PartOfDay(_, _, i) => i

  private def choiceCount(task: Task): Int = task match
    case Task.Match(_, _, o, _)  => o.size
    case Task.PartOfDay(_, o, _) => o.size

  private def prompt(task: Task): Strings => String = task match
    case Task.Match(Direction.AnalogToDigital, _, _, _) => _.clock.whatTime
    case Task.Match(Direction.DigitalToAnalog, _, _, _) => _.clock.pickClock
    case _: Task.PartOfDay                              => _.clock.partOfDay

  /** Whether choices are tall visual tiles (clocks / day parts) rather than text. */
  private def pictorial(task: Task): Boolean = task match
    case Task.Match(Direction.AnalogToDigital, _, _, _) => false
    case _                                              => true

  /** The digital reading of a time, honouring the persistent format. */
  private def digital(t: ClockTime, fmt: ClockFormat): String = fmt match
    case ClockFormat.H24 =>
      f"${t.hour}%02d:${t.minute}%02d"
    case ClockFormat.H12 =>
      val h    = t.hour % 12
      val h12  = if h == 0 then 12 else h
      val ampm = if t.hour < 12 then "AM" else "PM"
      f"$h12:${t.minute}%02d $ampm"

  // ---------- render ----------

  def render(): HtmlElement =
    ModeChooser.render(id, List(
      Mode(
        id = "match",
        label = _.clock.matchName,
        description = Some(_.clock.matchDesc),
        render = () => renderPlay(buildMatch)
      ),
      Mode(
        id = "part-of-day",
        label = _.clock.todName,
        description = Some(_.clock.todDesc),
        render = () => renderPlay(buildPartOfDay)
      )
    ))

  private def formatSelector(): HtmlElement =
    div(
      cls := "clock-format no-print",
      span(cls := "clock-format__label", child.text <-- s(_.clock.formatLabel)),
      Components.segmentedToggle[ClockFormat](
        "pill-toggle",
        "pill-btn",
        List(
          ClockFormat.H12 -> s(_.clock.format12),
          ClockFormat.H24 -> s(_.clock.format24)
        ),
        AppState.clockFormat
      )
    )

  private def renderPlay(build: Option[Task] => Task): HtmlElement =
    val task: Var[Task]              = Var(build(None))
    val solved: Var[Boolean]         = Var(false)
    val wrongIdx: Var[Option[Int]]   = Var(None)

    def nextTask(): Unit =
      task.set(build(Some(task.now())))
      solved.set(false)
      wrongIdx.set(None)

    def pick(idx: Int): Unit =
      if solved.now() then ()
      else if idx == correctIdx(task.now()) then solved.set(true)
      else
        wrongIdx.set(Some(idx))
        val _ = js.timers.setTimeout(420)(
          if wrongIdx.now().contains(idx) then wrongIdx.set(None)
        )

    div(
      cls := "clock stack-lg",
      formatSelector(),
      div(
        cls := "mp-prompt center",
        child.text <-- task.signal.combineWith(AppState.strings).map((t, str) => prompt(t)(str))
      ),
      div(
        cls := "mp-stem",
        cls("mp-stem--solved") <-- solved.signal,
        child <-- task.signal.map(stemNode)
      ),
      div(
        cls := "mp-choices",
        children <-- task.signal.combineWith(wrongIdx.signal).combineWith(solved.signal).map {
          (t, wrong, isSolved) =>
            (0 until choiceCount(t)).map { i =>
              button(
                cls := "mp-choice",
                cls("mp-choice--pic")     := pictorial(t),
                cls("mp-choice--wrong")   := wrong.contains(i),
                cls("mp-choice--correct") := (isSolved && i == correctIdx(t)),
                disabled := isSolved,
                choiceNode(t, i),
                onClick --> (_ => pick(i))
              )
            }
        }
      ),
      child <-- solved.signal.map { isSolved =>
        if isSolved then
          div(
            cls := "stack",
            Components.banner("win", s(_.clock.correct)),
            div(
              cls := "row no-print",
              styleAttr := "justify-content: center;",
              button(
                cls := "btn btn--lg",
                child.text <-- s(_.clock.next),
                onClick --> (_ => nextTask())
              )
            )
          )
        else
          div(
            cls := "row no-print",
            styleAttr := "justify-content: center;",
            Components.ghost(s(_.clock.skip), nextTask())
          )
      }
    )

  /** Big digital readout of a time, reacting to the persistent format. */
  private def digitalStem(t: ClockTime): HtmlElement =
    div(cls := "clock-digital", child.text <-- AppState.clockFormat.signal.map(digital(t, _)))

  private def stemNode(task: Task): HtmlElement = task match
    case Task.Match(Direction.AnalogToDigital, t, _, _) =>
      div(cls := "clock-analog clock-analog--lg", analogClock(t, 200))
    // Digital-to-analog ("pick the clock") and part-of-day both show the time
    // as a digital readout.
    case other =>
      digitalStem(target(other))

  private def choiceNode(task: Task, i: Int): HtmlElement = task match
    case Task.Match(Direction.AnalogToDigital, _, options, _) =>
      span(
        cls := "clock-choice-time",
        child.text <-- AppState.clockFormat.signal.map(digital(options(i), _))
      )
    case Task.Match(Direction.DigitalToAnalog, _, options, _) =>
      div(cls := "clock-analog", analogClock(options(i), 110))
    case Task.PartOfDay(_, options, _) =>
      val dp = options(i)
      div(
        cls := "clock-daypart",
        span(cls := "clock-daypart__emoji", dp.emoji),
        span(cls := "clock-daypart__label", child.text <-- s(dp.label))
      )

  // ---------- analog dial ----------

  private def num(d: Double): String = f"$d%.2f"

  /** A learning clock face with hour numerals and hour/minute hands, drawn on a
    * 0..100 viewBox and scaled to `size` px. */
  private def analogClock(t: ClockTime, size: Int): SvgElement =
    val centre = 50.0

    /** Point on the dial at `deg` clockwise from 12, at `radius` from centre. */
    def at(deg: Double, radius: Double): (Double, Double) =
      val a = deg * math.Pi / 180.0
      (centre + radius * math.sin(a), centre - radius * math.cos(a))

    val minuteDeg = t.minute * 6.0
    val hourDeg   = (t.hour % 12) * 30.0 + t.minute * 0.5
    val (hx, hy)  = at(hourDeg, 26)
    val (mx, my)  = at(minuteDeg, 38)

    val ticks = (0 until 12).map { i =>
      val major     = i % 3 == 0
      val (x1, y1)  = at(i * 30.0, 45)
      val (x2, y2)  = at(i * 30.0, if major then 39 else 42)
      S.line(
        S.x1 := num(x1), S.y1 := num(y1), S.x2 := num(x2), S.y2 := num(y2),
        S.stroke := "var(--color-border-strong)",
        S.strokeWidth := (if major then "2" else "1")
      )
    }

    val numerals = (1 to 12).map { n =>
      val (x, y) = at(n * 30.0, 33)
      S.text(S.x := num(x), S.y := num(y), n.toString)
    }

    S.svg(
      S.viewBox := "0 0 100 100",
      S.width := size.toString,
      S.height := size.toString,
      S.className := "clock-dial",
      S.circle(
        S.cx := "50", S.cy := "50", S.r := "48",
        S.fill := "var(--color-surface)",
        S.stroke := "var(--color-border-strong)",
        S.strokeWidth := "2"
      ),
      S.g(ticks),
      S.g(numerals),
      S.line(
        S.x1 := "50", S.y1 := "50", S.x2 := num(hx), S.y2 := num(hy),
        S.stroke := "var(--color-text)", S.strokeWidth := "4", S.strokeLineCap := "round"
      ),
      S.line(
        S.x1 := "50", S.y1 := "50", S.x2 := num(mx), S.y2 := num(my),
        S.stroke := "var(--color-text)", S.strokeWidth := "2.5", S.strokeLineCap := "round"
      ),
      S.circle(S.cx := "50", S.cy := "50", S.r := "3", S.fill := "var(--color-text)")
    )
