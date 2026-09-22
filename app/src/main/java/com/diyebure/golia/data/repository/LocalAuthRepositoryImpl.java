package com.diyebure.golia.data.repository;

import android.database.sqlite.SQLiteConstraintException;

import com.diyebure.golia.data.local.dao.UserDao;
import com.diyebure.golia.data.local.entity.UserEntity;
import com.diyebure.golia.data.mapper.UserEntityMapper;
import com.diyebure.golia.domain.common.Result;
import com.diyebure.golia.domain.error.AuthError;
import com.diyebure.golia.domain.error.AuthException;
import com.diyebure.golia.domain.model.User;
import com.diyebure.golia.domain.repository.AuthRepository;
import com.diyebure.golia.domain.security.PasswordCredential;
import com.diyebure.golia.domain.security.PasswordHasher;

import java.util.Locale;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Local implementation of {@link AuthRepository} backed by Room (SQLite).
 *
 * <p>This implementation persists user accounts locally and derives/verifies
 * password credentials via {@link PasswordHasher}. It does not talk to any
 * remote backend; the remote {@code AuthRepositoryImpl} is kept for the future
 * but is not bound in the MVP.</p>
 *
 * <p>Both {@link #register} and {@link #login} run synchronously and return a
 * {@link Result} directly; the use cases are responsible for dispatching to the
 * IO executor. Typed failures travel inside {@code Result.Error} via
 * {@link AuthException}.</p>
 */
@Singleton
public class LocalAuthRepositoryImpl implements AuthRepository {

    private final UserDao userDao;
    private final PasswordHasher passwordHasher;

    @Inject
    public LocalAuthRepositoryImpl(UserDao userDao, PasswordHasher passwordHasher) {
        this.userDao = userDao;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public Result<User> register(String fullName, String username, String email, String password) {
        try {
            String normEmail = normalize(email);
            String normUsername = normalizeUsernameOrNull(username);

            // Pre-check uniqueness for a friendly, field-specific error.
            if (normUsername != null && userDao.existsByUsername(normUsername)) {
                return error(AuthError.USERNAME_TAKEN);
            }
            if (userDao.existsByEmail(normEmail)) {
                return error(AuthError.EMAIL_TAKEN);
            }

            PasswordCredential credential = passwordHasher.hash(password);
            UserEntity entity = UserEntity.newUser(
                    UUID.randomUUID().toString(),
                    fullName != null ? fullName.trim() : null,
                    normUsername,
                    normEmail,
                    credential,
                    System.currentTimeMillis());

            // Insert aborts (throws) on a UNIQUE conflict; treated as a safeguard below.
            userDao.insert(entity);

            return new Result.Success<>(UserEntityMapper.toDomain(entity));

        } catch (SQLiteConstraintException e) {
            // Another writer inserted the same email/username between the pre-check
            // and the insert. The insert is atomic, so no partial data was persisted.
            return resolveConstraint(username, email, e);
        } catch (Exception e) {
            return new Result.Error(new AuthException(AuthError.PERSISTENCE_ERROR, e));
        }
    }

    @Override
    public Result<User> login(String identifier, String password) {
        try {
            UserEntity entity = userDao.findByUsernameOrEmail(normalize(identifier));
            if (entity == null) {
                return error(AuthError.INVALID_CREDENTIALS);
            }
            if (!passwordHasher.verify(entity.toCredential(), password)) {
                return error(AuthError.INVALID_CREDENTIALS);
            }
            return new Result.Success<>(UserEntityMapper.toDomain(entity));
        } catch (Exception e) {
            return new Result.Error(new AuthException(AuthError.PERSISTENCE_ERROR, e));
        }
    }

    // ==================== Session / remote-oriented methods ====================
    // The local MVP has no tokens; the session flag is owned by the ViewModel
    // through PreferencesManager. These are minimal local implementations.

    @Override
    public Result<Void> logout() {
        return new Result.Success<>(null);
    }

    @Override
    public Result<User> refreshToken() {
        return new Result.Error(new AuthException(AuthError.INVALID_CREDENTIALS));
    }

    @Override
    public boolean isLoggedIn() {
        return false;
    }

    @Override
    public Result<User> getCurrentUser() {
        return new Result.Error(new AuthException(AuthError.INVALID_CREDENTIALS));
    }

    @Override
    public void saveAuthTokens(String accessToken, String refreshToken, long expiresIn) {
        // No-op: the local implementation does not issue or store tokens.
    }

    @Override
    public void clearSession() {
        // No-op: session state is managed by PreferencesManager in the ViewModel.
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
     * Re-check uniqueness after a constraint violation to discriminate between
     * a duplicate username and a duplicate email; fall back to
     * {@link AuthError#PERSISTENCE_ERROR} when it cannot be determined.
     */
    private Result<User> resolveConstraint(String username, String email, Exception cause) {
        try {
            String normUsername = normalizeUsernameOrNull(username);
            if (normUsername != null && userDao.existsByUsername(normUsername)) {
                return new Result.Error(new AuthException(AuthError.USERNAME_TAKEN, cause));
            }
            if (userDao.existsByEmail(normalize(email))) {
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
    private static Result<User> error(AuthError authError) {
        return new Result.Error(new AuthException(authError));
    }
}
