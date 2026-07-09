package com.android.car.mockwidgets;

import android.app.Activity;
import android.content.ClipData;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class SmartDeviceActivity extends Activity {
    private static final String TAG = "SmartDeviceActivity";
    private boolean mGarageClosed = true;
    private boolean mLightsOff = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.smart_device_widget);


        View btnGarage = findViewById(R.id.btn_garage);
        TextView badgeGarage = findViewById(R.id.badge_garage);
        btnGarage.setOnClickListener(v -> {
            mGarageClosed = !mGarageClosed;
            if (mGarageClosed) {
                badgeGarage.setText("CLOSED");
                badgeGarage.setTextColor(0xFFFFB300);
                badgeGarage.setBackgroundColor(0x33FFB300); // Brighter Amber
            } else {
                badgeGarage.setText("OPEN");
                badgeGarage.setTextColor(0xFF00E676);
                badgeGarage.setBackgroundColor(0x3300E676); // Neon Green
            }
        });

        View btnLights = findViewById(R.id.btn_lights);
        TextView badgeLights = findViewById(R.id.badge_lights);
        btnLights.setOnClickListener(v -> {
            mLightsOff = !mLightsOff;
            if (mLightsOff) {
                badgeLights.setText("OFF");
                badgeLights.setTextColor(0xFF9E9E9E);
                badgeLights.setBackgroundColor(0x339E9E9E);
            } else {
                badgeLights.setText("ON");
                badgeLights.setTextColor(0xFF18FFFF);
                badgeLights.setBackgroundColor(0x3318FFFF); // Neon Cyan
            }
        });
    }
}
