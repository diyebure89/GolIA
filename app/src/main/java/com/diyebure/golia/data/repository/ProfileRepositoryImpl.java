package com.diyebure.golia.data.repository;

import android.database.sqlite.SQLiteConstraintException;

import com.diyebure.golia.data.local.PhotoStorage;
import com.diyebure.golia.data.local.PreferencesManager;
import com.diyebure.golia.data.local.dao.UserDao;
import com.diyebure.golia.data.local.entity.UserEntity;
import com.diyebure.golia.data.mapper.UserEntityMapper;
import com.diyebure.golia.domain.common.Result;
import com.diyebure.golia.domain.error.AuthError;
import com.diyebure.golia.domain.error.AuthException;
import com.diyebure.golia.domain.model.User;
import com.diyebure.golia.domain.repository.ProfileRepository;
import com.diyebure.golia.domain.security.PasswordCredential;
import com.diyebure.golia.domain.security.PasswordHasher;

import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Local implementation of {@link ProfileRepository} backed by Room (SQLite).
 *
 * <p>Mirrors the patterns of {@code LocalAuthRepositoryImpl}: normalization
 * helpers ({@code trim} + {@code toLowerCase(ROOT)}, single leading {@code '@'}
 * stripped from usernames), pre-check of uniqueness for friendly field-specific
 * errors, {@link SQLiteConstraintException} discrimination, and typed failures
 * carried inside {@code Result.Error} via {@link AuthException}.</p>
 *
 * <p>All methods run synchronously and return a {@link Result} directly; the use
 * cases are responsible for dispatching to the IO executor.</p>
 */
@Singleton
public class ProfileRepositoryImpl implements ProfileRepository {

    private final UserDao userDao;
    private final PasswordHasher passwordHasher;
    private final PreferencesManager preferencesManager;
    private final PhotoStorage photoStorage;

    @Inject
    public ProfileRepositoryImpl(UserDao userDao,
                                 PasswordHasher passwordHasher,
                                 PreferencesManager preferencesManager,
                                 PhotoStorage photoStorage) {
        this.userDao = userDao;
        this.passwordHasher = passwordHasher;
        this.preferencesManager = preferencesManager;
        this.photoStorage = photoStorage;
    }

    @Override
    public Result<User> getCurrentUser(String id) {
        try {
            UserEntity entity = userDao.getById(id);
            if (entity == null) {
                return userError(AuthError.PERSISTENCE_ERROR);
            }
            return new Result.Success<>(UserEntityMapper.toDomain(entity));
        } catch (Exception e) {
            return new Result.Error(new AuthException(AuthError.PERSISTENCE_ERROR, e));
        }
    }

    @Override
    public Result<User> updatePersonalData(String id, String username, String email) {
        try {
            String normEmail = normalize(email);
            String normUsername = normalizeUsernameOrNull(username);

            // Pre-check uniqueness excluding the user's own id for a friendly,
            // field-specific error.
            if (normUsername != null && userDao.existsByUsernameExcludingId(normUsername, id)) {
                return userError(AuthError.USERNAME_TAKEN);
            }
            if (userDao.existsByEmailExcludingId(normEmail, id)) {
                return userError(AuthError.EMAIL_TAKEN);
            }

            // Update only the editable personal-data columns.
            userDao.updatePersonalData(id, normUsername, normEmail);

            // Reload to return the persisted state.
            UserEntity updated = userDao.getById(id);
            if (updated == null) {
                return userError(AuthError.PERSISTENCE_ERROR);
            }
            return new Result.Success<>(UserEntityMapper.toDomain(updated));

        } catch (SQLiteConstraintException e) {
            // Another writer took the same email/username between the pre-check and
            // the update. The update is atomic, so no partial data was persisted.
            return resolveConstraint(id, username, email, e);
        } catch (Exception e) {
            return new Result.Error(new AuthException(AuthError.PERSISTENCE_ERROR, e));
        }
    }

    @Override
    public Result<Void> changePassword(String id, String currentPassword, String newPassword) {
        try {
            UserEntity entity = userDao.getById(id);
            if (entity == null) {
                return new Result.Error(new AuthException(AuthError.SESSION_UNAVAILABLE));
            }

            // Verify the current password in constant time; do not touch the DB on failure.
            if (!passwordHasher.verify(entity.toCredential(), currentPassword)) {
                return new Result.Error(new AuthException(AuthError.INVALID_CREDENTIALS));
            }

            // Derive a fresh credential (new random salt) for the new password.
            PasswordCredential credential = passwordHasher.hash(newPassword);

            // Update only the credential columns; never persist plain text.
            userDao.updateCredentials(
                    id,
                    credential.getAlgorithm(),
                    credential.getIterations(),
                    credential.getSaltBase64(),
                    credential.getHashBase64());

            return new Result.Success<>(null);
        } catch (Exception e) {
            return new Result.Error(new AuthException(AuthError.PERSISTENCE_ERROR, e));
        }
    }

    @Override
    public Result<User> updateAvatar(String id, String absolutePath) {
        try {
            userDao.updateAvatar(id, absolutePath);

            UserEntity updated = userDao.getById(id);
            if (updated == null) {
                return userError(AuthError.PERSISTENCE_ERROR);
            }
            return new Result.Success<>(UserEntityMapper.toDomain(updated));
        } catch (Exception e) {
            return new Result.Error(new AuthException(AuthError.PERSISTENCE_ERROR, e));
        }
    }

    // ==================== Helpers ====================

    /**
     * Normalize a free-form value: {@code trim} + {@code toLowerCase(ROOT)}.
     * Null-safe.
     */
    private static String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Normalize a username: {@code trim}, drop a single leading {@code '@'},
     * {@code toLowerCase(ROOT)}; an empty result becomes {@code null}
     * (username not provided).
     */
    private static String normalizeUsernameOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("@")) {
            trimmed = trimmed.substring(1);
        }
        trimmed = trimmed.trim().toLowerCase(Locale.ROOT);
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Re-check uniqueness (excluding the user's own id) after a constraint
     * violation to discriminate between a duplicate username and a duplicate
     * email; fall back to {@link AuthError#PERSISTENCE_ERROR} when it cannot be
     * determined.
     */
    private Result<User> resolveConstraint(String id, String username, String email, Exception cause) {
        try {
            String normUsername = normalizeUsernameOrNull(username);
            if (normUsername != null && userDao.existsByUsernameExcludingId(normUsername, id)) {
                return new Result.Error(new AuthException(AuthError.USERNAME_TAKEN, cause));
            }
            if (userDao.existsByEmailExcludingId(normalize(email), id)) {
                return new Result.Error(new AuthException(AuthError.EMAIL_TAKEN, cause));
            }
        } catch (Exception ignored) {
            // Fall through to a generic persistence error below.
        }
        return new Result.Error(new AuthException(AuthError.PERSISTENCE_ERROR, cause));
    }

    /**
     * Build a {@code Result.Error} carrying the given typed {@link AuthError}.
     */
    private static Result<User> userError(AuthError authError) {
        return new Result.Error(new AuthException(authError));
    }
}
