import { defineConfig } from "vite";
import scalaJSPlugin from "@scala-js/vite-plugin-scalajs";
import { VitePWA } from "vite-plugin-pwa";

export default defineConfig({
  plugins: [
    scalaJSPlugin(),
    VitePWA({
      registerType: "autoUpdate",
      manifest: {
        name: "Tandu",
        short_name: "Tandu",
        description: "Pick an activity to play with the kids",
        lang: "en",
        theme_color: "#2563eb",
        background_color: "#f8fafc",
        display: "standalone",
        orientation: "portrait",
        start_url: "/",
        icons: []
      }
    })
  ]
});
