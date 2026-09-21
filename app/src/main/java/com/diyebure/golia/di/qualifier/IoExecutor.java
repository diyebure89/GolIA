package com.diyebure.golia.di.qualifier;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.inject.Qualifier;

/**
 * Qualifies the shared background {@link java.util.concurrent.ExecutorService}
 * used for IO-bound work (network + database).
 *
 * <p>Using a qualifier lets us inject a single, app-wide pool wherever
 * background work is needed, instead of each repository creating its own
 * threads. In tests this can be replaced by a synchronous/direct executor to
 * make repository behaviour deterministic.
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface IoExecutor {
}
