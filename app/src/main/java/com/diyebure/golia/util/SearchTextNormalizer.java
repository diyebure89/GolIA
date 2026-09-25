package com.diyebure.golia.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Utility that normalizes free-text used for match searching.
 *
 * <p>Normalization lowercases the input and strips diacritics/accents so that
 * accent-insensitive, case-insensitive comparisons are possible. For example,
 * both {@code "Atletico"} and {@code "Atlético"} normalize to {@code "atletico"}.
 *
 * <p>The transformation is idempotent: {@code normalize(normalize(s)).equals(normalize(s))}.
 *
 * <p>This is a plain Java class with no Android dependencies, so it is unit-testable
 * on the JVM. It is stateless and therefore safe to share across threads.
 *
 * <p>Implements requirement 10.4 (Requisito 10) of the matches-live-fixtures spec.
 */
public class SearchTextNormalizer {

    /**
     * Matches Unicode combining marks (accents/diacritics) produced when a string
     * is decomposed with {@link Normalizer.Form#NFD}.
     */
    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");

    /**
     * Normalizes the given input for accent- and case-insensitive comparison.
     *
     * <p>Steps:
     * <ol>
     *   <li>Lowercase using {@link Locale#ROOT} to keep results locale-independent.</li>
     *   <li>Decompose accented characters using Unicode NFD.</li>
     *   <li>Remove all combining marks ({@code \p{M}}).</li>
     * </ol>
     *
     * <p>Null handling: a {@code null} input returns {@code null}. Callers comparing
     * two normalized values should treat {@code null} explicitly rather than relying
     * on it matching an empty string.
     *
     * @param input the raw text to normalize; may be {@code null}
     * @return the normalized text, or {@code null} when {@code input} is {@code null}
     */
    public String normalize(String input) {
        if (input == null) {
            return null;
        }
        String lowered = input.toLowerCase(Locale.ROOT);
        String decomposed = Normalizer.normalize(lowered, Normalizer.Form.NFD);
        return COMBINING_MARKS.matcher(decomposed).replaceAll("");
    }
}
