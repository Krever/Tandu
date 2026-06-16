package tandu.tools

import com.raquo.laminar.api.L.*
import tandu.i18n.Strings

trait Tool:
  def id: String
  def glyph: String
  def name(s: Strings): String
  def description(s: Strings): String
  def render(): HtmlElement

object Tools:
  val all: List[Tool] = List(Dice, Timer, Paint, Paper)
  def byId(id: String): Option[Tool] = all.find(_.id == id)
