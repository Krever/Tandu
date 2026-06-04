package tandu.ui

import com.raquo.laminar.api.L.*
import org.scalajs.dom
import tandu.activities.Activity
import tandu.ui.Components.s

import scala.util.Random

/** Slot-reel "spin" that plays before a Suggest navigation.
  *
  * A strip of catalog cards scrolls fast through a framed focus window and
  * decelerates onto the pre-picked activity, gives a little stamp "thunk", then
  * hands the pick back via `onLanded` (the caller navigates). Tapping anywhere
  * skips straight to the result.
  *
  * The reel is built up front with the real (filtered) catalog as filler, so the
  * cards whizzing past read as a genuine shuffle of what's on the home grid.
  */
object SuggestSpin:

  /** A built reel: the strip to render plus the index that comes to rest in the
    * focus window. Geometry (item height, window size) lives in CSS — see
    * `.suggest-reel__item`; `ItemPx` below must stay in sync with it. */
  final case class Reel(items: Vector[Activity], targetIndex: Int):
    def target: Activity = items(targetIndex)

  private val ItemPx     = 76 // must match .suggest-reel__item height in styles.css
  private val LeadCount  = 26 // filler cards spun past before the pick lands
  private val TrailCount = 2  // filler cards below the pick (fill the bottom fade)
  private val FallbackMs = 3000.0 // safety net if transitionend never fires

  /** Build a reel from the pool of candidate activities, landing on `target`. */
  def build(pool: List[Activity], target: Activity): Reel =
    val candidates     = if pool.isEmpty then List(target) else pool
    def filler(): Activity = candidates(Random.nextInt(candidates.size))
    val lead           = Vector.fill(LeadCount)(filler())
    val trail          = Vector.fill(TrailCount)(filler())
    Reel(lead ++ Vector(target) ++ trail, LeadCount)

  /** Full-screen overlay bound to `state`. When `state` holds a reel the
    * animation mounts and plays; `onLanded` fires once (after the thunk, or
    * immediately on tap-to-skip). */
  def overlay(state: Var[Option[Reel]], onLanded: Activity => Unit): HtmlElement =
    div(
      cls := "suggest-spin no-print",
      cls("is-open") <-- state.signal.map(_.isDefined),
      child <-- state.signal.map {
        case None       => emptyNode
        case Some(reel) => stage(reel, onLanded)
      }
    )

  private def stage(reel: Reel, onLanded: Activity => Unit): HtmlElement =
    // The strip animates from "item 1 centred" to "target centred". With the
    // focus window one item tall and centred, centring index k means
    // translateY = (1 - k) * ItemPx (item 1 sits at translateY 0).
    val startY  = 0
    val endY    = (1 - reel.targetIndex) * ItemPx
    val running = Var(false)
    val landed  = Var(false)
    var done    = false

    def navigate(): Unit =
      if !done then
        done = true
        onLanded(reel.target)

    // Reel has come to rest: flash the stamp thunk, then hand back the pick.
    def settle(): Unit =
      if !landed.now() && !done then
        landed.set(true)
        val _ = dom.window.setTimeout(() => navigate(), 460)

    div(
      cls := "suggest-spin__backdrop",
      onClick --> (_ => navigate()), // tap anywhere to skip the wait
      div(
        cls := "suggest-spin__panel",
        cls("is-landed") <-- landed.signal,
        onClick.stopPropagation --> (_ => ()),
        div(cls := "suggest-spin__title", child.text <-- s(_.home.spinning)),
        div(
          cls := "suggest-reel",
          // Viewport carries the top/bottom fade; it stays put while the strip
          // scrolls behind it, so the mask never travels with the content.
          div(
            cls := "suggest-reel__viewport",
            div(
              cls := "suggest-reel__strip",
              styleAttr <-- running.signal.map(go => s"transform: translateY(${if go then endY else startY}px);"),
              onTransitionEnd
                .filter(_.asInstanceOf[dom.TransitionEvent].propertyName == "transform") --> (_ => settle()),
              reel.items.toList.map(item)
            )
          ),
          div(cls := "suggest-reel__window")
        )
      ),
      onMountCallback { _ =>
        // Two rAFs so the browser paints the start transform before we flip to
        // the end transform — without this the transition is skipped.
        val _ = dom.window.requestAnimationFrame { _ =>
          val _ = dom.window.requestAnimationFrame(_ => running.set(true))
        }
        // Safety net: drives `settle` even if transitionend is missed (e.g. a
        // backgrounded tab, or transitions disabled for reduced motion).
        val _ = dom.window.setTimeout(() => settle(), FallbackMs)
      }
    )

  private def item(a: Activity): HtmlElement =
    div(
      cls := s"suggest-reel__item activity-card--${a.tint}",
      span(cls := "suggest-reel__glyph", a.glyph),
      span(cls := "suggest-reel__name", child.text <-- s(a.name))
    )
