package tandu.pages

import com.raquo.laminar.api.L.*
import com.raquo.laminar.api.L.svg as S
import tandu.{Page, Routing}
import tandu.activities.{Category, Registry}
import tandu.tools.Tools
import tandu.ui.Components
import tandu.ui.Components.s

object HomePage:

  def render(): HtmlElement =
    val filter: Var[Option[Category]] = Var(None)
    val aboutOpen: Var[Boolean] = Var(false)

    div(
      cls := "app stack-lg",
      Components.header(
        s(_.appTitle),
        back = None,
        onInfo = Some(() => aboutOpen.set(true)),
        subtitle = Some(s(_.tagline))
      ),
      Components.modal(
        aboutOpen,
        s(_.about.title),
        s(_.about.body),
        extraActions = Seq(
          a(
            cls := "btn btn--ghost btn--icon",
            href := "https://github.com/Krever/Tandu",
            target := "_blank",
            rel := "noopener noreferrer",
            aria.label := "GitHub",
            title := "GitHub",
            S.svg(
              S.width := "20",
              S.height := "20",
              S.viewBox := "0 0 16 16",
              S.fill := "currentColor",
              S.path(
                S.d := "M8 0C3.58 0 0 3.58 0 8a8 8 0 0 0 5.47 7.59c.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82a7.42 7.42 0 0 1 4 0c1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.01 8.01 0 0 0 16 8c0-4.42-3.58-8-8-8z"
              )
            )
          )
        )
      ),
      Components.primaryBig(
        s(_.home.suggestActivity),
        Routing.go(Page.Activity(Registry.pickRandom(filter.now()).id))
      ),
      sectionTag(
        cls := "stack",
        div(
          cls := "row",
          styleAttr := "justify-content: space-between; align-items: center;",
          h2(cls := "h2", child.text <-- s(_.home.activities)),
          categoryPill(filter)
        ),
        div(
          cls := "activity-grid",
          children <-- filter.signal.map { c =>
            Registry.filtered(c).map { a =>
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

  private def categoryPill(filter: Var[Option[Category]]): HtmlElement =
    val options: List[(Option[Category], Signal[String])] =
      (None, s(_.category.all)) ::
        Category.values.toList.map(c => (Some(c), s(c.label)))
    Components.segmentedToggle("pill-toggle no-print", "pill-btn", options, filter)
