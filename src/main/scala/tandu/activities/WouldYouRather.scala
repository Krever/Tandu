package tandu.activities

import com.raquo.laminar.api.L.*
import tandu.{AppState, Kind}
import tandu.i18n.{Lang, Strings}
import tandu.ui.Components
import tandu.ui.Components.s

object WouldYouRather extends Activity:
  val id = "would-you-rather"
  def name(s: Strings): String = s.wouldYouRather.name
  def description(s: Strings): String = s.wouldYouRather.description
  val minPlayers: Int = 2
  val maxPlayers: Int = Int.MaxValue
  override val kind: Kind = Kind.OnTheGo
  val glyph: String = "⇆"
  val tint: String = "sky"

  def render(): HtmlElement =
    // Two draws from one roller give two distinct options, and no option
    // recurs until the whole bank has cycled — so consecutive dilemmas never
    // share an item (repeats feel like bugs even with a large pool).
    val roller = new Roller[String]
    def pickPair(lang: Lang): (String, String) =
      val pool = WouldYouRatherBank.forLang(lang)
      (roller.next(pool), roller.next(pool))

    val pair: Var[(String, String)] = Var(pickPair(AppState.lang.now()))

    def next(): Unit = pair.set(pickPair(AppState.lang.now()))

    val langChange = AppState.lang.signal.changes --> { lang =>
      pair.set(pickPair(lang))
    }

    div(
      cls := "stack-lg",
      langChange,
      p(cls := "muted center", child.text <-- s(_.wouldYouRather.hint)),
      p(cls := "wyr-prefix center", child.text <-- s(_.wouldYouRather.prefix)),
      div(
        cls := "wa-card wa-card--phrase",
        child.text <-- pair.signal.map(_._1)
      ),
      div(cls := "wyr-or center", child.text <-- s(_.wouldYouRather.or)),
      div(
        cls := "wa-card wa-card--phrase",
        child.text <-- pair.signal.map(_._2)
      ),
      div(
        cls := "row no-print",
        styleAttr := "justify-content: center;",
        Components.speakBtn(
          pair.signal.combineWith(AppState.strings).map { (a, b, str) =>
            s"${str.wouldYouRather.prefix} $a, ${str.wouldYouRather.or} $b?"
          }
        ),
        button(
          cls := "btn btn--lg",
          child.text <-- s(_.wouldYouRather.next),
          onClick --> (_ => next())
        )
      )
    )
