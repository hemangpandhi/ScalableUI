# Scalable UI Deployment on Pixel Tablet (tangorpro)

This document outlines the root cause and the fix for the issue where the legacy Android navigation bar (containing All Apps, Recents, and Pinned buttons) was rendering over the custom Scalable UI on the physical Pixel Tablet, despite functioning correctly in the emulator.

## Root Cause Analysis

The persistence of the "old navigation bar" was caused by a combination of two factors specific to the tablet environment:

1. **Missing OemDemoRRO Package**:
   The beautiful custom Scalable UI layout—which includes the floating media bar, floating HVAC panels, and custom navigation layout—was packaged entirely inside the `OemDemoRRO` overlay. This overlay was removed/disabled on the tablet. Without it, the Scalable UI framework naturally fell back to its base state (`CarSysuiScalableBarRRO`), which defaults to inflating `car_sysui_scalable_footer.xml`. This fallback layout inherently contains the legacy AOSP navigation icons.

2. **Android 15 DEWD (Dynamic Early Window Decor) Conflicts**:
   The Pixel Tablet runs Android 15, which introduces DEWD for drawing system bars. The tablet's system image contained immutable DEWD packages (`android.car.config.rro.dewd`, `com.android.systemui.rro.dewd.aosp.dynamic`, etc.). These overlays were overriding our Scalable UI configuration and forcing the standard Android `nav` and `status` windows to draw over our application space.

## The Fix

To resolve the conflict and restore the custom Scalable UI, the following steps were executed:

### 1. Disable the Android 15 DEWD Overlays
The immutable DEWD packages were forcefully disabled using the Android Package Manager to prevent them from injecting standard navigation bars over the custom UI:

```bash
adb shell pm disable-user --user 0 com.android.systemui.rro.dewd.aosp.dynamic
adb shell pm disable-user --user 10 com.android.systemui.rro.dewd.aosp.dynamic
adb shell pm disable-user --user 0 android.car.config.rro.dewd
adb shell pm disable-user --user 10 android.car.config.rro.dewd
adb shell pm disable-user --user 0 com.android.car.resources.dewd
adb shell pm disable-user --user 10 com.android.car.resources.dewd
```

### 2. Recompile the Custom OemDemoRRO
The custom layouts (Face Login, Floating Navigation, Floating HVAC) were recompiled from the AOSP source tree:

```bash
source build/envsetup.sh
lunch aosp_cf_x86_64_auto-trunk_staging-userdebug
m OemDemoRRO
```

### 3. Deploy and Enable the Custom UI
The newly compiled `OemDemoRRO.apk` was pushed to the tablet, installed, and enabled. Finally, the System UI was restarted to apply the changes cleanly:

```bash
# Push and install the overlay
adb push out/soong/.intermediates/vendor/aospstack/ScalableUI/overlays/OemDemoRRO/OemDemoRRO/android_common/signed/OemDemoRRO.apk /data/local/tmp/
adb shell pm install -r -d -g /data/local/tmp/OemDemoRRO.apk

# Enable the overlay for relevant users
adb shell cmd overlay enable --user 0 com.android.systemui.rro.scalableUI.oemDemo
adb shell cmd overlay enable --user 10 com.android.systemui.rro.scalableUI.oemDemo

# Restart System UI
adb shell stop && sleep 2 && adb shell start
```

## Verification
Following the restart, a `dumpsys window windows` verification confirmed that the legacy `nav` window was completely eliminated. The system successfully transitioned to rendering the custom embedded panels (e.g., `Embedded{floating_nav_panel}`, `Embedded{floating_hvac_driver_panel}`).
