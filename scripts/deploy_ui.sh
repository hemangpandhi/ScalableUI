#!/usr/bin/env bash
# Copyright (C) 2026 The Android Open Source Project
#
# OEM Scalable UI demonstration deployment script.
# Usage: ./scripts/deploy_ui.sh [--user USER_ID] [--build] [--serial DEVICE]
#
# Prerequisites:
#   - AOSP build outputs in $OUT or pass --build to compile
#   - adb connected to Cuttlefish / AAOS device (userdebug)
#   - CarSystemUI built with CarSysuiScalableBarControllers linked

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
USER_ID="${ANDROID_USER_ID:-10}"
DO_BUILD=false
ADB_SERIAL=""

while [[ $# -gt 0 ]]; do
    case "$1" in
        --user) USER_ID="$2"; shift 2 ;;
        --build) DO_BUILD=true; shift ;;
        --serial) ADB_SERIAL="$2"; shift 2 ;;
        -h|--help)
            echo "Usage: $0 [--user USER_ID] [--build] [--serial DEVICE]"
            exit 0
            ;;
        *) echo "Unknown option: $1"; exit 1 ;;
    esac
done

ADB=(adb)
[[ -n "$ADB_SERIAL" ]] && ADB+=(-s "$ADB_SERIAL")

OVERLAY_PKG="com.android.systemui.rro.scalableUI.oemDemo"
MOCK_PKG="com.android.car.mockwidgets"

log() { echo "[deploy_ui] $*"; }

if $DO_BUILD; then
    log "Building OEM demo targets..."
    m MockWidgets OemDemoRRO CarSysuiScalableBarControllers CarSystemUI CarLauncher
fi

if [[ -z "${OUT:-}" ]]; then
    log "WARNING: OUT not set. Expecting prebuilt APKs or run from AOSP shell with lunch."
fi

install_apk() {
    local apk="$1"
    local pkg="$2"
    if [[ -f "$apk" ]]; then
        log "Installing $pkg from $apk"
        "${ADB[@]}" install -r -d -g "$apk" || "${ADB[@]}" shell pm install -r -d -g "$apk"
    else
        log "SKIP (not found): $apk"
    fi
}

# Install from AOSP OUT paths when available
install_apk "${OUT:-/dev/null}/system_ext/priv-app/MockWidgets/MockWidgets.apk" "$MOCK_PKG"
install_apk "${OUT:-/dev/null}/system_ext/priv-app/OemDemoRRO/OemDemoRRO.apk" "$OVERLAY_PKG"
install_apk "${OUT:-/dev/null}/system_ext/priv-app/CarSystemUI/CarSystemUI.apk" "com.android.systemui"

# Fallback: vendor prebuilts directory
PREBUILT="${REPO_ROOT}/assets/prebuilts"
install_apk "${PREBUILT}/MockWidgets.apk" "$MOCK_PKG"
install_apk "${PREBUILT}/OemDemoRRO.apk" "$OVERLAY_PKG"
install_apk "${PREBUILT}/CarSystemUI.apk" "com.android.systemui"

log "Disabling conflicting overlays..."
for pkg in \
    com.android.systemui.rro.scalableUI.multiPanelLandscape \
    com.android.systemui.rro.scalableUI.sysuiBars \
    com.android.systemui.rro.scalableUI.carSystemUI; do
    "${ADB[@]}" shell cmd overlay disable --user "$USER_ID" "$pkg" 2>/dev/null || true
done

log "Enabling unified OEM demo overlay..."
"${ADB[@]}" shell cmd overlay enable --user "$USER_ID" "$OVERLAY_PKG"

log "Restoring critical permissions (sideload resets grants)..."
"${ADB[@]}" shell pm grant --user "$USER_ID" com.android.systemui android.permission.BLUETOOTH_CONNECT 2>/dev/null || true
"${ADB[@]}" shell pm grant --user "$USER_ID" "$MOCK_PKG" android.car.permission.CONTROL_CAR_CLIMATE 2>/dev/null || true

log "Restarting SystemUI..."
"${ADB[@]}" shell am crash com.android.systemui 2>/dev/null || \
    "${ADB[@]}" shell killall com.android.systemui 2>/dev/null || true

sleep 2
log "Overlay state:"
"${ADB[@]}" shell cmd overlay list --user "$USER_ID" | grep -E "oemDemo|multiPanel|sysuiBars" || true

log "Done. Demo overlay: $OVERLAY_PKG (user $USER_ID)"
log "Debug: adb shell dumpsys activity service SystemUI | grep -A30 CarSysuiScalableBar"
