package tandu.activities

import com.raquo.laminar.api.L.*
import org.scalajs.dom
import tandu.{AppState, Page, Routing}
import tandu.i18n.Strings
import tandu.ui.{Components, Mode, ModeChooser, Printable, RulesCard}
import tandu.ui.Components.s

import scala.collection.mutable
import scala.scalajs.js
import scala.util.Random

/** Seek & find: a busy themed emoji scene hiding a handful of target
  * pictures. Two in-app games (find-them-all and how-many counting) plus
  * printable find-and-circle / count-and-write sheets.
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

  private final case class Theme(themeId: String, pool: Vector[String])

  /** Pools stick to long-established emoji so every platform's font has
    * them. ~15 per theme: enough distinct distractors for three targets. */
  private val themes: Vector[Theme] = Vector(
    Theme("ocean", Vector("🐟", "🐠", "🐡", "🦈", "🐬", "🐳", "🐙", "🦑", "🦀", "🦞", "🦐", "🐚", "🐢", "⚓", "⛵")),
    Theme("forest", Vector("🦊", "🐺", "🦌", "🐻", "🐿️", "🦔", "🦉", "🐗", "🍄", "🌰", "🦅", "🐍", "🐸", "🦋", "🐛")),
    Theme("farm", Vector("🐮", "🐷", "🐔", "🐤", "🦆", "🐑", "🐐", "🐴", "🐰", "🚜", "🌽", "🥕", "🍎", "🌻", "🐶")),
    Theme("space", Vector("🚀", "🛸", "⭐", "🌟", "☄️", "🌙", "🪐", "🌍", "👽", "🤖", "🛰️", "☀️", "💫", "🔭")),
    Theme("sweets", Vector("🍩", "🍪", "🧁", "🍰", "🍭", "🍬", "🍫", "🍦", "🍓", "🍒", "🥨", "🍿", "🍉", "🧃")),
    Theme("garden", Vector("🐝", "🦋", "🐞", "🐛", "🐌", "🐜", "🕷️", "🌸", "🌷", "🌻", "🍀", "🐸", "🐦", "🍇"))
  )

  // ---------- model & generator ----------

  final case class Target(emoji: String, count: Int)
  final case class Item(emoji: String, x: Double, y: Double, size: Double, tilt: Double, isTarget: Boolean)
  final case class Puzzle(themeId: String, targets: Vector[Target], items: Vector[Item], w: Double, h: Double)

  /** One field recipe. `total` is the item count *including* targets; a
    * total at or below the dealt target count means a distractor-free field
    * (the counting sheet). `sizeFactor` scales items relative to their grid
    * cell; `jitterBoost` > 1 lets offsets exceed the cell's free space
    * (crowding); `maxTilt` is degrees either way. `distinctKinds` keeps
    * confusable look-alikes off the field — set for counting, where a kid
    * conflating ⭐ with 🌟 gets judged wrong through no fault; in seek mode
    * look-alikes are welcome difficulty. */
  private final case class Spec(
      total: Int,
      targetTypes: Int,
      countMin: Int,
      countMax: Int,
      sizeFactor: Double,
      jitterBoost: Double,
      sizeJitter: Double,
      maxTilt: Double,
      distinctKinds: Boolean = false
  )

  /** Within-theme pairs/trios young kids genuinely mix up at a glance. */
  private val confusables = Vector(
    Set("⭐", "🌟", "💫"),
    Set("🐟", "🐠", "🐡"),
    Set("🦊", "🐺"),
    Set("🐬", "🦈")
  )

  /** Keep only the first-drawn member of each confusable group. */
  private def dedupeConfusables(pool: Vector[String]): Vector[String] =
    val taken = mutable.Set.empty[Int]
    pool.filter { e =>
      confusables.indexWhere(_.contains(e)) match
        case -1 => true
        case g  => taken.add(g)
    }

  private final case class Level(levelId: String, app: Spec, print: Option[Spec] = None, nameKey: Strings => String)

  /** Difficulty turns three knobs at once: density, number of target kinds,
    * and how much sizes/tilts vary (uniform rows are easy to scan; jitter
    * defeats that). Print fields run denser — A4 is big. */
  private val seekLevels: List[Level] = List(
    Level(
      "easy",
      app = Spec(total = 28, targetTypes = 1, countMin = 4, countMax = 6, sizeFactor = 0.72, jitterBoost = 1.15, sizeJitter = 0.12, maxTilt = 0),
      print = Some(Spec(total = 48, targetTypes = 1, countMin = 5, countMax = 7, sizeFactor = 0.72, jitterBoost = 1.15, sizeJitter = 0.12, maxTilt = 0)),
      nameKey = _.seekAndFind.easy
    ),
    Level(
      "medium",
      app = Spec(total = 54, targetTypes = 2, countMin = 3, countMax = 5, sizeFactor = 0.80, jitterBoost = 1.15, sizeJitter = 0.15, maxTilt = 8),
      print = Some(Spec(total = 80, targetTypes = 2, countMin = 4, countMax = 6, sizeFactor = 0.80, jitterBoost = 1.15, sizeJitter = 0.15, maxTilt = 8)),
      nameKey = _.seekAndFind.medium
    ),
    Level(
      "hard",
      app = Spec(total = 88, targetTypes = 3, countMin = 3, countMax = 5, sizeFactor = 0.88, jitterBoost = 1.4, sizeJitter = 0.22, maxTilt = 18),
      print = Some(Spec(total = 120, targetTypes = 3, countMin = 4, countMax = 6, sizeFactor = 0.88, jitterBoost = 1.4, sizeJitter = 0.22, maxTilt = 18)),
      nameKey = _.seekAndFind.hard
    )
  )

  /** Counting keeps fields sparser than seeking at the same tier — the work
    * is keeping track, not spotting — and grows the answer instead. */
  private val countLevels: List[Level] = List(
    Level("easy", app = Spec(18, 1, 3, 6, 0.72, 1.0, 0.08, 0, distinctKinds = true), nameKey = _.seekAndFind.easy),
    Level("medium", app = Spec(32, 1, 5, 9, 0.78, 1.1, 0.15, 8, distinctKinds = true), nameKey = _.seekAndFind.medium),
    Level("hard", app = Spec(55, 1, 7, 12, 0.85, 1.3, 0.22, 15, distinctKinds = true), nameKey = _.seekAndFind.hard)
  )

  /** The count-and-write sheet: every kind on the field is counted, so no
    * distractors at all (total = 0 → targets only). */
  private val countSheetSpec = Spec(total = 0, targetTypes = 4, countMin = 4, countMax = 9, sizeFactor = 0.70, jitterBoost = 1.0, sizeJitter = 0.10, maxTilt = 0, distinctKinds = true)

  private def generate(spec: Spec, w: Double, h: Double, rng: Random, avoidTheme: String = ""): Puzzle =
    val candidates = themes.filter(_.themeId != avoidTheme)
    val theme = candidates(rng.nextInt(candidates.size))
    val shuffled = rng.shuffle(theme.pool)
    val pool = if spec.distinctKinds then dedupeConfusables(shuffled) else shuffled
    val targets = pool.take(spec.targetTypes).map { e =>
      Target(e, spec.countMin + rng.nextInt(spec.countMax - spec.countMin + 1))
    }
    val distractorPool = pool.drop(spec.targetTypes)
    val targetGlyphs = targets.flatMap(t => Vector.fill(t.count)(t.emoji))
    val distractorGlyphs =
      if distractorPool.isEmpty then Vector.empty
      else Vector.fill(math.max(0, spec.total - targetGlyphs.size))(distractorPool(rng.nextInt(distractorPool.size)))
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
      val amp = math.max(0.0, (cell - size) / 2) * spec.jitterBoost
      math.max(lo, math.min(hi, centre + (rng.nextDouble() * 2 - 1) * amp))

    val items = glyphs.zip(cells).map { (e, c) =>
      val size = cellMin * spec.sizeFactor * (1 + (rng.nextDouble() * 2 - 1) * spec.sizeJitter)
      val x = jittered((c % cols + 0.5) * cellW, cellW, size, size / 2, w - size / 2)
      val y = jittered((c / cols + 0.5) * cellH, cellH, size, size / 2, h - size / 2)
      val tilt = (rng.nextDouble() * 2 - 1) * spec.maxTilt
      Item(e, x, y, size, tilt, targetSet.contains(e))
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
    val puzzle = Var(generate(level.app, AppW, AppH, rng))
    val found = Var(Set.empty[Int])

    def newGame(): Unit =
      puzzle.set(generate(level.app, AppW, AppH, rng, avoidTheme = puzzle.now().themeId))
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
      val p = generate(level.app, AppW, AppH, rng, avoid)
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

  /** Chrome's print preview is crash-prone rasterising sheets full of large
    * colour-emoji glyphs (macOS's emoji font is bitmap-based); the screen
    * renderer is fine. So printed sheets never draw emoji as text — each
    * distinct emoji is rasterised once to a PNG data URL (cached for the
    * session) and placed as an image, the same dodge dot-to-dot uses. */
  private val rasterCache = mutable.Map.empty[String, String]
  private val RasterPx = 192

  private def emojiUrl(emoji: String): String =
    rasterCache.getOrElseUpdate(emoji, {
      val c = dom.document.createElement("canvas").asInstanceOf[dom.HTMLCanvasElement]
      c.width = RasterPx
      c.height = RasterPx
      val ctx = c.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
      ctx.textAlign = "center"
      ctx.textBaseline = "middle"
      ctx.font = s"${(RasterPx * 0.8).toInt}px sans-serif"
      ctx.fillText(emoji, RasterPx / 2.0, RasterPx / 2.0)
      c.toDataURL("image/png")
    })

  private def renderOffline(): HtmlElement =
    val sheet: Var[Option[HtmlElement]] = Var(None)
    val rng = new Random()

    def printJob(title: Strings => String, body: HtmlElement): Unit =
      sheet.set(Some(Printable.render(title = title, body = body)))
      val _ = js.timers.setTimeout(50)(Printable.print())

    div(
      cls := "stack-lg",
      div(
        cls := "no-print",
        RulesCard.render(List(RulesCard.fromRules(_.offline.seekAndFind.rules)))
      ),
      div(
        cls := "no-print stack-lg snf-print-actions",
        seekLevels.map { l =>
          button(
            cls := "btn btn--lg btn--block",
            child.text <-- AppState.strings.map(str => s"${str.printable.print} — ${l.nameKey(str)}"),
            onClick --> { _ =>
              val spec = l.print.getOrElse(l.app)
              printJob(_.offline.seekAndFind.printTitle, seekSheet(generate(spec, PrintW, PrintH, rng)))
            }
          )
        },
        button(
          cls := "btn btn--lg btn--block",
          child.text <-- AppState.strings.map(str => s"${str.printable.print} — ${str.offline.seekAndFind.countSheetName}"),
          onClick --> { _ =>
            printJob(_.offline.seekAndFind.countPrintTitle, countSheet(generate(countSheetSpec, PrintW, PrintH, rng)))
          }
        )
      ),
      div(
        cls := "print-only",
        child <-- sheet.signal.map(_.getOrElse(div()))
      )
    )

  private def seekSheet(p: Puzzle): HtmlElement =
    div(
      cls := "snf-print",
      div(
        cls := "snf-print-legend",
        span(cls := "snf-print-legend__label", child.text <-- s(_.offline.seekAndFind.findLabel)),
        p.targets.map(t =>
          span(
            cls := "snf-print-chip",
            img(cls := "snf-print-emoji", src := emojiUrl(t.emoji), alt := t.emoji),
            s" × ${t.count}"
          )
        )
      ),
      printField(p)
    )

  private def countSheet(p: Puzzle): HtmlElement =
    div(
      cls := "snf-print",
      div(
        cls := "snf-print-legend",
        span(cls := "snf-print-legend__label", child.text <-- s(_.offline.seekAndFind.countLabel))
      ),
      printField(p),
      div(
        cls := "snf-count-row",
        p.targets.map(t =>
          div(
            cls := "snf-count-cell",
            img(cls := "snf-print-emoji", src := emojiUrl(t.emoji), alt := t.emoji),
            span(cls := "snf-count-box")
          )
        )
      )
    )

  private def printField(p: Puzzle): SvgElement =
    svg.svg(
      svg.cls := "snf-print-field",
      svg.viewBox := s"0 0 ${fmt(p.w)} ${fmt(p.h)}",
      p.items.map { item =>
        // The canvas glyph fills ~0.8 of its raster square, so the image box
        // is upscaled to keep the visible emoji at the item's size.
        val side = item.size * 1.25
        svg.image(
          svg.x := fmt(item.x - side / 2),
          svg.y := fmt(item.y - side / 2),
          svg.width := fmt(side),
          svg.height := fmt(side),
          svg.transform := s"rotate(${fmt(item.tilt)} ${fmt(item.x)} ${fmt(item.y)})",
          svg.href := emojiUrl(item.emoji)
        )
      }
    )

  private def fmt(v: Double): String = f"$v%.2f"
