/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Stores enrolled driver profiles. Demo enrollments come from RRO string-array;
 * runtime enrollments (from GestureDetection-style registration) persist via SharedPreferences.
 */

package com.android.systemui.car.wm.scalableui.panel.controller.facelogin;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class FaceIdentityStore {
    private static final String TAG = "FaceIdentityStore";
    private static final String PREFS = "face_login_enrollments";
    private static final String KEY_COUNT = "count";

    private final List<DriverProfile> mProfiles = new ArrayList<>();

    public void load(Context context, String rroPackage) {
        mProfiles.clear();
        loadFromRro(context, rroPackage);
        loadFromPrefs(context);
        Log.i(TAG, "Loaded " + mProfiles.size() + " enrolled profiles");
    }

    public List<DriverProfile> getProfiles() {
        return Collections.unmodifiableList(mProfiles);
    }

    public DriverProfile findByName(String name) {
        if (name == null) return null;
        for (DriverProfile p : mProfiles) {
            if (name.equalsIgnoreCase(p.displayName)) return p;
        }
        return null;
    }

    public void enroll(Context context, DriverProfile profile) {
        // Replace same name if present
        mProfiles.removeIf(p -> p.displayName.equalsIgnoreCase(profile.displayName));
        mProfiles.add(profile);
        persist(context);
    }

    /**
     * Match GestureDetection-style landmark ratio signatures.
     * Threshold mirrors GestureDetection (diff &lt; 0.25).
     */
    public DriverProfile matchSignature(float[] signature, float maxDiff) {
        if (signature == null || signature.length == 0) return null;
        DriverProfile best = null;
        float bestDiff = Float.MAX_VALUE;
        for (DriverProfile p : mProfiles) {
            if (p.faceSignature == null || p.faceSignature.length != signature.length) continue;
            float diff = 0f;
            for (int i = 0; i < signature.length; i++) {
                diff += Math.abs(signature[i] - p.faceSignature[i]);
            }
            if (diff < maxDiff && diff < bestDiff) {
                bestDiff = diff;
                best = p;
            }
        }
        return best;
    }

    private void loadFromRro(Context context, String rroPackage) {
        try {
            Context rro = context.createPackageContext(rroPackage, 0);
            int id = rro.getResources().getIdentifier(
                    "face_login_enrolled_profiles", "array", rroPackage);
            if (id == 0) return;
            String[] rows = rro.getResources().getStringArray(id);
            for (String row : rows) {
                DriverProfile p = parseRow(row);
                if (p != null) mProfiles.add(p);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "RRO package missing: " + rroPackage);
        } catch (Exception e) {
            Log.w(TAG, "Failed loading RRO enrollments", e);
        }
    }

    /** Format: name;androidUserId;hvacC;seatPos;acOn */
    private static DriverProfile parseRow(String row) {
        if (row == null || row.trim().isEmpty()) return null;
        String[] parts = row.split(";");
        if (parts.length < 5) return null;
        try {
            return new DriverProfile(
                    parts[0].trim(),
                    Integer.parseInt(parts[1].trim()),
                    Float.parseFloat(parts[2].trim()),
                    Integer.parseInt(parts[3].trim()),
                    "1".equals(parts[4].trim()) || Boolean.parseBoolean(parts[4].trim()),
                    null);
        } catch (NumberFormatException e) {
            Log.w(TAG, "Bad enrollment row: " + row);
            return null;
        }
    }

    private void loadFromPrefs(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int count = prefs.getInt(KEY_COUNT, 0);
        for (int i = 0; i < count; i++) {
            String row = prefs.getString("p" + i, null);
            DriverProfile p = parseRow(row);
            if (p != null) {
                mProfiles.removeIf(x -> x.displayName.equalsIgnoreCase(p.displayName));
                mProfiles.add(p);
            }
        }
    }

    private void persist(Context context) {
        SharedPreferences.Editor ed =
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
        ed.putInt(KEY_COUNT, mProfiles.size());
        for (int i = 0; i < mProfiles.size(); i++) {
            DriverProfile p = mProfiles.get(i);
            ed.putString("p" + i, String.format(Locale.US, "%s;%d;%.1f;%d;%d",
                    p.displayName, p.androidUserId, p.hvacTemperatureC, p.seatPosition,
                    p.acOn ? 1 : 0));
        }
        ed.apply();
    }
}
