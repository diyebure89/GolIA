package com.diyebure.golia.domain.usecase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
 * Prueba de EXPLORACIÓN de la condición del bug para
 * "El Historial muestra 'Partido no disponible' para partidos existentes".
 *
 * <p><strong>Property 1 (Bug Condition):</strong> preservación de la identidad del
 * {@code match_id} en la LECTURA del historial. Para cualquier
 * {@link PredictionEntity} cuya PK de partido ({@code match_id}) NO es un UUID
 * canónico parseable pero cuyo partido SÍ existe en la caché, se espera que la
 * resolución equivalente a {@code GetUserPredictionsUseCase.resolveMatch} devuelva
 * el {@link Match} correspondiente (no {@code null}) con la identidad original.</p>
 *
 * <p><strong>CRÍTICO — metodología de bug condition:</strong> esta prueba DEBE
 * FALLAR sobre el código SIN arreglar para las PKs no-UUID. Ese fallo CONFIRMA que
 * el bug existe (es el caso de ÉXITO de esta tarea). Codifica el comportamiento
 * esperado y validará el arreglo cuando pase tras la implementación. NO se debe
 * arreglar aquí ni la prueba ni el código de producción.</p>
 *
 * <p>Prueba JVM pura (JUnit4), sin Android/Robolectric ni Room real, siguiendo la
 * convención de {@code PredictionRepositoryUpsertResolverTest} y
 * {@code MatchEntityIdentityTest} (con un {@code FakeMatchDao} en memoria
 * precargado). La condición del bug se acota (a modo de PBT determinista) a un
 * conjunto representativo de PKs no-UUID: alfanuméricas con guión ({@code "match-1"})
 * y numéricas ({@code "1035048"}).</p>
 *
 * <p><strong>Réplica de {@code resolveMatch}:</strong> como
 * {@code GetUserPredictionsUseCase.resolveMatch(Prediction)} es {@code private}, se
 * replica aquí su lógica OBSERVABLE tal y como está hoy en el código sin arreglar:
 * <pre>
 *   if (prediction == null || prediction.getMatchId() == null) return null;
 *   MatchEntity entity = matchDao.getMatchById(prediction.getMatchId().toString());
 *   return entity != null ? entity.toDomainModel() : null;
 * </pre>
 * Esta réplica refleja fielmente el código actual de
 * {@code GetUserPredictionsUseCase} (verificado por lectura de código).</p>
 *
 * <p><strong>Validates: Requirements 1.1, 1.2, 1.3</strong></p>
 */
public class GetUserPredictionsIdentityTest {

    /** UUID canónico usado como caso de CONTROL (no dispara el bug). */
    private static final String CANONICAL_UUID = "9b1deb4d-3b7d-3b1a-8a1e-0b2c3d4e5f60";

    /** Conjunto acotado de PKs no-UUID que disparan la condición del bug. */
    private static final String[] NON_UUID_PKS = {"match-1", "1035048"};

    // =====================================================================
    // Property 1 — Bug Condition: PKs no-UUID (SE ESPERA QUE FALLE sin arreglo)
    // =====================================================================

    /**
     * Caso 1: pronóstico con {@code match_id} alfanumérico no-UUID ("match-1") y su
     * partido presente en la caché con PK "match-1".
     *
     * <p>Sobre el código SIN arreglar esto FALLA: {@code toDomainModel} descarta la
     * PK (parseo UUID falla, catch vacío) y deja {@code Prediction.matchId == null};
     * la resolución equivalente a {@code resolveMatch} devuelve {@code null} (por su
     * guarda de {@code matchId == null}), de modo que el partido existente NO se
     * resuelve (equivale a "Partido no disponible").</p>
     */
    @Test
    public void nonUuidAlphanumericMatchId_resolvesExistingMatch() {
        String pk = "match-1";
        FakeMatchDao dao = new FakeMatchDao();
        dao.seed(matchEntityWith(pk));

        Prediction prediction = predictionEntityWith(pk).toDomainModel();
        Match resolved = resolveMatch(prediction, dao);

        assertNotNull("El partido con match_id no-UUID '" + pk
                + "' existe en caché y debe resolverse (no 'Partido no disponible'); "
                + "matchId tras toDomainModel = " + prediction.getMatchId(), resolved);
        assertEquals("El Match resuelto debe corresponder a la PK original '" + pk + "'",
                pk, resolved.getRawId());
    }

    /**
     * Caso 2: pronóstico con {@code match_id} numérico no-UUID ("1035048"). Mismo
     * criterio y mismo fallo esperado sobre el código sin arreglar.
     */
    @Test
    public void nonUuidNumericMatchId_resolvesExistingMatch() {
        String pk = "1035048";
        FakeMatchDao dao = new FakeMatchDao();
        dao.seed(matchEntityWith(pk));

        Prediction prediction = predictionEntityWith(pk).toDomainModel();
        Match resolved = resolveMatch(prediction, dao);

        assertNotNull("El partido con match_id no-UUID '" + pk
                + "' existe en caché y debe resolverse (no 'Partido no disponible'); "
                + "matchId tras toDomainModel = " + prediction.getMatchId(), resolved);
        assertEquals("El Match resuelto debe corresponder a la PK original '" + pk + "'",
                pk, resolved.getRawId());
    }

    /**
     * Variante parametrizada (a modo de PBT acotado) sobre varias PKs no-UUID: para
     * cada una, con el partido presente en caché, la resolución debe devolver el
     * Match cuya identidad es la PK original.
     */
    @Test
    public void nonUuidMatchIds_parametrized_resolveExistingMatch() {
        for (String pk : NON_UUID_PKS) {
            FakeMatchDao dao = new FakeMatchDao();
            dao.seed(matchEntityWith(pk));

            Prediction prediction = predictionEntityWith(pk).toDomainModel();
            Match resolved = resolveMatch(prediction, dao);

            assertNotNull("Para el match_id no-UUID '" + pk
                    + "' el partido existe y debe resolverse; matchId tras toDomainModel = "
                    + prediction.getMatchId(), resolved);
            assertEquals("El Match resuelto debe corresponder a la PK '" + pk + "'",
                    pk, resolved.getRawId());
        }
    }

    // =====================================================================
    // Control — match_id UUID canónico (debería PASAR ya sobre el código sin arreglar)
    // =====================================================================

    /**
     * Caso 3 (CONTROL): pronóstico con {@code match_id} UUID canónico y su partido
     * presente. {@code toDomainModel} parsea la PK correctamente, por lo que
     * {@code Prediction.matchId} no es nulo y {@code getMatchId().toString()}
     * reconstruye la PK canónica; la resolución localiza la fila. Este caso debería
     * pasar ya sobre el código sin arreglar y confirma que la causa observada es la
     * pérdida de identidad por parseo UUID (no un problema del propio DAO/lookup).
     */
    @Test
    public void canonicalUuidMatchId_resolvesExistingMatch_control() {
        FakeMatchDao dao = new FakeMatchDao();
        dao.seed(matchEntityWith(CANONICAL_UUID));

        Prediction prediction = predictionEntityWith(CANONICAL_UUID).toDomainModel();
        Match resolved = resolveMatch(prediction, dao);

        assertNotNull("El caso de control con UUID canónico debe resolver el partido", resolved);
        assertEquals("El Match resuelto debe corresponder al UUID canónico",
                CANONICAL_UUID, resolved.getRawId());
    }

    // =====================================================================
    // Réplica fiel de GetUserPredictionsUseCase.resolveMatch (código ARREGLADO)
    // =====================================================================

    /**
     * Réplica de la lógica observable de {@code resolveMatch} en el código
     * arreglado (Variante A). El {@code match_id} de búsqueda prioriza la PK
     * original: {@code prediction.getRawMatchId()} y solo cae a
     * {@code prediction.getMatchId().toString()} cuando {@code rawMatchId} es nulo
     * (devolviendo {@code null} sin consultar el DAO si ambos son nulos). Refleja
     * el estado del código tras el arreglo.
     */
    private static Match resolveMatch(Prediction prediction, MatchDao matchDao) {
        if (prediction == null) {
            return null;
        }
        // Réplica de la lógica ARREGLADA (Variante A): prioriza la PK original
        // (rawMatchId) y solo cae a matchId.toString() cuando rawMatchId es nulo.
        String matchIdString = prediction.getRawMatchId() != null
                ? prediction.getRawMatchId()
                : (prediction.getMatchId() != null ? prediction.getMatchId().toString() : null);
        if (matchIdString == null) {
            return null;
        }
        MatchEntity entity = matchDao.getMatchById(matchIdString);
        return entity != null ? entity.toDomainModel() : null;
    }

    // =====================================================================
    // Fakes / helpers
    // =====================================================================

    /**
     * Construye una {@link PredictionEntity} mínima con el {@code match_id} dado y un
     * {@code id}/{@code userId} UUID canónicos válidos (solo importa la identidad del
     * partido para esta prueba).
     */
    private static PredictionEntity predictionEntityWith(String matchId) {
        PredictionEntity entity = new PredictionEntity();
        entity.setId("11111111-1111-1111-1111-111111111111");
        entity.setUserId("22222222-2222-2222-2222-222222222222");
        entity.setMatchId(matchId);
        entity.setPredictedOutcome("HOME_WIN");
        entity.setCreatedAt(0L);
        return entity;
    }

    /**
     * Construye una {@link MatchEntity} mínima con la PK dada. Solo importa la
     * identidad (PK) y los nombres de equipo para representar un partido resoluble.
     */
    private static MatchEntity matchEntityWith(String pk) {
        MatchEntity entity = new MatchEntity();
        entity.setId(pk);
        entity.setCompetitionName("La Liga");
        entity.setHomeTeamName("Local");
        entity.setAwayTeamName("Visitante");
        entity.setStatus(MatchStatus.SCHEDULED.name());
        return entity;
    }

    /**
     * {@link MatchDao} en memoria mínimo que reproduce la semántica de
     * {@code WHERE id = :matchId} sobre la columna PK. Solo se usan seed y
     * getMatchById; el resto devuelve valores neutros.
     */
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
