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
    child <-- Routing.router.currentPageSignal.map {
      case Page.Home          => HomePage.render()
      case Page.Activity(id)  => ActivityPage.render(id)
      case Page.Tool(id)      => ToolPage.render(id)
    }
  )
  val _ = render(container, app)
