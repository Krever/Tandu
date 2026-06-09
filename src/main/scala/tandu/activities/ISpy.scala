package tandu.activities

import com.raquo.laminar.api.L.*
import tandu.Kind
import tandu.i18n.Strings
import tandu.ui.Components.s

object ISpy extends Activity:
  val id = "i-spy"
  def name(s: Strings): String = s.iSpy.name
  def description(s: Strings): String = s.iSpy.description
  val minPlayers: Int = 2
  val maxPlayers: Int = Int.MaxValue
  override val kind: Kind = Kind.OnTheGo
  val glyph: String = "🔍"
  val tint: String = "rose"

  def render(): HtmlElement =
    div(
      cls := "stack-lg",
      p(cls := "muted center", child.text <-- s(_.iSpy.hint)),
      div(
        cls := "rules-card",
        h3(cls := "rules-card__title", child.text <-- s(_.iSpy.howTitle)),
        ol(
          cls := "rules-list",
          li(child.text <-- s(_.iSpy.step1)),
          li(child.text <-- s(_.iSpy.step2)),
          li(child.text <-- s(_.iSpy.step3))
        ),
        h3(cls := "rules-card__title", child.text <-- s(_.iSpy.tipsTitle)),
        ul(
          cls := "rules-list",
          li(child.text <-- s(_.iSpy.tip1)),
          li(child.text <-- s(_.iSpy.tip2))
        )
      )
    )
