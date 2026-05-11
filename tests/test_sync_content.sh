#!/usr/bin/env bash
# Scripted test cases for scripts/sync_content.sh (B4).
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SCRIPT="$REPO_ROOT/scripts/sync_content.sh"
PASS=0
FAIL=0

pass() { printf '[PASS] %s\n' "$1"; PASS=$((PASS + 1)); }
fail() { printf '[FAIL] %s\n' "$1"; FAIL=$((FAIL + 1)); }

# Case 1: script_executable
if test -x "$SCRIPT"; then
    pass "script_executable"
else
    fail "script_executable"
fi

# Case 2: script_rejects_unknown_tag
if ! "$SCRIPT" v0.0.0-doesnotexist 2>/dev/null; then
    pass "script_rejects_unknown_tag"
else
    fail "script_rejects_unknown_tag"
fi

# Case 3: script_verifies_sha256
# sync_content.sha256 contains a dummy hash for the 'test' tag.
# /tmp/fake.tar.gz does not exist (or has wrong content) so sha256 will not match.
output=$(OC_LOCAL_TARBALL=/tmp/fake.tar.gz "$SCRIPT" test 2>&1 || true)
if echo "$output" | grep -q "SHA256 mismatch"; then
    pass "script_verifies_sha256"
else
    fail "script_verifies_sha256 — output was: $output"
fi

# Cases 4 and 5 (extraction_target_correct, tarball_strips_top_level) require
# a real tarball and network access. Run manually:
#
#   ./scripts/sync_content.sh <tag>
#   test -f app/src/main/assets/html/index.html
#   test -f app/src/main/assets/html/DC/DC_1.html
printf '\n[NOTE] Cases extraction_target_correct and tarball_strips_top_level\n'
printf '       require a real tarball. Run manually after syncing content:\n'
printf '         ./scripts/sync_content.sh <tag>\n'
printf '         test -f app/src/main/assets/html/index.html\n'
printf '         test -f app/src/main/assets/html/DC/DC_1.html\n\n'

printf 'Results: %d passed, %d failed\n' "$PASS" "$FAIL"
[[ "$FAIL" -eq 0 ]]
