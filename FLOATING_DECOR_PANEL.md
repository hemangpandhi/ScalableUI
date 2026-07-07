# Scalable UI: Floating Decor Panel Architecture & Updates

The **Floating Decor Panel** provides a rich, dynamic, and fluid overlay mechanism within the Android Automotive OS Scalable UI framework. It enables OEM-customizable panels that float above the base UI (e.g., Media Hub and Weather Widgets), controlled entirely through XML variants and dynamically loaded Java controllers.

---

## 1. How the Decor Panel is Created

The creation of the Decor Panel leverages the **System Window Orchestration** architecture and follows a 3-step initialization flow:

### Step 1: Base Layout Placeholder
The base `CarSystemUI` package must contain a placeholder for the view hierarchy (`res/layout/floating_decor_view.xml`). Even if the file is an empty `LinearLayout`, it provides an anchor point so that resource resolution succeeds during layout inflation in the system context.

### Step 2: Overlay Definition (RRO)
The Runtime Resource Overlay (e.g., `MultiPanelLandscapeRRO`) provides the actual UI content.
* **Layout (`floating_decor_view.xml`)**: Overrides the base placeholder with the rich Media Hub and Weather UI, complete with rounded glassmorphism backgrounds.
* **Panel Config (`floating_decor_panel.xml`)**: Defines the geometric `Variant`s (e.g., `@id/hidden` collapsing to a 10x10 point vs `@id/visible` expanding to a 640x440 center window) and the `Transition` triggers (`toggle_floating_decor`).
* **Controller Config (`floating_decor_controller.xml`)**: Maps the panel to the `FloatingDecorViewController` Java class.

### Step 3: Controller Initialization
When the `StateManager` identifies the `floating_decor_panel` in the layout XMLs, the `PanelControllerInitializer` reflects the class defined in the controller config. The `FloatingDecorViewController` then dynamically inflates the view using `mContext.getResources().getIdentifier("floating_decor_view", ...)`, attaches animations (e.g., spinning album art), and injects it into the system window.

---

## 2. Current Architecture

The Scalable UI architecture orchestrates panels strictly through state variants and intent dispatching, separating structural orchestration from visual logic.

```mermaid
graph TD
    A[CarSystemBarButton (Nav Bar)] -->|Broadcast Event| B[EventDispatcher]
    B -->|toggle_floating_decor| C[StateManager]
    
    C -->|Calculates Variant| D[floating_decor_panel.xml]
    D -->|@id/hidden to @id/visible| E[PanelTransitionCoordinator]
    
    E -->|Animates Bounds/Alpha/Focus| F[System Window]
    
    G[PanelControllerInitializer] -->|Instantiates| H[FloatingDecorViewController]
    H -->|Inflates Override Layout| F
```

**Key Architectural Pillars:**
1. **Decoupled Event Flow**: Buttons don't interact with panels directly. They send string-based events to the `EventDispatcher`, which `StateManager` processes to find matching `Transition` elements in the XML configs.
2. **Controller Injection**: The Navigation Buttons must be injected with `CarSystemBarButtonController`. Without this, the button lacks access to the `EventDispatcher`, breaking the event chain.
3. **Variant-Driven Geometry**: Panels define their own bounds, alpha, and layer rules in `Variant` blocks, keeping all dimension and positional logic out of Java.

---

## 3. Change Points Based on Latest Updates

Recent updates resolved a critical bug where the Floating Decor panel failed to become visible when the notification icon was clicked.

### Fixed: Missing SystemBar Controllers
* **Issue**: The `floating_decor_nav`, `page2_nav`, and `two_panel_nav` buttons were defined in `car_bottom_system_bar.xml` without a designated controller.
* **Fix**: Injected `systemui:controller="com.android.systemui.car.systembar.base.CarSystemBarButtonController"` into the XML definitions. This enabled the buttons to attach to the `EventDispatcher` on initialization and successfully fire custom string events (e.g., `toggle_floating_decor`).

### Fixed: Layout Resource Inflation Crashes
* **Issue**: `FloatingDecorViewController` requested the layout identifier for `floating_decor_view`. Since the file only existed in the `MultiPanelLandscapeRRO` namespace and not the base package, it returned `0`, resulting in a null view payload.
* **Fix**: Added a lightweight, empty `floating_decor_view.xml` into `packages/apps/Car/SystemUI/res/layout/`. The Resource Manager now properly resolves the ID within the base namespace and serves the rich override layout from the RRO.

---

## 4. Frequently Asked Questions (FAQ)

**Q: Why do I see "No transition for floating_decor_panel" in the logs?**
A: This usually means the `toggle_floating_decor` event was never broadcasted because the emitting button lacks the `systemui:controller` attribute, or the event string in `systemui:selectedEvent` doesn't exactly match the `onEvent` string in `floating_decor_panel.xml`.

**Q: Can I add another floating panel using this architecture?**
A: Yes. Create a new `[name]_panel.xml` defining the geometric variants, link a controller via `<Controller>`, provide a base layout placeholder in `CarSystemUI`, and trigger it using a navigation button with a uniquely mapped string event.

**Q: Why isn't my RRO layout appearing? It keeps showing the base layout.**
A: Ensure that the RRO targets `com.android.systemui` correctly, has `android:isStatic="true"` or high priority, and is active (`adb shell cmd overlay list`). 

**Q: Why does the album cover reset its animation when the panel collapses?**
A: The `StateManager` re-evaluates visibility during state shifts. Depending on the `DecorPanelController` implementation, if `destroy()` is called on hide, the animator is canceled to conserve GPU and CPU cycles in the background.
