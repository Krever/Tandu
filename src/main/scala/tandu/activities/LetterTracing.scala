package tandu.activities

import com.raquo.laminar.api.L.*
import org.scalajs.dom
import tandu.{AppState, Kind, Page, Routing}
import tandu.audio.Speech
import tandu.i18n.{Lang, Strings}
import tandu.ui.{Components, Mode, ModeChooser, Printable}
import tandu.ui.Components.s
import tandu.ui.DomExt.*

import scala.scalajs.js

object LetterTracing extends Activity:
  val id = "letter-tracing"
  def name(s: Strings): String = s.letterTracing.name
  def description(s: Strings): String = s.letterTracing.description
  val minPlayers: Int = 1
  val maxPlayers: Int = 1
  val glyph: String = "✍️"
  val tint: String = "peach"
  override val kind: Kind = Kind.Learn

  // ---------- model ----------

  /** One pill-selectable set of characters to practise. Letters reuse the
    * per-language Hangman alphabets so diacritics are covered. German ß has
    * no upper-case form, so the upper set keeps only true capitals.
    * A `shuffled` variant is dealt in a fresh random order on each visit,
    * like a deck — every character still comes up exactly once. */
  final case class Variant(
      id: String,
      label: Strings => String,
      chars: Lang => Vector[Char],
      shuffled: Boolean = false
  )

  private def upperChars(l: Lang): Vector[Char] = HangmanBank.lettersFor(l).filter(_.isUpper)
  private def lowerChars(l: Lang): Vector[Char] = HangmanBank.lettersFor(l).map(_.toLower).distinct
  private val digitChars: Vector[Char] = ('0' to '9').toVector

  val variants: List[Variant] = List(
    Variant("upper",   _.letterTracing.upper,   upperChars),
    Variant("lower",   _.letterTracing.lower,   lowerChars),
    Variant("numbers", _.letterTracing.numbers, _ => digitChars),
    Variant("random",  _.letterTracing.random,  l => upperChars(l) ++ lowerChars(l) ++ digitChars, shuffled = true)
  )

  // Random order only makes sense interactively; printed sheets stay ordered.
  private val printVariants: List[Variant] = variants.filterNot(_.shuffled)

  private val DefaultVariant: Variant = variants.head

  // Canvas guide palette: paper like PaintSurface, ruled lines in a soft
  // blue, the model glyph in a gray light enough to trace over.
  private val Paper     = "#fffdf6"
  private val RuleBlue  = "#b8d4e8"
  private val GuideGray = "#d8d2c6"
  private val Ink       = "#2d7fc1"
  private val BrushCss  = 12 // CSS px; scaled to the buffer like PaintSurface

  // ---------- render ----------

  def render(): HtmlElement =
    ModeChooser.render(id, List(
      Mode(
        id = "in-app",
        label = _.mode.inApp,
        hint = Some(_.letterTracing.instruction),
        render = () => renderInApp()
      ),
      Mode(
        id = "print",
        label = _.mode.offline,
        materials = List(_.offline.materials.printer, _.offline.materials.paperPen),
        render = () => renderPrint()
      )
    ))

  // Read the variant from the URL path so it persists across reloads /
  // shares, mirroring WordBuilder's level-pill pattern.
  private def variantSignal(modeId: String): Signal[Variant] =
    Routing.router.currentPageSignal.map {
      case Page.Activity(`id`, m :: v :: _) if m == modeId =>
        variants.find(_.id == v).getOrElse(DefaultVariant)
      case _ => DefaultVariant
    }.distinct

  private def variantPill(modeId: String, current: Signal[Variant]): HtmlElement =
    div(
      cls := "center no-print",
      div(
        cls := "pill-toggle",
        variants.map { v =>
          button(
            cls := "pill-btn",
            cls("is-active") <-- current.map(_.id == v.id),
            child.text <-- AppState.strings.map(v.label),
            onClick --> (_ => Routing.go(Page.Activity(id, List(modeId, v.id))))
          )
        }
      )
    )

  // ---------- in-app ----------

  private def renderInApp(): HtmlElement =
    val current = variantSignal("in-app")
    div(
      cls := "stack-lg",
      variantPill("in-app", current),
      child <-- current.map(playForVariant)
    )

  private def playForVariant(v: Variant): HtmlElement =
    val idx = Var(0)

    // A shuffled variant draws its order once per mount (and per language),
    // so prev/next walk one stable deal instead of re-rolling every render.
    val charsFor: Lang => Vector[Char] =
      if !v.shuffled then v.chars
      else
        val dealt = collection.mutable.Map.empty[Lang, Vector[Char]]
        l => dealt.getOrElseUpdate(l, scala.util.Random.shuffle(v.chars(l)))

    def step(d: Int): Unit =
      val n = charsFor(AppState.lang.now()).size
      idx.update(i => (i + d + n) % n)

    // Character set and clamped position derived once, shared by the glyph
    // and the counter. Clamp via modulo: a language switch can shrink the
    // set before the reset below lands, and idx must never index out of
    // bounds.
    val posSig: Signal[(Vector[Char], Int)] =
      AppState.lang.signal.map(charsFor).combineWith(idx.signal).map { (cs, i) =>
        (cs, ((i % cs.size) + cs.size) % cs.size)
      }

    val charSig: Signal[Char]      = posSig.map((cs, i) => cs(i)).distinct
    val counterSig: Signal[String] = posSig.map((cs, i) => s"${i + 1} / ${cs.size}")

    val langChange = AppState.lang.signal.changes --> (_ => idx.set(0))

    // --- tracing canvas, modelled on PaintSurface with a guide background ---

    // Stroke-in-progress state lives outside Airstream: it changes on every
    // pointermove and never drives the UI.
    var drawing = false
    var lastX   = 0.0
    var lastY   = 0.0
    var current = ' '

    val canvasEl = canvasTag(cls := "lt-canvas")
    def cv = canvasEl.ref
    val ctx = canvasEl.ref.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]

    // Sizes the buffer (devicePixelRatio-aware), then paints paper, writing
    // rules and the guide glyph. Doubles as "clear": strokes are simply
    // painted over.
    def drawGuide(): Unit =
      val rect = cv.getBoundingClientRect()
      val dpr  = math.max(1.0, dom.window.devicePixelRatio)
      cv.width = (rect.width * dpr).toInt
      cv.height = (rect.height * dpr).toInt
      ctx.lineCap = "round"
      ctx.lineJoin = "round"
      ctx.fillStyle = Paper
      ctx.fillRect(0, 0, cv.width, cv.height)
      val w    = cv.width.toDouble
      val h    = cv.height.toDouble
      val top  = h * 0.24
      val base = h * 0.78
      def rule(y: Double, dashed: Boolean): Unit =
        ctx.strokeStyle = RuleBlue
        ctx.lineWidth = math.max(1.0, 1.5 * dpr)
        if dashed then ctx.setLineDash(js.Array(8.0 * dpr, 8.0 * dpr))
        ctx.beginPath()
        ctx.moveTo(0, y)
        ctx.lineTo(w, y)
        ctx.stroke()
        ctx.setLineDash(js.Array[Double]())
      rule(top, dashed = false)
      rule((top + base) / 2, dashed = true)
      rule(base, dashed = false)
      // Cap height is ~0.72em in common UI fonts, so this size makes a
      // capital span the top rule to the baseline.
      val fontPx = ((base - top) / 0.72).toInt
      ctx.fillStyle = GuideGray
      ctx.font = s"600 ${fontPx}px system-ui, sans-serif"
      ctx.textAlign = "center"
      ctx.textBaseline = "alphabetic"
      ctx.fillText(current.toString, w / 2, base)

    def at(e: dom.PointerEvent): (Double, Double, Double) =
      val r  = cv.getBoundingClientRect()
      val sx = cv.width / r.width
      (
        (e.clientX - r.left) * sx,
        (e.clientY - r.top) * cv.height / r.height,
        BrushCss * sx
      )

    def dot(x: Double, y: Double, brush: Double): Unit =
      ctx.fillStyle = Ink
      ctx.beginPath()
      ctx.arc(x, y, brush / 2, 0, math.Pi * 2)
      ctx.fill()

    def strokeTo(x: Double, y: Double, brush: Double): Unit =
      ctx.strokeStyle = Ink
      ctx.lineWidth = brush
      ctx.beginPath()
      ctx.moveTo(lastX, lastY)
      ctx.lineTo(x, y)
      ctx.stroke()

    canvasEl.amend(
      // rAF so the first paint happens after layout settles on mount; later
      // changes just repaint with a fresh guide.
      charSig --> { c =>
        current = c
        dom.window.requestAnimationFrame(_ => drawGuide()): Unit
      },
      onPointerDown --> { e =>
        e.preventDefault()
        cv.setPointerCapture(e.pointerId)
        drawing = true
        val (x, y, brush) = at(e)
        lastX = x
        lastY = y
        dot(x, y, brush)
      },
      onPointerMove --> { e =>
        if drawing then
          val (x, y, brush) = at(e)
          strokeTo(x, y, brush)
          lastX = x
          lastY = y
      },
      onPointerUp --> (_ => drawing = false),
      onPointerCancel --> (_ => drawing = false)
    )

    div(
      cls := "stack-lg",
      langChange,
      canvasEl,
      div(
        cls := "row no-print",
        styleAttr := "justify-content: center; align-items: center; gap: 0.75rem;",
        button(
          cls := "btn",
          aria.label <-- s(_.letterTracing.previous),
          "‹",
          onClick --> (_ => step(-1))
        ),
        div(cls := "lt-counter muted", child.text <-- counterSig),
        button(
          cls := "btn",
          aria.label <-- s(_.letterTracing.next),
          "›",
          onClick --> (_ => step(1))
        )
      ),
      div(
        cls := "row no-print",
        styleAttr := "justify-content: center; gap: 0.75rem;",
        // Hear the letter's sound while tracing its shape — the pairing is
        // the point of the exercise, and nothing here is a hidden answer.
        Components.speakBtn(charSig.map(Speech.spokenLetter)),
        Components.ghost(s(_.paint.clear), drawGuide())
      )
    )

  // ---------- print ----------

  private val RowsPerSheet = 10

  // Row geometry in viewBox units. A 75px glyph (cap height ~0.72em, set in
  // CSS) spans the 54-unit gap between the top rule and the baseline.
  private val RowW    = 760
  private val RowH    = 88
  private val RowTop  = 14.0
  private val RowBase = 68.0

  private def renderPrint(): HtmlElement =
    val slot = Printable.printSlot[Variant]()
    div(
      cls := "stack-lg",
      div(
        cls := "no-print stack",
        div(
          cls := "row",
          styleAttr := "justify-content: center; gap: 0.5rem; flex-wrap: wrap;",
          printVariants.map { v =>
            button(
              cls := "btn btn--lg",
              child.text <-- AppState.strings.map(str => s"${str.printable.print} — ${v.label(str)}"),
              onClick --> (_ => slot.trigger(v))
            )
          }
        ),
        p(cls := "muted center", child.text <-- s(_.letterTracing.printHint))
      ),
      slot.mount { v =>
        val sheets = v.chars(AppState.lang.now()).grouped(RowsPerSheet).toList
        div(
          sheets.map { group =>
            Printable.render(
              title = _.letterTracing.printTitle,
              body = div(cls := "lt-print-sheet", group.map(printRow))
            )
          }
        )
      }
    )

  /** One ruled worksheet row: a solid model glyph, then trace glyphs that
    * fade out towards the end of the line — guidance tapers off and the
    * last stretch is for writing freely. SVG keeps the glyph baselines
    * exactly on the drawn rules — no font-metric guesswork in CSS. */
  private def printRow(c: Char): SvgElement =
    def rule(y: Double, extraCls: String) =
      svg.line(
        svg.cls := s"lt-print-rule $extraCls".trim,
        svg.x1 := "0",
        svg.x2 := RowW.toString,
        svg.y1 := y.toString,
        svg.y2 := y.toString
      )
    def glyphAt(x: Int, glyphCls: String) =
      svg.text(
        svg.cls := s"lt-print-glyph $glyphCls".trim,
        svg.x := x.toString,
        svg.y := RowBase.toString,
        svg.textAnchor := "middle",
        c.toString
      )
    svg.svg(
      svg.cls := "lt-print-row",
      svg.viewBox := s"0 0 $RowW $RowH",
      rule(RowTop, ""),
      rule((RowTop + RowBase) / 2, "lt-print-rule--mid"),
      rule(RowBase, ""),
      glyphAt(55, ""),
      List(150, 245, 340, 435).map(x => glyphAt(x, "lt-print-glyph--trace")),
      List(530, 625).map(x => glyphAt(x, "lt-print-glyph--faint"))
    )
