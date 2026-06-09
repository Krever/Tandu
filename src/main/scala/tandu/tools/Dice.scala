package tandu.tools

import com.raquo.laminar.api.L.*
import tandu.i18n.Strings
import tandu.ui.Components
import tandu.ui.Components.s

import scala.util.Random

object Dice extends Tool:
  val id = "dice"
  val glyph = "⚀"
  def name(s: Strings): String = s.dice.name
  def description(s: Strings): String = s.dice.description

  private val SidesOptions = List(4, 6, 8, 10, 12, 20)
  private val CountOptions = List(1, 2, 3, 4, 5)

  private final case class State(count: Int, sides: Int, faces: Vector[Int], rolling: Boolean, history: List[Vector[Int]])
  private object State:
    val initial: State = State(2, 6, Vector.fill(2)(1), rolling = false, history = Nil)

  def render(): HtmlElement =
    val state = Var(State.initial)

    def roll(): Unit =
      val st = state.now()
      val newFaces = Vector.fill(st.count)(Random.nextInt(st.sides) + 1)
      state.set(st.copy(faces = newFaces, rolling = true, history = (newFaces :: st.history).take(5)))
      val _ = org.scalajs.dom.window.setTimeout(
        () => state.update(s => s.copy(rolling = false)),
        500
      )

    def setCount(n: Int): Unit =
      state.update(s => s.copy(count = n, faces = Vector.fill(n)(if s.sides > 0 then 1 else 1)))

    def setSides(n: Int): Unit =
      state.update(s => s.copy(sides = n))

    div(
      cls := "stack-lg",
      Components.card(
        div(
          cls := "row row--wrap",
          styleAttr := "justify-content: center;",
          label(
            cls := "field",
            child.text <-- s(_.dice.dice),
            select(
              CountOptions.map(n => option(value := n.toString, n.toString)),
              value <-- state.signal.map(_.count.toString),
              onChange.mapToValue.map(_.toInt) --> setCount
            )
          ),
          label(
            cls := "field",
            child.text <-- s(_.dice.sides),
            select(
              SidesOptions.map(n => option(value := n.toString, s"d$n")),
              value <-- state.signal.map(_.sides.toString),
              onChange.mapToValue.map(_.toInt) --> setSides
            )
          )
        )
      ),
      div(
        cls := "dice-faces",
        children <-- state.signal.map { st =>
          st.faces.toList.map { face =>
            div(
              cls := "die",
              cls("is-rolling") := st.rolling,
              face.toString
            )
          }
        }
      ),
      div(
        cls := "center",
        styleAttr := "font-size: 18px;",
        span(cls := "muted", child.text <-- s(_.dice.total), ": "),
        span(child.text <-- state.signal.map(_.faces.sum.toString))
      ),
      Components.primaryBig(s(_.dice.roll), roll()),
      div(
        cls := "stack",
        h2(cls := "h2", child.text <-- s(_.dice.lastRolls)),
        div(
          cls := "stack",
          children <-- state.signal.map(_.history).map { hist =>
            hist.map { faces =>
              div(
                cls := "row row--between card",
                styleAttr := "padding: 8px 12px;",
                span(faces.mkString(", ")),
                span(cls := "muted", faces.sum.toString)
              )
            }
          }
        )
      )
    )
