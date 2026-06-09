package tandu

import org.scalajs.dom
import scala.util.Try

object Storage:
  private val KeyLang           = "tandu.lang"
  private val KeyKinds          = "tandu.kinds"
  private val KeyFavourites     = "tandu.favourites"
  private val KeyFavouritesOnly = "tandu.favouritesOnly"
  private val KeyHidden         = "tandu.hidden"
  private val KeyClockFormat    = "tandu.clockFormat"

  def getString(key: String): Option[String] =
    Try(Option(dom.window.localStorage.getItem(key))).toOption.flatten

  def setString(key: String, value: String): Unit =
    val _ = Try(dom.window.localStorage.setItem(key, value))

  def loadLangCode(): Option[String]   = getString(KeyLang)
  def saveLangCode(code: String): Unit = setString(KeyLang, code)

  /** Selected activity kinds, by enum name. None when never set, so callers can
    * fall back to "all kinds selected" (the neutral default). */
  def loadKinds(): Option[Set[String]] =
    getString(KeyKinds)
      .map(_.split(',').iterator.map(_.trim).filter(_.nonEmpty).toSet)
  def saveKinds(names: Set[String]): Unit =
    setString(KeyKinds, names.toList.sorted.mkString(","))

  def loadFavourites(): Set[String] =
    getString(KeyFavourites)
      .map(_.split(',').iterator.map(_.trim).filter(_.nonEmpty).toSet)
      .getOrElse(Set.empty)
  def saveFavourites(ids: Set[String]): Unit =
    setString(KeyFavourites, ids.toList.sorted.mkString(","))

  def loadFavouritesOnly(): Boolean         = getString(KeyFavouritesOnly).contains("true")
  def saveFavouritesOnly(value: Boolean): Unit = setString(KeyFavouritesOnly, value.toString)

  def loadHidden(): Set[String] =
    getString(KeyHidden)
      .map(_.split(',').iterator.map(_.trim).filter(_.nonEmpty).toSet)
      .getOrElse(Set.empty)
  def saveHidden(ids: Set[String]): Unit =
    setString(KeyHidden, ids.toList.sorted.mkString(","))

  def loadClockFormat(): Option[String]    = getString(KeyClockFormat)
  def saveClockFormat(value: String): Unit = setString(KeyClockFormat, value)
