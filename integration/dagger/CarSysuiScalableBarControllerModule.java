/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Drop-in Dagger module for packages/apps/Car/SystemUI.
 *
 * Integration steps:
 * 1. Copy this file next to PanelControllerModule.java (or merge bindings).
 * 2. Add CarSysuiScalableBarControllers to CarSystemUI static_libs.
 * 3. Include this module in the SystemUI Dagger component that installs
 *    PanelControllerModule.
 * 4. Rebuild CarSystemUI, then enable OemDemo window_states_pleos arrays.
 */

package com.android.systemui.car.wm.scalableui.panel.controller;

import android.content.Context;

import com.android.systemui.car.wm.scalableui.panel.controller.CarSysuiScalableBarControllerFactory;
import com.android.systemui.car.wm.scalableui.panel.controller.ScalableDecorPanelController;

import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import dagger.multibindings.StringKey;

/**
 * Registers Pleos scalable header/footer controllers with the panel controller map.
 * Panel IDs must match OemDemoRRO / CarSysuiScalableBarRRO XML {@code id} attributes.
 *
 * NOTE: Exact multibinding types may need adjustment to match the AOSP
 * {@code PanelControllerModule} signature in your tree (Map&lt;String, Provider&lt;...&gt;&gt;).
 */
@Module
public abstract class CarSysuiScalableBarControllerModule {

    public static final String HEADER_PANEL = "sysui_scalable_header_panel";
    public static final String DRIVER_ZONE = "sysui_driver_zone_panel";
    public static final String PASSENGER_ZONE = "sysui_passenger_zone_panel";
    public static final String FOOTER_PANEL = "sysui_scalable_footer_panel";
    public static final String FOOTER_MEDIA = "sysui_footer_media_panel";
    public static final String FACE_LOGIN = "face_login_panel";

    @Provides
    @IntoMap
    @StringKey(FACE_LOGIN)
    static ScalableDecorPanelController provideFaceLogin(Context context) {
        return CarSysuiScalableBarControllerFactory.create(context, FACE_LOGIN);
    }

    @Provides
    @IntoMap
    @StringKey(HEADER_PANEL)
    static ScalableDecorPanelController provideHeader(Context context) {
        return CarSysuiScalableBarControllerFactory.create(context, HEADER_PANEL);
    }

    @Provides
    @IntoMap
    @StringKey(DRIVER_ZONE)
    static ScalableDecorPanelController provideDriverZone(Context context) {
        return CarSysuiScalableBarControllerFactory.create(context, DRIVER_ZONE);
    }

    @Provides
    @IntoMap
    @StringKey(PASSENGER_ZONE)
    static ScalableDecorPanelController providePassengerZone(Context context) {
        return CarSysuiScalableBarControllerFactory.create(context, PASSENGER_ZONE);
    }

    @Provides
    @IntoMap
    @StringKey(FOOTER_PANEL)
    static ScalableDecorPanelController provideFooter(Context context) {
        return CarSysuiScalableBarControllerFactory.create(context, FOOTER_PANEL);
    }

    @Provides
    @IntoMap
    @StringKey(FOOTER_MEDIA)
    static ScalableDecorPanelController provideFooterMedia(Context context) {
        return CarSysuiScalableBarControllerFactory.create(context, FOOTER_MEDIA);
    }
}
