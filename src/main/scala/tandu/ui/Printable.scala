package tandu.ui

import com.raquo.laminar.api.L.*
import tandu.AppState
import tandu.i18n.Strings

/** Common A4 layout wrapper for activities that print a sheet.
  * Provides margins, a title, and a footer line. The body is the
  * activity-specific content (boards, cards, grids). */
object Printable:

  def render(
      title: Strings => String,
      body: HtmlElement,
      pageBreakAfter: Boolean = false
  ): HtmlElement =
    div(
      cls := "printable",
      cls("printable--break") := pageBreakAfter,
      headerTag(
        cls := "printable__header",
        h2(cls := "printable__title", child.text <-- AppState.strings.map(title)),
        div(cls := "printable__brand", "Tandu")
      ),
      div(cls := "printable__body", body),
      footerTag(
        cls := "printable__footer muted",
        child.text <-- AppState.strings.map(s => s"${s.appTitle} — ${s.tagline}")
      )
    )

  def printButton(label: Strings => String = _.printable.print): HtmlElement =
    div(
      cls := "no-print center",
      button(
        cls := "btn btn--lg",
        child.text <-- AppState.strings.map(label),
        onClick --> (_ => ModeChooser.print())
      )
    )
