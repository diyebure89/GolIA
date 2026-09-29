# Implementation Plan: Pantalla de Ranking (ranking-screen)

## Overview

Este plan implementa la pantalla `RankingFragment` de GolIA (Android nativo, Java, Clean Architecture + MVVM + Hilt + Room). Se construye de dentro hacia afuera: primero los modelos de dominio y el motor puro `Ranking_Calculator` (con sus pruebas basadas en propiedades), luego la fuente de participantes (`SeedProfiles` + `Ranking_Participant_Provider`), el `GetRankingUseCase`, y finalmente la capa de presentación (`RankingViewModel`, `RankingFragment`, `RankingAdapter`, layout) cableada al `LiveRefreshScheduler` y a Hilt.

Todo el cálculo reutiliza `Prediction_Scorer` (nunca `points_earned`), se ejecuta en `@IoExecutor` y publica en `LiveData` desde un `BaseViewModel`. El ranking solo lee de `Local_Database` y de `SeedProfiles` en memoria: no realiza peticiones a la API.

## Tasks

- [x] 1. Definir los modelos de dominio del ranking
  - [x] 1.1 Crear el enum `Ranking_Period` y los modelos de datos del ranking
    - Crear `Ranking_Period` (SEMANAL=7 días, MENSUAL=30 días, TOTAL sin límite) con `windowStartMillis(now)` que devuelve el límite inferior de la ventana o `0` para TOTAL
    - Crear `ScoredPrediction` (predictedHome, predictedAway, actualHome nullable, actualAway nullable, status:MatchStatus, timestamp)
    - Crear `Ranking_Participant` (id, displayName, avatarRef, isCurrentUser, List<ScoredPrediction>)
    - Crear `Ranking_Entry` (position, displayName, avatarRef, points, winPercentage, streak, isCurrentUser, hasLive)
    - Crear `RankingResult` (List<Ranking_Entry> ordenada, currentUserPosition, hasLiveAny)
    - _Requirements: 2.1, 2.2, 4.1, 5.1, 6.1, 8.2_

- [x] 2. Implementar el motor puro de cálculo del ranking
  - [x] 2.1 Implementar `Ranking_Calculator` (dominio puro) reutilizando `Prediction_Scorer`
    - `pointsForPeriod(participant, period, now)`: suma de consolidado (FINISHED dentro del periodo, puntuados con `Prediction_Scorer.score`) + Live_Points (LIVE con marcador no nulo; 0 si nulo), conjuntos disjuntos por estado
    - `winPercentage(participant, period, now)`: `Math.round(aciertos/resueltos*100)` sobre FINISHED del periodo; 0 si no hay resueltos
    - `streak(participant)`: GLOBAL, ordena FINISHED por timestamp desc y cuenta consecutivos con `score >= 1` desde el más reciente, se corta con el primer 0
    - `hasLive(participant)`: true si tiene algún ScoredPrediction LIVE con marcador que aporte Live_Points
    - `rank(participants, period, now)`: calcula métricas, ordena por puntos desc y aplica desempate (a) winPercentage desc, (b) displayName asc case-insensitive, (c) id asc; asigna posiciones 1..N y arma `RankingResult`
    - `now` inyectable como parámetro para determinismo en pruebas
    - _Requirements: 3.4, 4.2, 4.4, 4.5, 4.6, 5.2, 5.3, 5.5, 6.1, 6.2, 6.3, 6.4, 8.2_

  - [x]* 2.2 Escribir property test: total del periodo = suma disjunta de consolidado y live
    - **Property 1: El total del periodo es la suma disjunta de consolidado y live**
    - **Validates: Requirements 6.1, 6.4**

  - [x]* 2.3 Escribir property test: live sin marcador aporta cero y no falla
    - **Property 2: Live sin marcador aporta cero y no falla**
    - **Validates: Requirements 6.3**

  - [x]* 2.4 Escribir property test: Win_Percentage en 0..100 y 0 sin resueltos
    - **Property 3: Win_Percentage está en 0..100 y es 0 sin resueltos**
    - **Validates: Requirements 5.2, 4.6**

  - [x]* 2.5 Escribir property test: racha global, no negativa, se corta con el primer fallo
    - **Property 4: La racha es global, no negativa y se corta con el primer fallo**
    - **Validates: Requirements 5.3**

  - [x]* 2.6 Escribir property test: orden y desempate deterministas y totales
    - **Property 5: El orden y el desempate son deterministas y totales**
    - **Validates: Requirements 3.4, 5.5, 8.2**

  - [x]* 2.7 Escribir property test: el filtro de periodo respeta la ventana temporal
    - **Property 6: El filtro de periodo respeta la ventana temporal**
    - **Validates: Requirements 4.2, 4.4**

  - [x]* 2.8 Escribir property test: un pronóstico sin Match en caché no participa
    - **Property 7: Un pronóstico sin Match en caché no participa**
    - **Validates: Requirements 4.5**

  - [x]* 2.9 Escribir tests unitarios de ejemplos de borde para `Ranking_Calculator`
    - 0 pronósticos, empates totales, un único participante con datos, live 0-0
    - Redondeo de `Win_Percentage` con `Math.round`; racha desde el más reciente por timestamp
    - _Requirements: 9.1, 9.2, 9.3, 9.4_

- [x] 3. Checkpoint — motor de cálculo verificado
  - Ensure all tests pass, ask the user if questions arise.

- [x] 4. Definir la fuente de participantes seed
  - [x] 4.1 Implementar `SeedProfiles` como constantes de dominio deterministas
    - Definir `Seed_Result` (predictedHome, predictedAway, actualHome, actualAway, status, timestampOffsetDays) y `SeedProfile` (name, avatarDrawable, List<Seed_Result>)
    - Definir al menos 3 perfiles (p. ej. CarlosGol, PronosticoPro, BetMaster99 y algunos más) con resultados fijos que caigan en Semanal/Mensual/Total relativos a `now`
    - Exponer `all()` que devuelve la lista como `Ranking_Participant` con sus `ScoredPrediction` (marcador pronosticado, marcador real, estado FINISHED/LIVE, timestamp)
    - Sin acceso a Room ni red
    - _Requirements: 2.1, 2.2, 2.3, 2.6_

  - [x]* 4.2 Escribir tests unitarios de determinismo de `SeedProfiles`
    - Verificar misma salida en dos evaluaciones y al menos 3 participantes producidos
    - _Requirements: 2.2, 2.6, 9.1_

- [x] 5. Implementar el proveedor de participantes y el use case
  - [x] 5.1 Implementar `Ranking_Participant_Provider`
    - Usuario real: `PreferencesManager.getUserId()`; nombre vía `DisplayName.resolve`; avatar desde `UserDao.getById`; pronósticos vía `PredictionDao.getPredictionsByUser`; resolver cada `Match` con `MatchDao.getMatchById` para marcador/estado; descartar el pronóstico si el `Match` no está en caché
    - Mapear cada pronóstico real a `ScoredPrediction`
    - Combinar con `SeedProfiles.all()` para producir la lista de `Ranking_Participant`
    - _Requirements: 2.1, 2.7, 4.5, 8.1, 8.3_

  - [x] 5.2 Implementar `GetRankingUseCase` sobre `@IoExecutor`
    - `execute(period, userId, token, Callback<Result<RankingResult>>)` despacha al `@IoExecutor`, obtiene participantes del provider, delega el cómputo a `Ranking_Calculator.rank` y devuelve `RankingResult`
    - Errores técnicos -> `Result.Error`; sin datos parciales
    - _Requirements: 1.2, 1.3, 1.6, 1.8, 7.5_

  - [ ]* 5.3 Escribir test instrumentado ligero de `Ranking_Participant_Provider`
    - Room en memoria: resuelve pronósticos + matches y descarta el pronóstico sin match
    - _Requirements: 4.5, 9.3_

- [x] 6. Checkpoint — cálculo end-to-end de dominio verificado
  - Ensure all tests pass, ask the user if questions arise.

- [x] 7. Implementar la capa de presentación (ViewModel y estado)
  - [x] 7.1 Implementar `RankingUiState` (sellado) y `RankingViewModel extends BaseViewModel`
    - `RankingUiState`: Loading / Content(podium top3, list, currentUserPosition, hasLive) / Empty / Error(message)
    - `@HiltViewModel` con `@Inject` de `GetRankingUseCase`, `PreferencesManager` y `LiveRefreshScheduler`
    - `load()` / `selectPeriod(period)`: valida sesión (`isLoggedIn`); si no hay, evento de navegación a login; si hay, incrementa `generationToken`, emite `Loading` y llama al use case; en el callback descarta resultados con token obsoleto (último gana) y publica Content/Empty/Error con `postValue`
    - `onScreenResumed()` y tick del `LiveRefreshScheduler`: recalculan con `selectedPeriod` (nuevo token) para refrescar Live_Points sin peticiones a la API
    - Retener `selectedPeriod` en el ViewModel (sobrevive rotación); detener suscripción al scheduler en `onCleared`/pausa
    - _Requirements: 1.2, 1.4, 1.7, 4.3, 6.7, 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.7_

  - [ ]* 7.2 Escribir tests unitarios de `RankingViewModel`
    - Anti-solapamiento (último token gana), retención de periodo, transición a estado Empty y Error, navegación a login sin sesión
    - _Requirements: 1.7, 7.4, 7.6, 7.7_

- [x] 8. Implementar la vista del ranking
  - [x] 8.1 Crear el layout `fragment_ranking.xml` según el mock
    - Encabezado "Ranking Global" + icono de trofeo y subtítulo fijo localizado con la nota "local/demo"
    - Tarjeta de podio con tres bloques: 2.º (izq), 1.º (centro elevado/destacado), 3.º (der), cada uno con avatar, nombre, puntos y número de posición
    - Grupo de chips/botones de periodo (Semanal / Mensual / Total), Semanal por defecto
    - `RecyclerView` para la lista, `ProgressBar` de carga y vista de estado vacío
    - _Requirements: 1.1, 2.4, 3.1, 3.2, 4.1_

  - [x] 8.2 Implementar `RankingAdapter` para la lista de posiciones
    - Cada fila: medalla/número de posición, avatar, nombre, "% acierto · Racha: N", puntos formateados con separador de miles regional ("3,420 pts"), distintivo "en vivo"
    - Resaltar visualmente la fila del `Current_User`; degradar avatar ausente a placeholder
    - _Requirements: 5.1, 5.4, 6.5, 2.5_

  - [x] 8.3 Implementar `RankingFragment` observando el estado
    - Reemplazar el placeholder: pasar a `Fragment` con `@AndroidEntryPoint` inflando `fragment_ranking.xml`
    - Observar `LiveData<RankingUiState>`; renderizar Loading/Content/Empty/Error; poblar podio (1 centro destacado, 2 izq, 3 der) y lista ordenada; mostrar solo los puestos disponibles si hay menos de 3
    - En `onResume` pedir recálculo y suscribirse al `LiveRefreshScheduler` mientras esté visible; manejar el cambio de chip de periodo llamando a `selectPeriod`; navegar a login ante el evento correspondiente
    - No accede a repositorios ni BD
    - _Requirements: 1.1, 1.4, 1.5, 1.7, 1.8, 3.1, 3.3, 4.3, 6.5, 6.7, 6.8, 7.1, 7.2, 7.3, 7.4_

- [x] 9. Cableado final (DI e integración)
  - [x] 9.1 Configurar la inyección Hilt y wiring de componentes
    - Asegurar `@Inject` constructor en `GetRankingUseCase`, `Ranking_Calculator`, `Ranking_Participant_Provider` (y reutilizar `Prediction_Scorer`)
    - Inyectar `LiveRefreshScheduler` (de `InfrastructureModule`) en `RankingViewModel`; marcar `RankingViewModel` como `@HiltViewModel`
    - Verificar que la navegación al `RankingFragment` funciona y que el ciclo en vivo se activa/desactiva con la visibilidad
    - _Requirements: 1.2, 6.7, 7.5, 7.7_

  - [ ]* 9.2 Escribir test de integración de wiring
    - Verificar que el grafo de dependencias del `RankingViewModel` se construye y que un cambio de periodo dispara un recálculo
    - _Requirements: 7.5, 7.6_

- [x] 10. Checkpoint final — asegurar que todas las pruebas pasan
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Las tareas marcadas con `*` son opcionales (pruebas unitarias, de propiedad e integración) y pueden omitirse para un MVP más rápido; las de implementación central nunca se omiten.
- Cada tarea referencia requisitos específicos para trazabilidad.
- Los checkpoints aseguran validación incremental.
- Las 7 pruebas de propiedad validan las propiedades de corrección universales definidas en el diseño (Property 1–7), cada una anotada con su número de propiedad y las cláusulas de requisito que verifica.
- Todo el cálculo reutiliza `Prediction_Scorer` (nunca `points_earned`), corre en `@IoExecutor` y no realiza peticiones a la API.

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1"] },
    { "id": 1, "tasks": ["2.1", "4.1"] },
    { "id": 2, "tasks": ["2.2", "2.3", "2.4", "2.5", "2.6", "2.7", "2.8", "2.9", "4.2", "5.1"] },
    { "id": 3, "tasks": ["5.2", "5.3"] },
    { "id": 4, "tasks": ["7.1", "8.1", "8.2"] },
    { "id": 5, "tasks": ["7.2", "8.3"] },
    { "id": 6, "tasks": ["9.1"] },
    { "id": 7, "tasks": ["9.2"] }
  ]
}
```
