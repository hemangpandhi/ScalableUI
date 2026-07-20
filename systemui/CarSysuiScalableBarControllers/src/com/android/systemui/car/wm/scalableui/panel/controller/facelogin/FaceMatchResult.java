/*
 * Copyright (C) 2026 The Android Open Source Project
 */

package com.android.systemui.car.wm.scalableui.panel.controller.facelogin;

/** Outcome of a face recognition attempt. */
public final class FaceMatchResult {
    public enum Status { MATCHED, UNREGISTERED, ERROR }

    public final Status status;
    public final DriverProfile profile; // non-null when MATCHED
    public final String message;

    private FaceMatchResult(Status status, DriverProfile profile, String message) {
        this.status = status;
        this.profile = profile;
        this.message = message;
    }

    public static FaceMatchResult matched(DriverProfile profile) {
        return new FaceMatchResult(Status.MATCHED, profile, profile.displayName);
    }

    public static FaceMatchResult unregistered(String message) {
        return new FaceMatchResult(Status.UNREGISTERED, null, message);
    }

    public static FaceMatchResult error(String message) {
        return new FaceMatchResult(Status.ERROR, null, message);
    }
}
