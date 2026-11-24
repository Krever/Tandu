# Sudoku print sheet: builder with measured difficulty

Date: 2026-11-24 · Size: project

## Intent

Third activity on the shared sheet builder (`ui/SheetBuilder.scala`; see
Seek & Find and Maze, local `PRINTING.md`). The parent sets up a Sudoku
sheet in a form, sees it in a live preview and prints it; the workbook
uses the same form per row.

The user wants **difficulty that is real**, not a count of givens (lesson
from the maze work): a puzzle's level is the set of solving techniques it
needs, measured by a solver.

## Goals

- Print page on `SheetBuilder`: presets, form, live preview, pages,
  recipe in the URL, Print. Form (agreed):
  - Grid size: 4×4, 6×6, 9×9 (one size per sheet).
  - Symbols: Shapes (● ▲ ■ ✚ ✕ ◆) or Digits.
  - Difficulty: Easy, Medium, Hard (by technique, below).
  - Puzzles per page: 1, 2, 4, 6.
  - Answers: a switch, **off by default**. When on, the sheet ends with
    answer page(s): the solved grids of all puzzles on the sheet.
  - Pages (the builder's stepper).
- Workbook: Sudoku is one row kind with settings, edited with the same
  form. Old rows `sudoku-emoji4|emoji6|easy|medium|hard` keep working.
- A statistical test (like `MazeDifficultySpec`) that keeps every
  generated puzzle at its level.

## Non-goals

- Changes to the in-app (playable) Sudoku, which keeps its variants and
  its generator.
- Several grid sizes on one sheet.
- Emoji symbols on paper (a child cannot draw an emoji).
- Guessing-level ("diabolical") puzzles.

## Anti-goals

- An "Easy" puzzle that needs a technique beyond "only one symbol fits
  this cell".
- Any puzzle with more than one solution, or one that needs guessing.
- A preview that freezes while it makes Hard 9×9 pages.

## Approaches

- **A. Dig holes at the level — picked.** From a full random grid, remove
  givens in random order; keep a removal only if the puzzle is still
  solvable by the rating solver with the level's techniques. Then check
  that the puzzle really needs the level (not solvable with the level
  below); if not, try again (bounded). Easy is easy by construction, and
  the puzzle has the fewest givens the level allows.
- B. Generate as today and reject puzzles of the wrong level. Rejected:
  Hard-without-guessing puzzles are rare, so many retries and a slow
  preview.
- C. Use the rating only offline to tune give counts. Rejected: no
  guarantee per puzzle.

## Decided

### Levels (techniques)

The rating solver works on candidates (pencil marks) and applies, in a
loop, only the techniques of the level:

- **Easy:** naked single — a cell with only one candidate.
- **Medium:** Easy + hidden single — a symbol with only one place left
  in a row, column or box.
- **Hard:** Medium + naked pairs and locked candidates (pointing and
  box-line). Never guessing.

A puzzle is *solvable at level L* when this loop fills every cell. A
puzzle solved by logic alone has exactly one solution, so the old
uniqueness check (`countSolutions`) is not needed for the new generator
(keep it for the in-app one).

A puzzle *needs* level L when it is solvable at L and not at the level
below. Generation for Medium/Hard retries until the puzzle needs the
level; after a bounded number of tries it keeps the last puzzle (solvable
at the level, so never harder than asked). On 4×4 (and maybe 6×6),
Medium/Hard may not be reachable; that fallback then applies. The form
does not hide levels.

### Recipe, presets, URL

- `SheetConfig(size: GridSize, symbols: Symbols, level: Level, perPage:
  PerPage, answers: Boolean)`. `GridSize` carries size and box shape
  (4: 2×2, 6: 2×3, 9: 3×3). **9×9 is digits only**: choosing 9×9 sets
  Digits, and the Shapes pill is disabled for 9×9; the decoder also
  forces Digits for 9×9.
- Presets: **Easy** = 4×4, shapes, Easy, 6 per page. **Medium** = 6×6,
  shapes, Medium, 6 per page. **Hard** = 9×9, digits, Hard, 4 per page.
  Answers off in all.
- URL token: tolerant, like Maze (for example `g9.digits.hard.pp4` plus
  `.ans` when answers are on). Missing or bad tokens keep the first
  preset's values.
- Row summary in the workbook: the preset name, otherwise a short
  description of the recipe ("9×9 · Easy"), not "Custom" (Sudoku's own
  summary function).

### Workbook migration

`Workbook.Codec.legacyRows` gets: `sudoku-emoji4` → Easy preset token;
`sudoku-emoji6` → Medium preset token; `sudoku-easy` / `sudoku-medium` /
`sudoku-hard` → 9×9, digits, that level, 6 per page (today's sheets have
6 per page). Book presets use the new row.

### Sheet layout

- One page body: a grid of `perPage` puzzle slots; each slot has a legend
  (shapes only) and the board. Board sizes in mm per per-page value and
  grid size are left to the builder, with the 6-per-page boards as today
  (75 mm for 9×9; 65 mm plus legend for 4×4 / 6×6); bigger boards and
  symbols for 1, 2, 4 per page.
- Answers: after the puzzle pages, page(s) titled "Answers" with small
  solved grids, each labelled with its page and puzzle number (for
  example "2·3"). Answers are compact, much smaller than the puzzles
  (user): about 25 (9×9), 36 (6×6) or 56 (4×4) per page. Givens and filled cells are visually different (for
  example givens bold, answers light). The pager in the preview shows the
  answer page(s) as the last page(s); the pages stepper counts puzzle
  pages only. In a workbook row with answers on, the answer page(s) follow
  the row's pages.

### Tests

- Unit: the rating solver on hand-made puzzles (one per level, plus one
  that needs guessing → not solvable at Hard); for each size × level,
  generated puzzles are solvable at the level and have one solution
  (cross-check with `countSolutions`); for 9×9, Medium and Hard puzzles
  need their level in at least 90% of cases; Easy < Medium < Hard in
  givens and in technique steps; codec and migration.
- e2e: presets and form, 9×9 disables Shapes, URL round trip, answers
  switch adds the answer page and its grids match the puzzles, pages,
  print matches preview, workbook row and legacy rows, no console errors.
- Existing `e2e/sudoku.spec.ts` print test is rewritten for the builder.

## Constraints

- Scala.js; `-Werror`; comments in ASD-STE100.
- Strings in all 5 languages.
- Generation time (decided after the builder check, my call): the
  preview makes **only the page it shows**. Each puzzle page has its own
  seed (from the sheet seed and the page index), so page k does not need
  pages before or after it. Sudoku's `pagesOf` returns a lazy, cached
  `IndexedSeq` (pages are made when read, once per recipe + seed + page);
  `SheetBuilder.Spec.pagesOf` widens its result type from `Vector[P]` to
  `IndexedSeq[P]` (Seek & Find and Maze keep returning a `Vector`, no
  behaviour change). Printing reads all pages (a short wait at print time
  is acceptable). Budget: one Hard 9×9 page (4 puzzles) in at most about
  300 ms, measured by a unit test that states the number.
- e2e needs `sbt fastLinkJS` first.

## Left to the builder

- Solver data structures and the exact retry bound.
- Board and font sizes per per-page value; answer page layout and
  labels.
- URL token spelling.
- Whether the shared `SheetBuilder.Spec` needs a small extension for
  answer pages (for example `pagesOf` returning extra pages), as long as
  Seek & Find and Maze do not change behaviour.

## Revision 1 (2026-11-24, after trying the branch)

User feedback: the difficulty pill does nothing on 4×4, and on 6×6 Hard
is barely different from Easy; the symbols could be mixed.

Measured cause: every 4×4 puzzle is solvable with naked singles, even at
the fewest givens (0/20 need Medium or Hard). 6×6 cannot reach Hard; the
fallback kept whatever the dig gave, so 6×6 Hard (advanced steps 1.4)
was easier than Medium (3.8), which retries until it needs Medium.

Decided (user picked "technique + givens" and "chips, one per page"):

- **Level = techniques + givens.** Each level sets the techniques allowed
  and a givens target: digging stops at the target (Hard: no target, dig
  to the minimum). Start targets, tuned by measurement:

  |     | Easy | Medium | Hard |
  |---|---|---|---|
  | 4×4 | 9 | 7 | minimum |
  | 6×6 | 20 | 13 | minimum |
  | 9×9 | 36 | 30 | minimum |

- **Fallback to the hardest reachable technique.** A level whose
  techniques a grid does not reach (4×4 Medium/Hard, 6×6 Hard) uses the
  hardest level that it reaches (4×4: Easy; 6×6: Medium), for the dig
  and for the "needs the level" check. The givens target still makes it
  differ. So on average Hard is harder than Medium on every grid (for
  one seed it can be easier). If a puzzle does not need its techniques
  at the givens target, digging goes on until it does (final review:
  6×6 Medium was slow and a quarter did not need Medium).
- **Measured, per size:** Easy > Medium > Hard in mean givens, and
  Easy < Medium < Hard in mean search effort: at each step, empty cells
  divided by open moves (cells where one symbol fits, or the only place
  of a symbol in a unit); about how many cells a solver looks at to find
  a move. (Changed from a raw count of open moves while building: that
  count falls with fewer empty cells, so it ranked Easy as hardest.) 9×9 Medium and Hard
  still need their technique level in at least 90% of cases.
- **Mixed symbols:** Symbols become multi-select chips (Shapes, Digits),
  like Maze shapes. With both on, the puzzles alternate on the page and
  from page to page (user, after trying "one kind per page": a one-page
  sheet showed no change). On a mixed page a digit puzzle has a blank
  legend line, so the boards are in line. The
  last chip cannot be turned off. 9×9 is digits only (Shapes chip
  disabled). URL: `shapes`, `digits`, `shapes-digits`; old tokens keep
  working. Presets: Easy and Medium shapes, Hard digits (as before).
