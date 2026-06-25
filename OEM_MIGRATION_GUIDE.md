# OEM Migration Guide: Scalable UI

This guide outlines the steps required for Original Equipment Manufacturers (OEMs) to migrate an existing Android Automotive OS (AAOS) platform to the new **System Window Orchestration / Scalable UI** framework.

## 1. Prerequisites
*   Android 14/15 (AAOS) source tree.
*   Hardware or emulator (e.g., Cuttlefish) capable of rendering multi-display or at least 1080p landscape.
*   Access to the `vendor` partition for Runtime Resource Overlays (RROs).

## 2. Integration Steps

### Step 1: Clone the Repository
Clone the `ScalableUI` repository into your `vendor/<oem_name>/` or `vendor/aospstack/` directory.
```bash
git clone https://github.com/hemangpandhi/ScalableUI vendor/aospstack/ScalableUI
```

### Step 2: Include Packages in the Build
You must include the Scalable UI packages in your device's `device.mk` or `systemui_sample_rros.mk` configuration.
```makefile
# Add the Scalable UI RROs and Mock Widgets
PRODUCT_PACKAGES += \
    CarSystemUIOverlay \
    MultiPanelLandscapeRRO \
    MockWidgets
```

### Step 3: Configure Default System UI Overlays
AAOS requires explicitly enabling overlays to override the default `CarSystemUI`. Modify your `overlay.xml` or system properties to prioritize the ScalableUI RROs.

For runtime enabling (e.g., in a startup script or `init.rc`):
```bash
cmd overlay enable --user 0 com.android.systemui.rro.scalableUI.multiPanelLandscape
cmd overlay disable --user 0 com.android.systemui.rro.scalableUI.carSystemUI
```

## 3. Disabling Default Components
To prevent the default AAOS CarLauncher from colliding with the System Window Orchestration framework, ensure that `CarSystemUI` is configured to use the multi-panel layout instead of the standard task view configuration.
*   Disable standard `com.android.car.carlauncher` features that interfere with the `MultiPanelLandscapeRRO` bounds.

## 4. Hardware Acceleration
For emulated environments (like Cuttlefish), ensure that hardware acceleration is configured properly:
*   Ensure Vulkan and GLES drivers are available.
*   Use `--gpu_mode=auto` or `--gpu_mode=gfxstream` when launching `cvd`.
