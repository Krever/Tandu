package tandu.activities

import tandu.i18n.Lang

/** Prompts for the Categories game. Each round picks one category and
  * one starting letter — so prompts only include categories that make
  * sense under a letter constraint (e.g. "Forest animals"). Categories
  * that don't work with a letter (colors, "round things") are not in
  * the bank by design.
  */
object CategoryBank:

  val pl: Vector[String] = Vector(
    "Zwierzęta w lesie",
    "Zwierzęta w zoo",
    "Zwierzęta domowe",
    "Ptaki",
    "Owady",
    "Ryby",
    "Owoce",
    "Warzywa",
    "Słodycze",
    "Rzeczy w kuchni",
    "Rzeczy w łazience",
    "Rzeczy w plecaku",
    "Rzeczy w aucie",
    "Imiona dziewczynek",
    "Imiona chłopców",
    "Państwa w Europie",
    "Miasta w Polsce",
    "Marki samochodów",
    "Bohaterowie z bajek",
    "Sporty",
    "Instrumenty muzyczne",
    "Drzewa",
    "Zawody",
    "Zabawki",
    "Rzeczy w szkole",
    "Napoje",
    "Dania obiadowe",
    "Postacie z filmów"
  )

  val en: Vector[String] = Vector(
    "Forest animals",
    "Zoo animals",
    "Pets",
    "Birds",
    "Insects",
    "Fish",
    "Fruits",
    "Vegetables",
    "Sweets",
    "Things in the kitchen",
    "Things in the bathroom",
    "Things in a backpack",
    "Things in a car",
    "Girls' names",
    "Boys' names",
    "European countries",
    "Capital cities",
    "Car brands",
    "Cartoon characters",
    "Sports",
    "Musical instruments",
    "Trees",
    "Jobs",
    "Toys",
    "Things at school",
    "Drinks",
    "Dinner dishes",
    "Movie characters"
  )

  // Letters that yield reasonable word counts for kid players. No
  // Polish diacritics (words almost never start with them), no Q/V/X/Y
  // in Polish, no Q/X/Y/Z in English.
  val plLetters: Vector[Char] = "ABCDEFGHIJKLMNOPRSTUWZ".toVector
  val enLetters: Vector[Char] = "ABCDEFGHIJKLMNOPRSTUVW".toVector

  def categoriesFor(lang: Lang): Vector[String] = lang match
    case Lang.Pl => pl
    case Lang.En => en

  def lettersFor(lang: Lang): Vector[Char] = lang match
    case Lang.Pl => plLetters
    case Lang.En => enLetters
