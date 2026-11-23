// Builds the two picture fonts that draw the emoji in Tandu.
//
//   node scripts/picture-fonts.mjs          Build the fonts again.
//   node scripts/picture-fonts.mjs --check  Fail if the fonts are out of date.
//
// The script finds each emoji in the Scala sources. Then it cuts the Noto
// fonts down to those emoji only. Thus the fonts stay small, and the app
// shows the same vector picture on all devices.
//
// Run the script again after you add a new emoji to the sources. If you do
// not, the new emoji falls back to the emoji font of the device.
//
// The build needs `uv` (https://docs.astral.sh/uv/). The check does not.
import { execFileSync } from "node:child_process";
import { createHash } from "node:crypto";
import fs from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const srcDir = path.join(root, "src/main/scala");
const outDir = path.join(root, "fonts");
const cacheDir = path.join(root, "node_modules/.cache/picture-fonts");
const manifestFile = path.join(outDir, "pictures.txt");
const fontFiles = ["pictures-color.woff2", "pictures-mono.woff2"];

const sources = {
  // Vector colour font (COLRv1).
  color: "https://github.com/google/fonts/raw/main/ofl/notocoloremoji/NotoColorEmoji-Regular.ttf",
  // Monochrome font, for black-and-white print.
  mono: "https://github.com/google/fonts/raw/main/ofl/notoemoji/NotoEmoji%5Bwght%5D.ttf",
  license: "https://github.com/google/fonts/raw/main/ofl/notoemoji/OFL.txt",
};

async function scalaFiles(dir) {
  const entries = await fs.readdir(dir, { withFileTypes: true });
  const nested = await Promise.all(
    entries.map((e) => {
      const p = path.join(dir, e.name);
      return e.isDirectory() ? scalaFiles(p) : p.endsWith(".scala") ? [p] : [];
    }),
  );
  return nested.flat();
}

/** All emoji in the sources, sorted. Full sequences come first in the
  * pattern, then single pictographs that have no variation selector. */
async function scanEmoji() {
  const found = new Set();
  for (const file of await scalaFiles(srcDir)) {
    const text = await fs.readFile(file, "utf8");
    for (const m of text.matchAll(/\p{RGI_Emoji}|\p{Extended_Pictographic}/gv)) found.add(m[0]);
  }
  return [...found].sort();
}

async function download(name, url) {
  const file = path.join(cacheDir, name);
  try {
    await fs.access(file);
    return file;
  } catch {
    // Not in the cache.
  }
  const res = await fetch(url);
  if (!res.ok) throw new Error(`Download failed (${res.status}): ${url}`);
  const data = Buffer.from(await res.arrayBuffer());
  // Write to a temporary file first. An interrupted download must not leave
  // a cut-off file in the cache.
  await fs.writeFile(`${file}.part`, data);
  await fs.rename(`${file}.part`, file);
  return file;
}

/** The manifest: a hash line for each font file, then the emoji, one in each
  * line. The check compares it with the sources and with the font files, so
  * a missing or old font file is found, not only a new emoji. */
async function manifestOf(emoji) {
  const hashes = [];
  for (const name of fontFiles) {
    const data = await fs.readFile(path.join(outDir, name)).catch(() => null);
    const hash = data ? createHash("sha256").update(data).digest("hex") : "missing";
    hashes.push(`# ${name} sha256 ${hash}`);
  }
  return [...hashes, ...emoji].join("\n") + "\n";
}

const emoji = await scanEmoji();

if (process.argv.includes("--check")) {
  const current = await fs.readFile(manifestFile, "utf8").catch(() => "");
  if (current !== (await manifestOf(emoji))) {
    const old = new Set(current.split("\n"));
    const added = emoji.filter((e) => !old.has(e));
    console.error(
      `Picture fonts are out of date${added.length ? ` (new: ${added.join(" ")})` : ""}. ` +
        "Run: npm run fonts",
    );
    process.exit(1);
  }
  console.log(`Picture fonts are up to date (${emoji.length} emoji).`);
  process.exit(0);
}

// fonttools does the cut. It keeps the colour tables (COLR, CPAL), which
// other subset tools drop. `uvx` gets fonttools on demand.
function fonttools(...args) {
  execFileSync("uvx", ["--from", "fonttools[woff]", ...args], { stdio: ["ignore", "inherit", "inherit"] });
}

function subset(input, output) {
  fonttools(
    "pyftsubset",
    input,
    `--text-file=${textFile}`,
    "--flavor=woff2",
    "--layout-features=*",
    // The SVG table is a second copy of all pictures (20 MB). COLR is sufficient.
    "--drop-tables+=SVG",
    `--output-file=${output}`,
  );
}

await fs.mkdir(outDir, { recursive: true });
await fs.mkdir(cacheDir, { recursive: true });
const textFile = path.join(cacheDir, "pictures-text.txt");
await fs.writeFile(textFile, emoji.join(""));

const colorSrc = await download("NotoColorEmoji-Regular.ttf", sources.color);
const colorOut = path.join(outDir, "pictures-color.woff2");
subset(colorSrc, colorOut);

// The mono font is a variable font. Fix its weight first, then cut it.
const monoSrc = await download("NotoEmoji-wght.ttf", sources.mono);
const monoFixed = path.join(cacheDir, "NotoEmoji-400.ttf");
fonttools("fonttools", "varLib.instancer", monoSrc, "wght=400", "-o", monoFixed);
fonttools("python", path.join(root, "scripts/add-variation-selectors.py"), monoFixed, textFile);
const monoOut = path.join(outDir, "pictures-mono.woff2");
subset(monoFixed, monoOut);

await fs.copyFile(await download("OFL.txt", sources.license), path.join(outDir, "OFL.txt"));
await fs.writeFile(manifestFile, await manifestOf(emoji));

const kb = async (f) => `${Math.round((await fs.stat(f)).size / 1024)} KB`;
console.log(`${emoji.length} emoji. Colour: ${await kb(colorOut)}. Mono: ${await kb(monoOut)}.`);
