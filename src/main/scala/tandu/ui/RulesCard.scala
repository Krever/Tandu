package tandu.ui

import com.raquo.laminar.api.L.*
import tandu.AppState
import tandu.i18n.Strings

/** Bilingual "How to play" card. Sections are titled and contain a list
  * of rule lines. Used by activities that play offline and need a quick
  * reference for the family. */
object RulesCard:

  final case class Section(title: Strings => String, lines: List[Strings => String])

  def render(sections: List[Section]): HtmlElement =
    div(
      cls := "rules-card",
      sections.flatMap { sec =>
        List(
          h3(cls := "rules-card__title", child.text <-- AppState.strings.map(sec.title)),
          ul(
            cls := "rules-list",
            sec.lines.map(line => li(child.text <-- AppState.strings.map(line)))
          )
        )
      }
    )
