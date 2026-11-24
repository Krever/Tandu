# Sudoku Print Builder Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give Sudoku the sheet-builder print page (presets, form, live preview, pages, URL, Print), with difficulty measured by a technique solver, an optional answers page and one workbook row kind with settings.

**Architecture:** A new pure `SudokuLogic` (rating solver and level-aware generator) and a new `SudokuSheet` (recipe, codec, lazy pages, sheet body, form, workbook hooks) plug into the existing `ui/SheetBuilder`. `SheetBuilder.Spec.pagesOf` widens to `IndexedSeq[P]`, and the pager counts the pages it gets, so answer pages can follow the puzzle pages. The in-app Sudoku does not change.

**Tech Stack:** Scala 3.8 / Scala.js, Laminar 17, Waypoint, scalatest on Node (`sbt test`), Playwright (`npx playwright test`).

**Spec:** `docs/superpowers/specs/2026-11-24-sudoku-print-builder-design.md`

## Global Constraints

- Work on branch `sudoku-print-builder` from `main`. One commit per task (message prefix `sudoku:`), ending with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- `-Werror`: no unused imports, members or parameters.
- Comments and docstrings in ASD-STE100 (no "thus"; use "therefore", "so" or two sentences).
- Every new string in all five languages in `Strings.scala` (en, pl, es, fr, de).
- Objects that touch the router or DOM when they start use `lazy val` (unit tests run on Node without a DOM).
- Run `sbt -batch fastLinkJS` before every Playwright run: the vite plugin links only when the server starts.
- The in-app Sudoku (variants, generator, play UI) does not change. Its `countSolutions` may become `private[activities]` for tests; nothing else.
- Seek & Find and Maze do not change behaviour: `e2e/seek-and-find.spec.ts`, `e2e/maze.spec.ts`, `e2e/pictures.spec.ts` pass unchanged.
- 9×9 is digits only. Shapes are ● ▲ ■ ✚ ✕ ◆, the first `n` for an n×n grid.
- Never a puzzle with more than one solution, and never one that needs guessing.

## Review Focus

1. Answers on, view the answer page (the last page), turn answers off → the preview shows a puzzle page, not an empty or broken one, and no console error. (Task 4 e2e.)
2. A hand-edited URL: `g9.shapes`, `g5`, `pp3`, `hard.hard`, empty token → valid recipe, 9×9 always digits. (Task 3 unit test.)
3. 10 pages of 4×4 at 6 per page with answers → 10 puzzle pages + 2 answer pages, and each answer matches its puzzle, in order. (Task 3 unit test; Task 4 e2e on a smaller sheet.)
4. 4×4 at Hard: the level cannot always be reached → the generator still returns a valid, uniquely solvable puzzle quickly (bounded tries). (Task 2 test.)
5. An old book with `sudoku-hard` and `sudoku-emoji4` rows → summaries "9×9 · Hard" and "Easy", 6 boards per page. (Task 5 e2e.)

---

### Task 1: Rating solver and level-aware generator

**Files:**
- Create: `src/main/scala/tandu/activities/SudokuLogic.scala`
- Modify: `src/main/scala/tandu/activities/Sudoku.scala` (only `countSolutions`: `private` → `private[activities]`)
- Test: `src/test/scala/tandu/activities/SudokuLogicSpec.scala`

**Interfaces:**
- Produces (all in `object SudokuLogic`):
  - `enum GridSize(val n: Int, val boxRows: Int, val boxCols: Int) { case G4 (4, 2, 2); case G6 (6, 2, 3); case G9 (9, 3, 3) }`
  - `enum Technique { case NakedSingle, HiddenSingle, NakedPair, LockedCandidates }`
  - `enum Level(val techniques: Set[Technique]) { case Easy, Medium, Hard }` with `def below: Option[Level]` (Easy → None). Easy = {NakedSingle}; Medium = + HiddenSingle; Hard = + NakedPair, LockedCandidates (pointing and box-line).
  - `final case class Rating(solved: Boolean, grid: Vector[Int], steps: Map[Technique, Int])` with `def advancedSteps: Int` (all steps except NakedSingle). `grid` is the grid as far as the solver came.
  - `def rate(size: GridSize, givens: IndexedSeq[Int], level: Level): Rating` — `givens` row-major, length n², 0 = empty, values 1..n.
  - `final case class Puzzle(givens: Vector[Int], solution: Vector[Int])` (row-major, 0 = empty in `givens`).
  - `def fill(size: GridSize, rng: Random): Vector[Int]` — a random full grid.
  - `val MaxTries = 20`
  - `def generate(size: GridSize, level: Level, rng: Random): Puzzle`

- [ ] **Step 0: Branch and spec**

```bash
git checkout -b sudoku-print-builder main
git add docs/superpowers/specs/2026-11-24-sudoku-print-builder-design.md docs/superpowers/plans/2026-11-24-sudoku-print-builder.md
```

(The spec and plan go into the Task 1 commit.)

- [ ] **Step 1: Write the failing tests** in `SudokuLogicSpec.scala`. Helpers: `def grid(rows: String*): Vector[Int]` reads `.` as 0; `def unique(size, givens): Boolean` calls `Sudoku.countSolutions(Sudoku.Variant("t", n, boxRows, boxCols, 0, emoji = false, _ => "", _ => ""), givens as Array[Array[Int]], 2) == 1`.

```scala
test("a full grid with one hole is Easy: one naked single") {
  val full = fill(GridSize.G9, new Random(1))
  val r = rate(GridSize.G9, full.updated(40, 0), Level.Easy)
  assert(r.solved && r.grid == full && r.steps == Map(Technique.NakedSingle -> 1))
}

test("a hidden single that is not a naked single needs Medium") {
  // 5 at (1,4), (2,7), (4,1), (7,2): in the top-left box, 5 fits only at (0,0).
  val g = Vector.fill(81)(0).updated(1 * 9 + 4, 5).updated(2 * 9 + 7, 5).updated(4 * 9 + 1, 5).updated(7 * 9 + 2, 5)
  val _ = assert(rate(GridSize.G9, g, Level.Easy).grid(0) == 0)
  val m = rate(GridSize.G9, g, Level.Medium)
  val _ = assert(m.grid(0) == 5)
  assert(m.steps.getOrElse(Technique.HiddenSingle, 0) >= 1 && !m.solved)
}

test("a puzzle that needs guessing is not solvable at Hard") {
  // AI Escargot (Arto Inkala): one solution, needs trial and error.
  val escargot = grid("1....7.9.", ".3..2...8", "..96..5..", "..53..9..", ".1..8...2", "6....4...", "3......1.", ".4......7", "..7...3..")
  val _ = assert(unique(GridSize.G9, escargot))
  assert(!rate(GridSize.G9, escargot, Level.Hard).solved)
}

test("an empty grid is not solvable at any level, and the solver stops") {
  for size <- GridSize.values; level <- Level.values do
    val _ = assert(!rate(size, Vector.fill(size.n * size.n)(0), level).solved)
}

test("generated puzzles: solvable at their level, the solver finds the solution, one solution") {
  for size <- GridSize.values; level <- Level.values; seed <- 1 to 5 do
    val p = generate(size, level, new Random(seed))
    val r = rate(size, p.givens, level)
    val _ = assert(r.solved && r.grid == p.solution, s"$size $level $seed")
    val _ = assert(p.givens.indices.forall(i => p.givens(i) == 0 || p.givens(i) == p.solution(i)))
    val _ = assert(unique(size, p.givens), s"$size $level $seed")
}

test("the generator is deterministic for a seed") {
  assert(generate(GridSize.G6, Level.Medium, new Random(9)) == generate(GridSize.G6, Level.Medium, new Random(9)))
}
```

- [ ] **Step 2: Run** `sbt -batch "testOnly tandu.activities.SudokuLogicSpec"`. Expected: compile error, `SudokuLogic` not found.

- [ ] **Step 3: Implement `SudokuLogic`.** Data: `Array[Int]` grid and `Array[Int]` candidate bitmasks (bit v for value v); precomputed units (rows, columns, boxes) and peers per cell. The rating loop:

```
place givens, compute candidates
loop:
  if no empty cell: solved
  if an empty cell has no candidate: stop, not solved
  try the level's techniques in this order, take the FIRST that makes progress, count one step for it, and go back to the top of the loop:
    NakedSingle      — a cell with one candidate: place it (update peers' candidates)
    HiddenSingle     — a value with one place left in a unit: place it
    NakedPair        — two cells of a unit with the same two candidates: remove those two from the unit's other cells
    LockedCandidates — a value's places in a box all in one row/column: remove it from the rest of that row/column (pointing);
                       a value's places in a row/column all in one box: remove it from the rest of that box (box-line)
  no technique made progress: stop, not solved
```

A pair or locked-candidates step counts only when it removes at least one candidate. Generator (approach A of the spec):

```
last = null
repeat MaxTries times:
  solution = fill(size, rng); givens = solution copy
  for pos in rng.shuffle(all cells):
    remove givens(pos); if !rate(size, givens, level).solved then put it back
  last = Puzzle(givens, solution)
  if level.below.forall(b => !rate(size, givens, b).solved) then return last
return last   // solvable at the level, so never harder than asked
```

`fill` is a randomized backtracker over the flat grid (the in-app `fillSolution` stays as it is).

- [ ] **Step 4: Run** `sbt -batch "testOnly tandu.activities.SudokuLogicSpec"`. Expected: all pass. If the Escargot test fails only on `unique`, the string is wrong: replace it with another well-known puzzle that needs guessing and record a ruling.

- [ ] **Step 5: Commit** `sudoku: rating solver and level-aware generator` (with the spec and the plan).

---

### Task 2: Difficulty statistics and the time budget

**Files:**
- Create: `src/test/scala/tandu/activities/SudokuDifficultySpec.scala`
- Modify (only if a test fails): `src/main/scala/tandu/activities/SudokuLogic.scala`

**Interfaces:**
- Consumes: Task 1 (`generate`, `rate`, `Rating.advancedSteps`, `Level.below`).

- [ ] **Step 1: Write the tests.** Puzzles from seeds 1..20 unless a test says otherwise.

```scala
ignore("report: givens, need-rate and steps by size and level") { /* prints a table, like MazeDifficultySpec */ }

test("9×9 Medium and Hard puzzles need their level in at least 90% of cases") {
  for level <- List(Level.Medium, Level.Hard) do
    val needs = (1 to 20).count { s =>
      val p = generate(GridSize.G9, level, new Random(s))
      !rate(GridSize.G9, p.givens, level.below.get).solved
    }
    val _ = assert(needs >= 18, s"$level: $needs / 20")
}

test("9×9: Easy < Medium < Hard in mean givens (fewer) and in mean advanced steps (more)") {
  // means over 20 puzzles per level: givens strictly falling, advancedSteps strictly rising
}

test("small grids at Hard: valid in bounded time") {
  for size <- List(GridSize.G4, GridSize.G6); s <- 1 to 10 do
    val p = generate(size, Level.Hard, new Random(s))
    val _ = assert(rate(size, p.givens, Level.Hard).solved)
}

test("one Hard 9×9 page (4 puzzles) takes at most about 300 ms") {
  generate(GridSize.G9, Level.Hard, new Random(0)) // warm up the JIT
  val ms = (1 to 3).map { s =>
    val t = System.nanoTime(); (1 to 4).foreach(i => generate(GridSize.G9, Level.Hard, new Random(s * 10 + i)))
    (System.nanoTime() - t) / 1e6
  }.sum / 3
  assert(ms <= 300, f"one Hard 9×9 page: $ms%.0f ms")
}
```

- [ ] **Step 2: Run** `sbt -batch "testOnly tandu.activities.SudokuDifficultySpec"`. Expected: the tests pass, or one fails with a number. Change the report to `test` once and keep its table for the final message.

- [ ] **Step 3: Tune only if a test fails.** Allowed knobs, in this order: make `rate` cheaper (incremental candidate updates, stop at the first contradiction); raise `MaxTries` (need-rate); remove cells in symmetric pairs (half the solver calls). Record each change as a ruling with the before/after numbers. Do not weaken an assertion; if a bound cannot be met, stop and report it.

- [ ] **Step 4: Run** `sbt -batch test`. Expected: the whole suite passes.

- [ ] **Step 5: Commit** `sudoku: difficulty statistics and time budget`.

---

### Task 3: Recipe, codec and lazy pages

**Files:**
- Create: `src/main/scala/tandu/activities/SudokuSheet.scala` (model part; the UI comes in Task 5)
- Test: `src/test/scala/tandu/activities/SudokuSheetSpec.scala`

**Interfaces:**
- Consumes: Task 1 (`GridSize`, `Level`, `Puzzle`, `generate`).
- Produces (all in `object SudokuSheet`):
  - `enum Symbols { case Shapes, Digits }`
  - `enum PerPage(val n: Int) { case One (1); case Two (2); case Four (4); case Six (6) }`
  - `final case class SheetConfig(size: GridSize, symbols: Symbols, level: Level, perPage: PerPage, answers: Boolean)`; extension `def withSize(g: GridSize): SheetConfig` (G9 sets Digits; others keep symbols).
  - `val sheetPresets: List[SheetBuilder.Preset[SheetConfig]]`: `easy` = (G4, Shapes, Easy, Six, false), `medium` = (G6, Shapes, Medium, Six, false), `hard` = (G9, Digits, Hard, Four, false). Names: `_.offline.sudoku.form.levelEasy` / `levelMedium` / `levelHard` (strings come in Task 5; until then, a temporary `_ => "Easy"` etc. is fine).
  - `def classicToken(level: Level): String` — encode of (G9, Digits, level, Six, false), for old 9×9 rows.
  - `def presetToken(presetId: String): String` — the token of a preset (the first preset for an unknown id). Once `sheetSpec` exists (Task 4), it delegates to `SheetBuilder.presetToken(sheetSpec, presetId)`.
  - `def encodeConfig(c: SheetConfig): String` — `g4.shapes.easy.pp6`, plus `.ans` when answers are on.
  - `def decodeConfig(token: String): SheetConfig` — fold the `.` tokens over the first preset; unknown or bad tokens are ignored; `g` other than 4/6/9 is ignored; `pp` other than 1/2/4/6 is ignored; G9 forces Digits after the fold; no `ans` token → answers off.
  - `def answersPerPage(size: GridSize): Int` — answers are compact, smaller than puzzles: G9 25 (5 × 5 boards of 34 mm), G6 36 (6 × 6 of 28 mm), G4 56 (8 × 7 of 21 mm).
  - `enum SheetPage { case Puzzles(puzzles: Vector[Puzzle]); case Answers(entries: Vector[(String, Puzzle)]) }` — entry label `"<page>·<puzzle>"`, both 1-based.
  - `def sheetPages(c: SheetConfig, n: Int, seed: Long): IndexedSeq[SheetPage]` — `n` puzzle pages, then `ceil(n * perPage.n / answersPerPage(size))` answer pages when answers are on. Lazy: a page is made when it is read, once (cached). Puzzle page k uses only `new Random(seed + 1_000_003L * k)`, so it does not depend on other pages. An answer page reads all puzzle pages.
  - The lazy sequence: `private final class LazyPages(n: Int, make: Int => SheetPage) extends IndexedSeq[SheetPage]` with a cache array; `equals`/`hashCode` by reference (element equality would make every page).

- [ ] **Step 1: Write the failing tests.**

```scala
test("codec round trip, presets and others") {
  for c <- sheetPresets.map(_.config) :+ SheetConfig(GridSize.G6, Symbols.Digits, Level.Hard, PerPage.Two, answers = true) do
    val _ = assert(decodeConfig(encodeConfig(c)) == c)
}

test("tokens") {
  val _ = assert(encodeConfig(preset("hard")) == "g9.digits.hard.pp4")
  assert(encodeConfig(preset("easy").copy(answers = true)) == "g4.shapes.easy.pp6.ans")
}

test("bad tokens keep the first preset's values; 9×9 is always digits") {
  val easy = preset("easy")
  val _ = assert(decodeConfig("") == easy)
  val _ = assert(decodeConfig("g5.pp3.bogus") == easy)
  val _ = assert(decodeConfig("g9.shapes").symbols == Symbols.Digits)
  val _ = assert(decodeConfig("hard.hard").level == Level.Hard)
  // The page count is not part of the recipe.
  assert(decodeConfig("g4.shapes.easy.pp6.p3") == easy)
}

test("withSize: 9×9 sets digits; back to 4×4 keeps digits") {
  val nine = preset("easy").withSize(GridSize.G9)
  val _ = assert(nine.symbols == Symbols.Digits)
  assert(nine.withSize(GridSize.G4).symbols == Symbols.Digits)
}

test("adding pages keeps the earlier pages; each page has perPage puzzles") {
  val c = preset("medium")
  val one = sheetPages(c, 1, 42L); val three = sheetPages(c, 3, 42L)
  val _ = assert(three.head == one.head && three.size == 3)
  assert(three.forall { case SheetPage.Puzzles(ps) => ps.size == 6; case _ => false })
}

test("answers: extra pages at the end, labelled page·puzzle, matching the puzzles") {
  val c = SheetConfig(GridSize.G4, Symbols.Shapes, Level.Easy, PerPage.Six, answers = true)
  val ps = sheetPages(c, 10, 7L)
  val _ = assert(ps.size == 12) // 60 puzzles, 56 per 4×4 answer page
  val puzzles = ps.take(10).zipWithIndex.flatMap { case (SheetPage.Puzzles(xs), k) => xs.zipWithIndex.map((p, j) => (s"${k + 1}·${j + 1}", p)); case _ => Nil }
  val answers = ps.drop(10).flatMap { case SheetPage.Answers(es) => es; case _ => Nil }
  assert(answers == puzzles)
}

test("reading one page makes only that page, once") {
  val made = scala.collection.mutable.ListBuffer.empty[Int]
  val pages = lazyPages(10, i => { made += i; SheetPage.Puzzles(Vector.empty) })
  val _ = assert(pages.size == 10 && made.isEmpty)
  val _ = pages(3); val _ = pages(3)
  assert(made.toList == List(3))
}
```

`lazyPages` is `private[activities] def lazyPages(n: Int, make: Int => SheetPage): IndexedSeq[SheetPage]`; `sheetPages` uses it.

- [ ] **Step 2: Run** `sbt -batch "testOnly tandu.activities.SudokuSheetSpec"`. Expected: compile error, `SudokuSheet` not found.

- [ ] **Step 3: Implement** the Produces list above in `SudokuSheet.scala`. Codec helpers from `SheetBuilder` (`named`, regex tokens like Maze's `SizeToken`).

- [ ] **Step 4: Run** `sbt -batch "testOnly tandu.activities.SudokuSheetSpec"`. Expected: all pass.

- [ ] **Step 5: Commit** `sudoku: sheet recipe, codec and lazy pages`.

---

### Task 4: Print page: builder, sheet body, answers page, form

**Files:**
- Modify: `src/main/scala/tandu/ui/SheetBuilder.scala` (`Spec.pagesOf` type, pager, `pills`)
- Modify: `src/main/scala/tandu/activities/SudokuSheet.scala` (spec, body, form)
- Modify: `src/main/scala/tandu/activities/Sudoku.scala` (print mode; remove the old print code)
- Modify: `src/main/scala/tandu/activities/Maze.scala`, `SeekAndFind.scala` only if the `pagesOf` type needs it (it should not: `Vector` is an `IndexedSeq`)
- Modify: `src/main/scala/tandu/i18n/Strings.scala` (`SudokuOff.form: SudokuForm`, five languages)
- Modify: `styles.css` (sudoku print rules ~1927–1975)
- Test: `e2e/sudoku.spec.ts` (rewrite the print test)

**Interfaces:**
- Consumes: Task 3 (everything in its Produces list).
- Produces:
  - `SheetBuilder.Spec.pagesOf: (C, Int, Long) => IndexedSeq[P]`. The pager label is `shown / pagesOf(...).size`; "next" is disabled at the last page of that list; the preview reads `ps(min(shown, ps.size - 1))` (only that page is made). The pages stepper still sets `n`.
  - `SheetBuilder.pills` gets `disabled: A => Signal[Boolean] = (_: A) => Val(false)` (last parameter; existing calls do not change).
  - `SudokuSheet.sheetSpec: SheetBuilder.Spec[SheetConfig, SheetPage]` with `testIdPrefix = "sudoku"`, `title = _ => _.offline.sudoku.printTitle`, `newLabel = _.sudoku.newGame`.
  - `Strings.SudokuForm(size, symbols, symbolsShapes, symbolsDigits, level, levelEasy, levelMedium, levelHard, perPage, answers, answersTitle)`.
  - Test ids: `sudoku-size-4|6|9`, `sudoku-symbols-shapes|digits`, `sudoku-level-easy|medium|hard`, `sudoku-per-page-1|2|4|6`, `sudoku-answers`, plus the builder's `sudoku-preview`, `sudoku-preset-<id>`, `sudoku-pages`, `sudoku-page-prev|next|label`, `sudoku-print`, `sudoku-new`.
  - CSS classes: `.sudoku-print-sheet.sudoku-print-sheet--pp<n>`, `.sudoku-print-puzzle`, `.sudoku-print-legend`, `.sudoku-print-board` (+ `--4` / `--6` for small grids, as today), `.sudoku-print-cell` (+ `--boxl`, `--boxt`), `.sudoku-answers`, `.sudoku-answers__title`, `.sudoku-answer`, `.sudoku-answer__label`, `.sudoku-print-cell--given`.

Decisions for this task:
- Form order: Size, Symbols, Difficulty, Puzzles per page, Answers (switch via `SheetBuilder.flag`). Size and per-page labels are literal (`4×4`, `1`…`6`). The Shapes pill is disabled while the size is 9×9. Size taps go through `withSize`.
- Board size in mm, set inline on the board, cell font 0.6 × cell size (mm):

  | per page | 9×9 | 4×4 / 6×6 (+ legend) |
  |---|---|---|
  | 6 | 75 | 65 |
  | 4 | 88 | 80 |
  | 2 | 105 | 95 |
  | 1 | 170 | 150 |

  Grid columns: 6 → 2×3, 4 → 2×2, 2 and 1 → one column.
- Answers page body: an `h3.sudoku-answers__title` ("Answers" string) inside the page (the printable title stays the sheet title, so the workbook needs no change), then a grid of small solved boards, sized by grid size (see `answersPerPage`; columns 5 / 6 / 8 for 9×9 / 6×6 / 4×4). Givens bold (`--given`), filled cells light. Symbols as on the puzzle page.
- Print mode in `Sudoku.render()`: `SheetBuilder.printPage(SudokuSheet.sheetSpec, id, RulesCard.render(List(RulesCard.fromRules(_.offline.sudoku.rules))))`. Remove `renderOffline`, `PrintPuzzle`, `printablePuzzle`, `printSheetBody`, `printShapes`, `printSymbolsFor` and the `.sudoku-print-actions` CSS. `printSheetBody` callers (the workbook) move in Task 5; until then keep the workbook compiling: each old variant's source renders `SheetBuilder.workbookPages(SudokuSheet.sheetSpec, token, 1, ctx.rng).head` with the token of the Task 5 legacy mapping (emoji4 → `presetToken("easy")`, emoji6 → `presetToken("medium")`, easy/medium/hard → `classicToken(level)`). Task 5 replaces it.

- [ ] **Step 1: Write the failing e2e tests** in `e2e/sudoku.spec.ts` (replace the old print test; add the console-error guard and `window.print` stub from `e2e/maze.spec.ts`; use `stepperPlus`, `startPrinting` from `e2e/helpers.ts`):

```ts
test("print mode: presets fill the form, the preview follows, print matches it", async ({ page }) => {
  await page.goto("/activity/sudoku/print")
  const preview = page.getByTestId("sudoku-preview")
  // Easy: 6 4×4 boards with shape legends.
  await expect(preview.locator(".sudoku-print-board--4")).toHaveCount(6)
  await expect(preview.locator(".sudoku-print-legend").first()).toHaveText("● ▲ ■ ✚")
  await page.getByTestId("sudoku-preset-hard").click()
  await expect(preview.locator(".sudoku-print-board")).toHaveCount(4)
  await expect(preview.locator(".sudoku-print-legend")).toHaveCount(0)
  await expect(page).toHaveURL(/\/print\/g9\.digits\.hard\.pp4$/)
  // Print copies equal the preview (same seed).
  const cells = (b) => b.locator(".sudoku-print-cell").allTextContents()
  await startPrinting(page)
  expect(await cells(page.locator(".print-only .sudoku-print-board").first())).toEqual(await cells(preview.locator(".sudoku-print-board").first()))
})

test("9×9 disables Shapes and switches to digits", async ({ page }) => {
  await page.goto("/activity/sudoku/print")
  await page.getByTestId("sudoku-size-9").click()
  await expect(page.getByTestId("sudoku-symbols-shapes")).toBeDisabled()
  await expect(page.getByTestId("sudoku-symbols-digits")).toHaveClass(/is-active/)
})

test("the URL recipe is read back", async ({ page }) => {
  await page.goto("/activity/sudoku/print/g6.digits.medium.pp2.ans.p2")
  await expect(page.getByTestId("sudoku-size-6")).toHaveClass(/is-active/)
  await expect(page.getByTestId("sudoku-per-page-2")).toHaveClass(/is-active/)
  await expect(page.getByTestId("sudoku-page-label")).toHaveText("1 / 3") // 2 puzzle pages + 1 answer page
})

test("answers: off by default; on adds an answer page whose grids match the puzzles", async ({ page }) => {
  await page.goto("/activity/sudoku/print")
  await expect(page.getByTestId("sudoku-page-label")).toBeHidden() // 1 page, pager hidden
  await page.getByTestId("sudoku-answers").click()
  await expect(page.getByTestId("sudoku-page-label")).toHaveText("1 / 2")
  // Givens of puzzle 1 appear in answer 1 at the same cells.
  const preview = page.getByTestId("sudoku-preview")
  const givens = await preview.locator(".sudoku-print-board").first().locator(".sudoku-print-cell").allTextContents()
  await page.getByTestId("sudoku-page-next").click()
  await expect(preview.locator(".sudoku-answer")).toHaveCount(6)
  await expect(preview.locator(".sudoku-answer__label").first()).toHaveText("1·1")
  const solved = await preview.locator(".sudoku-answer").first().locator(".sudoku-print-cell").allTextContents()
  givens.forEach((g, i) => { if (g.trim()) expect(solved[i]).toBe(g) })
  expect(solved.every(t => t.trim() !== "")).toBe(true)
  // On the answer page, answers off: back to "1 / 1", a puzzle page shows.
  await page.getByTestId("sudoku-answers").click()
  await expect(preview.locator(".sudoku-print-board--4")).toHaveCount(6)
})

test("pages: 3 pages with answers print 3 puzzle pages and 1 answer page", async ({ page }) => {
  await page.goto("/activity/sudoku/print")
  await page.getByTestId("sudoku-answers").click()
  await stepperPlus(page, "sudoku-pages").click()
  await stepperPlus(page, "sudoku-pages").click()
  await expect(page.getByTestId("sudoku-page-label")).toHaveText("1 / 4")
  await startPrinting(page)
  await expect(page.locator(".print-only .sudoku-print-sheet")).toHaveCount(3)
  await expect(page.locator(".print-only .sudoku-answer")).toHaveCount(18)
})
```

- [ ] **Step 2: Run** `sbt -batch fastLinkJS && npx playwright test e2e/sudoku.spec.ts`. Expected: the new print tests fail (no `sudoku-preview`); the play tests pass.

- [ ] **Step 3: Implement** the Produces list and the decisions above. Strings (en): size "Size", symbols "Symbols", symbolsShapes "Shapes", symbolsDigits "Digits", level "Difficulty", levelEasy "Easy", levelMedium "Medium", levelHard "Hard", perPage "Puzzles per page", answers "Answers", answersTitle "Answers"; translate for pl, es, fr, de. Preset names now use the strings.

- [ ] **Step 4: Run** `sbt -batch test && sbt -batch fastLinkJS && npx playwright test e2e/sudoku.spec.ts e2e/maze.spec.ts e2e/seek-and-find.spec.ts e2e/pictures.spec.ts`. Expected: all pass (Maze and Seek & Find unchanged).

- [ ] **Step 5: Commit** `sudoku: print page on the sheet builder, with answers`.

---

### Task 5: Workbook row with settings; old rows migrate

**Files:**
- Modify: `src/main/scala/tandu/activities/SudokuSheet.scala` (workbook hooks)
- Modify: `src/main/scala/tandu/workbook/Workbook.scala` (Sudoku group, `legacyRows`, presets)
- Test: `e2e/workbook.spec.ts`, `src/test/scala/tandu/activities/SudokuSheetSpec.scala`

**Interfaces:**
- Consumes: Task 3 and 4 (`sheetSpec`, `sheetPresets`, `classicToken`, codec).
- Produces (in `SudokuSheet`):
  - `def workbookPages(token: String, n: Int, rng: Random): Seq[HtmlElement] = SheetBuilder.workbookPages(sheetSpec, token, n, rng)` (answer pages follow when answers are on)
  - `def settingsEditor(token: Var[String]): HtmlElement = SheetBuilder.embedded(sheetSpec, token)`
  - `def tokenSummary(token: String): Strings => String` — the preset name, else `"<n>×<n> · <level name>"` (for example "9×9 · Hard").
- Produces (in `Workbook`):
  - Group `sudoku` has one `Source("sudoku", _.sudoku.name, _.offline.sudoku.printTitle, ctx => SudokuSheet.workbookPages(SudokuSheet.presetToken("easy"), 1, ctx.rng).head, settings = Some(Settings(default = SudokuSheet.presetToken("easy"), summary = SudokuSheet.tokenSummary, title = _ => _.offline.sudoku.printTitle, pages = SudokuSheet.workbookPages, editor = SudokuSheet.settingsEditor)))`.
  - `legacyRows` adds `sudoku-emoji4` → `("sudoku", presetToken("easy"))`, `sudoku-emoji6` → `("sudoku", presetToken("medium"))`, `sudoku-easy|medium|hard` → `("sudoku", classicToken(level))`.
  - `private def sudokuRow(token: String): Row = Row("sudoku", 1, Some(token))`; book presets replace every `Row("sudoku-…", 1)` with the same mapping (age 4–5 `presetToken("easy")`, age 6–7 and travel and math boost `presetToken("medium")`, age 8 `classicToken(Medium)` and `classicToken(Hard)`, math boost `classicToken(Easy)`).

- [ ] **Step 1: Write the failing tests.**

Unit (`SudokuSheetSpec`):

```scala
test("summary: preset name, else size and level") {
  val _ = assert(tokenSummary(presetToken("hard"))(Strings.en) == "Hard")
  assert(tokenSummary(classicToken(Level.Hard))(Strings.en) == "9×9 · Hard")
}
```

(`Strings.en` is the English strings value.)

e2e (`e2e/workbook.spec.ts`): add `const sudokuRows = (page) => kindRows(page, "Sudoku")`.

```ts
test("a Sudoku row has its own settings form; they save and print", async ({ page }) => {
  await createBook(page) // 4–5 preset: one Easy Sudoku row
  const row = sudokuRows(page)
  await expect(row.getByTestId("wbk-row-summary")).toHaveText("Easy")
  await expect(row.locator(".wbk-variant-select")).toHaveCount(0)
  await row.getByTestId("wbk-row-summary").click()
  await row.getByTestId("sudoku-size-9").click()
  await expect(row.getByTestId("wbk-row-summary")).toHaveText("9×9 · Easy")
  await row.getByTestId("sudoku-answers").click()
  await page.reload()
  await expect(sudokuRows(page).getByTestId("wbk-row-summary")).toHaveText("9×9 · Easy")
  await page.locator(".wbk-print-btn").click()
  await expect.poll(async () => page.evaluate(() => (window as any).__printCalls)).toBeGreaterThan(0)
  await expect(page.locator(".print-only .sudoku-answers")).toHaveCount(1)
})

test("books saved with the old Sudoku variants keep their sheets", async ({ page }) => {
  // localStorage book with rows sudoku-hard and sudoku-emoji4, as the old-maze test does
  await expect(page.getByTestId("wbk-row-summary")).toHaveText(["9×9 · Hard", "Easy"])
  // print: first sheet has 6 9×9 boards, second 6 4×4 boards
})
```

Update the existing tests that used the Sudoku variant select: "adding pages is by kind…" switches the variant on the Word Search row instead (it has variants), and asserts the Sudoku row opens with `.sudoku-print-board--4` in its preview; "print assembles cover plus one sheet per page" keeps `toHaveCount(12)` (age 8: two 9×9 rows at 6 per page).

- [ ] **Step 2: Run** `sbt -batch "testOnly tandu.activities.SudokuSheetSpec" && sbt -batch fastLinkJS && npx playwright test e2e/workbook.spec.ts`. Expected: the new tests fail (summary is the old variant name, the row has a variant select).

- [ ] **Step 3: Implement** the Produces lists above. Remove the temporary per-variant workbook wiring from Task 4.

- [ ] **Step 4: Run the whole suite:** `sbt -batch test && sbt -batch fastLinkJS && npx playwright test`. Expected: all pass.

- [ ] **Step 5: Update the local `PRINTING.md`** (git-excluded, not committed): Sudoku marked done, with the difficulty table from Task 2 and lessons.

- [ ] **Step 6: Commit** `sudoku: workbook row with settings; old rows migrate`.

---

## Revision 1 (spec "Revision 1")

### Task 6: Level = techniques + givens; fallback to the hardest reachable technique

**Files:** `SudokuLogic.scala`, `SudokuLogicSpec.scala`, `SudokuDifficultySpec.scala`

**Interfaces:**
- Produces: `def givensTarget(size: GridSize, level: Level): Int` (Hard: 0); `def effective(size: GridSize, level: Level): Level` (replaces `outOfReach`: 4×4 → Easy; 6×6 Hard → Medium; else the level); `Rating.meanOpenMoves: Double` (mean, over steps, of the cells that the level's single techniques can fill before the step).
- `generate(size, level, rng)`: dig with `effective` techniques, never below `givensTarget`; the "needs" check uses `effective(size, level).below`; `MaxTries` tries unless `effective != level` and the target is 0 (then one try).

- [ ] Tests first (`SudokuDifficultySpec`): for every size, Easy > Medium > Hard in mean givens and in mean open moves (strict); 6×6 Hard puzzles are not solvable at Easy (no regression to Easy); 9×9 need-rate test stays; time budgets stay. `SudokuLogicSpec`: generated givens never below the target; `effective` table.
- [ ] Run → fail. Implement. Run → pass; tune the targets only by measurement (ruling with numbers).
- [ ] Commit `sudoku: level is techniques plus givens; fallback to the hardest reachable technique`.

### Task 7: Mixed symbols (chips, one kind per page)

**Files:** `SudokuSheet.scala`, `SheetBuilder.scala` (`chip` gets `isDisabled: Signal[Boolean] = Val(false)`; the `pills` `isDisabled` parameter goes, no other user), `SudokuSheetSpec.scala`, `e2e/sudoku.spec.ts`, `e2e/workbook.spec.ts`

**Interfaces:**
- `SheetConfig.symbols: Set[Symbols]`; `withSymbol(s)` toggles and keeps the last one; `withSize(G9)` sets `Set(Digits)`.
- `SheetPage.Puzzles(symbols: Symbols, puzzles: Vector[Puzzle])`; `SheetPage.Answers(entries: Vector[AnswerEntry])` with `final case class AnswerEntry(label: String, symbols: Symbols, puzzle: Puzzle)`.
- Page kinds from `SheetBuilder.rounds(Symbols.values.filter(c.symbols), max(n, MaxPages), new Random(seed))`, so page k stays when pages are added.
- Codec: one token, kinds joined by `-` in enum order (`shapes`, `digits`, `shapes-digits`); a token of only known kinds sets the set; others are ignored.

- [ ] Tests first: codec (old single tokens, `shapes-digits`, `digits-shapes`, `g9.shapes-digits` → digits), last chip stays, pages alternate in rounds with both on, answers carry their page's symbols; e2e: both chips on → pages 1 and 2 differ in kind (legend on one, not the other), 9×9 disables the Shapes chip.
- [ ] Run → fail. Implement. Run unit + `e2e/sudoku.spec.ts e2e/workbook.spec.ts e2e/maze.spec.ts e2e/seek-and-find.spec.ts` → pass.
- [ ] Commit `sudoku: shapes and digits can be mixed, one kind per page`.
