package com.diyebure.golia.data.security;

import android.util.Base64;

import com.diyebure.golia.domain.security.PasswordCredential;
import com.diyebure.golia.domain.security.PasswordHasher;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * PBKDF2 implementation of {@link PasswordHasher} for the data layer.
 *
 * <p>Uses {@code PBKDF2WithHmacSHA256} with a 16-byte random salt (via
 * {@link SecureRandom}), 120000 iterations and a 256-bit derived key. The salt
 * and hash are stored as Base64.
 *
 * <p>Base64 encoding/decoding is isolated behind the {@link #encodeBase64(byte[])}
 * and {@link #decodeBase64(String)} methods so the class can be exercised in a
 * pure JVM test (where {@code android.util.Base64} is not available) by
 * overriding those methods. Production uses {@code android.util.Base64} with the
 * {@code NO_WRAP} flag.
 */
@Singleton
public class Pbkdf2PasswordHasher implements PasswordHasher {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 120_000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_BYTES = 16;

    private final SecureRandom secureRandom = new SecureRandom();

    @Inject
    public Pbkdf2PasswordHasher() {
    }

    @Override
    public PasswordCredential hash(String plainPassword) {
        byte[] salt = new byte[SALT_BYTES];
        secureRandom.nextBytes(salt);
        byte[] hash = pbkdf2(plainPassword.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS);
        return new PasswordCredential(
                ALGORITHM,
                ITERATIONS,
                encodeBase64(salt),
                encodeBase64(hash));
    }

    @Override
    public boolean verify(PasswordCredential credential, String plainPassword) {
        byte[] salt = decodeBase64(credential.getSaltBase64());
        byte[] expected = decodeBase64(credential.getHashBase64());
        // Derive the key length from the stored hash so credentials created with
        // different parameters keep verifying.
        int keyLengthBits = expected.length * 8;
        byte[] actual = pbkdf2(
                plainPassword.toCharArray(),
                salt,
                credential.getIterations(),
                keyLengthBits);
        // Constant-time comparison; does not reveal why it fails.
        return MessageDigest.isEqual(expected, actual);
    }

    private byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLengthBits) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLengthBits);
            try {
                SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
                return factory.generateSecret(spec).getEncoded();
            } finally {
                spec.clearPassword();
            }
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 no disponible", e);
        }
    }

    /**
     * Encodes bytes to Base64. Production uses {@code android.util.Base64} with
     * {@code NO_WRAP}. Overridable so the class can be tested on a pure JVM.
     */
    protected String encodeBase64(byte[] data) {
        return Base64.encodeToString(data, Base64.NO_WRAP);
    }

    /**
     * Decodes a Base64 string to bytes. Production uses {@code android.util.Base64}
     * with {@code NO_WRAP}. Overridable so the class can be tested on a pure JVM.
     */
    protected byte[] decodeBase64(String data) {
        return Base64.decode(data, Base64.NO_WRAP);
    }
}
