package com.diyebure.golia.data.remote.interceptor;

import android.content.Context;

import androidx.annotation.NonNull;

import com.diyebure.golia.data.local.PreferencesManager;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Interceptor that adds Authorization header to requests.
 * Automatically includes JWT access token when available.
 */
public class AuthInterceptor implements Interceptor {

    private final PreferencesManager preferencesManager;

    public AuthInterceptor(Context context) {
        this.preferencesManager = PreferencesManager.getInstance(context);
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request originalRequest = chain.request();

        // Skip adding auth header for login/register/refresh endpoints
        String path = originalRequest.url().encodedPath();
        if (path.contains("/auth/login") || 
            path.contains("/auth/register") || 
            path.contains("/auth/refresh")) {
            return chain.proceed(originalRequest);
        }

        // Get authorization header from preferences
        String authorizationHeader = preferencesManager.getAuthorizationHeader();
        if (authorizationHeader == null || authorizationHeader.isEmpty()) {
            return chain.proceed(originalRequest);
        }

        // Add authorization header to request
        Request.Builder requestBuilder = originalRequest.newBuilder()
                .header("Authorization", authorizationHeader);

        return chain.proceed(requestBuilder.build());
    }
}