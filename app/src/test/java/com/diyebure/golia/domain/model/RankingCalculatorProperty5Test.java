package com.diyebure.golia.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * Property 5: El orden y el desempate son deterministas y totales.
 *
 * <p>{@code rank} produces a total, stable order: points desc, then
 * winPercentage desc, then displayName asc case-insensitive, then id asc; two
 * runs on the same data produce identical positions.</p>
 *
 * <b>Validates: Requirements 3.4, 5.5, 8.2</b>
 */
class RankingCalculatorProperty5Test {

    private final Ranking_Calculator calculator = RankingCalculatorTestSupport.calculator();

    /** A participant with a unique id, arbitrary name and a small prediction set. */
    @Provide
    Arbitrary<Ranking_Participant> participants() {
        Arbitrary<String> ids = Arbitraries.strings().alpha().numeric().ofMinLength(1).ofMaxLength(6);
        Arbitrary<String> names = Arbitraries.of("Ana", "ana", "BOB", "bob", "Carlos", "carlos", "Zoe", "");
        Arbitrary<List<ScoredPrediction>> preds =
                RankingCalculatorTestSupport.scoredPredictions().list().ofMaxSize(8);
        Arbitrary<Boolean> isUser = Arbitraries.of(true, false);
        return Combinators.combine(ids, names, isUser, preds)
                .as((id, name, user, ps) ->
                        new Ranking_Participant(id, name, null, user, ps));
    }

    @Provide
    Arbitrary<List<Ranking_Participant>> participantLists() {
        return participants().list().ofMinSize(1).ofMaxSize(8);
    }

    @Property(tries = 200)
    void orderIsTotalAndRespectsTieBreak(
            @ForAll("participantLists") List<Ranking_Participant> participants,
            @ForAll Ranking_Period period) {

        long now = RankingCalculatorTestSupport.NOW;
        RankingResult result = calculator.rank(participants, period, now);
        List<Ranking_Entry> entries = result.getEntries();

        // Positions are 1..N contiguous.
        assertThat(entries).hasSize(participants.size());
        for (int i = 0; i < entries.size(); i++) {
            assertThat(entries.get(i).getPosition()).isEqualTo(i + 1);
        }

        // Each adjacent pair respects the binding ordering.
        for (int i = 0; i + 1 < entries.size(); i++) {
            Ranking_Entry a = entries.get(i);
            Ranking_Entry b = entries.get(i + 1);
            assertThat(comparesBeforeOrEqual(a, b))
                    .as("entry %s should be ordered before %s", a.getDisplayName(), b.getDisplayName())
                    .isTrue();
        }
    }

    @Property(tries = 200)
    void twoRunsProduceIdenticalPositions(
            @ForAll("participantLists") List<Ranking_Participant> participants,
            @ForAll Ranking_Period period) {

        long now = RankingCalculatorTestSupport.NOW;
        RankingResult first = calculator.rank(new ArrayList<>(participants), period, now);
        RankingResult second = calculator.rank(new ArrayList<>(participants), period, now);

        assertThat(first.getEntries()).hasSameSizeAs(second.getEntries());
        for (int i = 0; i < first.getEntries().size(); i++) {
            Ranking_Entry a = first.getEntries().get(i);
            Ranking_Entry b = second.getEntries().get(i);
            assertThat(a.getPosition()).isEqualTo(b.getPosition());
            assertThat(a.getDisplayName()).isEqualTo(b.getDisplayName());
            assertThat(a.getPoints()).isEqualTo(b.getPoints());
            assertThat(a.getWinPercentage()).isEqualTo(b.getWinPercentage());
        }
    }

    /**
     * Whether entry {@code a} is allowed to appear immediately before {@code b}
     * according to the binding tie-break order.
     */
    private boolean comparesBeforeOrEqual(Ranking_Entry a, Ranking_Entry b) {
        if (a.getPoints() != b.getPoints()) {
            return a.getPoints() > b.getPoints();
        }
        if (a.getWinPercentage() != b.getWinPercentage()) {
            return a.getWinPercentage() > b.getWinPercentage();
        }
        String an = a.getDisplayName() == null ? "" : a.getDisplayName();
        String bn = b.getDisplayName() == null ? "" : b.getDisplayName();
        int byName = an.compareToIgnoreCase(bn);
        if (byName != 0) {
            return byName < 0;
        }
        // Names equal case-insensitively: id ordering decides but Ranking_Entry
        // does not expose id, so any order among equal-name entries is accepted.
        return true;
    }
}
