package tandu.activities

import com.raquo.laminar.api.L.*
import org.scalajs.dom
import tandu.{AppState, Page, Routing}
import tandu.i18n.Strings
import tandu.ui.{Components, Mode, ModeChooser, Pictures, Printable, RulesCard}
import tandu.ui.Components.s

import scala.collection.mutable
import scala.scalajs.js
import scala.util.Random

/** Seek & find: a busy themed emoji scene hiding a handful of target
  * pictures. Two in-app games (find-them-all and how-many counting) plus
  * printable find-and-circle / count-and-circle sheets.
  *
  * Scenes are themed (ocean, forest, farm, ...) rather than drawn from one
  * big pool: a cat among other animals is a real search, a cat among cars
  * and pizza is not. Targets are dealt exact counts and *excluded* from the
  * distractor pool, so the legend's numbers are always honest.
  *
  * Placement is a jittered grid: cells are shuffled, each item lands near
  * its cell centre with random offset, size jitter and (on hard) tilt.
  * That guarantees full placement at any density — no rejection sampling —
  * while reading as organic scatter. Harder levels let the jitter exceed
  * the cell's free space, so items start to crowd and overlap slightly.
  */
object SeekAndFind extends Activity:
  val id = "seek-and-find"
  def name(s: Strings): String = s.seekAndFind.name
  def description(s: Strings): String = s.seekAndFind.description
  val minPlayers: Int = 1
  val maxPlayers: Int = 1
  val glyph: String = "👀"
  val tint: String = "vermilion"

  // ---------- geometry ----------

  /** Board space in SVG user units. The app board is phone-portrait-ish;
    * the print field is shaped to fill an A4 body under its legend. The
    * CSS aspect-ratio of .snf-board must match AppW : AppH. */
  private val AppW = 100.0
  private val AppH = 120.0
  private val PrintW = 100.0
  private val PrintH = 112.0

  // ---------- themes ----------

  /** `lookAlikes` are groups of pictures from `pool` that young children
    * mix up at a glance. They are per theme, because the same emoji can be
    * in two themes with different neighbours (🐛 in forest and garden). */
  private final case class Theme(themeId: String, pool: Vector[String], lookAlikes: Vector[Set[String]])

  /** Pools stick to long-established emoji so every platform's font has
    * them. ~15 per theme: enough distinct distractors for three targets. */
  private val themes: Vector[Theme] = Vector(
    Theme("ocean", Vector("🐟", "🐠", "🐡", "🦈", "🐬", "🐳", "🐙", "🦑", "🦀", "🦞", "🦐", "🐚", "🐢", "⚓", "⛵"),
      Vector(Set("🐟", "🐠", "🐡"), Set("🐬", "🦈", "🐳"), Set("🦀", "🦞", "🦐"), Set("🐙", "🦑"))),
    Theme("forest", Vector("🦊", "🐺", "🦌", "🐻", "🐿️", "🦔", "🦉", "🐗", "🍄", "🌰", "🦅", "🐍", "🐸", "🦋", "🐛"),
      Vector(Set("🦊", "🐺"), Set("🦉", "🦅"), Set("🐿️", "🦔"), Set("🐻", "🐗"), Set("🐍", "🐛"))),
    Theme("farm", Vector("🐮", "🐷", "🐔", "🐤", "🦆", "🐑", "🐐", "🐴", "🐰", "🚜", "🌽", "🥕", "🍎", "🌻", "🐶"),
      Vector(Set("🐔", "🐤", "🦆"), Set("🐑", "🐐"), Set("🐮", "🐷"), Set("🌽", "🥕"))),
    Theme("space", Vector("🚀", "🛸", "⭐", "🌟", "☄️", "🌙", "🪐", "🌍", "👽", "🤖", "🛰️", "☀️", "💫", "🔭"),
      Vector(Set("⭐", "🌟", "💫"), Set("🚀", "🛸", "🛰️"), Set("🌍", "🪐"), Set("👽", "🤖"))),
    Theme("sweets", Vector("🍩", "🍪", "🧁", "🍰", "🍭", "🍬", "🍫", "🍦", "🍓", "🍒", "🥨", "🍿", "🍉", "🧃"),
      Vector(Set("🍩", "🍪", "🥨"), Set("🧁", "🍰", "🍦"), Set("🍭", "🍬"), Set("🍓", "🍒", "🍉"))),
    Theme("garden", Vector("🐝", "🦋", "🐞", "🐛", "🐌", "🐜", "🕷️", "🌸", "🌷", "🌻", "🍀", "🐸", "🐦", "🍇"),
      Vector(Set("🐝", "🐞"), Set("🐛", "🐌"), Set("🐜", "🕷️"), Set("🌸", "🌷", "🌻")))
  )

  // ---------- model & generator ----------

  final case class Target(emoji: String, count: Int)
  final case class Item(emoji: String, x: Double, y: Double, size: Double, tilt: Double, isTarget: Boolean, mirror: Boolean)
  final case class Puzzle(themeId: String, targets: Vector[Target], items: Vector[Item], w: Double, h: Double)

  /** How many pictures are on the field besides the targets.
    *   - Total(n): n pictures in all, targets included. The in-app levels use
    *     this, so the field size does not depend on the random target counts.
    *   - Extras(n): exactly n other pictures (the printable sheets). */
  private enum Crowd:
    case Total(n: Int)
    case Extras(n: Int)

  /** How far a picture can move from the centre of its grid cell.
    *   - FreeSpace(boost): a share of the free space around the picture;
    *     boost > 1 lets pictures crowd (the in-app levels).
    *   - OfCell(f): a fraction of the cell, independent of the picture
    *     size; large values let pictures overlap (the printable layouts). */
  private enum Jitter:
    case FreeSpace(boost: Double)
    case OfCell(f: Double)

  /** What to do with pictures of a theme that children mix up (⭐ and 🌟).
    *   - Avoid: keep one of each group, for counting, where a child who
    *     mixes them up is judged wrong through no fault.
    *   - Allow: no rule; welcome difficulty in seek mode.
    *   - Favor(share): targets come from the groups, and `share` of the
    *     other pictures are look-alikes of the targets. */
  private enum LookAlikes:
    case Avoid, Allow
    case Favor(share: Double)

  /** One field recipe. `sizeFactor` scales items relative to their grid
    * cell; `sizeJitter` is ± a fraction of that size; `maxTilt` is degrees
    * either way; `mirrorChance` is the probability that a picture is drawn
    * mirrored. */
  private final case class Spec(
      crowd: Crowd,
      targetTypes: Int,
      countMin: Int,
      countMax: Int,
      sizeFactor: Double,
      jitter: Jitter,
      sizeJitter: Double,
      maxTilt: Double,
      lookAlikes: LookAlikes = LookAlikes.Allow,
      mirrorChance: Double = 0
  )

  /** Keep only the first-drawn member of each look-alike group. */
  private def dedupeConfusables(pool: Vector[String], groups: Vector[Set[String]]): Vector[String] =
    val taken = mutable.Set.empty[Int]
    pool.filter { e =>
      groups.indexWhere(_.contains(e)) match
        case -1 => true
        case g  => taken.add(g)
    }

  private final case class Level(levelId: String, app: Spec, nameKey: Strings => String)

  /** Difficulty turns three knobs at once: density, number of target kinds,
    * and how much sizes/tilts vary (uniform rows are easy to scan; jitter
    * defeats that). The printable sheets have their own recipe: see
    * [[SheetConfig]]. */
  private val seekLevels: List[Level] = List(
    Level(
      "easy",
      app = Spec(Crowd.Total(28), targetTypes = 1, countMin = 4, countMax = 6, sizeFactor = 0.72, Jitter.FreeSpace(1.15), sizeJitter = 0.12, maxTilt = 0),
      nameKey = _.seekAndFind.easy
    ),
    Level(
      "medium",
      app = Spec(Crowd.Total(54), targetTypes = 2, countMin = 3, countMax = 5, sizeFactor = 0.80, Jitter.FreeSpace(1.15), sizeJitter = 0.15, maxTilt = 8),
      nameKey = _.seekAndFind.medium
    ),
    Level(
      "hard",
      app = Spec(Crowd.Total(88), targetTypes = 3, countMin = 3, countMax = 5, sizeFactor = 0.88, Jitter.FreeSpace(1.4), sizeJitter = 0.22, maxTilt = 18),
      nameKey = _.seekAndFind.hard
    )
  )

  /** Counting keeps fields sparser than seeking at the same tier — the work
    * is keeping track, not spotting — and grows the answer instead. */
  private val countLevels: List[Level] = List(
    Level("easy", app = Spec(Crowd.Total(18), 1, 3, 6, 0.72, Jitter.FreeSpace(1.0), 0.08, 0, LookAlikes.Avoid), nameKey = _.seekAndFind.easy),
    Level("medium", app = Spec(Crowd.Total(32), 1, 5, 9, 0.78, Jitter.FreeSpace(1.1), 0.15, 8, LookAlikes.Avoid), nameKey = _.seekAndFind.medium),
    Level("hard", app = Spec(Crowd.Total(55), 1, 7, 12, 0.85, Jitter.FreeSpace(1.3), 0.22, 15, LookAlikes.Avoid), nameKey = _.seekAndFind.hard)
  )

  // ---------- printable sheet recipe ----------

  enum Task:
    case Find, Count

  /** How the child gives the answer on a counting sheet. `Circle` prints a
    * number line per kind (for children who cannot write digits yet);
    * `Write` prints an empty box. */
  enum Answer:
    case Circle, Write

  /** How regular the scene is.
    *   - Tidy: an exact grid of same-size, upright pictures. The child can
    *     go row by row, which is the easiest way to search and to count.
    *   - Mixed: loose scatter, sizes vary, pictures lean.
    *   - Wild: big and small pictures, turned up to 90° or mirrored, and
    *     they can overlap a little. Small pictures are drawn on top, so
    *     none is hidden completely.
    * `jitter` is a fraction of the grid cell; `sizeJitter` is ± a fraction
    * of the base size; `maxTilt` is degrees either way. */
  enum Layout(val sizeFactor: Double, val sizeJitter: Double, val jitter: Double, val maxTilt: Double, val mirrorChance: Double):
    case Tidy  extends Layout(0.72, 0.0,  0.0,  0,  0.0)
    case Mixed extends Layout(0.72, 0.30, 0.20, 25, 0.0)
    case Wild  extends Layout(0.80, 0.45, 0.35, 90, 0.5)

  /** The full recipe of one printable sheet. The builder form edits this
    * value and nothing else, so the same form can configure a sheet in
    * other places (for example a workbook row).
    *
    *   - `themes`: the theme ids to use. Empty means all themes. The set
    *     never holds every theme: [[SheetConfig.withTheme]] makes that
    *     empty, so there is one value for "all".
    *   - `kinds`: how many different pictures the child finds or counts.
    *   - `perKindMin`/`perKindMax`: the range of copies of each kind.
    *   - `extras`: pictures on the field that are not in the task.
    *   - `lookAlikes`: permit easily confused pictures (⭐ and 🌟).
    *   - `showCounts`: Find only. Print "× n" next to each picture.
    *   - `answer`: Count only. */
  final case class SheetConfig(
      task: Task,
      themes: Set[String],
      kinds: Int,
      perKindMin: Int,
      perKindMax: Int,
      extras: Int,
      layout: Layout,
      lookAlikes: Boolean,
      showCounts: Boolean,
      answer: Answer
  )

  object SheetConfig:
    val MaxKinds = 4      // the counting sheet has a 2 × 2 answer grid
    val MaxPerKind = 10   // the number line goes from 1 to 10
    val MaxExtras = 120
    val ExtrasStep = 5

  private val themeIdSet: Set[String] = themes.map(_.themeId).toSet

  /** Every theme selected is the same as none selected ("all"), so the
    * recipe has one value for "all". */
  private def normalizeThemes(ids: Set[String]): Set[String] =
    if ids.size == themes.size then Set.empty else ids

  extension (c: SheetConfig)
    /** The theme ids that pages can use, in catalog order. */
    def themeIds: Vector[String] =
      themes.map(_.themeId).filter(t => c.themes.isEmpty || c.themes(t))

    /** Add or remove one theme. */
    def withTheme(themeId: String): SheetConfig =
      c.copy(themes = normalizeThemes(if c.themes(themeId) then c.themes - themeId else c.themes + themeId))

  /** A named starting point for the builder form. */
  final case class SheetPreset(presetId: String, name: Strings => String, config: SheetConfig)

  val sheetPresets: List[SheetPreset] = List(
    SheetPreset("easy", _.seekAndFind.easy,
      SheetConfig(Task.Find, Set.empty, kinds = 2, perKindMin = 5, perKindMax = 7, extras = 40, Layout.Tidy, lookAlikes = true, showCounts = true, Answer.Circle)),
    SheetPreset("medium", _.seekAndFind.medium,
      SheetConfig(Task.Find, Set.empty, kinds = 2, perKindMin = 4, perKindMax = 6, extras = 70, Layout.Mixed, lookAlikes = true, showCounts = true, Answer.Circle)),
    SheetPreset("hard", _.seekAndFind.hard,
      SheetConfig(Task.Find, Set.empty, kinds = 3, perKindMin = 4, perKindMax = 6, extras = 105, Layout.Wild, lookAlikes = true, showCounts = true, Answer.Circle))
  )

  /** The workbook's counting sheet. Every kind on the field is counted, so
    * there are no other pictures. Not a preset: in the form, counting is a
    * task, not a starting point. */
  private val countSheetConfig =
    SheetConfig(Task.Count, Set.empty, kinds = 4, perKindMin = 4, perKindMax = 9, extras = 0, Layout.Tidy, lookAlikes = false, showCounts = true, Answer.Circle)

  private def presetConfig(presetId: String): SheetConfig =
    sheetPresets.find(_.presetId == presetId).getOrElse(sheetPresets.head).config

  // ---------- recipe in the URL ----------

  /** The recipe as one URL path segment, for example
    * `find.any.k1.n5-7.x40.tidy.la1.sc1.circle`. The order of tokens does
    * not matter. A missing or bad token keeps the value of the first
    * preset, and numbers are clamped to the form's limits, so an edited or
    * old link always gives a valid recipe. */
  def encodeConfig(c: SheetConfig): String =
    List(
      c.task.toString.toLowerCase,
      // Several themes are joined by "-" (ocean-space). Not "+": the router
      // encodes it as %2B.
      if c.themes.isEmpty then "any" else c.themeIds.mkString("-"),
      s"k${c.kinds}",
      s"n${c.perKindMin}-${c.perKindMax}",
      s"x${c.extras}",
      c.layout.toString.toLowerCase,
      s"la${if c.lookAlikes then 1 else 0}",
      s"sc${if c.showCounts then 1 else 0}",
      c.answer.toString.toLowerCase
    ).mkString(".")

  private def clamp(v: Int, lo: Int, hi: Int): Int = math.max(lo, math.min(hi, v))

  private val NumRange = "n(\\d+)-(\\d+)".r

  /** The enum case whose lower-case name is `t`. */
  private def named[E](values: Array[E], t: String): Option[E] = values.find(_.toString.toLowerCase == t)

  def decodeConfig(token: String): SheetConfig =
    import SheetConfig.*
    val raw = token.split('.').foldLeft(sheetPresets.head.config) { (c, t) =>
      lazy val picked = t.split('-').toSet.intersect(themeIdSet)
      named(Task.values, t).map(v => c.copy(task = v))
        .orElse(named(Layout.values, t).map(v => c.copy(layout = v)))
        .orElse(named(Answer.values, t).map(v => c.copy(answer = v)))
        .getOrElse(t match
          case "any" => c.copy(themes = Set.empty)
          case _ if picked.nonEmpty => c.copy(themes = normalizeThemes(picked))
          case "la0" => c.copy(lookAlikes = false)
          case "la1" => c.copy(lookAlikes = true)
          case "sc0" => c.copy(showCounts = false)
          case "sc1" => c.copy(showCounts = true)
          // toIntOption: a digit run too long for an Int must not throw.
          case NumRange(a, b) =>
            (a.toIntOption, b.toIntOption) match
              case (Some(lo), Some(hi)) => c.copy(perKindMin = lo, perKindMax = hi)
              case _                    => c
          case k if k.startsWith("k") => k.drop(1).toIntOption.fold(c)(v => c.copy(kinds = v))
          case x if x.startsWith("x") => x.drop(1).toIntOption.fold(c)(v => c.copy(extras = v))
          case _ => c
        )
    }
    val lo = clamp(raw.perKindMin, 1, MaxPerKind)
    raw.copy(
      kinds = clamp(raw.kinds, 1, MaxKinds),
      perKindMin = lo,
      perKindMax = clamp(raw.perKindMax, lo, MaxPerKind),
      extras = clamp(raw.extras, 0, MaxExtras)
    )

  private def sheetPuzzle(c: SheetConfig, rng: Random, themeId: String): Puzzle =
    val spec = Spec(
      Crowd.Extras(c.extras),
      targetTypes = c.kinds,
      countMin = c.perKindMin,
      countMax = c.perKindMax,
      sizeFactor = c.layout.sizeFactor,
      Jitter.OfCell(c.layout.jitter),
      sizeJitter = c.layout.sizeJitter,
      maxTilt = c.layout.maxTilt,
      // On: about half of the other pictures are look-alikes of the
      // targets. Without this the toggle only permits them, and with a
      // big pool they seldom land next to a target by chance.
      if c.lookAlikes then LookAlikes.Favor(0.5) else LookAlikes.Avoid,
      mirrorChance = c.layout.mirrorChance
    )
    generate(spec, PrintW, PrintH, rng, themes.find(_.themeId == themeId).getOrElse(themes.head))

  /** The theme of each page. The selected themes come in shuffled rounds:
    * each theme is used once before any theme comes back, and a new round
    * does not start with the theme that ended the last one. */
  private def themeOrder(ids: Vector[String], n: Int, rng: Random): Vector[String] =
    Iterator.iterate(Vector.empty[String]) { acc =>
      val round = rng.shuffle(ids)
      val fixed =
        if round.size > 1 && acc.lastOption.contains(round.head) then round.tail.head +: round.head +: round.drop(2)
        else round
      acc ++ fixed
    }.dropWhile(_.size < n).next().take(n)

  /** `n` different sheets from one recipe. The theme order is fixed for
    * `MaxPages` first, and one generator then makes the pages in order, so
    * page k does not change when pages are added after it. The print page,
    * the workbook and both previews all use this. */
  private def sheetPuzzles(c: SheetConfig, n: Int, seed: Long): Vector[Puzzle] =
    val rng = new Random(seed)
    val order = themeOrder(c.themeIds, math.max(n, MaxPages), rng)
    order.take(n).map(t => sheetPuzzle(c, rng, t))

  val MaxPages = 10

  /** The page count is a token in the same URL segment as the recipe
    * (`.p3`), but it is not part of [[SheetConfig]]: a workbook row has its
    * own page count. */
  private def encodePages(n: Int): String = if n == 1 then "" else s".p$n"

  private def decodePages(token: String): Int =
    token.split('.').iterator.filter(_.startsWith("p")).flatMap(_.drop(1).toIntOption).nextOption()
      .fold(1)(clamp(_, 1, MaxPages))

  /** A random theme, not `avoid` (the in-app games). */
  private def pickTheme(rng: Random, avoid: String = ""): Theme =
    val candidates = themes.filter(_.themeId != avoid)
    candidates(rng.nextInt(candidates.size))

  private def generate(spec: Spec, w: Double, h: Double, rng: Random, theme: Theme): Puzzle =
    val shuffled = rng.shuffle(theme.pool)
    val pool = spec.lookAlikes match
      case LookAlikes.Avoid    => dedupeConfusables(shuffled, theme.lookAlikes)
      case LookAlikes.Allow    => shuffled
      case LookAlikes.Favor(_) =>
        // Targets come first in the pool. Put one member of each look-alike
        // group at the front, so each target has look-alikes, and its
        // partners stay in the pool as distractors.
        val leads = rng.shuffle(theme.lookAlikes).flatMap(g => shuffled.find(g))
        leads ++ shuffled.filterNot(leads.contains)
    val targets = pool.take(spec.targetTypes).map { e =>
      Target(e, spec.countMin + rng.nextInt(spec.countMax - spec.countMin + 1))
    }
    val distractorPool = pool.drop(spec.targetTypes)
    val targetGlyphs = targets.flatMap(t => Vector.fill(t.count)(t.emoji))
    val distractorGlyphs =
      if distractorPool.isEmpty then Vector.empty
      else
        val n = spec.crowd match
          case Crowd.Total(total) => math.max(0, total - targetGlyphs.size)
          case Crowd.Extras(n)    => n
        val share = spec.lookAlikes match
          case LookAlikes.Favor(sh) => sh
          case _                    => 0.0
        val targetEmoji = targets.map(_.emoji).toSet
        val similar = distractorPool.filter(e =>
          theme.lookAlikes.exists(g => g.contains(e) && g.exists(targetEmoji)))
        Vector.fill(n) {
          val from = if similar.nonEmpty && rng.nextDouble() < share then similar else distractorPool
          from(rng.nextInt(from.size))
        }
    val glyphs = rng.shuffle(targetGlyphs ++ distractorGlyphs)

    val n = glyphs.size
    val cols = math.max(1, math.round(math.sqrt(n * w / h)).toInt)
    val rows = math.max(1, math.ceil(n.toDouble / cols).toInt)
    val cellW = w / cols
    val cellH = h / rows
    val cellMin = math.min(cellW, cellH)
    // Shuffled cells, surplus dropped at random — the holes break the grid.
    val cells = rng.shuffle((0 until cols * rows).toVector).take(n)
    val targetSet = targets.map(_.emoji).toSet

    def jittered(centre: Double, cell: Double, size: Double, lo: Double, hi: Double): Double =
      val amp = spec.jitter match
        case Jitter.OfCell(f)         => cell * f
        case Jitter.FreeSpace(boost)  => math.max(0.0, (cell - size) / 2) * boost
      math.max(lo, math.min(hi, centre + (rng.nextDouble() * 2 - 1) * amp))

    val items = glyphs.zip(cells).map { (e, c) =>
      val size = cellMin * spec.sizeFactor * (1 + (rng.nextDouble() * 2 - 1) * spec.sizeJitter)
      val x = jittered((c % cols + 0.5) * cellW, cellW, size, size / 2, w - size / 2)
      val y = jittered((c / cols + 0.5) * cellH, cellH, size, size / 2, h - size / 2)
      val tilt = (rng.nextDouble() * 2 - 1) * spec.maxTilt
      val mirror = rng.nextDouble() < spec.mirrorChance
      Item(e, x, y, size, tilt, targetSet.contains(e), mirror)
    }
    Puzzle(theme.themeId, targets, items, w, h)

  // ---------- UI ----------

  def render(): HtmlElement =
    ModeChooser.render(id, List(
      Mode(
        id = "seek",
        label = _.seekAndFind.seekMode,
        description = Some(_.seekAndFind.seekModeDesc),
        render = () => renderSeek()
      ),
      Mode(
        id = "count",
        label = _.seekAndFind.countMode,
        description = Some(_.seekAndFind.countModeDesc),
        render = () => renderCount()
      ),
      Mode(
        id = "print",
        label = _.mode.offline,
        materials = List(_.offline.materials.printer, _.offline.materials.paperPen),
        hint = Some(_.offline.seekAndFind.sheetHint),
        render = () => renderOffline()
      )
    ))

  /** The level is the path segment after the mode id; easy is the default. */
  private def levelSignal(levels: List[Level]): Signal[Level] =
    Routing.router.currentPageSignal.map {
      case Page.Activity(`id`, _ :: lvl :: _) => levels.find(_.levelId == lvl).getOrElse(levels.head)
      case _ => levels.head
    }.distinct

  private def levelPill(modeId: String, levels: List[Level], current: Signal[Level]): HtmlElement =
    div(
      cls := "center no-print",
      div(
        cls := "pill-toggle",
        levels.map { l =>
          button(
            cls := "pill-btn",
            cls("is-active") <-- current.map(_.levelId == l.levelId),
            child.text <-- AppState.strings.map(l.nameKey),
            onClick --> (_ => Routing.go(Page.Activity(id, List(modeId, l.levelId))))
          )
        }
      )
    )

  // ---------- seek mode ----------

  private def renderSeek(): HtmlElement =
    val level = levelSignal(seekLevels)
    div(
      cls := "stack-lg",
      levelPill("seek", seekLevels, level),
      p(cls := "muted center no-print", child.text <-- s(_.seekAndFind.instruction)),
      child <-- level.map(l => seekForLevel(l))
    )

  private def seekForLevel(level: Level): HtmlElement =
    val rng = new Random()
    val puzzle = Var(generate(level.app, AppW, AppH, rng, pickTheme(rng)))
    val found = Var(Set.empty[Int])

    def newGame(): Unit =
      puzzle.set(generate(level.app, AppW, AppH, rng, pickTheme(rng, avoid = puzzle.now().themeId)))
      found.set(Set.empty)

    def tap(item: Item, i: Int, el: dom.Element): Unit =
      if item.isTarget then found.update(_ + i)
      else
        // A miss gets a gentle wobble and nothing else — no penalty, and
        // never a hint: spotting the target is the whole exercise.
        el.classList.add("is-miss")
        val _ = js.timers.setTimeout(380)(el.classList.remove("is-miss"))

    val wonSig = puzzle.signal.combineWith(found.signal)
      .map((p, f) => f.size >= p.items.count(_.isTarget)).distinct

    div(
      cls := "stack-lg",
      child <-- puzzle.signal.map(p => legend(p, found.signal)),
      child <-- puzzle.signal.map(p => board(p, found.signal, Some(tap))),
      child <-- wonSig.map {
        case true => wonView(_.seekAndFind.won, () => newGame())
        case false =>
          div(
            cls := "row no-print",
            styleAttr := "justify-content: center;",
            Components.ghost(s(_.seekAndFind.newGame), newGame())
          )
      }
    )

  /** One chip per target: the emoji plus a dot per copy to find, filling in
    * as they're found — readable with zero words or numerals. */
  private def legend(p: Puzzle, found: Signal[Set[Int]]): HtmlElement =
    div(
      cls := "snf-legend no-print",
      p.targets.map { t =>
        val foundCount = found
          .map(f => p.items.zipWithIndex.count((it, i) => it.emoji == t.emoji && f(i)))
          .distinct
        div(
          cls := "snf-chip",
          cls("is-done") <-- foundCount.map(_ >= t.count),
          span(cls := "snf-chip__emoji", t.emoji),
          div(
            cls := "snf-dots",
            (0 until t.count).map(k => span(cls := "snf-dot", cls("is-on") <-- foundCount.map(_ > k)))
          )
        )
      }
    )

  /** The scene. `marked` items wear a ring; `onTap` is absent in modes where
    * the field itself isn't interactive (counting). */
  private def board(
      p: Puzzle,
      marked: Signal[Set[Int]],
      onTap: Option[(Item, Int, dom.Element) => Unit]
  ): SvgElement =
    svg.svg(
      svg.cls := "snf-board",
      svg.viewBox := s"0 0 ${fmt(p.w)} ${fmt(p.h)}",
      p.items.zipWithIndex.map { (item, i) =>
        svg.g(
          svg.cls <-- marked.map(m => if m(i) then "snf-item is-found" else "snf-item"),
          svg.transform := s"translate(${fmt(item.x)} ${fmt(item.y)}) rotate(${fmt(item.tilt)})",
          onTap.map { handler =>
            onPointerDown --> (ev => handler(item, i, ev.currentTarget.asInstanceOf[dom.Element]))
          },
          svg.circle(svg.cls := "snf-hit", svg.r := fmt(item.size * 0.55)),
          svg.g(
            svg.cls := "snf-emoji",
            svg.text(
              svg.textAnchor := "middle",
              svg.dominantBaseline := "central",
              svg.fontSize := fmt(item.size),
              item.emoji
            )
          ),
          svg.circle(svg.cls := "snf-ring", svg.r := fmt(item.size * 0.58))
        )
      }
    )

  // ---------- count mode ----------

  private def renderCount(): HtmlElement =
    val level = levelSignal(countLevels)
    div(
      cls := "stack-lg",
      levelPill("count", countLevels, level),
      p(cls := "muted center no-print", child.text <-- s(_.seekAndFind.countInstruction)),
      child <-- level.map(l => countForLevel(l))
    )

  private final case class Round(puzzle: Puzzle, options: Vector[Int])

  /** Three near-misses around the true count, sorted so the row reads as a
    * number line rather than betraying which option was inserted. */
  private def answerOptions(correct: Int, rng: Random): Vector[Int] =
    val pool = (math.max(1, correct - 3) to (correct + 3)).filterNot(_ == correct).toVector
    (rng.shuffle(pool).take(3) :+ correct).sorted

  private def countForLevel(level: Level): HtmlElement =
    val rng = new Random()

    def makeRound(avoid: String): Round =
      val p = generate(level.app, AppW, AppH, rng, pickTheme(rng, avoid))
      Round(p, answerOptions(p.targets.head.count, rng))

    val round = Var(makeRound(""))
    val solved = Var(false)
    val wrong = Var(Set.empty[Int])

    def newRound(): Unit =
      round.set(makeRound(round.now().puzzle.themeId))
      solved.set(false)
      wrong.set(Set.empty)

    div(
      cls := "stack-lg",
      child <-- round.signal.map { r =>
        val target = r.puzzle.targets.head
        // The reveal: on a correct answer every counted item gets its ring,
        // confirming the number the kid just worked out.
        val ringed = solved.signal.map { ok =>
          if ok then r.puzzle.items.zipWithIndex.collect { case (it, i) if it.isTarget => i }.toSet
          else Set.empty[Int]
        }
        div(
          cls := "stack-lg",
          div(
            cls := "snf-question no-print",
            child.text <-- AppState.strings.map(_.seekAndFind.howMany.replace("{}", target.emoji))
          ),
          board(r.puzzle, ringed, None),
          child <-- solved.signal.distinct.map {
            case true => wonView(_.seekAndFind.countWon, () => newRound())
            case false =>
              div(
                cls := "snf-answers no-print",
                r.options.map { n =>
                  button(
                    cls := "btn btn--lg snf-answer",
                    cls("is-wrong") <-- wrong.signal.map(_(n)),
                    disabled <-- wrong.signal.map(_(n)),
                    n.toString,
                    onClick --> (_ => if n == target.count then solved.set(true) else wrong.update(_ + n))
                  )
                }
              )
          }
        )
      }
    )

  private def wonView(msg: Strings => String, again: () => Unit): HtmlElement =
    div(
      cls := "handoff card",
      div(cls := "handoff__title", child.text <-- s(msg)),
      button(
        cls := "btn btn--lg",
        child.text <-- s(_.seekAndFind.newGame),
        onClick --> (_ => again())
      )
    )

  // ---------- print ----------

  /** Printed sheets do not draw the pictures as text. The print preview of
    * Chrome is not stable with a sheet full of large colour glyphs, and an
    * image keeps an exact position in the scene. Thus each picture is an
    * image made from the picture font (`Pictures.src`), which also follows
    * the black-and-white switch. */

  // ---------- workbook ----------

  /** The settings token of a preset, for workbook rows. */
  def presetToken(presetId: String): String = encodeConfig(presetConfig(presetId))

  /** The settings token of the classic counting sheet. */
  val countSheetToken: String = encodeConfig(countSheetConfig)

  /** The row label: the preset name, "Counting" for the counting sheet, or
    * "Custom" after a change in the form. */
  def tokenSummary(token: String): Strings => String =
    val c = decodeConfig(token)
    sheetPresets.find(_.config == c).map(_.name)
      .orElse(Option.when(c == countSheetConfig)(_.offline.seekAndFind.countSheetName))
      .getOrElse(_.offline.seekAndFind.form.custom)

  def tokenTitle(token: String): Strings => String = sheetTitle(decodeConfig(token))

  /** All `n` pages of a workbook row: the same generator as the print page,
    * so the themes come in rounds there too. */
  def workbookPages(token: String, n: Int, rng: Random): Seq[HtmlElement] =
    val c = decodeConfig(token)
    sheetPuzzles(c, n, rng.nextLong()).map(sheetBody(c, _))

  /** The workbook row editor: presets, the form and a live preview of one
    * page. `token` is a two-way view of the row's settings, so a change
    * from outside (a book preset) shows in the form at once. */
  def settingsEditor(token: Var[String]): HtmlElement =
    val config = token.zoomLazy(decodeConfig)((_, c) => encodeConfig(c))
    val seed = Var(Random.nextLong())
    val sheet = config.signal.distinct.combineWith(seed.signal).map((c, sd) => page(c, sheetPuzzles(c, 1, sd).head))
    div(
      cls := "snf-builder snf-builder--embedded",
      previewBox(sheet, div(cls := "row snf-builder__actions", newSceneButton(seed))),
      div(cls := "stack snf-builder__form", presetPills(config), sheetForm(config))
    )

  /** The sheet title that agrees with the recipe. */
  def sheetTitle(c: SheetConfig): Strings => String = (c.task, c.answer) match
    case (Task.Find, _)             => _.offline.seekAndFind.printTitle
    case (Task.Count, Answer.Circle) => _.offline.seekAndFind.countPrintTitle
    case (Task.Count, Answer.Write)  => _.offline.seekAndFind.form.countWriteTitle

  private def sheetBody(c: SheetConfig, p: Puzzle): HtmlElement = c.task match
    case Task.Find  => seekSheet(p, c.showCounts)
    case Task.Count => countSheet(p, c.answer)

  private def page(c: SheetConfig, p: Puzzle): HtmlElement =
    Printable.render(title = sheetTitle(c), body = sheetBody(c, p))

  /** The scaled-down live page, with action rows under it. */
  private def previewBox(sheet: Signal[HtmlElement], below: HtmlElement*): HtmlElement =
    div(
      cls := "snf-builder__preview stack",
      div(
        cls := "snf-preview",
        dataAttr("testid") := "snf-preview",
        div(cls := "snf-preview__page", child <-- sheet)
      ),
      below
    )

  private def newSceneButton(seed: Var[Long]): HtmlElement =
    Components.ghost(s(_.seekAndFind.newGame), seed.set(Random.nextLong()))

  /** The sheet builder: presets, a form to change the recipe, and a live
    * preview. The preview and the printed page come from the same recipe
    * and the same seed, so the print is exactly what the preview shows. */
  private def renderOffline(): HtmlElement =
    // The segment after "print" holds the recipe (see `encodeConfig`).
    val urlToken = Routing.router.currentPageSignal.now() match
      case Page.Activity(`id`, "print" :: token :: _) => Some(token)
      case _ => None
    val config = Var(urlToken.fold(sheetPresets.head.config)(decodeConfig))
    val pages = Var(urlToken.fold(1)(decodePages))
    val seed = Var(Random.nextLong())
    // The page that the preview shows, 0-based. `setPages` keeps it in range.
    val shown = Var(0)
    // The print copies exist only while printing: building every page in
    // hidden DOM on each change in the form is wasted work.
    val printing = Var(false)
    val sheets = config.signal.combineWith(pages.signal, seed.signal).map { (c, n, sd) =>
      (c, sheetPuzzles(c, n, sd))
    }
    def setPages(n: Int): Unit =
      pages.set(n)
      shown.update(i => math.min(i, n - 1))
    def pagerButton(testId: String, label: String, isDisabled: Signal[Boolean], go: Int => Int): HtmlElement =
      Components.ghost(Val(label), shown.update(go), isDisabled).amend(
        typ := "button",
        cls := "snf-pager__btn",
        dataAttr("testid") := testId
      )

    div(
      cls := "stack-lg",
      // Keep the URL in step with the form, so the page can be bookmarked
      // or shared. Replace, not push: each tap must not add a history entry.
      config.signal.combineWith(pages.signal).changes --> { (c, n) =>
        Routing.replace(Page.Activity(id, List("print", encodeConfig(c) + encodePages(n))))
      },
      // Ctrl+P and the browser menu print too, not only the Print button.
      windowEvents(_.onBeforePrint) --> (_ => printing.set(true)),
      windowEvents(_.onAfterPrint) --> (_ => printing.set(false)),
      div(
        cls := "no-print",
        RulesCard.render(List(RulesCard.fromRules(_.offline.seekAndFind.rules)))
      ),
      div(cls := "no-print", presetPills(config)),
      div(
        cls := "snf-builder no-print",
        previewBox(
          sheets.combineWith(shown.signal).map((c, ps, i) => page(c, ps(i))),
          // Flip through the pages; hidden for a single page.
          div(
            cls := "row snf-pager",
            cls("is-hidden") <-- pages.signal.map(_ == 1),
            pagerButton("snf-page-prev", "‹", shown.signal.map(_ == 0), _ - 1),
            span(
              cls := "snf-pager__label",
              dataAttr("testid") := "snf-page-label",
              child.text <-- shown.signal.combineWith(pages.signal).map((i, n) => s"${i + 1} / $n")
            ),
            pagerButton("snf-page-next", "›", shown.signal.combineWith(pages.signal).map((i, n) => i >= n - 1), _ + 1)
          ),
          div(
            cls := "row snf-builder__actions",
            span(cls := "snf-form__label", child.text <-- s(_.offline.seekAndFind.form.pages)),
            Components.stepper(
              pages.signal,
              pages.signal.map(_ > 1),
              pages.signal.map(_ < MaxPages),
              () => setPages(pages.now() - 1),
              () => setPages(pages.now() + 1),
              testId = "snf-pages"
            )
          ),
          Pictures.bwToggle(),
          div(
            cls := "row snf-builder__actions",
            newSceneButton(seed),
            Components.primary(s(_.printable.print), {
              // Mounted synchronously, so the pages are there before the
              // print snapshot; beforeprint does the same for Ctrl+P.
              printing.set(true)
              Printable.print()
            }).amend(dataAttr("testid") := "snf-print")
          )
        ),
        sheetForm(config)
      ),
      div(
        cls := "print-only",
        children <-- printing.signal.combineWith(sheets).map { (on, c, ps) =>
          if on then ps.map(p => page(c, p)) else Nil
        }
      )
    )

  // ---------- sheet builder form ----------

  /** The presets row. A preset is active only while the recipe is equal to
    * it; after a change in the form, no preset is active. */
  def presetPills(config: Var[SheetConfig]): HtmlElement =
    div(
      cls := "snf-presets",
      span(cls := "snf-form__label", child.text <-- s(_.offline.seekAndFind.form.preset)),
      pills("snf-preset", config.signal, sheetPresets.map(pr => (pr.config, pr.presetId, s(pr.name))), config.set)
    )

  /** The form. It reads and writes only `config`, so a different host (a
    * workbook row) can supply its own Var. */
  def sheetForm(config: Var[SheetConfig]): HtmlElement =
    import SheetConfig.*
    val c = config.signal
    def form(f: Strings.SeekAndFindForm => String): Strings => String = str => f(str.offline.seekAndFind.form)
    /** A stepper for one field; the limits may depend on other fields. */
    def fieldStepper(testId: String, get: SheetConfig => Int, lo: SheetConfig => Int, hi: SheetConfig => Int, set: (SheetConfig, Int) => SheetConfig): HtmlElement =
      Components.stepper(
        c.map(get),
        c.map(k => get(k) > lo(k)),
        c.map(k => get(k) < hi(k)),
        () => config.update(k => set(k, get(k) - 1)),
        () => config.update(k => set(k, get(k) + 1)),
        testId
      )
    def flag(testId: String, label: Strings => String, get: SheetConfig => Boolean, flip: SheetConfig => SheetConfig): HtmlElement =
      formRow(label, Components.switch(c.map(get), s(label), () => config.update(flip), testId))
    // While the slider is dragged, only its number follows; the recipe
    // changes on release. Each recipe change regenerates the preview and
    // writes the URL or the workbook, which is too much per input event.
    val dragging = Var(Option.empty[Int])

    div(
      cls := "snf-form",
      formRow(form(_.task), pills("snf-task", c.map(_.task), List(
        (Task.Find, "find", s(form(_.taskFind))),
        (Task.Count, "count", s(form(_.taskCount)))
      ), t => config.update(_.copy(task = t)))),
      // Multi-select: pages go through the selected themes (see themeOrder).
      formRow(form(_.theme), div(
        cls := "snf-chips",
        chip("snf-theme-all", c.map(_.themes.isEmpty), s(form(_.themeAll)),
          () => config.update(_.copy(themes = Set.empty))),
        themes.map { t =>
          chip(s"snf-theme-${t.themeId}", c.map(_.themes(t.themeId)), Val(t.pool.head),
            () => config.update(_.withTheme(t.themeId)))
        }
      )),
      formRow(form(_.kinds), fieldStepper("snf-kinds", _.kinds, _ => 1, _ => MaxKinds, (k, v) => k.copy(kinds = v))),
      formRow(form(_.perKind), div(
        cls := "row snf-form__range",
        fieldStepper("snf-per-min", _.perKindMin, _ => 1, _.perKindMax, (k, v) => k.copy(perKindMin = v)),
        span("–"),
        fieldStepper("snf-per-max", _.perKindMax, _.perKindMin, _ => MaxPerKind, (k, v) => k.copy(perKindMax = v))
      )),
      formRow(form(_.extras), div(
        cls := "row snf-form__range",
        input(
          typ := "range",
          dataAttr("testid") := "snf-extras",
          minAttr := "0",
          maxAttr := MaxExtras.toString,
          stepAttr := ExtrasStep.toString,
          value <-- c.map(_.extras.toString),
          onInput.mapToValue --> (v => dragging.set(v.toIntOption)),
          onChange.mapToValue --> { v =>
            dragging.set(None)
            config.update(_.copy(extras = v.toIntOption.getOrElse(0)))
          }
        ),
        span(cls := "snf-form__value", child.text <-- c.combineWith(dragging.signal).map((k, d) => d.getOrElse(k.extras).toString))
      )),
      formRow(form(_.layout), pills("snf-layout", c.map(_.layout), List(
        (Layout.Tidy, "tidy", s(form(_.tidy))),
        (Layout.Mixed, "mixed", s(form(_.mixed))),
        (Layout.Wild, "wild", s(form(_.wild)))
      ), l => config.update(_.copy(layout = l)))),
      flag("snf-look-alikes", form(_.lookAlikes), _.lookAlikes, k => k.copy(lookAlikes = !k.lookAlikes)),
      // The last row depends on the task.
      child <-- c.map(_.task).distinct.map {
        case Task.Find =>
          flag("snf-show-counts", form(_.showCounts), _.showCounts, k => k.copy(showCounts = !k.showCounts))
        case Task.Count =>
          formRow(form(_.answer), pills("snf-answer", c.map(_.answer), List(
            (Answer.Circle, "circle", s(form(_.answerCircle))),
            (Answer.Write, "write", s(form(_.answerWrite)))
          ), a => config.update(_.copy(answer = a))))
      }
    )

  private def formRow(label: Strings => String, control: HtmlElement): HtmlElement =
    div(
      cls := "snf-form__row",
      span(cls := "snf-form__label", child.text <-- s(label)),
      control
    )

  private def pills[A](
      testId: String,
      current: Signal[A],
      options: List[(A, String, Signal[String])],
      pick: A => Unit
  ): HtmlElement =
    div(
      cls := "pill-toggle",
      options.map { (value, key, label) =>
        button(
          typ := "button",
          cls := "pill-btn",
          dataAttr("testid") := s"$testId-$key",
          cls("is-active") <-- current.map(_ == value),
          child.text <-- label,
          onClick --> (_ => pick(value))
        )
      }
    )

  private def chip(testId: String, on: Signal[Boolean], label: Signal[String], tap: () => Unit): HtmlElement =
    button(
      typ := "button",
      cls := "snf-chip-btn",
      dataAttr("testid") := testId,
      cls("is-active") <-- on,
      aria.pressed <-- on.map(_.toString),
      child.text <-- label,
      onClick --> (_ => tap())
    )

  // ---------- sheet bodies ----------

  private def seekSheet(p: Puzzle, showCounts: Boolean): HtmlElement =
    div(
      cls := "snf-print",
      dataAttr("theme") := p.themeId,
      div(
        cls := "snf-print-legend",
        span(cls := "snf-print-legend__label", child.text <-- s(_.offline.seekAndFind.findLabel)),
        p.targets.map(t =>
          span(
            cls := "snf-print-chip",
            img(cls := "snf-print-emoji", src <-- Pictures.src(t.emoji), alt := t.emoji),
            if showCounts then s" × ${t.count}" else ""
          )
        )
      ),
      printField(p)
    )

  /** The numbers printed next to each kind on the counting sheet. The range
    * must include every count that a recipe can deal. */
  private val CountChoices: Range = 1 to SheetConfig.MaxPerKind

  private def countSheet(p: Puzzle, answer: Answer): HtmlElement =
    div(
      cls := "snf-print",
      dataAttr("theme") := p.themeId,
      div(
        cls := "snf-print-legend",
        span(cls := "snf-print-legend__label", child.text <-- s(str => answer match
          case Answer.Circle => str.offline.seekAndFind.countLabel
          case Answer.Write  => str.offline.seekAndFind.form.countWriteLabel
        ))
      ),
      printField(p),
      div(
        cls := "snf-count-row",
        cls("snf-count-row--write") := answer == Answer.Write,
        p.targets.map(t =>
          div(
            cls := "snf-count-cell",
            img(cls := "snf-print-emoji", src <-- Pictures.src(t.emoji), alt := t.emoji),
            answer match
              // The child circles the answer. Pre-readers do not know how
              // to write digits, and the ordered line lets them count along
              // it to find a number that they do not recognize.
              case Answer.Circle => CountChoices.map(n => span(cls := "snf-count-num", n.toString))
              case Answer.Write  => List(span(cls := "snf-count-box"))
          )
        )
      )
    )

  private def printField(p: Puzzle): SvgElement =
    svg.svg(
      svg.cls := "snf-print-field",
      svg.viewBox := s"0 0 ${fmt(p.w)} ${fmt(p.h)}",
      // Big pictures first, so a small one that overlaps a big one is on
      // top and never hidden completely.
      p.items.sortBy(-_.size).map { item =>
        // The canvas glyph fills ~0.8 of its raster square, so the image box
        // is upscaled to keep the visible emoji at the item's size.
        val side = item.size * 1.25
        val flip = if item.mirror then " scale(-1 1)" else ""
        svg.image(
          svg.x := fmt(-side / 2),
          svg.y := fmt(-side / 2),
          svg.width := fmt(side),
          svg.height := fmt(side),
          svg.transform := s"translate(${fmt(item.x)} ${fmt(item.y)}) rotate(${fmt(item.tilt)})$flip",
          svg.href <-- Pictures.src(item.emoji)
        )
      }
    )

  private def fmt(v: Double): String = f"$v%.2f"
