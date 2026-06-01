package tandu.activities

import tandu.i18n.{Lang, Strings}

/**
 * The list aims to recoomend actually valuable books.
 * Every item on the list should either be a classic verified by a few generations or a very very important piece of modern \
 * culture. Preferably both.
 * We dont wan't to show titles that are merely popular - Avengaers are popular but it doesn't make them valuable.
 * We also want to show titles that are recognizeable in the given country. Each country has its own history and culture and it applies to children book as well.
 * */
object ReadingBank:

  enum AgeBand(val label: Strings => String):
    case Tots    extends AgeBand(_.reading.bandTots)
    case Picture extends AgeBand(_.reading.bandPicture)
    case Chapter extends AgeBand(_.reading.bandChapter)
    case Older   extends AgeBand(_.reading.bandOlder)

  final case class Entry(
      title: String,
      author: String,
      band: AgeBand,
      freeUrl: Option[String] = None
  )
  private def free(url: String): Option[String] = Some(url)

  def entriesFor(lang: Lang): Vector[Entry] = lang match
    case Lang.En => english
    case Lang.Pl => polish
    case Lang.Es => spanish
    case Lang.Fr => french
    case Lang.De => german

  // ---------- English (UK-leaning, with US classics that crossed over) ----------

  val english: Vector[Entry] = Vector(
    Entry("The Very Hungry Caterpillar", "Eric Carle", AgeBand.Tots),
    Entry("Goodnight Moon", "Margaret Wise Brown", AgeBand.Tots),
    Entry("Dear Zoo", "Rod Campbell", AgeBand.Tots),
    Entry("Where's Spot?", "Eric Hill", AgeBand.Tots),
    Entry("Guess How Much I Love You", "Sam McBratney", AgeBand.Tots),
    Entry("Each Peach Pear Plum", "Janet and Allan Ahlberg", AgeBand.Tots),

    Entry("Where the Wild Things Are", "Maurice Sendak", AgeBand.Picture),
    Entry("The Tiger Who Came to Tea", "Judith Kerr", AgeBand.Picture),
    Entry("The Tale of Peter Rabbit", "Beatrix Potter", AgeBand.Picture,
      free("https://www.gutenberg.org/ebooks/14838")),
    Entry("We're Going on a Bear Hunt", "Michael Rosen", AgeBand.Picture),
    Entry("The Snowman", "Raymond Briggs", AgeBand.Picture),
    Entry("The Gruffalo", "Julia Donaldson", AgeBand.Picture),

    Entry("Winnie-the-Pooh", "A. A. Milne", AgeBand.Chapter,
      free("https://standardebooks.org/ebooks/a-a-milne/winnie-the-pooh")),
    Entry("Charlotte's Web", "E. B. White", AgeBand.Chapter),
    Entry("Matilda", "Roald Dahl", AgeBand.Chapter),
    Entry("Charlie and the Chocolate Factory", "Roald Dahl", AgeBand.Chapter),
    Entry("Fantastic Mr Fox", "Roald Dahl", AgeBand.Chapter),
    Entry("The Lion, the Witch and the Wardrobe", "C. S. Lewis", AgeBand.Chapter),
    Entry("Alice's Adventures in Wonderland", "Lewis Carroll", AgeBand.Chapter,
      free("https://standardebooks.org/ebooks/lewis-carroll/alices-adventures-in-wonderland/john-tenniel")),
    Entry("Harry Potter and the Philosopher's Stone", "J. K. Rowling", AgeBand.Chapter),
    Entry("The Little Prince", "Antoine de Saint-Exupéry", AgeBand.Chapter),

    Entry("The Hobbit", "J. R. R. Tolkien", AgeBand.Older),
    Entry("The Wind in the Willows", "Kenneth Grahame", AgeBand.Older,
      free("https://standardebooks.org/ebooks/kenneth-grahame/the-wind-in-the-willows")),
    Entry("The Secret Garden", "Frances Hodgson Burnett", AgeBand.Older,
      free("https://standardebooks.org/ebooks/frances-hodgson-burnett/the-secret-garden")),
    Entry("Treasure Island", "Robert Louis Stevenson", AgeBand.Older,
      free("https://standardebooks.org/ebooks/robert-louis-stevenson/treasure-island")),
    Entry("Anne of Green Gables", "L. M. Montgomery", AgeBand.Older,
      free("https://standardebooks.org/ebooks/l-m-montgomery/anne-of-green-gables")),
    Entry("The Neverending Story", "Michael Ende", AgeBand.Older)
  )

  // ---------- Polish ----------

  val polish: Vector[Entry] = Vector(
    Entry("Lokomotywa", "Julian Tuwim", AgeBand.Picture,
      free("https://wolnelektury.pl/katalog/lektura/tuwim-lokomotywa-i-inne-wiersze-dla-dzieci/")),
    Entry("Brzechwa dzieciom", "Jan Brzechwa", AgeBand.Picture),
    Entry("Tuwim dzieciom", "Julian Tuwim", AgeBand.Picture),
    Entry("Plastusiowy pamiętnik", "Maria Kownacka", AgeBand.Picture),
    Entry("Tam, gdzie żyją dzikie stwory", "Maurice Sendak", AgeBand.Picture),

    Entry("Kubuś Puchatek", "A. A. Milne", AgeBand.Chapter),
    Entry("Dzieci z Bullerbyn", "Astrid Lindgren", AgeBand.Chapter),
    Entry("Pippi Pończoszanka", "Astrid Lindgren", AgeBand.Chapter),
    Entry("Mikołajek", "René Goscinny & Jean-Jacques Sempé", AgeBand.Chapter),
    Entry("Akademia Pana Kleksa", "Jan Brzechwa", AgeBand.Chapter),
    Entry("Mały Książę", "Antoine de Saint-Exupéry", AgeBand.Chapter),
    Entry("Alicja w Krainie Czarów", "Lewis Carroll", AgeBand.Chapter,
      free("https://wolnelektury.pl/katalog/lektura/carroll-alicja-w-krainie-czarow/")),
    Entry("Charlie i fabryka czekolady", "Roald Dahl", AgeBand.Chapter),
    Entry("Matylda", "Roald Dahl", AgeBand.Chapter),
    Entry("Harry Potter i Kamień Filozoficzny", "J. K. Rowling", AgeBand.Chapter),

    Entry("Hobbit, czyli tam i z powrotem", "J. R. R. Tolkien", AgeBand.Older),
    Entry("Lew, czarownica i stara szafa", "C. S. Lewis", AgeBand.Older),
    Entry("W pustyni i w puszczy", "Henryk Sienkiewicz", AgeBand.Older,
      free("https://wolnelektury.pl/katalog/lektura/w-pustyni-i-w-puszczy/")),
    Entry("Król Maciuś Pierwszy", "Janusz Korczak", AgeBand.Older,
      free("https://wolnelektury.pl/katalog/lektura/krol-macius-pierwszy/")),
    Entry("W Dolinie Muminków", "Tove Jansson", AgeBand.Older),
    Entry("Ania z Zielonego Wzgórza", "L. M. Montgomery", AgeBand.Older),
    Entry("Tajemniczy ogród", "Frances Hodgson Burnett", AgeBand.Older,
      free("https://wolnelektury.pl/katalog/lektura/tajemniczy-ogrod/")),
    Entry("Wyspa Skarbów", "Robert Louis Stevenson", AgeBand.Older,
      free("https://wolnelektury.pl/katalog/lektura/wyspa-skarbow/")),
    Entry("Niekończąca się historia", "Michael Ende", AgeBand.Older),
    Entry("Dwadzieścia tysięcy mil podmorskiej żeglugi", "Jules Verne", AgeBand.Older,
      free("https://wolnelektury.pl/katalog/lektura/20-000-mil-podmorskiej-zeglugi/"))
  )

  // ---------- Spanish ----------

  val spanish: Vector[Entry] = Vector(
    Entry("La oruga muy hambrienta", "Eric Carle", AgeBand.Tots),
    Entry("Adivina cuánto te quiero", "Sam McBratney", AgeBand.Tots),
    Entry("¿A qué sabe la luna?", "Michael Grejniec", AgeBand.Tots),
    Entry("Buenas noches, luna", "Margaret Wise Brown", AgeBand.Tots),

    Entry("Donde viven los monstruos", "Maurice Sendak", AgeBand.Picture),
    Entry("El monstruo de colores", "Anna Llenas", AgeBand.Picture),
    Entry("Frederick", "Leo Lionni", AgeBand.Picture),
    Entry("Elmer", "David McKee", AgeBand.Picture),
    Entry("El Grúfalo", "Julia Donaldson", AgeBand.Picture),
    Entry("El topo que quería saber quién se había hecho aquello en su cabeza", "Werner Holzwarth", AgeBand.Picture),

    Entry("Manolito Gafotas", "Elvira Lindo", AgeBand.Chapter),
    Entry("Matilda", "Roald Dahl", AgeBand.Chapter),
    Entry("Charlie y la fábrica de chocolate", "Roald Dahl", AgeBand.Chapter),
    Entry("Las brujas", "Roald Dahl", AgeBand.Chapter),
    Entry("Pippi Calzaslargas", "Astrid Lindgren", AgeBand.Chapter),
    Entry("El Principito", "Antoine de Saint-Exupéry", AgeBand.Chapter),
    Entry("Alicia en el País de las Maravillas", "Lewis Carroll", AgeBand.Chapter,
      free("https://www.gutenberg.org/ebooks/11288")),
    Entry("Harry Potter y la piedra filosofal", "J. K. Rowling", AgeBand.Chapter),
    Entry("Cuentos por teléfono", "Gianni Rodari", AgeBand.Chapter),
    Entry("La telaraña de Carlota", "E. B. White", AgeBand.Chapter),

    Entry("El Hobbit", "J. R. R. Tolkien", AgeBand.Older),
    Entry("El león, la bruja y el armario", "C. S. Lewis", AgeBand.Older),
    Entry("Momo", "Michael Ende", AgeBand.Older),
    Entry("La historia interminable", "Michael Ende", AgeBand.Older),
    Entry("Veinte mil leguas de viaje submarino", "Jules Verne", AgeBand.Older,
      free("https://www.gutenberg.org/ebooks/15090")),
    Entry("La isla del tesoro", "Robert Louis Stevenson", AgeBand.Older),
    Entry("El jardín secreto", "Frances Hodgson Burnett", AgeBand.Older),
    Entry("Las aventuras de Tom Sawyer", "Mark Twain", AgeBand.Older),
    Entry("Platero y yo", "Juan Ramón Jiménez", AgeBand.Older),
    Entry("Ana de las Tejas Verdes", "L. M. Montgomery", AgeBand.Older)
  )

  // ---------- French ----------

  val french: Vector[Entry] = Vector(
    Entry("La chenille qui fait des trous", "Eric Carle", AgeBand.Tots),
    Entry("Devine combien je t'aime", "Sam McBratney", AgeBand.Tots),
    Entry("Petit-Bleu et Petit-Jaune", "Leo Lionni", AgeBand.Tots),
    Entry("Bonsoir Lune", "Margaret Wise Brown", AgeBand.Tots),

    Entry("Max et les Maximonstres", "Maurice Sendak", AgeBand.Picture),
    Entry("L'histoire de Babar le petit éléphant", "Jean de Brunhoff", AgeBand.Picture),
    Entry("Les Trois Brigands", "Tomi Ungerer", AgeBand.Picture),
    Entry("Roule galette", "Natha Caputo & Pierre Belvès", AgeBand.Picture),
    Entry("Le Gruffalo", "Julia Donaldson", AgeBand.Picture),

    Entry("Winnie l'ourson", "A. A. Milne", AgeBand.Chapter),
    Entry("Le Petit Nicolas", "René Goscinny & Jean-Jacques Sempé", AgeBand.Chapter),
    Entry("Fifi Brindacier", "Astrid Lindgren", AgeBand.Chapter),
    Entry("Charlie et la chocolaterie", "Roald Dahl", AgeBand.Chapter),
    Entry("Matilda", "Roald Dahl", AgeBand.Chapter),
    Entry("La toile de Charlotte", "E. B. White", AgeBand.Chapter),
    Entry("Le club des cinq", "Enid Blyton", AgeBand.Chapter),
    Entry("Harry Potter à l'école des sorciers", "J. K. Rowling", AgeBand.Chapter),
    Entry("Alice au pays des merveilles", "Lewis Carroll", AgeBand.Chapter,
      free("https://www.gutenberg.org/ebooks/55456")),

    Entry("Le Petit Prince", "Antoine de Saint-Exupéry", AgeBand.Older),
    Entry("Le Hobbit", "J. R. R. Tolkien", AgeBand.Older),
    Entry("Le Lion, la Sorcière blanche et l'Armoire magique", "C. S. Lewis", AgeBand.Older),
    Entry("Vingt mille lieues sous les mers", "Jules Verne", AgeBand.Older,
      free("https://www.gutenberg.org/ebooks/5097")),
    Entry("Le Tour du monde en quatre-vingts jours", "Jules Verne", AgeBand.Older,
      free("https://www.gutenberg.org/ebooks/800")),
    Entry("L'Île au trésor", "Robert Louis Stevenson", AgeBand.Older,
      free("https://www.gutenberg.org/ebooks/12356")),
    Entry("Sans Famille", "Hector Malot", AgeBand.Older,
      free("https://www.gutenberg.org/ebooks/17791")),
    Entry("La Guerre des boutons", "Louis Pergaud", AgeBand.Older),
    Entry("L'Histoire sans fin", "Michael Ende", AgeBand.Older),
    Entry("Le Jardin secret", "Frances Hodgson Burnett", AgeBand.Older),
    Entry("Astérix le Gaulois", "René Goscinny & Albert Uderzo", AgeBand.Older)
  )

  // ---------- German ----------

  val german: Vector[Entry] = Vector(
    Entry("Die kleine Raupe Nimmersatt", "Eric Carle", AgeBand.Tots),
    Entry("Weißt du eigentlich, wie lieb ich dich hab?", "Sam McBratney", AgeBand.Tots),
    Entry("Der Maulwurf, der wissen wollte, wer ihm auf den Kopf gemacht hat", "Werner Holzwarth", AgeBand.Tots),
    Entry("Bobo Siebenschläfer", "Markus Osterwalder", AgeBand.Tots),

    Entry("Wo die wilden Kerle wohnen", "Maurice Sendak", AgeBand.Picture),
    Entry("Der Grüffelo", "Julia Donaldson", AgeBand.Picture),
    Entry("Der Regenbogenfisch", "Marcus Pfister", AgeBand.Picture),
    Entry("Die Häschenschule", "Albert Sixtus & Fritz Koch-Gotha", AgeBand.Picture),
    Entry("Oh, wie schön ist Panama", "Janosch", AgeBand.Picture),
    Entry("Frederick", "Leo Lionni", AgeBand.Picture),

    Entry("Pu der Bär", "A. A. Milne", AgeBand.Chapter),
    Entry("Pippi Langstrumpf", "Astrid Lindgren", AgeBand.Chapter),
    Entry("Emil und die Detektive", "Erich Kästner", AgeBand.Chapter),
    Entry("Das doppelte Lottchen", "Erich Kästner", AgeBand.Chapter),
    Entry("Die kleine Hexe", "Otfried Preußler", AgeBand.Chapter),
    Entry("Der Räuber Hotzenplotz", "Otfried Preußler", AgeBand.Chapter),
    Entry("Charlie und die Schokoladenfabrik", "Roald Dahl", AgeBand.Chapter),
    Entry("Matilda", "Roald Dahl", AgeBand.Chapter),
    Entry("Harry Potter und der Stein der Weisen", "J. K. Rowling", AgeBand.Chapter),
    Entry("Schweinchen Wilbur und seine Freunde", "E. B. White", AgeBand.Chapter),

    Entry("Der Hobbit", "J. R. R. Tolkien", AgeBand.Older),
    Entry("Der König von Narnia", "C. S. Lewis", AgeBand.Older),
    Entry("Die unendliche Geschichte", "Michael Ende", AgeBand.Older),
    Entry("Momo", "Michael Ende", AgeBand.Older),
    Entry("Heidi", "Johanna Spyri", AgeBand.Older,
      free("https://www.gutenberg.org/ebooks/47591")),
    Entry("Krabat", "Otfried Preußler", AgeBand.Older),
    Entry("Tintenherz", "Cornelia Funke", AgeBand.Older),
    Entry("Anne auf Green Gables", "L. M. Montgomery", AgeBand.Older),
    Entry("Der geheime Garten", "Frances Hodgson Burnett", AgeBand.Older),
    Entry("Die Schatzinsel", "Robert Louis Stevenson", AgeBand.Older)
  )
