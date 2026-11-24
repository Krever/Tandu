package tandu.ui

import org.scalatest.funsuite.AnyFunSuite

import scala.util.Random

class SheetBuilderSpec extends AnyFunSuite:
  private val ids = Vector("a", "b", "c")

  test("rounds: each id once per round") {
    for seed <- 1 to 20 do
      val r = SheetBuilder.rounds(ids, 9, new Random(seed))
      val _ = assert(r.size == 9)
      for round <- r.grouped(3) do
        val _ = assert(round.sorted == ids, s"seed $seed: $r")
  }

  test("rounds: no id twice in a row at a round boundary") {
    for seed <- 1 to 20 do
      val r = SheetBuilder.rounds(ids, 9, new Random(seed))
      val _ = assert(r.sliding(2).forall(p => p(0) != p(1)), s"seed $seed: $r")
  }

  test("rounds: one id repeats") {
    assert(SheetBuilder.rounds(Vector("x"), 4, new Random(1)) == Vector.fill(4)("x"))
  }
