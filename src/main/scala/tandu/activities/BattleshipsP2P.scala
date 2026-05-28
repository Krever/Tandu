package tandu.activities

import com.raquo.laminar.api.L.*
import scala.scalajs.js
import scala.util.Random
import tandu.activities.battleships.{Board, Domain}
import tandu.activities.battleships.Domain.{Cell, EnemyView, Fleet, Resolution, ShotResult, randomFleet}
import tandu.i18n.Strings
import tandu.net.Trystero
import tandu.ui.Components.s

/** P2P multi-device battleships. Each device holds its own fleet; shots
  * cross the wire via trystero.
  *
  * All gameplay logic lives in [[tandu.battleships.Domain]]; this file
  * only handles transport (wire encoding, lobby flow) and orchestrates
  * the local state machine. */
object BattleshipsP2P:

  // ---------- wire format ----------

  private trait WireCell extends js.Object:
    val x: Int
    val y: Int

  private object WireCell:
    def fromCell(c: Cell): WireCell =
      js.Dynamic.literal(x = c.x, y = c.y).asInstanceOf[WireCell]
    def toCell(w: WireCell): Cell = Cell(w.x, w.y)

  private trait WireShot extends js.Object:
    val x: Int
    val y: Int

  private trait WireResolution extends js.Object:
    val x: Int
    val y: Int
    val result: String              // "miss" | "hit" | "sunk"
    val sunkCells: js.UndefOr[js.Array[WireCell]]
    val gameOver: Boolean

  private object Wire:
    def encodeShot(c: Cell): WireShot =
      js.Dynamic.literal(x = c.x, y = c.y).asInstanceOf[WireShot]

    def decodeShot(w: WireShot): Cell = Cell(w.x, w.y)

    def encodeResolution(r: Resolution): WireResolution =
      val resultStr = r.result match
        case ShotResult.Miss => "miss"
        case ShotResult.Hit  => "hit"
        case ShotResult.Sunk => "sunk"
      val sunkArr: js.UndefOr[js.Array[WireCell]] =
        if r.sunkCells.isEmpty then js.undefined
        else js.Array(r.sunkCells.toSeq.map(WireCell.fromCell)*)
      js.Dynamic.literal(
        x         = r.target.x,
        y         = r.target.y,
        result    = resultStr,
        sunkCells = sunkArr,
        gameOver  = r.gameOver
      ).asInstanceOf[WireResolution]

    def decodeResolution(w: WireResolution): Resolution =
      val target = Cell(w.x, w.y)
      val result = w.result match
        case "hit"  => ShotResult.Hit
        case "sunk" => ShotResult.Sunk
        case _      => ShotResult.Miss
      val sunkCells: Set[Cell] = w.sunkCells.toOption
        .map(_.iterator.map(WireCell.toCell).toSet)
        .getOrElse(if result == ShotResult.Sunk then Set(target) else Set.empty)
      Resolution(target, result, sunkCells, w.gameOver)

  /** Bundle of typed channels for one trystero room. Crucially,
    * `makeAction` must run *before* peers connect — trystero negotiates
    * one data channel per namespace as part of the WebRTC handshake,
    * and a namespace registered after the connection is up won't
    * transport any messages. Each Channels is therefore built
    * immediately after the room is joined, in the lobby, before
    * `onPeerJoin` ever fires. */
  private final case class Channels(
      sendShot:    WireShot => Unit,
      onShot:      (WireShot => Unit) => Unit,
      sendResolve: WireResolution => Unit,
      onResolve:   (WireResolution => Unit) => Unit,
      sendEnd:     js.Object => Unit,
      onEnd:       (js.Object => Unit) => Unit,
      leave:       () => Unit,
      strategy:    Trystero.Strategy
  )

  private object Channels:
    def from(room: Trystero.Room): Channels =
      val (sShot, oShot)       = room.makeAction[WireShot]("shot")
      val (sResolve, oResolve) = room.makeAction[WireResolution]("resolve")
      val (sEnd, oEnd)         = room.makeAction[js.Object]("endTurn")
      Channels(sShot, oShot, sResolve, oResolve, sEnd, oEnd, () => room.leave(), room.strategy)

  // ---------- net session (torrent + nostr fallback) ----------

  /** Manages the lifecycle of joining a P2P room with a fallback
    * signaling strategy.
    *
    * Both peers follow the same deterministic schedule: open torrent
    * immediately, and if no peer has appeared after [[FallbackDelayMs]],
    * also open nostr. This keeps both sides on the same strategy — a
    * naive "race two strategies" approach can produce a split where
    * each peer picks the transport on which their *own* peer-join
    * fired first, leaving them on different rooms unable to talk.
    *
    * Whichever room sees the first peer wins; the other is left. */
  private final class NetSession private (appId: String, roomId: String):
    private val torrentRoom = Trystero.joinTorrentRoom(appId, roomId)
    private val torrentCh   = Channels.from(torrentRoom)
    private var fallback: Option[(Trystero.Room, Channels)] = None
    private var winnerOpt: Option[Channels] = None
    private var onConnectedFn: Option[Channels => Unit] = None
    private var fallbackTimer: Option[js.timers.SetTimeoutHandle] = None

    torrentRoom.onPeerJoin(_ => recordWinner(torrentCh))

    fallbackTimer = Some(js.timers.setTimeout(NetSession.FallbackDelayMs) {
      if winnerOpt.isEmpty then
        val r = Trystero.joinNostrRoom(appId, roomId)
        val ch = Channels.from(r)
        fallback = Some((r, ch))
        r.onPeerJoin(_ => recordWinner(ch))
    })

    private def recordWinner(ch: Channels): Unit =
      if winnerOpt.isEmpty then
        winnerOpt = Some(ch)
        fallbackTimer.foreach(js.timers.clearTimeout)
        if ch eq torrentCh then fallback.foreach { case (r, _) => r.leave() }
        else torrentRoom.leave()
        onConnectedFn.foreach(_(ch))
        onConnectedFn = None

    def onConnected(fn: Channels => Unit): Unit =
      winnerOpt match
        case Some(ch) => fn(ch)
        case None     => onConnectedFn = Some(fn)

    /** Tear down all rooms we ever opened. Idempotent. */
    def leaveAll(): Unit =
      fallbackTimer.foreach(js.timers.clearTimeout)
      fallbackTimer = None
      torrentRoom.leave()
      fallback.foreach { case (r, _) => r.leave() }

  private object NetSession:
    /** How long to give torrent before adding nostr as a parallel
      * fallback. Long enough that a successful torrent connection
      * completes first; short enough that users don't stare at a
      * "waiting" screen if torrent trackers are down. */
    val FallbackDelayMs = 6000

    def open(appId: String, roomId: String): NetSession =
      new NetSession(appId, roomId)

  // ---------- per-device state ----------

  private enum Phase:
    /** It's my turn — I should fire. */
    case MyTurn
    /** I just fired and am waiting for the opponent to report the result. */
    case AwaitingResolution(target: Cell)
    /** It's the opponent's turn — waiting for their shot to land. */
    case TheirTurn
    /** I lost — the opponent sunk my last ship. */
    case Lost
    /** I won — my last shot sunk their fleet. */
    case Won

  private final case class NetGame(
      myFleet: Fleet,
      incoming: Set[Cell],     // shots received from opponent
      enemyView: EnemyView,    // what I know about their board
      phase: Phase
  )

  // ---------- entry ----------

  private val AppId = "tandu-battleships-v1"

  def render(): HtmlElement =
    val state: Var[LobbyState] = Var(LobbyState.Idle)

    div(
      cls := "stack-lg",
      child <-- state.signal.map {
        case LobbyState.Idle =>
          lobbyChooser(
            onCreate = () => state.set(LobbyState.Hosting(genCode())),
            onJoin   = () => state.set(LobbyState.Joining)
          )
        case LobbyState.Hosting(code) =>
          hostingView(code, state)
        case LobbyState.Joining =>
          joiningView(state)
        case LobbyState.Connected(ch, me) =>
          gameView(ch, me)
      }
    )

  // ---------- lobby ----------

  private enum LobbyState:
    case Idle
    case Hosting(code: String)
    case Joining
    case Connected(channels: Channels, me: Player)

  private def genCode(): String =
    // 4-digit numeric, easy for a kid to type.
    val n = Random.between(0, 10000)
    f"$n%04d"

  private def lobbyChooser(onCreate: () => Unit, onJoin: () => Unit): HtmlElement =
    sectionTag(
      cls := "stack-lg",
      dataAttr("testid") := "p2p-lobby",
      h2(cls := "h2 center", child.text <-- s(_.battleships.p2p.title)),
      p(cls := "muted center", child.text <-- s(_.battleships.p2p.intro)),
      div(
        cls := "row",
        styleAttr := "justify-content: center; flex-wrap: wrap;",
        button(
          cls := "btn btn--lg",
          dataAttr("testid") := "p2p-create",
          child.text <-- s(_.battleships.p2p.create),
          onClick --> (_ => onCreate())
        ),
        button(
          cls := "btn btn--lg btn--ghost",
          dataAttr("testid") := "p2p-join",
          child.text <-- s(_.battleships.p2p.join),
          onClick --> (_ => onJoin())
        )
      )
    )

  /** Host: shows the code, joins the room, waits for the guest. */
  private def hostingView(code: String, state: Var[LobbyState]): HtmlElement =
    val net = NetSession.open(AppId, code)
    net.onConnected(ch => state.set(LobbyState.Connected(ch, Player.P1)))

    sectionTag(
      cls := "stack-lg center",
      dataAttr("testid") := "p2p-hosting",
      h2(cls := "h2", child.text <-- s(_.battleships.p2p.shareCode)),
      div(cls := "p2p-code", dataAttr("testid") := "p2p-code", code),
      p(cls := "muted", child.text <-- s(_.battleships.p2p.waiting)),
      button(
        cls := "btn btn--ghost",
        child.text <-- s(_.common.close),
        onClick --> { _ =>
          net.leaveAll()
          state.set(LobbyState.Idle)
        }
      ),
      // On unmount, if we transitioned to Connected the winning Channels
      // owns the room — otherwise tear everything down.
      onUnmountCallback(_ => if !state.now().isInstanceOf[LobbyState.Connected] then net.leaveAll())
    )

  /** Guest: types the code, joins the room, peer-join fires once host is there. */
  private def joiningView(state: Var[LobbyState]): HtmlElement =
    val typed = Var("")
    val net = Var(Option.empty[NetSession])

    def attempt(): Unit =
      val code = typed.now().trim
      if net.now().isDefined then ()
      else if code.length != 4 || !code.forall(_.isDigit) then ()
      else
        val session = NetSession.open(AppId, code)
        net.set(Some(session))
        session.onConnected { ch =>
          state.set(LobbyState.Connected(ch, Player.P2))
        }

    sectionTag(
      cls := "stack-lg center",
      h2(cls := "h2", child.text <-- s(_.battleships.p2p.enterCode)),
      div(
        cls := "row",
        styleAttr := "justify-content: center; flex-wrap: wrap; align-items: stretch; gap: var(--space-3);",
        input(
          cls := "p2p-input",
          dataAttr("testid") := "p2p-code-input",
          typ := "tel",
          maxLength := 4,
          placeholder := "0000",
          value <-- typed.signal,
          onInput.mapToValue.map(_.filter(_.isDigit).take(4)) --> typed,
          onKeyDown --> { ev =>
            if ev.key == "Enter" then attempt()
          }
        ),
        child <-- net.signal.map(_.isDefined).map { joining =>
          if joining then p(cls := "muted", child.text <-- s(_.battleships.p2p.waiting))
          else button(
            cls := "btn btn--lg",
            dataAttr("testid") := "p2p-connect",
            disabled <-- typed.signal.map(_.length != 4),
            child.text <-- s(_.battleships.p2p.connect),
            onClick --> (_ => attempt())
          )
        }
      ),
      button(
        cls := "btn btn--ghost",
        child.text <-- s(_.common.close),
        onClick --> { _ =>
          net.now().foreach(_.leaveAll())
          state.set(LobbyState.Idle)
        }
      ),
      onUnmountCallback { _ =>
        if !state.now().isInstanceOf[LobbyState.Connected] then
          net.now().foreach(_.leaveAll())
      }
    )

  // ---------- gameplay ----------

  private def gameView(net: Channels, me: Player): HtmlElement =
    // P1 (host) starts.
    val initial = NetGame(
      myFleet   = randomFleet(),
      incoming  = Set.empty,
      enemyView = EnemyView(),
      phase     = if me == Player.P1 then Phase.MyTurn else Phase.TheirTurn
    )
    val game = Var(initial)
    val showMyBoard = Var(true)

    // Defender side: resolve the incoming shot against my fleet, reply
    // with the outcome, and update my own state.
    net.onShot { wire =>
      val target = Wire.decodeShot(wire)
      val g = game.now()
      val res = Domain.resolveShot(g.myFleet, g.incoming, target)
      net.sendResolve(Wire.encodeResolution(res))
      game.update(_.copy(
        incoming = g.incoming + target,
        phase    = if res.gameOver then Phase.Lost else Phase.TheirTurn
      ))
    }

    // Shooter side: fold the resolution into my enemy view and
    // auto-hand-off (no manual "end turn" tap — the result is visible
    // on the enemy board itself).
    net.onResolve { wire =>
      val res = Wire.decodeResolution(wire)
      if !res.gameOver then
        net.sendEnd(js.Dynamic.literal().asInstanceOf[js.Object])
      game.update { g =>
        g.copy(
          enemyView = g.enemyView.apply(res),
          phase     = if res.gameOver then Phase.Won else Phase.TheirTurn
        )
      }
    }

    net.onEnd { _ =>
      if game.now().phase == Phase.TheirTurn then
        game.update(_.copy(phase = Phase.MyTurn))
    }

    def fire(c: Cell): Unit =
      val g = game.now()
      if g.phase != Phase.MyTurn then ()
      else if g.enemyView.knownCells.contains(c) then ()
      else
        net.sendShot(Wire.encodeShot(c))
        game.update(_.copy(phase = Phase.AwaitingResolution(c)))

    val phaseSig = game.signal.map(_.phase).distinct

    div(
      cls := s"stack-lg player-page player-page--p${me.num}",
      dataAttr("testid") := "p2p-game",
      dataAttr("p2p-me") := me.num.toString,
      // Phase indicator for e2e tests so they don't have to read
      // translated UI text to know when to act.
      dataAttr("p2p-phase") <-- phaseSig.map {
        case Phase.MyTurn                  => "my-turn"
        case Phase.AwaitingResolution(_)   => "awaiting"
        case Phase.TheirTurn               => "their-turn"
        case Phase.Won                     => "won"
        case Phase.Lost                    => "lost"
      },
      child <-- phaseSig.map {
        case Phase.MyTurn                     => activeView(_.battleships.p2p.yourTurn,      Some(_.battleships.fireAt),                game, showMyBoard, onTap = Some(fire))
        case Phase.AwaitingResolution(target) => activeView(_.battleships.p2p.waitingResolve, None,                                      game, showMyBoard, onTap = None, highlight = Some(target))
        case Phase.TheirTurn                  => activeView(_.battleships.p2p.opponentTurn,  Some(_.battleships.p2p.waitingShot),       game, showMyBoard, onTap = None)
        case Phase.Won                        => endView(_.battleships.allSunk, _.battleships.p2p.youWin)
        case Phase.Lost                       => endView(_.battleships.allSunk, _.battleships.p2p.youLose)
      },
      onUnmountCallback(_ => net.leave())
    )

  // ---------- subviews ----------

  /** All three "active" phases share the same layout — only the header
    * text, the optional intro line, and the enemy-board click/highlight
    * behavior differ. */
  private def activeView(
      headerText: Strings => String,
      intro: Option[Strings => String],
      game: Var[NetGame],
      showMyBoard: Var[Boolean],
      onTap: Option[Cell => Unit],
      highlight: Option[Cell] = None
  ): HtmlElement =
    div(
      cls := "stack-lg",
      Board.headerBadge(s(headerText)),
      intro match
        case Some(f) => p(cls := "muted center", child.text <-- s(f))
        case None    => emptyNode,
      Board.enemyBoardSection(
        view = game.signal.map(_.enemyView),
        onTap = onTap,
        highlight = highlight
      ),
      Board.myBoardSection(
        fleet    = game.signal.map(_.myFleet),
        incoming = game.signal.map(_.incoming),
        showMyBoard = showMyBoard
      )
    )

  private def endView(
      titleFn: Strings => String,
      subtitleFn: Strings => String
  ): HtmlElement =
    Board.handoffCard(
      badge = s(subtitleFn),
      title = s(titleFn)
    )
