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
  val glyph: String = "🔤"
  val tint: String = "teal"
  override val kind: Kind = Kind.Learn

  // ---------- model ----------

  final case class Tile(idx: Int, char: Char)

  enum RoundKind:
    case Spell(pool: Vector[Tile], slots: Vector[Option[Int]])
    case Read(choices: Vector[WordBuilderBank.Entry], correctIdx: Int, picked: Option[Int])

    def isRead: Boolean = this match
      case _: Read => true
      case _       => false

  final case class Round(
      entry: WordBuilderBank.Entry,
      target: String,                   // displayed casing
      kind: RoundKind
  ):
    def isCorrect: Boolean = kind match
      case RoundKind.Spell(pool, slots) =>
        slots.forall(_.isDefined) &&
          slots.flatMap(_.flatMap(i => pool.find(_.idx == i).map(_.char))).mkString == target
      case RoundKind.Read(_, correctIdx, picked) =>
        picked.contains(correctIdx)

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

  private def buildRound(roller: Roller[WordBuilderBank.Entry], lang: Lang, level: Level): Round =
    val pool  = WordBuilderBank.entriesInRange(lang, level.minLen, level.maxLen)
    val entry = roller.next(pool)
    randomRound(lang, level, entry, pool)

  private def randomRound(
      lang: Lang, level: Level, entry: WordBuilderBank.Entry, pool: Vector[WordBuilderBank.Entry]
  ): Round =
    val display = if level.upperCase then entry.word.toUpperCase else entry.word
    if Random.nextBoolean() then spellRound(lang, level, entry, display)
    else readRound(lang, entry, display, pool)

  private def spellRound(lang: Lang, level: Level, entry: WordBuilderBank.Entry, display: String): Round =
    val wordChars = display.toVector

    val alphabet      = HangmanBank.lettersFor(lang)
    val cased         = if level.upperCase then alphabet else alphabet.map(_.toLower)
    val distractorSrc = cased.filterNot(wordChars.contains)
    val distractors   = Random.shuffle(distractorSrc.toList).take(level.distractors).toVector

    val poolChars = scrambled(wordChars ++ distractors, display)
    val tiles = poolChars.zipWithIndex.map((c, i) => Tile(i, c))
    Round(entry, display, RoundKind.Spell(tiles, Vector.fill(wordChars.length)(None)))

  /** Read mode: pick 3 distractor entries from the same length band so all
    * four emoji choices look comparable. Falls back to the whole-language
    * bank if the band can't yield enough distinct entries. */
  private def readRound(
      lang: Lang, entry: WordBuilderBank.Entry, display: String, pool: Vector[WordBuilderBank.Entry]
  ): Round =
    val band = pool.filter(_ != entry)
    val source =
      if band.size >= 3 then band
      else WordBuilderBank.entriesFor(lang).filter(_ != entry)
    val distractors = Random.shuffle(source).take(3)
    val all = Random.shuffle(entry +: distractors)
    val idx = all.indexOf(entry)
    Round(entry, display, RoundKind.Read(all, idx, None))

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
    val roller = new Roller[WordBuilderBank.Entry]
    val round: Var[Round] = Var(buildRound(roller, AppState.lang.now(), level))
    val wrongFlash: Var[Boolean] = Var(false)
    val wrongPick: Var[Option[Int]] = Var(None)

    def newRound(): Unit =
      round.set(buildRound(roller, AppState.lang.now(), level))
      wrongFlash.set(false)
      wrongPick.set(None)

    def placeTile(t: Tile): Unit =
      val r = round.now()
      r.kind match
        case spell: RoundKind.Spell if !r.isCorrect && !spell.slots.flatten.contains(t.idx) =>
          val nextSlot = spell.slots.indexWhere(_.isEmpty)
          if nextSlot >= 0 then
            val updatedSlots = spell.slots.updated(nextSlot, Some(t.idx))
            val updated = r.copy(kind = spell.copy(slots = updatedSlots))
            round.set(updated)
            if updatedSlots.forall(_.isDefined) && !updated.isCorrect then
              wrongFlash.set(true)
              val _ = js.timers.setTimeout(420)(wrongFlash.set(false))
        case _ => ()

    def removeAt(slotIdx: Int): Unit =
      val r = round.now()
      r.kind match
        case spell: RoundKind.Spell if !r.isCorrect =>
          round.set(r.copy(kind = spell.copy(slots = spell.slots.updated(slotIdx, None))))
          wrongFlash.set(false)
        case _ => ()

    def pickPic(idx: Int): Unit =
      val r = round.now()
      r.kind match
        case read: RoundKind.Read if !r.isCorrect =>
          if idx == read.correctIdx then
            round.set(r.copy(kind = read.copy(picked = Some(idx))))
          else
            wrongPick.set(Some(idx))
            val _ = js.timers.setTimeout(420)(
              if wrongPick.now().contains(idx) then wrongPick.set(None)
            )
        case _ => ()

    val langChange = AppState.lang.signal.changes --> { _ =>
      round.set(buildRound(roller, AppState.lang.now(), level))
      wrongFlash.set(false)
      wrongPick.set(None)
    }

    // Switch layouts only when the round's kind changes (i.e., between
    // rounds), not on every tile click — that keeps the inner reactive
    // bindings stable within a round.
    val isReadSig: Signal[Boolean]    = round.signal.map(_.kind.isRead).distinct
    val isCorrectSig: Signal[Boolean] = round.signal.map(_.isCorrect).distinct

    div(
      cls := "stack-lg",
      langChange,
      child <-- isReadSig.map { isRead =>
        if isRead then readLayout(round.signal, wrongPick.signal, pickPic)
        else spellLayout(round.signal, wrongFlash.signal, placeTile, removeAt)
      },
      child <-- isCorrectSig.map { isCorrect =>
        if isCorrect then
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

  // ---------- spell layout (picture -> letters) ----------

  private def spellLayout(
      roundSig: Signal[Round],
      wrongSig: Signal[Boolean],
      onPlace: Tile => Unit,
      onRemove: Int => Unit
  ): HtmlElement =
    div(
      cls := "stack-lg",
      div(
        cls := "wb-picture-row",
        div(
          cls := "wb-picture",
          child.text <-- roundSig.map(_.entry.emoji)
        ),
        // Hear the word the picture stands for — the way in for a pre-reader
        // who can't otherwise know what to spell.
        Components.speakBtn(roundSig.map(_.entry.word))
      ),
      slotsView(roundSig, wrongSig, onRemove),
      poolView(roundSig, onPlace)
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
        r.kind match
          case RoundKind.Spell(pool, slots) =>
            slots.zipWithIndex.map { (slot, idx) =>
              slot match
                case None =>
                  div(cls := "wb-slot wb-slot--empty", "")
                case Some(tileIdx) =>
                  val ch = pool.find(_.idx == tileIdx).map(_.char.toString).getOrElse("")
                  button(
                    cls := "wb-slot wb-slot--filled",
                    disabled := r.isCorrect,
                    ch,
                    onClick --> (_ => onRemove(idx))
                  )
            }
          case _ => Nil
      }
    )

  private def poolView(roundSig: Signal[Round], onPlace: Tile => Unit): HtmlElement =
    div(
      cls := "wb-pool no-print",
      children <-- roundSig.map { r =>
        r.kind match
          case RoundKind.Spell(pool, slots) =>
            val placed = slots.flatten.toSet
            pool.map { tile =>
              val used = placed.contains(tile.idx)
              button(
                cls := "wb-tile",
                cls("wb-tile--used") := used,
                disabled := used || r.isCorrect,
                tile.char.toString,
                onClick --> (_ => onPlace(tile))
              )
            }
          case _ => Nil
      }
    )

  // ---------- read layout (word -> picture) ----------

  private def readLayout(
      roundSig: Signal[Round],
      wrongPickSig: Signal[Option[Int]],
      onPick: Int => Unit
  ): HtmlElement =
    div(
      cls := "stack-lg",
      // No speak button here: hearing the word would give the answer away —
      // the point of this mode is to read it.
      div(
        cls := "wb-word",
        cls("wb-word--correct") <-- roundSig.map(_.isCorrect),
        child.text <-- roundSig.map(_.target)
      ),
      picChoicesView(roundSig, wrongPickSig, onPick)
    )

  private def picChoicesView(
      roundSig: Signal[Round],
      wrongPickSig: Signal[Option[Int]],
      onPick: Int => Unit
  ): HtmlElement =
    div(
      cls := "wb-pic-choices no-print",
      children <-- roundSig.combineWith(wrongPickSig).map { (r, wrong) =>
        r.kind match
          case RoundKind.Read(choices, correctIdx, picked) =>
            choices.zipWithIndex.map { (entry, i) =>
              val isWrong   = wrong.contains(i)
              val isCorrect = picked.contains(i) && i == correctIdx
              button(
                cls := "wb-pic-choice",
                cls("wb-pic-choice--wrong")   := isWrong,
                cls("wb-pic-choice--correct") := isCorrect,
                disabled := r.isCorrect,
                entry.emoji,
                onClick --> (_ => onPick(i))
              )
            }
          case _ => Nil
      }
    )

  // ---------- print ----------

  /** One printed worksheet's rows, as a bare body for composed documents like
    * workbooks. */
  def printSheetBody(level: Level, lang: Lang): HtmlElement =
    val pool = WordBuilderBank.entriesInRange(lang, level.minLen, level.maxLen)
    val picks = Random.shuffle(pool.toList).take(WorksheetRows).toVector
    div(
      cls := "wb-print-sheet",
      picks.map(e => printableRow(randomRound(lang, level, e, pool)))
    )

  private def renderPrint(): HtmlElement =
    val rounds: Var[Vector[Round]] = Var(Vector.empty)

    def generate(level: Level): Unit =
      val lang = AppState.lang.now()
      val pool = WordBuilderBank.entriesInRange(lang, level.minLen, level.maxLen)
      val picks = Random.shuffle(pool.toList).take(WorksheetRows).toVector
      rounds.set(picks.map(e => randomRound(lang, level, e, pool)))
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

  private def printableRow(r: Round): HtmlElement = r.kind match
    case RoundKind.Spell(pool, slots) =>
      div(
        cls := "wb-print-row",
        div(cls := "wb-print-emoji", r.entry.emoji),
        div(
          cls := "wb-print-pool",
          pool.map(t => div(cls := "wb-print-tile", t.char.toString))
        ),
        div(cls := "wb-print-arrow", "→"),
        div(
          cls := "wb-print-slots",
          slots.map(_ => div(cls := "wb-print-slot", ""))
        )
      )
    case RoundKind.Read(choices, _, _) =>
      div(
        cls := "wb-print-row wb-print-row--read",
        div(cls := "wb-print-word", r.target),
        div(cls := "wb-print-arrow", "→"),
        div(
          cls := "wb-print-pics",
          choices.map(e => div(cls := "wb-print-pic", e.emoji))
        )
      )
