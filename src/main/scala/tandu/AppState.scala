package tandu

import com.raquo.laminar.api.L.*
import tandu.activities.Players
import tandu.i18n.{Lang, Strings}

object AppState:
  val lang: Var[Lang] =
    Var(Storage.loadLangCode().flatMap(Lang.fromCode).getOrElse(Lang.detect()))

  val playersFilter: Var[Option[Players]] = Var(None)
  val handsFreeOnly: Var[Boolean]         = Var(false)
  val kindFilter: Var[Kind] =
    Var(Storage.loadKind().flatMap(Kind.fromString).getOrElse(Kind.All))

  val strings: Signal[Strings] = lang.signal.map(Strings.of)

  // Persist language whenever it changes, and reflect it on the root <html>
  // element so crawlers and assistive tech see the active document language.
  val _ = lang.signal.foreach { l =>
    Storage.saveLangCode(l.code)
    org.scalajs.dom.document.documentElement.setAttribute("lang", l.code)
  }(using unsafeWindowOwner)

  val _ = kindFilter.signal.foreach(k => Storage.saveKind(k.toString))(using unsafeWindowOwner)
