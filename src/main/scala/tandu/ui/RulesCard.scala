package tandu.ui

import com.raquo.laminar.api.L.*
import tandu.AppState
import tandu.i18n.Strings

object RulesCard:

  final case class Section(title: Strings => String, lines: Strings => List[String])

  def fromRules(get: Strings => Strings.Rules): Section =
    Section(s => get(s).title, s => get(s).lines)

  def render(sections: List[Section]): HtmlElement =
    div(
      cls := "rules-card",
      sections.flatMap { sec =>
        List(
          h3(cls := "rules-card__title", child.text <-- AppState.strings.map(sec.title)),
          ul(
            cls := "rules-list",
            children <-- AppState.strings.map(s => sec.lines(s).map(line => li(line)))
          )
        )
      }
    )
