package tandu.activities

import tandu.i18n.Lang

/** Words for Draw & Guess — concrete, easily drawable nouns a young child
  * knows. The five lists translate the same concepts, with the occasional
  * substitution where a translation would be ambiguous or hard to draw.
  */
object DrawAndGuessBank:

  val en: Vector[String] = Vector(
    "sun", "moon", "star", "cloud", "rainbow", "tree", "flower", "house",
    "car", "train", "plane", "rocket", "boat", "fish", "cat", "dog",
    "bird", "duck", "frog", "snake", "spider", "butterfly", "bee", "snail",
    "turtle", "elephant", "giraffe", "lion", "monkey", "pig", "cow", "horse",
    "sheep", "mouse", "apple", "banana", "ice cream", "cake", "pizza", "carrot",
    "ball", "kite", "book", "clock", "key", "chair", "cup", "hat",
    "shoe", "glasses", "umbrella", "snowman", "ghost", "robot", "crown", "heart",
    "candle", "gift", "drum", "guitar", "bridge", "castle", "mountain", "ladder"
  )

  val pl: Vector[String] = Vector(
    "słońce", "księżyc", "gwiazda", "chmura", "tęcza", "drzewo", "kwiat", "dom",
    "samochód", "pociąg", "samolot", "rakieta", "łódka", "ryba", "kot", "pies",
    "ptak", "kaczka", "żaba", "wąż", "pająk", "motyl", "pszczoła", "ślimak",
    "żółw", "słoń", "żyrafa", "lew", "małpa", "świnia", "krowa", "koń",
    "owca", "mysz", "jabłko", "banan", "lody", "tort", "pizza", "marchewka",
    "piłka", "latawiec", "książka", "zegar", "klucz", "krzesło", "kubek", "kapelusz",
    "but", "okulary", "parasol", "bałwan", "duch", "robot", "korona", "serce",
    "świeczka", "prezent", "bęben", "gitara", "most", "zamek", "góra", "drabina"
  )

  val es: Vector[String] = Vector(
    "sol", "luna", "estrella", "nube", "arcoíris", "árbol", "flor", "casa",
    "coche", "tren", "avión", "cohete", "barco", "pez", "gato", "perro",
    "pájaro", "pato", "rana", "serpiente", "araña", "mariposa", "abeja", "caracol",
    "tortuga", "elefante", "jirafa", "león", "mono", "cerdo", "vaca", "caballo",
    "oveja", "ratón", "manzana", "plátano", "helado", "pastel", "pizza", "zanahoria",
    "pelota", "cometa", "libro", "reloj", "llave", "silla", "taza", "sombrero",
    "zapato", "gafas", "paraguas", "muñeco de nieve", "fantasma", "robot", "corona", "corazón",
    "vela", "regalo", "tambor", "guitarra", "puente", "castillo", "montaña", "escalera"
  )

  val fr: Vector[String] = Vector(
    "soleil", "lune", "étoile", "nuage", "arc-en-ciel", "arbre", "fleur", "maison",
    "voiture", "train", "avion", "fusée", "bateau", "poisson", "chat", "chien",
    "oiseau", "canard", "grenouille", "serpent", "araignée", "papillon", "abeille", "escargot",
    "tortue", "éléphant", "girafe", "lion", "singe", "cochon", "vache", "cheval",
    "mouton", "souris", "pomme", "banane", "glace", "gâteau", "pizza", "carotte",
    "ballon", "cerf-volant", "livre", "horloge", "clé", "chaise", "tasse", "chapeau",
    "chaussure", "lunettes", "parapluie", "bonhomme de neige", "fantôme", "robot", "couronne", "cœur",
    "bougie", "cadeau", "tambour", "guitare", "pont", "château", "montagne", "échelle"
  )

  val de: Vector[String] = Vector(
    "Sonne", "Mond", "Stern", "Wolke", "Regenbogen", "Baum", "Blume", "Haus",
    "Auto", "Zug", "Flugzeug", "Rakete", "Boot", "Fisch", "Katze", "Hund",
    "Vogel", "Ente", "Frosch", "Schlange", "Spinne", "Schmetterling", "Biene", "Schnecke",
    "Schildkröte", "Elefant", "Giraffe", "Löwe", "Affe", "Schwein", "Kuh", "Pferd",
    "Schaf", "Maus", "Apfel", "Banane", "Eis", "Kuchen", "Pizza", "Karotte",
    "Ball", "Drachen", "Buch", "Uhr", "Schlüssel", "Stuhl", "Tasse", "Hut",
    "Schuh", "Brille", "Regenschirm", "Schneemann", "Gespenst", "Roboter", "Krone", "Herz",
    "Kerze", "Geschenk", "Trommel", "Gitarre", "Brücke", "Burg", "Berg", "Leiter"
  )

  def wordsFor(lang: Lang): Vector[String] = lang match
    case Lang.Pl => pl
    case Lang.En => en
    case Lang.Es => es
    case Lang.Fr => fr
    case Lang.De => de
