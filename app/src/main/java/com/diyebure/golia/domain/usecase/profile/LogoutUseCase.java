package com.diyebure.golia.domain.usecase.profile;

import com.diyebure.golia.data.local.PreferencesManager;
import com.diyebure.golia.di.qualifier.IoExecutor;
import com.diyebure.golia.domain.common.Callback;
import com.diyebure.golia.domain.common.Result;
import com.diyebure.golia.domain.error.AuthError;
import com.diyebure.golia.domain.error.AuthException;

import java.util.concurrent.ExecutorService;

import javax.inject.Inject;

/**
 * Clears the local session on logout ({@code Cerrar_Sesion}, R12).
 *
 * <p>Same skeleton as {@code LoginUseCase}: dispatches the blocking work to the shared
 * IO executor and delivers the {@link Result} through a {@link Callback}.
 *
 * <p>Invokes {@link PreferencesManager#clearTokens()} to remove the session tokens. If
 * something fails, it returns {@code Result.Error(new AuthException(SESSION_CLEAR_FAILED))}
 * (R12.5). It never deletes the user record from the local database (R12.6).
 */
public class LogoutUseCase {

    private final PreferencesManager preferencesManager;
    private final ExecutorService executor;

    @Inject
    public LogoutUseCase(PreferencesManager preferencesManager,
                         @IoExecutor ExecutorService executor) {
        this.preferencesManager = preferencesManager;
        this.executor = executor;
    }

    /**
     * Executes the logout off the main thread.
     *
     * @param callback receives {@code Result.Success<Void>} on success, or
     *                 {@code Result.Error(new AuthException(SESSION_CLEAR_FAILED))} on failure
     */
    public void execute(Callback<Void> callback) {
        executor.execute(() -> {
            try {
                preferencesManager.clearTokens();
                callback.onResult(new Result.Success<>(null));
            } catch (Exception e) {
                callback.onResult(
                        new Result.Error(new AuthException(AuthError.SESSION_CLEAR_FAILED, e)));
            }
        });
    }
}
