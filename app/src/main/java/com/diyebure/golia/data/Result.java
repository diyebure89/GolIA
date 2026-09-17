package com.diyebure.golia.data;

/**
 * A generic class that holds a result success w/ data or an error exception.
 */
public class Result<T> {
    // hide the private constructor to limit subclass types (Success, Error)
    private Result() {
    }

    @Override
    public String toString() {
        if (this instanceof Result.Success) {
            Result.Success success = (Result.Success) this;
            return "Success[data=" + success.getData().toString() + "]";
        } else if (this instanceof Result.Error) {
            Result.Error error = (Result.Error) this;
            return "Error[exception=" + error.getError().toString() + "]";
        }
        return "";
    }

    // Success sub-class
    public final static class Success<T> extends Result {
        private T data;

        public Success(T data) {
            this.data = data;
        }

        public T getData() {
            return this.data;
        }
    }

    // Error sub-class
    public final static class Error extends Result {
        private Exception error;

        public Error(Exception error) {
            this.error = error;
        }

        public Exception getError() {
            return this.error;
        }
    }

    // ==================== Utility Methods ====================

    /**
     * Check if this result is a success.
     *
     * @return true if this is a Success result, false otherwise
     */
    public boolean isSuccess() {
        return this instanceof Success;
    }

    /**
     * Check if this result is an error.
     *
     * @return true if this is an Error result, false otherwise
     */
    public boolean isError() {
        return this instanceof Error;
    }

    /**
     * Get the data if this is a Success result, or null otherwise.
     *
     * @return The data value or null if this is an Error
     */
    public T getOrNull() {
        if (this instanceof Success) {
            return ((Success<T>) this).getData();
        }
        return null;
    }

    /**
     * Get the data if this is a Success result, or the provided default value otherwise.
     *
     * @param defaultValue The value to return if this is an Error
     * @return The data value or the default value
     */
    public T getOrDefault(T defaultValue) {
        if (this instanceof Success) {
            return ((Success<T>) this).getData();
        }
        return defaultValue;
    }

    /**
     * Get the error if this is an Error result, or null otherwise.
     *
     * @return The exception or null if this is a Success
     */
    public Exception getErrorOrNull() {
        if (this instanceof Error) {
            return ((Error) this).getError();
        }
        return null;
    }

    /**
     * Map the success value to a different type.
     *
     * @param mapper Function to transform the data
     * @param <R>    The new type
     * @return A new Result with the transformed value or the same error
     */
    public <R> Result<R> map(java.util.function.Function<T, R> mapper) {
        if (this instanceof Success) {
            return new Success<>(mapper.apply(((Success<T>) this).getData()));
        }
        return new Result.Error(((Error) this).getError());
    }

    /**
     * Execute an action if this is a success.
     *
     * @param action Action to execute with the data
     * @return This result for chaining
     */
    public Result<T> onSuccess(java.util.function.Consumer<T> action) {
        if (this instanceof Success) {
            action.accept(((Success<T>) this).getData());
        }
        return this;
    }

    /**
     * Execute an action if this is an error.
     *
     * @param action Action to execute with the exception
     * @return This result for chaining
     */
    public Result<T> onError(java.util.function.Consumer<Exception> action) {
        if (this instanceof Error) {
            action.accept(((Error) this).getError());
        }
        return this;
    }
}