import { test } from "@playwright/test"
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
