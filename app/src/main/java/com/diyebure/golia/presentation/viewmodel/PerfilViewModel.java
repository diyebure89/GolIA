package com.diyebure.golia.presentation.viewmodel;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.diyebure.golia.data.local.PreferencesManager;
import com.diyebure.golia.domain.error.AuthError;
import com.diyebure.golia.domain.error.AuthException;
import com.diyebure.golia.domain.model.RankingSnapshot;
import com.diyebure.golia.domain.model.User;
import com.diyebure.golia.domain.repository.RankingDataSource;
import com.diyebure.golia.domain.usecase.profile.ChangePasswordUseCase;
import com.diyebure.golia.domain.usecase.profile.LoadProfileUseCase;
import com.diyebure.golia.domain.usecase.profile.LogoutUseCase;
import com.diyebure.golia.domain.usecase.profile.UpdatePersonalDataUseCase;
import com.diyebure.golia.domain.usecase.profile.UpdateProfilePhotoUseCase;
import com.diyebure.golia.domain.validation.ProfileValidator;
import com.diyebure.golia.domain.validation.ValidationError;
import com.diyebure.golia.domain.validation.ValidationResult;
import com.diyebure.golia.di.qualifier.IoExecutor;
import com.diyebure.golia.presentation.ProfileFormState;
import com.diyebure.golia.presentation.util.Event;

import java.util.concurrent.ExecutorService;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * ViewModel for {@code Pantalla_Perfil}. Coordinates loading the current user's
 * profile, editing personal data, changing the password, updating the avatar and
 * logging out, exposing everything to the (thin) Fragment through {@link LiveData}.
 *
 * <p>Inherits {@code isLoading}/{@code errorMessage}/{@code isSuccess} from
 * {@link BaseViewModel} and adds the profile-specific state:
 * {@link ProfileFormState} (full name read-only, editable email/username plus
 * per-field errors), the avatar preview path, the {@link RankingSnapshot} for the
 * "Historial" section, and single-use {@link Event Events} for navigation, success
 * and error so they are consumed exactly once and not re-delivered after a
 * configuration change (R14).
 *
 * <p>The use cases already dispatch their blocking work to the shared IO executor
 * and deliver the outcome on a background thread, so results are published with
 * {@code postValue}. The ViewModel adds three UX concerns on top:
 * <ul>
 *   <li><b>Anti-double-submit</b>: an in-flight flag per operation blocks a second
 *       trigger while the first is running (R13.1, R13.2).</li>
 *   <li><b>Timeout</b>: every async operation schedules a 30s timer; if no answer
 *       arrives, loading is hidden, the control re-enabled and a timeout event is
 *       emitted (R13.5).</li>
 *   <li><b>Password attempt lockout</b>: an in-memory counter blocks the change
 *       flow for 60s after 5 consecutive {@code INVALID_CREDENTIALS}. It lives in
 *       the ViewModel, so it resets when the screen/VM is recreated (R7.10). It is
 *       a UX mitigation, not a security control.</li>
 * </ul>
 */
@HiltViewModel
public class PerfilViewModel extends BaseViewModel {

    /** One-shot navigation targets emitted through {@link #navigationEvent}. */
    public enum NavTarget {
        LOGIN
    }

    /** One-shot success messages emitted through {@link #successEvent}. */
    public enum UiMessage {
        PERSONAL_DATA_SAVED,
        PASSWORD_CHANGED,
        PHOTO_UPDATED
    }

    /** Operations tracked for anti-double-submit and timeout handling. */
    public enum Operation {
        LOAD,
        SAVE_PERSONAL_DATA,
        CHANGE_PASSWORD,
        UPDATE_PHOTO,
        LOGOUT
    }

    /** Maximum consecutive failed password attempts before locking (R7.10). */
    private static final int MAX_PASSWORD_ATTEMPTS = 5;
    /** Lockout window after reaching the attempt limit, in milliseconds (R7.10). */
    private static final long LOCKOUT_MILLIS = 60_000L;
    /** Per-operation timeout, in milliseconds (R13.5). */
    private static final long OPERATION_TIMEOUT_MILLIS = 30_000L;

    private final LoadProfileUseCase loadProfileUseCase;
    private final UpdatePersonalDataUseCase updatePersonalDataUseCase;
    private final ChangePasswordUseCase changePasswordUseCase;
    private final UpdateProfilePhotoUseCase updateProfilePhotoUseCase;
    private final LogoutUseCase logoutUseCase;
    private final ProfileValidator profileValidator;
    private final RankingDataSource rankingDataSource;
    private final PreferencesManager preferencesManager;
    private final ExecutorService ioExecutor;

    private final MutableLiveData<ProfileFormState> formState = new MutableLiveData<>();
    private final MutableLiveData<String> avatarPreviewPath = new MutableLiveData<>();
    private final MutableLiveData<RankingSnapshot> ranking = new MutableLiveData<>();

    private final MutableLiveData<Event<NavTarget>> navigationEvent = new MutableLiveData<>();
    private final MutableLiveData<Event<UiMessage>> successEvent = new MutableLiveData<>();
    private final MutableLiveData<Event<AuthError>> errorEvent = new MutableLiveData<>();
    private final MutableLiveData<Event<Operation>> timeoutEvent = new MutableLiveData<>();

    /** Main-thread handler used to schedule per-operation timeouts (R13.5). */
    private final Handler timeoutHandler = new Handler(Looper.getMainLooper());

    /**
     * In-flight guards, indexed by {@link Operation#ordinal()}. A {@code true}
     * entry means the operation is running and further triggers are ignored
     * (anti-double-submit, R13.1, R13.2).
     */
    private final boolean[] inFlight = new boolean[Operation.values().length];
    /** Pending timeout runnables, indexed by {@link Operation#ordinal()}. */
    private final Runnable[] timeoutRunnables = new Runnable[Operation.values().length];

    /** Consecutive {@code INVALID_CREDENTIALS} failures (in-memory, R7.10). */
    private int failedPasswordAttempts = 0;
    /** Epoch millis until which the change-password flow is locked; 0 == not locked. */
    private long lockedUntilMillis = 0L;

    @Inject
    public PerfilViewModel(LoadProfileUseCase loadProfileUseCase,
                           UpdatePersonalDataUseCase updatePersonalDataUseCase,
                           ChangePasswordUseCase changePasswordUseCase,
                           UpdateProfilePhotoUseCase updateProfilePhotoUseCase,
                           LogoutUseCase logoutUseCase,
                           ProfileValidator profileValidator,
                           RankingDataSource rankingDataSource,
                           PreferencesManager preferencesManager,
                           @IoExecutor ExecutorService ioExecutor) {
        this.loadProfileUseCase = loadProfileUseCase;
        this.updatePersonalDataUseCase = updatePersonalDataUseCase;
        this.changePasswordUseCase = changePasswordUseCase;
        this.updateProfilePhotoUseCase = updateProfilePhotoUseCase;
        this.logoutUseCase = logoutUseCase;
        this.profileValidator = profileValidator;
        this.rankingDataSource = rankingDataSource;
        this.preferencesManager = preferencesManager;
        this.ioExecutor = ioExecutor;

        // The ranking source now reads real users/predictions from Room, so it
        // must run off the main thread and publish with postValue (R11).
        this.ioExecutor.execute(() ->
                ranking.postValue(rankingDataSource.getRankingForCurrentUser()));
    }

    // ==================== Exposed state ====================

    /** @return the editable profile form state, retained across rotation (R14.4). */
    public LiveData<ProfileFormState> getFormState() {
        return formState;
    }

    /** @return the current/preview avatar absolute path, retained across rotation (R14.4). */
    public LiveData<String> getAvatarPreviewPath() {
        return avatarPreviewPath;
    }

    /** @return the ranking metrics shown in the "Historial" section (R11). */
    public LiveData<RankingSnapshot> getRanking() {
        return ranking;
    }

    /** @return one-shot navigation event (e.g. to login) (R14.1). */
    public LiveData<Event<NavTarget>> getNavigationEvent() {
        return navigationEvent;
    }

    /** @return one-shot success event (R14.1). */
    public LiveData<Event<UiMessage>> getSuccessEvent() {
        return successEvent;
    }

    /** @return one-shot typed error event (R13.6, R14.1). */
    public LiveData<Event<AuthError>> getErrorEvent() {
        return errorEvent;
    }

    /** @return one-shot timeout event carrying the timed-out operation (R13.5). */
    public LiveData<Event<Operation>> getTimeoutEvent() {
        return timeoutEvent;
    }

    // ==================== Actions ====================

    /**
     * Loads the current user's profile. Validates the local session first: when
     * there is none, emits a one-shot navigation event to login and does not touch
     * the database (R1.8, R1.9, R7.6). Otherwise it shows loading, calls
     * {@link LoadProfileUseCase} with the stored user id and, on success, fills the
     * form state and avatar preview; on a technical error it emits {@link #errorEvent}
     * (R1.10).
     */
    public void load() {
        if (!preferencesManager.isLoggedIn()) {
            navigationEvent.postValue(new Event<>(NavTarget.LOGIN));
            return;
        }
        String userId = preferencesManager.getUserId();
        if (userId == null) {
            // Session flagged as logged-in but no id available: treat as no session.
            navigationEvent.postValue(new Event<>(NavTarget.LOGIN));
            return;
        }
        if (!begin(Operation.LOAD)) {
            return;
        }
        loadProfileUseCase.execute(userId, result -> {
            if (!finish(Operation.LOAD)) {
                return; // already timed out
            }
            if (result.isSuccess()) {
                User user = result.getOrNull();
                if (user != null) {
                    formState.postValue(
                            ProfileFormState.of(user.getFullName(), user.getEmail(), user.getUsername()));
                    avatarPreviewPath.postValue(user.getAvatarUrl());
                }
            } else {
                emitError(result.getErrorOrNull());
            }
        });
    }

    /**
     * Validates and persists the editable personal data. On validation failure it
     * publishes per-field errors and does not persist (R15.4). On success it shows
     * loading, guards against a double submit and calls
     * {@link UpdatePersonalDataUseCase}; the reloaded user refreshes the form and a
     * success event is emitted (R6).
     *
     * @param username the edited username
     * @param email    the edited email
     */
    public void savePersonalData(String username, String email) {
        ValidationResult validation = profileValidator.validateProfile(username, email);
        if (!validation.isValid()) {
            ValidationError usernameError = validation.errorFor(ValidationResult.Field.USERNAME);
            ValidationError emailError = validation.errorFor(ValidationResult.Field.EMAIL);
            // Keep the edited values so the user can fix them in place (R13.4).
            formState.postValue(new ProfileFormState(currentFullName(), email, username,
                    emailError, usernameError));
            return;
        }

        if (!begin(Operation.SAVE_PERSONAL_DATA)) {
            return;
        }
        String userId = preferencesManager.getUserId();
        updatePersonalDataUseCase.execute(userId, username, email, result -> {
            if (!finish(Operation.SAVE_PERSONAL_DATA)) {
                return;
            }
            if (result.isSuccess()) {
                User user = result.getOrNull();
                if (user != null) {
                    formState.postValue(
                            ProfileFormState.of(user.getFullName(), user.getEmail(), user.getUsername()));
                }
                setSuccess(true);
                successEvent.postValue(new Event<>(UiMessage.PERSONAL_DATA_SAVED));
            } else {
                emitError(result.getErrorOrNull());
            }
        });
    }

    /**
     * Validates the password-change form, enforces the in-memory lockout and, when
     * allowed, calls {@link ChangePasswordUseCase}. Format errors are surfaced as a
     * {@code VALIDATION_ERROR} event; an {@code INVALID_CREDENTIALS} outcome
     * increments the consecutive-failure counter and, at the limit, locks the flow
     * for 60s (R7.2–R7.5, R7.9, R7.10).
     *
     * @param current the current password to verify
     * @param neu     the new password
     * @param confirm the confirmation of the new password
     */
    public void changePassword(String current, String neu, String confirm) {
        // Lockout check first: while locked, reject without hitting the use case.
        if (isPasswordChangeLocked()) {
            errorEvent.postValue(new Event<>(AuthError.INVALID_CREDENTIALS));
            return;
        }

        // Format validation (does not consume an attempt).
        if (isBlank(current)) {
            errorEvent.postValue(new Event<>(AuthError.VALIDATION_ERROR));
            return;
        }
        ValidationError newPwdError = profileValidator.validateNewPassword(neu);
        if (newPwdError != null) {
            errorEvent.postValue(new Event<>(AuthError.VALIDATION_ERROR));
            return;
        }
        ValidationError confirmError = profileValidator.validateConfirm(neu, confirm);
        if (confirmError != null) {
            errorEvent.postValue(new Event<>(AuthError.VALIDATION_ERROR));
            return;
        }

        if (!begin(Operation.CHANGE_PASSWORD)) {
            return;
        }
        String userId = preferencesManager.getUserId();
        changePasswordUseCase.execute(userId, current, neu, result -> {
            if (!finish(Operation.CHANGE_PASSWORD)) {
                return;
            }
            if (result.isSuccess()) {
                // A correct current password resets the consecutive-failure counter.
                failedPasswordAttempts = 0;
                lockedUntilMillis = 0L;
                setSuccess(true);
                successEvent.postValue(new Event<>(UiMessage.PASSWORD_CHANGED));
            } else {
                AuthError error = extractAuthError(result.getErrorOrNull());
                if (error == AuthError.INVALID_CREDENTIALS) {
                    registerFailedPasswordAttempt();
                }
                errorEvent.postValue(new Event<>(error));
            }
        });
    }

    /**
     * Processes and persists the picked/captured photo via
     * {@link UpdateProfilePhotoUseCase}. On success it updates the avatar preview
     * from the reloaded user and emits a success event (R8, R9).
     *
     * @param sourceUri the origin {@code content://} URI of the picked/captured image
     */
    public void updatePhoto(Uri sourceUri) {
        if (!begin(Operation.UPDATE_PHOTO)) {
            return;
        }
        String userId = preferencesManager.getUserId();
        updateProfilePhotoUseCase.execute(userId, sourceUri, result -> {
            if (!finish(Operation.UPDATE_PHOTO)) {
                return;
            }
            if (result.isSuccess()) {
                User user = result.getOrNull();
                if (user != null) {
                    avatarPreviewPath.postValue(user.getAvatarUrl());
                }
                setSuccess(true);
                successEvent.postValue(new Event<>(UiMessage.PHOTO_UPDATED));
            } else {
                emitError(result.getErrorOrNull());
            }
        });
    }

    /**
     * Clears the local session via {@link LogoutUseCase}. On success it emits a
     * one-shot navigation event to login; on failure ({@code SESSION_CLEAR_FAILED})
     * it emits an error event and stays on the profile screen (R12).
     */
    public void logout() {
        if (!begin(Operation.LOGOUT)) {
            return;
        }
        logoutUseCase.execute(result -> {
            if (!finish(Operation.LOGOUT)) {
                return;
            }
            if (result.isSuccess()) {
                navigationEvent.postValue(new Event<>(NavTarget.LOGIN));
            } else {
                emitError(result.getErrorOrNull());
            }
        });
    }

    // ==================== Anti-double-submit + timeout ====================

    /**
     * Marks {@code op} as in-flight and schedules its 30s timeout. Returns
     * {@code false} (and does nothing) when the operation is already running, which
     * is how a double submit is blocked (R13.1, R13.2, R13.5).
     */
    private boolean begin(Operation op) {
        int i = op.ordinal();
        if (inFlight[i]) {
            return false;
        }
        inFlight[i] = true;
        setLoading(true);
        scheduleTimeout(op);
        return true;
    }

    /**
     * Marks {@code op} as finished, cancels its pending timeout and hides loading.
     * Returns {@code false} when the operation was no longer in flight (already
     * timed out), so the late callback can be ignored.
     */
    private boolean finish(Operation op) {
        int i = op.ordinal();
        if (!inFlight[i]) {
            return false;
        }
        inFlight[i] = false;
        cancelTimeout(op);
        setLoading(false);
        return true;
    }

    private void scheduleTimeout(Operation op) {
        int i = op.ordinal();
        Runnable r = () -> {
            // Fire only if still in flight; a completed op clears the flag first.
            if (inFlight[i]) {
                inFlight[i] = false;
                setLoading(false);
                timeoutEvent.postValue(new Event<>(op));
            }
        };
        timeoutRunnables[i] = r;
        timeoutHandler.postDelayed(r, OPERATION_TIMEOUT_MILLIS);
    }

    private void cancelTimeout(Operation op) {
        int i = op.ordinal();
        Runnable r = timeoutRunnables[i];
        if (r != null) {
            timeoutHandler.removeCallbacks(r);
            timeoutRunnables[i] = null;
        }
    }

    // ==================== Password lockout ====================

    /** @return {@code true} while the change-password flow is locked (R7.10). */
    private boolean isPasswordChangeLocked() {
        return System.currentTimeMillis() < lockedUntilMillis;
    }

    /**
     * Records a consecutive {@code INVALID_CREDENTIALS} failure and, once the limit
     * is reached, locks the change-password flow for 60s (R7.10).
     */
    private void registerFailedPasswordAttempt() {
        failedPasswordAttempts++;
        if (failedPasswordAttempts >= MAX_PASSWORD_ATTEMPTS) {
            lockedUntilMillis = System.currentTimeMillis() + LOCKOUT_MILLIS;
            failedPasswordAttempts = 0; // start a fresh streak after the lock
        }
    }

    // ==================== Helpers ====================

    private void emitError(Exception error) {
        errorEvent.postValue(new Event<>(extractAuthError(error)));
    }

    /**
     * Extracts the typed {@link AuthError} carried by an {@link AuthException},
     * falling back to {@link AuthError#PERSISTENCE_ERROR} for anything else.
     */
    private static AuthError extractAuthError(Exception error) {
        if (error instanceof AuthException) {
            AuthError authError = ((AuthException) error).getAuthError();
            if (authError != null) {
                return authError;
            }
        }
        return AuthError.PERSISTENCE_ERROR;
    }

    private String currentFullName() {
        ProfileFormState state = formState.getValue();
        return state == null ? "" : state.getFullName();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * Cancels any pending timeout callbacks so the {@link Handler} does not hold a
     * reference past the ViewModel's lifecycle.
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        timeoutHandler.removeCallbacksAndMessages(null);
    }
}
