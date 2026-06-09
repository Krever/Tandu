package tandu

import com.raquo.laminar.api.L.*
import tandu.activities.{ClockFormat, Players}
import tandu.i18n.{Lang, Strings}

object AppState:
  val lang: Var[Lang] =
    Var(Storage.loadLangCode().flatMap(Lang.fromCode).getOrElse(Lang.detect()))

  // Which party sizes are shown. Additive multi-select, mirroring `kinds`: all
  // selected is the neutral default (any size); an activity passes if it fits
  // any selected size. Ephemeral — resets to "all" on each fresh visit home.
  val players: Var[Set[Players]] = Var(Players.values.toSet)

  // Which activity kinds are shown. Additive multi-select: all selected is the
  // neutral default (show everything); deselecting a kind hides its activities.
  // Unknown/empty persisted values fall back to "all" so the grid is never blank
  // on load.
  val kinds: Var[Set[Kind]] =
    val restored = Storage.loadKinds().map(_.flatMap(Kind.fromString)).getOrElse(Set.empty)
    Var(if restored.isEmpty then Kind.values.toSet else restored)

  val favourites: Var[Set[String]] = Var(Storage.loadFavourites())
  val favouritesOnly: Var[Boolean] = Var(Storage.loadFavouritesOnly())

  // Activities the user has hidden from the grid (a "reverse favourite"). They
  // stay excluded from the catalog and from suggestions until restored.
  val hidden: Var[Set[String]] = Var(Storage.loadHidden())

  // Digital-clock notation, shared across the app and persisted. 12-hour is the
  // usual starting point for kids learning to tell the time.
  val clockFormat: Var[ClockFormat] =
    Var(Storage.loadClockFormat().flatMap(ClockFormat.fromString).getOrElse(ClockFormat.H12))

  def toggleFavourite(id: String): Unit =
    favourites.update(s => if s.contains(id) then s - id else s + id)

  def toggleHidden(id: String): Unit =
    hidden.update(s => if s.contains(id) then s - id else s + id)

  val strings: Signal[Strings] = lang.signal.map(Strings.of)

  // Persist language whenever it changes, and reflect it on the root <html>
  // element so crawlers and assistive tech see the active document language.
  val _ = lang.signal.foreach { l =>
    Storage.saveLangCode(l.code)
    org.scalajs.dom.document.documentElement.setAttribute("lang", l.code)
  }(using unsafeWindowOwner)

  val _ = kinds.signal.foreach(ks => Storage.saveKinds(ks.map(_.toString)))(using unsafeWindowOwner)

  val _ = favourites.signal.foreach(Storage.saveFavourites)(using unsafeWindowOwner)
  val _ = favouritesOnly.signal.foreach(Storage.saveFavouritesOnly)(using unsafeWindowOwner)
  val _ = hidden.signal.foreach(Storage.saveHidden)(using unsafeWindowOwner)

  val _ = clockFormat.signal.foreach(f => Storage.saveClockFormat(f.toString))(using unsafeWindowOwner)
