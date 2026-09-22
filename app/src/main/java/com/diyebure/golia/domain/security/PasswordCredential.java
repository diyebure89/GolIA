package com.diyebure.golia.domain.security;

import androidx.annotation.NonNull;

/**
 * Immutable value object grouping the four self-describing parameters of a
 * password credential. Lives in the domain layer so {@link PasswordHasher}
 * stays independent of Room and Android APIs.
 */
public final class PasswordCredential {

    private final String algorithm;    // e.g. "PBKDF2WithHmacSHA256"
    private final int iterations;      // e.g. 120000
    private final String saltBase64;   // 16 bytes -> Base64
    private final String hashBase64;   // 256 bits -> Base64

    public PasswordCredential(String algorithm, int iterations,
                              String saltBase64, String hashBase64) {
        this.algorithm = algorithm;
        this.iterations = iterations;
        this.saltBase64 = saltBase64;
        this.hashBase64 = hashBase64;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public int getIterations() {
        return iterations;
    }

    public String getSaltBase64() {
        return saltBase64;
    }

    public String getHashBase64() {
        return hashBase64;
    }

    @NonNull
    @Override
    public String toString() {
        // Never expose salt/hash content in logs; only structural metadata.
        return "PasswordCredential{" +
                "algorithm='" + algorithm + '\'' +
                ", iterations=" + iterations +
                '}';
    }
}
