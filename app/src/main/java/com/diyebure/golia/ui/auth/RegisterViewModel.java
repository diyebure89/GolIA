package com.diyebure.golia.ui.auth;

import androidx.annotation.StringRes;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.diyebure.golia.R;
import com.diyebure.golia.domain.error.AuthError;
import com.diyebure.golia.domain.error.AuthException;
import com.diyebure.golia.domain.usecase.auth.RegisterUseCase;
import com.diyebure.golia.domain.validation.RegistrationValidator;
import com.diyebure.golia.domain.validation.ValidationError;
import com.diyebure.golia.domain.validation.ValidationResult;
import com.diyebure.golia.ui.common.Event;

import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * ViewModel for the registration screen.
 *
 * <p>Collects the five form fields (including {@code fullName}), validates them
 * through the centralized {@link RegistrationValidator} and delegates the actual
 * account creation to {@link RegisterUseCase}. Validation errors are published
 * per field; success and error outcomes are exposed as single-use
 * {@link Event Events} so they are consumed exactly once and not re-delivered on
 * configuration changes (R13.1, R13.2).
 *
 * <p>The {@code LOADING} state is a plain {@link LiveData} (not an event) so it
 * survives rotation and is used to block a double submit: {@link #register}
 * ignores calls while loading is {@code true} (R10.1, R13.3).
 *
 * <p>Registration does <b>not</b> auto-login: this ViewModel never touches
 * {@code PreferencesManager}; navigation to the login screen is handled by the
 * Activity after a success event (R11.1).
 */
@HiltViewModel
public class RegisterViewModel extends ViewModel {

    private final RegisterUseCase registerUseCase;
    private final RegistrationValidator validator;

    /** {@code true} while a registration request is in flight. Blocks double submit. */
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    /** One-shot per-field validation errors (empty map is never emitted). */
    private final MutableLiveData<Event<Map<ValidationResult.Field, ValidationError>>> fieldErrors =
            new MutableLiveData<>();

    /** One-shot success signal (registration completed, no auto-login). */
    private final MutableLiveData<Event<Boolean>> registerSuccess = new MutableLiveData<>();

    /** One-shot error signal carrying a mapped string resource id. */
    private final MutableLiveData<Event<Integer>> errorMessage = new MutableLiveData<>();

    @Inject
    public RegisterViewModel(RegisterUseCase registerUseCase, RegistrationValidator validator) {
        this.registerUseCase = registerUseCase;
        this.validator = validator;
    }

    /** @return loading state; {@code true} while a request is in flight (R10.1, R13.3). */
    public LiveData<Boolean> getLoading() {
        return loading;
    }

    /** @return one-shot per-field validation errors (R13.1, R14.1). */
    public LiveData<Event<Map<ValidationResult.Field, ValidationError>>> getFieldErrors() {
        return fieldErrors;
    }

    /** @return one-shot success event; registration does NOT auto-login (R11.1, R13.1). */
    public LiveData<Event<Boolean>> getRegisterSuccess() {
        return registerSuccess;
    }

    /** @return one-shot error event carrying a {@code @StringRes} id (R13.1, R15.2). */
    public LiveData<Event<Integer>> getErrorMessage() {
        return errorMessage;
    }

    /**
     * Validates the form and, if valid, triggers registration through the use
     * case. Ignored while a request is already in flight (double-submit guard,
     * R10.1). On invalid input, publishes per-field errors as a one-shot event
     * and does not call the use case.
     *
     * @param fullName        full name (required)
     * @param username        desired username (optional, may be empty/null)
     * @param email           email (required)
     * @param password        password (required)
     * @param confirmPassword password confirmation (required)
     */
    public void register(String fullName, String username, String email,
                         String password, String confirmPassword) {
        // Block double submit while a request is in flight.
        if (Boolean.TRUE.equals(loading.getValue())) {
            return;
        }

        ValidationResult result =
                validator.validateRegistration(fullName, username, email, password, confirmPassword);
        if (!result.isValid()) {
            fieldErrors.setValue(new Event<>(result.getErrors()));
            return;
        }

        loading.setValue(true);

        registerUseCase.execute(fullName, username, email, password, useCaseResult -> {
            // Results arrive on a background thread -> postValue.
            loading.postValue(false);
            if (useCaseResult.isSuccess()) {
                registerSuccess.postValue(new Event<>(true));
            } else {
                errorMessage.postValue(new Event<>(mapError(useCaseResult.getErrorOrNull())));
            }
        });
    }

    /**
     * Maps an exception coming back from the use case to a localized string
     * resource id. Typed {@link AuthError}s are mapped one-to-one; anything
     * else falls back to a generic message (R15.2).
     *
     * @param error the exception carried by {@code Result.Error}, may be {@code null}
     * @return the string resource id to display
     */
    @StringRes
    private int mapError(Exception error) {
        if (error instanceof AuthException) {
            return mapAuthError(((AuthException) error).getAuthError());
        }
        return R.string.error_generic;
    }

    /**
     * Maps a typed {@link AuthError} to its string resource.
     *
     * @param authError the typed error, may be {@code null}
     * @return the matching string resource id
     */
    @StringRes
    private int mapAuthError(AuthError authError) {
        if (authError == null) {
            return R.string.error_generic;
        }
        switch (authError) {
            case USERNAME_TAKEN:
                return R.string.error_username_taken;
            case EMAIL_TAKEN:
                return R.string.error_email_taken;
            case INVALID_CREDENTIALS:
                return R.string.error_invalid_credentials;
            case PERSISTENCE_ERROR:
                return R.string.error_persistence;
            case VALIDATION_ERROR:
                return R.string.error_validation;
            default:
                return R.string.error_generic;
        }
    }
}
