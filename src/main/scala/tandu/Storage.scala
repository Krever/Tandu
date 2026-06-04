package tandu

import org.scalajs.dom
import scala.util.Try

object Storage:
  private val KeyLang           = "tandu.lang"
  private val KeyKind           = "tandu.kind"
  private val KeyFavourites     = "tandu.favourites"
  private val KeyFavouritesOnly = "tandu.favouritesOnly"
  private val KeyClockFormat    = "tandu.clockFormat"

  def getString(key: String): Option[String] =
    Try(Option(dom.window.localStorage.getItem(key))).toOption.flatten

  def setString(key: String, value: String): Unit =
    val _ = Try(dom.window.localStorage.setItem(key, value))

  def loadLangCode(): Option[String]   = getString(KeyLang)
  def saveLangCode(code: String): Unit = setString(KeyLang, code)

  def loadKind(): Option[String]   = getString(KeyKind)
  def saveKind(value: String): Unit = setString(KeyKind, value)

  def loadFavourites(): Set[String] =
    getString(KeyFavourites)
      .map(_.split(',').iterator.map(_.trim).filter(_.nonEmpty).toSet)
      .getOrElse(Set.empty)
  def saveFavourites(ids: Set[String]): Unit =
    setString(KeyFavourites, ids.toList.sorted.mkString(","))

  def loadFavouritesOnly(): Boolean         = getString(KeyFavouritesOnly).contains("true")
  def saveFavouritesOnly(value: Boolean): Unit = setString(KeyFavouritesOnly, value.toString)

  def loadClockFormat(): Option[String]    = getString(KeyClockFormat)
  def saveClockFormat(value: String): Unit = setString(KeyClockFormat, value)
