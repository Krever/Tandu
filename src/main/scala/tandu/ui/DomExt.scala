package tandu.ui

import org.scalajs.dom

import scala.scalajs.js

/** Gaps in scala-js-dom's facades, papered over in one place. Extensions are
  * only consulted when the member is absent, so each one retires silently
  * once the facade catches up.
  */
object DomExt:

  /** Run `k` one time: when `promise` settles (success or failure), or after
    * `timeoutMs`, whichever comes first. Use it for a wait that must not
    * block the user if the browser is slow, for example a font load or an
    * image decode. */
  def afterOrTimeout(promise: js.Promise[?], timeoutMs: Int)(k: () => Unit): Unit =
    var done = false
    def finish(): Unit = if !done then { done = true; k() }
    val _ = promise.`then`[Unit]((_: Any) => finish(), (_: Any) => finish())
    val _ = js.timers.setTimeout(timeoutMs)(finish())

  extension (el: dom.Element)
    def setPointerCapture(pointerId: Double): Unit =
      val _ = el.asInstanceOf[js.Dynamic].setPointerCapture(pointerId)
