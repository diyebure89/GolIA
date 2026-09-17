package com.diyebure.golia.util;

/**
 * Application-wide constants for the GolIA app.
 */
public final class Constants {

    private Constants() {
        // Private constructor to prevent instantiation
    }

    // API Configuration
    public static final String API_BASE_URL = "https://api.golia.example.com/";

    // Authentication Keys
    public static final String TOKEN_KEY = "auth_token";
    public static final String REFRESH_TOKEN_KEY = "refresh_token";

    // Session Configuration
    public static final long SESSION_TIMEOUT = 30 * 60 * 1000L; // 30 minutes in milliseconds

    // SharedPreferences Name
    public static final String PREF_NAME = "golia_prefs";

    // Intent Extras
    public static final String EXTRA_USER_ID = "extra_user_id";
    public static final String EXTRA_USER_EMAIL = "extra_user_email";

    // Request Codes
    public static final int REQUEST_LOGIN = 1001;
    public static final int REQUEST_REGISTER = 1002;

    // Animation Durations
    public static final int ANIMATION_SHORT = 200;
    public static final int ANIMATION_MEDIUM = 400;
    public static final int ANIMATION_LONG = 600;

    // Validation Constants
    public static final int MIN_PASSWORD_LENGTH = 6;
    public static final int MIN_USERNAME_LENGTH = 3;
    public static final int MAX_USERNAME_LENGTH = 30;

    // Points Calculation Constants
    public static final int MAX_POINTS = 100;
    public static final double POINTS_HOME_WIN_WEIGHT = 1.0;
    public static final double POINTS_DRAW_WEIGHT = 1.5;
    public static final double POINTS_AWAY_WIN_WEIGHT = 1.2;
}