/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Applies per-user cabin preferences to VHAL after successful face login.
 */

package com.android.systemui.car.wm.scalableui.panel.controller.facelogin;

import android.car.VehicleAreaSeat;
import android.car.VehiclePropertyIds;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.android.systemui.car.wm.scalableui.panel.controller.vhal.CarVhalSubscriptionHelper;

public final class UserPreferenceRestorer {
    private static final String TAG = "UserPreferenceRestorer";
    public static final String ACTION_PREFS_APPLIED =
            "com.android.systemui.car.facelogin.PREFS_APPLIED";

    private static final int AREA_DRIVER = VehicleAreaSeat.SEAT_ROW_1_LEFT;

    private final Context mContext;
    private final CarVhalSubscriptionHelper mVhal;

    public UserPreferenceRestorer(Context context, CarVhalSubscriptionHelper vhal) {
        mContext = context.getApplicationContext();
        mVhal = vhal;
    }

    public void apply(DriverProfile profile) {
        if (profile == null) return;
        Log.i(TAG, "Applying prefs for " + profile.displayName
                + " hvac=" + profile.hvacTemperatureC
                + " seat=" + profile.seatPosition
                + " ac=" + profile.acOn);

        boolean hvacOk = mVhal.setFloatProperty(
                VehiclePropertyIds.HVAC_TEMPERATURE_SET, AREA_DRIVER, profile.hvacTemperatureC);
        boolean seatOk = mVhal.setIntProperty(
                VehiclePropertyIds.HVAC_SEAT_TEMPERATURE, AREA_DRIVER, profile.seatPosition);
        // HVAC_AC_ON is boolean on most platforms — notified via broadcast for mock widgets
        boolean acOk = false;

        if (!hvacOk && !seatOk) {
            Log.w(TAG, "VHAL unavailable — broadcasting preference intent for mocks");
        }

        Intent i = new Intent(ACTION_PREFS_APPLIED);
        i.putExtra("driver", profile.displayName);
        i.putExtra("hvac_c", profile.hvacTemperatureC);
        i.putExtra("seat", profile.seatPosition);
        i.putExtra("ac_on", profile.acOn);
        i.putExtra("hvac_ok", hvacOk);
        i.putExtra("seat_ok", seatOk);
        i.putExtra("ac_ok", acOk);
        mContext.sendBroadcast(i);
    }
}
