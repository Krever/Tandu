package tandu.activities

import com.raquo.laminar.api.L.*
import org.scalajs.dom
import tandu.{AppState, Page, Routing}
import tandu.i18n.Strings
import tandu.ui.{Components, Mode, ModeChooser, Printable, RulesCard}
import tandu.ui.Components.s
import tandu.ui.DomExt.*

import scala.collection.mutable
import scala.scalajs.js
import scala.util.Random

/** Guide the Robot — a gentle first taste of "coding". The child assembles a
  * program, then runs it: the robot marches it out one step at a time and
  * either reaches its flag or bumps into a wall.
  *
  * Two control schemes, by difficulty:
  *  - Easy uses absolute arrows (↑↓←→) — the youngest just trace the path.
  *  - Medium and hard switch to a turning robot (forward / turn-left /
  *    turn-right), which forces perspective-taking: the child must track which
  *    way the robot faces. That's the richer skill, and it sets this apart from
  *    the Maze activity. */
object GuideRobot extends Activity:
  val id = "guide-robot"
  def name(s: Strings): String = s.guideRobot.name
  def description(s: Strings): String = s.guideRobot.description
  val minPlayers: Int = 1
  val maxPlayers: Int = 1
  val glyph: String = "🤖"
  val tint: String = "sky"

  // ---------- model ----------

  enum Dir(val dr: Int, val dc: Int, val arrow: String):
    case Up    extends Dir(-1, 0, "↑")
    case Down  extends Dir( 1, 0, "↓")
    case Left  extends Dir( 0, -1, "←")
    case Right extends Dir( 0, 1, "→")

    /** Quarter-turn clockwise / anticlockwise — the robot's heading after a
      * turn-right / turn-left command. */
    def cw: Dir = this match
      case Up => Right; case Right => Down; case Down => Left; case Left => Up
    def ccw: Dir = this match
      case Up => Left; case Left => Down; case Down => Right; case Right => Up

    def css: String = toString.toLowerCase

  /** A single program instruction. Absolute moves drive the easy (arrows) tier;
    * the relative trio drives the turning tiers. A program is homogeneous — the
    * variant decides which kind the palette emits. */
  enum Cmd(val arrow: String):
    case Step(dir: Dir) extends Cmd(dir.arrow)
    case Forward        extends Cmd("↑")
    case TurnLeft       extends Cmd("↺")
    case TurnRight      extends Cmd("↻")

  private type Cell = (Int, Int)

  /** Where the robot is and which way it faces. Facing only matters in the
    * turning tiers, but we carry it everywhere so one code path serves both. */
  final case class Bot(cell: Cell, facing: Dir)

  /** A board: the robot starts at `start` facing `startFacing`, must reach
    * `goal`, optionally picking up `star` on the way; `walls` are impassable. */
  final case class Puzzle(size: Int, start: Cell, startFacing: Dir, goal: Cell, star: Option[Cell], walls: Set[Cell]):
    def inBounds(c: Cell): Boolean = c._1 >= 0 && c._1 < size && c._2 >= 0 && c._2 < size
    def passable(c: Cell): Boolean = inBounds(c) && !walls.contains(c)

  /** The robot states as it executes a program, start first. A turn yields a
    * new state in the same cell (so the robot visibly rotates); the run stops
    * early when a move would leave the grid or hit a wall — that aborted move is
    * the crash. */
  final case class Run(states: Vector[Bot], crashed: Boolean):
    def end: Cell = states.last.cell
    def gotStar(star: Option[Cell]): Boolean = star.forall(sc => states.exists(_.cell == sc))

  def simulate(p: Puzzle, program: Vector[Cmd]): Run =
    var states  = Vector(Bot(p.start, p.startFacing))
    var crashed = false
    var i       = 0
    def advance(d: Dir): Unit =
      val (r, c) = states.last.cell
      val next   = (r + d.dr, c + d.dc)
      if p.passable(next) then states = states :+ Bot(next, d) else crashed = true
    while i < program.length && !crashed do
      program(i) match
        case Cmd.Step(d)    => advance(d)
        case Cmd.Forward    => advance(states.last.facing)
        case Cmd.TurnLeft   => states = states :+ states.last.copy(facing = states.last.facing.ccw)
        case Cmd.TurnRight  => states = states :+ states.last.copy(facing = states.last.facing.cw)
      i += 1
    Run(states, crashed)

  enum Outcome:
    case Won, Crashed, MissedStar, Missed

  def outcome(p: Puzzle, run: Run): Outcome =
    if run.crashed then Outcome.Crashed
    else if run.end != p.goal then Outcome.Missed
    else if !run.gotStar(p.star) then Outcome.MissedStar
    else Outcome.Won

  private enum Phase:
    case Editing, Running, Done

  // ---------- generator ----------

  private def manhattan(a: Cell, b: Cell): Int =
    math.abs(a._1 - b._1) + math.abs(a._2 - b._2)

  private def allCells(size: Int): Vector[Cell] =
    (for r <- 0 until size; c <- 0 until size yield (r, c)).toVector

  /** Breadth-first reachability over the free cells. Turning is free and always
    * available, so a turning robot can reach exactly the same cells as a
    * sliding one — generation is identical for both schemes. */
  private def reachable(p: Puzzle, from: Cell, to: Cell): Boolean =
    if !p.passable(from) || !p.passable(to) then false
    else
      val seen = mutable.Set[Cell](from)
      val q    = mutable.Queue[Cell](from)
      var found = false
      while q.nonEmpty && !found do
        val cur = q.dequeue()
        if cur == to then found = true
        else
          for d <- Dir.values do
            val nc = (cur._1 + d.dr, cur._2 + d.dc)
            if p.passable(nc) && !seen.contains(nc) then
              seen += nc
              q.enqueue(nc)
      found

  /** One randomised attempt: place start/goal (and a star) far enough apart,
    * scatter walls, and keep it only if every required leg is still walkable.
    * In turning tiers the robot starts facing a random direction — telegraphed
    * by the heading arrow, it adds a small "which way am I pointing?" step. */
  private def tryGen(v: Variant, rng: Random): Option[Puzzle] =
    def randCell(): Cell = (rng.nextInt(v.size), rng.nextInt(v.size))
    val start = randCell()
    val goal  = randCell()
    if start == goal || manhattan(start, goal) < v.minDist then None
    else
      val reserved = mutable.Set[Cell](start, goal)
      val star =
        if !v.hasStar then None
        else
          val candidates = allCells(v.size).filterNot(reserved.contains)
          Option.when(candidates.nonEmpty) {
            val c = candidates(rng.nextInt(candidates.size))
            reserved += c
            c
          }
      if v.hasStar && star.isEmpty then None
      else
        val walls = mutable.Set[Cell]()
        var tries = 0
        val cap   = v.size * v.size * 4
        while walls.size < v.walls && tries < cap do
          val c = randCell()
          if !reserved.contains(c) then walls += c
          tries += 1
        val facing = if v.turns then Dir.values(rng.nextInt(Dir.values.length)) else Dir.Right
        val puzzle = Puzzle(v.size, start, facing, goal, star, walls.toSet)
        val solvable = reachable(puzzle, start, goal) &&
          star.forall(sc => reachable(puzzle, start, sc) && reachable(puzzle, sc, goal))
        Option.when(solvable)(puzzle)

  /** A guaranteed-solvable, wall-free fallback in the vanishingly unlikely event
    * the random search never lands a valid board. */
  private def fallback(v: Variant): Puzzle =
    Puzzle(v.size, (0, 0), Dir.Right, (v.size - 1, v.size - 1),
      Option.when(v.hasStar)((0, v.size - 1)), Set.empty)

  def generate(v: Variant, rng: Random = new Random()): Puzzle =
    var result: Option[Puzzle] = None
    var attempt = 0
    while result.isEmpty && attempt < 500 do
      result = tryGen(v, rng)
      attempt += 1
    result.getOrElse(fallback(v))

  // ---------- variants ----------

  final case class Variant(
      id: String,
      size: Int,
      walls: Int,
      hasStar: Boolean,
      turns: Boolean,
      minDist: Int,
      perPage: Int,
      writeBoxes: Int,
      nameKey: Strings => String,
      descKey: Strings => String
  )

  val variants: List[Variant] = List(
    Variant("easy",   5, 0, false, false, 4, 6, 10, _.guideRobot.easy.name,   _.guideRobot.easy.description),
    Variant("medium", 6, 6, false, true,  5, 4, 16, _.guideRobot.medium.name, _.guideRobot.medium.description),
    Variant("hard",   6, 5, true,  true,  4, 4, 18, _.guideRobot.hard.name,   _.guideRobot.hard.description)
  )

  // ---------- UI ----------

  // Discrete cell-to-cell hops (and in-place turns); slow enough that a young
  // child can follow the robot and read its own program back, step by step.
  private val StepMs = 420

  def render(): HtmlElement =
    ModeChooser.render(id, List(
      Mode(
        id = "in-app",
        label = _.mode.inApp,
        render = () => renderPlay()
      ),
      Mode(
        id = "print",
        label = _.mode.offline,
        materials = List(_.offline.materials.printer, _.offline.materials.paperPen),
        hint = Some(_.offline.guideRobot.sheetHint),
        render = () => renderOffline()
      )
    ))

  private val DefaultVariant = variants(0)
  private val variantSignal: Signal[Variant] =
    Routing.router.currentPageSignal.map {
      case Page.Activity(`id`, _ :: vId :: _) =>
        variants.find(_.id == vId).getOrElse(DefaultVariant)
      case _ => DefaultVariant
    }.distinct

  private def renderPlay(): HtmlElement =
    div(
      cls := "robot stack-lg",
      variantPill(),
      child <-- variantSignal.map(playForVariant)
    )

  private def variantPill(): HtmlElement =
    div(
      cls := "center no-print",
      div(
        cls := "pill-toggle",
        variants.map { v =>
          button(
            cls := "pill-btn",
            cls("is-active") <-- variantSignal.map(_.id == v.id),
            child.text <-- AppState.strings.map(v.nameKey),
            onClick --> (_ => Routing.go(Page.Activity(id, List("in-app", v.id))))
          )
        }
      )
    )

  private def playForVariant(v: Variant): HtmlElement =
    val init     = generate(v)
    val puzzle   = Var(init)
    val program  = Var(Vector.empty[Cmd])
    val robot    = Var(Bot(init.start, init.startFacing))
    val gotStar  = Var(false)
    val phase    = Var(Phase.Editing)
    val outcomeV = Var(Option.empty[Outcome])
    val dragging = Var(false)             // a palette button is being dragged
    val dropGap  = Var(Option.empty[Int]) // insertion gap under the pointer

    var handle: Option[js.timers.SetIntervalHandle] = None
    def stopTimer(): Unit =
      handle.foreach(js.timers.clearInterval)
      handle = None

    def resetRobot(): Unit =
      val p = puzzle.now()
      robot.set(Bot(p.start, p.startFacing))
      gotStar.set(false)

    val editable: Signal[Boolean] = phase.signal.map(_ == Phase.Editing)
    def whenEditing(f: => Unit): Unit = if phase.now() == Phase.Editing then f

    def add(c: Cmd): Unit = whenEditing(program.update(_ :+ c))
    def undo(): Unit      = whenEditing(program.update(p => if p.isEmpty then p else p.init))
    def clearProg(): Unit = whenEditing(program.set(Vector.empty))

    def toEditing(): Unit =
      stopTimer()
      outcomeV.set(None)
      resetRobot()
      phase.set(Phase.Editing)

    def newPuzzle(): Unit =
      stopTimer()
      puzzle.set(generate(v))
      program.set(Vector.empty)
      resetRobot()
      outcomeV.set(None)
      phase.set(Phase.Editing)

    def runProgram(): Unit =
      val prog = program.now()
      if prog.nonEmpty && phase.now() == Phase.Editing then
        val p   = puzzle.now()
        val sim = simulate(p, prog)
        resetRobot()
        phase.set(Phase.Running)
        var idx = 0
        handle = Some(js.timers.setInterval(StepMs) {
          idx += 1
          if idx < sim.states.length then
            val st = sim.states(idx)
            robot.set(st)
            if p.star.contains(st.cell) then gotStar.set(true)
          else
            stopTimer()
            outcomeV.set(Some(outcome(p, sim)))
            phase.set(Phase.Done)
        })

    val runDisabled: Signal[Boolean] =
      program.signal.combineWith(editable).map((prog, ed) => prog.isEmpty || !ed)

    val board =
      div(
        cls := "robot-board",
        styleAttr <-- puzzle.signal.map(p => s"--robot-size: ${p.size}"),
        children <-- puzzle.signal.combineWith(robot.signal, gotStar.signal).map((p, bot, star) =>
          boardCells(p, bot, star, v.turns)
        )
      )

    def removeAt(idx: Int): Unit =
      whenEditing(program.update(p => if idx >= 0 && idx < p.length then p.patch(idx, Nil, 1) else p))

    // Resolve the pointer to an insertion gap (0..length). A gap element answers
    // directly; landing on a chip rounds to the nearer side so a drop anywhere
    // over the strip still picks a slot.
    def gapUnder(ev: dom.PointerEvent): Option[Int] =
      Option(dom.document.elementFromPoint(ev.clientX, ev.clientY)).flatMap { el =>
        Option(el.closest(".robot-gap")).map(_.getAttribute("data-gap").toInt)
          .orElse(Option(el.closest(".robot-chip")).map { chip =>
            val idx  = chip.getAttribute("data-index").toInt
            val rect = chip.getBoundingClientRect()
            if ev.clientX < rect.left + rect.width / 2 then idx else idx + 1
          })
      }

    // A queued step — tap it to delete. Steps aren't drag sources; new steps
    // come from the palette buttons dropped into the gaps between these.
    def chipEl(c: Cmd, idx: Int): HtmlElement =
      span(
        cls := "robot-chip",
        dataAttr("index") := idx.toString,
        c.arrow,
        onClick --> (_ => removeAt(idx))
      )

    // An insertion point between two steps (or at either end). Thin until a drag
    // is underway, when it widens into a drop zone and lights up under the pointer.
    def gapEl(k: Int): HtmlElement =
      span(
        cls := "robot-gap",
        cls("is-drop") <-- dropGap.signal.map(_.contains(k)),
        dataAttr("gap") := k.toString
      )

    val programStrip =
      div(
        cls := "robot-program",
        span(cls := "robot-program__label muted", child.text <-- s(_.guideRobot.programLabel)),
        child <-- program.signal.map { prog =>
          if prog.isEmpty then
            span(
              cls := "robot-gap robot-gap--empty",
              cls("is-drop") <-- dropGap.signal.map(_.contains(0)),
              dataAttr("gap") := "0",
              span(cls := "robot-program__empty muted", child.text <-- s(_.guideRobot.emptyProgram))
            )
          else
            div(
              cls := "robot-program__chips",
              cls("is-dragging") <-- dragging.signal,
              (0 to prog.length).flatMap { k =>
                if k < prog.length then Seq(gapEl(k), chipEl(prog(k), k)) else Seq(gapEl(k))
              }
            )
        }
      )

    // Tap a palette button to append; drag it into a gap to insert there. Tap vs
    // drag is split on travel distance — the same gesture the Maze board uses.
    def cmdBtn(c: Cmd): HtmlElement =
      var downX = 0.0
      var downY = 0.0
      var moved = false
      button(
        cls := "robot-dpad__btn",
        tpe := "button",
        c.arrow,
        disabled <-- editable.map(!_),
        onPointerDown --> { ev =>
          if phase.now() == Phase.Editing then
            moved = false
            downX = ev.clientX; downY = ev.clientY
            try ev.currentTarget.asInstanceOf[dom.Element].setPointerCapture(ev.pointerId)
            catch case _: Throwable => ()
        },
        onPointerMove --> { ev =>
          if phase.now() == Phase.Editing then
            if !moved && (math.abs(ev.clientX - downX) > 8 || math.abs(ev.clientY - downY) > 8) then
              moved = true
              dragging.set(true)
            if moved then dropGap.set(gapUnder(ev))
        },
        onPointerUp --> { ev =>
          if phase.now() == Phase.Editing then
            if !moved then add(c) // tap → append
            else gapUnder(ev).foreach(k => whenEditing(program.update(_.patch(k, Vector(c), 0))))
            dragging.set(false)
            dropGap.set(None)
        },
        onPointerCancel --> { _ =>
          moved = false
          dragging.set(false)
          dropGap.set(None)
        }
      )

    // Easy: a +-shaped d-pad of absolute arrows. Turning tiers: one row of
    // turn-left / forward / turn-right.
    val palette =
      if v.turns then
        div(
          cls := "robot-dpad",
          cmdBtn(Cmd.TurnLeft),
          cmdBtn(Cmd.Forward),
          cmdBtn(Cmd.TurnRight)
        )
      else
        div(
          cls := "robot-dpad",
          span(),
          cmdBtn(Cmd.Step(Dir.Up)),
          span(),
          cmdBtn(Cmd.Step(Dir.Left)),
          cmdBtn(Cmd.Step(Dir.Down)),
          cmdBtn(Cmd.Step(Dir.Right))
        )

    val buildSection =
      div(
        cls := "stack robot-build",
        programStrip,
        palette,
        div(
          cls := "row robot-controls",
          Components.primary(s(_.guideRobot.run), runProgram(), runDisabled),
          Components.ghost(s(_.guideRobot.undo), undo(), runDisabled),
          Components.ghost(s(_.guideRobot.clear), clearProg(), runDisabled)
        )
      )

    def doneSection(o: Outcome): HtmlElement =
      o match
        case Outcome.Won =>
          div(
            cls := "handoff card",
            div(cls := "handoff__title", child.text <-- s(_.guideRobot.won)),
            button(
              cls := "btn btn--lg",
              child.text <-- s(_.guideRobot.newGame),
              onClick --> (_ => newPuzzle())
            )
          )
        case other =>
          val msg: Strings => String = other match
            case Outcome.Crashed    => _.guideRobot.crashed
            case Outcome.MissedStar => _.guideRobot.missedStar
            case _                  => _.guideRobot.missed
          div(
            cls := "stack",
            Components.banner("warn", s(msg)),
            div(
              cls := "row robot-controls",
              Components.primary(s(_.guideRobot.tryAgain), toEditing()),
              Components.ghost(s(_.guideRobot.newGame), newPuzzle())
            )
          )

    val instr: Strings => String =
      if v.turns then _.guideRobot.instructionTurns else _.guideRobot.instruction

    div(
      cls := "stack-lg",
      onUnmountCallback(_ => stopTimer()),
      p(cls := "robot-instruction muted center no-print", child.text <-- s(instr)),
      if v.hasStar then
        p(cls := "robot-hint muted center no-print", child.text <-- s(_.guideRobot.starHint))
      else emptyNode,
      board,
      child <-- phase.signal.combineWith(outcomeV.signal).map {
        case (Phase.Done, Some(o)) => doneSection(o)
        case _                     => buildSection
      }
    )

  /** A small caret pinned to the robot's leading edge, showing which way it
    * faces. Drawn only in the turning tiers, where heading matters. */
  private def facingArrow(facing: Dir): HtmlElement =
    span(cls := s"robot-facing robot-facing--${facing.css}", "▲")

  /** The marker(s) drawn in a cell: the robot — with a heading caret in the
    * turning tiers — wins over the star/goal/wall beneath it. Shared by the live
    * board and the print sheet, which differ only in which cell holds the robot,
    * where its facing comes from, and whether a collected star is hidden. */
  private def cellBody(p: Puzzle, cell: Cell, isBot: Boolean, facing: Dir, showStar: Boolean, turns: Boolean): Seq[Modifier[HtmlElement]] =
    if isBot then span(cls := "robot-bot", "🤖") +: (if turns then Seq(facingArrow(facing)) else Nil)
    else if p.star.contains(cell) && showStar then Seq(span("⭐"))
    else if cell == p.goal then Seq(span("🏁"))
    else if p.walls.contains(cell) then Seq(span("🧱"))
    else Nil

  /** A live board cell. A collected star disappears so the child sees progress. */
  private def boardCells(p: Puzzle, robot: Bot, starGot: Boolean, turns: Boolean): Seq[HtmlElement] =
    for r <- 0 until p.size; c <- 0 until p.size yield
      val cell    = (r, c)
      val isRobot = robot.cell == cell
      div(
        cls := "robot-cell",
        cls("robot-cell--wall") := p.walls.contains(cell),
        cls("robot-cell--robot") := isRobot,
        cellBody(p, cell, isRobot, robot.facing, showStar = !starGot, turns)
      )

  // ---------- print ----------

  private def renderOffline(): HtmlElement =
    // PrintSlot mounts a single variant's sheet, fires the print dialog, then
    // unmounts it — so only the chosen tier lands in the print job.
    val slot = Printable.printSlot[Variant]()

    div(
      cls := "stack-lg",
      div(
        cls := "no-print",
        RulesCard.render(List(RulesCard.fromRules(_.offline.guideRobot.rules)))
      ),
      div(
        cls := "no-print stack-lg robot-print-actions",
        variants.map { v =>
          button(
            cls := "btn btn--lg btn--block",
            child.text <-- AppState.strings.map(str => s"${str.printable.print} — ${v.nameKey(str)}"),
            onClick --> (_ => slot.trigger(v))
          )
        }
      ),
      slot.mount(v =>
        Printable.render(
          title = _.offline.guideRobot.printTitle,
          body = div(
            cls := "robot-print-sheet",
            List.fill(v.perPage)(generate(v)).map(p => printablePuzzle(v, p))
          )
        )
      )
    )

  private def printablePuzzle(v: Variant, p: Puzzle): HtmlElement =
    div(
      cls := s"robot-print-item robot-print-item--${v.id}",
      div(
        cls := "robot-print-board",
        styleAttr := s"--robot-size: ${p.size}",
        printCells(p, v.turns)
      ),
      div(
        cls := "robot-print-write",
        span(cls := "robot-print-write__label", child.text <-- AppState.strings.map(_.offline.guideRobot.writeLabel)),
        div(
          cls := "robot-print-write__boxes",
          (0 until v.writeBoxes).map(_ => span(cls := "robot-print-box"))
        )
      )
    )

  private def printCells(p: Puzzle, turns: Boolean): Seq[HtmlElement] =
    for r <- 0 until p.size; c <- 0 until p.size yield
      val cell = (r, c)
      div(
        cls := "robot-cell robot-print-cell",
        cls("robot-cell--wall") := p.walls.contains(cell),
        cellBody(p, cell, cell == p.start, p.startFacing, showStar = true, turns)
      )
