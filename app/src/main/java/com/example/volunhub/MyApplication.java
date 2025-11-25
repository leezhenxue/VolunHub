package com.example.volunhub;

import android.app.Application;
import com.cloudinary.android.MediaManager;

import java.util.HashMap;
import java.util.Map;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Map<String, String> config = new HashMap<>();
        //noinspection SpellCheckingInspection
        config.put("cloud_name", "dw1ccoqrq");
        MediaManager.init(this, config);
    }
}