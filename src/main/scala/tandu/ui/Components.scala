package tandu.ui

import com.raquo.laminar.api.L.*
import tandu.{AppState, Page, Pwa, Routing}
import tandu.i18n.{Lang, Strings}

object Components:

  def header(
      title: Signal[String],
      back: Option[Page] = Some(Page.Home),
      subtitle: Option[Signal[String]] = None
  ): HtmlElement =
    div(
      cls := "header no-print",
      back match
        case Some(target) =>
          button(
            cls := "btn btn--ghost btn--icon",
            "←",
            onClick --> (_ => Routing.go(target))
          )
        case None => div()
      ,
      div(
        cls := "header__titles",
        h1(child.text <-- title),
        subtitle.map(sub => p(cls := "header__tagline muted", child.text <-- sub))
      ),
      div(
        cls := "header__actions",
        cornerMenu()
      )
    )

  def cornerMenu(): HtmlElement =
    val open: Var[Boolean] = Var(false)
    val aboutOpen: Var[Boolean] = Var(false)

    def closeAnd(action: => Unit): Unit =
      action
      open.set(false)

    div(
      cls := "corner-menu",
      cls("is-open") <-- open.signal,
      button(
        cls := "btn btn--ghost btn--icon",
        aria.label <-- s(_.menu.open),
        "⋯",
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
            button(
              cls := "corner-menu__item",
              span(child.text <-- s(_.home.installApp)),
              span(cls := "corner-menu__item-icon", "⤓"),
              onClick --> (_ => closeAnd(Pwa.prompt()))
            )
          case false => emptyNode
        },
        button(
          cls := "corner-menu__item",
          span(child.text <-- s(_.about.open)),
          onClick --> (_ => closeAnd(aboutOpen.set(true)))
        ),
        a(
          cls := "corner-menu__item",
          href := "mailto:feedback@tandu.app?subject=Tandu%20feedback",
          target := "_blank",
          rel := "noopener noreferrer",
          span(child.text <-- s(_.menu.feedback)),
          span(cls := "corner-menu__item-icon", "✉"),
          onClick --> (_ => open.set(false))
        ),
        a(
          cls := "corner-menu__item",
          href := "https://github.com/Krever/Tandu",
          target := "_blank",
          rel := "noopener noreferrer",
          span("GitHub"),
          span(cls := "corner-menu__item-icon", "↗"),
          onClick --> (_ => open.set(false))
        )
      ),
      modal(aboutOpen, s(_.about.title), s(_.about.body))
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
      cls := "btn btn--lg btn--block",
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

  def tile(name: Signal[String], desc: Signal[String], onTap: => Unit): HtmlElement =
    button(
      cls := "tile",
      div(
        div(cls := "tile__name", child.text <-- name),
        div(cls := "tile__desc", child.text <-- desc)
      ),
      div(cls := "tile__chev", "›"),
      onClick --> (_ => onTap)
    )

  def activityCard(name: Signal[String], desc: Signal[String], onTap: => Unit): HtmlElement =
    button(
      cls := "activity-card",
      div(cls := "activity-card__name", child.text <-- name),
      div(cls := "activity-card__desc", child.text <-- desc),
      onClick --> (_ => onTap)
    )

  def s(f: Strings => String): Signal[String] = AppState.strings.map(f)

  def modal(
      isOpen: Var[Boolean],
      title: Signal[String],
      body: Signal[String],
      extraActions: Seq[HtmlElement] = Nil
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
        )
      )
    )
