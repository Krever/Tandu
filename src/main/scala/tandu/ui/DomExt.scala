package tandu.ui

import org.scalajs.dom

import scala.scalajs.js

/** Gaps in scala-js-dom's facades, papered over in one place. Extensions are
  * only consulted when the member is absent, so each one retires silently
  * once the facade catches up.
  */
object DomExt:

  extension (el: dom.Element)
    def setPointerCapture(pointerId: Double): Unit =
      val _ = el.asInstanceOf[js.Dynamic].setPointerCapture(pointerId)
