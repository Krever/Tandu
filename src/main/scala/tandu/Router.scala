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
  case Workbook(book: Option[Int] = None)
  /** A shared workbook recipe arriving as a link; `payload` is the encoded
    * recipe, imported on visit (see WorkbookPage). */
  case WorkbookShared(payload: String)

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

  // The books list is a value-equality static route (it must not swallow the
  // editor pages); the editor route is total over Page.Workbook, so order
  // matters: list first, editor as the fallback for Some(idx).
  private val routeWorkbookList: Route[Page.Workbook, Unit] =
    Route.staticPartial(Page.Workbook(None), root / "workbook" / endOfSegments)

  private val routeWorkbookEditor: Route[Page.Workbook, Int] =
    Route[Page.Workbook, Int](
      encode = _.book.getOrElse(0),
      decode = i => Page.Workbook(Some(i)),
      pattern = root / "workbook" / segment[Int] / endOfSegments
    )

  private val routeWorkbookShared: Route[Page.WorkbookShared, String] =
    Route[Page.WorkbookShared, String](
      encode = _.payload,
      decode = p => Page.WorkbookShared(p),
      pattern = root / "workbook" / "shared" / segment[String] / endOfSegments
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
    case Page.Workbook(None)     => "w"
    case Page.Workbook(Some(i))  => s"w:$i"
    case Page.WorkbookShared(p)  => s"ws:$p"

  private def deserialize(s: String): Page =
    if s == "h" then Page.Home
    else if s == "w" then Page.Workbook(None)
    else if s.startsWith("ws:") then Page.WorkbookShared(s.drop(3))
    else if s.startsWith("w:") then Page.Workbook(s.drop(2).toIntOption)
    else if s.startsWith("a:") then
      val parts = s.drop(2).split("/").toList
      parts match
        case Nil       => Page.Home
        case id :: tail => Page.Activity(id, tail)
    else if s.startsWith("t:") then Page.Tool(s.drop(2))
    else Page.Home

  val router: Router[Page] = new Router[Page](
    routes = List(routeHome, routeWorkbookList, routeWorkbookShared, routeWorkbookEditor, routeActivity, routeTool),
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
    case Page.Workbook(_)        => s"${s.workbook.name} — ${s.appTitle}"
    case Page.WorkbookShared(_)  => s"${s.workbook.name} — ${s.appTitle}"
