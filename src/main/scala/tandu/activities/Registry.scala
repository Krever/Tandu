package tandu.activities

import scala.util.Random

object Registry:
  val all: List[Activity] = List(Battleships, Solitaire, TicTacToe, Memory, Hangman, Checkers, Chess, WordAssociation, Categories, TwentyQuestions, StoryBuilding)

  def byId(id: String): Option[Activity] = all.find(_.id == id)

  def filtered(category: Option[Category]): List[Activity] = category match
    case None    => all
    case Some(c) => all.filter(_.categories.contains(c))

  private var lastPicked: Option[String] = None

  private def orFallback[A](xs: List[A], fallback: List[A]): List[A] =
    if xs.isEmpty then fallback else xs

  def pickRandom(category: Option[Category]): Activity =
    val byCategory = orFallback(filtered(category), all)
    val pool = orFallback(byCategory.filterNot(a => lastPicked.contains(a.id)), byCategory)
    val pick = pool(Random.nextInt(pool.size))
    lastPicked = Some(pick.id)
    pick
