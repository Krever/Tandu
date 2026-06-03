import Tooling.*
import scala.util.Try

/** Verify the Android/TWA setup end-to-end:
  *   1. the signed .aab exists,
  *   2. the local keystore fingerprint is in the generated assetlinks.json,
  *   3. the LIVE assetlinks.json on the host is valid JSON and lists it,
  *   4. Google's Digital Asset Links API confirms the web <-> app association
  *      (exactly what Chrome checks to hide the address bar).
  *
  * Run:  scala-cli run android/tooling -M verifyAndroid
  * Exits non-zero if any check fails.
  */
@main def verifyAndroid(): Unit =
  var failed = false
  def fail(msg: String): Unit = { bad(msg); failed = true }

  println("== 1. Bundle ==")
  if os.exists(aabPath) then
    pass(s"found ${aabPath.relativeTo(root)} (${os.size(aabPath) / 1024}K)")
  else
    fail(s"missing ${aabPath.relativeTo(root)} — run packageAndroid first")

  println("== 2. Local keystore fingerprint vs generated assetlinks.json ==")
  val localFp: Option[String] =
    if os.exists(keystorePath) && os.exists(credsPath) then
      val fp = normalizeFingerprint(fingerprintOf(storePassword()))
      pass(s"upload key SHA-256: $fp")
      val generated = if os.exists(assetlinksPath) then os.read(assetlinksPath) else ""
      if generated.toUpperCase.contains(fp) then
        pass("present in public/.well-known/assetlinks.json")
      else
        fail("NOT in public/.well-known/assetlinks.json — re-run packageAndroid")
      Some(fp)
    else
      fail("keystore or credentials missing — run packageAndroid first")
      None

  println(s"== 3. Live assetlinks.json on $host ==")
  val live = Try(requests.get(assetlinksUrl, check = false)).toOption
  live match
    case Some(r) if r.statusCode == 200 && Try(ujson.read(r.text())).isSuccess =>
      val ct = r.headers.getOrElse("content-type", Seq("?")).head
      pass(s"reachable and valid JSON (Content-Type: $ct)")
      val body = r.text().toUpperCase
      if body.contains(packageId.toUpperCase) then pass(s"lists package $packageId")
      else fail(s"does not list $packageId")
      localFp.foreach { fp =>
        if body.contains(fp) then pass("live file contains the upload-key fingerprint")
        else fail("live file is MISSING the fingerprint — re-run packageAndroid then ./deploy.sh")
      }
    case Some(r) if r.statusCode == 200 =>
      val ct = r.headers.getOrElse("content-type", Seq("non-JSON")).head
      fail(s"$assetlinksUrl returns $ct (SPA fallback) — file not deployed; run ./deploy.sh")
    case Some(r) =>
      fail(s"$assetlinksUrl returned HTTP ${r.statusCode} (not deployed yet? run ./deploy.sh)")
    case None =>
      fail(s"could not reach $assetlinksUrl")

  println("== 4. Google Digital Asset Links API ==")
  localFp.foreach { fp =>
    val resp = Try(
      requests.get(
        "https://digitalassetlinks.googleapis.com/v1/assetlinks:check",
        params = Map(
          "source.web.site"                                -> s"https://$host",
          "relation"                                       -> "delegate_permission/common.handle_all_urls",
          "target.androidApp.packageName"                  -> packageId,
          "target.androidApp.certificate.sha256Fingerprint" -> fp
        ),
        check = false
      )
    ).toOption
    resp match
      case Some(r) if Try(ujson.read(r.text())("linked").bool).getOrElse(false) =>
        pass("Google confirms the web <-> app association is VALID")
      case Some(r) =>
        fail("Google reports the association is NOT valid yet")
        Try(ujson.read(r.text())).foreach(j => println("      " + ujson.write(j)))
        warn("DNS/CDN caches can take a few minutes after deploy; retry shortly.")
      case None =>
        fail("could not reach the Digital Asset Links API")
  }

  println()
  if failed then
    println("[31mSome checks failed.[0m See above.")
    sys.exit(1)
  else
    println("[32mAll checks passed.[0m The TWA will launch full-screen with no address bar.")
