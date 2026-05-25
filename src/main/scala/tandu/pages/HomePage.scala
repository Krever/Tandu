package tandu.pages

import com.raquo.laminar.api.L.*
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
        onInfo = Some(() => aboutOpen.set(true))
      ),
      Components.modal(aboutOpen, s(_.about.title), s(_.about.body)),
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
          cls := "stack",
          children <-- filter.signal.map { c =>
            Registry.filtered(c).map { a =>
              Components.tile(
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
