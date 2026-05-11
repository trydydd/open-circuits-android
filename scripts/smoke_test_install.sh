#!/usr/bin/env bash
# Smoke-tests B1 done_when: "APK installs on an emulator and shows a blank
# activity without crashing."
#
# Usage:
#   ./scripts/smoke_test_install.sh              # uses any running emulator/device
#   ./scripts/smoke_test_install.sh --create-avd # creates + boots a local AVD first
#
# Prerequisites:
#   ANDROID_HOME set (or sdk.dir in local.properties)
#   emulator, adb, avdmanager, sdkmanager on PATH or found via ANDROID_HOME
#   KVM available for --create-avd on Linux (check: ls /dev/kvm)

set -euo pipefail

# ── Colours ───────────────────────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'
pass() { echo -e "${GREEN}PASS${NC}  $1"; }
fail() { echo -e "${RED}FAIL${NC}  $1"; FAILURES=$((FAILURES + 1)); }
info() { echo -e "${YELLOW}INFO${NC}  $1"; }

FAILURES=0
CREATE_AVD=false
AVD_NAME="oc_smoke"
SYSTEM_IMAGE="system-images;android-35;default;x86_64"
PACKAGE_ID="org.hearth.circuits"
ACTIVITY=".MainActivity"
EMULATOR_PID=""

# ── Args ──────────────────────────────────────────────────────────────────────
for arg in "$@"; do
  case $arg in
    --create-avd) CREATE_AVD=true ;;
    *) echo "Unknown argument: $arg"; exit 1 ;;
  esac
done

# ── Resolve ANDROID_HOME ──────────────────────────────────────────────────────
if [ -z "${ANDROID_HOME:-}" ]; then
  if [ -f local.properties ]; then
    ANDROID_HOME=$(grep '^sdk.dir=' local.properties | cut -d= -f2)
  fi
fi
if [ -z "${ANDROID_HOME:-}" ]; then
  echo "ERROR: ANDROID_HOME is not set and sdk.dir not found in local.properties."
  exit 1
fi

ADB="$ANDROID_HOME/platform-tools/adb"
EMULATOR_BIN="$ANDROID_HOME/emulator/emulator"
AVDMANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/avdmanager"
SDKMANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"

for tool in "$ADB" "$EMULATOR_BIN" "$AVDMANAGER" "$SDKMANAGER"; do
  if [ ! -x "$tool" ]; then
    echo "ERROR: required tool not found or not executable: $tool"
    echo "Run: \$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager \"emulator\" \"platform-tools\""
    exit 1
  fi
done

cleanup() {
  if [ -n "$EMULATOR_PID" ]; then
    info "Shutting down emulator (pid $EMULATOR_PID)..."
    "$ADB" -s emulator-5554 emu kill 2>/dev/null || kill "$EMULATOR_PID" 2>/dev/null || true
  fi
}
trap cleanup EXIT

# ── Optionally create + start AVD ─────────────────────────────────────────────
if $CREATE_AVD; then
  if ! "$SDKMANAGER" --list_installed 2>/dev/null | grep -q "$SYSTEM_IMAGE"; then
    info "Installing system image: $SYSTEM_IMAGE"
    yes | "$SDKMANAGER" "$SYSTEM_IMAGE"
  fi

  if ! "$AVDMANAGER" list avd 2>/dev/null | grep -q "Name: $AVD_NAME"; then
    info "Creating AVD: $AVD_NAME"
    echo no | "$AVDMANAGER" create avd \
      --name "$AVD_NAME" \
      --package "$SYSTEM_IMAGE" \
      --device "pixel_6" \
      --force
  fi

  info "Starting emulator..."
  "$EMULATOR_BIN" -avd "$AVD_NAME" -no-audio -no-window -gpu swiftshader_indirect &
  EMULATOR_PID=$!

  info "Waiting for emulator to boot (up to 3 min)..."
  "$ADB" wait-for-device
  timeout 180 bash -c "
    until \"$ADB\" shell getprop sys.boot_completed 2>/dev/null | grep -q '^1$'; do
      sleep 3
    done
  "
  "$ADB" shell input keyevent 82  # unlock screen
  info "Emulator ready."
fi

# ── Check a device/emulator is connected ──────────────────────────────────────
DEVICE=$("$ADB" devices | awk '/emulator-|device$/{print $1; exit}')
if [ -z "$DEVICE" ]; then
  echo "ERROR: No emulator or device connected."
  echo "Start an AVD in Android Studio, or rerun with --create-avd."
  exit 1
fi
info "Using device: $DEVICE"

# ── Build debug APK if needed ─────────────────────────────────────────────────
APK="app/build/outputs/apk/debug/app-debug.apk"
if [ ! -f "$APK" ]; then
  info "Building debug APK..."
  ./gradlew :app:assembleDebug --quiet
fi

if [ ! -f "$APK" ]; then
  fail "APK not found at $APK after build"
  exit 1
fi
pass "APK present ($(du -sh "$APK" | cut -f1))"

# ── Uninstall any previous install ────────────────────────────────────────────
"$ADB" -s "$DEVICE" uninstall "$PACKAGE_ID" 2>/dev/null || true

# ── Install ───────────────────────────────────────────────────────────────────
if "$ADB" -s "$DEVICE" install -r "$APK" > /dev/null 2>&1; then
  pass "APK installed"
else
  fail "adb install failed"
  exit 1
fi

# ── Launch MainActivity ───────────────────────────────────────────────────────
"$ADB" -s "$DEVICE" shell am start -n "$PACKAGE_ID/$ACTIVITY" > /dev/null
sleep 3

# ── Check process is alive ────────────────────────────────────────────────────
PID=$("$ADB" -s "$DEVICE" shell pidof "$PACKAGE_ID" 2>/dev/null | tr -d '[:space:]')
if [ -n "$PID" ]; then
  pass "Process running (pid $PID)"
else
  fail "Process not found — app may have crashed on launch"
fi

# ── Check logcat for FATAL EXCEPTION in our process ──────────────────────────
CRASHES=$("$ADB" -s "$DEVICE" logcat -d -t 200 2>/dev/null \
  | grep -c "FATAL EXCEPTION.*$PACKAGE_ID\|$PACKAGE_ID.*FATAL EXCEPTION" || true)
if [ "$CRASHES" -eq 0 ]; then
  pass "No FATAL EXCEPTION in logcat"
else
  fail "FATAL EXCEPTION found in logcat ($CRASHES occurrence(s))"
  "$ADB" -s "$DEVICE" logcat -d -t 200 \
    | grep -A 20 "FATAL EXCEPTION" | head -40
fi

# ── Result ────────────────────────────────────────────────────────────────────
echo ""
if [ "$FAILURES" -eq 0 ]; then
  echo -e "${GREEN}All checks passed — B1 done_when satisfied.${NC}"
  exit 0
else
  echo -e "${RED}$FAILURES check(s) failed.${NC}"
  exit 1
fi
