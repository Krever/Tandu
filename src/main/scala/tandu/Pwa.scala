package tandu

import com.raquo.airstream.state.Var
import com.raquo.airstream.core.Signal
import org.scalajs.dom
import scala.scalajs.js

object Pwa:

  private val deferred: Var[Option[js.Dynamic]] = Var(None)

  val available: Signal[Boolean] = deferred.signal.map(_.isDefined)

  def init(): Unit =
    if isStandalone then return

    dom.window.addEventListener[dom.Event](
      "beforeinstallprompt",
      (e: dom.Event) =>
        e.preventDefault()
        deferred.set(Some(e.asInstanceOf[js.Dynamic]))
    )

    dom.window.addEventListener[dom.Event](
      "appinstalled",
      (_: dom.Event) => deferred.set(None)
    )

  def prompt(): Unit =
    deferred.now().foreach: e =>
      e.prompt()
      val choice = e.userChoice.asInstanceOf[js.Promise[js.Dynamic]]
      choice.`then`((_: js.Dynamic) => deferred.set(None))

  private def isStandalone: Boolean =
    dom.window.matchMedia("(display-mode: standalone)").matches ||
      js.Dynamic.global.navigator.standalone.asInstanceOf[js.UndefOr[Boolean]].getOrElse(false)
