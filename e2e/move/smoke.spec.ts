import { test, expect } from "@playwright/test"

// Hot Potato reuses the Freeze Dance synth for its music bed, so it builds the
// same Web Audio graph on a real clock. This guards the start → passing → stop
// path against runtime errors; the random cut (7–22s) isn't waited out here.
test("hot potato: music starts, passing stage shows, stop returns to idle", async ({ page }) => {
  const errors: string[] = []
  page.on("pageerror", e => errors.push(`pageerror: ${e.message}`))
  page.on("console", m => {
    if (m.type() === "error") errors.push(`console.error: ${m.text()}`)
  })

  await page.goto("/activity/hot-potato")

  const start = page.getByRole("button", { name: /Start the music/ })
  await expect(start).toBeVisible({ timeout: 15_000 })
  await expect(page.locator(".potato-stage")).toBeHidden()

  // A trusted click is the gesture Web Audio needs to start.
  await start.click()
  await expect(page.locator(".potato-stage.is-passing")).toBeVisible()

  // Let the lookahead scheduler run a couple of bars.
  await page.waitForTimeout(2_000)

  await page.getByRole("button", { name: /^Stop$/ }).click()
  await expect(page.locator(".potato-stage")).toBeHidden()
  await expect(start).toBeVisible()

  expect(errors, `unexpected runtime errors:\n${errors.join("\n")}`).toEqual([])
})

// Active Games is a rules hub: a mode chooser whose tiles open per-game rules
// cards. Pure content, but verify the navigation and that a card renders steps.
test("active games: hub lists games and opens a rules card", async ({ page }) => {
  await page.goto("/activity/active-games")

  await expect(page.getByTestId("mode-floor-is-lava")).toBeVisible({ timeout: 15_000 })
  await expect(page.getByTestId("mode-tag")).toBeVisible()
  await expect(page.getByTestId("mode-hide-and-seek")).toBeVisible()
  await expect(page.getByTestId("mode-red-light")).toBeVisible()

  await page.getByTestId("mode-hide-and-seek").click()

  await expect(page.locator(".rules-card")).toBeVisible()
  await expect(page.locator(".rules-card .rules-list li").first()).toBeVisible()
})

// The kind filter is additive: every kind is shown by default, and tapping a
// chip toggles it off. Isolating the movement category means deselecting the
// other three; the movement games should then be the only ones left.
test("home: narrowing to Move shows only the movement category", async ({ page }) => {
  await page.goto("/")

  await page.getByRole("button", { name: /^Games$/ }).click()
  await page.getByRole("button", { name: /^On the go$/ }).click()
  await page.getByRole("button", { name: /^Learn$/ }).click()

  await expect(page.getByRole("button", { name: /Freeze Dance/ })).toBeVisible()
  await expect(page.getByRole("button", { name: /Hot Potato/ })).toBeVisible()
  await expect(page.getByRole("button", { name: /Active Games/ })).toBeVisible()
  // A non-movement game should now be filtered out.
  await expect(page.getByRole("button", { name: /Sudoku/ })).toHaveCount(0)
})
