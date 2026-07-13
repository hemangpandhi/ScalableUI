#!/usr/bin/env bash
# Regenerate overlays/OemDemoRRO from MultiPanelLandscape + CarSysuiScalableBar sources.
# Run after editing either parent overlay.

set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"

rm -rf "${ROOT}/overlays/OemDemoRRO"
cp -r "${ROOT}/overlays/MultiPanelLandscapeRRO" "${ROOT}/overlays/OemDemoRRO"

# Merge Pleos system bar assets
cp "${ROOT}"/overlays/CarSysuiScalableBarRRO/res/layout/car_sysui*.xml \
   "${ROOT}/overlays/OemDemoRRO/res/layout/"
cp "${ROOT}"/overlays/CarSysuiScalableBarRRO/res/xml/sysui*.xml \
   "${ROOT}/overlays/OemDemoRRO/res/xml/"
cp "${ROOT}"/overlays/CarSysuiScalableBarRRO/res/drawable/sysui*.xml \
   "${ROOT}/overlays/OemDemoRRO/res/drawable/"
cp "${ROOT}"/overlays/CarSysuiScalableBarRRO/res/drawable/ic_sysui*.xml \
   "${ROOT}/overlays/OemDemoRRO/res/drawable/"
cp "${ROOT}"/overlays/CarSysuiScalableBarRRO/res/values/colors.xml \
   "${ROOT}/overlays/OemDemoRRO/res/values/sysui_colors.xml"
cp "${ROOT}"/overlays/CarSysuiScalableBarRRO/res/values/dimens.xml \
   "${ROOT}/overlays/OemDemoRRO/res/values/sysui_dimens.xml"
cp "${ROOT}"/overlays/CarSysuiScalableBarRRO/res/values/strings.xml \
   "${ROOT}/overlays/OemDemoRRO/res/values/sysui_strings.xml"

echo "OemDemoRRO scaffold regenerated. Re-apply config.xml / manifest customizations."
