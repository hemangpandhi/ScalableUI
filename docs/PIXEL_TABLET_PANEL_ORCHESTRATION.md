# Pixel Tablet: Scalable UI Panel Orchestration Guide

This document explains the architecture and configuration pipeline that enables the Scalable UI panels on your Pixel Tablet. It outlines how the system connects XML layout files, Activities, and backend Controllers using the `OemDemoRRO` (Runtime Resource Overlay).

## Overview

The Scalable UI uses a "Zero-Compile" dynamic binding architecture. Instead of hardcoding UI elements directly into `CarSystemUI` Java code, panels are defined via XML and injected dynamically. 

The central nervous system for this operation is the **`OemDemoRRO`** package, specifically its `res/values/config.xml` file.

---

## 1. Enabling the Architecture

To activate the Scalable UI engine and bypass the legacy Android Automotive `CarSystemUI` layout logic, the following boolean flag is enforced in the `OemDemoRRO` configuration:

```xml
<bool name="config_enableScalableUI" translatable="false">true</bool>
```

### Suppressing Legacy System Bars
Because the Scalable UI relies on custom floating "pills" (navigation, HVAC, media), the default Android navigation bars must be suppressed to prevent visual overlap and artifacts (like the "Home back button" artifact we recently fixed).

```xml
<bool name="config_enableBottomSystemBar">false</bool>
<!-- Note: Top system bar is kept enabled in the current configuration -->
<bool name="config_enableTopSystemBar">true</bool>
```

---

## 2. Defining Panel Geometry (`window_states`)

Every panel on the screen (whether it's the Map background, the App Grid, or a floating pill) is defined in the `window_states` array.

```xml
<array name="window_states" replace="true">
    <item>@xml/map_panel</item>
    <item>@xml/panel_app_grid</item>
    <item>@xml/floating_nav_panel</item>
    <item>@xml/floating_media_panel</item>
    <item>@xml/hvac_left_panel</item>
    ...
</array>
```

**What happens here?**
Each `@xml/` reference points to a configuration file that dictates the panel's Z-order, dimensions (bounds), and the SystemUI `ControllerName` responsible for managing it (if applicable).

---

## 3. Binding Content to Panels (`config_default_activities`)

Once a panel's geometry is defined, the system needs to know *what* to display inside it. This is handled by the `config_default_activities` array, which maps a Panel ID to either an **Activity** (for complex apps) or a **Layout View** (for static/simple UI elements).

```xml
<string-array name="config_default_activities" translatable="false" replace="true">
    
    <!-- Binding an Activity (e.g., The Map) -->
    <item>map_panel;com.android.car.mapsplaceholder/.MapsPlaceholderActivity</item>
    
    <!-- Binding an Activity (e.g., The App Grid) -->
    <item>panel_app_grid;com.android.car.carlauncher/.AppGridActivity</item>
    
    <!-- Binding a Layout View (e.g., The Floating Navigation Pill) -->
    <item>floating_nav_panel;@layout/floating_nav_view</item>
    
    <!-- Binding a Layout View (e.g., The Media Control Pill) -->
    <item>floating_media_panel;@layout/floating_media_view</item>

</string-array>
```

---

## 4. How the "Floating Pills" Work (Controllers)

For panels bound to a `@layout/...` instead of an Activity (like the navigation pill), `CarSystemUI` uses a **ViewController** to breathe life into the static XML.

For example, when `CarSystemUI` reads the `floating_nav_panel` configuration:
1. It looks at `@xml/floating_nav_panel` and sees that it requires the `FloatingNavViewController`.
2. It inflates the view defined in `@layout/floating_nav_view` (which we recently cleaned up to remove the Home button).
3. It passes that View to the `FloatingNavViewController`.
4. The Controller attaches the necessary click listeners (e.g., clicking the App Grid icon triggers an Intent to open `AppGridActivity`).

## Summary of the OEM Demo Payload

When you push this configuration to your Pixel Tablet using `./scripts/deploy_ui.sh --mode oem`, the following sequence occurs:

1. **`CarSystemUI`** boots up and sees `config_enableScalableUI = true`.
2. It parses the **`window_states`** array from the **`OemDemoRRO`**.
3. It draws the **Map Panel** (`MapsPlaceholderActivity`) as the base layer.
4. It injects the **Floating Navigation Pill** (`floating_nav_view`) and the **Media Pill** (`floating_media_view`) over the map.
5. It hides the default bottom Android navigation bar.
6. When a user clicks the apps button on the floating pill, it opens the **App Grid** (`AppGridActivity`) which we have configured (via `CarLauncherMultiPanelRRO`) to be translucent so the map remains visible underneath.

## 5. Deployment Verification

After running `./scripts/deploy_ui.sh --pixel` on your deployment machine, you can verify that the correct overlays were enabled on the Pixel Tablet by running the following command via ADB:

```bash
adb shell cmd overlay list --user current | grep -i "scalable"
```

### Expected Output (OEM Mode)

Your output must look exactly like this:

```text
[x] com.android.car.carlauncher.rro.scalableUI.multiPanelLandscape
[ ] com.android.systemui.rro.scalableUI.carSystemUI
[ ] com.android.systemui.rro.scalableUI.sysuiBars
[ ] com.android.systemui.rro.scalableUI.multiPanelLandscape
[x] com.android.systemui.rro.scalableUI.oemDemo
```

### What this means:
- **`[x] ...oemDemo`**: The master configuration overlay that injects all of our floating panels (HVAC, Media, Navigation) over the map is **enabled**.
- **`[x] ...multiPanelLandscape`** (CarLauncher RRO): The launcher overlay that fixes the transparent App Grid issue (allowing the map to stay visible in the background) is **enabled**.
- All other legacy overlays (like `sysuiBars` or `carSystemUI`) are safely disabled (`[ ]`), preventing any configuration conflicts.
