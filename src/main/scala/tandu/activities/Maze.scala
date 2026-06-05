package tandu.activities

import com.raquo.laminar.api.L.*
import org.scalajs.dom
import tandu.{AppState, Page, Routing}
import tandu.i18n.Strings
import tandu.ui.{Components, Mode, ModeChooser, Printable, RulesCard}
import tandu.ui.Components.s

import scala.collection.mutable
import scala.scalajs.js
import scala.util.Random

object Maze extends Activity:
  val id = "maze"
  def name(s: Strings): String = s.maze.name
  def description(s: Strings): String = s.maze.description
  val minPlayers: Int = 1
  val maxPlayers: Int = 1
  val handsFree: Boolean = false
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

  private def neighbors(r: Int, c: Int, rows: Int, cols: Int): Vector[(Int, Int)] =
    Vector((r - 1, c), (r + 1, c), (r, c - 1), (r, c + 1))
      .filter((nr, nc) => nr >= 0 && nr < rows && nc >= 0 && nc < cols)

  // ---------- generator ----------

  /** Recursive-backtracker (randomised DFS): carve a spanning tree of the grid
    * so there is exactly one path between any two cells — long winding
    * corridors that feel good to trace. Done iteratively with an explicit stack
    * so a 24×24 board can't blow the call stack.
    *
    * `braid` (0..1) optionally knocks an extra wall out of that fraction of
    * dead-ends, turning some into gentle loops. We use it on the easy tier so
    * the youngest solvers meet fewer frustrating dead-ends. */
  def generate(rows: Int, cols: Int, braid: Double, rng: Random = new Random()): Grid =
    val openRight = Array.fill(rows * cols)(false)
    val openDown  = Array.fill(rows * cols)(false)
    val visited   = Array.fill(rows * cols)(false)
    def idx(r: Int, c: Int) = r * cols + c

    // Carve the passage between two adjacent cells: like `connected`, it lives
    // on the right/down wall of the pair's upper-left cell.
    def carve(r1: Int, c1: Int, r2: Int, c2: Int): Unit =
      if r1 == r2 then openRight(idx(r1, math.min(c1, c2))) = true
      else openDown(idx(math.min(r1, r2), c1)) = true

    val stack = mutable.Stack[(Int, Int)]()
    visited(idx(0, 0)) = true
    stack.push((0, 0))
    while stack.nonEmpty do
      val (r, c) = stack.top
      val unvisited = neighbors(r, c, rows, cols).filterNot((nr, nc) => visited(idx(nr, nc)))
      if unvisited.isEmpty then { val _ = stack.pop() }
      else
        val (nr, nc) = unvisited(rng.nextInt(unvisited.size))
        carve(r, c, nr, nc)
        visited(idx(nr, nc)) = true
        stack.push((nr, nc))

    if braid > 0 then
      // Same upper-left normalisation as `carve`/`connected`, read straight off
      // the working arrays (every cell passed here is in-grid and adjacent).
      def linked(r: Int, c: Int, nr: Int, nc: Int): Boolean =
        if r == nr then openRight(idx(r, math.min(c, nc)))
        else openDown(idx(math.min(r, nr), c))
      def degree(r: Int, c: Int): Int =
        neighbors(r, c, rows, cols).count((nr, nc) => linked(r, c, nr, nc))
      for r <- 0 until rows; c <- 0 until cols do
        if degree(r, c) == 1 && (r, c) != (0, 0) && (r, c) != (rows - 1, cols - 1)
          && rng.nextDouble() < braid
        then
          val opts = neighbors(r, c, rows, cols).filterNot((nr, nc) => linked(r, c, nr, nc))
          if opts.nonEmpty then
            val (nr, nc) = opts(rng.nextInt(opts.size))
            carve(r, c, nr, nc)

    Grid(rows, cols, openRight.toVector, openDown.toVector)

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

  final case class Variant(
      id: String,
      rows: Int,
      cols: Int,
      braid: Double,
      perPage: Int,
      nameKey: Strings => String,
      descKey: Strings => String
  )

  val variants: List[Variant] = List(
    Variant("easy",   10, 10, 0.30, 4, _.maze.easy.name,   _.maze.easy.description),
    Variant("medium", 16, 16, 0.0,  2, _.maze.medium.name, _.maze.medium.description),
    Variant("hard",   24, 24, 0.0,  1, _.maze.hard.name,   _.maze.hard.description)
  )

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
  private val variantSignal: Signal[Variant] =
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
          val _ = el.asInstanceOf[js.Dynamic].setPointerCapture(ev.pointerId)
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

  private def renderOffline(): HtmlElement =
    // Each printed batch is a single variant, so the sheet's column count and
    // tile size key off that variant alone.
    val mazes: Var[List[(Variant, Grid)]] = Var(Nil)

    def printBatch(v: Variant): Unit =
      mazes.set(List.fill(v.perPage)(v -> generate(v.rows, v.cols, v.braid)))
      val _ = js.timers.setTimeout(50)(Printable.print())

    div(
      cls := "stack-lg",
      div(
        cls := "no-print",
        RulesCard.render(List(RulesCard.fromRules(_.offline.maze.rules)))
      ),
      div(
        cls := "no-print stack-lg maze-print-actions",
        variants.map { v =>
          button(
            cls := "btn btn--lg btn--block",
            child.text <-- AppState.strings.map(str => s"${str.printable.print} — ${v.nameKey(str)}"),
            onClick --> (_ => printBatch(v))
          )
        }
      ),
      div(
        cls := "print-only",
        Printable.render(
          title = _.offline.maze.printTitle,
          body = div(
            cls := "maze-print-sheet",
            // One big maze per page (hard) drops to a single column; the rest
            // tile two-up. Column layout stays in CSS, not inline styles.
            cls("maze-print-sheet--single") <-- mazes.signal.map(_.headOption.exists(_._1.perPage == 1)),
            children <-- mazes.signal.map(_.map((v, g) => printableMaze(v, g)))
          )
        )
      )
    )

  private def printableMaze(v: Variant, g: Grid): HtmlElement =
    div(
      cls := s"maze-print-board maze-print-board--${v.id}",
      svg.svg(
        svg.cls := "maze-print-svg",
        svg.viewBox := s"0 0 ${g.cols * CELL} ${g.rows * CELL}",
        wallLines(g),
        mazeGlyph("maze-start-glyph", svg.x := cx(0).toString, svg.y := cy(0).toString, "🐭"),
        mazeGlyph("maze-goal", svg.x := cx(g.cols - 1).toString, svg.y := cy(g.rows - 1).toString, "🧀")
      )
    )
