package tandu.activities

import com.raquo.laminar.api.L.*
import tandu.{AppState, Kind}
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
  override val kind: Kind = Kind.OnTheGo
  val glyph: String = "Z"
  val tint: String = "peach"

  def render(): HtmlElement =
    val roller = new Roller[Char]
    def pick(lang: Lang): Char = roller.next(CategoryBank.lettersFor(lang))

    val letter: Var[Char] = Var(pick(AppState.lang.now()))

    def next(): Unit = letter.set(pick(AppState.lang.now()))

    val langChange = AppState.lang.signal.changes --> { lang =>
      letter.set(pick(lang))
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
