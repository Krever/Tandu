import Tooling.*

/** Build a signed Android App Bundle (.aab) that wraps the tandu.app PWA as a
  * Trusted Web Activity, ready to upload to the Play Console.
  *
  * Run:  scala-cli run android/tooling -M packageAndroid
  *
  * First run is interactive ONCE: Bubblewrap downloads its own JDK 17 + Android
  * SDK (answer "Yes"). Later runs are non-interactive. Outputs the .aab/.apk
  * under android/ and regenerates public/.well-known/assetlinks.json (publish it
  * with ./deploy.sh).
  */
@main def packageAndroid(): Unit =
  // --- 1. Keystore credentials (generated once, gitignored) ------------------
  if !os.exists(credsPath) then
    info(s"Generating keystore credentials -> ${credsPath.relativeTo(root)}")
    val bytes = new Array[Byte](24)
    java.security.SecureRandom.getInstanceStrong.nextBytes(bytes)
    val pw = java.util.Base64.getEncoder.encodeToString(bytes)
    os.write.over(
      credsPath,
      s"""# Bubblewrap signing credentials for Tandu. BACK THIS UP. Do not commit.
         |BUBBLEWRAP_KEYSTORE_PASSWORD=$pw
         |BUBBLEWRAP_KEY_PASSWORD=$pw
         |""".stripMargin
    )

  val creds   = readCreds()
  val storePw = creds("BUBBLEWRAP_KEYSTORE_PASSWORD")
  val keyPw   = creds("BUBBLEWRAP_KEY_PASSWORD")

  // --- 2. Signing keystore ---------------------------------------------------
  if !os.exists(keystorePath) then
    info(s"Creating signing keystore -> ${keystorePath.relativeTo(root)}")
    os.proc(
      "keytool", "-genkeypair",
      "-keystore", keystorePath.toString,
      "-alias", keyAlias,
      "-keyalg", "RSA", "-keysize", "2048", "-validity", "10000",
      "-storepass", storePw, "-keypass", keyPw,
      "-dname", "CN=Tandu, O=Tandu, C=US"
    ).call(stdin = os.Inherit, stdout = os.Inherit, stderr = os.Inherit)
    warn(s"BACK UP ${keystorePath.relativeTo(root)} and ${credsPath.relativeTo(root)} now — " +
      "losing them means you cannot publish updates.")

  // --- 3. Generate / refresh the Android project ------------------------------
  // `update` auto-increments appVersionCode in twa-manifest.json (so every build
  // is a higher, never-duplicate Play versionCode) and stamps versionName.
  // Commit the bumped twa-manifest.json to keep the counter monotonic.
  val env = Map("BUBBLEWRAP_KEYSTORE_PASSWORD" -> storePw, "BUBBLEWRAP_KEY_PASSWORD" -> keyPw)
  val versionName = packageVersion
  info(s"Updating Android project (versionName=$versionName, versionCode auto-bumped)")
  bubblewrap(env, "update", "--appVersionName", versionName)

  // --- 4. Build the signed bundle --------------------------------------------
  info("Building signed .aab / .apk")
  bubblewrap(env, "build", "--skipPwaValidation")

  // --- 5. Regenerate assetlinks.json from the signing fingerprint ------------
  info("Extracting SHA-256 fingerprint and regenerating assetlinks.json")
  val written = writeAssetlinks(fingerprintOf(storePw) +: playFingerprints())
  println(s"  wrote ${assetlinksPath.relativeTo(root)} for $packageId:")
  written.foreach(fp => println(s"    $fp"))

  println(
    s"""
       |==> Done.
       |    Bundle : ${aabPath.relativeTo(root)}              (upload to Play Console)
       |    APK    : android/app-release-signed.apk           (adb install for local test)
       |
       |Next:
       |  1. ./deploy.sh                                       # publish assetlinks.json to $host
       |  2. scala-cli run android/tooling -M verifyAndroid    # check the association is live
       |  3. Upload the .aab in Play Console. After the first upload, copy the Play
       |     "App signing key" SHA-256 (Play Console -> Test and release -> App integrity)
       |     into android/play-app-signing-fingerprints.txt, then re-run this and ./deploy.sh.
       |""".stripMargin
  )

/** Run `npx @bubblewrap/cli@latest <args>` in the android/ dir with stdio
  * inherited (so the one-time JDK/SDK install prompts work) and signing
  * passwords in the environment. */
private def bubblewrap(env: Map[String, String], args: String*): Unit =
  os.proc("npx", "--yes", "@bubblewrap/cli@latest", args)
    .call(cwd = androidDir, env = env, stdin = os.Inherit, stdout = os.Inherit, stderr = os.Inherit)
