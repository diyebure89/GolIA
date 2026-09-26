package com.diyebure.golia.domain.news;

import com.diyebure.golia.data.mapper.FixtureMapper;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

/**
 * Domain bridge between the numeric league id used by the Partidos feature
 * ({@link FixtureMapper#TARGET_LEAGUE_IDS} and its {@code LEAGUE_*} constants) and the
 * prioritized textual search terms that drive the news query ({@code q}).
 *
 * <p>This is the complementary source of truth required by Requirement 1.6: every league
 * in {@link FixtureMapper#TARGET_LEAGUE_IDS} must resolve to a non-empty, priority-ordered
 * list of terms (directly for individual leagues, or via {@link #WC_QUALIFICATION_IDS} for
 * the aggregated World Cup qualification chip, Requirements 1.4, 3.1).</p>
 *
 * <p>Empty or unknown ids never yield an empty result: they fall back to the aggregated
 * feed terms ({@link #aggregatedTerms()}), per Requirement 3.3.</p>
 *
 * <p>This class is a pure-Java domain artifact: it expresses <em>what</em> to search per
 * league, independent of the provider's query syntax. The construction of the provider
 * {@code q} string (OR operators, phrase quoting, length trimming) lives in
 * {@code NewsDataQueryBuilder} and is out of scope here (Requirement 4.6). Accordingly this
 * class has no Android nor provider dependencies.</p>
 */
public class LeagueNewsQuery {

    /** Textual aggregation key for the World Cup qualification (eliminatorias) chip. */
    public static final String KEY_WC_QUALIFICATION = "wc_qualification";

    /** Textual aggregation key for the Chip_Todos aggregated feed. */
    public static final String KEY_ALL = "all";

    /**
     * The seven World Cup qualification league ids ({@code {32,34,29,30,31,33,37}}). These
     * collapse conceptually into a single {@code wc_qualification} aggregate for both the
     * news query and the chip built by the ViewModel (task 11.2). Exposed as a public,
     * unmodifiable set so the ViewModel and tests can reference it (Requirements 1.4, 3.1).
     */
    public static final Set<Integer> WC_QUALIFICATION_IDS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    FixtureMapper.LEAGUE_WC_QUAL_EUROPE,        // 32
                    FixtureMapper.LEAGUE_WC_QUAL_SOUTH_AMERICA, // 34
                    FixtureMapper.LEAGUE_WC_QUAL_AFRICA,        // 29
                    FixtureMapper.LEAGUE_WC_QUAL_ASIA,          // 30
                    FixtureMapper.LEAGUE_WC_QUAL_CONCACAF,      // 31
                    FixtureMapper.LEAGUE_WC_QUAL_OCEANIA,       // 33
                    FixtureMapper.LEAGUE_WC_QUAL_PLAYOFFS       // 37
            )));

    /**
     * Prioritized terms for the World Cup qualification aggregate (descending priority).
     */
    private static final List<String> WC_QUALIFICATION_TERMS = Collections.unmodifiableList(
            Arrays.asList("Eliminatorias Mundial", "clasificación Mundial"));

    /**
     * Terms for the Chip_Todos aggregated feed (≤100 chars). Also used as the controlled
     * default for unknown/empty ids (Requirement 3.3).
     */
    private static final List<String> AGGREGATED_TERMS = Collections.unmodifiableList(
            Arrays.asList("fútbol"));

    /**
     * Immutable priority-ordered terms per individual league id (descending priority),
     * matching the initial table of the design (section "2. Tabla inicial de
     * LeagueNewsQuery"). The seven eliminatorias ids are intentionally absent here: they
     * are served via {@link #WC_QUALIFICATION_IDS} / {@link #WC_QUALIFICATION_TERMS}.
     */
    private static final Map<Integer, List<String>> LEAGUE_TERMS;

    static {
        Map<Integer, List<String>> terms = new HashMap<>();
        terms.put(FixtureMapper.LEAGUE_PREMIER_LEAGUE,
                Collections.unmodifiableList(Arrays.asList("Premier League")));
        terms.put(FixtureMapper.LEAGUE_LA_LIGA,
                Collections.unmodifiableList(Arrays.asList("LaLiga", "Primera División")));
        terms.put(FixtureMapper.LEAGUE_SERIE_A,
                Collections.unmodifiableList(Arrays.asList("Serie A")));
        terms.put(FixtureMapper.LEAGUE_BUNDESLIGA,
                Collections.unmodifiableList(Arrays.asList("Bundesliga")));
        terms.put(FixtureMapper.LEAGUE_LIGUE_1,
                Collections.unmodifiableList(Arrays.asList("Ligue 1")));
        terms.put(FixtureMapper.LEAGUE_COLOMBIA_PRIMERA_A,
                Collections.unmodifiableList(Arrays.asList("Liga BetPlay", "Primera A Colombia")));
        terms.put(FixtureMapper.LEAGUE_CHAMPIONS_LEAGUE,
                Collections.unmodifiableList(Arrays.asList("Champions League")));
        terms.put(FixtureMapper.LEAGUE_UEFA_NATIONS_LEAGUE,
                Collections.unmodifiableList(Arrays.asList("Nations League")));
        LEAGUE_TERMS = Collections.unmodifiableMap(terms);
    }

    @Inject
    public LeagueNewsQuery() {
        // No-arg Hilt-injectable constructor; term tables are static and immutable.
    }

    /**
     * Resolves the prioritized search terms for a league id (Requirements 1.4, 1.6, 3.1,
     * 3.3). Resolution order:
     * <ol>
     *   <li>If the id is in {@link #WC_QUALIFICATION_IDS} &rarr; the eliminatorias aggregate
     *       terms.</li>
     *   <li>Else if it is a known individual league &rarr; its priority list.</li>
     *   <li>Else &rarr; the aggregated feed terms ({@link #aggregatedTerms()}), so no id
     *       ever yields an empty list (Requirement 3.3).</li>
     * </ol>
     *
     * @param leagueId the numeric league id (e.g. a {@code FixtureMapper.LEAGUE_*} value)
     * @return a non-null, non-empty, unmodifiable list of terms in descending priority
     */
    public List<String> termsFor(int leagueId) {
        if (WC_QUALIFICATION_IDS.contains(leagueId)) {
            return WC_QUALIFICATION_TERMS;
        }
        List<String> terms = LEAGUE_TERMS.get(leagueId);
        if (terms != null && !terms.isEmpty()) {
            return terms;
        }
        return aggregatedTerms();
    }

    /**
     * Returns the Chip_Todos aggregated feed terms (Requirements 3.3, 3.5).
     *
     * @return a non-null, non-empty, unmodifiable list of aggregated terms
     */
    public List<String> aggregatedTerms() {
        return AGGREGATED_TERMS;
    }

    /**
     * Reports whether the given league id resolves to a non-empty terms list, either
     * directly as an individual league or via {@link #WC_QUALIFICATION_IDS}. Used by the
     * coverage test of Requirement 1.6 (task 5.4).
     *
     * @param leagueId the numeric league id
     * @return {@code true} when the id resolves to a non-empty, league-specific terms list
     */
    public boolean hasTermsFor(int leagueId) {
        if (WC_QUALIFICATION_IDS.contains(leagueId)) {
            return !WC_QUALIFICATION_TERMS.isEmpty();
        }
        List<String> terms = LEAGUE_TERMS.get(leagueId);
        return terms != null && !terms.isEmpty();
    }
}
