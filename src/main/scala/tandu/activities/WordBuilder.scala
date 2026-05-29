package tandu.activities

import com.raquo.laminar.api.L.*
import tandu.{AppState, Kind, Page, Routing}
import tandu.i18n.{Lang, Strings}
import tandu.ui.{Components, Mode, ModeChooser, Printable}
import tandu.ui.Components.s

import scala.scalajs.js
import scala.util.Random

object WordBuilder extends Activity:
  val id = "word-builder"
  def name(s: Strings): String = s.wordBuilder.name
  def description(s: Strings): String = s.wordBuilder.description
  val minPlayers: Int = 1
  val maxPlayers: Int = 1
  val handsFree: Boolean = false
  override val kind: Kind = Kind.Learn

  // ---------- model ----------

  final case class Tile(idx: Int, char: Char)

  final case class Round(
      entry: WordBuilderBank.Entry,
      target: String,                   // displayed casing
      pool: Vector[Tile],
      slots: Vector[Option[Int]]
  ):
    def placedIdxs: Set[Int] = slots.flatten.toSet
    def isComplete: Boolean  = slots.forall(_.isDefined)
    def attempt: String =
      slots.flatMap(_.flatMap(i => pool.find(_.idx == i).map(_.char))).mkString
    def isCorrect: Boolean = isComplete && attempt == target

  final case class Level(
      id: String,
      label: Strings => String,
      description: Strings => String,
      minLen: Int,
      maxLen: Int,
      distractors: Int,
      upperCase: Boolean
  )

  // Three linear levels. Keep extra criteria as separate fields so tuning
  // is a data change, not a code change.
  val levels: List[Level] = List(
    Level("easy",   _.wordBuilder.easy.name,   _.wordBuilder.easy.description, minLen = 3, maxLen = 4, distractors = 0, upperCase = true),
    Level("medium", _.wordBuilder.medium.name, _.wordBuilder.medium.description, minLen = 5, maxLen = 6, distractors = 2, upperCase = false),
    Level("hard",   _.wordBuilder.hard.name,   _.wordBuilder.hard.description, minLen = 7, maxLen = 9, distractors = 3, upperCase = false)
  )

  private val DefaultLevel: Level = levels.head
  private val WorksheetRows: Int  = 12

  // ---------- round generation ----------

  private def buildRound(lang: Lang, level: Level, avoid: Option[WordBuilderBank.Entry]): Round =
    val entry = Picker.pickAvoiding(WordBuilderBank.entriesInRange(lang, level.minLen, level.maxLen), avoid)
    roundFor(lang, level, entry)

  private def roundFor(lang: Lang, level: Level, entry: WordBuilderBank.Entry): Round =
    val display = if level.upperCase then entry.word.toUpperCase else entry.word
    val wordChars = display.toVector

    val alphabet      = HangmanBank.lettersFor(lang)
    val cased         = if level.upperCase then alphabet else alphabet.map(_.toLower)
    val distractorSrc = cased.filterNot(wordChars.contains)
    val distractors   = Random.shuffle(distractorSrc.toList).take(level.distractors).toVector

    val poolChars = scrambled(wordChars ++ distractors, display)
    val tiles = poolChars.zipWithIndex.map((c, i) => Tile(i, c))
    Round(entry, display, tiles, Vector.fill(wordChars.length)(None))

  /** Shuffle until the pool, read left-to-right, doesn't spell the target.
    * Without this an Easy-level round (no distractors) can land on the
    * identity permutation and hand the answer away. With duplicates or
    * 1-letter words a deranged order may be impossible; cap retries and
    * accept whatever came out. */
  private def scrambled(chars: Vector[Char], target: String): Vector[Char] =
    var attempt = Random.shuffle(chars.toList).toVector
    var tries = 0
    while attempt.mkString == target && tries < 20 do
      attempt = Random.shuffle(chars.toList).toVector
      tries += 1
    attempt

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

  // Read the level from the URL path so it persists across reloads / shares,
  // mirroring Sudoku's variant-pill pattern.
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
      cls := "wb stack-lg",
      levelPill("in-app", current),
      child <-- current.map(playForLevel)
    )

  private def playForLevel(level: Level): HtmlElement =
    val round: Var[Round] = Var(buildRound(AppState.lang.now(), level, avoid = None))
    val wrongFlash: Var[Boolean] = Var(false)

    def newRound(): Unit =
      round.set(buildRound(AppState.lang.now(), level, avoid = Some(round.now().entry)))
      wrongFlash.set(false)

    def placeTile(t: Tile): Unit =
      val r = round.now()
      if r.placedIdxs.contains(t.idx) || r.isCorrect then ()
      else
        val nextSlot = r.slots.indexWhere(_.isEmpty)
        if nextSlot < 0 then ()
        else
          val updatedSlots = r.slots.updated(nextSlot, Some(t.idx))
          val updated = r.copy(slots = updatedSlots)
          round.set(updated)
          if updated.isComplete && !updated.isCorrect then
            wrongFlash.set(true)
            val _ = js.timers.setTimeout(420)(wrongFlash.set(false))

    def removeAt(slotIdx: Int): Unit =
      val r = round.now()
      if r.isCorrect then ()
      else
        round.set(r.copy(slots = r.slots.updated(slotIdx, None)))
        wrongFlash.set(false)

    val langChange = AppState.lang.signal.changes --> { lang =>
      round.set(buildRound(lang, level, avoid = None))
      wrongFlash.set(false)
    }

    div(
      cls := "stack-lg",
      langChange,
      div(
        cls := "wb-picture",
        child.text <-- round.signal.map(_.entry.emoji)
      ),
      slotsView(round.signal, wrongFlash.signal, removeAt),
      poolView(round.signal, placeTile),
      child <-- round.signal.map { r =>
        if r.isCorrect then
          div(
            cls := "stack",
            Components.banner("win", s(_.wordBuilder.correct)),
            div(
              cls := "row no-print",
              styleAttr := "justify-content: center;",
              button(
                cls := "btn btn--lg",
                child.text <-- s(_.wordBuilder.nextWord),
                onClick --> (_ => newRound())
              )
            )
          )
        else
          div(
            cls := "row no-print",
            styleAttr := "justify-content: center;",
            Components.ghost(s(_.wordBuilder.skip), newRound())
          )
      }
    )

  private def slotsView(
      roundSig: Signal[Round],
      wrongSig: Signal[Boolean],
      onRemove: Int => Unit
  ): HtmlElement =
    div(
      cls := "wb-slots",
      cls("wb-slots--wrong") <-- wrongSig,
      cls("wb-slots--correct") <-- roundSig.map(_.isCorrect),
      children <-- roundSig.map { r =>
        r.slots.zipWithIndex.map { (slot, idx) =>
          slot match
            case None =>
              div(cls := "wb-slot wb-slot--empty", "")
            case Some(tileIdx) =>
              val ch = r.pool.find(_.idx == tileIdx).map(_.char.toString).getOrElse("")
              button(
                cls := "wb-slot wb-slot--filled",
                disabled := r.isCorrect,
                ch,
                onClick --> (_ => onRemove(idx))
              )
        }
      }
    )

  private def poolView(roundSig: Signal[Round], onPlace: Tile => Unit): HtmlElement =
    div(
      cls := "wb-pool no-print",
      children <-- roundSig.map { r =>
        r.pool.map { tile =>
          val used = r.placedIdxs.contains(tile.idx)
          button(
            cls := "wb-tile",
            cls("wb-tile--used") := used,
            disabled := used || r.isCorrect,
            tile.char.toString,
            onClick --> (_ => onPlace(tile))
          )
        }
      }
    )

  // ---------- print ----------

  private def renderPrint(): HtmlElement =
    val rounds: Var[Vector[Round]] = Var(Vector.empty)

    def generate(level: Level): Unit =
      val lang = AppState.lang.now()
      val pool = WordBuilderBank.entriesInRange(lang, level.minLen, level.maxLen)
      val picks = Random.shuffle(pool.toList).take(WorksheetRows).toVector
      rounds.set(picks.map(e => roundFor(lang, level, e)))
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
        p(cls := "muted center", child.text <-- s(_.wordBuilder.printHint))
      ),
      div(
        cls := "print-only",
        Printable.render(
          title = _.wordBuilder.printTitle,
          body = div(
            cls := "wb-print-sheet",
            children <-- rounds.signal.map(_.map(printableRow))
          )
        )
      )
    )

  private def printableRow(r: Round): HtmlElement =
    div(
      cls := "wb-print-row",
      div(cls := "wb-print-emoji", r.entry.emoji),
      div(
        cls := "wb-print-pool",
        r.pool.map(t => div(cls := "wb-print-tile", t.char.toString))
      ),
      div(cls := "wb-print-arrow", "→"),
      div(
        cls := "wb-print-slots",
        r.slots.map(_ => div(cls := "wb-print-slot", ""))
      )
    )

