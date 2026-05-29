package tandu

import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.*
import com.raquo.waypoint.*
import tandu.activities.Registry
import tandu.i18n.Strings
import tandu.tools.Tools

enum Page:
  case Home
  case Activity(id: String, path: List[String] = Nil)
  case Tool(id: String)

object Routing:

  // Use staticPartial (value-equality match) rather than static. With Scala 3
  // enum cases, ClassTag[Page.Home.type].runtimeClass is the erased enum class
  // (Page), so a Total route's `case p: Page.Home.type` matches every Page —
  // making routeHome swallow Activity/Tool pages during encode and pushState
  // would always set the URL to "/".
  private val routeHome: Route[Page.Home.type, Unit] =
    Route.staticPartial(Page.Home, root / endOfSegments)

  private val routeActivity: Route[Page.Activity, (String, List[String])] =
    Route[Page.Activity, (String, List[String])](
      encode = a => (a.id, a.path),
      decode = (id, path) => Page.Activity(id, path),
      pattern = root / "activity" / segment[String] / remainingSegments
    )

  private val routeTool: Route[Page.Tool, String] =
    Route[Page.Tool, String](
      encode = _.id,
      decode = id => Page.Tool(id),
      pattern = root / "tool" / segment[String] / endOfSegments
    )

  private def serialize(page: Page): String = page match
    case Page.Home               => "h"
    case Page.Activity(i, Nil)   => s"a:$i"
    case Page.Activity(i, path)  => s"a:$i/${path.mkString("/")}"
    case Page.Tool(i)            => s"t:$i"

  private def deserialize(s: String): Page =
    if s == "h" then Page.Home
    else if s.startsWith("a:") then
      val parts = s.drop(2).split("/").toList
      parts match
        case Nil       => Page.Home
        case id :: tail => Page.Activity(id, tail)
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

  /** Document title for a page in the current language. Used by the
    * Main subscription so the browser tab and shared-link previews
    * reflect the activity/tool the user is on. */
  def title(page: Page, s: Strings): String = page match
    case Page.Home            => s.appTitle
    case Page.Activity(id, _) =>
      Registry.byId(id).map(a => s"${a.name(s)} — ${s.appTitle}").getOrElse(s.appTitle)
    case Page.Tool(id)        =>
      Tools.byId(id).map(t => s"${t.name(s)} — ${s.appTitle}").getOrElse(s.appTitle)
