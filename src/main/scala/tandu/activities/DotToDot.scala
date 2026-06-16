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

/** Connect-the-dots: tap numbered dots in order and a picture appears.
  *
  * Puzzles are generated, not authored: an emoji is rasterised onto an
  * off-screen canvas, its alpha channel thresholded into a mask, the largest
  * connected blob's outer boundary traced (Moore-neighbour tracing), and that
  * pixel-dense contour simplified with Ramer–Douglas–Peucker. The tolerance is
  * not a fixed dot count: it's binary-searched for the *fewest dots whose
  * straight-line polygon still covers the silhouette faithfully* (IoU against
  * the source mask), so simple shapes get few dots, intricate ones get more,
  * and shapes that can't stay recognisable are rejected outright.
  *
  * Recognisability also needs more than the outline — paper dot-to-dots print
  * an eye or a mouth as anchors. We recover the same from the emoji itself:
  * colour edges *inside* the silhouette (eroded so the outline stays secret)
  * plus the full shape of small detached parts become a faint line-art layer
  * under the dots, both on screen and on the printed sheet.
  *
  * Everything runs in tens of milliseconds, so puzzles are made on demand; an
  * emoji whose silhouette traces badly on this platform's font is simply
  * rejected and another drawn.
  *
  * Dev note: /activity/dot-to-dot/lab renders every pool emoji's *finished*
  * puzzle (outline + detail layer + stats) for eyeballing the generator.
  */
object DotToDot extends Activity:
  val id = "dot-to-dot"
  def name(s: Strings): String = s.dotToDot.name
  def description(s: Strings): String = s.dotToDot.description
  val minPlayers: Int = 1
  val maxPlayers: Int = 1
  val glyph: String = "✏️"
  val tint: String = "sky"

  // ---------- geometry spaces ----------

  /** Off-screen raster size in px. Big enough that the boundary trace sees
    * smooth curves, small enough that mask work stays in the low milliseconds. */
  private val Raster = 480

  /** SVG user units for the board. All dots are normalised into this square
    * with `Pad` of headroom so number labels never clip at the edge. */
  private val View = 100.0
  private val Pad  = 9.0

  // ---------- generator tunables (exercised via the /lab page) ----------

  /** A candidate polygon must cover at least this fraction of the silhouette
    * (intersection-over-union) to count as faithful. */
  private val TargetIoU = 0.94

  /** Dot-count guard rails: below MinDots a puzzle is over too fast even when
    * faithful; above MaxDots the shape is too intricate to be fun. */
  private val MinDots = 18
  private val MaxDots = 60

  /** Minimum dot gap in raster px — comfortably above a fingertip once the
    * board is scaled to screen size, and wide enough that adjacent number
    * labels don't pile up on dense stretches (elephant legs). */
  private val MinGapPx = 20.0

  /** Detail layer: how far inside the silhouette colour edges may sit (so the
    * outline itself is never given away), and how strong a colour step counts
    * as a feature line. */
  private val EdgeMargin = 9
  private val EdgeThreshold = 52

  /** Emoji whose *outline alone* still reads as the thing — solid one-blob
    * silhouettes. Faces, donuts and anything whose identity lives in interior
    * detail or thin strokes don't survive silhouetting and are left out. */
  private val emojis = Vector(
    "🐟", "🐠", "🦋", "⭐", "❤️", "🚀", "⛵", "🏠", "☂️", "🍎",
    "🍐", "🍓", "🥕", "🐢", "🐘", "🦆", "🐇", "🐳", "🦈", "🌙",
    "🎈", "👕", "🧦", "🎩", "🔔", "🌵", "🐦", "🍄", "✈️", "🚗",
    "🐬", "🌻", "🧸", "🦴", "🍦", "🦖", "🐧", "🎸", "👟", "🐌",
    "🎁", "⚽", "🍕", "🐙"
  )

  // ---------- model ----------

  /** One numbered dot: position plus a precomputed label spot just outside
    * the shape, both in View units. */
  final case class Dot(x: Double, y: Double, lx: Double, ly: Double)

  /** `imageUrl` is the very canvas the dots were traced from, so placing it at
    * the raster square mapped into view units (`imgX/imgY/imgSize`) lines the
    * reveal up with the outline exactly — no font-metric guesswork.
    * `detailUrl` is the faint interior line-art shown during play and print. */
  final case class Puzzle(
      emoji: String,
      dots: Vector[Dot],
      imageUrl: String,
      detailUrl: String,
      imgX: Double,
      imgY: Double,
      imgSize: Double
  )

  // ---------- generator: emoji → mask → contour → dots ----------

  /** One shared off-screen canvas for all raster work (emoji rasterising, IoU
    * scoring, detail layer). JS is single-threaded and every caller snapshots
    * the pixels (getImageData / toDataURL) before returning, so reuse is safe
    * and spares ~15 canvas allocations per generated puzzle. */
  private lazy val scratchCanvas: dom.HTMLCanvasElement =
    val c = dom.document.createElement("canvas").asInstanceOf[dom.HTMLCanvasElement]
    c.width = Raster
    c.height = Raster
    c

  private def scratchCtx(): dom.CanvasRenderingContext2D =
    val ctx = scratchCanvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
    ctx.clearRect(0, 0, Raster, Raster)
    ctx

  private final case class Rastered(data: js.typedarray.Uint8ClampedArray, url: String)

  /** Rasterise the emoji centred on the scratch canvas; keep both the pixel
    * data (for tracing) and a PNG data URL (for the reveal). */
  private def rasterize(emoji: String): Rastered =
    val ctx = scratchCtx()
    ctx.textAlign = "center"
    ctx.textBaseline = "middle"
    ctx.font = s"${(Raster * 0.8).toInt}px sans-serif"
    ctx.fillText(emoji, Raster / 2.0, Raster / 2.0)
    Rastered(ctx.getImageData(0, 0, Raster, Raster).data, scratchCanvas.toDataURL("image/png"))

  /** Anti-aliased fringe pixels (alpha ≤ 40) count as background so the traced
    * edge hugs the visible shape. */
  private def alphaMask(data: js.typedarray.Uint8ClampedArray): Array[Boolean] =
    Array.tabulate(Raster * Raster)(i => data(i * 4 + 3) > 40)

  /** Keep only the largest 8-connected blob — drops detached flourishes like
    * a rocket's exhaust flame so the trace follows one closed shape. Returns
    * the filtered mask and the blob's pixel count. */
  private def largestComponent(mask: Array[Boolean]): (Array[Boolean], Int) =
    val w = Raster
    val seen = new Array[Boolean](mask.length)
    var best: mutable.ArrayBuffer[Int] = mutable.ArrayBuffer.empty
    val queue = mutable.ArrayDeque.empty[Int]
    for start <- mask.indices if mask(start) && !seen(start) do
      val blob = mutable.ArrayBuffer.empty[Int]
      seen(start) = true
      queue.append(start)
      while queue.nonEmpty do
        val i = queue.removeHead()
        blob += i
        val x = i % w
        val y = i / w
        var dy = -1
        while dy <= 1 do
          var dx = -1
          while dx <= 1 do
            val nx = x + dx
            val ny = y + dy
            if nx >= 0 && nx < w && ny >= 0 && ny < w then
              val n = ny * w + nx
              if mask(n) && !seen(n) then
                seen(n) = true
                queue.append(n)
            dx += 1
          dy += 1
      if blob.size > best.size then best = blob
    val out = new Array[Boolean](mask.length)
    best.foreach(out(_) = true)
    (out, best.size)

  /** The 8 Moore neighbours in clockwise order (y grows downward), starting
    * west: W NW N NE E SE S SW. */
  private val moore = Vector((-1, 0), (-1, -1), (0, -1), (1, -1), (1, 0), (1, 1), (0, 1), (-1, 1))

  /** Moore-neighbour boundary tracing of a single blob: start at its
    * row-major-first pixel (whose west neighbour is guaranteed background),
    * sweep the Moore neighbourhood clockwise from the backtrack pixel, step to
    * the first foreground hit, repeat. Terminates on returning to the exact
    * start state (start pixel with the original backtrack), which closes the
    * loop without double-tracing; the step cap is a belt-and-braces guard. */
  private def traceBoundary(comp: Array[Boolean]): Vector[(Int, Int)] =
    val w = Raster
    def at(x: Int, y: Int): Boolean = x >= 0 && x < w && y >= 0 && y < w && comp(y * w + x)
    val startIdx = comp.indexWhere(identity)
    if startIdx < 0 then return Vector.empty
    val sx = startIdx % w
    val sy = startIdx / w
    val origBack = (sx - 1, sy)
    val contour = mutable.ArrayBuffer((sx, sy))
    var cur = (sx, sy)
    var back = origBack
    var done = false
    var steps = 0
    val cap = 6 * w * w
    while !done && steps < cap do
      steps += 1
      val d0 = moore.indexOf((back._1 - cur._1, back._2 - cur._2))
      var k = 1
      var moved = false
      var lastChecked = back
      while k <= 8 && !moved do
        val d = (d0 + k) % 8
        val next = (cur._1 + moore(d)._1, cur._2 + moore(d)._2)
        if at(next._1, next._2) then
          back = lastChecked
          cur = next
          moved = true
        else
          lastChecked = next
        k += 1
      if !moved then done = true // isolated pixel — nothing to trace
      else if cur == (sx, sy) && back == origBack then done = true
      else contour += cur
    contour.toVector

  /** Ramer–Douglas–Peucker on an open polyline: keep both endpoints, recurse
    * on the farthest-out point while any point strays beyond `eps`. */
  private def rdp(pts: IndexedSeq[(Double, Double)], eps: Double): Vector[(Double, Double)] =
    if pts.length < 3 then pts.toVector
    else
      val (x1, y1) = pts.head
      val (x2, y2) = pts.last
      val dx = x2 - x1
      val dy = y2 - y1
      val len = math.hypot(dx, dy)
      var maxDist = -1.0
      var maxIdx = 0
      var i = 1
      while i < pts.length - 1 do
        val (px, py) = pts(i)
        val dist =
          if len < 1e-9 then math.hypot(px - x1, py - y1)
          else math.abs(dy * px - dx * py + x2 * y1 - y2 * x1) / len
        if dist > maxDist then
          maxDist = dist
          maxIdx = i
        i += 1
      if maxDist <= eps then Vector(pts.head, pts.last)
      else rdp(pts.slice(0, maxIdx + 1), eps).init ++ rdp(pts.slice(maxIdx, pts.length), eps)

  /** RDP for a closed contour: anchor at point 0 and the point farthest from
    * it (guaranteed real corners of the shape), simplify each half. The two
    * shared anchors are deduplicated on concatenation. */
  private def rdpClosed(pts: IndexedSeq[(Double, Double)], eps: Double): Vector[(Double, Double)] =
    if pts.length < 4 then pts.toVector
    else
      val (x0, y0) = pts.head
      var farIdx = 1
      var farDist = -1.0
      var i = 1
      while i < pts.length do
        val d = math.hypot(pts(i)._1 - x0, pts(i)._2 - y0)
        if d > farDist then
          farDist = d
          farIdx = i
        i += 1
      val a = rdp(pts.slice(0, farIdx + 1), eps)
      val b = rdp(pts.slice(farIdx, pts.length) :+ pts.head, eps)
      a.init ++ b.init

  /** Binary-search the RDP tolerance to land near `target` dots — used to top
    * a too-sparse faithful polygon back up to a playable dot count. */
  private def simplifyToCount(pts: IndexedSeq[(Double, Double)], target: Int): Vector[(Double, Double)] =
    var lo = 0.2
    var hi = Raster / 4.0
    var best: Vector[(Double, Double)] = null
    var iter = 0
    while iter < 20 do
      val mid = (lo + hi) / 2
      val r = rdpClosed(pts, mid)
      if best == null || math.abs(r.size - target) < math.abs(best.size - target) then best = r
      if r.size > target then lo = mid else hi = mid
      iter += 1
    best

  /** Greedy minimum-gap pass so dots stay tappable: drop any dot closer than
    * `minGap` to the previously kept one (and the closing pair). Tiny detail
    * clusters — an ear, a leaf stem — collapse to a single dot. */
  private def spaceOut(pts: Vector[(Double, Double)], minGap: Double): Vector[(Double, Double)] =
    val kept = mutable.ArrayBuffer(pts.head)
    for p <- pts.tail do
      if math.hypot(p._1 - kept.last._1, p._2 - kept.last._2) >= minGap then kept += p
    if kept.size > 2 && math.hypot(kept.last._1 - kept.head._1, kept.last._2 - kept.head._2) < minGap then
      val _ = kept.remove(kept.size - 1)
    kept.toVector

  /** How faithfully the dots' straight-line polygon covers the silhouette:
    * fill it on a scratch canvas and compare pixels (intersection over union)
    * with the component mask. This is the recognisability gate. */
  private def polygonIoU(comp: Array[Boolean], poly: Vector[(Double, Double)]): Double =
    if poly.size < 3 then return 0.0
    val ctx = scratchCtx()
    ctx.fillStyle = "#000"
    ctx.beginPath()
    ctx.moveTo(poly.head._1, poly.head._2)
    poly.tail.foreach((x, y) => ctx.lineTo(x, y))
    ctx.closePath()
    ctx.fill()
    val data = ctx.getImageData(0, 0, Raster, Raster).data
    var inter = 0
    var union = 0
    var i = 0
    while i < comp.length do
      val a = comp(i)
      val b = data(i * 4 + 3) > 127
      if a && b then inter += 1
      if a || b then union += 1
      i += 1
    if union == 0 then 0.0 else inter.toDouble / union

  /** Pick the dots: the *coarsest* simplification (fewest dots) whose polygon
    * still meets `TargetIoU`, found by binary search on the RDP tolerance.
    * Too-simple results are topped up to MinDots (extra dots never hurt
    * fidelity); shapes needing more than MaxDots are rejected. */
  private def chooseDots(
      contour: Vector[(Double, Double)],
      comp: Array[Boolean]
  ): Option[Vector[(Double, Double)]] =
    var lo = 0.6
    var hi = 24.0
    var feasible: Vector[(Double, Double)] = null
    var iter = 0
    while iter < 12 do
      val mid = (lo + hi) / 2
      val c = spaceOut(rdpClosed(contour, mid), MinGapPx)
      if polygonIoU(comp, c) >= TargetIoU then
        feasible = c
        lo = mid // faithful — try even coarser
      else hi = mid
      iter += 1
    Option(feasible).filter(_.size <= MaxDots).map { d =>
      if d.size >= MinDots then d
      else
        val finer = spaceOut(simplifyToCount(contour, MinDots), MinGapPx)
        if finer.size > d.size then finer else d
    }

  // ---------- detail layer: the printed "eye" that anchors recognition ----------

  /** Binary erosion by a (2r+1)² square, as two 1D min passes. */
  private def erode(mask: Array[Boolean], r: Int): Array[Boolean] =
    val w = Raster
    def pass(src: Array[Boolean], horizontal: Boolean): Array[Boolean] =
      val out = new Array[Boolean](src.length)
      var y = 0
      while y < w do
        var x = 0
        while x < w do
          var ok = true
          var k = -r
          while ok && k <= r do
            val (nx, ny) = if horizontal then (x + k, y) else (x, y + k)
            if nx < 0 || nx >= w || ny < 0 || ny >= w || !src(ny * w + nx) then ok = false
            k += 1
          out(y * w + x) = ok
          x += 1
        y += 1
      out
    pass(pass(mask, true), false)

  /** Faint line-art recovered from the emoji itself: colour edges deep inside
    * the silhouette (the eye, the smile, the fin) plus the outlines of small
    * detached parts (a rocket's flame), which paper dot-to-dots would print.
    * The erosion margin keeps anything near the main outline out — the
    * outline is the puzzle. */
  private def detailLayer(
      data: js.typedarray.Uint8ClampedArray,
      mask: Array[Boolean],
      comp: Array[Boolean]
  ): String =
    val w = Raster
    val interior = erode(mask, EdgeMargin)
    val edges = new Array[Boolean](mask.length)
    var y = 1
    while y < w - 1 do
      var x = 1
      while x < w - 1 do
        val i = y * w + x
        if interior(i) then
          // strongest channel step across this pixel, horizontally or vertically
          var grad = 0
          var c = 0
          while c < 3 do
            val gx = math.abs(data((i + 1) * 4 + c) - data((i - 1) * 4 + c))
            val gy = math.abs(data((i + w) * 4 + c) - data((i - w) * 4 + c))
            if gx > grad then grad = gx
            if gy > grad then grad = gy
            c += 1
          if grad > EdgeThreshold then edges(i) = true
        else if mask(i) && !comp(i) then
          // detached part: draw its silhouette (pixels touching background)
          if !mask(i - 1) || !mask(i + 1) || !mask(i - w) || !mask(i + w) then edges(i) = true
        x += 1
      y += 1
    val ctx = scratchCtx()
    val out = ctx.createImageData(w, w)
    def plot(i: Int): Unit =
      out.data(i * 4) = 60
      out.data(i * 4 + 1) = 60
      out.data(i * 4 + 2) = 60
      out.data(i * 4 + 3) = 210
    // One-pixel dilation: single-pixel edge lines all but vanish once the
    // raster is scaled down to a print board, so thicken them to ~3px.
    var i = w + 1
    while i < mask.length - w - 1 do
      if edges(i) then
        plot(i)
        plot(i - 1)
        plot(i + 1)
        plot(i - w)
        plot(i + w)
      i += 1
    ctx.putImageData(out, 0, 0)
    scratchCanvas.toDataURL("image/png")

  // ---------- assembly ----------

  /** Ray-cast point-in-polygon, used to push number labels to the *outside*
    * of the shape. */
  private def inside(poly: Vector[(Double, Double)], x: Double, y: Double): Boolean =
    var in = false
    var i = 0
    var j = poly.length - 1
    while i < poly.length do
      val (xi, yi) = poly(i)
      val (xj, yj) = poly(j)
      if (yi > y) != (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi then in = !in
      j = i
      i += 1
    in

  private val LabelOffset = 3.4

  /** Place each number along the corner's bisector, flipped outward when the
    * first guess lands inside the polygon, then clamped into the view box. */
  private def withLabels(pts: Vector[(Double, Double)]): Vector[Dot] =
    val n = pts.length
    pts.zipWithIndex.map { case ((x, y), i) =>
      val (px, py) = pts((i - 1 + n) % n)
      val (nx, ny) = pts((i + 1) % n)
      val ul = math.hypot(px - x, py - y).max(1e-9)
      val vl = math.hypot(nx - x, ny - y).max(1e-9)
      var dx = -((px - x) / ul + (nx - x) / vl)
      var dy = -((py - y) / ul + (ny - y) / vl)
      val bl = math.hypot(dx, dy)
      if bl < 1e-6 then
        // collinear neighbours — use the perpendicular of the through-line
        dx = -(ny - py)
        dy = nx - px
      val l = math.hypot(dx, dy).max(1e-9)
      dx /= l
      dy /= l
      if inside(pts, x + dx * LabelOffset, y + dy * LabelOffset) then
        dx = -dx
        dy = -dy
      val lx = (x + dx * LabelOffset).max(2.5).min(View - 2.5)
      val ly = (y + dy * LabelOffset).max(2.5).min(View - 2.5)
      Dot(x, y, lx, ly)
    }

  /** Scale raster-space dots into the padded view square (aspect preserved,
    * centred), start numbering at the topmost dot, and map the raster square
    * through the same transform so the image layers land exactly under the
    * outline. */
  private def assemble(
      emoji: String,
      raster: Vector[(Double, Double)],
      imageUrl: String,
      detailUrl: String
  ): Puzzle =
    val minX = raster.map(_._1).min
    val maxX = raster.map(_._1).max
    val minY = raster.map(_._2).min
    val maxY = raster.map(_._2).max
    val k = (View - 2 * Pad) / math.max(maxX - minX, maxY - minY).max(1e-9)
    val offX = (View - (maxX - minX) * k) / 2
    val offY = (View - (maxY - minY) * k) / 2
    val view = raster.map((x, y) => ((x - minX) * k + offX, (y - minY) * k + offY))
    val topIdx = view.indices.minBy(i => view(i)._2)
    val rotated = view.drop(topIdx) ++ view.take(topIdx)
    Puzzle(
      emoji,
      withLabels(rotated),
      imageUrl,
      detailUrl,
      imgX = (0 - minX) * k + offX,
      imgY = (0 - minY) * k + offY,
      imgSize = Raster * k
    )

  /** Trace one emoji into a puzzle, or explain why it was rejected (the /lab
    * page surfaces the reason; play just draws another emoji). Also returns
    * the achieved IoU for the lab's stats line. */
  private def tracePuzzleInfo(emoji: String): Either[String, (Puzzle, Double)] =
    val r = rasterize(emoji)
    val mask = alphaMask(r.data)
    val (comp, size) = largestComponent(mask)
    if size < Raster * Raster * 0.04 then Left("blob too small")
    else
      val contour = traceBoundary(comp)
      if contour.size < 80 then Left("contour too short")
      else
        val pts = contour.map((x, y) => (x.toDouble, y.toDouble))
        chooseDots(pts, comp) match
          case None => Left("can't stay faithful within dot budget")
          case Some(dots) =>
            val iou = polygonIoU(comp, dots)
            Right((assemble(emoji, dots, r.url, detailLayer(r.data, mask, comp)), iou))

  /** Whether an emoji traces well is a fixed property of this platform's
    * font, so rejections are remembered and never re-traced. */
  private val knownBad = mutable.Set.empty[String]

  /** `avoid` keeps a new draw from repeating the picture the caller already
    * has (the current board, or the other puzzle on a print sheet). */
  def makePuzzle(rng: Random, avoid: String = ""): Puzzle =
    var attempts = 0
    var result: Option[Puzzle] = None
    while result.isEmpty && attempts < 30 do
      val e = emojis(rng.nextInt(emojis.size))
      if e != avoid && !knownBad(e) then
        result = tracePuzzleInfo(e).toOption.map(_._1)
        if result.isEmpty then knownBad += e
      attempts += 1
    result.getOrElse(starPuzzle())

  /** Last-resort puzzle if every traced emoji is rejected (e.g. a platform
    * with no colour-emoji font at all): a five-point star from pure maths. */
  private def starPuzzle(): Puzzle =
    val c = Raster / 2.0
    val pts = Vector.tabulate(10) { i =>
      val a = -math.Pi / 2 + i * math.Pi / 5
      val r = if i % 2 == 0 then Raster * 0.42 else Raster * 0.18
      (c + r * math.cos(a), c + r * math.sin(a))
    }
    assemble("⭐", pts, "", "")

  // ---------- UI ----------

  def render(): HtmlElement =
    // The dev lab lives outside the mode chooser; resolve it reactively so
    // URL edits within the activity (chooser ↔ /lab) re-render correctly.
    val labPage: Signal[Option[Int]] = Routing.router.currentPageSignal.map {
      case Page.Activity(`id`, "lab" :: rest) =>
        Some(rest.headOption.flatMap(_.toIntOption).getOrElse(0))
      case _ => None
    }.distinct
    div(
      child <-- labPage.map {
        case Some(page) => renderLab(page)
        case None =>
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
              hint = Some(_.offline.dotToDot.sheetHint),
              render = () => renderOffline()
            )
          ))
      }
    )

  private def renderPlay(): HtmlElement =
    val rng = new Random()
    val puzzle = Var(makePuzzle(rng))
    // Dots connected so far; tapping dot k (0-based) when progress == k links
    // it in. progress == dots.size means the loop is closed — picture done.
    val progress = Var(0)

    def newPicture(): Unit =
      puzzle.set(makePuzzle(rng, avoid = puzzle.now().emoji))
      progress.set(0)

    div(
      cls := "stack-lg",
      p(cls := "dots-instruction muted center no-print", child.text <-- s(_.dotToDot.instruction)),
      child <-- puzzle.signal.map(p => board(p, progress)),
      child <-- puzzle.signal.combineWith(progress.signal).map((p, k) => k >= p.dots.size).distinct.map {
        case true  => wonView(newPicture)
        case false =>
          div(
            cls := "row dots-controls no-print",
            styleAttr := "justify-content: center;",
            Components.ghost(s(_.dotToDot.newGame), newPicture())
          )
      }
    )

  /** The raster square mapped into view units, for both image layers. */
  private def imageLayer(p: Puzzle, url: String, cssClass: String): SvgElement =
    svg.image(
      svg.cls := cssClass,
      svg.x := fmt(p.imgX),
      svg.y := fmt(p.imgY),
      svg.width := fmt(p.imgSize),
      svg.height := fmt(p.imgSize),
      svg.href := url
    )

  private def board(p: Puzzle, progress: Var[Int]): SvgElement =
    val n = p.dots.size
    val doneSig = progress.signal.map(_ >= n)

    def tap(i: Int): Unit = progress.update(k => if i == k then k + 1 else k)

    // The drawn line: dots connected so far, closing back to dot 1 at the end.
    def pathPoints(k: Int): String =
      val sel = if k >= n then p.dots :+ p.dots.head else p.dots.take(k)
      sel.map(d => s"${fmt(d.x)},${fmt(d.y)}").mkString(" ")

    svg.svg(
      svg.cls <-- doneSig.map(d => if d then "dots-svg is-done" else "dots-svg"),
      svg.viewBox := s"0 0 ${View.toInt} ${View.toInt}",
      // The real picture sits exactly under the traced outline, invisible
      // until the loop closes — the reveal is the reward.
      if p.imageUrl.nonEmpty then imageLayer(p, p.imageUrl, "dots-reveal") else emptyNode,
      if p.detailUrl.nonEmpty then imageLayer(p, p.detailUrl, "dots-detail") else emptyNode,
      svg.polyline(svg.cls := "dots-path", svg.points <-- progress.signal.map(pathPoints)),
      p.dots.zipWithIndex.map { (d, i) =>
        svg.g(
          // No "you're next" highlight: spotting the next number is the
          // whole exercise, so only already-linked dots change colour.
          svg.cls <-- progress.signal.map(k => if i < k then "dots-dot is-linked" else "dots-dot"),
          onPointerDown --> (_ => tap(i)),
          svg.circle(svg.cls := "dots-hit", svg.cx := fmt(d.x), svg.cy := fmt(d.y), svg.r := "4.5"),
          svg.circle(svg.cls := "dots-mark", svg.cx := fmt(d.x), svg.cy := fmt(d.y), svg.r := "1.4"),
          svg.text(
            svg.cls := "dots-num",
            svg.x := fmt(d.lx),
            svg.y := fmt(d.ly),
            svg.textAnchor := "middle",
            svg.dominantBaseline := "central",
            (i + 1).toString
          )
        )
      }
    )

  private def fmt(v: Double): String = f"$v%.2f"

  private def wonView(newPicture: () => Unit): HtmlElement =
    div(
      cls := "handoff card",
      div(cls := "handoff__title", child.text <-- s(_.dotToDot.won)),
      button(
        cls := "btn btn--lg",
        child.text <-- s(_.dotToDot.newGame),
        onClick --> (_ => newPicture())
      )
    )

  // ---------- print ----------

  /** One printed page of two puzzles (the offline mode's usual sheet), as a
    * bare body for composed documents like workbooks. */
  def printSheetBody(rng: Random = new Random()): HtmlElement =
    val first = makePuzzle(rng)
    div(
      cls := "dots-print-sheet",
      List(first, makePuzzle(rng, avoid = first.emoji)).map(printablePuzzle)
    )

  // ---------- coloring pages ----------
  //
  // Not surfaced anywhere yet: the outlines this produces aren't good enough
  // for a coloring book (the detail layer reads as noise at print size).
  // Kept for a future quality pass — see docs/workbook.md.

  /** A coloring page reuses the same emoji → silhouette pipeline, but keeps a
    * fine RDP tolerance so the outline stays smooth and skips the numbered
    * dots entirely: the outline plus the interior detail layer prints as line
    * art to colour in. */
  final case class Coloring(
      emoji: String,
      outline: Vector[(Double, Double)],
      detailUrl: String,
      imgX: Double,
      imgY: Double,
      imgSize: Double
  )

  private def traceColoring(emoji: String): Option[Coloring] =
    val r = rasterize(emoji)
    val mask = alphaMask(r.data)
    val (comp, size) = largestComponent(mask)
    if size < Raster * Raster * 0.04 then None
    else
      val contour = traceBoundary(comp)
      if contour.size < 80 then None
      else
        val pts = rdpClosed(contour.map((x, y) => (x.toDouble, y.toDouble)), 1.6)
        // The same scale-into-view transform as `assemble`, minus the dots.
        val minX = pts.map(_._1).min
        val maxX = pts.map(_._1).max
        val minY = pts.map(_._2).min
        val maxY = pts.map(_._2).max
        val k = (View - 2 * Pad) / math.max(maxX - minX, maxY - minY).max(1e-9)
        val offX = (View - (maxX - minX) * k) / 2
        val offY = (View - (maxY - minY) * k) / 2
        Some(Coloring(
          emoji,
          pts.map((x, y) => ((x - minX) * k + offX, (y - minY) * k + offY)),
          detailLayer(r.data, mask, comp),
          imgX = (0 - minX) * k + offX,
          imgY = (0 - minY) * k + offY,
          imgSize = Raster * k
        ))

  /** Coloring is far less demanding than dot placement (no dot-count budget),
    * so it has its own attempt loop and ignores `knownBad`. */
  def makeColoring(rng: Random, avoid: String = ""): Coloring =
    var attempts = 0
    var result: Option[Coloring] = None
    while result.isEmpty && attempts < 30 do
      val e = emojis(rng.nextInt(emojis.size))
      if e != avoid then result = traceColoring(e)
      attempts += 1
    result.getOrElse {
      val star = starPuzzle()
      Coloring("⭐", star.dots.map(d => (d.x, d.y)), "", 0, 0, 0)
    }

  /** One printed coloring page, as a bare body for composed documents like
    * workbooks. */
  def coloringSheetBody(rng: Random = new Random()): HtmlElement =
    val c = makeColoring(rng)
    div(
      cls := "dots-coloring-board",
      dataAttr("emoji") := c.emoji,
      svg.svg(
        svg.cls := "dots-coloring-svg",
        svg.viewBox := s"0 0 ${View.toInt} ${View.toInt}",
        if c.detailUrl.nonEmpty then
          svg.image(
            svg.cls := "dots-detail",
            svg.x := fmt(c.imgX),
            svg.y := fmt(c.imgY),
            svg.width := fmt(c.imgSize),
            svg.height := fmt(c.imgSize),
            svg.href := c.detailUrl
          )
        else emptyNode,
        svg.polygon(
          svg.cls := "dots-coloring-outline",
          svg.points := c.outline.map((x, y) => s"${fmt(x)},${fmt(y)}").mkString(" ")
        )
      )
    )

  private def renderOffline(): HtmlElement =
    val sheets: Var[List[Puzzle]] = Var(Nil)
    val rng = new Random()

    div(
      cls := "stack-lg",
      div(
        cls := "no-print",
        RulesCard.render(List(RulesCard.fromRules(_.offline.dotToDot.rules)))
      ),
      div(
        cls := "no-print stack-lg dots-print-actions",
        button(
          cls := "btn btn--lg btn--block",
          child.text <-- AppState.strings.map(_.printable.print),
          onClick --> { _ =>
            val first = makePuzzle(rng)
            sheets.set(List(first, makePuzzle(rng, avoid = first.emoji)))
            val _ = js.timers.setTimeout(50)(Printable.print())
          }
        )
      ),
      div(
        cls := "print-only",
        Printable.render(
          title = _.offline.dotToDot.printTitle,
          body = div(
            cls := "dots-print-sheet",
            children <-- sheets.signal.map(_.map(printablePuzzle))
          )
        )
      )
    )

  private def printablePuzzle(p: Puzzle): HtmlElement =
    div(
      cls := "dots-print-board",
      dataAttr("emoji") := p.emoji, // e2e/debug: identify the generated puzzle
      svg.svg(
        svg.cls := "dots-print-svg",
        svg.viewBox := s"0 0 ${View.toInt} ${View.toInt}",
        if p.detailUrl.nonEmpty then imageLayer(p, p.detailUrl, "dots-detail") else emptyNode,
        p.dots.zipWithIndex.map { (d, i) =>
          svg.g(
            // A ring around dot 1 so the start is findable on paper.
            if i == 0 then
              svg.circle(svg.cls := "dots-print-start", svg.cx := fmt(d.x), svg.cy := fmt(d.y), svg.r := "2.6")
            else emptyNode,
            svg.circle(svg.cls := "dots-print-mark", svg.cx := fmt(d.x), svg.cy := fmt(d.y), svg.r := "0.9"),
            svg.text(
              svg.cls := "dots-print-num",
              svg.x := fmt(d.lx),
              svg.y := fmt(d.ly),
              svg.textAnchor := "middle",
              svg.dominantBaseline := "central",
              (i + 1).toString
            )
          )
        }
      )
    )

  // ---------- lab: generator eyeball page (dev only, not in any menu) ----------

  /** One card per pool emoji showing the *finished* puzzle as a kid would see
    * it on paper — straight-line outline plus the detail layer — next to the
    * stats and the real emoji, so the generator can be judged at a glance.
    * Paged (/lab/0, /lab/1, …) so screenshots stay readable. */
  private val LabPageSize = 12

  private def renderLab(pageIdx: Int): HtmlElement =
    div(
      cls := "dots-lab",
      emojis.slice(pageIdx * LabPageSize, (pageIdx + 1) * LabPageSize).map { e =>
        tracePuzzleInfo(e) match
          case Right((p, iou)) =>
            div(
              cls := "dots-lab-card",
              div(cls := "dots-lab-meta", s"$e  ${p.dots.size} dots  ${math.round(iou * 100)}%"),
              labBoard(p)
            )
          case Left(reason) =>
            div(
              cls := "dots-lab-card dots-lab-card--rejected",
              div(cls := "dots-lab-meta", s"$e  rejected: $reason")
            )
      }
    )

  private def labBoard(p: Puzzle): SvgElement =
    svg.svg(
      svg.cls := "dots-lab-svg",
      svg.viewBox := s"0 0 ${View.toInt} ${View.toInt}",
      // Half-opacity reveal under the outline: any transform bug shows as the
      // outline ghosting away from the picture.
      if p.imageUrl.nonEmpty then imageLayer(p, p.imageUrl, "dots-lab-reveal") else emptyNode,
      if p.detailUrl.nonEmpty then imageLayer(p, p.detailUrl, "dots-detail") else emptyNode,
      svg.polygon(
        svg.cls := "dots-lab-outline",
        svg.points := p.dots.map(d => s"${fmt(d.x)},${fmt(d.y)}").mkString(" ")
      ),
      p.dots.map(d =>
        svg.circle(svg.cls := "dots-lab-mark", svg.cx := fmt(d.x), svg.cy := fmt(d.y), svg.r := "0.9")
      )
    )
