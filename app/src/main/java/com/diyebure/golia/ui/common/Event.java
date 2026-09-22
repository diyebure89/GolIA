package com.diyebure.golia.ui.common;

/**
 * Single-use event wrapper (Google's standard pattern for one-shot events with
 * {@code LiveData}). Used by the ViewModels to expose navigation and messages
 * that must be consumed exactly once, so they are not re-delivered on
 * configuration changes such as screen rotation (R13.1, R13.2).
 *
 * <p>The load state ({@code LOADING}) is kept as normal {@code LiveData} and is
 * intentionally not wrapped, since it should survive rotation (R13.3).
 *
 * @param <T> the type of the content carried by this event.
 */
public class Event<T> {

    private final T content;
    private boolean hasBeenHandled = false;

    public Event(T content) {
        this.content = content;
    }

    /**
     * Returns the content only the first time it is called and {@code null} on
     * every subsequent call. Marks the event as handled on the first call.
     *
     * @return the content the first time, {@code null} afterwards.
     */
    public T getContentIfNotHandled() {
        if (hasBeenHandled) {
            return null;
        }
        hasBeenHandled = true;
        return content;
    }

    /**
     * Returns the content without marking the event as consumed. Useful for
     * inspecting the payload (e.g. logging) without affecting the single-use
     * semantics of {@link #getContentIfNotHandled()}.
     *
     * @return the wrapped content.
     */
    public T peekContent() {
        return content;
    }
}
