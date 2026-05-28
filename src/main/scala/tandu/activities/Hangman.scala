package tandu.activities

import com.raquo.laminar.api.L.*
import tandu.AppState
import tandu.i18n.{Lang, Strings}
import tandu.ui.{Components, Mode, ModeChooser, RulesCard}
import tandu.ui.Components.s

object Hangman extends Activity:
  val id = "hangman"
  def name(s: Strings): String = s.hangman.name
  def description(s: Strings): String = s.hangman.description
  val categories: Set[Category] = Set(Category.Tabletop)

  val DefaultLives: Int = 6
  private val LivesOptions: List[Int] = List(4, 6, 8, 10)
  private val GallowsStages: Int = 10

  final case class State(
      display: String,
      guesses: Set[Char],
      wrong: Int,
      maxWrong: Int
  ):
    def word: String = display.toLowerCase
    def lost: Boolean = wrong >= maxWrong
    def won: Boolean = word.forall(c => !c.isLetter || guesses.contains(c))
    def finished: Boolean = lost || won

  private def newState(lang: Lang, avoid: Option[String], maxWrong: Int): State =
    State(
      display  = Picker.pickAvoiding(HangmanBank.wordsFor(lang), avoid),
      guesses  = Set.empty,
      wrong    = 0,
      maxWrong = maxWrong
    )

  def render(): HtmlElement =
    ModeChooser.render(List(
      Mode(
        id = "in-app",
        label = _.mode.inApp,
        render = () => renderPlay()
      ),
      Mode(
        id = "word-picker",
        label = _.mode.offline,
        materials = List(_.offline.materials.paperPen),
        hint = Some(_.offline.hangman.keeperHint),
        render = () => renderKeeper()
      )
    ))

  private def renderKeeper(): HtmlElement =
    val word    = Var(Picker.pickAvoiding(HangmanBank.wordsFor(AppState.lang.now()), None))
    val shown   = Var(false)

    val langChange = AppState.lang.signal.changes --> { lang =>
      word.set(Picker.pickAvoiding(HangmanBank.wordsFor(lang), None))
      shown.set(false)
    }

    def nextWord(): Unit =
      val w = word.now()
      word.set(Picker.pickAvoiding(HangmanBank.wordsFor(AppState.lang.now()), Some(w)))
      shown.set(false)

    div(
      cls := "stack-lg",
      langChange,
      RulesCard.render(List(
        RulesCard.fromRules(_.offline.hangman.rules),
        RulesCard.Section(_.offline.hangman.gallowsTitle, s => List(s.offline.hangman.drawHint))
      )),
      div(
        cls := "center",
        gallowsView(Val(GallowsStages))
      ),
      div(
        cls := "card stack",
        h3(cls := "h2 center", child.text <-- s(_.offline.hangman.keeperTitle)),
        div(
          cls := "hangman-keeper-word center",
          child.text <-- shown.signal.combineWith(word.signal).map { (sh, w) =>
            if sh then w.toUpperCase else "•" * w.length
          }
        ),
        div(
          cls := "row no-print",
          styleAttr := "justify-content: center; gap: var(--space-3);",
          button(
            cls := "btn",
            child.text <-- shown.signal.combineWith(AppState.strings).map { (sh, str) =>
              if sh then str.offline.hangman.hide else str.offline.hangman.reveal
            },
            onClick --> (_ => shown.update(!_))
          ),
          button(
            cls := "btn btn--ghost",
            child.text <-- s(_.hangman.newWord),
            onClick --> (_ => nextWord())
          )
        )
      )
    )

  private def renderPlay(): HtmlElement =
    val lives: Var[Int]   = Var(DefaultLives)
    val state: Var[State] = Var(newState(AppState.lang.now(), avoid = None, maxWrong = lives.now()))

    def guess(c: Char): Unit =
      val cur = state.now()
      if cur.finished then ()
      else
        val lc = c.toLower
        if cur.guesses.contains(lc) then ()
        else
          val isHit = cur.word.contains(lc)
          state.set(cur.copy(
            guesses = cur.guesses + lc,
            wrong = if isHit then cur.wrong else cur.wrong + 1
          ))

    def nextWord(): Unit =
      state.set(newState(AppState.lang.now(), avoid = Some(state.now().display), maxWrong = lives.now()))

    def setLives(n: Int): Unit =
      lives.set(n)
      state.set(newState(AppState.lang.now(), avoid = Some(state.now().display), maxWrong = n))

    val langChange = AppState.lang.signal.changes --> { lang =>
      state.set(newState(lang, avoid = None, maxWrong = lives.now()))
    }

    val lettersSig: Signal[Vector[Char]] =
      AppState.lang.signal.map(HangmanBank.lettersFor)

    val stagesSig: Signal[Int] = state.signal.map { st =>
      math.min(GallowsStages, (GallowsStages - st.maxWrong) + st.wrong)
    }

    div(
      cls := "stack-lg hangman",
      langChange,
      gallowsView(stagesSig),
      div(
        cls := "hangman-word center",
        children <-- state.signal.map(st =>
          st.display.toList.map { ch =>
            val lc = ch.toLower
            val shown =
              if !ch.isLetter then ch.toString
              else if st.guesses.contains(lc) || st.lost then ch.toString
              else "_"
            val isMiss = st.lost && ch.isLetter && !st.guesses.contains(lc)
            span(
              cls := "hangman-letter",
              cls("hangman-letter--miss") := isMiss,
              shown
            )
          }
        )
      ),
      child <-- state.signal.map { st =>
        if st.won then Components.banner("win", s(_.hangman.youWon))
        else if st.lost then Components.banner("hit", s(_.hangman.youLost))
        else lifeIndicator(st.wrong, st.maxWrong)
      },
      div(
        cls := "hangman-keys no-print",
        children <-- lettersSig.combineWith(state.signal).map { (letters, st) =>
          letters.toList.map { ch =>
            val lc = ch.toLower
            val used = st.guesses.contains(lc)
            val isHit = used && st.word.contains(lc)
            val isMiss = used && !st.word.contains(lc)
            button(
              cls := "hangman-key",
              cls("hangman-key--hit") := isHit,
              cls("hangman-key--miss") := isMiss,
              disabled := used || st.finished,
              ch.toString,
              onClick --> (_ => guess(ch))
            )
          }
        }
      ),
      livesPicker(lives.signal, setLives),
      div(
        cls := "row no-print",
        styleAttr := "justify-content: center;",
        Components.replayButton(s(_.hangman.newWord), nextWord(), state.signal.map(_.finished))
      )
    )

  private def livesPicker(livesSig: Signal[Int], onPick: Int => Unit): HtmlElement =
    div(
      cls := "row no-print",
      styleAttr := "justify-content: center; flex-wrap: wrap; gap: var(--space-2);",
      span(cls := "muted", child.text <-- s(_.hangman.livesLeft), ":"),
      LivesOptions.map { n =>
        button(
          cls := "btn btn--ghost btn--icon",
          cls("is-active") <-- livesSig.map(_ == n),
          n.toString,
          onClick --> (_ => onPick(n))
        )
      }
    )

  private def lifeIndicator(wrong: Int, max: Int): HtmlElement =
    val remaining = math.max(0, max - wrong)
    div(
      cls := "hangman-lives center",
      span(cls := "muted", child.text <-- s(_.hangman.livesLeft)),
      span(cls := "hangman-lives__value", remaining.toString)
    )

  private def gallowsView(stagesSig: Signal[Int]): SvgElement =
    import com.raquo.laminar.api.L.svg as S
    def at(n: Int) = S.display <-- stagesSig.map(s => if s >= n then "inline" else "none")
    S.svg(
      S.cls := "hangman-svg",
      S.viewBox := "0 0 200 220",
      S.width := "180",
      S.height := "200",
      // 1: base
      S.line(S.cls := "hangman-part", S.x1 := "20",  S.y1 := "210", S.x2 := "180", S.y2 := "210", at(1)),
      // 2: pole
      S.line(S.cls := "hangman-part", S.x1 := "60",  S.y1 := "210", S.x2 := "60",  S.y2 := "20",  at(2)),
      // 3: beam
      S.line(S.cls := "hangman-part", S.x1 := "60",  S.y1 := "20",  S.x2 := "140", S.y2 := "20",  at(3)),
      // 4: rope
      S.line(S.cls := "hangman-part", S.x1 := "140", S.y1 := "20",  S.x2 := "140", S.y2 := "45",  at(4)),
      // 5: head
      S.circle(S.cls := "hangman-part", S.cx := "140", S.cy := "60", S.r := "15", at(5)),
      // 6: body
      S.line(S.cls := "hangman-part", S.x1 := "140", S.y1 := "75",  S.x2 := "140", S.y2 := "140", at(6)),
      // 7: left arm
      S.line(S.cls := "hangman-part", S.x1 := "140", S.y1 := "90",  S.x2 := "115", S.y2 := "115", at(7)),
      // 8: right arm
      S.line(S.cls := "hangman-part", S.x1 := "140", S.y1 := "90",  S.x2 := "165", S.y2 := "115", at(8)),
      // 9: left leg
      S.line(S.cls := "hangman-part", S.x1 := "140", S.y1 := "140", S.x2 := "120", S.y2 := "175", at(9)),
      // 10: right leg
      S.line(S.cls := "hangman-part", S.x1 := "140", S.y1 := "140", S.x2 := "160", S.y2 := "175", at(10))
    )
