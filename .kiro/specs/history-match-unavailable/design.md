# Diseño del Bugfix: Historial muestra "Partido no disponible" para partidos existentes

## Overview

El Historial de pronósticos resuelve cada `Prediction` contra su `Match` en la caché local por
`match_id`. El bug aparece cuando el `match_id` guardado NO es un UUID canónico (p. ej.
`"match-1"`, `"1035048"`): al leer, `PredictionEntity.toDomainModel()` intenta
`UUID.fromString(match_id)` dentro de un `try/catch` vacío, el parseo falla y `Prediction.matchId`
queda `null`. Después, `GetUserPredictionsUseCase.resolveMatch` invoca
`prediction.getMatchId().toString()`, que lanza `NullPointerException` (capturado como
`PERSISTENCE_ERROR`); y aun cuando no hubiese NPE, la identidad original ya se perdió, por lo que
`MatchDao.getMatchById` no localiza la fila y el historial muestra "Partido no disponible".

El arreglo sigue el patrón **Variante A** ya aplicado a `Match` (campo `rawId`): añadir a
`Prediction` un campo `String rawMatchId` que porte la PK original del partido tal cual, poblarlo
en `PredictionEntity.toDomainModel()`, y hacer que `GetUserPredictionsUseCase.resolveMatch` use
ese `rawMatchId` original para `getMatchById` en lugar de reconstruir el id vía `UUID.toString()`.
Esto preserva la identidad de extremo a extremo en la LECTURA sin cambiar el tipo `UUID` del
dominio `Prediction`, sin tocar el guardado, el índice único ni la lógica de puntuación.

## Glossary

- **Bug_Condition (C)**: la fila de pronóstico tiene un `match_id` cuya PK no es un UUID canónico
  parseable, por lo que el parseo a `UUID` pierde la identidad de búsqueda.
- **Property (P)**: al leer el historial, el partido referenciado se localiza en la caché usando
  el `match_id` original y se devuelve el `Match` (no `null`), sin `NullPointerException`.
- **Preservation**: el comportamiento para `match_id` con UUID canónico, para partidos realmente
  ausentes, el guardado (`buildUpsertEntity` e índice único) y la puntuación (`resolveForMatch`,
  `Prediction_Scorer`) permanecen inalterados.
- **PredictionEntity.toDomainModel()**: mapper Entidad→Dominio en
  `data/local/entity/PredictionEntity.java` que hoy pierde la PK no-UUID en `UUID.fromString`.
- **GetUserPredictionsUseCase.resolveMatch(...)**: en `domain/usecase/GetUserPredictionsUseCase.java`;
  resuelve el `Match` por `matchDao.getMatchById(prediction.getMatchId().toString())`.
- **rawMatchId**: nuevo campo `String` en `Prediction` que porta la PK ORIGINAL del partido
  (`PredictionEntity.match_id`), la identidad de búsqueda que usa `MatchDao.getMatchById`.
- **match_id**: PK del partido almacenada en la columna `match_id` de la tabla `predictions`;
  igual a `MatchEntity.id`. Puede no ser UUID canónico.

## Bug Details

### Bug Condition

El bug se manifiesta cuando un pronóstico persistido referencia un `match_id` que no es un UUID
canónico parseable. En la lectura, `PredictionEntity.toDomainModel()` descarta la PK (el parseo
`UUID.fromString` falla y el `catch` está vacío), dejando `Prediction.matchId = null`. Luego
`resolveMatch` o bien lanza NPE en `getMatchId().toString()`, o bien —si se hubiese parseado a un
UUID distinto— busca por un id que no coincide con la PK, de modo que `getMatchById` no encuentra
la fila.

**Especificación formal:**
```
FUNCTION isBugCondition(pred)
  INPUT: pred de tipo PredictionEntity (fila persistida en `predictions`)
  OUTPUT: boolean

  RETURN pred.match_id <> null
         AND NOT esUuidCanonico(pred.match_id)   // p. ej. "match-1", "1035048"
END FUNCTION
```

### Examples

- `match_id = "match-1"`, partido presente en caché con PK `"match-1"`:
  esperado el `Match` resuelto (Local vs Visitante); actual: `matchId=null` → NPE
  (`PERSISTENCE_ERROR`) o "Partido no disponible".
- `match_id = "1035048"`, partido presente con PK `"1035048"`:
  esperado `Match` resuelto; actual: identidad perdida → "Partido no disponible".
- `match_id = "9b1deb4d-3b7d-3b1a-8a1e-0b2c3d4e5f60"` (UUID canónico), partido presente
  (CONTROL): esperado y actual coinciden → `Match` resuelto correctamente.
- `match_id = "match-999"` con partido AUSENTE de la caché (borde): esperado `match = null` y
  "Partido no disponible" (comportamiento legítimo que debe preservarse).

## Expected Behavior

### Preservation Requirements

**Comportamientos inalterados:**
- Pronósticos con `match_id` UUID canónico deben seguir resolviéndose y mostrándose igual.
- Pronósticos cuyo partido realmente no está en caché deben seguir mostrando "Partido no
  disponible" sin romper la lista completa.
- El guardado (`PredictionRepositoryImpl.buildUpsertEntity`) persiste `match_id` tal cual; el
  índice único `(user_id, match_id)` y su semántica de upsert no cambian.
- La resolución/puntuación (`resolveForMatch`, `Prediction_Scorer`) no cambia.
- Todos los tests JVM existentes siguen pasando.

**Alcance:**
Todas las entradas que NO cumplen la condición del bug (`NOT isBugCondition`) deben quedar
completamente inalteradas por este arreglo. Esto incluye:
- Pronósticos con `match_id` UUID canónico.
- Pronósticos con partido realmente ausente de la caché.
- Rutas de guardado y de puntuación/resolución.

## Hypothesized Root Cause

Con base en el análisis del bug y la lectura del código, las causas probables son:

1. **Pérdida de identidad por parseo UUID (causa principal)**: `PredictionEntity.toDomainModel()`
   convierte `match_id` con `UUID.fromString` en un `try/catch` vacío. Para PKs no-UUID el parseo
   lanza `IllegalArgumentException`, se ignora, y `Prediction.matchId` queda `null` (mismo patrón
   que afectaba a `Match` antes del `rawId`).

2. **NPE en la resolución**: `resolveMatch` hace `prediction.getMatchId().toString()`. Con
   `matchId == null` se produce un `NullPointerException`, capturado en `load` como
   `PERSISTENCE_ERROR`.

3. **Búsqueda por identidad incorrecta**: aunque no hubiese `null`, reconstruir el id vía
   `UUID.toString()` no recupera una PK no-UUID original, por lo que `getMatchById` no localiza la
   fila (equivale a "Partido no disponible").

4. **Tipo del dominio**: `Prediction.matchId` es `UUID` y no puede portar una PK arbitraria; hace
   falta un canal `String` (Variante A) para transportar la identidad real de búsqueda.

La prueba de exploración (Tarea 1) confirmará o refutará estas hipótesis: si el `Match` resuelto
es `null`/hay NPE para `match_id = "match-1"` y el caso de control UUID pasa, se confirma la causa
#1–#3.

## Correctness Properties

Property 1: Bug Condition - Identidad del match_id preservada en la lectura del historial

_For any_ pronóstico persistido cuya PK de partido (`match_id`) cumple la condición del bug
(`isBugCondition` devuelve verdadero: no es un UUID canónico) y cuyo partido existe en la caché,
la función arreglada SHALL conservar el `match_id` original de extremo a extremo y resolver el
`Match` correspondiente (no `null`), sin lanzar `NullPointerException`.

**Validates: Requirements 2.1, 2.2, 2.3**

Property 2: Preservation - Comportamiento inalterado fuera de la condición del bug

_For any_ entrada donde la condición del bug NO se cumple (`isBugCondition` devuelve falso:
`match_id` con UUID canónico, o partido realmente ausente de la caché, o rutas de guardado y
puntuación), la función arreglada SHALL producir el mismo resultado que la función original,
preservando la resolución del historial, el placeholder para partidos ausentes, el guardado con
índice único y la puntuación.

**Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5**

## Fix Implementation

### Changes Required

Suponiendo que el análisis de causa raíz es correcto (Variante A, mínimo, solo en la LECTURA):

**Archivo 1**: `app/src/main/java/com/diyebure/golia/domain/model/Prediction.java`

**Cambio**: añadir un campo `String rawMatchId` con getter/setter, documentado igual que
`Match.rawId`. Porta la PK ORIGINAL del partido (`PredictionEntity.match_id`) sin depender del
parseo a `UUID`. Puede ser `null` para pronósticos construidos en memoria que no provienen de
Room (en ese caso se cae al comportamiento previo). NO se cambia el tipo `UUID matchId`.

**Archivo 2**: `app/src/main/java/com/diyebure/golia/data/local/entity/PredictionEntity.java`

**Cambio**: en `toDomainModel()`, antes/además del intento de `UUID.fromString(matchId)`, poblar
`prediction.setRawMatchId(matchId)` con el `String` crudo cuando `match_id` no es nulo/vacío. El
parseo a `UUID` se mantiene (comportamiento preservado para PKs canónicas), pero la identidad de
búsqueda ya no depende de él.

**Archivo 3**: `app/src/main/java/com/diyebure/golia/domain/usecase/GetUserPredictionsUseCase.java`

**Cambio**: en `resolveMatch`, resolver el id de búsqueda priorizando `rawMatchId` cuando esté
presente; solo caer a `matchId.toString()` si `rawMatchId` es nulo. Además, evitar el NPE cuando
`matchId` es `null` pero hay `rawMatchId`. Pseudocódigo:
```
matchIdString := prediction.getRawMatchId() != null
                    ? prediction.getRawMatchId()
                    : (prediction.getMatchId() != null ? prediction.getMatchId().toString() : null)
if matchIdString == null: return null
entity := matchDao.getMatchById(matchIdString)
return entity != null ? entity.toDomainModel() : null
```
Esto mantiene el contrato de devolver `null` (placeholder) cuando el partido no existe (R3.2) y
no lanza NPE.

**No se modifica**: `buildUpsertEntity`, el índice único `(user_id, match_id)`, `resolveForMatch`,
`Prediction_Scorer`, ni el mapper de UI del historial.

## Testing Strategy

### Validation Approach

Enfoque en dos fases: primero surgir contraejemplos que demuestren el bug sobre el código SIN
arreglar; después verificar que el arreglo funciona y preserva el comportamiento existente.
Pruebas JVM puras (JUnit4), sin Android/Robolectric ni Room real, siguiendo la convención de
`PredictionRepositoryUpsertResolverTest` y `MatchEntityIdentityTest` (con un `FakeMatchDao` en
memoria precargado). `build.gradle` ya define `testOptions.unitTests.returnDefaultValues=true`.

### Exploratory Bug Condition Checking

**Goal**: surgir contraejemplos que demuestren el bug ANTES de implementar el arreglo. Confirmar
o refutar el análisis de causa raíz; si se refuta, re-hipotetizar.

**Test Plan**: construir una `PredictionEntity` con `match_id` no-UUID (`"match-1"`), aplicar
`toDomainModel()` y luego el emparejamiento equivalente a `resolveMatch` (usando un `FakeMatchDao`
precargado con una `MatchEntity` de PK `"match-1"`), y comprobar que el `Match` resuelto NO es
`null` y corresponde a esa PK. Ejecutar sobre el código SIN arreglar para observar el fallo.

**Test Cases**:
1. **PK alfanumérica no-UUID**: `match_id = "match-1"` con partido presente; el `Match` resuelto
   debe corresponder a `"match-1"` (fallará en código sin arreglar: `matchId=null` → NPE o no
   resuelto).
2. **PK numérica no-UUID**: `match_id = "1035048"` con partido presente; ídem (fallará).
3. **CONTROL — UUID canónico**: `match_id` UUID canónico con partido presente; debe resolver ya
   sobre el código sin arreglar (confirma que la causa es la pérdida de identidad por parseo).

**Expected Counterexamples**:
- Para `match_id = "match-1"`: `Prediction.matchId == null` tras `toDomainModel()`; la resolución
  lanza NPE o devuelve `null` → el partido existente no se resuelve.
- Causas posibles: parseo UUID que descarta la PK, NPE en `getMatchId().toString()`, búsqueda por
  identidad incorrecta.

### Fix Checking

**Goal**: verificar que para toda entrada que cumple la condición del bug, la función arreglada
produce el comportamiento esperado.

**Pseudocode:**
```
FOR ALL pred WHERE isBugCondition(pred) DO
  prediction := pred.toDomainModel_fixed()
  match := resolveMatch_fixed(prediction)
  ASSERT match != null AND identidad(match) = pred.match_id
END FOR
```

### Preservation Checking

**Goal**: verificar que para toda entrada que NO cumple la condición del bug, la función arreglada
produce el mismo resultado que la original.

**Pseudocode:**
```
FOR ALL pred WHERE NOT isBugCondition(pred) DO
  ASSERT resolveMatch_original(pred) = resolveMatch_fixed(pred)
END FOR
```

**Testing Approach**: se recomienda testing basado en propiedades para la preservación porque:
- Genera muchos casos automáticamente sobre el dominio de entrada.
- Cubre bordes que las pruebas unitarias manuales podrían omitir.
- Da garantías fuertes de que el comportamiento no cambia para las entradas no-buggy.

**Test Plan**: observar primero el comportamiento sobre el código SIN arreglar para UUID canónico
y para partidos ausentes; luego escribir pruebas (acotadas como PBT determinista) que fijen ese
comportamiento y verificar que pasan sobre el código sin arreglar.

**Test Cases**:
1. **Preservación UUID canónico**: varios `match_id` UUID canónicos con partido presente resuelven
   correctamente antes y después del arreglo.
2. **Preservación partido ausente**: `match_id` con partido NO presente devuelve `match = null`
   (placeholder) antes y después.
3. **Preservación de guardado/puntuación**: `PredictionRepositoryUpsertResolverTest` sigue en
   verde (upsert por índice único e idempotencia del resolver sin cambios).

### Unit Tests

- Resolución del historial para `match_id` no-UUID con partido presente (fix).
- Resolución para `match_id` UUID canónico con partido presente (preservación/control).
- Resolución para partido ausente → `null` (preservación de borde).
- Ausencia de NPE cuando `Prediction.matchId` es `null` pero hay `rawMatchId`.

### Property-Based Tests

- Conjunto acotado de PKs no-UUID (`"match-1"`, `"1035048"`) → el `Match` resuelto corresponde a
  la PK original (Property 1).
- Conjunto acotado de UUID canónicos → resolución preservada (Property 2).
- Conjunto de PKs ausentes de la caché → `null` preservado (Property 2).

### Integration Tests

- Flujo completo del Historial con pronósticos de `match_id` no-UUID: el partido se muestra en
  lugar de "Partido no disponible" (requiere emulador; no ejecutable en este entorno JVM — se
  reporta como limitación, no como fallo).
- Verificación por lectura de código de `HistorialViewModel.buildMatchLabel`: al recibir un
  `Match` no nulo con nombres de equipo, produce "Local vs Visitante" en lugar del placeholder.
