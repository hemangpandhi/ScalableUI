# Fluidic Precision: Scalable UI Architecture for Android Automotive OS

This repository contains the architecture definitions and Runtime Resource Overlay (RRO) for deploying the next-generation **"Fluidic Precision"** Glassmorphism UI on top of Android Automotive OS (AAOS) via **System Window Orchestration**.

## Core Architectural Shift

Traditionally, AAOS applications relied on **UI Embedding** using `ActivityView` and `TaskView` directly inside the `CarLauncher` Java codebase. This method was tightly coupled, memory-heavy, and prone to Z-ordering conflicts because every widget ran an isolated Android window hierarchy within another process.

This architecture transitions completely to **System Window Orchestration** using the AOSP `car-scalableui-lib`. Instead of embedding tasks, the framework delegates the spatial bounding, Z-ordering, and state transitions of actual Android Tasks directly to the WindowManager via an XML-driven state machine. This dramatically improves performance, decouples the Launcher from the application layout entirely, and enables multi-display orchestration.

## Detailed Architecture & Data Flow

The following diagram details how the AOSP WindowManager Shell interacts with the System Window Orchestrator, and how our custom RRO and code-level controllers drive spatial orchestration for both native apps and our custom `MockWidgets` suite.

```mermaid
graph TD
    %% AOSP Core Systems
    subgraph WindowManager
        WO[WindowOrganizer]
        STO[Shell Task Organizer]
    end

    %% System Window Orchestrator Framework
    subgraph Orchestration ["System Window Orchestration (car-scalable-ui-lib)"]
        TPTC[TaskPanelTransitionCoordinator]
        PCR[PanelConfigReader]
        PanelPool[Panel Pool]
        EventBus[Orchestrator Event Bus]
        PC[Panel Controllers]
        
        PCR -->|Instantiates Decor & Task Panels| PanelPool
        PanelPool -->|Feeds panel bounds| TPTC
        TPTC <-->|Z-Order & Crop commands| WO
        TPTC <-->|Listens for app launches| STO
        EventBus -->|Triggers State Changes| TPTC
        PC <-->|Dynamic Event Dispatch & Routing| EventBus
    end

    %% Our RRO Definitions
    subgraph RRO ["CarSystemUIScalableUIOverlay (Our RRO)"]
        ConfigXML["config.xml (Panel Routing & Auto-Launch)"]
        PanelXMLs["scalable_panel_*.xml (Bounds & Transitions)"]
        
        ConfigXML -.->|Defines Routing| PCR
        PanelXMLs -.->|Defines UI State Machine| PCR
    end

    %% The Visual Output Layers
    subgraph Rendered UI Hierarchy
        DecorLayer["Layer -1: Static 3D Decor Backgrounds"]
        GlassLayer["Layer 1: Glassmorphism Blur Layers"]
        WidgetLayer["Layer 3-6: Orchestrated Widgets (Climate, Smart Home, Agenda)"]
        AppLayer["Layer 10+: Fullscreen App Panels (Maps, Settings)"]
        
        DecorLayer -->|Draws beneath| GlassLayer
        GlassLayer -->|Draws beneath| WidgetLayer
        WidgetLayer -->|Draws beneath| AppLayer
    end

    %% Applications
    subgraph Applications
        LauncherApp[Stub CarLauncher]
        MockWidgets[MockWidgets Package]
        ThirdParty[Native OS Apps: Settings, Maps, Dialer]
    end

    %% App to Layer Mapping
    LauncherApp == "Provides Base Background" ==> DecorLayer
    MockWidgets == "Launched via config_default_activities" ==> WidgetLayer
    ThirdParty == "Triggered via EventBus" ==> AppLayer

    %% State Machine Event Interactions
    EventBus -.->|_System_TaskOpenEvent| AppLayer
    EventBus -.->|_System_OnHomeEvent| AppLayer
```

## The XML Declarative Model

Scalable UI abstracts custom windowing logic into a high-level, config-driven XML model composed of the following core building blocks:

```mermaid
classDiagram
    class Panel {
        +String id
        +Type type (TaskPanel | DecorPanel)
        +List~Variant~ variants
        +List~Transition~ transitions
    }
    class Variant {
        +String id
        +Rect bounds
        +int layer (Z-Order)
        +boolean visibility
        +float cornerRadius
    }
    class Transition {
        +String fromVariant
        +String toVariant
        +String eventTrigger
        +int duration
    }
    Panel "1" *-- "many" Variant : contains
    Panel "1" *-- "many" Transition : defines
```

* **Panels**: The fundamental rectangular containers. 
  * `TaskPanel`: Hosts actual application tasks running in separate processes (e.g., Settings, Maps, MockWidgets).
  * `DecorPanel`: Hosts view-based layouts (e.g., UI shadows, static overlays) running in the System UI process for lower overhead.
* **Variants**: Define specific visual states (bounds, visibility, Z-order layers, alpha, corner radius) for each panel.
* **Transitions**: Define animation paths (duration, interpolators) to move a panel between variants triggered by an Event.

### Panel Transition Lifecycle & State Machine

```mermaid
stateDiagram-v2
    [*] --> Closed : Default state on boot
    
    state Closed {
        direction LR
        c1: visibility = false
    }

    state Opened {
        direction LR
        o1: visibility = true
        o2: bounds = Primary Region
    }

    state Shifted {
        direction LR
        s1: visibility = true
        s2: bounds = Shifted / Stacked
    }

    Closed --> Opened : Event (_System_TaskOpenEvent)
    Opened --> Shifted : Event (e.g., Another panel opens)
    Shifted --> Opened : Event (e.g., Obstructing panel closes)
    Opened --> Closed : Event (_System_OnHomeEvent)
    Shifted --> Closed : Event (_System_OnHomeEvent)
```

---

## Fluidic Precision UI & MockWidgets Integration

With the latest architectural updates, the traditional `CarLauncher` is treated as a **Stub**. All complex widgets have been decoupled into the standalone **`MockWidgets`** package.

### Decoupled Widget Micro-Apps
Instead of monolithic fragments inside the Launcher, each widget is its own standard Android Activity:
* `MockMediaActivity` (Radio / Media Player)
* `AgendaActivity` (Calendar / Schedule)
* `ClimateActivity` (HVAC Control)
* `SmartDeviceActivity` (Smart Home Controls)
* `TimeWidgetActivity` (Clock / Weather)
* *(Note: `DrivingStatsActivity` was decommissioned in Model S-16 updates to maximize UI real estate.)*

### Auto-Launch Orchestration
These independent apps are seamlessly embedded into the UI grid at boot via SystemUI's `config.xml`:
```xml
<string-array name="config_default_activities" translatable="false">
    <item>media_panel;com.android.car.mockwidgets/com.android.car.mockwidgets.MockMediaActivity</item>
    <item>smart_home;com.android.car.mockwidgets/com.android.car.mockwidgets.SmartDeviceActivity</item>
    <!-- Additional widgets mapped to their respective TaskPanels -->
</string-array>
```

### Advanced Role Resolution
To capture system intents (like opening the Settings App via different aliases), panel roles in `strings.xml` utilize `<string-array>` definitions. This ensures the Orchestrator safely traps both explicit and alias intent launches:
```xml
<string-array name="settings_panel_role" translatable="false">
    <item>com.android.car.settings/com.android.car.settings.common.CarSettingActivities$HomepageActivity</item>
    <item>com.android.car.settings/com.android.car.settings.Settings_Launcher_Homepage</item>
</string-array>
```

---

## Deployment & Integration Requirements

Deploying Scalable UI requires configuring critical parameters within AOSP overlays:

1. **Framework Insets (Framework RRO):**
   Set `config_remoteInsetsControllerControlsSystemBars` to `true` so the WindowManager Shell can direct window boundaries.
2. **Orchestrator Enablement (System UI RRO):**
   Set `config_enableScalableUI` to `true` and register custom XML layouts under the `<array name="window_states">` parameter.
3. **Legacy Widget Disable:**
   Clear the `config_homeCardModuleClasses` array in the standard `CarLauncher` to prevent legacy embedding conflicts. The `StubCarLauncher` simply provides the base decor background while System UI handles all widget placement.
