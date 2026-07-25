#!/usr/bin/env bash
# Copyright (C) 2026 The Android Open Source Project
#
# OEM Scalable UI demonstration deployment.
#
# Usage:
#   ./scripts/deploy_ui.sh [--mode tip|oem|pleos] [--user USER_ID] [--build] [--serial DEVICE]
#
# Modes:
#   tip   (default) — MultiPanelLandscapeRRO + CarLauncher RRO + MockMap (prebuilt-safe)
#   oem             — OemDemoRRO (tip MPL panels inside oemDemo package) + CarLauncher RRO
#   pleos           — OemDemoRRO with Pleos arrays (requires CarSystemUI rebuilt with Controllers)
#
# Canonical prebuilts: assets/prebuilts/  (prebuilt_apks/ is a sync mirror)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
USER_ID="${ANDROID_USER_ID:-10}"
MODE="tip"
DO_BUILD=false
REMOTE_PIXEL_BUILD=false
ADB_SERIAL=""

while [[ $# -gt 0 ]]; do
    case "$1" in
        --mode) MODE="$2"; shift 2 ;;
        --user) USER_ID="$2"; shift 2 ;;
        --build) DO_BUILD=true; shift ;;
        --pixel) REMOTE_PIXEL_BUILD=true; shift ;;
        --serial) ADB_SERIAL="$2"; shift 2 ;;
        -h|--help)
            sed -n '2,17p' "$0"
            exit 0
            ;;
        *) echo "Unknown option: $1"; exit 1 ;;
    esac
done

ADB=(adb)
[[ -n "$ADB_SERIAL" ]] && ADB+=(-s "$ADB_SERIAL")

PREBUILT="${REPO_ROOT}/assets/prebuilts"
# Prefer canonical tree; fall back to mirror
[[ -d "$PREBUILT" ]] || PREBUILT="${REPO_ROOT}/prebuilt_apks"

PKG_MPL="com.android.systemui.rro.scalableUI.multiPanelLandscape"
PKG_OEM="com.android.systemui.rro.scalableUI.oemDemo"
PKG_BARS="com.android.systemui.rro.scalableUI.sysuiBars"
PKG_SCALABLE="com.android.systemui.rro.scalableUI.carSystemUI"
PKG_LAUNCHER_RRO="com.android.car.carlauncher.rro.scalableUI.multiPanelLandscape"

log() { echo "[deploy_ui] $*"; }

need_root() {
    log "Ensuring adb root + remount..."
    "${ADB[@]}" root >/dev/null
    sleep 1
    "${ADB[@]}" remount >/dev/null || true
}

push_apk() {
    local apk="$1"
    local dest="$2"
    if [[ ! -f "$apk" ]]; then
        log "SKIP missing: $apk"
        return 0
    fi
    log "Push $apk -> $dest"
    "${ADB[@]}" shell mkdir -p "$(dirname "$dest")"
    "${ADB[@]}" push "$apk" "$dest"
}

install_runtime() {
    local apk="$1"
    if [[ -f "$apk" ]]; then
        log "pm install -r $apk"
        "${ADB[@]}" install -r -d -g "$apk" 2>/dev/null \
            || "${ADB[@]}" shell pm install -r -d -g < "$apk" 2>/dev/null \
            || true
    fi
}

if $DO_BUILD; then
    log "Building demo targets..."
    case "$MODE" in
        tip)
            m MockWidgets MultiPanelLandscapeRRO CarLauncherMultiPanelRRO \
              CarSystemUIScalableUIOverlay CarSystemUI CarLauncher
            ;;
        oem|pleos)
            m MockWidgets OemDemoRRO CarLauncherMultiPanelRRO \
              CarSysuiScalableBarControllers CarSystemUI CarLauncher
            ;;
    esac
fi

if $REMOTE_PIXEL_BUILD; then
    log "Triggering remote Pixel build via SSH..."
    "${SCRIPT_DIR}/remote_build.sh" || { log "Remote build failed!"; exit 1; }
    
    log "Syncing prebuilts from remote laptop..."
    "${SCRIPT_DIR}/sync_prebuilts.sh" || { log "Sync failed!"; exit 1; }
fi

need_root

# Always push core runtime stack for tip/oem demos
push_apk "${PREBUILT}/CarSystemUI.apk" /system/priv-app/CarSystemUI/CarSystemUI.apk
push_apk "${PREBUILT}/CarLauncher.apk" /system_ext/priv-app/CarLauncher/CarLauncher.apk
push_apk "${PREBUILT}/MockWidgets.apk" /system/app/MockWidgets/MockWidgets.apk
push_apk "${PREBUILT}/MockMap.apk" /system/app/MockMap/MockMap.apk
push_apk "${PREBUILT}/CarLauncherMultiPanelRRO.apk" /product/overlay/CarLauncherMultiPanelRRO.apk
push_apk "${PREBUILT}/CarSystemUIScalableUIOverlay.apk" \
    /product/overlay/CarSystemUIScalableUIOverlay/CarSystemUIScalableUIOverlay.apk

# Also try OUT paths when present
if [[ -n "${OUT:-}" ]]; then
    push_apk "${OUT}/system_ext/priv-app/CarSystemUI/CarSystemUI.apk" /system/priv-app/CarSystemUI/CarSystemUI.apk
    push_apk "${OUT}/system_ext/priv-app/CarLauncher/CarLauncher.apk" /system_ext/priv-app/CarLauncher/CarLauncher.apk
fi

log "Disabling all Scalable UI overlays..."
for pkg in "$PKG_MPL" "$PKG_OEM" "$PKG_BARS" "$PKG_SCALABLE" "$PKG_LAUNCHER_RRO"; do
    "${ADB[@]}" shell cmd overlay disable --user 0 "$pkg" 2>/dev/null || true
    "${ADB[@]}" shell cmd overlay disable --user "$USER_ID" "$pkg" 2>/dev/null || true
done

case "$MODE" in
    tip)
        push_apk "${PREBUILT}/MultiPanelLandscapeRRO.apk" /product/overlay/MultiPanelLandscapeRRO.apk
        install_runtime "${PREBUILT}/MultiPanelLandscapeRRO.apk"
        log "Enabling tip MultiPanelLandscape + CarLauncher RRO..."
        "${ADB[@]}" shell cmd overlay enable --user 0 "$PKG_MPL" 2>/dev/null || true
        "${ADB[@]}" shell cmd overlay enable --user "$USER_ID" "$PKG_MPL" 2>/dev/null || true
        "${ADB[@]}" shell cmd overlay enable --user 0 "$PKG_LAUNCHER_RRO" 2>/dev/null || true
        "${ADB[@]}" shell cmd overlay enable --user "$USER_ID" "$PKG_LAUNCHER_RRO" 2>/dev/null || true
        "${ADB[@]}" shell cmd overlay enable --user 0 "$PKG_SCALABLE" 2>/dev/null || true
        "${ADB[@]}" shell cmd overlay enable --user "$USER_ID" "$PKG_SCALABLE" 2>/dev/null || true
        ;;
    oem)
        push_apk "${PREBUILT}/OemDemoRRO.apk" /product/overlay/OemDemoRRO.apk
        install_runtime "${PREBUILT}/OemDemoRRO.apk"
        log "Enabling OemDemoRRO (tip panels, prebuilt-safe) + CarLauncher RRO..."
        "${ADB[@]}" shell cmd overlay enable --user 0 "$PKG_OEM" 2>/dev/null || true
        "${ADB[@]}" shell cmd overlay enable --user "$USER_ID" "$PKG_OEM" 2>/dev/null || true
        "${ADB[@]}" shell cmd overlay enable --user 0 "$PKG_LAUNCHER_RRO" 2>/dev/null || true
        "${ADB[@]}" shell cmd overlay enable --user "$USER_ID" "$PKG_LAUNCHER_RRO" 2>/dev/null || true
        ;;
    pleos)
        push_apk "${PREBUILT}/OemDemoRRO.apk" /product/overlay/OemDemoRRO.apk
        push_apk "${PREBUILT}/CarSysuiScalableBarRRO.apk" /product/overlay/CarSysuiScalableBarRRO.apk
        install_runtime "${PREBUILT}/OemDemoRRO.apk"
        install_runtime "${PREBUILT}/CarSysuiScalableBarRRO.apk"
        log "WARNING: pleos mode needs CarSystemUI rebuilt with CarSysuiScalableBarControllers."
        log "Point overlays.xml at window_states_pleos after linking Controllers, then rebuild OemDemoRRO."
        "${ADB[@]}" shell cmd overlay enable --user 0 "$PKG_OEM" 2>/dev/null || true
        "${ADB[@]}" shell cmd overlay enable --user "$USER_ID" "$PKG_OEM" 2>/dev/null || true
        "${ADB[@]}" shell cmd overlay enable --user 0 "$PKG_LAUNCHER_RRO" 2>/dev/null || true
        "${ADB[@]}" shell cmd overlay enable --user "$USER_ID" "$PKG_LAUNCHER_RRO" 2>/dev/null || true
        "${ADB[@]}" shell cmd overlay enable --user 0 "com.android.systemui.rro.scalableUI.sysuiBars" 2>/dev/null || true
        "${ADB[@]}" shell cmd overlay enable --user "$USER_ID" "com.android.systemui.rro.scalableUI.sysuiBars" 2>/dev/null || true
        ;;
    *)
        log "Unknown mode: $MODE (use tip|oem|pleos)"
        exit 1
        ;;
esac

log "Restoring permissions..."
"${ADB[@]}" shell pm grant --user "$USER_ID" com.android.systemui android.permission.BLUETOOTH_CONNECT 2>/dev/null || true
"${ADB[@]}" shell pm grant --user "$USER_ID" com.android.car.mockwidgets android.car.permission.CONTROL_CAR_CLIMATE 2>/dev/null || true

log "Restarting system (stop/start)..."
"${ADB[@]}" shell stop || true
sleep 2
"${ADB[@]}" shell start || true
sleep 3

log "Overlay state (user $USER_ID):"
"${ADB[@]}" shell cmd overlay list --user "$USER_ID" 2>/dev/null \
    | grep -E "scalableUI|multiPanel|oemDemo|sysuiBars|carlauncher.rro" || true

log "Done. mode=$MODE prebuilts=$PREBUILT"
log "Verify: home panel, 3 floating pills (tip/oem), map, app grid."
