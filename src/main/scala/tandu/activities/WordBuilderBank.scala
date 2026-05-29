package tandu.activities

import tandu.i18n.Lang

/** Picture + word pairs for Word Builder. Each entry pairs a concrete,
  * kid-recognizable noun with a single emoji. Words are stored lowercased;
  * the activity decides whether to display them upper- or lowercase.
  *
  * Lengths are spread across 3-9 letters so the three difficulty bands all
  * have plenty of choices. */
object WordBuilderBank:

  final case class Entry(word: String, emoji: String):
    def length: Int = word.length

  val pl: Vector[Entry] = Vector(
    // 3 letters
    Entry("kot", "🐱"),
    Entry("dom", "🏠"),
    Entry("lew", "🦁"),
    Entry("ser", "🧀"),
    Entry("oko", "👁️"),
    Entry("nos", "👃"),
    Entry("las", "🌲"),
    Entry("rak", "🦞"),
    Entry("sok", "🧃"),
    Entry("wąż", "🐍"),
    // 4 letters
    Entry("pies", "🐶"),
    Entry("lody", "🍦"),
    Entry("ryba", "🐟"),
    Entry("kura", "🐔"),
    Entry("koza", "🐐"),
    Entry("woda", "💧"),
    Entry("góra", "⛰️"),
    Entry("buty", "👢"),
    Entry("ucho", "👂"),
    Entry("smok", "🐉"),
    Entry("ptak", "🐦"),
    Entry("kawa", "☕"),
    // 5 letters
    Entry("chleb", "🍞"),
    Entry("klucz", "🔑"),
    Entry("mleko", "🥛"),
    Entry("motyl", "🦋"),
    Entry("balon", "🎈"),
    Entry("lampa", "💡"),
    Entry("zegar", "⏰"),
    Entry("serce", "❤️"),
    Entry("jajko", "🥚"),
    Entry("tęcza", "🌈"),
    Entry("morze", "🌊"),
    Entry("banan", "🍌"),
    Entry("pizza", "🍕"),
    // 6 letters
    Entry("talerz", "🍽️"),
    Entry("ciasto", "🎂"),
    Entry("kometa", "☄️"),
    Entry("miasto", "🏙️"),
    Entry("jabłko", "🍎"),
    Entry("żyrafa", "🦒"),
    Entry("delfin", "🐬"),
    Entry("ananas", "🍍"),
    Entry("bałwan", "⛄"),
    Entry("wulkan", "🌋"),
    // 7+ letters
    Entry("gwiazda", "⭐"),
    Entry("książka", "📕"),
    Entry("telefon", "📞"),
    Entry("parasol", "☂️"),
    Entry("rakieta", "🚀"),
    Entry("samochód", "🚗"),
    Entry("kapelusz", "🎩"),
    Entry("dinozaur", "🦕"),
    Entry("choinka", "🎄"),
    Entry("kanapka", "🥪"),
    Entry("pomidor", "🍅"),
    Entry("ognisko", "🔥"),
    Entry("truskawka", "🍓"),
    Entry("krokodyl", "🐊"),
    Entry("cytryna", "🍋")
  )

  val en: Vector[Entry] = Vector(
    // 3 letters
    Entry("cat", "🐱"),
    Entry("dog", "🐶"),
    Entry("sun", "☀️"),
    Entry("bee", "🐝"),
    Entry("cow", "🐄"),
    Entry("owl", "🦉"),
    Entry("pig", "🐷"),
    Entry("fox", "🦊"),
    Entry("ant", "🐜"),
    Entry("bus", "🚌"),
    Entry("car", "🚗"),
    Entry("cup", "☕"),
    Entry("egg", "🥚"),
    // 4 letters
    Entry("book", "📕"),
    Entry("bear", "🐻"),
    Entry("duck", "🦆"),
    Entry("fish", "🐟"),
    Entry("frog", "🐸"),
    Entry("lion", "🦁"),
    Entry("moon", "🌙"),
    Entry("star", "⭐"),
    Entry("tree", "🌳"),
    Entry("ball", "⚽"),
    Entry("bike", "🚲"),
    Entry("milk", "🥛"),
    Entry("cake", "🎂"),
    // 5 letters
    Entry("apple", "🍎"),
    Entry("house", "🏠"),
    Entry("horse", "🐴"),
    Entry("mouse", "🐭"),
    Entry("snake", "🐍"),
    Entry("train", "🚂"),
    Entry("plane", "✈️"),
    Entry("robot", "🤖"),
    Entry("snail", "🐌"),
    Entry("tiger", "🐯"),
    Entry("bread", "🍞"),
    Entry("lemon", "🍋"),
    Entry("pizza", "🍕"),
    // 6 letters
    Entry("dragon", "🐉"),
    Entry("rocket", "🚀"),
    Entry("monkey", "🐵"),
    Entry("banana", "🍌"),
    Entry("planet", "🪐"),
    Entry("guitar", "🎸"),
    Entry("flower", "🌸"),
    Entry("basket", "🧺"),
    // 7+ letters
    Entry("balloon", "🎈"),
    Entry("dolphin", "🐬"),
    Entry("giraffe", "🦒"),
    Entry("rainbow", "🌈"),
    Entry("octopus", "🐙"),
    Entry("penguin", "🐧"),
    Entry("dinosaur", "🦕"),
    Entry("elephant", "🐘"),
    Entry("mountain", "⛰️"),
    Entry("umbrella", "☂️"),
    Entry("sandwich", "🥪"),
    Entry("crocodile", "🐊"),
    Entry("pineapple", "🍍"),
    Entry("butterfly", "🦋"),
    Entry("volcano", "🌋")
  )

  def entriesFor(lang: Lang): Vector[Entry] = lang match
    case Lang.Pl => pl
    case _       => en

  /** Entries whose word length falls in `[minLen, maxLen]`. Falls back to
    * the full bank if nothing matches the range. */
  def entriesInRange(lang: Lang, minLen: Int, maxLen: Int): Vector[Entry] =
    val filtered = entriesFor(lang).filter(e => e.length >= minLen && e.length <= maxLen)
    if filtered.isEmpty then entriesFor(lang) else filtered
