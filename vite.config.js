import { execSync } from "node:child_process";
import { defineConfig } from "vite";
import scalaJSPlugin from "@scala-js/vite-plugin-scalajs";
import { VitePWA } from "vite-plugin-pwa";

// Build stamp embedded in the UI: "{commit-count}-{short-hash}", with a
// trailing "+" when built from a dirty tree. The "+" is deliberately subtle —
// developers recognise it as "and some uncommitted changes" (semver
// build-metadata / git-describe convention) while users just see a code.
// Runs locally at build time (deploy.sh builds here, then uploads dist/), so
// git is always available; falls back to "dev" if it somehow isn't.
function appVersion() {
  try {
    const git = (c) => execSync(c, { stdio: ["ignore", "pipe", "ignore"] }).toString().trim();
    const count = git("git rev-list --count HEAD");
    const hash = git("git rev-parse --short HEAD");
    const dirty = git("git status --porcelain") !== "";
    return `${count}-${hash}${dirty ? "+" : ""}`;
  } catch {
    return "dev";
  }
}

export default defineConfig({
  define: { __APP_VERSION__: JSON.stringify(appVersion()) },
  plugins: [
    scalaJSPlugin(),
    VitePWA({
      registerType: "autoUpdate",
      workbox: {
        // Audio is deliberately absent here: built-in Freeze Dance loops and any
        // pasted audio are NOT precached (they'd bloat the install most users
        // never use). Instead they're cached at runtime, on first play, so the
        // feature still works offline afterwards.
        globPatterns: ["**/*.{js,css,html,ico,png,svg,woff2}"],
        runtimeCaching: [
          {
            urlPattern: ({ request }) => request.destination === "audio",
            handler: "CacheFirst",
            options: {
              cacheName: "tandu-audio",
              expiration: { maxEntries: 16, maxAgeSeconds: 60 * 60 * 24 * 30 },
              cacheableResponse: { statuses: [0, 200] },
              rangeRequests: true
            }
          }
        ]
      },
      manifest: {
        name: "Tandu",
        short_name: "Tandu",
        description: "Pick an activity to play with the kids",
        lang: "en",
        theme_color: "#d94f2a",
        background_color: "#f8fafc",
        display: "standalone",
        orientation: "portrait",
        start_url: "/",
        icons: [
          { src: "/pwa-192x192.png", sizes: "192x192", type: "image/png" },
          { src: "/pwa-512x512.png", sizes: "512x512", type: "image/png" },
          { src: "/pwa-512x512.png", sizes: "512x512", type: "image/png", purpose: "maskable" }
        ]
      }
    })
  ]
});
