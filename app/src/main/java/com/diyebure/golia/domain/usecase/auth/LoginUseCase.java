package com.diyebure.golia.domain.usecase.auth;

import com.diyebure.golia.di.qualifier.IoExecutor;
import com.diyebure.golia.domain.common.Callback;
import com.diyebure.golia.domain.common.Result;
import com.diyebure.golia.domain.model.User;
import com.diyebure.golia.domain.repository.AuthRepository;

import java.util.concurrent.ExecutorService;

import javax.inject.Inject;

/**
 * Authenticates a user with email and password.
 *
 * <p>A use case represents one business action. It depends only on the domain
 * {@link AuthRepository} interface (never on a concrete data class), which keeps
 * business rules independent of networking/persistence details.
 *
 * <p>The repository call is blocking, so the use case runs it on the shared IO
 * executor and delivers the outcome through a {@link Callback}. The ViewModel
 * stays free of threading concerns and only reacts to the {@link Result}.
 */
public class LoginUseCase {

    private final AuthRepository authRepository;
    private final ExecutorService executor;

    @Inject
    public LoginUseCase(AuthRepository authRepository, @IoExecutor ExecutorService executor) {
        this.authRepository = authRepository;
        this.executor = executor;
    }

    /**
     * Executes the login off the main thread.
     *
     * @param identifier user username or email
     * @param password   user password
     * @param callback   receives {@code Result.Success<User>} or {@code Result.Error}
     */
    public void execute(String identifier, String password, Callback<User> callback) {
        executor.execute(() -> {
            Result<User> result = authRepository.login(identifier, password);
            callback.onResult(result);
        });
    }
}
