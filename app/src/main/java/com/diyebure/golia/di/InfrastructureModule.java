package com.diyebure.golia.di;

import com.diyebure.golia.util.LiveRefreshScheduler;
import com.diyebure.golia.util.SearchTextNormalizer;
import com.diyebure.golia.util.TimeRangeCalculator;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

/**
 * Provides the stateless infrastructure utilities used by the presentation and
 * data layers of the matches-live-fixtures feature.
 *
 * <p>Unlike {@code RequestBudgetManager} and the use cases (which declare
 * {@code @Inject} constructors and are therefore built by Hilt automatically),
 * these utilities are plain Java classes with no {@code @Inject} constructor, so
 * they need explicit {@code @Provides} bindings here:
 * <ul>
 *   <li>{@link TimeRangeCalculator}: single source of truth for the temporal
 *       ranges (Rango_Hoy / Rango_Manana / Rango_Esta_Semana).</li>
 *   <li>{@link SearchTextNormalizer}: stateless, accent/case-insensitive search
 *       normalizer.</li>
 *   <li>{@link LiveRefreshScheduler}: main-thread periodic timer for live
 *       polling.</li>
 * </ul>
 *
 * <p>{@code TimeRangeCalculator} and {@code SearchTextNormalizer} are stateless
 * and thread-safe, so a single {@code @Singleton} instance is shared. The
 * {@code LiveRefreshScheduler} holds mutable scheduling state, but it is bound to
 * the main-thread {@code Looper} and driven by a single {@code PartidosViewModel}
 * at a time, so a singleton instance is appropriate here.
 *
 * <p>Requirements: 12.3, 12.5.
 */
@Module
@InstallIn(SingletonComponent.class)
public class InfrastructureModule {

    @Provides
    @Singleton
    public TimeRangeCalculator provideTimeRangeCalculator() {
        return new TimeRangeCalculator();
    }

    @Provides
    @Singleton
    public SearchTextNormalizer provideSearchTextNormalizer() {
        return new SearchTextNormalizer();
    }

    @Provides
    @Singleton
    public LiveRefreshScheduler provideLiveRefreshScheduler() {
        return new LiveRefreshScheduler();
    }
}
