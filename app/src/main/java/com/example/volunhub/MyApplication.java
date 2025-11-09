package com.example.volunhub;

import android.app.Application;
import com.cloudinary.android.MediaManager;
import java.util.HashMap;
import java.util.Map;

/**
 * This is the main entry point for the application.
 * It runs ONCE when the app is first launched, before any Activity.
 * This is the perfect place to initialize libraries like Cloudinary.
 */
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // This code will now run ONCE when the app starts,
        // guaranteeing MediaManager is always initialized.
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", "dw1ccoqrq");

        MediaManager.init(this, config);
    }
}