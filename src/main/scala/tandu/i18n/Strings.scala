package tandu.i18n

final case class Strings(
    appTitle: String,
    tagline: String,
    common: Strings.Common,
    home: Strings.Home,
    filters: Strings.Filters,
    about: Strings.About,
    installHelp: Strings.InstallHelp,
    menu: Strings.Menu,
    mode: Strings.Mode,
    printable: Strings.Printable,
    timer: Strings.Timer,
    offline: Strings.Offline,
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
    dice: Strings.Dice,
    sudoku: Strings.Sudoku,
    minesweeper: Strings.Minesweeper,
    wordBuilder: Strings.WordBuilder,
    mathPractice: Strings.MathPractice,
    clock: Strings.Clock,
    reading: Strings.Reading,
    memoryChain: Strings.MemoryChain,
    iSpy: Strings.ISpy,
    maze: Strings.Maze,
    wordSearch: Strings.WordSearch,
    guideRobot: Strings.GuideRobot,
    freezeDance: Strings.FreezeDance,
    hotPotato: Strings.HotPotato,
    activeGames: Strings.ActiveGames
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
      spinning: String,
      tools: String,
      activities: String,
      installApp: String
  )

  final case class Filters(
      games: String,
      move: String,
      learn: String,
      solo: String,
      two: String,
      group: String,
      onTheGo: String,
      favourites: String,
      addToFavourites: String,
      removeFromFavourites: String,
      noFavouritesYet: String,
      searchPlaceholder: String,
      noMatches: String,
      hide: String,
      unhide: String,
      showHidden: String
  )

  final case class About(
      open: String,
      title: String,
      body: String,
      privacy: String
  )

  /** Shown on iOS, where there is no install prompt — the user must add the
    * app to the home screen manually from Safari. */
  final case class InstallHelp(
      title: String,
      body: String
  )

  final case class Menu(
      open: String,
      feedback: String,
      language: String,
      readAloud: String
  )

  final case class Mode(
      choose: String,
      inApp: String,
      offline: String,
      lichess: String,
      external: String,
      p2p: String,
      experimentalBadge: String,
      experimentalWarning: String
  )

  final case class Printable(
      print: String,
      printMaps: String,
      printRules: String
  )

  final case class Timer(
      name: String,
      description: String,
      start: String,
      pause: String,
      restart: String
  )

  final case class Materials(
      paperPen: String,
      printer: String,
      scissors: String,
      laminatorOptional: String,
      deck52: String,
      chessBoard: String,
      checkersBoard: String,
      board: String,
      none: String
  )

  final case class Rules(
      title: String,
      lines: List[String]
  )

  final case class BattleshipsOff(
      printTitle: String,
      ownLabel: String,
      enemyLabel: String,
      fleetTitle: String,
      fleetLine: String,
      rules: Rules,
      modeAsk: String
  )

  final case class MemoryOff(
      printTitle: String,
      cutHint: String,
      rules: Rules
  )

  final case class HangmanOff(
      keeperTitle: String,
      keeperHint: String,
      reveal: String,
      hide: String,
      tap: String,
      categorySetting: String,
      categoryAny: String,
      categoryAnimals: String,
      categoryFoods: String,
      categoryCountries: String,
      drawHint: String,
      rules: Rules,
      gallowsTitle: String
  )

  final case class ChessOff(
      rules: Rules,
      pieces: Rules,
      specials: Rules,
      printTitle: String,
      lichessLabel: String
  )

  final case class TicTacToeOff(
      rules: Rules,
      gomokuTipTitle: String,
      gomokuTip: String
  )

  final case class SolitaireOff(
      rules: Rules,
      setupExample: String
  )

  final case class CategoriesOff(
      printTitle: String,
      categoriesLabel: String,
      lettersLabel: String,
      scoresLabel: String,
      curatedNote: String,
      rules: Rules
  )

  final case class CheckersOff(
      rules: Rules,
      lichessLabel: String
  )

  final case class SudokuOff(
      printTitle: String,
      sheetHint: String,
      rules: Rules
  )

  final case class MazeOff(
      printTitle: String,
      sheetHint: String,
      rules: Rules
  )

  final case class WordSearchOff(
      printTitle: String,
      sheetHint: String,
      wordsLabel: String,
      rules: Rules
  )

  final case class GuideRobotOff(
      printTitle: String,
      sheetHint: String,
      writeLabel: String,
      rules: Rules
  )

  final case class Offline(
      materials: Materials,
      battleships: BattleshipsOff,
      memory: MemoryOff,
      hangman: HangmanOff,
      chess: ChessOff,
      ticTacToe: TicTacToeOff,
      solitaire: SolitaireOff,
      categories: CategoriesOff,
      checkers: CheckersOff,
      sudoku: SudokuOff,
      maze: MazeOff,
      wordSearch: WordSearchOff,
      guideRobot: GuideRobotOff
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

  final case class BattleshipsP2P(
      title: String,
      intro: String,
      create: String,
      join: String,
      connect: String,
      shareCode: String,
      enterCode: String,
      waiting: String,
      yourTurn: String,
      opponentTurn: String,
      waitingShot: String,
      waitingResolve: String,
      youWin: String,
      youLose: String
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
      endTurn: String,
      p2p: BattleshipsP2P
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
      next: String,
      // Spoken-only connector, read as "{category} {startingWith} {letter}"
      // e.g. "Vegetables starting with letter J"
      startingWith: String
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

  final case class MemoryChain(
      name: String,
      description: String,
      hint: String,
      newTheme: String
  )

  final case class ISpy(
      name: String,
      description: String,
      hint: String,
      howTitle: String,
      step1: String,
      step2: String,
      step3: String,
      tipsTitle: String,
      tip1: String,
      tip2: String
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

  final case class SudokuVariant(name: String, description: String)

  final case class Sudoku(
      name: String,
      description: String,
      chooseVariant: String,
      changeVariant: String,
      newGame: String,
      undo: String,
      pencil: String,
      easy: SudokuVariant,
      medium: SudokuVariant,
      hard: SudokuVariant
  )

  final case class WordBuilderLevel(name: String, description: String)

  final case class WordBuilder(
      name: String,
      description: String,
      easy: WordBuilderLevel,
      medium: WordBuilderLevel,
      hard: WordBuilderLevel,
      correct: String,
      nextWord: String,
      skip: String,
      printTitle: String,
      printHint: String
  )

  final case class MathPracticeLevel(name: String, description: String)

  final case class MathPractice(
      name: String,
      description: String,
      easy: MathPracticeLevel,
      medium: MathPracticeLevel,
      hard: MathPracticeLevel,
      howMany: String,
      pickGroup: String,
      correct: String,
      nextProblem: String,
      skip: String,
      printTitle: String,
      printHint: String,
      // Spoken-only words used to read a problem aloud (text-to-speech).
      plus: String,
      minus: String,
      equals: String,
      whatNumber: String,
      // Connector for a comparison, read as "{a} {compare} {b}?"
      // e.g. "8 is greater or less than 6?"
      compare: String
  )

  final case class Clock(
      name: String,
      description: String,
      matchName: String,
      matchDesc: String,
      todName: String,
      todDesc: String,
      formatLabel: String,
      format12: String,
      format24: String,
      whatTime: String,
      pickClock: String,
      partOfDay: String,
      morning: String,
      afternoon: String,
      evening: String,
      night: String,
      correct: String,
      next: String,
      skip: String
  )

  final case class Reading(
      name: String,
      description: String,
      hint: String,
      search: String,
      freeEbook: String,
      bandTots: String,
      bandPicture: String,
      bandChapter: String,
      bandOlder: String
  )

  final case class MinesweeperVariant(name: String, description: String)

  final case class Minesweeper(
      name: String,
      description: String,
      chooseVariant: String,
      changeVariant: String,
      newGame: String,
      playing: String,
      youWon: String,
      youLost: String,
      revealMode: String,
      flagMode: String,
      easy: MinesweeperVariant,
      medium: MinesweeperVariant,
      hard: MinesweeperVariant
  )

  final case class MazeVariant(name: String, description: String)

  final case class Maze(
      name: String,
      description: String,
      instruction: String,
      won: String,
      clearPath: String,
      newGame: String,
      easy: MazeVariant,
      medium: MazeVariant,
      hard: MazeVariant
  )

  final case class WordSearchVariant(name: String, description: String)

  final case class WordSearch(
      name: String,
      description: String,
      instruction: String,
      foundLabel: String,
      won: String,
      newGame: String,
      easy: WordSearchVariant,
      medium: WordSearchVariant,
      hard: WordSearchVariant
  )

  final case class GuideRobotVariant(name: String, description: String)

  final case class FreezeDance(
      name: String,
      description: String,
      instruction: String,
      start: String,
      stop: String,
      danceCue: String,
      freezeCue: String,
      srcSynth: String,
      srcSongs: String,
      synthHint: String,
      songsHint: String,
      pasteLabel: String,
      linkPlaceholder: String,
      linkLoad: String,
      linkInvalid: String,
      ytAdsNote: String,
      uploadLabel: String,
      uploadButton: String,
      freeMusicLabel: String,
      addOwn: String
  )

  final case class HotPotato(
      name: String,
      description: String,
      instruction: String,
      start: String,
      again: String,
      stop: String,
      passCue: String,
      caughtCue: String
  )

  /** One game's rules card inside the Active Games hub. `steps` and `tips` are
    * lists so each game can have however many it needs. */
  final case class ActiveGameRules(
      name: String,
      blurb: String,
      howTitle: String,
      steps: List[String],
      tipsTitle: String,
      tips: List[String]
  )

  final case class ActiveGames(
      name: String,
      description: String,
      lava: ActiveGameRules,
      tag: ActiveGameRules,
      hideSeek: ActiveGameRules,
      redLight: ActiveGameRules
  )

  final case class GuideRobot(
      name: String,
      description: String,
      instruction: String,
      instructionTurns: String,
      programLabel: String,
      emptyProgram: String,
      run: String,
      undo: String,
      clear: String,
      starHint: String,
      won: String,
      crashed: String,
      missed: String,
      missedStar: String,
      tryAgain: String,
      newGame: String,
      easy: GuideRobotVariant,
      medium: GuideRobotVariant,
      hard: GuideRobotVariant
  )

  val en: Strings = Strings(
    freezeDance = FreezeDance(
      name = "Freeze Dance",
      description = "Dance to the music — and freeze when it stops!",
      instruction = "Press Start and dance. When the music stops, freeze! The last one still moving sits out the round.",
      start = "Start the music ▶",
      stop = "Stop",
      danceCue = "Dance! 🕺",
      freezeCue = "Freeze! 🧊",
      srcSynth = "Built-in",
      srcSongs = "Songs",
      synthHint = "A fresh, playful tune every time — no setup, works offline.",
      songsHint = "Tap a song to load it — or add your own below.",
      pasteLabel = "Or paste a link",
      linkPlaceholder = "YouTube or audio link…",
      linkLoad = "Load",
      linkInvalid = "Couldn't read that link. Try a YouTube link or a direct .mp3 link.",
      ytAdsNote = "Heads up: YouTube may show ads, which can interrupt the game.",
      uploadLabel = "Or use a file from this device",
      uploadButton = "Choose a song…",
      freeMusicLabel = "No file? Free music:",
      addOwn = "Use a link or file"
    ),
    hotPotato = HotPotato(
      name = "Hot Potato",
      description = "Pass it fast — don't get caught holding it when the music stops!",
      instruction = "Pass the potato — any small, soft object — around the circle while the music plays. It cuts out at a random moment, and whoever's holding it then is caught. Tap Play again for the next round.",
      start = "Start the music ▶",
      again = "Play again",
      stop = "Stop",
      passCue = "Pass it on! 🥔",
      caughtCue = "Caught! 💥"
    ),
    activeGames = ActiveGames(
      name = "Active Games",
      description = "Classic run-around games to play right now — no equipment, no setup.",
      lava = ActiveGameRules(
        name = "The Floor is Lava",
        blurb = "Whatever you do, stay off the floor.",
        howTitle = "How to play",
        steps = List(
          "Someone shouts \"the floor is lava!\" — from that second, touching the floor is forbidden.",
          "Everyone scrambles to get their feet off the ground: onto a sofa, a cushion, a chair.",
          "Anyone who touches the floor is out. The last one still safe wins the round."
        ),
        tipsTitle = "Make it your own",
        tips = List(
          "Agree first which furniture counts as safe and which is out of bounds.",
          "Scatter cushions as stepping stones and try to cross the whole room without touching down.",
          "Safety first: clear sharp corners and don't climb on anything that can tip over."
        )
      ),
      tag = ActiveGameRules(
        name = "Tag",
        blurb = "One player is \"it\" and chases the rest.",
        howTitle = "How to play",
        steps = List(
          "Pick who's \"it\" — a quick countdown or a chorus of \"not it!\" settles it.",
          "\"It\" chases everyone else and tries to tag a player with a touch.",
          "Whoever gets tagged is the new \"it\". Agree on the boundaries before you start."
        ),
        tipsTitle = "Fun variants",
        tips = List(
          "Freeze tag: a tagged player freezes until a free teammate crawls under their legs.",
          "Chain tag: everyone tagged joins hands with \"it\", and the chain keeps growing.",
          "Shadow tag: instead of touching, \"it\" stomps on your shadow — best in bright sun."
        )
      ),
      hideSeek = ActiveGameRules(
        name = "Hide and Seek",
        blurb = "One seeker counts while everyone hides.",
        howTitle = "How to play",
        steps = List(
          "One player is the seeker. They cover their eyes and count to twenty out loud.",
          "Everyone else scatters and hides while the counting goes on.",
          "\"Ready or not, here I come!\" — the seeker hunts until everyone is found."
        ),
        tipsTitle = "Fun variants",
        tips = List(
          "Sardines: one person hides and everyone seeks — when you find them, squeeze in and hide too.",
          "Agree where hiding is allowed, so nobody waits forever in a spot no one checks.",
          "Last one found becomes the next seeker."
        )
      ),
      redLight = ActiveGameRules(
        name = "Red Light, Green Light",
        blurb = "Creep forward — but freeze on red.",
        howTitle = "How to play",
        steps = List(
          "One player is the traffic light and stands at the far end with their back turned.",
          "On \"green light!\" everyone creeps forward; on \"red light!\" the light spins around.",
          "Anyone caught moving goes back to the start. First to tag the light wins and becomes it."
        ),
        tipsTitle = "Make it your own",
        tips = List(
          "The light can play with the timing — a long green, then a sudden red to catch sneaky feet.",
          "Try it in slow motion, or hopping on one leg, for extra giggles.",
          "Loads of space outdoors is best, but a long hallway works too."
        )
      )
    ),
    appTitle = "Tandu",
    tagline = "Your time together",
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
      suggestActivity = "Suggest!",
      suggestAnother = "Suggest another",
      spinning = "Picking…",
      tools = "Tools",
      activities = "Activities",
      installApp = "Install app"
    ),
    filters = Filters(
      games = "Games",
      move = "Move",
      learn = "Learn",
      solo = "Solo",
      two = "Two",
      group = "3+",
      onTheGo = "On the go",
      favourites = "Favourites",
      addToFavourites = "Add to favourites",
      removeFromFavourites = "Remove from favourites",
      noFavouritesYet = "Tap the star on any activity to save it as a favourite.",
      searchPlaceholder = "Search activities…",
      noMatches = "No activities match your search.",
      hide = "Hide activity",
      unhide = "Show activity",
      showHidden = "Hidden"
    ),
    about = About(
      open = "About",
      title = "About Tandu",
      body = "Tandu is a little helper for picking something fun to do with the kids. Tap \"Suggest activity\" for a random idea, or browse the list. Some games are playable in the app, others are prompts for things you do offline — perfect for the car, the couch, or a rainy afternoon.",
      privacy = "Privacy policy"
    ),
    installHelp = InstallHelp(
      title = "Add Tandu to your Home Screen",
      body = "On iPhone or iPad, open this page in Safari (other browsers can't install it), tap the Share button at the bottom, then choose \"Add to Home Screen\". Tandu will then open like a normal app, full-screen and offline-ready."
    ),
    menu = Menu(
      open = "Menu",
      feedback = "Send feedback",
      language = "Language",
      readAloud = "Read aloud"
    ),
    mode = Mode(
      choose = "How do you want to play?",
      inApp = "Play in app",
      offline = "Play offline",
      lichess = "Play online",
      external = "Open online",
      p2p = "Play on two devices",
      experimentalBadge = "Experimental",
      experimentalWarning = "This mode is experimental and may not connect reliably."
    ),
    printable = Printable(
      print = "Print",
      printMaps = "Print maps",
      printRules = "Print rules"
    ),
    timer = Timer(
      name = "Timer",
      description = "Pick a duration and count down with a beep at the end",
      start = "Start",
      pause = "Pause",
      restart = "Restart"
    ),
    offline = Offline(
      materials = Materials(
        paperPen = "paper + pen",
        printer = "printer",
        scissors = "scissors",
        laminatorOptional = "laminator (optional, for reuse)",
        deck52 = "standard 52-card deck",
        chessBoard = "chess board + pieces",
        checkersBoard = "checkers board + pieces",
        board = "a flat surface",
        none = "nothing — just imagination"
      ),
      battleships = BattleshipsOff(
        printTitle = "Battleships — boards for two players",
        ownLabel = "My fleet",
        enemyLabel = "Enemy waters",
        fleetTitle = "Fleet (Polish variant)",
        fleetLine = "1 × 4-deck · 2 × 3-deck · 3 × 2-deck · 4 × 1-deck",
        rules = Rules("How to play", List(
          "Place your fleet in secret on the \"My fleet\" grid. Ships are placed horizontally or vertically; ships may not touch, not even diagonally.",
          "Take turns calling shots by grid coordinates (letter + number). Mark hits with × and misses with · on the \"Enemy waters\" grid.",
          "After a hit, call \"hit\" — the opponent answers \"sunk\" once the whole ship is down.",
          "First player to sink the entire enemy fleet wins."
        )),
        modeAsk = "How do you want to play Battleships?"
      ),
      memory = MemoryOff(
        printTitle = "Memory — cut out the cards",
        cutHint = "Print the sheet, cut along the lines, shuffle face-down and play.",
        rules = Rules("How to play", List(
          "Shuffle the cards and lay them face-down in a grid.",
          "Take turns. Flip two cards: keep them if they match and go again; otherwise turn them back over.",
          "The player with the most pairs when the table is empty wins."
        ))
      ),
      hangman = HangmanOff(
        keeperTitle = "Word for the keeper",
        keeperHint = "Hold the phone, peek at the word, then hide it. The others guess letters and you draw the gallows on paper.",
        reveal = "Show word",
        hide = "Hide word",
        tap = "Tap to reveal",
        categorySetting = "Category",
        categoryAny = "Any word",
        categoryAnimals = "Animals",
        categoryFoods = "Foods",
        categoryCountries = "Countries",
        drawHint = "Draw one body part of the hanged figure for each wrong letter.",
        rules = Rules("How to play", List(
          "One player thinks of a word (or the phone picks one) and draws blanks on paper — one blank per letter.",
          "The others guess letters one at a time. Correct letters go on the blanks; wrong letters are listed off to the side.",
          "Each wrong letter adds one body part to the gallows.",
          "Win by completing the word before the figure is whole; lose if the figure is finished first."
        )),
        gallowsTitle = "Drawing the gallows"
      ),
      chess = ChessOff(
        rules = Rules("How to play", List(
          "Two players. White moves first; players alternate one move per turn.",
          "Capture the opposing king (checkmate) to win.",
          "A king under attack is \"in check\" — you must get out of check on the next move.",
          "If you have no legal moves and you're not in check, the game is a draw (stalemate)."
        )),
        pieces = Rules("How the pieces move", List(
          "Pawn: forward one square; two from its starting square. Captures diagonally one square.",
          "Knight: L-shape — two then one, jumps over other pieces.",
          "Bishop: any number of squares diagonally.",
          "Rook: any number of squares in a straight line (horizontal or vertical).",
          "Queen: any number of squares in any direction.",
          "King: one square in any direction."
        )),
        specials = Rules("Special moves", List(
          "Castling: king moves two squares toward a rook, the rook hops over to the other side. Allowed if neither piece has moved, no pieces are between them, and the king isn't crossing through check.",
          "En passant: a pawn that has just moved two squares can be captured by an adjacent enemy pawn — but only on the very next move.",
          "Promotion: a pawn that reaches the far rank becomes a queen (or rook/bishop/knight, your choice)."
        )),
        printTitle = "Chess — how the pieces move",
        lichessLabel = "Open Lichess"
      ),
      ticTacToe = TicTacToeOff(
        rules = Rules("How to play", List(
          "Draw a 3×3 grid on paper.",
          "Take turns marking X or O in empty squares.",
          "First to get three in a row — horizontal, vertical, or diagonal — wins."
        )),
        gomokuTipTitle = "Mastered it?",
        gomokuTip = "Try gomoku next: a bigger grid, get five in a row. Same idea, much more interesting."
      ),
      solitaire = SolitaireOff(
        rules = Rules("Klondike — how to play", List(
          "Deal seven tableau columns: 1, 2, 3, 4, 5, 6, 7 cards. Top card of each column face-up.",
          "Remaining cards form the stock — flip one (or three) onto the waste pile.",
          "Build the tableau down in alternating colors. Move sequences as a unit. An empty column accepts a king.",
          "Build the foundations up by suit, Ace to King. Win when all four foundations are complete."
        )),
        setupExample = "Initial setup"
      ),
      categories = CategoriesOff(
        printTitle = "Scattergories sheet",
        categoriesLabel = "Category",
        lettersLabel = "Letter",
        scoresLabel = "Score",
        curatedNote = "Three rounds, one letter per column. Fill in a word for every category that starts with that letter.",
        rules = Rules("How to play", List(
          "Each column has a letter at the top — that's the letter for the round.",
          "Set a timer (about three minutes). Everyone tries to write one word per category that starts with that letter.",
          "When time's up, compare answers: one point per unique answer; if two players wrote the same word, neither scores.",
          "Play all three columns. Highest total wins."
        ))
      ),
      checkers = CheckersOff(
        rules = Rules("How to play", List(
          "Each player has 12 pieces on the dark squares of the back three rows.",
          "Move diagonally forward to an empty dark square. Capture by jumping over an adjacent enemy onto the empty square beyond.",
          "If a capture is available you must take it; multi-jumps continue in one turn.",
          "Reach the back row and your piece is \"crowned\" — a king can move and capture both forward and backward.",
          "Win by capturing all enemy pieces or blocking them so they can't move."
        )),
        lichessLabel = "Open lidraughts"
      ),
      sudoku = SudokuOff(
        printTitle = "Sudoku — six puzzles",
        sheetHint = "Print the sheet, grab a pen, and solve. No solutions included — that's part of the fun.",
        rules = Rules("How to play", List(
          "Fill the grid so every row, every column, and every 3×3 box contains each digit 1–9 exactly once.",
          "The given numbers can't change. Use small pencil marks for candidates when you're unsure.",
          "Work by elimination: if a cell can only hold one digit, that's the answer."
        ))
      ),
      maze = MazeOff(
        printTitle = "Mazes — help the mouse find the cheese",
        sheetHint = "Print the sheet and draw a path from the mouse to the cheese with a pencil. There's one way through each maze.",
        rules = Rules("How to play", List(
          "Start at the mouse 🐭 and find a path to the cheese 🧀.",
          "You can't cross the walls — only follow the open corridors.",
          "Stuck in a dead-end? Back up and try another turn."
        ))
      ),
      wordSearch = WordSearchOff(
        printTitle = "Word Search — find the hidden words",
        sheetHint = "Print the sheet and circle each word you find. The word list sits below the grid.",
        wordsLabel = "Find these words",
        rules = Rules("How to play", List(
          "Every word from the list is hidden in the grid of letters.",
          "Words run in a straight line — across, down, or diagonally, sometimes backwards.",
          "Circle each word as you find it and cross it off the list."
        ))
      ),
      guideRobot = GuideRobotOff(
        printTitle = "Guide the Robot — write the arrow path",
        sheetHint = "Print the sheet and write the arrows that drive each robot to its flag.",
        writeLabel = "Write the arrows",
        rules = Rules("How to play", List(
          "The robot 🤖 starts on the grid and must reach the flag 🏁.",
          "On the easy sheet, write arrows (↑ ↓ ← →) — one square per arrow.",
          "On the harder sheets the robot turns: ↑ forward, ↺ turn left, ↻ turn right.",
          "Walls 🧱 block the way, so steer around them.",
          "On the hard sheets, drive over the star ⭐ before the flag."
        ))
      )
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
      endTurn = "End turn",
      p2p = BattleshipsP2P(
        title = "Two-device play",
        intro = "Connect two phones to play with hidden boards. One device creates a game, the other joins with the same code.",
        create = "Create game",
        join = "Join game",
        connect = "Connect",
        shareCode = "Share this code with the other device",
        enterCode = "Enter the code from the other device",
        waiting = "Waiting for the other device…",
        yourTurn = "Your turn",
        opponentTurn = "Opponent's turn",
        waitingShot = "Waiting for their shot…",
        waitingResolve = "Waiting for the result…",
        youWin = "You win!",
        youLose = "You lose."
      )
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
      next = "New round",
      startingWith = "starting with letter"
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
    ),
    sudoku = Sudoku(
      name = "Sudoku",
      description = "Fill the 9×9 grid so every row, column and box has 1–9.",
      chooseVariant = "Choose difficulty",
      changeVariant = "Change difficulty",
      newGame = "New game",
      undo = "Undo",
      pencil = "Pencil",
      easy = SudokuVariant("Easy", "More clues — a friendly start."),
      medium = SudokuVariant("Medium", "Fewer clues — a real challenge."),
      hard = SudokuVariant("Hard", "Sparse clues — for sharp solvers.")
    ),
    minesweeper = Minesweeper(
      name = "Minesweeper",
      description = "Clear the board without stepping on a mine.",
      chooseVariant = "Choose size",
      changeVariant = "Change size",
      newGame = "New game",
      playing = "Watch your step…",
      youWon = "Cleared!",
      youLost = "Boom!",
      revealMode = "Reveal",
      flagMode = "Flag",
      easy = MinesweeperVariant("Easy", "9×9 with 10 mines."),
      medium = MinesweeperVariant("Medium", "12×12 with 25 mines."),
      hard = MinesweeperVariant("Hard", "16×12 with 40 mines.")
    ),
    maze = Maze(
      name = "Maze",
      description = "Help the mouse find the cheese.",
      instruction = "Swipe or drag to guide the mouse to the cheese — arrow keys work too.",
      won = "You found the cheese! 🧀",
      clearPath = "Clear path",
      newGame = "New maze",
      easy = MazeVariant("Easy", "10×10 with gentle loops."),
      medium = MazeVariant("Medium", "16×16, one way through."),
      hard = MazeVariant("Hard", "24×24, a real labyrinth.")
    ),
    wordSearch = WordSearch(
      name = "Word Search",
      description = "Find the hidden words in the grid.",
      instruction = "Drag across the letters to trace a word — across, down, or diagonally.",
      foundLabel = "Found",
      won = "You found them all! 🔍",
      newGame = "New puzzle",
      easy = WordSearchVariant("Easy", "8×8, 6 words, across and down."),
      medium = WordSearchVariant("Medium", "11×11, 8 words, with diagonals."),
      hard = WordSearchVariant("Hard", "13×13, 10 words, every direction.")
    ),
    guideRobot = GuideRobot(
      name = "Guide the Robot",
      description = "Program the arrows to drive the robot home.",
      instruction = "Tap the arrows to build a path, then press Run to send the robot off.",
      instructionTurns = "Tap forward and the turn buttons to steer the robot, then press Run.",
      programLabel = "Your program",
      emptyProgram = "Tap the arrows below to add steps.",
      run = "Run ▶",
      undo = "Undo",
      clear = "Clear",
      starHint = "Drive over the star ⭐ before reaching the flag 🏁.",
      won = "The robot made it! 🤖",
      crashed = "Bonk! The robot hit a wall. Try again.",
      missed = "The robot didn't reach the flag. Try again.",
      missedStar = "Grab the star ⭐ first! Try again.",
      tryAgain = "Try again",
      newGame = "New puzzle",
      easy = GuideRobotVariant("Easy", "5×5, a clear path to the flag."),
      medium = GuideRobotVariant("Medium", "6×6 with walls to steer around."),
      hard = GuideRobotVariant("Hard", "6×6 — grab the star, then the flag.")
    ),
    wordBuilder = WordBuilder(
      name = "Read & Spell",
      description = "Spell words and pick matching pictures.",
      easy = WordBuilderLevel("Easy", "Short words, no extra letters."),
      medium = WordBuilderLevel("Medium", "Longer words, a few extra letters."),
      hard = WordBuilderLevel("Hard", "Long words with extra letters."),
      correct = "Well done!",
      nextWord = "Next word",
      skip = "Skip",
      printTitle = "Read & Spell — worksheet",
      printHint = "Pick a level, then print a sheet."
    ),
    mathPractice = MathPractice(
      name = "Math Practice",
      description = "Count, compare, add and subtract.",
      easy   = MathPracticeLevel("Easy",   "Count and compare with pictures, up to 10."),
      medium = MathPracticeLevel("Medium", "Add and subtract within 10."),
      hard   = MathPracticeLevel("Hard",   "Add and subtract within 20, with missing numbers."),
      howMany = "How many?",
      pickGroup = "Pick the matching group",
      correct = "Well done!",
      nextProblem = "Next",
      skip = "Skip",
      printTitle = "Math Practice — worksheet",
      printHint = "Pick a level, then print a sheet.",
      plus = "plus",
      minus = "minus",
      equals = "equals",
      whatNumber = "what number",
      compare = "is greater or less than"
    ),
    clock = Clock(
      name = "Clock",
      description = "Tell the time on a clock.",
      matchName = "Read the clock",
      matchDesc = "Match analog and digital, to the quarter hour.",
      todName = "Part of the day",
      todDesc = "Name the part of the day from the time.",
      formatLabel = "Clock",
      format12 = "12-hour",
      format24 = "24-hour",
      whatTime = "What time is it?",
      pickClock = "Pick the matching clock",
      partOfDay = "What part of the day?",
      morning = "Morning",
      afternoon = "Afternoon",
      evening = "Evening",
      night = "Night",
      correct = "Well done!",
      next = "Next",
      skip = "Skip"
    ),
    reading = Reading(
      name = "Reading",
      description = "Read a classic together.",
      hint = "Below you can find some inspirations.",
      search = "Search",
      freeEbook = "E-book",
      bandTots = "Ages 0–3",
      bandPicture = "Ages 3–6",
      bandChapter = "Ages 5–9",
      bandOlder = "Ages 8+"
    ),
    memoryChain = MemoryChain(
      name = "Memory train",
      description = "Repeat the whole list, then add one more.",
      hint = "Take turns. Each player repeats everything said so far, in order, then adds one new thing. Break the chain and you're out.",
      newTheme = "New theme"
    ),
    iSpy = ISpy(
      name = "I Spy",
      description = "Spot something nearby and give one clue.",
      hint = "A looking game — best with things you can all see right now.",
      howTitle = "How to play",
      step1 = "One player secretly picks something everyone can see.",
      step2 = "They give one clue, like \"I spy something red\".",
      step3 = "The others guess out loud. First to get it picks the next thing.",
      tipsTitle = "Clue ideas",
      tip1 = "By colour, shape, size or what it's made of.",
      tip2 = "Or by first letter: \"…something beginning with B\"."
    )
  )

  val pl: Strings = Strings(
    freezeDance = FreezeDance(
      name = "Tańcz i zastygnij",
      description = "Tańcz do muzyki — i zastygnij w bezruchu, gdy ucichnie!",
      instruction = "Naciśnij Start i tańcz. Gdy muzyka cichnie — zastygnij w bezruchu! Kto poruszy się ostatni, pauzuje rundę.",
      start = "Włącz muzykę ▶",
      stop = "Stop",
      danceCue = "Tańcz! 🕺",
      freezeCue = "Zastygnij! 🧊",
      srcSynth = "Wbudowana",
      srcSongs = "Piosenki",
      synthHint = "Za każdym razem nowa, wesoła melodia — bez konfiguracji, działa offline.",
      songsHint = "Dotknij piosenki, aby ją wczytać — lub dodaj własną poniżej.",
      pasteLabel = "Albo wklej link",
      linkPlaceholder = "Link do YouTube lub audio…",
      linkLoad = "Wczytaj",
      linkInvalid = "Nie udało się odczytać linku. Spróbuj linku z YouTube lub bezpośredniego linku .mp3.",
      ytAdsNote = "Uwaga: YouTube może wyświetlać reklamy, które przerwą zabawę.",
      uploadLabel = "Albo użyj pliku z tego urządzenia",
      uploadButton = "Wybierz utwór…",
      freeMusicLabel = "Brak pliku? Darmowa muzyka:",
      addOwn = "Użyj linku lub pliku"
    ),
    hotPotato = HotPotato(
      name = "Gorący ziemniak",
      description = "Podawaj szybko — nie daj się złapać z nim w rękach, gdy muzyka ucichnie!",
      instruction = "Podawajcie ziemniaka — dowolny mały, miękki przedmiot — w kółko, gdy gra muzyka. Urwie się w losowym momencie, a kto go wtedy trzyma, ten złapany. Naciśnij „Zagraj jeszcze raz”, by zacząć kolejną rundę.",
      start = "Włącz muzykę ▶",
      again = "Zagraj jeszcze raz",
      stop = "Stop",
      passCue = "Podawaj dalej! 🥔",
      caughtCue = "Złapany! 💥"
    ),
    activeGames = ActiveGames(
      name = "Gry ruchowe",
      description = "Klasyczne gry do biegania od ręki — bez sprzętu, bez przygotowań.",
      lava = ActiveGameRules(
        name = "Podłoga to lawa",
        blurb = "Cokolwiek robisz, nie dotykaj podłogi.",
        howTitle = "Jak grać",
        steps = List(
          "Ktoś krzyczy „podłoga to lawa!” — od tej sekundy nie wolno dotykać podłogi.",
          "Wszyscy w pośpiechu odrywają stopy od ziemi: na kanapę, poduszkę, krzesło.",
          "Kto dotknie podłogi, odpada. Wygrywa ten, kto jako ostatni zostanie bezpieczny."
        ),
        tipsTitle = "Zróbcie to po swojemu",
        tips = List(
          "Ustalcie najpierw, które meble są bezpieczne, a co jest poza zasięgiem.",
          "Rozłóżcie poduszki jak kamienie do przeskakiwania i spróbujcie przejść przez pokój, nie dotykając podłogi.",
          "Bezpieczeństwo przede wszystkim: usuńcie ostre rogi i nie wchodźcie na nic, co może się przewrócić."
        )
      ),
      tag = ActiveGameRules(
        name = "Berek",
        blurb = "Jedna osoba goni — to „berek”.",
        howTitle = "Jak grać",
        steps = List(
          "Wybierzcie, kto jest berkiem — szybkie odliczanie albo chóralne „nie ja!” załatwia sprawę.",
          "Berek goni pozostałych i próbuje kogoś klepnąć.",
          "Kto zostanie klepnięty, ten jest nowym berkiem. Ustalcie granice pola przed startem."
        ),
        tipsTitle = "Ciekawe odmiany",
        tips = List(
          "Berek-zamrażacz: klepnięty zastyga, dopóki wolny kolega nie przejdzie mu pod nogami.",
          "Berek-łańcuch: każdy złapany łapie berka za rękę i łańcuch rośnie.",
          "Berek-cień: zamiast dotykać, berek nadeptuje twój cień — najlepiej w słońcu."
        )
      ),
      hideSeek = ActiveGameRules(
        name = "Chowany",
        blurb = "Jeden szuka, reszta się chowa.",
        howTitle = "Jak grać",
        steps = List(
          "Jedna osoba szuka. Zasłania oczy i głośno liczy do dwudziestu.",
          "Reszta w tym czasie rozbiega się i chowa.",
          "„Pora na mnie, szukam!” — szukający szuka, aż znajdzie wszystkich."
        ),
        tipsTitle = "Ciekawe odmiany",
        tips = List(
          "Sardynki: jedna osoba się chowa, a reszta szuka — gdy ją znajdziesz, wciśnij się obok i chowaj razem z nią.",
          "Ustalcie, gdzie wolno się chować, żeby nikt nie czekał w nieskończoność w miejscu, którego nikt nie sprawdza.",
          "Kto zostanie znaleziony ostatni, szuka w kolejnej rundzie."
        )
      ),
      redLight = ActiveGameRules(
        name = "Raz, dwa, trzy — Baba Jaga patrzy",
        blurb = "Skradaj się do przodu — i zastygnij, gdy patrzy.",
        howTitle = "Jak grać",
        steps = List(
          "Jedna osoba to Baba Jaga i stoi na drugim końcu, tyłem do reszty.",
          "Gdy mówi „raz, dwa, trzy”, wszyscy skradają się naprzód; na „Baba Jaga patrzy!” odwraca się.",
          "Kto się wtedy poruszy, wraca na start. Kto pierwszy dotknie Baby Jagi, wygrywa."
        ),
        tipsTitle = "Zróbcie to po swojemu",
        tips = List(
          "Baba Jaga może bawić się tempem — długo czekać, a potem nagle się odwrócić, by złapać niecierpliwych.",
          "Spróbujcie w zwolnionym tempie albo skacząc na jednej nodze — będzie więcej śmiechu.",
          "Najlepiej dużo miejsca na dworze, ale długi korytarz też się sprawdzi."
        )
      )
    ),
    appTitle = "Tandu",
    tagline = "Wasz czas razem",
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
      suggestActivity = "Zaproponuj!",
      suggestAnother = "Inna propozycja",
      spinning = "Losowanie…",
      tools = "Narzędzia",
      activities = "Zabawy",
      installApp = "Zainstaluj aplikację"
    ),
    filters = Filters(
      games = "Gry",
      move = "Ruch",
      learn = "Nauka",
      solo = "Solo",
      two = "Dwóch",
      group = "3+",
      onTheGo = "W drodze",
      favourites = "Ulubione",
      addToFavourites = "Dodaj do ulubionych",
      removeFromFavourites = "Usuń z ulubionych",
      noFavouritesYet = "Dotknij gwiazdki przy aktywności, aby zapisać ją jako ulubioną.",
      searchPlaceholder = "Szukaj zabaw…",
      noMatches = "Brak zabaw pasujących do wyszukiwania.",
      hide = "Ukryj zabawę",
      unhide = "Pokaż zabawę",
      showHidden = "Ukryte"
    ),
    about = About(
      open = "O aplikacji",
      title = "O Tandu",
      body = "Tandu to mały pomocnik, gdy szukacie pomysłu na zabawę z dziećmi. Stuknij „Zaproponuj zabawę\", żeby wylosować coś na chybił trafił, albo przeglądaj listę. Część gier zagrasz w aplikacji, inne to pomysły do zabawy offline — idealne do auta, na kanapę albo deszczowe popołudnie.",
      privacy = "Polityka prywatności"
    ),
    installHelp = InstallHelp(
      title = "Dodaj Tandu do ekranu początkowego",
      body = "Na iPhonie lub iPadzie otwórz tę stronę w Safari (inne przeglądarki nie zainstalują aplikacji), stuknij przycisk Udostępnij na dole, a następnie wybierz „Do ekranu początkowego\". Tandu będzie się otwierać jak zwykła aplikacja — na pełnym ekranie i offline."
    ),
    menu = Menu(
      open = "Menu",
      feedback = "Prześlij opinię",
      language = "Język",
      readAloud = "Czytaj na głos"
    ),
    mode = Mode(
      choose = "Jak chcecie grać?",
      inApp = "Gra w aplikacji",
      offline = "Gra offline",
      p2p = "Gra na dwóch urządzeniach",
      lichess = "Zagraj online",
      external = "Otwórz online",
      experimentalBadge = "Beta",
      experimentalWarning = "Ta funkcjonalność jest eksperymentalna i może czasami nie działać"
    ),
    printable = Printable(
      print = "Drukuj",
      printMaps = "Drukuj plansze",
      printRules = "Drukuj zasady"
    ),
    timer = Timer(
      name = "Stoper",
      description = "Wybierz czas i odlicz, na końcu zabrzmi sygnał",
      start = "Start",
      pause = "Pauza",
      restart = "Restart"
    ),
    offline = Offline(
      materials = Materials(
        paperPen = "kartka + długopis",
        printer = "drukarka",
        scissors = "nożyczki",
        laminatorOptional = "laminator (opcjonalnie, do wielokrotnego użytku)",
        deck52 = "talia 52 kart",
        chessBoard = "szachownica + figury",
        checkersBoard = "warcabnica + pionki",
        board = "płaski stół",
        none = "nic — sama wyobraźnia"
      ),
      battleships = BattleshipsOff(
        printTitle = "Statki — plansze dla dwóch graczy",
        ownLabel = "Moja flota",
        enemyLabel = "Wody przeciwnika",
        fleetTitle = "Flota (wariant polski)",
        fleetLine = "1 × 4-masztowiec · 2 × 3-masztowce · 3 × 2-masztowce · 4 × 1-masztowce",
        rules = Rules("Jak grać", List(
          "Ustaw flotę w tajemnicy na planszy „Moja flota\". Statki stawia się poziomo lub pionowo; statki nie mogą się stykać, nawet po skosie.",
          "Na zmianę podawajcie współrzędne (litera + cyfra). Trafienia oznaczcie ×, pudła kropką · na planszy „Wody przeciwnika\".",
          "Po trafieniu mów „trafiony\" — przeciwnik odpowiada „zatopiony\", gdy cały statek pada.",
          "Wygrywa ten, kto pierwszy zatopi całą flotę przeciwnika."
        )),
        modeAsk = "Jak chcecie zagrać w Statki?"
      ),
      memory = MemoryOff(
        printTitle = "Memory — karty do wycięcia",
        cutHint = "Wydrukujcie kartkę, wytnijcie po liniach, potasujcie odwrócone i grajcie.",
        rules = Rules("Jak grać", List(
          "Potasujcie karty i ułóżcie zakryte w siatce.",
          "Na zmianę: odkryj dwie karty. Jeśli pasują — zatrzymujesz parę i grasz dalej; jeśli nie — odwracasz z powrotem.",
          "Wygrywa osoba z największą liczbą par, gdy plansza jest pusta."
        ))
      ),
      hangman = HangmanOff(
        keeperTitle = "Słowo dla trzymającego telefon",
        keeperHint = "Trzymasz telefon, podglądasz słowo i je chowasz. Reszta zgaduje litery, a wisielca rysujecie na kartce.",
        reveal = "Pokaż słowo",
        hide = "Ukryj słowo",
        tap = "Stuknij, by pokazać",
        categorySetting = "Kategoria",
        categoryAny = "Dowolne słowo",
        categoryAnimals = "Zwierzęta",
        categoryFoods = "Jedzenie",
        categoryCountries = "Państwa",
        drawHint = "Za każdą złą literę dorysuj jedną część wisielca.",
        rules = Rules("Jak grać", List(
          "Ktoś wymyśla słowo (albo telefon je losuje) i rysuje na kartce kreski — po jednej na każdą literę.",
          "Reszta zgaduje litery, jedną na raz. Trafione litery wpisz na kreski; pudła wypisz z boku.",
          "Każde pudło dorysowuje kolejną część wisielca.",
          "Wygrasz, jeśli ułożycie słowo, zanim rysunek się dokończy; przegrana — gdy wisielec gotowy."
        )),
        gallowsTitle = "Jak rysować szubienicę"
      ),
      chess = ChessOff(
        rules = Rules("Jak grać", List(
          "Dwóch graczy. Białe zaczynają; gracze wykonują po jednym ruchu na zmianę.",
          "Wygrywasz dając mata królowi przeciwnika.",
          "Król pod atakiem jest „w szachu\" — następny ruch musi go z szachu uwolnić.",
          "Brak legalnych ruchów bez szacha to remis (pat)."
        )),
        pieces = Rules("Jak chodzą figury", List(
          "Pion: do przodu o jedno pole; o dwa z pozycji startowej. Bije po skosie o jedno pole.",
          "Skoczek: w kształcie litery L — dwa pola i jedno; może przeskakiwać inne figury.",
          "Goniec: dowolna liczba pól po skosie.",
          "Wieża: dowolna liczba pól w pionie lub poziomie.",
          "Hetman: dowolna liczba pól w dowolnym kierunku.",
          "Król: o jedno pole w dowolnym kierunku."
        )),
        specials = Rules("Ruchy specjalne", List(
          "Roszada: król rusza się o dwa pola w stronę wieży, a wieża przeskakuje na drugą stronę króla. Wolno, jeśli żadna z figur się nie ruszała, między nimi jest pusto i król nie przechodzi przez pole bicia.",
          "Bicie w przelocie: pion, który właśnie zrobił dwa kroki, może być zbity przez pion przeciwnika obok — tylko w następnym ruchu.",
          "Promocja: pion, który dotrze do ostatniego rzędu, zamienia się w hetmana (lub wieżę/gońca/skoczka — do wyboru)."
        )),
        printTitle = "Szachy — jak chodzą figury",
        lichessLabel = "Otwórz Lichess"
      ),
      ticTacToe = TicTacToeOff(
        rules = Rules("Jak grać", List(
          "Narysujcie siatkę 3×3 na kartce.",
          "Na zmianę stawiajcie X lub O w pustym polu.",
          "Pierwszy, który ułoży trzy w rzędzie — w poziomie, pionie lub po skosie — wygrywa."
        )),
        gomokuTipTitle = "Umiecie już?",
        gomokuTip = "Spróbujcie gomoku: większa plansza, pięć w rzędzie. Ten sam pomysł, znacznie ciekawszy."
      ),
      solitaire = SolitaireOff(
        rules = Rules("Klondike — jak grać", List(
          "Rozłóżcie siedem kolumn: 1, 2, 3, 4, 5, 6, 7 kart. Górna karta każdej kolumny odkryta.",
          "Pozostałe karty tworzą stos rezerwowy — odkrywajcie po jednej (lub po trzy) na stos odrzucony.",
          "Na kolumnach buduj malejąco i naprzemiennie w kolorach. Sekwencję możesz przesunąć w całości. Pustą kolumnę zaczyna król.",
          "Na fundamentach buduj rosnąco wg koloru, od asa do króla. Zwycięstwo, gdy wszystkie cztery fundamenty pełne."
        )),
        setupExample = "Początkowy układ"
      ),
      categories = CategoriesOff(
        printTitle = "Państwa-Miasta — arkusz",
        categoriesLabel = "Kategoria",
        lettersLabel = "Litera",
        scoresLabel = "Punkty",
        curatedNote = "Trzy rundy, jedna litera na kolumnę. W każdej kategorii wpiszcie słowo zaczynające się na tę literę.",
        rules = Rules("Jak grać", List(
          "Każda kolumna ma na górze swoją literę — to litera tej rundy.",
          "Włączcie czas (około 3 minuty). Każdy stara się wpisać po jednym haśle na kategorię, zaczynającym się na tę literę.",
          "Po czasie porównajcie: punkt za unikalne hasło; jeśli dwie osoby wpisały to samo, nikt nie dostaje punktu.",
          "Zagrajcie wszystkie trzy kolumny. Wygrywa największa suma."
        ))
      ),
      checkers = CheckersOff(
        rules = Rules("Jak grać", List(
          "Każdy gracz ma 12 pionków na czarnych polach w trzech tylnych rzędach.",
          "Pionek idzie po skosie do przodu na puste czarne pole. Bije, przeskakując sąsiada na puste pole za nim.",
          "Bicie jest obowiązkowe; wielokrotne bicia kontynuuj w jednej turze.",
          "Pionek, który dojdzie do ostatniego rzędu, zostaje damką — porusza się i bije w obie strony.",
          "Wygrasz, zbijając wszystkie pionki przeciwnika lub blokując mu ruchy."
        )),
        lichessLabel = "Otwórz lidraughts"
      ),
      sudoku = SudokuOff(
        printTitle = "Sudoku — sześć zagadek",
        sheetHint = "Wydrukuj kartkę, weź długopis i rozwiązuj. Bez rozwiązań — taka zabawa.",
        rules = Rules("Jak grać", List(
          "Wypełnij planszę tak, by w każdym wierszu, każdej kolumnie i każdym kwadracie 3×3 znalazła się każda cyfra od 1 do 9 dokładnie raz.",
          "Podane cyfry są niezmienne. Używaj małych notatek przy niepewnych polach.",
          "Działaj przez eliminację: jeśli w polu pasuje tylko jedna cyfra — to jest rozwiązanie."
        ))
      ),
      maze = MazeOff(
        printTitle = "Labirynty — pomóż myszce znaleźć ser",
        sheetHint = "Wydrukuj kartkę i narysuj ołówkiem drogę od myszki do sera. Przez każdy labirynt prowadzi jedna ścieżka.",
        rules = Rules("Jak grać", List(
          "Zacznij przy myszce 🐭 i znajdź drogę do sera 🧀.",
          "Nie można przechodzić przez ściany — idź tylko otwartymi korytarzami.",
          "Ślepa uliczka? Cofnij się i spróbuj innej drogi."
        ))
      ),
      wordSearch = WordSearchOff(
        printTitle = "Wykreślanka — znajdź ukryte słowa",
        sheetHint = "Wydrukuj kartkę i zakreślaj znalezione słowa. Lista słów jest pod planszą.",
        wordsLabel = "Znajdź te słowa",
        rules = Rules("Jak grać", List(
          "Każde słowo z listy jest ukryte w siatce liter.",
          "Słowa biegną w linii prostej — w poziomie, pionie lub po skosie, czasem wspak.",
          "Zakreślaj każde znalezione słowo i skreślaj je z listy."
        ))
      ),
      guideRobot = GuideRobotOff(
        printTitle = "Poprowadź robota — zapisz trasę ze strzałek",
        sheetHint = "Wydrukuj kartkę i zapisz strzałki, które doprowadzą każdego robota do mety.",
        writeLabel = "Zapisz strzałki",
        rules = Rules("Jak grać", List(
          "Robot 🤖 startuje na planszy i musi dotrzeć do mety 🏁.",
          "Na łatwej kartce zapisz strzałki (↑ ↓ ← →) — jedna kratka na strzałkę.",
          "Na trudniejszych robot się obraca: ↑ naprzód, ↺ w lewo, ↻ w prawo.",
          "Ściany 🧱 blokują drogę, więc je omijaj.",
          "Na trudnych kartkach przejedź przez gwiazdkę ⭐ przed metą."
        ))
      )
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
      endTurn = "Zakończ turę",
      p2p = BattleshipsP2P(
        title = "Gra na dwóch urządzeniach",
        intro = "Połącz dwa telefony, żeby zagrać z ukrytymi planszami. Jedno urządzenie tworzy grę, drugie dołącza tym samym kodem.",
        create = "Utwórz grę",
        join = "Dołącz",
        connect = "Połącz",
        shareCode = "Podaj ten kod drugiemu urządzeniu",
        enterCode = "Wpisz kod z drugiego urządzenia",
        waiting = "Czekam na drugie urządzenie…",
        yourTurn = "Twoja tura",
        opponentTurn = "Tura przeciwnika",
        waitingShot = "Czekam na strzał przeciwnika…",
        waitingResolve = "Czekam na wynik…",
        youWin = "Wygrywasz!",
        youLose = "Przegrywasz."
      )
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
      next = "Nowa runda",
      startingWith = "na literę"
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
    ),
    sudoku = Sudoku(
      name = "Sudoku",
      description = "Wypełnij siatkę 9×9 tak, by każdy wiersz, kolumna i kwadrat miał 1–9.",
      chooseVariant = "Wybierz poziom",
      changeVariant = "Zmień poziom",
      newGame = "Nowa gra",
      undo = "Cofnij",
      pencil = "Notatka",
      easy = SudokuVariant("Łatwe", "Więcej podpowiedzi — przyjazny start."),
      medium = SudokuVariant("Średnie", "Mniej podpowiedzi — prawdziwe wyzwanie."),
      hard = SudokuVariant("Trudne", "Mało podpowiedzi — dla wytrawnych.")
    ),
    minesweeper = Minesweeper(
      name = "Saper",
      description = "Odkryj planszę, nie wchodząc na minę.",
      chooseVariant = "Wybierz rozmiar",
      changeVariant = "Zmień rozmiar",
      newGame = "Nowa gra",
      playing = "Uważaj na miny…",
      youWon = "Czysto!",
      youLost = "Bum!",
      revealMode = "Odkryj",
      flagMode = "Flaga",
      easy = MinesweeperVariant("Łatwy", "9×9 z 10 minami."),
      medium = MinesweeperVariant("Średni", "12×12 z 25 minami."),
      hard = MinesweeperVariant("Trudny", "16×12 z 40 minami.")
    ),
    maze = Maze(
      name = "Labirynt",
      description = "Pomóż myszce znaleźć ser.",
      instruction = "Przesuwaj palcem lub przeciągaj, by zaprowadzić myszkę do sera — strzałki też działają.",
      won = "Ser znaleziony! 🧀",
      clearPath = "Wyczyść drogę",
      newGame = "Nowy labirynt",
      easy = MazeVariant("Łatwy", "10×10 z łagodnymi pętlami."),
      medium = MazeVariant("Średni", "16×16, jedna droga."),
      hard = MazeVariant("Trudny", "24×24, prawdziwy labirynt.")
    ),
    wordSearch = WordSearch(
      name = "Wykreślanka",
      description = "Znajdź ukryte słowa w siatce liter.",
      instruction = "Przeciągaj palcem po literach, by zaznaczyć słowo — w poziomie, pionie lub po skosie.",
      foundLabel = "Znaleziono",
      won = "Znalazłeś wszystkie! 🔍",
      newGame = "Nowa plansza",
      easy = WordSearchVariant("Łatwy", "8×8, 6 słów, w poziomie i pionie."),
      medium = WordSearchVariant("Średni", "11×11, 8 słów, ze skosami."),
      hard = WordSearchVariant("Trudny", "13×13, 10 słów, w każdą stronę.")
    ),
    guideRobot = GuideRobot(
      name = "Poprowadź robota",
      description = "Ułóż strzałki i doprowadź robota do mety.",
      instruction = "Dotykaj strzałek, by ułożyć trasę, a potem naciśnij Start, by wysłać robota.",
      instructionTurns = "Dotykaj „naprzód” i przycisków obrotu, by sterować robotem, potem naciśnij Start.",
      programLabel = "Twój program",
      emptyProgram = "Dotykaj strzałek poniżej, by dodać kroki.",
      run = "Start ▶",
      undo = "Cofnij",
      clear = "Wyczyść",
      starHint = "Przejedź przez gwiazdkę ⭐ przed dotarciem do mety 🏁.",
      won = "Robot dotarł! 🤖",
      crashed = "Bęc! Robot wpadł na ścianę. Spróbuj jeszcze raz.",
      missed = "Robot nie dotarł do mety. Spróbuj jeszcze raz.",
      missedStar = "Najpierw zabierz gwiazdkę ⭐! Spróbuj jeszcze raz.",
      tryAgain = "Spróbuj jeszcze raz",
      newGame = "Nowa plansza",
      easy = GuideRobotVariant("Łatwy", "5×5, wolna droga do mety."),
      medium = GuideRobotVariant("Średni", "6×6 ze ścianami do ominięcia."),
      hard = GuideRobotVariant("Trudny", "6×6 — zabierz gwiazdkę, potem meta.")
    ),
    wordBuilder = WordBuilder(
      name = "Czytaj i pisz",
      description = "Układaj słowa i wybieraj pasujące obrazki.",
      easy = WordBuilderLevel("Łatwy", "Krótkie słowa, bez dodatkowych liter."),
      medium = WordBuilderLevel("Średni", "Dłuższe słowa, kilka dodatkowych liter."),
      hard = WordBuilderLevel("Trudny", "Długie słowa z dodatkowymi literami."),
      correct = "Super!",
      nextWord = "Następne słowo",
      skip = "Pomiń",
      printTitle = "Czytaj i pisz — karta do druku",
      printHint = "Wybierz poziom i wydrukuj kartę."
    ),
    mathPractice = MathPractice(
      name = "Matematyka",
      description = "Liczenie, dodawanie i odejmowanie.",
      easy   = MathPracticeLevel("Łatwy",   "Liczenie i porównywanie z obrazkami, do 10."),
      medium = MathPracticeLevel("Średni",  "Dodawanie i odejmowanie do 10."),
      hard   = MathPracticeLevel("Trudny",  "Do 20 z brakującą liczbą."),
      howMany = "Ile?",
      pickGroup = "Wybierz pasującą grupę",
      correct = "Brawo!",
      nextProblem = "Następne",
      skip = "Pomiń",
      printTitle = "Matematyka — karta pracy",
      printHint = "Wybierz poziom i wydrukuj kartę.",
      plus = "plus",
      minus = "minus",
      equals = "równa się",
      whatNumber = "ile",
      compare = "jest większe czy mniejsze niż"
    ),
    clock = Clock(
      name = "Zegar",
      description = "Odczytuj godziny z zegara.",
      matchName = "Odczytaj zegar",
      matchDesc = "Dopasuj zegar wskazówkowy i cyfrowy, co kwadrans.",
      todName = "Pora dnia",
      todDesc = "Nazwij porę dnia na podstawie godziny.",
      formatLabel = "Zegar",
      format12 = "12-godzinny",
      format24 = "24-godzinny",
      whatTime = "Która godzina?",
      pickClock = "Wybierz pasujący zegar",
      partOfDay = "Jaka to pora dnia?",
      morning = "Rano",
      afternoon = "Popołudnie",
      evening = "Wieczór",
      night = "Noc",
      correct = "Brawo!",
      next = "Dalej",
      skip = "Pomiń"
    ),
    reading = Reading(
      name = "Czytanie",
      description = "Przeczytaj razem klasykę.",
      hint = "Poniżej znajdziesz kilka inspiracji.",
      search = "Szukaj",
      freeEbook = "E-book",
      bandTots = "0–3 lat",
      bandPicture = "3–6 lat",
      bandChapter = "5–9 lat",
      bandOlder = "8+ lat"
    ),
    memoryChain = MemoryChain(
      name = "Pociąg pamięci",
      description = "Powtórz całą listę i dodaj jedną rzecz.",
      hint = "Gracie po kolei. Każdy powtarza po kolei wszystko, co już padło, i dodaje jedną nową rzecz. Pomylisz kolejność — odpadasz.",
      newTheme = "Nowy temat"
    ),
    iSpy = ISpy(
      name = "Widzę coś",
      description = "Wypatrz coś w pobliżu i podaj jedną wskazówkę.",
      hint = "Gra na spostrzegawczość — najlepiej z rzeczami, które wszyscy teraz widzicie.",
      howTitle = "Jak grać",
      step1 = "Jeden gracz po cichu wybiera coś, co wszyscy widzą.",
      step2 = "Podaje jedną wskazówkę, np. „Widzę coś czerwonego”.",
      step3 = "Reszta zgaduje na głos. Kto trafi pierwszy, wybiera następną rzecz.",
      tipsTitle = "Pomysły na wskazówki",
      tip1 = "Po kolorze, kształcie, wielkości albo z czego jest zrobione.",
      tip2 = "Albo po pierwszej literze: „…coś na literę B”."
    )
  )

  val es: Strings = Strings(
    freezeDance = FreezeDance(
      name = "Baile congelado",
      description = "¡Baila con la música y congélate cuando pare!",
      instruction = "Pulsa Empezar y baila. Cuando la música pare, ¡congélate! El último en moverse se salta la ronda.",
      start = "Poner música ▶",
      stop = "Parar",
      danceCue = "¡A bailar! 🕺",
      freezeCue = "¡Congelado! 🧊",
      srcSynth = "Integrada",
      srcSongs = "Canciones",
      synthHint = "Una melodía divertida y distinta cada vez: sin ajustes, funciona sin conexión.",
      songsHint = "Toca una canción para cargarla — o añade la tuya abajo.",
      pasteLabel = "O pega un enlace",
      linkPlaceholder = "Enlace de YouTube o audio…",
      linkLoad = "Cargar",
      linkInvalid = "No se pudo leer el enlace. Prueba un enlace de YouTube o un enlace directo .mp3.",
      ytAdsNote = "Atención: YouTube puede mostrar anuncios que interrumpan el juego.",
      uploadLabel = "O usa un archivo de este dispositivo",
      uploadButton = "Elige una canción…",
      freeMusicLabel = "¿Sin archivo? Música gratis:",
      addOwn = "Usar un enlace o archivo"
    ),
    hotPotato = HotPotato(
      name = "La patata caliente",
      description = "¡Pásala rápido y que no te pille con ella cuando pare la música!",
      instruction = "Id pasando la patata —cualquier objeto pequeño y blando— en círculo mientras suena la música. Se corta en un momento al azar, y quien la tenga en ese instante queda pillado. Toca Jugar otra vez para la siguiente ronda.",
      start = "Poner música ▶",
      again = "Jugar otra vez",
      stop = "Parar",
      passCue = "¡Pásala! 🥔",
      caughtCue = "¡Pillado! 💥"
    ),
    activeGames = ActiveGames(
      name = "Juegos de movimiento",
      description = "Juegos de toda la vida para corretear ahora mismo: sin material y sin preparativos.",
      lava = ActiveGameRules(
        name = "El suelo es lava",
        blurb = "Hagas lo que hagas, no toques el suelo.",
        howTitle = "Cómo se juega",
        steps = List(
          "Alguien grita «¡el suelo es lava!» y, desde ese instante, está prohibido tocar el suelo.",
          "Todos corren a subirse a algo: un sofá, un cojín, una silla.",
          "Quien toca el suelo queda eliminado. Gana el último que siga a salvo."
        ),
        tipsTitle = "Hazlo a tu manera",
        tips = List(
          "Acordad primero qué muebles valen como zona segura y cuáles quedan fuera.",
          "Repartid cojines como piedras y cruzad la habitación entera sin pisar el suelo.",
          "Seguridad ante todo: apartad las esquinas peligrosas y no os subáis a nada que pueda volcar."
        )
      ),
      tag = ActiveGameRules(
        name = "El pilla-pilla",
        blurb = "Uno la liga y persigue a los demás.",
        howTitle = "Cómo se juega",
        steps = List(
          "Elegid quién la liga — un conteo rápido o un coro de «¡yo no!» lo decide.",
          "Quien la liga persigue a los demás e intenta tocar a alguien.",
          "A quien toque, se la liga. Acordad los límites del terreno antes de empezar."
        ),
        tipsTitle = "Variantes divertidas",
        tips = List(
          "Pilla-pilla congelado: el tocado se queda congelado hasta que un compañero pasa por debajo de sus piernas.",
          "Pilla-pilla en cadena: cada uno que es tocado se da la mano con quien la liga y la cadena crece.",
          "Pilla-pilla de sombras: en vez de tocar, hay que pisar la sombra del otro — mejor a pleno sol."
        )
      ),
      hideSeek = ActiveGameRules(
        name = "El escondite",
        blurb = "Uno cuenta mientras los demás se esconden.",
        howTitle = "Cómo se juega",
        steps = List(
          "Una persona se la queda: se tapa los ojos y cuenta en voz alta hasta veinte.",
          "Los demás se esconden mientras tanto.",
          "«¡Listos o no, allá voy!» — quien cuenta busca hasta encontrar a todos."
        ),
        tipsTitle = "Variantes divertidas",
        tips = List(
          "Sardinas: solo uno se esconde y los demás buscan — cuando lo encuentres, métete con él sin que se note.",
          "Acordad dónde se puede esconder, para que nadie espere eternamente en un sitio que nadie mira.",
          "El último en ser encontrado se la queda en la ronda siguiente."
        )
      ),
      redLight = ActiveGameRules(
        name = "Un, dos, tres, al escondite inglés",
        blurb = "Avanza a hurtadillas… y congélate cuando se gire.",
        howTitle = "Cómo se juega",
        steps = List(
          "Una persona se la queda y se pone al fondo, de espaldas a los demás.",
          "Mientras dice «un, dos, tres, al escondite inglés», todos avanzan; al terminar, se gira de golpe.",
          "A quien pille moviéndose, vuelve a la salida. El primero en tocarle gana y se la queda."
        ),
        tipsTitle = "Hazlo a tu manera",
        tips = List(
          "Quien la liga puede jugar con el ritmo — recitar despacio y girarse de repente para pillar pies traviesos.",
          "Probad a cámara lenta o a la pata coja, para reír más.",
          "Lo mejor es mucho espacio al aire libre, pero un pasillo largo también vale."
        )
      )
    ),
    appTitle = "Tandu",
    tagline = "Vuestro tiempo juntos",
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
      suggestActivity = "Sugerir!",
      suggestAnother = "Otra sugerencia",
      spinning = "Eligiendo…",
      tools = "Herramientas",
      activities = "Actividades",
      installApp = "Instalar app"
    ),
    filters = Filters(
      games = "Juegos",
      move = "Moverse",
      learn = "Aprender",
      solo = "Solo",
      two = "Dos",
      group = "3+",
      onTheGo = "En camino",
      favourites = "Favoritos",
      addToFavourites = "Añadir a favoritos",
      removeFromFavourites = "Quitar de favoritos",
      noFavouritesYet = "Toca la estrella en una actividad para guardarla como favorita.",
      searchPlaceholder = "Buscar actividades…",
      noMatches = "Ninguna actividad coincide con tu búsqueda.",
      hide = "Ocultar actividad",
      unhide = "Mostrar actividad",
      showHidden = "Ocultas"
    ),
    about = About(
      open = "Acerca de",
      title = "Acerca de Tandu",
      body = "Tandu es un pequeño ayudante para elegir algo divertido que hacer con los niños. Toca \"Sugerir actividad\" para una idea al azar, o explora la lista. Algunos juegos se juegan en la app, otros son ideas para hacer sin pantalla — perfectos para el coche, el sofá o una tarde lluviosa.",
      privacy = "Política de privacidad"
    ),
    installHelp = InstallHelp(
      title = "Añade Tandu a la pantalla de inicio",
      body = "En iPhone o iPad, abre esta página en Safari (otros navegadores no pueden instalarla), toca el botón Compartir abajo y elige \"Añadir a pantalla de inicio\". Tandu se abrirá como una app normal, a pantalla completa y lista para usar sin conexión."
    ),
    menu = Menu(
      open = "Menú",
      feedback = "Enviar comentarios",
      language = "Idioma",
      readAloud = "Leer en voz alta"
    ),
    mode = Mode(
      choose = "¿Cómo queréis jugar?",
      inApp = "Jugar en la app",
      offline = "Jugar sin conexión",
      lichess = "Jugar online",
      external = "Abrir online",
      p2p = "Jugar en dos dispositivos",
      experimentalBadge = "Experimental",
      experimentalWarning = "Este modo es experimental y puede no conectarse de forma fiable."
    ),
    printable = Printable(
      print = "Imprimir",
      printMaps = "Imprimir tableros",
      printRules = "Imprimir reglas"
    ),
    timer = Timer(
      name = "Cronómetro",
      description = "Elige una duración y haz cuenta atrás con un pitido al final",
      start = "Empezar",
      pause = "Pausa",
      restart = "Reiniciar"
    ),
    offline = Offline(
      materials = Materials(
        paperPen = "papel + bolígrafo",
        printer = "impresora",
        scissors = "tijeras",
        laminatorOptional = "plastificadora (opcional, para reutilizar)",
        deck52 = "baraja de 52 cartas",
        chessBoard = "tablero de ajedrez + piezas",
        checkersBoard = "tablero de damas + fichas",
        board = "una superficie plana",
        none = "nada — solo imaginación"
      ),
      battleships = BattleshipsOff(
        printTitle = "Hundir la flota — tableros para dos",
        ownLabel = "Mi flota",
        enemyLabel = "Aguas enemigas",
        fleetTitle = "Flota (variante)",
        fleetLine = "1 × 4 casillas · 2 × 3 casillas · 3 × 2 casillas · 4 × 1 casilla",
        rules = Rules("Cómo se juega", List(
          "Coloca tu flota en secreto en \"Mi flota\". Los barcos van en horizontal o vertical y no pueden tocarse, ni siquiera en diagonal.",
          "Por turnos decid coordenadas (letra + número). Marca tocados con × y aguas con · en \"Aguas enemigas\".",
          "Al tocar di \"tocado\"; cuando cae el barco entero di \"hundido\".",
          "Gana quien hunda primero toda la flota enemiga."
        )),
        modeAsk = "¿Cómo queréis jugar a hundir la flota?"
      ),
      memory = MemoryOff(
        printTitle = "Memoria — recorta las cartas",
        cutHint = "Imprime la hoja, recorta por las líneas, baraja boca abajo y juega.",
        rules = Rules("Cómo se juega", List(
          "Baraja las cartas y colócalas boca abajo en cuadrícula.",
          "Por turnos. Da la vuelta a dos cartas: si son iguales, te las quedas y vuelves a jugar; si no, dales la vuelta.",
          "Gana quien tenga más parejas cuando la mesa quede vacía."
        ))
      ),
      hangman = HangmanOff(
        keeperTitle = "Palabra para quien tiene el teléfono",
        keeperHint = "Tienes el teléfono, miras la palabra y la ocultas. Los demás adivinan letras y dibujáis al ahorcado en papel.",
        reveal = "Mostrar palabra",
        hide = "Ocultar palabra",
        tap = "Toca para mostrar",
        categorySetting = "Categoría",
        categoryAny = "Cualquier palabra",
        categoryAnimals = "Animales",
        categoryFoods = "Comidas",
        categoryCountries = "Países",
        drawHint = "Por cada letra fallada, dibuja una parte del ahorcado.",
        rules = Rules("Cómo se juega", List(
          "Alguien piensa una palabra (o la sortea el teléfono) y dibuja rayas en papel — una por letra.",
          "Los demás van diciendo letras. Las acertadas se escriben en las rayas; las falladas se apuntan al lado.",
          "Cada fallo añade una parte al dibujo.",
          "Ganas si completas la palabra antes de terminar el dibujo; pierdes si el dibujo se completa."
        )),
        gallowsTitle = "Cómo dibujar la horca"
      ),
      chess = ChessOff(
        rules = Rules("Cómo se juega", List(
          "Dos jugadores. Blancas mueven primero; se alterna una jugada por turno.",
          "Ganas dando jaque mate al rey rival.",
          "Un rey atacado está en jaque — la próxima jugada debe salir del jaque.",
          "Sin jugadas legales y sin estar en jaque: tablas (ahogado)."
        )),
        pieces = Rules("Cómo se mueven", List(
          "Peón: uno hacia adelante; dos desde su casilla inicial. Captura en diagonal una casilla.",
          "Caballo: en L — dos y uno, salta sobre otras piezas.",
          "Alfil: cualquier número de casillas en diagonal.",
          "Torre: cualquier número en línea recta.",
          "Dama: cualquier número en cualquier dirección.",
          "Rey: una casilla en cualquier dirección."
        )),
        specials = Rules("Movimientos especiales", List(
          "Enroque: el rey se mueve dos casillas hacia una torre y la torre salta al otro lado. Si ninguno se ha movido, no hay piezas en medio y el rey no cruza por jaque.",
          "Al paso: un peón que acaba de avanzar dos casillas puede ser capturado por el peón rival contiguo — solo en la jugada siguiente.",
          "Coronación: un peón que llega al fondo se convierte en dama (o torre/alfil/caballo)."
        )),
        printTitle = "Ajedrez — cómo se mueven las piezas",
        lichessLabel = "Abrir Lichess"
      ),
      ticTacToe = TicTacToeOff(
        rules = Rules("Cómo se juega", List(
          "Dibujad una cuadrícula 3×3 en papel.",
          "Por turnos marcad X o O en una casilla vacía.",
          "El primero que haga tres en raya — horizontal, vertical o diagonal — gana."
        )),
        gomokuTipTitle = "¿Lo dominas?",
        gomokuTip = "Prueba gomoku: cuadrícula mayor, cinco en raya. La misma idea, mucho más interesante."
      ),
      solitaire = SolitaireOff(
        rules = Rules("Klondike — cómo se juega", List(
          "Reparte siete columnas: 1, 2, 3, 4, 5, 6, 7 cartas. La carta superior boca arriba.",
          "Las restantes son el mazo; gira una (o tres) al descarte.",
          "Construye en las columnas hacia abajo y alternando colores. Mueve secuencias enteras. Una columna vacía acepta un rey.",
          "Construye los cimientos por palo, del as al rey. Ganas cuando los cuatro están completos."
        )),
        setupExample = "Disposición inicial"
      ),
      categories = CategoriesOff(
        printTitle = "Hoja de Scattergories",
        categoriesLabel = "Categoría",
        lettersLabel = "Letra",
        scoresLabel = "Puntos",
        curatedNote = "Tres rondas, una letra por columna. Rellenad una palabra por categoría que empiece por esa letra.",
        rules = Rules("Cómo se juega", List(
          "Cada columna tiene una letra arriba — esa es la letra de la ronda.",
          "Poned un temporizador (unos 3 minutos). Cada uno intenta escribir una palabra por categoría que empiece por esa letra.",
          "Al acabar el tiempo, comparad: un punto por respuesta única; si dos jugadores pusieron la misma palabra, ninguno puntúa.",
          "Jugad las tres columnas. Gana quien sume más puntos."
        ))
      ),
      checkers = CheckersOff(
        rules = Rules("Cómo se juega", List(
          "Cada jugador tiene 12 fichas en las casillas oscuras de las tres filas traseras.",
          "Movéis en diagonal hacia adelante a una casilla oscura vacía. Capturáis saltando a un rival adyacente y cayendo en la casilla siguiente.",
          "Si hay captura disponible es obligatoria; los saltos múltiples siguen en el mismo turno.",
          "Al llegar al fondo, la ficha se corona — puede moverse y capturar en ambos sentidos.",
          "Ganáis al capturar todas las fichas o bloquear los movimientos del rival."
        )),
        lichessLabel = "Abrir lidraughts"
      ),
      sudoku = SudokuOff(
        printTitle = "Sudoku — seis enigmas",
        sheetHint = "Imprime la hoja, coge un lápiz y a resolver. Sin soluciones — esa es la gracia.",
        rules = Rules("Cómo jugar", List(
          "Rellena la cuadrícula de modo que cada fila, cada columna y cada caja 3×3 contenga los dígitos del 1 al 9 exactamente una vez.",
          "Los números dados no cambian. Usa pequeñas anotaciones para los candidatos.",
          "Trabaja por eliminación: si una casilla solo admite un dígito, ese es la respuesta."
        ))
      ),
      maze = MazeOff(
        printTitle = "Laberintos — ayuda al ratón a encontrar el queso",
        sheetHint = "Imprime la hoja y dibuja con lápiz un camino del ratón al queso. Hay un solo camino en cada laberinto.",
        rules = Rules("Cómo jugar", List(
          "Empieza en el ratón 🐭 y encuentra el camino hasta el queso 🧀.",
          "No puedes cruzar las paredes — sigue solo los pasillos abiertos.",
          "¿Sin salida? Retrocede y prueba otro camino."
        ))
      ),
      wordSearch = WordSearchOff(
        printTitle = "Sopa de letras — encuentra las palabras",
        sheetHint = "Imprime la hoja y rodea cada palabra que encuentres. La lista está debajo de la cuadrícula.",
        wordsLabel = "Encuentra estas palabras",
        rules = Rules("Cómo jugar", List(
          "Cada palabra de la lista está escondida en la cuadrícula de letras.",
          "Las palabras van en línea recta — en horizontal, vertical o diagonal, a veces al revés.",
          "Rodea cada palabra que encuentres y táchala de la lista."
        ))
      ),
      guideRobot = GuideRobotOff(
        printTitle = "Guía al robot — escribe el camino de flechas",
        sheetHint = "Imprime la hoja y escribe las flechas que llevan a cada robot a su meta.",
        writeLabel = "Escribe las flechas",
        rules = Rules("Cómo jugar", List(
          "El robot 🤖 empieza en la cuadrícula y debe llegar a la meta 🏁.",
          "En la hoja fácil, escribe flechas (↑ ↓ ← →) — una casilla por flecha.",
          "En las hojas difíciles el robot gira: ↑ avanzar, ↺ girar a la izquierda, ↻ girar a la derecha.",
          "Los muros 🧱 cierran el paso, así que esquívalos.",
          "En las hojas difíciles, pasa por la estrella ⭐ antes de la meta."
        ))
      )
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
      endTurn = "Fin del turno",
      p2p = BattleshipsP2P(
        title = "Jugar en dos dispositivos",
        intro = "Conecta dos teléfonos para jugar con tableros ocultos. Un dispositivo crea la partida y el otro se une con el mismo código.",
        create = "Crear partida",
        join = "Unirse",
        connect = "Conectar",
        shareCode = "Comparte este código con el otro dispositivo",
        enterCode = "Introduce el código del otro dispositivo",
        waiting = "Esperando al otro dispositivo…",
        yourTurn = "Tu turno",
        opponentTurn = "Turno del rival",
        waitingShot = "Esperando su disparo…",
        waitingResolve = "Esperando el resultado…",
        youWin = "¡Ganaste!",
        youLose = "Perdiste."
      )
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
      next = "Nueva ronda",
      startingWith = "que empiezan por la letra"
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
    ),
    sudoku = Sudoku(
      name = "Sudoku",
      description = "Rellena la cuadrícula 9×9 con 1–9 en cada fila, columna y caja.",
      chooseVariant = "Elige dificultad",
      changeVariant = "Cambiar dificultad",
      newGame = "Nueva partida",
      undo = "Deshacer",
      pencil = "Lápiz",
      easy = SudokuVariant("Fácil", "Más pistas — un buen comienzo."),
      medium = SudokuVariant("Medio", "Menos pistas — un reto real."),
      hard = SudokuVariant("Difícil", "Pocas pistas — para expertos.")
    ),
    minesweeper = Minesweeper(
      name = "Buscaminas",
      description = "Despeja el tablero sin pisar una mina.",
      chooseVariant = "Elige tamaño",
      changeVariant = "Cambiar tamaño",
      newGame = "Nueva partida",
      playing = "Cuidado dónde pisas…",
      youWon = "¡Despejado!",
      youLost = "¡Bum!",
      revealMode = "Descubrir",
      flagMode = "Bandera",
      easy = MinesweeperVariant("Fácil", "9×9 con 10 minas."),
      medium = MinesweeperVariant("Medio", "12×12 con 25 minas."),
      hard = MinesweeperVariant("Difícil", "16×12 con 40 minas.")
    ),
    maze = Maze(
      name = "Laberinto",
      description = "Ayuda al ratón a encontrar el queso.",
      instruction = "Desliza o arrastra para llevar al ratón hasta el queso — las flechas también funcionan.",
      won = "¡Encontraste el queso! 🧀",
      clearPath = "Borrar camino",
      newGame = "Nuevo laberinto",
      easy = MazeVariant("Fácil", "10×10 con bucles suaves."),
      medium = MazeVariant("Medio", "16×16, un solo camino."),
      hard = MazeVariant("Difícil", "24×24, un verdadero laberinto.")
    ),
    wordSearch = WordSearch(
      name = "Sopa de letras",
      description = "Encuentra las palabras escondidas en la cuadrícula.",
      instruction = "Arrastra por las letras para marcar una palabra — horizontal, vertical o diagonal.",
      foundLabel = "Encontradas",
      won = "¡Las encontraste todas! 🔍",
      newGame = "Nueva sopa",
      easy = WordSearchVariant("Fácil", "8×8, 6 palabras, horizontal y vertical."),
      medium = WordSearchVariant("Medio", "11×11, 8 palabras, con diagonales."),
      hard = WordSearchVariant("Difícil", "13×13, 10 palabras, en todas direcciones.")
    ),
    guideRobot = GuideRobot(
      name = "Guía al robot",
      description = "Programa las flechas para llevar al robot a la meta.",
      instruction = "Toca las flechas para crear un camino y pulsa Marcha para enviar al robot.",
      instructionTurns = "Toca avanzar y los botones de giro para dirigir al robot, y pulsa Marcha.",
      programLabel = "Tu programa",
      emptyProgram = "Toca las flechas de abajo para añadir pasos.",
      run = "Marcha ▶",
      undo = "Deshacer",
      clear = "Borrar",
      starHint = "Pasa por la estrella ⭐ antes de llegar a la meta 🏁.",
      won = "¡El robot llegó! 🤖",
      crashed = "¡Pum! El robot chocó con un muro. Inténtalo de nuevo.",
      missed = "El robot no llegó a la meta. Inténtalo de nuevo.",
      missedStar = "¡Recoge la estrella ⭐ primero! Inténtalo de nuevo.",
      tryAgain = "Inténtalo de nuevo",
      newGame = "Nuevo reto",
      easy = GuideRobotVariant("Fácil", "5×5, camino libre a la meta."),
      medium = GuideRobotVariant("Medio", "6×6 con muros que esquivar."),
      hard = GuideRobotVariant("Difícil", "6×6 — coge la estrella y luego la meta.")
    ),
    wordBuilder = WordBuilder(
      name = "Lee y deletrea",
      description = "Forma palabras y elige la imagen que corresponde.",
      easy = WordBuilderLevel("Fácil", "Palabras cortas, sin letras extra."),
      medium = WordBuilderLevel("Medio", "Palabras más largas, algunas letras extra."),
      hard = WordBuilderLevel("Difícil", "Palabras largas con letras extra."),
      correct = "¡Muy bien!",
      nextWord = "Siguiente palabra",
      skip = "Saltar",
      printTitle = "Lee y deletrea — hoja",
      printHint = "Elige un nivel e imprime una hoja."
    ),
    mathPractice = MathPractice(
      name = "Práctica de matemáticas",
      description = "Contar, comparar, sumar y restar.",
      easy   = MathPracticeLevel("Fácil",   "Contar y comparar con imágenes, hasta 10."),
      medium = MathPracticeLevel("Medio",   "Sumar y restar hasta 10."),
      hard   = MathPracticeLevel("Difícil", "Hasta 20 con número que falta."),
      howMany = "¿Cuántos?",
      pickGroup = "Elige el grupo correcto",
      correct = "¡Muy bien!",
      nextProblem = "Siguiente",
      skip = "Saltar",
      printTitle = "Matemáticas — hoja",
      printHint = "Elige un nivel e imprime una hoja.",
      plus = "más",
      minus = "menos",
      equals = "es igual a",
      whatNumber = "qué número",
      compare = "es mayor o menor que"
    ),
    clock = Clock(
      name = "Reloj",
      description = "Aprende a leer la hora.",
      matchName = "Lee el reloj",
      matchDesc = "Relaciona analógico y digital, en cuartos de hora.",
      todName = "Parte del día",
      todDesc = "Nombra la parte del día según la hora.",
      formatLabel = "Reloj",
      format12 = "12 horas",
      format24 = "24 horas",
      whatTime = "¿Qué hora es?",
      pickClock = "Elige el reloj correcto",
      partOfDay = "¿Qué parte del día es?",
      morning = "Mañana",
      afternoon = "Tarde",
      evening = "Atardecer",
      night = "Noche",
      correct = "¡Muy bien!",
      next = "Siguiente",
      skip = "Saltar"
    ),
    reading = Reading(
      name = "Hora de leer",
      description = "Lee un clásico juntos.",
      hint = "A continuación encontrarás algunas inspiraciones.",
      search = "Buscar",
      freeEbook = "E-book",
      bandTots = "0–3 años",
      bandPicture = "3–6 años",
      bandChapter = "5–9 años",
      bandOlder = "8+ años"
    ),
    memoryChain = MemoryChain(
      name = "Tren de memoria",
      description = "Repite toda la lista y añade una cosa más.",
      hint = "Por turnos. Cada jugador repite en orden todo lo dicho y añade algo nuevo. Si rompes la cadena, quedas fuera.",
      newTheme = "Nuevo tema"
    ),
    iSpy = ISpy(
      name = "Veo veo",
      description = "Fíjate en algo cercano y da una pista.",
      hint = "Un juego de observación — mejor con cosas que todos podáis ver ahora.",
      howTitle = "Cómo se juega",
      step1 = "Un jugador elige en secreto algo que todos puedan ver.",
      step2 = "Da una sola pista, como «Veo algo rojo».",
      step3 = "Los demás adivinan en voz alta. Quien acierte primero elige lo siguiente.",
      tipsTitle = "Ideas de pistas",
      tip1 = "Por color, forma, tamaño o material.",
      tip2 = "O por la primera letra: «…algo que empieza por B»."
    )
  )

  val fr: Strings = Strings(
    freezeDance = FreezeDance(
      name = "Danse statue",
      description = "Danse sur la musique — et fige-toi quand elle s'arrête !",
      instruction = "Appuie sur Démarrer et danse. Quand la musique s'arrête, fige-toi ! Le dernier qui bouge passe son tour.",
      start = "Lancer la musique ▶",
      stop = "Arrêter",
      danceCue = "Danse ! 🕺",
      freezeCue = "Statue ! 🧊",
      srcSynth = "Intégrée",
      srcSongs = "Chansons",
      synthHint = "Un air joyeux et différent à chaque fois — sans réglage, fonctionne hors ligne.",
      songsHint = "Touche une chanson pour la charger — ou ajoute la tienne ci-dessous.",
      pasteLabel = "Ou colle un lien",
      linkPlaceholder = "Lien YouTube ou audio…",
      linkLoad = "Charger",
      linkInvalid = "Impossible de lire ce lien. Essaie un lien YouTube ou un lien .mp3 direct.",
      ytAdsNote = "Attention : YouTube peut afficher des publicités qui interrompent le jeu.",
      uploadLabel = "Ou utilise un fichier de cet appareil",
      uploadButton = "Choisir une chanson…",
      freeMusicLabel = "Pas de fichier ? Musique gratuite :",
      addOwn = "Utiliser un lien ou un fichier"
    ),
    hotPotato = HotPotato(
      name = "La patate chaude",
      description = "Fais-la passer vite — et ne te fais pas prendre avec quand la musique s'arrête !",
      instruction = "Faites passer la patate — n'importe quel petit objet mou — autour du cercle pendant que la musique joue. Elle s'arrête à un moment au hasard, et celui qui la tient à cet instant est pris. Touche Rejouer pour la manche suivante.",
      start = "Lancer la musique ▶",
      again = "Rejouer",
      stop = "Arrêter",
      passCue = "Fais-la passer ! 🥔",
      caughtCue = "Pris ! 💥"
    ),
    activeGames = ActiveGames(
      name = "Jeux de mouvement",
      description = "Des jeux classiques pour gigoter tout de suite — sans matériel, sans préparation.",
      lava = ActiveGameRules(
        name = "Le sol, c'est de la lave",
        blurb = "Quoi que tu fasses, ne touche pas le sol.",
        howTitle = "Comment jouer",
        steps = List(
          "Quelqu'un crie « le sol, c'est de la lave ! » — à partir de cette seconde, interdit de toucher le sol.",
          "Tout le monde se dépêche de lever les pieds : sur un canapé, un coussin, une chaise.",
          "Celui qui touche le sol est éliminé. Le dernier encore à l'abri gagne la manche."
        ),
        tipsTitle = "À ta façon",
        tips = List(
          "Mettez-vous d'accord d'abord : quels meubles sont sûrs et lesquels sont interdits.",
          "Disposez des coussins comme des pierres et traversez toute la pièce sans poser le pied par terre.",
          "Sécurité avant tout : écartez les coins pointus et ne grimpez sur rien qui puisse basculer."
        )
      ),
      tag = ActiveGameRules(
        name = "Le loup",
        blurb = "Un joueur est le loup et poursuit les autres.",
        howTitle = "Comment jouer",
        steps = List(
          "Choisissez qui est le loup — un petit décompte ou un « c'est pas moi ! » général tranche.",
          "Le loup poursuit les autres et essaie d'en toucher un.",
          "Celui qui est touché devient le nouveau loup. Fixez les limites du terrain avant de commencer."
        ),
        tipsTitle = "Variantes amusantes",
        tips = List(
          "Loup glacé : le joueur touché est figé jusqu'à ce qu'un camarade libre passe sous ses jambes.",
          "Loup chaîne : chaque joueur touché donne la main au loup, et la chaîne s'allonge.",
          "Loup ombre : au lieu de toucher, le loup marche sur ton ombre — idéal en plein soleil."
        )
      ),
      hideSeek = ActiveGameRules(
        name = "Cache-cache",
        blurb = "Un chercheur compte pendant que les autres se cachent.",
        howTitle = "Comment jouer",
        steps = List(
          "Un joueur est le chercheur : il se cache les yeux et compte jusqu'à vingt à voix haute.",
          "Les autres se cachent pendant le décompte.",
          "« Prêts ou pas, j'arrive ! » — le chercheur cherche jusqu'à trouver tout le monde."
        ),
        tipsTitle = "Variantes amusantes",
        tips = List(
          "Sardine : une seule personne se cache et tous les autres cherchent — quand tu la trouves, glisse-toi à côté en douce.",
          "Convenez où l'on a le droit de se cacher, pour que personne n'attende des heures dans un coin que personne ne fouille.",
          "Le dernier trouvé devient le prochain chercheur."
        )
      ),
      redLight = ActiveGameRules(
        name = "Un, deux, trois, soleil",
        blurb = "Avance en douce — et fige-toi à « soleil ».",
        howTitle = "Comment jouer",
        steps = List(
          "Un joueur est le meneur et se place au fond, dos tourné.",
          "Il dit « un, deux, trois… soleil ! » puis se retourne d'un coup ; pendant le décompte, les autres avancent.",
          "Celui qui bouge encore à « soleil » retourne au départ. Le premier à toucher le meneur gagne."
        ),
        tipsTitle = "À ta façon",
        tips = List(
          "Le meneur peut jouer avec le rythme — compter lentement, puis se retourner d'un coup pour piéger les pieds pressés.",
          "Essayez au ralenti, ou à cloche-pied, pour rire encore plus.",
          "Le mieux, c'est beaucoup d'espace dehors, mais un long couloir fait l'affaire."
        )
      )
    ),
    appTitle = "Tandu",
    tagline = "Votre temps ensemble",
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
      suggestActivity = "Proposer!",
      suggestAnother = "Une autre idée",
      spinning = "Tirage…",
      tools = "Outils",
      activities = "Activités",
      installApp = "Installer l'app"
    ),
    filters = Filters(
      games = "Jeux",
      move = "Bouger",
      learn = "Apprendre",
      solo = "Solo",
      two = "Deux",
      group = "3+",
      onTheGo = "En route",
      favourites = "Favoris",
      addToFavourites = "Ajouter aux favoris",
      removeFromFavourites = "Retirer des favoris",
      noFavouritesYet = "Touchez l'étoile sur une activité pour l'enregistrer comme favorite.",
      searchPlaceholder = "Rechercher une activité…",
      noMatches = "Aucune activité ne correspond à votre recherche.",
      hide = "Masquer l'activité",
      unhide = "Afficher l'activité",
      showHidden = "Masquées"
    ),
    about = About(
      open = "À propos",
      title = "À propos de Tandu",
      body = "Tandu est un petit assistant pour choisir une activité amusante avec les enfants. Appuyez sur « Proposer une activité » pour une idée au hasard, ou parcourez la liste. Certains jeux se jouent dans l'application, d'autres sont des idées à faire hors-écran — parfaits pour la voiture, le canapé ou un après-midi pluvieux.",
      privacy = "Politique de confidentialité"
    ),
    installHelp = InstallHelp(
      title = "Ajoutez Tandu à l'écran d'accueil",
      body = "Sur iPhone ou iPad, ouvrez cette page dans Safari (les autres navigateurs ne peuvent pas l'installer), appuyez sur le bouton Partager en bas, puis choisissez « Sur l'écran d'accueil ». Tandu s'ouvrira comme une vraie application, en plein écran et utilisable hors ligne."
    ),
    menu = Menu(
      open = "Menu",
      feedback = "Envoyer un avis",
      language = "Langue",
      readAloud = "Lecture à voix haute"
    ),
    mode = Mode(
      choose = "Comment voulez-vous jouer ?",
      inApp = "Jouer dans l'app",
      offline = "Jouer hors ligne",
      lichess = "Jouer en ligne",
      external = "Ouvrir en ligne",
      p2p = "Jouer à deux appareils",
      experimentalBadge = "Expérimental",
      experimentalWarning = "Ce mode est expérimental et peut ne pas se connecter de façon fiable."
    ),
    printable = Printable(
      print = "Imprimer",
      printMaps = "Imprimer les plateaux",
      printRules = "Imprimer les règles"
    ),
    timer = Timer(
      name = "Minuteur",
      description = "Choisis une durée et lance le compte à rebours, bip à la fin",
      start = "Démarrer",
      pause = "Pause",
      restart = "Redémarrer"
    ),
    offline = Offline(
      materials = Materials(
        paperPen = "papier + stylo",
        printer = "imprimante",
        scissors = "ciseaux",
        laminatorOptional = "plastifieuse (optionnel, pour réutiliser)",
        deck52 = "jeu de 52 cartes",
        chessBoard = "échiquier + pièces",
        checkersBoard = "damier + pions",
        board = "une surface plane",
        none = "rien — juste de l'imagination"
      ),
      battleships = BattleshipsOff(
        printTitle = "Bataille navale — plateaux pour deux",
        ownLabel = "Ma flotte",
        enemyLabel = "Eaux ennemies",
        fleetTitle = "Flotte (variante)",
        fleetLine = "1 × 4 cases · 2 × 3 cases · 3 × 2 cases · 4 × 1 case",
        rules = Rules("Comment jouer", List(
          "Placez votre flotte en secret sur « Ma flotte ». Les bateaux sont à l'horizontale ou à la verticale ; ils ne doivent pas se toucher, même en diagonale.",
          "À tour de rôle, annoncez des coordonnées (lettre + chiffre). Notez les touches × et les manqués · sur « Eaux ennemies ».",
          "Sur une touche, dites « touché » ; quand le bateau entier tombe, dites « coulé ».",
          "Le premier qui coule toute la flotte adverse gagne."
        )),
        modeAsk = "Comment voulez-vous jouer à la bataille navale ?"
      ),
      memory = MemoryOff(
        printTitle = "Memory — découpez les cartes",
        cutHint = "Imprimez la feuille, découpez le long des lignes, mélangez face cachée et jouez.",
        rules = Rules("Comment jouer", List(
          "Mélangez les cartes et étalez-les face cachée en grille.",
          "Chacun son tour. Retournez deux cartes : si elles sont identiques, gardez-les et rejouez ; sinon, retournez-les.",
          "Celui qui a le plus de paires quand la table est vide gagne."
        ))
      ),
      hangman = HangmanOff(
        keeperTitle = "Mot pour celui qui tient le téléphone",
        keeperHint = "Tu tiens le téléphone, tu regardes le mot puis tu le caches. Les autres devinent des lettres et vous dessinez le pendu sur papier.",
        reveal = "Voir le mot",
        hide = "Cacher le mot",
        tap = "Touche pour voir",
        categorySetting = "Catégorie",
        categoryAny = "N'importe quel mot",
        categoryAnimals = "Animaux",
        categoryFoods = "Aliments",
        categoryCountries = "Pays",
        drawHint = "À chaque lettre fausse, dessine une partie du pendu.",
        rules = Rules("Comment jouer", List(
          "Quelqu'un choisit un mot (ou le téléphone) et trace des tirets sur papier — un par lettre.",
          "Les autres proposent des lettres une par une. Les bonnes vont sur les tirets ; les mauvaises sont notées à côté.",
          "Chaque mauvaise lettre ajoute une partie au dessin.",
          "Vous gagnez si vous complétez le mot avant la fin du dessin ; sinon vous perdez."
        )),
        gallowsTitle = "Comment dessiner la potence"
      ),
      chess = ChessOff(
        rules = Rules("Comment jouer", List(
          "Deux joueurs. Les blancs commencent ; un coup chacun à tour de rôle.",
          "Vous gagnez en faisant échec et mat au roi adverse.",
          "Un roi attaqué est « en échec » — le coup suivant doit le sortir de l'échec.",
          "Pas de coup légal et pas d'échec : partie nulle (pat)."
        )),
        pieces = Rules("Comment les pièces se déplacent", List(
          "Pion : une case en avant ; deux depuis sa case de départ. Prend en diagonale une case.",
          "Cavalier : en L — deux puis une, saute par-dessus.",
          "Fou : n'importe quel nombre de cases en diagonale.",
          "Tour : n'importe quel nombre de cases en ligne droite.",
          "Dame : n'importe quel nombre de cases dans toute direction.",
          "Roi : une case dans toute direction."
        )),
        specials = Rules("Coups spéciaux", List(
          "Roque : le roi avance de deux cases vers une tour ; la tour saute de l'autre côté. Possible si aucun n'a bougé, aucune pièce entre eux et le roi ne traverse pas l'échec.",
          "Prise en passant : un pion qui vient d'avancer de deux cases peut être pris par un pion adverse contigu — uniquement au coup suivant.",
          "Promotion : un pion qui atteint la dernière rangée devient dame (ou tour/fou/cavalier)."
        )),
        printTitle = "Échecs — comment les pièces se déplacent",
        lichessLabel = "Ouvrir Lichess"
      ),
      ticTacToe = TicTacToeOff(
        rules = Rules("Comment jouer", List(
          "Dessinez une grille 3×3 sur papier.",
          "À tour de rôle, marquez X ou O sur une case vide.",
          "Le premier à aligner trois symboles — ligne, colonne ou diagonale — gagne."
        )),
        gomokuTipTitle = "Vous maîtrisez ?",
        gomokuTip = "Essayez gomoku : grille plus grande, cinq en ligne. Même idée, beaucoup plus intéressant."
      ),
      solitaire = SolitaireOff(
        rules = Rules("Klondike — comment jouer", List(
          "Distribuez sept colonnes : 1, 2, 3, 4, 5, 6, 7 cartes. La carte du haut face visible.",
          "Le reste forme la pioche ; retournez une (ou trois) carte vers la défausse.",
          "Construisez sur le tableau en descendant et en couleurs alternées. Déplacez les suites en bloc. Une colonne vide accepte un roi.",
          "Construisez les fondations par couleur, de l'as au roi. Vous gagnez quand les quatre sont complètes."
        )),
        setupExample = "Disposition initiale"
      ),
      categories = CategoriesOff(
        printTitle = "Feuille de Petit Bac",
        categoriesLabel = "Catégorie",
        lettersLabel = "Lettre",
        scoresLabel = "Score",
        curatedNote = "Trois manches, une lettre par colonne. Remplissez un mot par catégorie qui commence par cette lettre.",
        rules = Rules("Comment jouer", List(
          "Chaque colonne a une lettre en haut — c'est la lettre de la manche.",
          "Lancez un minuteur (environ 3 minutes). Chacun essaie d'écrire un mot par catégorie qui commence par cette lettre.",
          "À la fin, comparez : un point par réponse unique ; si deux joueurs ont le même mot, personne ne marque.",
          "Jouez les trois colonnes. Le plus grand total gagne."
        ))
      ),
      checkers = CheckersOff(
        rules = Rules("Comment jouer", List(
          "Chaque joueur a 12 pions sur les cases sombres des trois rangées du fond.",
          "Avancez en diagonale vers une case sombre vide. Capturez en sautant un pion adverse adjacent vers la case vide derrière.",
          "Si une prise est possible, elle est obligatoire ; les prises multiples continuent dans le même tour.",
          "Au dernier rang, le pion est couronné — la dame se déplace et capture dans les deux sens.",
          "Vous gagnez en capturant tous les pions adverses ou en les bloquant."
        )),
        lichessLabel = "Ouvrir lidraughts"
      ),
      sudoku = SudokuOff(
        printTitle = "Sudoku — six grilles",
        sheetHint = "Imprimez la feuille, prenez un stylo et résolvez. Pas de solutions — ça fait partie du jeu.",
        rules = Rules("Comment jouer", List(
          "Remplissez la grille pour que chaque ligne, chaque colonne et chaque carré 3×3 contienne chaque chiffre de 1 à 9 exactement une fois.",
          "Les chiffres donnés ne changent pas. Notez les candidats en petit quand vous hésitez.",
          "Procédez par élimination : si une case n'accepte qu'un seul chiffre, c'est la réponse."
        ))
      ),
      maze = MazeOff(
        printTitle = "Labyrinthes — aide la souris à trouver le fromage",
        sheetHint = "Imprimez la feuille et tracez au crayon un chemin de la souris au fromage. Un seul passage par labyrinthe.",
        rules = Rules("Comment jouer", List(
          "Pars de la souris 🐭 et trouve le chemin jusqu'au fromage 🧀.",
          "On ne traverse pas les murs — suis seulement les couloirs ouverts.",
          "Cul-de-sac ? Reviens en arrière et essaie un autre tournant."
        ))
      ),
      wordSearch = WordSearchOff(
        printTitle = "Mots mêlés — trouve les mots cachés",
        sheetHint = "Imprimez la feuille et entourez chaque mot trouvé. La liste est sous la grille.",
        wordsLabel = "Trouve ces mots",
        rules = Rules("Comment jouer", List(
          "Chaque mot de la liste est caché dans la grille de lettres.",
          "Les mots vont en ligne droite — à l'horizontale, à la verticale ou en diagonale, parfois à l'envers.",
          "Entoure chaque mot trouvé et raye-le de la liste."
        ))
      ),
      guideRobot = GuideRobotOff(
        printTitle = "Guide le robot — écris le chemin en flèches",
        sheetHint = "Imprimez la feuille et écrivez les flèches qui mènent chaque robot à son arrivée.",
        writeLabel = "Écris les flèches",
        rules = Rules("Comment jouer", List(
          "Le robot 🤖 démarre sur la grille et doit atteindre l'arrivée 🏁.",
          "Sur la feuille facile, écris des flèches (↑ ↓ ← →) — une case par flèche.",
          "Sur les feuilles difficiles le robot tourne : ↑ avancer, ↺ tourner à gauche, ↻ tourner à droite.",
          "Les murs 🧱 bloquent le passage, alors contourne-les.",
          "Sur les feuilles difficiles, passe sur l'étoile ⭐ avant l'arrivée."
        ))
      )
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
      endTurn = "Fin du tour",
      p2p = BattleshipsP2P(
        title = "Jouer à deux appareils",
        intro = "Connectez deux téléphones pour jouer avec des plateaux cachés. Un appareil crée la partie, l'autre rejoint avec le même code.",
        create = "Créer une partie",
        join = "Rejoindre",
        connect = "Connecter",
        shareCode = "Partagez ce code avec l'autre appareil",
        enterCode = "Saisissez le code de l'autre appareil",
        waiting = "En attente de l'autre appareil…",
        yourTurn = "Votre tour",
        opponentTurn = "Tour de l'adversaire",
        waitingShot = "En attente de son tir…",
        waitingResolve = "En attente du résultat…",
        youWin = "Vous gagnez !",
        youLose = "Vous perdez."
      )
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
      next = "Nouvelle manche",
      startingWith = "commençant par la lettre"
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
    ),
    sudoku = Sudoku(
      name = "Sudoku",
      description = "Remplissez la grille 9×9 avec 1–9 sur chaque ligne, colonne et carré.",
      chooseVariant = "Choisir la difficulté",
      changeVariant = "Changer la difficulté",
      newGame = "Nouvelle partie",
      undo = "Annuler",
      pencil = "Crayon",
      easy = SudokuVariant("Facile", "Plus d'indices — un démarrage doux."),
      medium = SudokuVariant("Moyen", "Moins d'indices — un vrai défi."),
      hard = SudokuVariant("Difficile", "Peu d'indices — pour les experts.")
    ),
    minesweeper = Minesweeper(
      name = "Démineur",
      description = "Découvrez le plateau sans toucher de mine.",
      chooseVariant = "Choisir la taille",
      changeVariant = "Changer la taille",
      newGame = "Nouvelle partie",
      playing = "Attention aux mines…",
      youWon = "Déminé !",
      youLost = "Boum !",
      revealMode = "Révéler",
      flagMode = "Drapeau",
      easy = MinesweeperVariant("Facile", "9×9 avec 10 mines."),
      medium = MinesweeperVariant("Moyen", "12×12 avec 25 mines."),
      hard = MinesweeperVariant("Difficile", "16×12 avec 40 mines.")
    ),
    maze = Maze(
      name = "Labyrinthe",
      description = "Aide la souris à trouver le fromage.",
      instruction = "Balaie ou glisse pour mener la souris au fromage — les flèches marchent aussi.",
      won = "Tu as trouvé le fromage ! 🧀",
      clearPath = "Effacer le chemin",
      newGame = "Nouveau labyrinthe",
      easy = MazeVariant("Facile", "10×10 avec des boucles douces."),
      medium = MazeVariant("Moyen", "16×16, un seul passage."),
      hard = MazeVariant("Difficile", "24×24, un vrai labyrinthe.")
    ),
    wordSearch = WordSearch(
      name = "Mots mêlés",
      description = "Trouve les mots cachés dans la grille.",
      instruction = "Glisse sur les lettres pour tracer un mot — à l'horizontale, à la verticale ou en diagonale.",
      foundLabel = "Trouvés",
      won = "Tu les as tous trouvés ! 🔍",
      newGame = "Nouvelle grille",
      easy = WordSearchVariant("Facile", "8×8, 6 mots, horizontal et vertical."),
      medium = WordSearchVariant("Moyen", "11×11, 8 mots, avec diagonales."),
      hard = WordSearchVariant("Difficile", "13×13, 10 mots, dans tous les sens.")
    ),
    guideRobot = GuideRobot(
      name = "Guide le robot",
      description = "Programme les flèches pour conduire le robot à l'arrivée.",
      instruction = "Touche les flèches pour tracer un chemin, puis appuie sur Go pour lancer le robot.",
      instructionTurns = "Touche avancer et les boutons de rotation pour diriger le robot, puis appuie sur Go.",
      programLabel = "Ton programme",
      emptyProgram = "Touche les flèches ci-dessous pour ajouter des pas.",
      run = "Go ▶",
      undo = "Annuler",
      clear = "Effacer",
      starHint = "Passe sur l'étoile ⭐ avant d'atteindre l'arrivée 🏁.",
      won = "Le robot est arrivé ! 🤖",
      crashed = "Boum ! Le robot a heurté un mur. Réessaie.",
      missed = "Le robot n'a pas atteint l'arrivée. Réessaie.",
      missedStar = "Attrape d'abord l'étoile ⭐ ! Réessaie.",
      tryAgain = "Réessayer",
      newGame = "Nouveau défi",
      easy = GuideRobotVariant("Facile", "5×5, chemin libre vers l'arrivée."),
      medium = GuideRobotVariant("Moyen", "6×6 avec des murs à contourner."),
      hard = GuideRobotVariant("Difficile", "6×6 — attrape l'étoile, puis l'arrivée.")
    ),
    wordBuilder = WordBuilder(
      name = "Lis et écris",
      description = "Forme des mots et choisis l'image qui correspond.",
      easy = WordBuilderLevel("Facile", "Mots courts, pas de lettres en plus."),
      medium = WordBuilderLevel("Moyen", "Mots plus longs, quelques lettres en plus."),
      hard = WordBuilderLevel("Difficile", "Mots longs avec lettres en plus."),
      correct = "Bravo !",
      nextWord = "Mot suivant",
      skip = "Passer",
      printTitle = "Lis et écris — fiche",
      printHint = "Choisis un niveau et imprime une fiche."
    ),
    mathPractice = MathPractice(
      name = "Maths",
      description = "Compter, comparer, additionner et soustraire.",
      easy   = MathPracticeLevel("Facile",    "Compter et comparer avec des images, jusqu'à 10."),
      medium = MathPracticeLevel("Moyen",     "Additions et soustractions jusqu'à 10."),
      hard   = MathPracticeLevel("Difficile", "Jusqu'à 20 avec nombre manquant."),
      howMany = "Combien ?",
      pickGroup = "Choisis le bon groupe",
      correct = "Bravo !",
      nextProblem = "Suivant",
      skip = "Passer",
      printTitle = "Maths — fiche",
      printHint = "Choisis un niveau et imprime une fiche.",
      plus = "plus",
      minus = "moins",
      equals = "égale",
      whatNumber = "quel nombre",
      compare = "est plus grand ou plus petit que"
    ),
    clock = Clock(
      name = "Horloge",
      description = "Apprends à lire l'heure.",
      matchName = "Lis l'horloge",
      matchDesc = "Associe l'analogique et le numérique, au quart d'heure.",
      todName = "Moment de la journée",
      todDesc = "Nomme le moment de la journée d'après l'heure.",
      formatLabel = "Horloge",
      format12 = "12 heures",
      format24 = "24 heures",
      whatTime = "Quelle heure est-il ?",
      pickClock = "Choisis la bonne horloge",
      partOfDay = "Quel moment de la journée ?",
      morning = "Matin",
      afternoon = "Après-midi",
      evening = "Soir",
      night = "Nuit",
      correct = "Bravo !",
      next = "Suivant",
      skip = "Passer"
    ),
    reading = Reading(
      name = "L'heure de la lecture",
      description = "Lisez un classique ensemble.",
      hint = "Ci-dessous, quelques idées pour vous inspirer.",
      search = "Rechercher",
      freeEbook = "E-book",
      bandTots = "0–3 ans",
      bandPicture = "3–6 ans",
      bandChapter = "5–9 ans",
      bandOlder = "8+ ans"
    ),
    memoryChain = MemoryChain(
      name = "Le train de la mémoire",
      description = "Répète toute la liste, puis ajoute un élément.",
      hint = "Chacun son tour. Chaque joueur répète tout ce qui a été dit, dans l'ordre, puis ajoute une nouvelle chose. Casse la chaîne et tu es éliminé.",
      newTheme = "Nouveau thème"
    ),
    iSpy = ISpy(
      name = "Je vois, je vois",
      description = "Repère un objet autour de toi et donne un indice.",
      hint = "Un jeu d'observation — idéal avec des choses que vous voyez tous maintenant.",
      howTitle = "Comment jouer",
      step1 = "Un joueur choisit en secret une chose que tout le monde peut voir.",
      step2 = "Il donne un seul indice, comme « Je vois quelque chose de rouge ».",
      step3 = "Les autres devinent à voix haute. Le premier qui trouve choisit ensuite.",
      tipsTitle = "Idées d'indices",
      tip1 = "Par couleur, forme, taille ou matière.",
      tip2 = "Ou par première lettre : « …quelque chose qui commence par B »."
    )
  )

  val de: Strings = Strings(
    freezeDance = FreezeDance(
      name = "Stopptanz",
      description = "Tanz zur Musik — und friere ein, wenn sie stoppt!",
      instruction = "Drücke Start und tanze. Wenn die Musik stoppt, einfrieren! Wer sich zuletzt bewegt, setzt eine Runde aus.",
      start = "Musik starten ▶",
      stop = "Stopp",
      danceCue = "Tanzen! 🕺",
      freezeCue = "Einfrieren! 🧊",
      srcSynth = "Eingebaut",
      srcSongs = "Lieder",
      synthHint = "Jedes Mal eine neue, fröhliche Melodie — ohne Einrichtung, auch offline.",
      songsHint = "Tippe auf ein Lied, um es zu laden — oder füge unten dein eigenes hinzu.",
      pasteLabel = "Oder füge einen Link ein",
      linkPlaceholder = "YouTube- oder Audio-Link…",
      linkLoad = "Laden",
      linkInvalid = "Link konnte nicht gelesen werden. Versuche einen YouTube-Link oder einen direkten .mp3-Link.",
      ytAdsNote = "Hinweis: YouTube kann Werbung zeigen, die das Spiel unterbricht.",
      uploadLabel = "Oder nutze eine Datei von diesem Gerät",
      uploadButton = "Lied auswählen…",
      freeMusicLabel = "Keine Datei? Kostenlose Musik:",
      addOwn = "Link oder Datei verwenden"
    ),
    hotPotato = HotPotato(
      name = "Heiße Kartoffel",
      description = "Gib sie schnell weiter — und lass dich nicht damit erwischen, wenn die Musik stoppt!",
      instruction = "Reicht die Kartoffel — irgendeinen kleinen, weichen Gegenstand — im Kreis herum, solange die Musik spielt. Sie stoppt in einem zufälligen Moment, und wer sie dann hält, ist erwischt. Tippe auf „Nochmal spielen“ für die nächste Runde.",
      start = "Musik starten ▶",
      again = "Nochmal spielen",
      stop = "Stopp",
      passCue = "Weitergeben! 🥔",
      caughtCue = "Erwischt! 💥"
    ),
    activeGames = ActiveGames(
      name = "Bewegungsspiele",
      description = "Klassische Lauf- und Tobespiele für sofort — ohne Material, ohne Aufbau.",
      lava = ActiveGameRules(
        name = "Der Boden ist Lava",
        blurb = "Was du auch tust — berühr nicht den Boden.",
        howTitle = "So wird gespielt",
        steps = List(
          "Jemand ruft „der Boden ist Lava!“ — ab dieser Sekunde ist der Boden tabu.",
          "Alle bringen schnell ihre Füße vom Boden weg: aufs Sofa, ein Kissen, einen Stuhl.",
          "Wer den Boden berührt, scheidet aus. Wer als Letzter in Sicherheit bleibt, gewinnt die Runde."
        ),
        tipsTitle = "Macht es zu eurem Spiel",
        tips = List(
          "Legt vorher fest, welche Möbel sicher sind und was tabu ist.",
          "Verteilt Kissen als Trittsteine und versucht, durchs ganze Zimmer zu kommen, ohne aufzutreten.",
          "Sicherheit zuerst: räumt scharfe Ecken weg und klettert auf nichts, was umkippen kann."
        )
      ),
      tag = ActiveGameRules(
        name = "Fangen",
        blurb = "Einer ist der Fänger und jagt die anderen.",
        howTitle = "So wird gespielt",
        steps = List(
          "Wählt, wer fängt — ein kurzes Abzählen oder ein lautes „nicht ich!“ entscheidet.",
          "Der Fänger jagt die anderen und versucht, jemanden abzuschlagen.",
          "Wer abgeschlagen wird, ist der neue Fänger. Legt vorher die Grenzen des Spielfelds fest."
        ),
        tipsTitle = "Lustige Varianten",
        tips = List(
          "Versteinerungs-Fangen: Wer abgeschlagen ist, erstarrt, bis ein freier Mitspieler unter seinen Beinen durchkriecht.",
          "Ketten-Fangen: Jeder Gefangene fasst den Fänger an der Hand, und die Kette wird länger.",
          "Schatten-Fangen: Statt zu berühren, tritt der Fänger auf deinen Schatten — am besten bei Sonne."
        )
      ),
      hideSeek = ActiveGameRules(
        name = "Verstecken",
        blurb = "Einer sucht, alle anderen verstecken sich.",
        howTitle = "So wird gespielt",
        steps = List(
          "Eine Person ist der Sucher: Sie hält sich die Augen zu und zählt laut bis zwanzig.",
          "Alle anderen verstecken sich währenddessen.",
          "„Ich komme!“ — der Sucher sucht, bis er alle gefunden hat."
        ),
        tipsTitle = "Lustige Varianten",
        tips = List(
          "Sardinen: Nur einer versteckt sich, alle anderen suchen — wer ihn findet, quetscht sich heimlich dazu.",
          "Macht aus, wo man sich verstecken darf, damit niemand ewig in einer Ecke wartet, die keiner absucht.",
          "Wer zuletzt gefunden wird, sucht in der nächsten Runde."
        )
      ),
      redLight = ActiveGameRules(
        name = "Ochs am Berg",
        blurb = "Schleich dich nach vorn — und erstarre, wenn er sich umdreht.",
        howTitle = "So wird gespielt",
        steps = List(
          "Eine Person ist der Rufer und steht am anderen Ende, mit dem Rücken zu den anderen.",
          "Beim Spruch „eins, zwei, drei — Ochs am Berg!“ schleichen alle vor; danach dreht sich der Rufer blitzschnell um.",
          "Wer sich dann noch bewegt, muss zurück zum Start. Wer den Rufer zuerst berührt, gewinnt."
        ),
        tipsTitle = "Macht es zu eurem Spiel",
        tips = List(
          "Der Rufer kann mit dem Tempo spielen — langsam sprechen und sich dann plötzlich umdrehen, um hektische Füße zu erwischen.",
          "Probiert es in Zeitlupe oder auf einem Bein — das gibt noch mehr Gelächter.",
          "Am besten viel Platz draußen, aber ein langer Flur tut's auch."
        )
      )
    ),
    appTitle = "Tandu",
    tagline = "Eure gemeinsame Zeit",
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
      suggestActivity = "Vorschlagen!",
      suggestAnother = "Andere Idee",
      spinning = "Wird gewählt…",
      tools = "Werkzeuge",
      activities = "Aktivitäten",
      installApp = "App installieren"
    ),
    filters = Filters(
      games = "Spiele",
      move = "Bewegung",
      learn = "Lernen",
      solo = "Solo",
      two = "Zwei",
      group = "3+",
      onTheGo = "Unterwegs",
      favourites = "Favoriten",
      addToFavourites = "Zu Favoriten hinzufügen",
      removeFromFavourites = "Aus Favoriten entfernen",
      noFavouritesYet = "Tippe auf den Stern bei einer Aktivität, um sie als Favorit zu speichern.",
      searchPlaceholder = "Aktivitäten suchen…",
      noMatches = "Keine Aktivitäten passen zu deiner Suche.",
      hide = "Aktivität ausblenden",
      unhide = "Aktivität anzeigen",
      showHidden = "Ausgeblendet"
    ),
    about = About(
      open = "Über",
      title = "Über Tandu",
      body = "Tandu ist ein kleiner Helfer, um etwas Lustiges mit den Kindern auszusuchen. Tippe auf „Aktivität vorschlagen\" für eine zufällige Idee oder stöbere in der Liste. Manche Spiele kannst du in der App spielen, andere sind Anregungen für Offline-Spaß — perfekt fürs Auto, das Sofa oder einen verregneten Nachmittag.",
      privacy = "Datenschutz"
    ),
    installHelp = InstallHelp(
      title = "Tandu zum Home-Bildschirm hinzufügen",
      body = "Öffne diese Seite auf iPhone oder iPad in Safari (andere Browser können sie nicht installieren), tippe unten auf „Teilen\" und wähle dann „Zum Home-Bildschirm\". Tandu öffnet sich danach wie eine normale App — im Vollbild und offline nutzbar."
    ),
    menu = Menu(
      open = "Menü",
      feedback = "Feedback senden",
      language = "Sprache",
      readAloud = "Vorlesen"
    ),
    mode = Mode(
      choose = "Wie möchtet ihr spielen?",
      inApp = "In der App spielen",
      offline = "Offline spielen",
      lichess = "Online spielen",
      external = "Online öffnen",
      p2p = "Auf zwei Geräten spielen",
      experimentalBadge = "Experimentell",
      experimentalWarning = "Dieser Modus ist experimentell und verbindet sich möglicherweise nicht zuverlässig."
    ),
    printable = Printable(
      print = "Drucken",
      printMaps = "Felder drucken",
      printRules = "Regeln drucken"
    ),
    timer = Timer(
      name = "Stoppuhr",
      description = "Wähle eine Dauer und zähle herunter, am Ende ertönt ein Ton",
      start = "Start",
      pause = "Pause",
      restart = "Neustart"
    ),
    offline = Offline(
      materials = Materials(
        paperPen = "Papier + Stift",
        printer = "Drucker",
        scissors = "Schere",
        laminatorOptional = "Laminiergerät (optional, zur Wiederverwendung)",
        deck52 = "52er-Kartendeck",
        chessBoard = "Schachbrett + Figuren",
        checkersBoard = "Damebrett + Steine",
        board = "eine ebene Fläche",
        none = "nichts — nur Fantasie"
      ),
      battleships = BattleshipsOff(
        printTitle = "Schiffe versenken — Bretter für zwei",
        ownLabel = "Meine Flotte",
        enemyLabel = "Gegnerische Gewässer",
        fleetTitle = "Flotte (Variante)",
        fleetLine = "1 × 4 Felder · 2 × 3 Felder · 3 × 2 Felder · 4 × 1 Feld",
        rules = Rules("So spielt man", List(
          "Platziert eure Flotte heimlich auf „Meine Flotte\". Schiffe stehen waagerecht oder senkrecht; sie dürfen sich nicht berühren, auch nicht diagonal.",
          "Reihum ruft ihr Koordinaten (Buchstabe + Zahl). Markiert Treffer mit × und Fehlschüsse mit · auf „Gegnerische Gewässer\".",
          "Sag bei einem Treffer „Treffer\"; sobald das ganze Schiff fällt, sag „versenkt\".",
          "Wer zuerst die gesamte Flotte versenkt, gewinnt."
        )),
        modeAsk = "Wie wollt ihr Schiffe versenken spielen?"
      ),
      memory = MemoryOff(
        printTitle = "Memory — Karten ausschneiden",
        cutHint = "Blatt drucken, an den Linien ausschneiden, verdeckt mischen und spielen.",
        rules = Rules("So spielt man", List(
          "Karten mischen und verdeckt in einem Raster auslegen.",
          "Reihum: zwei Karten aufdecken. Passen sie, behalten und noch einmal; sonst wieder umdrehen.",
          "Wer am Ende die meisten Paare hat, gewinnt."
        ))
      ),
      hangman = HangmanOff(
        keeperTitle = "Wort für den, der das Handy hält",
        keeperHint = "Du hältst das Handy, schaust das Wort an und versteckst es. Die anderen raten Buchstaben und ihr zeichnet den Galgen auf Papier.",
        reveal = "Wort zeigen",
        hide = "Wort verstecken",
        tap = "Tippen zum Anzeigen",
        categorySetting = "Kategorie",
        categoryAny = "Beliebiges Wort",
        categoryAnimals = "Tiere",
        categoryFoods = "Essen",
        categoryCountries = "Länder",
        drawHint = "Pro falschem Buchstaben einen Körperteil ergänzen.",
        rules = Rules("So spielt man", List(
          "Jemand denkt sich ein Wort aus (oder das Handy wählt eins) und malt Striche auf Papier — einen pro Buchstabe.",
          "Die anderen raten Buchstaben. Treffer kommen auf die Striche; Fehler an den Rand.",
          "Jeder Fehler ergänzt einen Körperteil am Galgen.",
          "Ihr gewinnt, wenn das Wort vor der Figur fertig ist; sonst Niederlage."
        )),
        gallowsTitle = "Wie man den Galgen zeichnet"
      ),
      chess = ChessOff(
        rules = Rules("So spielt man", List(
          "Zwei Spieler. Weiß zieht zuerst; abwechselnd ein Zug.",
          "Du gewinnst durch Schachmatt am gegnerischen König.",
          "Ein angegriffener König steht „im Schach\" — der nächste Zug muss das Schach aufheben.",
          "Keine legalen Züge und kein Schach: Patt (Remis)."
        )),
        pieces = Rules("Wie die Figuren ziehen", List(
          "Bauer: ein Feld vor; zwei vom Startfeld. Schlägt diagonal ein Feld.",
          "Springer: L-Form — zwei und eins, überspringt Figuren.",
          "Läufer: beliebig viele Felder diagonal.",
          "Turm: beliebig viele Felder gerade.",
          "Dame: beliebig viele Felder in jede Richtung.",
          "König: ein Feld in jede Richtung."
        )),
        specials = Rules("Sonderzüge", List(
          "Rochade: König zieht zwei Felder zu einem Turm, der Turm springt auf die andere Seite. Erlaubt, wenn keine der Figuren gezogen hat, dazwischen frei ist und der König nicht durch ein Schachfeld läuft.",
          "En passant: ein Bauer, der gerade zwei Felder gezogen hat, kann von einem benachbarten gegnerischen Bauern geschlagen werden — nur im nächsten Zug.",
          "Umwandlung: ein Bauer auf der letzten Reihe wird Dame (oder Turm/Läufer/Springer)."
        )),
        printTitle = "Schach — wie die Figuren ziehen",
        lichessLabel = "Lichess öffnen"
      ),
      ticTacToe = TicTacToeOff(
        rules = Rules("So spielt man", List(
          "Zeichnet ein 3×3-Gitter auf Papier.",
          "Reihum X oder O in ein leeres Feld setzen.",
          "Wer zuerst drei in einer Reihe hat — waagerecht, senkrecht oder diagonal — gewinnt."
        )),
        gomokuTipTitle = "Schon zu einfach?",
        gomokuTip = "Probiert Gomoku: größeres Gitter, fünf in einer Reihe. Gleiche Idee, viel spannender."
      ),
      solitaire = SolitaireOff(
        rules = Rules("Klondike — so spielt man", List(
          "Sieben Tableau-Spalten austeilen: 1, 2, 3, 4, 5, 6, 7 Karten. Oberste Karte offen.",
          "Der Rest ist der Stock; eine (oder drei) Karte auf den Talon umdrehen.",
          "Auf dem Tableau absteigend in wechselnden Farben aufbauen. Sequenzen am Stück bewegen. Leere Spalten beginnt ein König.",
          "Die Fundamente pro Farbe aufsteigend vom Ass bis König. Gewonnen, wenn alle vier voll sind."
        )),
        setupExample = "Anfangsaufstellung"
      ),
      categories = CategoriesOff(
        printTitle = "Stadt-Land-Fluss — Blatt",
        categoriesLabel = "Kategorie",
        lettersLabel = "Buchstabe",
        scoresLabel = "Punkte",
        curatedNote = "Drei Runden, ein Buchstabe pro Spalte. Tragt in jede Kategorie ein Wort ein, das mit diesem Buchstaben beginnt.",
        rules = Rules("So spielt man", List(
          "Jede Spalte hat oben einen Buchstaben — das ist der Buchstabe der Runde.",
          "Stellt einen Timer (etwa 3 Minuten). Jeder versucht, pro Kategorie ein Wort mit diesem Buchstaben zu schreiben.",
          "Wenn die Zeit um ist, vergleicht: ein Punkt pro einzigartiger Antwort; haben zwei Spieler dasselbe geschrieben, gibt es keinen Punkt.",
          "Spielt alle drei Spalten. Wer am meisten Punkte hat, gewinnt."
        ))
      ),
      checkers = CheckersOff(
        rules = Rules("So spielt man", List(
          "Jeder Spieler hat 12 Steine auf den dunklen Feldern der hintersten drei Reihen.",
          "Bewegt diagonal nach vorn auf ein leeres dunkles Feld. Schlagt, indem ihr über einen benachbarten Gegner auf das leere Feld dahinter springt.",
          "Schlagzwang; mehrfache Sprünge gehören zum gleichen Zug.",
          "Auf der Grundlinie wird der Stein zur Dame — sie zieht und schlägt in beide Richtungen.",
          "Gewonnen, wenn alle gegnerischen Steine geschlagen sind oder der Gegner nicht ziehen kann."
        )),
        lichessLabel = "lidraughts öffnen"
      ),
      sudoku = SudokuOff(
        printTitle = "Sudoku — sechs Rätsel",
        sheetHint = "Druckt das Blatt aus, schnappt euch einen Stift und löst los. Keine Lösungen — das gehört zum Spaß.",
        rules = Rules("So wird gespielt", List(
          "Füllt das Gitter so aus, dass jede Reihe, jede Spalte und jeder 3×3-Block die Ziffern 1–9 jeweils genau einmal enthält.",
          "Die vorgegebenen Zahlen sind fest. Tragt kleine Kandidatennotizen ein, wenn ihr unsicher seid.",
          "Arbeitet per Ausschluss: passt nur eine Ziffer in ein Feld, ist sie die Lösung."
        ))
      ),
      maze = MazeOff(
        printTitle = "Labyrinthe — hilf der Maus zum Käse",
        sheetHint = "Druckt das Blatt aus und malt mit einem Stift einen Weg von der Maus zum Käse. Durch jedes Labyrinth führt ein Weg.",
        rules = Rules("So wird gespielt", List(
          "Starte bei der Maus 🐭 und finde den Weg zum Käse 🧀.",
          "Wände kann man nicht überqueren — folge nur den offenen Gängen.",
          "Sackgasse? Geh zurück und versuch eine andere Abzweigung."
        ))
      ),
      wordSearch = WordSearchOff(
        printTitle = "Wortsuche — finde die versteckten Wörter",
        sheetHint = "Druckt das Blatt aus und kreist jedes gefundene Wort ein. Die Wortliste steht unter dem Gitter.",
        wordsLabel = "Finde diese Wörter",
        rules = Rules("So wird gespielt", List(
          "Jedes Wort aus der Liste ist im Buchstabengitter versteckt.",
          "Die Wörter verlaufen gerade — waagerecht, senkrecht oder diagonal, manchmal rückwärts.",
          "Kreise jedes gefundene Wort ein und streiche es von der Liste."
        ))
      ),
      guideRobot = GuideRobotOff(
        printTitle = "Führe den Roboter — schreibe den Pfeilweg",
        sheetHint = "Druckt das Blatt aus und schreibt die Pfeile, die jeden Roboter zu seinem Ziel führen.",
        writeLabel = "Schreibe die Pfeile",
        rules = Rules("So wird gespielt", List(
          "Der Roboter 🤖 startet im Gitter und muss das Ziel 🏁 erreichen.",
          "Auf dem leichten Blatt schreibe Pfeile (↑ ↓ ← →) — ein Feld pro Pfeil.",
          "Auf den schweren Blättern dreht sich der Roboter: ↑ vorwärts, ↺ links, ↻ rechts.",
          "Wände 🧱 versperren den Weg, also weiche ihnen aus.",
          "Auf den schweren Blättern fahr über den Stern ⭐ vor dem Ziel."
        ))
      )
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
      endTurn = "Zug beenden",
      p2p = BattleshipsP2P(
        title = "Auf zwei Geräten spielen",
        intro = "Verbindet zwei Handys, um mit verborgenen Brettern zu spielen. Ein Gerät erstellt das Spiel, das andere tritt mit demselben Code bei.",
        create = "Spiel erstellen",
        join = "Beitreten",
        connect = "Verbinden",
        shareCode = "Diesen Code mit dem anderen Gerät teilen",
        enterCode = "Code vom anderen Gerät eingeben",
        waiting = "Warte auf das andere Gerät…",
        yourTurn = "Du bist dran",
        opponentTurn = "Gegner ist dran",
        waitingShot = "Warte auf seinen Schuss…",
        waitingResolve = "Warte auf das Ergebnis…",
        youWin = "Du gewinnst!",
        youLose = "Du verlierst."
      )
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
      next = "Neue Runde",
      startingWith = "mit dem Buchstaben"
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
    ),
    sudoku = Sudoku(
      name = "Sudoku",
      description = "Füllt das 9×9-Gitter so, dass jede Reihe, Spalte und Box 1–9 enthält.",
      chooseVariant = "Schwierigkeit wählen",
      changeVariant = "Schwierigkeit ändern",
      newGame = "Neues Spiel",
      undo = "Rückgängig",
      pencil = "Notiz",
      easy = SudokuVariant("Leicht", "Mehr Hinweise — sanfter Einstieg."),
      medium = SudokuVariant("Mittel", "Weniger Hinweise — echte Herausforderung."),
      hard = SudokuVariant("Schwer", "Wenige Hinweise — für Profis.")
    ),
    minesweeper = Minesweeper(
      name = "Minesweeper",
      description = "Räume das Feld, ohne auf eine Mine zu treten.",
      chooseVariant = "Größe wählen",
      changeVariant = "Größe ändern",
      newGame = "Neues Spiel",
      playing = "Pass auf, wohin du trittst…",
      youWon = "Geschafft!",
      youLost = "Bumm!",
      revealMode = "Aufdecken",
      flagMode = "Flagge",
      easy = MinesweeperVariant("Leicht", "9×9 mit 10 Minen."),
      medium = MinesweeperVariant("Mittel", "12×12 mit 25 Minen."),
      hard = MinesweeperVariant("Schwer", "16×12 mit 40 Minen.")
    ),
    maze = Maze(
      name = "Labyrinth",
      description = "Hilf der Maus, den Käse zu finden.",
      instruction = "Wische oder zieh, um die Maus zum Käse zu führen — Pfeiltasten gehen auch.",
      won = "Du hast den Käse gefunden! 🧀",
      clearPath = "Weg löschen",
      newGame = "Neues Labyrinth",
      easy = MazeVariant("Leicht", "10×10 mit sanften Schleifen."),
      medium = MazeVariant("Mittel", "16×16, ein Weg hindurch."),
      hard = MazeVariant("Schwer", "24×24, ein echtes Labyrinth.")
    ),
    wordSearch = WordSearch(
      name = "Wortsuche",
      description = "Finde die versteckten Wörter im Gitter.",
      instruction = "Zieh über die Buchstaben, um ein Wort zu markieren — waagerecht, senkrecht oder diagonal.",
      foundLabel = "Gefunden",
      won = "Du hast alle gefunden! 🔍",
      newGame = "Neues Gitter",
      easy = WordSearchVariant("Leicht", "8×8, 6 Wörter, waagerecht und senkrecht."),
      medium = WordSearchVariant("Mittel", "11×11, 8 Wörter, mit Diagonalen."),
      hard = WordSearchVariant("Schwer", "13×13, 10 Wörter, in alle Richtungen.")
    ),
    guideRobot = GuideRobot(
      name = "Führe den Roboter",
      description = "Programmiere die Pfeile und bring den Roboter ins Ziel.",
      instruction = "Tippe auf die Pfeile, um einen Weg zu bauen, dann drücke Los, um den Roboter zu starten.",
      instructionTurns = "Tippe auf Vorwärts und die Drehtasten, um den Roboter zu lenken, dann drücke Los.",
      programLabel = "Dein Programm",
      emptyProgram = "Tippe unten auf die Pfeile, um Schritte hinzuzufügen.",
      run = "Los ▶",
      undo = "Zurück",
      clear = "Löschen",
      starHint = "Fahr über den Stern ⭐, bevor du das Ziel 🏁 erreichst.",
      won = "Der Roboter hat es geschafft! 🤖",
      crashed = "Bumm! Der Roboter ist gegen eine Wand gefahren. Versuch es nochmal.",
      missed = "Der Roboter hat das Ziel nicht erreicht. Versuch es nochmal.",
      missedStar = "Hol erst den Stern ⭐! Versuch es nochmal.",
      tryAgain = "Nochmal versuchen",
      newGame = "Neues Rätsel",
      easy = GuideRobotVariant("Leicht", "5×5, freier Weg zum Ziel."),
      medium = GuideRobotVariant("Mittel", "6×6 mit Wänden zum Ausweichen."),
      hard = GuideRobotVariant("Schwer", "6×6 — hol den Stern, dann das Ziel.")
    ),
    wordBuilder = WordBuilder(
      name = "Lesen & Schreiben",
      description = "Wörter bilden und das passende Bild auswählen.",
      easy = WordBuilderLevel("Leicht", "Kurze Wörter, keine zusätzlichen Buchstaben."),
      medium = WordBuilderLevel("Mittel", "Längere Wörter, ein paar zusätzliche Buchstaben."),
      hard = WordBuilderLevel("Schwer", "Lange Wörter mit zusätzlichen Buchstaben."),
      correct = "Super!",
      nextWord = "Nächstes Wort",
      skip = "Überspringen",
      printTitle = "Lesen & Schreiben — Arbeitsblatt",
      printHint = "Stufe wählen und Blatt drucken."
    ),
    mathPractice = MathPractice(
      name = "Rechnen üben",
      description = "Zählen, vergleichen, addieren und subtrahieren.",
      easy   = MathPracticeLevel("Leicht",  "Zählen und vergleichen mit Bildern, bis 10."),
      medium = MathPracticeLevel("Mittel",  "Addieren und subtrahieren bis 10."),
      hard   = MathPracticeLevel("Schwer",  "Bis 20 mit fehlender Zahl."),
      howMany = "Wie viele?",
      pickGroup = "Wähle die passende Gruppe",
      correct = "Super!",
      nextProblem = "Weiter",
      skip = "Überspringen",
      printTitle = "Rechnen — Arbeitsblatt",
      printHint = "Stufe wählen und Blatt drucken.",
      plus = "plus",
      minus = "minus",
      equals = "ist gleich",
      whatNumber = "welche Zahl",
      compare = "ist größer oder kleiner als"
    ),
    clock = Clock(
      name = "Uhr",
      description = "Lerne, die Uhr zu lesen.",
      matchName = "Lies die Uhr",
      matchDesc = "Verbinde analog und digital, im Viertelstundentakt.",
      todName = "Tageszeit",
      todDesc = "Benenne die Tageszeit anhand der Uhrzeit.",
      formatLabel = "Uhr",
      format12 = "12-Stunden",
      format24 = "24-Stunden",
      whatTime = "Wie spät ist es?",
      pickClock = "Wähle die passende Uhr",
      partOfDay = "Welche Tageszeit?",
      morning = "Morgen",
      afternoon = "Nachmittag",
      evening = "Abend",
      night = "Nacht",
      correct = "Gut gemacht!",
      next = "Weiter",
      skip = "Überspringen"
    ),
    reading = Reading(
      name = "Lesezeit",
      description = "Lest gemeinsam einen Klassiker.",
      hint = "Unten findet ihr einige Inspirationen.",
      search = "Suchen",
      freeEbook = "E-Book",
      bandTots = "0–3 Jahre",
      bandPicture = "3–6 Jahre",
      bandChapter = "5–9 Jahre",
      bandOlder = "8+ Jahre"
    ),
    memoryChain = MemoryChain(
      name = "Gedächtniszug",
      description = "Wiederhole die ganze Liste und füge eine Sache hinzu.",
      hint = "Reihum. Jeder wiederholt der Reihe nach alles bisher Gesagte und fügt eine neue Sache hinzu. Wer die Kette unterbricht, scheidet aus.",
      newTheme = "Neues Thema"
    ),
    iSpy = ISpy(
      name = "Ich sehe was",
      description = "Entdecke etwas in der Nähe und gib einen Hinweis.",
      hint = "Ein Suchspiel — am besten mit Dingen, die ihr alle gerade sehen könnt.",
      howTitle = "So wird gespielt",
      step1 = "Ein Spieler wählt heimlich etwas, das alle sehen können.",
      step2 = "Er gibt einen Hinweis, zum Beispiel „Ich sehe etwas Rotes“.",
      step3 = "Die anderen raten laut. Wer zuerst richtig liegt, ist als Nächstes dran.",
      tipsTitle = "Hinweis-Ideen",
      tip1 = "Nach Farbe, Form, Größe oder Material.",
      tip2 = "Oder nach dem ersten Buchstaben: „…etwas mit B“."
    )
  )

  def of(lang: Lang): Strings = lang match
    case Lang.En => en
    case Lang.Pl => pl
    case Lang.Es => es
    case Lang.Fr => fr
    case Lang.De => de
