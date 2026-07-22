#!/bin/bash

# ==============================================================================
# Configuration
# Please update these variables to match your remote Linux laptop environment
# ==============================================================================
REMOTE_USER="hemang"
REMOTE_HOST="192.168.3.36"
REMOTE_AOSP_DIR="/path/to/remote/aosp/workspace"
# ==============================================================================

LOCAL_PREBUILTS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../assets/prebuilts" && pwd)"

echo "[sync_prebuilts] Syncing APKs from ${REMOTE_HOST} to local prebuilts directory..."

# The exact paths to the APKs on the remote machine (assuming standard out/ structure)
# Update these paths if the remote target uses a different output structure (e.g. out/target/product/tangorpro/...)
REMOTE_CAR_SYSUI="${REMOTE_AOSP_DIR}/out/target/product/tangorpro/system/priv-app/CarSystemUI/CarSystemUI.apk"
REMOTE_CAR_LAUNCHER="${REMOTE_AOSP_DIR}/out/target/product/tangorpro/system/priv-app/CarLauncher/CarLauncher.apk"
REMOTE_OEM_DEMO="${REMOTE_AOSP_DIR}/out/target/product/tangorpro/product/overlay/OemDemoRRO.apk"

# Ensure local prebuilts directory exists
mkdir -p "${LOCAL_PREBUILTS_DIR}"

echo "[sync_prebuilts] Fetching CarSystemUI.apk..."
scp "${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_CAR_SYSUI}" "${LOCAL_PREBUILTS_DIR}/CarSystemUI.apk" || { echo "Failed to fetch CarSystemUI"; }

echo "[sync_prebuilts] Fetching CarLauncher.apk..."
scp "${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_CAR_LAUNCHER}" "${LOCAL_PREBUILTS_DIR}/CarLauncher.apk" || { echo "Failed to fetch CarLauncher"; }

echo "[sync_prebuilts] Fetching OemDemoRRO.apk..."
scp "${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_OEM_DEMO}" "${LOCAL_PREBUILTS_DIR}/OemDemoRRO.apk" || { echo "Failed to fetch OemDemoRRO"; }

echo "[sync_prebuilts] Sync complete. The prebuilts in ${LOCAL_PREBUILTS_DIR} are now updated for the Pixel target."
