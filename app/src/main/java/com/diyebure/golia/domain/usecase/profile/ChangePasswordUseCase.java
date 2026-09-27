package com.diyebure.golia.domain.usecase.profile;

import com.diyebure.golia.di.qualifier.IoExecutor;
import com.diyebure.golia.domain.common.Callback;
import com.diyebure.golia.domain.common.Result;
import com.diyebure.golia.domain.repository.ProfileRepository;

import java.util.concurrent.ExecutorService;

import javax.inject.Inject;

/**
 * Changes the user's password after verifying the current one (R7).
 *
 * <p>Same skeleton as {@code LoginUseCase}: dispatches the blocking repository call
 * to the shared IO executor and delivers the {@link Result} through a {@link Callback}.
 */
public class ChangePasswordUseCase {

    private final ProfileRepository repository;
    private final ExecutorService executor;

    @Inject
    public ChangePasswordUseCase(ProfileRepository repository,
                                 @IoExecutor ExecutorService executor) {
        this.repository = repository;
        this.executor = executor;
    }

    /**
     * Executes the password change off the main thread.
     *
     * @param id       the user id
     * @param current  the current password to verify
     * @param neu      the new password to hash and persist
     * @param callback receives {@code Result.Success<Void>} or {@code Result.Error}
     */
    public void execute(String id, String current, String neu, Callback<Void> callback) {
        executor.execute(() -> {
            Result<Void> result = repository.changePassword(id, current, neu);
            callback.onResult(result);
        });
    }
}
