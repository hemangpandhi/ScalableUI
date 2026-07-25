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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import java.io.PrintWriter;
import java.util.List;

/**
 * Controller for the dynamic footer navigation bar and state-driven media playback panel.
 * Media panel visibility is orchestrated by WM variants in sysui_footer_media_panel.xml —
 * this controller only binds media session controls and nav intents.
 */
public class CarSysuiScalableFooterController extends CarSysuiScalableBarControllerBase {

    private static final String TAG = "CarSysuiFooterCtrl";
    private static final String DEFAULT_RRO_PACKAGE =
            "com.android.systemui.rro.scalableUI.sysuiBars";
    private static final String DEFAULT_MEDIA_PANEL_ID = "sysui_footer_media_panel";
    private static final String DEFAULT_PRIMARY_MEDIA =
            "com.android.car.media/com.android.car.media.MediaDispatcherActivity";

    private final String mPrimaryMediaComponent;
    private final String mMediaPanelId;

    private MediaSessionManager mMediaSessionManager;
    private MediaController mActiveController;
    private boolean mMediaPanelVisible = true;

    // Footer nav
    private ImageButton mNavAllApps;
    private ImageButton mNavRecent;
    private ImageButton mNavPinned;
    private ImageButton mFullscreenToggle;
    private View mFooterUxScrim;

    // Media controls
    private View mMediaRoot;
    private ImageButton mMediaPrev;
    private ImageButton mMediaPlayPause;
    private ImageButton mMediaNext;
    private TextView mMediaTrackName;
    private View mMediaUxScrim;

    private boolean mIsPlaying;

    private final MediaSessionManager.OnActiveSessionsChangedListener mSessionsListener =
            controllers -> mContext.getMainExecutor().execute(
                    () -> bindActiveMediaController(controllers));

    private final MediaController.Callback mMediaCallback = new MediaController.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            mContext.getMainExecutor().execute(() -> updatePlaybackUi(state));
        }

        @Override
        public void onMetadataChanged(android.media.MediaMetadata metadata) {
            mContext.getMainExecutor().execute(() -> updateTrackTitle(metadata));
        }
    };

    public CarSysuiScalableFooterController(Context context) {
        this(context, "sysui_scalable_footer_panel", DEFAULT_RRO_PACKAGE,
                DEFAULT_PRIMARY_MEDIA, DEFAULT_MEDIA_PANEL_ID);
    }

    public CarSysuiScalableFooterController(Context context, String panelId, String rroPackage,
            String primaryMediaComponent, String mediaPanelId) {
        super(context, panelId, rroPackage);
        mPrimaryMediaComponent = primaryMediaComponent;
        mMediaPanelId = mediaPanelId;
    }

    @Override
    protected String getLayoutResourceName() {
        if ("sysui_footer_media_panel".equals(mPanelId)) {
            return "car_sysui_scalable_footer_media";
        }
        return "car_sysui_scalable_footer";
    }

    @Override
    protected void bindViews(View root) {
        bindFooterNav(root);
        bindMediaControls(root);
        wireClickListeners();
    }

    private void bindFooterNav(View root) {
        mNavAllApps = findButton(root, "sysui_nav_all_apps");
        mNavRecent = findButton(root, "sysui_nav_recent");
        mNavPinned = findButton(root, "sysui_nav_pinned");
        mFullscreenToggle = findButton(root, "sysui_footer_fullscreen_toggle");
        int uxId = resolveRroId("sysui_footer_ux_scrim");
        mFooterUxScrim = uxId != 0 ? root.findViewById(uxId) : null;
    }

    private void bindMediaControls(View root) {
        int rootId = resolveRroId("sysui_footer_media_root");
        mMediaRoot = rootId != 0 ? root.findViewById(rootId) : root;
        mMediaPrev = findButton(root, "sysui_media_prev");
        mMediaPlayPause = findButton(root, "sysui_media_play_pause");
        mMediaNext = findButton(root, "sysui_media_next");
        int trackId = resolveRroId("sysui_media_track_name");
        mMediaTrackName = trackId != 0 ? root.findViewById(trackId) : null;
        int uxId = resolveRroId("sysui_media_ux_scrim");
        mMediaUxScrim = uxId != 0 ? root.findViewById(uxId) : null;
    }

    private ImageButton findButton(View root, String name) {
        int id = resolveRroId(name);
        return id != 0 ? root.findViewById(id) : null;
    }

    private void wireClickListeners() {
        if (mNavAllApps != null) {
            mNavAllApps.setOnClickListener(v -> launchAllApps());
        }
        if (mNavRecent != null) {
            mNavRecent.setOnClickListener(v -> launchRecent());
        }
        if (mNavPinned != null) {
            mNavPinned.setOnClickListener(v -> launchPinned());
        }
        if (mFullscreenToggle != null) {
            mFullscreenToggle.setOnClickListener(v -> {
                com.android.car.scalableui.manager.StateManager.handleEvent(
                        new com.android.car.scalableui.model.Event.Builder("toggle_fullscreen").build());
            });
        }
        if (mMediaPrev != null) {
            mMediaPrev.setOnClickListener(v -> dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS));
        }
        if (mMediaPlayPause != null) {
            mMediaPlayPause.setOnClickListener(v -> togglePlayPause());
        }
        if (mMediaNext != null) {
            mMediaNext.setOnClickListener(v -> dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT));
        }
    }

    @Override
    protected void onControllerAttached() {
        mMediaSessionManager = mContext.getSystemService(MediaSessionManager.class);
        if (mMediaSessionManager != null) {
            try {
                ComponentName listenerComponent = new ComponentName(
                        mContext.getPackageName(),
                        "com.android.systemui.car.wm.scalableui.panel.controller.CarSysuiScalableFooterController");
                mMediaSessionManager.addOnActiveSessionsChangedListener(
                        mSessionsListener, listenerComponent);
                bindActiveMediaController(
                        mMediaSessionManager.getActiveSessions(listenerComponent));
            } catch (SecurityException e) {
                Log.w(TAG, "Media session access denied — using key-event fallback", e);
                bindActiveMediaController(null);
            }
        }
    }

    @Override
    protected void onControllerDetached() {
        unbindMediaController();
        if (mMediaSessionManager != null) {
            try {
                mMediaSessionManager.removeOnActiveSessionsChangedListener(mSessionsListener);
            } catch (Exception e) {
                Log.w(TAG, "Failed to remove sessions listener", e);
            }
        }
    }

    /**
     * Called by the WM orchestrator when the media panel variant changes.
     * Visibility is driven declaratively — this only updates controller telemetry state.
     */
    public void onMediaPanelVisibilityChanged(boolean visible) {
        mMediaPanelVisible = visible;
        Log.d(TAG, "Media panel visibility (WM-driven): " + visible);
    }

    private void bindActiveMediaController(List<MediaController> controllers) {
        unbindMediaController();
        if (controllers == null || controllers.isEmpty()) {
            setDefaultTrackTitle();
            return;
        }
        mActiveController = controllers.get(0);
        mActiveController.registerCallback(mMediaCallback);
        updatePlaybackUi(mActiveController.getPlaybackState());
        updateTrackTitle(mActiveController.getMetadata());
    }

    private void unbindMediaController() {
        if (mActiveController != null) {
            mActiveController.unregisterCallback(mMediaCallback);
            mActiveController = null;
        }
    }

    private void togglePlayPause() {
        if (mFullyRestricted) {
            return;
        }
        if (mActiveController == null) {
            dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY);
            return;
        }
        PlaybackState state = mActiveController.getPlaybackState();
        if (state != null && state.getState() == PlaybackState.STATE_PLAYING) {
            mActiveController.getTransportControls().pause();
        } else {
            mActiveController.getTransportControls().play();
        }
    }

    private void dispatchMediaKey(int keyCode) {
        if (mFullyRestricted) {
            return;
        }
        if (mActiveController != null) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                    mActiveController.getTransportControls().skipToPrevious();
                    return;
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                    mActiveController.getTransportControls().skipToNext();
                    return;
                case KeyEvent.KEYCODE_MEDIA_PLAY:
                    mActiveController.getTransportControls().play();
                    return;
                default:
                    break;
            }
        }
        KeyEvent down = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
        KeyEvent up = new KeyEvent(KeyEvent.ACTION_UP, keyCode);
        mContext.sendOrderedBroadcast(new Intent(Intent.ACTION_MEDIA_BUTTON)
                .putExtra(Intent.EXTRA_KEY_EVENT, down), null);
        mContext.sendOrderedBroadcast(new Intent(Intent.ACTION_MEDIA_BUTTON)
                .putExtra(Intent.EXTRA_KEY_EVENT, up), null);
    }

    private void updatePlaybackUi(PlaybackState state) {
        if (mMediaPlayPause == null) {
            return;
        }
        mIsPlaying = state != null && state.getState() == PlaybackState.STATE_PLAYING;
        int iconRes = resolveRroDrawable(mIsPlaying ? "ic_sysui_media_pause"
                : "ic_sysui_media_play");
        if (iconRes != 0) {
            mMediaPlayPause.setImageResource(iconRes);
        }
    }

    private void updateTrackTitle(android.media.MediaMetadata metadata) {
        if (mMediaTrackName == null) {
            return;
        }
        if (metadata == null) {
            setDefaultTrackTitle();
            return;
        }
        String title = metadata.getString(android.media.MediaMetadata.METADATA_KEY_TITLE);
        if (title == null || title.isEmpty()) {
            title = metadata.getString(android.media.MediaMetadata.METADATA_KEY_DISPLAY_TITLE);
        }
        mMediaTrackName.setText(title != null ? title : getDefaultTrackTitleString());
    }

    private void setDefaultTrackTitle() {
        if (mMediaTrackName != null) {
            mMediaTrackName.setText(getDefaultTrackTitleString());
        }
    }

    private String getDefaultTrackTitleString() {
        String fallback = resolveRroString("sysui_media_not_playing");
        return fallback != null ? fallback : "Not Playing";
    }

    private void launchAllApps() {
        if (mFullyRestricted) {
            return;
        }
        Intent intent = new Intent("com.android.car.carlauncher.ACTION_APP_GRID");
        intent.setPackage("com.android.car.carlauncher");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        mContext.startActivity(intent);
    }

    private void launchRecent() {
        if (mFullyRestricted) {
            return;
        }
        Intent intent = new Intent("com.android.systemui.car.action.SHOW_RECENTS");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.sendBroadcast(intent);
    }

    private void launchPinned() {
        if (mFullyRestricted) {
            return;
        }
        Intent intent = new Intent("com.android.car.carlauncher.ACTION_SHOW_FAVORITES");
        intent.setPackage("com.android.car.carlauncher");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
    }

    @Override
    protected void applyUxRestrictions() {
        setScrimVisible(mFooterUxScrim, mFullyRestricted);
        setScrimVisible(mMediaUxScrim, mFullyRestricted);
        boolean enabled = !mFullyRestricted;
        setViewEnabled(mNavAllApps, enabled);
        setViewEnabled(mNavRecent, enabled);
        setViewEnabled(mNavPinned, enabled);
        setViewEnabled(mMediaPrev, enabled);
        setViewEnabled(mMediaPlayPause, enabled);
        setViewEnabled(mMediaNext, enabled);
    }

    @Override
    protected void applyVhalDisconnectedState() {
        // Footer does not depend on VHAL; no-op.
    }

    @Override
    protected void applyVhalConnectedState() {
        // Footer does not depend on VHAL; no-op.
    }

    @Override
    protected void dumpControllerState(PrintWriter pw) {
        pw.println("  mediaPanelId=" + mMediaPanelId);
        pw.println("  mediaPanelVisible=" + mMediaPanelVisible);
        pw.println("  primaryMediaComponent=" + mPrimaryMediaComponent);
        pw.println("  isPlaying=" + mIsPlaying);
        pw.println("  activeController="
                + (mActiveController != null ? mActiveController.getPackageName() : "none"));
        pw.println("  panelLayout=" + getLayoutResourceName());
        if (mMediaTrackName != null) {
            pw.println("  trackTitle=" + mMediaTrackName.getText());
        }
    }
}
