import { test, expect } from "@playwright/test"

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

test("print mode fills a sheet with six emoji puzzles with legends", async ({ page }) => {
  await page.goto("/activity/sudoku/print")
  await page.evaluate(() => { window.print = () => {} })
  await page.locator(".sudoku-print-actions button").first().click() // emoji 4×4

  await expect(page.locator(".sudoku-print-board")).toHaveCount(6)
  await expect(page.locator(".sudoku-print-legend")).toHaveCount(6)
  const legend = (await page.locator(".sudoku-print-legend").first().textContent())!.trim()
  expect(legend.split(/\s+/)).toHaveLength(4)

  await page.emulateMedia({ media: "print" })
  await page.setViewportSize({ width: 794, height: 1123 }) // A4 @ 96dpi
  await page.screenshot({ path: "/tmp/sudoku-print-emoji.png", fullPage: true })
})
