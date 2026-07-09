# Android Automotive Scalable UI: Comprehensive FAQ (50+ Questions)

This document serves as the ultimate technical resource and troubleshooting guide for OEMs, developers, and stakeholders working with the Android Automotive Scalable UI architecture.

---

## Part 1: General Architecture & Concepts

**1. What is the Android Automotive Scalable UI?**
Scalable UI is a next-generation System Window Orchestration framework for AAOS. It moves away from hardcoded XML layouts in SystemUI and CarLauncher, replacing them with dynamic, configuration-driven panels that can scale across different screen sizes (portrait, landscape, ultrawide) using Runtime Resource Overlays (RROs).

**2. How does Scalable UI differ from the standard AAOS CarSystemUI?**
Standard CarSystemUI relies on fixed layout structures (top, bottom, left, right system bars). Scalable UI removes these rigid structures and treats the entire screen as a grid. Every UI component (apps, navigation, climate, status) is a "Panel" mapped to the grid via XML configurations, allowing dynamic resizing and drag-and-drop orchestration.

**3. What is a Task Panel?**
A Task Panel is a designated container that hosts standard Android applications (like Google Maps or Spotify) using a `TaskView` or `ActivityView`. It provides a dedicated window bounds area for an external app to render securely.

**4. What is a Decor Panel?**
A Decor Panel is a specialized container used for native SystemUI elements, not external apps. Decor panels are used for floating navigation bars, status icons, and system grips. They do not use `TaskViews` and are rendered directly by the SystemUI process.

**5. What is Window Orchestration?**
Window Orchestration is the background engine within `ScalableUIController` that determines where Task Panels and Decor Panels should be positioned, what their bounds are, what layer they sit on, and how they transition when the user opens the App Drawer or changes pages.

**6. Does Scalable UI require modifications to the Android Framework (base)?**
No. Scalable UI is built purely as a customization layer inside `CarSystemUI` and `CarLauncher`, relying entirely on standard Android APIs (`TaskView`, `WindowManager`, `RRO`, `BroadcastReceiver`).

**7. What is a Variant in Scalable UI?**
A Variant defines a specific state for a Panel. For example, a `media_panel` might have a `visible` variant (full screen), a `minimized` variant (card size), and a `hidden` variant. Transitions interpolate between these Variants.

---

## Part 2: RRO & Theming (Runtime Resource Overlay)

**8. What is an RRO?**
A Runtime Resource Overlay (RRO) is a specialized Android APK containing only resources (XML, drawables, strings) and no executable code. It is used to overwrite the resources of a target package (like `CarSystemUI`) at runtime without altering the target's source code.

**9. How does Scalable UI use RROs?**
Instead of editing the Java source of `CarSystemUI` for every new car model, OEMs create an RRO (e.g., `MultiPanelLandscapeRRO`). This RRO contains the `config.xml` defining all panels and their variants. When the RRO is applied, SystemUI automatically rebuilds the interface.

**10. Why did my RRO cause SystemUI to crash with an `InflateException`?**
When inflating an RRO layout that contains Android theme attributes (`?android:attr/selectableItemBackground`), the inflation engine crashes if the `PackageContext` is un-themed. RRO contexts are un-themed by default.

**11. How do you safely inflate an RRO layout in Java?**
Instead of using `LayoutInflater.from(rroContext)`, you must extract the `XmlResourceParser` from the RRO and feed it into the base SystemUI context:
`LayoutInflater.from(systemUiContext).inflate(rroParser, null);`

**12. Why does `findViewById` fail when looking for RRO elements?**
Because the elements are compiled into the RRO's package namespace, not the `SystemUI` namespace. You must look up the integer ID dynamically:
`int id = rroContext.getResources().getIdentifier("nav_home", "id", rroPackageName);`

**13. Do RROs require rebooting to apply?**
Usually no. Activating an RRO via `cmd overlay enable` restarts the target application's activities. However, for `SystemUI`, it may cause a brief screen flicker as the `SystemUIService` restarts its view hierarchy.

**14. What is `config_default_activities`?**
It is an XML string-array inside the RRO that maps Panel IDs to the default Activities or Layouts that should automatically launch inside them when the vehicle boots.

**15. Can an RRO override animations?**
Yes. Since Transitions and Variants are defined in XML, an RRO can change the duration, interpolator, and scale/alpha properties of any panel transition.

---

## Part 3: Floating Decor Panels & Z-Ordering

**16. How does the Floating Navigation Bar work?**
It is mapped as a Decor Panel. `ScalableUIController` reads its layout parameters and uses `WindowManager` to pin it to Layer 15, ensuring it physically floats above the application layer (Layer 10).

**17. Why use Layer 15?**
Layer 15 guarantees that even if a user maximizes a Task Panel (like a full-screen map), the floating navigation bar remains visible and interactive on top of the map.

**18. What happens if a Decor Panel layout is missing?**
If `FloatingNavViewController` cannot find the layout in the RRO, it falls back to instantiating an empty `new View(mContext)`. The window will still exist, but it will be invisible.

**19. How do you handle click events in a Decor Panel?**
Because Decor Panels run inside the `SystemUI` process, click events are bound in the Controller (e.g., `FloatingNavViewController`). The controller fires standard Android Intents (e.g., `ACTION_MAIN`, `CATEGORY_HOME`) to navigate the system.

**20. Can a Decor Panel have transparent backgrounds?**
Yes. By setting the root layout to `@android:color/transparent` and applying rounded corner boundaries in the XML Variant, the panel appears as a floating "island" or "pill" over the background apps.

**21. How do Drag Handles work?**
Drag Handles are UI elements embedded in Decor Panels. They intercept touch events and communicate with the `TaskPanelTransitionCoordinator` to expand or shrink the underlying Task Panels.

**22. What is the difference between `DecorPanel` and `TaskPanel` XML definitions?**
None structurally. Both use the `<Variant>` tag. However, `DecorPanel` XML includes a `controller` attribute pointing to its Java Controller, while `TaskPanel` XML typically points to a target activity.

**23. Can I have multiple floating panels?**
Yes. You can define `floating_nav_panel`, `floating_media_panel`, `floating_climate_panel`, etc., as long as their bounds do not overlap (unless explicitly designed to stack).

**24. Why did my Decor Panel draw behind a Task Panel?**
Check your Layer configuration in the XML Variant. If your Decor Panel is on Layer 10 and the Task Panel is on Layer 12, the app will cover the Decor Panel. Ensure Decor Panels are on higher layers (e.g., Layer 15).

---

## Part 4: Multi-Window & Application Hosting

**25. How do I launch a specific app into a specific panel?**
Use the `TaskPanelTransitionCoordinator`. You can programmatically request an app launch by passing the `ComponentName` and the target `Panel ID`.

**26. Can two apps run side-by-side?**
Yes. Define two Task Panels (e.g., `map_panel` and `media_panel`) side-by-side. CarSystemUI will host both `TaskViews` simultaneously.

**27. What happens if an app crashes inside a Task Panel?**
The `TaskView` relies on the `ActivityManager`. If the hosted activity crashes, the `TaskView` becomes empty. CarSystemUI usually detects this lifecycle change and can re-launch the default activity defined in `config_default_activities`.

**28. How does the App Drawer open?**
The App Drawer (`CarLauncher` App Grid) is triggered via an Intent. When fired, the Orchestrator initiates a Transition that scales down the Map/Media panels and scales up the App Grid panel.

**29. Do third-party apps need to be modified for Scalable UI?**
No. Standard Android apps run perfectly inside `TaskViews`. However, apps that do not support `resizeableActivity="true"` may display black borders or refuse to launch in split-screen panels.

**30. How is audio focus handled?**
Audio focus is handled natively by the Android Audio Framework. Running multiple media apps in different Task Panels will result in standard audio focus stealing behavior.

**31. Can I move an app from one panel to another?**
Yes. The Orchestrator allows reparenting the underlying Activity token from one `TaskView` to another without killing the app process.

---

## Part 5: MockWidgets & Prototyping

**32. What is the MockWidgets package?**
It is a lightweight utility package provided with the AOSP Stack containing standalone dummy applications (Climate, MockMedia, Agenda, Status) used for filling Task Panels during UI development.

**33. Why use MockWidgets instead of real system apps?**
Real automotive apps often require deep HAL (Hardware Abstraction Layer) dependencies (e.g., real HVAC controllers). MockWidgets bypass these requirements, allowing UI designers to test the Scalable UI on standard PC emulators without vehicle hardware.

**34. How are MockWidgets styled?**
They follow standard Android XML theming but are designed to blend seamlessly with the "Fluidic Precision" glassmorphic design language of the Scalable UI framework.

**35. Can I interact with MockWidgets?**
Yes. For example, the `ClimateActivity` has clickable temperature controls, and the `MockMediaActivity` features rotating album art. However, they do not actually alter the vehicle's state.

---

## Part 6: Build, Deployment & Emulation

**36. How do I build the Scalable UI?**
Source your AOSP environment, run `lunch` for your target (e.g., `aosp_cf_x86_64_auto-trunk_staging-userdebug`), and run `m CarSystemUI CarLauncher MockWidgets MultiPanelLandscapeRRO`.

**37. How long does a full build take?**
An initial AOSP tree build takes hours. Incremental module builds (like `m CarSystemUI`) usually take 5–15 minutes depending on Soong dependency analysis.

**38. How do I test the UI on a PC?**
We use `Cuttlefish` (Android's native cloud/virtual emulator). You launch it using `launch_cvd` or via a Docker container provided in the `scalable_ui_bundle`.

**39. Why do I lose permissions when pushing `CarSystemUI.apk` manually?**
When you use `adb push` to overwrite a privileged system app, Android's `PackageManager` detects the signature/file change and conservatively revokes all granted runtime permissions for security reasons.

**40. What is the `start.sh` script?**
It is an automated deployment pipeline that starts the Cuttlefish emulator, waits for the boot completion flag, injects the prebuilt APKs, and restores the required permissions.

**41. Why does my emulator show a black screen after `start.sh`?**
This usually means `SystemUIService` crashed. Check `logcat`. The most common causes are `SecurityException` (missing permissions) or `InflateException` (bad XML references in your RRO).

**42. How do I fix the Bluetooth `SecurityException`?**
Run `adb shell pm grant com.android.systemui android.permission.BLUETOOTH_CONNECT` and `adb shell pm grant com.android.systemui android.permission.BLUETOOTH_SCAN`.

**43. Can I test on a real vehicle display?**
Yes. If the vehicle runs an engineering/userdebug build of AAOS 14+, you can push the APKs and RRO to the `system_ext` and `product` partitions respectively, then reboot.

---

## Part 7: Troubleshooting & Debugging

**44. SystemUI is crash-looping. How do I read the logs?**
Run `adb logcat | grep -E "AndroidRuntime|SystemUI"`. Look for `FATAL EXCEPTION: main`.

**45. My RRO is not applying. What do I do?**
Run `adb shell cmd overlay list`. Look for your RRO. If there is an `[ ]` next to it, it is disabled. Run `adb shell cmd overlay enable com.android.systemui.rro.scalableUI.multiPanelLandscape`.

**46. `adb devices` is empty or says offline.**
Your Cuttlefish emulator or Docker container has crashed. Restart it using `stop_cvd` followed by `launch_cvd`. If using Docker, restart the container.

**47. The Map panel is blank.**
The `MapsPlaceholderActivity` might be disabled, or the emulator has no internet connection to render the mock maps. Check network connectivity via `adb shell ping google.com`.

**48. My custom Decor Panel isn't showing up.**
1) Check if the ID is listed in `window_states` in the RRO `config.xml`. 2) Check if it is listed in `config_default_activities`. 3) Verify the XML variant has `isVisible="true"` and `alpha="1.0"`.

**49. The Drag Handle isn't responding to swipes.**
Drag Handles require the `TaskPanelTransitionCoordinator` to be properly bound. Check Logcat for "Unbound drag handler" warnings. Ensure the Decor Panel sits on a higher Z-layer than the app panel beneath it; otherwise, the app consumes the touch events.

**50. How do I force a UI refresh without rebooting?**
Run `adb shell am crash com.android.systemui`. The Android watchdog will immediately restart the SystemUI process and reload all XML layouts and RRO configurations from scratch.

---

## Part 8: Advanced Concepts

**51. Can Scalable UI handle multiple displays (Passenger, Cluster)?**
Yes. `ScalableUIController` checks the `displayId`. You can create entirely separate RRO XML configurations mapping `displayId="1"` (Passenger) to different Task Panels.

**52. Is Scalable UI open-source?**
The core components rely on AOSP Car features. OEM-specific implementations (like the "Fluidic Precision" RRO) are proprietary configurations managed by the respective design teams.
