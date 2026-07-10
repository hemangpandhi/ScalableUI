# Scalable UI: Dynamic RRO Binding Architecture

This document details the **"Zero-Compile Binding"** concept used in the Scalable UI architecture. This approach provides maximum agility for OEMs, allowing complete UI overhauls—including adding new buttons or custom graphics—without ever needing to recompile the core `CarSystemUI` Java code.

## The Paradigm Shift: Total OEM Agility

Historically, SystemUI modifications required touching deep Java files, defining static IDs, calculating complex screen coordinates, and compiling custom widgets directly into the AOSP framework. 

The **Dynamic RRO Binding** approach shifts this paradigm. It completely decouples the **Visual Presentation (The View)** from the **Logic (The Controller)** using native AOSP Scalable UI configuration schemas combined with dynamic context inflation.

### 1. The Controller (Java) - The Logic Layer
The Controller (e.g., `FloatingNavViewController`) acts as the "Brain" of the panel. 
*   **Responsibilities:** It connects to hardware (e.g., HVAC via `CarPropertyManager`), listens to media sessions (`MediaSessionManager`), and handles UI intent routing.
*   **The Decoupling:** The Controller **does not know what the UI looks like, nor where it is placed on the screen.** It relies on dynamic inflation (`createPackageContext`) and reflection (`getIdentifier`) to find whatever views the OEM decided to include in the RRO.

### 2. The View Definition (XML) - The Presentation Layer
The visual representation of the UI (buttons, icons, colors) is defined as a standard Android XML layout inside a **Runtime Resource Overlay (RRO)** (e.g., `floating_nav_view.xml`).
*   **OEM Agility:** Because the controller uses `getIdentifier`, the OEM can define *new* IDs directly in the RRO (`@+id/nav_new_feature`) without modifying the base `CarSystemUI/res/values/ids.xml`. The Java code will safely ignore buttons it doesn't recognize, and seamlessly bind logic to the ones it does.

### 3. The Orchestration Binding (XML) - The Configuration Layer
The RRO also contains the configuration files (`floating_nav_panel.xml` and `floating_nav_controller.xml`) which tell the System Window Orchestrator how to manage the window.
*   **AOSP Standard Compliance:** We strictly follow the AOSP `<DecorPanel>` schema.
    1.  The root tag requires `systemui:id`, `systemui:role`, and `systemui:defaultVariant`.
    2.  All spatial constraints (`<Bounds>`) and rendering layers (`<Layer>`) are strictly nested inside `<Variant>` blocks.
    3.  The layout mapping is provided natively in `config_default_activities`.

---

## The Request Lifecycle: How It Works Under The Hood

When the AAOS device boots, the following pipeline executes:

1.  **System Boot:** The `CarSystemUI` process starts.
2.  **Configuration Reading:** The Scalable UI orchestrator reads the `window_states` array in `config.xml` and finds `@xml/floating_nav_panel`. It also reads `config_default_activities` to map the panel to `@layout/floating_nav_view`.
3.  **Variant Parsing:** The orchestrator reads the `<DecorPanel>` schema, identifying the default `<Variant>` and extracting the `<Bounds>` (physical geometry) and `<Layer>` (Z-index).
4.  **Controller Injection:** Dagger instantiates the `FloatingNavViewController`.
5.  **View Inflation (Zero-Compile Magic):** The Controller uses `createPackageContext` to reach into the OEM's RRO package and uses `LayoutInflater` to inflate the layout.
6.  **Logic Binding:** The Controller executes `getIdentifier("nav_hvac_up", "id", rroPackage)` to dynamically resolve the OEM's button IDs and attach hardware listeners.

## Conclusion

By combining the strict `DecorPanel` `<Variant>` schema with dynamic `createPackageContext` inflation, OEMs achieve the best of both worlds: 100% compliant and stable SystemUI orchestration, with the extreme agility of dropping in new graphics and buttons without ever touching Java code.
