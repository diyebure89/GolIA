package com.diyebure.golia.domain.usecase.profile;

import com.diyebure.golia.di.qualifier.IoExecutor;
import com.diyebure.golia.domain.common.Callback;
import com.diyebure.golia.domain.common.Result;
import com.diyebure.golia.domain.model.User;
import com.diyebure.golia.domain.repository.ProfileRepository;

import java.util.concurrent.ExecutorService;

import javax.inject.Inject;

/**
 * Loads the current user's profile ({@code Pantalla_Perfil}).
 *
 * <p>Depends only on the domain {@link ProfileRepository} interface. The repository
 * call is blocking, so the use case runs it on the shared IO executor and delivers
 * the outcome through a {@link Callback}, keeping the ViewModel free of threading
 * concerns (same skeleton as {@code LoginUseCase}).
 */
public class LoadProfileUseCase {

    private final ProfileRepository repository;
    private final ExecutorService executor;

    @Inject
    public LoadProfileUseCase(ProfileRepository repository, @IoExecutor ExecutorService executor) {
        this.repository = repository;
        this.executor = executor;
    }

    /**
     * Executes the profile load off the main thread.
     *
     * @param userId   the id of the {@code Current_User}
     * @param callback receives {@code Result.Success<User>} or {@code Result.Error}
     */
    public void execute(String userId, Callback<User> callback) {
        executor.execute(() -> {
            Result<User> result = repository.getCurrentUser(userId);
            callback.onResult(result);
        });
    }
}
