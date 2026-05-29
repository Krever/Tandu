package tandu

import com.raquo.laminar.api.L.*
import org.scalajs.dom
import tandu.pages.{ActivityPage, HomePage, ToolPage}

@main def main(): Unit =
  Pwa.init()

  // Drive document.title from (route × language) so deep links and lang
  // switches both update the tab — Waypoint's getPageTitle only fires on
  // route changes.
  val _ = Routing.router.currentPageSignal
    .combineWith(AppState.strings)
    .map((p, s) => Routing.title(p, s))
    .foreach(t => dom.document.title = t)(using unsafeWindowOwner)

  val container = dom.document.getElementById("app")
  val app = div(
    // Drop the path before .distinct so navigating within an activity doesn't
    // remount its body — sub-choosers subscribe to the path themselves.
    child <-- Routing.router.currentPageSignal
      .map {
        case Page.Home            => Page.Home
        case Page.Activity(id, _) => Page.Activity(id, Nil)
        case t @ Page.Tool(_)     => t
      }
      .distinct
      .map {
        case Page.Home            => HomePage.render()
        case Page.Activity(id, _) => ActivityPage.render(id)
        case Page.Tool(id)        => ToolPage.render(id)
      }
  )
  val _ = render(container, app)
