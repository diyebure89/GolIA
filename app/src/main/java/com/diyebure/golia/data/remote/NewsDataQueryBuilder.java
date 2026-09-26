package com.diyebure.golia.data.remote;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/**
 * Translates the prioritized list of {@code Terminos_Consulta_Liga} produced by
 * {@code LeagueNewsQuery} (domain layer) into the concrete {@code q} query string
 * syntax expected by the NewsData.io provider.
 *
 * <p>This component isolates all provider-specific query syntax (the {@code OR}
 * operator, phrase quoting for multi-word terms, and URL encoding concerns) from
 * the domain layer, satisfying Requirement 4.6: the domain
 * {@code Repositorio_Noticias} hands over a plain priority-ordered list of terms
 * and never builds provider syntax itself.
 *
 * <p><b>Construction rules (Requirement 3.2 / 3.5):</b>
 * <ol>
 *   <li>Terms are concatenated in descending priority order (the first element of
 *       the input list is the highest priority) joined by the provider's
 *       {@code OR} operator.</li>
 *   <li>Multi-word terms are wrapped in phrase quotes ({@code "..."}) so the
 *       provider treats them as an exact phrase; single-word terms are emitted
 *       verbatim.</li>
 *   <li>The length of the {@code q} parameter is measured in characters
 *       <em>before</em> any URL encoding.</li>
 *   <li>WHILE the built string exceeds {@link #MAX_QUERY_LENGTH} characters, the
 *       lowest-priority term is removed (on a tie, the term with the higher index
 *       in the list), until the length is {@code <= 100}. At least the
 *       highest-priority term is always preserved, even if it alone still exceeds
 *       the limit.</li>
 * </ol>
 *
 * <p>Because terms are appended in list order and only trailing (lowest-priority)
 * terms are dropped, "remove the lowest-priority term (tie-break: higher index)"
 * reduces to removing the last remaining element of the working list.
 *
 * <p>This class is pure Java (no Android nor Retrofit dependencies) so it can be
 * unit- and property-tested in isolation.
 *
 * <p>Requirements: 3.2, 3.5, 4.6.
 */
public class NewsDataQueryBuilder {

    /**
     * Maximum allowed length of the {@code q} parameter, measured in characters
     * before URL encoding. NewsData.io's free plan limits keyword queries to 100
     * characters (Requirement 3.2 / 3.5).
     */
    public static final int MAX_QUERY_LENGTH = 100;

    /** Provider operator used to combine query terms. */
    private static final String OR_OPERATOR = " OR ";

    /** Character used to wrap multi-word terms as an exact phrase. */
    private static final char PHRASE_QUOTE = '"';

    @Inject
    public NewsDataQueryBuilder() {
        // No dependencies: term formatting is stateless.
    }

    /**
     * Builds the provider {@code q} string from a prioritized list of terms.
     *
     * @param prioritizedTerms terms in descending priority order (index 0 is the
     *                         highest priority). {@code null} or empty entries are
     *                         ignored; a {@code null} list yields an empty string.
     * @return the {@code q} string, {@code <= 100} characters whenever the
     *         highest-priority term alone fits, otherwise just the (possibly
     *         over-length) highest-priority term. Never {@code null}.
     */
    public String build(List<String> prioritizedTerms) {
        List<String> formatted = formatTerms(prioritizedTerms);
        if (formatted.isEmpty()) {
            return "";
        }

        // Remove trailing (lowest-priority, highest-index) terms while the joined
        // query exceeds the limit, always keeping at least the highest-priority one.
        List<String> working = new ArrayList<>(formatted);
        String query = join(working);
        while (query.length() > MAX_QUERY_LENGTH && working.size() > 1) {
            working.remove(working.size() - 1);
            query = join(working);
        }
        return query;
    }

    /**
     * Normalizes the input into formatted, provider-ready term tokens, preserving
     * priority order and skipping blank/null entries.
     */
    private List<String> formatTerms(List<String> prioritizedTerms) {
        List<String> formatted = new ArrayList<>();
        if (prioritizedTerms == null) {
            return formatted;
        }
        for (String term : prioritizedTerms) {
            if (term == null) {
                continue;
            }
            String trimmed = term.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            formatted.add(formatTerm(trimmed));
        }
        return formatted;
    }

    /**
     * Wraps multi-word terms in phrase quotes; single-word terms are returned as-is.
     */
    private String formatTerm(String term) {
        if (term.indexOf(' ') >= 0) {
            return PHRASE_QUOTE + term + PHRASE_QUOTE;
        }
        return term;
    }

    /**
     * Joins the already-formatted terms with the provider {@code OR} operator.
     */
    private String join(List<String> terms) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < terms.size(); i++) {
            if (i > 0) {
                sb.append(OR_OPERATOR);
            }
            sb.append(terms.get(i));
        }
        return sb.toString();
    }
}
