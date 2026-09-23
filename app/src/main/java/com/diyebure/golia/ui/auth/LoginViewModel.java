package com.diyebure.golia.ui.auth;

import androidx.annotation.StringRes;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.diyebure.golia.R;
import com.diyebure.golia.data.local.PreferencesManager;
import com.diyebure.golia.domain.error.AuthError;
import com.diyebure.golia.domain.error.AuthException;
import com.diyebure.golia.domain.model.User;
import com.diyebure.golia.domain.usecase.auth.LoginUseCase;
import com.diyebure.golia.ui.common.Event;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * ViewModel for the login screen.
 *
 * <p>Receives an identifier (username or email) plus a password, performs a
 * minimal non-empty validation and delegates the actual authentication to
 * {@link LoginUseCase}. On success it persists the session
 * ({@link PreferencesManager#setLoggedIn(boolean)} + {@code saveUserId(id)})
 * <b>before</b> emitting the one-shot navigation event to {@code Home_Screen}
 * (R11.2). On error it emits {@code INVALID_CREDENTIALS} as a one-shot event so
 * the Activity shows a global {@code Toast} that does not reveal which field
 * failed (R10.5, R15.2).
 *
 * <p>Success and error outcomes are exposed as single-use {@link Event Events}
 * so they are consumed exactly once and not re-delivered on configuration
 * changes such as screen rotation (R13.1, R13.2).
 *
 * <p>The {@code LOADING} state is a plain {@link LiveData} (not an event) so it
 * survives rotation and is used to block a double submit: {@link #login} ignores
 * calls while loading is {@code true} (R10.4, R13.3).
 */
@HiltViewModel
public class LoginViewModel extends ViewModel {

    private final LoginUseCase loginUseCase;
    private final PreferencesManager preferencesManager;

    /** {@code true} while a login request is in flight. Blocks double submit. */
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    /** One-shot navigation event to {@code Home_Screen} after a successful login. */
    private final MutableLiveData<Event<Boolean>> navigateToHome = new MutableLiveData<>();

    /** One-shot error signal carrying a mapped string resource id (global Toast). */
    private final MutableLiveData<Event<Integer>> errorMessage = new MutableLiveData<>();

    @Inject
    public LoginViewModel(LoginUseCase loginUseCase, PreferencesManager preferencesManager) {
        this.loginUseCase = loginUseCase;
        this.preferencesManager = preferencesManager;
    }

    /** @return loading state; {@code true} while a request is in flight (R10.4, R13.3). */
    public LiveData<Boolean> getLoading() {
        return loading;
    }

    /** @return one-shot navigation event to {@code Home_Screen} (R11.2, R13.1). */
    public LiveData<Event<Boolean>> getNavigateToHome() {
        return navigateToHome;
    }

    /** @return one-shot error event carrying a {@code @StringRes} id (R13.1, R15.2). */
    public LiveData<Event<Integer>> getErrorMessage() {
        return errorMessage;
    }

    /**
     * Validates the input (identifier and password must be non-empty) and, if
     * valid, triggers authentication through the use case. Ignored while a
     * request is already in flight (double-submit guard, R10.4). On empty
     * input, publishes a one-shot error event and does not call the use case.
     *
     * @param identifier username or email
     * @param password   password
     */
    public void login(String identifier, String password) {
        // Block double submit while a request is in flight.
        if (Boolean.TRUE.equals(loading.getValue())) {
            return;
        }

        // Minimal validation: both fields must be non-empty.
        if (isBlank(identifier) || isBlank(password)) {
            errorMessage.setValue(new Event<>(R.string.error_validation));
            return;
        }

        loading.setValue(true);

        loginUseCase.execute(identifier, password, result -> {
            // Results arrive on a background thread -> postValue.
            loading.postValue(false);
            if (result.isSuccess()) {
                User user = result.getOrNull();
                // Persist the session BEFORE emitting the navigation event (R11.2).
                preferencesManager.setLoggedIn(true);
                if (user != null) {
                    preferencesManager.saveUserId(user.getId());
                    preferencesManager.saveUserName(user.getFullName()); // R11.1
                }
                navigateToHome.postValue(new Event<>(true));
            } else {
                errorMessage.postValue(new Event<>(mapError(result.getErrorOrNull())));
            }
        });
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
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
