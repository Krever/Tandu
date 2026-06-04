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

  private def pickWord(lang: Lang, avoid: Option[String]): String =
    Picker.pickAvoiding(WordBank.forLang(lang), avoid)

  def render(): HtmlElement =
    val currentWord: Var[String] = Var(pickWord(AppState.lang.now(), avoid = None))

    def next(): Unit =
      currentWord.update(prev => pickWord(AppState.lang.now(), avoid = Some(prev)))

    // If the user switches language while on the page, draw a fresh
    // seed word from the new language's bank.
    val langChange = AppState.lang.signal.changes --> { lang =>
      currentWord.set(pickWord(lang, avoid = None))
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
