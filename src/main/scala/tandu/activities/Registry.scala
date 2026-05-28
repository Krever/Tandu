package tandu.activities

import scala.util.Random

object Registry:
  val all: List[Activity] = List(Battleships, Solitaire, TicTacToe, Memory, Hangman, Checkers, Chess, WordAssociation, Categories, TwentyQuestions, StoryBuilding, LastLetter, WouldYouRather)

  def byId(id: String): Option[Activity] = all.find(_.id == id)

  def filtered(partySize: Option[Players], handsFreeOnly: Boolean): List[Activity] =
    all
      .filter(a => partySize.forall(fitsParty(a, _)))
      .filter(a => !handsFreeOnly || a.handsFree)

  private def fitsParty(a: Activity, party: Players): Boolean = party match
    case Players.Solo  => a.minPlayers <= 1
    case Players.Two   => a.minPlayers <= 2 && a.maxPlayers >= 2
    case Players.Group => a.maxPlayers >= 3

  private var lastPicked: Option[String] = None

  private def orFallback[A](xs: List[A], fallback: List[A]): List[A] =
    if xs.isEmpty then fallback else xs

  def pickRandom(partySize: Option[Players], handsFreeOnly: Boolean): Activity =
    val matching = orFallback(filtered(partySize, handsFreeOnly), all)
    val pool = orFallback(matching.filterNot(a => lastPicked.contains(a.id)), matching)
    val pick = pool(Random.nextInt(pool.size))
    lastPicked = Some(pick.id)
    pick
