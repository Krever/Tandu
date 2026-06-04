package tandu.activities

import com.raquo.laminar.api.L.*
import tandu.AppState
import tandu.i18n.{Lang, Strings}
import tandu.ui.Components
import tandu.ui.Components.s

object MemoryChain extends Activity:
  val id = "memory-chain"
  def name(s: Strings): String = s.memoryChain.name
  def description(s: Strings): String = s.memoryChain.description
  val minPlayers: Int = 2
  val maxPlayers: Int = Int.MaxValue
  val handsFree: Boolean = true
  val glyph: String = "🚂"
  val tint: String = "olive"

  def render(): HtmlElement =
    val roller = new Roller[String]
    def pick(lang: Lang): String = roller.next(MemoryChainBank.forLang(lang))

    val theme: Var[String] = Var(pick(AppState.lang.now()))

    def next(): Unit = theme.set(pick(AppState.lang.now()))

    // Switching language mid-game pulls a fresh starter from the new bank.
    val langChange = AppState.lang.signal.changes --> { lang =>
      theme.set(pick(lang))
    }

    div(
      cls := "stack-lg",
      langChange,
      p(cls := "muted center", child.text <-- s(_.memoryChain.hint)),
      div(
        cls := "wa-card wa-card--phrase",
        child.text <-- theme.signal
      ),
      div(
        cls := "row no-print",
        styleAttr := "justify-content: center;",
        Components.speakBtn(theme.signal),
        button(
          cls := "btn btn--lg",
          child.text <-- s(_.memoryChain.newTheme),
          onClick --> (_ => next())
        )
      )
    )
