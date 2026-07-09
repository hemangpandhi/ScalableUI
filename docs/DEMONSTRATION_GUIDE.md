# Scalable UI: Internal Demonstration & Architecture Guide

This document provides a comprehensive technical overview and presentation script for demonstrating the Android Automotive Scalable UI architecture. It is designed for internal engineering teams, product managers, and external OEM stakeholders.

---

## 1. Executive Summary

The Scalable UI architecture is a paradigm shift in how Android Automotive OS (AAOS) handles screen real estate. Traditional AAOS implementations hardcode the System UI boundaries (top navigation, bottom climate controls) and confine third-party applications to a single rectangular viewport. 

**Scalable UI** treats the entire screen as a dynamic, grid-based orchestration canvas. Using **TaskViews** (for app hosting) and **Decor Panels** (for floating native UI), the system can freely move, resize, and layer components without requiring modifications to the underlying Android application code.

### Core Value Propositions for OEMs
1.  **Zero Java Modifications:** OEMs can entirely redesign the dashboard layout (e.g., portrait to ultrawide) purely by updating the XML configuration in a Runtime Resource Overlay (RRO).
2.  **Hardware Independence:** Using the `MockWidgets` package, designers can prototype and validate the UI on standard PC emulators (Cuttlefish) before deploying to physical vehicle ECUs.
3.  **App Compatibility:** Standard Android apps (`resizeableActivity="true"`) run natively within Task Panels and automatically adapt to dragged panel sizes.

---

## 2. Architectural Components

### A. Window Orchestrator (`ScalableUIController`)
The central engine running inside the `SystemUIService`. It parses the RRO configuration on boot and translates the requested "Variants" (states) into physical `WindowManager` bounds. 

### B. Task Panels
These are dynamic bounding boxes that host external Android applications.
*   **Implementation:** They use Android's native `TaskView` API.
*   **Layering:** Hosted on standard application layers (e.g., Layer 10).
*   **Example:** `map_panel`, `media_panel`, `climate_panel`.

### C. Decor Panels (Floating Elements)
These are native SystemUI views that float *above* the applications.
*   **Implementation:** Rendered directly by `SystemUI` (no `TaskView` wrapper).
*   **Layering:** Pinned to elevated system layers (e.g., Layer 15) to prevent apps from occluding them.
*   **Example:** `floating_nav_panel` (the transparent navigation pill), Status bar icons, Drag Handles.

### D. Runtime Resource Overlays (RRO)
The RRO (`MultiPanelLandscapeRRO`) is the styling engine. It contains:
1.  **`config.xml`**: Maps Panel IDs to their physical variants/states on the screen.
2.  **`floating_nav_view.xml`**: The physical visual layout of the Decor Panels.
3.  **Drawables/Colors**: The "Fluidic Precision" glassmorphism assets.

---

## 3. The Boot Sequence & Initialization

When demonstrating the system boot, explain the following steps happening in the background:

1.  **Service Start:** Android `SystemServer` starts the `SystemUIService`.
2.  **Controller Injection:** Dagger injects `ScalableUIController`.
3.  **RRO Binding:** The controller queries `PackageManager` for active RROs targeting the `scalableUI` namespace.
4.  **XML Parsing & Inflation:**
    *   *Technical Detail:* The system extracts the `XmlResourceParser` from the RRO but applies the base `SystemUI` theme Context to it. This prevents `InflateException` crashes when resolving theme attributes like button ripples.
5.  **Window Creation:** `WindowManager` assigns Decor Panels to Layer 15 and Task Panels to Layer 10.
6.  **App Hosting:** `TaskPanelTransitionCoordinator` launches the `config_default_activities` (Maps, Media, etc.) into the respective TaskViews.

---

## 4. Demonstration Script & Feature Showcase

When presenting the Scalable UI to a customer or stakeholder, follow this sequence:

### Showcase 1: The "Fluidic" Floating Navigation
**Action:** Point out the floating navigation bar at the bottom or side of the screen.
**Talking Point:** "Notice how the navigation bar is not a rigid black strip at the bottom of the screen. It is a transparent 'Decor Panel' residing on Window Layer 15. Because it is detached from the base layout, it floats dynamically above the map. This was achieved entirely via XML RRO overrides, zero Java changes were required to position this."

### Showcase 2: Dynamic Task Panels (Multi-App)
**Action:** Show the default dashboard state with the Map panel and the Media/Radio panel side-by-side.
**Talking Point:** "We are hosting two completely independent Android applications simultaneously. The system uses `TaskViews` to containerize these apps. The bounds of these containers are strictly defined by our RRO configuration."

### Showcase 3: Drag & Drop Orchestration
**Action:** Tap and drag the handle between the Map and the Media panel.
**Talking Point:** "When I interact with this drag handle, I am actually touching a 'Decor Panel' that acts as an invisible touch-interceptor. It communicates with our `TaskPanelTransitionCoordinator`, which fluidly recalculates the dimensions of the `TaskViews` in real-time. Notice how the underlying apps seamlessly resize without crashing or restarting."

### Showcase 4: Mocking & Hardware Bypass
**Action:** Open the Climate or Driving Stats widget.
**Talking Point:** "In a real vehicle, this climate widget would communicate with the Vehicle HAL (VHAL). However, to accelerate UI development and demonstration, we created the `MockWidgets` package. These are lightweight Android apps that perfectly mimic the visual style but bypass hardware dependencies, allowing us to validate the entire orchestration framework in the cloud or on a standard laptop using Cuttlefish."

---

## 5. Deployment & Technical Handover

For developers taking over the project, ensure they understand the deployment pipeline:

1.  **Build Execution:** Standard AOSP module builds (`m CarSystemUI`).
2.  **The Start Script (`start.sh`):** This script automates launching the Cuttlefish emulator and sideloading the custom SystemUI and RRO APKs.
3.  **Permissions:** Explain that sideloading system apps revokes permissions. The deployment pipeline explicitly executes `pm grant` commands for `BLUETOOTH_CONNECT` to prevent the `LocalBluetoothManager` from crashing the SystemUI on boot.
4.  **Logcat Debugging:** Instruct them to monitor `adb logcat | grep SystemUI` for `InflateException` or Window Manager bound errors when editing XML layouts.

---
*End of Document. Refer to `FAQ.md` for troubleshooting and detailed technical Q&A.*
