package tandu.i18n

import org.scalajs.dom

enum Lang(val code: String, val flag: String, val display: String):
  case En extends Lang("en", "🇬🇧", "English")
  case Pl extends Lang("pl", "🇵🇱", "Polski")
  case Es extends Lang("es", "🇪🇸", "Español")
  case Fr extends Lang("fr", "🇫🇷", "Français")
  case De extends Lang("de", "🇩🇪", "Deutsch")

object Lang:
  def fromCode(c: String): Option[Lang] = values.find(_.code == c)

  def detect(): Lang =
    val nav = Option(dom.window.navigator.language).getOrElse("").toLowerCase
    values.find(l => nav.startsWith(l.code)).getOrElse(En)
