package tandu.activities.battleships

import scala.annotation.tailrec
import scala.util.Random

/** Pure battleships domain — board geometry, fleet placement, shot
  * resolution, and the two perspectives a player can hold on the
  * game (their own incoming shots and what they've learned about
  * the opponent).
  *
  * No UI, no networking. Both the pass-and-play and P2P activities
  * sit on top of this. */
object Domain:

  val Size: Int = 10

  // Polish variant: 1×4, 2×3, 3×2, 4×1 — placed largest first so big ships find space.
  private val FleetSizes: List[Int] = List(4, 3, 3, 2, 2, 2, 1, 1, 1, 1)

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

  /** What a defender reports about a single incoming shot. The same
    * shape is produced locally in pass-and-play and received over the
    * wire in P2P. */
  final case class Resolution(
      target: Cell,
      result: ShotResult,
      sunkCells: Set[Cell],   // non-empty iff result == Sunk
      gameOver: Boolean
  )

  /** Everything a shooter has learned about the opponent's board from
    * the resolutions reported back. Auto-marked diagonals (around hits)
    * and rings (around sunks) are folded into [[misses]] so the UI
    * doesn't need to know about that rule. */
  final case class EnemyView(
      misses: Set[Cell] = Set.empty,
      hits: Set[Cell] = Set.empty,
      sunk: Set[Cell] = Set.empty
  ):
    def knownCells: Set[Cell] = misses ++ hits ++ sunk

    /** Apply a resolution from the defender. Adds the new info plus the
      * inferred-empty cells around it (rule: ships can't touch even
      * diagonally, so the diagonals of a hit and the full ring of a
      * sunk are known misses). */
    def apply(res: Resolution): EnemyView =
      res.result match
        case ShotResult.Miss =>
          copy(misses = misses + res.target)
        case ShotResult.Hit =>
          val diag = diagonalNeighbors(res.target).filter(inBounds) -- knownCells
          copy(hits = hits + res.target, misses = misses ++ diag)
        case ShotResult.Sunk =>
          val ring = withNeighbors(res.sunkCells).diff(res.sunkCells).filter(inBounds) -- knownCells
          copy(
            hits   = hits -- res.sunkCells,
            sunk   = sunk ++ res.sunkCells,
            misses = misses ++ ring
          )

  // ---------- geometry ----------

  def inBounds(c: Cell): Boolean =
    c.x >= 0 && c.x < Size && c.y >= 0 && c.y < Size

  def withNeighbors(cells: Set[Cell]): Set[Cell] =
    cells.flatMap(c =>
      for dx <- -1 to 1; dy <- -1 to 1
      yield Cell(c.x + dx, c.y + dy)
    )

  def diagonalNeighbors(c: Cell): Set[Cell] = Set(
    Cell(c.x - 1, c.y - 1),
    Cell(c.x + 1, c.y - 1),
    Cell(c.x - 1, c.y + 1),
    Cell(c.x + 1, c.y + 1)
  )

  val allCells: Vector[Cell] =
    (for y <- 0 until Size; x <- 0 until Size yield Cell(x, y)).toVector

  // ---------- fleet placement ----------

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

  // ---------- shot resolution ----------

  /** Single source of truth for "what did this shot do?" Used by both
    * the pass-and-play orchestrator (local resolution) and the P2P
    * defender (computing the reply to send over the wire).
    *
    * @param fleet    the defender's fleet
    * @param incoming the defender's accumulated incoming shots so far,
    *                 NOT yet including [[target]]
    * @param target   the cell just shot at */
  def resolveShot(fleet: Fleet, incoming: Set[Cell], target: Cell): Resolution =
    if !fleet.isHit(target) then
      Resolution(target, ShotResult.Miss, Set.empty, gameOver = false)
    else
      val ship = fleet.shipAt(target).get
      val nextIncoming = incoming + target
      if ship.isSunk(nextIncoming) then
        Resolution(target, ShotResult.Sunk, ship.cells, gameOver = fleet.allSunk(nextIncoming))
      else
        Resolution(target, ShotResult.Hit, Set.empty, gameOver = false)
