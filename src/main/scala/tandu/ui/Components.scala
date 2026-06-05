package tandu.ui

import com.raquo.laminar.api.L.*
import tandu.{AppState, Page, Pwa, Routing, Version}
import tandu.audio.Speech
import tandu.i18n.{Lang, Strings}

object Components:

  def header(
      title: Signal[String],
      back: Option[Page] = Some(Page.Home),
      subtitle: Option[Signal[String]] = None,
      brand: Boolean = false,
      glyph: Option[String] = None,
      tint: Option[String] = None
  ): HtmlElement =
    val titleNode: HtmlElement =
      if brand then
        // Home wordmark — fixed "Tandu" across all languages (appTitle is
        // never translated). Italic vermilion 'a' anchors the brand.
        h1(cls := "header__brand", "T", span(cls := "header__accent", "a"), "ndu")
      else h1(child.text <-- title)
    div(
      cls := "header no-print",
      cls("header--brand") := brand,
      back match
        case Some(target) =>
          button(
            cls := "btn btn--ghost btn--icon",
            "←",
            onClick --> (_ => Routing.go(target))
          )
        case None => emptyNode
      ,
      div(
        cls := "header__lede",
        // A tinted glyph stamp echoes the activity's home-grid card, anchoring
        // the page to the swatch the user just tapped.
        glyph.map(g =>
          span(cls := s"header__glyph activity-card--${tint.getOrElse("teal")}", g)
        ),
        div(
          cls := "header__titles",
          titleNode,
          // A short rule in the activity's own tint underlines the title,
          // pairing with the glyph stamp so both accents share one swatch.
          tint.map(t => span(cls := s"header__rule activity-card--$t")),
          subtitle.map(sub => p(cls := "header__tagline muted", child.text <-- sub))
        )
      ),
      div(
        cls := "header__actions",
        cornerMenu()
      )
    )

  def cornerMenu(): HtmlElement =
    val open: Var[Boolean] = Var(false)
    val aboutOpen: Var[Boolean] = Var(false)
    val iosHelpOpen: Var[Boolean] = Var(false)

    def closeAnd(action: => Unit): Unit =
      action
      open.set(false)

    div(
      cls := "corner-menu",
      cls("is-open") <-- open.signal,
      button(
        cls := "btn btn--ghost btn--icon corner-menu__toggle",
        aria.label <-- s(_.menu.open),
        svg.svg(
          svg.viewBox := "0 0 24 24",
          svg.fill := "none",
          svg.stroke := "currentColor",
          svg.strokeWidth := "2.5",
          svg.strokeLineCap := "round",
          svg.line(svg.x1 := "4", svg.y1 := "7",  svg.x2 := "20", svg.y2 := "7"),
          svg.line(svg.x1 := "4", svg.y1 := "12", svg.x2 := "20", svg.y2 := "12"),
          svg.line(svg.x1 := "4", svg.y1 := "17", svg.x2 := "20", svg.y2 := "17")
        ),
        onClick.stopPropagation --> (_ => open.update(!_))
      ),
      div(
        cls := "corner-menu__backdrop",
        onClick --> (_ => open.set(false))
      ),
      div(
        cls := "corner-menu__panel",
        onClick.stopPropagation --> (_ => ()),
        div(
          cls := "corner-menu__langs",
          Lang.values.toList.map: l =>
            button(
              cls := "corner-menu__lang",
              cls("is-active") <-- AppState.lang.signal.map(_ == l),
              aria.label := l.display,
              l.flag,
              onClick --> (_ => closeAnd(AppState.lang.set(l)))
            )
        ),
        div(cls := "corner-menu__divider"),
        child <-- Pwa.available.map {
          case true =>
            // Chromium: one-tap install via the deferred prompt.
            button(
              cls := "corner-menu__item",
              menuIcon(iconDownload),
              span(child.text <-- s(_.home.installApp)),
              onClick --> (_ => closeAnd(Pwa.prompt()))
            )
          case false if Pwa.needsManualInstall =>
            // iOS: no prompt exists — explain the Share → Add to Home Screen flow.
            button(
              cls := "corner-menu__item",
              menuIcon(iconDownload),
              span(child.text <-- s(_.home.installApp)),
              onClick --> (_ => closeAnd(iosHelpOpen.set(true)))
            )
          case false => emptyNode
        },
        button(
          cls := "corner-menu__item",
          menuIcon(iconInfo),
          span(child.text <-- s(_.about.open)),
          onClick --> (_ => closeAnd(aboutOpen.set(true)))
        ),
        a(
          cls := "corner-menu__item",
          href := "mailto:feedback@tandu.app?subject=Tandu%20feedback",
          target := "_blank",
          rel := "noopener noreferrer",
          menuIcon(iconMail),
          span(child.text <-- s(_.menu.feedback)),
          onClick --> (_ => open.set(false))
        ),
        a(
          cls := "corner-menu__item",
          href := "https://github.com/Krever/Tandu",
          target := "_blank",
          rel := "noopener noreferrer",
          menuIcon(iconGithub),
          span("GitHub"),
          onClick --> (_ => open.set(false))
        )
      ),
      modal(
        aboutOpen,
        s(_.about.title),
        s(_.about.body),
        extraActions = Seq(
          a(
            cls := "btn btn--ghost",
            href := "/privacy",
            target := "_blank",
            rel := "noopener noreferrer",
            child.text <-- s(_.about.privacy)
          )
        ),
        // Build stamp — quiet, no translated label. Recognisable to anyone
        // filing a bug; invisible noise to everyone else.
        footer = Some(p(cls := "modal__version muted", Version.current))
      ),
      modal(iosHelpOpen, s(_.installHelp.title), s(_.installHelp.body))
    )

  def segmentedToggle[A](
      containerCls: String,
      btnCls: String,
      options: List[(A, Signal[String])],
      selected: Var[A]
  ): HtmlElement =
    div(
      cls := containerCls,
      options.map: (value, label) =>
        button(
          cls := btnCls,
          cls("is-active") <-- selected.signal.map(_ == value),
          child.text <-- label,
          onClick --> (_ => selected.set(value))
        )
    )

  def card(content: Modifier[HtmlElement]*): HtmlElement =
    div(cls := "card stack", content)

  def banner(kind: String, text: Signal[String]): HtmlElement =
    div(cls := s"banner banner--$kind", child.text <-- text)

  def primaryBig(label: Signal[String], onTap: => Unit): HtmlElement =
    button(
      cls := "btn btn--hero btn--block",
      child.text <-- label,
      onClick --> (_ => onTap)
    )

  def ghost(label: Signal[String], onTap: => Unit, isDisabled: Signal[Boolean] = Val(false)): HtmlElement =
    button(
      cls := "btn btn--ghost",
      child.text <-- label,
      disabled <-- isDisabled,
      onClick --> (_ => onTap)
    )

  def replayButton(label: Signal[String], onTap: => Unit, finished: Signal[Boolean]): HtmlElement =
    button(
      cls := "btn",
      cls("btn--ghost") <-- finished.map(!_).distinct,
      cls("btn--player") <-- finished.distinct,
      child.text <-- label,
      onClick --> (_ => onTap)
    )

  def primary(label: Signal[String], onTap: => Unit, isDisabled: Signal[Boolean] = Val(false)): HtmlElement =
    button(
      cls := "btn",
      child.text <-- label,
      disabled <-- isDisabled,
      onClick --> (_ => onTap)
    )

  def tile(name: Signal[String], desc: Signal[String], onTap: => Unit, glyph: String = "✦"): HtmlElement =
    button(
      cls := "tile",
      span(cls := "tile__glyph", glyph),
      div(
        cls := "tile__body",
        div(cls := "tile__name", child.text <-- name),
        div(cls := "tile__desc", child.text <-- desc)
      ),
      span(cls := "tile__chev", "›"),
      onClick --> (_ => onTap)
    )

  def activityCard(
      name: Signal[String],
      desc: Signal[String],
      onTap: => Unit,
      isFavourite: Signal[Boolean],
      onToggleFavourite: () => Unit,
      favouriteLabel: Signal[String],
      isHidden: Signal[Boolean],
      onToggleHidden: () => Unit,
      hideLabel: Signal[String],
      glyph: String = "✦",
      tint: String = "teal"
  ): HtmlElement =
    button(
      cls := s"activity-card activity-card--$tint",
      cls("activity-card--muted") <-- isHidden,
      span(cls := "activity-card__glyph", glyph),
      div(
        cls := "activity-card__body",
        div(cls := "activity-card__name", child.text <-- name),
        div(cls := "activity-card__desc", child.text <-- desc)
      ),
      div(
        cls := "activity-card__actions no-print",
        button(
          cls := "activity-card__action activity-card__hide",
          cls("is-active") <-- isHidden,
          aria.label <-- hideLabel,
          aria.pressed <-- isHidden.map(_.toString),
          eyeIcon("activity-card__action-icon", "activity-card__eye-slash"),
          onClick.stopPropagation --> (_ => onToggleHidden())
        ),
        button(
          cls := "activity-card__action activity-card__fav",
          cls("is-active") <-- isFavourite,
          aria.label <-- favouriteLabel,
          aria.pressed <-- isFavourite.map(_.toString),
          child.text <-- isFavourite.map(if _ then "★" else "☆"),
          onClick.stopPropagation --> (_ => onToggleFavourite())
        )
      ),
      onClick --> (_ => onTap)
    )

  /** Eye glyph shared by the card hide-toggle and the "show hidden" pill. The
    * slash line is always present; give it `slashCls` so CSS can reveal it per
    * state (open eye = visible, slashed = hidden), or leave it unclassed to
    * show a permanently-slashed "hidden items" eye. */
  def eyeIcon(iconCls: String, slashCls: String = ""): SvgElement =
    svg.svg(
      svg.cls := iconCls,
      svg.viewBox := "0 0 24 24",
      svg.fill := "none",
      svg.stroke := "currentColor",
      svg.strokeWidth := "2",
      svg.strokeLineCap := "round",
      svg.strokeLineJoin := "round",
      svg.path(svg.d := "M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7-10-7-10-7z"),
      svg.circle(svg.cx := "12", svg.cy := "12", svg.r := "3"),
      svg.line(svg.cls := slashCls, svg.x1 := "3", svg.y1 := "3", svg.x2 := "21", svg.y2 := "21")
    )

  /** Primary call-to-action card living in the activity grid. Vermilion fill,
    * italic display type — visually announces itself as the showpiece while
    * still belonging to the grid family. */
  def suggestCard(label: Signal[String], onTap: => Unit): HtmlElement =
    button(
      cls := "activity-card activity-card--suggest",
      // The › ‹ marks mirror the reel's focus needles, tying the card to the
      // spin it launches — the same motif marks the per-activity re-suggest.
      span(cls := "suggest-chev", "›"),
      div(cls := "activity-card__name", child.text <-- label),
      span(cls := "suggest-chev", "‹"),
      onClick --> (_ => onTap)
    )

  /** Round "tap to hear" button. Speaks the current value of `text` in the
    * active language. Hidden when the browser can't synthesize speech, so call
    * sites can place it unconditionally. `no-print` keeps it off worksheets. */
  def speakBtn(text: Signal[String]): HtmlElement =
    button(
      cls := "speak-btn no-print",
      tpe := "button",
      aria.label <-- s(_.menu.readAloud),
      hidden := !Speech.supported,
      svg.svg(
        svg.cls := "speak-btn__icon",
        svg.viewBox := "0 0 24 24",
        svg.fill := "none",
        svg.stroke := "currentColor",
        svg.strokeWidth := "2",
        svg.strokeLineCap := "round",
        svg.strokeLineJoin := "round",
        iconSpeaker
      ),
      onClick.stopPropagation.compose(_.sample(text)) -->
        (value => Speech.speak(value, AppState.lang.now()))
    )

  def s(f: Strings => String): Signal[String] = AppState.strings.map(f)

  def modal(
      isOpen: Var[Boolean],
      title: Signal[String],
      body: Signal[String],
      extraActions: Seq[HtmlElement] = Nil,
      footer: Option[HtmlElement] = None
  ): HtmlElement =
    div(
      cls := "modal-backdrop no-print",
      cls("is-open") <-- isOpen.signal,
      onClick --> (_ => isOpen.set(false)),
      div(
        cls := "modal",
        onClick.stopPropagation --> (_ => ()),
        h2(cls := "modal__title", child.text <-- title),
        p(cls := "modal__body", child.text <-- body),
        div(
          cls := "modal__actions",
          extraActions,
          button(
            cls := "btn",
            child.text <-- s(_.common.close),
            onClick --> (_ => isOpen.set(false))
          )
        ),
        footer
      )
    )

  private def menuIcon(content: Modifier[SvgElement]): HtmlElement =
    span(
      cls := "corner-menu__item-icon",
      svg.svg(
        svg.viewBox := "0 0 24 24",
        svg.fill := "none",
        svg.stroke := "currentColor",
        svg.strokeWidth := "2",
        svg.strokeLineCap := "round",
        svg.strokeLineJoin := "round",
        content
      )
    )

  private val iconDownload: Modifier[SvgElement] =
    nodeSeq(
      svg.path(svg.d := "M12 4v12"),
      svg.path(svg.d := "M6 12l6 6 6-6"),
      svg.path(svg.d := "M4 20h16")
    )

  private val iconInfo: Modifier[SvgElement] =
    nodeSeq(
      svg.circle(svg.cx := "12", svg.cy := "12", svg.r := "9"),
      svg.path(svg.d := "M12 11v5"),
      svg.circle(svg.cx := "12", svg.cy := "7.5", svg.r := "0.6", svg.fill := "currentColor")
    )

  private val iconMail: Modifier[SvgElement] =
    nodeSeq(
      svg.rect(svg.x := "3", svg.y := "5", svg.width := "18", svg.height := "14", svg.rx := "2"),
      svg.path(svg.d := "M3 7l9 6 9-6")
    )

  private val iconSpeaker: Modifier[SvgElement] =
    nodeSeq(
      svg.path(svg.d := "M11 5L6 9H3v6h3l5 4V5z"),
      svg.path(svg.d := "M15.5 8.5a5 5 0 0 1 0 7"),
      svg.path(svg.d := "M18.5 6a8 8 0 0 1 0 12")
    )

  private val iconGithub: Modifier[SvgElement] =
    svg.path(
      svg.fill := "currentColor",
      svg.stroke := "none",
      svg.d := "M12 2a10 10 0 0 0-3.16 19.49c.5.09.68-.22.68-.48v-1.7c-2.78.6-3.37-1.34-3.37-1.34-.46-1.16-1.11-1.47-1.11-1.47-.91-.62.07-.6.07-.6 1 .07 1.53 1.03 1.53 1.03.89 1.52 2.34 1.08 2.91.83.09-.65.35-1.08.63-1.33-2.22-.25-4.55-1.11-4.55-4.94 0-1.09.39-1.98 1.03-2.68-.1-.25-.45-1.27.1-2.64 0 0 .84-.27 2.75 1.02a9.56 9.56 0 0 1 5 0c1.91-1.29 2.75-1.02 2.75-1.02.55 1.37.2 2.39.1 2.64.64.7 1.03 1.59 1.03 2.68 0 3.84-2.34 4.69-4.57 4.94.36.31.68.92.68 1.85v2.74c0 .27.18.58.69.48A10 10 0 0 0 12 2z"
    )
