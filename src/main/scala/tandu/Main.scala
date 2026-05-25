package tandu

import com.raquo.laminar.api.L.*
import org.scalajs.dom
import tandu.pages.{ActivityPage, HomePage, ToolPage}

@main def main(): Unit =
  Pwa.init()
  val container = dom.document.getElementById("app")
  val app = div(
    child <-- Routing.router.currentPageSignal.map {
      case Page.Home          => HomePage.render()
      case Page.Activity(id)  => ActivityPage.render(id)
      case Page.Tool(id)      => ToolPage.render(id)
    }
  )
  render(container, app)
