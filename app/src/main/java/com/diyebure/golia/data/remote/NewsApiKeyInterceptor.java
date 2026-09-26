package com.diyebure.golia.data.remote;

import androidx.annotation.NonNull;

import com.diyebure.golia.BuildConfig;

import java.io.IOException;

import javax.inject.Inject;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Interceptor that adds the NewsData.io authentication key to every outgoing
 * request as a query parameter.
 *
 * <p>Unlike {@link ApiKeyInterceptor} (which attaches an
 * {@code x-apisports-key} header for API-Football), NewsData.io expects the key
 * as an {@code apikey} query parameter appended to the request URL. The value is
 * populated from {@link BuildConfig#NEWSDATA_API_KEY}, read from
 * {@code local.properties} at build time (see task 2). Registering this
 * interceptor on the {@code OkHttpClient} used by the NewsData.io
 * {@code Retrofit} instance ensures the key is attached transparently to all
 * news requests without exposing it at any call site.
 *
 * <p>Requirements: 5.3, 5.4.
 */
public class NewsApiKeyInterceptor implements Interceptor {

    private static final String QUERY_API_KEY = "apikey";

    @Inject
    public NewsApiKeyInterceptor() {
        // No dependencies: the key is resolved from BuildConfig at request time.
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        HttpUrl url = chain.request().url().newBuilder()
                .addQueryParameter(QUERY_API_KEY, BuildConfig.NEWSDATA_API_KEY)
                .build();
        Request request = chain.request().newBuilder()
                .url(url)
                .build();
        return chain.proceed(request);
    }
}
