/*
 * Copyright (C) 2026 The Android Open Source Project
 */

package com.android.systemui.car.wm.scalableui.panel.controller.facelogin;

/** Per-driver cabin preferences restored after face login. */
public final class DriverProfile {
    public final String displayName;
    public final int androidUserId;
    public final float hvacTemperatureC;
    public final int seatPosition; // 0..3 heater / position level
    public final boolean acOn;
    /** Optional landmark-ratio signature (GestureDetection-compatible). */
    public final float[] faceSignature;

    public DriverProfile(String displayName, int androidUserId, float hvacTemperatureC,
            int seatPosition, boolean acOn, float[] faceSignature) {
        this.displayName = displayName;
        this.androidUserId = androidUserId;
        this.hvacTemperatureC = hvacTemperatureC;
        this.seatPosition = seatPosition;
        this.acOn = acOn;
        this.faceSignature = faceSignature;
    }
}
