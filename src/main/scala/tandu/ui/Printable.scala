package tandu.ui

import com.raquo.laminar.api.L.*
import org.scalajs.dom
import tandu.AppState
import tandu.i18n.Strings

import scala.scalajs.js

/** Common A4 layout wrapper for activities that print a sheet.
  * Provides margins, a title, and a footer line. The body is the
  * activity-specific content (boards, cards, grids). */
object Printable:

  def print(): Unit = dom.window.print()

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
        onClick --> (_ => print())
      )
    )

  /** A print-only slot that briefly mounts a chosen sheet, fires the browser
    * print dialog, then unmounts it. Use when one page can print more than
    * one variant (e.g. "print maps" vs "print rules") and they shouldn't all
    * appear in every print job. For a single, always-mounted sheet just put
    * the printable inside a `cls := "print-only"` div and use `printButton`.
    */
  final class PrintSlot[K] private[Printable] ():
    private val current: Var[Option[K]] = Var(None)

    def mount(render: K => HtmlElement): HtmlElement =
      div(
        cls := "print-only",
        child <-- current.signal.map(_.fold(emptyNode)(render))
      )

    // 50ms grace lets Laminar commit the freshly mounted body before
    // window.print() snapshots the DOM — without it the preview is blank.
    def trigger(key: K): Unit =
      current.set(Some(key))
      js.timers.setTimeout(50) {
        print()
        current.set(None)
      }

  def printSlot[K](): PrintSlot[K] = new PrintSlot[K]
