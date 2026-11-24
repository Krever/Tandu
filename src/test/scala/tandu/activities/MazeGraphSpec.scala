package tandu.activities

import org.scalatest.funsuite.AnyFunSuite

import scala.collection.mutable
import scala.util.Random

class MazeGraphSpec extends AnyFunSuite:
  import MazeGraph.*
  import Maze.Style

  /** Cells that can be reached from the start through open passages. */
  private def reachable(c: Carved): Int =
    val seen = mutable.Set(c.layout.start)
    val queue = mutable.Queue(c.layout.start)
    while queue.nonEmpty do
      val cell = queue.dequeue()
      for n <- c.layout.neighbours(cell) if c.open(cell, n) && seen.add(n) do queue.enqueue(n)
    seen.size

  private def symmetric(l: Layout): Boolean =
    (0 until l.cellCount).forall(a => l.neighbours(a).forall(b => l.neighbours(b).contains(a)))

  private def perfect(l: Layout): Unit =
    for style <- Style.values; seed <- 1 to 5 do
      val c = carve(l, style, 0.0, new Random(seed))
      val _ = assert(reachable(c) == l.cellCount, s"$l $style seed $seed")
      val _ = assert(c.passages.size == l.cellCount - 1, s"$l $style seed $seed")

  test("rect neighbours are symmetric and 2 to 4 per cell") {
    val l = RectLayout(5, 7)
    val _ = assert(symmetric(l))
    assert((0 until l.cellCount).forall(c => (2 to 4).contains(l.neighbours(c).size)))
  }

  test("every style carves a perfect maze on the rect layout") {
    perfect(RectLayout(12, 15))
  }

  test("loops open every dead end except start and goal") {
    val l = RectLayout(12, 15)
    for style <- Style.values do
      val c = carve(l, style, 1.0, new Random(3))
      val deadEnds = (0 until l.cellCount).filter(c.degree(_) == 1)
      val _ = assert(deadEnds.forall(d => d == l.start || d == l.goal), s"$style: $deadEnds")
      val _ = assert(reachable(c) == l.cellCount)
  }

  // ---------- hex and circle ----------

  test("hex neighbours are symmetric; interior cells have 6") {
    val l = HexLayout(7, 9)
    val _ = assert(symmetric(l))
    val _ = assert(l.neighbours(3 * 9 + 4).size == 6)
    assert(Set(2, 3).contains(l.neighbours(0).size))
  }

  test("circle rings: centre is one cell, outer rings subdivide evenly") {
    val l = CircleLayout(8)
    val _ = assert(l.ringSizes.size == 9)
    val _ = assert(l.ringSizes(0) == 1)
    for i <- 2 to 8 do
      val _ = assert(l.ringSizes(i) % l.ringSizes(i - 1) == 0, s"ring $i: ${l.ringSizes}")
    for i <- 1 to 8 do
      val w = 2 * math.Pi * i / l.ringSizes(i)
      val _ = assert(w >= 0.7 && w <= 1.5, s"ring $i arc width $w: ${l.ringSizes}")
  }

  test("circle neighbours are symmetric; centre is the goal; start is on the outer ring at the bottom") {
    val l = CircleLayout(8)
    val _ = assert(symmetric(l))
    val _ = assert(l.goal == 0)
    val _ = assert(l.neighbours(0).size == l.ringSizes(1))
    val _ = assert(l.ringOf(l.start) == 8)
    val (a0, a1) = l.arc(l.start)
    assert(a0 < math.Pi / 2 && math.Pi / 2 < a1, s"start arc ($a0, $a1)")
  }

  test("every style carves a perfect maze on hex and circle") {
    perfect(HexLayout(9, 11))
    perfect(CircleLayout(7))
  }
