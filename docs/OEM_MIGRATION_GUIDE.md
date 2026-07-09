# OEM Migration Guide: Deploying the Scalable UI Framework

While app developers only need to worry about responsive XML layouts, **Original Equipment Manufacturers (OEMs)** are responsible for integrating the Scalable UI Framework at the AOSP (Android Open Source Project) platform level.

This guide details the architectural, hardware, and policy changes an OEM must make to successfully migrate a standard Android Automotive OS (AAOS) vehicle to the Scalable UI Framework.

---

## 1. Window Manager & Display Policies

By default, AAOS is designed to run one application in full screen. To allow the `SystemWindowOrchestrator` to trap apps inside floating `TaskView` panels, the WindowManager must be unlocked.

### Enable Freeform Windows
The OEM must explicitly enable freeform multi-window support in the framework overlay:
**Path:** `frameworks/base/core/res/res/values/config.xml`
```xml
<!-- Must be true to allow the Orchestrator to manipulate window bounds -->
<bool name="config_freeformWindowManagement">true</bool>
<bool name="config_supportsMultiWindow">true</bool>
```

### CarLauncher DisplayArea Policies
The `CarLauncher` app acts as the host for the Scalable UI. The OEM must ensure that `CarLauncher` is granted the `MANAGE_ACTIVITY_TASKS` permission, allowing it to hijack an app's `SurfaceControl` and pipe it into a `TaskView`.

---

## 2. Hardware & Memory Constraints (The 4x Multiplier)

Migrating to Scalable UI fundamentally changes the resource profile of the infotainment system.

*   **The Classic AAOS Model:** 1 App is `RESUMED` (using GPU/RAM). 10 Apps are `STOPPED` in the background (using minimal RAM, 0 GPU).
*   **The Scalable UI Model:** 4 Apps (e.g., Map, Media, Climate, Smart Home) are simultaneously `RESUMED` and rendering active frames on the screen.

### OEM Requirements:
1.  **RAM:** The system requires significantly more RAM to hold 4 concurrent active applications without triggering the Android Low Memory Killer Daemon (LMKD).
2.  **GPU Surface Limits:** The hardware composer (HWC) must support compositing multiple active `SurfaceControl` layers. Animating 4 panels at 60fps requires substantial GPU fill-rate.
3.  **Thermal Throttling:** Running multiple active apps (especially Maps) concurrently generates more heat. The OEM must tune the VHAL thermal profiles to prevent aggressive CPU throttling during panel transitions.

---

## 3. App Allow-listing and Exception Management

Not all applications are designed to run in a squashed 400dp box. Some apps *must* bypass the Scalable UI and launch natively in full screen.

### The Exclusion List
The OEM must build a configuration list inside `CarSystemUI` or `CarLauncher` for apps that bypass the Orchestrator:
*   **DRM Media Apps:** Netflix, YouTube, or Hulu often refuse to render in freeform multi-window mode due to Widevine DRM constraints.
*   **Camera / Surround View:** The 360-degree parking camera must instantly take over the entire physical screen for safety reasons. It cannot be trapped in a TaskPanel.
*   **Implementation:** When an Intent is fired, the Orchestrator must intercept it, check the Exclusion List, and if matched, route the Intent back to the default Android WindowManager.

---

## 4. Input Routing & Rotary Controllers

If the vehicle uses a physical rotary dial or D-Pad instead of a touchscreen, the OEM must completely rewrite the `CarInputService` focus routing.

*   **The Problem:** In a classic UI, pressing "Right" on the dial simply moves to the next button. In Scalable UI, pressing "Right" might mean jumping from the `Media_Panel` Activity completely across the IPC boundary into the `Climate_Panel` Activity.
*   **The Fix:** The OEM must configure `CarInputService` to track which `TaskView` currently has global window focus, and inject hardware key events specifically into that bounded surface, while allowing edge-escapes to jump between panels.

---

## 5. System-Level Dialogs and Notifications

Because standard `AlertDialog`s triggered by apps are trapped inside their small `TaskView` boundaries, the OEM must standardize how critical vehicle alerts are displayed.

*   **Heads-Up Notifications (HUN):** For standard alerts (e.g., "New Text Message"), apps should fire standard AAOS Notifications. The OEM's SystemUI will render the HUN *above* the Scalable UI Orchestrator.
*   **Critical Vehicle Errors:** For severe alerts (e.g., "Brake Failure", "Engine Overheating"), the VHAL must trigger a system app to draw a window using `WindowManager.LayoutParams.TYPE_SYSTEM_ALERT` or `TYPE_APPLICATION_OVERLAY`. This guarantees the warning completely covers all Scalable panels and dims the physical screen.

---

## 6. AOSP Build Integration (Pre-baking the RRO)

For a production vehicle, the Scalable UI cannot be pushed via `adb`. It must be compiled directly into the system image.

### Updating `device/oem/car.mk`
The OEM must include the `MultiPanelLandscapeRRO` and the `MockWidgets` packages in their product makefiles.

```makefile
PRODUCT_PACKAGES += \
    CarSystemUI \
    CarLauncher \
    MockWidgets \
    MultiPanelLandscapeRRO
```

### Static vs. Dynamic Overlays
*   **Static (Always On):** If the vehicle *only* features the Scalable UI, the OEM sets `android:isStatic="true"` in the RRO Manifest. It cannot be disabled.
*   **Dynamic (User Togglable):** If the OEM wants a "Classic Mode" vs "Dashboard Mode" toggle in Quick Settings, they set `android:isStatic="false"` and assign the overlay a priority. The OEM then implements a toggle that calls the `OverlayManager` system API to enable/disable it on the fly.

---

## 7. System Bars (Insets) and Z-Ordering

The Scalable UI sits in the middle of the screen, sandwiched between the top Status Bar and the bottom Navigation Bar.

*   **Strict Heights:** The OEM must define absolute heights for the `car_top_system_bar` and `car_bottom_system_bar`.
*   **Insets:** The Orchestrator relies on `WindowInsets` to calculate its maximum drawing bounds. If the OEM uses transparent or floating system bars, the Orchestrator might accidentally slide a panel *behind* the climate bar, making buttons unclickable. The OEM must ensure the Z-order (`layer="..."`) of the System Bars in `CarSystemUI` is strictly higher than the Orchestrator's `DecorPanel` layer.

---

## 8. CarLauncher Modifications & Multi-Display Architecture

A critical question OEMs face is: *"Do we need to build multiple CarLaunchers if our vehicle has a Center Console, a Passenger Display, and a Driver Cluster?"*

**The Answer:** No, you do not build multiple `CarLauncher` APKs. However, the OEM must drastically modify how the single `CarLauncher` application handles routing, intents, and displays.

### Display Area Policies & Multi-Display
The Android WindowManager treats each physical screen in the vehicle as a unique `displayId` (e.g., `displayId=0` for Center Console, `displayId=1` for Passenger, `displayId=2` for Cluster).
*   **Targeted Orchestration:** The Scalable UI framework is usually mapped specifically to the Center Console (`displayId=0`). The OEM must configure the Orchestrator XML to explicitly bind `TaskPanels` to this display. 
*   **Cluster Independence:** The Driver Cluster is mission-critical and legally regulated. The OEM must ensure `CarLauncher` never accidentally attempts to apply Scalable UI freeform logic to the Cluster display. The Cluster must remain locked in a dedicated, un-resizeable `ActivityView`.

### App Drawer (App Grid) Intent Routing
In a classic AAOS setup, when a user opens the App Drawer and taps an icon, `CarLauncher` simply fires `startActivity(intent)`. The WindowManager then launches that app in full-screen over the Launcher.
In the Scalable UI model, this default behavior will break the dashboard. If the user taps the "Settings" app, it shouldn't take over the screen; it should open *inside* an empty or newly created `TaskView` panel.

**OEM Responsibility:**
The OEM must rewrite the click-handlers in the `CarLauncher` App Drawer. Instead of firing a raw `startActivity()`, the Launcher must broadcast an Orchestrator Event (e.g., `_System_LaunchApp_Event`). The Orchestrator will intercept this event, determine if there is an available `TaskView` (or transition a panel to make room), and *then* inject the requested app's `SurfaceControl` into that panel.

### Recents (Task Manager) and App Ghosting
The AAOS "Recent Apps" screen (Task Manager) relies on taking snapshots of recently used Activities. Because Scalable UI apps are trapped in smaller `TaskViews`, the default Android Recents screen might capture weird, squashed snapshots of the apps. The OEM must implement a custom Recents provider that takes snapshots of the entire Orchestrator Layout, rather than querying Android for individual Activity snapshots.

---

## 9. Handling Multi-Display with Scalable UI

If your vehicle has multiple interactive screens (e.g., a Center Console and a Passenger Display), the Scalable UI framework can orchestrate both screens simultaneously. However, they must be explicitly partitioned in the `SystemWindowOrchestrator` XML configuration.

### The `displayId` Binding
Every `<TaskPanel>` and `<DecorPanel>` in the Scalable UI layout must be bound to a specific physical screen using the `displayId` attribute. 

*   `displayId="0"`: Usually the primary Center Console.
*   `displayId="1"`: Usually the Passenger Display.
*   `displayId="2"`: Usually the Driver Cluster.

### XML Configuration Example
If the OEM wants the passenger to have their own separate drag-and-drop media widgets that don't interfere with the driver's Center Console, they define it like this in the `scalable_ui_layout.xml`:

```xml
<SystemWindowOrchestrator>
    <!-- ============================================== -->
    <!-- DISPLAY 0: CENTER CONSOLE                      -->
    <!-- ============================================== -->
    <TaskPanel 
        id="driver_media_panel" 
        displayId="0" 
        role="com.android.car.media/.MediaActivity" 
        defaultVariant="@id/opened"/>
        
    <TaskPanel 
        id="driver_climate_panel" 
        displayId="0" 
        role="com.android.car.climate/.ClimateActivity" 
        defaultVariant="@id/stacked"/>

    <!-- ============================================== -->
    <!-- DISPLAY 1: PASSENGER SCREEN                    -->
    <!-- ============================================== -->
    <TaskPanel 
        id="passenger_media_panel" 
        displayId="1" 
        role="com.android.car.media/.PassengerMediaActivity" 
        defaultVariant="@id/passenger_fullscreen"/>

</SystemWindowOrchestrator>
```

### 🚨 Multi-Display Warning: Process Isolation
Android does not easily allow a single Activity instance to exist on two physical displays simultaneously. 
*   **The Bug:** If you try to launch `com.android.car.media/.MediaActivity` on `displayId="0"` (Center Console) and then launch that exact same Activity on `displayId="1"` (Passenger), the Android WindowManager will forcefully rip the Activity off the Center Console and move it to the Passenger display. The driver's media panel will instantly turn into a black box.
*   **The OEM Fix:** To have Scalable UI panels on both screens, the OEM must ensure that the apps running on the passenger screen have different `ComponentNames` (e.g., `PassengerMediaActivity`) or they must explicitly flag the Activity with `android:resizeableActivity="true"` and `android:launchMode="standard"` while handling intense multi-window instance management in the App logic.
