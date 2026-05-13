# Releasing Open Circuits Android

This document covers the full release process from scratch, including one-time
setup steps and the routine steps for each subsequent release.

---

## One-time setup

### 1. Create the Android signing keystore

Your signing key proves every release came from you. Create it once and keep it.
**Losing it means you can never update the app on Google Play** — back it up
somewhere outside the repo (password manager, offline storage).

```bash
keytool -genkeypair \
  -keystore release.jks \
  -alias open-circuits \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000 \
  -storepass <choose-a-strong-password> \
  -keypass <choose-a-strong-password> \
  -dname "CN=Open Circuits, O=Hearth, C=US"
```

Then encode it for the GitHub secret:

```bash
base64 -w 0 release.jks > release.jks.b64
```

Note the four values you will need as secrets:

| Secret | Value |
|---|---|
| `KEYSTORE_BASE64` | contents of `release.jks.b64` |
| `KEYSTORE_PASSWORD` | the `--storepass` value above |
| `KEY_ALIAS` | `open-circuits` (or whatever alias you chose) |
| `KEY_PASSWORD` | the `--keypass` value above |

### 2. Create a Google Play developer account

1. Go to [play.google.com/console](https://play.google.com/console) and sign up.
   There is a one-time $25 USD registration fee.
2. Fill in your developer profile (name, email, address).
3. Accept the developer distribution agreement.

### 3. Create the app listing on Play Console

1. Click **Create app**, set the package name to `org.hearth.circuits`, language
   English.
2. Fill in the store listing. The text files in
   `fastlane/metadata/android/en-US/` contain exactly this content — copy them
   in.
3. Upload the feature graphic (`images/featureGraphic.png`) and screenshots
   (`images/phoneScreenshots/`).
4. Complete the **Content rating** questionnaire. This app scores lowest on
   every category (no violence, no ads, no user-generated content).
5. Complete the **Data safety** form. Answer "no" to every question — the app
   collects nothing and has no internet access. Link to the bundled
   `privacy.html` page if a URL is required.
6. Set app pricing to **Free**.

### 4. Create a Google Play service account

The service account lets the CI workflow upload AABs without manual console
clicks.

1. Open [Google Cloud Console](https://console.cloud.google.com) and create a
   project (or use an existing one).
2. Enable the **Google Play Android Developer API** on that project.
3. Go to **IAM & Admin > Service Accounts** and create a service account.
   Give it no project-level roles.
4. Create and download a JSON key for the service account. The full contents of
   this file become the `PLAY_SERVICE_ACCOUNT_JSON` secret.
5. Back in Play Console, go to **Setup > API access**, link your Google Cloud
   project, find the service account, and grant it **Release manager**
   permissions.

### 5. Do the first upload manually

Play requires at least one release to be uploaded through the console before
the API can push subsequent ones.

Build a signed AAB locally first. Write `keystore.properties` (this file is
gitignored):

```
keystorePath=/path/to/release.jks
keystorePassword=<your-keystore-password>
keyAlias=open-circuits
keyPassword=<your-key-password>
```

Then build and upload:

```bash
./gradlew :app:bundlePlayRelease
```

In Play Console go to **Testing > Internal testing**, create a release, and
upload `app/build/outputs/bundle/playRelease/app-play-release.aab`.

Once that first release exists the service account can upload all future ones.

### 6. Add secrets to GitHub

In your repo go to **Settings > Secrets and variables > Actions > New
repository secret** and add each of the following:

| Secret name | Where it comes from |
|---|---|
| `KEYSTORE_BASE64` | Contents of `release.jks.b64` (step 1) |
| `KEYSTORE_PASSWORD` | Keystore password (step 1) |
| `KEY_ALIAS` | Key alias (step 1) |
| `KEY_PASSWORD` | Key password (step 1) |
| `PLAY_SERVICE_ACCOUNT_JSON` | Full JSON file contents (step 4) |

---

## Each release

Follow the checklist in `.github/RELEASE_CHECKLIST.md` before tagging.

In short:

1. Run `./scripts/sync_content.sh <open-circuits-tag>` if the bundled content
   has changed and commit the result.
2. Bump `versionCode` and `versionName` in `app/build.gradle.kts`.
3. Add `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt` (500
   chars max).
4. Update screenshots in `fastlane/metadata/android/en-US/images/` if the UI
   changed.
5. Run local checks:
   ```bash
   ./gradlew :app:lintFossRelease :app:testFossReleaseUnitTest :app:assembleFossRelease
   ./scripts/verify_reproducible.sh
   ./gradlew :app:assertNoGms
   ```
6. Tag and push:
   ```bash
   git tag -s v1.0.0 -m "v1.0.0"
   git push origin v1.0.0
   ```

The release workflow (`.github/workflows/release.yml`) then builds and signs
the AAB and APK, uploads the AAB to the Play internal track, and attaches the
APK and its sha256 checksum to the GitHub Release.

After the workflow completes, promote the release from internal to production
manually in Play Console.

---

## F-Droid

F-Droid requires no account and no fee. When ready:

1. Complete task B11 (the `fdroid/metadata/org.hearth.circuits.yml` file must
   exist and `fdroid lint` must pass cleanly).
2. Verify two reproducible builds locally (`./scripts/verify_reproducible.sh`).
3. Open a pull request to the
   [fdroiddata](https://gitlab.com/fdroid/fdroiddata) repository with the
   metadata file.

F-Droid's infrastructure builds the APK from source independently and
publishes it. No signing on your end is involved.
