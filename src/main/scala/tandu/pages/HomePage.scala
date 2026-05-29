package tandu.pages

import com.raquo.laminar.api.L.*
import tandu.{AppState, Page, Routing}
import tandu.activities.{ActivityVisual, Players, Registry}
import tandu.tools.Tools
import tandu.ui.Components
import tandu.ui.Components.s

object HomePage:

  def render(): HtmlElement =
    val playersFilter = AppState.playersFilter
    val handsFreeOnly = AppState.handsFreeOnly

    val visible = playersFilter.signal.combineWith(handsFreeOnly.signal).map { (p, hf) =>
      Registry.filtered(p, hf)
    }

    div(
      cls := "app stack-lg",
      Components.header(
        s(_.appTitle),
        back = None,
        subtitle = Some(s(_.tagline)),
        brand = true
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
        div(
          cls := "activity-grid",
          children <-- visible.map { activities =>
            val suggest = Components.suggestCard(
              s(_.home.suggestActivity),
              Routing.go(Page.Activity(Registry.pickRandom(playersFilter.now(), handsFreeOnly.now()).id))
            )
            suggest :: activities.map { a =>
              val v = ActivityVisual.get(a.id)
              Components.activityCard(
                s(a.name),
                s(a.description),
                Routing.go(Page.Activity(a.id)),
                glyph = v.glyph,
                tint = v.tint
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
            val glyph = t.id match
              case "dice"  => "⚀"
              case "timer" => "⏱"
              case _       => "✦"
            Components.tile(
              s(t.name),
              s(t.description),
              Routing.go(Page.Tool(t.id)),
              glyph = glyph
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
