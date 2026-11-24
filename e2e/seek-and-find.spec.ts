import { test, expect } from "@playwright/test"
import { stepperPlus, startPrinting } from "./helpers"

// The scene is generated at runtime from themed emoji pools, so these tests
// double as a smoke check that the dealer keeps its promises: exact target
// counts, targets excluded from distractors, and a win when all are tapped.

/** A scene's identity: each picture and where it sits. */
const scene = (root) => root.locator(".snf-print-field image").evaluateAll(
  es => es.map(e => `${e.getAttribute("href")}@${e.getAttribute("transform")}`).join("|"))

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

test("print mode: presets fill the form, the preview follows, print matches it", async ({ page }) => {
  await page.goto("/activity/seek-and-find/print")
  await page.evaluate(() => { window.print = () => {} })
  const preview = page.getByTestId("snf-preview")
  const printed = page.locator(".print-only")

  // Easy preset is active at first. Find sheet: a legend with counts plus a
  // field. Emoji print as pre-rasterised images, never text — Chrome's
  // print preview crashes on sheets full of colour-emoji glyphs.
  await expect(page.getByTestId("snf-preset-easy")).toHaveClass(/is-active/)
  await expect(preview.locator(".snf-print-chip")).toHaveCount(2)
  await expect(preview.locator(".snf-print-chip").first()).toContainText("×")
  expect(await preview.locator(".snf-print-field text").count()).toBe(0)
  const easyCount = await preview.locator(".snf-print-field image").count()
  expect(easyCount).toBeGreaterThanOrEqual(40 + 2 * 5)
  expect(easyCount).toBeLessThanOrEqual(40 + 2 * 7)

  // The printed page is the same page as the preview.
  await startPrinting(page)
  expect(await scene(printed)).toEqual(await scene(preview))

  // A change in the form updates the preview and clears the active preset.
  await page.getByTestId("snf-show-counts").click()
  await expect(preview.locator(".snf-print-chip").first()).not.toContainText("×")
  await expect(page.getByTestId("snf-preset-easy")).not.toHaveClass(/is-active/)

  // Counting: one number line (1 to 10) per kind, and nothing on the field
  // that isn't one of the counted kinds. The raster cache hands identical
  // data URLs to the legend <img>s and the field <image>s.
  await page.getByTestId("snf-task-count").click()
  await page.getByTestId("snf-look-alikes").click()
  const kindsPlus = stepperPlus(page, "snf-kinds")
  for (let i = 0; i < 2; i++) await kindsPlus.click() // Easy has 2 kinds; the sheet needs 4
  await page.getByTestId("snf-extras").fill("0")
  const cells = preview.locator(".snf-count-cell")
  await expect(cells).toHaveCount(4)
  for (const cell of await cells.all()) {
    expect(await cell.locator(".snf-count-num").allTextContents())
      .toEqual(["1", "2", "3", "4", "5", "6", "7", "8", "9", "10"])
  }
  const kinds = new Set(
    await preview.locator(".snf-count-cell img").evaluateAll(es => es.map(e => e.getAttribute("src"))))
  const onField = await preview.locator(".snf-print-field image").evaluateAll(
    es => es.map(e => e.getAttribute("href")))
  expect(onField.length).toBeGreaterThanOrEqual(16) // 4 kinds × at least 4 each
  for (const href of onField) expect(kinds.has(href)).toBe(true)
  for (const kind of kinds) {
    const n = onField.filter(h => h === kind).length
    expect(n).toBeGreaterThanOrEqual(5)
    expect(n).toBeLessThanOrEqual(7)
  }

  // Write-in answer: boxes instead of number lines.
  await page.getByTestId("snf-answer-write").click()
  await expect(preview.locator(".snf-count-box")).toHaveCount(4)
  await expect(preview.locator(".snf-count-num")).toHaveCount(0)

  // Steppers keep min ≤ max.
  await page.getByTestId("snf-preset-easy").click()
  const minPlus = stepperPlus(page, "snf-per-min")
  await minPlus.click()
  await minPlus.click()
  await expect(minPlus).toBeDisabled() // min reached max (7)

  await page.getByTestId("snf-print").click()
})

test("print mode: the recipe is kept in the URL", async ({ page }) => {
  await page.goto("/activity/seek-and-find/print")
  await page.getByTestId("snf-preset-medium").click()
  await page.getByTestId("snf-task-count").click()
  await page.getByTestId("snf-answer-write").click()
  await page.getByTestId("snf-theme-ocean").click()
  await expect(page).toHaveURL(/\/print\/count\.ocean\.k2\.n4-6\.x70\.mixed\.la1\.sc1\.write$/)

  // Opening the link restores the form and the sheet.
  await page.goto(page.url())
  await expect(page.getByTestId("snf-task-count")).toHaveClass(/is-active/)
  await expect(page.getByTestId("snf-answer-write")).toHaveClass(/is-active/)
  await expect(page.getByTestId("snf-theme-ocean")).toHaveClass(/is-active/)
  await expect(page.getByTestId("snf-preview").locator(".snf-count-box")).toHaveCount(2)

  // Bad values are clamped, not trusted.
  await page.goto("/activity/seek-and-find/print/find.k9.n12-3.x999.bogus")
  await expect(page.getByTestId("snf-kinds")).toContainText("4")
  await expect(page.getByTestId("snf-per-min")).toContainText("10")
  await expect(page.getByTestId("snf-per-max")).toContainText("10")
  await expect(page.getByTestId("snf-extras")).toHaveValue("120")

  // A number too long for an Int is ignored; the page still works.
  await page.goto("/activity/seek-and-find/print/find.n99999999999-3")
  await expect(page.getByTestId("snf-per-min")).toContainText("5")
  await expect(page.getByTestId("snf-preview").locator(".snf-print-field")).toBeVisible()
})

/** With look-alikes on, about half of the other pictures are look-alikes of
  * the target, so one non-target kind is much more common than the rest.
  * Every theme must take part: before the fix, farm, sweets and garden had
  * no look-alike groups and the toggle did nothing there. One scene is
  * noisy, so the check sums 18 scenes (3 per theme). Measured: on 480–590,
  * off 250–270. */
test("print mode: look-alikes put similar pictures next to the target, in every theme", async ({ page }) => {
  const preview = page.getByTestId("snf-preview")
  const topOtherKind = async () => {
    const target = await preview.locator(".snf-print-chip img").getAttribute("src")
    const hrefs = await preview.locator(".snf-print-field image").evaluateAll(
      es => es.map(e => e.getAttribute("href")))
    const counts = new Map<string, number>()
    for (const h of hrefs) if (h !== target) counts.set(h!, (counts.get(h!) ?? 0) + 1)
    return Math.max(...counts.values())
  }
  const sumOverScenes = async () => {
    let sum = 0
    for (let i = 0; i < 3; i++) {
      sum += await topOtherKind()
      await page.getByRole("button", { name: "New scene" }).click()
    }
    return sum
  }
  let on = 0
  let off = 0
  for (const theme of ["ocean", "forest", "farm", "space", "sweets", "garden"]) {
    await page.goto(`/activity/seek-and-find/print/find.${theme}.k1.n5-7.x70.tidy.la1.sc1.circle`)
    await expect(preview.locator(".snf-print-field image").first()).toBeAttached()
    const themeOn = await sumOverScenes()
    await page.getByTestId("snf-look-alikes").click()
    const themeOff = await sumOverScenes()
    // A theme without look-alike groups gives the same scene both ways.
    expect(themeOn, `${theme}: the toggle must change the scene`).not.toBe(themeOff)
    on += themeOn
    off += themeOff
  }
  expect(on).toBeGreaterThanOrEqual(1.5 * off)
})

test("print mode: several pages, each with a different scene", async ({ page }) => {
  await page.goto("/activity/seek-and-find/print")
  await page.evaluate(() => { window.print = () => {} })
  const preview = page.getByTestId("snf-preview")
  // One page: no pager, one printed sheet.
  await startPrinting(page)
  await expect(page.getByTestId("snf-page-label")).toBeHidden()
  await expect(page.locator(".print-only .printable")).toHaveCount(1)
  const first = await scene(preview)

  const plus = stepperPlus(page, "snf-pages")
  await plus.click()
  await plus.click()
  await expect(page).toHaveURL(/\.p3$/)
  const printed = page.locator(".print-only .printable")
  await expect(printed).toHaveCount(3)

  // Adding pages keeps page 1, and every page is a different scene.
  const scenes = await Promise.all([0, 1, 2].map(i => scene(printed.nth(i))))
  expect(scenes[0]).toBe(first)
  expect(new Set(scenes).size).toBe(3)

  // The pager shows each printed page in turn.
  await expect(page.getByTestId("snf-page-label")).toHaveText("1 / 3")
  await page.getByTestId("snf-page-next").click()
  await expect(page.getByTestId("snf-page-label")).toHaveText("2 / 3")
  expect(await scene(preview)).toBe(scenes[1])

  // Removing pages moves the preview back to a page that still exists.
  await page.getByTestId("snf-page-next").click()
  await page.getByTestId("snf-pages").locator("button").first().click()
  await expect(page.getByTestId("snf-page-label")).toHaveText("2 / 2")

  // The link restores the page count.
  await page.goto(page.url())
  await startPrinting(page)
  await expect(page.locator(".print-only .printable")).toHaveCount(2)
})

test("print mode: themes are a multi-select, pages go through them in rounds", async ({ page }) => {
  await page.goto("/activity/seek-and-find/print")
  const all = page.getByTestId("snf-theme-all")
  await expect(all).toHaveClass(/is-active/)

  // Two themes and five pages: each round uses both themes once.
  await page.getByTestId("snf-theme-ocean").click()
  await page.getByTestId("snf-theme-space").click()
  await expect(all).not.toHaveClass(/is-active/)
  const plus = stepperPlus(page, "snf-pages")
  for (let i = 0; i < 4; i++) await plus.click()
  await startPrinting(page)
  await expect(page).toHaveURL(/\/print\/find\.ocean-space\..*\.p5$/)
  const pageThemes = () => page.locator(".print-only .snf-print").evaluateAll(
    es => es.map(e => e.getAttribute("data-theme")))
  for (let n = 0; n < 5; n++) {
    const t = await pageThemes()
    expect(t).toHaveLength(5)
    expect(new Set([t[0], t[1]])).toEqual(new Set(["ocean", "space"]))
    expect(new Set([t[2], t[3]])).toEqual(new Set(["ocean", "space"]))
    for (let i = 1; i < 5; i++) expect(t[i]).not.toBe(t[i - 1])
    await page.getByRole("button", { name: "New scene" }).click()
  }

  // All themes, six pages: every theme once, no repeat.
  await all.click()
  await plus.click()
  expect(new Set(await pageThemes()).size).toBe(6)

  // Selecting every theme one by one is the same as "All".
  for (const t of ["ocean", "forest", "farm", "space", "sweets", "garden"]) {
    await page.getByTestId(`snf-theme-${t}`).click()
  }
  await expect(all).toHaveClass(/is-active/)
  await expect(page.getByTestId("snf-theme-ocean")).not.toHaveClass(/is-active/)

  // Removing the last selected theme also gives "All".
  await page.getByTestId("snf-theme-farm").click()
  await page.getByTestId("snf-theme-farm").click()
  await expect(all).toHaveClass(/is-active/)
})
