package tandu.activities

import com.raquo.laminar.api.L.*
import tandu.AppState
import tandu.i18n.{Lang, Strings}
import tandu.ui.Components
import tandu.ui.Components.s

object TwentyQuestions extends Activity:
  val id = "twenty-questions"
  def name(s: Strings): String = s.twentyQuestions.name
  def description(s: Strings): String = s.twentyQuestions.description
  val categories: Set[Category] = Set(Category.Car)

  val MaxQuestions: Int = 20

  private def pickWord(lang: Lang, avoid: Option[String]): String =
    Picker.pickAvoiding(WordBank.forLang(lang), avoid)

  def render(): HtmlElement =
    val word: Var[String]        = Var(pickWord(AppState.lang.now(), avoid = None))
    val revealed: Var[Boolean]   = Var(false)
    val asked: Var[Int]          = Var(0)

    def resetWith(w: String): Unit =
      word.set(w)
      revealed.set(false)
      asked.set(0)

    def newWord(): Unit =
      resetWith(pickWord(AppState.lang.now(), avoid = Some(word.now())))

    val langChange = AppState.lang.signal.changes --> { lang =>
      resetWith(pickWord(lang, avoid = None))
    }

    val cardText: Signal[String] =
      revealed.signal.combineWith(word.signal).combineWith(AppState.strings).map {
        (isRevealed, w, str) =>
          if isRevealed then w else str.twentyQuestions.hidden
      }

    val toggleLabel: Signal[String] =
      revealed.signal.combineWith(AppState.strings).map { (isRevealed, str) =>
        if isRevealed then str.twentyQuestions.hide else str.twentyQuestions.reveal
      }

    div(
      cls := "stack-lg",
      langChange,
      p(cls := "muted center", child.text <-- s(_.twentyQuestions.hint)),
      div(
        cls := "wa-card wa-card--phrase tq-card",
        cls("tq-card--hidden") <-- revealed.signal.map(!_),
        span(cls := "tq-card__text", child.text <-- cardText),
        button(
          cls := "btn btn--ghost btn--icon tq-card__toggle no-print",
          child.text <-- toggleLabel,
          onClick --> (_ => revealed.update(!_))
        )
      ),
      div(
        cls := "tq-actions no-print",
        button(
          cls := "btn",
          child.text <-- s(_.twentyQuestions.undo),
          disabled <-- asked.signal.map(_ <= 0),
          onClick --> (_ => asked.update(n => math.max(0, n - 1)))
        ),
        div(
          cls := "tq-counter",
          span(
            cls := "tq-counter__value",
            child.text <-- asked.signal.map(n => (MaxQuestions - n).toString)
          ),
          span(cls := "tq-counter__label muted", child.text <-- s(_.twentyQuestions.questionsLeft))
        ),
        button(
          cls := "btn btn--lg",
          child.text <-- s(_.twentyQuestions.askedOne),
          disabled <-- asked.signal.map(_ >= MaxQuestions),
          onClick --> (_ => asked.update(n => math.min(MaxQuestions, n + 1)))
        )
      ),
      child <-- asked.signal.map { n =>
        if n >= MaxQuestions then
          Components.banner("hit", s(_.twentyQuestions.outOfQuestions))
        else emptyNode
      },
      div(
        cls := "row no-print",
        styleAttr := "justify-content: center;",
        Components.replayButton(
          s(_.twentyQuestions.newWord),
          newWord(),
          asked.signal.map(_ >= MaxQuestions)
        )
      )
    )