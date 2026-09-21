package com.diyebure.golia.di;

import com.diyebure.golia.di.qualifier.IoExecutor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

/**
 * Provides the shared executors used for background work.
 *
 * <p>Centralising thread pools here (instead of {@code Executors.newFixedThreadPool}
 * scattered across repositories) gives us one place to tune concurrency, and a
 * single seam to swap for a synchronous executor in tests.
 */
@Module
@InstallIn(SingletonComponent.class)
public class ExecutorModule {

    private static final int IO_THREAD_POOL_SIZE = 4;

    @Provides
    @Singleton
    @IoExecutor
    public ExecutorService provideIoExecutor() {
        return Executors.newFixedThreadPool(IO_THREAD_POOL_SIZE);
    }
}
