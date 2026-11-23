import { expect, test } from "@playwright/test"

// The content pictures are emoji that our own fonts draw: a vector colour
// font on screen, and a mono font for black-and-white print.

test("content pictures use the picture font", async ({ page }) => {
  await page.goto("/activity/seek-and-find/seek/easy")
  const chip = page.locator(".snf-chip__emoji").first()
  await expect(chip).toBeVisible()

  const family = await chip.evaluate((el) => getComputedStyle(el).fontFamily)
  expect(family).toContain("Tandu Pictures")

  // The font file is loaded and it has the glyph. If not, the browser
  // silently shows the emoji of the device.
  const text = (await chip.textContent()) ?? ""
  const loaded = await page.evaluate(
    (t) => document.fonts.check('24px "Tandu Pictures"', t) &&
      [...document.fonts].some((f) => f.family.includes("Tandu Pictures") && f.status === "loaded"),
    text,
  )
  expect(loaded).toBe(true)
})

test("black-and-white print: the switch persists and selects the mono pictures", async ({ page }) => {
  await page.addInitScript(() => {
    ;(window as any).__printCalls = 0
    window.print = () => { ;(window as any).__printCalls++ }
  })
  await page.goto("/activity/seek-and-find/print")
  const toggle = page.getByTestId("print-bw")
  // The pictures of a seek-and-find sheet are images, in the live preview
  // and in the print.
  const previewPicture = page.getByTestId("snf-preview").locator(".snf-print-field image").first()
  const pictureAt = (el) => el.evaluate((n) => `${n.getAttribute("x")},${n.getAttribute("y")}`)

  // Colour is the default.
  await expect(toggle).toHaveAttribute("aria-pressed", "false")
  const colourSrc = await previewPicture.getAttribute("href")
  const position = await pictureAt(previewPicture)

  await toggle.click()
  await expect(toggle).toHaveAttribute("aria-pressed", "true")
  await expect(page.locator("html")).toHaveClass(/print-bw/)

  // The preview changes to the mono pictures, with the same scene.
  await expect(previewPicture).not.toHaveAttribute("href", colourSrc ?? "")
  expect(await pictureAt(previewPicture)).toBe(position)
  const monoSrc = await previewPicture.getAttribute("href")

  // The print uses the same images as the preview. The browser has decoded
  // them for the preview, so the print does not show empty pictures.
  await page.getByTestId("snf-print").click()
  const printed = page.locator(".print-only .snf-print-field image").first()
  await expect(printed).toHaveAttribute("href", monoSrc ?? "")
  // The print starts after the images are decoded, thus poll for it.
  await expect.poll(() => page.evaluate(() => (window as any).__printCalls)).toBe(1)

  // In print media, pictures that are text change to the mono font.
  await page.emulateMedia({ media: "print" })
  const printFamily = await page.evaluate(() =>
    getComputedStyle(document.documentElement).getPropertyValue("--font-pictures"))
  expect(printFamily).toContain("Tandu Pictures Mono")
  await page.emulateMedia({ media: null })

  // The setting stays after a reload.
  await page.reload()
  await expect(page.getByTestId("print-bw")).toHaveAttribute("aria-pressed", "true")
  await expect(page.locator("html")).toHaveClass(/print-bw/)
  // After the reload, the preview shows the mono pictures without a change
  // of the switch: the image is the same as after "off, then on" again.
  await expect(previewPicture).toHaveAttribute("href", /^data:image\/png/)
  await page.waitForFunction(() => document.fonts.check('16px "Tandu Pictures Mono"', "🐟") &&
    [...document.fonts].some((f) => f.family.includes("Tandu Pictures Mono") && f.status === "loaded"))
  const afterReload = await previewPicture.getAttribute("href")
  const reloadedToggle = page.getByTestId("print-bw")
  await reloadedToggle.click()
  await expect(previewPicture).not.toHaveAttribute("href", afterReload ?? "")
  await reloadedToggle.click()
  await expect(previewPicture).toHaveAttribute("href", afterReload ?? "")
})

test("a second click during the print wait does not print two times", async ({ page }) => {
  await page.addInitScript(() => {
    ;(window as any).__printCalls = 0
    window.print = () => { ;(window as any).__printCalls++ }
  })
  await page.goto("/activity/seek-and-find/print")
  const print = page.getByTestId("snf-print")
  // Two clicks in one task: the second one comes before the images of the
  // print copies are decoded.
  await print.evaluate((b: HTMLElement) => { b.click(); b.click() })
  await expect.poll(() => page.evaluate(() => (window as any).__printCalls)).toBe(1)
  await page.waitForTimeout(500)
  expect(await page.evaluate(() => (window as any).__printCalls)).toBe(1)
})
