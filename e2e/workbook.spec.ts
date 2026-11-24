import { test, expect } from "@playwright/test"

// The workbook feature: a books list at /workbook, a per-book editor at
// /workbook/{idx}. Page selection is two-step — rows are added by kind and
// the variant is switched in place on the row, preview in hand. Printing is
// intercepted; we assert on the assembled print-only document instead.

test.beforeEach(async ({ page }) => {
  await page.addInitScript(() => {
    ;(window as any).__printCalls = 0
    window.print = () => {
      ;(window as any).__printCalls++
    }
  })
})

async function pickPreset(page, name: string) {
  // The panel may already be unfolded (fresh books lead with it).
  if ((await page.locator(".wbk-preset-chip").count()) === 0) {
    await page.locator(".wbk-presets-toggle").click()
  }
  await page.locator(".wbk-preset-chip", { hasText: name }).click()
}

/** The rows of one page kind, by the kind name on the row. */
const kindRows = (page, name: string) =>
  page.locator(".wbk-row", { has: page.locator(".wbk-row__name", { hasText: name }) })
const seekAndFindRows = (page) => kindRows(page, "Seek & Find")
const mazeRows = (page) => kindRows(page, "Maze")

// Creates a book and gives it the first age band — the composition most
// tests poke at.
async function createBook(page) {
  await page.goto("/workbook")
  await page.locator(".btn--hero").click()
  await expect(page).toHaveURL(/\/workbook\/[0-9a-f]+$/)
  await pickPreset(page, "4–5")
}

test("home banner leads to the books list, empty at first", async ({ page }) => {
  await page.goto("/")
  const banner = page.locator(".workbook-hero")
  await expect(banner).toBeVisible()
  await banner.click()
  await expect(page).toHaveURL(/\/workbook$/)
  await expect(page.locator(".wbk-empty")).toBeVisible()
  await expect(page.locator(".btn--hero")).toBeVisible()
})

test("a new book starts empty, leading with the preset choice", async ({ page }) => {
  await page.goto("/workbook")
  await page.locator(".btn--hero").click()
  await expect(page).toHaveURL(/\/workbook\/[0-9a-f]+$/)
  // No placeholder name is persisted; the empty name field gets focus.
  await expect(page.locator(".wbk-name-input")).toHaveValue("")
  await expect(page.locator(".wbk-name-input")).toBeFocused()
  // No placeholder composition either: the empty state itself offers the
  // presets (all but "Empty" — it's already empty).
  await expect(page.locator(".wbk-empty-rows .wbk-preset-chip")).toHaveCount(5)
  await page.locator(".wbk-preset-chip", { hasText: "4–5" }).click()
  // First age band: 7 source rows + the pinned cover row.
  await expect(page.locator(".wbk-row")).toHaveCount(8)
  // The cover is a fixed row: a toggle, not a stepper; off dims it.
  const cover = page.locator(".wbk-row--cover")
  await expect(cover.locator(".wbk-toggle")).toBeVisible()
  await expect(cover.locator(".wbk-stepper")).toHaveCount(0)
  await cover.locator(".wbk-toggle").click()
  await expect(cover).toHaveClass(/is-off/)
})

test("the book emoji is a button beside the name that opens a picker", async ({ page }) => {
  await createBook(page)
  // Picker closed by default; the current emoji is the button.
  await expect(page.locator(".wbk-emoji-btn")).toHaveCount(0)
  await page.locator(".wbk-emoji-current").click()
  await page.locator(".wbk-emoji-btn", { hasText: "🦖" }).click()
  await expect(page.locator(".wbk-emoji-current")).toHaveText("🦖")
  // Picking closes the picker; the cover row glyph follows the identity.
  await expect(page.locator(".wbk-emoji-btn")).toHaveCount(0)
  await expect(page.locator(".wbk-row--cover .wbk-row__glyph")).toHaveText("🦖")
})

test("the Empty preset clears the book", async ({ page }) => {
  await createBook(page)
  await pickPreset(page, "Empty")
  await expect(page.locator(".wbk-row")).toHaveCount(1) // just the cover
  await expect(page.locator(".wbk-print-btn")).toBeDisabled()
})

test("deleting a blank book skips the confirmation", async ({ page }) => {
  await page.goto("/workbook")
  await page.locator(".btn--hero").click()
  await expect(page).toHaveURL(/\/workbook\/[0-9a-f]+$/)
  // Untouched book: no name, no pages — delete goes straight through.
  await page.locator(".wbk-delete-book").click()
  await expect(page).toHaveURL(/\/workbook$/)
  await expect(page.locator(".modal-backdrop.is-open")).toHaveCount(0)
  await expect(page.locator(".wbk-empty")).toBeVisible()
})

test("preset chips swap the composition and steppers adjust it", async ({ page }) => {
  await createBook(page)
  await pickPreset(page, "8+")
  // 9 source rows + cover row.
  await expect(page.locator(".wbk-row")).toHaveCount(10)

  const firstSource = page.locator(".wbk-row:not(.wbk-row--cover)").first()
  await firstSource.locator(".wbk-stepper__btn").nth(1).click()
  await expect(firstSource.locator(".wbk-stepper__count")).toHaveText("2")
  await firstSource.locator(".wbk-stepper__btn").nth(0).click()
  await firstSource.locator(".wbk-stepper__btn").nth(0).click()
  await expect(page.locator(".wbk-row")).toHaveCount(9)
})

test("adding pages is by kind; the variant is switched on the row, preview in hand", async ({ page }) => {
  await createBook(page)
  // Step one: the add grid lists kinds, not variants.
  await page.locator(".wbk-add-toggle").click()
  await expect(page.locator(".wbk-add-chip")).toHaveCount(12)
  await expect(page.locator(".wbk-add-chip", { hasText: "Sudoku" })).toHaveCount(1)

  // The default book already has a Sudoku row (Emoji 4×4). Open its preview.
  const sudokuRow = page.locator(".wbk-row", { hasText: "Sudoku" }).first()
  await sudokuRow.locator(".wbk-row__main").click()
  await expect(sudokuRow.locator(".wbk-preview .sudoku-print-board--4").first()).toBeVisible()

  // Step two: switch the variant via the row's dropdown — the preview re-renders.
  await expect(sudokuRow.locator(".wbk-variant-select option")).toHaveCount(5)
  // Handwriting offers all four sets, including the random mix.
  const writingRow = page.locator(".wbk-row", { hasText: "Handwriting" })
  await expect(writingRow.locator(".wbk-variant-select option")).toHaveCount(4)
  await expect(writingRow.locator(".wbk-variant-select option").last()).toHaveText("Random")
  await sudokuRow.locator(".wbk-variant-select").selectOption({ label: "Hard" })
  await expect(sudokuRow.locator(".wbk-preview .sudoku-print-board--4")).toHaveCount(0)
  await expect(sudokuRow.locator(".wbk-preview .sudoku-print-board").first()).toBeVisible()
})

test("rows reorder by dragging the grip", async ({ page }) => {
  await createBook(page)
  const rows = page.locator(".wbk-row:not(.wbk-row--cover)")
  await expect(rows.first().locator(".wbk-row__name")).toHaveText("Handwriting")

  // Drag the first row's grip down past the second row.
  const grip = await rows.first().locator(".wbk-row__drag").boundingBox()
  const second = await rows.nth(1).boundingBox()
  await page.mouse.move(grip.x + grip.width / 2, grip.y + grip.height / 2)
  await page.mouse.down()
  await page.mouse.move(grip.x + grip.width / 2, second.y + second.height, { steps: 8 })
  await page.mouse.up()

  await expect(rows.first().locator(".wbk-row__name")).toHaveText("Dot to Dot")
  await expect(rows.nth(1).locator(".wbk-row__name")).toHaveText("Handwriting")

  // Keyboard path: arrows on the focused grip swap with the neighbour and
  // keep focus on the moved row's grip.
  await rows.first().locator(".wbk-row__drag").focus()
  await page.keyboard.press("ArrowDown")
  await expect(rows.first().locator(".wbk-row__name")).toHaveText("Handwriting")
  await expect(rows.nth(1).locator(".wbk-row__drag")).toBeFocused()
  await page.keyboard.press("ArrowUp")
  await expect(rows.first().locator(".wbk-row__name")).toHaveText("Dot to Dot")
})

test("the paper kind offers blank, lined and squared sheets", async ({ page }) => {
  await createBook(page)
  await page.locator(".wbk-add-toggle").click()
  await page.locator(".wbk-add-chip", { hasText: "Paper" }).click()
  const paperRow = page.locator(".wbk-row", { hasText: "Paper" })
  await expect(paperRow.locator(".wbk-variant-select option")).toHaveCount(3)
  await paperRow.locator(".wbk-variant-select").selectOption({ label: "Squared" })
  await paperRow.locator(".wbk-row__main").click()
  await expect(paperRow.locator(".wbk-preview .wbk-paper--squared")).toBeVisible()
})

test("sharing copies a link that imports the book elsewhere", async ({ page }) => {
  await page.addInitScript(() => {
    navigator.clipboard.writeText = (t: string) => {
      ;(window as any).__shareUrl = t
      return Promise.resolve()
    }
  })
  await createBook(page)
  await page.locator(".wbk-name-input").fill("Hanna")
  await page.locator(".wbk-share-book").click()
  const url = await page.evaluate(() => (window as any).__shareUrl)
  expect(url).toContain("/workbook/shared/")

  // A "different device": wipe storage, then open the link.
  await page.evaluate(() => localStorage.clear())
  await page.goto(url)
  await expect(page).toHaveURL(/\/workbook\/[0-9a-f]+$/)
  await expect(page.locator(".wbk-name-input")).toHaveValue("Hanna")

  // Opening the same link again must not duplicate the book.
  await page.goto(url)
  await page.locator(".header .btn--icon").first().click()
  await expect(page.locator(".wbk-book-card")).toHaveCount(1)
})

test("print assembles cover plus one sheet per page", async ({ page }) => {
  await createBook(page)
  await pickPreset(page, "8+")
  await page.locator(".wbk-name-input").fill("Hanna")

  await page.locator(".wbk-print-btn").click()
  await expect
    .poll(async () => page.evaluate(() => (window as any).__printCalls))
    .toBeGreaterThan(0)

  // 9 single-count rows + cover = 10 printable pages, cover first with the name.
  const printables = page.locator(".print-only .printable")
  await expect(printables).toHaveCount(10)
  await expect(printables.first()).toHaveClass(/wbk-cover/)
  await expect(printables.first().locator(".wbk-cover__name")).toHaveText("Hanna")
  await expect(page.locator(".print-only .sudoku-print-board")).toHaveCount(12)
})

test("a book left unnamed shows a muted untitled label, not a fake name", async ({ page }) => {
  await createBook(page)
  await page.locator(".header .btn--icon").first().click()
  const name = page.locator(".wbk-book-card__name")
  await expect(name).toHaveText("Untitled")
  await expect(name).toHaveClass(/--unnamed/)
})

test("deleting from the editor asks for confirmation first", async ({ page }) => {
  await createBook(page)
  await page.locator(".wbk-delete-book").click()
  const modal = page.locator(".modal-backdrop.is-open")
  await expect(modal).toBeVisible()
  // Cancel keeps the book and stays in the editor.
  await modal.locator(".btn--ghost").click()
  await expect(page).toHaveURL(/\/workbook\/[0-9a-f]+$/)
  // Confirm deletes and returns to the (now empty) list.
  await page.locator(".wbk-delete-book").click()
  await modal.locator(".wbk-confirm-delete").click()
  await expect(page).toHaveURL(/\/workbook$/)
  await expect(page.locator(".wbk-empty")).toBeVisible()
})

test("a book's URL is a stable id, not its list position", async ({ page }) => {
  // Two books; capture the second's editor URL.
  await page.goto("/workbook")
  await page.locator(".btn--hero").click()
  await page.locator(".wbk-name-input").fill("First")
  await page.locator(".header .btn--icon").first().click()
  await page.locator(".btn--hero").click()
  await page.locator(".wbk-name-input").fill("Second")
  const secondUrl = page.url()
  await page.locator(".header .btn--icon").first().click()

  // Delete the FIRST book — the second's slot shifts from index 1 to 0.
  await page
    .locator(".wbk-book-card", { hasText: "First" })
    .locator(".wbk-book-card__delete")
    .click()
  await page.locator(".modal-backdrop.is-open .wbk-confirm-delete").click()

  // The captured URL still opens the SECOND book, not whatever moved into
  // its old index (which is how positional ids would mis-resolve).
  await page.goto(secondUrl)
  await expect(page.locator(".wbk-name-input")).toHaveValue("Second")
})

test("export saves the whole library and import merges it back", async ({ page }) => {
  await createBook(page)
  await page.locator(".wbk-name-input").fill("Hanna")
  await page.locator(".header .btn--icon").first().click()
  await expect(page.locator(".wbk-book-card", { hasText: "Hanna" })).toBeVisible()

  const [download] = await Promise.all([
    page.waitForEvent("download"),
    page.locator(".wbk-export").click(),
  ])
  const file = await download.path()

  // A "cleared cache": wipe storage, the library is empty again.
  await page.evaluate(() => localStorage.clear())
  await page.goto("/workbook")
  await expect(page.locator(".wbk-empty")).toBeVisible()

  // Import the file — the book comes back.
  await page.locator(".wbk-import-input").setInputFiles(file)
  await expect(page.locator(".wbk-book-card", { hasText: "Hanna" })).toBeVisible()

  // Re-importing the same file is a no-op — merge by id, not duplicate.
  await page.locator(".wbk-import-input").setInputFiles(file)
  await expect(page.locator(".wbk-book-card")).toHaveCount(1)
})

test("books autosave onto the list; back returns to it; reload and delete work", async ({ page }) => {
  await createBook(page)
  await page.locator(".wbk-name-input").fill("Hanna")
  await page.locator(".wbk-emoji-current").click()
  await page.locator(".wbk-emoji-btn", { hasText: "🦖" }).click()

  // The back arrow returns to the list; the book card carries the identity.
  await page.locator(".header .btn--icon").first().click()
  await expect(page).toHaveURL(/\/workbook$/)
  const card = page.locator(".wbk-book-card", { hasText: "Hanna" })
  await expect(card).toBeVisible()
  await expect(card.locator(".wbk-book-card__emoji")).toHaveText("🦖")

  await page.reload()
  await expect(page.locator(".wbk-book-card", { hasText: "Hanna" })).toBeVisible()

  // Reopen, then delete from the list → empty state returns.
  await page.locator(".wbk-book-card__main").click()
  await expect(page).toHaveURL(/\/workbook\/[0-9a-f]+$/)
  await page.locator(".header .btn--icon").first().click()
  await page.locator(".wbk-book-card__delete").click()
  await page.locator(".modal-backdrop.is-open .wbk-confirm-delete").click()
  await expect(page.locator(".wbk-empty")).toBeVisible()
})

test("a Seek & Find row has its own settings form; they save, share and print", async ({ page }) => {
  await createBook(page) // 4–5 preset: an Easy and a Counting Seek & Find row
  const seekRows = seekAndFindRows(page)
  await expect(seekRows).toHaveCount(2)
  await expect(seekRows.nth(0).getByTestId("wbk-row-summary")).toHaveText("Easy")
  await expect(seekRows.nth(1).getByTestId("wbk-row-summary")).toHaveText("Counting")
  await expect(seekRows.locator(".wbk-variant-select")).toHaveCount(0)

  // The summary unfolds the form; a change makes the row "Custom" and the
  // preview follows.
  const first = seekRows.nth(0)
  await first.getByTestId("wbk-row-summary").click()
  await first.getByTestId("snf-task-count").click()
  await first.getByTestId("snf-answer-write").click()
  await expect(first.getByTestId("wbk-row-summary")).toHaveText("Custom")
  await expect(first.getByTestId("snf-preview").locator(".snf-count-box")).toHaveCount(2)

  // The slider keeps working across several changes: the form is not rebuilt.
  const extras = first.getByTestId("snf-extras")
  await extras.fill("10")
  await extras.fill("20")
  await expect(extras).toHaveValue("20")
  await expect(first.getByTestId("snf-answer-write")).toHaveClass(/is-active/)

  // A preset in the row form brings the name back.
  await first.getByTestId("snf-preset-hard").click()
  await expect(first.getByTestId("wbk-row-summary")).toHaveText("Hard")
  await first.getByTestId("snf-task-count").click()
  await first.getByTestId("snf-answer-write").click()

  // Saved: a reload keeps the settings.
  await page.reload()
  await expect(seekRows.nth(0).getByTestId("wbk-row-summary")).toHaveText("Custom")

  // Printed: two pages for the row, both write-in counting sheets with the
  // themes in a round (Hard uses all themes, so two pages differ).
  await seekRows.nth(0).locator(".wbk-stepper__btn").nth(1).click()
  await page.locator(".wbk-print-btn").click()
  await expect.poll(async () => page.evaluate(() => (window as any).__printCalls)).toBeGreaterThan(0)
  const writeSheets = page.locator(".print-only .snf-print", { has: page.locator(".snf-count-box") })
  await expect(writeSheets).toHaveCount(2)
  const themes = await writeSheets.evaluateAll(es => es.map(e => e.getAttribute("data-theme")))
  expect(themes[0]).not.toBe(themes[1])
  await expect(page.locator(".print-only .printable__title", { hasText: "count and write" })).toHaveCount(2)
})

test("books saved with the old Seek & Find variants keep their sheets", async ({ page }) => {
  await page.goto("/workbook")
  await page.evaluate(() => localStorage.setItem("tandu.workbook.books", JSON.stringify([{
    id: "0123abcd", cover: false, name: "Old", emoji: "🦊",
    rows: [{ s: "seek-and-find-medium", c: 1 }, { s: "seek-and-find-count", c: 2 }, { s: "maze-easy", c: 1 }],
  }])))
  await page.goto("/workbook/0123abcd")
  const summaries = page.getByTestId("wbk-row-summary")
  // The maze row has its own settings too (see the maze tests below).
  await expect(summaries).toHaveText(["Medium", "Counting", "Easy"])
  await expect(page.locator(".wbk-row:not(.wbk-row--cover)")).toHaveCount(3)
})

test("a book preset replacing the rows refreshes an open Seek & Find form", async ({ page }) => {
  await createBook(page)
  const row = seekAndFindRows(page).first()
  await row.getByTestId("wbk-row-summary").click()
  await row.getByTestId("snf-task-count").click()
  await expect(row.getByTestId("wbk-row-summary")).toHaveText("Custom")

  // Same preset again, with the form still open: the form shows Easy.
  await pickPreset(page, "4–5")
  await expect(row.getByTestId("wbk-row-summary")).toHaveText("Easy")
  await expect(row.getByTestId("snf-task-find")).toHaveClass(/is-active/)

  // The next tap changes only what was tapped, not the old task.
  await row.getByTestId("snf-layout-wild").click()
  const stored = await page.evaluate(() =>
    JSON.parse(localStorage.getItem("tandu.workbook.books")!)[0].rows.find(r => r.s === "seek-and-find").p)
  expect(stored).toMatch(/^find\./)
  expect(stored).toContain(".wild.")
})

test("a Maze row has its own settings form; they save and print", async ({ page }) => {
  await createBook(page) // 4–5 preset: one Easy maze row
  const row = mazeRows(page)
  await expect(row).toHaveCount(1)
  await expect(row.getByTestId("wbk-row-summary")).toHaveText("Easy")
  await expect(row.locator(".wbk-variant-select")).toHaveCount(0)

  // The summary unfolds the form; a change makes the row "Custom" and the
  // preview follows.
  await row.getByTestId("wbk-row-summary").click()
  await row.getByTestId("maze-per-page-2").click()
  await expect(row.getByTestId("wbk-row-summary")).toHaveText("Custom")
  await expect(row.getByTestId("maze-preview").locator(".maze-print-board")).toHaveCount(2)
  // Circle only (the preset has all shapes).
  await row.getByTestId("maze-shape-square").click()
  await row.getByTestId("maze-shape-hex").click()
  await expect(row.getByTestId("maze-preview").locator(".maze-print-board--circle")).toHaveCount(2)

  // Saved: a reload keeps the settings.
  await page.reload()
  await expect(mazeRows(page).getByTestId("wbk-row-summary")).toHaveText("Custom")

  // Printed: two pages for the row, both with two mazes.
  await mazeRows(page).locator(".wbk-stepper__btn").nth(1).click()
  await page.locator(".wbk-print-btn").click()
  await expect.poll(async () => page.evaluate(() => (window as any).__printCalls)).toBeGreaterThan(0)
  await expect(page.locator(".print-only .maze-print-sheet--pp2")).toHaveCount(2)
  await expect(page.locator(".print-only .maze-print-board--circle")).toHaveCount(4)
})

test("books saved with the old maze variants keep their sheets", async ({ page }) => {
  await page.goto("/workbook")
  await page.evaluate(() => localStorage.setItem("tandu.workbook.books", JSON.stringify([{
    id: "0123abce", cover: false, name: "Old mazes", emoji: "🐭",
    rows: [{ s: "maze-hard", c: 1 }, { s: "maze-medium", c: 1 }],
  }])))
  await page.goto("/workbook/0123abce")
  await expect(page.getByTestId("wbk-row-summary")).toHaveText(["Hard", "Medium"])

  await page.locator(".wbk-print-btn").click()
  await expect.poll(async () => page.evaluate(() => (window as any).__printCalls)).toBeGreaterThan(0)
  const firstSheet = page.locator(".print-only .maze-print-sheet").first()
  // Hard: one maze per page (of any shape: the presets use all shapes).
  await expect(firstSheet.locator(".maze-print-board")).toHaveCount(1)
})

test("a book preset replacing the rows refreshes an open Maze form", async ({ page }) => {
  await createBook(page)
  const row = mazeRows(page)
  await row.getByTestId("wbk-row-summary").click()
  await row.getByTestId("maze-loops-many").click()
  await expect(row.getByTestId("wbk-row-summary")).toHaveText("Custom")

  // Same preset again, with the form still open: the form shows Easy.
  await pickPreset(page, "4–5")
  await expect(row.getByTestId("wbk-row-summary")).toHaveText("Easy")
  await expect(row.getByTestId("maze-loops-few")).toHaveClass(/is-active/)

  // The next tap changes only what was tapped.
  await row.getByTestId("maze-per-page-1").click()
  const stored = await page.evaluate(() =>
    JSON.parse(localStorage.getItem("tandu.workbook.books")!)[0].rows.find(r => r.s === "maze").p)
  expect(stored).toMatch(/^s\d+\.winding\.few\.pp1$/)
})
