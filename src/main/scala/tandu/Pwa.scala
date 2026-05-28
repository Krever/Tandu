package tandu

import com.raquo.airstream.state.Var
import com.raquo.airstream.core.Signal
import org.scalajs.dom
import scala.scalajs.js

/** Non-standard event fired by Chromium when the PWA install criteria are
  * met. Not in `org.scalajs.dom` because no spec ratifies it yet. */
@js.native
private trait BeforeInstallPromptEvent extends dom.Event:
  def prompt(): js.Promise[Unit] = js.native
  def userChoice: js.Promise[UserChoiceResult] = js.native

@js.native
private trait UserChoiceResult extends js.Object:
  val outcome: String   // "accepted" | "dismissed"
  val platform: String

/** Safari iOS exposes a non-standard `navigator.standalone` boolean to
  * indicate launch-from-home-screen. Not part of any standard, so we
  * declare a minimal typed facade rather than reaching through
  * `js.Dynamic.global`. */
@js.native
private trait SafariNavigator extends js.Object:
  val standalone: js.UndefOr[Boolean] = js.native

object Pwa:

  private val deferred: Var[Option[BeforeInstallPromptEvent]] = Var(None)

  val available: Signal[Boolean] = deferred.signal.map(_.isDefined)

  def init(): Unit =
    if isStandalone then return

    dom.window.addEventListener[dom.Event](
      "beforeinstallprompt",
      (e: dom.Event) =>
        e.preventDefault()
        deferred.set(Some(e.asInstanceOf[BeforeInstallPromptEvent]))
    )

    dom.window.addEventListener[dom.Event](
      "appinstalled",
      (_: dom.Event) => deferred.set(None)
    )

  def prompt(): Unit =
    deferred.now().foreach: e =>
      e.prompt()
      e.userChoice.`then`((_: UserChoiceResult) => deferred.set(None))

  private def isStandalone: Boolean =
    dom.window.matchMedia("(display-mode: standalone)").matches ||
      dom.window.navigator.asInstanceOf[SafariNavigator].standalone.getOrElse(false)
