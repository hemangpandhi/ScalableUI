package com.android.car.mockwidgets;

import android.app.Activity;
import android.content.ClipData;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class DrivingStatsActivity extends Activity {
    private static final String TAG = "DrivingStatsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.driving_stats_widget);

    }
}
