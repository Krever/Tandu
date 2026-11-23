import { test, expect } from "@playwright/test"
import {
  shoot,
  clickEndTurn,
  expectCellResolved,
  expectPassPhonePhase,
} from "./helpers"

/** Plays three full turn cycles in pass-and-play mode. Each cycle:
  *   shoot → resolved → end turn → next player.
  * Catches state-machine regressions like the bug where the receiver
  * of an `endTurn` message never transitioned to MyTurn. */
test("pass-and-play: turn alternation across three cycles", async ({ page }) => {
  await page.goto("/activity/battleships")
  await page.getByTestId("mode-in-app").click()

  await expectPassPhonePhase(page, "awaiting-1")

  // Round 1 — P1 fires (0,0), resolves, hands off to P2.
  await shoot(page, "0,0")
  await expectCellResolved(page, "0,0")
  await expectPassPhonePhase(page, "resolved-1")
  await clickEndTurn(page)
  await expectPassPhonePhase(page, "awaiting-2")

  // Round 2 — P2 fires (0,0) on their own enemy board, resolves, back to P1.
  await shoot(page, "0,0")
  await expectCellResolved(page, "0,0")
  await expectPassPhonePhase(page, "resolved-2")
  await clickEndTurn(page)
  await expectPassPhonePhase(page, "awaiting-1")

  // Round 3 — different cell to ensure shotsFired bookkeeping still works.
  await shoot(page, "9,9")
  await expectCellResolved(page, "9,9")
  await expectPassPhonePhase(page, "resolved-1")
  await clickEndTurn(page)
  await expectPassPhonePhase(page, "awaiting-2")
})

/** The result screen is the hand-off screen: the device can go to the
  * other player while it shows. Thus the shooter's own board and its
  * toggle must not be available there (issue #1). */
test("pass-and-play: own board is not available after the shot", async ({ page }) => {
  await page.goto("/activity/battleships")
  await page.getByTestId("mode-in-app").click()
  await expectPassPhonePhase(page, "awaiting-1")

  const toggle = page.getByTestId("toggle-my-board")
  const ownGrid = page.locator('[data-testid="my-board"] .bs-grid')

  // P1 shows their own board, then fires.
  await toggle.click()
  await expect(ownGrid).toBeVisible()
  await shoot(page, "0,0")
  await expectPassPhonePhase(page, "resolved-1")

  // The result is shown: no own board and no toggle.
  await expect(ownGrid).toHaveCount(0)
  await expect(toggle).toHaveCount(0)

  // P2 gets the toggle back, with their board hidden.
  await clickEndTurn(page)
  await expectPassPhonePhase(page, "awaiting-2")
  await expect(toggle).toBeVisible()
  await expect(ownGrid).toHaveCount(0)
})
