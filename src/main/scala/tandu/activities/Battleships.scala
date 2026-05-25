package tandu.activities

import com.raquo.laminar.api.L.*
import tandu.AppState
import tandu.i18n.Strings
import tandu.ui.Components
import tandu.ui.Components.s

import scala.annotation.tailrec
import scala.util.Random

object Battleships extends Activity:
  val id = "battleships"
  def name(s: Strings): String = s.battleships.name
  def description(s: Strings): String = s.battleships.description
  val categories: Set[Category] = Set(Category.Tabletop)

  private val Size = 10
  // Polish variant: 1×4, 2×3, 3×2, 4×1 — placed largest first so big ships find space.
  private val FleetSizes = List(4, 3, 3, 2, 2, 2, 1, 1, 1, 1)

  final case class Cell(x: Int, y: Int)
  final case class Ship(cells: Set[Cell]):
    def contains(c: Cell): Boolean = cells.contains(c)
    def isSunk(shots: Set[Cell]): Boolean = cells.subsetOf(shots)

  final case class Fleet(ships: List[Ship]):
    lazy val occupied: Set[Cell] = ships.flatMap(_.cells).toSet
    def isHit(c: Cell): Boolean = occupied.contains(c)
    def shipAt(c: Cell): Option[Ship] = ships.find(_.contains(c))
    def allSunk(shots: Set[Cell]): Boolean = occupied.subsetOf(shots)

  enum ShotResult:
    case Miss, Hit, Sunk

  enum Phase:
    case AwaitingShot(player: Player)
    case ShotResolved(player: Player, target: Cell, result: ShotResult)
    case GameOver(winner: Player)

  final case class GameState(
      fleets: Map[Player, Fleet],
      shotsFired: Map[Player, Set[Cell]],
      phase: Phase
  ):
    def shotsAt(p: Player): Set[Cell] = shotsFired(p.other) // shots received by p
    def withShot(by: Player, c: Cell): GameState =
      copy(shotsFired = shotsFired.updated(by, shotsFired(by) + c))

  // ---------- placement ----------

  private def inBounds(c: Cell): Boolean =
    c.x >= 0 && c.x < Size && c.y >= 0 && c.y < Size

  private def withNeighbors(cells: Set[Cell]): Set[Cell] =
    cells.flatMap(c =>
      for dx <- -1 to 1; dy <- -1 to 1
      yield Cell(c.x + dx, c.y + dy)
    )

  private def diagonalNeighbors(c: Cell): Set[Cell] = Set(
    Cell(c.x - 1, c.y - 1),
    Cell(c.x + 1, c.y - 1),
    Cell(c.x - 1, c.y + 1),
    Cell(c.x + 1, c.y + 1)
  )

  def randomFleet(): Fleet =
    @tailrec
    def placeOne(size: Int, forbidden: Set[Cell]): Ship =
      val horizontal = Random.nextBoolean()
      val xMax = if horizontal then Size - size else Size - 1
      val yMax = if horizontal then Size - 1 else Size - size
      val x = Random.nextInt(xMax + 1)
      val y = Random.nextInt(yMax + 1)
      val cells = (0 until size).map { i =>
        if horizontal then Cell(x + i, y) else Cell(x, y + i)
      }.toSet
      if cells.exists(forbidden.contains) then placeOne(size, forbidden)
      else Ship(cells)

    val ships = FleetSizes.foldLeft(List.empty[Ship]) { (acc, sz) =>
      val forbidden = withNeighbors(acc.flatMap(_.cells).toSet)
      placeOne(sz, forbidden) :: acc
    }
    Fleet(ships.reverse)

  private def freshGame(): GameState =
    GameState(
      fleets = Map(Player.P1 -> randomFleet(), Player.P2 -> randomFleet()),
      shotsFired = Map(Player.P1 -> Set.empty, Player.P2 -> Set.empty),
      phase = Phase.AwaitingShot(Player.P1)
    )

  // ---------- rendering ----------

  def render(): HtmlElement = renderPlay()

  private def renderPlay(): HtmlElement =
    val game = Var(freshGame())
    val showMyBoard = Var(false)

    def setPhase(p: Phase): Unit = game.update(_.copy(phase = p))

    def fire(shooter: Player, c: Cell): Unit =
      val g = game.now()
      if g.shotsFired(shooter).contains(c) then ()
      else
        val enemy = g.fleets(shooter.other)
        val baseShots = g.shotsFired(shooter) + c
        val (result, nextShots) =
          if !enemy.isHit(c) then (ShotResult.Miss, baseShots)
          else
            val ship = enemy.shipAt(c).get
            if ship.isSunk(baseShots) then
              val ring = withNeighbors(ship.cells).diff(ship.cells).filter(inBounds)
              (ShotResult.Sunk, baseShots ++ ring)
            else
              // Ships can't touch even diagonally, so the 4 diagonal
              // neighbors of any hit cell can't hold a ship — mark them as
              // miss to save the player taps.
              val diag = diagonalNeighbors(c).filter(inBounds)
              (ShotResult.Hit, baseShots ++ diag)
        game.set(g.copy(
          shotsFired = g.shotsFired.updated(shooter, nextShots),
          phase = Phase.ShotResolved(shooter, c, result)
        ))

    def endTurn(): Unit =
      val g = game.now()
      g.phase match
        case Phase.ShotResolved(shooter, _, _) =>
          val enemy = g.fleets(shooter.other)
          if enemy.allSunk(g.shotsFired(shooter)) then
            setPhase(Phase.GameOver(shooter))
          else
            // No hand-off screen between shots — own board is hidden by
            // default and the turn header is colored by player, so passing
            // the phone is enough. Reset the toggle so the next player
            // starts with their own ships hidden.
            showMyBoard.set(false)
            setPhase(Phase.AwaitingShot(shooter.other))
        case _ => ()

    def restart(): Unit =
      showMyBoard.set(false)
      game.set(freshGame())

    div(
      cls := "stack-lg",
      child <-- game.signal.map(_.phase).map {
        case Phase.AwaitingShot(p)       => playerPage(p, turnView(p, game, fire, showMyBoard))
        case Phase.ShotResolved(p, t, r) => playerPage(p, resolvedView(p, t, r, game, endTurn, showMyBoard))
        case Phase.GameOver(w)           => playerPage(w, gameOverView(w, restart))
      },
      div(
        cls := "center no-print",
        // Print mode is not implemented yet — see DESIGN.md (printable board).
        button(
          cls := "btn btn--ghost",
          disabled := true,
          child.text <-- s(_.battleships.print)
        )
      )
    )

  private def playerPage(p: Player, content: HtmlElement): HtmlElement =
    div(
      cls := s"player-page player-page--p${p.num} stack-lg",
      content
    )

  // -- subviews

  private def playerHeader(me: Player): HtmlElement =
    div(
      cls := "center",
      div(
        cls := "player-badge",
        child.text <-- AppState.strings.map(str => s"${me.labelKey(str)} — ${str.battleships.yourTurn}")
      )
    )

  private def enemyWatersSection(
      shooter: Player,
      gameSignal: Signal[GameState],
      onTap: Option[Cell => Unit],
      highlight: Option[Cell]
  ): HtmlElement =
    sectionTag(
      cls := "stack",
      h2(cls := "h2 center", child.text <-- s(_.battleships.enemyBoard)),
      boardView(
        cells = gameSignal.map { g =>
          val enemyFleet = g.fleets(shooter.other)
          val myShots = g.shotsFired(shooter)
          allCells.map { c =>
            if myShots.contains(c) then
              if enemyFleet.isHit(c) then
                val ship = enemyFleet.shipAt(c).get
                if ship.isSunk(myShots) then CellView(CellKind.Sunk, "✖")
                else CellView(CellKind.Hit, "✖")
              else CellView(CellKind.Miss, "·")
            else CellView(CellKind.Empty, "")
          }
        },
        onTap = onTap,
        highlight = highlight
      ),
      opponentFleetStatus(gameSignal, shooter)
    )

  private def myBoardSection(
      me: Player,
      gameSignal: Signal[GameState],
      showMyBoard: Var[Boolean]
  ): HtmlElement =
    sectionTag(
      cls := "stack no-print",
      div(
        cls := "center",
        button(
          cls := "btn btn--ghost",
          child.text <-- showMyBoard.signal.combineWith(AppState.strings).map { (shown, str) =>
            if shown then str.battleships.hideMyBoard else str.battleships.showMyBoard
          },
          onClick --> (_ => showMyBoard.update(!_))
        )
      ),
      child.maybe <-- showMyBoard.signal.map { shown =>
        Option.when(shown)(
          boardView(
            cells = gameSignal.map { g =>
              val myFleet = g.fleets(me)
              val incoming = g.shotsFired(me.other)
              allCells.map { c =>
                val isShip = myFleet.isHit(c)
                val isShot = incoming.contains(c)
                (isShip, isShot) match
                  case (true,  true)  => CellView(CellKind.Hit, "✖")
                  case (true,  false) => CellView(CellKind.Ship, "")
                  case (false, true)  => CellView(CellKind.Miss, "·")
                  case (false, false) => CellView(CellKind.Empty, "")
              }
            },
            onTap = None
          )
        )
      }
    )

  private def turnView(
      me: Player,
      game: Var[GameState],
      fire: (Player, Cell) => Unit,
      showMyBoard: Var[Boolean]
  ): HtmlElement =
    val gameSignal = game.signal
    div(
      cls := "stack-lg",
      playerHeader(me),
      p(cls := "muted center", child.text <-- s(_.battleships.fireAt)),
      enemyWatersSection(me, gameSignal, onTap = Some(c => fire(me, c)), highlight = None),
      myBoardSection(me, gameSignal, showMyBoard)
    )

  private def resolvedView(
      shooter: Player,
      target: Cell,
      result: ShotResult,
      game: Var[GameState],
      endTurn: () => Unit,
      showMyBoard: Var[Boolean]
  ): HtmlElement =
    val gameSignal = game.signal
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
          child.text <-- s(_.battleships.endTurn),
          onClick --> (_ => endTurn())
        )
      ),
      enemyWatersSection(shooter, gameSignal, onTap = Some(_ => endTurn()), highlight = Some(target)),
      myBoardSection(shooter, gameSignal, showMyBoard)
    )

  private def gameOverView(winner: Player, restart: () => Unit): HtmlElement =
    div(
      cls := "handoff card no-print",
      div(
        cls := "player-badge",
        child.text <-- AppState.strings.map(str => winner.labelKey(str))
      ),
      div(
        cls := "handoff__title",
        child.text <-- s(_.battleships.allSunk)
      ),
      button(
        cls := "btn btn--player btn--lg",
        child.text <-- s(_.common.playAgain),
        onClick --> (_ => restart())
      )
    )

  // ---------- board primitives ----------

  private val allCells: Vector[Cell] =
    (for y <- 0 until Size; x <- 0 until Size yield Cell(x, y)).toVector

  private enum CellKind:
    case Empty, Ship, Hit, Miss, Sunk

  private final case class CellView(kind: CellKind, label: String)

  private def boardView(
      cells: Signal[Vector[CellView]],
      onTap: Option[Cell => Unit],
      highlight: Option[Cell] = None
  ): HtmlElement =
    div(
      cls := "board bs-grid",
      allCells.zipWithIndex.map { (cellPos, i) =>
        val viewSig = cells.map(_(i)).distinct
        div(
          cls := "cell",
          cls("cell--btn") := onTap.isDefined,
          cls("cell--ship") <-- viewSig.map(_.kind == CellKind.Ship),
          cls("cell--hit")  <-- viewSig.map(_.kind == CellKind.Hit),
          cls("cell--miss") <-- viewSig.map(_.kind == CellKind.Miss),
          cls("cell--sunk") <-- viewSig.map(_.kind == CellKind.Sunk),
          cls("cell--fresh") := highlight.contains(cellPos),
          child.text <-- viewSig.map(_.label),
          onTap match
            case Some(handler) => onClick --> (_ => handler(cellPos))
            case None          => emptyMod
        )
      }
    )

  private val emptyMod: Modifier[HtmlElement] = emptyNode

  private def opponentFleetStatus(
      gameSignal: Signal[GameState],
      shooter: Player
  ): HtmlElement =
    div(
      cls := "fleet-list",
      styleAttr := "justify-content: center;",
      children <-- gameSignal.map { g =>
        val enemy = g.fleets(shooter.other)
        val shots = g.shotsFired(shooter)
        enemy.ships.sortBy(-_.cells.size).map { ship =>
          span(
            cls := "fleet-pill",
            cls("is-sunk") := ship.isSunk(shots),
            "■" * ship.cells.size
          )
        }
      }
    )

