# Documento de Requisitos del Bugfix

## Introducción

En la pantalla de Historial de pronósticos (`HistorialPronosticosActivity`), algunas filas
muestran el texto de reserva "Partido no disponible" (string `historial_partido_no_disponible`
/ constante `MATCH_UNAVAILABLE_LABEL` en `HistorialViewModel`) para pronósticos cuyo partido SÍ
existe en la caché local de Room. En ciertos casos aparece además un error técnico
(`PERSISTENCE_ERROR`) causado por un `NullPointerException`.

Este bug pertenece al MISMO linaje que el arreglado en el spec `match-detail-not-found`: una
pérdida silenciosa de identidad al convertir la clave primaria (PK) de un partido a `UUID`
canónico. Aquí ocurre en la LECTURA del historial, no en el guardado.

Origen del defecto (confirmado por lectura de código):

- **Guardado (correcto):** `PredictionRepositoryImpl.buildUpsertEntity` persiste
  `PredictionEntity.match_id` con el valor de `matchId` (el `EXTRA_MATCH_ID`, que es la PK real
  del partido / `MatchEntity.id`) TAL CUAL, como `String`. Esa PK puede NO ser un UUID canónico
  (p. ej. `"match-1"` o `"1035048"`).
- **Lectura (defectuosa):**
  1. `PredictionEntity.toDomainModel()` convierte `match_id` (`String`) a `UUID matchId` con
     `UUID.fromString(matchId)` dentro de un `try/catch` vacío. Para una PK no-UUID el parseo
     falla y `Prediction.matchId` queda `null` (lo mismo aplica a `id` y `userId`).
  2. `GetUserPredictionsUseCase.resolveMatch` hace
     `matchDao.getMatchById(prediction.getMatchId().toString())`. Si `matchId` es `null` se
     produce un NPE (capturado como `PERSISTENCE_ERROR`). Si el `match_id` original no era un
     UUID canónico, la identidad ya se perdió en el paso 1 y la búsqueda no localiza el partido,
     por lo que `HistorialViewModel` muestra "Partido no disponible".

El dominio `Prediction` tipa `id`/`userId`/`matchId` como `java.util.UUID`, por lo que no puede
portar una PK no-UUID sin pérdida, igual que ocurría con `Match`.

## Bug Analysis

### Current Behavior (Defect)

Cuando un pronóstico persistido referencia un partido cuya PK (`match_id`) NO es un UUID
canónico, aunque el partido exista en la caché local:

1.1 WHEN un pronóstico tiene `match_id` con una PK no-UUID (p. ej. `"match-1"`) y el partido
existe en la caché THEN el sistema pierde la identidad al convertir a `UUID` en
`PredictionEntity.toDomainModel()` (queda `Prediction.matchId = null`)

1.2 WHEN `resolveMatch` intenta resolver un pronóstico cuyo `Prediction.matchId` es `null` THEN
el sistema lanza un `NullPointerException` en `prediction.getMatchId().toString()`, que se
captura como `PERSISTENCE_ERROR`

1.3 WHEN el historial no logra resolver el partido de un pronóstico cuyo partido sí existe THEN
el sistema muestra el texto de reserva "Partido no disponible" en esa fila

### Expected Behavior (Correct)

2.1 WHEN un pronóstico tiene `match_id` con una PK no-UUID y el partido existe en la caché THEN
el sistema SHALL conservar la identidad original del `match_id` de extremo a extremo en la
lectura (sin depender del parseo a `UUID`)

2.2 WHEN `resolveMatch` resuelve un pronóstico cuyo partido existe en la caché THEN el sistema
SHALL localizar ese partido usando el `match_id` original y devolver el `Match` resuelto (no
`null`), sin lanzar `NullPointerException`

2.3 WHEN el partido de un pronóstico existe en la caché THEN el sistema SHALL mostrar el partido
(p. ej. "Local vs Visitante") en lugar de "Partido no disponible"

### Unchanged Behavior (Regression Prevention)

3.1 WHEN un pronóstico tiene `match_id` con un UUID canónico y el partido existe en la caché
THEN el sistema SHALL CONTINUE TO resolver y mostrar el partido correctamente

3.2 WHEN el partido referenciado por un pronóstico realmente NO existe en la caché THEN el
sistema SHALL CONTINUE TO devolver `match = null` y mostrar "Partido no disponible" sin fallar
la lista completa

3.3 WHEN se guarda un pronóstico (`buildUpsertEntity`, índice único `(user_id, match_id)`) THEN
el sistema SHALL CONTINUE TO persistir `match_id` tal cual y respetar la unicidad como antes

3.4 WHEN se resuelve/puntúa un pronóstico (`resolveForMatch`, `Prediction_Scorer`) THEN el
sistema SHALL CONTINUE TO calcular puntos y estado sin cambios

3.5 WHEN se ejecutan los tests JVM existentes (`Prediction_ScorerTest`,
`Season_Stats_CalculatorTest`, `PredictionRepositoryUpsertResolverTest`,
`ProfileRepositoryChangePasswordTest`, `ScoreValidatorTest`, `ProfileValidatorTest`,
`MatchEntityIdentityTest`) THEN el sistema SHALL CONTINUE TO pasarlos todos en verde

## Condición del Bug (derivada)

**Función de condición del bug** — identifica las entradas que disparan el defecto:

```pascal
FUNCTION isBugCondition(pred)
  INPUT: pred de tipo PredictionEntity (fila persistida)
  OUTPUT: boolean

  // La PK del partido referenciada NO es un UUID canónico parseable,
  // por lo que el parseo a UUID en toDomainModel() pierde la identidad.
  RETURN pred.match_id <> null
         AND NOT esUuidCanonico(pred.match_id)
END FUNCTION
```

Ejemplo: `isBugCondition` es verdadero para `match_id = "match-1"` o `match_id = "1035048"`.

**Especificación de la propiedad** — comportamiento correcto para las entradas buggy:

```pascal
// Propiedad: Fix Checking — la identidad del match_id se preserva en la lectura
FOR ALL pred WHERE isBugCondition(pred) DO
  prediction ← pred.toDomainModel'()
  match ← resolveMatch'(prediction)     // usando el match_id ORIGINAL
  ASSERT match <> null
         AND identidad(match) = pred.match_id   // localiza la fila correcta
END FOR
```

**Definiciones clave:**
- **F**: función original (sin arreglo) — `toDomainModel` + `resolveMatch` actuales.
- **F'**: función arreglada — tras preservar el `match_id` original (Variante A).

**Meta de preservación:**

```pascal
// Propiedad: Preservation Checking
FOR ALL pred WHERE NOT isBugCondition(pred) DO
  ASSERT F(pred) = F'(pred)   // UUID canónico y partidos realmente ausentes: sin cambios
END FOR
```
