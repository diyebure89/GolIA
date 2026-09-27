package com.diyebure.golia.domain.usecase.profile;

import android.net.Uri;

import com.diyebure.golia.data.local.PhotoStorage;
import com.diyebure.golia.di.qualifier.IoExecutor;
import com.diyebure.golia.domain.common.Callback;
import com.diyebure.golia.domain.common.Result;
import com.diyebure.golia.domain.model.User;
import com.diyebure.golia.domain.repository.ProfileRepository;

import java.util.concurrent.ExecutorService;

import javax.inject.Inject;

/**
 * Processes the picked/captured image and persists the resulting avatar path (R8/R9).
 *
 * <p>Same skeleton as {@code LoginUseCase}: dispatches the blocking work to the shared
 * IO executor and delivers the {@link Result} through a {@link Callback}. The pipeline is:
 * <ol>
 *   <li>{@link PhotoStorage#processAndStore(String, Uri)} validates, processes and writes
 *       the image, returning the absolute path.</li>
 *   <li>On success, {@link ProfileRepository#updateAvatar(String, String)} persists the path
 *       and returns the reloaded {@link User}.</li>
 * </ol>
 * If the photo processing fails, its error is propagated as-is and the avatar update is
 * skipped.
 */
public class UpdateProfilePhotoUseCase {

    private final ProfileRepository repository;
    private final PhotoStorage photoStorage;
    private final ExecutorService executor;

    @Inject
    public UpdateProfilePhotoUseCase(ProfileRepository repository,
                                     PhotoStorage photoStorage,
                                     @IoExecutor ExecutorService executor) {
        this.repository = repository;
        this.photoStorage = photoStorage;
        this.executor = executor;
    }

    /**
     * Executes the photo processing and avatar update off the main thread.
     *
     * @param id       the user id
     * @param source   the origin {@code content://} URI of the picked/captured image
     * @param callback receives {@code Result.Success<User>} or {@code Result.Error}
     */
    public void execute(String id, Uri source, Callback<User> callback) {
        executor.execute(() -> {
            Result<String> stored = photoStorage.processAndStore(id, source);
            if (stored.isError()) {
                // Propagate the photo-processing error unchanged; do not touch the avatar.
                callback.onResult(new Result.Error(stored.getErrorOrNull()));
                return;
            }
            Result<User> result = repository.updateAvatar(id, stored.getOrNull());
            callback.onResult(result);
        });
    }
}
