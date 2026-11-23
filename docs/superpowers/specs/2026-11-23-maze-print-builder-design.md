# Maze print page: sheet builder

Date: 2026-11-23 · Size: small change (refactor + one activity)

## Intent

All print pages move to the Seek & Find sheet-builder pattern (commits
`56b8538`, `f12e0dd`; index in local `PRINTING.md`). Maze is the first
activity after Seek & Find. The parent sets up a sheet in a form, sees it
in a live preview, and prints exactly that. The workbook uses the same
form per row.

## Goals

- Maze print page: presets, form, live preview, page count with pager,
  "new maze" button, recipe in the URL, Print (also Ctrl+P).
- Workbook: Maze is one row kind with its own settings, edited with the
  same form.
- A shared builder helper, so that the next activities need only their
  own recipe, codec, generator, sheet body and form rows.

## Non-goals

- Themed start/goal pictures, random start/goal positions, answer page
  (discussed; not now). Start stays 🐭 top-left, goal 🧀 bottom-right.
- Changes to the in-app maze.
- A generic "activity sheet" framework with workbook wiring and a generic
  codec (approach C, rejected).

## Anti-goals

- Seek & Find must not change visibly. Its e2e tests must pass unchanged.
- Old workbooks with `maze-easy` / `maze-medium` / `maze-hard` rows must
  keep their sheets.
- Cells must not become too small to draw a line through on paper.

## Approaches

- **A. Extract a shared helper now, then build Maze on it — picked.** The
  next six activities become cheap and consistent. Cost: the change also
  touches Seek & Find (guarded by its e2e tests).
- B. Copy Seek & Find code into Maze, extract later. Rejected: ~200
  duplicated lines and a bigger refactor later.
- C. Full framework incl. workbook wiring and generic codec. Rejected:
  too much design from two examples; workbook `Settings` is already
  generic.

## Decided

### Shared helper (`ui/SheetBuilder.scala`)

Moved out of `SeekAndFind.scala`, generic over the recipe type `C`:

- preview box (scaled A4 page) with action rows under it;
- pager `‹ i / n ›` and the pages stepper (1..10);
- "new scene" button (re-seed) and Print button;
- URL sync with `Routing.replace` (recipe token + `.pN`);
- print-only copies mounted only while printing (`beforeprint` /
  `afterprint` + Print button), then `Printable.print()`;
- preset pills (active only while the recipe equals the preset);
- form building blocks: form row (label + control), pills, chip,
  stepper bound to a field, switch bound to a flag;
- codec helpers: clamp, enum-by-lower-case-name, page-count token.

Seek & Find uses the helper; its CSS classes and test ids stay as they are
(either the helper takes a class/test-id prefix, or the shared classes
keep the current names — builder's call).

### Maze recipe

`SheetConfig(size: Int, loops: Loops, perPage: Int)`; pages are separate
(as in Seek & Find, a workbook row has its own page count).

- **size**: cells on the short side of the maze.
- **loops**: None / Some / Many → braid 0 / 0.3 / higher (builder picks,
  e.g. 0.6).
- **perPage**: 1, 2 or 4.
- **Shape follows the slot**: the long side is derived from the aspect of
  the slot that one maze gets on the page — 1 per page: tall; 2 per page:
  wide; 4 per page: tall. Mazes fill their slot; the board size in mm is
  computed, not set per variant in CSS.
- **Size limits depend on perPage**, so that cells stay large enough to
  draw in. Changing perPage clamps size.

Presets (match today's sheets):

| Preset | size | loops | perPage |
|---|---|---|---|
| Easy | 10 | Some | 4 |
| Medium | 16 | None | 4 |
| Hard | 24 | None | 1 |

Each page and each maze is a fresh random maze from one seeded RNG, so
page k does not change when pages are added after it.

### Workbook

- Group `maze` has one source `maze` with `Settings(default = Easy token,
  summary, title, pages, editor)`.
- Legacy rows `maze-easy|medium|hard` map to `maze` + the preset token,
  next to the existing `legacySeekRows` mapping in `WorkbookPage`.
- Book presets that use maze rows switch to the new row with a token.

### Tests

- New `e2e/maze.spec.ts`: presets, form changes reach the preview, URL
  round trip, pager and page count, print mounts the pages, size clamps
  with perPage.
- `e2e/workbook.spec.ts`: maze row editor, and a legacy maze row still
  renders.
- `e2e/seek-and-find.spec.ts` passes unchanged.

## Constraints

- The working tree has uncommitted picture-font / B&W work that touches
  `Maze.scala` (`Pictures.bwToggle()` on the print page) and `Printable`.
  Build on top of it; keep the B&W toggle next to the Print button.
- Strings: add the maze form labels in every language in `Strings.scala`,
  as Seek & Find does.
- Comments in ASD-STE100.

## Left to the builder

- Exact size limits per perPage and the minimum cell size in mm.
- Exact slot sizes in mm (from the printable body area) and the rounding
  of the long side.
- URL token format (tolerant, as `SeekAndFind.decodeConfig`).
- Loops "Many" braid value.
- Helper API shape and how CSS classes / test ids are parameterised.
