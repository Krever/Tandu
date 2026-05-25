package tandu.activities

import com.raquo.laminar.api.L.*
import scala.util.Random
import tandu.AppState
import tandu.i18n.{Lang, Strings}
import tandu.ui.Components.s

object WouldYouRather extends Activity:
  val id = "would-you-rather"
  def name(s: Strings): String = s.wouldYouRather.name
  def description(s: Strings): String = s.wouldYouRather.description
  val categories: Set[Category] = Set(Category.Car)

  // Avoid the previous pair's items so consecutive dilemmas don't share
  // an option — repeats feel like bugs even when the pool is large.
  private def pickPair(pool: Vector[String], avoid: Set[String]): (String, String) =
    val available =
      if pool.size > avoid.size + 1 then pool.filterNot(avoid.contains) else pool
    val a = available(Random.nextInt(available.size))
    val rest = available.filterNot(_ == a)
    val b = if rest.nonEmpty then rest(Random.nextInt(rest.size)) else a
    (a, b)

  def render(): HtmlElement =
    val pair: Var[(String, String)] =
      Var(pickPair(WouldYouRatherBank.forLang(AppState.lang.now()), Set.empty))

    def next(): Unit =
      pair.update { case (a, b) =>
        pickPair(WouldYouRatherBank.forLang(AppState.lang.now()), Set(a, b))
      }

    val langChange = AppState.lang.signal.changes --> { lang =>
      pair.set(pickPair(WouldYouRatherBank.forLang(lang), Set.empty))
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
        button(
          cls := "btn btn--lg",
          child.text <-- s(_.wouldYouRather.next),
          onClick --> (_ => next())
        )
      )
    )
