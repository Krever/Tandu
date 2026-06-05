package tandu

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal

/** Build stamp injected by Vite's `define` (see vite.config.js). Vite replaces
  * every textual occurrence of `__APP_VERSION__` — including in the compiled
  * Scala.js output — with a string literal, so this read compiles down to the
  * constant and never hits an undefined global. */
@js.native @JSGlobal("__APP_VERSION__")
private val raw: String = js.native

object Version:
  /** Format: "{commit-count}-{short-hash}", with a trailing "+" for a build
    * made from a dirty working tree. */
  val current: String = raw
