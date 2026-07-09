# Scalable UI Architecture: Developer Guidelines & FAQ

This document serves as the canonical reference for developing, maintaining, and extending the multi-panel Scalable UI framework built on top of Android Automotive OS (AAOS) 17.

---

## 1. Google Automotive UI Guidelines Reference
When modifying bounds or designing new widgets for this Scalable UI, you must adhere strictly to Google's Automotive Design Guidelines to ensure driver safety and usability.

### Key Principles:
- **Touch Target Sizes:** Every interactive element MUST have a minimum touch target size of **48dp x 48dp** (or 64dp for critical elements like navigation). This ensures ease of use while driving.
- **Glanceability:** The UI must be designed so the driver can parse information in under 1.5 seconds.
- **Contrast Ratios:** Ensure at least a **4.5:1** contrast ratio for text against background panels. The 24dp grid spacing ensures panels do not bleed into each other, maintaining visual distinction.
- **Cognitive Load Minimization:** Do not present more than 3-4 primary pieces of information simultaneously. Our 4-column grid naturally enforces this constraint.

> [!TIP]
> **Reference:** For more details, consult the official [Android Automotive App Quality Guidelines](https://developer.android.com/cars/app-quality).

---

## 2. The 24dp Symmetric Grid System
To solve the layout fragmentation and overlapping bounds, the entire MultiPanelLandscapeRRO architecture has been unified into a **Mathematically Symmetric 4-Column Grid**.

**Total Canvas Resolution:** 1920dp x 1080dp
**Global Margin & Gutter Size:** 24dp

If you build a new panel, you **must** snap its `left` and `right` bounds to one or more of these columns:

| Column | Left Bound | Right Bound | Width | Primary Use Case |
| :--- | :--- | :--- | :--- | :--- |
| **Col 1** | 24dp | 408dp | 384dp | Media Panel / Agenda (Page 2) |
| **Col 2** | 432dp | 816dp | 384dp | Phone Panel / Climate (Page 2) |
| **Col 3** | 840dp | 1488dp | 648dp | Maps Placeholder / Smart Home / Clock |
| **Col 4** | 1512dp | 1896dp | 384dp | Agenda (Page 1) / Widgets |

### Combining Columns
If an application requires more horizontal space (e.g., the `media_source_panel` Radio App or `app_panel`), it can span multiple columns. 
- *Example:* A panel spanning Columns 1 and 2 will have bounds: `left=24dp`, `right=816dp`.

> [!WARNING]
> **Do not invent arbitrary boundaries.** Boundaries like `17dp`, `98dp`, or `591dp` will break the glassmorphism overlapping logic and cause orphaned shadows.

---

## 3. Z-Ordering (Layers)
Panels overlap and slide behind one another using the `layer` attribute defined in the XML.

- **Layer 3-5:** Background Carousels & Base Panels (e.g., Mini Media)
- **Layer 6-11:** Primary Widgets (Phone, Maps, Climate)
- **Layer 12-14:** Decor Shadows (MUST sit exactly one layer below their parent panel)
- **Layer 15+:** Full-screen applications (`app_panel`, `media_source_panel`)

---

## 4. Frequently Asked Questions (FAQ)

### Q: Why do shadows float randomly on the screen?
**A:** Decor shadows (like `phone_shadow` and `settings_shadow`) are separate `DecorPanel` entities from their parent `TaskPanel`. If you add a transition to a `TaskPanel` (e.g., making the Phone Panel slide off-screen to `-816dp` when the Radio App opens), you **MUST** mirror that exact transition in the corresponding shadow XML file. If you forget, the panel will move but the shadow will stay behind.

### Q: How do I add a new widget to Page 2?
**A:**
1. Determine which column(s) the widget will occupy.
2. Create an XML file in `/res/xml/`.
3. Set its `defaultVariant` to a base bounds located **off-screen to the right** (e.g., `left=1944dp right=2328dp`).
4. Add a `<Transition onEvent="show_page2">` that triggers a variant moving the widget to its target column.
5. Register the widget in `/res/values/config.xml` under `config_default_activities`.

### Q: Why did the layout look broken when the Radio App was open?
**A:** The Radio App (`media_source_panel`) triggers a `media_stacked` transition for the Phone and Media panels. Previously, the `media_stacked` coordinates were hardcoded to arbitrary values (like `98dp` and `591dp`), which completely ignored the symmetric grid. All `stacked` and `page2_opened` sub-variants have now been mathematically mapped to the 24dp grid.

### Q: Can I build an interactive widget (e.g., an HVAC control with +/- buttons) entirely via an RRO?
**A:** No, not entirely. An RRO can define the visual layout (`@layout/my_hvac`), the Panel bounds in XML, and the drawables. However, an RRO **cannot** contain executable Java logic to attach `OnClickListeners` or read vehicle properties.
To build an interactive widget:
1. **(Recommended)** Build a lightweight Android App/Service. Bind it to the UI by registering the Panel ID to the App's component name in `config_default_activities`. The System Window Orchestrator will animate the app inside the panel.
2. **(Alternative)** Build a custom Java controller extending `PanelOverlayController` directly inside the `CarSystemUI` source code, and reference it via the `controller` attribute in your RRO's `<DecorPanel>` XML.

### Q: How does SystemUI resolve `findViewById()` at compile-time if the layout XML is injected via RRO at runtime?
**A:** This is a classic RRO architecture challenge. The SystemUI compiler cannot see IDs defined only in an RRO. There are two solutions:
1. **Pre-define IDs in SystemUI (Best Practice):** Declare empty IDs (e.g., `<item type="id" name="my_custom_button" />`) in `CarSystemUI`'s base `res/values/ids.xml`. The SystemUI Java code can then safely use `findViewById(R.id.my_custom_button)`. In your RRO layout, assign the button to this pre-defined ID using the `@*` syntax (`android:id="@*com.android.systemui:id/my_custom_button"`).
2. **Dynamic Runtime Resolution:** If you don't want to modify SystemUI's base code, define the ID normally in your RRO (`android:id="@+id/my_custom_button"`). In your SystemUI Java Controller, look up the generated integer ID at runtime using `context.getResources().getIdentifier("my_custom_button", "id", "com.android.systemui")`, and then pass that integer to `findViewById()`.
