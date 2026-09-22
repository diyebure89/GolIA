package com.diyebure.golia.domain.validation;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Immutable, per-field validation outcome. The key is the logical form field
 * and the value is the first {@link ValidationError} detected for that field.
 * An empty error map means every field is valid.
 *
 * <p>Instances are built through {@link #builder()} to keep them immutable;
 * once created the error map cannot be mutated.
 */
public final class ValidationResult {

    /** Logical fields of the registration form. */
    public enum Field {
        FULL_NAME,
        USERNAME,
        EMAIL,
        PASSWORD,
        CONFIRM_PASSWORD
    }

    private final Map<Field, ValidationError> errors; // empty == valid

    private ValidationResult(@NonNull Map<Field, ValidationError> errors) {
        // Defensive copy + unmodifiable view to guarantee immutability.
        this.errors = Collections.unmodifiableMap(new EnumMap<>(errors));
    }

    /** @return {@code true} when there are no field errors. */
    public boolean isValid() {
        return errors.isEmpty();
    }

    /**
     * @param f the field to inspect
     * @return the error for the given field, or {@code null} if it is valid
     */
    public ValidationError errorFor(Field f) {
        return errors.get(f);
    }

    /** @return an unmodifiable view of the field errors (empty == valid). */
    public Map<Field, ValidationError> getErrors() {
        return errors;
    }

    /** @return a new builder to accumulate per-field errors. */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Internal builder that accumulates per-field errors before producing an
     * immutable {@link ValidationResult}. {@code null} errors are ignored so
     * callers can pass the direct output of per-field validation methods.
     */
    public static final class Builder {

        private final Map<Field, ValidationError> errors = new EnumMap<>(Field.class);

        private Builder() {
        }

        /**
         * Records an error for a field. A {@code null} error is treated as
         * "field is valid" and leaves the map unchanged.
         *
         * @param field the field the error belongs to
         * @param error the detected error, or {@code null} if the field is valid
         * @return this builder for chaining
         */
        public Builder put(@NonNull Field field, ValidationError error) {
            if (error != null) {
                errors.put(field, error);
            }
            return this;
        }

        /** @return an immutable {@link ValidationResult} with the accumulated errors. */
        public ValidationResult build() {
            return new ValidationResult(errors);
        }
    }
}
