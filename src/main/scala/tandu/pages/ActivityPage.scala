package tandu.pages

import com.raquo.laminar.api.L.*
import tandu.{AppState, Page, Routing}
import tandu.activities.{Activity, Registry}
import tandu.ui.{Components, SuggestSpin}
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
    // Holds the in-flight slot-reel spin; cleared once it lands and navigates.
    val spin = Var(Option.empty[SuggestSpin.Reel])
    div(
      cls := "app stack-lg",
      SuggestSpin.overlay(spin, picked => { spin.set(None); Routing.replace(Page.Activity(picked.id)) }),
      Components.header(s(a.name), glyph = Some(a.glyph), tint = Some(a.tint)),
      a.render(),
      suggestBar(spin)
    )

  /** The "give me the next one" loop control — a sticky bar docked to the bottom
    * of every activity, always one tap away as you play. Vermilion (ties it to
    * the home Suggest hero) with a shuffle glyph that reads as "another", and
    * the active filters shown beneath so the draw's scope is clear. */
  private def suggestBar(spin: Var[Option[SuggestSpin.Reel]]): HtmlElement =
    val filtersLabel: Signal[String] =
      AppState.playersFilter.signal
        .combineWith(AppState.handsFreeOnly.signal)
        .combineWith(AppState.strings)
        .map { case (p, hf, str) =>
          val parts = List(p.map(_.label(str)), Option.when(hf)(str.filters.handsFree)).flatten
          parts.mkString(" · ")
        }
    div(
      cls := "activity-bar no-print",
      hr(cls := "activity-bar__rule"),
      button(
        cls := "activity-resuggest",
        onClick --> { _ =>
          val pool = Registry.filtered(
            AppState.playersFilter.now(),
            AppState.handsFreeOnly.now(),
            AppState.kindFilter.now(),
            AppState.favouritesOnly.now(),
            AppState.favourites.now(),
            AppState.hidden.now()
          )
          val pick = Registry.pickRandom(pool)
          spin.set(Some(SuggestSpin.build(pool, pick)))
        },
        span(
          cls := "activity-resuggest__main",
          span(cls := "activity-resuggest__chev", "›"),
          span(cls := "activity-resuggest__word", child.text <-- s(_.home.suggestAnother)),
          span(cls := "activity-resuggest__chev", "‹")
        ),
        span(
          cls := "activity-resuggest__sub",
          cls("is-hidden") <-- filtersLabel.map(_.isEmpty),
          child.text <-- filtersLabel
        )
      )
    )
