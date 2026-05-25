package tandu.activities

import tandu.i18n.Strings

enum Player:
  case P1, P2
  def other: Player = this match { case P1 => P2; case P2 => P1 }
  def num: Int = this match { case P1 => 1; case P2 => 2 }
  def labelKey(str: Strings): String = this match
    case P1 => str.common.player1
    case P2 => str.common.player2
