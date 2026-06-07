import { test, expect } from "@playwright/test"

// Smoke test for the Freeze Dance built-in synth: it builds a fairly large Web
// Audio graph on a real audio clock. Guards against runtime errors there — it
// can't judge whether the music sounds good, only that it runs. Also checks the
// phase-based layout: the stage appears only once the game starts.
test("freeze dance: built-in synth starts and runs without errors", async ({ page }) => {
  const errors: string[] = []
  page.on("pageerror", e => errors.push(`pageerror: ${e.message}`))
  page.on("console", m => {
    if (m.type() === "error") errors.push(`console.error: ${m.text()}`)
  })

  await page.goto("/activity/freeze-dance")

  // Built-in is the default source; the stage is hidden until we start.
  const start = page.getByRole("button", { name: /Start the music/ })
  await expect(start).toBeVisible({ timeout: 15_000 })
  await expect(page.locator(".freeze-stage")).toBeHidden()

  await start.click()

  // Stage now shows and is animating; a trusted click is the gesture Web Audio needs.
  await expect(page.locator(".freeze-stage.is-dancing")).toBeVisible()

  // Let the lookahead scheduler run across several bars + a loop regeneration.
  await page.waitForTimeout(3_000)

  await page.getByRole("button", { name: /Stop/ }).click()
  await expect(page.locator(".freeze-stage")).toBeHidden()
  await expect(start).toBeVisible()

  expect(errors, `unexpected runtime errors:\n${errors.join("\n")}`).toEqual([])
})
