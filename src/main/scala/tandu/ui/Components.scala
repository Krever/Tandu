package tandu.ui

import com.raquo.laminar.api.L.*
import tandu.{AppState, Page, Routing}
import tandu.i18n.{Lang, Strings}

object Components:

  def header(
      title: Signal[String],
      back: Option[Page] = Some(Page.Home),
      showLang: Boolean = true
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
      if showLang then langToggle() else div()
    )

  def langToggle(): HtmlElement =
    div(
      cls := "lang-toggle",
      Lang.values.toList.map: l =>
        button(
          cls := "lang-btn",
          cls("is-active") <-- AppState.lang.signal.map(_ == l),
          l.code.toUpperCase,
          onClick --> (_ => AppState.lang.set(l))
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
