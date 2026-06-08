package tandu.activities

import com.raquo.laminar.api.L.*
import tandu.{AppState, Kind}
import tandu.i18n.Strings
import tandu.ui.{Mode, ModeChooser}
import tandu.ui.Components.s

/** Active Games — a small hub of classic run-around games that need *no* live
  * device support: the floor is lava, tag, hide and seek, red light/green light.
  * The honest support for these is the same as I-Spy's — suggest the game, show
  * how to play plus variants and a safety note, then get out of the way. One
  * curated entry rather than four near-empty activities. Each game is a [[Mode]]
  * so it gets its own shareable URL. */
object ActiveGames extends Activity:
  val id = "active-games"
  def name(s: Strings): String = s.activeGames.name
  def description(s: Strings): String = s.activeGames.description
  val minPlayers: Int = 2
  val maxPlayers: Int = Int.MaxValue
  val handsFree: Boolean = true
  override val kind: Kind = Kind.Move
  val glyph: String = "🏃"
  val tint: String = "olive"

  def render(): HtmlElement =
    ModeChooser.render(id, modes)

  private val modes: List[Mode] = List(
    game("floor-is-lava", _.activeGames.lava),
    game("tag", _.activeGames.tag),
    game("hide-and-seek", _.activeGames.hideSeek),
    game("red-light", _.activeGames.redLight)
  )

  private def game(modeId: String, pick: Strings => Strings.ActiveGameRules): Mode =
    Mode(
      id = modeId,
      label = st => pick(st).name,
      description = Some(st => pick(st).blurb),
      render = () => rules(pick)
    )

  /** A rules card for one game: how-to steps, then variant/safety tips —
    * mirroring [[ISpy]], but the steps and tips come from the i18n lists so each
    * game can have however many it needs. */
  private def rules(pick: Strings => Strings.ActiveGameRules): HtmlElement =
    div(
      cls := "stack-lg",
      div(
        cls := "rules-card",
        h3(cls := "rules-card__title", child.text <-- s(st => pick(st).howTitle)),
        ol(
          cls := "rules-list",
          children <-- AppState.strings.map(st => pick(st).steps.map(step => li(step)))
        ),
        h3(cls := "rules-card__title", child.text <-- s(st => pick(st).tipsTitle)),
        ul(
          cls := "rules-list",
          children <-- AppState.strings.map(st => pick(st).tips.map(tip => li(tip)))
        )
      )
    )
