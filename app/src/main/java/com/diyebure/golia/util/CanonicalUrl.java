package com.diyebure.golia.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Utility that computes a canonical form of an article URL and a stable identifier
 * derived from it.
 *
 * <p>Two URLs that point at the same article but differ only in scheme case, host
 * case, tracking parameters ({@code utm_*}, {@code fbclid}, ...), fragment, query
 * ordering or a redundant trailing slash canonicalize to the same string, so their
 * {@link #articleId(String)} matches. This lets the news cache treat such variants
 * as a single article (Requirement 13.3).
 *
 * <p>Canonicalization steps (see design "Algoritmo de URL canónica"):
 * <ol>
 *   <li>Trim and lowercase the scheme and host.</li>
 *   <li>Promote {@code http} to {@code https} when possible.</li>
 *   <li>Remove tracking query parameters.</li>
 *   <li>Remove the fragment ({@code #...}).</li>
 *   <li>Sort the remaining query parameters alphabetically and drop a redundant
 *       trailing slash on the path.</li>
 * </ol>
 *
 * <p>The article identifier is the hex-encoded SHA-256 of the canonical URL, chosen
 * over {@code String.hashCode()} to avoid practical collisions.
 *
 * <p>This is a plain Java class with no Android dependencies, so it is unit-testable
 * on the JVM. It is stateless and safe to share across threads.
 *
 * <p>Implements requirement 13.3 of the football-news-feed spec.
 */
public final class CanonicalUrl {

    /**
     * Query parameter names considered tracking noise and stripped during
     * canonicalization. Compared case-insensitively.
     */
    private static final Set<String> TRACKING_PARAMS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "utm_source",
            "utm_medium",
            "utm_campaign",
            "utm_term",
            "utm_content",
            "fbclid",
            "gclid",
            "mc_cid",
            "mc_eid",
            "ref",
            "igshid"
    )));

    private CanonicalUrl() {
        // Utility class; not instantiable.
    }

    /**
     * Computes the canonical form of the given URL.
     *
     * <p>When the input cannot be parsed as a URI, it is returned trimmed and
     * unchanged so that callers always get a non-null, deterministic value they can
     * hash.
     *
     * @param rawUrl the raw article URL; may be {@code null}
     * @return the canonical URL, or {@code null} when {@code rawUrl} is {@code null}
     */
    public static String canonicalize(String rawUrl) {
        if (rawUrl == null) {
            return null;
        }
        String trimmed = rawUrl.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }

        final URI uri;
        try {
            uri = new URI(trimmed);
        } catch (URISyntaxException e) {
            // Not a parseable URI: return it trimmed so hashing stays deterministic.
            return trimmed;
        }

        String scheme = uri.getScheme();
        if (scheme == null) {
            // Without a scheme we cannot safely reassemble the URI; keep it trimmed.
            return trimmed;
        }
        scheme = scheme.trim().toLowerCase(Locale.ROOT);
        // Promote http -> https.
        if ("http".equals(scheme)) {
            scheme = "https";
        }

        String host = uri.getHost();
        if (host != null) {
            host = host.trim().toLowerCase(Locale.ROOT);
        }

        String path = uri.getPath();
        if (path == null) {
            path = "";
        }
        // Drop a redundant trailing slash (but keep a lone "/" root as empty).
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        String sortedQuery = canonicalizeQuery(uri.getRawQuery());

        StringBuilder sb = new StringBuilder();
        sb.append(scheme).append("://");
        if (uri.getRawUserInfo() != null) {
            sb.append(uri.getRawUserInfo()).append('@');
        }
        if (host != null) {
            sb.append(host);
        }
        if (uri.getPort() != -1) {
            sb.append(':').append(uri.getPort());
        }
        sb.append(path);
        if (sortedQuery != null && !sortedQuery.isEmpty()) {
            sb.append('?').append(sortedQuery);
        }
        // Fragment is intentionally dropped.
        return sb.toString();
    }

    /**
     * Computes the stable article identifier for the given URL: the hex-encoded
     * SHA-256 of its canonical form.
     *
     * @param rawUrl the raw article URL; may be {@code null}
     * @return the lowercase hex SHA-256 of the canonical URL, or {@code null} when
     * {@code rawUrl} is {@code null}
     */
    public static String articleId(String rawUrl) {
        String canonical = canonicalize(rawUrl);
        if (canonical == null) {
            return null;
        }
        return sha256Hex(canonical);
    }

    /**
     * Removes tracking parameters from the raw query string and sorts the remaining
     * parameters alphabetically, preserving each parameter's raw (encoded) form.
     *
     * @param rawQuery the raw query string (without the leading {@code ?}); may be {@code null}
     * @return the canonical query string, or {@code null} when there is no query
     */
    private static String canonicalizeQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.isEmpty()) {
            return null;
        }
        List<String> kept = new ArrayList<>();
        for (String pair : rawQuery.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int eq = pair.indexOf('=');
            String name = eq >= 0 ? pair.substring(0, eq) : pair;
            if (TRACKING_PARAMS.contains(name.toLowerCase(Locale.ROOT))) {
                continue;
            }
            kept.add(pair);
        }
        if (kept.isEmpty()) {
            return null;
        }
        Collections.sort(kept);
        return String.join("&", kept);
    }

    /**
     * Computes the lowercase hex-encoded SHA-256 digest of the given text.
     *
     * @param text the text to hash; must be non-null
     * @return the 64-character lowercase hex digest
     */
    private static String sha256Hex(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                int value = b & 0xFF;
                if (value < 0x10) {
                    hex.append('0');
                }
                hex.append(Integer.toHexString(value));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is required by every Java platform; this should never happen.
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
