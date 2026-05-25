package tandu

import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.*
import com.raquo.waypoint.*

enum Page:
  case Home
  case Activity(id: String)
  case Tool(id: String)

object Routing:

  private val routeHome: Route[Page.Home.type, Unit] =
    Route.static(Page.Home, root / endOfSegments)

  private val routeActivity: Route[Page.Activity, String] =
    Route[Page.Activity, String](
      encode = _.id,
      decode = id => Page.Activity(id),
      pattern = root / "activity" / segment[String] / endOfSegments
    )

  private val routeTool: Route[Page.Tool, String] =
    Route[Page.Tool, String](
      encode = _.id,
      decode = id => Page.Tool(id),
      pattern = root / "tool" / segment[String] / endOfSegments
    )

  private def serialize(page: Page): String = page match
    case Page.Home        => "h"
    case Page.Activity(i) => s"a:$i"
    case Page.Tool(i)     => s"t:$i"

  private def deserialize(s: String): Page =
    if s == "h" then Page.Home
    else if s.startsWith("a:") then Page.Activity(s.drop(2))
    else if s.startsWith("t:") then Page.Tool(s.drop(2))
    else Page.Home

  val router: Router[Page] = new Router[Page](
    routes = List(routeHome, routeActivity, routeTool),
    serializePage = serialize,
    deserializePage = deserialize,
    getPageTitle = _ => "Tandu",
    routeFallback = _ => Page.Home,
    popStateEvents = L.windowEvents(_.onPopState),
    owner = L.unsafeWindowOwner
  )

  def go(page: Page): Unit = router.pushState(page)
  def replace(page: Page): Unit = router.replaceState(page)
