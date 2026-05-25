package tandu.activities

import tandu.i18n.Lang

/** Seed words for verbal/prompt activities. Concrete, kid-friendly,
  * mixed across domains (nature, objects, food, feelings, places).
  *
  * Add freely. Lists do not need to match across languages — a word
  * that doesn't translate naturally just lives on one side.
  */
object WordBank:

  val pl: Vector[String] = Vector(
    "kot", "pies", "dom", "samochód", "słońce", "księżyc", "gwiazda",
    "książka", "deszcz", "lody", "plaża", "las", "ryba", "sen",
    "podróż", "urodziny", "drzewo", "koń", "pociąg", "kawa", "śnieg",
    "ogród", "telefon", "piłka", "morze", "góra", "miasto", "balon",
    "róża", "motyl", "koło", "świeczka", "klucz", "mleko", "chleb",
    "papier", "muzyka", "taniec", "zegar", "słowo", "list", "śmiech",
    "lód", "krzesło", "łyżka", "talerz", "parasol", "lustro", "okno",
    "droga", "most", "lampa", "koc", "świat", "ptak", "smok", "rycerz",
    "zamek", "skarb", "kapelusz", "rakieta", "robot", "dinozaur",
    "tęcza", "burza", "wiatr", "ognisko", "namiot", "pizza", "czekolada",
    "jabłko", "banan", "marchewka", "zupa", "ser", "miód", "cukierek",
    "rower", "łódź", "samolot", "balonik", "kometa", "planeta",
    "skała", "rzeka", "jezioro", "wyspa", "wulkan", "piasek", "muszla"
  )

  val en: Vector[String] = Vector(
    "cat", "dog", "house", "car", "sun", "moon", "star",
    "book", "rain", "ice cream", "beach", "forest", "fish", "dream",
    "journey", "birthday", "tree", "horse", "train", "coffee", "snow",
    "garden", "phone", "ball", "sea", "mountain", "city", "balloon",
    "rose", "butterfly", "wheel", "candle", "key", "milk", "bread",
    "paper", "music", "dance", "clock", "word", "letter", "laughter",
    "ice", "chair", "spoon", "plate", "umbrella", "mirror", "window",
    "road", "bridge", "lamp", "blanket", "world", "bird", "dragon",
    "knight", "castle", "treasure", "hat", "rocket", "robot", "dinosaur",
    "rainbow", "storm", "wind", "campfire", "tent", "pizza", "chocolate",
    "apple", "banana", "carrot", "soup", "cheese", "honey", "candy",
    "bike", "boat", "plane", "kite", "comet", "planet",
    "rock", "river", "lake", "island", "volcano", "sand", "shell"
  )

  def forLang(lang: Lang): Vector[String] = lang match
    case Lang.Pl => pl
    case Lang.En => en
