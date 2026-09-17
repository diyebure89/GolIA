package com.diyebure.golia.data.remote.interceptor;

import android.util.Log;

import androidx.annotation.NonNull;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;

/**
 * HTTP logging interceptor for development and debugging.
 * Logs request and response details including headers and body.
 */
public class LoggingInterceptor implements Interceptor {

    private static final String TAG = "HTTP";

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request request = chain.request();
        long startTime = System.nanoTime();

        // Log request details
        Log.d(TAG, String.format("Sending request %s on %s%n%s",
                request.url(), chain.connection(), request.headers()));

        if (request.body() != null) {
            // For logging request body, we'd need to buffer it
            // For now, just note that there's a body
            Log.d(TAG, "Request has body");
        }

        // Proceed with request
        Response response = chain.proceed(request);
        long endTime = System.nanoTime();

        // Log response details
        Log.d(TAG, String.format("Received response for %s in %.1fms%n%s",
                response.request().url(), (endTime - startTime) / 1e6d, response.headers()));

        // Clone response to read body without consuming it
        ResponseBody responseBody = response.body();
        String responseBodyString = responseBody != null ? responseBody.string() : null;

        // Log response body (truncate for large responses)
        if (responseBodyString != null) {
            if (responseBodyString.length() > 1000) {
                Log.d(TAG, "Response body (truncated): " + responseBodyString.substring(0, 1000) + "...");
            } else {
                Log.d(TAG, "Response body: " + responseBodyString);
            }
        }

        // Recreate response since body was consumed
        ResponseBody clonedResponseBody = responseBodyString != null 
                ? ResponseBody.create(responseBody.contentType(), responseBodyString) 
                : null;
                
        return response.newBuilder()
                .body(clonedResponseBody)
                .build();
    }
}