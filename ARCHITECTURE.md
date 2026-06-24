# Scalable UI - Android System Architecture

This document details the internal Android Automotive OS (AAOS) component architecture that powers the "Fluidic Precision" Scalable UI. It focuses on how the SystemUI, CarLauncher, and window management layers were modified to support a dynamic, multi-panel drag-and-drop interface.

## 1. System Architecture Diagram

The Scalable UI modifies the core Android UI rendering pipeline. Instead of a single static application window, the system manages multiple concurrent `TaskView` surfaces controlled by a custom transition coordinator.

```mermaid
graph TD
    subgraph "Android OS Framework"
        WMS[WindowManagerService]
        ATM[ActivityTaskManager]
        SurfaceFlinger[SurfaceFlinger]
    end

    subgraph "CarSystemUI / CarLauncher (System App)"
        RootLayout[Multi-Panel Root View]
        TPTC[TaskPanelTransitionCoordinator]
        
        subgraph "TaskViews (Embedded Activities)"
            MainPanel[Main TaskView Panel]
            SidePanel[Side TaskView Panel]
            BottomBar[ControlBarActivity]
        end
        
        RootLayout --> TPTC
        TPTC --> MainPanel
        TPTC --> SidePanel
        TPTC --> WMS
    end

    subgraph "Runtime Resource Overlays (RRO)"
        RRO[MultiPanelLandscapeRRO]
        RRO -.->|Overrides Layouts & Dims| RootLayout
    end

    subgraph "System Privileged Apps (/system_ext)"
        MockWidgets[MockWidgets Package]
        ClimateWidget[Climate Fragment]
        MediaWidget[AudioCardModule]
        
        MockWidgets --> ClimateWidget
        MockWidgets --> MediaWidget
    end

    MainPanel -.->|Hosts| MockWidgets
    ATM -->|embeds| TaskViews
    SurfaceFlinger -->|composites| RootLayout
```

---

## 2. Component Deep Dive

### A. `MultiPanelLandscapeRRO` (Runtime Resource Overlay)
The cornerstone of the visual changes is the `MultiPanelLandscapeRRO`. Instead of forking the entire AOSP CarSystemUI repository, we use this overlay to dynamically inject the "Fluidic Precision" aesthetics.

- **Layout Overrides:** Overrides standard AAOS layouts (e.g., `car_launcher_multi_window.xml`) to divide the screen into a Main Panel (e.g., 70% width) and a Sidebar Panel (30% width).
- **Glassmorphism Tokens:** Injects custom dimension arrays, corner radiuses (`@dimen/panel_corner_radius`), and translucent color states (`@color/glass_background`) directly into the SystemUI resource pool.
- **Dynamic State:** The overlay is set as `isStatic="false"` in its `AndroidManifest.xml`, allowing it to be toggled on/off via the `deploy_ui.sh` script without requiring a device wipe or affecting standard build targets.

### B. `TaskPanelTransitionCoordinator`
This is the custom orchestration engine responsible for the dynamic drag-and-drop functionality between the panels.

- **Touch Interception:** It acts as a gesture listener on the boundaries of the `TaskView` panels. When a user holds and drags the top bar of a widget, the coordinator calculates the touch delta.
- **Geometry Animation:** Upon release, it uses `ValueAnimator` to visually morph the bounds of the source `TaskView` into the destination bounds.
- **Activity Reparenting:** Once the animation concludes, it communicates with the `ActivityTaskManager` (via WindowManager API) to officially reparent the activity's `WindowToken` from the Sidebar's `TaskView` into the Main Panel's `TaskView`.

### C. `ControlBarActivity` & `AudioCardModule`
The persistent bottom and side controls were modified to integrate directly with native AOSP media and radio.
- **`AudioCardModule`:** Restored to natively bind to the active `MediaBrowserService` (e.g., Spotify or the native Radio app), rendering track info and playback controls persistently across all panel transitions.
- **`ControlBarActivity`:** The HVAC and System Nav bar. It intercepts climate events and broadcasts them to the VHAL.

### D. `MockWidgets` (Privileged Modules)
A suite of modular app fragments that populate the `TaskView` panels.
- **Location:** Installed to `/system_ext/priv-app/MockWidgets`.
- **Privileged Access:** Contains widgets for Smart Home, Agenda, and Climate. 
- **Security:** Because it requires `android.car.permission.CONTROL_CAR_CLIMATE`, it is accompanied by a strictly generated `privapp-permissions-mockwidgets.xml` allowlist file in `/system_ext/etc/permissions/`, allowing the `SystemServer` to grant it deep HAL access without crashing the OS.

---

## 3. Modification Summary Checklist

| Component | Path / Location | Modification | Purpose |
| :--- | :--- | :--- | :--- |
| **SystemUI Build Config** | `packages/apps/Car/SystemUI/samples/systemui_sample_rros.mk` | Removed `DewdDynamicAospRRO` | Prevented legacy overlay conflicts that broke panel geometry. |
| **Overlay Manifest** | `vendor/aospstack/ScalableUI/MultiPanelLandscapeRRO/AndroidManifest.xml` | Set `isStatic="false"` | Allowed dynamic toggling of the new UI without forcing it on all builds. |
| **Widget Permissions** | `vendor/aospstack/ScalableUI/MockWidgets/Android.bp` | Added `required: ["privapp-permissions-mockwidgets.xml"]` | Fixed a fatal `SystemServer` boot-loop by properly linking the privileged allowlist. |
| **Deployment Script** | `/home/hemang/deploy_ui.sh` | Added `cmd overlay enable` commands | Automates the activation of the `MultiPanelLandscapeRRO` at runtime. |
| **System Ext Permissions** | `/system_ext/etc/permissions/privapp-permissions-mockwidgets.xml` | Pushed via root ADB | Granted `CONTROL_CAR_CLIMATE` to the widget suite. |
