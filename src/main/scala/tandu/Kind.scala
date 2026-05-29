package tandu

import tandu.i18n.Strings

enum Kind:
  case All, Games, Learn

  def label(s: Strings): String = this match
    case Kind.All   => s.filters.all
    case Kind.Games => s.filters.games
    case Kind.Learn => s.filters.learn

object Kind:
  def fromString(s: String): Option[Kind] = values.find(_.toString == s)
