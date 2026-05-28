package tandu.net

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/** Thin facade over the trystero library. We bind to both the torrent
  * and nostr strategies so the caller can fall back from one to the
  * other when public signaling infra is flaky. */
@js.native
@JSImport("trystero/torrent", JSImport.Namespace)
private object TrysteroTorrentJs extends js.Object:
  def joinRoom(config: TrysteroRoomConfig, roomId: String): TrysteroRoomJs = js.native

@js.native
@JSImport("trystero/nostr", JSImport.Namespace)
private object TrysteroNostrJs extends js.Object:
  def joinRoom(config: TrysteroRoomConfig, roomId: String): TrysteroRoomJs = js.native

/** Native facade for a trystero room object. */
@js.native
private trait TrysteroRoomJs extends js.Object:
  def onPeerJoin(fn: js.Function1[String, Unit]): Unit = js.native
  def onPeerLeave(fn: js.Function1[String, Unit]): Unit = js.native
  def makeAction[A <: js.Any](namespace: String): js.Tuple2[
    js.Function1[A, js.Promise[js.Any]],
    js.Function1[js.Function2[A, String, Unit], Unit]
  ] = js.native
  def leave(): Unit = js.native

/** Configuration object passed to `joinRoom`. trystero accepts a single
  * shape with optional fields used by different strategies. */
private trait TrysteroRoomConfig extends js.Object:
  val appId: String
  val rtcConfig: RtcConfig
  val relayUrls: js.UndefOr[js.Array[String]]
  val relayRedundancy: js.UndefOr[Int]

private trait RtcConfig extends js.Object:
  val iceServers: js.Array[IceServer]

private trait IceServer extends js.Object:
  val urls: String
  val username: js.UndefOr[String]
  val credential: js.UndefOr[String]

object Trystero:

  /** Which underlying signaling strategy a Room was opened with. */
  enum Strategy:
    case Torrent, Nostr

  /** A live connection to a trystero room. */
  final class Room private[Trystero] (val strategy: Strategy, private val raw: TrysteroRoomJs):
    def onPeerJoin(fn: String => Unit): Unit =
      raw.onPeerJoin((id: String) => fn(id))

    def onPeerLeave(fn: String => Unit): Unit =
      raw.onPeerLeave((id: String) => fn(id))

    /** Create a typed channel. Returns (send, onReceive). */
    def makeAction[A <: js.Any](namespace: String): (A => Unit, (A => Unit) => Unit) =
      val tuple  = raw.makeAction[A](namespace)
      val sendFn = tuple._1
      val recvFn = tuple._2
      val send: A => Unit = (a: A) => { sendFn(a); () }
      val onRecv: (A => Unit) => Unit = handler =>
        recvFn((data: A, _: String) => handler(data))
      (send, onRecv)

    def leave(): Unit =
      raw.leave()

  private def iceServer(
      urls: String,
      username: js.UndefOr[String] = js.undefined,
      credential: js.UndefOr[String] = js.undefined
  ): IceServer =
    val _urls = urls
    val _username = username
    val _credential = credential
    new IceServer:
      val urls = _urls
      val username = _username
      val credential = _credential

  /** ICE servers used by every Room. STUN handles the common case
    * (peers behind home routers). TURN is the fallback for strict NATs
    * (notably mobile carrier CGNAT) where direct UDP between peers is
    * impossible — without it, two phones on cellular often can't
    * connect at all.
    *
    * `openrelay.metered.ca` is a free, no-signup public TURN service
    * with credentials embedded openly in many WebRTC apps. It's not
    * meant for production: if usage grows we should switch to
    * Cloudflare Realtime TURN (10GB/month free with signup) or a
    * self-hosted coturn instance. */
  private val IceServers: js.Array[IceServer] = js.Array(
    iceServer("stun:stun.l.google.com:19302"),
    iceServer("stun:openrelay.metered.ca:80"),
    iceServer("turn:openrelay.metered.ca:80",      "openrelayproject", "openrelayproject"),
    iceServer("turn:openrelay.metered.ca:443",     "openrelayproject", "openrelayproject"),
    iceServer("turn:openrelay.metered.ca:443?transport=tcp", "openrelayproject", "openrelayproject")
  )

  private lazy val RtcConfig: RtcConfig =
    val servers = IceServers
    new RtcConfig:
      val iceServers = servers

  /** Curated nostr relays. We deliberately exclude relay.damus.io: it
    * rate-limits our publishes ("you are noting too much") which drops
    * ICE-candidate messages and breaks the WebRTC handshake. */
  private val NostrRelayUrls: js.Array[String] = js.Array(
    "wss://nos.lol",
    "wss://relay.nostr.band",
    "wss://relay.snort.social"
  )

  private def torrentConfig(appId: String): TrysteroRoomConfig =
    val _appId = appId
    val _rtc = RtcConfig
    new TrysteroRoomConfig:
      val appId           = _appId
      val rtcConfig       = _rtc
      val relayUrls       = js.undefined
      val relayRedundancy = js.undefined

  private def nostrConfig(appId: String): TrysteroRoomConfig =
    val _appId = appId
    val _rtc = RtcConfig
    val _relays = NostrRelayUrls
    new TrysteroRoomConfig:
      val appId           = _appId
      val rtcConfig       = _rtc
      val relayUrls       = _relays
      val relayRedundancy = _relays.length

  /** Join (or create) a room via the torrent strategy — public
    * WebTorrent WSS trackers, which are mature infra purpose-built
    * for peer rendezvous. This is our preferred path. */
  def joinTorrentRoom(appId: String, roomId: String): Room =
    new Room(Strategy.Torrent, TrysteroTorrentJs.joinRoom(torrentConfig(appId), roomId))

  /** Join (or create) a room via the nostr strategy — used as a
    * fallback if torrent doesn't surface a peer in time. */
  def joinNostrRoom(appId: String, roomId: String): Room =
    new Room(Strategy.Nostr, TrysteroNostrJs.joinRoom(nostrConfig(appId), roomId))
