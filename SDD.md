# System Design Document (SDD) - Scalable UI Implementation

This document serves as the chronological System Design Document for the "Fluidic Precision" Scalable UI. It covers the step-by-step implementation points, the specific errors encountered during development, and the exact fixes applied across the Android Automotive OS (AAOS) and Web Orchestrator stacks.

---

## 1. CarSystemUI & Overlays Stabilization

### Objective
Replace the standard AOSP SystemUI layout with a dynamic, multi-panel glassmorphism interface managed by a custom `TaskPanelTransitionCoordinator`.

### Implementation Steps
- **`MultiPanelLandscapeRRO` Creation:** Created a Runtime Resource Overlay to safely override `car_launcher_multi_window.xml` layout and system dimensions (`@dimen/panel_corner_radius`, etc.) without modifying base Android frameworks.
- **Dynamic Activation:** Modified `MultiPanelLandscapeRRO/AndroidManifest.xml` to set `isStatic="false"`. This allowed the UI to be toggled dynamically on demand rather than forcing it on all system users.
- **`deploy_ui.sh` Update:** Updated the deployment script to execute `cmd overlay enable` at runtime, applying the glassmorphism layout upon script execution.

### Errors & Fixes
- **Error:** The panels were rendering incorrectly with overlapping widget boundaries.
- **Root Cause:** A default AOSP overlay (`DewdDynamicAospRRO`) was actively competing for layout resource precedence.
- **Fix:** Removed `DewdDynamicAospRRO` from the `systemui_sample_rros.mk` build file, fully decoupling legacy overlays from the Scalable UI.

---

## 2. Dashboard Modules & Real Estate Management

### Objective
Integrate functional media and HVAC control modules into the primary dashboard while maximizing usable UI real estate.

### Implementation Steps
- **Media Re-integration:** Restored the `ControlBarActivity` and natively bound the `AudioCardModule` to the active `MediaBrowserService` (e.g., Radio, Spotify), rendering it on Page 1.
- **Lifecycle Auditing:** Verified that the `TaskPanelTransitionCoordinator` correctly manages the visibility and pause/resume states of the `media_panel` during drag-and-drop actions across windows.

### Errors & Fixes
- **Error:** The UI felt overly cluttered and vertically constrained on 180dp and smaller form factors.
- **Fix:** Permanently decommissioned the redundant `DrivingStats` widget module, clearing vertical UI space for the newly expanded dual-panel grid and the radio module.

---

## 3. MockWidgets Package & Build Pipeline

### Objective
Deploy custom, system-privileged dashboard widgets (Climate, Smart Home, Agenda, Clock) embedded seamlessly within the `TaskView` boundaries.

### Errors & Fixes
- **Error (Soong/Bazel):** During `m MockWidgets CarSystemUI`, the build aborted with `failed to load @config//main.star`.
- **Root Cause:** Corrupt intermediate file states and Bazel cache desynchronization in the Android build `out/` directory.
- **Fix:** Executed `rm -rf out/soong` to clean the environment, ensuring the `lunch aosp_cf_x86_64_auto-userdebug` target compiled freshly.

---

## 4. Boot-Loop Crash Resolution (Privileged Permissions)

### Objective
Ensure the `MockWidgets` package can operate natively and communicate with the VHAL (e.g., HVAC controls) using the highly privileged `android.car.permission.CONTROL_CAR_CLIMATE` permission.

### Errors & Fixes
- **Error:** The `cvd-6` emulator entered a persistent boot loop. `adb logcat` revealed a fatal `SystemServer` crash:
  `java.lang.IllegalStateException: Signature|privileged permissions not in privileged permission allowlist: {com.android.car.mockwidgets: android.car.permission.CONTROL_CAR_CLIMATE}`
- **Root Cause:** The `Android.bp` configuration utilized `privapp_allowlist: true`, a deprecated directive that failed to package the explicit XML allowlist file into the system image.
- **Fix (Source Code):** Replaced the property in `Android.bp` with the explicit instruction: `required: ["privapp-permissions-mockwidgets.xml"]`.
- **Fix (Live Emulator Injection):** To prevent waiting for a 45-minute clean build, we intercepted the boot-looping emulator directly:
  1. Ran `adb root` and `vdc checkpoint commitChanges` to finalize the initial filesystem state.
  2. Executed `adb remount` to turn the read-only `/system_ext` partition into read-write.
  3. Force-pushed the local `privapp-permissions-mockwidgets.xml` to `/system_ext/etc/permissions/`.
  4. Force-rebooted the container, allowing `SystemServer` to read the allowlist, granting the permission and successfully completing the boot animation.

---

## 5. Community Showroom Web Orchestrator

### Objective
Expose the AOSP Scalable UI to external community users through an automated Next.js frontend and Node.js backend.

### Errors & Fixes (Backend / Redis)
- **Error:** Published demo sessions were silently failing to appear or immediately disappearing from `workspace.aospstack.com/community`.
- **Root Cause:** In `publish_community.js`, the orchestrator failed to enforce persistence on the `aosp:share:<token>` key, leading to immediate expiration by the garbage collector.
- **Fix:** Restructured the Redis registry logic to ensure the share token is stored with a reliable TTL alongside the primary `aosp:community:published` index.

### Errors & Fixes (Frontend / Next.js)
- **Error:** The "Wake Up Session" action frequently failed to open the WebRTC emulator viewer for end users.
- **Root Cause:** The `app/community/page.tsx` triggered an asynchronous `window.open` after resolving the `POST /api/share/start` fetch request. Modern browsers interpret deferred window openings as unauthorized popups and block them.
- **Fix:** Implemented a synchronous window-opening pattern. The UI immediately opens a placeholder loading tab (`window.open('', '_blank')`) on the synchronous click event, and then modifies the tab's `location.href` once the API confirms the emulator has successfully reached the `RUNNING` state.

---

## Conclusion
The combination of RRO-driven UI overrides, strict system permission compliance for custom privileged widgets, and synchronous web orchestration has finalized the architecture. The Scalable UI is fully resilient and prepared for scalable community deployments.
