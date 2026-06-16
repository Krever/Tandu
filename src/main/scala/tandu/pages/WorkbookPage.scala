package tandu.pages

import com.raquo.laminar.api.L.*
import org.scalajs.dom
import tandu.{AppState, Page, Routing, Storage}
import tandu.ui.{Components, Printable}
import tandu.ui.DomExt.*
import tandu.ui.Components.s
import tandu.workbook.Workbook
import tandu.workbook.Workbook.{Codec, Ctx, Recipe, Row, Source}

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.util.{Random, Try}

/** The workbook feature: a books list (the landing screen) and a one-screen
  * editor per book. Page selection is two-step — a row is added by *kind*
  * (sudoku, maze, …) and its variant is switched in place, preview in hand,
  * because parents don't know which difficulty fits until they've seen it.
  * Every edit autosaves into the book; the name + emoji identify the book
  * everywhere.
  */
object WorkbookPage:

  /** Max copies of one row — enough for a fat book, low enough that a stuck
    * stepper can't queue a 100-page print job. */
  private val MaxCount = 5

  /** The composition-list key the pinned cover row uses in the shared
    * preview-toggle state (source rows use their index). */
  private val CoverKey = "cover"

  /** Nothing worth confirming: deleting a book with no name and no pages
    * loses nothing. */
  private def isBlank(r: Recipe): Boolean = r.rows.isEmpty && r.name.trim.isEmpty

  /** Flip a flag on, then back off after `ms` — the transient "Copied!" /
    * import-failed flash. */
  private def flash(v: Var[Boolean], ms: Int): Unit =
    v.set(true)
    val _ = js.timers.setTimeout(ms)(v.set(false))

  def render(): HtmlElement =
    val books: Var[Vector[Recipe]] =
      Var(Storage.loadWorkbookBooks().map(Codec.booksFromJson).getOrElse(Vector.empty))

    // List ↔ editor ↔ shared-import is the path after /workbook; this page
    // stays mounted across the switch so `books` survives navigation.
    val view: Signal[Page] = Routing.router.currentPageSignal.distinct

    // localStorage can refuse a write (quota, private mode, blocked storage).
    // Autosave is invisible, so a silent failure means the parent edits, walks
    // away, and loses the book — surface it instead with a standing banner.
    val saveOk: Var[Boolean] = Var(true)

    div(
      cls := "app stack-lg",
      books.signal --> (bs => saveOk.set(Storage.saveWorkbookBooks(Codec.booksToJson(bs)))),
      // .distinct: a save runs (and succeeds) on every edit, so without it the
      // banner's child rebuilds on each keystroke instead of only when the
      // save state flips.
      child <-- saveOk.signal.distinct.map {
        case true  => emptyNode
        case false => div(cls := "wbk-save-warning no-print", child.text <-- s(_.workbook.saveFailed))
      },
      child <-- view.map {
        case Page.Workbook(Some(id)) => editorView(books, id)
        case Page.WorkbookShared(p)  => importView(books, p)
        case _                       => listView(books)
      }
    )

  /** A shared link landing: import the encoded recipe (or jump to the same
    * book if it's already on the list) and open it in the editor. The work
    * runs on a fresh tick: inside the mount's render transaction a Var
    * update is deferred, so books.now() would still see the old vector. */
  private def importView(books: Var[Vector[Recipe]], payload: String): HtmlElement =
    div(
      onMountCallback { _ =>
        val _ = js.timers.setTimeout(0) {
          Codec.recipeFromShare(payload) match
            case None => Routing.replace(Page.Workbook(None))
            case Some(r) =>
              // The shared recipe carries the sender's id, so re-opening the
              // same link finds the already-imported book by id instead of
              // duplicating it — even after the recipient renamed it.
              if !books.now().exists(_.id == r.id) then books.update(_ :+ r)
              Routing.replace(Page.Workbook(Some(r.id)))
        }
      }
    )

  // ---------- books list ----------

  private def listView(books: Var[Vector[Recipe]]): HtmlElement =
    // No placeholder name and no placeholder composition: a fresh book is
    // empty, and the editor opens with the preset panel unfolded — picking a
    // starting point IS the first step. The list falls back to a muted
    // "untitled" for the name.
    def createBook(): Unit =
      val fresh = Workbook.freshRecipe()
      books.update(_ :+ fresh)
      Routing.go(Page.Workbook(Some(fresh.id)))

    // The book whose ✕ was tapped, awaiting confirmation — by id, so a list
    // that shifts under the open modal can't retarget the delete.
    val pendingDelete: Var[Option[String]] = Var(None)
    // A failed import (unreadable / not a workbook file) flashes a line.
    val importError: Var[Boolean] = Var(false)

    div(
      cls := "stack-lg",
      Components.header(s(_.workbook.name), glyph = Some("📖"), tint = Some("sky")),
      sectionTag(
        cls := "stack",
        children <-- books.signal.map { bs =>
          // No card chrome on the empty state: anything card-shaped here reads
          // as a second, dead "create" button above the real one.
          if bs.isEmpty then
            List(
              emptyArt(),
              p(cls := "muted center wbk-empty", child.text <-- s(_.workbook.noBooksYet))
            )
          else bs.map(b => bookCard(b, books, pendingDelete)).toList
        },
        Components.primaryBig(s(_.workbook.createBook), createBook()),
        libraryActions(books, importError)
      ),
      confirmDeleteModal(
        isOpen = pendingDelete.signal.map(_.isDefined),
        bookName = pendingDelete.signal.combineWith(books.signal)
          .map((p, bs) => p.flatMap(id => bs.find(_.id == id)).map(_.name).getOrElse("")),
        onCancel = () => pendingDelete.set(None),
        onConfirm = () => {
          pendingDelete.now().foreach(id => books.update(_.filterNot(_.id == id)))
          pendingDelete.set(None)
        }
      )
    )

  /** Whole-library backup: export every book as one JSON file and import it
    * back (merging by id, so re-importing your own export is a no-op). The
    * per-book share link covers "send one book"; this covers "don't lose them
    * all to a cleared cache". */
  private def libraryActions(books: Var[Vector[Recipe]], importError: Var[Boolean]): HtmlElement =
    div(
      cls := "stack wbk-library-actions no-print",
      div(
        cls := "row",
        // Hidden file input, surfaced as the label button (the Jigsaw pattern).
        label(
          cls := "btn btn--ghost wbk-import",
          child.text <-- s(_.workbook.importBooks),
          input(
            cls := "wbk-import-input",
            tpe := "file",
            accept := "application/json,.json",
            onChange --> { ev =>
              val inp = ev.target.asInstanceOf[dom.HTMLInputElement]
              val files = inp.files
              if files != null && files.length > 0 then importBooksFromFile(files(0), books, importError)
              inp.value = "" // let the same file be re-picked
            }
          )
        ),
        child <-- books.signal.map(_.isEmpty).distinct.map { isEmpty =>
          if isEmpty then emptyNode
          else
            button(
              cls := "btn btn--ghost wbk-export",
              child.text <-- s(_.workbook.exportBooks),
              onClick --> (_ => downloadJson("tandu-workbooks.json", Codec.booksToJson(books.now())))
            )
        }
      ),
      child <-- importError.signal.map {
        case false => emptyNode
        case true  => p(cls := "muted center wbk-import-error", child.text <-- s(_.workbook.importFailed))
      }
    )

  private def importBooksFromFile(
      file: dom.File,
      books: Var[Vector[Recipe]],
      importError: Var[Boolean]
  ): Unit =
    val reader = new dom.FileReader()
    reader.onload = (_: dom.Event) =>
      val imported = Codec.booksFromJson(reader.result.asInstanceOf[String])
      if imported.isEmpty then flash(importError, 4000)
      else
        importError.set(false)
        // Merge, not replace: keep what's here, add only ids not already held.
        books.update { existing =>
          val have = existing.map(_.id).toSet
          existing ++ imported.filterNot(b => have.contains(b.id))
        }
    reader.readAsText(file)

  /** Trigger a client-side download of a text blob — no backend involved. */
  private def downloadJson(filename: String, json: String): Unit =
    val blob = new dom.Blob(js.Array[dom.BlobPart](json), new dom.BlobPropertyBag { `type` = "application/json" })
    val url = dom.URL.createObjectURL(blob)
    val a = dom.document.createElement("a").asInstanceOf[dom.HTMLAnchorElement]
    a.href = url
    a.setAttribute("download", filename)
    val _ = dom.document.body.appendChild(a)
    a.click()
    val _ = dom.document.body.removeChild(a)
    dom.URL.revokeObjectURL(url)

  /** A miniature fan of "pages" — the empty list's set-piece, selling the
    * book (not print-queue) mental model before any book exists. */
  private def emptyArt(): HtmlElement =
    div(
      cls := "wbk-empty-art",
      aria.hidden := true,
      List("✏️", "#", "👀").zipWithIndex.map { (g, i) =>
        div(
          cls := s"wbk-empty-page wbk-empty-page--$i",
          span(cls := "wbk-empty-page__glyph", g),
          div(cls := "wbk-empty-page__line"),
          div(cls := "wbk-empty-page__line wbk-empty-page__line--short"),
          div(cls := "wbk-empty-page__line")
        )
      }
    )

  private def bookCard(
      b: Recipe,
      books: Var[Vector[Recipe]],
      pendingDelete: Var[Option[String]]
  ): HtmlElement =
    // What's inside, at a glance: one glyph per kind, in page order.
    val strip = b.rows
      .flatMap(row => Workbook.byId(row.sourceId))
      .map(_._1.glyph)
      .distinct
      .take(8)
    div(
      cls := "card wbk-book-card",
      button(
        cls := "wbk-book-card__main",
        span(cls := "wbk-book-card__emoji", b.coverEmoji),
        div(
          cls := "wbk-book-card__body",
          div(
            cls := "wbk-book-card__name",
            cls("wbk-book-card__name--unnamed") := b.name.trim.isEmpty,
            if b.name.trim.nonEmpty then b.name
            else child.text <-- s(_.workbook.unnamedBook)
          ),
          when(strip.nonEmpty)(
            div(
              cls := "wbk-book-card__strip",
              aria.hidden := true,
              strip.map(g => span(cls := "wbk-book-card__strip-glyph", g))
            )
          )
        ),
        span(cls := "wbk-book-card__chev", "›"),
        onClick --> (_ => Routing.go(Page.Workbook(Some(b.id))))
      ),
      button(
        cls := "wbk-book-card__delete",
        aria.label <-- s(_.workbook.deleteBook),
        "✕",
        // A blank book loses nothing — no confirmation theatre.
        onClick --> { _ =>
          if isBlank(b) then books.update(_.filterNot(_.id == b.id))
          else pendingDelete.set(Some(b.id))
        }
      )
    )

  /** Deleting a whole book is the one destructive act here — confirm it.
    * Mirrors Components.modal, with a cancel + destructive-confirm pair. */
  private def confirmDeleteModal(
      isOpen: Signal[Boolean],
      bookName: Signal[String],
      onCancel: () => Unit,
      onConfirm: () => Unit
  ): HtmlElement =
    div(
      cls := "modal-backdrop no-print",
      cls("is-open") <-- isOpen,
      onClick --> (_ => onCancel()),
      div(
        cls := "modal",
        onClick.stopPropagation --> (_ => ()),
        h2(cls := "modal__title", child.text <-- s(_.workbook.deleteBook)),
        p(
          cls := "modal__body",
          child.text <-- bookName.combineWith(AppState.strings).map { (n, str) =>
            val shown = if n.trim.nonEmpty then n.trim else str.workbook.unnamedBook
            str.workbook.deleteConfirmBody.replace("{}", shown)
          }
        ),
        div(
          cls := "modal__actions",
          button(
            cls := "btn btn--ghost",
            child.text <-- s(_.common.close),
            onClick --> (_ => onCancel())
          ),
          button(
            cls := "btn wbk-confirm-delete",
            child.text <-- s(_.common.confirm),
            onClick --> (_ => onConfirm())
          )
        )
      )
    )

  // ---------- editor ----------

  /** Copy text to the clipboard, resolving to whether it landed. The async
    * Clipboard API needs a secure context and a granted permission; where it's
    * absent or rejects (plain http, some in-app webviews) fall back to the
    * legacy execCommand path before giving up. */
  private def copyToClipboard(text: String): Future[Boolean] =
    val clip = dom.window.navigator.clipboard
    if js.isUndefined(clip) || clip == null then Future.successful(legacyCopy(text))
    else
      import scala.scalajs.js.Thenable.Implicits.thenable2future
      clip.writeText(text).map(_ => true).recover { case _ => legacyCopy(text) }

  /** The pre-Clipboard-API copy: a throwaway off-screen textarea + execCommand.
    * Works in insecure contexts and old webviews where the async API doesn't. */
  private def legacyCopy(text: String): Boolean =
    val ta = dom.document.createElement("textarea").asInstanceOf[dom.HTMLTextAreaElement]
    ta.value = text
    ta.style.position = "fixed"
    ta.style.top = "0"
    ta.style.opacity = "0"
    val _ = dom.document.body.appendChild(ta)
    ta.focus()
    ta.select()
    val ok = Try(dom.document.asInstanceOf[js.Dynamic].execCommand("copy").asInstanceOf[Boolean]).getOrElse(false)
    val _ = dom.document.body.removeChild(ta)
    ok

  private def editorView(books: Var[Vector[Recipe]], id: String): HtmlElement =
    books.now().find(_.id == id) match
      case None =>
        // Stale URL (deleted book, an id that isn't here) — bounce to the list.
        div(onMountCallback(_ => Routing.replace(Page.Workbook(None))))
      case Some(initial) =>
        val recipe: Var[Recipe] = Var(initial)
        val previewOpen: Var[Option[String]] = Var(None)
        val addOpen: Var[Boolean] = Var(false)
        // The assembled print document; built fresh on every Print tap.
        val book: Var[Option[HtmlElement]] = Var(None)
        // Generating a book full of hard puzzles blocks the main thread for a
        // beat; this drives a "Preparing…" button state so the tap registers
        // instead of seeming to do nothing.
        val preparing: Var[Boolean] = Var(false)

        def printBook(): Unit =
          if preparing.now() then () // ignore re-taps while a build is in flight
          else
            preparing.set(true)
            // Yield a frame first so the dock can paint "Preparing…" before the
            // (occasionally heavy) page generation blocks the main thread.
            val _ = js.timers.setTimeout(30) {
              book.set(Some(buildBook(recipe.now())))
              // Same grace as PrintSlot, a touch longer for the bigger DOM: let
              // Laminar commit the pages before the print dialog snapshots them.
              val _ = js.timers.setTimeout(150) {
                Printable.print()
                preparing.set(false)
              }
            }

        val confirmDelete: Var[Boolean] = Var(false)
        val shareCopied: Var[Boolean] = Var(false)
        // The share link to copy by hand when the clipboard is unreachable.
        val shareManualUrl: Var[Option[String]] = Var(None)
        val presetsOpen: Var[Boolean] = Var(false)

        def deleteBook(): Unit =
          books.update(_.filterNot(_.id == id))
          Routing.go(Page.Workbook(None))

        def shareBook(): Unit =
          val payload = Codec.recipeToShare(recipe.now())
          val url = Routing.router.absoluteUrlForPage(Page.WorkbookShared(payload))
          copyToClipboard(url).foreach { copied =>
            if copied then
              shareManualUrl.set(None)
              flash(shareCopied, 2000)
            else
              // Nothing reached the clipboard (locked-down webview, denied
              // permission) — reveal the link instead of claiming "Copied!".
              shareManualUrl.set(Some(url))
          }

        div(
          cls := "stack-lg",
          // Autosave: the book on the list IS the recipe being edited. Resolve
          // by id every time, so a list reordered/trimmed elsewhere can't make
          // this write land on the wrong slot (or on a since-deleted book).
          recipe.signal --> (r => books.update { bs =>
            val i = bs.indexWhere(_.id == id)
            if i >= 0 then bs.updated(i, r) else bs
          }),
          // The page's subject is the book itself, so its name is the title.
          Components.header(
            recipe.signal.combineWith(AppState.strings).map { (r, str) =>
              if r.name.trim.nonEmpty then r.name.trim else str.workbook.name
            },
            back = Some(Page.Workbook(None)),
            glyph = Some("📖"),
            tint = Some("sky")
          ),
          sectionTag(
            cls := "stack no-print",
            identityCard(recipe),
            rowsList(recipe, previewOpen),
            addSection(recipe, addOpen),
            // The quiet management row: presets live here too — replacing the
            // composition is book management, not a headline action.
            div(
              cls := "stack wbk-secondary",
              // The chips unfold above the row — below, the sticky Print dock
              // would sit on top of them.
              child <-- presetsOpen.signal.map {
                case false => emptyNode
                case true  => presetPanel(recipe, includeEmpty = true, onPicked = () => presetsOpen.set(false))
              },
              div(
                cls := "row wbk-secondary-actions",
                presetToggle(presetsOpen),
                button(
                  cls := "btn btn--ghost wbk-share-book",
                  child.text <-- shareCopied.signal.combineWith(AppState.strings).map { (copied, str) =>
                    if copied then str.workbook.shareCopied else s"🔗 ${str.workbook.share}"
                  },
                  onClick --> (_ => shareBook())
                ),
                button(
                  cls := "btn btn--ghost wbk-delete-book",
                  child.text <-- s(str => s"🗑 ${str.workbook.deleteBook}"),
                  // A blank book loses nothing — delete without ceremony.
                  onClick --> { _ =>
                    if isBlank(recipe.now()) then deleteBook() else confirmDelete.set(true)
                  }
                )
              ),
              // Clipboard fallback: when the copy couldn't happen, show the link
              // in a read-only field, pre-selected, so it can be copied by hand.
              child <-- shareManualUrl.signal.map {
                case None => emptyNode
                case Some(url) =>
                  div(
                    cls := "stack wbk-share-manual",
                    p(cls := "muted", child.text <-- s(_.workbook.shareManual)),
                    input(
                      cls := "wbk-input wbk-share-url",
                      readOnly := true,
                      value := url,
                      onMountCallback(c => c.thisNode.ref.select()),
                      onClick --> (e => e.currentTarget.asInstanceOf[dom.HTMLInputElement].select())
                    )
                  )
              }
            ),
            printDock(recipe, preparing.signal, () => printBook())
          ),
          confirmDeleteModal(
            isOpen = confirmDelete.signal,
            bookName = recipe.signal.map(_.name),
            onCancel = () => confirmDelete.set(false),
            onConfirm = () => {
              confirmDelete.set(false)
              deleteBook()
            }
          ),
          div(cls := "print-only", child <-- book.signal.map(_.getOrElse(div())))
        )

  // ---------- the assembled book ----------

  private def buildBook(r: Recipe): HtmlElement =
    val lang = AppState.lang.now()
    val rng = new Random()
    div(
      when(r.cover)(coverPage(r)),
      r.rows.flatMap { row =>
        Workbook.byId(row.sourceId).toList.flatMap { (_, src) =>
          (0 until row.count).map(i =>
            Printable.render(title = src.printTitle, body = src.render(Ctx(lang, rng, i)))
          )
        }
      }
    )

  /** The cover: the book's emoji and name plus the date — `printable` so the
    * shared page-break rules treat it as page one. */
  private def coverPage(r: Recipe): HtmlElement =
    // The facade's toLocaleDateString takes no locale; call dynamically so the
    // date renders in the active language's convention.
    val date = new js.Date().asInstanceOf[js.Dynamic]
      .toLocaleDateString(AppState.lang.now().code).asInstanceOf[String]
    div(
      cls := "printable wbk-cover",
      div(cls := "wbk-cover__emoji", r.coverEmoji),
      if r.name.trim.nonEmpty then h1(cls := "wbk-cover__name", r.name.trim) else emptyNode,
      div(cls := "wbk-cover__date", date),
      div(cls := "wbk-cover__brand", "Tandu")
    )

  // ---------- identity ----------

  /** The book's identity: the picture as a tap-to-change button beside the
    * name. They label the book on the list and land on the printed cover. */
  private def identityCard(recipe: Var[Recipe]): HtmlElement =
    val pickerOpen: Var[Boolean] = Var(false)
    val nameInput = input(
      cls := "wbk-input wbk-name-input",
      placeholder <-- s(_.workbook.namePlaceholder),
      controlled(
        value <-- recipe.signal.map(_.name),
        onInput.mapToValue --> (n => recipe.update(_.copy(name = n)))
      )
    )
    div(
      cls := "card wbk-identity",
      // A fresh book lands here unnamed — put the cursor where the name goes.
      onMountCallback(_ => if recipe.now().name.isEmpty then nameInput.ref.focus()),
      div(
        cls := "row wbk-identity__line",
        button(
          cls := "wbk-emoji-current",
          cls("is-open") <-- pickerOpen.signal,
          aria.label <-- s(_.workbook.emojiLabel),
          aria.expanded <-- pickerOpen.signal,
          child.text <-- recipe.signal.map(_.coverEmoji),
          onClick --> (_ => pickerOpen.update(!_))
        ),
        nameInput
      ),
      child <-- pickerOpen.signal.map {
        case false => emptyNode
        case true =>
          div(
            cls := "row wbk-emoji-row",
            role := "group",
            aria.label <-- s(_.workbook.emojiLabel),
            Workbook.coverEmojis.map { e =>
              button(
                cls := "wbk-emoji-btn",
                cls("is-active") <-- recipe.signal.map(_.coverEmoji == e),
                e,
                onClick --> { _ =>
                  recipe.update(_.copy(coverEmoji = e))
                  pickerOpen.set(false)
                }
              )
            }
          )
      }
    )

  // ---------- presets ----------

  /** Presets hide behind an explicit toggle: ever-present chips read as
    * filters, and tapping one quietly *replaces* the composition — the
    * toggle's label owns up to that. Picking one applies and folds the
    * panel away. */
  private def presetToggle(presetsOpen: Var[Boolean]): HtmlElement =
    button(
      cls := "btn btn--ghost wbk-presets-toggle",
      aria.expanded <-- presetsOpen.signal,
      resetIcon,
      span(child.text <-- s(_.workbook.replaceWithPreset)),
      onClick --> (_ => presetsOpen.update(!_))
    )

  /** A restart arrow, drawn at icon size — the ⟲ glyph renders too small. */
  private def resetIcon: SvgElement =
    svg.svg(
      svg.cls := "wbk-reset-icon",
      svg.viewBox := "0 0 24 24",
      svg.fill := "none",
      svg.stroke := "currentColor",
      svg.strokeWidth := "2",
      svg.strokeLineCap := "round",
      svg.strokeLineJoin := "round",
      svg.polyline(svg.points := "1 4 1 10 7 10"),
      svg.path(svg.d := "M3.51 15a9 9 0 1 0 2.13-9.36L1 10")
    )

  private def presetPanel(recipe: Var[Recipe], includeEmpty: Boolean, onPicked: () => Unit): HtmlElement =
    val presets = if includeEmpty then Workbook.presets else Workbook.presets.filter(_.rows.nonEmpty)
    div(
      cls := "row wbk-presets__row",
      presets.map { p =>
        button(
          cls := "pill-btn wbk-preset-chip",
          cls("is-active") <-- recipe.signal.map(_.rows == p.rows),
          child.text <-- s(p.name),
          onClick --> { _ =>
            recipe.update(_.copy(rows = p.rows))
            onPicked()
          }
        )
      }
    )

  // ---------- composition list ----------

  private def rowsList(recipe: Var[Recipe], previewOpen: Var[Option[String]]): HtmlElement =
    div(
      cls := "stack wbk-rows",
      coverRow(recipe, previewOpen),
      // Rows are addressed by index (duplicate kinds are legal — sudoku easy
      // ×2 and sudoku hard ×1 are two rows), so only a length change rebuilds
      // the list; everything inside a row is signal-driven.
      children <-- recipe.signal.map(_.rows.size).distinct.map { n =>
        if n == 0 then
          // The empty book leads with the choice itself: the hint carries the
          // preset chips (sans "Empty" — it's already empty).
          List(
            div(
              cls := "card stack wbk-empty-rows",
              p(cls := "muted center", child.text <-- s(_.workbook.empty)),
              presetPanel(recipe, includeEmpty = false, onPicked = () => ())
            )
          )
        else (0 until n).map(i => rowItem(i, recipe, previewOpen)).toList
      }
    )

  /** The shared preview affordance: an open eye, tinted while unfolded. */
  private def previewEye(isOpen: Signal[Boolean], toggle: () => Unit): HtmlElement =
    button(
      cls := "wbk-row__peek",
      cls("is-open") <-- isOpen,
      aria.label <-- s(_.workbook.previewLabel),
      aria.expanded <-- isOpen,
      Components.eyeIcon("wbk-row__eye", "wbk-row__eye-slash"),
      onClick --> (_ => toggle())
    )

  /** The cover as the book's first page in the list: pinned, toggleable,
    * never duplicated — so the page list reads as the whole book. */
  private def coverRow(recipe: Var[Recipe], previewOpen: Var[Option[String]]): HtmlElement =
    val isOpen = previewOpen.signal.map(_.contains(CoverKey)).distinct
    def togglePreview(): Unit =
      previewOpen.update(c => if c.contains(CoverKey) then None else Some(CoverKey))
    div(
      cls := "card wbk-row wbk-row--cover",
      cls("is-off") <-- recipe.signal.map(!_.cover),
      div(
        cls := "row wbk-row__line",
        button(
          cls := "wbk-row__main",
          aria.label <-- s(_.workbook.previewLabel),
          aria.expanded <-- isOpen,
          span(cls := "wbk-row__glyph wbk-row__glyph--emoji", child.text <-- recipe.signal.map(_.coverEmoji)),
          span(cls := "wbk-row__name", child.text <-- s(_.workbook.coverPage)),
          onClick --> (_ => togglePreview())
        ),
        div(
          cls := "wbk-row__end",
          previewEye(isOpen, () => togglePreview()),
          button(
            cls := "wbk-toggle",
            cls("is-on") <-- recipe.signal.map(_.cover),
            aria.pressed <-- recipe.signal.map(_.cover.toString),
            aria.label <-- s(_.workbook.coverPage),
            span(cls := "wbk-toggle__knob"),
            onClick --> (_ => recipe.update(r => r.copy(cover = !r.cover)))
          )
        )
      ),
      child <-- isOpen.map {
        case false => emptyNode
        case true  => div(cls := "wbk-preview", div(cls := "wbk-preview__page", coverPage(recipe.now())))
      }
    )

  private def rowItem(idx: Int, recipe: Var[Recipe], previewOpen: Var[Option[String]]): HtmlElement =
    val rowKey = s"row-$idx"
    val rowSig = recipe.signal.map(_.rows.lift(idx)).distinct
    val isOpen = previewOpen.signal.map(_.contains(rowKey)).distinct

    def updateRow(f: Row => Option[Row]): Unit =
      recipe.update { r =>
        r.rows.lift(idx) match
          case None => r
          case Some(row) =>
            f(row) match
              case Some(updated) => r.copy(rows = r.rows.updated(idx, updated))
              case None          => r.copy(rows = r.rows.patch(idx, Nil, 1))
      }

    // − at one removes the row; + saturates at MaxCount.
    def bump(delta: Int): Unit =
      updateRow { row =>
        val next = row.count + delta
        Option.when(next > 0)(row.copy(count = math.min(MaxCount, next)))
      }


    div(
      cls := "card wbk-row",
      child <-- rowSig.map {
        case None => emptyNode // length change re-renders the list right after
        case Some(row) =>
          Workbook.byId(row.sourceId) match
            case None => emptyNode
            case Some((group, src)) =>
              div(
                cls := "stack wbk-row__stack",
                div(
                  cls := "row wbk-row__line",
                  button(
                    cls := "wbk-row__main",
                    aria.label <-- s(_.workbook.previewLabel),
                    aria.expanded <-- isOpen,
                    span(cls := s"wbk-row__glyph activity-card--${group.tint}", group.glyph),
                    span(cls := "wbk-row__name", child.text <-- s(group.name)),
                    onClick --> (_ => previewOpen.update(c => if c.contains(rowKey) then None else Some(rowKey)))
                  ),
                  // The variant rides right next to the title; eye + stepper
                  // keep to the row's end.
                  when(group.sources.size > 1)(
                    select(
                      cls := "wbk-variant-select",
                      aria.label <-- s(group.name),
                      group.sources.map { v =>
                        option(
                          value := v.id,
                          selected := v.id == row.sourceId,
                          child.text <-- s(v.variantLabel)
                        )
                      },
                      onChange.mapToValue --> (id => updateRow(r => Some(r.copy(sourceId = id))))
                    )
                  ),
                  div(
                    cls := "wbk-row__end",
                    previewEye(isOpen, () => previewOpen.update(c => if c.contains(rowKey) then None else Some(rowKey))),
                    div(
                      cls := "wbk-stepper",
                      button(
                        cls := "wbk-stepper__btn",
                        aria.label <-- s(_.workbook.fewer),
                        "−",
                        onClick --> (_ => bump(-1))
                      ),
                      span(cls := "wbk-stepper__count", aria.live := "polite", row.count.toString),
                      button(
                        cls := "wbk-stepper__btn",
                        aria.label <-- s(_.workbook.more),
                        "+",
                        disabled := row.count >= MaxCount,
                        onClick --> (_ => bump(1))
                      )
                    ),
                    dragHandle(idx, recipe, previewOpen)
                  )
                ),
                child <-- isOpen.map {
                  case false => emptyNode
                  case true  => previewPane(src)
                }
              )
      }
    )

  /** Drag-to-reorder, iOS style: a grip at the row's end. While dragging,
    * the model is untouched — the dragged card follows the pointer and
    * displaced neighbours shift via transforms (re-rendering mid-drag would
    * replace the captured element and kill the gesture). The reorder commits
    * once, on drop. */
  private def dragHandle(idx: Int, recipe: Var[Recipe], previewOpen: Var[Option[String]]): HtmlElement =
    var dragging = false
    var startY = 0.0
    var rowEls: List[dom.HTMLElement] = Nil
    var centers: List[Double] = Nil
    var slotH = 0.0 // dragged row height + list gap: what a displaced row shifts by
    var dragged: dom.HTMLElement = null
    var listEl: dom.HTMLElement = null
    var target = idx

    def reset(): Unit =
      dragging = false
      if listEl != null then listEl.classList.remove("is-dragging-list")
      rowEls.foreach { el =>
        el.style.transform = ""
        el.classList.remove("is-dragging")
      }

    // The keyboard path: arrows swap with a neighbour. The row re-renders on
    // the swap, replacing this very element, so focus is restored onto the
    // moved row's grip a tick later.
    def keyMove(delta: Int, handle: dom.HTMLElement): Unit =
      val to = idx + delta
      // Resolve the list before the update: the swap re-renders this row,
      // detaching `handle`, after which closest() finds nothing.
      val list = handle.closest(".wbk-rows")
      recipe.update { r =>
        if to < 0 || to >= r.rows.size then r
        else r.copy(rows = r.rows.updated(idx, r.rows(to)).updated(to, r.rows(idx)))
      }
      previewOpen.set(None)
      val _ = js.timers.setTimeout(0) {
        if list != null then
          val grips = list.querySelectorAll(".wbk-row__drag")
          if to >= 0 && to < grips.length then grips(to).asInstanceOf[dom.HTMLElement].focus()
      }

    button(
      cls := "wbk-row__drag",
      aria.label <-- s(_.workbook.reorder),
      gripIcon,
      onKeyDown --> { e =>
        e.key match
          case "ArrowUp"   => e.preventDefault(); keyMove(-1, e.currentTarget.asInstanceOf[dom.HTMLElement])
          case "ArrowDown" => e.preventDefault(); keyMove(1, e.currentTarget.asInstanceOf[dom.HTMLElement])
          case _           => ()
      },
      onPointerDown --> { e =>
        e.preventDefault()
        val handle = e.currentTarget.asInstanceOf[dom.HTMLElement]
        handle.setPointerCapture(e.pointerId)
        // Previews inflate row heights unpredictably — measure with them shut.
        previewOpen.set(None)
        dragged = handle.closest(".wbk-row").asInstanceOf[dom.HTMLElement]
        listEl = dragged.parentElement
        val nodes = listEl.querySelectorAll(".wbk-row:not(.wbk-row--cover)")
        rowEls = (0 until nodes.length).map(nodes(_).asInstanceOf[dom.HTMLElement]).toList
        val rects = rowEls.map(_.getBoundingClientRect())
        centers = rects.map(r => r.top + r.height / 2)
        val gap = if rects.size > 1 then rects(1).top - rects(0).bottom else 0.0
        slotH = dragged.getBoundingClientRect().height + gap
        startY = e.clientY
        target = idx
        dragging = true
        dragged.classList.add("is-dragging")
        listEl.classList.add("is-dragging-list")
      },
      onPointerMove --> { e =>
        if dragging then
          val dy = e.clientY - startY
          dragged.style.transform = s"translateY(${dy}px)"
          val cur = centers(idx) + dy
          // The drop slot: how many other rows' (original) centres sit above
          // the dragged centre — exactly its index once removed and reinserted.
          target = centers.zipWithIndex.count((c, j) => j != idx && c < cur)
          rowEls.zipWithIndex.foreach { (el, j) =>
            if j == idx then ()
            else if idx < target && j > idx && j <= target then el.style.transform = s"translateY(${-slotH}px)"
            else if target < idx && j >= target && j < idx then el.style.transform = s"translateY(${slotH}px)"
            else el.style.transform = ""
          }
      },
      onPointerUp --> { _ =>
        if dragging then
          val to = target
          reset()
          if to != idx then
            recipe.update { r =>
              r.rows.lift(idx) match
                case None      => r
                case Some(row) => r.copy(rows = r.rows.patch(idx, Nil, 1).patch(to, Vector(row), 0))
            }
      },
      onPointerCancel --> (_ => reset())
    )

  /** The classic six-dot grip. */
  private def gripIcon: SvgElement =
    svg.svg(
      svg.cls := "wbk-row__grip",
      svg.viewBox := "0 0 24 24",
      svg.fill := "currentColor",
      for
        cy <- List("6", "12", "18")
        cx <- List("9", "15")
      yield svg.circle(svg.cx := cx, svg.cy := cy, svg.r := "1.7")
    )

  /** A freshly generated sample page, scaled down. Names like "Word Builder,
    * medium" mean nothing until you've seen the page once — and the variant
    * chips re-render it, so the parent picks difficulty by eye. */
  private def previewPane(src: Source): HtmlElement =
    div(
      cls := "wbk-preview",
      div(
        cls := "wbk-preview__page",
        src.render(Ctx(AppState.lang.now(), new Random(), 0))
      )
    )

  // ---------- adding pages ----------

  /** Step one of page selection: pick the kind. The row arrives with the
    * kind's first variant; step two is the in-row variant switch. */
  private def addSection(recipe: Var[Recipe], addOpen: Var[Boolean]): HtmlElement =
    div(
      cls := "stack",
      button(
        cls := "btn btn--ghost btn--block wbk-add-toggle",
        child.text <-- s(_.workbook.addPages).combineWith(addOpen.signal).map((t, o) => if o then s"▴ $t" else s"＋ $t"),
        onClick --> (_ => addOpen.update(!_))
      ),
      child <-- addOpen.signal.map {
        case false => emptyNode
        case true =>
          div(
            cls := "wbk-add-grid",
            Workbook.groups.map { group =>
              val ids = group.sources.map(_.id).toSet
              // Pages of this kind already in the book — shown as an explicit
              // badge; a styling-only "added" state reads as a rendering quirk.
              val pagesOfKind = recipe.signal
                .map(_.rows.filter(r => ids.contains(r.sourceId)).map(_.count).sum)
                .distinct
              button(
                cls := "wbk-add-chip",
                cls("is-added") <-- pagesOfKind.map(_ > 0),
                span(cls := s"wbk-row__glyph activity-card--${group.tint}", group.glyph),
                span(cls := "wbk-add-chip__name", child.text <-- s(group.name)),
                child <-- pagesOfKind.map { n =>
                  if n == 0 then emptyNode else span(cls := "wbk-add-chip__badge", n.toString)
                },
                onClick --> (_ => recipe.update(r => r.copy(rows = r.rows :+ Row(group.sources.head.id, 1))))
              )
            }
          )
      }
    )

  // ---------- print ----------

  /** The one real action, docked: it sticks to the viewport bottom while the
    * composition scrolls, and settles into the page flow at the end. No Done
    * button — saving is continuous and the back arrow already exits. */
  private def printDock(recipe: Var[Recipe], preparing: Signal[Boolean], onPrint: () => Unit): HtmlElement =
    div(
      cls := "wbk-print-dock",
      button(
        cls := "btn btn--hero btn--block wbk-print-btn",
        cls("is-preparing") <-- preparing,
        child.text <-- preparing.combineWith(AppState.strings).map { (busy, str) =>
          if busy then str.workbook.printingBook else str.workbook.printBook
        },
        disabled <-- recipe.signal.combineWith(preparing).map((r, busy) => r.rows.isEmpty || busy),
        onClick --> (_ => onPrint())
      )
    )
