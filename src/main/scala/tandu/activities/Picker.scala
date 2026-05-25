package tandu.activities

import scala.util.Random

object Picker:
  /** Pick uniformly at random, avoiding `avoid` when the pool has more
    * than one element so two consecutive draws differ. */
  def pickAvoiding[A](pool: Vector[A], avoid: Option[A]): A =
    val candidates = avoid match
      case Some(a) if pool.size > 1 => pool.filterNot(_ == a)
      case _                        => pool
    candidates(Random.nextInt(candidates.size))
