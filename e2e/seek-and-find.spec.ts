import { test, expect } from "@playwright/test"

// The scene is generated at runtime from themed emoji pools, so these tests
// double as a smoke check that the dealer keeps its promises: exact target
// counts, targets excluded from distractors, and a win when all are tapped.

test("seek mode: legend counts match the field and tapping them all wins", async ({ page }) => {
  await page.goto("/activity/seek-and-find/seek/easy")
  const board = page.locator(".snf-board")
  await expect(board).toBeVisible({ timeout: 15_000 })

  // Easy deals one target kind; its emoji sits in the legend chip and its
  // promised count is the chip's dot row.
  const chip = page.locator(".snf-chip")
  await expect(chip).toHaveCount(1)
  const emoji = (await chip.locator(".snf-chip__emoji").textContent())!
  const promised = await chip.locator(".snf-dot").count()
  expect(promised).toBeGreaterThanOrEqual(4)
  expect(promised).toBeLessThanOrEqual(6)

  // The field must hold exactly the promised number of copies — no extras
  // sneaking in via the distractor pool.
  const targets = board.locator(".snf-item").filter({ hasText: emoji })
  await expect(targets).toHaveCount(promised)

  // A wrong tap wobbles but never wins or marks.
  const distractor = board.locator(".snf-item").filter({ hasNotText: emoji }).first()
  await distractor.dispatchEvent("pointerdown")
  await expect(page.locator(".handoff")).toHaveCount(0)
  await expect(board.locator(".snf-item.is-found")).toHaveCount(0)

  for (let i = 0; i < promised; i++) {
    await targets.nth(i).dispatchEvent("pointerdown")
  }
  await expect(board.locator(".snf-item.is-found")).toHaveCount(promised)
  await expect(page.locator(".handoff")).toBeVisible()
  await page.screenshot({ path: "/tmp/snf-seek-won.png" })
})

test("count mode: the true count is offered and answering it reveals the rings", async ({ page }) => {
  await page.goto("/activity/seek-and-find/count/easy")
  const board = page.locator(".snf-board")
  await expect(board).toBeVisible({ timeout: 15_000 })

  // The asked emoji is embedded in the question; count its copies ourselves.
  const question = (await page.locator(".snf-question").textContent())!
  const answers = page.locator(".snf-answer")
  await expect(answers).toHaveCount(4)
  let truth = 0
  const items = board.locator(".snf-item")
  for (const text of await items.locator("text").allTextContents()) {
    if (question.includes(text)) truth++
  }
  expect(truth).toBeGreaterThanOrEqual(3)

  // A wrong answer is disabled and play continues; the right one wins and
  // rings every counted item as confirmation.
  const labels = await answers.allTextContents()
  const wrong = labels.find(l => Number(l) !== truth)!
  await answers.filter({ hasText: new RegExp(`^${wrong}$`) }).click()
  await expect(page.locator(".handoff")).toHaveCount(0)

  await answers.filter({ hasText: new RegExp(`^${truth}$`) }).click()
  await expect(page.locator(".handoff")).toBeVisible()
  await expect(board.locator(".snf-item.is-found")).toHaveCount(truth)
})

test("print mode: seek and counting sheets fill with a scene", async ({ page }) => {
  await page.goto("/activity/seek-and-find/print")
  await page.evaluate(() => { window.print = () => {} })
  const buttons = page.locator(".snf-print-actions button")
  await expect(buttons).toHaveCount(4) // easy, medium, hard, counting

  // Seek sheet: a legend with counts plus a dense field. Emoji print as
  // pre-rasterised images, never text — Chrome's print preview crashes on
  // sheets full of colour-emoji glyphs.
  await buttons.first().click()
  await expect(page.locator(".snf-print-chip")).toHaveCount(1)
  expect(await page.locator(".snf-print-field text").count()).toBe(0)
  expect(await page.locator(".snf-print-field image").count()).toBe(48)

  // Counting sheet: one write-in box per kind, and nothing on the field
  // that isn't one of the counted kinds. The raster cache hands identical
  // data URLs to the legend <img>s and the field <image>s, so the sets match.
  await buttons.nth(3).click()
  await expect(page.locator(".snf-count-box")).toHaveCount(4)
  const kinds = new Set(
    await page.locator(".snf-count-cell img").evaluateAll(es => es.map(e => e.getAttribute("src"))))
  const onField = await page.locator(".snf-print-field image").evaluateAll(
    es => es.map(e => e.getAttribute("href")))
  expect(onField.length).toBeGreaterThanOrEqual(16) // 4 kinds × at least 4 each
  for (const href of onField) expect(kinds.has(href)).toBe(true)

  await page.emulateMedia({ media: "print" })
  await page.setViewportSize({ width: 794, height: 1123 }) // A4 @ 96dpi
  await page.screenshot({ path: "/tmp/snf-print.png", fullPage: true })
})
