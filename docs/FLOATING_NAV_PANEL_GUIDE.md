# Scalable UI: Floating Navigation Panel Architecture Guide

This document is a comprehensive, step-by-step guide detailing how to implement a custom Floating Navigation Panel using the **Zero-Compile Binding** approach. 

This architecture allows OEMs to define custom layouts, add new buttons, and change graphics entirely within a Runtime Resource Overlay (RRO), **without ever modifying the base CarSystemUI Java code.**

---

## Step 1: Define the Custom Layout (The OEM RRO)

The OEM defines the visual appearance and generates new IDs dynamically in the Runtime Resource Overlay.

**File Location:** `/vendor/aospstack/ScalableUI/overlays/MultiPanelLandscapeRRO/res/layout/floating_nav_view.xml`

```xml
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:gravity="center">

    <!-- Notice we use @+id/ to dynamically generate the ID in the RRO! -->
    <ImageView
        android:id="@+id/nav_home"
        android:layout_width="80dp"
        android:layout_height="80dp"
        android:src="@drawable/ic_custom_home_icon" />
        
    <!-- You can add new buttons here, and you don't need to touch Java! -->
    <ImageView
        android:id="@+id/nav_custom_feature"
        android:layout_width="80dp"
        android:layout_height="80dp"
        android:src="@drawable/ic_custom_feature" />
</LinearLayout>
```
*Why this matters:* By using `@+id/`, the OEM doesn't have to wait for the base system developers to add static IDs to `ids.xml`. The IDs are generated natively inside the RRO's package namespace.

---

## Step 2: Implement the Dynamic Controller Logic (Base CarSystemUI)

The base Java Controller is written *once* by the platform team. It uses dynamic inflation and reflection to find the OEM's buttons.

**File Location:** `/packages/apps/Car/SystemUI/src/com/android/systemui/car/wm/scalableui/panel/controller/FloatingNavViewController.java`

```java
public class FloatingNavViewController implements DecorPanelController {
    
    // ...

    @Override
    public View getView() {
        if (mView == null) {
            // 1. We dynamically create a context for the RRO package!
            String rroPackage = "com.android.systemui.rro.scalableUI.multiPanelLandscape";
            Context rroContext = mContext.createPackageContext(rroPackage, 0);
            
            // 2. We inflate the RRO's layout directly.
            mView = LayoutInflater.from(rroContext).inflate(layoutId, null);
            
            // 3. We dynamically search for the OEM's IDs.
            int homeBtnId = rroContext.getResources().getIdentifier("nav_home", "id", rroPackage);
            if (homeBtnId != 0) {
                mView.findViewById(homeBtnId).setOnClickListener(v -> { /* Launch Home Intent */ });
            }
        }
        return mView;
    }
}
```

---

## Step 3: Configure the Strict Schema Variants (The OEM RRO)

This is the most critical step for AOSP compatibility. The panel configuration must strictly use the `<Variant>` schema structures. Properties like `<Bounds>` and `<Layer>` **cannot** sit at the root level.

**File Location:** `/vendor/aospstack/ScalableUI/overlays/MultiPanelLandscapeRRO/res/xml/floating_nav_panel.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<!-- 1. You MUST use systemui: attributes for the root DecorPanel tag -->
<DecorPanel 
    xmlns:systemui="http://schemas.android.com/apk/res-auto" 
    systemui:id="floating_nav_panel" 
    systemui:role="@string/floating_nav_role" 
    systemui:controller="@xml/floating_nav_controller" 
    systemui:defaultVariant="@id/visible" 
    systemui:displayId="0">
    
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

## Step 4: Register the Panel (The OEM RRO)

Finally, tell the orchestrator which Controller handles this panel, and which Layout it should draw on boot.

**File Location:** `/vendor/aospstack/ScalableUI/overlays/MultiPanelLandscapeRRO/res/values/config.xml`

1.  **Register the View:**
    ```xml
    <array name="config_default_activities">
        <item>floating_nav_panel;@layout/floating_nav_view</item>
    </array>
    ```

2.  **Register the Controller and Variants:**
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

> [!TIP]
> By strictly separating the `systemui:role` and `<Variant>` schema definitions in XML from the dynamic `createPackageContext` layout inflation in Java, you guarantee 100% compatibility with the AOSP Window Orchestrator while maintaining complete Zero-Compile agility for your OEM designers.
