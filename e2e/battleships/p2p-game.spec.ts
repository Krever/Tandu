import { test } from "@playwright/test"
import {
  connectP2PPair,
  shoot,
  expectCellResolved,
  expectP2PPhase,
} from "./helpers"

/** Plays multiple turn cycles across two devices. P2P mode auto-advances
  * after a shot resolves: the shooter sees its cell resolved on the
  * enemy board, sends `endTurn` implicitly, and transitions straight
  * to TheirTurn. */
test("P2P: full turn cycle across two devices", async ({ browser }) => {
  const { hostPage, guestPage, cleanup } = await connectP2PPair(browser)

  try {
    await Promise.all([
      expectP2PPhase(hostPage, "my-turn"),
      expectP2PPhase(guestPage, "their-turn"),
    ])

    await shoot(hostPage, "0,0")
    await expectCellResolved(hostPage, "0,0")
    await Promise.all([
      expectP2PPhase(hostPage, "their-turn"),
      expectP2PPhase(guestPage, "my-turn"),
    ])

    await shoot(guestPage, "0,0")
    await expectCellResolved(guestPage, "0,0")
    await Promise.all([
      expectP2PPhase(guestPage, "their-turn"),
      expectP2PPhase(hostPage, "my-turn"),
    ])

    // Second full cycle on different cells exercises shot bookkeeping
    // beyond the first slot (incomingShots / enemy view both grow).
    await shoot(hostPage, "9,9")
    await expectCellResolved(hostPage, "9,9")
    await Promise.all([
      expectP2PPhase(hostPage, "their-turn"),
      expectP2PPhase(guestPage, "my-turn"),
    ])

    await shoot(guestPage, "9,9")
    await expectCellResolved(guestPage, "9,9")
    await Promise.all([
      expectP2PPhase(guestPage, "their-turn"),
      expectP2PPhase(hostPage, "my-turn"),
    ])
  } finally {
    await cleanup()
  }
})
