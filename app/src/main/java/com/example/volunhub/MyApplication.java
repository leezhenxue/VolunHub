package com.example.volunhub;

import android.app.Application;
import com.cloudinary.android.MediaManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Runs once when the app starts to initialize global libraries like Cloudinary for image storage.
 */
public class MyApplication extends Application {

    /**
     * Initializes the Cloudinary configuration with the specific cloud name.
     * This ensures image uploading works throughout the app.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", "dw1ccoqrq");
        MediaManager.init(this, config);
    }
}