/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.android.systemui.car.wm.scalableui.panel.controller;

import android.content.Context;

/**
 * Factory for OEM scalable system bar controllers.
 * Wire into the scalable UI panel controller registry in CarSystemUI Dagger.
 */
public final class CarSysuiScalableBarControllerFactory {

    private CarSysuiScalableBarControllerFactory() {}

    public static ScalableDecorPanelController create(Context context, String panelId) {
        if (panelId == null) {
            return null;
        }
        switch (panelId) {
            case "sysui_scalable_header_panel":
            case "sysui_driver_zone_panel":
            case "sysui_passenger_zone_panel":
                return new CarSysuiScalableHeaderController(context, panelId,
                        "com.android.systemui.rro.scalableUI.oemDemo");
            case "sysui_scalable_footer_panel":
            case "sysui_footer_media_panel":
                return new CarSysuiScalableFooterController(context, panelId,
                        "com.android.systemui.rro.scalableUI.oemDemo",
                        "com.android.car.mockwidgets/com.android.car.mockwidgets.MockMediaActivity",
                        "sysui_footer_media_panel");
            case "face_login_panel":
                return new FaceLoginViewController(context, panelId,
                        "com.android.systemui.rro.scalableUI.multiPanelLandscape");
            default:
                return null;
        }
    }
}
