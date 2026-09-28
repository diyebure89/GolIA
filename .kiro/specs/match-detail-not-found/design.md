# Diseño del Bugfix: Detalle del Partido no encuentra el partido (MATCH_NOT_FOUND)

## Overview

El bug se manifiesta cuando el usuario abre el Detalle de un partido que sí aparece listado y recibe `PredictionError.MATCH_NOT_FOUND`. La causa raíz hipotetizada es la pérdida silenciosa de la identidad del partido al reconstruir el modelo de dominio desde la caché de Room.

La estrategia de arreglo es **acotada y de mínimo impacto**: conservar la identidad del partido en `MatchEntity.toDomainModel()` en lugar de descartarla. Hoy la conversión Entidad → Dominio hace `new Match()` (que en su constructor asigna un `UUID.randomUUID()`) y luego intenta `match.setId(UUID.fromString(entity.id))` dentro de un `try/catch` que ignora en silencio `IllegalArgumentException`. Si la PK almacenada no es un UUID canónico, el parseo falla y el `Match` queda con el UUID **aleatorio** del constructor. Ese id ya no coincide con la PK, viaja hasta `MatchUiModel.id` y el Detalle busca una fila inexistente.

Existe un tradeoff de tipado: el dominio `Match` modela el id como `java.util.UUID`, por lo que no puede representar directamente una PK no-UUID. Cambiar el tipo del id de dominio a `String` sería lo más correcto conceptualmente, pero es invasivo: `MatchRepositoryImpl.stableId` devuelve `UUID`, `onPredictClicked` hace `loadedMatch.getId().toString()`, `fromDomainModel` hace `match.getId().toString()`, y varios tests construyen `Match` con `UUID`. El diseño propone la corrección acotada que preserva la identidad sin cambiar el tipo del dominio (ver "Fix Implementation"), y documenta explícitamente el tradeoff.

## Glossary

- **Bug_Condition (C)**: La condición que dispara el bug — reconstruir un `Match` desde una `MatchEntity` cuya PK (`id`) no es un UUID canónico parseable, provocando que el dominio adopte un id que no coincide con la PK.
- **Property (P)**: El comportamiento deseado — la identidad del partido se conserva de extremo a extremo, de modo que `MatchUiModel.id` sea igual a la PK persistida y el Detalle localice la fila.
- **Preservation**: El comportamiento de los partidos con PK que sí es UUID canónico, la generación determinista de `stableId`, la validación de id nulo/vacío en `load`, y los tests unitarios existentes, que deben permanecer inalterados.
- **Identity_Roundtrip**: El recorrido `MatchEntity.id` → `MatchEntity.toDomainModel()` → `Match.getId()` → `MatchUiMapper.toUiModel()` → `MatchUiModel.id`, que debe devolver un valor igual a `MatchEntity.id`.
- **stableId**: Método de `MatchRepositoryImpl` que deriva un `UUID` determinista del `externalId` (`UUID.nameUUIDFromBytes`) o un `UUID` aleatorio si no hay `externalId`.
- **PK (clave primaria)**: La columna `id` (String) de la tabla `matches`, que `MatchDao.getMatchById` usa en `WHERE id = :matchId`.

## Bug Details

### Bug Condition

El bug se manifiesta cuando `MatchEntity.toDomainModel()` reconstruye un `Match` desde una fila cuya PK (`id`) no es un UUID canónico parseable. El constructor `new Match()` asigna un `UUID.randomUUID()`; luego `UUID.fromString(entity.id)` lanza `IllegalArgumentException`, el `catch` la ignora, y el `Match` conserva el UUID aleatorio, que no coincide con la PK almacenada. Esa desincronización de identidad se propaga a `MatchUiModel.id` y hace que el Detalle no encuentre el partido.

**Formal Specification:**
```
FUNCTION isBugCondition(entity)
  INPUT: entity of type MatchEntity (fila persistida en Room)
  OUTPUT: boolean

  // La PK existe pero no es un UUID canónico parseable,
  // por lo que toDomainModel no puede reconstruir la identidad correcta.
  RETURN entity.id != null
         AND NOT isParseableCanonicalUuid(entity.id)
END FUNCTION

FUNCTION isParseableCanonicalUuid(s)
  // true si UUID.fromString(s) no lanza y UUID.toString() reproduce s
  RETURN tryParseUuid(s) succeeds AND UUID.fromString(s).toString() == s
END FUNCTION
```

### Examples

- **Fila no-UUID (defecto)**: `MatchEntity.id = "match-1"`, `externalId = "39-2024-100"`. `toDomainModel()` produce `Match.id = <UUID aleatorio>`. `MatchUiMapper.toUiModel(match).getId()` ≠ `"match-1"`. `load(uiId)` → `getMatchById(uiId)` → sin fila → `MATCH_NOT_FOUND`. Esperado: `MatchUiModel.id == "match-1"` y el Detalle encuentra la fila.
- **Fila no-UUID numérica (defecto)**: `MatchEntity.id = "1035048"`. Mismo síntoma: id de UI aleatorio, Detalle falla. Esperado: id de UI == `"1035048"`.
- **Fila con UUID canónico (no dispara el bug)**: `MatchEntity.id = "9b1deb4d-3b7d-3b1a-8a1e-0b2c3d4e5f60"`. `toDomainModel()` parsea correctamente; `MatchUiModel.id` == PK; el Detalle encuentra la fila. Debe seguir funcionando igual.
- **Caso límite id vacío en load**: `load("")` o `load(null)` debe seguir devolviendo `MATCH_NOT_FOUND` de inmediato (validación de entrada, no parte del arreglo de identidad).

## Expected Behavior

### Preservation Requirements

**Comportamientos Inalterados:**
- Los partidos cuya PK ya es un UUID canónico deben reconstruirse con el mismo id y producir el mismo `MatchUiModel.id` que hoy.
- `MatchRepositoryImpl.stableId` debe seguir generando el id determinista con `UUID.nameUUIDFromBytes(externalId)` (y aleatorio sin `externalId`), y `fromDomainModel` debe seguir persistiendo la PK de forma estable entre refrescos.
- `DetallePartidoViewModel.load(null)` y `load("")` deben seguir emitiendo `MATCH_NOT_FOUND` de inmediato, sin consultar la caché.
- Todos los tests unitarios JVM existentes deben seguir pasando sin cambios de comportamiento.

**Alcance:**
Toda entrada que NO cumpla la Bug_Condition debe quedar completamente inalterada por el arreglo. Esto incluye:
- Filas con PK que es UUID canónico parseable.
- La ruta de refresco desde la red (que asigna `stableId`, siempre UUID canónico).
- Las llamadas a `load` con id nulo/vacío.
- La lógica de puntuación, estadísticas y validación ajena al mapeo de identidad.

> El comportamiento correcto concreto para las entradas que sí disparan el bug se define en la sección "Correctness Properties" (Property 1). Esta sección se centra en lo que NO debe cambiar.

## Hypothesized Root Cause

Con base en el análisis del bug, las causas más probables son:

1. **Pérdida de identidad en `MatchEntity.toDomainModel()` (causa principal)**: El patrón `new Match()` (constructor con `UUID.randomUUID()`) seguido de `try { setId(UUID.fromString(id)) } catch (IllegalArgumentException ignored) {}` deja un id **aleatorio** cuando la PK no es un UUID canónico. El id resultante ni es null ni coincide con la PK.
   - El dominio `Match` tipa el id como `UUID`, por lo que no puede representar una PK no-UUID sin transformación.
   - El `catch` vacío convierte un fallo de datos en una corrupción silenciosa de identidad.

2. **Origen de PKs no-UUID**: Filas persistidas por versiones anteriores de la app, migraciones, o cualquier ruta que haya insertado un `id` no canónico (p. ej. `externalId` crudo usado como PK en el pasado). La ruta actual de refresco asigna `stableId` (UUID canónico), pero las filas antiguas sobreviven en la caché.

3. **`MatchUiMapper` enmascara el defecto**: `m.getId() != null ? m.getId().toString() : ""` produce un id no vacío (el aleatorio), por lo que la validación de `load` (que solo rechaza null/vacío) no lo detiene; la consulta llega a Room y falla por no-coincidencia de PK.

4. **`stableId` con `externalId` ausente (causa secundaria a descartar)**: Si un partido llega sin `externalId`, `stableId` devuelve un UUID aleatorio en cada refresco, lo que puede duplicar filas. No es la causa del `MATCH_NOT_FOUND` observado (ese id sí es UUID canónico y se persiste consistentemente en un mismo refresco), pero la prueba de exploración debe confirmar que la causa observada es la #1 y no esta.

## Correctness Properties

Property 1: Bug Condition - Preservación de identidad en el ida y vuelta Entidad → Dominio → UI

_For any_ entrada donde la condición del bug se cumple (`isBugCondition` devuelve true: la PK no es un UUID canónico parseable), la función corregida SHALL reconstruir el `Match` de forma que `MatchUiMapper.toUiModel(match).getId()` sea no vacío e igual a la PK original (`MatchEntity.id`), y que una consulta por ese id localice la misma fila (no se produce `MATCH_NOT_FOUND`).

**Validates: Requirements 2.1, 2.2, 2.3**

Property 2: Preservation - Comportamiento inalterado fuera de la condición del bug

_For any_ entrada donde la condición del bug NO se cumple (`isBugCondition` devuelve false: la PK es un UUID canónico parseable, o la entrada corresponde a `stableId`/refresco/`load` con id nulo-vacío), la función corregida SHALL producir el mismo resultado que la función original, preservando el `MatchUiModel.id` igual a la PK, la generación determinista de `stableId` y la validación de id nulo/vacío en `load`.

**Validates: Requirements 3.1, 3.2, 3.3, 3.4**

## Fix Implementation

### Changes Required

Asumiendo que el análisis de causa raíz es correcto (a confirmar con la prueba de exploración antes de arreglar):

**Archivo**: `app/src/main/java/com/diyebure/golia/data/local/entity/MatchEntity.java`

**Función**: `toDomainModel()`

**Cambios específicos (corrección acotada, sin cambiar el tipo del id de dominio):**

1. **Preservar la identidad de forma determinista**: En lugar de dejar un UUID aleatorio cuando `UUID.fromString(id)` falla, derivar un UUID **estable a partir de la PK** para no perder la correspondencia. Estrategia:
   - Si `id` es un UUID canónico parseable → `match.setId(UUID.fromString(id))` (comportamiento actual, preservado).
   - Si `id` NO es parseable como UUID canónico → `match.setId(UUID.nameUUIDFromBytes(id.getBytes(UTF_8)))`, obteniendo un UUID determinista y estable derivado de la PK.
   - Aplicar la **misma** transformación determinista en `MatchUiMapper` **no** es necesario: la clave es que el id sea estable y reversible al buscar. Para que `MatchDao.getMatchById` encuentre la fila, el id que llega a `load` debe ser el que está en la columna `id`.

2. **Garantizar la coincidencia con la PK que consulta el Detalle (decisión de diseño)**: Como `MatchDao.getMatchById` filtra por la columna `id` (la PK original), el id que llega desde la UI debe ser exactamente esa PK. Por tanto la corrección real debe asegurar que `MatchUiModel.id == MatchEntity.id`. Dado que el dominio tipa el id como `UUID` y no puede portar una PK no-UUID sin pérdida, se elige la opción robusta y mínima: **exponer la PK original a través del dominio sin depender del parseo UUID**. Dos variantes válidas, a decidir en implementación según impacto medido:
   - **Variante A (recomendada, mínima):** añadir al dominio `Match` un campo `String rawId` (o reutilizar `externalId` no; usar un campo dedicado) que porte la PK original tal cual. `toDomainModel` lo rellena con `entity.id`; `MatchUiMapper` emite `rawId` si está presente, cayendo a `id.toString()` si no. Esto conserva `UUID id` para todo el código existente (tests, `stableId`, `fromDomainModel`, `onPredictClicked`) y añade solo un campo que transporta la identidad de búsqueda.
   - **Variante B (alternativa):** cambiar el tipo del id de dominio a `String`. Más correcta conceptualmente pero invasiva (afecta `stableId`, `fromDomainModel`, `onPredictClicked` y varios tests). Se documenta como tradeoff; no se recomienda para un bugfix mínimo.
   - La prueba de exploración (Property 1) fijará el criterio observable (`MatchUiModel.id == MatchEntity.id`) y la implementación elegirá la variante que lo satisfaga sin romper la preservación.

3. **Mantener intacta la ruta UUID canónica**: cuando la PK ya es UUID canónico, el resultado debe ser idéntico al actual (Property 2 / Requisito 3.1).

4. **No tocar `stableId` ni `fromDomainModel`**: la generación determinista y la persistencia se mantienen (Requisito 3.2). El refresco de red sigue produciendo PKs UUID canónicas.

5. **No tocar la validación de `load`**: `load(null)`/`load("")` siguen devolviendo `MATCH_NOT_FOUND` de inmediato (Requisito 3.3).

> Nota de tradeoff: la Variante A es la de menor riesgo porque no altera la firma `UUID getId()` de la que dependen tests y `stableId`. La decisión final se toma en implementación tras confirmar con la prueba de exploración que la causa observada es la #1.

## Testing Strategy

### Validation Approach

La estrategia sigue dos fases: primero exponer contraejemplos que demuestren el bug sobre el código SIN arreglar; después verificar que el arreglo funciona y que preserva el comportamiento existente.

Todas las pruebas son **JVM puras (JUnit4)**, siguiendo la convención del proyecto (`app/src/test/java/com/diyebure/golia/...`, sin Android/Robolectric, sin Room real). No hay librería de property-based testing en el proyecto; las "property-based tests" se implementan como pruebas parametrizadas sobre un conjunto representativo de entradas (varios ids no-UUID y varios UUID canónicos) que cubren el dominio de forma efectiva. No requieren emulador.

### Exploratory Bug Condition Checking

**Goal**: Exponer contraejemplos que demuestren el bug ANTES de implementar el arreglo. Confirmar o refutar la causa raíz (pérdida de identidad en `toDomainModel`). Si se refuta, re-hipotetizar (p. ej. desincronización por `stableId` aleatorio).

**Test Plan**: Construir `MatchEntity` con distintas PKs y verificar el ida y vuelta Entidad → Dominio → `MatchUiModel.id`. Ejecutar sobre el código SIN arreglar para observar el fallo con las PKs no-UUID.

**Test Cases**:
1. **PK no-UUID (`"match-1"`)**: `toUiModel(toDomainModel(entity)).getId()` debe ser igual a `"match-1"` (fallará en código sin arreglar: será un UUID aleatorio).
2. **PK no-UUID numérica (`"1035048"`)**: mismo criterio (fallará en código sin arreglar).
3. **PK UUID canónico**: `toUiModel(toDomainModel(entity)).getId()` == la PK (debería pasar ya en código sin arreglar; sirve de control).
4. **Simulación de búsqueda en caché**: con un DAO en memoria (fake) precargado con la fila de PK no-UUID, comprobar que `getMatchById(uiId)` encuentra la fila (fallará en código sin arreglar porque `uiId` es aleatorio).

**Expected Counterexamples**:
- `toUiModel(toDomainModel(entity("match-1", ...))).getId()` devuelve un UUID aleatorio (p. ej. `"3f2a...":`), no `"match-1"`.
- Causas posibles: `catch` vacío en `toDomainModel`, constructor `Match()` con `UUID.randomUUID()`, tipado `UUID` del id de dominio.

### Fix Checking

**Goal**: Verificar que para toda entrada que cumple la condición del bug, la función corregida produce el comportamiento esperado.

**Pseudocode:**
```
FOR ALL entity WHERE isBugCondition(entity) DO
  match  := MatchEntity.toDomainModel_fixed(entity)
  uiId   := MatchUiMapper.toUiModel(match).getId()
  ASSERT uiId == entity.id           // id de UI igual a la PK
  ASSERT dao.getMatchById(uiId) != null   // el Detalle localiza la fila
END FOR
```

### Preservation Checking

**Goal**: Verificar que para toda entrada que NO cumple la condición del bug, la función corregida produce el mismo resultado que la original.

**Pseudocode:**
```
FOR ALL entity WHERE NOT isBugCondition(entity) DO
  ASSERT MatchEntity.toDomainModel_original(entity) EQUIV MatchEntity.toDomainModel_fixed(entity)
  // en particular, para PK UUID canónico: mismo Match.id y mismo MatchUiModel.id
END FOR

FOR ALL match WITH externalId no vacío DO
  ASSERT stableId(match) sigue siendo UUID.nameUUIDFromBytes(externalId)  // sin cambios
END FOR

ASSERT load(null) == MATCH_NOT_FOUND AND load("") == MATCH_NOT_FOUND      // sin cambios
```

**Testing Approach**: Se recomienda el enfoque de propiedades (pruebas parametrizadas sobre múltiples entradas) para la preservación porque:
- Cubre muchos casos del dominio de entrada de forma automática.
- Detecta casos límite que un test unitario aislado podría omitir.
- Da mayor garantía de que el comportamiento no cambia para todas las entradas no-bug.

**Test Plan**: Observar primero el comportamiento del código SIN arreglar para PKs UUID canónicas y para `stableId`, y escribir pruebas que fijen ese comportamiento observado.

**Test Cases**:
1. **Preservación UUID canónico**: `toUiModel(toDomainModel(entity(uuid))).getId()` == uuid (pasa en código sin arreglar; debe seguir pasando).
2. **Preservación de `stableId`**: para varios `externalId`, `stableId` == `UUID.nameUUIDFromBytes(externalId)` (determinista, sin cambios).
3. **Preservación de validación de `load`**: `load(null)` y `load("")` emiten `MATCH_NOT_FOUND` (sin cambios).

### Unit Tests

- Ida y vuelta Entidad → Dominio → `MatchUiModel.id` para PKs no-UUID (arreglo) y UUID canónicas (preservación).
- Caso límite: PK vacía y `load` con id nulo/vacío.
- `stableId` determinista para varios `externalId`.

### Property-Based Tests

- Pruebas parametrizadas con un conjunto de PKs no-UUID variadas (alfanuméricas, numéricas, con guiones no canónicos) verificando `MatchUiModel.id == PK`.
- Pruebas parametrizadas con varios UUID canónicos verificando preservación del ida y vuelta.
- Pruebas parametrizadas con varios `externalId` verificando que `stableId` no cambia.

### Integration Tests

- Con un `MatchDao` en memoria (fake al estilo `PredictionRepositoryUpsertResolverTest`) precargado con filas de PK no-UUID y UUID: verificar que el id que produce la lista localiza la fila vía `getMatchById` (sin `MATCH_NOT_FOUND`).
- Verificar el flujo completo lista → id de UI → `getMatchById` para ambos tipos de PK.
- **Limitación de entorno**: no hay emulador/dispositivo disponible, por lo que los `androidTest` instrumentados (Room real, `MigrationTestHelper`) NO pueden ejecutarse aquí; se reporta como limitación, no como fallo. La cobertura se logra con fakes JVM.
