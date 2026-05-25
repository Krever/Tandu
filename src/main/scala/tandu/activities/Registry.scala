package tandu.activities

import scala.util.Random

object Registry:
  val all: List[Activity] = List(Battleships, TicTacToe)

  def byId(id: String): Option[Activity] = all.find(_.id == id)

  private var lastPicked: Option[String] = None

  def pickRandom(): Activity =
    val candidates = all.filterNot(a => lastPicked.contains(a.id))
    val pool = if candidates.isEmpty then all else candidates
    val pick = pool(Random.nextInt(pool.size))
    lastPicked = Some(pick.id)
    pick
