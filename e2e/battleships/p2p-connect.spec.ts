import { test, expect } from "@playwright/test"
import { connectP2PPair } from "./helpers"

test("P2P: two browsers connect via room code and both enter the game view", async ({ browser }) => {
  const { hostPage, guestPage, cleanup } = await connectP2PPair(browser)
  try {
    // Host is P1 (created), guest is P2 (joined).
    await expect(hostPage.getByTestId("p2p-game")).toHaveAttribute("data-p2p-me", "1")
    await expect(guestPage.getByTestId("p2p-game")).toHaveAttribute("data-p2p-me", "2")
  } finally {
    await cleanup()
  }
})
