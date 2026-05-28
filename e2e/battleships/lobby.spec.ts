import { test, expect } from "@playwright/test"

test("P2P lobby is reachable with create/join buttons", async ({ page }) => {
  await page.goto("/activity/battleships")
  await expect(page.locator(".app")).toBeVisible({ timeout: 15_000 })

  await page.getByTestId("mode-p2p").click()

  await expect(page.getByTestId("p2p-lobby")).toBeVisible()
  await expect(page.getByTestId("p2p-create")).toBeVisible()
  await expect(page.getByTestId("p2p-join")).toBeVisible()
})
