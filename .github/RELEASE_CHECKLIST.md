# Release Checklist

Pre-flight steps before tagging a release. Complete all items, then push the tag.

## Before tagging

- [ ] Content tarball synced — run `./scripts/sync_content.sh <tag>` with the target open-circuits version and commit the extracted assets
- [ ] Version bumped — update `versionCode` and `versionName` in `app/build.gradle.kts`
- [ ] Changelog added — create `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt` (500 chars max)
- [ ] Screenshots updated — if UI changed, recapture per `fastlane/README` and replace `phoneScreenshots/`

## Local verification

- [ ] `./gradlew :app:lintFossRelease` exits 0
- [ ] `./gradlew :app:testFossReleaseUnitTest` passes
- [ ] `./gradlew :app:assembleFossRelease` produces a valid APK
- [ ] `./scripts/verify_reproducible.sh` exits 0
- [ ] `./gradlew :app:assertNoGms` exits 0

## Tagging

```
git tag -s v<version> -m "v<version>"
git push origin v<version>
```

The release workflow builds, signs, and publishes automatically.

## After the workflow completes

- [ ] Verify APK attached to the GitHub Release and sha256 checksum matches
- [ ] Review the build on Play Console internal track before promoting
- [ ] Promote from internal to production manually in Play Console
