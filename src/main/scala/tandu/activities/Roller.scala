package tandu.activities

import scala.util.Random

/** A non-repeating random sequence over a pool.
  *
  * Each `next` returns a uniformly random element, but no element repeats
  * until the whole pool has been handed out; then the pool reshuffles and a
  * new cycle begins. The reshuffle is invisible: the first draw of a new
  * cycle is never equal to the last draw of the previous one (when the pool
  * has more than one element), so two consecutive draws always differ.
  *
  * Passing a different pool than the previous call — e.g. after a language or
  * difficulty switch — transparently restarts the cycle on the new pool.
  *
  * Holds mutable state, so create one per play session (typically a `val` in
  * an activity's `render`). It is not thread-safe, which is fine on the
  * single-threaded JS runtime.
  */
final class Roller[A]:
  private var pool: Vector[A] = Vector.empty
  private var bag: List[A]    = Nil       // shuffled items not yet drawn this cycle
  private var last: Option[A] = None      // most recent draw, to bridge cycles

  def next(from: Vector[A]): A =
    require(from.nonEmpty, "Roller.next: empty pool")
    if from != pool then
      pool = from
      bag = Nil
    if bag.isEmpty then refill()
    val head = bag.head
    bag = bag.tail
    last = Some(head)
    head

  private def refill(): Unit =
    val shuffled = Random.shuffle(pool).toList
    bag = last match
      // Rotate a repeated head to the end so the cycle seam isn't a repeat.
      case Some(l) if pool.sizeIs > 1 && shuffled.headOption.contains(l) =>
        shuffled.tail :+ l
      case _ => shuffled
