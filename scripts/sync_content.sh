#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SHA256_FILE="$REPO_ROOT/scripts/sync_content.sha256"
ASSETS_DIR="$REPO_ROOT/app/src/main/assets/html"
UPSTREAM="https://github.com/trydydd/open-circuits"

usage() {
    printf 'Usage: %s <tag> [android|ios|both]\n' "$0" >&2
    printf 'Example: %s v0.4.0\n' "$0" >&2
    exit 1
}

[[ $# -lt 1 ]] && usage

TAG="$1"
PLATFORM="${2:-both}"

case "$PLATFORM" in
    android|ios|both) ;;
    *) printf 'Unknown platform: %s (expected android, ios, or both)\n' "$PLATFORM" >&2; exit 1 ;;
esac

# Look up expected SHA256 for this tag
expected_sha=$(grep "  ${TAG}$" "$SHA256_FILE" 2>/dev/null | awk '{print $1}' || true)
if [[ -z "$expected_sha" ]]; then
    printf 'No SHA256 entry for tag %s in %s\n' "$TAG" "$SHA256_FILE" >&2
    exit 1
fi

# Determine tarball source
if [[ -n "${OC_LOCAL_TARBALL:-}" ]]; then
    tarball="$OC_LOCAL_TARBALL"
else
    tarball="/tmp/oc-${TAG}.tar.gz"
    url="${UPSTREAM}/releases/download/${TAG}/open-circuits-${TAG}.tar.gz"
    printf 'Downloading %s\n' "$url" >&2
    curl --fail --location --no-progress-meter -o "$tarball" "$url"
fi

# Verify SHA256
actual_sha=$(sha256sum "$tarball" 2>/dev/null | awk '{print $1}') || actual_sha=""
if [[ "$actual_sha" != "$expected_sha" ]]; then
    printf 'SHA256 mismatch for %s (expected %s, got %s)\n' \
        "$tarball" "$expected_sha" "${actual_sha:-<unreadable>}" >&2
    exit 1
fi

# Extract to temp directory to inspect structure
TEMP_DIR=$(mktemp -d)
trap "rm -rf '$TEMP_DIR'" EXIT
tar --strip-components=1 -C "$TEMP_DIR" -xzf "$tarball"

# Move content to final location
# If output/html/ exists (pre-built structure), use that; otherwise use root
rm -rf "$ASSETS_DIR"
mkdir -p "$ASSETS_DIR"
if [[ -d "$TEMP_DIR/output/html" && -f "$TEMP_DIR/output/html/index.html" ]]; then
    cp -r "$TEMP_DIR/output/html/." "$ASSETS_DIR/"
elif [[ -f "$TEMP_DIR/index.html" ]]; then
    cp -r "$TEMP_DIR/." "$ASSETS_DIR/"
else
    printf 'ERROR: Could not locate index.html in tarball structure\n' >&2
    exit 1
fi

printf 'Content synced to %s\n' "$ASSETS_DIR" >&2
