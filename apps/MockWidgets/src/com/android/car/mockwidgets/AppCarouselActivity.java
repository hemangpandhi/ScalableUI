package com.android.car.mockwidgets;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;

public class AppCarouselActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_carousel_activity);

        LinearLayout carouselContainer = findViewById(R.id.carousel_container);
        PackageManager pm = getPackageManager();
        
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = pm.queryIntentActivities(mainIntent, 0);

        LayoutInflater inflater = LayoutInflater.from(this);

        for (ResolveInfo info : apps) {
            String packageName = info.activityInfo.packageName;
            
            View itemView = inflater.inflate(R.layout.app_carousel_item, carouselContainer, false);
            ImageView iconView = itemView.findViewById(R.id.app_icon);
            TextView labelView = itemView.findViewById(R.id.app_label);

            CharSequence label = info.loadLabel(pm);
            Drawable icon = info.loadIcon(pm);

            labelView.setText(label);
            iconView.setImageDrawable(icon);

            itemView.setOnClickListener(v -> {
                Intent launchIntent = pm.getLaunchIntentForPackage(packageName);
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(launchIntent);
                }
            });

            carouselContainer.addView(itemView);
        }
    }
}
