package tandu.activities

import org.scalatest.funsuite.AnyFunSuite

import scala.util.Random

class SudokuSpec extends AnyFunSuite:

  private def isValidSolution(v: Sudoku.Variant, grid: Sudoku.Grid): Boolean =
    val full = (1 to v.size).toSet
    val rowsOk = grid.forall(_.toSet == full)
    val colsOk = (0 until v.size).forall(c => grid.map(_(c)).toSet == full)
    val boxesOk =
      (0 until v.size by v.boxRows).forall { br =>
        (0 until v.size by v.boxCols).forall { bc =>
          val box = for
            r <- br until br + v.boxRows
            c <- bc until bc + v.boxCols
          yield grid(r)(c)
          box.toSet == full
        }
      }
    rowsOk && colsOk && boxesOk

  for v <- Sudoku.variants do
    test(s"generate produces a valid, consistent puzzle for ${v.id}") {
      val rng = new Random(42)
      for _ <- 1 to 3 do
        val (puzzle, solution) = Sudoku.generate(v, rng)
        val _ = assert(isValidSolution(v, solution))
        // Every clue must agree with the solution.
        for r <- 0 until v.size; c <- 0 until v.size do
          val _ = assert(puzzle(r)(c) == 0 || puzzle(r)(c) == solution(r)(c))
        val clues = puzzle.flatten.count(_ != 0)
        val _ = assert(clues >= v.clues, s"expected at least ${v.clues} clues, got $clues")
        // Digging should get reasonably close to the target.
        assert(clues <= v.clues + 6, s"expected close to ${v.clues} clues, got $clues")
    }
