# Scalable UI: Internal Demonstration Guide

This guide details the internal workings of the Scalable UI architecture for Android Automotive OS (AAOS), specifically focusing on how the demonstration manages multi-window layouts, floating elements, and transitions.

## 1. Core Architecture: RROs and System Window Orchestration

The Scalable UI does not rely on hardcoded bounds inside the native `CarSystemUI`. Instead, it leverages **Runtime Resource Overlays (RROs)** (e.g., `MultiPanelLandscapeRRO`) to define the visual layout dynamically. 

By pushing XML configurations through the RRO, we overwrite `CarSystemUI` arrays like `config_systemUIWindowStates`. This approach is known as **System Window Orchestration**, where the System UI reads these XML definitions at runtime and allocates window bounds for activities without requiring core framework modifications.

## 2. Floating Decor Panels

**Decor Panels** are specialized, non-full-width UI elements (like the "Pill Dock" navigation bar or the floating media controller) that sit in a persistent window layer above standard applications.

- **Implementation**: They implement the `DecorPanelController` interface.
- **Window Layer**: They are assigned to **Layer 15**, ensuring they remain visible and un-obscured by user applications (`TaskViews`).
- **Inflation**: The views are dynamically inflated using `PackageContext` resolution. This allows the core `CarSystemUI` to lookup layouts like `floating_nav_view.xml` that are injected purely via the RRO, ensuring stability even if resources are missing.

## 3. Task Panels and Containers

**Task Panels** act as the containers for traditional Android Activities (e.g., Google Maps, Spotify, MockWidgets).

- **Implementation**: Under the hood, they utilize `TaskView` elements. A `TaskView` is an Android framework component that allows an Activity from a different process to be embedded seamlessly into the System UI.
- **Role Binding**: Each Task Panel in the XML is assigned a `role` (e.g., `role="map"`). When an intent matching that role is fired, the System UI directs the resulting Activity into the bounds of that specific Task Panel.
- **Glassmorphism**: Task Panels are rendered with translucent properties and drop-shadow underlays (via Shadow Controllers) to create a multi-layered, premium glassmorphism effect.

## 4. Window States and Transitions

The entire UI is state-driven. A **Window State** (defined in `window_states.xml` within the RRO) is a snapshot of all active panels, their dimensions, and their coordinates.

- **Transitions**: When a user triggers an event (like opening the App Grid or dragging a panel handle), the System UI broadcasts an intent. The Scalable UI controller intercepts this, looks up the target Window State, and uses `ObjectAnimator` to fluidly transition all Task Panels and Decor Panels from their current bounds to their new bounds simultaneously.
- **Responsiveness**: Drag-and-drop handles dynamically update the active `Variant` bounds via reflection, instantly scaling the `TaskView` containers alongside the touch event.
