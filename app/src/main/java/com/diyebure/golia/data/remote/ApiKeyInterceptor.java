package com.diyebure.golia.data.remote;

import androidx.annotation.NonNull;

import com.diyebure.golia.BuildConfig;

import java.io.IOException;

import javax.inject.Inject;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Interceptor that adds the API-Football authentication header to every outgoing
 * request.
 *
 * <p>The header {@code x-apisports-key} is populated from
 * {@link BuildConfig#API_FOOTBALL_KEY}, which is read from {@code local.properties}
 * at build time (see task 1.1). Registering this interceptor on the
 * {@code OkHttpClient} used by the API-Football {@code Retrofit} instance ensures
 * the key is attached transparently to all fixture requests.
 *
 * <p>Requirements: 1.2, 2.1.
 */
public class ApiKeyInterceptor implements Interceptor {

    private static final String HEADER_API_KEY = "x-apisports-key";

    @Inject
    public ApiKeyInterceptor() {
        // No dependencies: the key is resolved from BuildConfig at request time.
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request request = chain.request().newBuilder()
                .header(HEADER_API_KEY, BuildConfig.API_FOOTBALL_KEY)
                .build();
        return chain.proceed(request);
    }
}
