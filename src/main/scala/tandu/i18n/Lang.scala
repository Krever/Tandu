package tandu.i18n

import org.scalajs.dom

enum Lang(val code: String, val display: String):
  case Pl extends Lang("pl", "Polski")
  case En extends Lang("en", "English")

object Lang:
  def fromCode(c: String): Option[Lang] = values.find(_.code == c)

  def detect(): Lang =
    val nav = Option(dom.window.navigator.language).getOrElse("").toLowerCase
    if nav.startsWith("pl") then Pl else En
