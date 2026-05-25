package tandu.pages

import com.raquo.laminar.api.L.*
import tandu.{Page, Routing}
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
        Components.ghost(
          s(_.suggestAnother),
          Routing.replace(Page.Activity(Registry.pickRandom().id))
        )
      )
    )
