package com.diyebure.golia.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.diyebure.golia.data.local.PhotoStorage;
import com.diyebure.golia.data.local.PreferencesManager;
import com.diyebure.golia.data.local.dao.UserDao;
import com.diyebure.golia.data.local.entity.UserEntity;
import com.diyebure.golia.data.security.Pbkdf2PasswordHasher;
import com.diyebure.golia.domain.common.Result;
import com.diyebure.golia.domain.error.AuthError;
import com.diyebure.golia.domain.error.AuthException;
import com.diyebure.golia.domain.security.PasswordCredential;

import org.junit.Before;
import org.junit.Test;

import java.util.Base64;

/**
 * Pure-JVM unit tests for {@link ProfileRepositoryImpl#changePassword} (task 7.2).
 *
 * <p>These tests exercise the real credential-verification and re-hash path with
 * a genuine PBKDF2 hasher (so salt-freshness and verifiability are asserted for
 * real, not stubbed), against an in-memory {@link UserDao} fake that records the
 * columns actually persisted. The whole suite runs on the JVM because the
 * hasher's Base64 hooks are overridden with {@link java.util.Base64} (avoiding
 * {@code android.util.Base64}) and no Room/Android APIs are touched.
 *
 * <p>Validates: Requisitos R16.3, R16.4, R7.11, R7.12.
 */
public class ProfileRepositoryChangePasswordTest {

    private static final String USER_ID = "user-1";
    private static final String CURRENT_PASSWORD = "Curr3ntPass";
    private static final String NEW_PASSWORD = "N3wStrongPass";

    /**
     * Real PBKDF2 hasher usable on the JVM: only the Base64 hooks are swapped
     * for {@link java.util.Base64} so no {@code android.util.Base64} is needed.
     * Everything else (SecureRandom salt, PBKDF2 derivation, constant-time
     * verify) is the production implementation.
     */
    private static final class JvmPbkdf2PasswordHasher extends Pbkdf2PasswordHasher {
        @Override
        protected String encodeBase64(byte[] data) {
            return Base64.getEncoder().encodeToString(data);
        }

        @Override
        protected byte[] decodeBase64(String data) {
            return Base64.getDecoder().decode(data);
        }
    }

    /**
     * Minimal in-memory {@link UserDao} that stores a single user and records
     * how many times {@link #updateCredentials} was invoked and with which
     * column values.
     */
    private static final class FakeUserDao implements UserDao {
        UserEntity stored;
        int updateCredentialsCalls = 0;

        @Override
        public void insert(UserEntity user) {
            this.stored = user;
        }

        @Override
        public UserEntity findByEmail(String email) {
            return stored != null && email != null && email.equals(stored.getEmail()) ? stored : null;
        }

        @Override
        public UserEntity findByUsername(String username) {
            return stored != null && username != null && username.equals(stored.getUsername()) ? stored : null;
        }

        @Override
        public UserEntity findByUsernameOrEmail(String identifier) {
            if (stored == null || identifier == null) {
                return null;
            }
            return identifier.equals(stored.getEmail()) || identifier.equals(stored.getUsername())
                    ? stored : null;
        }

        @Override
        public boolean existsByEmail(String email) {
            return findByEmail(email) != null;
        }

        @Override
        public boolean existsByUsername(String username) {
            return findByUsername(username) != null;
        }

        @Override
        public UserEntity getById(String id) {
            return stored != null && id != null && id.equals(stored.getId()) ? stored : null;
        }

        @Override
        public void updatePersonalData(String id, String username, String email) {
            if (stored != null && stored.getId().equals(id)) {
                stored.setUsername(username);
                stored.setEmail(email);
            }
        }

        @Override
        public void updateCredentials(String id, String alg, int iter, String salt, String hash) {
            updateCredentialsCalls++;
            if (stored != null && stored.getId().equals(id)) {
                stored.setPasswordAlgorithm(alg);
                stored.setPasswordIterations(iter);
                stored.setPasswordSalt(salt);
                stored.setPasswordHash(hash);
            }
        }

        @Override
        public void updateAvatar(String id, String avatarUri) {
            if (stored != null && stored.getId().equals(id)) {
                stored.setAvatarUri(avatarUri);
            }
        }

        @Override
        public boolean existsByEmailExcludingId(String email, String selfId) {
            return stored != null && email != null && email.equals(stored.getEmail())
                    && !stored.getId().equals(selfId);
        }

        @Override
        public boolean existsByUsernameExcludingId(String username, String selfId) {
            return stored != null && username != null && username.equals(stored.getUsername())
                    && !stored.getId().equals(selfId);
        }
    }

    private FakeUserDao userDao;
    private JvmPbkdf2PasswordHasher hasher;
    private ProfileRepositoryImpl repository;

    @Before
    public void setUp() {
        userDao = new FakeUserDao();
        hasher = new JvmPbkdf2PasswordHasher();
        // PreferencesManager and PhotoStorage are not exercised by changePassword;
        // null is safe here because the code path under test never touches them.
        repository = new ProfileRepositoryImpl(userDao, hasher, (PreferencesManager) null,
                (PhotoStorage) null);

        // Seed a user whose stored credential is a real PBKDF2 hash of CURRENT_PASSWORD.
        PasswordCredential seedCredential = hasher.hash(CURRENT_PASSWORD);
        UserEntity entity = UserEntity.newUser(
                USER_ID,
                "Full Name",
                "john_doe",
                "john@example.com",
                seedCredential,
                123L);
        userDao.insert(entity);
    }

    // ---------------------------------------------------------------------
    // Case 1: wrong current password -> INVALID_CREDENTIALS, DB untouched
    // ---------------------------------------------------------------------

    @Test
    public void changePassword_wrongCurrentPassword_returnsInvalidCredentials_andDoesNotTouchCredentials() {
        String originalAlgorithm = userDao.stored.getPasswordAlgorithm();
        int originalIterations = userDao.stored.getPasswordIterations();
        String originalSalt = userDao.stored.getPasswordSalt();
        String originalHash = userDao.stored.getPasswordHash();

        Result<Void> result = repository.changePassword(USER_ID, "WrongPass1", NEW_PASSWORD);

        assertTrue("expected an error result", result.isError());
        assertEquals(AuthError.INVALID_CREDENTIALS, authErrorOf(result));

        // The stored credential columns must be unchanged and updateCredentials never called.
        assertEquals(0, userDao.updateCredentialsCalls);
        assertEquals(originalAlgorithm, userDao.stored.getPasswordAlgorithm());
        assertEquals(originalIterations, userDao.stored.getPasswordIterations());
        assertEquals(originalSalt, userDao.stored.getPasswordSalt());
        assertEquals(originalHash, userDao.stored.getPasswordHash());
    }

    // ---------------------------------------------------------------------
    // Case 2: correct current password -> success, new salt, verifiable
    // ---------------------------------------------------------------------

    @Test
    public void changePassword_correctCurrentPassword_succeeds_rehashesWithFreshSaltAndVerifies() {
        String oldSalt = userDao.stored.getPasswordSalt();
        String oldHash = userDao.stored.getPasswordHash();

        Result<Void> result = repository.changePassword(USER_ID, CURRENT_PASSWORD, NEW_PASSWORD);

        assertTrue("expected success", result.isSuccess());
        assertEquals(1, userDao.updateCredentialsCalls);

        // A fresh random salt must have been used (different from the old one).
        String newSalt = userDao.stored.getPasswordSalt();
        String newHash = userDao.stored.getPasswordHash();
        assertNotNull(newSalt);
        assertNotEquals("a fresh random salt must be generated", oldSalt, newSalt);
        assertNotEquals("re-hashing must change the stored hash", oldHash, newHash);

        // The persisted credential must verify against the NEW password...
        PasswordCredential persisted = new PasswordCredential(
                userDao.stored.getPasswordAlgorithm(),
                userDao.stored.getPasswordIterations(),
                userDao.stored.getPasswordSalt(),
                userDao.stored.getPasswordHash());
        assertTrue("new credential must verify the new password",
                hasher.verify(persisted, NEW_PASSWORD));
        // ...and must NOT verify against the old password anymore.
        assertFalse("old password must no longer verify",
                hasher.verify(persisted, CURRENT_PASSWORD));
    }

    // ---------------------------------------------------------------------
    // Case 3: entity not found -> SESSION_UNAVAILABLE
    // ---------------------------------------------------------------------

    @Test
    public void changePassword_userNotFound_returnsSessionUnavailable() {
        Result<Void> result = repository.changePassword("does-not-exist", CURRENT_PASSWORD, NEW_PASSWORD);

        assertTrue("expected an error result", result.isError());
        assertEquals(AuthError.SESSION_UNAVAILABLE, authErrorOf(result));
        assertEquals(0, userDao.updateCredentialsCalls);
    }

    // ---------------------------------------------------------------------
    // Case 4: plain text is never exposed (in the Result nor persisted columns)
    // ---------------------------------------------------------------------

    @Test
    public void changePassword_neverExposesPlainTextInResult() {
        Result<Void> result = repository.changePassword(USER_ID, CURRENT_PASSWORD, NEW_PASSWORD);

        assertTrue(result.isSuccess());
        // A successful password change carries no payload at all
        // (Result.Success<Void> with null data), so no password material can be
        // read out of the returned Result. (Result.toString() is deliberately not
        // used here: it dereferences the null Void payload.)
        assertNull("a Void success must not carry any payload",
                ((Result.Success<Void>) result).getData());
        assertNull(result.getOrNull());
        assertNull("a success result carries no error/exception", result.getErrorOrNull());
    }

    @Test
    public void changePassword_neverPersistsPlainTextInCredentialColumns() {
        repository.changePassword(USER_ID, CURRENT_PASSWORD, NEW_PASSWORD);

        String algorithm = userDao.stored.getPasswordAlgorithm();
        String salt = userDao.stored.getPasswordSalt();
        String hash = userDao.stored.getPasswordHash();

        // No persisted credential column may equal or contain either plain password.
        for (String column : new String[]{algorithm, salt, hash}) {
            assertNotNull(column);
            assertNotEquals(CURRENT_PASSWORD, column);
            assertNotEquals(NEW_PASSWORD, column);
            assertFalse("current password leaked in a credential column",
                    column.contains(CURRENT_PASSWORD));
            assertFalse("new password leaked in a credential column",
                    column.contains(NEW_PASSWORD));
        }

        // Sanity: the persisted hash/salt are genuine Base64 (not raw text).
        Base64.getDecoder().decode(salt);
        Base64.getDecoder().decode(hash);
    }

    @Test
    public void changePassword_wrongPassword_neverExposesPlainTextInResult() {
        Result<Void> result = repository.changePassword(USER_ID, "WrongPass1", NEW_PASSWORD);

        assertTrue(result.isError());
        // The typed error carries no password material, only the enum name.
        assertSame(AuthException.class, result.getErrorOrNull().getClass());
        String rendered = result.toString();
        assertFalse(rendered.contains("WrongPass1"));
        assertFalse(rendered.contains(NEW_PASSWORD));
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private static AuthError authErrorOf(Result<?> result) {
        Exception ex = result.getErrorOrNull();
        assertNotNull("expected an exception in the error result", ex);
        assertTrue("expected an AuthException, got " + ex.getClass(), ex instanceof AuthException);
        return ((AuthException) ex).getAuthError();
    }
}
