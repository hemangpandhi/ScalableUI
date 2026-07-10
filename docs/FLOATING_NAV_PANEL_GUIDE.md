# Scalable UI: Floating Navigation Panel Architecture Guide

This document details the complete end-to-end architecture of the Floating Navigation Panel, specifically focusing on how the HVAC temperature view is defined, how the controller is created and bound, and how the entire system leverages the official AOSP "Scalable UI" Decor Panel approach.

---

## 1. Defining the View (The RRO Layout)

In the Scalable UI architecture, the visual definition of the panel is entirely decoupled from the base Java code. The HVAC temperature view (and the rest of the navigation bar) is defined inside the **Runtime Resource Overlay (RRO)**.

**File Location:** `/vendor/aospstack/ScalableUI/overlays/MultiPanelLandscapeRRO/res/layout/floating_nav_view.xml`

Instead of defining the layout in the base `CarSystemUI` package, we define it in the OEM's RRO. This layout uses a `RelativeLayout` to perfectly center the Seat Heater and symmetrically balance the HVAC cluster on the left and the Media cluster on the right.

```xml
<!-- Inside floating_nav_view.xml -->
<LinearLayout
    android:id="@+id/hvac_cluster"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:layout_toStartOf="@id/nav_seat_heater"
    android:layout_toEndOf="@id/nav_home"
    android:orientation="horizontal"
    android:gravity="center">

    <!-- 1. HVAC Temperature DOWN Button -->
    <ImageView
        android:id="@+id/nav_hvac_down"
        android:layout_width="80dp"
        android:layout_height="80dp"
        android:src="@drawable/ic_nav_minus"
        android:scaleType="centerInside"
        android:background="?android:attr/selectableItemBackgroundBorderless"
        android:padding="16dp" />

    <!-- 2. HVAC Temperature DISPLAY -->
    <TextView
        android:id="@+id/nav_hvac_temp"
        android:layout_width="100dp"
        android:layout_height="wrap_content"
        android:text="22°"
        android:textSize="36sp"
        android:textColor="@android:color/white"
        android:textStyle="bold"
        android:gravity="center" />

    <!-- 3. HVAC Temperature UP Button -->
    <ImageView
        android:id="@+id/nav_hvac_up"
        android:layout_width="80dp"
        android:layout_height="80dp"
        android:src="@drawable/ic_nav_add"
        android:scaleType="centerInside"
        android:background="?android:attr/selectableItemBackgroundBorderless"
        android:padding="16dp" />
</LinearLayout>
```
*Note: Because this is inside an RRO, the `@+id/nav_hvac_up`, `@+id/nav_hvac_down`, and `@+id/nav_hvac_temp` declarations are not just referencing existing IDs—they dynamically **generate** unique integer resource IDs directly within the RRO's compiled package space during build time.*

---

## 2. Defining the Controller Configuration

To tell the Scalable UI orchestrator that this panel exists and needs a specific Java controller, we define a panel configuration XML.

**File Location:** `/vendor/aospstack/ScalableUI/overlays/MultiPanelLandscapeRRO/res/xml/floating_nav_panel.xml`

This file is critical because it utilizes the **Scalable UI approach**. It defines the window as a `<DecorPanel>`, specifies its physical bounds on the screen, and links it to a controller.

```xml
<!-- Inside floating_nav_panel.xml -->
<DecorPanel
    xmlns:systemui="http://schemas.android.com/apk/res-auto"
    systemui:layer="15">
    
    <!-- Scalable UI Bounds: Sizes and centers the panel without Java code -->
    <Bounds left="432dp" top="920dp" right="1488dp" bottom="1040dp"/>

    <!-- Controller Binding -->
    <Controller controller="@xml/floating_nav_controller" />
</DecorPanel>
```
The `@xml/floating_nav_controller` file simply contains the fully qualified class name of the Java controller: `com.android.systemui.car.wm.scalableui.panel.controller.FloatingNavViewController`.

---

## 3. Creating the Java Controller

The controller acts as the brain of the panel. It is responsible for inflating the RRO layout, finding the views, and binding the logic (like incrementing the HVAC temperature).

**File Location:** `/packages/apps/Car/SystemUI/src/com/android/systemui/car/wm/scalableui/panel/controller/FloatingNavViewController.java`

### Step A: Implementing the Interface
The class implements `DecorPanelController`, meaning it natively plugs into the AOSP Scalable UI framework as a certified Decor Panel. It uses Dagger (`@AssistedInject`) for dependency injection.

```java
public class FloatingNavViewController implements DecorPanelController {
    // ...
}
```

### Step B: Dynamic RRO Inflation
Because the layout exists in the RRO and not the base package, the controller uses `createPackageContext` to extract the RRO's resources and inflate `floating_nav_view.xml`.

```java
String rroPackage = "com.android.systemui.rro.scalableUI.multiPanelLandscape";
Context rroContext = mContext.createPackageContext(rroPackage, 0);
mView = LayoutInflater.from(rroContext).inflate(layoutId, null);
```

### Step C: Binding the HVAC Logic
The controller dynamically searches the inflated RRO view for the HVAC IDs using `getIdentifier()`. Once found, it binds the click listeners to update the local `mHvacTemp` state and modify the `TextView`.

```java
// Dynamically resolve the ID generated by the RRO
int hvacUpId = inflateContext.getResources().getIdentifier("nav_hvac_up", "id", rroPackage);
int hvacTempId = inflateContext.getResources().getIdentifier("nav_hvac_temp", "id", rroPackage);

TextView tempText = view.findViewById(hvacTempId);
view.findViewById(hvacUpId).setOnClickListener(v -> {
    if (mHvacTemp < 30) mHvacTemp++;
    tempText.setText(mHvacTemp + "°"); // Updates the UI
});
```
*(Note: In production, this would dispatch intents via `CarPropertyManager` to control the physical vehicle hardware).*

---

## 4. Utilizing the Scalable UI Approach

This entire architecture perfectly aligns with Google's recommended standards for Android Automotive **Scalable UI**:

1. **Decor Panel Isolation:** By defining the window as a `DecorPanel` in XML, the System Window Orchestrator guarantees that this navigation bar floats safely over apps (TaskViews) on layer 15, preventing Z-fighting and UI glitches during app transitions.
2. **Code-Free Geometry:** The physical size and position of the navigation bar (1056dp width, centered) is handled entirely by the `<Bounds>` tag in XML. The Java code does zero layout calculations.
3. **Extreme OEM Customization:** An OEM can completely redesign the navigation bar, remove the HVAC controls, or add new buttons solely by providing a new RRO `floating_nav_view.xml`. The base SystemUI Java code remains completely untouched and safely ignores missing buttons.
