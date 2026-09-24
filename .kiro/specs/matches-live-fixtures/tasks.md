# Implementation Plan: Matches Live Fixtures

## Overview

Este plan implementa la Pantalla_Partidos de GolIA con fixtures reales de API-Football, respetando la arquitectura del proyecto (Clean Architecture + MVVM + Hilt + Room + Retrofit, 100% Java, sin Kotlin ni coroutines). Se sigue la secuenciación del diseño para evitar retrabajo: (1) contrato del proveedor y reescritura del cliente, (2) ampliación de datos y migración Room, (3) infraestructura, (4) refactor del repositorio, (5) casos de uso, (6) presentación, (7) UI/Layout, (8) wiring Hilt.

Cada tarea es incremental y construye sobre las anteriores. Las sub-tareas de pruebas están integradas junto a la implementación que validan. Las tareas marcadas con `*` son opcionales (no bloquean el MVP funcional) y NO se implementan automáticamente. Los paquetes usan la base `com.diyebure.golia`.

## Tasks

- [ ] 1. Contrato del proveedor API-Football y reescritura del cliente
  - [ ] 1.1 Configurar la API_Key vía BuildConfig
    - Añadir en `app/build.gradle` la lectura de `API_FOOTBALL_KEY` desde `local.properties` y exponerla como `buildConfigField "String", "API_FOOTBALL_KEY", ...`
    - Añadir la clave a `local.properties` (no versionado) y asegurar que `local.properties` esté en `.gitignore`
    - No incluir el valor literal de la clave en ningún archivo versionado
    - _Requirements: 2.1, 2.2_

  - [ ] 1.2 Crear los DTOs anidados de API-Football
    - Crear en `data/remote/dto`: `FixtureResponseDto`, `FixtureItemDto`, `FixtureDto`, `StatusDto`, `VenueDto`, `LeagueDto`, `TeamsDto`, `TeamSideDto`, `GoalsDto` con anotaciones `@SerializedName` según el JSON de `/fixtures` (`response[].fixture/teams/goals/league`)
    - `StatusDto` expone `short` (mapeado a `shortCode`) y `elapsed`; `LeagueDto` expone `id`, `name`, `round`, `logo`; `VenueDto` expone `name`, `city`
    - _Requirements: 1.4, 1B.1, 1B.3_

  - [ ] 1.3 Reescribir la interfaz FootballApiService al esquema /fixtures
    - Reescribir `data/remote/api/FootballApiService` con `getFixturesByDate(@Query("date") String date)` y `getFixturesByLeague(@Query("league") int leagueId, @Query("season") int season)`, ambos devolviendo `Call<FixtureResponseDto>`
    - Eliminar/deprecar los métodos estilo football-data.org (`getCompetitions`, `getMatchesByDate` antiguo, `competitions/{id}/matches`, `teams/{id}`, `teams/{id}/matches`)
    - No usar identificadores de competición en formato cadena (`"PL"`), solo IDs numéricos vía query
    - _Requirements: 1.1, 1B.1, 1B.2, 1B.4_

  - [ ] 1.4 Crear el ApiKeyInterceptor
    - Crear `data/remote/ApiKeyInterceptor` (OkHttp `Interceptor`) que añade el header `x-apisports-key` con `BuildConfig.API_FOOTBALL_KEY` a cada petición
    - _Requirements: 1.2, 2.1_

  - [ ] 1.5 Crear el StatusMapper
    - Crear `data/mapper/StatusMapper` con `MatchStatus map(String apiShortCode)` implementando la tabla del Requisito 6B (NS→SCHEDULED; 1H/HT/2H/ET/BT/P/LIVE→LIVE; FT/AET/PEN→FINISHED; PST→POSTPONED; CANC/ABD→CANCELLED)
    - Códigos nulos/vacíos/desconocidos → `SCHEDULED` por defecto y registrar (log) el código no reconocido
    - _Requirements: 6B.1, 6B.2_

  - [ ]* 1.6 Test unitario de StatusMapper
    - En `test`, verificar cada código de la tabla → estado esperado y códigos desconocidos/nulos → `SCHEDULED`
    - _Requirements: 6B.1, 6B.2_

  - [ ]* 1.7 Test de propiedad de StatusMapper (jqwik)
    - **Property 1: StatusMapper es total y correcto**
    - Tag: `Feature: matches-live-fixtures, Property 1`; ≥100 iteraciones; totalidad, correctitud e idempotencia
    - _Requirements: 6B.1, 6B.2_ _Properties: 1_

  - [ ] 1.8 Crear el mapper DTO→dominio con filtro de Ligas_Objetivo
    - Crear `data/mapper/FixtureMapper` que transforma `FixtureItemDto` en `Match`, `Team`, `Competition`, usando `StatusMapper` para el estado y `fixture.status.elapsed` para el Minuto_Juego cuando el estado normalizado es `LIVE`
    - Descartar partidos cuya `league.id` no pertenezca al conjunto de las seis Ligas_Objetivo (39, 140, 135, 78, 61, 239)
    - Definir constante con el mapeo de las seis Ligas_Objetivo a sus IDs numéricos
    - _Requirements: 1.3, 1.4, 1.5, 1.6, 6B.3_

  - [ ]* 1.9 Test unitario de FixtureMapper
    - Verificar mapeo de `FixtureItemDto` a `Match`/`Team`/`Competition` incluyendo venue/round/elapsed y el filtro de Ligas_Objetivo (descartar ligas fuera del conjunto)
    - _Requirements: 1.3, 1.4, 1.5, 1.6, 6B.3_

- [ ] 2. Ampliación de datos y migración Room
  - [ ] 2.1 Ampliar el modelo de dominio Match
    - Añadir a `Match` los campos `Integer elapsedMinute`, `String venueName`, `String venueCity` con sus getters/setters; conservar las cuotas existentes sin cambios
    - _Requirements: 5.1, 5.5, 6.5_

  - [ ] 2.2 Ampliar MatchEntity y su mapeo
    - Añadir a `data/local/entity/MatchEntity` las columnas `@ColumnInfo(name="elapsed_minute") Integer elapsedMinute`, `@ColumnInfo(name="venue_name") String venueName`, `@ColumnInfo(name="venue_city") String venueCity`
    - Actualizar `toDomainModel()`/`fromDomainModel()` para mapear los tres campos nuevos y el marcador (goles local/visitante) y el Minuto_Juego
    - _Requirements: 5.1, 5.5, 6.5_

  - [ ] 2.3 Definir la migración Room 2→3 y subir versión
    - Añadir en `GolIADatabase` la `Migration MIGRATION_2_3` con `ALTER TABLE matches ADD COLUMN` para `elapsed_minute`, `venue_name`, `venue_city`; subir `@Database(version = 3)`
    - Registrar `.addMigrations(MIGRATION_2_3)` en el `DatabaseModule` (Hilt)
    - _Requirements: 5.1, 5.5, 6.5_

  - [ ]* 2.4 Test de migración Room (MigrationTestHelper)
    - Crear la BD en versión 2, migrar a 3 con `MIGRATION_2_3` y verificar que existen las columnas nuevas y que los datos previos se conservan
    - _Requirements: 5.1, 5.5, 6.5_

- [ ] 3. Componentes de infraestructura
  - [ ] 3.1 Implementar TimeRangeCalculator (Utilidad_Zona_Local)
    - Crear la utilidad con `Range today(ZoneId)`, `Range tomorrow(ZoneId)`, `Range thisWeek(ZoneId)` e `isWithin(long epochMillis, Range)`, con límites en ms inclusivos (`00:00:00.000` / `23:59:59.999`), y clase interna `Range { long startMs; long endMs; }`
    - Único punto de verdad del cálculo de rangos temporales
    - _Requirements: 3.7, 4.1, 4.2_

  - [ ]* 3.2 Test unitario de TimeRangeCalculator
    - Verificar límites `00:00:00.000`/`23:59:59.999`, varias zonas horarias y cambios de día/semana; verificar Rango_Hoy ⊆ Rango_Esta_Semana
    - _Requirements: 3.4, 3.5, 3.6, 3.8, 4.1, 4.2_

  - [ ]* 3.3 Test de propiedad de TimeRangeCalculator (jqwik)
    - **Property 2: Rangos temporales consistentes e independientes de la representación**
    - Tag: `Feature: matches-live-fixtures, Property 2`; ≥100 iteraciones
    - _Requirements: 3.4, 3.5, 3.6, 3.8, 4.1, 4.2_ _Properties: 2_

  - [ ] 3.4 Implementar RequestBudgetManager
    - Crear el gestor persistente (SharedPreferences vía `PreferencesManager`) del Presupuesto_Peticiones con `canRequest()`, `isMinIntervalElapsed()` (≥60000 ms), `recordRequest()`, `isExhausted()` (≥100) y `resetIfNewDay()`
    - _Requirements: 7.3, 7.4, 7.7_

  - [ ]* 3.5 Test unitario de RequestBudgetManager
    - Verificar incremento, tope de 100, reset diario y ventana de 60 s (usando un reloj/tiempo controlable)
    - _Requirements: 7.3, 7.4, 7.7_

  - [ ]* 3.6 Test de propiedad de RequestBudgetManager (jqwik)
    - **Property 5: El presupuesto de peticiones nunca se excede y respeta el intervalo compartido**
    - Tag: `Feature: matches-live-fixtures, Property 5`; ≥100 iteraciones
    - _Requirements: 7.3, 7.4, 7.7_ _Properties: 5_

  - [ ] 3.7 Implementar SearchTextNormalizer
    - Crear la utilidad `String normalize(String)` que pasa a minúsculas y elimina diacríticos (`java.text.Normalizer` NFD + regex `\p{M}`)
    - _Requirements: 10.4_

  - [ ]* 3.8 Test unitario de SearchTextNormalizer
    - Verificar acentos, mayúsculas, cadenas mixtas e idempotencia ("Atletico" coincide con "Atlético")
    - _Requirements: 10.4_

  - [ ]* 3.9 Test de propiedad de SearchTextNormalizer (jqwik)
    - **Property 4: Normalización de búsqueda idempotente e insensible a acentos/mayúsculas**
    - Tag: `Feature: matches-live-fixtures, Property 4`; ≥100 iteraciones
    - _Requirements: 10.4_ _Properties: 4_

  - [ ] 3.10 Implementar LiveRefreshScheduler
    - Crear el temporizador ligado al lifecycle con `start(long intervalMs, Runnable onTick)`, `stop()`, `isRunning()` (basado en `Handler`/`ScheduledExecutorService`)
    - _Requirements: 7.5, 7.6, 7B.1, 7B.2_

- [ ] 4. Refactor del repositorio a firmas asíncronas
  - [ ] 4.1 Actualizar la interfaz MatchRepository
    - Redefinir `MatchRepository` con `getMatches(Callback<Result<List<Match>>>)` (cache-first), `refreshMatchesByDate(String isoDate, Callback<Result<List<Match>>>)`, `refreshLiveMatches(Callback<Result<List<Match>>>)`, `clearCache()`
    - Deprecar/eliminar los métodos football-data.org y las variantes síncronas previas
    - _Requirements: 1B.2, 12.5, 12.6_

  - [ ] 4.2 Refactorizar MatchRepositoryImpl con @IoExecutor y Result
    - Ejecutar toda E/S de red y Room en el `@IoExecutor` (`ExecutorService`), publicando resultados vía `Callback<Result<...>>`; nunca en el hilo principal
    - `getMatches`: leer de Cache_Partidos primero; `refreshMatchesByDate`: llamar `getFixturesByDate`, aplicar `StatusMapper`, filtrar a Ligas_Objetivo, mapear a dominio y upsert en Room; `refreshLiveMatches`: refresco en vivo
    - Integrar `RequestBudgetManager`: comprobar `canRequest()`/`isMinIntervalElapsed()` antes de cada petición, `recordRequest()` al realizarla; si `isExhausted()`, no realizar petición y devolver `Result.Success` con datos de caché
    - Propagar fallos de red/API/persistencia como `Result.Error` tipado
    - _Requirements: 1.1, 6B.1, 6B.3, 7.1, 7.2, 7.3, 7.7, 7.8, 12.5, 12.6_

  - [ ]* 4.3 Test unitario de MatchRepositoryImpl
    - Con fakes de `FootballApiService`, DAOs y `RequestBudgetManager`: verificar cache-first, filtro de ligas, upsert, degradación a caché con cuota agotada y propagación de `Result.Error`
    - _Requirements: 7.1, 7.7, 7.8, 12.6_

- [ ] 5. Casos de uso del dominio
  - [ ] 5.1 Implementar GetMatchesUseCase
    - Crear `domain/usecase/GetMatchesUseCase` con `execute(Callback<Result<List<Match>>>)` delegando en `MatchRepository.getMatches`
    - _Requirements: 12.2, 12.3_

  - [ ] 5.2 Implementar RefreshMatchesUseCase
    - Crear `domain/usecase/RefreshMatchesUseCase` con `execute(String isoDate, Callback<Result<List<Match>>>)` delegando en `refreshMatchesByDate`
    - _Requirements: 7.1, 7.2, 12.2, 12.3_

  - [ ] 5.3 Implementar RefreshLiveMatchesUseCase
    - Crear `domain/usecase/RefreshLiveMatchesUseCase` con `execute(Callback<Result<List<Match>>>)` delegando en `refreshLiveMatches`
    - _Requirements: 7.5, 12.2, 12.3_

- [ ] 6. Capa de presentación
  - [ ] 6.1 Crear modelos de presentación PartidosUiState, MatchUiModel y ChipHorario
    - Crear `PartidosUiState` (Loading, Content con `offlineNotice`/`quotaNotice`, Empty con `Type{FILTER,SEARCH}`, Error con `message`/`retryable`)
    - Crear `MatchUiModel` con los campos del diseño (liga, jornada, nombres, logos, hora local formateada, marcador/minuto, estadio, `status`, `scheduledDateTime`)
    - Crear `enum ChipHorario { HOY, MANANA, ESTA_SEMANA }`
    - _Requirements: 9.1, 9.2, 9.3, 10.7, 11.1, 7.7_

  - [ ] 6.2 Implementar PartidosViewModel (@HiltViewModel)
    - Anotar con `@HiltViewModel`, inyectar los tres casos de uso, `TimeRangeCalculator`, `SearchTextNormalizer`, `LiveRefreshScheduler`; exponer `LiveData<PartidosUiState>`
    - `onChipSelected`: recalcular rango con `TimeRangeCalculator` y re-filtrar; `onSearchQueryChanged`: debounce 300 ms + normalización y re-filtrar dentro del chip activo; `onRefresh`/`onRetry`: solicitar refresco por fecha respetando el intervalo compartido de 60 s
    - `sortMatches`: `Comparator` compuesto grupo (LIVE=0, resto=1) → `scheduledDateTime` ascendente → liga alfabética → equipo local alfabético
    - `startLivePolling`/`stopLivePolling`: arrancar el scheduler solo si hay LIVE en la lista filtrada y detenerlo cuando no haya LIVE o en pausa
    - Estado Error si `BuildConfig.API_FOOTBALL_KEY` está vacío al iniciar
    - _Requirements: 2.3, 3.3, 3.4, 3.5, 3.6, 3.7, 4.1, 4.2, 7.4, 7.5, 7.6, 7.7, 7B.1, 7B.2, 8.1, 8.2, 8.3, 8.4, 8.5, 9.1, 9.2, 9.3, 9.4, 10.2, 10.3, 10.5, 10.6, 10.7, 10.8, 12.1, 12.2, 12.3, 12.7_

  - [ ]* 6.3 Test de propiedad del ordenamiento (jqwik)
    - **Property 3: Ordenamiento por grupos, ascendente y con desempate estable**
    - Tag: `Feature: matches-live-fixtures, Property 3`; ≥100 iteraciones
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5_ _Properties: 3_

  - [ ]* 6.4 Test unitario del ordenamiento
    - Verificar mezclas de LIVE/No-LIVE, empates de horario y estabilidad del desempate
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5_

  - [ ]* 6.5 Tests de PartidosViewModel (InstantTaskExecutorRule)
    - Con `InstantTaskExecutorRule` y fakes de casos de uso/utilidades: transiciones Loading→Content/Empty/Error, selección de chip, búsqueda con debounce (tiempo controlable), reintento y arranque/parada de polling
    - _Requirements: 3.3, 7.7, 9.1, 9.2, 9.3, 9.4, 10.3, 10.6, 10.7_

  - [ ] 6.6 Implementar MatchesAdapter con DiffUtil y Glide
    - Crear `MatchesAdapter extends ListAdapter<MatchUiModel, VH>` con `MatchDiffCallback` (`areItemsTheSame` por id, `areContentsTheSame` por campos visibles)
    - En bind: liga+jornada, nombres, hora local, marcador/minuto según estado, estadio; cargar logos con Glide con placeholder ante error
    - _Requirements: 4.3, 5.2, 5.3, 5.4, 6.1, 6.2, 6.3, 12.4_

- [ ] 7. UI y layouts con la paleta del login
  - [ ] 7.1 Rediseñar item_match.xml
    - Quitar `text_odds1`/`text_odds_x`/`text_odds2`; añadir `ImageView` de logos local/visitante, `TextView` de marcador, minuto en vivo y estadio; aplicar la paleta (`blue_dark`, `blue_start`, `blue_end`, `text_gray`)
    - _Requirements: 5.1, 5.2, 5.3, 6.1, 6.2, 6.3, 13.3_

  - [ ] 7.2 Rediseñar fragment_partidos.xml
    - Reemplazar `TabLayout` por un `ChipGroup` (`singleSelection=true`) con chips "Hoy"/"Mañana"/"Esta semana"; encabezado "GOL-IA" con campana, título "Partidos" con icono de búsqueda; `SwipeRefreshLayout` envolviendo `recycler_matches`; `empty_view` (id `empty_view`); fondo con la paleta oscura azul del login
    - _Requirements: 3.1, 9.2, 13.1, 13.2_

  - [ ] 7.3 Refactorizar PartidosFragment
    - Eliminar la lógica mock y el adapter interno; extender `BasePlaceholderFragment` (`bindPlaceholderData()`); obtener `PartidosViewModel` con Hilt
    - `setupChips` (selección única, "Hoy" por defecto, resaltar activo con `blue_end`), `setupSearch` (TextWatcher → debounce delegado al ViewModel), `setupSwipeRefresh` (→ `onRefresh`), `observeState`/`render` (carga/contenido/vacío filtro/vacío búsqueda/error con reintentar, avisos offline/cuota, `submitList`)
    - `onResume` → `startLivePolling`, `onPause` → `stopLivePolling`
    - _Requirements: 3.1, 3.2, 3.3, 3.9, 7.2, 7B.1, 7B.2, 9.1, 9.2, 9.3, 9.4, 10.1, 10.5, 10.7, 11.1, 11.2, 12.1, 12.4_

- [ ] 8. Wiring de Hilt
  - [ ] 8.1 Configurar el módulo de red con ApiKeyInterceptor y FootballApiService
    - En el `NetworkModule` (Hilt): registrar `ApiKeyInterceptor` en el `OkHttpClient`, configurar `Retrofit` con la base URL de API-Football y proveer `FootballApiService`
    - _Requirements: 1.2, 2.1, 12.3_

  - [ ] 8.2 Proveer las utilidades de infraestructura y casos de uso
    - Proveer `RequestBudgetManager`, `TimeRangeCalculator`, `SearchTextNormalizer`, `LiveRefreshScheduler` y los tres casos de uso; asegurar el binding de `MatchRepository`→`MatchRepositoryImpl` y del `@IoExecutor`
    - _Requirements: 12.3, 12.5_

- [ ] 9. Checkpoint final
  - Asegurar que todas las pruebas pasan; preguntar al usuario si surgen dudas.

## Notes

- Las tareas marcadas con `*` son opcionales (pruebas) y pueden omitirse para un MVP más rápido; el agente NO las implementa automáticamente.
- Cada tarea referencia requisitos concretos para trazabilidad; las tareas de PBT referencian además su propiedad (P1–P5) con el tag `Feature: matches-live-fixtures, Property N`.
- Las PBT usan jqwik con ≥100 iteraciones. El usuario compila y ejecuta las pruebas en su máquina; el entorno del agente no compila Hilt/dex ni ejecuta instrumentación Android.
- Los checkpoints garantizan validación incremental.
- Cuotas fuera de alcance en la UI: `item_match.xml` no renderiza cuotas aunque el modelo conserve los campos.

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2", "3.1", "3.4", "3.7", "3.10"] },
    { "id": 1, "tasks": ["1.3", "1.4", "1.5", "2.1", "3.2", "3.3", "3.5", "3.6", "3.8", "3.9"] },
    { "id": 2, "tasks": ["1.6", "1.7", "1.8", "2.2"] },
    { "id": 3, "tasks": ["1.9", "2.3", "4.1"] },
    { "id": 4, "tasks": ["2.4", "4.2"] },
    { "id": 5, "tasks": ["4.3", "5.1", "5.2", "5.3"] },
    { "id": 6, "tasks": ["6.1", "6.6"] },
    { "id": 7, "tasks": ["6.2", "7.1"] },
    { "id": 8, "tasks": ["6.3", "6.4", "6.5", "7.2", "8.1"] },
    { "id": 9, "tasks": ["7.3", "8.2"] }
  ]
}
```
