package tandu.activities

import com.raquo.laminar.api.L.*
import tandu.AppState
import tandu.i18n.{Lang, Strings}
import tandu.audio.Speech
import tandu.ui.Components
import tandu.ui.Components.s

object LastLetter extends Activity:
  val id = "last-letter"
  def name(s: Strings): String = s.lastLetter.name
  def description(s: Strings): String = s.lastLetter.description
  val minPlayers: Int = 2
  val maxPlayers: Int = Int.MaxValue
  val handsFree: Boolean = true

  private def pickLetter(lang: Lang, avoid: Option[Char]): Char =
    Picker.pickAvoiding(CategoryBank.lettersFor(lang), avoid)

  def render(): HtmlElement =
    val letter: Var[Char] = Var(pickLetter(AppState.lang.now(), avoid = None))

    def next(): Unit =
      letter.update(prev => pickLetter(AppState.lang.now(), avoid = Some(prev)))

    val langChange = AppState.lang.signal.changes --> { lang =>
      letter.set(pickLetter(lang, avoid = None))
    }

    div(
      cls := "stack-lg",
      langChange,
      p(cls := "muted center", child.text <-- s(_.lastLetter.hint)),
      div(
        cls := "wa-card wa-card--letter",
        child.text <-- letter.signal.map(_.toString)
      ),
      div(
        cls := "row no-print",
        styleAttr := "justify-content: center;",
        Components.speakBtn(letter.signal.map(Speech.spokenLetter)),
        button(
          cls := "btn btn--lg",
          child.text <-- s(_.lastLetter.newLetter),
          onClick --> (_ => next())
        )
      )
    )
