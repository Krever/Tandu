package tandu.activities.battleships

import com.raquo.laminar.api.L.*
import tandu.AppState
import tandu.activities.battleships.Domain.{Cell, EnemyView, Fleet, allCells}
import tandu.ui.Components.s

/** Laminar widgets for rendering battleship boards. Operates on the
  * domain types in [[Domain]] — both the pass-and-play and P2P
  * activities reuse these widgets by projecting their state into
  * `Signal[EnemyView]` / `Signal[Fleet]` / `Signal[Set[Cell]]`. */
object Board:

  enum CellKind:
    case Empty, Ship, Hit, Miss, Sunk

  final case class CellView(kind: CellKind, label: String)

  // ---------- low-level grid ----------

  def boardView(
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
          // Stable hook for e2e tests to click any specific cell.
          dataAttr("cell") := s"${cellPos.x},${cellPos.y}",
          dataAttr("kind") <-- viewSig.map(_.kind.toString.toLowerCase),
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

  // ---------- enemy board ----------

  /** Project an EnemyView into the per-cell view vector the grid expects. */
  private def cellsFromEnemyView(view: EnemyView): Vector[CellView] =
    allCells.map { c =>
      if view.sunk.contains(c) then CellView(CellKind.Sunk, "✖")
      else if view.hits.contains(c) then CellView(CellKind.Hit, "✖")
      else if view.misses.contains(c) then CellView(CellKind.Miss, "·")
      else CellView(CellKind.Empty, "")
    }

  /** Enemy waters: the board the shooter clicks on, derived purely from
    * what they've learned. `extras` slot in additional content
    * underneath (e.g. opponent fleet status in pass-and-play). */
  def enemyBoardSection(
      view: Signal[EnemyView],
      onTap: Option[Cell => Unit],
      highlight: Option[Cell] = None,
      extras: Modifier[HtmlElement]*
  ): HtmlElement =
    sectionTag(
      cls := "stack",
      dataAttr("testid") := "enemy-board",
      h2(cls := "h2 center", child.text <-- s(_.battleships.enemyBoard)),
      boardView(view.map(cellsFromEnemyView), onTap, highlight),
      extras
    )

  // ---------- own board ----------

  private def cellsFromOwnBoard(fleet: Fleet, incoming: Set[Cell]): Vector[CellView] =
    allCells.map { c =>
      val isShip = fleet.isHit(c)
      val isShot = incoming.contains(c)
      (isShip, isShot) match
        case (true,  true)  => CellView(CellKind.Hit, "✖")
        case (true,  false) => CellView(CellKind.Ship, "")
        case (false, true)  => CellView(CellKind.Miss, "·")
        case (false, false) => CellView(CellKind.Empty, "")
    }

  /** My board: shows my fleet plus any shots that have landed on it,
    * gated behind a show/hide toggle so passing the phone in
    * pass-and-play doesn't leak ship positions. */
  def myBoardSection(
      fleet: Signal[Fleet],
      incoming: Signal[Set[Cell]],
      showMyBoard: Var[Boolean],
      extraCls: String = ""
  ): HtmlElement =
    sectionTag(
      cls := s"stack $extraCls",
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
            fleet.combineWith(incoming).map((f, i) => cellsFromOwnBoard(f, i)),
            onTap = None
          )
        )
      }
    )

  // ---------- chrome ----------

  /** Centered "badge" header used above each phase view. */
  def headerBadge(text: Signal[String]): HtmlElement =
    div(
      cls := "center",
      div(cls := "player-badge", child.text <-- text)
    )

  /** End-of-game / handoff card. `actions` slots in optional buttons
    * (e.g. "Play again" in pass-and-play; none in P2P). */
  def handoffCard(
      badge: Signal[String],
      title: Signal[String],
      actions: Modifier[HtmlElement]*
  ): HtmlElement =
    div(
      cls := "handoff card no-print",
      div(cls := "player-badge", child.text <-- badge),
      div(cls := "handoff__title", child.text <-- title),
      actions
    )
