package tandu.tools

import com.raquo.laminar.api.L.*
import org.scalajs.dom
import tandu.i18n.Strings
import tandu.ui.Components.s
import tandu.ui.DomExt.*

object Paint extends Tool:
  val id = "paint"
  val glyph = "✎"
  def name(s: Strings): String = s.paint.name
  def description(s: Strings): String = s.paint.description

  // Big crayon-box palette; white doubles as an eraser on the paper-white
  // canvas, so no separate eraser mode is needed.
  private val Colors: List[String] = List(
    "#1a1410", // ink
    "#d94f2a", // vermilion
    "#f2913c", // orange
    "#f7c948", // yellow
    "#4f9d4f", // green
    "#2d7fc1", // blue
    "#7b4fa6", // purple
    "#e87ea1", // pink
    "#8d5a2b", // brown
    "#ffffff"  // white / eraser
  )

  // One round brush, three sizes (CSS px; scaled to the canvas buffer when
  // drawing so strokes stay consistent across screen densities).
  private val Sizes: List[Int] = List(4, 10, 22)

  private val PaperColor = "#fffdf6"

  def render(): HtmlElement =
    val color = Var(Colors(1))
    val size  = Var(Sizes(1))

    // Stroke-in-progress state lives outside Airstream: it changes on every
    // pointermove and never drives the UI.
    var drawing = false
    var lastX   = 0.0
    var lastY   = 0.0

    val canvasEl = canvasTag(cls := "paint__canvas")
    def cv = canvasEl.ref
    val ctx = canvasEl.ref.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]

    // Sizes the buffer to the rendered element (devicePixelRatio-aware) and
    // fills it with paper. Also serves as "clear". Resizing the buffer resets
    // all context state, so the brush shape is (re)applied here.
    def resetCanvas(): Unit =
      val rect = cv.getBoundingClientRect()
      val dpr  = math.max(1.0, dom.window.devicePixelRatio)
      cv.width = (rect.width * dpr).toInt
      cv.height = (rect.height * dpr).toInt
      ctx.lineCap = "round"
      ctx.lineJoin = "round"
      ctx.fillStyle = PaperColor
      ctx.fillRect(0, 0, cv.width, cv.height)

    // Maps the event into buffer coordinates plus the matching brush width,
    // reading the layout rect once per event (pointermove is a hot path).
    // Going through the live rect keeps drawing accurate even if the element
    // is later resized.
    def at(e: dom.PointerEvent): (Double, Double, Double) =
      val r  = cv.getBoundingClientRect()
      val sx = cv.width / r.width
      (
        (e.clientX - r.left) * sx,
        (e.clientY - r.top) * cv.height / r.height,
        size.now() * sx
      )

    // A plain tap should leave a mark, so pointerdown stamps a dot.
    def dot(x: Double, y: Double, brush: Double): Unit =
      ctx.fillStyle = color.now()
      ctx.beginPath()
      ctx.arc(x, y, brush / 2, 0, math.Pi * 2)
      ctx.fill()

    def strokeTo(x: Double, y: Double, brush: Double): Unit =
      ctx.strokeStyle = color.now()
      ctx.lineWidth = brush
      ctx.beginPath()
      ctx.moveTo(lastX, lastY)
      ctx.lineTo(x, y)
      ctx.stroke()

    canvasEl.amend(
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
      cls := "paint no-print",
      // Defer sizing one frame so the layout (and therefore the element's
      // rect) is settled before the buffer is measured.
      onMountCallback { _ =>
        dom.window.requestAnimationFrame(_ => resetCanvas()): Unit
      },
      canvasEl,
      div(
        cls := "paint__palette",
        Colors.map { c =>
          button(
            cls := "paint__swatch",
            cls("is-active") <-- color.signal.map(_ == c),
            tpe := "button",
            aria.label := c,
            styleAttr := s"--swatch: $c;",
            onClick --> (_ => color.set(c))
          )
        }
      ),
      div(
        cls := "row row--between",
        div(
          cls := "paint__sizes",
          Sizes.map { sz =>
            button(
              cls := "paint__size",
              cls("is-active") <-- size.signal.map(_ == sz),
              tpe := "button",
              span(cls := "paint__size-dot", styleAttr := s"width: ${sz}px; height: ${sz}px;"),
              onClick --> (_ => size.set(sz))
            )
          }
        ),
        button(
          cls := "btn btn--ghost",
          child.text <-- s(_.paint.clear),
          onClick --> (_ => resetCanvas())
        )
      )
    )
