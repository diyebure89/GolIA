package com.diyebure.golia.domain.usecase.profile;

import com.diyebure.golia.di.qualifier.IoExecutor;
import com.diyebure.golia.domain.common.Callback;
import com.diyebure.golia.domain.common.Result;
import com.diyebure.golia.domain.model.User;
import com.diyebure.golia.domain.repository.ProfileRepository;

import java.util.concurrent.ExecutorService;

import javax.inject.Inject;

/**
 * Updates the user's editable personal data (username and email).
 *
 * <p>Same skeleton as {@code LoginUseCase}: dispatches the blocking repository call
 * to the shared IO executor and delivers the {@link Result} through a {@link Callback}.
 */
public class UpdatePersonalDataUseCase {

    private final ProfileRepository repository;
    private final ExecutorService executor;

    @Inject
    public UpdatePersonalDataUseCase(ProfileRepository repository,
                                     @IoExecutor ExecutorService executor) {
        this.repository = repository;
        this.executor = executor;
    }

    /**
     * Executes the personal-data update off the main thread.
     *
     * @param id       the user id
     * @param username the new username (nullable when cleared)
     * @param email    the new email
     * @param callback receives {@code Result.Success<User>} or {@code Result.Error}
     */
    public void execute(String id, String username, String email, Callback<User> callback) {
        executor.execute(() -> {
            Result<User> result = repository.updatePersonalData(id, username, email);
            callback.onResult(result);
        });
    }
}
