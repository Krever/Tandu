package tandu.i18n

final case class Strings(
    appTitle: String,
    common: Strings.Common,
    home: Strings.Home,
    category: Strings.CategoryLabels,
    about: Strings.About,
    ticTacToe: Strings.TicTacToe,
    battleships: Strings.Battleships,
    wordAssociation: Strings.WordAssociation,
    categoriesGame: Strings.Categories,
    solitaire: Strings.Solitaire,
    memory: Strings.Memory,
    hangman: Strings.Hangman,
    checkers: Strings.Checkers,
    chess: Strings.Chess,
    twentyQuestions: Strings.TwentyQuestions,
    storyBuilding: Strings.StoryBuilding,
    lastLetter: Strings.LastLetter,
    wouldYouRather: Strings.WouldYouRather,
    dice: Strings.Dice
)

object Strings:

  final case class Common(
      back: String,
      playAgain: String,
      youWin: String,
      draw: String,
      player1: String,
      player2: String,
      confirm: String,
      close: String
  )

  final case class Home(
      suggestActivity: String,
      suggestAnother: String,
      tools: String,
      activities: String,
      installApp: String
  )

  final case class CategoryLabels(
      all: String,
      tabletop: String,
      car: String
  )

  final case class About(
      open: String,
      title: String,
      body: String
  )

  final case class TttVariant(name: String, description: String)

  final case class TicTacToe(
      name: String,
      description: String,
      turn: String,
      wins: String,
      chooseVariant: String,
      changeVariant: String,
      classic: TttVariant,
      gomoku: TttVariant
  )

  final case class Battleships(
      name: String,
      description: String,
      yourTurn: String,
      fireAt: String,
      hit: String,
      miss: String,
      sunk: String,
      allSunk: String,
      showMyBoard: String,
      hideMyBoard: String,
      enemyBoard: String,
      print: String,
      endTurn: String
  )

  final case class WordAssociation(
      name: String,
      description: String,
      hint: String,
      nextWord: String
  )

  final case class Categories(
      name: String,
      description: String,
      hint: String,
      next: String
  )

  final case class Solitaire(
      name: String,
      description: String,
      undo: String,
      newGame: String
  )

  final case class MemoryVariant(name: String, description: String)

  final case class Memory(
      name: String,
      description: String,
      turn: String,
      wins: String,
      chooseVariant: String,
      changeVariant: String,
      easy: MemoryVariant,
      medium: MemoryVariant,
      hard: MemoryVariant
  )

  final case class Hangman(
      name: String,
      description: String,
      livesLeft: String,
      newWord: String,
      youWon: String,
      youLost: String
  )

  final case class Checkers(
      name: String,
      description: String,
      turn: String
  )

  final case class Chess(
      name: String,
      description: String,
      turn: String,
      check: String
  )

  final case class TwentyQuestions(
      name: String,
      description: String,
      hint: String,
      hidden: String,
      reveal: String,
      hide: String,
      questionsLeft: String,
      askedOne: String,
      undo: String,
      outOfQuestions: String,
      newWord: String
  )

  final case class StoryBuilding(
      name: String,
      description: String,
      hint: String,
      howTitle: String,
      step1: String,
      step2: String,
      step3: String,
      variantsTitle: String,
      variantWord: String,
      variantSentence: String
  )

  final case class LastLetter(
      name: String,
      description: String,
      hint: String,
      newLetter: String
  )

  final case class WouldYouRather(
      name: String,
      description: String,
      hint: String,
      prefix: String,
      or: String,
      next: String
  )

  final case class Dice(
      name: String,
      description: String,
      roll: String,
      dice: String,
      sides: String,
      lastRolls: String,
      total: String
  )

  val en: Strings = Strings(
    appTitle = "Tandu",
    common = Common(
      back = "Back",
      playAgain = "Play again",
      youWin = "You win!",
      draw = "It's a draw",
      player1 = "Player 1",
      player2 = "Player 2",
      confirm = "OK",
      close = "Close"
    ),
    home = Home(
      suggestActivity = "Suggest activity",
      suggestAnother = "Suggest another",
      tools = "Tools",
      activities = "Activities",
      installApp = "Install app"
    ),
    category = CategoryLabels(
      all = "All",
      tabletop = "At home",
      car = "In the car"
    ),
    about = About(
      open = "About",
      title = "About Tandu",
      body = "Tandu is a little helper for picking something fun to do with the kids. Tap \"Suggest activity\" for a random idea, or browse the list. Some games are playable in the app, others are prompts for things you do offline — perfect for the car, the couch, or a rainy afternoon."
    ),
    ticTacToe = TicTacToe(
      name = "Tic-tac-toe",
      description = "Classic 3-in-a-row or Gomoku 5-in-a-row.",
      turn = "Turn",
      wins = "wins!",
      chooseVariant = "Choose game",
      changeVariant = "Change game",
      classic = TttVariant("Classic", "3×3 board, get 3 in a row. Quick and simple."),
      gomoku = TttVariant("Gomoku", "10×10 board, get 5 in a row. Harder strategy.")
    ),
    battleships = Battleships(
      name = "Battleships",
      description = "Sink the other player's fleet on a 10×10 grid.",
      yourTurn = "Your turn",
      fireAt = "Tap a cell on the enemy board to fire.",
      hit = "Hit!",
      miss = "Miss.",
      sunk = "Ship sunk!",
      allSunk = "Fleet destroyed!",
      showMyBoard = "Show my board",
      hideMyBoard = "Hide my board",
      enemyBoard = "Enemy waters",
      print = "Print boards",
      endTurn = "End turn"
    ),
    wordAssociation = WordAssociation(
      name = "Word association",
      description = "Say the first word that pops into your head.",
      hint = "Take turns. Each player says the first word the previous one brings to mind.",
      nextWord = "New word"
    ),
    categoriesGame = Categories(
      name = "Categories",
      description = "Name things in a category, all starting with a given letter.",
      hint = "Take turns. Whoever stalls or repeats loses the round.",
      next = "New round"
    ),
    solitaire = Solitaire(
      name = "Solitaire",
      description = "The classic one-player card game.",
      undo = "Undo",
      newGame = "New game"
    ),
    memory = Memory(
      name = "Memory",
      description = "Find the matching pairs. Take turns.",
      turn = "Turn",
      wins = "wins!",
      chooseVariant = "Choose size",
      changeVariant = "Change size",
      easy = MemoryVariant("Easy", "6 pairs — quick warm-up."),
      medium = MemoryVariant("Medium", "8 pairs — a real game."),
      hard = MemoryVariant("Hard", "12 pairs — for sharp eyes.")
    ),
    hangman = Hangman(
      name = "Hangman",
      description = "Guess the word, letter by letter.",
      livesLeft = "Lives left:",
      newWord = "New word",
      youWon = "You got it!",
      youLost = "Out of guesses."
    ),
    checkers = Checkers(
      name = "Checkers",
      description = "Capture all the other player's pieces, or block them in.",
      turn = "Turn"
    ),
    chess = Chess(
      name = "Chess",
      description = "The classic strategy game.",
      turn = "Turn",
      check = "Check!"
    ),
    twentyQuestions = TwentyQuestions(
      name = "20 questions",
      description = "One player thinks of a thing. The others have 20 yes/no questions to guess it.",
      hint = "Keeper: tap reveal to peek at the word, then hide it. Tap \"Asked!\" each time a question is asked.",
      hidden = "🤫 hidden",
      reveal = "Reveal",
      hide = "Hide",
      questionsLeft = "Questions left:",
      askedOne = "Asked!",
      undo = "−1",
      outOfQuestions = "Out of questions! Last guess wins or loses.",
      newWord = "New word"
    ),
    storyBuilding = StoryBuilding(
      name = "Story building",
      description = "Make up a story together, one piece at a time.",
      hint = "No app help — just rules. Take turns and see where the story goes.",
      howTitle = "How to play",
      step1 = "Someone starts with one piece — a word or a sentence.",
      step2 = "Going around, each person adds one more piece that continues the story.",
      step3 = "Try to keep it making sense. Laugh when it doesn't.",
      variantsTitle = "Choose a granularity",
      variantWord = "One word each — quick, chaotic, great for short bursts.",
      variantSentence = "One sentence each — slower, builds a longer story."
    ),
    lastLetter = LastLetter(
      name = "Last letter",
      description = "Each word starts with the last letter of the previous one.",
      hint = "Take turns. Say a word that starts with the last letter of the word before. No repeats.",
      newLetter = "New letter"
    ),
    wouldYouRather = WouldYouRather(
      name = "Would you rather",
      description = "Pick between two silly options — and say why.",
      hint = "Read both. Everyone picks one and explains why. No wrong answers.",
      prefix = "Would you rather...",
      or = "or",
      next = "New dilemma"
    ),
    dice = Dice(
      name = "Virtual dice",
      description = "Roll dice when you've lost the real ones",
      roll = "Roll",
      dice = "Dice",
      sides = "Sides",
      lastRolls = "Last rolls",
      total = "Total"
    )
  )

  val pl: Strings = Strings(
    appTitle = "Tandu",
    common = Common(
      back = "Wstecz",
      playAgain = "Zagraj ponownie",
      youWin = "Wygrywasz!",
      draw = "Remis",
      player1 = "Gracz 1",
      player2 = "Gracz 2",
      confirm = "OK",
      close = "Zamknij"
    ),
    home = Home(
      suggestActivity = "Zaproponuj zabawę",
      suggestAnother = "Inna propozycja",
      tools = "Narzędzia",
      activities = "Zabawy",
      installApp = "Zainstaluj aplikację"
    ),
    category = CategoryLabels(
      all = "Wszystkie",
      tabletop = "W domu",
      car = "W aucie"
    ),
    about = About(
      open = "O aplikacji",
      title = "O Tandu",
      body = "Tandu to mały pomocnik, gdy szukacie pomysłu na zabawę z dziećmi. Stuknij „Zaproponuj zabawę\", żeby wylosować coś na chybił trafił, albo przeglądaj listę. Część gier zagrasz w aplikacji, inne to pomysły do zabawy offline — idealne do auta, na kanapę albo deszczowe popołudnie."
    ),
    ticTacToe = TicTacToe(
      name = "Kółko i krzyżyk",
      description = "Klasyk 3 w rzędzie lub Gomoku 5 w rzędzie.",
      turn = "Tura",
      wins = "wygrywa!",
      chooseVariant = "Wybierz grę",
      changeVariant = "Zmień grę",
      classic = TttVariant("Klasyk", "Plansza 3×3, 3 w rzędzie wygrywa. Szybkie i proste."),
      gomoku = TttVariant("Gomoku", "Plansza 10×10, 5 w rzędzie. Trudniejsza strategia.")
    ),
    battleships = Battleships(
      name = "Statki",
      description = "Zatop flotę przeciwnika na planszy 10×10.",
      yourTurn = "Twoja tura",
      fireAt = "Wybierz pole na planszy przeciwnika, by strzelić.",
      hit = "Trafiony!",
      miss = "Pudło.",
      sunk = "Statek zatopiony!",
      allSunk = "Flota zatopiona!",
      showMyBoard = "Pokaż moją planszę",
      hideMyBoard = "Ukryj moją planszę",
      enemyBoard = "Wody przeciwnika",
      print = "Drukuj plansze",
      endTurn = "Zakończ turę"
    ),
    wordAssociation = WordAssociation(
      name = "Skojarzenia",
      description = "Powiedz pierwsze słowo, które przychodzi ci do głowy.",
      hint = "Grajcie po kolei. Każdy mówi pierwsze słowo, które kojarzy mu się z poprzednim.",
      nextWord = "Nowe słowo"
    ),
    categoriesGame = Categories(
      name = "Kategorie",
      description = "Wymieniajcie rzeczy z kategorii, wszystkie na podaną literę.",
      hint = "Po kolei. Kto się zatnie albo powtórzy — przegrywa rundę.",
      next = "Nowa runda"
    ),
    solitaire = Solitaire(
      name = "Pasjans",
      description = "Klasyczna karciana gra dla jednego.",
      undo = "Cofnij",
      newGame = "Nowa gra"
    ),
    memory = Memory(
      name = "Memory",
      description = "Znajdź pary obrazków. Grajcie na zmianę.",
      turn = "Ruch",
      wins = "wygrywa!",
      chooseVariant = "Wybierz rozmiar",
      changeVariant = "Zmień rozmiar",
      easy = MemoryVariant("Łatwy", "6 par — krótka rozgrzewka."),
      medium = MemoryVariant("Średni", "8 par — prawdziwa gra."),
      hard = MemoryVariant("Trudny", "12 par — dla bystrych oczu.")
    ),
    hangman = Hangman(
      name = "Wisielec",
      description = "Zgadnij słowo, litera po literze.",
      livesLeft = "Życia:",
      newWord = "Nowe słowo",
      youWon = "Udało się!",
      youLost = "Koniec prób."
    ),
    checkers = Checkers(
      name = "Warcaby",
      description = "Zbij wszystkie pionki przeciwnika albo go zablokuj.",
      turn = "Tura"
    ),
    chess = Chess(
      name = "Szachy",
      description = "Klasyczna gra strategiczna.",
      turn = "Tura",
      check = "Szach!"
    ),
    twentyQuestions = TwentyQuestions(
      name = "20 pytań",
      description = "Ktoś wymyśla rzecz. Reszta zgaduje, zadając 20 pytań tak/nie.",
      hint = "Trzymający hasło: stuknij „Pokaż\", podejrzyj słowo i ukryj. Stukaj „Pytanie!\" przy każdym pytaniu.",
      hidden = "🤫 ukryte",
      reveal = "Pokaż",
      hide = "Ukryj",
      questionsLeft = "Pytań zostało:",
      askedOne = "Pytanie!",
      undo = "−1",
      outOfQuestions = "Koniec pytań! Czas zgadywać.",
      newWord = "Nowe słowo"
    ),
    storyBuilding = StoryBuilding(
      name = "Budowanie historii",
      description = "Wymyślcie wspólnie historię, kawałek po kawałku.",
      hint = "Bez pomocy aplikacji — same zasady. Po kolei, zobaczcie, dokąd zaprowadzi was opowieść.",
      howTitle = "Jak grać",
      step1 = "Ktoś zaczyna — jednym słowem albo jednym zdaniem.",
      step2 = "Po kolei każdy dodaje kolejny kawałek, który ciągnie historię dalej.",
      step3 = "Starajcie się, żeby miało sens. Śmiejcie się, gdy nie ma.",
      variantsTitle = "Wybierzcie wariant",
      variantWord = "Po jednym słowie — szybko, chaotycznie, świetne na krótkie zabawy.",
      variantSentence = "Po jednym zdaniu — wolniej, ale powstaje dłuższa opowieść."
    ),
    lastLetter = LastLetter(
      name = "Ostatnia litera",
      description = "Każde słowo zaczyna się od ostatniej litery poprzedniego.",
      hint = "Po kolei. Powiedz słowo zaczynające się na ostatnią literę poprzedniego. Bez powtórek.",
      newLetter = "Nowa litera"
    ),
    wouldYouRather = WouldYouRather(
      name = "Co wolisz",
      description = "Wybierz między dwoma szalonymi opcjami — i powiedz dlaczego.",
      hint = "Przeczytajcie obie. Każdy wybiera jedną i mówi dlaczego. Nie ma złych odpowiedzi.",
      prefix = "Wolisz...",
      or = "czy",
      next = "Nowy dylemat"
    ),
    dice = Dice(
      name = "Wirtualne kości",
      description = "Rzucaj kośćmi, gdy prawdziwe się zgubiły",
      roll = "Rzuć",
      dice = "Kości",
      sides = "Ścianki",
      lastRolls = "Ostatnie rzuty",
      total = "Suma"
    )
  )

  val es: Strings = Strings(
    appTitle = "Tandu",
    common = Common(
      back = "Atrás",
      playAgain = "Jugar otra vez",
      youWin = "¡Ganaste!",
      draw = "Empate",
      player1 = "Jugador 1",
      player2 = "Jugador 2",
      confirm = "OK",
      close = "Cerrar"
    ),
    home = Home(
      suggestActivity = "Sugerir actividad",
      suggestAnother = "Otra sugerencia",
      tools = "Herramientas",
      activities = "Actividades",
      installApp = "Instalar app"
    ),
    category = CategoryLabels(
      all = "Todas",
      tabletop = "En casa",
      car = "En el coche"
    ),
    about = About(
      open = "Acerca de",
      title = "Acerca de Tandu",
      body = "Tandu es un pequeño ayudante para elegir algo divertido que hacer con los niños. Toca \"Sugerir actividad\" para una idea al azar, o explora la lista. Algunos juegos se juegan en la app, otros son ideas para hacer sin pantalla — perfectos para el coche, el sofá o una tarde lluviosa."
    ),
    ticTacToe = TicTacToe(
      name = "Tres en raya",
      description = "El clásico 3 en línea o Gomoku 5 en línea.",
      turn = "Turno",
      wins = "¡gana!",
      chooseVariant = "Elige el juego",
      changeVariant = "Cambiar juego",
      classic = TttVariant("Clásico", "Tablero 3×3, 3 en línea. Rápido y sencillo."),
      gomoku = TttVariant("Gomoku", "Tablero 10×10, 5 en línea. Estrategia más difícil.")
    ),
    battleships = Battleships(
      name = "Hundir la flota",
      description = "Hunde la flota del otro jugador en una cuadrícula 10×10.",
      yourTurn = "Tu turno",
      fireAt = "Toca una casilla del tablero enemigo para disparar.",
      hit = "¡Tocado!",
      miss = "Agua.",
      sunk = "¡Barco hundido!",
      allSunk = "¡Flota destruida!",
      showMyBoard = "Mostrar mi tablero",
      hideMyBoard = "Ocultar mi tablero",
      enemyBoard = "Aguas enemigas",
      print = "Imprimir tableros",
      endTurn = "Fin del turno"
    ),
    wordAssociation = WordAssociation(
      name = "Asociación de palabras",
      description = "Di la primera palabra que se te ocurra.",
      hint = "Por turnos. Cada jugador dice la primera palabra que le sugiere la anterior.",
      nextWord = "Nueva palabra"
    ),
    categoriesGame = Categories(
      name = "Categorías",
      description = "Nombra cosas de una categoría, todas empezando por una letra dada.",
      hint = "Por turnos. Quien dude o repita pierde la ronda.",
      next = "Nueva ronda"
    ),
    solitaire = Solitaire(
      name = "Solitario",
      description = "El clásico juego de cartas para uno.",
      undo = "Deshacer",
      newGame = "Nueva partida"
    ),
    memory = Memory(
      name = "Memoria",
      description = "Encuentra las parejas. Por turnos.",
      turn = "Turno",
      wins = "¡gana!",
      chooseVariant = "Elige el tamaño",
      changeVariant = "Cambiar tamaño",
      easy = MemoryVariant("Fácil", "6 parejas — calentamiento rápido."),
      medium = MemoryVariant("Medio", "8 parejas — partida de verdad."),
      hard = MemoryVariant("Difícil", "12 parejas — para ojos atentos.")
    ),
    hangman = Hangman(
      name = "Ahorcado",
      description = "Adivina la palabra, letra a letra.",
      livesLeft = "Vidas:",
      newWord = "Nueva palabra",
      youWon = "¡Acertaste!",
      youLost = "Sin intentos."
    ),
    checkers = Checkers(
      name = "Damas",
      description = "Captura todas las piezas del otro jugador o bloquéalo.",
      turn = "Turno"
    ),
    chess = Chess(
      name = "Ajedrez",
      description = "El clásico juego de estrategia.",
      turn = "Turno",
      check = "¡Jaque!"
    ),
    twentyQuestions = TwentyQuestions(
      name = "20 preguntas",
      description = "Alguien piensa una cosa. Los demás tienen 20 preguntas de sí/no para adivinar.",
      hint = "El que sabe: toca \"Mostrar\" para ver la palabra y vuelve a ocultarla. Toca \"¡Preguntado!\" en cada pregunta.",
      hidden = "🤫 oculto",
      reveal = "Mostrar",
      hide = "Ocultar",
      questionsLeft = "Preguntas restantes:",
      askedOne = "¡Preguntado!",
      undo = "−1",
      outOfQuestions = "¡Sin preguntas! Toca adivinar.",
      newWord = "Nueva palabra"
    ),
    storyBuilding = StoryBuilding(
      name = "Construir una historia",
      description = "Inventad una historia juntos, trozo a trozo.",
      hint = "Sin ayuda de la app — solo las reglas. Por turnos, a ver adónde llega la historia.",
      howTitle = "Cómo jugar",
      step1 = "Alguien empieza con un trozo — una palabra o una frase.",
      step2 = "Por turnos, cada cual añade otro trozo que continúa la historia.",
      step3 = "Intentad que tenga sentido. Reíd cuando no lo tenga.",
      variantsTitle = "Elegid la unidad",
      variantWord = "Una palabra cada uno — rápido, caótico, ideal para ratos cortos.",
      variantSentence = "Una frase cada uno — más lento, pero crece una historia más larga."
    ),
    lastLetter = LastLetter(
      name = "Última letra",
      description = "Cada palabra empieza con la última letra de la anterior.",
      hint = "Por turnos. Di una palabra que empiece con la última letra de la anterior. Sin repetir.",
      newLetter = "Nueva letra"
    ),
    wouldYouRather = WouldYouRather(
      name = "¿Esto o lo otro?",
      description = "Elige entre dos opciones disparatadas — y di por qué.",
      hint = "Leed las dos. Cada uno elige una y dice por qué. No hay respuesta incorrecta.",
      prefix = "¿Qué prefieres?",
      or = "o",
      next = "Otro dilema"
    ),
    dice = Dice(
      name = "Dados virtuales",
      description = "Tira los dados cuando hayas perdido los de verdad",
      roll = "Tirar",
      dice = "Dados",
      sides = "Caras",
      lastRolls = "Últimas tiradas",
      total = "Total"
    )
  )

  val fr: Strings = Strings(
    appTitle = "Tandu",
    common = Common(
      back = "Retour",
      playAgain = "Rejouer",
      youWin = "Vous gagnez !",
      draw = "Égalité",
      player1 = "Joueur 1",
      player2 = "Joueur 2",
      confirm = "OK",
      close = "Fermer"
    ),
    home = Home(
      suggestActivity = "Proposer une activité",
      suggestAnother = "Une autre idée",
      tools = "Outils",
      activities = "Activités",
      installApp = "Installer l'app"
    ),
    category = CategoryLabels(
      all = "Toutes",
      tabletop = "À la maison",
      car = "En voiture"
    ),
    about = About(
      open = "À propos",
      title = "À propos de Tandu",
      body = "Tandu est un petit assistant pour choisir une activité amusante avec les enfants. Appuyez sur « Proposer une activité » pour une idée au hasard, ou parcourez la liste. Certains jeux se jouent dans l'application, d'autres sont des idées à faire hors-écran — parfaits pour la voiture, le canapé ou un après-midi pluvieux."
    ),
    ticTacToe = TicTacToe(
      name = "Morpion",
      description = "Le classique 3 en ligne ou Gomoku 5 en ligne.",
      turn = "Tour",
      wins = "gagne !",
      chooseVariant = "Choisir le jeu",
      changeVariant = "Changer de jeu",
      classic = TttVariant("Classique", "Plateau 3×3, 3 en ligne. Rapide et simple."),
      gomoku = TttVariant("Gomoku", "Plateau 10×10, 5 en ligne. Stratégie plus difficile.")
    ),
    battleships = Battleships(
      name = "Bataille navale",
      description = "Coulez la flotte de l'adversaire sur une grille 10×10.",
      yourTurn = "Votre tour",
      fireAt = "Touchez une case du plateau adverse pour tirer.",
      hit = "Touché !",
      miss = "Manqué.",
      sunk = "Bateau coulé !",
      allSunk = "Flotte détruite !",
      showMyBoard = "Afficher mon plateau",
      hideMyBoard = "Cacher mon plateau",
      enemyBoard = "Eaux ennemies",
      print = "Imprimer les plateaux",
      endTurn = "Fin du tour"
    ),
    wordAssociation = WordAssociation(
      name = "Association de mots",
      description = "Dites le premier mot qui vous vient à l'esprit.",
      hint = "Chacun son tour. Chaque joueur dit le premier mot que lui inspire le précédent.",
      nextWord = "Nouveau mot"
    ),
    categoriesGame = Categories(
      name = "Catégories",
      description = "Nommez des choses d'une catégorie, toutes commençant par une lettre donnée.",
      hint = "Chacun son tour. Celui qui hésite ou répète perd la manche.",
      next = "Nouvelle manche"
    ),
    solitaire = Solitaire(
      name = "Solitaire",
      description = "Le jeu de cartes classique en solo.",
      undo = "Annuler",
      newGame = "Nouvelle partie"
    ),
    memory = Memory(
      name = "Memory",
      description = "Trouvez les paires identiques. Chacun son tour.",
      turn = "Tour",
      wins = "gagne !",
      chooseVariant = "Choisir la taille",
      changeVariant = "Changer la taille",
      easy = MemoryVariant("Facile", "6 paires — petit échauffement."),
      medium = MemoryVariant("Moyen", "8 paires — vraie partie."),
      hard = MemoryVariant("Difficile", "12 paires — pour les yeux vifs.")
    ),
    hangman = Hangman(
      name = "Le pendu",
      description = "Devinez le mot, lettre par lettre.",
      livesLeft = "Vies :",
      newWord = "Nouveau mot",
      youWon = "Trouvé !",
      youLost = "Plus d'essais."
    ),
    checkers = Checkers(
      name = "Dames",
      description = "Capturez toutes les pièces de l'adversaire ou bloquez-le.",
      turn = "Tour"
    ),
    chess = Chess(
      name = "Échecs",
      description = "Le classique jeu de stratégie.",
      turn = "Tour",
      check = "Échec !"
    ),
    twentyQuestions = TwentyQuestions(
      name = "20 questions",
      description = "Quelqu'un pense à une chose. Les autres ont 20 questions oui/non pour deviner.",
      hint = "Le maître du mot : touche « Révéler » pour jeter un œil, puis cache. Touche « Posée ! » à chaque question.",
      hidden = "🤫 caché",
      reveal = "Révéler",
      hide = "Cacher",
      questionsLeft = "Questions restantes :",
      askedOne = "Posée !",
      undo = "−1",
      outOfQuestions = "Plus de questions ! À vous de deviner.",
      newWord = "Nouveau mot"
    ),
    storyBuilding = StoryBuilding(
      name = "Histoire à plusieurs",
      description = "Inventez une histoire ensemble, morceau par morceau.",
      hint = "Pas d'aide de l'app — juste les règles. Chacun son tour, voyez où l'histoire vous emmène.",
      howTitle = "Comment jouer",
      step1 = "Quelqu'un commence par un morceau — un mot ou une phrase.",
      step2 = "Chacun son tour ajoute un morceau qui continue l'histoire.",
      step3 = "Essayez que ça ait du sens. Riez quand ça en a plus.",
      variantsTitle = "Choisissez l'unité",
      variantWord = "Un mot chacun — rapide, chaotique, parfait pour les courtes parties.",
      variantSentence = "Une phrase chacun — plus lent, mais ça donne une vraie histoire."
    ),
    lastLetter = LastLetter(
      name = "Dernière lettre",
      description = "Chaque mot commence par la dernière lettre du précédent.",
      hint = "Chacun son tour. Dites un mot qui commence par la dernière lettre du mot précédent. Sans répétition.",
      newLetter = "Nouvelle lettre"
    ),
    wouldYouRather = WouldYouRather(
      name = "Ceci ou cela ?",
      description = "Choisis entre deux options farfelues — et dis pourquoi.",
      hint = "Lisez les deux. Chacun en choisit une et dit pourquoi. Pas de mauvaise réponse.",
      prefix = "Tu préfères ?",
      or = "ou",
      next = "Nouveau dilemme"
    ),
    dice = Dice(
      name = "Dés virtuels",
      description = "Lancez des dés quand vous avez perdu les vrais",
      roll = "Lancer",
      dice = "Dés",
      sides = "Faces",
      lastRolls = "Derniers lancers",
      total = "Total"
    )
  )

  val de: Strings = Strings(
    appTitle = "Tandu",
    common = Common(
      back = "Zurück",
      playAgain = "Nochmal spielen",
      youWin = "Du gewinnst!",
      draw = "Unentschieden",
      player1 = "Spieler 1",
      player2 = "Spieler 2",
      confirm = "OK",
      close = "Schließen"
    ),
    home = Home(
      suggestActivity = "Aktivität vorschlagen",
      suggestAnother = "Andere Idee",
      tools = "Werkzeuge",
      activities = "Aktivitäten",
      installApp = "App installieren"
    ),
    category = CategoryLabels(
      all = "Alle",
      tabletop = "Zu Hause",
      car = "Im Auto"
    ),
    about = About(
      open = "Über",
      title = "Über Tandu",
      body = "Tandu ist ein kleiner Helfer, um etwas Lustiges mit den Kindern auszusuchen. Tippe auf „Aktivität vorschlagen\" für eine zufällige Idee oder stöbere in der Liste. Manche Spiele kannst du in der App spielen, andere sind Anregungen für Offline-Spaß — perfekt fürs Auto, das Sofa oder einen verregneten Nachmittag."
    ),
    ticTacToe = TicTacToe(
      name = "Tic-Tac-Toe",
      description = "Der Klassiker 3 in einer Reihe oder Gomoku 5 in einer Reihe.",
      turn = "Zug",
      wins = "gewinnt!",
      chooseVariant = "Spiel wählen",
      changeVariant = "Spiel wechseln",
      classic = TttVariant("Klassisch", "3×3 Brett, 3 in einer Reihe. Schnell und einfach."),
      gomoku = TttVariant("Gomoku", "10×10 Brett, 5 in einer Reihe. Anspruchsvollere Strategie.")
    ),
    battleships = Battleships(
      name = "Schiffe versenken",
      description = "Versenke die Flotte des Gegners auf einem 10×10-Raster.",
      yourTurn = "Du bist dran",
      fireAt = "Tippe ein Feld auf dem gegnerischen Brett an, um zu schießen.",
      hit = "Treffer!",
      miss = "Daneben.",
      sunk = "Schiff versenkt!",
      allSunk = "Flotte versenkt!",
      showMyBoard = "Mein Brett zeigen",
      hideMyBoard = "Mein Brett verbergen",
      enemyBoard = "Gegnerisches Gewässer",
      print = "Bretter drucken",
      endTurn = "Zug beenden"
    ),
    wordAssociation = WordAssociation(
      name = "Wortassoziation",
      description = "Sag das erste Wort, das dir einfällt.",
      hint = "Reihum. Jeder sagt das erste Wort, das ihm zum vorherigen einfällt.",
      nextWord = "Neues Wort"
    ),
    categoriesGame = Categories(
      name = "Kategorien",
      description = "Nenne Dinge aus einer Kategorie, alle mit einem vorgegebenen Anfangsbuchstaben.",
      hint = "Reihum. Wer stockt oder wiederholt, verliert die Runde.",
      next = "Neue Runde"
    ),
    solitaire = Solitaire(
      name = "Solitär",
      description = "Das klassische Kartenspiel für eine Person.",
      undo = "Rückgängig",
      newGame = "Neues Spiel"
    ),
    memory = Memory(
      name = "Memory",
      description = "Finde die passenden Paare. Reihum.",
      turn = "Zug",
      wins = "gewinnt!",
      chooseVariant = "Größe wählen",
      changeVariant = "Größe ändern",
      easy = MemoryVariant("Leicht", "6 Paare — kurze Runde."),
      medium = MemoryVariant("Mittel", "8 Paare — echtes Spiel."),
      hard = MemoryVariant("Schwer", "12 Paare — für scharfe Augen.")
    ),
    hangman = Hangman(
      name = "Galgenmännchen",
      description = "Errate das Wort, Buchstabe für Buchstabe.",
      livesLeft = "Leben:",
      newWord = "Neues Wort",
      youWon = "Geschafft!",
      youLost = "Keine Versuche mehr."
    ),
    checkers = Checkers(
      name = "Dame",
      description = "Schlage alle Steine des Gegners oder blockiere ihn.",
      turn = "Zug"
    ),
    chess = Chess(
      name = "Schach",
      description = "Das klassische Strategiespiel.",
      turn = "Zug",
      check = "Schach!"
    ),
    twentyQuestions = TwentyQuestions(
      name = "20 Fragen",
      description = "Eine Person denkt sich eine Sache aus. Die anderen haben 20 Ja/Nein-Fragen, um sie zu erraten.",
      hint = "Wortgeber: Tippe „Zeigen\", schau das Wort an und versteck es wieder. Tippe „Gefragt!\" bei jeder Frage.",
      hidden = "🤫 versteckt",
      reveal = "Zeigen",
      hide = "Verstecken",
      questionsLeft = "Fragen übrig:",
      askedOne = "Gefragt!",
      undo = "−1",
      outOfQuestions = "Keine Fragen mehr! Jetzt raten.",
      newWord = "Neues Wort"
    ),
    storyBuilding = StoryBuilding(
      name = "Geschichte bauen",
      description = "Erfindet zusammen eine Geschichte, Stück für Stück.",
      hint = "Keine App-Hilfe — nur Regeln. Reihum, und schaut, wohin die Geschichte führt.",
      howTitle = "So geht's",
      step1 = "Jemand beginnt mit einem Stück — einem Wort oder einem Satz.",
      step2 = "Reihum hängt jede:r ein weiteres Stück an, das die Geschichte fortsetzt.",
      step3 = "Versucht, dass es Sinn ergibt. Lacht, wenn nicht.",
      variantsTitle = "Wählt die Einheit",
      variantWord = "Ein Wort pro Person — schnell, chaotisch, ideal für kurze Runden.",
      variantSentence = "Ein Satz pro Person — langsamer, dafür wird daraus eine längere Geschichte."
    ),
    lastLetter = LastLetter(
      name = "Letzter Buchstabe",
      description = "Jedes Wort beginnt mit dem letzten Buchstaben des vorigen.",
      hint = "Reihum. Sag ein Wort, das mit dem letzten Buchstaben des vorigen beginnt. Keine Wiederholungen.",
      newLetter = "Neuer Buchstabe"
    ),
    wouldYouRather = WouldYouRather(
      name = "Lieber so oder so?",
      description = "Wähle zwischen zwei verrückten Möglichkeiten — und sag warum.",
      hint = "Lest beide. Jeder wählt eine aus und sagt warum. Keine falschen Antworten.",
      prefix = "Was wäre dir lieber:",
      or = "oder",
      next = "Neues Dilemma"
    ),
    dice = Dice(
      name = "Virtuelle Würfel",
      description = "Würfle, wenn die echten weg sind",
      roll = "Würfeln",
      dice = "Würfel",
      sides = "Seiten",
      lastRolls = "Letzte Würfe",
      total = "Summe"
    )
  )

  def of(lang: Lang): Strings = lang match
    case Lang.En => en
    case Lang.Pl => pl
    case Lang.Es => es
    case Lang.Fr => fr
    case Lang.De => de
