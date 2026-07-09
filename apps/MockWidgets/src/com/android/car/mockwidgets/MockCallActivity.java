package com.android.car.mockwidgets;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.DragEvent;
import android.view.View;
import android.widget.TextView;

public class MockCallActivity extends Activity {
    private static final String TAG = "MockCallActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mock_call_activity);

        // Setup Drag Handle

        // Setup Drop Target on Root View
        View rootView = findViewById(R.id.root_view);
        rootView.setOnDragListener((v, event) -> {
            if (event.getAction() == DragEvent.ACTION_DROP) {
                ClipData.Item item = event.getClipData().getItemAt(0);
                String sourcePanel = item.getText().toString();
                Log.d(TAG, "Dropped " + sourcePanel + " onto climate_panel");
                if ("media_panel".equals(sourcePanel)) {
                    sendFocusBroadcast("event_focus_media");
                } else if ("map_panel".equals(sourcePanel)) {
                    sendFocusBroadcast("event_focus_navigation");
                }
            }
            return true;
        });

        // Focus Button
        findViewById(R.id.btn_focus).setOnClickListener(v -> {
            sendFocusBroadcast("event_focus_climate");
        });

        // Hangup Button finishes activity or resets status
        findViewById(R.id.btn_hangup).setOnClickListener(v -> {
            TextView callerStatus = findViewById(R.id.call_status);
            callerStatus.setText("Call Ended");
        });
    }

    private void sendFocusBroadcast(String eventId) {
        Log.d(TAG, "Broadcasting event trigger: " + eventId);
        Intent intent = new Intent("com.android.systemui.car.wm.scalableui.TRIGGER_EVENT");
        intent.putExtra("event_id", eventId);
        sendBroadcast(intent);
    }
}
