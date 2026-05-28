import { defineConfig, devices } from "@playwright/test"

export default defineConfig({
  testDir: "./e2e",
  // P2P tests need the dev server to be ready before they run.
  // Real-time room signalling is slow on first connect, so be generous.
  timeout: 60_000,
  expect: { timeout: 15_000 },

  // P2P tests share public signaling infra (torrent trackers, nostr
  // relays) that gets cranky if multiple rooms negotiate at once.
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: [["list"], ["html", { open: "never" }]],

  use: {
    baseURL: "http://localhost:5173",
    trace: "retain-on-failure",
    video: "retain-on-failure",
    screenshot: "only-on-failure",
  },

  projects: [
    { name: "chromium", use: { ...devices["Desktop Chrome"] } },
  ],

  webServer: {
    command: "npm run dev",
    url: "http://localhost:5173",
    reuseExistingServer: !process.env.CI,
    timeout: 120_000,
  },
})
