#!/usr/bin/env bash
# Regenerate overlays/OemDemoRRO from tip MultiPanelLandscapeRRO + Pleos bar assets.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MPL="${ROOT}/overlays/MultiPanelLandscapeRRO"
BARS="${ROOT}/overlays/CarSysuiScalableBarRRO"
OEM="${ROOT}/overlays/OemDemoRRO"

# Preserve OemDemo-specific config/manifest if present
TMP=$(mktemp -d)
[[ -f "$OEM/res/values/config.xml" ]] && cp "$OEM/res/values/config.xml" "$TMP/config.xml" || true
[[ -f "$OEM/AndroidManifest.xml" ]] && cp "$OEM/AndroidManifest.xml" "$TMP/AndroidManifest.xml" || true
[[ -f "$OEM/Android.bp" ]] && cp "$OEM/Android.bp" "$TMP/Android.bp" || true

rm -rf "$OEM"
cp -r "$MPL" "$OEM"

cp "$BARS"/res/layout/car_sysui*.xml "$OEM/res/layout/"
cp "$BARS"/res/xml/sysui*.xml "$OEM/res/xml/"
cp "$BARS"/res/drawable/sysui*.xml "$OEM/res/drawable/" 2>/dev/null || true
cp "$BARS"/res/drawable/ic_sysui*.xml "$OEM/res/drawable/" 2>/dev/null || true
cp "$BARS"/res/values/colors.xml "$OEM/res/values/sysui_colors.xml"
cp "$BARS"/res/values/dimens.xml "$OEM/res/values/sysui_dimens.xml"
cp "$BARS"/res/values/strings.xml "$OEM/res/values/sysui_strings.xml"

# Restore OemDemo package identity + unified config if we had them
[[ -f "$TMP/Android.bp" ]] && cp "$TMP/Android.bp" "$OEM/Android.bp"
[[ -f "$TMP/AndroidManifest.xml" ]] && cp "$TMP/AndroidManifest.xml" "$OEM/AndroidManifest.xml"
[[ -f "$TMP/config.xml" ]] && cp "$TMP/config.xml" "$OEM/res/values/config.xml"

# Ensure controller RRO package points at oemDemo
sed -i 's/scalableUI\.sysuiBars/scalableUI.oemDemo/g' \
    "$OEM"/res/xml/sysui_scalable_*_controller.xml 2>/dev/null || true

rm -rf "$TMP"
echo "OemDemoRRO regenerated from tip MPL + Pleos assets. Verify res/values/config.xml."
