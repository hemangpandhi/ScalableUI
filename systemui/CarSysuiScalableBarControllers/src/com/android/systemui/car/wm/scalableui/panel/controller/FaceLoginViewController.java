/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Scalable UI Face Login DecorPanel controller.
 *
 * Flow (car start / OnHome):
 *  1. Show oval frame (scanning glow)
 *  2. Match face against enrolled profiles (demo engine or MediaPipe hook)
 *  3. MATCH → neon blue glow → UserManager switch → restore HVAC/seat prefs → dismiss
 *  4. NO MATCH → neon orange glow → "not registered" → Switch to Guest?
 *
 * Compatible with GestureDetection landmark-signature enrollments when provided.
 */

package com.android.systemui.car.wm.scalableui.panel.controller;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Outline;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.systemui.car.wm.scalableui.panel.controller.facelogin.CarUserSwitchHelper;
import com.android.systemui.car.wm.scalableui.panel.controller.facelogin.DriverProfile;
import com.android.systemui.car.wm.scalableui.panel.controller.facelogin.FaceIdentityStore;
import com.android.systemui.car.wm.scalableui.panel.controller.facelogin.FaceMatchResult;
import com.android.systemui.car.wm.scalableui.panel.controller.facelogin.UserPreferenceRestorer;
import com.android.systemui.car.wm.scalableui.panel.controller.vhal.CarVhalSubscriptionHelper;

import java.io.PrintWriter;

public class FaceLoginViewController implements ScalableDecorPanelController, SysuiDumpable {

    private static final String TAG = "FaceLoginCtrl";
    private static final String DEFAULT_RRO =
            "com.android.systemui.rro.scalableUI.multiPanelLandscape";
    private static final String TRIGGER =
            "com.android.systemui.car.wm.scalableui.TRIGGER_EVENT";

    private final Context mContext;
    private final String mPanelId;
    private final String mRroPackage;
    private final Handler mMain = new Handler(Looper.getMainLooper());

    private Context mRroContext;
    private View mView;
    private boolean mAttached;
    private String mLastState = "idle";

    private TextureView mPreview;
    private ImageView mPlaceholder;
    private View mGlowFrame;
    private TextView mStatus;
    private TextView mTitle;
    private Button mBtnGuest;
    private Button mBtnRetry;

    private final FaceIdentityStore mStore = new FaceIdentityStore();
    private final CarVhalSubscriptionHelper mVhal = new CarVhalSubscriptionHelper();
    private CarUserSwitchHelper mUserSwitch;
    private UserPreferenceRestorer mPrefs;

    private boolean mDemoMatchSuccess = true;
    private int mDemoScanMs = 2500;
    private int mGuestUserId = 0;

    public FaceLoginViewController(Context context) {
        this(context, "face_login_panel", DEFAULT_RRO);
    }

    public FaceLoginViewController(Context context, String panelId, String rroPackage) {
        mContext = context;
        mPanelId = panelId;
        mRroPackage = rroPackage;
    }

    @Override
    public String getPanelId() {
        return mPanelId;
    }

    @Override
    public View getView() {
        if (mView == null) {
            mRroContext = resolveRroContext();
            int layoutId = mRroContext.getResources().getIdentifier(
                    "face_login_view", "layout", mRroContext.getPackageName());
            if (layoutId == 0) {
                Log.e(TAG, "face_login_view layout missing in RRO");
                mView = new View(mContext);
            } else {
                mView = LayoutInflater.from(mRroContext).inflate(layoutId, null);
                bindViews(mView);
                applyOvalClip(mPreview);
                applyOvalClip(mPlaceholder);
                applyOvalClip(mGlowFrame);
            }
        }
        return mView;
    }

    @Override
    public void onPanelAttached() {
        if (mAttached) return;
        mAttached = true;
        mUserSwitch = new CarUserSwitchHelper(mContext);
        mVhal.start(mContext);
        mPrefs = new UserPreferenceRestorer(mContext, mVhal);
        mStore.load(mContext, mRroPackage);
        loadDemoConfig();
        startScan();
    }

    @Override
    public void onPanelDetached() {
        if (!mAttached) return;
        mAttached = false;
        mMain.removeCallbacksAndMessages(null);
        mVhal.stop();
    }

    private void bindViews(View root) {
        mPreview = find(root, "face_login_preview");
        mPlaceholder = find(root, "face_login_placeholder");
        mGlowFrame = find(root, "face_login_glow_frame");
        mStatus = find(root, "face_login_status");
        mTitle = find(root, "face_login_title");
        mBtnGuest = find(root, "face_login_btn_guest");
        mBtnRetry = find(root, "face_login_btn_retry");

        if (mBtnGuest != null) {
            mBtnGuest.setOnClickListener(v -> onGuestSelected());
        }
        if (mBtnRetry != null) {
            mBtnRetry.setOnClickListener(v -> {
                fireEvent("face_login_retry");
                startScan();
            });
        }
    }

    private void loadDemoConfig() {
        if (mRroContext == null) mRroContext = resolveRroContext();
        String pkg = mRroContext.getPackageName();
        int msId = mRroContext.getResources().getIdentifier(
                "face_login_demo_scan_ms", "integer", pkg);
        if (msId != 0) mDemoScanMs = mRroContext.getResources().getInteger(msId);
        int guestId = mRroContext.getResources().getIdentifier(
                "face_login_guest_user_id", "integer", pkg);
        if (guestId != 0) mGuestUserId = mRroContext.getResources().getInteger(guestId);
        int boolId = mRroContext.getResources().getIdentifier(
                "face_login_demo_match_success", "bool", pkg);
        if (boolId != 0) mDemoMatchSuccess = mRroContext.getResources().getBoolean(boolId);
    }

    private void startScan() {
        mLastState = "scanning";
        setGlow("face_login_oval_glow_scanning");
        setStatus(rroString("face_login_scanning", "Looking for a registered driver…"));
        setGuestVisible(false);
        setRetryVisible(false);
        if (mPlaceholder != null) mPlaceholder.setVisibility(View.VISIBLE);

        // Production hook: replace with MediaPipe / GestureDetection face pipeline.
        // Demo path: resolve after delay (Cuttlefish often has no camera).
        mMain.postDelayed(this::resolveDemoIdentity, mDemoScanMs);
    }

    /**
     * Demo identity resolution. When GestureDetection / MediaPipe is linked,
     * call {@link #onFaceMatch(FaceMatchResult)} from the vision callback instead.
     */
    private void resolveDemoIdentity() {
        if (!mAttached) return;
        if (mDemoMatchSuccess && !mStore.getProfiles().isEmpty()) {
            onFaceMatch(FaceMatchResult.matched(mStore.getProfiles().get(0)));
        } else {
            onFaceMatch(FaceMatchResult.unregistered(
                    rroString("face_login_failed", "You are not a registered user.")));
        }
    }

    /** Entry point for real face engines (GestureDetection landmark match, etc.). */
    public void onFaceMatch(FaceMatchResult result) {
        if (!mAttached || result == null) return;
        mMain.post(() -> handleMatch(result));
    }

    private void handleMatch(FaceMatchResult result) {
        if (result.status == FaceMatchResult.Status.MATCHED && result.profile != null) {
            mLastState = "success";
            setGlow("face_login_oval_glow_blue");
            String welcome = rroString("face_login_success_format", "Welcome, %1$s");
            setStatus(String.format(welcome, result.profile.displayName));
            setGuestVisible(false);
            setRetryVisible(false);
            fireEvent("face_login_success");

            mUserSwitch.switchToProfile(result.profile);
            setStatus(rroString("face_login_applying_prefs", "Applying your cabin preferences…"));
            mPrefs.apply(result.profile);

            mMain.postDelayed(() -> {
                fireEvent("face_login_dismiss");
                mLastState = "hidden";
            }, 1200);
        } else {
            mLastState = "failed";
            setGlow("face_login_oval_glow_orange");
            setStatus(result.message != null ? result.message
                    : rroString("face_login_failed", "You are not a registered user."));
            setGuestVisible(true);
            setRetryVisible(true);
            fireEvent("face_login_failed");
        }
    }

    private void onGuestSelected() {
        mLastState = "guest";
        String guestName = rroString("face_login_guest_name", "Guest");
        mUserSwitch.switchToGuest(mGuestUserId, guestName);
        // Guest uses default cabin prefs — no enrolled profile restore
        fireEvent("face_login_dismiss");
        mLastState = "hidden";
    }

    private void setGlow(String drawableName) {
        if (mGlowFrame == null || mRroContext == null) return;
        int id = mRroContext.getResources().getIdentifier(
                drawableName, "drawable", mRroContext.getPackageName());
        if (id != 0) mGlowFrame.setBackgroundResource(id);
    }

    private void setStatus(String text) {
        if (mStatus != null) mStatus.setText(text);
    }

    private void setGuestVisible(boolean visible) {
        if (mBtnGuest != null) mBtnGuest.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void setRetryVisible(boolean visible) {
        if (mBtnRetry != null) mBtnRetry.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void fireEvent(String eventId) {
        Intent intent = new Intent(TRIGGER);
        intent.putExtra("event_id", eventId);
        mContext.sendBroadcast(intent);
        Log.d(TAG, "TRIGGER_EVENT " + eventId);
    }

    private void applyOvalClip(View view) {
        if (view == null) return;
        view.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View v, Outline outline) {
                outline.setOval(0, 0, v.getWidth(), v.getHeight());
            }
        });
        view.setClipToOutline(true);
        view.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> v.invalidateOutline());
    }

    @SuppressWarnings("unchecked")
    private <T extends View> T find(View root, String name) {
        int id = mRroContext.getResources().getIdentifier(name, "id", mRroContext.getPackageName());
        return id != 0 ? (T) root.findViewById(id) : null;
    }

    private String rroString(String name, String fallback) {
        if (mRroContext == null) return fallback;
        int id = mRroContext.getResources().getIdentifier(
                name, "string", mRroContext.getPackageName());
        return id != 0 ? mRroContext.getString(id) : fallback;
    }

    private Context resolveRroContext() {
        try {
            return mContext.createPackageContext(mRroPackage, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "RRO missing: " + mRroPackage, e);
            return mContext;
        }
    }

    @Override
    public void dump(PrintWriter pw, String[] args) {
        pw.println("FaceLoginViewController{panel=" + mPanelId
                + " state=" + mLastState
                + " enrolled=" + mStore.getProfiles().size()
                + " demoMatch=" + mDemoMatchSuccess + "}");
        for (DriverProfile p : mStore.getProfiles()) {
            pw.println("  profile=" + p.displayName + " userId=" + p.androidUserId
                    + " hvac=" + p.hvacTemperatureC + " seat=" + p.seatPosition);
        }
    }
}
