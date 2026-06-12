package com.android.car.mockwidgets;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.DragEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

public class MockMediaActivity extends Activity {
    private static final String TAG = "MockMediaActivity";
    private boolean mIsPlaying = true;
    private int mProgress = 42;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mock_media_activity);

        // Setup Drag Handle

        // Setup Drop Target on Root View
        View rootView = findViewById(R.id.root_view);
        rootView.setOnDragListener((v, event) -> {
            if (event.getAction() == DragEvent.ACTION_DROP) {
                ClipData.Item item = event.getClipData().getItemAt(0);
                String sourcePanel = item.getText().toString();
                Log.d(TAG, "Dropped " + sourcePanel + " onto media_panel");
                if ("climate_panel".equals(sourcePanel)) {
                    sendFocusBroadcast("event_focus_climate");
                } else if ("map_panel".equals(sourcePanel)) {
                    sendFocusBroadcast("event_focus_navigation");
                }
            }
            return true;
        });

        // Focus Button
        findViewById(R.id.btn_focus).setOnClickListener(v -> {
            sendFocusBroadcast("event_focus_media");
        });

        // Media Controls
        ImageButton btnPlay = findViewById(R.id.btn_play);
        btnPlay.setOnClickListener(v -> {
            mIsPlaying = !mIsPlaying;
            if (mIsPlaying) {
                btnPlay.setImageResource(android.R.drawable.ic_media_pause);
            } else {
                btnPlay.setImageResource(android.R.drawable.ic_media_play);
            }
        });

        // Playback Progress Simulation
        SeekBar seekbar = findViewById(R.id.playback_progress);
        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mProgress = progress;
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void sendFocusBroadcast(String eventId) {
        Log.d(TAG, "Broadcasting event trigger: " + eventId);
        Intent intent = new Intent("com.android.systemui.car.wm.scalableui.TRIGGER_EVENT");
        intent.putExtra("event_id", eventId);
        sendBroadcast(intent);
    }
}
