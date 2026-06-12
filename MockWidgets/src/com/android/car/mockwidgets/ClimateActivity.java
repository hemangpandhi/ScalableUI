package com.android.car.mockwidgets;

import android.app.Activity;
import android.content.ClipData;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

public class ClimateActivity extends Activity {
    private static final String TAG = "ClimateActivity";
    private int mTemp = 72;
    private boolean mAcOn = true;
    private boolean mAutoOn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.climate_widget);


        TextView txtTemp = findViewById(R.id.txt_temperature);
        ImageButton btnDown = findViewById(R.id.btn_temp_down);
        ImageButton btnUp = findViewById(R.id.btn_temp_up);

        btnDown.setOnClickListener(v -> {
            mTemp--;
            txtTemp.setText(mTemp + "°F");
        });

        btnUp.setOnClickListener(v -> {
            mTemp++;
            txtTemp.setText(mTemp + "°F");
        });

        Button btnAc = findViewById(R.id.btn_ac_toggle);
        btnAc.setOnClickListener(v -> {
            mAcOn = !mAcOn;
            btnAc.setText(mAcOn ? "A/C ON" : "A/C OFF");
            btnAc.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    mAcOn ? 0x4DFFFFFF : 0x1AFFFFFF));
        });

        Button btnAuto = findViewById(R.id.btn_auto_toggle);
        btnAuto.setOnClickListener(v -> {
            mAutoOn = !mAutoOn;
            btnAuto.setText(mAutoOn ? "AUTO: ON" : "AUTO");
            btnAuto.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    mAutoOn ? 0x4DFFFFFF : 0x1AFFFFFF));
        });
    }
}
