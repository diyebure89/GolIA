# Plan de Implementación

- [ ] 1. Escribir la prueba de exploración de la condición del bug
  - **Property 1: Bug Condition** - Identidad del match_id preservada en la lectura del historial
  - **CRÍTICO**: esta prueba DEBE FALLAR sobre el código sin arreglar — el fallo CONFIRMA que el bug existe
  - **NO intentes arreglar la prueba ni el código de producción cuando falle**
  - **NOTA**: la prueba codifica el comportamiento esperado; validará el arreglo cuando pase tras la implementación
  - **GOAL**: surgir contraejemplos que demuestren que el bug existe
  - **Enfoque PBT acotado**: para este bug determinista, acotar la propiedad a PKs no-UUID concretas: `"match-1"` y `"1035048"`
  - Crear un test JVM puro (JUnit4), sin Android/Robolectric ni Room real, siguiendo la convención de `PredictionRepositoryUpsertResolverTest` y `MatchEntityIdentityTest`. Sugerencia de ubicación: `app/src/test/java/com/diyebure/golia/domain/usecase/GetUserPredictionsIdentityTest.java`
  - Construir una `PredictionEntity` con `match_id` no-UUID (`"match-1"`), aplicar `toDomainModel()` y luego el emparejamiento equivalente a `GetUserPredictionsUseCase.resolveMatch`, usando un `FakeMatchDao` en memoria precargado con una `MatchEntity` de PK `"match-1"` (estilo de los tests existentes)
  - Aserción: el `Match` resuelto NO es `null` y su identidad corresponde a la PK `"match-1"` (y análogamente `"1035048"`)
  - Incluir un caso de CONTROL con `match_id` UUID canónico y partido presente (debería resolver ya sobre el código sin arreglar)
  - Ejecutar sobre el código SIN arreglar
  - **EXPECTED OUTCOME**: la prueba FALLA para los casos no-UUID (correcto — demuestra el bug); el control UUID pasa
  - Documentar los contraejemplos hallados (p. ej. "`Prediction.matchId == null` tras `toDomainModel()` para `match_id='match-1'`; `resolveMatch` lanza NPE / devuelve null aunque el partido existe")
  - Marcar la tarea como completa cuando la prueba esté escrita, ejecutada y el fallo documentado
  - _Requirements: 1.1, 1.2, 1.3_

- [ ] 2. Escribir las pruebas de preservación (ANTES de implementar el arreglo)
  - **Property 2: Preservation** - Comportamiento inalterado fuera de la condición del bug
  - **IMPORTANTE**: seguir la metodología de observación primero
  - Observar sobre el código SIN arreglar: para `match_id` UUID canónico con partido presente, `resolveMatch` devuelve el `Match` correcto
  - Observar sobre el código SIN arreglar: para un `match_id` cuyo partido NO está en la caché, `resolveMatch` devuelve `null` (placeholder legítimo)
  - Escribir pruebas (acotadas como PBT determinista) que fijen ese comportamiento: conjunto de UUID canónicos → `Match` resuelto; conjunto de PKs ausentes → `null`
  - Confirmar por lectura de código que el guardado (`buildUpsertEntity`, índice único) y la puntuación (`resolveForMatch`, `Prediction_Scorer`) no se ven afectados; apoyarse en `PredictionRepositoryUpsertResolverTest` como base de puntuación/upsert
  - Ejecutar las pruebas sobre el código SIN arreglar
  - **EXPECTED OUTCOME**: las pruebas PASAN (confirman la base a preservar)
  - Marcar la tarea como completa cuando las pruebas estén escritas, ejecutadas y en verde sobre el código sin arreglar
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

- [ ] 3. Arreglo para la pérdida de identidad del match_id en la lectura del historial (Variante A)

  - [ ] 3.1 Implementar el arreglo
    - En `domain/model/Prediction.java`: añadir campo `String rawMatchId` con getter/setter, documentado como en `Match.rawId` (porta la PK original del partido; puede ser `null` para pronósticos en memoria). NO cambiar el tipo `UUID matchId`
    - En `data/local/entity/PredictionEntity.java`: en `toDomainModel()`, poblar `prediction.setRawMatchId(matchId)` con el `String` crudo cuando `match_id` no es nulo/vacío; mantener el intento de `UUID.fromString` para preservar el UUID en PKs canónicas
    - En `domain/usecase/GetUserPredictionsUseCase.java`: en `resolveMatch`, resolver el id de búsqueda priorizando `getRawMatchId()`; caer a `getMatchId().toString()` solo si `rawMatchId` es nulo; devolver `null` (sin NPE) si ambos son nulos
    - NO tocar `buildUpsertEntity`, el índice único `(user_id, match_id)`, `resolveForMatch` ni `Prediction_Scorer`
    - _Bug_Condition: isBugCondition(pred) donde pred.match_id no es UUID canónico (design)_
    - _Expected_Behavior: expectedBehavior(result) — Match resuelto por match_id original, sin NPE (design)_
    - _Preservation: Preservation Requirements del design_
    - _Requirements: 2.1, 2.2, 2.3, 3.1, 3.2, 3.3, 3.4, 3.5_

  - [ ] 3.2 Verificar que la prueba de exploración ahora pasa
    - **Property 1: Expected Behavior** - Identidad del match_id preservada en la lectura del historial
    - **IMPORTANTE**: re-ejecutar la MISMA prueba de la Tarea 1 — NO escribir una nueva
    - Ejecutar la prueba de exploración de la Tarea 1
    - **EXPECTED OUTCOME**: la prueba PASA (confirma que el bug está arreglado)
    - _Requirements: 2.1, 2.2, 2.3_

  - [ ] 3.3 Verificar que las pruebas de preservación siguen pasando
    - **Property 2: Preservation** - Comportamiento inalterado fuera de la condición del bug
    - **IMPORTANTE**: re-ejecutar las MISMAS pruebas de la Tarea 2 — NO escribir nuevas
    - Ejecutar las pruebas de preservación de la Tarea 2
    - **EXPECTED OUTCOME**: las pruebas PASAN (confirman que no hay regresiones)
    - Confirmar que todos los tests JVM existentes siguen pasando tras el arreglo
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

- [ ] 4. Checkpoint - Asegurar que toda la suite JVM pasa
  - Compilar con `gradlew` y ejecutar `gradlew :app:testDebugUnitTest`; verificar por los XML en `app/build/test-results/testDebugUnitTest/`
  - Asegurar que todos los tests pasan; consultar al usuario si surgen dudas
