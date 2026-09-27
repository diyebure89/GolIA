package com.diyebure.golia.presentation.util;

/**
 * Wraps a piece of content that is meant to be consumed exactly once
 * (SingleLiveEvent pattern). Used for one-shot presentation events such as
 * navigation (e.g. logout), operation success and error, so they are not
 * re-delivered after a configuration change (R14.1, R14.2, R14.3).
 *
 * @param <T> the type of the wrapped content
 */
public final class Event<T> {

    private final T content;
    private boolean handled = false;

    public Event(T content) {
        this.content = content;
    }

    /**
     * Returns the content and marks the event as handled the first time it is
     * called; every subsequent call returns {@code null}.
     *
     * @return the content on the first call, {@code null} afterwards
     */
    public T getContentIfNotHandled() {
        if (handled) {
            return null;
        }
        handled = true;
        return content;
    }

    /**
     * Returns the content without marking the event as handled. Consuming state
     * is left untouched.
     *
     * @return the wrapped content
     */
    public T peekContent() {
        return content;
    }
}
