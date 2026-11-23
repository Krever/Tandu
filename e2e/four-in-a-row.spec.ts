import { test, expect, Page } from "@playwright/test"

const COLS = 7
const ROWS = 6

// Tap the top cell of a column; the disc drops to the lowest free cell.
const dropIn = (page: Page, col: number) =>
  page.locator(`.c4-cell[data-col="${col}"]`).first().click()

test("two players: discs stack and four in a column wins", async ({ page }) => {
  await page.goto("/activity/four-in-a-row/in-app/friend")
  const cells = page.locator(".c4-cell")
  await expect(cells).toHaveCount(COLS * ROWS)

  // First disc lands on the bottom row.
  await dropIn(page, 3)
  await expect(cells.nth((ROWS - 1) * COLS + 3).locator(".c4-disc--p1")).toBeVisible()

  // P1 stacks column 3, P2 answers in column 4: P1 completes four vertically.
  for (const col of [4, 3, 4, 3, 4, 3]) await dropIn(page, col)

  await expect(page.locator(".c4-board .c4-disc--p1")).toHaveCount(4)
  await expect(page.locator(".c4-board .c4-disc--p2")).toHaveCount(3)
  await expect(page.locator(".cell--win")).toHaveCount(4)
  await expect(page.getByTestId("c4-status")).toContainText("Player 1")

  // The board is locked once the game is over.
  await dropIn(page, 0)
  await expect(page.locator(".c4-board .c4-disc")).toHaveCount(7)

  await page.getByRole("button", { name: "Play again" }).click()
  await expect(page.locator(".c4-board .c4-disc")).toHaveCount(0)
})

test("vs computer: the computer replies to each move", async ({ page }) => {
  await page.goto("/activity/four-in-a-row/in-app/computer")
  await dropIn(page, 3)
  await expect(page.locator(".c4-board .c4-disc--p1")).toHaveCount(1)
  await expect(page.locator(".c4-board .c4-disc--p2")).toHaveCount(1)

  // A second tap while the computer is "thinking" is ignored.
  await dropIn(page, 0)
  await dropIn(page, 0)
  await expect(page.locator(".c4-board .c4-disc--p2")).toHaveCount(2)
  await expect(page.locator(".c4-board .c4-disc--p1")).toHaveCount(2)
})

test("offers in-app and paper modes", async ({ page }) => {
  await page.goto("/activity/four-in-a-row")
  await expect(page.getByTestId("mode-in-app")).toBeVisible()
  await page.getByTestId("mode-paper").click()
  await expect(page.getByText("Draw a grid 7 squares wide and 6 tall.")).toBeVisible()
})
