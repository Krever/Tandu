import { defineConfig } from "vite";
import scalaJSPlugin from "@scala-js/vite-plugin-scalajs";
import { VitePWA } from "vite-plugin-pwa";

export default defineConfig({
  plugins: [
    scalaJSPlugin(),
    VitePWA({
      registerType: "autoUpdate",
      workbox: {
        globPatterns: ["**/*.{js,css,html,ico,png,svg,woff2}"]
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
