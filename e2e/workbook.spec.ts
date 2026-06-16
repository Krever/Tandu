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

// Creates a book and gives it the first age band — the composition most
// tests poke at.
async function createBook(page) {
  await page.goto("/workbook")
  await page.locator(".btn--hero").click()
  await expect(page).toHaveURL(/\/workbook\/0$/)
  await pickPreset(page, "4–5")
}

test("home banner leads to the books list, empty at first", async ({ page }) => {
  await page.goto("/")
  const banner = page.locator(".workbook-banner")
  await expect(banner).toBeVisible()
  await banner.click()
  await expect(page).toHaveURL(/\/workbook$/)
  await expect(page.locator(".wbk-empty")).toBeVisible()
  await expect(page.locator(".btn--hero")).toBeVisible()
})

test("a new book starts empty, leading with the preset choice", async ({ page }) => {
  await page.goto("/workbook")
  await page.locator(".btn--hero").click()
  await expect(page).toHaveURL(/\/workbook\/0$/)
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
  await expect(page).toHaveURL(/\/workbook\/0$/)
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
  await expect(page).toHaveURL(/\/workbook\/0$/)
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
  await expect(page).toHaveURL(/\/workbook\/0$/)
  // Confirm deletes and returns to the (now empty) list.
  await page.locator(".wbk-delete-book").click()
  await modal.locator(".wbk-confirm-delete").click()
  await expect(page).toHaveURL(/\/workbook$/)
  await expect(page.locator(".wbk-empty")).toBeVisible()
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
  await expect(page).toHaveURL(/\/workbook\/0$/)
  await page.locator(".header .btn--icon").first().click()
  await page.locator(".wbk-book-card__delete").click()
  await page.locator(".modal-backdrop.is-open .wbk-confirm-delete").click()
  await expect(page.locator(".wbk-empty")).toBeVisible()
})
