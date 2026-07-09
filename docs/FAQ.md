# Scalable UI Customer FAQ

This document provides standardized answers for customer queries regarding the Scalable UI architecture, focusing on panel orchestration, floating containers, and window management.

## 1. General Architecture

**Q: What is the Scalable UI?**
A: Scalable UI is a modular System Window Orchestration framework for Android Automotive OS (AAOS). Instead of hardcoding bounds for applications, it uses XML-driven "Panels" that can dynamically transition, stack, and move based on system events, creating a fluid, multi-window dashboard experience.

**Q: Does Scalable UI require modifying the core Android framework?**
A: No. It utilizes standard Runtime Resource Overlays (RROs) to inject window configurations and layouts, avoiding intrusive framework hacks. This makes it highly portable across different OEM hardware.

## 2. Decor Panels & Floating Windows

**Q: How does the "Pill Dock" (Navigation Bar) or the floating media player work?**
A: These elements are implemented as **Decor Panels**. Unlike standard apps, they are inflated by the `DecorPanelController` directly into a persistent system window layer (Layer 15). This ensures they float above all other content and are never hidden when apps transition.

**Q: What happens if a Decor Panel layout is missing from the configuration?**
A: The architecture is fault-tolerant. We utilize dynamic `PackageContext` resolution to locate resources. If an RRO fails to provide a layout, the system falls back to an empty, invisible container, completely preventing `NullPointerExceptions` and System UI crashes.

## 3. Task Panels & Application Containers

**Q: How are third-party apps contained within the dashboard?**
A: Apps are hosted within **Task Panels**. These panels utilize Android's native `TaskView` API, which securely embeds external Activities into the System UI space. The bounds of the `TaskView` are strictly managed by our orchestration engine.

**Q: Why doesn't my newly added app show up in a panel?**
A: Ensure the following:
1. Your application's `AndroidManifest.xml` must declare `android:resizeableActivity="true"`.
2. The `role` attribute in your RRO's `panel.xml` must perfectly match the `ComponentName` or intent category of your app.

## 4. Window States & Transitions

**Q: How do multi-panel layouts transition so smoothly?**
A: Layouts are defined as **Window States**. A Window State maps exactly where every panel should be. When shifting from one state to another (e.g., from dual-panel to single-panel), the system animates the bounds of the `TaskViews` and Decor Panels simultaneously using synchronized Object Animators.

**Q: Can the user resize the panels manually?**
A: Yes. We support interactive Drag Handles. The handles intercept touch events and dynamically adjust the active Window State bounds in real-time, instantly resizing the adjacent Task Panels.
