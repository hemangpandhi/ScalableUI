# Scalable UI: Floating Navigation Panel Architecture Guide

This document is the **comprehensive, production-grade guide** detailing how to implement a custom Floating Navigation Panel using the **Zero-Compile Binding** approach. 

This architecture allows OEMs to define custom layouts, add new buttons, and change graphics entirely within a Runtime Resource Overlay (RRO), **without ever modifying the base CarSystemUI Java code.** Furthermore, it is fully scalable and optimized for Android 15 (API 35).

---

## Step 1: Configure the RRO Manifest (Android 15 / API 35)

To ensure strict compliance with Android 15 and avoid legacy compatibility modes, explicitly target API 35 in your RRO's manifest.

**File Location:** `/vendor/.../overlays/MultiPanelLandscapeRRO/AndroidManifest.xml`

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.android.systemui.rro.scalableUI.multiPanelLandscape">
    
    <!-- Explicitly target Android 15 to enforce modern Overlay Manager behaviors -->
    <uses-sdk android:minSdkVersion="35" android:targetSdkVersion="35" />

    <application android:hasCode="false" />
    <overlay
        android:targetPackage="com.android.systemui"
        android:isStatic="true"
        android:resourcesMap="@xml/overlays"
        android:priority="100" />
</manifest>
```

---

## Step 2: Define the Custom Layout (The OEM RRO)

Define your visual appearance and generate new IDs dynamically using `@+id/`. You do not need to add these to the base `ids.xml`.

**File Location:** `/vendor/.../overlays/MultiPanelLandscapeRRO/res/layout/floating_nav_view.xml`

```xml
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:gravity="center"
    android:orientation="horizontal">

    <!-- 1. Rich Display: Weather -->
    <ImageView
        android:id="@+id/nav_weather_display"
        android:layout_width="120dp"
        android:layout_height="120dp"
        android:src="@drawable/ic_weather_cloudy"
        android:scaleType="centerCrop"
        android:background="@drawable/rounded_album_bg"
        android:clipToOutline="true" />

    <!-- 2. HVAC Cluster -->
    <LinearLayout
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="center">
        <ImageView
            android:id="@+id/nav_hvac_down"
            android:layout_width="80dp"
            android:layout_height="80dp"
            android:src="@drawable/ic_nav_minus" />
            
        <TextView
            android:id="@+id/nav_hvac_temp"
            android:layout_width="100dp"
            android:layout_height="wrap_content"
            android:text="22°" />

        <ImageView
            android:id="@+id/nav_hvac_up"
            android:layout_width="80dp"
            android:layout_height="80dp"
            android:src="@drawable/ic_nav_add" />
    </LinearLayout>

    <!-- 3. Rich Display: Media Album Art -->
    <ImageView
        android:id="@+id/nav_media_album"
        android:layout_width="120dp"
        android:layout_height="120dp"
        android:src="@drawable/album_cover_rhythm"
        android:scaleType="centerCrop"
        android:background="@drawable/rounded_album_bg"
        android:clipToOutline="true" />
</LinearLayout>
```

---

## Step 3: Implement the Dynamic Controller Logic (Base CarSystemUI)

The base Java Controller is written *once* by the platform team. It uses dynamic inflation and reflection to find the OEM's buttons. 

> [!TIP]
> **Scalability Best Practice:** Cache the `createPackageContext` to avoid memory overhead, and always wrap `getIdentifier` in a `!= 0` check to prevent crashes if an OEM removes a button!

**File Location:** `/packages/apps/Car/SystemUI/src/com/android/systemui/car/wm/scalableui/panel/controller/FloatingNavViewController.java`

```java
public class FloatingNavViewController implements DecorPanelController {
    
    private int mHvacTemp = 22;
    // ...

    @Override
    public View getView() {
        if (mView == null) {
            // 1. We dynamically create a context for the RRO package! (Cache this!)
            String rroPackage = "com.android.systemui.rro.scalableUI.multiPanelLandscape";
            Context rroContext = mContext.createPackageContext(rroPackage, 0);
            
            // 2. We inflate the RRO's layout directly.
            mView = LayoutInflater.from(rroContext).inflate(layoutId, null);
            
            // 3. We dynamically search for the OEM's IDs.
            int hvacUpId = rroContext.getResources().getIdentifier("nav_hvac_up", "id", rroPackage);
            int hvacTempId = rroContext.getResources().getIdentifier("nav_hvac_temp", "id", rroPackage);
            
            // 4. Safely bind logic only if the OEM included the buttons in the RRO!
            if (hvacUpId != 0 && hvacTempId != 0) {
                TextView tempText = mView.findViewById(hvacTempId);
                mView.findViewById(hvacUpId).setOnClickListener(v -> { 
                    if (mHvacTemp < 30) mHvacTemp++;
                    tempText.setText(mHvacTemp + "°");
                    // Dispatch to CarPropertyManager to change actual HVAC
                });
            }

            // 5. Safe Resource Handling for Media State (Rich Display)
            // If the OEM forgets to include a pause icon (ic_media_pause), this won't crash!
            int playId = rroContext.getResources().getIdentifier("nav_media_play_pause", "id", rroPackage);
            if (playId != 0) {
                ImageView playBtn = mView.findViewById(playId);
                int pauseIconRes = rroContext.getResources().getIdentifier("ic_media_pause", "drawable", rroPackage);
                if (pauseIconRes != 0) {
                    playBtn.setImageResource(pauseIconRes);
                } else {
                    Log.w("ScalableUI", "Designer forgot ic_media_pause! Safe fallback engaged.");
                }
            }
        }
        return mView;
    }
}
```

---

## Step 4: Configure the Strict Schema Variants (The OEM RRO)

This is the most critical step for AOSP compatibility. The panel configuration must strictly use the `<Variant>` schema structures. Properties like `<Bounds>` and `<Layer>` **cannot** sit at the root level.

> [!WARNING]
> Do **not** prefix the attributes with `systemui:` (e.g., `systemui:id`). Standard AOSP orchestration expects raw attributes (`id="floating_nav_panel"`).

**File Location:** `/vendor/.../overlays/MultiPanelLandscapeRRO/res/xml/floating_nav_panel.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<!-- 1. You MUST use id and role attributes for the root DecorPanel tag -->
<DecorPanel 
    id="floating_nav_panel" 
    role="floating_nav_role" 
    controller="@xml/floating_nav_controller" 
    defaultVariant="@id/visible" 
    displayId="0">
    
    <!-- 2. You MUST define state variants. Properties sit inside these variants. -->
    <Variant id="@+id/hidden">
        <Layer layer="15"/>
        <Visibility isVisible="false"/>
        <!-- 3. You MUST define physical geometry inside a variant -->
        <Bounds left="432dp" top="920dp" right="1488dp" bottom="1040dp"/>
    </Variant>
    
    <Variant id="@+id/visible">
        <Layer layer="15"/>
        <Visibility isVisible="true"/>
        <Bounds left="432dp" top="920dp" right="1488dp" bottom="1040dp"/>
    </Variant>
</DecorPanel>
```

---

## Step 5: Register the Panel (The OEM RRO)

Finally, tell the orchestrator which Controller handles this panel, and which Layout it should draw on boot.

**File Location:** `/vendor/.../overlays/MultiPanelLandscapeRRO/res/values/config.xml`

1.  **Register the View mapping:**
    ```xml
    <array name="config_default_activities">
        <item>floating_nav_panel;@layout/floating_nav_view</item>
    </array>
    ```

2.  **Register the Controller mapping:**
    ```xml
    <array name="window_states">
        <item>@xml/floating_nav_panel</item>
    </array>
    ```

And in `/vendor/.../res/xml/floating_nav_controller.xml`:
```xml
<Controller id="floating_nav_controller">
    <ControllerName>com.android.systemui.car.wm.scalableui.panel.controller.FloatingNavViewController</ControllerName>
</Controller>
```

By following these 5 steps, your OEM designers achieve infinite UI scalability via RROs, perfectly compliant with the Android 15 AOSP System Window Orchestrator.
