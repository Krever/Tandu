package tandu.pages

import com.raquo.laminar.api.L.*
import tandu.{AppState, Page, Routing}
import tandu.activities.Registry
import tandu.tools.Tools
import tandu.ui.Components
import tandu.ui.Components.s

object HomePage:

  def render(): HtmlElement =
    div(
      cls := "app stack-lg",
      Components.header(s(_.appTitle), back = None),
      Components.primaryBig(
        s(_.suggestActivity),
        Routing.go(Page.Activity(Registry.pickRandom().id))
      ),
      sectionTag(
        cls := "stack",
        h2(cls := "h2", child.text <-- s(_.activities)),
        div(
          cls := "stack",
          Registry.all.map: a =>
            Components.tile(
              s(a.name),
              s(a.description),
              Routing.go(Page.Activity(a.id))
            )
        )
      ),
      sectionTag(
        cls := "stack",
        h2(cls := "h2", child.text <-- s(_.tools)),
        div(
          cls := "stack",
          Tools.all.map: t =>
            Components.tile(
              s(t.name),
              s(t.description),
              Routing.go(Page.Tool(t.id))
            )
        )
      )
    )
