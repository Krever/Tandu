package tandu.ui

import com.raquo.laminar.api.L.*
import tandu.{AppState, Page, Pwa, Routing}
import tandu.i18n.{Lang, Strings}

object Components:

  def header(
      title: Signal[String],
      back: Option[Page] = Some(Page.Home),
      showLang: Boolean = true,
      onInfo: Option[() => Unit] = None
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
      h1(child.text <-- title),
      div(
        cls := "header__actions",
        child <-- Pwa.available.map {
          case true =>
            button(
              cls := "btn btn--ghost btn--icon",
              aria.label <-- s(_.home.installApp),
              "⤓",
              onClick --> (_ => Pwa.prompt())
            )
          case false => emptyNode
        },
        onInfo.map: cb =>
          button(
            cls := "btn btn--ghost btn--icon",
            "ⓘ",
            onClick --> (_ => cb())
          )
        ,
        if showLang then langToggle() else emptyNode
      )
    )

  def langToggle(): HtmlElement =
    select(
      cls := "lang-select",
      value <-- AppState.lang.signal.map(_.code),
      onChange.mapToValue --> { code =>
        Lang.fromCode(code).foreach(AppState.lang.set)
      },
      Lang.values.toList.map(l => option(value := l.code, l.flag))
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
