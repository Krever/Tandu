package tandu.activities

import com.raquo.laminar.api.L.*
import org.scalajs.dom
import tandu.{AppState, Page, Routing}
import tandu.i18n.Strings
import tandu.ui.{Components, Mode, ModeChooser}
import tandu.ui.Components.s

import scala.scalajs.js
import scala.util.Random

/** A drag-and-drop jigsaw.
  *
  * The picture is a square raster (a generated emoji scene, or an uploaded
  * photo cover-cropped to square) drawn once to a canvas and kept as a data
  * URL. Each piece is that *same* full image, clipped to an interlocking
  * jigsaw outline and wrapped in a `<g>` we translate: at offset (0,0) the slice
  * sits exactly in its home cell, so when every piece is home the slices retile
  * into the original picture. Dragging just changes a piece's offset; dropping
  * near home (offset ≈ 0) snaps it down and locks it.
  *
  * Why the whole image per piece rather than per-piece bitmaps: the clip path
  * and the image live in the same translated coordinate system, so one image +
  * one clip path expresses both *what* a piece shows and *where* it belongs,
  * and reassembly is pixel-exact for free.
  */
object Jigsaw extends Activity:
  val id = "jigsaw"
  def name(s: Strings): String        = s.jigsaw.name
  def description(s: Strings): String = s.jigsaw.description
  val minPlayers: Int = 1
  val maxPlayers: Int = Int.MaxValue
  val glyph: String = "🧩"
  val tint: String  = "mustard"

  // ---------- geometry constants ----------

  /** The board is drawn in this square user-unit space and scaled to fit by the
    * SVG viewBox, so all piece maths is resolution-independent. The tray for
    * loose pieces sits below it. */
  private val Board   = 600.0
  private val TrayGap = Board * 0.05
  private val TrayTop = Board + TrayGap
  private val TrayH   = Board * 0.85
  private val ViewH   = TrayTop + TrayH

  /** How close (as a fraction of the smaller cell dimension) a dropped piece
    * must be to its home before it snaps in. */
  private val SnapFraction = 0.33

  // ---------- variants ----------

  final case class Variant(id: String, rows: Int, cols: Int, nameKey: Strings => String, descKey: Strings => String)

  val variants: List[Variant] = List(
    Variant("easy",   3, 3, _.jigsaw.easy.name,   _.jigsaw.easy.description),
    Variant("medium", 4, 4, _.jigsaw.medium.name, _.jigsaw.medium.description),
    Variant("hard",   5, 5, _.jigsaw.hard.name,   _.jigsaw.hard.description)
  )

  // ---------- piece outline generation ----------

  private type Pt = (Double, Double)

  /** A jigsaw knob as waypoints in edge-local coordinates: `t` runs 0→1 along
    * the edge, `p` is the perpendicular bulge (0 on the baseline, up to ~0.3 at
    * the top of the knob). The bulb is wider than the neck (t doubles back),
    * which is what makes pieces read as interlocking. Catmull-Rom smoothing
    * later rounds these corners. */
  private val Knob: Vector[Pt] = Vector(
    (0.00, 0.00), (0.42, 0.00), (0.48, 0.10), (0.35, 0.18), (0.42, 0.30),
    (0.58, 0.30), (0.65, 0.18), (0.52, 0.10), (0.58, 0.00), (1.00, 0.00)
  )

  /** Sample one edge from `p0` to `p1`. `sign` 0 → a straight border edge;
    * ±1 → a knob bulging to the left of travel (so a tab on one piece is the
    * matching blank on its neighbour, since the neighbour walks the same edge
    * the other way with the same sign). `tab` scales the bulge in user units. */
  private def edgeCurve(p0: Pt, p1: Pt, sign: Int, tab: Double): Vector[Pt] =
    if sign == 0 then Vector(p0, p1)
    else
      val (x0, y0) = p0
      val (x1, y1) = p1
      val dx = x1 - x0
      val dy = y1 - y0
      val len = math.hypot(dx, dy)
      val ux = dx / len
      val uy = dy / len
      // Left normal of the travel direction.
      val nx = uy
      val ny = -ux
      Knob.map { case (t, p) =>
        (x0 + ux * (t * len) + nx * (sign * p * tab),
         y0 + uy * (t * len) + ny * (sign * p * tab))
      }

  /** Turn an edge's sample points into SVG path commands continuing from the
    * point already reached (the edge's first point). Two points → a straight
    * line; more → a Catmull-Rom spline emitted as cubic Béziers. Each edge is
    * smoothed on its own (endpoints clamped) so the four corners of a piece stay
    * crisp right angles. */
  private def smoothSegs(pts: Vector[Pt]): String =
    if pts.size < 3 then pts.tail.map((x, y) => f"L $x%.2f $y%.2f").mkString(" ")
    else
      val n = pts.size
      (0 until n - 1).map { i =>
        val (x0, y0) = pts(math.max(i - 1, 0))
        val (x1, y1) = pts(i)
        val (x2, y2) = pts(i + 1)
        val (x3, y3) = pts(math.min(i + 2, n - 1))
        val c1x = x1 + (x2 - x0) / 6; val c1y = y1 + (y2 - y0) / 6
        val c2x = x2 - (x3 - x1) / 6; val c2y = y2 - (y3 - y1) / 6
        f"C $c1x%.2f $c1y%.2f $c2x%.2f $c2y%.2f $x2%.2f $y2%.2f"
      }.mkString(" ")

  final case class Piece(idx: Int, r: Int, c: Int, d: String, homeCx: Double, homeCy: Double)

  final case class Puzzle(
      image: String,
      rows: Int,
      cols: Int,
      salt: Int,                 // makes clipPath ids unique across regenerations
      pieces: Vector[Piece],
      scatter: Vector[Pt]        // initial offset (from home) for each piece
  )

  /** Build a fresh puzzle: random shared-edge signs, one outline path per piece,
    * and a random scatter of every piece into the tray below the board. */
  private def buildPuzzle(image: String, v: Variant, rng: Random): Puzzle =
    val rows = v.rows
    val cols = v.cols
    val cw = Board / cols
    val ch = Board / rows
    val tab = math.min(cw, ch)

    def sign() = if rng.nextBoolean() then 1 else -1
    // hSign(r)(c): horizontal boundary below row r (canonical travel: left→right).
    val hSign = Vector.tabulate(rows - 1, cols)((_, _) => sign())
    // vSign(r)(c): vertical boundary right of column c (canonical travel: top→bottom).
    val vSign = Vector.tabulate(rows, cols - 1)((_, _) => sign())

    // One edge, walked from p0 to p1. sign 0 → a straight border (edgeCurve
    // already collapses to just the two endpoints); `rev` flips the canonical
    // curve for the two edges a clockwise piece traverses backwards.
    def edge(p0: Pt, p1: Pt, sign: Int, rev: Boolean): Vector[Pt] =
      val pts = edgeCurve(p0, p1, sign, tab)
      if rev then pts.reverse else pts

    def pieceD(r: Int, c: Int): String =
      val tl = (c * cw, r * ch)
      val tr = ((c + 1) * cw, r * ch)
      val br = ((c + 1) * cw, (r + 1) * ch)
      val bl = (c * cw, (r + 1) * ch)
      val top    = edge(tl, tr, if r == 0        then 0 else hSign(r - 1)(c), rev = false)
      val right  = edge(tr, br, if c == cols - 1 then 0 else vSign(r)(c),     rev = false)
      val bottom = edge(bl, br, if r == rows - 1 then 0 else hSign(r)(c),     rev = true)
      val left   = edge(tl, bl, if c == 0        then 0 else vSign(r)(c - 1), rev = true)
      val segs = List(top, right, bottom, left).map(smoothSegs).mkString(" ")
      f"M ${tl._1}%.2f ${tl._2}%.2f " + segs + " Z"

    val pieces = Vector.tabulate(rows, cols) { (r, c) =>
      Piece(r * cols + c, r, c, pieceD(r, c), (c + 0.5) * cw, (r + 0.5) * ch)
    }.flatten

    val pad = math.min(cw, ch) * 0.6
    val scatter = pieces.map { p =>
      val tx = pad + rng.nextDouble() * (Board - 2 * pad)
      val ty = TrayTop + ch * 0.6 + rng.nextDouble() * (TrayH - ch * 1.2)
      (tx - p.homeCx, ty - p.homeCy)
    }

    Puzzle(image, rows, cols, rng.nextInt(1000000), pieces, scatter)

  // ---------- scene generation ----------

  private final case class Theme(top: String, bottom: String, emojis: Vector[String])

  private val themes: Vector[Theme] = Vector(
    Theme("#bde7ff", "#9be57f", Vector("🌳", "🌻", "🦋", "🐝", "🐞", "🌷", "🐰", "🍄", "🌈", "🐌", "🌼", "🦔")),
    Theme("#7ec8ff", "#ffe6a1", Vector("🌴", "🦀", "🐚", "⛵", "🐠", "🦩", "🍉", "🪁", "🐢", "🌅", "🩴", "🐳")),
    Theme("#2a1b4a", "#0a0a1a", Vector("🪐", "🚀", "⭐", "🌕", "👽", "☄️", "🌟", "🛸", "✨", "🌠", "🔭", "👾")),
    Theme("#2aa3d4", "#0a4a6a", Vector("🐠", "🐙", "🐳", "🦀", "🐚", "🐡", "🦈", "🐟", "🦑", "🪸", "🐬", "⭐")),
    Theme("#bfe9ff", "#cdeb8b", Vector("🐄", "🐓", "🐖", "🚜", "🌾", "🏠", "🐑", "🦆", "🐴", "🌻", "🍎", "🧺"))
  )

  /** Paint a themed scene onto an off-screen canvas and return it as a PNG data
    * URL. A jittered 4×4 grid guarantees every region has a recognisable
    * landmark (so no piece is a featureless blank), with one big focal emoji. */
  private def generateScene(rng: Random): String =
    val canvas = dom.document.createElement("canvas").asInstanceOf[dom.HTMLCanvasElement]
    canvas.width = Board.toInt
    canvas.height = Board.toInt
    val ctx = canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
    val theme = themes(rng.nextInt(themes.size))

    val grad = ctx.createLinearGradient(0, 0, 0, Board)
    grad.addColorStop(0, theme.top)
    grad.addColorStop(1, theme.bottom)
    ctx.fillStyle = grad
    ctx.fillRect(0, 0, Board, Board)

    ctx.textAlign = "center"
    ctx.textBaseline = "middle"
    val gridN = 4
    val cell = Board / gridN
    for gr <- 0 until gridN; gc <- 0 until gridN do
      val e = theme.emojis(rng.nextInt(theme.emojis.size))
      val size = cell * (0.5 + rng.nextDouble() * 0.35)
      ctx.font = s"${size.toInt}px sans-serif"
      val jx = gc * cell + cell * 0.5 + (rng.nextDouble() - 0.5) * cell * 0.4
      val jy = gr * cell + cell * 0.5 + (rng.nextDouble() - 0.5) * cell * 0.4
      ctx.fillText(e, jx, jy)

    ctx.font = s"${(Board * 0.32).toInt}px sans-serif"
    ctx.fillText(theme.emojis(rng.nextInt(theme.emojis.size)), Board * 0.5, Board * 0.5)

    canvas.toDataURL("image/png")

  /** Cover-crop a loaded image into the square board canvas. */
  private def drawCoverToDataUrl(img: dom.HTMLImageElement): String =
    val canvas = dom.document.createElement("canvas").asInstanceOf[dom.HTMLCanvasElement]
    canvas.width = Board.toInt
    canvas.height = Board.toInt
    val ctx = canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
    val iw = img.naturalWidth.toDouble
    val ih = img.naturalHeight.toDouble
    val scale = math.max(Board / iw, Board / ih)
    val dw = iw * scale
    val dh = ih * scale
    ctx.drawImage(img, (Board - dw) / 2, (Board - dh) / 2, dw, dh)
    canvas.toDataURL("image/png")

  private def loadPhoto(file: dom.File)(cb: String => Unit): Unit =
    val reader = new dom.FileReader()
    reader.onload = (_: dom.Event) =>
      val img = dom.document.createElement("img").asInstanceOf[dom.HTMLImageElement]
      img.onload = (_: dom.Event) => cb(drawCoverToDataUrl(img))
      img.src = reader.result.asInstanceOf[String]
    reader.readAsDataURL(file)

  // ---------- play state ----------

  final case class PieceSt(offset: Pt, placed: Boolean)
  final case class BoardSt(pieces: Vector[PieceSt]):
    def solved: Boolean = pieces.forall(_.placed)

  private def initialBoard(p: Puzzle): BoardSt =
    BoardSt(p.scatter.map(o => PieceSt(o, false)))

  // ---------- UI ----------

  def render(): HtmlElement =
    // A single in-app mode (ModeChooser collapses it to the body) — matching the
    // other interactive activities, leaving room to add a print/rules mode later.
    ModeChooser.render(id, List(
      Mode(id = "in-app", label = _.mode.inApp, render = () => renderPlay())
    ))

  private def renderPlay(): HtmlElement =
    div(cls := "jigsaw stack-lg", variantPill(), child <-- variantSignal.map(playForVariant))

  // Easy is the implicit default when the URL has no variant segment.
  private val DefaultVariant = variants(0)
  private val variantSignal: Signal[Variant] =
    Routing.router.currentPageSignal.map {
      case Page.Activity(`id`, vId :: _) => variants.find(_.id == vId).getOrElse(DefaultVariant)
      case _                             => DefaultVariant
    }.distinct

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
            onClick --> (_ => Routing.go(Page.Activity(id, List(v.id))))
          )
        }
      )
    )

  private def playForVariant(v: Variant): HtmlElement =
    val rng    = new Random()
    val image  = Var(generateScene(rng))
    val puzzle = Var(buildPuzzle(image.now(), v, rng))
    val peek   = Var(false)

    def newPicture(): Unit =
      val scene = generateScene(rng)
      image.set(scene)
      puzzle.set(buildPuzzle(scene, v, rng))

    def reshuffle(): Unit = puzzle.set(buildPuzzle(image.now(), v, rng))

    def usePhoto(url: String): Unit =
      image.set(url)
      puzzle.set(buildPuzzle(url, v, rng))

    div(
      cls := "stack-lg",
      p(cls := "muted center no-print", child.text <-- s(_.jigsaw.instruction)),
      child <-- puzzle.signal.map(p => boardView(p, peek, () => newPicture())),
      div(
        cls := "row jigsaw-controls no-print",
        styleAttr := "justify-content: center; flex-wrap: wrap;",
        Components.ghost(s(_.jigsaw.newPicture), newPicture()),
        Components.ghost(s(_.jigsaw.shuffle), reshuffle()),
        Components.ghost(
          s(_.jigsaw.peek),
          peek.update(!_)
        ),
        label(
          cls := "btn btn--ghost",
          child.text <-- s(_.jigsaw.photo),
          input(
            cls := "jigsaw-file-input",
            tpe := "file",
            accept := "image/*",
            onChange --> { ev =>
              val inp = ev.target.asInstanceOf[dom.HTMLInputElement]
              val fs  = inp.files
              if fs != null && fs.length > 0 then loadPhoto(fs(0))(usePhoto)
              inp.value = ""
            }
          )
        )
      )
    )

  private def boardView(puzzle: Puzzle, peek: Var[Boolean], onNew: () => Unit): HtmlElement =
    val board = Var(initialBoard(puzzle))
    val solved = board.signal.map(_.solved).distinct
    div(
      cls := "stack-lg",
      svg.svg(
        svg.cls := "jigsaw-svg",
        // Once solved every piece is home, so the tray is empty — crop the
        // viewBox to just the board so the finished picture sits flush above
        // the win card instead of leaving a tall blank gap.
        svg.cls("is-solved") <-- solved,
        svg.viewBox <-- solved.map(s => if s then s"0 0 ${Board.toInt} ${Board.toInt}" else s"0 0 ${Board.toInt} ${ViewH.toInt}"),
        svg.defs(
          // The picture is declared once and every piece (and the ghost) draws
          // it via <use>, so the multi-KB data URL isn't duplicated per piece.
          svg.image(
            svg.idAttr := s"js-${puzzle.salt}",
            svg.href := puzzle.image,
            svg.x := "0", svg.y := "0",
            svg.width := Board.toInt.toString, svg.height := Board.toInt.toString
          ),
          puzzle.pieces.map { p =>
            svg.clipPathTag(
              svg.idAttr := s"jc-${puzzle.salt}-${p.idx}",
              svg.path(svg.d := p.d)
            )
          }
        ),
        // Faint goal image, raised on "peek".
        svg.use(
          svg.cls := "jigsaw-ghost",
          svg.href := s"#js-${puzzle.salt}",
          svg.opacity <-- peek.signal.map(if _ then "0.4" else "0")
        ),
        svg.rect(
          svg.cls := "jigsaw-frame",
          svg.x := "1", svg.y := "1",
          svg.width := (Board - 2).toString, svg.height := (Board - 2).toString,
          svg.rx := "8"
        ),
        svg.line(
          svg.cls := "jigsaw-tray-divider",
          svg.x1 := "0", svg.y1 := (Board + TrayGap / 2).toString,
          svg.x2 := Board.toString, svg.y2 := (Board + TrayGap / 2).toString
        ),
        svg.g(
          svg.cls := "jigsaw-pieces",
          puzzle.pieces.map(p => renderPiece(p, puzzle, board))
        )
      ),
      child <-- solved.map {
        case false => emptyNode
        case true =>
          div(
            cls := "handoff card",
            div(cls := "handoff__title", child.text <-- s(_.jigsaw.won)),
            button(cls := "btn btn--lg", child.text <-- s(_.jigsaw.newPicture), onClick --> (_ => onNew()))
          )
      }
    )

  private def renderPiece(p: Piece, puzzle: Puzzle, board: Var[BoardSt]): SvgElement =
    val sig = board.signal.map(_.pieces(p.idx)).distinct

    // Drag bookkeeping — plain closure vars, since only one piece drags at a
    // time and these never drive rendering.
    var dragging = false
    var startCx  = 0.0
    var startCy  = 0.0
    var startOff: Pt = (0.0, 0.0)
    var scale    = 1.0

    def updateOffset(o: Pt): Unit =
      board.update(b => b.copy(pieces = b.pieces.updated(p.idx, b.pieces(p.idx).copy(offset = o))))

    svg.g(
      svg.cls := "jigsaw-piece",
      svg.cls("is-placed") <-- sig.map(_.placed),
      svg.transform <-- sig.map(st => s"translate(${st.offset._1} ${st.offset._2})"),
      svg.use(
        svg.href := s"#js-${puzzle.salt}",
        svg.clipPathAttr := s"url(#jc-${puzzle.salt}-${p.idx})"
      ),
      svg.path(svg.cls := "jigsaw-piece-edge", svg.d := p.d),
      onPointerDown --> { ev =>
        if !board.now().pieces(p.idx).placed then
          ev.preventDefault()
          val g     = ev.currentTarget.asInstanceOf[dom.SVGGElement]
          val svgEl = g.ownerSVGElement
          val rect  = svgEl.getBoundingClientRect()
          scale    = Board / rect.width
          startCx  = ev.clientX
          startCy  = ev.clientY
          startOff = board.now().pieces(p.idx).offset
          dragging = true
          // Bring to front: move to the end of the (statically-built) sibling
          // list so a grabbed piece always draws over the others.
          val parent = g.parentNode
          val _ = parent.removeChild(g)
          val _ = parent.appendChild(g)
          // Capture AFTER re-inserting: removing the node from the DOM releases
          // pointer capture, so capturing first would lose it immediately and
          // fast drags that outrun the piece would stop firing pointermove.
          val _ = g.asInstanceOf[js.Dynamic].setPointerCapture(ev.pointerId)
      },
      onPointerMove --> { ev =>
        if dragging then
          updateOffset((startOff._1 + (ev.clientX - startCx) * scale,
                        startOff._2 + (ev.clientY - startCy) * scale))
      },
      onPointerUp --> { _ =>
        if dragging then
          dragging = false
          val off  = board.now().pieces(p.idx).offset
          val snap = math.min(Board / puzzle.cols, Board / puzzle.rows) * SnapFraction
          if math.hypot(off._1, off._2) < snap then
            board.update(b => b.copy(pieces = b.pieces.updated(p.idx, PieceSt((0.0, 0.0), placed = true))))
      },
      onPointerCancel --> (_ => dragging = false)
    )
