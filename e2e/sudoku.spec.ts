import { test, expect } from "@playwright/test"
import { stepperPlus, startPrinting } from "./helpers"

// Solve an n×n sudoku with boxRows×boxCols boxes by backtracking.
// Small grids (4×4, 6×6) solve instantly.
function solve(grid: number[][], boxRows: number, boxCols: number): number[][] | null {
  const n = grid.length
  const safe = (r: number, c: number, v: number) => {
    for (let i = 0; i < n; i++) if (grid[r][i] === v || grid[i][c] === v) return false
    const br = Math.floor(r / boxRows) * boxRows
    const bc = Math.floor(c / boxCols) * boxCols
    for (let rr = br; rr < br + boxRows; rr++)
      for (let cc = bc; cc < bc + boxCols; cc++)
        if (grid[rr][cc] === v) return false
    return true
  }
  const go = (idx: number): boolean => {
    if (idx === n * n) return true
    const r = Math.floor(idx / n), c = idx % n
    if (grid[r][c] !== 0) return go(idx + 1)
    for (let v = 1; v <= n; v++) {
      if (safe(r, c, v)) {
        grid[r][c] = v
        if (go(idx + 1)) return true
        grid[r][c] = 0
      }
    }
    return false
  }
  return go(0) ? grid : null
}

test("emoji 4×4 renders kid-sized and plays through to the win screen", async ({ page }) => {
  await page.goto("/activity/sudoku/in-app/emoji4")
  const board = page.locator(".sudoku-board")
  await expect(board).toBeVisible()
  await expect(board).toHaveClass(/sudoku-board--4/)

  const cells = page.locator(".sudoku-cell")
  await expect(cells).toHaveCount(16)

  // Pad: four emoji symbols + erase, and no pencil toggle for the kid variant.
  const padBtns = page.locator(".sudoku-pad__btn:not(.sudoku-pad__btn--erase)")
  await expect(padBtns).toHaveCount(4)
  const symbols = await padBtns.allTextContents()
  expect(new Set(symbols).size).toBe(4)
  for (const s of symbols) expect(s).not.toMatch(/^\d$/)
  await expect(page.locator(".sudoku-controls .btn")).toHaveCount(3) // helper + undo + new game (no pencil)

  await page.screenshot({ path: "/tmp/sudoku-emoji4.png" })

  // Read the clues off the board, solve, then tap the answer in.
  const texts = await cells.allTextContents()
  const grid = Array.from({ length: 4 }, (_, r) =>
    Array.from({ length: 4 }, (_, c) => {
      const t = texts[r * 4 + c].trim()
      return t === "" ? 0 : symbols.indexOf(t) + 1
    })
  )
  const solved = solve(grid.map(row => [...row]), 2, 2)
  expect(solved).not.toBeNull()

  // With the opt-in helper on, selecting an empty cell disables pad buttons
  // that would duplicate a symbol already in its row, column, or 2×2 box.
  // Helper off (the default) leaves everything enabled.
  const [er, ec] = (() => {
    for (let r = 0; r < 4; r++) for (let c = 0; c < 4; c++) if (grid[r][c] === 0) return [r, c]
    throw new Error("no empty cell")
  })()
  const taken = new Set<number>()
  for (let i = 0; i < 4; i++) {
    if (grid[er][i] !== 0) taken.add(grid[er][i])
    if (grid[i][ec] !== 0) taken.add(grid[i][ec])
  }
  const br = Math.floor(er / 2) * 2, bc = Math.floor(ec / 2) * 2
  for (let r = br; r < br + 2; r++)
    for (let c = bc; c < bc + 2; c++)
      if (grid[r][c] !== 0) taken.add(grid[r][c])
  await cells.nth(er * 4 + ec).click()
  for (let v = 1; v <= 4; v++) {
    await expect(padBtns.nth(v - 1)).toBeEnabled()
  }
  await page.locator(".sudoku-assist").click()
  for (let v = 1; v <= 4; v++) {
    if (taken.has(v)) await expect(padBtns.nth(v - 1)).toBeDisabled()
    else await expect(padBtns.nth(v - 1)).toBeEnabled()
  }
  await page.screenshot({ path: "/tmp/sudoku-emoji4-selected.png" })
  await page.locator(".sudoku-assist").click() // back off

  // Selecting a given clue disables the whole pad, erase included.
  const [fr, fc] = (() => {
    for (let r = 0; r < 4; r++) for (let c = 0; c < 4; c++) if (grid[r][c] !== 0) return [r, c]
    throw new Error("no fixed cell")
  })()
  await cells.nth(fr * 4 + fc).click()
  for (const btn of await page.locator(".sudoku-pad__btn").all()) {
    await expect(btn).toBeDisabled()
  }

  for (let r = 0; r < 4; r++) {
    for (let c = 0; c < 4; c++) {
      if (grid[r][c] !== 0) continue
      await cells.nth(r * 4 + c).click()
      await padBtns.nth(solved![r][c] - 1).click()
    }
  }
  await expect(page.locator(".handoff")).toBeVisible()
  await page.screenshot({ path: "/tmp/sudoku-emoji4-won.png" })
})

test("emoji 6×6 renders with 2×3 boxes and a seven-button pad", async ({ page }) => {
  await page.goto("/activity/sudoku/in-app/emoji6")
  const board = page.locator(".sudoku-board")
  await expect(board).toHaveClass(/sudoku-board--6/)
  await expect(page.locator(".sudoku-cell")).toHaveCount(36)
  await expect(page.locator(".sudoku-pad__btn")).toHaveCount(7)
  // Box seams: vertical at col 3 (one per row), horizontal at rows 2 and 4.
  await expect(page.locator(".sudoku-cell--boxl")).toHaveCount(6)
  await expect(page.locator(".sudoku-cell--boxt")).toHaveCount(12)
  await page.screenshot({ path: "/tmp/sudoku-emoji6.png" })
})

test("classic 9×9 still renders with pencil and digit pad", async ({ page }) => {
  await page.goto("/activity/sudoku/in-app/medium")
  await expect(page.locator(".sudoku-cell")).toHaveCount(81)
  await expect(page.locator(".sudoku-pad__btn")).toHaveCount(10)
  await expect(page.locator(".sudoku-controls .btn")).toHaveCount(4)
  await expect(page.locator(".sudoku-cell--boxl")).toHaveCount(18)
  await page.screenshot({ path: "/tmp/sudoku-classic.png" })
})

// The print page is a sheet builder: presets fill a form, a live preview
// shows the page, and the print matches the preview.
test.describe("print", () => {
  // Errors that Airstream catches go to the console and do not fail a
  // step. Therefore each test also checks that the console has no errors.
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

  const cells = (board) => board.locator(".sudoku-print-cell").allTextContents()

  test("presets fill the form, the preview follows, print matches it", async ({ page }) => {
    await page.goto("/activity/sudoku/print")
    const preview = page.getByTestId("sudoku-preview")
    // Easy: six 4×4 boards with shape legends.
    await expect(preview.locator(".sudoku-print-board--4")).toHaveCount(6)
    await expect(preview.locator(".sudoku-print-legend").first()).toHaveText("● ▲ ■ ✚")
    await page.getByTestId("sudoku-preset-hard").click()
    await expect(preview.locator(".sudoku-print-board")).toHaveCount(4)
    await expect(preview.locator(".sudoku-print-legend")).toHaveCount(0)
    await expect(page).toHaveURL(/\/print\/g9\.digits\.hard\.pp4$/)
    // The print copies are the preview (same seed).
    await startPrinting(page)
    expect(await cells(page.locator(".print-only .sudoku-print-board").first()))
      .toEqual(await cells(preview.locator(".sudoku-print-board").first()))
    await page.emulateMedia({ media: "print" })
    await page.setViewportSize({ width: 794, height: 1123 }) // A4 @ 96dpi
    await page.screenshot({ path: "/tmp/sudoku-print-hard.png", fullPage: true })
  })

  test("9×9 disables Shapes and switches to digits", async ({ page }) => {
    await page.goto("/activity/sudoku/print")
    await page.getByTestId("sudoku-size-9").click()
    await expect(page.getByTestId("sudoku-symbols-shapes")).toBeDisabled()
    await expect(page.getByTestId("sudoku-symbols-digits")).toHaveClass(/is-active/)
    // Kids who solve 9×9 know the digits; the board has no room for a legend.
    await expect(page.getByTestId("sudoku-preview").locator(".sudoku-print-legend")).toHaveCount(0)
  })

  test("both symbol kinds: puzzles alternate on the page", async ({ page }) => {
    await page.goto("/activity/sudoku/print")
    const preview = page.getByTestId("sudoku-preview")
    await page.getByTestId("sudoku-symbols-digits").click()
    await expect(page.getByTestId("sudoku-symbols-shapes")).toHaveClass(/is-active/)
    await expect(page.getByTestId("sudoku-symbols-digits")).toHaveClass(/is-active/)
    await expect(page).toHaveURL(/\.shapes-digits\./)
    // One page: three shape puzzles and three digit puzzles, each with
    // its legend.
    const legends = preview.locator(".sudoku-print-legend")
    await expect(legends).toHaveCount(6)
    const texts = await legends.allTextContents()
    expect(texts.filter(t => t === "● ▲ ■ ✚")).toHaveLength(3)
    expect(texts.filter(t => t === "1 2 3 4")).toHaveLength(3)
    // The print is the preview.
    await startPrinting(page)
    expect(await page.locator(".print-only .sudoku-print-legend").allTextContents()).toEqual(texts)
    // The last kind cannot be turned off; digits only keeps the digit legend.
    await page.getByTestId("sudoku-symbols-shapes").click()
    await page.getByTestId("sudoku-symbols-digits").click()
    await expect(page.getByTestId("sudoku-symbols-digits")).toHaveClass(/is-active/)
    await expect(legends).toHaveText(Array(6).fill("1 2 3 4"))
  })

  test("the URL recipe is read back", async ({ page }) => {
    await page.goto("/activity/sudoku/print/g6.digits.medium.pp2.ans.p2")
    await expect(page.getByTestId("sudoku-size-6")).toHaveClass(/is-active/)
    await expect(page.getByTestId("sudoku-per-page-2")).toHaveClass(/is-active/)
    await expect(page.getByTestId("sudoku-answers")).toHaveAttribute("aria-pressed", "true")
    // Two puzzle pages and one answer page.
    await expect(page.getByTestId("sudoku-page-label")).toHaveText("1 / 3")
  })

  test("answers: off by default; on adds an answer page whose grids match the puzzles", async ({ page }) => {
    await page.goto("/activity/sudoku/print")
    const preview = page.getByTestId("sudoku-preview")
    // One page: the pager is hidden.
    await expect(page.getByTestId("sudoku-page-label")).toBeHidden()
    await page.getByTestId("sudoku-answers").click()
    await expect(page.getByTestId("sudoku-page-label")).toHaveText("1 / 2")
    // The givens of puzzle 1 are in answer 1, at the same cells.
    const givens = await cells(preview.locator(".sudoku-print-board").first())
    await page.getByTestId("sudoku-page-next").click()
    await expect(preview.locator(".sudoku-answer")).toHaveCount(6)
    await expect(preview.locator(".sudoku-answer__label").first()).toHaveText("1·1")
    const solved = await cells(preview.locator(".sudoku-answer").first())
    expect(solved).toHaveLength(16)
    givens.forEach((g, i) => { if (g.trim()) expect(solved[i]).toBe(g) })
    expect(solved.every(t => t.trim() !== "")).toBe(true)
    // On the answer page, answers off: a puzzle page shows.
    await page.getByTestId("sudoku-answers").click()
    await expect(preview.locator(".sudoku-print-board--4")).toHaveCount(6)
    await expect(page.getByTestId("sudoku-page-label")).toBeHidden()
  })

  test("pages: 3 pages with answers print 3 puzzle pages and 1 answer page", async ({ page }) => {
    await page.goto("/activity/sudoku/print")
    await page.getByTestId("sudoku-answers").click()
    await stepperPlus(page, "sudoku-pages").click()
    await stepperPlus(page, "sudoku-pages").click()
    await expect(page.getByTestId("sudoku-page-label")).toHaveText("1 / 4")
    await startPrinting(page)
    await expect(page.locator(".print-only .sudoku-print-sheet")).toHaveCount(3)
    await expect(page.locator(".print-only .sudoku-answer")).toHaveCount(18)
  })
})
