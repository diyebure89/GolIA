# Plan de Implementación

- [ ] 1. Escribir la prueba de exploración de la condición del bug
  - **Property 1: Bug Condition** - Preservación de identidad en el ida y vuelta Entidad → Dominio → UI
  - **CRÍTICO**: Esta prueba DEBE FALLAR sobre el código sin arreglar; el fallo confirma que el bug existe
  - **NO intentes arreglar la prueba ni el código cuando falle**
  - **NOTA**: Esta prueba codifica el comportamiento esperado; validará el arreglo cuando pase tras la implementación
  - **OBJETIVO**: Exponer contraejemplos que demuestren la pérdida de identidad
  - **Enfoque PBT acotado**: al ser un bug determinista, acotar la propiedad a casos concretos que fallan: PKs no-UUID como `"match-1"` y `"1035048"` (parametrizado sobre varias PKs no-UUID)
  - Crear una prueba JVM JUnit4 en `app/src/test/java/com/diyebure/golia/data/local/entity/MatchEntityIdentityTest.java` (JVM pura, sin Android/Robolectric ni Room real, siguiendo la convención de los tests existentes)
  - Construir un `MatchEntity` con PK no-UUID (p. ej. `"match-1"`) y `externalId` no vacío, y afirmar que `MatchUiMapper.toUiModel(entity.toDomainModel()).getId()` es igual a la PK (`"match-1"`) — ver Bug Condition en el diseño
  - Añadir un caso de control con PK que es UUID canónico y afirmar que el id de UI == la PK (debería pasar ya)
  - Añadir un caso con un `MatchDao` en memoria (fake al estilo de `PredictionRepositoryUpsertResolverTest`) precargado con la fila de PK no-UUID, y afirmar que `getMatchById(uiId)` encuentra la fila
  - Ejecutar la prueba sobre el código SIN arreglar
  - **RESULTADO ESPERADO**: la prueba FALLA para las PKs no-UUID (esto es correcto: prueba que el bug existe)
  - Documentar los contraejemplos encontrados (p. ej. "toUiModel(toDomainModel(entity(\"match-1\"))).getId() devuelve un UUID aleatorio en lugar de \"match-1\"") para confirmar la causa raíz #1 del diseño
  - Si la prueba pasara inesperadamente sobre el código sin arreglar, re-hipotetizar (revisar la causa secundaria #4: `stableId` aleatorio por `externalId` ausente)
  - Marcar la tarea completa cuando la prueba esté escrita, ejecutada y el fallo documentado
  - _Requirements: 1.1, 1.2, 1.3_

- [ ] 2. Escribir las pruebas de preservación (ANTES de implementar el arreglo)
  - **Property 2: Preservation** - Comportamiento inalterado fuera de la condición del bug
  - **IMPORTANTE**: Seguir la metodología de observación primero
  - Observar sobre el código SIN arreglar: `toUiModel(toDomainModel(entity(uuidCanónico))).getId()` == el UUID canónico
  - Observar sobre el código SIN arreglar: para varios `externalId`, `MatchRepositoryImpl.stableId` devuelve `UUID.nameUUIDFromBytes(externalId)` (determinista)
  - Observar sobre el código SIN arreglar: `DetallePartidoViewModel.load(null)` y `load("")` emiten `MATCH_NOT_FOUND` de inmediato
  - Escribir pruebas parametrizadas (a modo de PBT) que fijen esos comportamientos observados a partir de las Preservation Requirements del diseño:
    - Varios UUID canónicos → id de UI == PK
    - Varios `externalId` → `stableId` estable e igual a `nameUUIDFromBytes`
    - `load(null)` / `load("")` → `MATCH_NOT_FOUND`
  - Ubicación: reutilizar/ampliar `MatchEntityIdentityTest.java` y, para la validación de `load`, un test JVM del ViewModel si el aislamiento lo permite; en caso contrario documentar la preservación de `load` como verificación por lectura de código
  - Ejecutar las pruebas sobre el código SIN arreglar
  - **RESULTADO ESPERADO**: las pruebas PASAN (confirma el comportamiento base a preservar)
  - Marcar la tarea completa cuando las pruebas estén escritas, ejecutadas y pasando sobre el código sin arreglar
  - _Requirements: 3.1, 3.2, 3.3, 3.4_

- [ ] 3. Arreglo para la pérdida de identidad del partido en la conversión Entidad → Dominio

  - [ ] 3.1 Implementar el arreglo (corrección acotada que preserva la identidad)
    - Modificar `MatchEntity.toDomainModel()` en `app/src/main/java/com/diyebure/golia/data/local/entity/MatchEntity.java` para que la identidad del partido se conserve aunque la PK no sea un UUID canónico
    - Aplicar la Variante A del diseño (recomendada, mínima): añadir al dominio `Match` un campo `String` dedicado que porte la PK original tal cual, rellenarlo en `toDomainModel` con `entity.id`, y hacer que `MatchUiMapper.toUiModel` emita esa PK original (con fallback a `id.toString()` cuando no esté presente), sin cambiar el tipo `UUID id` del dominio
    - Alternativamente, si tras medir el impacto se prefiere la Variante B (id de dominio como `String`), documentar el tradeoff y actualizar `stableId`, `fromDomainModel` y `onPredictClicked` en consecuencia
    - Mantener idéntica la ruta de PK con UUID canónico (Requisito 3.1) y NO tocar `stableId` ni `fromDomainModel` (Requisito 3.2) ni la validación de `load` (Requisito 3.3)
    - Añadir comentarios en español explicando por qué se preserva la PK original
    - _Bug_Condition: isBugCondition(entity) del diseño (PK no es UUID canónico parseable)_
    - _Expected_Behavior: MatchUiModel.id == MatchEntity.id y getMatchById(uiId) localiza la fila, del diseño_
    - _Preservation: Preservation Requirements del diseño_
    - _Requirements: 2.1, 2.2, 2.3, 3.1, 3.2, 3.3, 3.4_

  - [ ] 3.2 Verificar que la prueba de exploración ahora pasa
    - **Property 1: Expected Behavior** - Preservación de identidad en el ida y vuelta Entidad → Dominio → UI
    - **IMPORTANTE**: Re-ejecutar la MISMA prueba de la tarea 1 — NO escribir una prueba nueva
    - La prueba de la tarea 1 codifica el comportamiento esperado; cuando pase confirma que la identidad se preserva
    - **RESULTADO ESPERADO**: la prueba PASA (confirma que el bug está arreglado)
    - _Requirements: 2.1, 2.2, 2.3_

  - [ ] 3.3 Verificar que las pruebas de preservación siguen pasando
    - **Property 2: Preservation** - Comportamiento inalterado fuera de la condición del bug
    - **IMPORTANTE**: Re-ejecutar las MISMAS pruebas de la tarea 2 — NO escribir pruebas nuevas
    - **RESULTADO ESPERADO**: las pruebas PASAN (confirma que no hay regresiones)
    - Confirmar además que los tests JVM existentes (Prediction_ScorerTest, Season_Stats_CalculatorTest, PredictionRepositoryUpsertResolverTest, ProfileRepositoryChangePasswordTest, ScoreValidatorTest, ProfileValidatorTest) siguen pasando sin cambios
    - _Requirements: 3.1, 3.2, 3.3, 3.4_

- [ ] 4. Checkpoint - Asegurar que todas las pruebas pasan
  - Compilar con `gradlew` y ejecutar los tests unitarios JVM afectados
  - **NOTA de entorno**: si el terminal síncrono da salida vacía / exit -1 por contención de daemons de Gradle, ejecutar la build en segundo plano redirigiendo a un log y verificar leyendo el log y los XML de resultados de test (`app/build/test-results`)
  - **Limitación**: no hay emulador/dispositivo disponible; los `androidTest` instrumentados (Room real, migraciones) no pueden correr aquí — reportarlo como limitación, no como fallo
  - Asegurar que todas las pruebas pasan; si surgen dudas, preguntar al usuario
