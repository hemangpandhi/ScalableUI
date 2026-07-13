# OEM Scalable UI demonstration — add to device/<oem>/<product>/device.mk or car.mk
#
# Include from product makefile:
#   $(call inherit-product, vendor/aospstack/ScalableUI/oem_demo_packages.mk)

PRODUCT_PACKAGES += \
    CarSystemUI \
    CarLauncher \
    MockWidgets \
    OemDemoRRO \
    CarSysuiScalableBarControllers \
    privapp-permissions-mockwidgets.xml

# Optional: legacy overlays (disable when using OemDemoRRO)
# MultiPanelLandscapeRRO \
# CarSysuiScalableBarRRO \
