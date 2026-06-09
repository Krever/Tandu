package tandu.pages

import com.raquo.laminar.api.L.*
import tandu.{AppState, Kind, Page, Routing}
import tandu.activities.{Activity, Players, Registry}
import tandu.tools.Tools
import tandu.ui.{Components, SuggestSpin}
import tandu.ui.Components.s

object HomePage:

  def render(): HtmlElement =
    val players         = AppState.players
    val kinds           = AppState.kinds
    val favourites      = AppState.favourites
    val favouritesOnly  = AppState.favouritesOnly

    // Holds the in-flight slot-reel spin; cleared once it lands and navigates.
    val spin = Var(Option.empty[SuggestSpin.Reel])

    // Ephemeral name filter — resets when leaving the page, which is the
    // desired behaviour (a fresh, unfiltered list on each visit home).
    val query = Var("")

    // Search starts collapsed to a single pill button (few visitors filter by
    // name); tapping it unwinds the pill into a full bar and focuses the field.
    val searchOpen = Var(false)

    // Reveal toggle for hidden activities — ephemeral, so a fresh visit always
    // starts with hidden items tucked away. While on, hidden cards reappear
    // (dimmed) so they can be restored.
    val revealHidden = Var(false)

    // The set to exclude from the grid: nothing while revealing, otherwise the
    // hidden set. Collapsing reveal + hidden into one signal keeps the filter
    // combine within Airstream's tuple arity.
    val excluded: Signal[Set[String]] =
      AppState.hidden.signal
        .combineWith(revealHidden.signal)
        .map((h, reveal) => if reveal then Set.empty else h)

    // Four display groups, in render order, one per Kind: screen/board games,
    // on-the-go (calm, hands-free verbal games), get-up-and-move, and learning.
    // The free-text query filters by name in the active language across all
    // groups.
    val visible: Signal[(List[Activity], List[Activity], List[Activity], List[Activity])] =
      players.signal
        .combineWith(kinds.signal)
        .combineWith(favouritesOnly.signal)
        .combineWith(favourites.signal)
        .combineWith(excluded)
        .combineWith(query.signal)
        .combineWith(AppState.strings)
        .map { (p, ks, favOnly, favs, hid, q, str) =>
          val base = Registry.filtered(p, ks, favOnly, favs, hid)
          val needle = q.trim.toLowerCase
          val matched =
            if needle.isEmpty then base
            else base.filter(_.name(str).toLowerCase.contains(needle))
          val games = matched.filter(_.kind == Kind.Games)
          val free  = matched.filter(_.kind == Kind.OnTheGo)
          val move  = matched.filter(_.kind == Kind.Move)
          val learn = matched.filter(_.kind == Kind.Learn)
          (games, free, move, learn)
        }

    div(
      cls := "app stack-lg",
      SuggestSpin.overlay(spin, a => { spin.set(None); Routing.go(Page.Activity(a.id)) }),
      Components.header(
        s(_.appTitle),
        back = None,
        subtitle = Some(s(_.tagline)),
        brand = true
      ),
      sectionTag(
        cls := "stack",
        div(
          cls := "row",
          styleAttr := "justify-content: space-between; align-items: center; gap: 0.5rem;",
          h2(cls := "h2", child.text <-- s(_.home.activities)),
          searchBar(query, searchOpen)
        ),
        div(
          cls := "row",
          styleAttr := "gap: 0.5rem; flex-wrap: wrap; align-items: center;",
          kindChips(kinds),
          playersChips(players),
          favouritesPill(favouritesOnly),
          // Only offered once something is hidden — keeps the cluster clean for
          // everyone who never hides anything.
          child <-- AppState.hidden.signal.map(h =>
            if h.isEmpty then emptyNode else revealHiddenPill(revealHidden)
          )
        ),
        div(
          cls := "activity-grid",
          children <-- visible.map { (games, free, move, learn) =>
            val total = games.size + free.size + move.size + learn.size
            if total == 0 then
              val message =
                if query.now().trim.nonEmpty then s(_.filters.noMatches)
                else s(_.filters.noFavouritesYet)
              List(emptyCard(message))
            else
              val suggest = Components.suggestCard(
                s(_.home.suggestActivity), {
                  val p       = players.now()
                  val ks      = kinds.now()
                  val favOnly = favouritesOnly.now()
                  val favs    = favourites.now()
                  // Suggestions never surface a hidden activity, regardless of
                  // the reveal toggle.
                  val pool    = Registry.filtered(p, ks, favOnly, favs, AppState.hidden.now())
                  val pick    = Registry.pickRandom(pool)
                  spin.set(Some(SuggestSpin.build(pool, pick)))
                }
              )
              // Label each non-empty section, but only when more than one is
              // shown — a lone group needs no divider.
              val sections = List(
                (s(_.filters.games),   games),
                (s(_.filters.onTheGo), free),
                (s(_.filters.move),    move),
                (s(_.filters.learn),   learn)
              ).filter(_._2.nonEmpty)
              val showLabels = sections.size > 1
              val body = sections.flatMap { (label, list) =>
                val head = if showLabels then List(sectionDivider(label)) else Nil
                head ++ list.map(activityCard)
              }
              suggest :: body
          }
        )
      ),
      sectionTag(
        cls := "stack",
        h2(cls := "h2", child.text <-- s(_.home.tools)),
        div(
          cls := "stack",
          Tools.all.map: t =>
            Components.tile(
              s(t.name),
              s(t.description),
              Routing.go(Page.Tool(t.id)),
              glyph = t.glyph
            )
        )
      )
    )

  /** Collapsible name filter. Lives as a pill button until tapped, then the
    * container unwinds (CSS width transition) into a full search bar with the
    * field focused. Tapping the toggle again collapses it and clears the query
    * so the grid returns to its full, unfiltered state. */
  private def searchBar(query: Var[String], open: Var[Boolean]): HtmlElement =
    val field = input(
      cls := "search__field",
      tpe := "search",
      placeholder <-- s(_.filters.searchPlaceholder),
      aria.label <-- s(_.filters.searchPlaceholder),
      onInput.mapToValue --> query
    )
    div(
      cls := "search no-print",
      cls("is-open") <-- open.signal,
      button(
        cls := "search__toggle",
        tpe := "button",
        aria.label <-- s(_.filters.searchPlaceholder),
        aria.expanded <-- open.signal,
        onClick --> { _ =>
          val opening = !open.now()
          open.set(opening)
          if opening then field.ref.focus()
          else query.set("")
        },
        child <-- open.signal.map(o => if o then span("✕") else magnifier)
      ),
      field
    )

  private def magnifier: SvgElement =
    svg.svg(
      svg.cls       := "search__icon",
      svg.viewBox   := "0 0 24 24",
      svg.fill      := "none",
      svg.stroke    := "currentColor",
      svg.strokeWidth := "2.2",
      svg.strokeLineCap := "round",
      svg.circle(svg.cx := "10.5", svg.cy := "10.5", svg.r := "6.5"),
      svg.line(svg.x1 := "20", svg.y1 := "20", svg.x2 := "15.5", svg.y2 := "15.5")
    )

  private def activityCard(a: Activity): HtmlElement =
    val isFav    = AppState.favourites.signal.map(_.contains(a.id))
    val isHidden = AppState.hidden.signal.map(_.contains(a.id))
    Components.activityCard(
      name = s(a.name),
      desc = s(a.description),
      onTap = Routing.go(Page.Activity(a.id)),
      isFavourite = isFav,
      onToggleFavourite = () => AppState.toggleFavourite(a.id),
      favouriteLabel = isFav.combineWith(AppState.strings).map { (fav, st) =>
        if fav then st.filters.removeFromFavourites else st.filters.addToFavourites
      },
      isHidden = isHidden,
      onToggleHidden = () => AppState.toggleHidden(a.id),
      hideLabel = isHidden.combineWith(AppState.strings).map { (hid, st) =>
        if hid then st.filters.unhide else st.filters.hide
      },
      glyph = a.glyph,
      tint = a.tint
    )

  private def sectionDivider(label: Signal[String]): HtmlElement =
    div(
      cls := "activity-grid__divider",
      span(cls := "activity-grid__divider-label", child.text <-- label)
    )

  private def emptyCard(message: Signal[String]): HtmlElement =
    div(
      cls := "activity-grid__empty",
      p(cls := "muted", child.text <-- message)
    )

  /** A grouped, additive multi-select track of checkbox options. Every option is
    * selected by default (the neutral "show everything" state); tapping toggles
    * one in or out. The last active option is locked (tap is a no-op) so the grid
    * can never be filtered down to nothing. Shared by the kind and players
    * filters so they look and behave identically. */
  private def checkboxTrack[A](
      options: List[(A, Signal[String])],
      selected: Var[Set[A]]
  ): HtmlElement =
    div(
      cls := "checkbox-track no-print",
      role := "group",
      options.map { (value, label) =>
        val active = selected.signal.map(_.contains(value))
        button(
          cls := "checkbox-track__opt",
          cls("is-active") <-- active,
          tpe := "button",
          aria.pressed <-- active.map(_.toString),
          child.text <-- label,
          onClick --> { _ =>
            selected.update { cur =>
              if cur.contains(value) then (if cur.size == 1 then cur else cur - value)
              else cur + value
            }
          }
        )
      }
    )

  private def kindChips(selected: Var[Set[Kind]]): HtmlElement =
    checkboxTrack(Kind.values.toList.map(k => (k, s(k.label))), selected)

  private def playersChips(selected: Var[Set[Players]]): HtmlElement =
    checkboxTrack(Players.values.toList.map(p => (p, s(p.label))), selected)

  private def togglePill(
      toggle: Var[Boolean],
      btnCls: String,
      content: Modifier[HtmlElement]*
  ): HtmlElement =
    div(
      cls := "pill-toggle no-print",
      button(
        cls := btnCls,
        cls("is-active") <-- toggle.signal,
        content,
        onClick --> (_ => toggle.update(!_))
      )
    )

  private def favouritesPill(toggle: Var[Boolean]): HtmlElement =
    togglePill(toggle, "pill-btn pill-btn--icon", aria.label <-- s(_.filters.favourites), "★")

  // Reveals hidden activities (dimmed) so they can be restored. The permanently
  // slashed eye signals "hidden items"; active state inverts like the other pills.
  private def revealHiddenPill(toggle: Var[Boolean]): HtmlElement =
    togglePill(toggle, "pill-btn pill-btn--icon", aria.label <-- s(_.filters.showHidden), Components.eyeIcon("pill-eye"))
