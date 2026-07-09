package com.android.car.mockwidgets;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.TextView;

public class CameraActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView view = new TextView(this);
        view.setText("PARKING CAMERA FEED");
        view.setTextColor(Color.WHITE);
        view.setTextSize(48);
        view.setGravity(Gravity.CENTER);
        view.setBackgroundColor(Color.DKGRAY);
        setContentView(view);
    }
}
