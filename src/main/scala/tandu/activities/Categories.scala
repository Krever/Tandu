package tandu.activities

import com.raquo.laminar.api.L.*
import tandu.AppState
import tandu.i18n.{Lang, Strings}
import tandu.ui.Components.s

object Categories extends Activity:
  val id = "categories"
  def name(s: Strings): String = s.categoriesGame.name
  def description(s: Strings): String = s.categoriesGame.description
  val categories: Set[Category] = Set(Category.Car)

  final case class Round(prompt: String, letter: Char)

  private def pickRound(lang: Lang, avoid: Option[Round]): Round =
    Round(
      Picker.pickAvoiding(CategoryBank.categoriesFor(lang), avoid.map(_.prompt)),
      Picker.pickAvoiding(CategoryBank.lettersFor(lang), avoid.map(_.letter))
    )

  def render(): HtmlElement =
    val round: Var[Round] = Var(pickRound(AppState.lang.now(), avoid = None))

    def next(): Unit =
      round.update(prev => pickRound(AppState.lang.now(), avoid = Some(prev)))

    val langChange = AppState.lang.signal.changes --> { lang =>
      round.set(pickRound(lang, avoid = None))
    }

    div(
      cls := "stack-lg",
      langChange,
      p(cls := "muted center", child.text <-- s(_.categoriesGame.hint)),
      div(
        cls := "cg-round",
        div(
          cls := "wa-card wa-card--phrase",
          child.text <-- round.signal.map(_.prompt)
        ),
        div(
          cls := "wa-card wa-card--letter",
          child.text <-- round.signal.map(_.letter.toString)
        )
      ),
      div(
        cls := "row no-print",
        styleAttr := "justify-content: center;",
        button(
          cls := "btn btn--lg",
          child.text <-- s(_.categoriesGame.next),
          onClick --> (_ => next())
        )
      )
    )
