package tandu.ui

import com.raquo.laminar.api.L.*
import tandu.{Page, Routing}
import tandu.i18n.Strings
import tandu.ui.Components.s

import scala.util.Random

/** The sheet builder of a print page: presets, a form that edits one
  * recipe value, a live preview, a page count and the Print button. The
  * preview and the printed pages come from the same recipe and the same
  * seed. Therefore the print is exactly what the preview shows.
  *
  * An activity supplies a [[Spec]]: its recipe type `C`, the page model `P`,
  * the codec of the recipe, the page generator, the sheet body and the form
  * rows. The same spec also makes the editor of a workbook row
  * ([[embedded]]) and the pages of that row ([[workbookPages]]). */
object SheetBuilder:

  val MaxPages = 10

  /** A named starting point for the form. */
  final case class Preset[C](presetId: String, name: Strings => String, config: C)

  /** What one activity supplies.
    *   - `testIdPrefix`: the prefix of all test ids (for example "snf").
    *   - `encode` / `decode`: the recipe as one URL path segment. `decode`
    *     must accept any text and give a valid recipe.
    *   - `pagesOf(c, n, seed)`: `n` pages from one seed. Page k must not
    *     change when pages are added after it.
    *   - `body`: one page body, without the printable frame.
    *   - `form`: the form rows; they read and write only the given Var.
    *   - `newLabel`: the label of the button that makes new pages. */
  final case class Spec[C, P](
      testIdPrefix: String,
      presets: List[Preset[C]],
      encode: C => String,
      decode: String => C,
      title: C => Strings => String,
      pagesOf: (C, Int, Long) => Vector[P],
      body: (C, P) => HtmlElement,
      form: Var[C] => HtmlElement,
      newLabel: Strings => String
  ):
    private[SheetBuilder] def page(c: C, p: P): HtmlElement =
      Printable.render(title = title(c), body = body(c, p))

    private[SheetBuilder] def testId(name: String): String = s"$testIdPrefix-$name"

  // ---------- codec helpers ----------

  def clamp(v: Int, lo: Int, hi: Int): Int = math.max(lo, math.min(hi, v))

  /** The enum case whose lower-case name is `t`. */
  def named[E](values: Array[E], t: String): Option[E] = values.find(_.toString.toLowerCase == t)

  /** The page count is a token in the same URL segment as the recipe
    * (`.p3`), but it is not part of the recipe: a workbook row has its own
    * page count. */
  def encodePages(n: Int): String = if n == 1 then "" else s".p$n"

  def decodePages(token: String): Int =
    token.split('.').iterator.filter(_.startsWith("p")).flatMap(_.drop(1).toIntOption).nextOption()
      .fold(1)(clamp(_, 1, MaxPages))

  // ---------- pages ----------

  /** The print mode of an activity. `intro` comes first (the rules card).
    * The recipe and the page count are kept in the URL segment after
    * "print". */
  def printPage[C, P](spec: Spec[C, P], activityId: String, intro: HtmlElement): HtmlElement =
    val urlToken = Routing.router.currentPageSignal.now() match
      case Page.Activity(`activityId`, "print" :: token :: _) => Some(token)
      case _ => None
    val config = Var(urlToken.fold(spec.presets.head.config)(spec.decode))
    val pages = Var(urlToken.fold(1)(decodePages))
    val seed = Var(Random.nextLong())
    // The page that the preview shows, 0-based. `setPages` keeps it in range.
    val shown = Var(0)
    // The print copies exist only while printing: building every page in
    // hidden DOM on each change in the form is wasted work.
    val printing = Var(false)
    val sheets = config.signal.combineWith(pages.signal, seed.signal).map { (c, n, sd) =>
      (c, spec.pagesOf(c, n, sd))
    }
    // Move the shown page into range first. Each `set` is a separate
    // transaction, and the preview must never see a shown page that is
    // not in the list of pages.
    def setPages(n: Int): Unit =
      shown.update(i => math.min(i, n - 1))
      pages.set(n)
    def pagerButton(testId: String, label: String, isDisabled: Signal[Boolean], go: Int => Int): HtmlElement =
      Components.ghost(Val(label), shown.update(go), isDisabled).amend(
        typ := "button",
        cls := "sb-pager__btn",
        dataAttr("testid") := spec.testId(testId)
      )

    div(
      cls := "stack-lg",
      // Keep the URL in step with the form, so the page can be bookmarked
      // or shared. Replace, not push: each tap must not add a history entry.
      config.signal.combineWith(pages.signal).changes --> { (c, n) =>
        Routing.replace(Page.Activity(activityId, List("print", spec.encode(c) + encodePages(n))))
      },
      // Ctrl+P and the browser menu print too, not only the Print button.
      windowEvents(_.onBeforePrint) --> (_ => printing.set(true)),
      windowEvents(_.onAfterPrint) --> (_ => printing.set(false)),
      div(cls := "no-print", intro),
      div(cls := "no-print", presetPills(spec, config)),
      div(
        cls := "sb-builder no-print",
        previewBox(
          spec,
          sheets.combineWith(shown.signal).map((c, ps, i) => spec.page(c, ps(i))),
          // Flip through the pages; hidden for a single page.
          div(
            cls := "row sb-pager",
            cls("is-hidden") <-- pages.signal.map(_ == 1),
            pagerButton("page-prev", "‹", shown.signal.map(_ == 0), _ - 1),
            span(
              cls := "sb-pager__label",
              dataAttr("testid") := spec.testId("page-label"),
              child.text <-- shown.signal.combineWith(pages.signal).map((i, n) => s"${i + 1} / $n")
            ),
            pagerButton("page-next", "›", shown.signal.combineWith(pages.signal).map((i, n) => i >= n - 1), _ + 1)
          ),
          div(
            cls := "row sb-builder__actions",
            span(cls := "sb-form__label", child.text <-- s(_.printable.pages)),
            Components.stepper(
              pages.signal,
              pages.signal.map(_ > 1),
              pages.signal.map(_ < MaxPages),
              () => setPages(pages.now() - 1),
              () => setPages(pages.now() + 1),
              testId = spec.testId("pages")
            )
          ),
          Pictures.bwToggle(),
          div(
            cls := "row sb-builder__actions",
            newButton(spec, seed),
            Components.primary(s(_.printable.print), {
              // Mounted synchronously, so the pages are there before the
              // print snapshot; beforeprint does the same for Ctrl+P.
              printing.set(true)
              Printable.print()
            }).amend(dataAttr("testid") := spec.testId("print"))
          )
        ),
        spec.form(config)
      ),
      div(
        cls := "print-only",
        children <-- printing.signal.combineWith(sheets).map { (on, c, ps) =>
          if on then ps.map(p => spec.page(c, p)) else Nil
        }
      )
    )

  /** The editor of a workbook row: presets, the form and a live preview of
    * one page. `token` is a two-way view of the row's settings, so a change
    * from outside (a book preset) shows in the form at once. */
  def embedded[C, P](spec: Spec[C, P], token: Var[String]): HtmlElement =
    val config = token.zoomLazy(spec.decode)((_, c) => spec.encode(c))
    val seed = Var(Random.nextLong())
    val sheet = config.signal.distinct.combineWith(seed.signal).map((c, sd) => spec.page(c, spec.pagesOf(c, 1, sd).head))
    div(
      cls := "sb-builder sb-builder--embedded",
      previewBox(spec, sheet, div(cls := "row sb-builder__actions", newButton(spec, seed))),
      div(cls := "stack sb-builder__form", presetPills(spec, config), spec.form(config))
    )

  /** All `n` page bodies of a workbook row, from the same generator as the
    * print page. */
  def workbookPages[C, P](spec: Spec[C, P], token: String, n: Int, rng: Random): Seq[HtmlElement] =
    val c = spec.decode(token)
    spec.pagesOf(c, n, rng.nextLong()).map(spec.body(c, _))

  /** The name of the preset that is equal to `c`, if there is one. */
  def presetName[C](spec: Spec[C, ?], c: C): Option[Strings => String] =
    spec.presets.find(_.config == c).map(_.name)

  /** The workbook row label: the preset name, or "Custom". */
  def summary[C](spec: Spec[C, ?], token: String): Strings => String =
    presetName(spec, spec.decode(token)).getOrElse(_.printable.custom)

  // ---------- parts ----------

  /** The scaled-down live page, with action rows under it. */
  private def previewBox(spec: Spec[?, ?], sheet: Signal[HtmlElement], below: HtmlElement*): HtmlElement =
    div(
      cls := "sb-builder__preview stack",
      div(
        cls := "sb-preview",
        dataAttr("testid") := spec.testId("preview"),
        div(cls := "sb-preview__page", child <-- sheet)
      ),
      below
    )

  private def newButton(spec: Spec[?, ?], seed: Var[Long]): HtmlElement =
    Components.ghost(s(spec.newLabel), seed.set(Random.nextLong())).amend(dataAttr("testid") := spec.testId("new"))

  /** The presets row. A preset is active only while the recipe is equal to
    * it; after a change in the form, no preset is active. */
  private def presetPills[C](spec: Spec[C, ?], config: Var[C]): HtmlElement =
    div(
      cls := "sb-presets",
      span(cls := "sb-form__label", child.text <-- s(_.printable.startFrom)),
      pills(spec.testId("preset"), config.signal, spec.presets.map(pr => (pr.config, pr.presetId, s(pr.name))), config.set)
    )

  // ---------- form blocks ----------

  def form(rows: Modifier[HtmlElement]*): HtmlElement = div(cls := "sb-form", rows)

  def formRow(label: Strings => String, control: HtmlElement): HtmlElement =
    div(
      cls := "sb-form__row",
      span(cls := "sb-form__label", child.text <-- s(label)),
      control
    )

  /** Exclusive choice. Each option is (value, key, label); the test id of
    * an option is `testId-key`. */
  def pills[A](
      testId: String,
      current: Signal[A],
      options: List[(A, String, Signal[String])],
      pick: A => Unit
  ): HtmlElement =
    div(
      cls := "pill-toggle",
      options.map { (value, key, label) =>
        button(
          typ := "button",
          cls := "pill-btn",
          dataAttr("testid") := s"$testId-$key",
          cls("is-active") <-- current.map(_ == value),
          child.text <-- label,
          onClick --> (_ => pick(value))
        )
      }
    )

  /** One button of a multi-select. */
  def chip(testId: String, on: Signal[Boolean], label: Signal[String], tap: () => Unit): HtmlElement =
    button(
      typ := "button",
      cls := "sb-chip-btn",
      dataAttr("testid") := testId,
      cls("is-active") <-- on,
      aria.pressed <-- on.map(_.toString),
      child.text <-- label,
      onClick --> (_ => tap())
    )

  /** A stepper for one number field; the limits may depend on other
    * fields. */
  def fieldStepper[C](config: Var[C], testId: String, get: C => Int, lo: C => Int, hi: C => Int, set: (C, Int) => C): HtmlElement =
    val c = config.signal
    Components.stepper(
      c.map(get),
      c.map(k => get(k) > lo(k)),
      c.map(k => get(k) < hi(k)),
      () => config.update(k => set(k, get(k) - 1)),
      () => config.update(k => set(k, get(k) + 1)),
      testId
    )

  /** A form row with a switch for one flag. */
  def flag[C](config: Var[C], testId: String, label: Strings => String, get: C => Boolean, flip: C => C): HtmlElement =
    formRow(label, Components.switch(config.signal.map(get), s(label), () => config.update(flip), testId))
