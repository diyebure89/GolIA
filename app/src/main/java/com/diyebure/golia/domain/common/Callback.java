package com.diyebure.golia.domain.common;

/**
 * Simple one-shot callback used to deliver the {@link Result} of an
 * asynchronous use case back to the presentation layer.
 *
 * <p>In a Java (non-coroutine) codebase this is the seam that lets use cases
 * run their blocking work on a background executor and post the outcome back,
 * while the ViewModel decides how to surface it (LiveData, etc.).
 *
 * @param <T> type of the value carried on success
 */
@FunctionalInterface
public interface Callback<T> {

    void onResult(Result<T> result);
}
