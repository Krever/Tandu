package tandu

import com.raquo.laminar.api.L.*
import tandu.i18n.{Lang, Strings}

object AppState:
  val lang: Var[Lang] =
    Var(Storage.loadLangCode().flatMap(Lang.fromCode).getOrElse(Lang.detect()))

  val strings: Signal[Strings] = lang.signal.map(Strings.of)

  // Persist language whenever it changes, and reflect it on the root <html>
  // element so crawlers and assistive tech see the active document language.
  lang.signal.foreach { l =>
    Storage.saveLangCode(l.code)
    org.scalajs.dom.document.documentElement.setAttribute("lang", l.code)
  }(unsafeWindowOwner)
