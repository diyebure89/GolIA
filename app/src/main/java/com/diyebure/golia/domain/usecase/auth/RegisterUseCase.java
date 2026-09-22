package com.diyebure.golia.domain.usecase.auth;

import com.diyebure.golia.di.qualifier.IoExecutor;
import com.diyebure.golia.domain.common.Callback;
import com.diyebure.golia.domain.common.Result;
import com.diyebure.golia.domain.model.User;
import com.diyebure.golia.domain.repository.AuthRepository;

import java.util.concurrent.ExecutorService;

import javax.inject.Inject;

/**
 * Registers a new user account.
 *
 * <p>Mirrors {@link LoginUseCase}: orchestrates the {@link AuthRepository} on a
 * background executor and returns the outcome via {@link Callback}. Keeping one
 * use case per action makes the intent explicit at the call site and gives a
 * natural home for future business rules (e.g. referral codes, terms
 * acceptance) without touching the ViewModel or the repository.
 */
public class RegisterUseCase {

    private final AuthRepository authRepository;
    private final ExecutorService executor;

    @Inject
    public RegisterUseCase(AuthRepository authRepository, @IoExecutor ExecutorService executor) {
        this.authRepository = authRepository;
        this.executor = executor;
    }

    /**
     * Executes the registration off the main thread.
     *
     * @param fullName user full name
     * @param username desired username (nullable when not provided)
     * @param email    user email
     * @param password user password
     * @param callback receives {@code Result.Success<User>} or {@code Result.Error}
     */
    public void execute(String fullName, String username, String email, String password,
                        Callback<User> callback) {
        executor.execute(() -> {
            Result<User> result = authRepository.register(fullName, username, email, password);
            callback.onResult(result);
        });
    }
}
