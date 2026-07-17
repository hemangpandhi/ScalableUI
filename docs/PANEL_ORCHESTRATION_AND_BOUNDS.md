# Scalable UI Panel Orchestration & Bounds Guide

This document provides a comprehensive mapping of all Scalable UI panels configured in the `MultiPanelLandscapeRRO` overlay. This architecture follows the 3-column "Fluidic Precision" OEM design for a 1920x1080 landscape display.

## 1. Grid Architecture Geometry
The underlying grid defines specific safe zones and boundaries to prevent overlaps and visual collisions:

*   **System Status Bar (Top)**: `0dp` to `84dp`
*   **Floating Navigation Bar (Bottom)**: `896dp` to `1080dp`
*   **Column 1 (Left - Media/Shortcuts)**: `24dp` to `408dp` (Width: 384dp)
*   **Column 2 (Center - Map/Core)**: `432dp` to `1488dp` (Width: 1056dp)
*   **Column 3 (Right - Widgets/Agenda)**: `1512dp` to `1896dp` (Width: 384dp)
*   *Note: There is a 24dp padding/gap between all columns.*

## 2. Configured Panels (`window_states`)

There are currently **23 window states** registered in `config.xml`. These define the bounding boxes and transition events.

### Primary Layout Panels
*   **`home_panel`**: Background root panel spanning `0dp` to `1920dp`. Hosts `CarLauncher`.
*   **`media_panel`**: Occupies Column 1 (`24dp` to `408dp`). Hosts `ControlBarActivity`.
*   **`map_panel`**: Occupies Column 2 (`432dp` to `1488dp`). Hosts `MapsPlaceholderActivity`.
*   **`panel_app_grid`** & **`app_panel`**: Spans Columns 1 & 2 (`24dp` to `1488dp`) when expanded. Hosts `AppGridActivity`.

### Page 2 / Secondary Panels
*   **`agenda_widget`**: Occupies Column 3 (`1512dp` to `1896dp`). Hosts `AgendaActivity`.
*   **`climate_widget`**: Mock widget in Column 2 right-side during Page 2 transitions. Hosts `ClimateActivity`.
*   **`smart_home`**: Expands across Column 2 & 3. Hosts `SmartDeviceActivity`.
*   **`clock`**: Time widget activity.
*   **`car_status`**: Car vehicle properties mock.
*   **`passenger_panel`**: Dedicated UI for passenger interactions.

### Decor & Floating Panels
*   **`floating_nav_panel`**: Bottom dock (`0dp` to `1920dp`, anchored at `896dp` bottom). Contains Home, Dual-Zone HVAC, Fan, and Apps.
*   **`floating_decor_panel`**: Global system-level overlays.
*   **`hvac_panel`** (`hvac_control_panel`): A floating overlay spanning `17dp` to `410dp`, anchored vertically above the navigation bar. Triggered by the Nav Bar Fan button.
*   **`assistant_bubble`**: Floating voice assistant UI.
*   **`camera_panel`**: Reversing camera UI overlay.

### Shadows and Overlays
*   **`phone_shadow`**, **`settings_shadow`**, **`media_source_shadow`**: Visual scrims injected behind sliding panels to darken the background map.

## 3. Transition Events
The UI is state-driven based on explicit Event ID dispatches:
*   `_System_OnHomeEvent`: Closes apps, resets Map to center, collapses floating panels.
*   `show_page2`: Slides the Map off-screen left, expanding the right-side widgets (Agenda, Climate).
*   `show_hvac` / `hide_hvac`: Manages the lifecycle of the floating `hvac_control_panel`.
*   `_System_TaskOpenEvent`: Dynamically opens targets via `panelId=` arguments.

## 4. Verification Checklist
- [x] Dual-zone HVAC buttons correctly map to their controller equivalents.
- [x] Panel `role` identifiers correctly bind to their target activities in `config_default_activities`.
- [x] Zero hard-coded logic in apps; all bounds are strictly defined via the RRO XML transitions.

