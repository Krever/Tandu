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

  private val DecodeTimeoutMs = 3000

  /** `true` during the wait for the images. A second click on a print
    * button in that time must not open a second print dialog. */
  private var printPending = false

  /** Open the print dialog of the browser, then run `done`.
    *
    * The printed sheets are hidden on the screen, thus the browser has not
    * decoded their images. A print that starts immediately can show a sheet
    * with no pictures. This function first waits until all images in the
    * print-only areas are decoded, with a time limit. A call during that
    * wait does nothing, and its `done` does not run. */
  def print(done: () => Unit = () => ()): Unit = if !printPending then
    printPending = true
    def go(): Unit =
      printPending = false
      dom.window.print()
      done()
    val images = dom.document.querySelectorAll(".print-only img, .print-only image")
    val decodes = (0 until images.length).flatMap { i =>
      val el = images(i).asInstanceOf[js.Dynamic]
      if js.typeOf(el.decode) == "function" then
        // An image that cannot decode must not stop the print.
        Some(el.decode().asInstanceOf[js.Promise[Any]].`catch`[Any]((_: Any) => ()))
      else None
    }
    if decodes.isEmpty then go()
    else DomExt.afterOrTimeout(js.Promise.all(js.Array(decodes*)), DecodeTimeoutMs)(() => go())

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
      val _ = js.timers.setTimeout(50) {
        print(() => current.set(None))
      }

  def printSlot[K](): PrintSlot[K] = new PrintSlot[K]
