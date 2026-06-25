# Scalable UI FAQ

**Q: What is the Scalable UI?**
A: Scalable UI is a modular System Window Orchestration framework for Android Automotive OS (AAOS). Instead of hardcoding bounds for applications, it uses XML-driven "Panels" that can dynamically transition, stack, and move based on system events, creating a fluid, multi-window dashboard experience.

**Q: How does the glassmorphism UI work?**
A: The glass effect is simulated using lightweight, layered `<TaskPanel>` elements combined with `Alpha` and `Corner` radius properties. Specialized shadow panels (e.g., `media_source_shadow_controller.xml`) run underneath the main application panels to create depth.

**Q: Why doesn't my newly added app show up in a panel?**
A: Check the following:
1. Does your application's `AndroidManifest.xml` declare `android:resizeableActivity="true"`?
2. Does the `role` attribute in your `panel.xml` match the `ComponentName` or intent array of your app perfectly?
3. Did you rebuild and enable the RRO? (`cmd overlay enable com.android.systemui.rro.scalableUI.multiPanelLandscape`)

**Q: The UI animations are stuttering, how can I fix it?**
A: Ensure you are running on a hardware-accelerated device or an emulator with GPU passthrough. On Cuttlefish, verify that `--gpu_mode=auto` or `--gpu_mode=gfxstream` is functioning correctly, as software rendering (`guest_swiftshader`) will cause UI lag.

**Q: How are Page 2 Mock Widgets different from regular apps?**
A: Main apps (like Maps or Media) are full Activities hosted inside `TaskViews`. Mock widgets are also Activities but are specifically designed to look like static, lightweight dashboard components. They slide into view simultaneously when the `show_page2` broadcast is fired, whereas regular apps slide in based on `_System_TaskOpenEvent`.
