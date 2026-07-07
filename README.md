# Scalable UI (Fluidic Precision)

The **Scalable UI** (codenamed "Fluidic Precision") is an advanced, custom dashboard interface built on top of the Android Automotive OS (AAOS). 

Instead of a static grid, the Scalable UI transforms the vehicle dashboard into a dynamic, multi-panel window environment. It features "glassmorphism" aesthetics (translucent backgrounds, rounded corners) and allows users to seamlessly drag and drop modular widgets (like Climate, Media, Navigation, and Agenda) across different panel zones on the screen.

## Key Features
*   **System Window Orchestration:** A custom layer in AAOS (`TaskPanelTransitionCoordinator`) that handles the math, animations, and lifecycle of dragging an app from a side panel into the main view.
*   **Runtime Resource Overlays (RRO):** Instead of permanently modifying the base Android source code, the project uses an overlay (`MultiPanelLandscapeRRO`). This overlay dynamically injects new dimensions, colors, and layout structures into the system at runtime.
*   **Privileged System Widgets:** A custom package called `MockWidgets` that provides the actual interactive tiles (Climate controls, Smart Home toggles, etc.).

---

## 1. Prerequisites (What to Prepare)
To develop or deploy the Scalable UI, you need the following environment:
1.  **AOSP Automotive Environment:** A full Android Open Source Project (AOSP) source tree synced to an Automotive branch (e.g., `trunk_staging`).
2.  **Cuttlefish Emulator:** The primary target for this project is `aosp_cf_x86_64_auto-userdebug`. You will need a Linux host capable of hardware virtualization to run it.
3.  **The Scalable UI Repository:** This repository (`vendor/aospstack/ScalableUI`), containing the overlays, widget applications, and deployment scripts.

---

## 2. Environment Setup (Step-by-Step)

If you are setting this up from scratch or onboarding a new developer, here is exactly what needs to be prepared in the codebase:

### A. Prepare the Build Configuration
1. Open your AOSP tree and navigate to `packages/apps/Car/SystemUI/samples/systemui_sample_rros.mk`. 
2. You **must remove `DewdDynamicAospRRO`**. If you don't do this, Android's default overlays will conflict with ours, resulting in distorted UI panels.
3. Ensure that `MockWidgets` and `MultiPanelLandscapeRRO` are included in your `make` targets for the build.

### B. Prepare the Privileged Permissions (Crucial Step)
The `MockWidgets` app interacts directly with the Vehicle Hardware Abstraction Layer (VHAL) to change things like the AC temperature. This requires deep system permissions (`android.car.permission.CONTROL_CAR_CLIMATE`).
1. Android requires privileged apps to be explicitly allowlisted. 
2. Ensure `MockWidgets/Android.bp` is configured to package `privapp-permissions-mockwidgets.xml` into `/system_ext/etc/permissions/` using the `required: ["privapp-permissions-mockwidgets.xml"]` directive.
> **Warning**: If you skip this, Android will instantly crash and boot-loop the emulator for security reasons.

### C. Prepare the Dynamic RRO
1. In the `AndroidManifest.xml` of your `MultiPanelLandscapeRRO`, ensure `isStatic="false"` is set.
2. This makes the Scalable UI an "opt-in" experience. The emulator will boot with the standard Android UI initially, ensuring standard compatibility tests pass. 

---

## 3. Build and Deployment

### 1. Build the System
Execute the standard AOSP build process to compile the overlays and widgets alongside CarSystemUI:
```bash
m MockWidgets CarSystemUI CarLauncher MultiPanelLandscapeRRO
```

### 2. Deploy to Emulator
To actually see the Scalable UI, use a deployment shell script (e.g., `deploy_ui.sh`) that runs on the host machine. The script should:
1. Push the compiled RRO APK to the emulator (if not already baked into the system image).
2. Execute the activation command:
   ```bash
   adb shell cmd overlay enable --user 10 com.android.systemui.multipanellandscaperro
   ```
3. Once this command runs, the Android dashboard will instantly morph into the multi-panel Fluidic Precision layout without needing a reboot!

---

## 4. Community Showroom (Web Deployment)
If you're looking to showcase this to external users over the web, you must also prepare the **Community Showroom Orchestrator**:
- **Orchestrator Backend:** A Node.js and Redis stack that provisions ephemeral Cuttlefish emulators on-demand and manages session tokens (`aosp:share:<token>`).
- **Web Frontend:** A Next.js web application that connects users to the running Cuttlefish instances via WebRTC for a smooth, browser-based demo.
\n- [Floating Decor Panel Architecture & Updates](FLOATING_DECOR_PANEL.md)
