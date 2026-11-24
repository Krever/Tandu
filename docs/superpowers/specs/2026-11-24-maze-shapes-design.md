# Maze print sheet: circle and hex shapes

Date: 2026-11-24 · Size: project · Branch: `maze-print-builder`

## Intent

The Maze print sheet (built on `ui/SheetBuilder`, see
`2026-11-23-maze-print-builder-design.md`) prints only rectangular mazes
on a square grid. The user wants **more variety and more difficulty**.
Two new shapes:

- **Circle**: rings of cells around a centre. Start at the outside, goal
  in the centre. The user: "circle is exactly what I thought".
- **Hex**: hexagon cells, 6 walls / ways out per cell instead of 4. The
  user expects this to add a lot of variety.

Shapes behave like the **themes of Seek & Find**: a multi-select; the
pages go through the selected shapes.

## Goals

- A "Shapes" multi-select (All · Square · Circle · Hex) in the maze form.
- One shape per page; pages go through the selected shapes in shuffled
  rounds (same rule as Seek & Find themes).
- Every style (winding, balanced, branchy) and every loops value works
  for every shape.
- Mazes per page, size limits (cells at least 5.5 mm), 0.5 mm walls, live
  preview = print, URL token, workbook row: all work for every shape.

## Non-goals

- Silhouette mazes (maze inside a picture outline): the user rejected
  them.
- Circle or hex in the in-app (on-screen, playable) maze. The in-app maze
  stays square and keeps its current `Grid` interface.
- Triangle, weave, or other shapes.
- Mixing shapes on one page.

## Anti-goals

- Old links and stored workbook rows must not change: a token without a
  shapes token means **Square only**.
- No shape may give cells smaller than 5.5 mm (measured across the cell)
  at any allowed size.
- The generator code must not be copied per shape (3 styles × 3 shapes).

## Approaches

- **General cell graph — picked.** Each shape is a *layout*: cells
  `0 until n`, the neighbours of each cell, start cell, goal cell, and its
  own drawing code. The three styles and the loops step work on the graph
  once. The square layout gives the same `Grid` that the in-app maze uses
  today.
- Separate generators per shape. Rejected: 9 near-copies, each fix made
  three times.
- Circle and hex as special rectangular arrays (offset rows, rows of
  different length) with special cases in each algorithm. Rejected: the
  same idea as the graph, with less readable code.

## Decided

### Model

- A layout (builder names it; for example `trait Layout`) has:
  `cellCount`, `neighbours(cell): Vector[Int]` (symmetric), `start`,
  `goal`.
- The carve result is the set of passages between neighbour cells (a
  spanning tree, plus extra passages from loops).
- The three styles keep their meaning (see `Maze.Style` docs): winding =
  recursive backtracker, balanced = Wilson's algorithm, branchy = Prim's
  with a random frontier. Loops: the same rule as today (open an extra
  wall at that fraction of dead ends, never at start or goal).
- The current `generate(rows, cols, braid, rng, style): Grid` stays as the
  API for the in-app maze and for tests; it uses the square layout
  internally. Today's `MazeSpec` tests must still pass.

### Square

- Unchanged: `cols × rows` from `size` and the slot shape (see `dims`);
  start top-left, goal bottom-right; current drawing.

### Circle

- Ring 0 is one centre cell (the goal, 🧀). Rings `1..R` around it.
- Outer rings have more cells, so that cells keep about the same width
  along the ring: the cell count of a ring doubles when the arc width of
  its cells would become more than about 2 ring widths (builder picks the
  exact rule; standard "theta maze" subdivision).
- Neighbours: the two cells beside it on the same ring; the cell(s) on
  the next ring out that share its arc; the cell on the next ring in
  whose arc contains it. Ring 0 neighbours all cells of ring 1.
- Start (🐭): a cell of the outer ring at the **bottom**, with a **gap in
  the outer wall** at that cell, so the child enters from outside.
  🐭 is drawn in the start cell.
- Rings from size: **R = max(2, size / 2 − 1)** (integer division). Then
  the ring width is about the same as the square cell width at the same
  size, and the 5.5 mm limit holds with today's max sizes:
  - 1 per page, size 30: R = 14, ring width = 95 / 15 ≈ 6.3 mm
  - 2 per page, size 20: R = 9, circle diameter 112 mm, ≈ 5.6 mm
  - 4 per page, size 16: R = 7, diameter 92 mm, ≈ 5.75 mm
  (Radius = ring width × (R + 1); the centre cell's radius is one ring
  width.)
- The circle's diameter is the shorter side of the slot; it is centred in
  the slot.
- Drawing: SVG arcs for walls between rings (one sub-arc per outer
  neighbour where there is no passage), radial lines between cells on the
  same ring, the full outer circle except the start gap.

### Hex

- Pointy-top hexagons in offset rows (odd rows shifted half a cell),
  making a rectangle-shaped block that fills the slot (the user does not
  mind rectangle vs big hexagon; the rectangle fills the page better).
- Up to 6 neighbours per cell.
- A passage between two hex cells is one hex edge, only `s = w/√3 ≈
  0.58·w` long. To keep the doorways drawable, hex uses **¾ of `size`**
  cells on the short side: `n = max(3, size * 3 / 4)` (integer
  arithmetic). Tall slots (1 and 4 per page): `n` columns; wide slot
  (2 per page): `n` rows. The other count follows from the slot (builder
  computes it from hex geometry: across-flats width `w = √3·s`, row step
  `1.5·s`). (Decided after the builder check; the size limits stay per
  `PerPage` only, the same for every shape.)
- At every max size: across-flats width at least 5.5 mm and **edge
  (doorway) length at least 4 mm** (a unit test checks both for each
  `PerPage`). Expected: 4 per page, size 16 → 12 columns, w ≈ 7.4 mm,
  edge ≈ 4.25 mm.
- Start top-left cell, goal bottom-right cell.
- Drawing: each hex edge is a wall if there is no passage across it (or
  no neighbour: the border). Draw each shared edge once.

### Recipe, form, URL

- `SheetConfig` gets `shapes: Set[Shape]` (`Square`, `Circle`, `Hex`),
  never empty.
- Form: a **Shapes** row first, with chips **All · Square · Circle ·
  Hex** (like the Seek & Find theme chips: `SheetBuilder.chip`). "All"
  selects all three. Removing the last selected shape is not possible
  (the chip stays on). Then Mazes per page, Size, Style, Loops.
- **Presets stay Square only**, so stored rows and links keep matching
  their preset ("Easy", "Hard"), and the presets keep their meaning. The
  parent picks shapes in the form.
- **A preset sets the whole recipe, shapes included** (user's decision):
  tapping "Hard" after picking Circle + Hex resets the shapes to Square,
  and a row with Hard settings plus Circle shows "Custom". This is how
  Seek & Find themes behave today. No `SheetBuilder` change.
- URL token: shape names joined by `-` (`circle-hex`,
  `square-circle-hex`), in a fixed order. **Square only is encoded with
  no shapes token**, so today's links and stored rows stay byte-identical
  (an existing workbook test pins `s10.winding.few.pp1`). The decoder
  still accepts `square`. Missing or unknown → Square only.
- Strings (5 languages): `shapes`, `shapeAll`, `shapeSquare`,
  `shapeCircle`, `shapeHex` in `MazeForm`.

### Pages

- One shape per page; all mazes on a page have that shape.
- Shape order: shuffled rounds; each selected shape once before any
  repeats; a new round does not start with the shape that ended the last
  one. This is Seek & Find's `themeOrder`: move it to `SheetBuilder` as a
  generic helper and use it in both.
- Page stability as today: the order is fixed for `MaxPages` first, then
  one RNG makes the pages in order, so page k does not change when pages
  are added after it.

### Tests

- Unit (`MazeSpec`): for each shape × style, a perfect maze (every cell
  reachable, passages = cells − 1 with loops off); neighbour lists are
  symmetric; hex interior cells have 6 neighbours; circle ring 0 is the
  goal and the start is on the outer ring; 5.5 mm limit per shape and
  `PerPage`; codec round trip with shapes, missing shapes token = Square;
  shape order in rounds.
- e2e (`maze.spec.ts`): shapes chips and URL token; a circle page shows
  the circle drawing, a hex page the hex drawing; with all shapes and 3
  pages, the 3 pages have 3 different shapes; print matches preview;
  no console errors.
- Existing Seek & Find and workbook tests pass unchanged (Seek & Find
  only gets the moved `themeOrder`).

## Constraints

- Scala.js, Laminar; SVG for print; `-Werror` (no unused imports).
- e2e needs `sbt fastLinkJS` first (the reused dev server links only at
  start).
- Comments in ASD-STE100.
- Walls: 0.5 mm, `vector-effect: non-scaling-stroke` (as square today).

## Left to the builder

- Names and file layout of the graph/layout code (a new file such as
  `activities/MazeLayouts.scala` is fine if `Maze.scala` gets too long).
- The exact circle subdivision rule, and SVG path details.
- Hex row count formula and centring in the slot.
- Whether the square print drawing moves onto the generic layout drawing
  or keeps `wallLines`.
- Marker (🐭/🧀) size for circle and hex cells.
