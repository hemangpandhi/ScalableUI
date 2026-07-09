# Scalable UI Configuration Documentation

This document explains the architectural configuration for the System Window Orchestration framework in the Scalable UI implementation. It details how application panels, state variants, transitions, and mock widgets are structured and configured within the `MultiPanelLandscapeRRO` and application manifests.

## 1. Panel Configuration (`TaskPanel`)
Each scalable panel (e.g., Radio, Dialer, Map) is defined using a `<TaskPanel>` XML configuration file located in the `res/xml` directory of the `MultiPanelLandscapeRRO`.

### App-Side Configuration & Role Mapping
The connection between the UI panel and the actual Android Application (e.g., Map, Dialer) is established via the `role` attribute.
*   **Role Mapping:** `role="@string/map_componentName"` or `role="@array/phone_components"`
*   The orchestrator uses this `role` to look up the exact `ComponentName` (package name and class name) of the application to launch inside this panel's `TaskView` or `ActivityView`.
*   **App Side:** The corresponding app (e.g., `MockMap`) must be declared in its `AndroidManifest.xml` as a standard, resizable Activity (`android:resizeableActivity="true"`) so it can dynamically scale and fit the bounds defined by the panel.

## 2. Layout Values & States (`Variant`)
A `Variant` represents a specific visual state or layout constraint for a panel. By defining multiple variants, the orchestrator knows exactly how the panel should look in different modes (e.g., fullscreen, stacked, or hidden).

### Common Attributes in a Variant:
*   **`id`**: The unique identifier for this state (e.g., `@+id/opened`, `@+id/stacked`, `@+id/page2_opened`).
*   **`Layer`**: Z-index ordering. A higher number brings the panel forward. (e.g., Maps is `layer="4"`, Phone is `layer="6"`).
*   **`Visibility`**: `isVisible="true"` or `false`.
*   **`Alpha`**: Opacity value (0.0 to 1.0) allowing for fade effects.
*   **`Corner`**: Glassmorphism rounded corner radius (e.g., `radius="48dp"`).
*   **`Bounds`**: Absolute layout positioning (`left`, `top`, `right`, `bottom`, `width`). This dictates the exact sizing.

### Example Values (Map Panel):
*   **`@id/opened`**: Normal default view (`left="835dp"`, `right="1424dp"`, `bottom="696dp"`).
*   **`@id/expanded`**: Fullscreen view covering most of the display (`left="16dp"`, `right="1424dp"`).
*   **`@id/stacked`**: Compressed view when another app is opened (`left="425dp"`, `right="1015dp"`).
*   **`@id/page_2_shown`**: Pushed completely off-screen (`left="-589dp"`, `right="0dp"`, `alpha="0.0"`).

## 3. Events and Transitions (`Transition`)
Transitions dictate how a panel moves from one `Variant` to another when specific system events occur.

### Transition Configuration:
*   **`onEvent`**: The system trigger. Examples include:
    *   `_System_TaskOpenEvent`: Triggered when an app is launched.
    *   `_System_OnHomeEvent`: Triggered when returning to the home dashboard.
    *   `show_page2`: A custom event triggered when the user swipes to the second page of mock widgets.
*   **`onEventTokens`**: A filter condition. For example, `onEventTokens="panelId=map_panel"` ensures this transition only runs if the Map app was the one opened.
*   **`fromVariant` / `toVariant`**: The starting and ending states. If `fromVariant` is omitted, the panel will transition to `toVariant` from *any* current state.
*   **`duration` & `interpolator`**: Controls the animation speed (e.g., `400ms`) and curve (e.g., `@android:anim/accelerate_decelerate_interpolator` for smooth easing or `overshoot_interpolator` for a bounce effect).

## 4. Page 2 Mock Widgets
Page 2 consists of static/mock widget XML layouts (e.g., `climate_widget.xml`, `smart_home.xml`) that slide into view when the `show_page2` event is fired.

### Widget Construction:
*   These widgets are pure Android layouts (`FrameLayout`, `LinearLayout`) utilizing the system's underlying glassmorphism drawables (`@drawable/glass_panel`, `@drawable/glass_button_ripple`).
*   Instead of hosting full `Activity` instances like the main panels, these are lightweight views.
*   **Layout Values:** They rely on standard `match_parent` / `wrap_content` mixed with specific hardcoded metrics (e.g., a 72sp `sans-serif-thin` font for the temperature, and 140dp x 64dp buttons for A/C toggles).
*   **Orchestration:** When the user swipes to Page 2, all main app panels (Media, Map, Phone) receive the `show_page2` event and transition to their offscreen variants (e.g., `@id/page2_is_visible` with `left="-637dp"`, `alpha="0.0"`), while the mock widget grid slides into the main focal area.
