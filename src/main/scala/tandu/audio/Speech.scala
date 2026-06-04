package tandu.audio

import org.scalajs.dom
import tandu.i18n.Lang

import scala.annotation.unused
import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal

/** Text-to-speech via the browser-native Web Speech API.
  *
  * scalajs-dom 2.8.0 does not bind `speechSynthesis` / `SpeechSynthesisUtterance`,
  * so we declare the slice we need here. Everything is defensive: unsupported
  * browsers and synthesis errors degrade to a silent no-op, mirroring the
  * try/catch around the Web Audio beep in `tandu.tools.Timer`.
  */
object Speech:

  @js.native
  @JSGlobal("SpeechSynthesisUtterance")
  private class Utterance(@unused text: String) extends js.Object:
    var lang: String = js.native
    var rate: Double = js.native
    var pitch: Double = js.native

  @js.native
  private trait Synth extends js.Object:
    def speak(u: Utterance): Unit = js.native
    def cancel(): Unit = js.native

  private def synth: js.UndefOr[Synth] =
    dom.window.asInstanceOf[js.Dynamic].speechSynthesis.asInstanceOf[js.UndefOr[Synth]]

  /** True when the running browser exposes the Speech Synthesis API. */
  lazy val supported: Boolean = synth.isDefined

  /** Speak `text` in `lang`. Cancels any in-flight utterance first so rapid
    * taps don't queue up. `rate` is slightly slow by default — easier for
    * early readers to follow. No-op when unsupported or `text` is blank. */
  def speak(text: String, lang: Lang, rate: Double = 1): Unit =
    val trimmed = text.trim
    synth.foreach { s =>
      if trimmed.nonEmpty then
        try
          s.cancel()
          val u = new Utterance(trimmed)
          u.lang = bcp47(lang)
          u.rate = rate
          s.speak(u)
        catch case _: Throwable => ()
    }

  /** Stop any ongoing speech (e.g. on round change or unmount). */
  def cancel(): Unit =
    synth.foreach(s => try s.cancel() catch case _: Throwable => ())

  private def bcp47(l: Lang): String = l match
    case Lang.En => "en-GB"
    case Lang.Pl => "pl-PL"
    case Lang.Es => "es-ES"
    case Lang.Fr => "fr-FR"
    case Lang.De => "de-DE"
