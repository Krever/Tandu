package tandu.pages

import com.raquo.laminar.api.L.*
import tandu.{AppState, Kind, Page, Routing}
import tandu.activities.{Activity, Players, Registry}
import tandu.tools.Tools
import tandu.ui.{Components, SuggestSpin}
import tandu.ui.Components.s

object HomePage:

  def render(): HtmlElement =
    val playersFilter   = AppState.playersFilter
    val handsFreeOnly   = AppState.handsFreeOnly
    val kindFilter      = AppState.kindFilter
    val favourites      = AppState.favourites
    val favouritesOnly  = AppState.favouritesOnly

    // Holds the in-flight slot-reel spin; cleared once it lands and navigates.
    val spin = Var(Option.empty[SuggestSpin.Reel])

    // Ephemeral name filter — resets when leaving the page, which is the
    // desired behaviour (a fresh, unfiltered list on each visit home).
    val query = Var("")

    // Three display groups, in render order: screen-based games, hands-free
    // games, and learning activities. Hands-free is split out of Games so the
    // long catalog is easier to scan. The free-text query filters by name in
    // the active language across all groups.
    val visible: Signal[(List[Activity], List[Activity], List[Activity])] =
      playersFilter.signal
        .combineWith(handsFreeOnly.signal)
        .combineWith(kindFilter.signal)
        .combineWith(favouritesOnly.signal)
        .combineWith(favourites.signal)
        .combineWith(query.signal)
        .combineWith(AppState.strings)
        .map { (p, hf, kind, favOnly, favs, q, str) =>
          val base = Registry.filtered(p, hf, kind, favOnly, favs)
          val needle = q.trim.toLowerCase
          val matched =
            if needle.isEmpty then base
            else base.filter(_.name(str).toLowerCase.contains(needle))
          val (free, rest)   = matched.partition(_.handsFree)
          val (games, learn) = rest.partition(_.kind == Kind.Games)
          (games, free, learn)
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
          styleAttr := "justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 0.5rem;",
          h2(cls := "h2", child.text <-- s(_.home.activities)),
          div(
            cls := "row",
            styleAttr := "gap: 0.5rem; flex-wrap: wrap;",
            kindPill(kindFilter),
            playersPill(playersFilter),
            handsFreePill(handsFreeOnly),
            favouritesPill(favouritesOnly)
          )
        ),
        input(
          cls := "activity-search no-print",
          tpe := "search",
          placeholder <-- s(_.filters.searchPlaceholder),
          aria.label <-- s(_.filters.searchPlaceholder),
          onInput.mapToValue --> query
        ),
        div(
          cls := "activity-grid",
          children <-- visible.map { (games, free, learn) =>
            val total = games.size + free.size + learn.size
            if total == 0 then
              val message =
                if query.now().trim.nonEmpty then s(_.filters.noMatches)
                else s(_.filters.noFavouritesYet)
              List(emptyCard(message))
            else
              val suggest = Components.suggestCard(
                s(_.home.suggestActivity), {
                  val p       = playersFilter.now()
                  val hf      = handsFreeOnly.now()
                  val k       = kindFilter.now()
                  val favOnly = favouritesOnly.now()
                  val favs    = favourites.now()
                  val pool    = Registry.filtered(p, hf, k, favOnly, favs)
                  val pick    = Registry.pickRandom(pool)
                  spin.set(Some(SuggestSpin.build(pool, pick)))
                }
              )
              // Label each non-empty section, but only when more than one is
              // shown — a lone group needs no divider.
              val sections = List(
                (s(_.filters.games),     games),
                (s(_.filters.handsFree), free),
                (s(_.filters.learn),     learn)
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
            val glyph = t.id match
              case "dice"  => "⚀"
              case "timer" => "⏱"
              case _       => "✦"
            Components.tile(
              s(t.name),
              s(t.description),
              Routing.go(Page.Tool(t.id)),
              glyph = glyph
            )
        )
      )
    )

  private def activityCard(a: Activity): HtmlElement =
    val isFav = AppState.favourites.signal.map(_.contains(a.id))
    Components.activityCard(
      name = s(a.name),
      desc = s(a.description),
      onTap = Routing.go(Page.Activity(a.id)),
      isFavourite = isFav,
      onToggleFavourite = () => AppState.toggleFavourite(a.id),
      favouriteLabel = isFav.combineWith(AppState.strings).map { (fav, st) =>
        if fav then st.filters.removeFromFavourites else st.filters.addToFavourites
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

  private def kindPill(filter: Var[Kind]): HtmlElement =
    val options: List[(Kind, Signal[String])] =
      Kind.values.toList.map(k => (k, s(k.label)))
    Components.segmentedToggle("pill-toggle no-print", "pill-btn", options, filter)

  private def playersPill(filter: Var[Option[Players]]): HtmlElement =
    val options: List[(Option[Players], Signal[String])] =
      (None, s(_.filters.all)) ::
        Players.values.toList.map(p => (Some(p), s(p.label)))
    Components.segmentedToggle("pill-toggle no-print", "pill-btn", options, filter)

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

  private def handsFreePill(toggle: Var[Boolean]): HtmlElement =
    togglePill(toggle, "pill-btn", child.text <-- s(_.filters.handsFree))

  private def favouritesPill(toggle: Var[Boolean]): HtmlElement =
    togglePill(toggle, "pill-btn pill-btn--icon", aria.label <-- s(_.filters.favourites), "★")
