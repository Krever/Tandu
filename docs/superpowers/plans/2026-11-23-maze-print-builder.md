# Maze Print Builder Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move the generic sheet-builder parts out of Seek & Find into a shared helper, then give Maze a builder print page (presets, form, live preview, pages, URL) and one workbook row kind with settings.

**Architecture:** New `ui/SheetBuilder.scala`, generic over a recipe type `C` and a page model `P`, owns preview, pager, pages stepper, URL sync, print mounting, preset pills and form blocks. Seek & Find and Maze each supply a recipe case class, a codec, a page generator, a sheet body and form rows. The workbook keeps its existing `Settings` hook.

**Tech Stack:** Scala 3.8 / Scala.js, Laminar 17, Waypoint, scalatest (unit, `sbt test`), Playwright (e2e, `npx playwright test`).

**Spec:** `docs/superpowers/specs/2026-11-23-maze-print-builder-design.md`

## Global Constraints

- Seek & Find must not change visibly; `e2e/seek-and-find.spec.ts` and `e2e/pictures.spec.ts` pass **unchanged**.
- Old workbook rows `maze-easy` / `maze-medium` / `maze-hard` keep their sheets.
- Start stays 🐭 top-left, goal 🧀 bottom-right. The in-app maze does not change.
- Every new string in all five languages in `Strings.scala` (en, pl, es, fr, de).
- Comments and docstrings in ASD-STE100.
- The working tree has uncommitted picture-font / B&W work in the same files (`Maze.scala`, `SeekAndFind.scala`, `Printable.scala`, `Strings.scala`, `styles.css`, `WorkbookPage.scala`). Build on top of it; never revert it. Keep `Pictures.bwToggle()` next to the Print button.
- **No commits** unless the user asks: per-task commits would mix in the unrelated uncommitted work. Each task ends with a checkpoint instead.
- Scala compile check: `sbt compile`. The dev server (`npm run dev`) is started by Playwright.

## Review Focus

1. Size at its max for 1 per page (30), then 4 per page → size clamps to 16; the `+` is disabled. (Task 3 test.)
2. A hand-edited URL: `pp3`, `s99`, `s99999999999`, unknown tokens → valid recipe, page renders. (Tasks 2 and 3 tests.)
3. 3 pages, view page 3, set pages to 1 → pager shows `1 / 1`, the preview is not empty. (Task 1: helper `setPages`; covered by the existing S&F pages test, and Task 3 test.)
4. A book preset replaces rows while a maze row form is open → the form shows the new settings. (Task 4 test.)
5. An old book with `maze-hard` → row summary "Hard", prints one 24-wide maze per page. (Task 4 test.)

---

### Task 1: Shared `SheetBuilder` helper; Seek & Find moves onto it

**Files:**
- Create: `src/main/scala/tandu/ui/SheetBuilder.scala`
- Modify: `src/main/scala/tandu/activities/SeekAndFind.scala` (sections "recipe in the URL", "workbook", `renderOffline`, "sheet builder form")
- Modify: `src/main/scala/tandu/i18n/Strings.scala` (`Printable`, `SeekAndFindForm`, five languages)
- Modify: `styles.css` (builder rules at ~3736–3783)
- Test: `e2e/seek-and-find.spec.ts`, `e2e/workbook.spec.ts`, `e2e/pictures.spec.ts` (unchanged)

**Interfaces:**
- Produces (all in `object SheetBuilder`):
  - `val MaxPages = 10`
  - `final case class Preset[C](presetId: String, name: Strings => String, config: C)`
  - `def clamp(v: Int, lo: Int, hi: Int): Int`
  - `def named[E](values: Array[E], t: String): Option[E]` — enum case by lower-case name
  - `def encodePages(n: Int): String` — `""` for 1, else `.pN`
  - `def decodePages(token: String): Int` — first `pN` token whose rest is an Int, clamped 1..MaxPages, default 1
  - `final case class Spec[C, P](testIdPrefix: String, presets: List[Preset[C]], encode: C => String, decode: String => C, title: C => Strings => String, pagesOf: (C, Int, Long) => Vector[P], body: (C, P) => HtmlElement, form: Var[C] => HtmlElement, newLabel: Strings => String)` — `pagesOf(c, n, seed)` must make page k independent of pages after it
  - `def printPage[C, P](spec: Spec[C, P], activityId: String, intro: HtmlElement): HtmlElement` — the whole print mode: `intro` (rules card), preset pills, preview + pager + pages stepper + bw toggle + new/print buttons, form, print-only copies, URL sync to `Page.Activity(activityId, List("print", encode(c) + encodePages(n)))` via `Routing.replace`
  - `def embedded[C, P](spec: Spec[C, P], token: Var[String]): HtmlElement` — workbook row editor: `token.zoomLazy(spec.decode)((_, c) => spec.encode(c))`, preset pills, form, preview of one page with "new" button
  - `def workbookPages[C, P](spec: Spec[C, P], token: String, n: Int, rng: Random): Seq[HtmlElement]` — bodies only (no `Printable.render`)
  - `def summary[C](spec: Spec[C, ?], token: String): Strings => String` — preset name, else `_.printable.custom`
  - Form blocks: `def form(rows: Modifier[HtmlElement]*): HtmlElement`, `def formRow(label: Strings => String, control: HtmlElement): HtmlElement`, `def pills[A](testId: String, current: Signal[A], options: List[(A, String, Signal[String])], pick: A => Unit): HtmlElement`, `def chip(testId: String, on: Signal[Boolean], label: Signal[String], tap: () => Unit): HtmlElement`, `def fieldStepper[C](config: Var[C], testId: String, get: C => Int, lo: C => Int, hi: C => Int, set: (C, Int) => C): HtmlElement`, `def flag[C](config: Var[C], testId: String, label: Strings => String, get: C => Boolean, flip: C => C): HtmlElement`
  - Test ids, from `testIdPrefix` `x`: `x-preview`, `x-preset-<id>`, `x-pages`, `x-page-prev`, `x-page-next`, `x-page-label`, `x-print`. (Seek & Find: `snf-*`, as today.)
- Produces (strings): `Strings.Printable` gets `startFrom`, `pages`, `custom`; `SeekAndFindForm` loses `preset`, `pages`, `custom` (same texts move over, per language).
- Produces (CSS): the generic builder classes are renamed `snf-builder*`, `snf-preview*`, `snf-pager*`, `snf-presets`, `snf-form*`, `snf-chips`, `snf-chip-btn` → `sb-builder*`, `sb-preview*`, `sb-pager*`, `sb-presets`, `sb-form*`, `sb-chips`, `sb-chip-btn` (also in the `.wbk-preset-chip, .snf-chip-btn` rule). Seek & Find sheet classes (`snf-print*`, `snf-count*`) stay. No e2e test uses the renamed classes.

- [ ] **Step 1: Baseline.** Run `npx playwright test e2e/seek-and-find.spec.ts e2e/workbook.spec.ts e2e/pictures.spec.ts`. Expected: all pass. Note any failure that exists before the change.
- [ ] **Step 2: Create `SheetBuilder.scala`** with the interfaces above, moving the code from `SeekAndFind.scala` (`previewBox`, `newSceneButton`, pager, pages stepper, `printing` Var with `beforeprint`/`afterprint`, `presetPills`, `formRow`, `pills`, `chip`, `fieldStepper`, `flag`, `clamp`, `named`, `encodePages`, `decodePages`, the `settingsEditor` body). Keep the slider drag logic in Seek & Find (only it has a slider). `printPage` reads the initial token from `Routing.router.currentPageSignal.now()` matching `Page.Activity(activityId, "print" :: token :: _)`.
- [ ] **Step 3: Move Seek & Find onto it.** `SeekAndFind` defines `private val sheetSpec: SheetBuilder.Spec[SheetConfig, Puzzle]` with `testIdPrefix = "snf"`, `newLabel = _.seekAndFind.newGame`. `renderOffline()` = `SheetBuilder.printPage(sheetSpec, id, rulesCard)`. `settingsEditor`, `workbookPages`, `tokenSummary` delegate to the helper; `tokenSummary` keeps its "Counting" case before the helper's fallback. `SheetPreset` becomes `SheetBuilder.Preset[SheetConfig]`.
- [ ] **Step 4: Strings and CSS** as in Interfaces.
- [ ] **Step 5: Compile.** Run `sbt compile`. Expected: success, no new warnings.
- [ ] **Step 6: Run the e2e tests from Step 1.** Expected: the same results as the baseline, with no edit to any spec file.
- [ ] **Step 7: Check.** `npm run dev`, open `/activity/seek-and-find/print` and a workbook Seek & Find row; the layout looks the same as before (desktop and 390 px wide).
- [ ] **Step 8: Checkpoint** — report to the user; no commit.

### Task 2: Maze recipe, codec and shape (pure)

**Files:**
- Modify: `src/main/scala/tandu/activities/Maze.scala` (new section "printable sheet recipe" after "variants")
- Test: `src/test/scala/tandu/activities/MazeSpec.scala` (create)

**Interfaces:**
- Consumes: `SheetBuilder.Preset`, `clamp`, `named`.
- Produces (public, in `object Maze`):
  - `enum Loops(val braid: Double): case Off extends Loops(0.0); case Few extends Loops(0.3); case Many extends Loops(0.6)`
  - `final case class SheetConfig(size: Int, loops: Loops, perPage: Int)`
  - `object SheetConfig`: `val PerPage = List(1, 2, 4)`, `val MinSize = 5`, `def maxSize(perPage: Int): Int` (1 → 30, 2 → 20, 4 → 16), `def slotMm(perPage: Int): (Double, Double)` (w, h: 1 → (190, 230), 2 → (190, 112), 4 → (92, 112))
  - `extension (c: SheetConfig) def dims: (Int, Int)` — (rows, cols). Short side = `size`; long side = `floor(size × long/short)` of the slot. Tall slots (1, 4): cols = size. Wide slot (2): rows = size.
  - `val sheetPresets: List[SheetBuilder.Preset[SheetConfig]]` — easy (10, Few, 4), medium (16, Off, 4), hard (24, Off, 1); names `_.maze.easy.name` etc.
  - `def encodeConfig(c: SheetConfig): String` — `s<size>.<loops>.pp<perPage>`, e.g. `s10.few.pp4`
  - `def decodeConfig(token: String): SheetConfig` — fold over `.`-split tokens from the first preset; `s\d+` and `pp\d+` via `toIntOption` (too long → ignored); `pp` not in `PerPage` → ignored; loops by `named`; others ignored; then size clamped to `MinSize..maxSize(perPage)`.
  - `def presetToken(presetId: String): String`

- [ ] **Step 1: Write the failing tests** in `MazeSpec` (`AnyFunSuite`, like `SudokuSpec`):
  - `"presets have today's shapes"`: `dims` of easy = (12, 10), medium = (19, 16), hard = (29, 24).
  - `"two per page is wide"`: `SheetConfig(10, Loops.Off, 2).dims == (10, 16)`.
  - `"cells stay at least 5.5 mm at every max size"`: for each `pp` in `PerPage`, at `maxSize(pp)`, `min(slotW/cols, slotH/rows) >= 5.5`.
  - `"codec round trip"`: for each preset and `SheetConfig(7, Loops.Many, 2)`, `decodeConfig(encodeConfig(c)) == c`; `encodeConfig(easy) == "s10.few.pp4"`.
  - `"bad tokens are clamped or ignored"`: `decodeConfig("s99.pp4.bogus").size == 16`; `decodeConfig("pp3").perPage == 4`; `decodeConfig("s99999999999") == easy`; `decodeConfig("s2.pp1").size == 5`; `decodeConfig("s10.few.pp4.p3") == easy` (the page token is not part of the recipe).
- [ ] **Step 2: Run** `sbt "testOnly tandu.activities.MazeSpec"`. Expected: FAIL (does not compile: `SheetConfig` not found).
- [ ] **Step 3: Implement** the interfaces above in `Maze.scala`.
- [ ] **Step 4: Run** `sbt "testOnly tandu.activities.MazeSpec"`. Expected: all pass.
- [ ] **Step 5: Checkpoint** — no commit.

### Task 3: Maze print page on the builder

**Files:**
- Modify: `src/main/scala/tandu/activities/Maze.scala` (section "print": `renderOffline`, `printSheetBody`, `printableMaze`; new form)
- Modify: `src/main/scala/tandu/i18n/Strings.scala` (new `MazeForm` in `MazeOff`, five languages)
- Modify: `styles.css` (maze print rules at ~2269–2284)
- Test: `e2e/maze.spec.ts` (create)

**Interfaces:**
- Consumes: Task 1 `SheetBuilder.Spec`, `printPage`, form blocks; Task 2 recipe and codec.
- Produces:
  - `def sheetGrids(c: SheetConfig, n: Int, seed: Long): Vector[Vector[Grid]]` — n pages × `perPage` grids from one `Random(seed)`, in page order, using `generate(rows, cols, c.loops.braid, rng)`.
  - `private val sheetSpec: SheetBuilder.Spec[SheetConfig, Vector[Grid]]` — `testIdPrefix = "maze"`, `title = _ => _.offline.maze.printTitle`, `newLabel = _.maze.newGame`.
  - `def sheetBody(c: SheetConfig, grids: Vector[Grid]): HtmlElement` — `div.maze-print-sheet.maze-print-sheet--pp<n>`; each board `div.maze-print-board` with inline `width`/`height` in mm: `cell = min(slotW/cols, slotH/rows)`, size `cols × cell` by `rows × cell`. Remove the per-variant `maze-print-board--easy|medium|hard` rules.
  - `renderOffline()` = `SheetBuilder.printPage(sheetSpec, id, rulesCard)`.
  - Form test ids: `maze-size` (stepper, `MinSize..maxSize(perPage)`), `maze-loops-off|few|many` (pills), `maze-per-page-1|2|4` (pills; a pick clamps size to the new max).
  - Strings: `Strings.MazeForm(size: String, loops: String, loopsOff: String, loopsFew: String, loopsMany: String, perPage: String)` at `offline.maze.form`. English: "Size", "Loops", "None", "A few", "Many", "Mazes per page".
  - CSS: `--pp1` and `--pp2` one column, `--pp4` two columns; 6 mm gap as today.
  - `printSheetBody(v: Variant, rng)` is removed in Task 4 (the workbook still uses it until then).

- [ ] **Step 1: Write the failing e2e tests** in `e2e/maze.spec.ts` (helpers as in `seek-and-find.spec.ts`: `stepperPlus`, `startPrinting`; `window.print = () => {}`):
  - `"print mode: presets fill the form, the preview follows, print matches it"`: at `/activity/maze/print`, `maze-preset-easy` is active; preview has 4 `.maze-print-board`; first board's svg `viewBox` is `"0 0 100 120"`. `startPrinting` → `.print-only .maze-print-board` count 4, and the wall lines of the first printed board equal those of the first preview board (compare `line` x1/y1/x2/y2 joined). Click `maze-per-page-2` → 2 boards, viewBox `"0 0 160 100"`, easy preset no longer active. Click `maze-preset-hard` → 1 board, viewBox `"0 0 240 290"`.
  - `"print mode: size is limited by mazes per page"`: hard preset (1 per page), press `maze-size` + until disabled → shows `30`; click `maze-per-page-4` → `maze-size` shows `16` and its `+` is disabled.
  - `"print mode: the recipe is kept in the URL"`: medium preset, `maze-loops-many` → URL ends `/print/s16.many.pp4`. Reload → `maze-loops-many` active. `/activity/maze/print/s99.pp3.bogus` → `pp3` is ignored (per page stays 4 from the easy preset), `maze-size` shows `16`, preview has 4 boards. `/activity/maze/print/s99999999999` → `maze-size` shows `10`, preview visible.
  - `"print mode: several pages, each with new mazes"`: `maze-pages` + twice → `maze-page-label` `1 / 3`; `maze-page-next` → `2 / 3` and its first board differs from page 1's; URL ends `.p3`; `maze-pages` − twice → label `1 / 1` and preview has boards. `startPrinting` after setting 3 pages → 12 printed boards.
- [ ] **Step 2: Run** `npx playwright test e2e/maze.spec.ts`. Expected: FAIL (no `maze-preview`).
- [ ] **Step 3: Implement** the interfaces above.
- [ ] **Step 4: Run** `sbt compile` then `npx playwright test e2e/maze.spec.ts e2e/seek-and-find.spec.ts e2e/pictures.spec.ts`. Expected: all pass.
- [ ] **Step 5: Check** in the browser: `/activity/maze/print`, all three presets and 2 per page; open the browser print preview (Ctrl+P) once and check that the mazes fill the page and do not run onto a second sheet.
- [ ] **Step 6: Checkpoint** — no commit.

### Task 4: Maze in the workbook

**Files:**
- Modify: `src/main/scala/tandu/workbook/Workbook.scala` (maze `Group` ~133, `Codec.legacySeekRows`/`migrate` ~319, presets ~245–300)
- Modify: `src/main/scala/tandu/activities/Maze.scala` (public workbook API; remove `printSheetBody(v, rng)`)
- Test: `e2e/workbook.spec.ts`

**Interfaces:**
- Consumes: Task 1 `embedded`, `workbookPages`, `summary`; Task 2 `presetToken`, `encodeConfig`.
- Produces in `Maze`: `def workbookPages(token: String, n: Int, rng: Random): Seq[HtmlElement]`, `def settingsEditor(token: Var[String]): HtmlElement`, `def tokenSummary(token: String): Strings => String`.
- In `Workbook`: group `maze` has one `Source("maze", _.maze.name, _.offline.maze.printTitle, ctx => Maze.workbookPages(Maze.presetToken("easy"), 1, ctx.rng).head, settings = Some(Settings(default = Maze.presetToken("easy"), summary = Maze.tokenSummary, title = _ => _.offline.maze.printTitle, pages = Maze.workbookPages, editor = Maze.settingsEditor)))`.
- `Codec.migrate` also maps `maze-easy|medium|hard` → `("maze", Some(Maze.presetToken(id)))`. Rename `legacySeekRows` → `legacyRows` (one map for both).
- Presets: `mazeRow(token: String): Row = Row("maze", 1, Some(token))`; replace each `Row("maze-<id>", 1)` with `mazeRow(Maze.presetToken("<id>"))`.

- [ ] **Step 1: Update existing tests that count all summary rows.** In `"a Seek & Find row has its own settings form…"` and `"a book preset replacing the rows…"`, limit the row locator to Seek & Find rows (filter by the Seek & Find kind name on the row), because the 4–5 preset also has a maze row with a summary now. In `"books saved with the old Seek & Find variants keep their sheets"`, expect summaries `["Medium", "Counting", "Easy"]`.
- [ ] **Step 2: Write the failing tests:**
  - `"a Maze row has its own settings form; they save and print"`: `createBook` (4–5), maze row summary `Easy`; open it, click `maze-per-page-2` → summary `Custom`, the row's `maze-preview` has 2 boards; reload → still `Custom`; row count + once, print → `.print-only .maze-print-sheet--pp2` count 2.
  - `"books saved with the old maze variants keep their sheets"`: stored rows `maze-hard` c 1 and `maze-medium` c 1 → summaries `["Hard", "Medium"]`; print → first maze sheet has 1 board with viewBox `"0 0 240 290"`.
  - `"a book preset replacing the rows refreshes an open Maze form"`: open the maze row form, click `maze-loops-many` → `Custom`; `pickPreset(page, "4–5")` → summary `Easy` and `maze-loops-few` active; click `maze-per-page-1` → stored `p` of the maze row matches `/^s\d+\.few\.pp1$/`.
- [ ] **Step 3: Run** `npx playwright test e2e/workbook.spec.ts`. Expected: the new tests FAIL; the edited ones FAIL only on the maze summary.
- [ ] **Step 4: Implement** the interfaces above.
- [ ] **Step 5: Run the full suite** `sbt test` and `npx playwright test e2e/maze.spec.ts e2e/seek-and-find.spec.ts e2e/pictures.spec.ts e2e/workbook.spec.ts e2e/dot-to-dot.spec.ts e2e/sudoku.spec.ts`. Expected: all pass.
- [ ] **Step 6: Update local `PRINTING.md`**: Maze `[x]`, a log line with the helper API location (`ui/SheetBuilder.scala`) and anything learned for the next activity.
- [ ] **Step 7: Checkpoint** — report to the user and ask how to commit (branch or not, and whether the picture-font work goes first).
