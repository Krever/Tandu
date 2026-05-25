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

  val es: Vector[String] = Vector(
    "gato", "perro", "casa", "coche", "sol", "luna", "estrella",
    "libro", "lluvia", "helado", "playa", "bosque", "pez", "sueño",
    "viaje", "cumpleaños", "árbol", "caballo", "tren", "café", "nieve",
    "jardín", "teléfono", "pelota", "mar", "montaña", "ciudad", "globo",
    "rosa", "mariposa", "rueda", "vela", "llave", "leche", "pan",
    "papel", "música", "baile", "reloj", "palabra", "carta", "risa",
    "hielo", "silla", "cuchara", "plato", "paraguas", "espejo", "ventana",
    "camino", "puente", "lámpara", "manta", "mundo", "pájaro", "dragón",
    "caballero", "castillo", "tesoro", "sombrero", "cohete", "robot", "dinosaurio",
    "arcoíris", "tormenta", "viento", "fogata", "tienda", "pizza", "chocolate",
    "manzana", "plátano", "zanahoria", "sopa", "queso", "miel", "caramelo",
    "bici", "barco", "avión", "cometa", "planeta",
    "roca", "río", "lago", "isla", "volcán", "arena", "concha"
  )

  val fr: Vector[String] = Vector(
    "chat", "chien", "maison", "voiture", "soleil", "lune", "étoile",
    "livre", "pluie", "glace", "plage", "forêt", "poisson", "rêve",
    "voyage", "anniversaire", "arbre", "cheval", "train", "café", "neige",
    "jardin", "téléphone", "ballon", "mer", "montagne", "ville", "ballon",
    "rose", "papillon", "roue", "bougie", "clé", "lait", "pain",
    "papier", "musique", "danse", "horloge", "mot", "lettre", "rire",
    "glaçon", "chaise", "cuillère", "assiette", "parapluie", "miroir", "fenêtre",
    "route", "pont", "lampe", "couverture", "monde", "oiseau", "dragon",
    "chevalier", "château", "trésor", "chapeau", "fusée", "robot", "dinosaure",
    "arc-en-ciel", "orage", "vent", "feu de camp", "tente", "pizza", "chocolat",
    "pomme", "banane", "carotte", "soupe", "fromage", "miel", "bonbon",
    "vélo", "bateau", "avion", "cerf-volant", "comète", "planète",
    "rocher", "rivière", "lac", "île", "volcan", "sable", "coquillage"
  )

  val de: Vector[String] = Vector(
    "Katze", "Hund", "Haus", "Auto", "Sonne", "Mond", "Stern",
    "Buch", "Regen", "Eis", "Strand", "Wald", "Fisch", "Traum",
    "Reise", "Geburtstag", "Baum", "Pferd", "Zug", "Kaffee", "Schnee",
    "Garten", "Telefon", "Ball", "Meer", "Berg", "Stadt", "Luftballon",
    "Rose", "Schmetterling", "Rad", "Kerze", "Schlüssel", "Milch", "Brot",
    "Papier", "Musik", "Tanz", "Uhr", "Wort", "Brief", "Lachen",
    "Eis", "Stuhl", "Löffel", "Teller", "Regenschirm", "Spiegel", "Fenster",
    "Straße", "Brücke", "Lampe", "Decke", "Welt", "Vogel", "Drache",
    "Ritter", "Schloss", "Schatz", "Hut", "Rakete", "Roboter", "Dinosaurier",
    "Regenbogen", "Sturm", "Wind", "Lagerfeuer", "Zelt", "Pizza", "Schokolade",
    "Apfel", "Banane", "Karotte", "Suppe", "Käse", "Honig", "Bonbon",
    "Fahrrad", "Boot", "Flugzeug", "Drachen", "Komet", "Planet",
    "Felsen", "Fluss", "See", "Insel", "Vulkan", "Sand", "Muschel"
  )

  def forLang(lang: Lang): Vector[String] = lang match
    case Lang.Pl => pl
    case Lang.En => en
    case Lang.Es => es
    case Lang.Fr => fr
    case Lang.De => de
