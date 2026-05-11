#!/usr/bin/env bash
set -euo pipefail

PASS=0
FAIL=0

pass() { echo "PASS: $1"; PASS=$((PASS+1)); }
fail() { echo "FAIL: $1"; FAIL=$((FAIL+1)); }

# title_length — Play Store limit is 30 chars; wc -c includes trailing newline so <= 31
if [ "$(wc -c < fastlane/metadata/android/en-US/title.txt)" -le 31 ]; then
    pass "title_length"
else
    fail "title_length ($(wc -c < fastlane/metadata/android/en-US/title.txt) bytes, expected <= 31)"
fi

# short_desc_length — Play Store limit is 80 chars; wc -c <= 81
if [ "$(wc -c < fastlane/metadata/android/en-US/short_description.txt)" -le 81 ]; then
    pass "short_desc_length"
else
    fail "short_desc_length ($(wc -c < fastlane/metadata/android/en-US/short_description.txt) bytes, expected <= 81)"
fi

# full_desc_length — Play Store limit is 4000 chars; wc -c <= 4001
if [ "$(wc -c < fastlane/metadata/android/en-US/full_description.txt)" -le 4001 ]; then
    pass "full_desc_length"
else
    fail "full_desc_length ($(wc -c < fastlane/metadata/android/en-US/full_description.txt) bytes, expected <= 4001)"
fi

# changelog_length — Play Store limit is 500 chars; wc -c <= 501
CHANGELOG_FAIL=0
for f in fastlane/metadata/android/en-US/changelogs/*.txt; do
    if [ "$(wc -c < "$f")" -gt 501 ]; then
        fail "changelog_length: $f is too long"
        CHANGELOG_FAIL=1
    fi
done
if [ "$CHANGELOG_FAIL" -eq 0 ]; then
    pass "changelog_length"
fi

# feature_graphic_dims — must be 1024x500
if command -v identify > /dev/null 2>&1; then
    dims=$(identify -format "%wx%h" \
        fastlane/metadata/android/en-US/images/featureGraphic.png 2>/dev/null)
    if [ "$dims" = "1024x500" ]; then
        pass "feature_graphic_dims"
    else
        fail "feature_graphic_dims (got '$dims', expected '1024x500')"
    fi
else
    # Fallback: read PNG dimensions from the IHDR chunk using Python
    dims=$(python3 -c "
import struct, sys
with open('fastlane/metadata/android/en-US/images/featureGraphic.png','rb') as f:
    f.read(16)  # signature + IHDR length + 'IHDR'
    w, h = struct.unpack('>II', f.read(8))
    print(f'{w}x{h}')
")
    if [ "$dims" = "1024x500" ]; then
        pass "feature_graphic_dims"
    else
        fail "feature_graphic_dims (got '$dims', expected '1024x500')"
    fi
fi

# at_least_two_screenshots — need >= 2 PNG files
count=$(ls fastlane/metadata/android/en-US/images/phoneScreenshots/*.png 2>/dev/null | wc -l)
if [ "$count" -ge 2 ]; then
    pass "at_least_two_screenshots ($count found)"
else
    fail "at_least_two_screenshots ($count found, need >= 2)"
fi

# no_emoji_in_metadata — grep for Unicode emoji range
if ! grep -Pr '[\x{1F300}-\x{1FAFF}]' fastlane/metadata/ 2>/dev/null; then
    pass "no_emoji_in_metadata"
else
    fail "no_emoji_in_metadata"
fi

echo ""
echo "Results: $PASS passed, $FAIL failed"
[ "$FAIL" -eq 0 ] || exit 1
