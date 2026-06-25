# Scalable UI Framework: Exhaustive FAQ & Deep-Dive Troubleshooting

This document provides a highly detailed, low-level explanation of the common pitfalls encountered when building for the Scalable UI Framework, alongside strategies for OEMs and App Developers migrating standard apps.

---

## ⚙️ OEM Integration: Toggling & Full-Screen Capabilities

### Q: In a real OEM scenario, when should Scalable UI be enabled vs. disabled? Can the user toggle it?
**A: Yes, it is designed to be fully togglable at runtime via Runtime Resource Overlays (RROs).**
* **OEM Scenario:** Many OEMs provide a "Classic Mode" (one app full screen) and a "Dashboard Mode" (Scalable UI multi-panel). 
* **How it works:** The OEM can add a toggle in Quick Settings. When the user flips the switch, the system executes:
  `adb shell cmd overlay disable com.android.systemui.rro.scalableUI...` (Disables Dashboard)
  `adb shell stop && adb shell start` (Restarts SystemUI to apply).
  Once disabled, the `SystemWindowOrchestrator` drops its custom XML rules, and apps launch exactly as they do on a standard Android tablet—taking up the entire screen.

### Q: If Scalable UI is enabled, how does the user launch an app into Full Screen later?
**A: You handle this via Orchestrator Transitions, NOT by disabling the framework.**
If the user is in the Scalable UI dashboard but wants the Map to temporarily take up the whole screen, the Orchestrator handles it.
1. **The Expansion Variant:** The RRO developer defines a `<Variant id="@+id/fullscreen">` inside `map_panel.xml` where the Bounds stretch from `0dp` to `1920dp` (the entire display).
2. **The Event:** The OEM places a "Maximize" button on the UI (usually on the DecorPanel frame above the app). When clicked, it broadcasts an event like `_System_TaskMaximizeEvent`.
3. **The Transition:** The Orchestrator catches this event, reads the Transition rule, and smoothly animates the Map from its small 400dp box to the massive `1920dp` full-screen variant, covering the other panels.
### Q: Is it possible for the user to change or swap tile positions at runtime?
**A: Yes, but not through free-form drag-and-drop.**
The Orchestrator relies on statically compiled XML files (e.g., `media_panel.xml`) that define precise `<Bounds>` for each state. Because the framework rigidly animates `SurfaceControls` between these predefined states to guarantee 60fps performance, it does not natively support dragging a tile to arbitrary pixel coordinates. 

However, you can achieve runtime tile swapping using two methods:
1. **RRO Profile Switching (Recommended & Standard)**: Compile multiple layouts (e.g., `LayoutA.apk` with Radio on the Left, and `LayoutB.apk` with Radio on the Right). A "Switch Layout" button in the Settings app can trigger the Android Overlay Manager Service (OMS) to dynamically disable the old layout and enable the new one at runtime (`cmd overlay enable/disable`). SystemUI will instantly reload the new layout definitions, snapping the tiles to their new configured positions.
2. **Custom Variant Triggers (Advanced)**: Define mirrored states inside your existing XML files (e.g., `@id/opened_left` and `@id/opened_right`). When a user taps a button, fire a custom broadcast event (e.g., `_System_SwapLayoutEvent`), and the Orchestrator's transition engine will physically animate the panels to swap places based on your defined `<Transition>` rules.

---

## 🚀 Migration Guide: Transforming Normal Apps into Scalable Apps

If you are a developer taking a standard Android application and migrating it to work perfectly inside the Scalable UI, here are the major architectural shifts you must prepare for:

### Q: What happens to my Popups, Dropdowns, and Dialogs?
**A: They get trapped inside the "Glass Window".**
* **The Problem:** In normal Android, an `AlertDialog` dims the entire screen and sits in the center. In Scalable UI, standard dialogs are constrained *to the TaskView*. The screen won't dim; only your small 400dp panel will dim, and the dialog will be squeezed inside it. 
* **The Fix:** If you need a small confirmation prompt, standard dialogs are fine. But if you need a massive, screen-blocking warning, your app can no longer use standard Dialogs. It must either trigger a System-Level notification (which the Orchestrator can handle natively), or the app must be granted `SYSTEM_ALERT_WINDOW` permissions (which is generally discouraged for third-party apps).

### Q: How do I handle the On-Screen Keyboard (IME)?
**A: You must coordinate with the Orchestrator.**
* **The Problem:** If your panel is pinned to the bottom of the screen (e.g., `bottom="1000dp"`), and the user clicks a text field, the Android keyboard will slide up from the bottom and completely cover your panel. Because your app is trapped in a `TaskView`, Android's default `adjustPan` behavior (which normally pushes the app up) might fail or cause visual clipping.
* **The Fix (App Side):** Ensure your manifest uses `android:windowSoftInputMode="adjustResize"`. 
* **The Fix (Framework Side):** The best OEMs define a specific `<Variant id="@+id/keyboard_open">` in the RRO. When the keyboard opens, the Orchestrator slides the affected panel upwards so it sits cleanly above the keyboard.

### Q: Do I need to rewrite my app's navigation (Back buttons, Menus)?
**A: No, but you must stop assuming you have unlimited screen real estate.**
* **The Problem:** Normal apps rely on massive side-drawers (Navigation Drawers) or bottom navigation bars. In a 400dp wide TaskPanel, a side drawer will cover 90% of your content.
* **The Fix:** Migrate to collapsible menus, hamburger icons that open lightweight dropdowns, or rely on horizontal scrolling (like tabs). Never assume the user can swipe from the absolute edge of the physical screen to open a drawer, because the physical edge of the screen might belong to a different app panel!

---

## 🧭 Boundary Troubleshooting: Is it an App Bug or an RRO Bug?

When a panel looks wrong (e.g., an icon is misplaced, or the height is wrong), the hardest part is knowing *where* to fix it. The Scalable UI separates external bounds from internal layout. Use this matrix to diagnose the issue:

### Scenario 1: The entire panel is in the wrong place on the screen, or the overall height/width of the "glass window" is wrong.
* **Where to fix:** **Framework Side (RRO XML)**
* **What to fix:** Open `media_panel.xml` (or your specific panel). Locate the active `<Variant>` (e.g., `@id/opened`) and adjust the `<Bounds left="..." top="..." right="..." bottom="..."/>`. The app has zero control over its physical screen coordinates.

### Scenario 2: The panel is the correct size, but my buttons/icons are too close to the edge and getting sliced off by the rounded corners.
* **Where to fix:** **App Side (App XML Layout)**
* **What to fix:** The Orchestrator applies a physical clipping mask (the rounded corners). The app needs to know to stay away from the "clip zone". Open your app's layout (e.g., `activity_main.xml`), find the root view (like `ConstraintLayout`), and increase the global `android:padding` (e.g., change `16dp` to `24dp`).

### Scenario 3: When the panel transitions from "opened" (large) to "stacked" (small), my UI elements overlap each other or get squished incorrectly.
* **Where to fix:** **App Side (App XML Layout)**
* **What to fix:** Your app's internal layout constraints are not responsive enough. Open the app's XML and ensure you are using relative constraints (`app:layout_constraint...`), `0dp` (match_constraint), or `layout_weight`. Remove any hardcoded pixel heights/widths on your parent containers.

### Scenario 4: The UI looks fine, but when I tap an icon, the touch registers a few pixels away from where my finger is.
* **Where to fix:** **Framework Side (RRO XML)**
* **What to fix:** This happens if the `TaskView` scaling matrix gets misaligned during a transition. Verify that your transition `duration` matches perfectly and that you aren't doing complex rotation animations on the bounds. This usually requires an Orchestrator restart (`adb shell stop && adb shell start`).

---

## 🏗️ Architecture & XML Orchestration Pitfalls

### Q: Why is my panel duplicating, overlapping with another panel, or becoming a "Ghost" when a background event fires?
**A: You are using "Generic Transitions" by omitting the `fromVariant` attribute.**
* **The Low-Level Problem:** The `SystemWindowOrchestrator` relies on a strict State Machine. When you define `<Transition onEvent="show_page2" toVariant="@id/page2_is_visible" />`, you are telling the orchestrator: "Whenever the `show_page2` event is broadcast on the Event Bus, grab this panel and force its bounds and alpha to the `page2_is_visible` state." 
* **The Bug:** If your panel is *already* hidden (e.g., the user closed it 5 minutes ago), the orchestrator still tries to apply this transition. It rips the panel out of its "closed" state, briefly makes it visible, and animates it to "page2". This breaks the view hierarchy constraints, causing panels to overlap or permanently ghost on the screen.
* **The Exhaustive Fix:** You must create an explicit, strict path for *every possible state* your panel could be in when that event fires.
```xml
<!-- CORRECT: Strict State Machine -->
<Transition onEvent="show_page2" fromVariant="@id/opened" toVariant="@id/page2_is_visible" />
<!-- Now, if the panel is "closed" when the event fires, the orchestrator safely ignores it! -->
```

### Q: Why does my panel instantly disappear (pop out) instead of smoothly sliding off the screen during an animation?
**A: You set `isVisible="false"` on the destination Variant.**
* **The Low-Level Problem:** A `<Transition>` mathematically interpolates the differences between two Variants over the specified `duration`. It interpolates `Bounds` (x/y coordinates) and `Alpha` (0.0 to 1.0). However, `isVisible` is a boolean. It cannot be interpolated. If the destination Variant has `isVisible="false"`, the Android View hierarchy immediately strips the `TaskView` from the layout tree at frame 1 of the animation. The panel vanishes instantly, even if the Bounds were supposed to take 500ms to slide off-screen.
* **The Exhaustive Fix:** If a panel is animating to an off-screen position (e.g., sliding left), its destination Variant MUST have `isVisible="true"`. To hide it, you rely on the `Bounds` pushing it off the physical screen coordinates (e.g., `left="-800dp"`), or `Alpha` dropping to `0.0`. Only set `isVisible="false"` on a state if the panel is meant to remain completely dormant and inactive in the background to save GPU memory.

---

## 📱 App-Side Configuration Pitfalls

### Q: Why does the SystemUI crash, or my app render as a tiny black box, when the panel tries to open?
**A: The Android WindowManager rejected the freeform resize request because of your Manifest.**
* **The Low-Level Problem:** Standard Android apps are designed to run full-screen. The Scalable UI framework uses a special system-level API called `TaskView` to capture the app's drawing surface (SurfaceControl) and forcefully inject it into a smaller, floating window inside the CarLauncher. If the app hasn't explicitly granted permission to be resized, the WindowManager panics.
* **The Exhaustive Fix:** You must add `android:resizeableActivity="true"` to the `<activity>` tag in your `AndroidManifest.xml`.

### Q: Why does the UI lag terribly or flicker black when the panel transitions between "opened" and "stacked"?
**A: Your Activity is being destroyed and recreated on every single frame of the animation.**
* **The Low-Level Problem:** When the Orchestrator animates the `Bounds` of a panel, the width and height of the app's window are physically changing in real-time. By default, Android handles a screen size change by completely destroying the Activity and rebuilding it from scratch. Doing this 60 times a second during a 300ms animation causes massive lag.
* **The Exhaustive Fix:** You must tell Android that your app will handle its own resizing. Add `android:configChanges="screenSize|smallestScreenSize|screenLayout|orientation"` to your `<activity>` tag. This prevents destruction and instead calls `onConfigurationChanged()`, which is lightning fast.

### Q: Why do I lose my app state (e.g., text entered in a box) when I swipe to Page 2 and come back?
**A: The Orchestrator is launching a duplicate instance of your Activity.**
* **The Low-Level Problem:** When transitioning back to Page 1, the Orchestrator fires an Intent to ensure the app is brought to the front of the `TaskView`. If your `launchMode` is set to `standard` (the default), Android creates a brand new copy of the Activity and slaps it on top of the old one.
* **The Exhaustive Fix:** Add `android:launchMode="singleTask"` or `singleTop` to your manifest. This instructs the WindowManager to find the existing, running instance of your app and simply resize it back into view, preserving all state.

---

## 💻 Hardware & Performance Guidelines

### Q: What are the CPU and Memory requirements for running the Scalable UI?
**A: 8GB RAM minimum, with a multi-core SoC and 4-6 hardware display overlays.**
Introducing the System Window Orchestrator heavily shifts the burden onto the GPU's compositor and system memory.
* **Memory (RAM):** Every `TaskPanel` is backed by a dedicated `SurfaceControl` and buffer queue. Maintaining 3 to 5 active background applications to populate the dashboard simultaneously will consume an additional **300MB – 600MB of RAM** compared to a single-app full-screen layout. 8GB is the strict minimum, with 12GB recommended for premium OEMs.
* **CPU:** When panels animate, the bounds of multiple applications resize at 60fps. The guest applications must recalculate their internal constraints on every frame. An 8-Core processor (e.g., Snapdragon Automotive Cockpit Gen 3 or Gen 4) is required to ensure the RenderThread does not miss the 16ms frame deadline.
* **GPU (Hardware Composer):** Your display controller **must support 4 to 6 hardware overlay planes (HWC)**. If the GPU cannot composite the different glass layers, shadow layers, and panels natively in hardware, Android falls back to "Client Composition" (SurfaceFlinger computing via the GPU pipeline). Client composition during a multi-panel animation causes massive thermal throttling and lag.

### Q: Does Google have any official recommendations for multi-window AAOS?
**A: Yes, strictly limit active TaskViews and tune the LMKD.**
1. **Limit Active TaskViews:** Google recommends ensuring that no more than 3 to 4 `TaskViews` are actively rendering visible pixels simultaneously. If you have 10 panels in your RRO, ensure the non-visible ones have `<Visibility isVisible="false"/>` to aggressively destroy dormant GPU surfaces.
2. **Memory Killer Tuning:** OEMs must tune the Android Low Memory Killer Daemon (LMKD). Thresholds must be relaxed so Android doesn't forcefully kill background media or map services just because the foreground app is using RAM; otherwise, panels will "black out" and cold-boot when swiped back into view.
