package tandu.pages

import com.raquo.laminar.api.L.*
import tandu.tools.Tools
import tandu.ui.Components
import tandu.ui.Components.s

object ToolPage:

  def render(id: String): HtmlElement =
    Tools.byId(id) match
      case Some(tool) =>
        div(
          cls := "app stack-lg",
          Components.header(s(tool.name)),
          tool.render()
        )
      case None =>
        div(
          cls := "app stack-lg",
          Components.header(s(_.appTitle)),
          div(cls := "card", "Unknown tool.")
        )
