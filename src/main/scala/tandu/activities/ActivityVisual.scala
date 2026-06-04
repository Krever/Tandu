package tandu.activities

/** Per-activity glyph + tint used by the home grid.
  *
  * Kept as a sidecar map (rather than fields on the Activity trait) to
  * preserve the "thin shared layer" principle: visual presentation is
  * a property of the catalog, not the gameplay.
  */
object ActivityVisual:

  final case class Visual(glyph: String, tint: String)

  private val byId: Map[String, Visual] = Map(
    "battleships"      -> Visual("⊕", "vermilion"),
    "solitaire"        -> Visual("♠",  "olive"),
    "tic-tac-toe"      -> Visual("✕",  "rose"),
    "memory"           -> Visual("◉",  "teal"),
    "hangman"          -> Visual("Ⓐ",  "peach"),
    "checkers"         -> Visual("●",  "mustard"),
    "chess"            -> Visual("♞",  "sky"),
    "sudoku"           -> Visual("#",  "plum"),
    "minesweeper"      -> Visual("✺",  "vermilion"),
    "word-association" -> Visual("≈",  "rose"),
    "categories"       -> Visual("◫",  "olive"),
    "twenty-questions" -> Visual("?",  "mustard"),
    "story-building"   -> Visual("✦",  "plum"),
    "last-letter"      -> Visual("Z",  "peach"),
    "would-you-rather" -> Visual("⇆",  "sky"),
    "word-builder"     -> Visual("🔤", "teal"),
    "math-practice"    -> Visual("➕", "mustard"),
    "clock"            -> Visual("🕐", "sky"),
    "reading"          -> Visual("📖", "plum")
  )

  // Every catalog activity must declare a visual. Validated eagerly so a new
  // activity without an entry fails loudly at startup rather than silently
  // rendering a shared fallback glyph.
  private val missing: List[String] = Registry.all.map(_.id).filterNot(byId.contains)
  require(missing.isEmpty, s"Activities missing an ActivityVisual entry: ${missing.mkString(", ")}")

  def get(id: String): Visual = byId(id)
