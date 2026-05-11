#!/usr/bin/env bash
# Verify that two sequential clean builds of the foss release APK are content-identical.
#
# "Content-identical" means every file within the APK (code, resources, assets) has the
# same SHA256 between both builds, and the ZIP central directory CRC32/size entries match.
# The APK signing block is excluded from comparison: AGP embeds a per-build PKDS
# (dependency-info) block with a random nonce by design; byte-level APK identity is
# therefore not achievable without patching AGP. F-Droid strips and re-signs the APK
# anyway, so content identity is the correct reproducibility criterion.
#
# Usage:  ./scripts/verify_reproducible.sh
# Exits 0 on identical content, 1 on mismatch or build failure.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
APK_OUT="app/build/outputs/apk/foss/release/app-foss-release.apk"
APK_A="/tmp/build-a.apk"
APK_B="/tmp/build-b.apk"
EXTRACT_A="/tmp/apk-extract-a"
EXTRACT_B="/tmp/apk-extract-b"

cleanup() {
    rm -rf "$EXTRACT_A" "$EXTRACT_B"
}
trap cleanup EXIT

cd "$PROJECT_DIR"

echo "=== Build 1 ==="
./gradlew :app:assembleFossRelease --no-daemon
cp "$APK_OUT" "$APK_A"

echo "=== Build 2 (clean, no build cache) ==="
./gradlew clean :app:assembleFossRelease --no-daemon --no-build-cache
cp "$APK_OUT" "$APK_B"

echo "=== Comparing APK contents ==="

# 1. Check that every ZIP entry (CRC32 + size + name) matches.
#    This is a fast check covering all files, including META-INF.
#    The signing block lives OUTSIDE the ZIP entries, so CRC32 comparison
#    is unaffected by the non-deterministic PKDS signing block.
MANIFEST_A="$(unzip -lv "$APK_A" | awk 'NF>=8{print $7,$1,$8}' | sort)"
MANIFEST_B="$(unzip -lv "$APK_B" | awk 'NF>=8{print $7,$1,$8}' | sort)"
if [ "$MANIFEST_A" != "$MANIFEST_B" ]; then
    echo "MISMATCH: ZIP entry CRC32/size/name lists differ."
    diff <(echo "$MANIFEST_A") <(echo "$MANIFEST_B") || true
    exit 1
fi
echo "  ZIP entries (CRC32 + sizes + names): identical"

# 2. Extract both APKs and compare SHA256 of every non-signature file.
#    Excludes META-INF/ which contains signatures (those are inherently
#    build-variant-specific and stripped by F-Droid before comparison).
rm -rf "$EXTRACT_A" "$EXTRACT_B"
unzip -q "$APK_A" -d "$EXTRACT_A"
unzip -q "$APK_B" -d "$EXTRACT_B"

HASH_A="$(find "$EXTRACT_A" -type f | grep -v '/META-INF/' | sort \
          | xargs sha256sum | sed "s|$EXTRACT_A/||" | sort)"
HASH_B="$(find "$EXTRACT_B" -type f | grep -v '/META-INF/' | sort \
          | xargs sha256sum | sed "s|$EXTRACT_B/||" | sort)"

if [ "$HASH_A" != "$HASH_B" ]; then
    echo "MISMATCH: extracted file contents differ (non-META-INF)."
    diff <(echo "$HASH_A") <(echo "$HASH_B") || true
    exit 1
fi
echo "  Extracted file SHA256 (non-META-INF): identical"

echo "OK: builds are content-identical."
