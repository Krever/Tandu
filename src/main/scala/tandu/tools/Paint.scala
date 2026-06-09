package tandu.tools

import com.raquo.laminar.api.L.*
import tandu.i18n.Strings
import tandu.ui.PaintSurface

object Paint extends Tool:
  val id = "paint"
  val glyph = "✎"
  def name(s: Strings): String = s.paint.name
  def description(s: Strings): String = s.paint.description

  def render(): HtmlElement = PaintSurface.render()
