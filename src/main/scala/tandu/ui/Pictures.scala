package tandu.ui

import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.*
import org.scalajs.dom
import tandu.{AppState, Storage}

import scala.collection.mutable
import scala.scalajs.js

/** The pictures of the app are emoji characters that our own two fonts draw
  * (see `scripts/picture-fonts.mjs`):
  *
  *   - "Tandu Pictures": vector colour pictures (Noto Color Emoji).
  *   - "Tandu Pictures Mono": line pictures for black-and-white print.
  *
  * Vector fonts stay sharp at all sizes and look the same on all devices.
  * The emoji font of a device is frequently a bitmap font, which becomes
  * blurred in a large print.
  *
  * In HTML and SVG, the CSS selects the font: add the class of the element
  * to the list of content pictures in `styles.css`. The CSS also selects the
  * mono font for a black-and-white print. On a canvas, CSS has no effect,
  * thus use `draw`. For a picture that must be an image, use `src`. */
object Pictures:

  val ColorFamily = "Tandu Pictures"
  val MonoFamily  = "Tandu Pictures Mono"

  /** `true` when printed sheets must use the mono pictures. The value is
    * persisted. The root element gets a class, and the print CSS reads it. */
  val printBw: Var[Boolean] = Var(Storage.loadPrintBw())

  val _ = printBw.signal.foreach { on =>
    Storage.savePrintBw(on)
    val _ = dom.document.documentElement.classList.toggle("print-bw", on)
  }(using unsafeWindowOwner)

  /** The switch for black-and-white print. Put it adjacent to a print button. */
  def bwToggle(): HtmlElement =
    val label = AppState.strings.map(_.printable.blackAndWhite)
    // A <label> element: a click on the text also operates the switch.
    L.label(
      cls := "no-print print-setting",
      span(cls := "print-setting__label", child.text <-- label),
      Components.switch(printBw.signal, label, () => printBw.update(!_), testId = "print-bw")
    )

  private def fontOf(px: Int, mono: Boolean): String =
    s"""${px}px "${if mono then MonoFamily else ColorFamily}", sans-serif"""

  /** Draw the picture on a canvas, with its centre at (cx, cy). `px` is the
    * font size. A canvas falls back to the emoji font of the device if the
    * picture font is not loaded, thus a screen that draws on mount must be
    * in `whenDrawable`. */
  def draw(ctx: dom.CanvasRenderingContext2D, emoji: String, px: Int, cx: Double, cy: Double, mono: Boolean = false): Unit =
    ctx.textAlign = "center"
    ctx.textBaseline = "middle"
    ctx.font = fontOf(px, mono)
    ctx.fillText(emoji, cx, cy)

  /** Part of a square raster that the glyph fills, for a picture drawn in
    * the centre of the square. */
  val SquareFill = 0.8

  /** Draw the picture in the centre of a square raster of `side` px. */
  def drawSquare(ctx: dom.CanvasRenderingContext2D, emoji: String, side: Int, mono: Boolean = false): Unit =
    draw(ctx, emoji, (side * SquareFill).toInt, side / 2.0, side / 2.0, mono)

  // ---------- font loading ----------
  //
  // The browser loads a font only when something uses it, and a canvas does
  // not wait for it. Thus this object starts the downloads and publishes
  // their state. Two states are different:
  //
  //   - "final": the load promise of the font is settled. The font is there,
  //     or the browser cannot use it (then the emoji of the device is the
  //     picture for this session). Results drawn now can be kept.
  //   - "drawable": final, or the time limit is over. A screen does not wait
  //     longer than the limit, but results drawn before "final" are not kept.

  private val LoadTimeoutMs = 2500
  private def fonts: js.Dynamic = dom.document.asInstanceOf[js.Dynamic].fonts
  private def loadFont(family: String): js.Promise[Any] =
    fonts.load(s"""16px "$family"""", "🐟").asInstanceOf[js.Promise[Any]]

  private val colorFinal    = Var(false)
  private val monoFinal     = Var(false)
  private val colorDrawable = Var(false)

  private def final_(mono: Boolean): Boolean = (if mono then monoFinal else colorFinal).now()

  /** `true` when the state of the colour font is final (see above). Canvas
    * code can keep a result for the session only when this is `true`. */
  def colorFontFinal: Boolean = colorFinal.now()

  /** Start the font downloads. Call it one time at the start of the app.
    * The mono font is necessary only for black-and-white print, thus its
    * download starts after the colour font, and the two do not share the
    * network. */
  def preload(): Unit =
    if js.isUndefined(fonts) then
      colorFinal.set(true)
      monoFinal.set(true)
      colorDrawable.set(true)
    else
      def settled(p: js.Promise[Any])(k: () => Unit): Unit =
        val _ = p.`then`[Unit]((_: Any) => k(), (_: Any) => k())
      settled(loadFont(ColorFamily))(() => colorFinal.set(true))
      DomExt.afterOrTimeout(loadFont(ColorFamily), LoadTimeoutMs) { () =>
        colorDrawable.set(true)
        settled(loadFont(MonoFamily))(() => monoFinal.set(true))
      }

  /** A screen that draws pictures on a canvas when it mounts (dot-to-dot,
    * jigsaw). `content` is built when the colour font is ready, or after the
    * time limit. The other screens of the app do not wait for the font. */
  def whenDrawable(content: => HtmlElement): HtmlElement =
    div(child <-- colorDrawable.signal.map(ok => if ok then content else emptyNode))

  private val RasterPx    = 384
  private val rasterCache = mutable.Map.empty[(String, Boolean), String]
  private lazy val rasterCanvas: dom.HTMLCanvasElement =
    val c = dom.document.createElement("canvas").asInstanceOf[dom.HTMLCanvasElement]
    c.width = RasterPx
    c.height = RasterPx
    c

  /** The picture as a PNG data URL (see `SquareFill` for its size in the
    * square). The result is cached for the session, but only when the
    * state of the font is final: a raster made with the fallback font,
    * while the picture font was on its way, must not stay. */
  def dataUrl(emoji: String, mono: Boolean = false): String =
    rasterCache.getOrElse((emoji, mono), {
      val ctx = rasterCanvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
      ctx.clearRect(0, 0, RasterPx, RasterPx)
      ctx.fillStyle = "#000"
      drawSquare(ctx, emoji, RasterPx, mono)
      val url = rasterCanvas.toDataURL("image/png")
      if final_(mono) then rasterCache((emoji, mono)) = url
      url
    })

  /** The image source of a picture on a printable sheet: the colour picture,
    * or the line picture while black-and-white print is on. Bind it to `src`
    * or `svg.href`. The image then follows the switch, and the code that
    * builds the sheet does not have to build it again. An image that was
    * drawn before its font arrived is drawn again when the font is there.
    *
    * Use an image for a printed picture that must have an exact position
    * (SVG `image`). */
  def src(emoji: String): Signal[String] =
    printBw.signal
      .combineWith(colorFinal.signal, monoFinal.signal)
      .map((bw, _, _) => dataUrl(emoji, bw))
      .distinct
