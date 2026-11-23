package tandu.activities

import org.scalatest.funsuite.AnyFunSuite

import scala.util.Random

class FourInARowSpec extends AnyFunSuite:
  import FourInARow.*

  /** Drop discs column by column, alternating P1/P2 from P1. Returns the
    * board and the index of the last disc placed. */
  private def play(cols: Int*): (Board, Int) =
    cols.zipWithIndex.foldLeft((emptyBoard, -1)) { case ((b, _), (c, i)) =>
      drop(b, c, if i % 2 == 0 then Player.P1 else Player.P2).get
    }

  test("discs stack from the bottom of a column") {
    val (b, at) = play(3, 3)
    val _ = assert(b(idx(3, Rows - 1)).contains(Player.P1))
    val _ = assert(at == idx(3, Rows - 2))
    assert(b(at).contains(Player.P2))
  }

  test("a full column is no longer legal") {
    val (b, _) = play(0, 0, 0, 0, 0, 0)
    val _ = assert(!legalCols(b).contains(0))
    assert(drop(b, 0, Player.P1).isEmpty)
  }

  test("detects horizontal, vertical and both diagonal wins") {
    val (h, hAt) = play(0, 0, 1, 1, 2, 2, 3)
    val _ = assert(winThrough(h, hAt).map(_.size).contains(4))

    val (v, vAt) = play(5, 6, 5, 6, 5, 6, 5)
    val _ = assert(winThrough(v, vAt).contains((Rows - 4 until Rows).map(r => idx(5, r)).toSet))

    // Rising diagonal for P1: (0,5) (1,4) (2,3) (3,2).
    val (d, dAt) = play(0, 1, 1, 2, 2, 3, 2, 3, 3, 6, 3)
    val _ = assert(winThrough(d, dAt).isDefined)

    // Falling diagonal for P1, mirrored.
    val (f, fAt) = play(6, 5, 5, 4, 4, 3, 4, 3, 3, 0, 3)
    assert(winThrough(f, fAt).isDefined)
  }

  test("three in a row is not a win") {
    val (b, at) = play(0, 0, 1, 1, 2)
    assert(winThrough(b, at).isEmpty)
  }

  test("computer takes an immediate win") {
    // P2 (computer) has three stacked in column 6; P1 has three on the bottom row.
    val (b, _) = play(0, 6, 1, 6, 2, 6, 4)
    (1 to 50).foreach(seed => assert(computerMove(b, Player.P2, new Random(seed)) == 6))
  }

  test("computer usually blocks an immediate threat") {
    // P1 threatens column 3 on the bottom row; computer to move.
    val (b, _) = play(0, 6, 1, 6, 2)
    val blocks = (1 to 200).count(seed => computerMove(b, Player.P2, new Random(seed)) == 3)
    assert(blocks > 140, s"blocked only $blocks/200")
  }

  test("computer always returns a legal column") {
    val rnd = new Random(7)
    var b   = emptyBoard
    var p   = Player.P1
    while legalCols(b).nonEmpty do
      val c = computerMove(b, p, rnd)
      val _ = assert(legalCols(b).contains(c))
      b = drop(b, c, p).get._1
      p = p.other
  }
