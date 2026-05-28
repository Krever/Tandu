package tandu.ui

import com.raquo.laminar.api.L.*
import tandu.AppState
import tandu.i18n.Strings

/** A way to play an activity. Each activity can expose one or more modes;
  * if more than one, the user gets a chooser screen before the body. */
final case class Mode(
    id: String,
    label: Strings => String,
    /** Optional list of items the family needs to play this mode.
      * Surfaced on the mode screen itself, not on the activity card. */
    materials: List[Strings => String] = Nil,
    /** Hint text shown under the materials, before the body. */
    hint: Option[Strings => String] = None,
    /** The body shown after the user has chosen this mode. */
    render: () => HtmlElement
)

object ModeChooser:

  /** Render an activity given its supported modes. With a single mode the
    * chooser collapses away; with multiple, the user picks a mode each
    * time they open the activity. */
  def render(modes: List[Mode]): HtmlElement =
    modes match
      case Nil => div(cls := "card", "No modes available.")
      case single :: Nil => bodyOnly(single)
      case _ =>
        val current: Var[Option[Mode]] = Var(None)
        div(
          cls := "stack-lg",
          child <-- current.signal.map {
            case None    => chooser(modes, m => current.set(Some(m)))
            case Some(m) => bodyOnly(m)
          }
        )

  private def chooser(modes: List[Mode], onPick: Mode => Unit): HtmlElement =
    sectionTag(
      cls := "stack mode-chooser",
      h2(cls := "h2", child.text <-- Components.s(_.mode.choose)),
      div(
        cls := "stack",
        modes.map { m =>
          val materialsText: Signal[String] = AppState.strings.map { str =>
            m.materials.map(f => f(str)) match
              case Nil  => ""
              case list => list.mkString(" · ")
          }
          button(
            cls := "tile",
            // Stable hook for e2e tests — avoids matching on translated label text.
            dataAttr("testid") := s"mode-${m.id}",
            div(
              cls := "stack",
              div(cls := "tile__name", child.text <-- AppState.strings.map(m.label)),
              child.maybe <-- materialsText.map(t =>
                Option.when(t.nonEmpty)(div(cls := "tile__desc", t))
              )
            ),
            div(cls := "tile__chev", "›"),
            onClick --> (_ => onPick(m))
          )
        }
      )
    )

  private def bodyOnly(m: Mode): HtmlElement =
    div(
      cls := "stack-lg",
      m.hint.map: hintFn =>
        p(cls := "muted center no-print", child.text <-- AppState.strings.map(hintFn))
      ,
      m.render()
    )
