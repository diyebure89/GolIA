package com.diyebure.golia.di;

import com.diyebure.golia.data.local.PreferencesManager;
import com.diyebure.golia.data.remote.ApiKeyInterceptor;
import com.diyebure.golia.data.remote.api.AuthApiService;
import com.diyebure.golia.data.remote.api.FootballApiService;
import com.diyebure.golia.data.remote.interceptor.AuthInterceptor;
import com.diyebure.golia.data.remote.interceptor.LoggingInterceptor;

import java.util.concurrent.TimeUnit;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Dagger/Hilt module for network dependencies.
 * Provides Retrofit instances and API services for the application.
 *
 * <p>The app talks to two distinct backends, so it maintains two independent
 * {@link OkHttpClient}/{@link Retrofit} stacks, kept apart with {@code @Named}
 * qualifiers:
 * <ul>
 *   <li>{@link #GOLIA_BACKEND}: the GolIA auth backend
 *       ({@value #GOLIA_BASE_URL}) with {@link AuthInterceptor}.</li>
 *   <li>{@link #FOOTBALL_BACKEND}: API-Football / API-SPORTS
 *       ({@value #FOOTBALL_BASE_URL}) with {@link ApiKeyInterceptor}, which
 *       attaches the {@code x-apisports-key} header to every fixtures request
 *       (Requirements 1.2, 2.1).</li>
 * </ul>
 * Splitting the clients keeps the API-Football key from ever being sent to the
 * GolIA backend and lets each service target its own base URL.
 */
@Module
@InstallIn(SingletonComponent.class)
public class NetworkModule {

    /** Qualifier for the GolIA auth backend OkHttp/Retrofit stack. */
    private static final String GOLIA_BACKEND = "golia";

    /** Qualifier for the API-Football (API-SPORTS) OkHttp/Retrofit stack. */
    private static final String FOOTBALL_BACKEND = "football";

    private static final String GOLIA_BASE_URL = "https://api.golia.app/v1/";
    private static final String FOOTBALL_BASE_URL = "https://v3.football.api-sports.io/";
    private static final long CONNECT_TIMEOUT_SECONDS = 30;
    private static final long READ_TIMEOUT_SECONDS = 30;
    private static final long WRITE_TIMEOUT_SECONDS = 30;

    @Provides
    @Singleton
    public HttpLoggingInterceptor provideHttpLoggingInterceptor() {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        return interceptor;
    }

    @Provides
    @Singleton
    public AuthInterceptor provideAuthInterceptor(PreferencesManager preferencesManager) {
        return new AuthInterceptor(preferencesManager);
    }

    @Provides
    @Singleton
    public LoggingInterceptor provideLoggingInterceptor() {
        return new LoggingInterceptor();
    }

    // --- GolIA auth backend stack ---

    @Provides
    @Singleton
    @Named(GOLIA_BACKEND)
    public OkHttpClient provideGoliaOkHttpClient(
            HttpLoggingInterceptor httpLoggingInterceptor,
            AuthInterceptor authInterceptor,
            LoggingInterceptor loggingInterceptor) {

        return new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .addInterceptor(httpLoggingInterceptor)
                .addInterceptor(authInterceptor)
                .addInterceptor(loggingInterceptor)
                .build();
    }

    @Provides
    @Singleton
    @Named(GOLIA_BACKEND)
    public Retrofit provideGoliaRetrofit(@Named(GOLIA_BACKEND) OkHttpClient okHttpClient) {
        return new Retrofit.Builder()
                .baseUrl(GOLIA_BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    @Provides
    @Singleton
    public AuthApiService provideAuthApiService(@Named(GOLIA_BACKEND) Retrofit retrofit) {
        return retrofit.create(AuthApiService.class);
    }

    // --- API-Football (API-SPORTS) stack ---

    @Provides
    @Singleton
    @Named(FOOTBALL_BACKEND)
    public OkHttpClient provideFootballOkHttpClient(
            HttpLoggingInterceptor httpLoggingInterceptor,
            ApiKeyInterceptor apiKeyInterceptor,
            LoggingInterceptor loggingInterceptor) {

        return new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .addInterceptor(httpLoggingInterceptor)
                .addInterceptor(apiKeyInterceptor)
                .addInterceptor(loggingInterceptor)
                .build();
    }

    @Provides
    @Singleton
    @Named(FOOTBALL_BACKEND)
    public Retrofit provideFootballRetrofit(@Named(FOOTBALL_BACKEND) OkHttpClient okHttpClient) {
        return new Retrofit.Builder()
                .baseUrl(FOOTBALL_BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    @Provides
    @Singleton
    public FootballApiService provideFootballApiService(@Named(FOOTBALL_BACKEND) Retrofit retrofit) {
        return retrofit.create(FootballApiService.class);
    }
}