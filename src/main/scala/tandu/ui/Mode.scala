package tandu.ui

import com.raquo.laminar.api.L.*
import tandu.{AppState, Page, Routing}
import tandu.i18n.Strings

/** A choice at one level of an activity's URL path. The top level is usually
  * mode (in-app/paper/print/...); nested levels are typically variants
  * (classic/gomoku, easy/medium/hard). */
final case class Mode(
    id: String,
    label: Strings => String,
    description: Option[Strings => String] = None,
    materials: List[Strings => String] = Nil,
    hint: Option[Strings => String] = None,
    experimental: Boolean = false,
    testIdPrefix: String = "mode",
    render: () => HtmlElement
)

object ModeChooser:

  /** Render a chooser that consumes one path segment.
    *
    * `prefix` is the path consumed by outer choosers; the chooser reads
    * `path(prefix.length)` for selection and pushes `prefix :+ pick.id` on
    * click. With a single mode the chooser collapses to the body. */
  def render(
      activityId: String,
      modes: List[Mode],
      prefix: List[String] = Nil,
      heading: Strings => String = _.mode.choose
  ): HtmlElement =
    modes match
      case Nil           => div(cls := "card", "No options available.")
      case single :: Nil => bodyOnly(single)
      case _ =>
        val current: Signal[Option[Mode]] =
          Routing.router.currentPageSignal.map {
            case Page.Activity(id, path) if id == activityId =>
              path.lift(prefix.length).flatMap(s => modes.find(_.id == s))
            case _ => None
          }.distinct
        div(
          cls := "stack-lg",
          child <-- current.map {
            case None    => chooser(activityId, modes, prefix, heading)
            case Some(m) => bodyOnly(m)
          }
        )

  private def chooser(
      activityId: String,
      modes: List[Mode],
      prefix: List[String],
      heading: Strings => String
  ): HtmlElement =
    sectionTag(
      cls := "stack mode-chooser",
      h2(cls := "h2", child.text <-- Components.s(heading)),
      div(
        cls := "stack",
        modes.map { m =>
          val subText: Signal[String] = AppState.strings.map { str =>
            m.description.map(_(str)).getOrElse {
              m.materials.map(_(str)) match
                case Nil  => ""
                case list => list.mkString(" · ")
            }
          }
          button(
            cls := "tile",
            dataAttr("testid") := s"${m.testIdPrefix}-${m.id}",
            div(
              cls := "stack",
              div(
                cls := "tile__name",
                span(child.text <-- AppState.strings.map(m.label)),
                if m.experimental then
                  span(cls := "tile__badge", child.text <-- Components.s(_.mode.experimentalBadge))
                else emptyNode
              ),
              child.maybe <-- subText.map(t =>
                Option.when(t.nonEmpty)(div(cls := "tile__desc", t))
              )
            ),
            div(cls := "tile__chev", "›"),
            onClick --> (_ => Routing.go(Page.Activity(activityId, prefix :+ m.id)))
          )
        }
      )
    )

  private def bodyOnly(m: Mode): HtmlElement =
    div(
      cls := "stack-lg",
      if m.experimental then
        Components.banner("warn", Components.s(_.mode.experimentalWarning))
      else emptyNode
      ,
      m.hint.map: hintFn =>
        p(cls := "muted center no-print", child.text <-- AppState.strings.map(hintFn))
      ,
      m.render()
    )
