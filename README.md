# open-circuits-android

Native Kotlin Android wrapper for [open-circuits](https://github.com/trydydd/open-circuits),
bundling the HTML output as a fully offline app.

Targets Google Play, F-Droid, IzzyOnDroid, and GitHub Releases.
No internet permission. No tracking. No Google Play Services.

## Development setup

### 1. JDK 21

The build requires JDK 21. Check your version:

```
java -version
```

If you need to install it:
- **macOS/Linux**: use [SDKMAN](https://sdkman.io/) — `sdk install java 21-tem`
- **Windows**: download the Temurin 21 installer from [adoptium.net](https://adoptium.net/)
- **Linux package managers**: `apt install openjdk-21-jdk` / `dnf install java-21-openjdk-devel`

### 2. Android SDK

**Option A — Android Studio (recommended for most contributors)**

Install [Android Studio](https://developer.android.com/studio). It bundles the SDK and sets
`ANDROID_HOME` automatically. In the SDK Manager, ensure these are installed:

- SDK Platform: Android 15 (API 35)
- SDK Build-Tools: 35.0.0

**Option B — command-line tools only**

Download the [Android command-line tools](https://developer.android.com/studio#command-tools),
extract to `~/android-sdk/cmdline-tools/latest/`, then:

```
export ANDROID_HOME=~/android-sdk
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
sdkmanager "platforms;android-35" "build-tools;35.0.0" "platform-tools"
```

Add the `export ANDROID_HOME` line to your shell profile so it persists.

### 3. SDK location

If `ANDROID_HOME` is set (Android Studio sets it automatically), no extra config is needed.

If you installed the SDK to a non-standard path and `ANDROID_HOME` is not set, create
`local.properties` in the repo root (it is gitignored):

```
sdk.dir=/path/to/your/android-sdk
```

### 4. Clone and verify

```
git clone https://github.com/trydydd/open-circuits-android.git
cd open-circuits-android
./gradlew :app:assembleDebug
```

A successful build produces `app/build/outputs/apk/debug/app-debug.apk`.

## Building

```sh
# Debug APK
./gradlew :app:assembleDebug

# Unit tests
./gradlew :app:testDebugUnitTest

# Lint
./gradlew :app:lintDebug
```

## Content sync

Before building a release, sync the bundled HTML from a tagged open-circuits release:

```sh
./scripts/sync_content.sh v0.4.0
```

This downloads the tarball from the open-circuits GitHub releases page, verifies its
SHA256 against `scripts/sync_content.sha256`, strips the top-level directory, and
extracts the HTML into `app/src/main/assets/html/`. Commit the result.

**Bumping the content version:**

1. Find the new tag in the [open-circuits releases](https://github.com/trydydd/open-circuits/releases).
2. Download the tarball and compute its SHA256:
   ```sh
   curl -fL -o /tmp/oc.tar.gz \
     https://github.com/trydydd/open-circuits/archive/refs/tags/<tag>.tar.gz
   sha256sum /tmp/oc.tar.gz
   ```
3. Add a line to `scripts/sync_content.sha256`:
   ```
   <sha256>  <tag>
   ```
4. Run the sync script and commit the extracted assets:
   ```sh
   ./scripts/sync_content.sh <tag>
   git add app/src/main/assets/html/
   git commit -m "sync content to <tag>"
   ```

The `OC_LOCAL_TARBALL` environment variable skips the download and uses a local file:
```sh
OC_LOCAL_TARBALL=/path/to/tarball.tar.gz ./scripts/sync_content.sh <tag>
```

## License

GPL-3.0-only. See [LICENSE](LICENSE).
