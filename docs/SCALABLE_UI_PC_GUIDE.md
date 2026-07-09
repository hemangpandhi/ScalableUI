# Scalable UI PC Deployment Guide

This guide explains how to extract the provided deployment bundle and run the "Fluidic Precision" Scalable UI emulator (Cuttlefish) on your local PC using Docker.

## System Requirements
1. **Operating System:** Ubuntu/Linux (Recommended) or Windows 11 with WSL2 enabled.
2. **Hardware Virtualization:** You MUST have Hardware Virtualization (VT-x/AMD-V) enabled in your BIOS.
   - For Linux: Ensure `/dev/kvm` exists.
   - For Windows (WSL2): Ensure Nested Virtualization is enabled for your WSL instance.
3. **Hardware Specs:** Minimum 8GB RAM (16GB Recommended), 4 CPU cores, and at least 30GB of free disk space.
4. **Software:** Docker installed.

---

## Step-1: Extract the Bundle
Unpack the downloaded `ScalableUI_Portable_Demo.tar.gz` bundle:
```bash
tar -xzf ScalableUI_Portable_Demo.tar.gz
cd scalable_ui_bundle
```

## Step-2: Build the Docker Image
Instead of downloading a massive 20GB+ pre-built Docker image, we provide the raw Android `.img` files and a `Dockerfile`. This allows you to construct the container environment efficiently on your local machine.

Run the following command in the terminal:
```bash
docker build -t scalable_ui_demo .
```
*(This process will take a few minutes as it downloads Ubuntu and installs the necessary KVM and Cuttlefish libraries).*

## Step-3: Run the Emulator
Because Cuttlefish runs an entire Android OS inside a virtual machine, the Docker container requires elevated privileges to access your PC's hardware acceleration (`/dev/kvm`).

Execute the container:
```bash
docker run -it --rm \
    --privileged \
    -v /dev/kvm:/dev/kvm \
    -v $(pwd)/images:/home/cvd/images \
    -p 8444:8443 \
    -p 6521:6520 \
    scalable_ui_demo
```

## Step-4: Access the Scalable UI
1. The terminal will output `Launching Cuttlefish WebRTC emulator...` followed by various boot logs.
2. The background patcher will automatically fix permissions and enable the Scalable UI overlays. Wait for it to print `Patcher finished!`.
3. Open your **Host PC's web browser** (Chrome/Edge recommended) and navigate to:
   **`https://localhost:8444`**
4. *Note: You may receive a "Your connection is not private" warning because the WebRTC server uses a self-signed SSL certificate. You can safely click "Advanced" -> "Proceed to localhost".*

You will now see the Android Automotive UI running right in your browser! The Scalable UI glassmorphism layout, drag-and-drop widgets, and climate controls will be fully functional.

---

## Debugging Commands & Troubleshooting

#### 1. Checking Emulator Logs
To see the container's boot logs and background patcher output:
```bash
docker logs $(docker ps -q -f ancestor=scalable_ui_demo)
```

#### 2. Executing ADB Commands Dynamically
You can run `adb` commands directly against the container without needing to look up the Container ID by using the `docker exec` shortcut:
```bash
# Example: Check connected devices
docker exec $(docker ps -q -f ancestor=scalable_ui_demo) /home/cvd/bin/adb devices

# Example: Read logcat
docker exec $(docker ps -q -f ancestor=scalable_ui_demo) /home/cvd/bin/adb -s 0.0.0.0:6520 logcat -d
```

#### 2.1 Fixing "get-state error: no devices/emulators found"
If you try to run an `adb` command and get a `no devices/emulators found` error (especially during a bootloop or early startup), the internal ADB daemon may have disconnected. Reconnect it manually to the internal virtual port before running commands:
```bash
# 1. Connect explicitly to the internal serial port
docker exec $(docker ps -q -f ancestor=scalable_ui_demo) /home/cvd/bin/adb connect 0.0.0.0:6520

# 2. Re-run your commands (reconnecting again after root is necessary)
docker exec $(docker ps -q -f ancestor=scalable_ui_demo) /home/cvd/bin/adb root
docker exec $(docker ps -q -f ancestor=scalable_ui_demo) /home/cvd/bin/adb connect 0.0.0.0:6520
docker exec $(docker ps -q -f ancestor=scalable_ui_demo) /home/cvd/bin/adb remount
```

#### 3. Connecting via ADB on Host
```bash
# Connect to the emulator ADB port
adb connect localhost:6521

# View the Android logs
adb -s localhost:6521 logcat -d | tail -n 100
```

### 2. View Docker Container Output
If the `start.sh` script failed, you can read the raw Docker logs:
```bash
# Get the container ID
docker ps

# Read the logs
docker logs <CONTAINER_ID>
```

### 3. Manually Push the Fix & Enable Overlays
If the automatic script fails due to a timeout, you can manually trigger the fix from your Host PC while the container is running:
```bash
# 1. Connect and gain root access
adb connect localhost:6521
adb -s localhost:6521 root
sleep 2
adb connect localhost:6521

# 2. Bypass userdata checkpoint (Required for first boot)
adb -s localhost:6521 shell vdc checkpoint commitChanges

# 3. Remount the filesystem as Read/Write
adb -s localhost:6521 remount

# NOTE: If remount says "Now reboot your device", you MUST reboot and run steps 1-3 again!
# adb -s localhost:6521 reboot

# 4. Push the permissions fix
echo '<?xml version="1.0" encoding="utf-8"?><permissions><privapp-permissions package="com.android.car.mockwidgets"><permission name="android.car.permission.CONTROL_CAR_CLIMATE"/></privapp-permissions></permissions>' > /tmp/priv.xml
adb -s localhost:6521 push /tmp/priv.xml /system_ext/etc/permissions/privapp-permissions-mockwidgets.xml

# 5. Enable the Scalable UI Overlays
adb -s localhost:6521 shell cmd overlay enable --user 0 com.android.systemui.rro.scalableUI.multiPanelLandscape
adb -s localhost:6521 shell cmd overlay enable --user 10 com.android.systemui.rro.scalableUI.multiPanelLandscape
adb -s localhost:6521 shell cmd overlay disable --user 0 com.android.systemui.rro.scalableUI.carSystemUI
adb -s localhost:6521 shell cmd overlay disable --user 10 com.android.systemui.rro.scalableUI.carSystemUI

# 6. Restart the System UI to apply changes
adb -s localhost:6521 shell stop
adb -s localhost:6521 shell start
```
