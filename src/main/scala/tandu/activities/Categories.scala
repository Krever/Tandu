package tandu.activities

import com.raquo.laminar.api.L.*
import tandu.AppState
import tandu.audio.Speech
import tandu.i18n.{Lang, Strings}
import tandu.tools.Timer
import tandu.ui.{Components, Mode, ModeChooser, Printable}
import tandu.ui.Components.s

object Categories extends Activity:
  val id = "categories"
  def name(s: Strings): String = s.categoriesGame.name
  def description(s: Strings): String = s.categoriesGame.description
  val minPlayers: Int = 2
  val maxPlayers: Int = Int.MaxValue
  val handsFree: Boolean = true
  val glyph: String = "◫"
  val tint: String = "olive"

  final case class Round(prompt: String, letter: Char)

  def render(): HtmlElement =
    ModeChooser.render(id, List(
      Mode(
        id = "in-app",
        label = _.mode.inApp,
        render = () => renderInApp()
      ),
      Mode(
        id = "print",
        label = _.mode.offline,
        materials = List(_.offline.materials.printer, _.offline.materials.paperPen),
        hint = Some(_.offline.categories.curatedNote),
        render = () => renderOffline()
      )
    ))

  private val PrintColumns: Int = 3

  private def renderOffline(): HtmlElement =
    val initLang = AppState.lang.now()
    val letterRoller = new Roller[Char]
    val letter: Var[Char] = Var(letterRoller.next(CategoryBank.lettersFor(initLang)))

    def nextLetter(): Unit =
      letter.set(letterRoller.next(CategoryBank.lettersFor(AppState.lang.now())))

    val langChange = AppState.lang.signal.changes --> { lang =>
      letter.set(letterRoller.next(CategoryBank.lettersFor(lang)))
    }

    div(
      cls := "stack-lg",
      langChange,
      Printable.printButton(),
      hr(),
      div(
        cls := "cg-letter-helper no-print",
        div(
          cls := "wa-card wa-card--letter cg-letter-helper__card",
          child.text <-- letter.signal.map(_.toString)
        ),
        button(
          cls := "btn",
          child.text <-- s(_.categoriesGame.next),
          onClick --> (_ => nextLetter())
        )
      ),
      Timer.render(180),
      div(cls := "print-only", printableSheet())
    )

  private def printableSheet(): HtmlElement =
    val lang = AppState.lang.now()
    val cats = CategoryBank.coreCategoriesFor(lang).toList
    Printable.render(
      title = _.offline.categories.printTitle,
      body = div(
        cls := "cg-print-sheet",
        table(
          cls := "cg-print-table",
          thead(
            tr(
              th(cls := "cg-print-th cg-print-th--cat", child.text <-- s(_.offline.categories.categoriesLabel)),
              (0 until PrintColumns).map(_ => th(cls := "cg-print-th", "")),
              th(cls := "cg-print-th cg-print-th--score", child.text <-- s(_.offline.categories.scoresLabel))
            )
          ),
          tbody(
            cats.map { cat =>
              tr(
                td(cls := "cg-print-td cg-print-td--cat", cat),
                (0 until PrintColumns).map(_ => td(cls := "cg-print-td", "")),
                td(cls := "cg-print-td cg-print-td--score", "")
              )
            }
          )
        )
      )
    )

  private def renderInApp(): HtmlElement =
    val promptRoller = new Roller[String]
    val letterRoller = new Roller[Char]
    def pickRound(lang: Lang): Round =
      Round(
        promptRoller.next(CategoryBank.categoriesFor(lang)),
        letterRoller.next(CategoryBank.lettersFor(lang))
      )

    val round: Var[Round] = Var(pickRound(AppState.lang.now()))

    def next(): Unit = round.set(pickRound(AppState.lang.now()))

    val langChange = AppState.lang.signal.changes --> { lang =>
      round.set(pickRound(lang))
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
        Components.speakBtn(
          round.signal.combineWith(AppState.strings).map { (r, str) =>
            s"${r.prompt} ${str.categoriesGame.startingWith} ${Speech.spokenLetter(r.letter)}"
          }
        ),
        button(
          cls := "btn btn--lg",
          child.text <-- s(_.categoriesGame.next),
          onClick --> (_ => next())
        )
      )
    )
