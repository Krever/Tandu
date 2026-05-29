package tandu.pages

import com.raquo.laminar.api.L.*
import tandu.{AppState, Kind, Page, Routing}
import tandu.activities.{Activity, ActivityVisual, Players, Registry}
import tandu.tools.Tools
import tandu.ui.Components
import tandu.ui.Components.s

object HomePage:

  def render(): HtmlElement =
    val playersFilter = AppState.playersFilter
    val handsFreeOnly = AppState.handsFreeOnly
    val kindFilter    = AppState.kindFilter

    val visible: Signal[(List[Activity], List[Activity])] =
      playersFilter.signal
        .combineWith(handsFreeOnly.signal)
        .combineWith(kindFilter.signal)
        .map { (p, hf, kind) =>
          Registry.filtered(p, hf, kind).partition(_.kind == Kind.Games)
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
            kindPill(kindFilter),
            playersPill(playersFilter),
            handsFreePill(handsFreeOnly)
          )
        ),
        div(
          cls := "activity-grid",
          children <-- visible.map { (games, learn) =>
            val suggest = Components.suggestCard(
              s(_.home.suggestActivity),
              Routing.go(Page.Activity(
                Registry.pickRandom(playersFilter.now(), handsFreeOnly.now(), kindFilter.now()).id
              ))
            )
            val divider =
              if games.nonEmpty && learn.nonEmpty then List(learnDivider()) else Nil
            suggest :: games.map(activityCard) ++ divider ++ learn.map(activityCard)
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

  private def activityCard(a: Activity): HtmlElement =
    val v = ActivityVisual.get(a.id)
    Components.activityCard(
      s(a.name),
      s(a.description),
      Routing.go(Page.Activity(a.id)),
      glyph = v.glyph,
      tint = v.tint
    )

  private def learnDivider(): HtmlElement =
    div(
      cls := "activity-grid__divider",
      span(cls := "activity-grid__divider-label", child.text <-- s(_.filters.learn))
    )

  private def kindPill(filter: Var[Kind]): HtmlElement =
    val options: List[(Kind, Signal[String])] =
      Kind.values.toList.map(k => (k, s(k.label)))
    Components.segmentedToggle("pill-toggle no-print", "pill-btn", options, filter)

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
