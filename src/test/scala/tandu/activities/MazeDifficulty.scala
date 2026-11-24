package tandu.activities

import scala.collection.mutable
import scala.util.Random

/** Difficulty measures of a carved maze, for statistical tests.
  *
  *   - `solution`: the cells of the shortest path from start to goal.
  *   - `decisions`: the cells on the solution (not the goal) where the
  *     solver can go more than one new way: a junction with a choice.
  *   - `effort`: the mean number of moves of a "random explorer". At each
  *     cell it takes a random open passage that it has not used; at a dead
  *     end it goes back. It stops at the goal. Back moves count. This is
  *     approximately how long a child who does not plan is busy.
  *   - `easiest`: the smaller of `effort` and the same measure from the
  *     goal back to the start. Many people solve a maze from the goal when
  *     that is easier. */
object MazeDifficulty:

  final case class Stats(cells: Double, solution: Double, decisions: Double, effort: Double, easiest: Double):
    /** Effort per solution cell: how much of the work is lost in wrong
      * branches. 1.0 means that the solver never goes wrong. */
    def detour: Double = effort / solution

  def solution(m: MazeGraph.Carved): Vector[Int] =
    val dist = m.distancesFrom(m.start)
    // Walk back from the goal, each step to a neighbour one step nearer.
    val back = Iterator.iterate(m.layout.goal)(cell =>
      m.layout.neighbours(cell).find(n => m.open(cell, n) && dist(n) == dist(cell) - 1).get)
    back.take(dist(m.layout.goal) + 1).toVector.reverse

  def decisions(m: MazeGraph.Carved): Int =
    val path = solution(m)
    path.init.zipWithIndex.count { (cell, i) =>
      val back = if i == 0 then None else Some(path(i - 1))
      m.layout.neighbours(cell).count(n => m.open(cell, n) && !back.contains(n)) > 1
    }

  def effort(m: MazeGraph.Carved, rng: Random, runs: Int, backwards: Boolean = false): Double =
    val l = m.layout
    val (from, to) = if backwards then (l.goal, m.start) else (m.start, l.goal)
    def once(): Int =
      val seen = mutable.Set(from)
      val stack = mutable.Stack(from)
      var moves = 0
      while stack.top != to do
        val cell = stack.top
        val next = rng.shuffle(l.neighbours(cell).filter(n => m.open(cell, n) && !seen(n)))
        if next.isEmpty then { val _ = stack.pop() }
        else
          seen += next.head
          stack.push(next.head)
        moves += 1
      moves
    (1 to runs).map(_ => once()).sum.toDouble / runs

  /** Mean stats over `mazes` carved mazes from the recipe. */
  def measure(c: Maze.SheetConfig, mazes: Int, seed: Long): Stats =
    val rng = new Random(seed)
    val pages = (mazes + c.perPage.n - 1) / c.perPage.n
    val all = Maze.sheetPages(c, pages, seed).flatMap(_.mazes).take(mazes)
    val rows = all.map { m =>
      val forward = effort(m, rng, 30)
      Vector(m.layout.cellCount.toDouble, solution(m).size.toDouble, decisions(m).toDouble, forward,
        math.min(forward, effort(m, rng, 30, backwards = true)))
    }
    def mean(i: Int) = rows.map(_(i)).sum / rows.size
    Stats(mean(0), mean(1), mean(2), mean(3), mean(4))
