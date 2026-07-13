/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.car.wm.scalableui.panel.controller;

import android.car.drivingstate.CarUxRestrictions;
import android.car.drivingstate.CarUxRestrictionsManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import com.android.systemui.car.wm.scalableui.panel.controller.vhal.CarVhalSubscriptionHelper;

import java.io.PrintWriter;

/**
 * Shared base for Pleos-style scalable system bar controllers.
 * Provides RRO dynamic binding, VHAL lifecycle, DDG UX restrictions, and Dumpable telemetry.
 */
public abstract class CarSysuiScalableBarControllerBase implements ScalableDecorPanelController,
        SysuiDumpable, CarVhalSubscriptionHelper.VhalConnectionListener,
        CarUxRestrictionsManager.OnUxRestrictionsChangedListener {

    private static final String TAG = "CarSysuiScalableBar";

    protected final Context mContext;
    protected final String mPanelId;
    protected final String mRroPackage;

    protected Context mRroContext;
    protected View mView;
    protected boolean mAttached;

    protected final CarVhalSubscriptionHelper mVhalHelper = new CarVhalSubscriptionHelper();
    protected CarUxRestrictionsManager mUxRestrictionsManager;
    protected boolean mFullyRestricted;
    protected boolean mVhalConnected;

    protected CarSysuiScalableBarControllerBase(Context context, String panelId,
            String rroPackage) {
        mContext = context;
        mPanelId = panelId;
        mRroPackage = rroPackage;
    }

  @Override
    public String getPanelId() {
        return mPanelId;
    }

    @Override
    public void onPanelAttached() {
        if (mAttached) {
            return;
        }
        mAttached = true;
        mVhalHelper.addConnectionListener(this);
        mVhalHelper.start(mContext);
        registerUxRestrictions();
        onControllerAttached();
    }

    @Override
    public void onPanelDetached() {
        if (!mAttached) {
            return;
        }
        mAttached = false;
        mVhalHelper.removeConnectionListener(this);
        mVhalHelper.stop();
        unregisterUxRestrictions();
        onControllerDetached();
    }

    /** Subclass hook after lifecycle attach. */
    protected abstract void onControllerAttached();

    /** Subclass hook before lifecycle detach. */
    protected abstract void onControllerDetached();

    /** Subclass layout resource name in the RRO package (without extension). */
    protected abstract String getLayoutResourceName();

    @Override
    public View getView() {
        if (mView == null) {
            mRroContext = resolveRroContext();
            int layoutId = mRroContext.getResources().getIdentifier(
                    getLayoutResourceName(), "layout", mRroContext.getPackageName());
            if (layoutId == 0) {
                Log.e(TAG, "Layout not found in RRO: " + getLayoutResourceName());
                mView = new View(mContext);
            } else {
                mView = LayoutInflater.from(mRroContext).inflate(layoutId, null);
                bindViews(mView);
            }
        }
        return mView;
    }

    /** Bind RRO view IDs to VHAL / media / nav logic. */
    protected abstract void bindViews(View root);

    /** Resolve a view ID from the RRO package by resource name. Returns 0 if absent. */
    protected int resolveRroId(String name) {
        if (mRroContext == null) {
            mRroContext = resolveRroContext();
        }
        String pkg = mRroContext.getPackageName();
        return mRroContext.getResources().getIdentifier(name, "id", pkg);
    }

    /** Resolve a string resource from the RRO package. */
    protected String resolveRroString(String name) {
        if (mRroContext == null) {
            mRroContext = resolveRroContext();
        }
        String pkg = mRroContext.getPackageName();
        int resId = mRroContext.getResources().getIdentifier(name, "string", pkg);
        if (resId == 0) {
            return null;
        }
        return mRroContext.getString(resId);
    }

    /** Resolve a drawable resource from the RRO package. */
    protected int resolveRroDrawable(String name) {
        if (mRroContext == null) {
            mRroContext = resolveRroContext();
        }
        String pkg = mRroContext.getPackageName();
        return mRroContext.getResources().getIdentifier(name, "drawable", pkg);
    }

    private Context resolveRroContext() {
        for (String pkg : RRO_PACKAGE_FALLBACKS) {
            try {
                Context ctx = mContext.createPackageContext(pkg, 0);
                if (ctx.getResources().getIdentifier(
                        getLayoutResourceName(), "layout", pkg) != 0) {
                    return ctx;
                }
            } catch (PackageManager.NameNotFoundException ignored) {
                // try next overlay package
            }
        }
        Log.e(TAG, "No RRO package found for layout: " + getLayoutResourceName());
        return mContext;
    }

    private static final String[] RRO_PACKAGE_FALLBACKS = {
            "com.android.systemui.rro.scalableUI.oemDemo",
            "com.android.systemui.rro.scalableUI.sysuiBars",
            "com.android.systemui.rro.scalableUI.multiPanelLandscape",
    };

    private void registerUxRestrictions() {
        try {
            android.car.Car car = android.car.Car.createCar(mContext);
            if (car == null) {
                return;
            }
            mUxRestrictionsManager = (CarUxRestrictionsManager) car.getCarManager(
                    android.car.Car.CAR_UX_RESTRICTION_SERVICE);
            if (mUxRestrictionsManager != null) {
                mUxRestrictionsManager.registerListener(this);
                onUxRestrictionsChanged(mUxRestrictionsManager.getCurrentCarUxRestrictions());
            }
        } catch (Exception e) {
            Log.w(TAG, "UX restrictions unavailable", e);
        }
    }

    private void unregisterUxRestrictions() {
        if (mUxRestrictionsManager != null) {
            try {
                mUxRestrictionsManager.unregisterListener(this);
            } catch (Exception e) {
                Log.w(TAG, "Failed to unregister UX listener", e);
            }
            mUxRestrictionsManager = null;
        }
    }

    @Override
    public void onUxRestrictionsChanged(CarUxRestrictions restrictions) {
        boolean fullyRestricted = restrictions != null
                && restrictions.isRequiresDistractionOptimization()
                && (restrictions.getActiveRestrictions()
                        & CarUxRestrictions.UX_RESTRICTIONS_FULLY_RESTRICTED) != 0;
        if (fullyRestricted != mFullyRestricted) {
            mFullyRestricted = fullyRestricted;
            mContext.getMainExecutor().execute(this::applyUxRestrictions);
        }
    }

    /** Apply DDG scrim and disable complex touch targets when fully restricted. */
    protected abstract void applyUxRestrictions();

    /** Apply grayed-out state when VHAL disconnects. */
    protected abstract void applyVhalDisconnectedState();

    /** Apply enabled state when VHAL reconnects. */
    protected abstract void applyVhalConnectedState();

    @Override
    public void onVhalConnected() {
        mVhalConnected = true;
        mContext.getMainExecutor().execute(this::applyVhalConnectedState);
    }

    @Override
    public void onVhalDisconnected() {
        mVhalConnected = false;
        mContext.getMainExecutor().execute(this::applyVhalDisconnectedState);
    }

    protected void setViewEnabled(View view, boolean enabled) {
        if (view == null) {
            return;
        }
        view.setEnabled(enabled);
        view.setAlpha(enabled ? 1.0f : 0.4f);
    }

    protected void setScrimVisible(View scrim, boolean visible) {
        if (scrim == null) {
            return;
        }
        scrim.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @Override
    public void dump(PrintWriter pw, String[] args) {
        pw.println("CarSysuiScalableBarController{panelId=" + mPanelId
                + ", rroPackage=" + mRroPackage
                + ", attached=" + mAttached
                + ", vhalConnected=" + mVhalConnected
                + ", fullyRestricted=" + mFullyRestricted + "}");
        dumpControllerState(pw);
    }

    /** Subclass telemetry for dumpsys activity service SystemUI. */
    protected abstract void dumpControllerState(PrintWriter pw);
}
