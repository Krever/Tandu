import java.security.{KeyStore, MessageDigest}
import java.security.cert.X509Certificate

/** Shared helpers for the Android/TWA tooling. No bash, no JavaScript: paths,
  * the signing fingerprint, and the Digital Asset Links file all live here.
  */
object Tooling:

  // --- Locations -------------------------------------------------------------

  /** Repo root = nearest ancestor of the cwd containing package.json, so the
    * tooling works regardless of where scala-cli is invoked from. */
  val root: os.Path =
    Iterator
      .iterate(os.pwd)(_ / os.up)
      .takeWhile(p => p != p / os.up)
      .find(p => os.exists(p / "package.json"))
      .getOrElse(sys.error("Could not locate repo root (no package.json found above cwd)"))

  val androidDir: os.Path           = root / "android"
  val manifestPath: os.Path         = androidDir / "twa-manifest.json"
  val keystorePath: os.Path         = androidDir / "android-upload.keystore"
  val credsPath: os.Path            = androidDir / "keystore.env"
  val playFingerprintsPath: os.Path = androidDir / "play-app-signing-fingerprints.txt"
  val aabPath: os.Path              = androidDir / "app-release-bundle.aab"
  val assetlinksPath: os.Path       = root / "public" / ".well-known" / "assetlinks.json"

  val keyAlias = "tandu"

  // --- twa-manifest.json -------------------------------------------------------

  def manifest: ujson.Value = ujson.read(os.read(manifestPath))
  def packageId: String     = manifest("packageId").str
  def host: String          = manifest("host").str
  def assetlinksUrl: String = s"https://$host/.well-known/assetlinks.json"

  def packageVersion: String =
    ujson.read(os.read(root / "package.json"))("version").str

  // --- Signing key -------------------------------------------------------------

  /** SHA-256 of the keystore's signing certificate, formatted AA:BB:.. — read
    * straight from the keystore via the JVM, no parsing of keytool output. */
  def fingerprintOf(storePass: String): String =
    val ks = KeyStore.getInstance("PKCS12")
    val in = java.nio.file.Files.newInputStream(keystorePath.toNIO)
    try ks.load(in, storePass.toCharArray)
    finally in.close()
    val cert = ks.getCertificate(keyAlias).asInstanceOf[X509Certificate]
    MessageDigest
      .getInstance("SHA-256")
      .digest(cert.getEncoded)
      .map(b => f"${b & 0xff}%02X")
      .mkString(":")

  def normalizeFingerprint(fp: String): String = fp.trim.toUpperCase

  /** key=value pairs from keystore.env (gitignored credentials file). */
  def readCreds(): Map[String, String] =
    if !os.exists(credsPath) then Map.empty
    else
      os.read.lines(credsPath).iterator
        .map(_.trim)
        .filter(l => l.nonEmpty && !l.startsWith("#") && l.contains('='))
        .map { l => val i = l.indexOf('='); l.take(i) -> l.drop(i + 1) }
        .toMap

  def storePassword(): String =
    readCreds().getOrElse(
      "BUBBLEWRAP_KEYSTORE_PASSWORD",
      sys.error(s"No BUBBLEWRAP_KEYSTORE_PASSWORD in $credsPath — run packageAndroid first")
    )

  /** Extra fingerprints to trust, e.g. the Play "App signing key" SHA-256. */
  def playFingerprints(): Seq[String] =
    if !os.exists(playFingerprintsPath) then Seq.empty
    else
      os.read.lines(playFingerprintsPath)
        .map(_.trim)
        .filter(l => l.nonEmpty && !l.startsWith("#"))
        .toSeq

  // --- Digital Asset Links -----------------------------------------------------

  /** Write public/.well-known/assetlinks.json — the proof that tandu.app and the
    * Android app share an owner. Without a matching entry the TWA shows a
    * browser address bar instead of launching full-screen. */
  def writeAssetlinks(fingerprints: Seq[String]): Seq[String] =
    val fps = fingerprints.map(normalizeFingerprint).distinct.filter(_.nonEmpty)
    require(fps.nonEmpty, "refusing to write assetlinks.json with no fingerprints")
    val statements = ujson.Arr(
      ujson.Obj(
        "relation" -> ujson.Arr("delegate_permission/common.handle_all_urls"),
        "target" -> ujson.Obj(
          "namespace"                -> "android_app",
          "package_name"             -> packageId,
          "sha256_cert_fingerprints" -> ujson.Arr.from(fps.map(ujson.Str(_)))
        )
      )
    )
    os.makeDir.all(assetlinksPath / os.up)
    os.write.over(assetlinksPath, ujson.write(statements, indent = 2) + "\n")
    fps

  // --- Console -----------------------------------------------------------------

  def pass(msg: String): Unit = println(s"  [32m✓[0m $msg")
  def warn(msg: String): Unit = println(s"  [33m![0m $msg")
  def info(msg: String): Unit = println(s"==> $msg")
  def bad(msg: String): Unit  = println(s"  [31m✗[0m $msg")
