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
  private val KeyWorkbookBooks  = "tandu.workbook.books"

  def getString(key: String): Option[String] =
    Try(Option(dom.window.localStorage.getItem(key))).toOption.flatten

  /** Returns whether the write landed. localStorage throws when the quota is
    * hit or in private-mode/blocked-storage browsers; callers that care
    * (workbooks) surface the failure rather than lose edits silently. */
  def setString(key: String, value: String): Boolean =
    Try(dom.window.localStorage.setItem(key, value)).isSuccess

  /** Fire-and-forget write, for secondary prefs where a silent failure is
    * acceptable (and would be unsurfaceable anyway). */
  private def setStringQuiet(key: String, value: String): Unit =
    val _ = setString(key, value)

  def loadLangCode(): Option[String]   = getString(KeyLang)
  def saveLangCode(code: String): Unit = setStringQuiet(KeyLang, code)

  /** Selected activity kinds, by enum name. None when never set, so callers can
    * fall back to "all kinds selected" (the neutral default). */
  def loadKinds(): Option[Set[String]] =
    getString(KeyKinds)
      .map(_.split(',').iterator.map(_.trim).filter(_.nonEmpty).toSet)
  def saveKinds(names: Set[String]): Unit =
    setStringQuiet(KeyKinds, names.toList.sorted.mkString(","))

  def loadFavourites(): Set[String] =
    getString(KeyFavourites)
      .map(_.split(',').iterator.map(_.trim).filter(_.nonEmpty).toSet)
      .getOrElse(Set.empty)
  def saveFavourites(ids: Set[String]): Unit =
    setStringQuiet(KeyFavourites, ids.toList.sorted.mkString(","))

  def loadFavouritesOnly(): Boolean            = getString(KeyFavouritesOnly).contains("true")
  def saveFavouritesOnly(value: Boolean): Unit = setStringQuiet(KeyFavouritesOnly, value.toString)

  def loadHidden(): Set[String] =
    getString(KeyHidden)
      .map(_.split(',').iterator.map(_.trim).filter(_.nonEmpty).toSet)
      .getOrElse(Set.empty)
  def saveHidden(ids: Set[String]): Unit =
    setStringQuiet(KeyHidden, ids.toList.sorted.mkString(","))

  def loadClockFormat(): Option[String]    = getString(KeyClockFormat)
  def saveClockFormat(value: String): Unit = setStringQuiet(KeyClockFormat, value)

  // The saved workbooks, as one JSON blob; the codec lives with the model
  // (workbook.Workbook.Codec), storage just moves an opaque string.
  def loadWorkbookBooks(): Option[String]      = getString(KeyWorkbookBooks)
  def saveWorkbookBooks(json: String): Boolean = setString(KeyWorkbookBooks, json)
