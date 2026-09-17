package com.diyebure.golia.di;

import android.content.Context;

import com.diyebure.golia.data.local.PreferencesManager;
import com.diyebure.golia.data.remote.api.AuthApiService;
import com.diyebure.golia.data.repository.AuthRepositoryImpl;
import com.diyebure.golia.domain.repository.AuthRepository;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

/**
 * Dagger/Hilt module for repository dependencies.
 */
@Module
@InstallIn(SingletonComponent.class)
public class RepositoryModule {

    @Provides
    @Singleton
    public PreferencesManager providePreferencesManager(@ApplicationContext Context context) {
        return PreferencesManager.getInstance(context);
    }

    @Provides
    @Singleton
    public AuthRepository provideAuthRepository(
            @ApplicationContext Context context,
            AuthApiService authApiService,
            PreferencesManager preferencesManager) {
        return new AuthRepositoryImpl(context, authApiService, preferencesManager);
    }
}