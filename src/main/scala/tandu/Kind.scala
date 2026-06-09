package tandu

import tandu.i18n.Strings

/** The nature of an activity — how you engage with it, not its genre. Each
  * activity has exactly one. The four are mutually-exclusive *modes*: sit and
  * play (Games), sit and talk with hands/eyes free (OnTheGo), get up and move
  * (Move), or learn something (Learn). Declared in home-grid display order. */
enum Kind:
  case Games, OnTheGo, Move, Learn

  def label(s: Strings): String = this match
    case Kind.Games   => s.filters.games
    case Kind.OnTheGo => s.filters.onTheGo
    case Kind.Move    => s.filters.move
    case Kind.Learn   => s.filters.learn

object Kind:
  def fromString(s: String): Option[Kind] = values.find(_.toString == s)
