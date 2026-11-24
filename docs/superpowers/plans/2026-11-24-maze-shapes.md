# Maze Shapes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add circle and hex mazes to the Maze print sheet, chosen like Seek & Find themes (multi-select, one shape per page, shuffled rounds).

**Architecture:** A new `MazeGraph` (cells + neighbours) with three layouts (rect, hex, circle) and one `carve` for all styles and loops. `Maze.generate` keeps its `Grid` API on top of the rect layout. The sheet recipe gets `shapes`; pages are `SheetPage(shape, mazes)`; each shape has its own SVG drawing.

**Tech Stack:** Scala 3.8 / Scala.js, Laminar 17, scalatest (`sbt test`), Playwright (`npx playwright test`).

**Spec:** `docs/superpowers/specs/2026-11-24-maze-shapes-design.md`

## Global Constraints

- Branch `maze-print-builder`; commit after each task (the user asked for commits on this branch). End commit messages with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- Square only is encoded with **no shapes token**; old links and stored rows stay byte-identical (`e2e/workbook.spec.ts` pins `s\d+\.winding\.few\.pp1`).
- Presets stay Square only; a preset sets the whole recipe, shapes included.
- Cells at least 5.5 mm (square cell / circle ring width / hex across-flats); hex edge (doorway) at least 4 mm; size limits stay per `PerPage` only.
- Circle rings: `R = max(2, size / 2 - 1)`. Hex short-side cells: `n = max(3, size * 3 / 4)`.
- Walls 0.5 mm with `vector-effect: non-scaling-stroke`; `<path>` walls need `fill: none`.
- The in-app maze and today's `Maze.generate(rows, cols, braid, rng, style): Grid` keep working; existing `MazeSpec` tests pass.
- `e2e/seek-and-find.spec.ts` and `e2e/pictures.spec.ts` pass unchanged.
- Before any e2e run: `sbt -batch fastLinkJS` (Playwright reuses a dev server that links only at start).
- `-Werror`: no unused imports. Comments in ASD-STE100 (no "thus").

## Review Focus

1. Deselecting the last selected shape → it stays selected; the recipe never has no shapes. (Task 4 unit, Task 5 e2e.)
2. Circle at 2 per page (wide slot) → a round board that fits the slot height, centred, not stretched. (Task 5 e2e.)
3. "All" shapes with more pages than shapes → no shape on two pages in a row at a round boundary. (Task 1 unit.)
4. Pick Circle + Hex, then tap a preset → shapes back to Square and the URL has no shapes token. (Task 5 e2e.)
5. Loops on circle and hex → start and goal never get an extra opening; the circle's outer gap is only at the start. (Task 2 unit for loops; Task 5 e2e checks one outer gap.)

---

### Task 1: `SheetBuilder.rounds` (moved from Seek & Find)

**Files:**
- Modify: `src/main/scala/tandu/ui/SheetBuilder.scala` (codec helpers section)
- Modify: `src/main/scala/tandu/activities/SeekAndFind.scala:332-347` (`themeOrder` and its use)
- Test: `src/test/scala/tandu/ui/SheetBuilderSpec.scala` (create)

**Interfaces:**
- Produces: `def rounds[A](ids: Vector[A], n: Int, rng: Random): Vector[A]` in `object SheetBuilder` — the body of `SeekAndFind.themeOrder`, made generic. `themeOrder` is removed; Seek & Find calls `SheetBuilder.rounds(c.themeIds, math.max(n, MaxPages), rng)`.

- [ ] **Step 1: Write the failing tests** in `SheetBuilderSpec` (`AnyFunSuite`):
  - `"rounds: each id once per round"`: for `ids = Vector("a","b","c")`, `n = 9`, seeds 1..20: each slice `grouped(3)` is a permutation of `ids`; result size 9.
  - `"rounds: no id twice in a row at a round boundary"`: same inputs; `result.sliding(2).forall(p => p(0) != p(1))`.
  - `"rounds: one id repeats"`: `rounds(Vector("x"), 4, rng) == Vector.fill(4)("x")`.
- [ ] **Step 2: Run** `sbt -batch "testOnly tandu.ui.SheetBuilderSpec"`. Expected: FAIL (does not compile: `rounds` not found).
- [ ] **Step 3: Implement** `rounds`; switch Seek & Find to it; delete `themeOrder`; update the comment at `SeekAndFind.scala:728` to name `SheetBuilder.rounds`.
- [ ] **Step 4: Run** `sbt -batch test`, then `sbt -batch fastLinkJS` and `npx playwright test e2e/seek-and-find.spec.ts`. Expected: all pass.
- [ ] **Step 5: Commit** `sheet builder: generic rounds helper (from Seek & Find themes)`.

### Task 2: `MazeGraph` with the rect layout; `generate` on top of it

**Files:**
- Create: `src/main/scala/tandu/activities/MazeGraph.scala`
- Modify: `src/main/scala/tandu/activities/Maze.scala:65-177` (generator section: the carving moves out; `generate` stays)
- Test: `src/test/scala/tandu/activities/MazeGraphSpec.scala` (create); `MazeSpec` unchanged

**Interfaces:**
- Produces (`object MazeGraph`):
  - `trait Layout { def cellCount: Int; def neighbours(cell: Int): Vector[Int]; def start: Int; def goal: Int }`
  - `final case class RectLayout(rows: Int, cols: Int) extends Layout` — cell = `r * cols + c`; 4 neighbours; `start = 0`, `goal = cellCount - 1`.
  - `final case class Carved(layout: Layout, passages: Set[(Int, Int)])` — pairs stored as `(min, max)`; `def open(a: Int, b: Int): Boolean`; `def degree(cell: Int): Int`.
  - `def carve(layout: Layout, style: Maze.Style, braid: Double, rng: Random): Carved` — winding / balanced / branchy exactly as `Maze.generate` does today, on `neighbours`; then loops: each dead end (degree 1) that is not `start` or `goal` opens one extra wall to a random non-open neighbour with probability `braid`.
- `Maze.generate(rows, cols, braid, rng, style): Grid` = `carve(RectLayout(rows, cols), …)` converted to `Grid` (`openRight(idx) = open(cell, cell + 1)`, `openDown(idx) = open(cell, cell + cols)`). The local `neighbors` helper stays if the play code uses it.

- [ ] **Step 1: Write the failing tests** in `MazeGraphSpec`, with test helpers `reachable(c: Carved): Int` (BFS from `layout.start` over `open`) and `passages.size`:
  - `"rect neighbours are symmetric and 2 to 4 per cell"` for `RectLayout(5, 7)`.
  - `"every style carves a perfect maze on the rect layout"`: styles × seeds 1..5 on `RectLayout(12, 15)`, braid 0: `reachable == 180`, `passages.size == 179`.
  - `"loops open every dead end except start and goal"`: `RectLayout(12, 15)`, braid 1.0, each style: every cell with `degree == 1` is `start` or `goal`; `reachable == 180`.
- [ ] **Step 2: Run** `sbt -batch "testOnly tandu.activities.MazeGraphSpec"`. Expected: FAIL (does not compile).
- [ ] **Step 3: Implement** `MazeGraph` and move `generate` onto it.
- [ ] **Step 4: Run** `sbt -batch test`. Expected: all pass, including every existing `MazeSpec` test (perfect-maze and dead-end-order tests now run through the graph).
- [ ] **Step 5: Commit** `maze: general cell graph; generate uses the rect layout`.

### Task 3: Hex and circle layouts

**Files:**
- Modify: `src/main/scala/tandu/activities/MazeGraph.scala`
- Test: `src/test/scala/tandu/activities/MazeGraphSpec.scala`

**Interfaces:**
- Produces:
  - `final case class HexLayout(rows: Int, cols: Int) extends Layout` — pointy-top, odd rows shifted half a cell right ("odd-r"); cell = `r * cols + c`; up to 6 neighbours; `start = 0`, `goal = cellCount - 1`.
  - `final case class CircleLayout(rings: Int) extends Layout` — `val ringSizes: Vector[Int]` (index 0..rings; `ringSizes(0) == 1`); `def cellAt(ring: Int, i: Int): Int`; `def ringOf(cell: Int): Int`; `def indexInRing(cell: Int): Int`; cell 0 = centre = `goal`; `start` = the outer-ring cell whose arc contains the bottom (angle π/2, y down). Neighbours as in the spec (same ring ±1 with wrap, outward cells sharing the arc, the inward cell; centre ↔ all of ring 1). Subdivision: builder's rule, with the properties tested below.

- [ ] **Step 1: Write the failing tests:**
  - `"hex neighbours are symmetric; interior cells have 6"`: `HexLayout(7, 9)`; cell `(3, 4)` has 6 neighbours; corner `(0, 0)` has 2 or 3.
  - `"circle rings: centre is one cell, outer rings subdivide evenly"`: `CircleLayout(8)`: `ringSizes(0) == 1`; for i ≥ 2, `ringSizes(i) % ringSizes(i - 1) == 0`; for i ≥ 1, the arc width `2π·i / ringSizes(i)` (in ring widths, at the ring's inner radius `i`) is in `[0.7, 2.2]`.
  - `"circle neighbours are symmetric; centre is the goal; start is on the outer ring at the bottom"`: `CircleLayout(8)`: `goal == 0`; `ringOf(start) == 8`; the start arc contains angle π/2.
  - `"every style carves a perfect maze on hex and circle"`: styles × seeds 1..5 × layouts `HexLayout(9, 11)`, `CircleLayout(7)`: `reachable == cellCount`, `passages.size == cellCount - 1`.
- [ ] **Step 2: Run** `sbt -batch "testOnly tandu.activities.MazeGraphSpec"`. Expected: FAIL (does not compile).
- [ ] **Step 3: Implement** `HexLayout` and `CircleLayout`.
- [ ] **Step 4: Run** `sbt -batch test`. Expected: all pass.
- [ ] **Step 5: Commit** `maze: hex and circle layouts`.

### Task 4: Recipe — shapes, per-shape dimensions, codec, pages

**Files:**
- Modify: `src/main/scala/tandu/activities/Maze.scala` (section "printable sheet recipe" ~249-320; `sheetGrids` ~535)
- Test: `src/test/scala/tandu/activities/MazeSpec.scala`

**Interfaces:**
- Consumes: Task 1 `SheetBuilder.rounds`; Tasks 2–3 layouts and `carve`.
- Produces (`object Maze`):
  - `enum Shape { case Square, Circle, Hex }` (this order is the token and chip order).
  - `SheetConfig(size: Int, loops: Loops, perPage: PerPage, style: Style, shapes: Set[Shape] = Set(Shape.Square))`.
  - Extensions on `SheetConfig`: `def withShape(s: Shape): SheetConfig` (toggle; removing the last shape returns `c` unchanged); `def withAllShapes: SheetConfig`; `def hexDims: (Int, Int)` (rows, cols); `def rings: Int`. Keep `dims` (square).
    - `hexDims`: `n = max(3, size * 3 / 4)`. Tall slot (One, Four): `cols = n`, `s = slotW / (√3 · (cols + 0.5))`, `rows = floor((slotH / s - 0.5) / 1.5)`. Wide slot (Two): `rows = n`, `s = slotH / (1.5 · rows + 0.5)`, `cols = floor(slotW / (√3 · s) - 0.5)`.
    - `rings = max(2, size / 2 - 1)`.
  - `final case class SheetPage(shape: Shape, mazes: Vector[MazeGraph.Carved])`.
  - `def sheetPages(c: SheetConfig, n: Int, seed: Long): Vector[SheetPage]` — replaces `sheetGrids`: one `Random(seed)`; `order = SheetBuilder.rounds(selected shapes in enum order, max(n, MaxPages), rng).take(n)`; then each page makes `perPage.n` mazes of its shape with `carve(layout, c.style, c.loops.braid, rng)`. Layouts: Square `RectLayout(dims)`, Hex `HexLayout(hexDims)`, Circle `CircleLayout(rings)`.
  - Codec: `encodeConfig` appends `.<names>` (lower-case shape names joined by `-`, enum order) only when `shapes != Set(Square)`; `decodeConfig` reads a token whose `-`-split parts are all shape names (so `square`, `circle-hex`, …).
  - `sheetSpec`'s page type becomes `SheetPage` (`pagesOf = sheetPages`). Keep `sheetBody(c, page: SheetPage)` drawing squares only for now (Task 5 draws the other shapes); it must compile.

- [ ] **Step 1: Update and add tests** in `MazeSpec`:
  - Change `"adding pages keeps the earlier pages"` to `sheetPages` (head page equal; every page has 4 mazes; 3 pages distinct).
  - `"hex dims follow the slot"`: `cfg(16, Four, Hex).hexDims == (17, 12)`; `cfg(20, Two, Hex).hexDims == (15, 22)`; `cfg(30, One, Hex).hexDims == (31, 22)`.
  - `"circle rings from size"`: size 10 → 4; 30 → 14; 5 → 2.
  - `"every shape stays drawable at every max size"`: for each `PerPage` at `maxSize`: square cell ≥ 5.5 mm; circle ring width `(min(slotW, slotH) / 2) / (rings + 1)` ≥ 5.5; hex `s` ≥ 4 and `√3·s` ≥ 5.5 (compute `s` from `hexDims` as in Interfaces).
  - `"shapes codec"`: `encodeConfig(easy) == "s10.winding.few.pp4"` (unchanged); `easy.withShape(Circle)` encodes with `.square-circle` and round-trips; `decodeConfig("s10.winding.few.pp4.circle-hex").shapes == Set(Circle, Hex)`; `decodeConfig("s10.square").shapes == Set(Square)`; `decodeConfig("s10.bogus-hex").shapes == Set(Square)`.
  - `"the last shape cannot be removed"`: `easy.withShape(Square) == easy`.
  - `"pages go through the shapes in rounds"`: `easy.withAllShapes`, 6 pages, seed 7: each `grouped(3)` of page shapes is a permutation of all shapes; a Circle page's mazes have `CircleLayout`, a Hex page's `HexLayout`.
  (Helper: `cfg(size, perPage, shape) = SheetConfig(size, Loops.Off, PerPage.X, Style.Winding, Set(shape))`.)
- [ ] **Step 2: Run** `sbt -batch "testOnly tandu.activities.MazeSpec"`. Expected: FAIL (does not compile).
- [ ] **Step 3: Implement** the interfaces above.
- [ ] **Step 4: Run** `sbt -batch test`, then `sbt -batch fastLinkJS` and `npx playwright test e2e/maze.spec.ts e2e/workbook.spec.ts`. Expected: all pass (square-only behaviour is unchanged).
- [ ] **Step 5: Commit** `maze: shapes in the recipe; pages go through shapes in rounds`.

### Task 5: Drawing, form, strings, e2e

**Files:**
- Modify: `src/main/scala/tandu/activities/Maze.scala` (print section: `sheetBody`, `printableMaze`, new `hexSvg`, `circleSvg`; `sheetForm`)
- Modify: `src/main/scala/tandu/i18n/Strings.scala` (`MazeForm`, five languages)
- Modify: `styles.css` (maze print rules ~2269–2290)
- Test: `e2e/maze.spec.ts`, `e2e/workbook.spec.ts`

**Interfaces:**
- Consumes: Task 4 `SheetPage`, `hexDims`, `rings`, `withShape`, `withAllShapes`; Task 3 layout accessors.
- Produces:
  - `sheetBody(c, page)` → `div.maze-print-sheet.maze-print-sheet--pp<n>` with `data-shape="square|circle|hex"`; each board `div.maze-print-board.maze-print-board--<shape>`, width in mm + `aspect-ratio` (as today) + `max-width: 100%`.
    - Square: as today (`Grid` from the carved rect maze, `wallLines`).
    - Hex: board `√3·s·(cols + 0.5)` × `s·(1.5·rows + 0.5)` mm; walls as `svg.line` with class `maze-wall`; each shared edge once; border edges always.
    - Circle: board is a square of side `min(slotW, slotH)`; walls as `svg.path` with class `maze-wall` (arcs between rings, one sub-arc per outer neighbour without a passage; radial lines between ring neighbours without a passage); the outer circle with **one gap** at the start cell's arc; 🐭 in the start cell, 🧀 in the centre.
  - CSS: `.maze-print-svg .maze-wall` also gets `fill: none`.
  - Form: first row **Shapes** with `SheetBuilder.chip`s: test ids `maze-shape-all` (active when all three are selected; tap → `withAllShapes`), `maze-shape-square|circle|hex` (tap → `withShape`). Labels from strings.
  - Strings `MazeForm`: `shapes`, `shapeAll`, `shapeSquare`, `shapeCircle`, `shapeHex`. en "Shapes", "All", "Square", "Circle", "Hex"; pl "Kształty", "Wszystkie", "Kwadrat", "Koło", "Sześciokąty"; es "Formas", "Todas", "Cuadrado", "Círculo", "Hexágono"; fr "Formes", "Toutes", "Carré", "Cercle", "Hexagone"; de "Formen", "Alle", "Quadrat", "Kreis", "Sechseck".

- [ ] **Step 1: Write the failing e2e tests** in `maze.spec.ts` (the console-error guard already applies):
  - Update `"size is limited by mazes per page"`: the form rows are now Shapes (0), per page (1), size (2), style (3).
  - `"print mode: shapes are a multi-select, pages go through them"`: open `/activity/maze/print`; `maze-shape-square` active. Tap `maze-shape-circle` → URL ends `.square-circle`; tap `maze-shape-square` → URL ends `.circle`; preview has `.maze-print-board--circle`. Tap `maze-shape-circle` again → still active (last shape stays). Tap `maze-shape-all` → `maze-shape-all` active; set pages to 3 → the 3 pages (via `maze-page-next`) have 3 different `data-shape` values. `startPrinting` → `.print-only .maze-print-sheet` `data-shape` values equal the preview's, in order.
  - `"print mode: circle maze has one entrance; hex maze has hexagon walls"`: `/activity/maze/print/s10.winding.off.pp1.circle` → board `--circle`; its svg has `path.maze-wall` elements; the outer circle has exactly one gap (builder: give the outer wall path(s) a class `maze-border` and assert their count/arc via a `data-gap` attribute, or another exact check the builder chooses). `/activity/maze/print/s10.winding.off.pp1.hex` → board `--hex` with `line.maze-wall`.
  - `"print mode: a circle at 2 per page is round and fits"`: `/activity/maze/print/s10.winding.off.pp2.circle` → each board's boundingBox width ≈ height (±1 px) and inside the preview body (as the existing fit check).
  - `"print mode: a preset resets the shapes to square"`: tap `maze-shape-hex`, then `maze-preset-hard` → `maze-shape-square` active, `maze-shape-hex` not; URL ends `.pp1` (no shapes token).
  - In `workbook.spec.ts`, `"a Maze row has its own settings form…"`: also tap `maze-shape-circle` and `maze-shape-square` in the row (circle only), and expect `.print-only .maze-print-board--circle` after print.
- [ ] **Step 2: Run** `sbt -batch fastLinkJS` and `npx playwright test e2e/maze.spec.ts e2e/workbook.spec.ts`. Expected: the new tests FAIL (no `maze-shape-*`).
- [ ] **Step 3: Implement** the interfaces above.
- [ ] **Step 4: Run** `sbt -batch test fastLinkJS` and `npx playwright test e2e/maze.spec.ts e2e/workbook.spec.ts e2e/seek-and-find.spec.ts e2e/pictures.spec.ts`. Expected: all pass.
- [ ] **Step 5: Check prints:** render A4 PDFs (Chromium print) for circle and hex at 1, 2, 4 per page and each style; each is one page; walls 0.5 mm; one circle entrance; markers inside their cells.
- [ ] **Step 6: Update local `PRINTING.md`** (Maze log: shapes, `MazeGraph`, `SheetBuilder.rounds`).
- [ ] **Step 7: Commit** `maze: circle and hex shapes on the print sheet`.
