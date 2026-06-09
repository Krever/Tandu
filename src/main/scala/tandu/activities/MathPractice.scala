package tandu.activities

import com.raquo.laminar.api.L.*
import tandu.{AppState, Kind, Page, Routing}
import tandu.i18n.Strings
import tandu.ui.{Components, Mode, ModeChooser, Printable}
import tandu.ui.Components.s

import scala.scalajs.js
import scala.util.Random

object MathPractice extends Activity:
  val id = "math-practice"
  def name(s: Strings): String = s.mathPractice.name
  def description(s: Strings): String = s.mathPractice.description
  val minPlayers: Int = 1
  val maxPlayers: Int = 1
  val glyph: String = "➕"
  val tint: String = "mustard"
  override val kind: Kind = Kind.Learn

  // ---------- model ----------

  enum TaskKind:
    case Count, Recognize, Compare, AddPic, SubPic, AddNum, SubNum, Missing

  final case class Group(emoji: String, count: Int)

  enum Stem:
    case Expression(text: String)                            // "3 + 4 = ?"
    case ShowGroup(group: Group)                             // count
    case ShowNumeral(n: Int)                                 // recognize
    case TwoGroups(a: Group, b: Group, op: Option[String])   // pictorial addition / pictorial compare
    case TwoNumerals(a: Int, b: Int)                         // numeric compare
    case TakeAway(emoji: String, total: Int, removed: Int)   // pictorial subtraction (cross-out)

  enum Choice:
    case Num(n: Int)
    case Pic(group: Group)
    case Sym(text: String)

  final case class Task(
      kind: TaskKind,
      stem: Stem,
      prompt: Strings => String,
      choices: Vector[Choice],
      correctIdx: Int
  )

  final case class Level(
      id: String,
      label: Strings => String,
      description: Strings => String,
      range: Int,
      kinds: Vector[TaskKind]
  )

  // Three tiers, each adding something new rather than just expanding range:
  // pictures → numeric symbols → bigger range + missing-number puzzles.
  val levels: List[Level] = List(
    Level("easy",   _.mathPractice.easy.name,   _.mathPractice.easy.description, range = 10,
      kinds = Vector(TaskKind.Count, TaskKind.Recognize, TaskKind.Compare, TaskKind.AddPic, TaskKind.SubPic)),
    Level("medium", _.mathPractice.medium.name, _.mathPractice.medium.description, range = 10,
      kinds = Vector(TaskKind.Compare, TaskKind.AddNum, TaskKind.SubNum)),
    Level("hard",   _.mathPractice.hard.name,   _.mathPractice.hard.description, range = 20,
      kinds = Vector(TaskKind.AddNum, TaskKind.SubNum, TaskKind.Missing))
  )

  private val DefaultLevel = levels.head
  private val WorksheetRows = 20

  private val emojiPool = Vector("🍎", "🍊", "🐶", "🐱", "⭐", "⚽", "🦋", "🐢", "🐠", "🌸", "🚗", "🎈")
  private def randomEmoji(): String = emojiPool(Random.nextInt(emojiPool.size))

  /** A row of `total` emoji with the last `removed` struck through — the visual
    * "take away" group, shared by the screen and print renderers. */
  private def strikeItems(emoji: String, total: Int, removed: Int): Seq[HtmlElement] =
    (0 until total).map(i => span(cls("mp-strike") := i >= total - removed, emoji))

  private def randInRange(min: Int, max: Int): Int =
    if max <= min then min else min + Random.nextInt(max - min + 1)

  // ---------- generation ----------

  private def buildTask(level: Level, avoid: Option[TaskKind]): Task =
    val k = pickKind(level, avoid)
    genTask(level, k)

  private def pickKind(level: Level, avoid: Option[TaskKind]): TaskKind =
    val pool = avoid match
      case Some(a) if level.kinds.size > 1 => level.kinds.filterNot(_ == a)
      case _                               => level.kinds
    pool(Random.nextInt(pool.size))

  private def genTask(level: Level, kind: TaskKind): Task = kind match
    case TaskKind.Count     => genCount(level)
    case TaskKind.Recognize => genRecognize(level)
    case TaskKind.Compare   => genCompare(level)
    case TaskKind.AddPic    => genArith(level, plus = true,  pictorial = true)
    case TaskKind.SubPic    => genArith(level, plus = false, pictorial = true)
    case TaskKind.AddNum    => genArith(level, plus = true,  pictorial = false)
    case TaskKind.SubNum    => genArith(level, plus = false, pictorial = false)
    case TaskKind.Missing   => genMissing(level)

  /** Build a set of 4 plausible numeric choices around `correct`. */
  private def numChoices(correct: Int, range: Int): (Vector[Int], Int) =
    val maxVal = math.max(range, correct + 3)
    val seen = scala.collection.mutable.Set(correct)
    var tries = 0
    while seen.size < 4 && tries < 80 do
      val n = math.max(0, math.min(maxVal, correct + randInRange(-3, 3)))
      seen += n
      tries += 1
    // Fall back to filling sequentially if random didn't yield enough distinct.
    var fill = 0
    while seen.size < 4 && fill <= maxVal do
      seen += fill
      fill += 1
    val shuffled = Random.shuffle(seen.toList).toVector
    (shuffled, shuffled.indexOf(correct))

  private def genCount(level: Level): Task =
    val n = randInRange(1, level.range)
    val g = Group(randomEmoji(), n)
    val (choices, idx) = numChoices(n, level.range)
    Task(TaskKind.Count, Stem.ShowGroup(g), _.mathPractice.howMany,
      choices.map(Choice.Num.apply), idx)

  private def genRecognize(level: Level): Task =
    val n = randInRange(1, level.range)
    val emoji = randomEmoji()
    val (nums, idx) = numChoices(n, level.range)
    val groups = nums.map(c => Group(emoji, math.max(1, c)))
    // After clamping zero counts to 1 the correct group's count may collide
    // with a distractor's; that's still unambiguous because the target numeral
    // is shown as the stem.
    Task(TaskKind.Recognize, Stem.ShowNumeral(n), _.mathPractice.pickGroup,
      groups.map(Choice.Pic.apply), idx)

  private def genCompare(level: Level): Task =
    val pictorial = level.kinds.contains(TaskKind.Count)
    var a = randInRange(1, level.range)
    var b = randInRange(1, level.range)
    var tries = 0
    while a == b && tries < 10 do
      b = randInRange(1, level.range); tries += 1
    if a == b then a = math.min(level.range, a + 1)
    // Choices are the comparator symbols themselves — randomized so kids learn
    // the glyph, not a fixed position. The `?` in the stem is the prompt.
    val syms = if Random.nextBoolean() then Vector(">", "<") else Vector("<", ">")
    val correctSym = if a > b then ">" else "<"
    val idx = syms.indexOf(correctSym)
    val choices = syms.map(Choice.Sym.apply)
    val stem =
      if pictorial then
        val emoji = randomEmoji()
        Stem.TwoGroups(Group(emoji, a), Group(emoji, b), None)
      else
        Stem.TwoNumerals(a, b)
    Task(TaskKind.Compare, stem, _ => "", choices, idx)

  private def genArith(level: Level, plus: Boolean, pictorial: Boolean): Task =
    val cap = level.range
    val (a, b, result) =
      if plus then
        val x = randInRange(1, cap - 1)
        val y = randInRange(1, cap - x)
        (x, y, x + y)
      else
        val x = randInRange(2, cap)
        val y = randInRange(1, x - 1)
        (x, y, x - y)
    val (choices, idx) = numChoices(result, cap)
    val op = if plus then "+" else "−"
    if pictorial then
      val emoji = randomEmoji()
      // Addition shows two groups (numeral + picture each), so the picture
      // supports the sum instead of replacing it. Subtraction shows a single
      // group with the subtracted items crossed out, so the picture actually
      // means "take away" and is solved by counting what remains.
      val stem =
        if plus then Stem.TwoGroups(Group(emoji, a), Group(emoji, b), Some(op))
        else Stem.TakeAway(emoji, a, b)
      Task(if plus then TaskKind.AddPic else TaskKind.SubPic,
        stem, _.mathPractice.howMany,
        choices.map(Choice.Num.apply), idx)
    else
      Task(if plus then TaskKind.AddNum else TaskKind.SubNum,
        Stem.Expression(s"$a $op $b = ?"), _.mathPractice.howMany,
        choices.map(Choice.Num.apply), idx)

  private def genMissing(level: Level): Task =
    val cap = level.range
    val plus = Random.nextBoolean()
    if plus then
      val sum = randInRange(3, cap)
      val a   = randInRange(1, sum - 1)
      val b   = sum - a
      val (choices, idx) = numChoices(b, cap)
      Task(TaskKind.Missing, Stem.Expression(s"$a + ? = $sum"), _.mathPractice.howMany,
        choices.map(Choice.Num.apply), idx)
    else
      val a   = randInRange(3, cap)
      val b   = randInRange(1, a - 1)
      val res = a - b
      val (choices, idx) = numChoices(b, cap)
      Task(TaskKind.Missing, Stem.Expression(s"$a − ? = $res"), _.mathPractice.howMany,
        choices.map(Choice.Num.apply), idx)

  // ---------- render ----------

  def render(): HtmlElement =
    ModeChooser.render(id, List(
      Mode(
        id = "in-app",
        label = _.mode.inApp,
        render = () => renderInApp()
      ),
      Mode(
        id = "print",
        label = _.mode.offline,
        materials = List(_.offline.materials.printer, _.offline.materials.paperPen),
        render = () => renderPrint()
      )
    ))

  private def levelSignal(modeId: String): Signal[Level] =
    Routing.router.currentPageSignal.map {
      case Page.Activity(`id`, m :: lvl :: _) if m == modeId =>
        levels.find(_.id == lvl).getOrElse(DefaultLevel)
      case _ => DefaultLevel
    }.distinct

  private def levelPill(modeId: String, current: Signal[Level]): HtmlElement =
    div(
      cls := "center no-print",
      div(
        cls := "pill-toggle",
        levels.map { lv =>
          button(
            cls := "pill-btn",
            cls("is-active") <-- current.map(_.id == lv.id),
            child.text <-- AppState.strings.map(lv.label),
            onClick --> (_ => Routing.go(Page.Activity(id, List(modeId, lv.id))))
          )
        }
      )
    )

  // ---------- in-app ----------

  private def renderInApp(): HtmlElement =
    val current = levelSignal("in-app")
    div(
      cls := "mp stack-lg",
      levelPill("in-app", current),
      child <-- current.map(playForLevel)
    )

  private def playForLevel(level: Level): HtmlElement =
    val task: Var[Task] = Var(buildTask(level, avoid = None))
    val solved: Var[Boolean] = Var(false)
    val wrongIdx: Var[Option[Int]] = Var(None)

    def nextTask(): Unit =
      task.set(buildTask(level, avoid = Some(task.now().kind)))
      solved.set(false)
      wrongIdx.set(None)

    def pick(idx: Int): Unit =
      if solved.now() then ()
      else if idx == task.now().correctIdx then solved.set(true)
      else
        wrongIdx.set(Some(idx))
        val _ = js.timers.setTimeout(420)(
          if wrongIdx.now().contains(idx) then wrongIdx.set(None)
        )

    div(
      cls := "stack-lg",
      div(
        cls := "mp-prompt-row",
        child <-- task.signal.combineWith(AppState.strings).map { (t, str) =>
          spoken(t, str) match
            case Some(txt) => Components.speakBtn(Val(txt))
            case None      => emptyNode
        }
      ),
      div(
        cls := "mp-stem",
        cls("mp-stem--solved") <-- solved.signal,
        child <-- task.signal.map(t => stemNode(t.stem))
      ),
      div(
        cls := "mp-choices no-print",
        children <-- task.signal.combineWith(wrongIdx.signal).combineWith(solved.signal).map {
          (t, wrong, isSolved) =>
            t.choices.zipWithIndex.map { (c, i) =>
              val isWrong  = wrong.contains(i)
              val isRight  = isSolved && i == t.correctIdx
              button(
                cls := "mp-choice",
                cls("mp-choice--pic")     := c.isInstanceOf[Choice.Pic],
                cls("mp-choice--wrong")   := isWrong,
                cls("mp-choice--correct") := isRight,
                disabled := isSolved,
                c match
                  case Choice.Num(n) => span(cls := "mp-choice-num", n.toString)
                  case Choice.Pic(g) => span(cls := "mp-choice-pic", g.emoji * g.count)
                  case Choice.Sym(t) => span(cls := "mp-choice-sym", t)
                ,
                onClick --> (_ => pick(i))
              )
            }
        }
      ),
      child <-- solved.signal.map { isSolved =>
        if isSolved then
          div(
            cls := "stack",
            Components.banner("win", s(_.mathPractice.correct)),
            div(
              cls := "row no-print",
              styleAttr := "justify-content: center;",
              button(
                cls := "btn btn--lg",
                child.text <-- s(_.mathPractice.nextProblem),
                onClick --> (_ => nextTask())
              )
            )
          )
        else
          div(
            cls := "row no-print",
            styleAttr := "justify-content: center;",
            Components.ghost(s(_.mathPractice.skip), nextTask())
          )
      }
    )

  // ---------- speech ----------

  /** What to read aloud for a task — the numbers on screen, not the (visible,
    * obvious) instruction. Returns None where the only number present is the
    * answer the child must produce, so speech never gives the game away:
    *   - Count: a group of pictures to tally — saying the count is the answer.
    *   - Pictorial compare: the two counts ARE the comparison.
    * Everything else has numbers that are part of the question, safe to voice. */
  private def spoken(t: Task, str: Strings): Option[String] = t.stem match
    case Stem.Expression(text)          => Some(exprToSpeech(text, str))
    case Stem.ShowNumeral(n)            => Some(n.toString)
    case Stem.TwoNumerals(a, b)         => Some(s"$a ${str.mathPractice.compare} $b?")
    case Stem.TwoGroups(a, b, Some(op)) => Some(exprToSpeech(s"${a.count} $op ${b.count}", str))
    case Stem.TakeAway(_, total, rem)   => Some(exprToSpeech(s"$total − $rem", str))
    case Stem.ShowGroup(_)              => None
    case Stem.TwoGroups(_, _, None)     => None

  private def exprToSpeech(text: String, str: Strings): String =
    val mp = str.mathPractice
    text
      .replace("+", s" ${mp.plus} ")
      .replace("−", s" ${mp.minus} ")
      .replace("=", s" ${mp.equals} ")
      .replace("?", s" ${mp.whatNumber} ")
      .replaceAll("\\s+", " ")
      .trim

  private def stemNode(stem: Stem): HtmlElement = stem match
    case Stem.Expression(text) =>
      div(cls := "mp-expr", text)
    case Stem.ShowGroup(g) =>
      div(cls := "mp-group", g.emoji * g.count)
    case Stem.ShowNumeral(n) =>
      div(cls := "mp-numeral", n.toString)
    case Stem.TwoGroups(a, b, op) =>
      // With an operator (addition) show the numeral above each group so the
      // picture supports the sum; without one (comparison) show bare groups so
      // the child compares quantities, not numerals.
      def operand(g: Group): HtmlElement = op match
        case Some(_) =>
          div(cls := "mp-operand",
            div(cls := "mp-operand-num", g.count.toString),
            div(cls := "mp-group", g.emoji * g.count))
        case None =>
          div(cls := "mp-group", g.emoji * g.count)
      val opNode = op match
        case Some(o) => div(cls := "mp-op", o)
        case None    => div(cls := "mp-op mp-op--q", "?")
      val tail: List[Modifier[HtmlElement]] = op match
        case Some(_) => List(div(cls := "mp-op", "="), div(cls := "mp-op", "?"))
        case None    => Nil
      div(
        cls := "mp-pic-row",
        operand(a),
        opNode,
        operand(b),
        tail
      )
    case Stem.TakeAway(emoji, total, removed) =>
      div(
        cls := "mp-pic-row",
        div(cls := "mp-operand",
          div(cls := "mp-operand-num", s"$total − $removed"),
          div(cls := "mp-group", strikeItems(emoji, total, removed))
        ),
        div(cls := "mp-op", "="),
        div(cls := "mp-op", "?")
      )
    case Stem.TwoNumerals(a, b) =>
      div(
        cls := "mp-pic-row",
        div(cls := "mp-numeral mp-numeral--side", a.toString),
        div(cls := "mp-op mp-op--q", "?"),
        div(cls := "mp-numeral mp-numeral--side", b.toString)
      )

  // ---------- print ----------

  private def renderPrint(): HtmlElement =
    val tasks: Var[Vector[Task]] = Var(Vector.empty)

    def generate(level: Level): Unit =
      // Recognize is hard to print (kid would have to draw a group); everything
      // else — count, compare, add/sub, missing — fits a fill-in row.
      val printable = level.kinds.filter(_ != TaskKind.Recognize)
      val pickable = if printable.isEmpty then level.kinds else printable
      val rows = (1 to WorksheetRows).map { _ =>
        val k = pickable(Random.nextInt(pickable.size))
        genTask(level, k)
      }.toVector
      tasks.set(rows)
      val _ = js.timers.setTimeout(50)(Printable.print())

    div(
      cls := "stack-lg",
      div(
        cls := "no-print stack",
        div(
          cls := "row",
          styleAttr := "justify-content: center; gap: 0.5rem; flex-wrap: wrap;",
          levels.map { lv =>
            button(
              cls := "btn btn--lg",
              child.text <-- AppState.strings.map(str => s"${str.printable.print} — ${lv.label(str)}"),
              onClick --> (_ => generate(lv))
            )
          }
        ),
        p(cls := "muted center", child.text <-- s(_.mathPractice.printHint))
      ),
      div(
        cls := "print-only",
        Printable.render(
          title = _.mathPractice.printTitle,
          body = div(
            cls := "mp-print-sheet",
            children <-- tasks.signal.map(_.map(printableRow))
          )
        )
      )
    )

  private def printableRow(t: Task): HtmlElement = t.stem match
    // Compare: the blank lives between the two values, no "= ___" tail.
    case Stem.TwoGroups(a, b, None) =>
      div(
        cls := "mp-print-row mp-print-row--cmp",
        div(cls := "mp-print-group", a.emoji * a.count),
        div(cls := "mp-print-slot mp-print-slot--box"),
        div(cls := "mp-print-group", b.emoji * b.count)
      )
    case Stem.TwoNumerals(a, b) =>
      div(
        cls := "mp-print-row mp-print-row--cmp",
        div(cls := "mp-print-expr", a.toString),
        div(cls := "mp-print-slot mp-print-slot--box"),
        div(cls := "mp-print-expr", b.toString)
      )
    case stem =>
      div(
        cls := "mp-print-row",
        div(cls := "mp-print-stem", printStem(stem)),
        div(cls := "mp-print-eq", "="),
        div(cls := "mp-print-slot")
      )

  private def printStem(stem: Stem): HtmlElement = stem match
    case Stem.Expression(text) =>
      val trimmed = text.replaceAll("\\s*=\\s*\\?\\s*$", "").trim
      div(cls := "mp-print-expr", trimmed)
    case Stem.ShowGroup(g) =>
      div(cls := "mp-print-group", g.emoji * g.count)
    case Stem.ShowNumeral(n) =>
      div(cls := "mp-print-expr", n.toString)
    case Stem.TwoGroups(a, b, op) =>
      div(
        cls := "mp-print-pic",
        span(cls := "mp-print-group", a.emoji * a.count),
        span(cls := "mp-print-op", op.getOrElse("?")),
        span(cls := "mp-print-group", b.emoji * b.count)
      )
    case Stem.TwoNumerals(a, b) =>
      div(
        cls := "mp-print-pic",
        span(cls := "mp-print-expr", a.toString),
        span(cls := "mp-print-op", "?"),
        span(cls := "mp-print-expr", b.toString)
      )
    case Stem.TakeAway(emoji, total, removed) =>
      div(
        cls := "mp-print-pic",
        span(cls := "mp-print-expr", s"$total − $removed"),
        span(cls := "mp-print-group", strikeItems(emoji, total, removed))
      )
