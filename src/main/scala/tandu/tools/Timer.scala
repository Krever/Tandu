package tandu.tools

import com.raquo.laminar.api.L.*
import org.scalajs.dom
import tandu.AppState
import tandu.i18n.Strings
import tandu.ui.Components.s

import scala.scalajs.js

object Timer extends Tool:
  val id = "timer"
  val glyph = "⏱"
  def name(s: Strings): String = s.timer.name
  def description(s: Strings): String = s.timer.description

  private val DefaultDurations: List[Int] = List(15, 30, 60, 120, 180)
  private val DefaultSeconds: Int = 30

  def render(): HtmlElement = render(DefaultSeconds)

  def render(initialSeconds: Int): HtmlElement =
    val total      = Var(initialSeconds)
    val remaining  = Var(initialSeconds)
    val running    = Var(false)
    var handle: Option[js.timers.SetIntervalHandle] = None

    def stopTicker(): Unit =
      handle.foreach(js.timers.clearInterval)
      handle = None

    def tick(): Unit =
      val now = remaining.now() - 1
      if now <= 0 then
        remaining.set(0)
        running.set(false)
        stopTicker()
        playBeep()
      else
        remaining.set(now)

    def start(): Unit =
      if running.now() then ()
      else
        if remaining.now() <= 0 then remaining.set(total.now())
        running.set(true)
        handle = Some(js.timers.setInterval(1000)(tick()))

    def pause(): Unit =
      running.set(false)
      stopTicker()

    def restart(): Unit =
      stopTicker()
      remaining.set(total.now())
      running.set(true)
      handle = Some(js.timers.setInterval(1000)(tick()))

    def setDuration(secs: Int): Unit =
      total.set(secs)
      pause()
      remaining.set(secs)

    val readout = remaining.signal.map(formatMmSs)
    val pctSig  = remaining.signal.combineWith(total.signal).map { (r, t) =>
      if t <= 0 then 0 else math.max(0, math.min(100, (r * 100) / t))
    }

    div(
      cls := "timer no-print",
      onUnmountCallback(_ => stopTicker()),
      div(
        cls := "timer__display",
        cls("is-running") <-- running.signal,
        cls("is-done") <-- remaining.signal.map(_ <= 0),
        styleAttr <-- pctSig.map(p => s"--pct: $p%;"),
        div(cls := "timer__value", child.text <-- readout)
      ),
      div(
        cls := "timer__controls row",
        styleAttr := "justify-content: center;",
        button(
          cls := "btn",
          child.text <-- running.signal.combineWith(AppState.strings).map { (run, str) =>
            if run then str.timer.pause else str.timer.start
          },
          onClick --> (_ => if running.now() then pause() else start())
        ),
        button(
          cls := "btn btn--ghost",
          child.text <-- s(_.timer.restart),
          onClick --> (_ => restart())
        )
      ),
      div(
        cls := "timer__presets row",
        styleAttr := "justify-content: center; flex-wrap: wrap;",
        DefaultDurations.map { d =>
          button(
            cls := "btn btn--ghost btn--icon",
            cls("is-active") <-- total.signal.map(_ == d),
            formatShort(d),
            onClick --> (_ => setDuration(d))
          )
        }
      )
    )

  private def formatMmSs(s: Int): String =
    val m = s / 60
    val sec = s % 60
    f"$m%d:$sec%02d"

  private def formatShort(s: Int): String =
    if s < 60 then s"${s}s"
    else if s % 60 == 0 then s"${s / 60}m"
    else f"${s / 60}m${s % 60}%02ds"

  private def playBeep(): Unit =
    try
      val ctx = new dom.AudioContext()
      val osc = ctx.createOscillator()
      val gain = ctx.createGain()
      osc.`type` = "sine"
      osc.frequency.value = 880
      gain.gain.value = 0.0001
      osc.connect(gain)
      gain.connect(ctx.destination)
      val t = ctx.currentTime
      gain.gain.exponentialRampToValueAtTime(0.2, t + 0.02)
      gain.gain.exponentialRampToValueAtTime(0.0001, t + 0.4)
      osc.start(t)
      osc.stop(t + 0.45)
    catch case _: Throwable => ()
