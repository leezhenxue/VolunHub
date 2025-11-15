package com.example.volunhub;

import android.app.Application;
import com.cloudinary.android.MediaManager;

import java.util.HashMap;
import java.util.Map;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // read config from AndroidManifest.xml to find CLOUDINARY_CLOUD_NAME
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", "dw1ccoqrq");
        MediaManager.init(this);
    }
}