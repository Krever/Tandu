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

  val es: Vector[String] = Vector(
    "Animales del bosque",
    "Animales del zoo",
    "Mascotas",
    "Aves",
    "Insectos",
    "Peces",
    "Frutas",
    "Verduras",
    "Dulces",
    "Cosas en la cocina",
    "Cosas en el baño",
    "Cosas en una mochila",
    "Cosas en el coche",
    "Nombres de chica",
    "Nombres de chico",
    "Países de Europa",
    "Ciudades",
    "Marcas de coches",
    "Personajes de dibujos",
    "Deportes",
    "Instrumentos musicales",
    "Árboles",
    "Profesiones",
    "Juguetes",
    "Cosas en la escuela",
    "Bebidas",
    "Platos de comida",
    "Personajes de películas"
  )

  val fr: Vector[String] = Vector(
    "Animaux de la forêt",
    "Animaux du zoo",
    "Animaux domestiques",
    "Oiseaux",
    "Insectes",
    "Poissons",
    "Fruits",
    "Légumes",
    "Bonbons",
    "Choses dans la cuisine",
    "Choses dans la salle de bain",
    "Choses dans un sac à dos",
    "Choses dans la voiture",
    "Prénoms de filles",
    "Prénoms de garçons",
    "Pays d'Europe",
    "Villes",
    "Marques de voitures",
    "Personnages de dessins animés",
    "Sports",
    "Instruments de musique",
    "Arbres",
    "Métiers",
    "Jouets",
    "Choses à l'école",
    "Boissons",
    "Plats du dîner",
    "Personnages de films"
  )

  val de: Vector[String] = Vector(
    "Tiere im Wald",
    "Tiere im Zoo",
    "Haustiere",
    "Vögel",
    "Insekten",
    "Fische",
    "Obst",
    "Gemüse",
    "Süßigkeiten",
    "Dinge in der Küche",
    "Dinge im Badezimmer",
    "Dinge im Rucksack",
    "Dinge im Auto",
    "Mädchennamen",
    "Jungennamen",
    "Länder in Europa",
    "Städte",
    "Automarken",
    "Zeichentrickfiguren",
    "Sportarten",
    "Musikinstrumente",
    "Bäume",
    "Berufe",
    "Spielzeug",
    "Dinge in der Schule",
    "Getränke",
    "Abendessen",
    "Filmfiguren"
  )

  // Letters that yield reasonable word counts for kid players. No
  // diacritics (words rarely start with them in these languages).
  val plLetters: Vector[Char] = "ABCDEFGHIJKLMNOPRSTUWZ".toVector
  val enLetters: Vector[Char] = "ABCDEFGHIJKLMNOPRSTUVW".toVector
  val esLetters: Vector[Char] = "ABCDEFGHIJLMNOPRSTUVZ".toVector
  val frLetters: Vector[Char] = "ABCDEFGHIJLMNOPRSTUV".toVector
  val deLetters: Vector[Char] = "ABDEFGHIJKLMNOPRSTUVWZ".toVector

  def categoriesFor(lang: Lang): Vector[String] = lang match
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
