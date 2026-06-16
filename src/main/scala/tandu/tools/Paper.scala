package tandu.tools

import com.raquo.laminar.api.L.*
import tandu.AppState
import tandu.i18n.Strings
import tandu.ui.Printable
import tandu.workbook.Workbook

/** Quick paper printing: the workbook's blank / lined / squared sheets as a
  * standalone tool — for the parent who just needs squared paper now, no
  * book composition involved. */
object Paper extends Tool:
  val id = "paper"
  val glyph = "📄"
  def name(s: Strings): String = s.workbook.paperName
  def description(s: Strings): String = s.workbook.paperDescription

  private val kinds: List[(String, Strings => String)] = List(
    "blank"   -> (_.workbook.paperBlank),
    "lined"   -> (_.workbook.paperLined),
    "squared" -> (_.workbook.paperSquared)
  )

  def render(): HtmlElement =
    val slot = Printable.printSlot[String]()
    div(
      cls := "stack-lg",
      div(
        cls := "no-print stack-lg paper-print-actions",
        kinds.map { (kind, label) =>
          button(
            cls := "btn btn--lg btn--block",
            child.text <-- AppState.strings.map(str => s"${str.printable.print} — ${label(str)}"),
            onClick --> (_ => slot.trigger(kind))
          )
        }
      ),
      slot.mount(kind =>
        Printable.render(
          title = _.workbook.paperName,
          body = Workbook.paperSheetBody(kind)
        )
      )
    )
