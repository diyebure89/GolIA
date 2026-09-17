package com.diyebure.golia;

import android.app.Application;

import dagger.hilt.android.HiltAndroidApp;

/**
 * Main Application class for GolIA
 * Initialize app-wide dependencies and configurations
 */
@HiltAndroidApp
public class GolIAApplication extends Application {

    private static GolIAApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        initializeApp();
    }

    /**
     * Initialize application-wide components
     */
    private void initializeApp() {
        // Hilt initialization is handled by @HiltAndroidApp annotation
    }

    /**
     * Get application instance
     */
    public static GolIAApplication getInstance() {
        return instance;
    }
}