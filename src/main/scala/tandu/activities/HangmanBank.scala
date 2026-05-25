package tandu.activities

import tandu.i18n.Lang

/** Words for Hangman. Kid-friendly, concrete nouns; no proper names,
  * no obscure vocabulary. 4–9 letters works best — long enough to be
  * a challenge, short enough to fit on a phone with a virtual keyboard.
  *
  * The alphabet used for the on-screen keyboard is also defined per
  * language so it includes diacritics that actually appear in the bank.
  */
object HangmanBank:

  val pl: Vector[String] = Vector(
    "kot", "pies", "dom", "samochód", "słońce", "księżyc",
    "gwiazda", "książka", "deszcz", "lody", "plaża", "ryba",
    "podróż", "drzewo", "koń", "pociąg", "śnieg", "ogród",
    "telefon", "piłka", "morze", "góra", "miasto", "balon",
    "motyl", "świeczka", "klucz", "mleko", "chleb", "muzyka",
    "taniec", "zegar", "krzesło", "łyżka", "talerz", "parasol",
    "lustro", "okno", "lampa", "ptak", "smok", "rycerz",
    "zamek", "skarb", "kapelusz", "rakieta", "robot", "dinozaur",
    "tęcza", "burza", "wiatr", "ognisko", "namiot", "pizza",
    "czekolada", "jabłko", "banan", "marchewka", "zupa", "miód",
    "rower", "łódź", "samolot", "kometa", "planeta", "wulkan"
  )

  val en: Vector[String] = Vector(
    "cat", "dog", "house", "car", "sun", "moon", "star",
    "book", "rain", "beach", "forest", "fish", "dream",
    "journey", "tree", "horse", "train", "snow", "garden",
    "phone", "ball", "sea", "mountain", "city", "balloon",
    "butterfly", "candle", "milk", "bread", "music", "clock",
    "chair", "spoon", "plate", "umbrella", "mirror", "window",
    "bridge", "lamp", "blanket", "bird", "dragon", "knight",
    "castle", "treasure", "rocket", "robot", "dinosaur",
    "rainbow", "storm", "wind", "tent", "pizza", "chocolate",
    "apple", "banana", "carrot", "honey", "candy",
    "bike", "boat", "plane", "comet", "planet", "volcano"
  )

  val es: Vector[String] = Vector(
    "gato", "perro", "casa", "coche", "sol", "luna", "estrella",
    "libro", "lluvia", "playa", "bosque", "sueño", "viaje",
    "árbol", "caballo", "tren", "nieve", "jardín", "teléfono",
    "pelota", "montaña", "ciudad", "globo", "mariposa", "vela",
    "leche", "música", "reloj", "silla", "cuchara", "plato",
    "paraguas", "espejo", "ventana", "puente", "lámpara", "manta",
    "pájaro", "dragón", "castillo", "tesoro", "cohete", "robot",
    "dinosaurio", "tormenta", "viento", "tienda", "pizza",
    "chocolate", "manzana", "plátano", "zanahoria", "caramelo",
    "barco", "avión", "cometa", "planeta", "volcán"
  )

  val fr: Vector[String] = Vector(
    "chat", "chien", "maison", "voiture", "soleil", "lune",
    "étoile", "livre", "pluie", "plage", "forêt", "poisson",
    "rêve", "voyage", "arbre", "cheval", "train", "neige",
    "jardin", "téléphone", "ballon", "mer", "montagne", "ville",
    "papillon", "bougie", "lait", "pain", "musique", "horloge",
    "chaise", "assiette", "miroir", "fenêtre", "pont", "lampe",
    "oiseau", "dragon", "château", "trésor", "fusée", "robot",
    "dinosaure", "orage", "vent", "tente", "pizza", "chocolat",
    "pomme", "banane", "carotte", "bonbon", "bateau", "avion",
    "comète", "planète", "volcan"
  )

  val de: Vector[String] = Vector(
    "Katze", "Hund", "Haus", "Auto", "Sonne", "Mond", "Stern",
    "Buch", "Regen", "Strand", "Wald", "Fisch", "Traum",
    "Reise", "Baum", "Pferd", "Zug", "Schnee", "Garten",
    "Telefon", "Ball", "Meer", "Berg", "Stadt", "Ballon",
    "Schmetterling", "Kerze", "Milch", "Brot", "Musik", "Uhr",
    "Stuhl", "Löffel", "Teller", "Schirm", "Spiegel", "Fenster",
    "Brücke", "Lampe", "Decke", "Vogel", "Drache", "Ritter",
    "Schloss", "Schatz", "Rakete", "Roboter", "Dinosaurier",
    "Sturm", "Wind", "Zelt", "Pizza", "Schokolade",
    "Apfel", "Banane", "Karotte", "Bonbon",
    "Boot", "Flugzeug", "Komet", "Planet", "Vulkan"
  )

  // Alphabet shown on the on-screen keyboard, per language. Includes
  // diacritics that actually appear in the bank so kids can guess them
  // directly.
  val plLetters: Vector[Char] = "AĄBCĆDEĘFGHIJKLŁMNŃOÓPRSŚTUWYZŹŻ".toVector
  val enLetters: Vector[Char] = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toVector
  val esLetters: Vector[Char] = "ABCDEFGHIJLMNÑOPQRSTUVZÁÉÍÓÚ".toVector
  val frLetters: Vector[Char] = "ABCDEFGHIJLMNOPRSTUVZÀÂÉÈÊÎÔÙÛÇ".toVector
  val deLetters: Vector[Char] = "ABCDEFGHIJKLMNOPRSTUVWZÄÖÜß".toVector

  def wordsFor(lang: Lang): Vector[String] = lang match
    case Lang.Pl => pl
    case Lang.En => en
    case Lang.Es => es
    case Lang.Fr => fr
    case Lang.De => de

  def lettersFor(lang: Lang): Vector[Char] = lang match
    case Lang.Pl => plLetters
    case Lang.En => enLetters
    case Lang.Es => esLetters
    case Lang.Fr => frLetters
    case Lang.De => deLetters
