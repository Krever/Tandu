package tandu.activities

import com.raquo.laminar.api.L.*
import tandu.AppState
import tandu.activities.battleships.{Board, Domain}
import tandu.activities.battleships.Domain.{Cell, EnemyView, Fleet, ShotResult, Size, randomFleet}
import tandu.i18n.Strings
import tandu.ui.{Components, Mode, ModeChooser, Printable, RulesCard}
import tandu.ui.Components.s

object Battleships extends Activity:
  val id = "battleships"
  def name(s: Strings): String = s.battleships.name
  def description(s: Strings): String = s.battleships.description
  val minPlayers: Int = 2
  val maxPlayers: Int = 2
  val handsFree: Boolean = false

  // ---------- pass-and-play state ----------

  enum Phase:
    case AwaitingShot(player: Player)
    case ShotResolved(player: Player, target: Cell, result: ShotResult)
    case GameOver(winner: Player)

  /** Pass-and-play state. Tracks both fleets locally; each player's
    * EnemyView is the same data structure the P2P shooter sees, so the
    * board widgets render identically across modes. */
  final case class GameState(
      fleets: Map[Player, Fleet],
      incoming: Map[Player, Set[Cell]],     // shots received at each player's board
      enemyViews: Map[Player, EnemyView],   // each player's knowledge of the opponent
      phase: Phase
  )

  private def freshGame(): GameState =
    GameState(
      fleets     = Map(Player.P1 -> randomFleet(), Player.P2 -> randomFleet()),
      incoming   = Map(Player.P1 -> Set.empty, Player.P2 -> Set.empty),
      enemyViews = Map(Player.P1 -> EnemyView(), Player.P2 -> EnemyView()),
      phase      = Phase.AwaitingShot(Player.P1)
    )

  // ---------- mode chooser ----------

  def render(): HtmlElement =
    ModeChooser.render(List(
      Mode(
        id = "in-app",
        label = _.mode.inApp,
        render = () => renderPlay()
      ),
      Mode(
        id = "p2p",
        label = _.mode.p2p,
        hint = Some(_.battleships.p2p.intro),
        render = () => BattleshipsP2P.render()
      ),
      Mode(
        id = "print",
        label = _.mode.offline,
        materials = List(_.offline.materials.printer, _.offline.materials.paperPen),
        hint = Some(_.offline.battleships.rules.title),
        render = () => renderOffline()
      )
    ))

  // ---------- pass-and-play game view ----------

  private def renderPlay(): HtmlElement =
    val game = Var(freshGame())
    val showMyBoard = Var(false)

    def fire(shooter: Player, target: Cell): Unit =
      val g = game.now()
      val defender = shooter.other
      if g.enemyViews(shooter).knownCells.contains(target) then ()
      else
        val res = Domain.resolveShot(g.fleets(defender), g.incoming(defender), target)
        game.set(g.copy(
          incoming   = g.incoming.updated(defender, g.incoming(defender) + target),
          enemyViews = g.enemyViews.updated(shooter, g.enemyViews(shooter).apply(res)),
          phase      = Phase.ShotResolved(shooter, target, res.result)
        ))

    def endTurn(): Unit =
      val g = game.now()
      g.phase match
        case Phase.ShotResolved(shooter, _, _) =>
          if g.fleets(shooter.other).allSunk(g.incoming(shooter.other)) then
            game.update(_.copy(phase = Phase.GameOver(shooter)))
          else
            // No hand-off screen between shots — own board is hidden by
            // default and the turn header is colored by player, so passing
            // the phone is enough. Reset the toggle so the next player
            // starts with their own ships hidden.
            showMyBoard.set(false)
            game.update(_.copy(phase = Phase.AwaitingShot(shooter.other)))
        case _ => ()

    def restart(): Unit =
      showMyBoard.set(false)
      game.set(freshGame())

    val phaseSig = game.signal.map(_.phase)

    div(
      cls := "stack-lg",
      // Phase indicator for e2e tests — also disambiguates pass-and-play
      // turn state without having to read translated text.
      dataAttr("testid") := "bs-game",
      dataAttr("bs-phase") <-- phaseSig.map {
        case Phase.AwaitingShot(p)       => s"awaiting-${p.num}"
        case Phase.ShotResolved(p, _, _) => s"resolved-${p.num}"
        case Phase.GameOver(w)           => s"gameover-${w.num}"
      },
      child <-- phaseSig.map {
        case Phase.AwaitingShot(p)       => playerPage(p, turnView(p, game, fire, showMyBoard))
        case Phase.ShotResolved(p, t, r) => playerPage(p, resolvedView(p, t, r, game, endTurn, showMyBoard))
        case Phase.GameOver(w)           => playerPage(w, gameOverView(w, restart))
      }
    )

  private def playerPage(p: Player, content: HtmlElement): HtmlElement =
    div(
      cls := s"player-page player-page--p${p.num} stack-lg",
      content
    )

  // ---------- subviews ----------

  private def playerHeader(me: Player): HtmlElement =
    Board.headerBadge(
      AppState.strings.map(str => s"${me.labelKey(str)} — ${str.battleships.yourTurn}")
    )

  private def enemyWaters(
      me: Player,
      gameSignal: Signal[GameState],
      onTap: Option[Cell => Unit],
      highlight: Option[Cell]
  ): HtmlElement =
    Board.enemyBoardSection(
      view = gameSignal.map(_.enemyViews(me)),
      onTap = onTap,
      highlight = highlight,
      extras = opponentFleetStatus(gameSignal, me)
    )

  private def myWaters(
      me: Player,
      gameSignal: Signal[GameState],
      showMyBoard: Var[Boolean]
  ): HtmlElement =
    Board.myBoardSection(
      fleet    = gameSignal.map(_.fleets(me)),
      incoming = gameSignal.map(_.incoming(me)),
      showMyBoard = showMyBoard,
      extraCls = "no-print"
    )

  private def turnView(
      me: Player,
      game: Var[GameState],
      fire: (Player, Cell) => Unit,
      showMyBoard: Var[Boolean]
  ): HtmlElement =
    div(
      cls := "stack-lg",
      playerHeader(me),
      p(cls := "muted center", child.text <-- s(_.battleships.fireAt)),
      enemyWaters(me, game.signal, onTap = Some(c => fire(me, c)), highlight = None),
      myWaters(me, game.signal, showMyBoard)
    )

  private def resolvedView(
      shooter: Player,
      target: Cell,
      result: ShotResult,
      game: Var[GameState],
      endTurn: () => Unit,
      showMyBoard: Var[Boolean]
  ): HtmlElement =
    val (bannerKind, msgFn): (String, Strings => String) = result match
      case ShotResult.Hit  => ("hit",  (str: Strings) => str.battleships.hit)
      case ShotResult.Sunk => ("hit",  (str: Strings) => str.battleships.sunk)
      case ShotResult.Miss => ("miss", (str: Strings) => str.battleships.miss)

    div(
      cls := "stack-lg",
      playerHeader(shooter),
      div(
        cls := "bs-resolve-bar",
        Components.banner(bannerKind, AppState.strings.map(msgFn)),
        button(
          cls := "btn btn--player btn--lg no-print",
          dataAttr("testid") := "end-turn",
          child.text <-- s(_.battleships.endTurn),
          onClick --> (_ => endTurn())
        )
      ),
      enemyWaters(shooter, game.signal, onTap = Some(_ => endTurn()), highlight = Some(target)),
      myWaters(shooter, game.signal, showMyBoard)
    )

  private def gameOverView(winner: Player, restart: () => Unit): HtmlElement =
    Board.handoffCard(
      badge = AppState.strings.map(str => winner.labelKey(str)),
      title = s(_.battleships.allSunk),
      actions = button(
        cls := "btn btn--player btn--lg",
        child.text <-- s(_.common.playAgain),
        onClick --> (_ => restart())
      )
    )

  private def opponentFleetStatus(
      gameSignal: Signal[GameState],
      shooter: Player
  ): HtmlElement =
    div(
      cls := "fleet-list",
      styleAttr := "justify-content: center;",
      children <-- gameSignal.map { g =>
        val enemy = g.fleets(shooter.other)
        val incoming = g.incoming(shooter.other)
        enemy.ships.sortBy(-_.cells.size).map { ship =>
          span(
            cls := "fleet-pill",
            cls("is-sunk") := ship.isSunk(incoming),
            "■" * ship.cells.size
          )
        }
      }
    )

  // ---------- printable mode ----------

  private val battleshipsRulesSections: List[RulesCard.Section] = List(
    RulesCard.fromRules(_.offline.battleships.rules),
    RulesCard.Section(_.offline.battleships.fleetTitle, s => List(s.offline.battleships.fleetLine))
  )

  private enum PrintKind:
    case Maps, Rules

  private def renderOffline(): HtmlElement =
    val slot = Printable.printSlot[PrintKind]()
    div(
      cls := "stack-lg",
      div(
        cls := "no-print",
        RulesCard.render(battleshipsRulesSections)
      ),
      div(
        cls := "row no-print",
        styleAttr := "justify-content: center; flex-wrap: wrap;",
        button(
          cls := "btn btn--lg",
          child.text <-- s(_.printable.printMaps),
          onClick --> (_ => slot.trigger(PrintKind.Maps))
        ),
        button(
          cls := "btn btn--lg btn--ghost",
          child.text <-- s(_.printable.printRules),
          onClick --> (_ => slot.trigger(PrintKind.Rules))
        )
      ),
      slot.mount {
        case PrintKind.Maps  => printableMaps()
        case PrintKind.Rules => printableRules()
      }
    )

  private def printableMaps(): HtmlElement =
    Printable.render(
      title = _.offline.battleships.printTitle,
      body = div(
        cls := "bs-print-sheet",
        printPlayerGrids(playerNum = 1),
        printPlayerGrids(playerNum = 2)
      )
    )

  private def printableRules(): HtmlElement =
    Printable.render(
      title = _.offline.battleships.rules.title,
      body = RulesCard.render(battleshipsRulesSections)
    )

  private val PrintColumnLetters: Vector[Char] = "ABCDEFGHIJ".toVector

  private def printPlayerGrids(playerNum: Int): HtmlElement =
    div(
      cls := s"bs-print-player bs-print-player--p$playerNum stack",
      h3(cls := "bs-print-player__title",
        child.text <-- AppState.strings.map(Player.labelOf(playerNum, _))
      ),
      div(
        cls := "bs-print-fleet muted",
        child.text <-- AppState.strings.map(_.offline.battleships.fleetLine)
      ),
      div(
        cls := "bs-print-grids",
        printGrid(_.offline.battleships.ownLabel),
        printGrid(_.offline.battleships.enemyLabel)
      )
    )

  private def printGrid(label: Strings => String): HtmlElement =
    div(
      cls := "bs-print-grid",
      div(cls := "bs-print-grid__label", child.text <-- AppState.strings.map(label)),
      div(
        cls := "bs-print-grid__table",
        div(cls := "bs-print-cell bs-print-cell--header", ""),
        PrintColumnLetters.map(c => div(cls := "bs-print-cell bs-print-cell--header", c.toString)),
        (1 to Size).flatMap { row =>
          div(cls := "bs-print-cell bs-print-cell--header", row.toString) ::
          (0 until Size).map(_ => div(cls := "bs-print-cell")).toList
        }
      )
    )
