package tandu.activities

import tandu.i18n.Strings

enum Players(val id: String, val label: Strings => String):
  case Solo  extends Players("solo",  _.filters.solo)
  case Two   extends Players("two",   _.filters.two)
  case Group extends Players("group", _.filters.group)
