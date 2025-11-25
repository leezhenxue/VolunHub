package com.example.volunhub;

import android.app.Application;
import com.cloudinary.android.MediaManager;

import java.util.HashMap;
import java.util.Map;

/**
 * The global Application class for VolunHub.
 * This class runs once when the app process is created, before any Activity.
 * It is responsible for initializing global libraries like Cloudinary (used to store profile image).
 */
public class MyApplication extends Application {

    /**
     * Called when the application is starting, before any other application objects have been created.
     * Initializes the Cloudinary MediaManager with the app's configuration.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", "dw1ccoqrq");
        MediaManager.init(this, config);
    }
}