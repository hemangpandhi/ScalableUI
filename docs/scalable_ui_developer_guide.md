# Scalable UI Developer Guide: Customizing Apps and Framework Orchestration

This guide explains the step-by-step process of integrating a new application feature into the "Fluidic Precision" Scalable UI framework. It covers both the **Application Side** (Activity Manifest and UI XML Rules) and the **Framework Side** (Orchestrating the UI through XML Variants and Transitions).

---

## 1. App Side: XML Layout Design Rules

Because the SystemUI Orchestrator dynamically resizes your application and applies physical clipping masks (like rounded corners), your app's standard XML layout (e.g., `activity_main.xml` or `climate_widget.xml`) must follow strict rules to avoid breaking visually.

### Rule 1: Never Hardcode Root Dimensions
The `TaskPanel` size changes dynamically (e.g., squashing from 800dp to 400dp width when moving to a "stacked" variant). 
* **DO NOT** use `android:layout_width="800dp"`.
* **DO** use `match_parent` on the root view.
* **DO** use `ConstraintLayout` or `LinearLayout` with `layout_weight` for all internal spacing. The UI must be able to squeeze like a sponge.

### Rule 2: The "Glassmorphism" Corner Clipping Rule
The Orchestrator applies a hardware-accelerated corner mask to your entire app surface (e.g., `<Corner radius="48dp"/>`). The app is completely unaware of this mask. 
If you place a clickable button at the absolute top-left corner (`0dp`, `0dp`), the Orchestrator's rounded corner mask will literally slice your button in half.
* **DO** add a global padding to your root layout that is at least half the size of the maximum corner radius defined in the RRO.
* **Example:** If the RRO defines `<Corner radius="48dp"/>`, your root layout should have `android:padding="24dp"`.

---

## 2. Dual Mode: Handling Scalable UI vs. Full Screen

A common requirement is for an app (like the Media Player) to look completely different when running inside a small Scalable UI panel versus when the user maximizes it to full screen (e.g., hiding album art in the panel, but showing massive album art in full screen).

Because your Manifest declares `android:configChanges="screenSize"`, Android **will not** automatically reload your layout from the `res/layout-w1000dp/` folder when the panel resizes. You must handle the layout switch dynamically.

### Strategy A: The Jetpack Compose Way (Recommended & Easiest)
If your app is built using modern Jetpack Compose, handling the transition from Panel to Full-Screen is effortless. You simply wrap your root UI in `BoxWithConstraints` and read the available width.

```kotlin
@Composable
fun MediaAppRoot() {
    BoxWithConstraints {
        if (maxWidth < 600.dp) {
            // We are trapped in a small Scalable UI panel!
            CompactScalableMediaPanel()
        } else {
            // We have the full 1920x1080 screen!
            FullScreenImmersiveMediaPlayer()
        }
    }
}
```
*Compose automatically recomposes the UI at 60Hz during the Orchestrator's resize animation, resulting in a buttery smooth transition between the two layouts.*

### Strategy B: The XML / ConstraintSet Way
If you are using legacy XML layouts, you must listen for the size change in your Activity and apply a new `ConstraintSet` to physically rearrange the views on the fly.

1. **Create two layouts**: `res/layout/media_compact.xml` and `res/layout/media_fullscreen.xml`. (Both must share the exact same view IDs!).
2. **Listen for the change:**
```java
@Override
public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    
    ConstraintLayout rootLayout = findViewById(R.id.root_media_layout);
    ConstraintSet set = new ConstraintSet();

    if (newConfig.screenWidthDp < 600) {
        // We are in the small Scalable Panel
        set.clone(this, R.layout.media_compact);
    } else {
        // We are in Full Screen mode
        set.clone(this, R.layout.media_fullscreen);
    }
    
    // Apply the new constraints smoothly
    TransitionManager.beginDelayedTransition(rootLayout);
    set.applyTo(rootLayout);
}
```

### Strategy C: The Fragment Swap Way
If the logic between the Full-Screen and Panel modes is vastly different, you can swap entirely different Fragments in `onConfigurationChanged()`.
```java
@Override
public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    if (newConfig.screenWidthDp < 600) {
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.fragment_container, new CompactMediaFragment())
            .commit();
    }
}
```

---

## 3. App Side: Manifest Configuration

To allow the Scalable UI Orchestrator to render your app inside a fluid, resizable panel, your application's `Activity` must be properly configured.

```xml
<activity
    android:name=".MyCustomFeatureActivity"
    android:exported="true"
    android:resizeableActivity="true" <!-- CRITICAL: Allows the orchestrator to scale it -->
    android:launchMode="singleTask"     <!-- Prevents duplicate instances when swiping pages -->
    android:configChanges="screenSize|smallestScreenSize|screenLayout|orientation">
</activity>
```

---

## 4. Framework Side: XML Orchestration (SystemUI RRO)

Once your app's UI is built and responsive, you define how it behaves in the Scalable UI by creating an XML file (e.g., `my_feature_panel.xml`) inside the Runtime Resource Overlay (RRO) package.

### Step 4.1: Define the Panel Type
```xml
<!-- The role must map exactly to the component name of your Activity -->
<TaskPanel 
    id="my_feature_panel" 
    defaultVariant="@id/opened" 
    role="com.example.myapp/.MyCustomFeatureActivity" 
    displayId="0">
```

### Step 4.2: Define UI States (Variants)
A `Variant` represents a static visual state of your panel at any given moment. You must define all possible states your panel can be in.

```xml
    <!-- Example: The fully opened state -->
    <Variant id="@+id/opened">
        <Layer layer="3"/>
        <Visibility isVisible="true"/>
        <Alpha alpha="1.0"/>
        <Corner radius="48dp"/>
        <Bounds left="16dp" top="84dp" right="409dp" bottom="696dp"/>
    </Variant>

    <!-- Example: Hidden offscreen to the left -->
    <Variant id="@+id/offscreen_left">
        <Layer layer="3"/>
        <Visibility isVisible="true"/> <!-- Kept true so it slides out smoothly -->
        <Alpha alpha="1.0"/>
        <Corner radius="48dp"/>
        <Bounds left="-364dp" top="84dp" right="-16dp" bottom="696dp"/>
    </Variant>
```

### Step 4.3: Define Transitions (The State Machine)
Transitions dictate how the panel moves from one `Variant` to another when a system event occurs. 

> [!WARNING]
> **Strict State Paths**: Always define the `fromVariant` explicitly. If you omit `fromVariant`, the orchestrator treats it as a "generic" transition, meaning *any* matching event will forcefully apply this rule, causing overlapping UI bugs.

```xml
    <Transitions>
        <!-- CORRECT: Strict State Path -->
        <Transition 
            onEvent="show_page2" 
            fromVariant="@id/opened" 
            toVariant="@id/offscreen_left" 
            duration="300" 
            interpolator="@android:anim/accelerate_decelerate_interpolator"/>
    </Transitions>
</TaskPanel>
```

## 5. Deployment

1. Add your panel to `scalable_ui_layout.xml` via `<Include layout="@xml/my_feature_panel"/>`.
2. Build your updated RRO package: `m MultiPanelLandscapeRRO`
3. Push the APK: `adb push MultiPanelLandscapeRRO.apk /system/product/overlay/`
4. Restart System UI to parse the new rules: `adb shell stop && adb shell start`
