package tandu.activities

import com.raquo.laminar.api.L.*
import org.scalajs.dom
import tandu.{AppState, Page, Routing}
import tandu.i18n.Strings
import tandu.ui.{Components, Mode, ModeChooser, RulesCard, SheetBuilder}
import tandu.ui.Components.s
import tandu.ui.DomExt.*

import scala.scalajs.js
import scala.util.Random

object Maze extends Activity:
  val id = "maze"
  def name(s: Strings): String = s.maze.name
  def description(s: Strings): String = s.maze.description
  val minPlayers: Int = 1
  val maxPlayers: Int = 1
  val glyph: String = "🐭"
  val tint: String = "peach"

  // ---------- model ----------

  /** SVG user units per cell. The whole maze is drawn in this coordinate space
    * and scaled to fit by the viewBox, so the same geometry serves both the
    * responsive on-screen board and the fixed-mm print sheet. */
  private val CELL = 10
  private def cx(c: Int): Double = c * CELL + CELL / 2.0
  private def cy(r: Int): Double = r * CELL + CELL / 2.0

  // A pointer gesture counts as a swipe (→ auto-run down the corridor) when it
  // travels at least this far in screen pixels and lasts no longer than this —
  // a quick flick, as opposed to a slow, deliberate cell-by-cell trace.
  private val SwipeMinPx = 28.0
  private val SwipeMaxMs = 500.0

  /** A perfect (or lightly braided) maze on an orthogonal grid. Rather than a
    * per-cell wall set we store, for each cell, whether the wall to its right
    * (east) and below (south) is carved open — every interior wall is shared by
    * exactly one such cell, so this is the minimal non-redundant encoding. */
  final case class Grid(rows: Int, cols: Int, openRight: Vector[Boolean], openDown: Vector[Boolean]):
    def idx(r: Int, c: Int): Int = r * cols + c
    def inBounds(r: Int, c: Int): Boolean = r >= 0 && r < rows && c >= 0 && c < cols
    def rightOpen(r: Int, c: Int): Boolean = c < cols - 1 && openRight(idx(r, c))
    def downOpen(r: Int, c: Int): Boolean  = r < rows - 1 && openDown(idx(r, c))

    /** Whether you can step directly between two orthogonally-adjacent cells.
      * Callers (arrow keys, swipe-run) probe off-grid cells freely, so we reject
      * anything out of bounds first. Every passage is stored as the right/down
      * wall of the pair's upper-left cell, so we normalise to it with `min`. */
    def connected(a: (Int, Int), b: (Int, Int)): Boolean =
      val (r1, c1) = a
      val (r2, c2) = b
      if !inBounds(r1, c1) || !inBounds(r2, c2) then false
      else if math.abs(r1 - r2) + math.abs(c1 - c2) != 1 then false
      else if r1 == r2 then rightOpen(r1, math.min(c1, c2))
      else downOpen(math.min(r1, r2), c1)

  // ---------- generator ----------

  /** How the maze is carved. Each style makes a spanning tree of the grid:
    * there is exactly one path between any two cells (before loops are
    * added). The styles differ in the feel of the paths:
    *   - Winding: recursive backtracker (randomised DFS). Long corridors and
    *     few dead ends; the path is long but easy to follow.
    *   - Balanced: Wilson's algorithm (loop-erased random walks). A uniform
    *     random spanning tree, the "puzzle book" look.
    *   - Branchy: Prim's algorithm with a random frontier. Many short dead
    *     ends, which give many decisions: the hardest style at one size. */
  enum Style:
    case Winding, Balanced, Branchy

  /** Carve a maze of `rows` × `cols` cells in the given style (see
    * [[MazeGraph.carve]], which also makes the other shapes).
    *
    * `braid` (0..1) optionally knocks an extra wall out of that fraction of
    * dead-ends, turning some into gentle loops. We use it on the easy tier so
    * the youngest solvers meet fewer frustrating dead-ends. */
  def generate(rows: Int, cols: Int, braid: Double, rng: Random = new Random()): Grid =
    toGrid(MazeGraph.carve(MazeGraph.RectLayout(rows, cols), Style.Winding, braid, rng))

  /** The in-app and square print drawing read a [[Grid]]. */
  private def toGrid(m: MazeGraph.Carved): Grid = m.layout match
    case MazeGraph.RectLayout(rows, cols) =>
      Grid(rows, cols,
        Vector.tabulate(rows * cols)(i => i % cols < cols - 1 && m.open(i, i + 1)),
        Vector.tabulate(rows * cols)(i => i / cols < rows - 1 && m.open(i, i + cols)))
    case other => throw new IllegalArgumentException(s"not a square maze: $other")

  // ---------- play state ----------

  /** `trail` is the breadcrumb path, head-first: `trail.head` is the mouse's
    * current cell, the last element is the start. */
  final case class Play(grid: Grid, trail: List[(Int, Int)]):
    def head: (Int, Int)  = trail.head
    def start: (Int, Int) = (0, 0)
    def goal: (Int, Int)  = (grid.rows - 1, grid.cols - 1)
    def won: Boolean      = head == goal

  private def newPlay(grid: Grid): Play = Play(grid, List((0, 0)))

  /** Move the mouse to an adjacent cell if a passage allows it. Walking back
    * onto an already-visited cell rewinds the trail to there — so reversing
    * erases your steps and crossing your own path (possible on braided mazes)
    * never leaves a tangled line. */
  private def step(p: Play, to: (Int, Int)): Play =
    if p.won then p
    else if !p.grid.connected(p.head, to) then p
    else
      p.trail.indexOf(to) match
        case -1 => p.copy(trail = to :: p.trail)
        case i  => p.copy(trail = p.trail.drop(i))

  /** Swipe-to-run: send the mouse off in one direction and let it follow the
    * corridor, taking forced turns automatically and stopping only where the
    * player has a real choice (a junction), a wall, or the cheese. One flick
    * clears a whole passage — the only sane way to solve a big maze on a phone.
    * Capped at the cell count so a braided loop can't spin forever. */
  private def runDir(p: Play, dr0: Int, dc0: Int): Play =
    var cur = p
    var dr = dr0
    var dc = dc0
    var go = true
    var steps = 0
    val cap = p.grid.rows * p.grid.cols
    while go && !cur.won && steps < cap do
      val (r, c) = cur.head
      if !cur.grid.connected((r, c), (r + dr, c + dc)) then go = false
      else
        cur = step(cur, (r + dr, c + dc))
        steps += 1
        val (hr, hc) = cur.head
        val back = (hr - dr, hc - dc)
        val exits = List((hr - 1, hc), (hr + 1, hc), (hr, hc - 1), (hr, hc + 1))
          .filter(n => n != back && cur.grid.connected((hr, hc), n))
        exits match
          case (nr, nc) :: Nil => dr = nr - hr; dc = nc - hc // forced turn — keep running
          case _               => go = false                 // junction or dead-end — let the player decide
    cur

  // ---------- variants ----------

  /** The in-app levels. The printable sheets have their own recipe: see
    * [[SheetConfig]]. */
  final case class Variant(
      id: String,
      rows: Int,
      cols: Int,
      braid: Double,
      nameKey: Strings => String,
      descKey: Strings => String
  )

  val variants: List[Variant] = List(
    Variant("easy",   10, 10, 0.30, _.maze.easy.name,   _.maze.easy.description),
    Variant("medium", 16, 16, 0.0,  _.maze.medium.name, _.maze.medium.description),
    Variant("hard",   24, 24, 0.0,  _.maze.hard.name,   _.maze.hard.description)
  )

  // ---------- printable sheet recipe ----------

  /** How many dead ends of the maze are opened into loops (see `generate`).
    * Loops give more than one way through, which is easier. */
  enum Loops(val braid: Double):
    case Off  extends Loops(0.0)
    case Few  extends Loops(0.3)
    case Many extends Loops(0.6)

  /** How many mazes are on one page, and the space that each maze gets.
    *   - `maxSize`: the largest size for this layout. Larger mazes would
    *     have cells too small to draw a line through (less than 5.5 mm).
    *   - `slotMm`: (width, height) of the space for one maze, in mm. The
    *     A4 body is approximately 190 × 255 mm. The slots use only 230 mm
    *     of the height, so that the sheet also fits on US Letter paper
    *     (body approximately 190 × 243 mm). 2 per page are one above the
    *     other; 4 per page are a 2 × 2 grid; the gap is 6 mm. */
  enum PerPage(val n: Int, val maxSize: Int, val slotMm: (Double, Double)):
    case One  extends PerPage(1, 30, (190.0, 230.0))
    case Two  extends PerPage(2, 20, (190.0, 112.0))
    case Four extends PerPage(4, 16, (92.0, 112.0))

  /** The shape of a maze. The order is the order of the URL token and of
    * the form chips. */
  enum Shape:
    case Square, Circle, Hex

  val AllShapes: Set[Shape] = Shape.values.toSet

  /** The full recipe of one printable sheet. The builder form edits this
    * value and nothing else.
    *   - `size`: cells on the short side of each maze, from
    *     [[SheetConfig.MinSize]] to `perPage.maxSize` (see `rings` and
    *     `hexDims` for the other shapes).
    *   - `shapes`: the shapes that the pages use; never empty. */
  final case class SheetConfig(size: Int, loops: Loops, perPage: PerPage, style: Style, shapes: Set[Shape])

  object SheetConfig:
    val MinSize = 5

  extension (c: SheetConfig)
    /** (rows, cols). The short side has `size` cells. The long side follows
      * the shape of the slot, so the maze fills its space on the page. */
    def dims: (Int, Int) =
      val (w, h) = c.perPage.slotMm
      if w <= h then ((c.size * h / w).toInt, c.size)
      else (c.size, (c.size * w / h).toInt)

    /** (rows, cols) of a hex maze. The count of cells is the nearest to
      * the square maze of the same size, up to the largest that the paper
      * allows (see [[hexMaxShort]]). */
    def hexDims: (Int, Int) =
      val (rows, cols) = c.dims
      val n = nearest(3 to hexMaxShort(c.perPage), rows * cols)(k => hexDimsFor(c.perPage, k)._1 * hexDimsFor(c.perPage, k)._2)
      hexDimsFor(c.perPage, n)

    /** The rings of a circle maze. The count of cells is the nearest to
      * the square maze of the same size, up to the largest that the paper
      * allows (see [[circleMaxRings]]). */
    def rings: Int =
      val (rows, cols) = c.dims
      nearest(2 to circleMaxRings(c.perPage), rows * cols)(r => MazeGraph.CircleLayout.ringSizes(r).sum)

    /** Add or remove one shape. The last shape cannot be removed. */
    def withShape(s: Shape): SheetConfig =
      if !c.shapes(s) then c.copy(shapes = c.shapes + s)
      else if c.shapes.size > 1 then c.copy(shapes = c.shapes - s)
      else c

    def withAllShapes: SheetConfig = c.copy(shapes = AllShapes)

  /** Of `options`, the one whose `cells` is nearest to `target` by ratio.
    * By ratio, not by difference: the cell counts of circle mazes grow in
    * large steps when the mazes are small. */
  private def nearest(options: Range, target: Int)(cells: Int => Int): Int =
    options.minBy(k => math.abs(math.log(cells(k).toDouble / target)))

  /** The hex side length in mm when `n` cells are on the short side. */
  private def hexSide(pp: PerPage, n: Int): Double =
    val (w, h) = pp.slotMm
    if w <= h then w / (math.sqrt(3) * (n + 0.5)) else h / (1.5 * n + 0.5)

  /** The hex side length in mm of a `rows` × `cols` hex maze in its slot:
    * the largest that fits. */
  private[activities] def hexSideMm(pp: PerPage, rows: Int, cols: Int): Double =
    val (w, h) = pp.slotMm
    math.min(w / (math.sqrt(3) * (cols + 0.5)), h / (1.5 * rows + 0.5))

  /** (rows, cols) of a hex maze with `n` cells on the short side. The other
    * count follows from the slot: the width across the flats is √3·s and
    * the row step is 1.5·s (s = side length). */
  private def hexDimsFor(pp: PerPage, n: Int): (Int, Int) =
    val (w, h) = pp.slotMm
    val side = hexSide(pp, n)
    if w <= h then (((h / side - 0.5) / 1.5).toInt, n)
    else (n, (w / (math.sqrt(3) * side) - 0.5).toInt)

  /** The most hex cells on the short side that the paper allows: a
    * passage is one hex edge, which must be at least 4 mm, and the width
    * across the flats at least 5.5 mm. */
  private lazy val hexMaxShort: Map[PerPage, Int] = PerPage.values.map { pp =>
    pp -> (3 to 200).takeWhile(n => hexSide(pp, n) >= 4.0 && math.sqrt(3) * hexSide(pp, n) >= 5.5).last
  }.toMap

  /** The most circle rings that the paper allows: the ring width must be
    * at least 5.5 mm, and the narrowest doorway (inner arc of a cell) at
    * least 4 mm. */
  private lazy val circleMaxRings: Map[PerPage, Int] = PerPage.values.map { pp =>
    val (w, h) = pp.slotMm
    pp -> (2 to 200).takeWhile { r =>
      val ring = math.min(w, h) / 2 / (r + 1)
      val sizes = MazeGraph.CircleLayout.ringSizes(r)
      val doorway = (1 to r).map(i => 2 * math.Pi * i / sizes(i)).min * ring
      ring >= 5.5 && doorway >= 4.0
    }.last
  }.toMap

  val sheetPresets: List[SheetBuilder.Preset[SheetConfig]] = List(
    SheetBuilder.Preset("easy", _.maze.easy.name, SheetConfig(11, Loops.Few, PerPage.Four, Style.Winding, AllShapes)),
    SheetBuilder.Preset("medium", _.maze.medium.name, SheetConfig(13, Loops.Off, PerPage.Four, Style.Balanced, AllShapes)),
    SheetBuilder.Preset("hard", _.maze.hard.name, SheetConfig(24, Loops.Off, PerPage.One, Style.Branchy, AllShapes))
  )

  /** The settings token of a preset, for workbook rows. */
  def presetToken(presetId: String): String = SheetBuilder.presetToken(sheetSpec, presetId)

  /** The recipe as one URL path segment, for example `s11.winding.few.pp4`
    * or `s11.winding.few.pp4.circle-hex`. All shapes (as in the presets)
    * have no shapes token; other sets are named. */
  def encodeConfig(c: SheetConfig): String =
    val shapes =
      if c.shapes == AllShapes then ""
      else "." + Shape.values.filter(c.shapes).map(_.toString.toLowerCase).mkString("-")
    s"s${c.size}.${c.style.toString.toLowerCase}.${c.loops.toString.toLowerCase}.pp${c.perPage.n}$shapes"

  private val SizeToken = "s(\\d+)".r
  private val PerPageToken = "pp(\\d+)".r

  /** The recipe from a URL path segment. The order of tokens does not
    * matter. A missing or bad token keeps the value of the first preset,
    * and the size is clamped to the limits of the layout. Therefore an edited
    * or old link always gives a valid recipe. */
  def decodeConfig(token: String): SheetConfig =
    val raw = token.split('.').foldLeft(sheetPresets.head.config) { (c, t) =>
      SheetBuilder.named(Loops.values, t).map(v => c.copy(loops = v))
        .orElse(SheetBuilder.named(Style.values, t).map(v => c.copy(style = v)))
        .getOrElse(t match
        // toIntOption: a digit run too long for an Int must not throw.
        case SizeToken(n)    => n.toIntOption.fold(c)(v => c.copy(size = v))
        case PerPageToken(n) => PerPage.values.find(p => n.toIntOption.contains(p.n)).fold(c)(v => c.copy(perPage = v))
        case _ =>
          val parts = t.split('-').toList.map(p => SheetBuilder.named(Shape.values, p))
          // A token of only dashes splits into no parts; it must not give
          // a recipe with no shapes.
          if parts.nonEmpty && parts.forall(_.isDefined) then c.copy(shapes = parts.flatten.toSet) else c
      )
    }
    raw.copy(size = SheetBuilder.clamp(raw.size, SheetConfig.MinSize, raw.perPage.maxSize))

  // ---------- UI ----------

  def render(): HtmlElement =
    ModeChooser.render(id, List(
      Mode(
        id = "in-app",
        label = _.mode.inApp,
        render = () => renderPlay()
      ),
      Mode(
        id = "print",
        label = _.mode.offline,
        materials = List(_.offline.materials.printer, _.offline.materials.paperPen),
        hint = Some(_.offline.maze.sheetHint),
        render = () => renderOffline()
      )
    ))

  // Easy is the implicit default when the URL has no variant segment after the
  // in-app mode — a friendly first board.
  private val DefaultVariant = variants(0)
  // Lazy: the router needs the browser window, and the unit tests load this
  // object without one.
  private lazy val variantSignal: Signal[Variant] =
    Routing.router.currentPageSignal.map {
      case Page.Activity(`id`, _ :: vId :: _) =>
        variants.find(_.id == vId).getOrElse(DefaultVariant)
      case _ => DefaultVariant
    }.distinct

  private def renderPlay(): HtmlElement =
    div(
      cls := "maze stack-lg",
      variantPill(),
      child <-- variantSignal.map(playForVariant)
    )

  private def playForVariant(v: Variant): HtmlElement =
    val state = Var(newPlay(generate(v.rows, v.cols, v.braid)))
    // Whether a finger/mouse drag is in progress. A plain closure var is enough:
    // it never drives rendering, only gates pointermove handling.
    var dragging = false

    def moveTo(to: (Int, Int)): Unit = state.update(step(_, to))

    // The board is static and pointer-captured for the whole drag, so its
    // bounding rect can't shift mid-drag — snapshot it on pointerdown rather
    // than forcing a layout flush on every pointermove.
    var dragRect: dom.DOMRect = null

    // Gesture start, kept to tell a swipe (quick flick → auto-run) from a slow
    // trace on pointerup. `downTrail` is the trail length at touch-down so we
    // don't fire a run after the player has already carefully traced cells.
    var downX = 0.0
    var downY = 0.0
    var downTime = 0.0
    var downTrail = 0

    def cellAt(rect: dom.DOMRect, clientX: Double, clientY: Double): (Int, Int) =
      val fx = (clientX - rect.left) / rect.width
      val fy = (clientY - rect.top) / rect.height
      val c = math.max(0, math.min(v.cols - 1, (fx * v.cols).toInt))
      val r = math.max(0, math.min(v.rows - 1, (fy * v.rows).toInt))
      (r, c)

    def keyMove(dr: Int, dc: Int): Unit =
      val (r, c) = state.now().head
      moveTo((r + dr, c + dc))

    def clearPath(): Unit = state.update(p => p.copy(trail = List(p.start)))
    def newMaze(): Unit   = state.set(newPlay(generate(v.rows, v.cols, v.braid)))

    val gridSig = state.signal.map(_.grid).distinct

    div(
      cls := "stack-lg",
      windowEvents(_.onKeyDown) --> { ev =>
        ev.key match
          case "ArrowUp"    => ev.preventDefault(); keyMove(-1, 0)
          case "ArrowDown"  => ev.preventDefault(); keyMove(1, 0)
          case "ArrowLeft"  => ev.preventDefault(); keyMove(0, -1)
          case "ArrowRight" => ev.preventDefault(); keyMove(0, 1)
          case _            => ()
      },
      p(cls := "maze-instruction muted center no-print", child.text <-- s(_.maze.instruction)),
      svg.svg(
        svg.cls := "maze-svg",
        svg.viewBox <-- gridSig.map(g => s"0 0 ${g.cols * CELL} ${g.rows * CELL}"),
        onPointerDown --> { ev =>
          dragging = true
          val el = ev.currentTarget.asInstanceOf[dom.Element]
          el.setPointerCapture(ev.pointerId)
          dragRect = el.getBoundingClientRect()
          downX = ev.clientX; downY = ev.clientY
          downTime = js.Date.now(); downTrail = state.now().trail.length
          moveTo(cellAt(dragRect, ev.clientX, ev.clientY))
        },
        onPointerMove --> { ev =>
          if dragging then moveTo(cellAt(dragRect, ev.clientX, ev.clientY))
        },
        onPointerUp --> { ev =>
          dragging = false
          // A quick flick that didn't already trace a path runs the corridor in
          // the dominant swipe direction — the mobile-friendly way to move.
          val dx = ev.clientX - downX
          val dy = ev.clientY - downY
          val flick = math.max(math.abs(dx), math.abs(dy)) >= SwipeMinPx &&
            (js.Date.now() - downTime) <= SwipeMaxMs
          if flick && state.now().trail.length - downTrail <= 1 then
            val (dr, dc) =
              if math.abs(dx) > math.abs(dy) then (0, if dx > 0 then 1 else -1)
              else (if dy > 0 then 1 else -1, 0)
            state.update(runDir(_, dr, dc))
        },
        onPointerCancel --> (_ => dragging = false),
        svg.g(svg.cls := "maze-walls", children <-- gridSig.map(wallLines)),
        svg.polyline(svg.cls := "maze-trail", svg.points <-- state.signal.map(trailPoints)),
        svg.circle(svg.cls := "maze-start", svg.cx := cx(0).toString, svg.cy := cy(0).toString, svg.r := "1.5"),
        markerText("maze-goal", gridSig.map(g => cx(g.cols - 1)), gridSig.map(g => cy(g.rows - 1)), "🧀"),
        markerText("maze-avatar", state.signal.map(p => cx(p.head._2)), state.signal.map(p => cy(p.head._1)), "🐭")
      ),
      child <-- state.signal.map(_.won).distinct.map {
        case true  => wonView(newMaze)
        case false => controls(clearPath, newMaze)
      }
    )

  /** The 🐭/🧀 glyphs. `x`/`y` are passed as modifiers so a caller can bind
    * them to a signal (the moving avatar) or pin them to a fixed cell centre
    * (start, goal, print sheet). */
  private def mazeGlyph(cssClass: String, x: Modifier[SvgElement], y: Modifier[SvgElement], glyph: String): SvgElement =
    svg.text(
      svg.cls := s"maze-marker $cssClass",
      x, y,
      svg.textAnchor := "middle",
      svg.dominantBaseline := "central",
      svg.fontSize := (CELL * 0.72).toString,
      glyph
    )

  private def markerText(cssClass: String, xSig: Signal[Double], ySig: Signal[Double], glyph: String): SvgElement =
    mazeGlyph(cssClass, svg.x <-- xSig.map(_.toString), svg.y <-- ySig.map(_.toString), glyph)

  private def controls(clearPath: () => Unit, newMaze: () => Unit): HtmlElement =
    div(
      cls := "row maze-controls",
      styleAttr := "justify-content: center; flex-wrap: wrap;",
      Components.ghost(s(_.maze.clearPath), clearPath()),
      Components.ghost(s(_.maze.newGame), newMaze())
    )

  private def wonView(newMaze: () => Unit): HtmlElement =
    div(
      cls := "handoff card",
      div(cls := "handoff__title", child.text <-- s(_.maze.won)),
      button(
        cls := "btn btn--lg",
        child.text <-- s(_.maze.newGame),
        onClick --> (_ => newMaze())
      )
    )

  private def variantPill(): HtmlElement =
    div(
      cls := "center no-print",
      div(
        cls := "pill-toggle",
        variants.map { v =>
          button(
            cls := "pill-btn",
            cls("is-active") <-- variantSignal.map(_.id == v.id),
            child.text <-- AppState.strings.map(v.nameKey),
            onClick --> (_ => Routing.go(Page.Activity(id, List("in-app", v.id))))
          )
        }
      )
    )

  // ---------- shared SVG drawing ----------

  /** The static wall geometry: an outer border plus every closed interior wall,
    * as line segments in CELL-unit space. */
  private def wallLines(g: Grid): Seq[SvgElement] =
    val w = g.cols * CELL
    val h = g.rows * CELL
    val border = svg.rect(
      svg.cls := "maze-border",
      svg.x := "0", svg.y := "0",
      svg.width := w.toString, svg.height := h.toString
    )
    val verticals =
      for r <- 0 until g.rows; c <- 0 until g.cols - 1 if !g.rightOpen(r, c) yield
        svg.line(
          svg.cls := "maze-wall",
          svg.x1 := ((c + 1) * CELL).toString, svg.y1 := (r * CELL).toString,
          svg.x2 := ((c + 1) * CELL).toString, svg.y2 := ((r + 1) * CELL).toString
        )
    val horizontals =
      for r <- 0 until g.rows - 1; c <- 0 until g.cols if !g.downOpen(r, c) yield
        svg.line(
          svg.cls := "maze-wall",
          svg.x1 := (c * CELL).toString, svg.y1 := ((r + 1) * CELL).toString,
          svg.x2 := ((c + 1) * CELL).toString, svg.y2 := ((r + 1) * CELL).toString
        )
    border +: (verticals ++ horizontals).toSeq

  private def trailPoints(p: Play): String =
    p.trail.reverse.map((r, c) => s"${cx(c)},${cy(r)}").mkString(" ")

  // ---------- print ----------

  /** One printed page: its shape and its mazes. */
  final case class SheetPage(shape: Shape, mazes: Vector[MazeGraph.Carved])

  private[activities] def layoutOf(c: SheetConfig, shape: Shape): MazeGraph.Layout = shape match
    case Shape.Square =>
      val (rows, cols) = c.dims
      MazeGraph.RectLayout(rows, cols)
    case Shape.Hex =>
      val (rows, cols) = c.hexDims
      MazeGraph.HexLayout(rows, cols)
    case Shape.Circle => MazeGraph.CircleLayout(c.rings)

  /** `n` pages of `perPage` mazes each. The shape of each page comes in
    * shuffled rounds (see [[SheetBuilder.rounds]]); the order is fixed for
    * `MaxPages` first. Then one generator makes the pages in order, so page
    * k does not change when pages are added after it. */
  def sheetPages(c: SheetConfig, n: Int, seed: Long): Vector[SheetPage] =
    val rng = new Random(seed)
    val order = SheetBuilder.rounds(Shape.values.toVector.filter(c.shapes), math.max(n, SheetBuilder.MaxPages), rng)
    val layouts = c.shapes.iterator.map(shape => shape -> layoutOf(c, shape)).toMap
    order.take(n).map { shape =>
      SheetPage(shape, Vector.fill(c.perPage.n)(MazeGraph.carve(layouts(shape), c.style, c.loops.braid, rng)))
    }

  /** One printed page of mazes, without the printable frame. Each maze gets
    * the largest square cells that fit in its slot. */
  def sheetBody(c: SheetConfig, page: SheetPage): HtmlElement =
    val (slotW, slotH) = c.perPage.slotMm
    div(
      cls := s"maze-print-sheet maze-print-sheet--pp${c.perPage.n}",
      dataAttr("shape") := page.shape.toString.toLowerCase,
      page.mazes.map { m =>
        m.layout match
          case _: MazeGraph.RectLayout =>
            val g = toGrid(m)
            val cell = math.min(slotW / g.cols, slotH / g.rows)
            squareMaze(g, g.cols * cell)
          case l: MazeGraph.HexLayout =>
            hexMaze(m, l, math.sqrt(3) * hexSideMm(c.perPage, l.rows, l.cols) * (l.cols + 0.5))
          case l: MazeGraph.CircleLayout =>
            circleMaze(m, l, math.min(slotW, slotH))
      }
    )

  // ---------- workbook ----------

  /** All `n` pages of a workbook row, from the same generator as the print
    * page. */
  def workbookPages(token: String, n: Int, rng: Random): Seq[HtmlElement] =
    SheetBuilder.workbookPages(sheetSpec, token, n, rng)

  /** The workbook row editor (see [[SheetBuilder.embedded]]). */
  def settingsEditor(token: Var[String]): HtmlElement = SheetBuilder.embedded(sheetSpec, token)

  /** The row label: the preset name, or "Custom". */
  def tokenSummary(token: String): Strings => String = SheetBuilder.summary(sheetSpec, token)

  private lazy val sheetSpec: SheetBuilder.Spec[SheetConfig, SheetPage] = SheetBuilder.Spec(
    testIdPrefix = "maze",
    presets = sheetPresets,
    encode = encodeConfig,
    decode = decodeConfig,
    title = _ => _.offline.maze.printTitle,
    pagesOf = sheetPages,
    body = sheetBody,
    form = sheetForm,
    newLabel = _.maze.newGame
  )

  /** The sheet builder (see [[SheetBuilder.printPage]]). */
  private def renderOffline(): HtmlElement =
    SheetBuilder.printPage(sheetSpec, id, RulesCard.render(List(RulesCard.fromRules(_.offline.maze.rules))))

  /** The form. It reads and writes only `config`, so a different host (a
    * workbook row) can supply its own Var. */
  def sheetForm(config: Var[SheetConfig]): HtmlElement =
    import SheetConfig.*
    import SheetBuilder.{formRow, pills}
    val c = config.signal
    def form(f: Strings.MazeForm => String): Strings => String = str => f(str.offline.maze.form)
    SheetBuilder.form(
      // Multi-select: pages go through the selected shapes (see sheetPages).
      formRow(form(_.shapes), div(
        cls := "sb-chips",
        SheetBuilder.chip("maze-shape-all", c.map(_.shapes.size == Shape.values.length), s(form(_.shapeAll)),
          () => config.update(_.withAllShapes)),
        List(
          (Shape.Square, "square", form(_.shapeSquare)),
          (Shape.Circle, "circle", form(_.shapeCircle)),
          (Shape.Hex, "hex", form(_.shapeHex))
        ).map { (shape, key, label) =>
          SheetBuilder.chip(s"maze-shape-$key", c.map(_.shapes(shape)), s(label), () => config.update(_.withShape(shape)))
        }
      )),
      // Before the size, because it sets the max size: a smaller slot has a
      // smaller max size, and a change clamps the size.
      formRow(form(_.perPage), pills("maze-per-page", c.map(_.perPage),
        PerPage.values.toList.map(p => (p, p.n.toString, Val(p.n.toString))),
        p => config.update(k => k.copy(perPage = p, size = math.min(k.size, p.maxSize))))),
      formRow(form(_.size), SheetBuilder.fieldStepper(config, "maze-size", _.size, _ => MinSize, _.perPage.maxSize,
        (k, v) => k.copy(size = v))),
      formRow(form(_.style), pills("maze-style", c.map(_.style), List(
        (Style.Winding, "winding", s(form(_.styleWinding))),
        (Style.Balanced, "balanced", s(form(_.styleBalanced))),
        (Style.Branchy, "branchy", s(form(_.styleBranchy)))
      ), st => config.update(_.copy(style = st)))),
      formRow(form(_.loops), pills("maze-loops", c.map(_.loops), List(
        (Loops.Off, "off", s(form(_.loopsOff))),
        (Loops.Few, "few", s(form(_.loopsFew))),
        (Loops.Many, "many", s(form(_.loopsMany)))
      ), l => config.update(_.copy(loops = l))))
    )

  private def squareMaze(g: Grid, widthMm: Double): HtmlElement =
    board("square", widthMm, g.cols * CELL, g.rows * CELL,
      wallLines(g),
      mazeGlyph("maze-start-glyph", svg.x := cx(0).toString, svg.y := cy(0).toString, "🐭"),
      mazeGlyph("maze-goal", svg.x := cx(g.cols - 1).toString, svg.y := cy(g.rows - 1).toString, "🧀")
    )

  /** A number for SVG and CSS, with 3 decimals. JavaScript's toFixed, not
    * f"%.3f": the Scala.js formatter is slow, and a big maze has
    * thousands of numbers. */
  private def num(d: Double): String =
    val fixed = d.asInstanceOf[js.Dynamic].toFixed(3).asInstanceOf[String]
    // "110.000" → "110", "1.500" → "1.5": shorter SVG.
    if fixed.contains('.') then fixed.reverse.dropWhile(_ == '0').dropWhile(_ == '.').reverse else fixed

  /** A board of one printed maze. The board is `widthMm` wide on paper;
    * `w` × `h` is the size of the SVG view box. The CSS lets the board
    * become narrower when its column is narrower (the preview page on the
    * screen has padding); the height follows from the aspect ratio. */
  private def board(shape: String, widthMm: Double, w: Double, h: Double, content: Modifier[SvgElement]*): HtmlElement =
    div(
      cls := s"maze-print-board maze-print-board--$shape",
      styleAttr := s"width: ${num(widthMm)}mm; aspect-ratio: ${num(w)} / ${num(h)};",
      svg.svg(
        svg.cls := "maze-print-svg",
        svg.viewBox := s"0 0 ${num(w)} ${num(h)}",
        content
      )
    )

  private def wall(x1: Double, y1: Double, x2: Double, y2: Double): SvgElement =
    svg.line(
      svg.cls := "maze-wall",
      svg.x1 := num(x1), svg.y1 := num(y1),
      svg.x2 := num(x2), svg.y2 := num(y2)
    )

  /** A hex maze. The side of a hexagon is `CELL` units; the hexagons have
    * a point at the top, and odd rows are shifted half a cell. */
  private def hexMaze(m: MazeGraph.Carved, l: MazeGraph.HexLayout, widthMm: Double): HtmlElement =
    val side = CELL.toDouble
    val across = math.sqrt(3) * side
    def centre(cell: Int): (Double, Double) =
      val (r, c) = (cell / l.cols, cell % l.cols)
      (across * (c + 0.5 + (r % 2) * 0.5), side * (1 + 1.5 * r))
    // Corner k is at -90° + 60°·k; the edge from corner k to corner k + 1
    // faces the neighbour in direction k (upper right, right, lower right,
    // lower left, left, upper left).
    def corner(cell: Int, k: Int): (Double, Double) =
      val (x, y) = centre(cell)
      val a = math.toRadians(-90 + 60 * k)
      (x + side * math.cos(a), y + side * math.sin(a))
    val walls =
      for
        cell <- 0 until l.cellCount
        k <- 0 until 6
        // A border edge always; a shared edge once (from the smaller cell),
        // and only where there is no passage.
        if l.neighbourIn(cell, k).forall(n => n > cell && !m.open(cell, n))
      yield
        val (x1, y1) = corner(cell, k)
        val (x2, y2) = corner(cell, (k + 1) % 6)
        wall(x1, y1, x2, y2)
    val (sx, sy) = centre(m.start)
    val (gx, gy) = centre(l.goal)
    board("hex", widthMm, across * (l.cols + 0.5), side * (1.5 * l.rows + 0.5),
      walls,
      mazeGlyph("maze-start-glyph", svg.x := num(sx), svg.y := num(sy), "🐭"),
      mazeGlyph("maze-goal", svg.x := num(gx), svg.y := num(gy), "🧀")
    )

  /** A circle maze. A ring is `CELL` units wide; the centre cell (the goal)
    * has a radius of one ring. The outer wall has one gap, at the start. */
  private def circleMaze(m: MazeGraph.Carved, l: MazeGraph.CircleLayout, widthMm: Double): HtmlElement =
    val unit = CELL.toDouble
    val outer = unit * (l.rings + 1)
    // The start can be any outer cell; turn the drawing so that it is
    // centred at the bottom.
    val (startFrom, startTo) = l.arc(m.start)
    val turn = math.Pi / 2 - (startFrom + startTo) / 2
    def point(radius: Double, angle: Double): String =
      s"${num(outer + radius * math.cos(angle + turn))} ${num(outer + radius * math.sin(angle + turn))}"
    def arcPath(radius: Double, from: Double, to: Double): String =
      val large = if to - from > math.Pi then 1 else 0
      s"M ${point(radius, from)} A ${num(radius)} ${num(radius)} 0 $large 1 ${point(radius, to)}"
    def path(cssClass: String, d: String): SvgElement = svg.path(svg.cls := cssClass, svg.d := d)
    val walls =
      for
        cell <- 1 until l.cellCount
        ring = l.ringOf(cell)
        (from, to) = l.arc(cell)
        part <- List(
          // The inner side of the cell: one arc, a wall where the cell has
          // no passage to the cell inside it.
          Option.when(!m.open(cell, l.inward(cell)))(path("maze-wall", arcPath(ring * unit, from, to))),
          // The radial wall to the next cell clockwise on the same ring.
          Option.when(!m.open(cell, l.cellAt(ring, (l.indexInRing(cell) + 1) % l.ringSizes(ring))))(
            path("maze-wall", s"M ${point(ring * unit, to)} L ${point((ring + 1) * unit, to)}"))
        ).flatten
      yield part
    val border = path("maze-border", arcPath(outer, startTo, startFrom + 2 * math.Pi))
    board("circle", widthMm, 2 * outer, 2 * outer,
      walls, border,
      // At the bottom, in the start cell.
      mazeGlyph("maze-start-glyph", svg.x := num(outer), svg.y := num(2 * outer - unit / 2), "🐭"),
      mazeGlyph("maze-goal", svg.x := num(outer), svg.y := num(outer), "🧀")
    )
