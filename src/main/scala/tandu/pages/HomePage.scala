package tandu.pages

import com.raquo.laminar.api.L.*
import tandu.{Page, Routing}
import tandu.activities.{Players, Registry}
import tandu.tools.Tools
import tandu.ui.Components
import tandu.ui.Components.s

object HomePage:

  def render(): HtmlElement =
    val playersFilter: Var[Option[Players]] = Var(None)
    val handsFreeOnly: Var[Boolean]         = Var(false)

    val visible = playersFilter.signal.combineWith(handsFreeOnly.signal).map { (p, hf) =>
      Registry.filtered(p, hf)
    }

    div(
      cls := "app stack-lg",
      Components.header(
        s(_.appTitle),
        back = None,
        subtitle = Some(s(_.tagline))
      ),
      sectionTag(
        cls := "stack",
        div(
          cls := "row",
          styleAttr := "justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 0.5rem;",
          h2(cls := "h2", child.text <-- s(_.home.activities)),
          div(
            cls := "row",
            styleAttr := "gap: 0.5rem; flex-wrap: wrap;",
            playersPill(playersFilter),
            handsFreePill(handsFreeOnly)
          )
        ),
        Components.primaryBig(
          s(_.home.suggestActivity),
          Routing.go(Page.Activity(Registry.pickRandom(playersFilter.now(), handsFreeOnly.now()).id))
        ),
        div(
          cls := "activity-grid",
          children <-- visible.map { activities =>
            activities.map { a =>
              Components.activityCard(
                s(a.name),
                s(a.description),
                Routing.go(Page.Activity(a.id))
              )
            }
          }
        )
      ),
      sectionTag(
        cls := "stack",
        h2(cls := "h2", child.text <-- s(_.home.tools)),
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

  private def playersPill(filter: Var[Option[Players]]): HtmlElement =
    val options: List[(Option[Players], Signal[String])] =
      (None, s(_.filters.all)) ::
        Players.values.toList.map(p => (Some(p), s(p.label)))
    Components.segmentedToggle("pill-toggle no-print", "pill-btn", options, filter)

  private def handsFreePill(toggle: Var[Boolean]): HtmlElement =
    div(
      cls := "pill-toggle no-print",
      button(
        cls := "pill-btn",
        cls("is-active") <-- toggle.signal,
        child.text <-- s(_.filters.handsFree),
        onClick --> (_ => toggle.update(!_))
      )
    )
