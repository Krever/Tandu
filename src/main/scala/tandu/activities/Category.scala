package tandu.activities

import tandu.i18n.Strings

enum Category(val id: String, val label: Strings => String):
  case Tabletop extends Category("tabletop", _.catTabletop)
  case Car      extends Category("car",      _.catCar)
