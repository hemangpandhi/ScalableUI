# Scalable UI Demo — APK Deployment Guide

## What's in `prebuilt_apks/`

| APK | Target Path on Device | Purpose |
|---|---|---|
| `MultiPanelLandscapeRRO.apk` | `/product/overlay/` | **Main Scalable UI config** — defines all panel layouts, floating nav split, HVAC panels, animation durations |
| `CarSystemUIScalableUIOverlay.apk` | `/product/overlay/CarSystemUIScalableUIOverlay/` | SystemUI overlay — dimension overrides, enabling Scalable UI feature flags |
| `CarLauncherMultiPanelRRO.apk` | `/product/overlay/` | CarLauncher overlay — AppGrid full-screen and multi-panel layout |
| `CarSystemUI.apk` | `/system/priv-app/CarSystemUI/` | CarSystemUI with FloatingNavViewController and panel orchestration logic |
| `CarLauncher.apk` | `/system/priv-app/CarLauncher/` | CarLauncher with AppGrid and ControlBarActivity |
| `MockWidgets.apk` | `/system/app/MockWidgets/` | Clock, Climate, Agenda, SmartHome, Camera widget activities |
| `MockMap.apk` | `/system/app/MockMap/` | Maps placeholder activity |

---

## Procedure: Fresh Push to Device

### Prerequisites
- Device connected via ADB (emulator or hardware)
- `adb root` and `adb remount` must succeed

### Step 1 — Enable root and remount
```bash
adb root
adb remount
```

### Step 2 — Push all APKs
```bash
PREBUILTS=/path/to/ScalableUI/prebuilt_apks

# Main overlay (most important)
adb push $PREBUILTS/MultiPanelLandscapeRRO.apk /product/overlay/MultiPanelLandscapeRRO.apk

# SystemUI overlay
adb shell mkdir -p /product/overlay/CarSystemUIScalableUIOverlay
adb push $PREBUILTS/CarSystemUIScalableUIOverlay.apk /product/overlay/CarSystemUIScalableUIOverlay/CarSystemUIScalableUIOverlay.apk

# CarLauncher RRO
adb push $PREBUILTS/CarLauncherMultiPanelRRO.apk /product/overlay/CarLauncherMultiPanelRRO.apk

# System apps (priv-app)
adb shell mkdir -p /system/priv-app/CarSystemUI
adb push $PREBUILTS/CarSystemUI.apk /system/priv-app/CarSystemUI/CarSystemUI.apk

adb shell mkdir -p /system/priv-app/CarLauncher
adb push $PREBUILTS/CarLauncher.apk /system/priv-app/CarLauncher/CarLauncher.apk

# System apps
adb shell mkdir -p /system/app/MockWidgets
adb push $PREBUILTS/MockWidgets.apk /system/app/MockWidgets/MockWidgets.apk

adb shell mkdir -p /system/app/MockMap
adb push $PREBUILTS/MockMap.apk /system/app/MockMap/MockMap.apk
```

### Step 3 — Enable overlays (for dynamic/non-static RROs)
```bash
adb shell cmd overlay enable com.android.systemui.rro.scalableUI.multiPanelLandscape
adb shell cmd overlay enable com.android.car.launcher.multipanel
adb shell cmd overlay enable com.android.systemui.rro.scalableUI
```

### Step 4 — Restart the system
```bash
adb shell stop && adb shell start
```
Wait ~30 seconds for the device to fully reboot.

---

## Procedure: Quick Overlay-Only Update

When only `MultiPanelLandscapeRRO` has been changed (layout tweaks, animation tuning, panel config):

```bash
adb root && adb remount
adb push prebuilt_apks/MultiPanelLandscapeRRO.apk /product/overlay/MultiPanelLandscapeRRO.apk
adb shell stop && adb shell start
```

---

## Procedure: Enable Scalable UI via AOSP Build System

To bake the overlays into the image permanently (no manual push needed on each boot):

1. Add to your `device.mk` or `product.mk`:
```makefile
PRODUCT_PACKAGES += \
    MultiPanelLandscapeRRO \
    CarSystemUIScalableUIOverlay \
    CarLauncherMultiPanelRRO \
    MockWidgets \
    MockMap
```

2. Enable the overlay at boot by adding to `config.xml` (in CarSystemUI resources):
```xml
<bool name="config_enableScalableUI">true</bool>
```
This is already set to `true` in `MultiPanelLandscapeRRO`.

3. Rebuild and flash:
```bash
m MultiPanelLandscapeRRO CarSystemUIScalableUIOverlay CarLauncherMultiPanelRRO MockWidgets MockMap
# Then re-flash the product/system image
```

---

## Verification Checklist

After pushing APKs and restarting, confirm the following:

- [ ] **Home Screen** — CarLauncher home scene is visible at startup
- [ ] **Three floating nav pills** at the bottom:
  - Left pill: Driver temperature +/- and seat heater
  - Center pill: Home, Apps, Weather, Media controls (song title + play/pause)
  - Right pill: Passenger temperature +/- and seat heater
- [ ] **App Grid** — Pressing the Apps button shows the app grid full-screen
- [ ] **Map** — Map panel visible (top-right or full-screen depending on state)
- [ ] **Dialer** — Phone panel opens without distorted toolbar
- [ ] **Media** — Media source panel opens snappily (≤400ms transition)
- [ ] **Home button** — Returns to the home launcher scene

---

## GitHub Repo

All source and prebuilt APKs are tracked at:
**https://github.com/hemangpandhi/ScalableUI** (branch: `integration`)
