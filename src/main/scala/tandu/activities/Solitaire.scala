package tandu.activities

import com.raquo.laminar.api.L.*
import tandu.i18n.Strings
import tandu.ui.{Components, Mode, ModeChooser, RulesCard}
import tandu.ui.Components.s

import scala.util.Random

object Solitaire extends Activity:
  val id = "solitaire"
  def name(s: Strings): String = s.solitaire.name
  def description(s: Strings): String = s.solitaire.description
  val minPlayers: Int = 1
  val maxPlayers: Int = 1
  val handsFree: Boolean = false

  // ---------- model ----------

  enum Suit:
    case Hearts, Diamonds, Clubs, Spades
    def glyph: String = this match
      case Hearts => "♥"; case Diamonds => "♦"
      case Clubs => "♣"; case Spades => "♠"
    def isRed: Boolean = this match
      case Hearts | Diamonds => true
      case _                 => false

  /** Rank 1..13. 1 = Ace, 11 = Jack, 12 = Queen, 13 = King. */
  opaque type Rank = Int
  object Rank:
    def apply(n: Int): Rank = n
    val Ace: Rank  = 1
    val King: Rank = 13
    extension (r: Rank)
      def value: Int = r
      def label: String = r match
        case 1  => "A"
        case 11 => "J"
        case 12 => "Q"
        case 13 => "K"
        case n  => n.toString

  final case class Card(rank: Rank, suit: Suit):
    def isRed: Boolean = suit.isRed
    def label: String  = s"${Rank.label(rank)}${suit.glyph}"

  /** faceUp is ordered top-of-pile -> bottom (last is the most accessible
    * single card; any prefix of the tail can be picked up). */
  final case class Column(faceDown: List[Card], faceUp: Vector[Card]):
    def isEmpty: Boolean = faceDown.isEmpty && faceUp.isEmpty
    def bottom: Option[Card] = faceUp.lastOption

  final case class GameState(
      stock: List[Card],
      waste: List[Card], // head = top
      foundations: Map[Suit, List[Card]], // head = top
      tableau: Vector[Column]
  ):
    def foundationTop(s: Suit): Option[Card] = foundations(s).headOption
    def isWon: Boolean = foundations.values.forall(_.size == 13)

  enum Source:
    case Waste
    case Foundation(suit: Suit)
    case Tableau(col: Int, idx: Int) // idx into faceUp

  enum Target:
    case Foundation(suit: Suit)
    case Tableau(col: Int)

  // ---------- deal ----------

  private val freshDeck: List[Card] =
    for s <- Suit.values.toList; r <- 1 to 13 yield Card(Rank(r), s)

  def deal(rng: Random = new Random()): GameState =
    val shuffled = rng.shuffle(freshDeck)
    // Tableau: column i (0..6) gets i face-down + 1 face-up = i+1 cards.
    var rest = shuffled
    val cols = (0 until 7).map { i =>
      val take = i + 1
      val (cs, tail) = rest.splitAt(take)
      rest = tail
      val faceDown = cs.init
      val faceUp   = Vector(cs.last)
      Column(faceDown, faceUp)
    }.toVector
    GameState(
      stock = rest,
      waste = Nil,
      foundations = Suit.values.iterator.map(_ -> List.empty[Card]).toMap,
      tableau = cols
    )

  // ---------- move legality ----------

  /** Card at the *top of the tail* (i.e. the one that determines what the
    * stack can land on). For a single card it's the card itself. */
  private def tailTop(state: GameState, src: Source): Option[Card] = src match
    case Source.Waste            => state.waste.headOption
    case Source.Foundation(s)    => state.foundationTop(s)
    case Source.Tableau(c, i)    =>
      val fu = state.tableau(c).faceUp
      if i >= 0 && i < fu.size then Some(fu(i)) else None

  /** How many cards are in the picked-up tail. Only Tableau sources can
    * lift more than one. */
  private def tailLength(state: GameState, src: Source): Int = src match
    case Source.Tableau(c, i) =>
      val fu = state.tableau(c).faceUp
      math.max(0, fu.size - i)
    case _ => 1

  private def canPlaceOnFoundation(card: Card, top: Option[Card]): Boolean =
    top match
      case None      => Rank.value(card.rank) == 1
      case Some(top) => top.suit == card.suit && Rank.value(card.rank) == Rank.value(top.rank) + 1

  private def canPlaceOnTableau(card: Card, dest: Column): Boolean =
    dest.bottom match
      case None       => Rank.value(card.rank) == 13 // empty column accepts King
      case Some(bot)  => bot.isRed != card.isRed && Rank.value(bot.rank) == Rank.value(card.rank) + 1

  def canMove(state: GameState, src: Source, tgt: Target): Boolean =
    tailTop(state, src) match
      case None => false
      case Some(card) =>
        tgt match
          case Target.Foundation(s) =>
            tailLength(state, src) == 1 &&
              card.suit == s &&
              canPlaceOnFoundation(card, state.foundationTop(s))
          case Target.Tableau(c) =>
            // Disallow no-op move to the same column.
            val sameCol = src match
              case Source.Tableau(sc, _) => sc == c
              case _                     => false
            !sameCol && canPlaceOnTableau(card, state.tableau(c))

  def legalTargets(state: GameState, src: Source): List[Target] =
    tailTop(state, src) match
      case None => Nil
      case _ =>
        val fnd =
          if tailLength(state, src) == 1 then
            Suit.values.iterator
              .map(Target.Foundation(_))
              .filter(canMove(state, src, _))
              .toList
          else Nil
        val tab = (0 until 7)
          .map(Target.Tableau(_))
          .filter(canMove(state, src, _))
          .toList
        fnd ++ tab

  // ---------- apply ----------

  /** Remove the tail starting at src from the state. Returns updated state
    * + the removed cards (in top-of-pile -> bottom order). Caller is
    * responsible for placing them somewhere. */
  private def liftTail(state: GameState, src: Source): (GameState, Vector[Card]) = src match
    case Source.Waste =>
      val w = state.waste
      (state.copy(waste = w.tail), Vector(w.head))
    case Source.Foundation(s) =>
      val f = state.foundations(s)
      (state.copy(foundations = state.foundations.updated(s, f.tail)), Vector(f.head))
    case Source.Tableau(c, i) =>
      val col = state.tableau(c)
      val (keep, lift) = col.faceUp.splitAt(i)
      val newCol =
        if keep.nonEmpty || col.faceDown.isEmpty then col.copy(faceUp = keep)
        else Column(col.faceDown.tail, Vector(col.faceDown.head))
      (state.copy(tableau = state.tableau.updated(c, newCol)), lift)

  def applyMove(state: GameState, src: Source, tgt: Target): GameState =
    if !canMove(state, src, tgt) then state
    else
      val (lifted, cards) = liftTail(state, src)
      tgt match
        case Target.Foundation(s) =>
          val newF = cards.head :: lifted.foundations(s)
          lifted.copy(foundations = lifted.foundations.updated(s, newF))
        case Target.Tableau(c) =>
          val col = lifted.tableau(c)
          val newCol = col.copy(faceUp = col.faceUp ++ cards)
          lifted.copy(tableau = lifted.tableau.updated(c, newCol))

  /** Tap on the stock pile. Either reveals one card to waste, or — if
    * stock is empty — recycles the waste back to stock (face-down). */
  def drawStock(state: GameState): GameState =
    state.stock match
      case top :: rest => state.copy(stock = rest, waste = top :: state.waste)
      case Nil =>
        if state.waste.isEmpty then state
        else state.copy(stock = state.waste.reverse, waste = Nil)

  /** Auto-move to a foundation if one exists. Tableau auto-routing is
    * intentionally avoided: even a "unique" tableau target may not be the
    * one the player wants (e.g. moving a king to clear a column). */
  def autoFoundation(state: GameState, src: Source): Option[Target.Foundation] =
    legalTargets(state, src).collectFirst { case f: Target.Foundation => f }

  // ---------- UI ----------

  /** A tappable region on the table. */
  private enum Spot:
    case Stock
    case Waste
    case FoundationSpot(suit: Suit)
    case TableauSpot(col: Int, idx: Option[Int])

  private def spotAsSource(state: GameState, sp: Spot): Option[Source] = sp match
    case Spot.Stock => None
    case Spot.Waste =>
      Option.when(state.waste.nonEmpty)(Source.Waste)
    case Spot.FoundationSpot(s) =>
      Option.when(state.foundationTop(s).isDefined)(Source.Foundation(s))
    case Spot.TableauSpot(c, Some(i)) =>
      Option.when(i >= 0 && i < state.tableau(c).faceUp.size)(Source.Tableau(c, i))
    case Spot.TableauSpot(_, None) => None

  private def spotAsTarget(sp: Spot): Option[Target] = sp match
    case Spot.Stock                => None
    case Spot.Waste                => None
    case Spot.FoundationSpot(s)    => Some(Target.Foundation(s))
    case Spot.TableauSpot(c, _)    => Some(Target.Tableau(c))

  def render(): HtmlElement =
    ModeChooser.render(List(
      Mode(
        id = "in-app",
        label = _.mode.inApp,
        render = () => renderPlay()
      ),
      Mode(
        id = "deck",
        label = _.mode.offline,
        materials = List(_.offline.materials.deck52),
        render = () => renderRules()
      )
    ))

  private def renderRules(): HtmlElement =
    val example = Val(deal(new Random(1)))
    div(
      cls := "stack-lg",
      RulesCard.render(List(RulesCard.fromRules(_.offline.solitaire.rules))),
      div(
        cls := "sol sol--readonly stack",
        h3(cls := "h2 center", child.text <-- s(_.offline.solitaire.setupExample)),
        tableView(example, Val(None), _ => ())
      )
    )

  private def renderPlay(): HtmlElement =
    val state     = Var(deal())
    val selection = Var(Option.empty[Source])
    val history   = Var(List.empty[GameState])

    def pushHistory(): Unit =
      history.update(state.now() :: _.take(99))

    def handleTap(spot: Spot): Unit =
      spot match
        case Spot.Stock =>
          pushHistory()
          state.update(drawStock)
          selection.set(None)

        case other =>
          val cur    = state.now()
          val curSel = selection.now()
          val asTarget = spotAsTarget(other)
          val asSource = spotAsSource(cur, other)

          curSel match
            case Some(src) =>
              if asSource.contains(src) then
                selection.set(None) // tap the selected card again to cancel
              else
                asTarget.filter(canMove(cur, src, _)) match
                  case Some(t) =>
                    pushHistory()
                    state.update(applyMove(_, src, t))
                    selection.set(None)
                  case None =>
                    asSource match
                      case Some(newSrc) =>
                        autoFoundation(cur, newSrc) match
                          case Some(t) =>
                            pushHistory()
                            state.update(applyMove(_, newSrc, t))
                            selection.set(None)
                          case None =>
                            selection.set(Some(newSrc))
                      case None =>
                        selection.set(None)

            case None =>
              asSource match
                case Some(src) =>
                  autoFoundation(cur, src) match
                    case Some(t) =>
                      pushHistory()
                      state.update(applyMove(_, src, t))
                    case None =>
                      selection.set(Some(src))
                case None => ()

    def undo(): Unit =
      history.now() match
        case prev :: rest =>
          state.set(prev)
          history.set(rest)
          selection.set(None)
        case Nil => ()

    def newGame(): Unit =
      state.set(deal())
      history.set(Nil)
      selection.set(None)

    div(
      cls := "sol stack-lg",
      child <-- state.signal.map(_.isWon).map {
        case true  => wonView(newGame)
        case false => tableView(state.signal, selection.signal, handleTap)
      },
      div(
        cls := "row sol-controls",
        styleAttr := "justify-content: center;",
        Components.ghost(
          s(_.solitaire.undo),
          undo(),
          isDisabled = history.signal.map(_.isEmpty)
        ),
        Components.ghost(s(_.solitaire.newGame), newGame())
      )
    )

  private def wonView(newGame: () => Unit): HtmlElement =
    div(
      cls := "handoff card",
      div(cls := "handoff__title", child.text <-- s(_.common.youWin)),
      button(
        cls := "btn btn--lg",
        child.text <-- s(_.solitaire.newGame),
        onClick --> (_ => newGame())
      )
    )

  private def tableView(
      gameSig: Signal[GameState],
      selSig: Signal[Option[Source]],
      onTap: Spot => Unit
  ): HtmlElement =
    div(
      cls := "sol-table stack",
      topRow(gameSig, selSig, onTap),
      tableauRow(gameSig, selSig, onTap)
    )

  private def topRow(
      gameSig: Signal[GameState],
      selSig: Signal[Option[Source]],
      onTap: Spot => Unit
  ): HtmlElement =
    div(
      cls := "sol-top",
      stockView(gameSig, onTap),
      wasteView(gameSig, selSig, onTap),
      Suit.values.toList.zipWithIndex.map { (suit, i) =>
        foundationView(suit, i, gameSig, selSig, onTap)
      }
    )

  private def stockView(gameSig: Signal[GameState], onTap: Spot => Unit): HtmlElement =
    val nonEmpty = gameSig.map(_.stock.nonEmpty)
    div(
      cls := "sol-stock",
      cls("sol-card") <-- nonEmpty,
      cls("sol-card--back") <-- nonEmpty,
      cls("sol-slot") <-- nonEmpty.map(!_),
      child.maybe <-- nonEmpty.map(ne => Option.when(!ne)(div(cls := "sol-foundation-hint", "↺"))),
      onClick --> (_ => onTap(Spot.Stock))
    )

  private def wasteView(
      gameSig: Signal[GameState],
      selSig: Signal[Option[Source]],
      onTap: Spot => Unit
  ): HtmlElement =
    val topCard = gameSig.map(_.waste.headOption)
    val isSel   = selSig.map(_.contains(Source.Waste))
    cardOrSlot("sol-waste", topCard, isSel, isTarget = Val(false), onTap = onTap(Spot.Waste), emptyHint = None)

  private def foundationView(
      suit: Suit,
      pos: Int,
      gameSig: Signal[GameState],
      selSig: Signal[Option[Source]],
      onTap: Spot => Unit
  ): HtmlElement =
    val topCard = gameSig.map(_.foundationTop(suit))
    val isSel   = selSig.map(_.contains(Source.Foundation(suit)))
    val isTgt   = selSig.combineWith(gameSig).map { (sel, g) =>
      sel.exists(src => canMove(g, src, Target.Foundation(suit)))
    }
    cardOrSlot(
      extraCls = s"sol-foundation sol-foundation-$pos",
      topCard = topCard,
      isSel = isSel,
      isTarget = isTgt,
      onTap = onTap(Spot.FoundationSpot(suit)),
      emptyHint = Some(suit.glyph)
    )

  /** A cell that morphs between a face-up card and an empty slot,
    * keeping its grid-column placement via extraCls. */
  private def cardOrSlot(
      extraCls: String,
      topCard: Signal[Option[Card]],
      isSel: Signal[Boolean],
      isTarget: Signal[Boolean],
      onTap: => Unit,
      emptyHint: Option[String]
  ): HtmlElement =
    val face = topCard.map {
      case Some(c) => (true, c.isRed, !c.isRed)
      case None    => (false, false, false)
    }
    div(
      cls := extraCls,
      cls("sol-card")        <-- face.map(_._1),
      cls("sol-card--red")   <-- face.map(_._2),
      cls("sol-card--black") <-- face.map(_._3),
      cls("sol-slot")        <-- face.map(!_._1),
      cls("is-selected")     <-- isSel,
      cls("is-target")       <-- isTarget,
      children <-- topCard.map {
        case Some(c) =>
          List(
            div(cls := "sol-card__rank", Rank.label(c.rank)),
            div(cls := "sol-card__suit", c.suit.glyph)
          )
        case None =>
          emptyHint.toList.map(h => div(cls := "sol-foundation-hint", h))
      },
      onClick --> (_ => onTap)
    )

  private def tableauRow(
      gameSig: Signal[GameState],
      selSig: Signal[Option[Source]],
      onTap: Spot => Unit
  ): HtmlElement =
    div(
      cls := "sol-tableau",
      (0 until 7).map(c => tableauColumn(c, gameSig, selSig, onTap))
    )

  private def tableauColumn(
      c: Int,
      gameSig: Signal[GameState],
      selSig: Signal[Option[Source]],
      onTap: Spot => Unit
  ): HtmlElement =
    val colSig = gameSig.map(_.tableau(c))
    val isTarget = selSig.combineWith(gameSig).map { (sel, g) =>
      sel.exists(src => canMove(g, src, Target.Tableau(c)))
    }
    // Number of step-sized vertical offsets in the column. The
    // bottom-most card sits at the end of the stack so we don't count
    // it (only the steps *between* cards contribute height beyond the
    // first card).
    val sizeStyle: Signal[String] = colSig.map { col =>
      val (down, up) =
        if col.faceUp.nonEmpty then (col.faceDown.size, col.faceUp.size - 1)
        else if col.faceDown.nonEmpty then (col.faceDown.size - 1, 0)
        else (0, 0)
      s"--down-count: $down; --up-count: $up;"
    }
    div(
      cls := "sol-col",
      cls("is-target") <-- isTarget,
      styleAttr <-- sizeStyle,
      div(
        cls := "sol-col-base",
        onClick --> (_ => onTap(Spot.TableauSpot(c, None)))
      ),
      children <-- colSig.map { col =>
        val downSize = col.faceDown.size
        val downCards = col.faceDown.indices.map { i =>
          tableauBack(downIdx = i, col = c, onTap = onTap)
        }
        val upCards = col.faceUp.zipWithIndex.map { (card, i) =>
          val inTail = selSig.map {
            case Some(Source.Tableau(sc, si)) => sc == c && si <= i
            case _                            => false
          }
          tableauFace(card, downIdx = downSize, idx = i, col = c, isSel = inTail, onTap = onTap)
        }
        downCards.toList ++ upCards.toList
      }
    )

  private def stackedStyle(downIdx: Int, upIdx: Int): String =
    s"--down: $downIdx; --up: $upIdx;"

  private def tableauBack(
      downIdx: Int,
      col: Int,
      onTap: Spot => Unit
  ): HtmlElement =
    div(
      cls := "sol-card sol-card--back sol-stacked",
      styleAttr := stackedStyle(downIdx, 0),
      onClick --> (_ => onTap(Spot.TableauSpot(col, None)))
    )

  private def tableauFace(
      card: Card,
      downIdx: Int,
      idx: Int,
      col: Int,
      isSel: Signal[Boolean],
      onTap: Spot => Unit
  ): HtmlElement =
    div(
      cls := "sol-card sol-stacked",
      cls("sol-card--red") := card.isRed,
      cls("sol-card--black") := !card.isRed,
      cls("is-selected") <-- isSel,
      styleAttr := stackedStyle(downIdx, idx),
      div(cls := "sol-card__rank", Rank.label(card.rank)),
      div(cls := "sol-card__suit", card.suit.glyph),
      onClick.stopPropagation --> (_ => onTap(Spot.TableauSpot(col, Some(idx))))
    )
