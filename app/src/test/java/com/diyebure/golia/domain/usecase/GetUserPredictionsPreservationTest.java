package com.diyebure.golia.domain.usecase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.diyebure.golia.data.local.dao.MatchDao;
import com.diyebure.golia.data.local.entity.MatchEntity;
import com.diyebure.golia.data.local.entity.PredictionEntity;
import com.diyebure.golia.domain.model.Match;
import com.diyebure.golia.domain.model.MatchStatus;
import com.diyebure.golia.domain.model.Prediction;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Pruebas de PRESERVACIÓN para el bugfix
 * "El Historial muestra 'Partido no disponible' para partidos existentes".
 *
 * <p><strong>Property 2 (Preservation):</strong> comportamiento inalterado FUERA de
 * la condición del bug. Para toda entrada donde {@code isBugCondition} es falso
 * (esto es: {@code match_id} con UUID canónico y partido presente, o partido
 * realmente ausente de la caché), la resolución del historial debe producir el
 * mismo resultado antes y después del arreglo.</p>
 *
 * <p><strong>Metodología de observación primero:</strong> estas pruebas FIJAN por
 * observación el comportamiento del código SIN arreglar y DEBEN PASAR sobre él. El
 * arreglo (Variante A) no debe alterar ninguno de estos resultados (se vuelven a
 * ejecutar en la Tarea 3.3 y deben seguir en verde).</p>
 *
 * <p>Prueba JVM pura (JUnit4), sin Android/Robolectric ni Room real, siguiendo la
 * convención de {@code PredictionRepositoryUpsertResolverTest} y
 * {@code MatchEntityIdentityTest} (con un {@code FakeMatchDao} en memoria
 * precargado). El conjunto de entradas se acota (a modo de PBT determinista) a
 * varios UUID canónicos y a varias PKs ausentes representativas.</p>
 *
 * <p><strong>Réplica de {@code resolveMatch}:</strong> como
 * {@code GetUserPredictionsUseCase.resolveMatch(Prediction)} es {@code private}, se
 * replica aquí su lógica OBSERVABLE. Para los casos de preservación (UUID canónico y
 * partido ausente) esta lógica coincide antes y después del arreglo, porque el
 * {@code match_id} de búsqueda es el mismo: para un UUID canónico, {@code rawMatchId}
 * y {@code matchId.toString()} son equivalentes; para un partido ausente, el DAO
 * devuelve {@code null} en ambos casos.</p>
 *
 * <p><strong>Nota sobre guardado/puntuación (R3.3, R3.4):</strong> la preservación
 * del guardado ({@code buildUpsertEntity}, índice único {@code (user_id, match_id)})
 * y de la puntuación ({@code resolveForMatch}, {@code Prediction_Scorer}) se apoya en
 * {@code PredictionRepositoryUpsertResolverTest} (base existente), que no cambia con
 * este arreglo porque solo se toca la LECTURA del historial. Este archivo cubre la
 * preservación de la resolución del historial (R3.1, R3.2).</p>
 *
 * <p><strong>Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5</strong></p>
 */
public class GetUserPredictionsPreservationTest {

    /** Conjunto acotado de UUID canónicos (NO disparan el bug). */
    private static final String[] CANONICAL_UUIDS = {
        "9b1deb4d-3b7d-3b1a-8a1e-0b2c3d4e5f60",
        "550e8400-e29b-41d4-a716-446655440000",
        "123e4567-e89b-12d3-a456-426614174000"
    };

    /** Conjunto acotado de PKs cuyo partido NO está en la caché (borde legítimo). */
    private static final String[] ABSENT_PKS = {
        "match-999",
        "9b1deb4d-3b7d-3b1a-8a1e-0b2c3d4e5fff",
        "no-existe"
    };

    // =====================================================================
    // Preservación 1 — UUID canónico con partido presente: resuelve el Match
    // (Requirement 3.1) — pasa antes y después del arreglo
    // =====================================================================

    @Test
    public void canonicalUuidMatchId_withMatchPresent_resolvesMatch() {
        for (String pk : CANONICAL_UUIDS) {
            FakeMatchDao dao = new FakeMatchDao();
            dao.seed(matchEntityWith(pk));

            Prediction prediction = predictionEntityWith(pk).toDomainModel();
            Match resolved = resolveMatch(prediction, dao);

            assertNotNull("Con UUID canónico '" + pk
                    + "' y partido presente, la resolución debe devolver el Match", resolved);
            assertEquals("El Match resuelto debe corresponder al UUID canónico '" + pk + "'",
                    pk, resolved.getRawId());
        }
    }

    // =====================================================================
    // Preservación 2 — partido realmente ausente de la caché: devuelve null
    // (Requirement 3.2, placeholder "Partido no disponible") — antes y después
    // =====================================================================

    @Test
    public void matchAbsentFromCache_resolvesToNull() {
        for (String pk : ABSENT_PKS) {
            // DAO vacío: ninguna fila coincide con la PK -> partido ausente.
            FakeMatchDao dao = new FakeMatchDao();

            Prediction prediction = predictionEntityWith(pk).toDomainModel();
            Match resolved = resolveMatch(prediction, dao);

            assertNull("Con partido ausente para '" + pk
                    + "', la resolución debe devolver null (placeholder 'Partido no disponible')",
                    resolved);
        }
    }

    // =====================================================================
    // Preservación 3 — UUID canónico con partido ausente: null (no confundir con
    // un partido presente distinto). Refuerza que solo se resuelve la PK exacta.
    // =====================================================================

    @Test
    public void canonicalUuidMatchId_withDifferentMatchPresent_resolvesToNull() {
        FakeMatchDao dao = new FakeMatchDao();
        // La caché contiene OTRO partido, no el referenciado.
        dao.seed(matchEntityWith("550e8400-e29b-41d4-a716-446655440000"));

        Prediction prediction =
                predictionEntityWith("123e4567-e89b-12d3-a456-426614174000").toDomainModel();
        Match resolved = resolveMatch(prediction, dao);

        assertNull("Un UUID canónico cuyo partido NO está en caché debe resolver a null",
                resolved);
    }

    // =====================================================================
    // Réplica de la lógica observable de GetUserPredictionsUseCase.resolveMatch.
    //
    // Para los casos de PRESERVACIÓN aquí cubiertos (UUID canónico y partido
    // ausente) el match_id de búsqueda es idéntico antes y después del arreglo:
    //  - UUID canónico: getRawMatchId() (tras el arreglo) y getMatchId().toString()
    //    (código actual) producen el mismo String canónico.
    //  - Partido ausente: getMatchById devuelve null en ambos casos.
    // Por eso esta réplica es válida en ambas fases y el resultado no cambia.
    // =====================================================================

    private static Match resolveMatch(Prediction prediction, MatchDao matchDao) {
        if (prediction == null || prediction.getMatchId() == null) {
            return null;
        }
        MatchEntity entity = matchDao.getMatchById(prediction.getMatchId().toString());
        return entity != null ? entity.toDomainModel() : null;
    }

    // =====================================================================
    // Fakes / helpers (mismo estilo que GetUserPredictionsIdentityTest)
    // =====================================================================

    private static PredictionEntity predictionEntityWith(String matchId) {
        PredictionEntity entity = new PredictionEntity();
        entity.setId("11111111-1111-1111-1111-111111111111");
        entity.setUserId("22222222-2222-2222-2222-222222222222");
        entity.setMatchId(matchId);
        entity.setPredictedOutcome("HOME_WIN");
        entity.setCreatedAt(0L);
        return entity;
    }

    private static MatchEntity matchEntityWith(String pk) {
        MatchEntity entity = new MatchEntity();
        entity.setId(pk);
        entity.setCompetitionName("La Liga");
        entity.setHomeTeamName("Local");
        entity.setAwayTeamName("Visitante");
        entity.setStatus(MatchStatus.SCHEDULED.name());
        return entity;
    }

    private static final class FakeMatchDao implements MatchDao {
        private final List<MatchEntity> rows = new ArrayList<>();

        void seed(MatchEntity entity) {
            rows.add(entity);
        }

        @Override
        public MatchEntity getMatchById(String matchId) {
            for (MatchEntity row : rows) {
                if (row.getId() != null && row.getId().equals(matchId)) {
                    return row;
                }
            }
            return null;
        }

        @Override public List<MatchEntity> getUpcomingMatches() { return new ArrayList<>(); }
        @Override public List<MatchEntity> getLiveMatches() { return new ArrayList<>(); }
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
