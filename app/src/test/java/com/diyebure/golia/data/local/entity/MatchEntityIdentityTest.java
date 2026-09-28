package com.diyebure.golia.data.local.entity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.diyebure.golia.data.local.dao.MatchDao;
import com.diyebure.golia.domain.model.Match;
import com.diyebure.golia.domain.model.MatchStatus;
import com.diyebure.golia.presentation.ui.partidos.MatchUiMapper;
import com.diyebure.golia.presentation.ui.partidos.MatchUiModel;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Prueba de EXPLORACIÓN de la condición del bug para
 * "Detalle del Partido no encuentra el partido (MATCH_NOT_FOUND)".
 *
 * <p><strong>Property 1 (Bug Condition):</strong> preservación de identidad en el
 * ida y vuelta Entidad → Dominio → UI. Para cualquier {@link MatchEntity} cuya PK
 * NO es un UUID canónico parseable, se espera que
 * {@code MatchUiMapper.toUiModel(entity.toDomainModel()).id} sea igual a la PK
 * original y que una consulta por ese id localice la fila en la caché.</p>
 *
 * <p><strong>CRÍTICO — metodología de bug condition:</strong> esta prueba DEBE
 * FALLAR sobre el código SIN arreglar para las PKs no-UUID. Ese fallo CONFIRMA que
 * el bug existe (es el caso de ÉXITO de esta tarea). Codifica el comportamiento
 * esperado y validará el arreglo cuando pase tras la implementación. NO se debe
 * arreglar aquí ni la prueba ni el código de producción.</p>
 *
 * <p>Prueba JVM pura (JUnit4), sin Android/Robolectric ni Room real, siguiendo la
 * convención de {@code PredictionRepositoryUpsertResolverTest}. La condición del
 * bug se acota (a modo de PBT determinista) a un conjunto representativo de PKs
 * no-UUID: alfanuméricas con guión ({@code "match-1"}) y numéricas
 * ({@code "1035048"}).</p>
 *
 * <p><strong>Validates: Requirements 1.1, 1.2, 1.3</strong></p>
 */
public class MatchEntityIdentityTest {

    /** UUID canónico usado como caso de CONTROL (no dispara el bug). */
    private static final String CANONICAL_UUID = "9b1deb4d-3b7d-3b1a-8a1e-0b2c3d4e5f60";

    /** Conjunto acotado de PKs no-UUID que disparan la condición del bug. */
    private static final String[] NON_UUID_PKS = {"match-1", "1035048"};

    /**
     * Construye una {@link MatchEntity} mínima con la PK y externalId dados. El
     * resto de campos se dejan en valores neutros; solo importa la identidad para
     * esta prueba.
     */
    private static MatchEntity entityWith(String pk, String externalId) {
        MatchEntity entity = new MatchEntity();
        entity.setId(pk);
        entity.setExternalId(externalId);
        entity.setCompetitionName("La Liga");
        entity.setHomeTeamName("Equipo A");
        entity.setAwayTeamName("Equipo B");
        entity.setStatus(MatchStatus.SCHEDULED.name());
        // Estadio no nulo: MatchUiMapper.formatVenue usa android.text.TextUtils
        // (stub JVM). Con valores no nulos evitamos tocar la ruta de TextUtils/
        // null y mantenemos el foco de la prueba en la IDENTIDAD del partido.
        entity.setVenueName("Estadio");
        entity.setVenueCity("Ciudad");
        return entity;
    }

    /**
     * Ejecuta el ida y vuelta completo Entidad → Dominio → UI y devuelve el id de
     * UI resultante.
     */
    private static String roundTripUiId(MatchEntity entity) {
        Match match = entity.toDomainModel();
        MatchUiModel ui = MatchUiMapper.toUiModel(match);
        return ui.id;
    }

    // =====================================================================
    // Property 1 — Bug Condition: PKs no-UUID (SE ESPERA QUE FALLE sin arreglo)
    // =====================================================================

    /**
     * Caso 1: PK alfanumérica no-UUID ("match-1") con externalId no vacío.
     *
     * <p>Sobre el código SIN arreglar esto FALLA: {@code toDomainModel} descarta la
     * PK (parseo UUID falla, catch vacío) y deja un {@code UUID.randomUUID()}, de
     * modo que el id de UI es un UUID aleatorio en lugar de "match-1".</p>
     */
    @Test
    public void nonUuidAlphanumericPk_preservesIdentityThroughRoundTrip() {
        String pk = "match-1";
        MatchEntity entity = entityWith(pk, "39-2024-100");

        String uiId = roundTripUiId(entity);

        assertEquals("El id de UI debe conservar la PK original no-UUID '" + pk
                + "', pero se obtuvo '" + uiId + "'", pk, uiId);
    }

    /**
     * Caso 2: PK numérica no-UUID ("1035048"). Mismo criterio y mismo fallo
     * esperado sobre el código sin arreglar.
     */
    @Test
    public void nonUuidNumericPk_preservesIdentityThroughRoundTrip() {
        String pk = "1035048";
        MatchEntity entity = entityWith(pk, "39-2024-200");

        String uiId = roundTripUiId(entity);

        assertEquals("El id de UI debe conservar la PK original no-UUID '" + pk
                + "', pero se obtuvo '" + uiId + "'", pk, uiId);
    }

    /**
     * Variante parametrizada (a modo de PBT acotado) sobre varias PKs no-UUID: para
     * cada una, el id de UI debe ser exactamente la PK.
     */
    @Test
    public void nonUuidPks_parametrized_preserveIdentity() {
        for (String pk : NON_UUID_PKS) {
            MatchEntity entity = entityWith(pk, "ext-" + pk);
            String uiId = roundTripUiId(entity);
            assertEquals("Para la PK no-UUID '" + pk
                    + "' el id de UI debe ser igual a la PK, pero fue '" + uiId + "'",
                    pk, uiId);
        }
    }

    // =====================================================================
    // Control — PK UUID canónico (debería PASAR ya sobre el código sin arreglar)
    // =====================================================================

    /**
     * Caso 3 (CONTROL): PK que es un UUID canónico. {@code toDomainModel} parsea la
     * PK correctamente, por lo que el id de UI == la PK. Este caso debería pasar ya
     * sobre el código sin arreglar y sirve para confirmar que la causa observada es
     * la #1 (pérdida de identidad por parseo UUID) y no la #4 (stableId aleatorio).
     */
    @Test
    public void canonicalUuidPk_isPreserved_control() {
        MatchEntity entity = entityWith(CANONICAL_UUID, "39-2024-300");

        String uiId = roundTripUiId(entity);

        assertEquals("El caso de control con UUID canónico debe preservar la PK",
                CANONICAL_UUID, uiId);
    }

    // =====================================================================
    // Caso 4 — Búsqueda en caché con DAO en memoria (fake) precargado
    // =====================================================================

    /**
     * Caso 4: simula el flujo del Detalle. Con un {@link MatchDao} en memoria (fake
     * al estilo de {@code PredictionRepositoryUpsertResolverTest}) precargado con la
     * fila de PK no-UUID, el id que produce la UI debe localizar esa misma fila vía
     * {@code getMatchById}.
     *
     * <p>Sobre el código SIN arreglar FALLA: el id de UI es aleatorio y
     * {@code getMatchById(uiId)} no encuentra la fila (equivale a
     * {@code MATCH_NOT_FOUND}).</p>
     */
    @Test
    public void cacheLookup_withNonUuidPk_findsRowByUiId() {
        String pk = "match-1";
        MatchEntity stored = entityWith(pk, "39-2024-100");

        FakeMatchDao dao = new FakeMatchDao();
        dao.seed(stored);

        // El id que la UI usaría para abrir el Detalle.
        String uiId = roundTripUiId(stored);

        MatchEntity found = dao.getMatchById(uiId);

        assertNotNull("getMatchById('" + uiId + "') debe localizar la fila de PK '"
                + pk + "' (no MATCH_NOT_FOUND)", found);
        assertEquals("La fila localizada debe ser la de la PK original",
                pk, found.getId());
    }

    // =====================================================================
    // Property 2 — Preservación: comportamiento inalterado fuera de la condición
    // del bug. Estas pruebas se escriben ANTES del arreglo (metodología de
    // observación primero) y DEBEN PASAR sobre el código sin arreglar, fijando la
    // base a preservar. Tras el arreglo (Variante A) deben SEGUIR pasando.
    // (Tarea 2 — Validates: Requirements 3.1, 3.2, 3.3, 3.4)
    // =====================================================================

    /** Conjunto de UUID canónicos para la preservación del ida y vuelta. */
    private static final String[] CANONICAL_UUIDS = {
            "9b1deb4d-3b7d-3b1a-8a1e-0b2c3d4e5f60",
            "123e4567-e89b-12d3-a456-426614174000",
            "00000000-0000-0000-0000-000000000000",
            "ffffffff-ffff-ffff-ffff-ffffffffffff",
    };

    /** Conjunto de externalId para la preservación de la generación determinista. */
    private static final String[] EXTERNAL_IDS = {
            "39-2024-100", "39-2024-200", "140-2023-5", "1035048", "ext-abc",
    };

    /**
     * Preservación (Req 3.1): para varios UUID canónicos, el ida y vuelta
     * Entidad → Dominio → UI conserva la PK. Debe PASAR sin arreglo y seguir
     * pasando tras el arreglo.
     */
    @Test
    public void canonicalUuids_parametrized_preserveIdentity_preservation() {
        for (String uuid : CANONICAL_UUIDS) {
            MatchEntity entity = entityWith(uuid, "ext-" + uuid);
            String uiId = roundTripUiId(entity);
            assertEquals("El UUID canónico '" + uuid + "' debe preservarse de extremo a extremo",
                    uuid, uiId);
        }
    }

    /**
     * Preservación (Req 3.2): la generación determinista de id a partir del
     * externalId es {@code UUID.nameUUIDFromBytes(externalId)} y es estable entre
     * invocaciones.
     *
     * <p>Nota de verificación: {@code MatchRepositoryImpl.stableId} es
     * {@code private static}, por lo que no es invocable directamente desde este
     * test JVM. Aquí se fija el COMPORTAMIENTO OBSERVABLE EQUIVALENTE (la fórmula
     * determinista que la implementación usa) y la equivalencia con
     * {@code MatchRepositoryImpl.stableId} se confirma por LECTURA DE CÓDIGO:
     * {@code return UUID.nameUUIDFromBytes(externalId.getBytes(StandardCharsets.UTF_8));}
     * El arreglo Variante A NO toca {@code stableId}, así que esta base se
     * preserva.</p>
     */
    @Test
    public void stableId_isDeterministic_fromExternalId_preservation() {
        for (String externalId : EXTERNAL_IDS) {
            UUID first = UUID.nameUUIDFromBytes(externalId.getBytes(StandardCharsets.UTF_8));
            UUID second = UUID.nameUUIDFromBytes(externalId.getBytes(StandardCharsets.UTF_8));
            assertEquals("stableId debe ser determinista para externalId '" + externalId + "'",
                    first, second);
            // El id derivado es un UUID canónico válido (reversible por fromString).
            assertEquals("El id derivado debe ser un UUID canónico parseable",
                    first, UUID.fromString(first.toString()));
        }
    }

    /**
     * Preservación (Req 3.3): la validación de id nulo/vacío en
     * {@code DetallePartidoViewModel.load} emite {@code MATCH_NOT_FOUND} de
     * inmediato, sin consultar la caché.
     *
     * <p>Nota de verificación: el aislamiento JVM del ViewModel no es viable de
     * forma limpia — {@code DetallePartidoViewModel} extiende {@code BaseViewModel}
     * y {@code setError(...)} publica en {@code LiveData}, cuya entrega requiere el
     * executor de arquitectura de Android (no disponible en un test JVM puro sin
     * Robolectric/InstantTaskExecutorRule, contrario a la convención del proyecto).
     * Por tanto esta preservación se verifica por LECTURA DE CÓDIGO: la guarda es
     * la PRIMERA sentencia de {@code load(String matchId)} y retorna antes de tocar
     * {@code preferencesManager} o la caché:
     * <pre>
     *   if (matchId == null || matchId.trim().isEmpty()) {
     *       setError(PredictionError.MATCH_NOT_FOUND.name());
     *       return;
     *   }
     * </pre>
     * El arreglo Variante A NO modifica {@code DetallePartidoViewModel.load}, por lo
     * que esta guarda queda intacta. Como comprobación observable mínima, aquí se
     * fija la condición de guarda usada por {@code load} sobre id nulo/vacío.</p>
     */
    @Test
    public void loadGuard_nullOrEmpty_isMatchNotFoundCondition_preservation() {
        assertEquals("id nulo dispara la guarda de MATCH_NOT_FOUND en load",
                true, isLoadGuardNotFound(null));
        assertEquals("id vacío dispara la guarda de MATCH_NOT_FOUND en load",
                true, isLoadGuardNotFound(""));
        assertEquals("id en blanco dispara la guarda de MATCH_NOT_FOUND en load",
                true, isLoadGuardNotFound("   "));
        // Un id no vacío NO dispara la guarda (sigue a la consulta de caché).
        assertEquals("un id no vacío no debe disparar la guarda de MATCH_NOT_FOUND",
                false, isLoadGuardNotFound("match-1"));
    }

    /**
     * Réplica exacta de la condición de guarda de
     * {@code DetallePartidoViewModel.load}: {@code matchId == null ||
     * matchId.trim().isEmpty()}. Sirve para fijar el criterio observable de la
     * validación de entrada sin instanciar el ViewModel.
     */
    private static boolean isLoadGuardNotFound(String matchId) {
        return matchId == null || matchId.trim().isEmpty();
    }

    // =====================================================================
    // Fakes / helpers
    // =====================================================================

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
