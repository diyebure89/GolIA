package com.diyebure.golia.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.database.sqlite.SQLiteConstraintException;

import com.diyebure.golia.data.local.dao.MatchDao;
import com.diyebure.golia.data.local.dao.PredictionDao;
import com.diyebure.golia.data.local.entity.MatchEntity;
import com.diyebure.golia.data.local.entity.PredictionEntity;
import com.diyebure.golia.domain.common.Result;
import com.diyebure.golia.domain.model.Match;
import com.diyebure.golia.domain.model.MatchStatus;
import com.diyebure.golia.domain.model.Prediction;
import com.diyebure.golia.domain.model.PredictionOutcome;
import com.diyebure.golia.domain.model.Prediction_Scorer;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Required repository/resolver tests for {@link PredictionRepositoryImpl}
 * (R11.4, R11.5).
 *
 * <p>Two behaviours are pinned down:</p>
 * <ul>
 *   <li><strong>Upsert uniqueness (R11.5 / R5.9 / R6.1 / R6.4):</strong> a
 *   second prediction of the same {@code (user_id, match_id)} replaces the
 *   previous one instead of creating a duplicate row, and the surviving row
 *   carries the latest scores/outcome.</li>
 *   <li><strong>Resolver idempotency (R11.4 / R9.7):</strong> once a prediction
 *   is resolved, reprocessing the same finished match leaves
 *   {@code pointsEarned}, {@code isCorrect} and {@code finalizedAt}
 *   unchanged.</li>
 * </ul>
 *
 * <p>These run as pure JVM tests (no Android/Robolectric, no real Room DB),
 * following the {@code ProfileRepositoryChangePasswordTest} convention: an
 * in-memory {@link FakePredictionDao} faithfully reproduces the two SQLite
 * behaviours the production code relies on — {@code @Insert(REPLACE)} that
 * evicts a row conflicting on the unique {@code (user_id, match_id)} index, and
 * {@code @Update} by primary key.</p>
 */
public class PredictionRepositoryUpsertResolverTest {

    private static final String USER_ID = "user-1";
    private static final String OTHER_USER_ID = "user-2";
    private static final String MATCH_ID = "match-1";

    private FakePredictionDao predictionDao;
    private FakeMatchDao matchDao;
    private PredictionRepositoryImpl repository;

    @Before
    public void setUp() {
        predictionDao = new FakePredictionDao();
        matchDao = new FakeMatchDao();
        repository = new PredictionRepositoryImpl(predictionDao, matchDao, new Prediction_Scorer());
    }

    // =====================================================================
    // R11.5 — upsert uniqueness by (user_id, match_id)
    // =====================================================================

    @Test
    public void savePrediction_secondSaveForSamePair_replacesWithoutDuplicating() {
        Result<Prediction> first = repository.savePrediction(USER_ID, MATCH_ID, 1, 0);
        assertTrue("first save should succeed", first.isSuccess());

        // Exactly one row so far and it captured the first scores.
        assertEquals(1, predictionDao.rowCount());
        String firstId = predictionDao.getUserPredictionForMatch(USER_ID, MATCH_ID).getId();

        // Second save for the SAME (user_id, match_id) but different scores.
        Result<Prediction> second = repository.savePrediction(USER_ID, MATCH_ID, 2, 3);
        assertTrue("second save should succeed", second.isSuccess());

        // Still exactly one row for that pair -> no duplicate created.
        assertEquals("a second prediction must not create a duplicate row",
                1, predictionDao.rowCount());
        assertEquals("still only one row for the (user, match) pair",
                1, predictionDao.countForPair(USER_ID, MATCH_ID));

        // The surviving row carries the LATEST scores and derived outcome, and
        // reuses the original primary key (upsert, not a fresh insert).
        PredictionEntity surviving = predictionDao.getUserPredictionForMatch(USER_ID, MATCH_ID);
        assertNotNull(surviving);
        assertEquals(firstId, surviving.getId());
        assertEquals(Integer.valueOf(2), surviving.getPredictedHomeScore());
        assertEquals(Integer.valueOf(3), surviving.getPredictedAwayScore());
        assertEquals(PredictionOutcome.AWAY_WIN.name(), surviving.getPredictedOutcome());
        // Re-saving keeps it pending.
        assertNull(surviving.getIsCorrect());
        assertEquals(0, surviving.getPointsEarned());
    }

    @Test
    public void savePrediction_differentUsersSameMatch_areNotMerged() {
        assertTrue(repository.savePrediction(USER_ID, MATCH_ID, 1, 0).isSuccess());
        assertTrue(repository.savePrediction(OTHER_USER_ID, MATCH_ID, 0, 2).isSuccess());

        // The unique constraint is on the PAIR; two different users may each
        // hold one prediction for the same match.
        assertEquals(2, predictionDao.rowCount());
        assertEquals(1, predictionDao.countForPair(USER_ID, MATCH_ID));
        assertEquals(1, predictionDao.countForPair(OTHER_USER_ID, MATCH_ID));
    }

    @Test
    public void savePrediction_conflictOnUniqueIndex_resolvesAsUpdateOfExistingRow() {
        // Seed a pre-existing row for the pair under some primary key, mirroring
        // a row that already owns the unique (user_id, match_id) slot.
        String existingId = UUID.randomUUID().toString();
        predictionDao.seed(new PredictionEntity(
                existingId, USER_ID, MATCH_ID,
                PredictionOutcome.DRAW.name(), 1, 1, 0, null, 100L, null));

        // Force the insert path to raise a unique-index conflict (as if the
        // repository generated a different PK). The repository must recover by
        // updating the existing row rather than surfacing an error.
        predictionDao.failNextInsertWithConflict = true;

        Result<Prediction> result = repository.savePrediction(USER_ID, MATCH_ID, 4, 2);

        assertTrue("upsert must recover from a unique-index conflict", result.isSuccess());
        assertEquals("conflict recovery must not duplicate the pair",
                1, predictionDao.countForPair(USER_ID, MATCH_ID));

        PredictionEntity surviving = predictionDao.getUserPredictionForMatch(USER_ID, MATCH_ID);
        assertEquals("must reuse the existing row's id", existingId, surviving.getId());
        assertEquals("created_at of the existing row must be preserved",
                100L, surviving.getCreatedAt());
        assertEquals(Integer.valueOf(4), surviving.getPredictedHomeScore());
        assertEquals(Integer.valueOf(2), surviving.getPredictedAwayScore());
        assertEquals(PredictionOutcome.HOME_WIN.name(), surviving.getPredictedOutcome());
    }

    // =====================================================================
    // R11.4 — resolver idempotency
    // =====================================================================

    @Test
    public void resolveForMatch_alreadyResolvedPrediction_isNotRecalculated() {
        // The resolver keys predictions by match.getId().toString(); save the
        // prediction under that exact string so it is found.
        UUID matchUuid = UUID.randomUUID();
        String matchIdStr = matchUuid.toString();
        assertTrue(repository.savePrediction(USER_ID, matchIdStr, 2, 1).isSuccess());

        Match finished = finishedMatch(matchUuid, 2, 1);

        // First resolution: computes and persists the score.
        assertTrue(repository.resolveForMatch(finished).isSuccess());

        PredictionEntity afterFirst = predictionDao.getUserPredictionForMatch(USER_ID, matchIdStr);
        int resolvedPoints = afterFirst.getPointsEarned();
        Boolean resolvedIsCorrect = afterFirst.getIsCorrect();
        Long resolvedFinalizedAt = afterFirst.getFinalizedAt();

        // Sanity: an exact 2-1 prediction earns the maximum (19) and is correct.
        assertEquals(19, resolvedPoints);
        assertEquals(Boolean.TRUE, resolvedIsCorrect);
        assertNotNull(resolvedFinalizedAt);

        int updatesAfterFirst = predictionDao.updateCalls;

        // Second resolution of the SAME match must be a no-op for the already
        // resolved prediction: no recomputation, no re-persist.
        assertTrue(repository.resolveForMatch(finished).isSuccess());

        PredictionEntity afterSecond = predictionDao.getUserPredictionForMatch(USER_ID, matchIdStr);
        assertEquals("pointsEarned must not change on reprocessing",
                resolvedPoints, afterSecond.getPointsEarned());
        assertSame("isCorrect must not change on reprocessing",
                resolvedIsCorrect, afterSecond.getIsCorrect());
        assertEquals("finalizedAt must not change on reprocessing",
                resolvedFinalizedAt, afterSecond.getFinalizedAt());
        assertEquals("an already-resolved prediction must not be updated again",
                updatesAfterFirst, predictionDao.updateCalls);
    }

    @Test
    public void resolveForMatch_wrongPrediction_resolvesAsIncorrectAndStaysStable() {
        UUID matchUuid = UUID.randomUUID();
        String matchIdStr = matchUuid.toString();
        // Prediction 0-0 against an actual 2-1 earns nothing -> incorrect.
        assertTrue(repository.savePrediction(USER_ID, matchIdStr, 0, 0).isSuccess());

        Match finished = finishedMatch(matchUuid, 2, 1);
        assertTrue(repository.resolveForMatch(finished).isSuccess());

        PredictionEntity afterFirst = predictionDao.getUserPredictionForMatch(USER_ID, matchIdStr);
        assertEquals(0, afterFirst.getPointsEarned());
        assertEquals(Boolean.FALSE, afterFirst.getIsCorrect());
        assertNotNull(afterFirst.getFinalizedAt());

        int updatesAfterFirst = predictionDao.updateCalls;
        Long finalizedAt = afterFirst.getFinalizedAt();

        // Reprocess: a resolved-as-incorrect prediction is still resolved
        // (isCorrect != null) and must be left untouched.
        assertTrue(repository.resolveForMatch(finished).isSuccess());

        PredictionEntity afterSecond = predictionDao.getUserPredictionForMatch(USER_ID, matchIdStr);
        assertEquals(0, afterSecond.getPointsEarned());
        assertEquals(Boolean.FALSE, afterSecond.getIsCorrect());
        assertEquals(finalizedAt, afterSecond.getFinalizedAt());
        assertEquals("a resolved-incorrect prediction must not be updated again",
                updatesAfterFirst, predictionDao.updateCalls);
    }

    // =====================================================================
    // Helpers / fakes
    // =====================================================================

    private static Match finishedMatch(UUID id, int homeScore, int awayScore) {
        Match match = new Match();
        match.setId(id);
        match.setStatus(MatchStatus.FINISHED);
        match.setHomeScore(homeScore);
        match.setAwayScore(awayScore);
        return match;
    }

    /**
     * In-memory {@link PredictionDao} that reproduces the SQLite semantics the
     * repository depends on.
     */
    private static final class FakePredictionDao implements PredictionDao {
        private final List<PredictionEntity> rows = new ArrayList<>();
        int updateCalls = 0;
        boolean failNextInsertWithConflict = false;

        void seed(PredictionEntity entity) {
            rows.add(copyOf(entity));
        }

        int rowCount() {
            return rows.size();
        }

        int countForPair(String userId, String matchId) {
            int count = 0;
            for (PredictionEntity row : rows) {
                if (eq(row.getUserId(), userId) && eq(row.getMatchId(), matchId)) {
                    count++;
                }
            }
            return count;
        }

        @Override
        public void insertPrediction(PredictionEntity prediction) {
            if (failNextInsertWithConflict) {
                failNextInsertWithConflict = false;
                throw new SQLiteConstraintException(
                        "UNIQUE constraint failed: predictions.user_id, predictions.match_id");
            }
            // @Insert(REPLACE): SQLite REPLACE first deletes any row that
            // conflicts on the primary key OR on a unique index, then inserts.
            removeByPrimaryKey(prediction.getId());
            removeByUniquePair(prediction.getUserId(), prediction.getMatchId());
            rows.add(copyOf(prediction));
        }

        @Override
        public void insertPredictions(List<PredictionEntity> predictions) {
            if (predictions != null) {
                for (PredictionEntity p : predictions) {
                    insertPrediction(p);
                }
            }
        }

        @Override
        public void updatePrediction(PredictionEntity prediction) {
            updateCalls++;
            // @Update matches by primary key.
            for (int i = 0; i < rows.size(); i++) {
                if (eq(rows.get(i).getId(), prediction.getId())) {
                    rows.set(i, copyOf(prediction));
                    return;
                }
            }
        }

        @Override
        public PredictionEntity getUserPredictionForMatch(String userId, String matchId) {
            for (PredictionEntity row : rows) {
                if (eq(row.getUserId(), userId) && eq(row.getMatchId(), matchId)) {
                    return copyOf(row);
                }
            }
            return null;
        }

        @Override
        public List<PredictionEntity> getPredictionsByMatch(String matchId) {
            List<PredictionEntity> result = new ArrayList<>();
            for (PredictionEntity row : rows) {
                if (eq(row.getMatchId(), matchId)) {
                    result.add(copyOf(row));
                }
            }
            return result;
        }

        @Override
        public PredictionEntity getPredictionById(String predictionId) {
            for (PredictionEntity row : rows) {
                if (eq(row.getId(), predictionId)) {
                    return copyOf(row);
                }
            }
            return null;
        }

        @Override
        public List<PredictionEntity> getPredictionsByUser(String userId) {
            List<PredictionEntity> result = new ArrayList<>();
            for (PredictionEntity row : rows) {
                if (eq(row.getUserId(), userId)) {
                    result.add(copyOf(row));
                }
            }
            return result;
        }

        @Override
        public List<PredictionEntity> getPredictionsByStatus(Boolean isCorrect) {
            return new ArrayList<>();
        }

        @Override
        public List<PredictionEntity> getUserPredictionsByStatus(String userId, Boolean isCorrect) {
            return new ArrayList<>();
        }

        @Override
        public List<PredictionEntity> getUserPredictionsSorted(String userId) {
            return getPredictionsByUser(userId);
        }

        @Override
        public List<PredictionEntity> getUserPredictionsByPoints(String userId) {
            return getPredictionsByUser(userId);
        }

        @Override
        public List<PredictionEntity> getUserPendingPredictions(String userId) {
            return new ArrayList<>();
        }

        @Override
        public List<PredictionEntity> getUserFinalizedPredictions(String userId) {
            return new ArrayList<>();
        }

        @Override
        public int getUserPredictionCount(String userId) {
            return getPredictionsByUser(userId).size();
        }

        @Override
        public int getUserCorrectPredictionCount(String userId) {
            return 0;
        }

        @Override
        public int getUserTotalPoints(String userId) {
            return 0;
        }

        @Override
        public void deletePrediction(PredictionEntity prediction) {
            removeByPrimaryKey(prediction.getId());
        }

        @Override
        public void deletePredictionById(String predictionId) {
            removeByPrimaryKey(predictionId);
        }

        @Override
        public void deleteAllUserPredictions(String userId) {
            rows.removeIf(row -> eq(row.getUserId(), userId));
        }

        @Override
        public void deleteAllPredictions() {
            rows.clear();
        }

        @Override
        public boolean userHasPredictionForMatch(String userId, String matchId) {
            return getUserPredictionForMatch(userId, matchId) != null;
        }

        private void removeByPrimaryKey(String id) {
            rows.removeIf(row -> eq(row.getId(), id));
        }

        private void removeByUniquePair(String userId, String matchId) {
            rows.removeIf(row -> eq(row.getUserId(), userId) && eq(row.getMatchId(), matchId));
        }

        private static boolean eq(String a, String b) {
            return a == null ? b == null : a.equals(b);
        }

        /** Defensive copy so tests observe stored state, not live references. */
        private static PredictionEntity copyOf(PredictionEntity e) {
            return new PredictionEntity(
                    e.getId(), e.getUserId(), e.getMatchId(),
                    e.getPredictedOutcome(), e.getPredictedHomeScore(),
                    e.getPredictedAwayScore(), e.getPointsEarned(), e.getIsCorrect(),
                    e.getCreatedAt(), e.getFinalizedAt());
        }
    }

    /**
     * Minimal in-memory {@link MatchDao}. Not exercised by the paths under test
     * (the repository's save/resolve only touch {@link PredictionDao}), so the
     * methods return empty/neutral values.
     */
    private static final class FakeMatchDao implements MatchDao {
        @Override public List<MatchEntity> getUpcomingMatches() { return new ArrayList<>(); }
        @Override public List<MatchEntity> getLiveMatches() { return new ArrayList<>(); }
        @Override public MatchEntity getMatchById(String matchId) { return null; }
        @Override public List<MatchEntity> getFinishedMatchesByTeam(String teamId) { return new ArrayList<>(); }
        @Override public MatchEntity getMatchByExternalId(String externalId) { return null; }
        @Override public List<MatchEntity> getMatchesByCompetition(String competitionId) { return new ArrayList<>(); }
        @Override public List<MatchEntity> getMatchesByDateRange(long startTime, long endTime) { return new ArrayList<>(); }
        @Override public List<MatchEntity> getAllMatches() { return new ArrayList<>(); }
        @Override public void insertMatch(MatchEntity match) { }
        @Override public void insertMatches(List<MatchEntity> matches) { }
        @Override public void updateMatch(MatchEntity match) { }
        @Override public void deleteOldMatches(long cutoffTime) { }
        @Override public void deleteAllMatches() { }
        @Override public void deleteMatchById(String matchId) { }
        @Override public int getMatchCount() { return 0; }
    }
}
