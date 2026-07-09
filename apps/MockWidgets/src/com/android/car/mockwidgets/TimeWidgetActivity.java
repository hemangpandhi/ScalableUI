package com.android.car.mockwidgets;

import android.app.Activity;
import android.content.ClipData;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class TimeWidgetActivity extends Activity {
    private static final String TAG = "TimeWidgetActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.time_widget);

    }
}
