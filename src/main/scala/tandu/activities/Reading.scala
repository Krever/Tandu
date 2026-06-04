package tandu.activities

import com.raquo.laminar.api.L.*
import com.raquo.laminar.api.L.svg as S
import tandu.AppState
import tandu.i18n.{Lang, Strings}
import tandu.ui.Components.s

import scala.scalajs.js.URIUtils

/** Reading-together activity: a curated list of classic children's books for
  * the current UI language, grouped by age band. Each entry links out to a
  * Goodreads search; public-domain titles additionally link to a free e-book
  * source (Standard Ebooks / Wolne Lektury / Project Gutenberg). */
object Reading extends Activity:
  val id = "reading"
  def name(s: Strings): String = s.reading.name
  def description(s: Strings): String = s.reading.description
  val minPlayers: Int = 1
  val maxPlayers: Int = Int.MaxValue
  val handsFree: Boolean = false
  val glyph: String = "📖"
  val tint: String = "plum"

  def render(): HtmlElement =
    div(
      cls := "stack-lg",
      p(cls := "muted center", child.text <-- s(_.reading.hint)),
      div(
        cls := "reading-list",
        children <-- AppState.lang.signal.map(renderForLang)
      )
    )

  private def renderForLang(lang: Lang): Seq[HtmlElement] =
    val entries = ReadingBank.entriesFor(lang)
    ReadingBank.AgeBand.values.toSeq.flatMap { band =>
      val inBand = entries.filter(_.band == band)
      if inBand.isEmpty then None
      else Some(renderSection(band, inBand))
    }

  private def renderSection(
      band: ReadingBank.AgeBand,
      entries: Vector[ReadingBank.Entry]
  ): HtmlElement =
    sectionTag(
      cls := "reading-section",
      h2(cls := "reading-band", child.text <-- s(band.label)),
      ul(
        cls := "reading-books",
        entries.map(entryView)
      )
    )

  private def entryView(e: ReadingBank.Entry): HtmlElement =
    val q = URIUtils.encodeURIComponent(s"${e.title} ${e.author}")
    val searchUrl = s"https://www.google.com/search?q=$q"
    li(
      cls := "reading-book",
      div(
        cls := "reading-book__text",
        span(cls := "reading-book__title", e.title),
        span(cls := "reading-book__author", " — ", e.author)
      ),
      div(
        cls := "reading-book__links",
        e.freeUrl.map { url =>
          a(
            cls := "reading-link reading-link--free",
            href := url,
            target := "_blank",
            rel := "noopener noreferrer",
            child.text <-- s(_.reading.freeEbook)
          )
        },
        a(
          cls := "reading-link reading-link--icon",
          href := searchUrl,
          target := "_blank",
          rel := "noopener noreferrer",
          title <-- s(_.reading.search),
          aria.label <-- s(_.reading.search),
          googleIcon
        )
      )
    )

  private def googleIcon: SvgElement =
    S.svg(
      S.xmlns    := "http://www.w3.org/2000/svg",
      S.viewBox  := "0 0 48 48",
      S.width    := "16",
      S.height   := "16",
      S.path(S.fill := "#FFC107",
        S.d := "M43.611 20.083H42V20H24v8h11.303c-1.649 4.657-6.08 8-11.303 8-6.627 0-12-5.373-12-12s5.373-12 12-12c3.059 0 5.842 1.154 7.961 3.039l5.657-5.657C34.046 6.053 29.268 4 24 4 12.955 4 4 12.955 4 24s8.955 20 20 20 20-8.955 20-20c0-1.341-.138-2.65-.389-3.917z"),
      S.path(S.fill := "#FF3D00",
        S.d := "M6.306 14.691l6.571 4.819C14.655 15.108 18.961 12 24 12c3.059 0 5.842 1.154 7.961 3.039l5.657-5.657C34.046 6.053 29.268 4 24 4 16.318 4 9.656 8.337 6.306 14.691z"),
      S.path(S.fill := "#4CAF50",
        S.d := "M24 44c5.166 0 9.86-1.977 13.409-5.192l-6.19-5.238C29.211 35.091 26.715 36 24 36c-5.202 0-9.619-3.317-11.283-7.946l-6.522 5.025C9.505 39.556 16.227 44 24 44z"),
      S.path(S.fill := "#1976D2",
        S.d := "M43.611 20.083H42V20H24v8h11.303c-.792 2.237-2.231 4.166-4.087 5.571l6.19 5.238C36.971 39.205 44 34 44 24c0-1.341-.138-2.65-.389-3.917z")
    )
