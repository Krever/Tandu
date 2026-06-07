package tandu.activities

import tandu.Kind

import scala.util.Random

object Registry:
  val all: List[Activity] = List(Battleships, Solitaire, TicTacToe, Memory, MemoryChain, Hangman, Checkers, Chess, Sudoku, Minesweeper, Maze, WordSearch, GuideRobot, FreezeDance, WordAssociation, Categories, TwentyQuestions, ISpy, StoryBuilding, LastLetter, WouldYouRather, WordBuilder, MathPractice, Clock, Reading)

  def byId(id: String): Option[Activity] = all.find(_.id == id)

  def filtered(
      partySize: Option[Players],
      handsFreeOnly: Boolean,
      kind: Kind,
      favouritesOnly: Boolean,
      favourites: Set[String],
      hidden: Set[String]
  ): List[Activity] =
    all
      .filter(a => kind == Kind.All || a.kind == kind)
      .filter(a => partySize.forall(fitsParty(a, _)))
      .filter(a => !handsFreeOnly || a.handsFree)
      .filter(a => !favouritesOnly || favourites.contains(a.id))
      .filterNot(a => hidden.contains(a.id))

  private def fitsParty(a: Activity, party: Players): Boolean = party match
    case Players.Solo  => a.minPlayers <= 1
    case Players.Two   => a.minPlayers <= 2 && a.maxPlayers >= 2
    case Players.Group => a.maxPlayers >= 3

  private var lastPicked: Option[String] = None

  private def orFallback[A](xs: List[A], fallback: List[A]): List[A] =
    if xs.isEmpty then fallback else xs

  /** Pick from an already-filtered pool, avoiding the previous draw. Lets a
    * caller that also needs the pool (e.g. the suggest reel) filter just once. */
  def pickRandom(pool: List[Activity]): Activity =
    val matching = orFallback(pool, all)
    val choices  = orFallback(matching.filterNot(a => lastPicked.contains(a.id)), matching)
    val pick     = choices(Random.nextInt(choices.size))
    lastPicked = Some(pick.id)
    pick
