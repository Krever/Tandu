package tandu

import com.raquo.laminar.api.L.*
import tandu.i18n.{Lang, Strings}

object AppState:
  val lang: Var[Lang] =
    Var(Storage.loadLangCode().flatMap(Lang.fromCode).getOrElse(Lang.detect()))

  val strings: Signal[Strings] = lang.signal.map(Strings.of)

  // Persist language whenever it changes.
  lang.signal.foreach(l => Storage.saveLangCode(l.code))(unsafeWindowOwner)
