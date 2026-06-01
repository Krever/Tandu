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
  * `js.Dynamic.global`. `maxTouchPoints` lets us spot iPadOS, which
  * masquerades as desktop Safari in its user-agent string. */
@js.native
private trait SafariNavigator extends js.Object:
  val standalone: js.UndefOr[Boolean] = js.native
  val maxTouchPoints: js.UndefOr[Int] = js.native

object Pwa:

  private val deferred: Var[Option[BeforeInstallPromptEvent]] = Var(None)

  val available: Signal[Boolean] = deferred.signal.map(_.isDefined)

  /** iOS has no `beforeinstallprompt` and no programmatic install — the user
    * must use Safari's Share → "Add to Home Screen". When we're on an iOS
    * device that isn't already running standalone, we surface a help sheet
    * instead of a one-tap install button. */
  lazy val needsManualInstall: Boolean = isIos && !isStandalone

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
      val _ = e.prompt()
      e.userChoice.`then`((_: UserChoiceResult) => deferred.set(None))

  private def isStandalone: Boolean =
    dom.window.matchMedia("(display-mode: standalone)").matches ||
      dom.window.navigator.asInstanceOf[SafariNavigator].standalone.getOrElse(false)

  private def isIos: Boolean =
    val nav = dom.window.navigator
    val ua  = nav.userAgent
    val phoneOrPad = ua.contains("iPhone") || ua.contains("iPad") || ua.contains("iPod")
    // iPadOS 13+ reports a Mac user-agent; touch points disambiguate it.
    val iPadOS = ua.contains("Macintosh") &&
      nav.asInstanceOf[SafariNavigator].maxTouchPoints.getOrElse(0) > 1
    phoneOrPad || iPadOS
