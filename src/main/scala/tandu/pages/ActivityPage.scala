package tandu.pages

import com.raquo.laminar.api.L.*
import tandu.{AppState, Page, Routing}
import tandu.activities.{Activity, Registry}
import tandu.ui.Components
import tandu.ui.Components.s

object ActivityPage:

  def render(id: String): HtmlElement =
    Registry.byId(id) match
      case Some(activity) => renderActivity(activity)
      case None =>
        div(
          cls := "app stack-lg",
          Components.header(s(_.appTitle)),
          div(cls := "card", "Unknown activity.")
        )

  private def renderActivity(a: Activity): HtmlElement =
    div(
      cls := "app stack-lg",
      Components.header(s(a.name)),
      a.render(),
      div(
        cls := "no-print",
        suggestAnotherButton()
      )
    )

  private def suggestAnotherButton(): HtmlElement =
    val filtersLabel: Signal[String] =
      AppState.playersFilter.signal
        .combineWith(AppState.handsFreeOnly.signal)
        .combineWith(AppState.strings)
        .map { case (p, hf, str) =>
          val parts = List(p.map(_.label(str)), Option.when(hf)(str.filters.handsFree)).flatten
          parts.mkString(" · ")
        }
    button(
      cls := "btn btn--ghost btn--stacked",
      onClick --> (_ =>
        Routing.replace(Page.Activity(
          Registry.pickRandom(AppState.playersFilter.now(), AppState.handsFreeOnly.now()).id
        ))
      ),
      span(cls := "btn__label", child.text <-- s(_.home.suggestAnother)),
      span(
        cls := "btn__sub muted",
        cls("is-hidden") <-- filtersLabel.map(_.isEmpty),
        child.text <-- filtersLabel
      )
    )
