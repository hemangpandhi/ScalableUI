/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Bridges face match → Android multi-user switch.
 * Production: CarUserManager / UserManager#switchUser (privileged SystemUI).
 * Demo: logs + broadcasts when permission is unavailable (Cuttlefish / non-priv).
 */

package com.android.systemui.car.wm.scalableui.panel.controller.facelogin;

import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;

public final class CarUserSwitchHelper {
    private static final String TAG = "CarUserSwitchHelper";
    public static final String ACTION_FACE_USER_SWITCHED =
            "com.android.systemui.car.facelogin.USER_SWITCHED";

    private final Context mContext;
    private final UserManager mUserManager;

    public CarUserSwitchHelper(Context context) {
        mContext = context.getApplicationContext();
        mUserManager = (UserManager) mContext.getSystemService(Context.USER_SERVICE);
    }

    /**
     * Switch to the Android user associated with the matched driver profile.
     * @return true if switch was requested successfully (or demo broadcast sent)
     */
    public boolean switchToProfile(DriverProfile profile) {
        if (profile == null) return false;
        return switchToUserId(profile.androidUserId, profile.displayName);
    }

    public boolean switchToGuest(int guestUserId, String guestName) {
        return switchToUserId(guestUserId, guestName != null ? guestName : "Guest");
    }

    private boolean switchToUserId(int userId, String displayName) {
        Log.i(TAG, "Request user switch → id=" + userId + " name=" + displayName);
        boolean switched = false;
        try {
            if (mUserManager != null) {
                // SystemUI / privileged path
                // Note: UserManager.switchUser(int) is not a public API.
                // In a real OEM build, use CarUserManager.switchUser() here.
                switched = false; // Demo path always
            }
        } catch (SecurityException e) {
            Log.w(TAG, "switchUser not permitted — emitting demo broadcast", e);
        } catch (Exception e) {
            Log.e(TAG, "switchUser failed", e);
        }

        Intent i = new Intent(ACTION_FACE_USER_SWITCHED);
        i.putExtra("user_id", userId);
        i.putExtra("display_name", displayName);
        i.putExtra("switched", switched);
        mContext.sendBroadcast(i);
        return switched || true; // demo path always continues to prefs
    }

    public int getCurrentUserId() {
        return UserHandle.myUserId();
    }
}
