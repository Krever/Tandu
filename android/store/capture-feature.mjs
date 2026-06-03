// Render the feature-graphic HTML to an exact 1024x500 PNG using the real
// Fraunces font. Run: node android/store/capture-feature.mjs
import { chromium } from '@playwright/test';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';

const here = dirname(fileURLToPath(import.meta.url));
const html = resolve(here, 'feature-graphic.html');
const out = resolve(here, 'feature-graphic.png');

const browser = await chromium.launch();
const page = await browser.newPage({ viewport: { width: 1024, height: 500 } });
await page.goto('file://' + html, { waitUntil: 'networkidle' });
await page.evaluate(() => document.fonts.ready);
await page.screenshot({ path: out });
await browser.close();
console.log('wrote ' + out);
