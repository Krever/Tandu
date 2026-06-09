import { test, expect } from "@playwright/test"

// The generator traces real emoji glyphs at runtime, so these tests double as
// a smoke check that the trace→simplify pipeline yields a playable puzzle on
// this platform's emoji font.

test("in-app puzzle generates, plays through, and reveals the picture", async ({ page }) => {
  await page.goto("/activity/dot-to-dot/in-app")
  const board = page.locator(".dots-svg")
  await expect(board).toBeVisible({ timeout: 15_000 })

  const dots = page.locator(".dots-dot")
  const n = await dots.count()
  expect(n).toBeGreaterThanOrEqual(15) // MinDots guard rail (18, star fallback 10)
  expect(n).toBeLessThanOrEqual(60)    // MaxDots guard rail

  await page.screenshot({ path: "/tmp/dots-inapp-before.png" })

  // Tapping out of order must not advance the line.
  await dots.nth(Math.min(5, n - 1)).dispatchEvent("pointerdown")
  await expect(board).not.toHaveClass(/is-done/)

  // Tap every dot in numbering order (DOM order == numbering order).
  for (let i = 0; i < n; i++) {
    await dots.nth(i).dispatchEvent("pointerdown")
  }
  await expect(board).toHaveClass(/is-done/)
  await page.waitForTimeout(700) // reveal fade
  await page.screenshot({ path: "/tmp/dots-inapp-done.png" })
})

test("print mode fills an A4 sheet with two puzzles", async ({ page }) => {
  await page.goto("/activity/dot-to-dot/print")
  await page.evaluate(() => { window.print = () => {} })
  await page.locator(".dots-print-actions button").click()

  await expect(page.locator(".dots-print-board")).toHaveCount(2)
  // Each printable board carries dots, numbers, and the detail line-art.
  const first = page.locator(".dots-print-board").first()
  expect(await first.locator(".dots-print-mark").count()).toBeGreaterThanOrEqual(15)
  expect(await first.locator(".dots-print-num").count()).toBeGreaterThanOrEqual(15)

  // The two puzzles must differ (lastEmoji guard).
  const emoji = await page.locator(".dots-print-board").evaluateAll(
    bs => bs.map(b => b.getAttribute("data-emoji")))
  expect(new Set(emoji).size).toBe(2)

  await page.emulateMedia({ media: "print" })
  await page.setViewportSize({ width: 794, height: 1123 }) // A4 @ 96dpi
  await page.screenshot({ path: "/tmp/dots-print.png", fullPage: true })
})
