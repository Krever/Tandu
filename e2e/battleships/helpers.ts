import { test, expect, Page, BrowserContext, Browser } from "@playwright/test"

/** Capture console messages and page errors from a page, tagged with a
  * label, so multi-context tests can attribute logs to a side. */
export function attachLogging(page: Page, label: string) {
  page.on("console", msg => {
    // eslint-disable-next-line no-console
    console.log(`[${label}/${msg.type()}] ${msg.text()}`)
  })
  page.on("pageerror", err => {
    // eslint-disable-next-line no-console
    console.log(`[${label}/pageerror] ${err.message}\n${err.stack ?? ""}`)
  })
  page.on("requestfailed", req => {
    // eslint-disable-next-line no-console
    console.log(`[${label}/requestfailed] ${req.method()} ${req.url()} — ${req.failure()?.errorText ?? ""}`)
  })
}

export async function openLobby(ctx: BrowserContext, label: string): Promise<Page> {
  const page = await ctx.newPage()
  attachLogging(page, label)
  await page.goto("/activity/battleships")
  await page.getByTestId("mode-p2p").click()
  await expect(page.getByTestId("p2p-lobby")).toBeVisible()
  return page
}

export interface P2PPair {
  hostPage: Page
  guestPage: Page
  cleanup: () => Promise<void>
}

/** Open two browser contexts, walk both through the lobby, and wait
  * for them to land in the game view. Used as the shared setup for
  * any P2P gameplay test. */
const CONNECT_TIMEOUT_MS = 30_000

export async function connectP2PPair(browser: Browser): Promise<P2PPair> {
  const [hostCtx, guestCtx] = await Promise.all([
    browser.newContext(),
    browser.newContext(),
  ])

  const [hostPage, guestPage] = await Promise.all([
    openLobby(hostCtx, "host"),
    openLobby(guestCtx, "guest"),
  ])

  await hostPage.getByTestId("p2p-create").click()
  const codeEl = hostPage.getByTestId("p2p-code")
  await expect(codeEl).toBeVisible()
  const code = (await codeEl.textContent())?.trim()
  expect(code).toMatch(/^\d{4}$/)

  await guestPage.getByTestId("p2p-join").click()
  await guestPage.getByTestId("p2p-code-input").fill(code!)
  await guestPage.getByTestId("p2p-connect").click()

  await Promise.all([
    expect(hostPage.getByTestId("p2p-game")).toBeVisible({ timeout: CONNECT_TIMEOUT_MS }),
    expect(guestPage.getByTestId("p2p-game")).toBeVisible({ timeout: CONNECT_TIMEOUT_MS }),
  ])

  return {
    hostPage,
    guestPage,
    cleanup: async () => {
      await Promise.all([hostCtx.close(), guestCtx.close()])
    }
  }
}

// ---------- battleships gameplay helpers ----------

/** Click a specific enemy-board cell by board coordinates ("x,y"). */
export async function shoot(page: Page, cell: string): Promise<void> {
  await page.locator(`[data-testid="enemy-board"] [data-cell="${cell}"]`).click()
}

export async function clickEndTurn(page: Page): Promise<void> {
  await page.getByTestId("end-turn").click()
}

/** Wait until the cell we just shot displays a resolved kind. Catches
  * regressions where a shot is "sent" but never lands visually. */
export async function expectCellResolved(page: Page, cell: string): Promise<void> {
  const c = page.locator(`[data-testid="enemy-board"] [data-cell="${cell}"]`)
  await expect(c).toHaveAttribute("data-kind", /hit|miss|sunk/)
}

// ---------- phase assertions ----------

export async function expectPassPhonePhase(page: Page, phase: string): Promise<void> {
  await expect(page.getByTestId("bs-game")).toHaveAttribute("data-bs-phase", phase)
}

export async function expectP2PPhase(page: Page, phase: string, timeoutMs = 10_000): Promise<void> {
  await expect(page.getByTestId("p2p-game")).toHaveAttribute("data-p2p-phase", phase, { timeout: timeoutMs })
}
