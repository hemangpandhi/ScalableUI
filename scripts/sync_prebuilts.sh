#!/usr/bin/env bash
# Sync prebuilt_apks/ mirror from canonical assets/prebuilts/.
# Also copies tip MPL-related APKs; OemDemoRRO stays in assets/prebuilts only unless present.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SRC="${ROOT}/assets/prebuilts"
DST="${ROOT}/prebuilt_apks"

mkdir -p "$DST"
for f in \
    CarSystemUI.apk \
    CarLauncher.apk \
    CarLauncherMultiPanelRRO.apk \
    CarSystemUIScalableUIOverlay.apk \
    MultiPanelLandscapeRRO.apk \
    MockWidgets.apk \
    MockMap.apk \
    OemDemoRRO.apk \
    CarSysuiScalableBarRRO.apk
do
    if [[ -f "$SRC/$f" ]]; then
        cp -f "$SRC/$f" "$DST/$f"
        echo "synced $f"
    else
        echo "skip missing $f"
    fi
done

# Write stamp
date -u +"%Y-%m-%dT%H:%M:%SZ synced from assets/prebuilts" > "$DST/SYNC_STAMP.txt"
echo "Canonical: assets/prebuilts/  Mirror: prebuilt_apks/"
