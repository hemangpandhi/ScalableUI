# OEM Scalable UI demonstration — add to device/<oem>/<product>/device.mk
#
# $(call inherit-product, vendor/aospstack/ScalableUI/oem_demo_packages.mk)

# Tip demo (prebuilt-safe) — recommended default
PRODUCT_PACKAGES += \
    CarSystemUI \
    CarLauncher \
    MockWidgets \
    MultiPanelLandscapeRRO \
    CarLauncherMultiPanelRRO \
    CarSystemUIScalableUIOverlay \
    privapp-permissions-mockwidgets.xml

# Unified OEM overlay package (tip panels by default; Pleos optional)
PRODUCT_PACKAGES += \
    OemDemoRRO \
    CarSysuiScalableBarControllers

# Optional Pleos-only bars overlay (when Controllers linked into CarSystemUI)
