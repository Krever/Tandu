package tandu

import org.scalajs.dom
import scala.util.Try

object Storage:
  private val KeyLang = "tandu.lang"
  private val KeyKind = "tandu.kind"

  def getString(key: String): Option[String] =
    Try(Option(dom.window.localStorage.getItem(key))).toOption.flatten

  def setString(key: String, value: String): Unit =
    val _ = Try(dom.window.localStorage.setItem(key, value))

  def loadLangCode(): Option[String] = getString(KeyLang)
  def saveLangCode(code: String): Unit = setString(KeyLang, code)

  def loadKind(): Option[String] = getString(KeyKind)
  def saveKind(value: String): Unit = setString(KeyKind, value)
