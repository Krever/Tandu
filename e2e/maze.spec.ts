import { test, expect } from "@playwright/test"

// The Maze print page is a sheet builder: presets fill a form, a live
// preview shows the page, and the print matches the preview.

/** The "+" button of a stepper. */
const stepperPlus = (page, testId: string) => page.getByTestId(testId).locator("button").nth(1)
const stepperMinus = (page, testId: string) => page.getByTestId(testId).locator("button").first()

/** The print copies are built only while printing; this starts that, as
  * Ctrl+P does. */
const startPrinting = (page) => page.evaluate(() => window.dispatchEvent(new Event("beforeprint")))

/** A maze's identity: all its walls. */
const walls = (board) => board.locator("line").evaluateAll(
  es => es.map(e => ["x1", "y1", "x2", "y2"].map(a => e.getAttribute(a)).join(",")).join("|"))

const viewBox = (board) => board.locator("svg").getAttribute("viewBox")

// Errors that Airstream catches go to the console and do not fail a step,
// thus each test also checks that the console has no errors.
let consoleErrors: string[] = []

test.beforeEach(async ({ page }) => {
  consoleErrors = []
  page.on("console", m => { if (m.type() === "error") consoleErrors.push(m.text()) })
  page.on("pageerror", e => consoleErrors.push(e.message))
  await page.addInitScript(() => { window.print = () => {} })
})

test.afterEach(() => {
  expect(consoleErrors).toEqual([])
})

test("print mode: presets fill the form, the preview follows, print matches it", async ({ page }) => {
  await page.goto("/activity/maze/print")
  const preview = page.getByTestId("maze-preview")
  const boards = preview.locator(".maze-print-board")

  // Easy: 4 mazes, 10 cells wide and 12 high (the slot is tall).
  await expect(page.getByTestId("maze-preset-easy")).toHaveClass(/is-active/)
  await expect(boards).toHaveCount(4)
  expect(await viewBox(boards.first())).toBe("0 0 100 120")

  // The preview page has padding on the screen; the mazes must still fit
  // in its body, not run off the right edge.
  const body = (await preview.locator(".printable__body").boundingBox())!
  for (const b of await boards.all()) {
    const box = (await b.boundingBox())!
    expect(box.x + box.width).toBeLessThanOrEqual(body.x + body.width + 0.5)
  }

  // The printed page is the same page as the preview.
  await startPrinting(page)
  const printed = page.locator(".print-only .maze-print-board")
  await expect(printed).toHaveCount(4)
  expect(await walls(printed.first())).toBe(await walls(boards.first()))

  // 2 per page: the slot is wide, so the maze is 16 wide and 10 high.
  await page.getByTestId("maze-per-page-2").click()
  await expect(boards).toHaveCount(2)
  expect(await viewBox(boards.first())).toBe("0 0 160 100")
  await expect(page.getByTestId("maze-preset-easy")).not.toHaveClass(/is-active/)

  // Hard: one big maze.
  await page.getByTestId("maze-preset-hard").click()
  await expect(boards).toHaveCount(1)
  expect(await viewBox(boards.first())).toBe("0 0 240 290")

  await page.getByTestId("maze-print").click()
})

test("print mode: size is limited by mazes per page", async ({ page }) => {
  await page.goto("/activity/maze/print")
  // Mazes per page comes first in the form, because it limits the size.
  // Shapes come first (they say what kind of maze), then mazes per page.
  const rows = page.locator(".sb-form__row")
  await expect(rows.nth(0).getByTestId("maze-shape-square")).toBeVisible()
  await expect(rows.nth(1).getByTestId("maze-per-page-1")).toBeVisible()
  await expect(rows.nth(2).getByTestId("maze-size")).toBeVisible()
  await expect(rows.nth(3).getByTestId("maze-style-winding")).toBeVisible()

  await page.getByTestId("maze-preset-hard").click() // 1 per page
  const plus = stepperPlus(page, "maze-size")
  while (await plus.isEnabled()) await plus.click()
  await expect(page.getByTestId("maze-size")).toContainText("30")

  await page.getByTestId("maze-per-page-4").click()
  await expect(page.getByTestId("maze-size")).toContainText("16")
  await expect(plus).toBeDisabled()
})

test("print mode: the style changes the maze and is kept in the URL", async ({ page }) => {
  await page.goto("/activity/maze/print")
  const first = page.getByTestId("maze-preview").locator(".maze-print-board").first()
  await expect(page.getByTestId("maze-style-winding")).toHaveClass(/is-active/) // Easy
  const before = await walls(first)

  await page.getByTestId("maze-style-branchy").click()
  await expect(page).toHaveURL(/\/print\/s10\.branchy\.few\.pp4$/)
  await expect(page.getByTestId("maze-preset-easy")).not.toHaveClass(/is-active/)
  expect(await walls(first)).not.toBe(before)

  // The presets get harder in style too.
  await page.getByTestId("maze-preset-medium").click()
  await expect(page.getByTestId("maze-style-balanced")).toHaveClass(/is-active/)
  await page.getByTestId("maze-preset-hard").click()
  await expect(page.getByTestId("maze-style-branchy")).toHaveClass(/is-active/)
})

test("print mode: walls have the same line width at every maze size", async ({ page }) => {
  // The walls are drawn in maze units; without a fixed width they would
  // become thick on a small maze and thin on a big one.
  const wallStyle = async (url: string) => {
    await page.goto(url)
    const wall = page.getByTestId("maze-preview").locator(".maze-wall").first()
    return wall.evaluate(e => {
      const cs = getComputedStyle(e)
      return { width: cs.strokeWidth, effect: cs.getPropertyValue("vector-effect") }
    })
  }
  const small = await wallStyle("/activity/maze/print/s5.off.pp1")
  const big = await wallStyle("/activity/maze/print/s30.off.pp1")
  expect(small.effect).toBe("non-scaling-stroke")
  expect(big).toEqual(small)
})

test("print mode: the recipe is kept in the URL", async ({ page }) => {
  await page.goto("/activity/maze/print")
  await page.getByTestId("maze-preset-medium").click()
  await page.getByTestId("maze-loops-many").click()
  await expect(page).toHaveURL(/\/print\/s16\.balanced\.many\.pp4$/)

  // Opening the link restores the form.
  await page.goto(page.url())
  await expect(page.getByTestId("maze-loops-many")).toHaveClass(/is-active/)
  await expect(page.getByTestId("maze-size")).toContainText("16")

  // Bad values are clamped or ignored, not trusted.
  await page.goto("/activity/maze/print/s99.pp3.bogus")
  await expect(page.getByTestId("maze-per-page-4")).toHaveClass(/is-active/)
  await expect(page.getByTestId("maze-size")).toContainText("16")
  await expect(page.getByTestId("maze-preview").locator(".maze-print-board")).toHaveCount(4)

  // A number too long for an Int is ignored; the page still works.
  await page.goto("/activity/maze/print/s99999999999")
  await expect(page.getByTestId("maze-size")).toContainText("10")
  await expect(page.getByTestId("maze-preview").locator(".maze-print-board")).toHaveCount(4)
})

test("print mode: several pages, each with new mazes", async ({ page }) => {
  await page.goto("/activity/maze/print")
  const preview = page.getByTestId("maze-preview")
  const first = await walls(preview.locator(".maze-print-board").first())

  const plus = stepperPlus(page, "maze-pages")
  await plus.click()
  await plus.click()
  await expect(page).toHaveURL(/\.p3$/)
  await expect(page.getByTestId("maze-page-label")).toHaveText("1 / 3")
  await page.getByTestId("maze-page-next").click()
  await expect(page.getByTestId("maze-page-label")).toHaveText("2 / 3")
  expect(await walls(preview.locator(".maze-print-board").first())).not.toBe(first)

  await startPrinting(page)
  await expect(page.locator(".print-only .maze-print-board")).toHaveCount(12)
  // Adding pages keeps page 1.
  expect(await walls(page.locator(".print-only .maze-print-board").first())).toBe(first)

  // Back to one page from page 2: the preview moves to the page that is left.
  const minus = stepperMinus(page, "maze-pages")
  await minus.click()
  await minus.click()
  await expect(page.getByTestId("maze-page-label")).toHaveText("1 / 1")
  await expect(preview.locator(".maze-print-board")).toHaveCount(4)
})

// ---------- shapes ----------

const shapeOf = (sheet) => sheet.locator(".maze-print-sheet").first().getAttribute("data-shape")

test("print mode: shapes are a multi-select, pages go through them", async ({ page }) => {
  await page.goto("/activity/maze/print")
  const preview = page.getByTestId("maze-preview")
  await expect(page.getByTestId("maze-shape-square")).toHaveClass(/is-active/)

  await page.getByTestId("maze-shape-circle").click()
  await expect(page).toHaveURL(/\.square-circle$/)
  await page.getByTestId("maze-shape-square").click()
  await expect(page).toHaveURL(/\.circle$/)
  await expect(preview.locator(".maze-print-board--circle").first()).toBeVisible()

  // The last shape stays on.
  await page.getByTestId("maze-shape-circle").click()
  await expect(page.getByTestId("maze-shape-circle")).toHaveClass(/is-active/)

  // All shapes, 3 pages: 3 different shapes.
  await page.getByTestId("maze-shape-all").click()
  await expect(page.getByTestId("maze-shape-all")).toHaveClass(/is-active/)
  const plus = stepperPlus(page, "maze-pages")
  await plus.click()
  await plus.click()
  const shapes: string[] = []
  for (let i = 0; i < 3; i++) {
    shapes.push((await shapeOf(preview))!)
    if (i < 2) await page.getByTestId("maze-page-next").click()
  }
  expect(new Set(shapes).size).toBe(3)

  // The print has the same shapes, in the same order.
  await startPrinting(page)
  const printed = await page.locator(".print-only .maze-print-sheet").evaluateAll(
    es => es.map(e => e.getAttribute("data-shape")))
  expect(printed).toEqual(shapes)
})

test("print mode: circle maze has one entrance; hex maze has hexagon walls", async ({ page }) => {
  await page.goto("/activity/maze/print/s10.winding.off.pp1.circle")
  const circle = page.getByTestId("maze-preview").locator(".maze-print-board--circle")
  await expect(circle).toHaveCount(1)
  expect(await circle.locator("path.maze-wall").count()).toBeGreaterThan(0)
  // The outer wall is one arc with a gap: one path, one arc command.
  const border = circle.locator(".maze-border")
  await expect(border).toHaveCount(1)
  const d = (await border.getAttribute("d"))!
  expect(d.match(/A/g)!.length).toBe(1)

  await page.goto("/activity/maze/print/s10.winding.off.pp1.hex")
  const hex = page.getByTestId("maze-preview").locator(".maze-print-board--hex")
  await expect(hex).toHaveCount(1)
  expect(await hex.locator("line.maze-wall").count()).toBeGreaterThan(0)
})

test("print mode: a circle at 2 per page is round and fits", async ({ page }) => {
  await page.goto("/activity/maze/print/s10.winding.off.pp2.circle")
  const preview = page.getByTestId("maze-preview")
  const boards = preview.locator(".maze-print-board--circle")
  await expect(boards).toHaveCount(2)
  const body = (await preview.locator(".printable__body").boundingBox())!
  for (const b of await boards.all()) {
    const box = (await b.boundingBox())!
    expect(Math.abs(box.width - box.height)).toBeLessThanOrEqual(1)
    expect(box.x + box.width).toBeLessThanOrEqual(body.x + body.width + 0.5)
  }
})

test("print mode: a preset resets the shapes to square", async ({ page }) => {
  await page.goto("/activity/maze/print")
  await page.getByTestId("maze-shape-hex").click()
  await page.getByTestId("maze-preset-hard").click()
  await expect(page.getByTestId("maze-shape-square")).toHaveClass(/is-active/)
  await expect(page.getByTestId("maze-shape-hex")).not.toHaveClass(/is-active/)
  await expect(page).toHaveURL(/\.pp1$/)
})
