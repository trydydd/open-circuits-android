# open-circuits-android

Native Kotlin Android wrapper for [open-circuits](https://github.com/trydydd/open-circuits),
bundling the HTML output as a fully offline app.

Targets Google Play, F-Droid, IzzyOnDroid, and GitHub Releases.
No internet permission. No tracking. No Google Play Services.

## Requirements

- JDK 21
- Android SDK with API 35 platform and build-tools 35.0.0

## Building

```
./gradlew :app:assembleDebug
```

## Content sync

Before building a release, sync the bundled HTML from a tagged open-circuits release:

```
./scripts/sync_content.sh <tag>
```

See `scripts/sync_content.sh` and `scripts/sync_content.sha256` for the pinned content version.

## License

GPL-3.0-only. See [LICENSE](LICENSE).
