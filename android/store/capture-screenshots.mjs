// Capture raw Play Store screenshots from the running app (vite dev on :5173).
// Phone: 1080x1920 (9:16). Tablet: 1600x2560 cropped to 9:16 → see TABLET.
// Run with the dev server up:  node android/store/capture-screenshots.mjs
import { chromium } from '@playwright/test';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';
import { mkdirSync } from 'node:fs';

const here = dirname(fileURLToPath(import.meta.url));
const base = 'http://localhost:5173';

const screens = [
  { name: '1-home',         path: '/' },
  { name: '2-chess',        path: '/activity/chess/in-app' },
  { name: '3-word-builder', path: '/activity/word-builder/in-app' },
  { name: '4-sudoku',       path: '/activity/sudoku/in-app/easy' },
  { name: '5-would-you-rather', path: '/activity/would-you-rather' },
  { name: '6-hangman',      path: '/activity/hangman/in-app' },
  { name: '7-print-worksheet', path: '/activity/categories/print', print: true },
];

// [label, dir, cssWidth, cssHeight, scale, isMobile] → device px = css * scale
const targets = [
  ['phone',  'phone',  540,  960,  2, true],  // → 1080x1920 portrait, mobile layout
];

const browser = await chromium.launch();
for (const [label, dir, w, h, scale, isMobile] of targets) {
  const outDir = resolve(here, dir);
  mkdirSync(outDir, { recursive: true });
  const ctx = await browser.newContext({
    viewport: { width: w, height: h },
    deviceScaleFactor: scale,
    isMobile,
    hasTouch: true,
    locale: 'en-US',
  });
  for (const s of screens) {
    const page = await ctx.newPage();
    await page.goto(base + s.path, { waitUntil: 'networkidle' });
    await page.waitForSelector('#app > *', { timeout: 15000 });
    // Print activities render the worksheet into a `print-only` slot that is
    // hidden on screen; emulating print media reveals it for the screenshot.
    if (s.print) await page.emulateMedia({ media: 'print' });
    await page.evaluate(() => document.fonts.ready);
    await page.waitForTimeout(900); // settle fonts/layout/animations
    await page.screenshot({ path: resolve(outDir, s.name + '.png') });
    console.log(`${label}: ${s.name} (${w * scale}x${h * scale})`);
    await page.close();
  }
  await ctx.close();
}
await browser.close();
