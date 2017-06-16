package com.viatom.smartbp.dfu;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.viatom.smartbp.MainActivity;

/**
 * Created by wangjiang on 2017/6/9.
 */

public class NotificationActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isTaskRoot()) {
//            final Intent parentIntent = new Intent(this, FeaturesActivity.class);
//            parentIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            final Intent startAppIntent = new Intent(this, MainActivity.class);
            startAppIntent.putExtras(getIntent().getExtras());
//            startActivities(new Intent[]{ parentIntent, startAppIntent});
            startActivities(new Intent[]{ startAppIntent});
        }

        finish();
    }
}
