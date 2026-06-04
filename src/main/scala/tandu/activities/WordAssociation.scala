package tandu.activities

import com.raquo.laminar.api.L.*
import tandu.AppState
import tandu.i18n.{Lang, Strings}
import tandu.ui.Components
import tandu.ui.Components.s

object WordAssociation extends Activity:
  val id = "word-association"
  def name(s: Strings): String = s.wordAssociation.name
  def description(s: Strings): String = s.wordAssociation.description
  val minPlayers: Int = 2
  val maxPlayers: Int = Int.MaxValue
  val handsFree: Boolean = true
  val glyph: String = "≈"
  val tint: String = "rose"

  def render(): HtmlElement =
    val roller = new Roller[String]
    def pick(lang: Lang): String = roller.next(WordBank.forLang(lang))

    val currentWord: Var[String] = Var(pick(AppState.lang.now()))

    def next(): Unit = currentWord.set(pick(AppState.lang.now()))

    // If the user switches language while on the page, draw a fresh
    // seed word from the new language's bank.
    val langChange = AppState.lang.signal.changes --> { lang =>
      currentWord.set(pick(lang))
    }

    div(
      cls := "stack-lg",
      langChange,
      p(cls := "muted center", child.text <-- s(_.wordAssociation.hint)),
      div(
        cls := "wa-card",
        child.text <-- currentWord.signal
      ),
      div(
        cls := "row no-print",
        styleAttr := "justify-content: center;",
        Components.speakBtn(currentWord.signal),
        button(
          cls := "btn btn--lg",
          child.text <-- s(_.wordAssociation.nextWord),
          onClick --> (_ => next())
        )
      )
    )
