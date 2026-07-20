/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Bridge adapter: when AOSP PanelControllerInitializer expects DecorPanelController
 * (or a base interface), wrap ScalableDecorPanelController instances.
 *
 * Copy into CarSystemUI and adjust the implements clause to the real AOSP type:
 *   com.android.systemui.car.wm.scalableui.view.DecorPanelController
 * (or whatever PanelControllerModule binds today).
 */

package com.android.systemui.car.wm.scalableui.panel.controller;

import android.view.View;

import java.io.PrintWriter;

/**
 * Thin adapter so Pleos controllers can be registered beside FloatingNavViewController
 * without forking the orchestrator.
 */
public final class CarSysuiDecorPanelControllerAdapter {

    private final ScalableDecorPanelController mDelegate;

    public CarSysuiDecorPanelControllerAdapter(ScalableDecorPanelController delegate) {
        mDelegate = delegate;
    }

    public View getView() {
        return mDelegate.getView();
    }

    public void onPanelAttached() {
        mDelegate.onPanelAttached();
    }

    public void onPanelDetached() {
        mDelegate.onPanelDetached();
    }

    public String getPanelId() {
        return mDelegate.getPanelId();
    }

    public void dump(PrintWriter pw, String[] args) {
        if (mDelegate instanceof SysuiDumpable) {
            ((SysuiDumpable) mDelegate).dump(pw, args);
        }
    }
}
