package com.diyebure.golia.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;

/**
 * Determinism tests for {@link SeedProfiles}: same output across two evaluations
 * of {@code all(now)} with the same {@code now}, and at least 3 participants
 * produced.
 *
 * <p>All tests inject a fixed literal {@code now} for determinism.</p>
 *
 * <b>Validates: Requirements 2.2, 2.6, 9.1</b>
 */
class SeedProfilesTest {

    private static final long NOW = RankingCalculatorTestSupport.NOW;

    private final Ranking_Calculator calculator = RankingCalculatorTestSupport.calculator();

    @Example
    void producesAtLeastThreeParticipants() {
        List<Ranking_Participant> participants = SeedProfiles.all(NOW);
        assertThat(participants).hasSizeGreaterThanOrEqualTo(3);
    }

    @Example
    void sameNowYieldsIdenticalParticipantsAcrossTwoEvaluations() {
        List<Ranking_Participant> first = SeedProfiles.all(NOW);
        List<Ranking_Participant> second = SeedProfiles.all(NOW);

        assertThat(first).hasSameSizeAs(second);
        for (int i = 0; i < first.size(); i++) {
            Ranking_Participant a = first.get(i);
            Ranking_Participant b = second.get(i);
            assertThat(a.getId()).isEqualTo(b.getId());
            assertThat(a.getDisplayName()).isEqualTo(b.getDisplayName());
            assertThat(a.getAvatarRef()).isEqualTo(b.getAvatarRef());
            assertThat(a.isCurrentUser()).isEqualTo(b.isCurrentUser());
            assertThat(a.getPredictions()).hasSameSizeAs(b.getPredictions());
        }
    }

    @Example
    void sameNowYieldsIdenticalMetricsAcrossTwoEvaluations() {
        List<Ranking_Participant> first = SeedProfiles.all(NOW);
        List<Ranking_Participant> second = SeedProfiles.all(NOW);

        for (int i = 0; i < first.size(); i++) {
            Ranking_Participant a = first.get(i);
            Ranking_Participant b = second.get(i);
            for (Ranking_Period period : Ranking_Period.values()) {
                assertThat(calculator.pointsForPeriod(a, period, NOW))
                        .isEqualTo(calculator.pointsForPeriod(b, period, NOW));
                assertThat(calculator.winPercentage(a, period, NOW))
                        .isEqualTo(calculator.winPercentage(b, period, NOW));
            }
            assertThat(calculator.streak(a)).isEqualTo(calculator.streak(b));
            assertThat(calculator.hasLive(a)).isEqualTo(calculator.hasLive(b));
        }
    }

    @Example
    void allSeedParticipantsAreNotCurrentUser() {
        for (Ranking_Participant p : SeedProfiles.all(NOW)) {
            assertThat(p.isCurrentUser()).isFalse();
        }
    }

    @Property(tries = 100)
    void determinsticForAnyFixedNow(@ForAll long now) {
        List<Ranking_Participant> first = SeedProfiles.all(now);
        List<Ranking_Participant> second = SeedProfiles.all(now);
        assertThat(first).hasSameSizeAs(second);
        for (int i = 0; i < first.size(); i++) {
            assertThat(first.get(i).getId()).isEqualTo(second.get(i).getId());
            assertThat(first.get(i).getPredictions())
                    .hasSameSizeAs(second.get(i).getPredictions());
        }
    }
}
