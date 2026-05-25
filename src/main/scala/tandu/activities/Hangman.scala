package tandu.activities

import com.raquo.laminar.api.L.*
import tandu.AppState
import tandu.i18n.{Lang, Strings}
import tandu.ui.Components
import tandu.ui.Components.s

object Hangman extends Activity:
  val id = "hangman"
  def name(s: Strings): String = s.hangman.name
  def description(s: Strings): String = s.hangman.description
  val categories: Set[Category] = Set(Category.Tabletop)

  val MaxWrong: Int = 6

  final case class State(
      display: String,
      guesses: Set[Char],
      wrong: Int
  ):
    def word: String = display.toLowerCase
    def lost: Boolean = wrong >= MaxWrong
    def won: Boolean = word.forall(c => !c.isLetter || guesses.contains(c))
    def finished: Boolean = lost || won

  private def newState(lang: Lang, avoid: Option[String]): State =
    State(display = Picker.pickAvoiding(HangmanBank.wordsFor(lang), avoid), guesses = Set.empty, wrong = 0)

  def render(): HtmlElement =
    val state: Var[State] = Var(newState(AppState.lang.now(), avoid = None))

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
      state.set(newState(AppState.lang.now(), avoid = Some(state.now().display)))

    val langChange = AppState.lang.signal.changes --> { lang =>
      state.set(newState(lang, avoid = None))
    }

    val lettersSig: Signal[Vector[Char]] =
      AppState.lang.signal.map(HangmanBank.lettersFor)

    div(
      cls := "stack-lg hangman",
      langChange,
      gallowsView(state.signal.map(_.wrong)),
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
        else lifeIndicator(st.wrong)
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
      div(
        cls := "row no-print",
        styleAttr := "justify-content: center;",
        button(
          cls := "btn",
          child.text <-- s(_.hangman.newWord),
          onClick --> (_ => nextWord())
        )
      )
    )

  private def lifeIndicator(wrong: Int): HtmlElement =
    val remaining = math.max(0, MaxWrong - wrong)
    div(
      cls := "hangman-lives center",
      span(cls := "muted", child.text <-- s(_.hangman.livesLeft)),
      span(cls := "hangman-lives__value", remaining.toString)
    )

  /** SVG gallows. Reveals body parts one by one as `wrong` increases:
    * 1 head, 2 body, 3 left arm, 4 right arm, 5 left leg, 6 right leg. */
  private def gallowsView(wrongSig: Signal[Int]): SvgElement =
    import com.raquo.laminar.api.L.svg as S
    S.svg(
      S.cls := "hangman-svg",
      S.viewBox := "0 0 200 220",
      S.width := "180",
      S.height := "200",
      // gallows frame
      S.g(
        S.cls := "hangman-frame",
        S.line(S.x1 := "20",  S.y1 := "210", S.x2 := "180", S.y2 := "210"), // base
        S.line(S.x1 := "60",  S.y1 := "210", S.x2 := "60",  S.y2 := "20"),  // pole
        S.line(S.x1 := "60",  S.y1 := "20",  S.x2 := "140", S.y2 := "20"),  // beam
        S.line(S.x1 := "140", S.y1 := "20",  S.x2 := "140", S.y2 := "45")   // rope
      ),
      // head
      S.circle(
        S.cls := "hangman-part",
        S.cx := "140", S.cy := "60", S.r := "15",
        S.display <-- wrongSig.map(w => if w >= 1 then "inline" else "none")
      ),
      // body
      S.line(
        S.cls := "hangman-part",
        S.x1 := "140", S.y1 := "75", S.x2 := "140", S.y2 := "140",
        S.display <-- wrongSig.map(w => if w >= 2 then "inline" else "none")
      ),
      // left arm
      S.line(
        S.cls := "hangman-part",
        S.x1 := "140", S.y1 := "90", S.x2 := "115", S.y2 := "115",
        S.display <-- wrongSig.map(w => if w >= 3 then "inline" else "none")
      ),
      // right arm
      S.line(
        S.cls := "hangman-part",
        S.x1 := "140", S.y1 := "90", S.x2 := "165", S.y2 := "115",
        S.display <-- wrongSig.map(w => if w >= 4 then "inline" else "none")
      ),
      // left leg
      S.line(
        S.cls := "hangman-part",
        S.x1 := "140", S.y1 := "140", S.x2 := "120", S.y2 := "175",
        S.display <-- wrongSig.map(w => if w >= 5 then "inline" else "none")
      ),
      // right leg
      S.line(
        S.cls := "hangman-part",
        S.x1 := "140", S.y1 := "140", S.x2 := "160", S.y2 := "175",
        S.display <-- wrongSig.map(w => if w >= 6 then "inline" else "none")
      )
    )
