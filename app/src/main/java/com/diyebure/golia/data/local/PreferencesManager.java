package com.diyebure.golia.data.local;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Secure SharedPreferences wrapper for storing authentication tokens.
 * Uses Android Keystore for encryption of sensitive token data.
 *
 * <p>Injected as an application-scoped singleton via Hilt (constructor
 * injection). The former static {@code getInstance()} was removed so the DI
 * container owns its single instance; this removes hidden global state and
 * lets tests supply their own instance.
 */
@Singleton
public class PreferencesManager {
    private static final String PREFS_NAME = "golia_auth_prefs";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_TOKEN_EXPIRY = "token_expiry";

    // Encryption constants
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "golia_auth_key";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_SIZE = 12;
    private static final int TAG_SIZE = 128;

    private final SharedPreferences sharedPreferences;
    private final KeyStore keyStore;

    @Inject
    public PreferencesManager(@ApplicationContext Context context) {
        sharedPreferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        keyStore = initKeyStore();
    }

    /**
     * Initialize Android Keystore with AES key for encryption.
     */
    private KeyStore initKeyStore() {
        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);

            if (!keyStore.containsAlias(KEY_ALIAS)) {
                generateKey();
            }

            return keyStore;
        } catch (Exception e) {
            // If Keystore is not available, return null (will use plain storage)
            return null;
        }
    }

    /**
     * Generate AES key and store in Android Keystore.
     */
    private void generateKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(
                    android.security.keystore.KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);

            keyGenerator.init(new android.security.keystore.KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    android.security.keystore.KeyProperties.PURPOSE_ENCRYPT |
                            android.security.keystore.KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build());

            keyGenerator.generateKey();
        } catch (Exception e) {
            // Fall back to generating key without hardware backing
            // Will use software encryption
        }
    }

    /**
     * Encrypt a string value using AES-GCM.
     */
    private String encrypt(String plainText) {
        if (keyStore == null || plainText == null) {
            return plainText;
        }

        try {
            SecretKey key = (SecretKey) keyStore.getKey(KEY_ALIAS, null);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key);

            byte[] iv = cipher.getIV();
            byte[] encrypted = cipher.doFinal(plainText.getBytes());

            // Prepend IV to encrypted data
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

            return Base64.encodeToString(combined, Base64.NO_WRAP);
        } catch (Exception e) {
            return plainText; // Fall back to plain text if encryption fails
        }
    }

    /**
     * Decrypt an encrypted string value using AES-GCM.
     */
    private String decrypt(String encryptedText) {
        if (keyStore == null || encryptedText == null) {
            return encryptedText;
        }

        try {
            byte[] combined = Base64.decode(encryptedText, Base64.NO_WRAP);

            // Extract IV from combined data
            byte[] iv = new byte[IV_SIZE];
            System.arraycopy(combined, 0, iv, 0, IV_SIZE);

            // Extract encrypted data
            byte[] encrypted = new byte[combined.length - IV_SIZE];
            System.arraycopy(combined, IV_SIZE, encrypted, 0, encrypted.length);

            SecretKey key = (SecretKey) keyStore.getKey(KEY_ALIAS, null);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(TAG_SIZE, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);

            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted);
        } catch (Exception e) {
            return encryptedText; // Fall back to encrypted text if decryption fails
        }
    }

    /**
     * Save access token securely.
     */
    public void saveToken(String token) {
        String encrypted = encrypt(token);
        sharedPreferences.edit().putString(KEY_ACCESS_TOKEN, encrypted).apply();
    }

    /**
     * Get access token.
     */
    public String getToken() {
        String encrypted = sharedPreferences.getString(KEY_ACCESS_TOKEN, null);
        return decrypt(encrypted);
    }

    /**
     * Save refresh token securely.
     */
    public void saveRefreshToken(String refreshToken) {
        String encrypted = encrypt(refreshToken);
        sharedPreferences.edit().putString(KEY_REFRESH_TOKEN, encrypted).apply();
    }

    /**
     * Get refresh token.
     */
    public String getRefreshToken() {
        String encrypted = sharedPreferences.getString(KEY_REFRESH_TOKEN, null);
        return decrypt(encrypted);
    }

    /**
     * Save user ID.
     */
    public void saveUserId(String userId) {
        sharedPreferences.edit().putString(KEY_USER_ID, userId).apply();
    }

    /**
     * Get user ID.
     */
    public String getUserId() {
        return sharedPreferences.getString(KEY_USER_ID, null);
    }

    /**
     * Save user name securely.
     */
    public void saveUserName(String userName) {
        String encrypted = encrypt(userName);
        sharedPreferences.edit().putString(KEY_USER_NAME, encrypted).apply();
    }

    /**
     * Get user name.
     */
    public String getUserName() {
        String encrypted = sharedPreferences.getString(KEY_USER_NAME, null);
        return decrypt(encrypted);
    }

    /**
     * Save token expiry timestamp.
     */
    public void saveTokenExpiry(long expiryTimestamp) {
        sharedPreferences.edit().putLong(KEY_TOKEN_EXPIRY, expiryTimestamp).apply();
    }

    /**
     * Get token expiry timestamp.
     */
    public long getTokenExpiry() {
        return sharedPreferences.getLong(KEY_TOKEN_EXPIRY, 0);
    }

    /**
     * Check if access token is expired.
     */
    public boolean isTokenExpired() {
        long expiry = getTokenExpiry();
        if (expiry == 0) {
            // If no expiry saved, assume not expired
            return false;
        }
        return System.currentTimeMillis() >= expiry;
    }

    /**
     * Check if user is logged in.
     */
    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false) &&
                getToken() != null &&
                !getToken().isEmpty();
    }

    /**
     * Set logged in state.
     */
    public void setLoggedIn(boolean loggedIn) {
        sharedPreferences.edit().putBoolean(KEY_IS_LOGGED_IN, loggedIn).apply();
    }

    /**
     * Clear all authentication data (logout).
     */
    public void clearTokens() {
        sharedPreferences.edit()
                .remove(KEY_ACCESS_TOKEN)
                .remove(KEY_REFRESH_TOKEN)
                .remove(KEY_USER_ID)
                .remove(KEY_USER_NAME)
                .remove(KEY_IS_LOGGED_IN)
                .remove(KEY_TOKEN_EXPIRY)
                .apply();
    }

    /**
     * Get authorization header value.
     */
    public String getAuthorizationHeader() {
        String token = getToken();
        if (token != null && !token.isEmpty()) {
            return "Bearer " + token;
        }
        return null;
    }
}