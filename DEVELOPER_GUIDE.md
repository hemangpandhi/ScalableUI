# Scalable UI Developer Guide

Welcome to the Scalable UI development guide. This document explains how to add new panels, widgets, and customize the Android Automotive System Window Orchestration framework.

## 1. Adding a New Panel
The System Window Orchestration framework uses `<TaskPanel>` configurations. To add a new panel:

1.  **Create the XML:** In `MultiPanelLandscapeRRO/res/xml/`, create a new XML file (e.g., `my_new_panel.xml`).
2.  **Define the TaskPanel:**
    ```xml
    <TaskPanel id="my_new_panel" defaultVariant="@id/opened" role="@string/my_app_component" displayId="0">
        <Variant id="@+id/opened">
            <Layer layer="5"/>
            <Visibility isVisible="true"/>
            <Alpha alpha="1.0"/>
            <Corner radius="48dp"/>
            <Bounds left="200dp" top="84dp" right="600dp" bottom="696dp"/>
        </Variant>
        <!-- Add offscreen and stacked variants -->
    </TaskPanel>
    ```
3.  **Define Transitions:** Add the `<Transitions>` block to handle `_System_TaskOpenEvent` and `_System_OnHomeEvent`.
4.  **Register the Panel:** Ensure the panel is registered in the main `config.xml` or orchestration config where panels are listed.

## 2. Adding a New Mock Widget (Page 2)
Mock widgets are lightweight static views that live on "Page 2" of the dashboard.

1.  **Create the Layout:** In `MockWidgets/res/layout/`, create a standard Android XML layout (e.g., `my_widget.xml`). Use `@drawable/glass_panel` for the background to maintain visual consistency.
2.  **Create the Activity:** Create a standard Activity in `MockWidgets/src/com/android/car/mockwidgets/` that inflates `my_widget.xml`.
3.  **Register in Manifest:** Add the Activity to `MockWidgets/AndroidManifest.xml` with `android:resizeableActivity="true"`.
4.  **Map to a Panel:** Create a `<TaskPanel>` in the RRO that uses the Activity's `ComponentName` as its `role`.

## 3. Customizing the Glassmorphism Effect
The glass effect is achieved through a combination of corner radii, background alphas, and underlying blurs.
*   **Radii:** Adjust `<Corner radius="48dp"/>` in the `Variant` tags.
*   **Shadows/Blurs:** Shadow and blur controllers (e.g., `app_grid_blur_controller.xml`) handle the dark underlying shapes that give the glass depth. Ensure these are transitioned perfectly in sync with your panel.

## 4. Build and Deploy
To compile your changes and deploy them to the emulator:

```bash
# Set up the build environment
source build/envsetup.sh
lunch aosp_cf_x86_64_auto-userdebug

# Build only the necessary packages
m MockWidgets CarSystemUI CarLauncher CarSystemUIOverlay MultiPanelLandscapeRRO

# Or deploy a specific APK via adb
adb root
adb remount
adb push out/target/product/vsoc_x86_64/system/product/overlay/MultiPanelLandscapeRRO.apk /system/product/overlay/
adb reboot
```
