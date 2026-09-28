# Implementation Plan: Detalle del Partido y Pronóstico (match-detail-prediction)

## Overview

Este plan convierte el diseño en pasos de codificación incrementales para la arquitectura real del proyecto (Android nativo, Java, Clean Architecture + MVVM + Hilt + Room, sin backend remoto). El orden respeta las dependencias reales: primero la capa de dominio pura y testeable (scorer, calculadora de estadísticas, errores), luego datos (columna/índice, migración, DAOs, repositorios), la resolución integrada al refresco, el cableado de Hilt, y por último la presentación (Activities + ViewModels + layouts) y el cableado de navegación desde la lista. Cada tarea referencia sus requisitos.

El lenguaje de implementación es Java. El diseño incluye una sección de "Correctness Properties", por lo que se incluyen subtareas de pruebas basadas en propiedades (PBT) marcadas como opcionales con `*`. Los tests exigidos por el Requisito 11 se mantienen como tareas requeridas.

## Tasks

- [x] 1. Dominio puro: puntuación y estadísticas (sin dependencias de Android)
  - [x] 1.1 Implementar `Prediction_Scorer` con la regla `Prediction_Scoring`
    - Crear la clase de dominio con `score(predictedHome, predictedAway, actualHome, actualAway)` que sume 5 (ganador 1X2), 2 (goles local exactos), 2 (goles visitante exactos), 1 (diferencia de goles) y 9 (marcador exacto), con máximo 19
    - Exponer `isCorrect(score)` = `score >= 1` y la derivación `deriveOutcome(home, away)` -> `HOME_WIN`/`DRAW`/`AWAY_WIN`
    - _Requisitos: R5.5, R9.3, R9.4, R9.6_

  - [x] 1.2 Implementar `Season_Stats_Calculator` (funciones puras sobre `List<Match>`)
    - `wins(teamId, matches)`, `goalsAverage(teamId, matches)` (1 decimal, 0.0 si vacío), `recentForm(teamId, matches)` (orden por `scheduledDateTime` desc, desempate por `Match.id`, hasta 5, conteo de V), y `barRatio(a, b)` (proporcional, 0 ante suma 0)
    - Definir el value object de dominio `SeasonStats` (por equipo: wins, goalsAverage, recentWins)
    - _Requisitos: R3.3, R3.4, R3.5, R3.7, R3.8, R3.9_

  - [x] 1.3 Definir el contrato de error tipado del pronóstico
    - Crear el enum `PredictionError` (`SESSION_UNAVAILABLE`, `PREDICTION_CLOSED`, `MATCH_NOT_FOUND`, `PERSISTENCE_ERROR`, `VALIDATION_ERROR`) y `PredictionException extends Exception` que lo transporta, siguiendo el patrón de `AuthException`
    - _Requisitos: R2.7, R5.6, R5.7, R10.5_

  - [x] 1.4 Implementar la validación del marcador
    - Función pura que valide que goles local y visitante son enteros en `0..99` y no vacíos, devolviendo errores por campo
    - _Requisitos: R5.3, R5.4_

  - [ ]* 1.5 Pruebas de propiedad del dominio puro
    - **Property 1: derivación 1X2 total y coherente**  — **Validates: Requirements 5.5**
    - **Property 2: puntuación acotada 0..19 y máxima en marcador exacto** — **Validates: Requirements 9.3, 9.4**
    - **Property 3: isCorrect consistente con score** — **Validates: Requirements 9.6**
    - **Property 5: victorias y promedio correctos** — **Validates: Requirements 3.3, 3.4**
    - **Property 6: forma reciente <=5 y orden temporal** — **Validates: Requirements 3.5, 3.9**
    - **Property 7: barras proporcionales y seguras ante suma cero** — **Validates: Requirements 3.7, 3.8**
    - **Property 9: validación de marcador acepta exactamente 0..99** — **Validates: Requirements 5.3, 5.4**

  - [x] 1.6 Tests unitarios JVM requeridos de dominio (R11)
    - Tests de `Prediction_Scorer` (ganador solo=5, un equipo=2, ambos=4, diferencia=1, exacto=19, fallo total=0/isCorrect=false)
    - Tests de `Season_Stats_Calculator` (0 partidos, <5 partidos, promedio a 1 decimal, forma reciente)
    - Tests de validación de marcador (rango y requeridos) y de `deriveOutcome`
    - _Requisitos: R11.1, R11.2, R11.3_

- [x] 2. Capa de datos: entidad, índice único, DAOs y migración
  - [x] 2.1 Añadir índice único (user_id, match_id) a `PredictionEntity`
    - Añadir `indices = { @Index(value = {"user_id", "match_id"}, unique = true) }` a la `@Entity` de `predictions`
    - _Requisitos: R6.1_

  - [x] 2.2 Añadir la consulta de estadísticas por equipo a `MatchDao`
    - `@Query("SELECT * FROM matches WHERE status = 'FINISHED' AND (home_team_id = :teamId OR away_team_id = :teamId)") List<MatchEntity> getFinishedMatchesByTeam(String teamId)`
    - Confirmar que `getMatchById(String)` ya existe para el detalle
    - _Requisitos: R2.1, R3.1, R3.2_

  - [x] 2.3 Subir versión de Room y crear la migración del índice único
    - Incrementar `version` en `GolIADatabase` y definir `MIGRATION_N_N+1` que (a) elimine duplicados por (user_id, match_id) conservando el más reciente por `created_at` y (b) cree el índice único; registrarla en `DatabaseModule.addMigrations(...)` junto a las existentes; mantener `exportSchema = true` y regenerar el JSON de esquema
    - _Requisitos: R6.2, R6.3, R6.5_

  - [ ]* 2.4 Test de migración de esquema (androidTest, patrón `MatchMigrationTest`)
    - Sembrar pronósticos con un duplicado por (user_id, match_id), correr la migración y verificar que queda una sola fila por pareja, que el índice único existe y que el resto se conserva
    - _Requisitos: R6.3, R11.6_

- [x] 3. Repositorio de partidos: obtención por id (cache-first)
  - [x] 3.1 Añadir `getMatchById` a `MatchRepository` e implementarlo
    - Añadir `void getMatchById(String matchId, Callback<Result<Match>> callback)` a la interfaz; implementar en `MatchRepositoryImpl` ejecutando en `@IoExecutor`, leyendo `matchDao.getMatchById`; si es `null` devolver `Result.Error(PredictionException(MATCH_NOT_FOUND))`; sin red ni `RequestBudgetManager`
    - _Requisitos: R2.1, R2.7, R2.9_

- [x] 4. Repositorio de pronósticos y resolución
  - [x] 4.1 Definir la interfaz `PredictionRepository` (dominio)
    - Métodos síncronos que devuelven `Result`: `getUserPrediction`, `savePrediction` (upsert), `getUserPredictions`, `getPredictionsForMatch`, `resolveForMatch`
    - _Requisitos: R5.8, R5.9, R8.1, R9.6_

  - [x] 4.2 Implementar `PredictionRepositoryImpl` (`@Singleton`, `@Inject`)
    - Depende de `PredictionDao`, `MatchDao` y `Prediction_Scorer`; opera con ids como String (sin conversiones UUID que puedan fallar)
    - `savePrediction`: normaliza identidad (`user_id` = getUserId, `match_id` = Match_Id), consulta `getUserPredictionForMatch`; si existe reutiliza su `id` y `created_at`, si no genera `id` UUID; deriva `predictedOutcome`; `isCorrect=null`, `pointsEarned=0`; persiste con `insertPrediction` (REPLACE); ante violación del índice único resuelve como actualización (upsert)
    - _Requisitos: R5.8, R5.9, R6.4, R8.1, R8.2_

  - [x] 4.3 Implementar `PredictionResolverUseCase` idempotente
    - Para cada `Match` `FINISHED` con marcador: `getPredictionsByMatch`, y por cada pendiente (`isCorrect == null`) calcular con `Prediction_Scorer`, fijar `isCorrect`, `pointsEarned` y `finalizedAt`, persistir con `updatePrediction`; ignorar los ya resueltos; sin llamadas de red; ejecutar en `@IoExecutor`
    - _Requisitos: R9.6, R9.7, R9.8_

  - [x] 4.4 Integrar el resolver en el refresco de partidos
    - Inyectar el resolver en `MatchRepositoryImpl`; tras `insertMatches`, filtrar los partidos `FINISHED` con marcador y pasarlos al resolver dentro del mismo hilo de I/O del refresco
    - _Requisitos: R9.8_

  - [x] 4.5 Tests requeridos de repositorio/resolver (R11)
    - Test de upsert: un segundo pronóstico del mismo usuario+partido reemplaza al anterior sin duplicar
    - Test de idempotencia del resolver: un pronóstico ya resuelto no se recalcula
    - _Requisitos: R11.4, R11.5_

  - [ ]* 4.6 Pruebas de propiedad de datos
    - **Property 4: upsert preserva unicidad por (user_id, match_id)** — **Validates: Requirements 5.9, 6.1, 6.4**
    - **Property 8: el resolver es idempotente** — **Validates: Requirements 9.7**
- [x] 5. Inyección de dependencias (Hilt)
  - [x] 5.1 Vincular `PredictionRepository` y exponer constante de Intent
    - Añadir `@Binds @Singleton PredictionRepository bindPredictionRepository(PredictionRepositoryImpl impl)` en `RepositoryModule`
    - Añadir `EXTRA_MATCH_ID` (String) a `Constants`
    - Confirmar que use cases, `Prediction_Scorer` y `Season_Stats_Calculator` se construyen por `@Inject` constructor; `MatchRepositoryImpl` recibe el resolver por constructor
    - _Requisitos: R1.4, R10.1_

- [x] 6. Checkpoint - Asegurar que compila y pasan los tests de dominio/datos
  - Compilar y ejecutar los tests de dominio, repositorio, resolver y migración; preguntar al usuario si surgen dudas.

- [x] 7. Presentación: casos de uso de lectura y detalle
  - [x] 7.1 Implementar los use cases de lectura que despachan al `@IoExecutor`
    - `GetMatchByIdUseCase`, `GetSeasonStatsUseCase` (usa `Season_Stats_Calculator` + `getFinishedMatchesByTeam`), `GetUserPredictionUseCase`, `GetUserPredictionsUseCase` (combina cada `Prediction` con su `Match` resuelto), `SavePredictionUseCase` (valida `Prediction_Lock_Threshold` y delega el upsert)
    - _Requisitos: R2.1, R3.1, R4.4, R5.7, R8.2_

  - [x] 7.2 Crear `DetallePartidoActivity` + `DetallePartidoViewModel` y su layout
    - Layout del mock: encabezado con atrás y título, barra "LIGA — JORNADA", sección "Estadísticas de temporada" con tres barras comparativas, botón "Hacer Pronóstico"; NO mostrar cuotas
    - ViewModel (`@HiltViewModel` extends `BaseViewModel`): `load(matchId)` valida sesión, obtiene detalle (MATCH_NOT_FOUND -> mensaje), estadísticas y pronóstico existente; expone `MatchDetailUiModel`, `SeasonStatsUiModel`, `PredictButtonState` (ENABLED_NEW/ENABLED_EDIT/VIEW_ONLY/CLOSED) e `isLoading`/`errorMessage`; representa el estado por `MatchStatus`
    - Reevaluar `Prediction_Lock_Threshold` en `onResume`
    - _Requisitos: R2.2, R2.3, R2.4, R2.5, R2.6, R2.8, R3.5, R3.6, R4.1, R4.2, R4.3, R4.4, R4.5, R4.6, R4.7_

- [x] 8. Presentación: registro del pronóstico y confirmación
  - [x] 8.1 Crear `PronosticoActivity` + `PronosticoViewModel` y su layout
    - Layout: cabecera de equipos y fecha, sección "Marcador exacto" con dos campos numéricos (local/visitante); sin cuotas ni selección 1X2
    - ViewModel: precarga pronóstico existente; al enviar valida (0..99, requeridos), deriva outcome, ejecuta `SavePredictionUseCase`; anti-doble-submit con `isLoading`; traduce `PredictionError` a strings; navega a confirmación en éxito transportando `Match_Id` y marcador
    - _Requisitos: R5.1, R5.2, R5.3, R5.4, R5.5, R5.6, R5.7, R5.10, R5.11, R5.12_

  - [x] 8.2 Crear `ConfirmacionActivity` y su layout
    - Mostrar equipos, marcador pronosticado, fecha y puntos posibles (19); botones "Ver historial" (abre Historial) e "Ir al inicio" (`MainActivity` con `FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_SINGLE_TOP`); back que no reabre Pronóstico ni reescribe en BD
    - _Requisitos: R7.1, R7.2, R7.3, R7.4, R7.5, R7.6_

- [x] 9. Presentación: historial de pronósticos
  - [x] 9.1 Crear `HistorialPronosticosActivity` + `HistorialViewModel`, adapter y layout
    - Lista con `PredictionHistoryAdapter` de `PredictionHistoryUiModel` (equipos o placeholder "Partido no disponible", marcador pronosticado, estado pendiente/acertado/fallido, puntos si resuelto); estado vacío; requiere sesión (si no, redirigir a login)
    - _Requisitos: R8.1, R8.2, R8.3, R8.4, R8.5, R8.6, R8.7, R8.8_

- [x] 10. Navegación desde la lista y strings
  - [x] 10.1 Cablear el click del partido en Inicio y Partidos
    - Construir `MatchesAdapter` con un `OnMatchClickListener` en `InicioFragment` y `PartidosFragment` que abra `DetallePartidoActivity` con `EXTRA_MATCH_ID = MatchUiModel.id` (la PK del partido, no el externalId)
    - _Requisitos: R1.1, R1.2, R1.3, R1.5, R1.6_

  - [x] 10.2 Añadir recursos de string localizados
    - Errores tipados (`error_match_not_found`, `error_session_unavailable`, `error_prediction_closed`, `error_persistence`), errores de campo del marcador, etiquetas de estado del partido y textos del flujo (pendiente/acertado/fallido, puntos, "Partido no disponible")
    - _Requisitos: R10.5_

  - [x] 10.3 Registrar las Activities y el back en el manifest
    - Declarar las cuatro Activities en `AndroidManifest.xml` con su `parentActivity`/comportamiento de "atrás" coherente
    - _Requisitos: R1.4, R10.6, R10.7_

- [x] 11. Checkpoint final - Compilar y verificar el flujo completo
  - Compilar la app, verificar navegación lista -> detalle -> pronóstico -> confirmación -> historial, y que la resolución de puntos ocurre al refrescar partidos finalizados; asegurar que todos los tests pasan.

## Notes

- Las tareas marcadas con `*` son pruebas basadas en propiedades (PBT) opcionales; pueden omitirse para un MVP más rápido.
- Los tests exigidos por el Requisito 11 (1.6, 2.4, 4.5) NO están marcados como opcionales.
- Cada tarea referencia requisitos específicos para trazabilidad.
- Los checkpoints permiten validación incremental.

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2", "1.3", "1.4", "2.1", "2.2"] },
    { "id": 1, "tasks": ["1.5", "1.6", "2.3", "3.1"] },
    { "id": 2, "tasks": ["2.4", "4.1"] },
    { "id": 3, "tasks": ["4.2", "4.3"] },
    { "id": 4, "tasks": ["4.4", "4.5", "4.6", "5.1"] },
    { "id": 5, "tasks": ["7.1"] },
    { "id": 6, "tasks": ["7.2", "8.1", "8.2", "9.1"] },
    { "id": 7, "tasks": ["10.1", "10.2", "10.3"] }
  ]
}
```
