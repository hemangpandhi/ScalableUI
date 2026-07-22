#!/bin/bash

# ==============================================================================
# Configuration
# Please update these variables to match your remote Linux laptop environment
# ==============================================================================
REMOTE_USER="hemang"
REMOTE_HOST="192.168.3.36"
REMOTE_AOSP_DIR="/path/to/remote/aosp/workspace"
REMOTE_TARGET="aosp_tangorpro-trunk_staging-userdebug"
# ==============================================================================

echo "[remote_build] Connecting to ${REMOTE_USER}@${REMOTE_HOST}..."

ssh "${REMOTE_USER}@${REMOTE_HOST}" << EOF
    echo "[remote_build] Connected to remote host."
    cd "${REMOTE_AOSP_DIR}" || { echo "Failed to find AOSP dir"; exit 1; }
    
    echo "[remote_build] Initializing environment..."
    source build/envsetup.sh
    lunch ${REMOTE_TARGET}
    
    echo "[remote_build] Building Scalable UI components..."
    m CarSystemUI CarLauncher OemDemoRRO || { echo "Build failed!"; exit 1; }
    
    echo "[remote_build] Build completed successfully on remote server."
EOF

if [ $? -eq 0 ]; then
    echo "[remote_build] Remote execution finished."
    exit 0
else
    echo "[remote_build] Error during remote execution."
    exit 1
fi
