package tandu.net

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/** Thin facade over the trystero library. We bind to both the torrent
  * and nostr strategies so the caller can fall back from one to the
  * other when public signaling infra is flaky. */
@js.native
@JSImport("trystero/torrent", JSImport.Namespace)
private object TrysteroTorrentJs extends js.Object:
  def joinRoom(config: js.Dynamic, roomId: String): js.Dynamic = js.native

@js.native
@JSImport("trystero/nostr", JSImport.Namespace)
private object TrysteroNostrJs extends js.Object:
  def joinRoom(config: js.Dynamic, roomId: String): js.Dynamic = js.native

object Trystero:

  /** Which underlying signaling strategy a Room was opened with. */
  enum Strategy:
    case Torrent, Nostr

  /** A live connection to a trystero room. */
  final class Room private[Trystero] (val strategy: Strategy, private val raw: js.Dynamic):
    def onPeerJoin(fn: String => Unit): Unit =
      raw.onPeerJoin(((id: String) => fn(id)): js.Function1[String, Unit])
      ()

    def onPeerLeave(fn: String => Unit): Unit =
      raw.onPeerLeave(((id: String) => fn(id)): js.Function1[String, Unit])
      ()

    /** Create a typed channel. Returns (send, onReceive). */
    def makeAction[A <: js.Any](namespace: String): (A => Unit, (A => Unit) => Unit) =
      val tuple = raw.makeAction(namespace).asInstanceOf[js.Array[js.Dynamic]]
      val sendFn = tuple(0).asInstanceOf[js.Function1[A, js.Promise[js.Any]]]
      val recvFn = tuple(1).asInstanceOf[js.Function1[js.Function2[A, String, Unit], Unit]]
      val send: A => Unit = (a: A) => { sendFn(a); () }
      val onRecv: (A => Unit) => Unit = handler =>
        recvFn(((data: A, _: String) => handler(data)): js.Function2[A, String, Unit])
      (send, onRecv)

    def leave(): Unit =
      raw.leave()
      ()

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
  private val IceServers: js.Array[js.Object] = js.Array(
    js.Dynamic.literal(urls = "stun:stun.l.google.com:19302").asInstanceOf[js.Object],
    js.Dynamic.literal(urls = "stun:openrelay.metered.ca:80").asInstanceOf[js.Object],
    js.Dynamic.literal(
      urls = "turn:openrelay.metered.ca:80",
      username = "openrelayproject",
      credential = "openrelayproject"
    ).asInstanceOf[js.Object],
    js.Dynamic.literal(
      urls = "turn:openrelay.metered.ca:443",
      username = "openrelayproject",
      credential = "openrelayproject"
    ).asInstanceOf[js.Object],
    js.Dynamic.literal(
      urls = "turn:openrelay.metered.ca:443?transport=tcp",
      username = "openrelayproject",
      credential = "openrelayproject"
    ).asInstanceOf[js.Object]
  )

  private lazy val RtcConfig: js.Object =
    js.Dynamic.literal(iceServers = IceServers).asInstanceOf[js.Object]

  /** Curated nostr relays. We deliberately exclude relay.damus.io: it
    * rate-limits our publishes ("you are noting too much") which drops
    * ICE-candidate messages and breaks the WebRTC handshake. */
  private val NostrRelayUrls: js.Array[String] = js.Array(
    "wss://nos.lol",
    "wss://relay.nostr.band",
    "wss://relay.snort.social"
  )

  /** Join (or create) a room via the torrent strategy — public
    * WebTorrent WSS trackers, which are mature infra purpose-built
    * for peer rendezvous. This is our preferred path. */
  def joinTorrentRoom(appId: String, roomId: String): Room =
    val config = js.Dynamic.literal(
      appId = appId,
      rtcConfig = RtcConfig
    )
    new Room(Strategy.Torrent, TrysteroTorrentJs.joinRoom(config, roomId))

  /** Join (or create) a room via the nostr strategy — used as a
    * fallback if torrent doesn't surface a peer in time. */
  def joinNostrRoom(appId: String, roomId: String): Room =
    val config = js.Dynamic.literal(
      appId = appId,
      relayUrls = NostrRelayUrls,
      relayRedundancy = NostrRelayUrls.length,
      rtcConfig = RtcConfig
    )
    new Room(Strategy.Nostr, TrysteroNostrJs.joinRoom(config, roomId))
