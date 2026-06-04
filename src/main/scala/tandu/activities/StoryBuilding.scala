package tandu.activities

import com.raquo.laminar.api.L.*
import tandu.i18n.Strings
import tandu.ui.Components.s

object StoryBuilding extends Activity:
  val id = "story-building"
  def name(s: Strings): String = s.storyBuilding.name
  def description(s: Strings): String = s.storyBuilding.description
  val minPlayers: Int = 2
  val maxPlayers: Int = Int.MaxValue
  val handsFree: Boolean = true
  val glyph: String = "✦"
  val tint: String = "plum"

  def render(): HtmlElement =
    div(
      cls := "stack-lg",
      p(cls := "muted center", child.text <-- s(_.storyBuilding.hint)),
      div(
        cls := "rules-card",
        h3(cls := "rules-card__title", child.text <-- s(_.storyBuilding.howTitle)),
        ol(
          cls := "rules-list",
          li(child.text <-- s(_.storyBuilding.step1)),
          li(child.text <-- s(_.storyBuilding.step2)),
          li(child.text <-- s(_.storyBuilding.step3))
        ),
        h3(cls := "rules-card__title", child.text <-- s(_.storyBuilding.variantsTitle)),
        ul(
          cls := "rules-list",
          li(child.text <-- s(_.storyBuilding.variantWord)),
          li(child.text <-- s(_.storyBuilding.variantSentence))
        )
      )
    )
