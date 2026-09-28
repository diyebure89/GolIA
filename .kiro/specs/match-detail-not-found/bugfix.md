# Documento de Requisitos del Bugfix

## Introducción

Al pulsar una tarjeta de partido en la Pantalla de Inicio o en la Pantalla de Partidos, la app navega a la pantalla de Detalle del Partido (`DetallePartidoActivity`) pasando `Constants.EXTRA_MATCH_ID`. En determinadas condiciones el Detalle muestra el error "No se encontró el partido" (`PredictionError.MATCH_NOT_FOUND`) para un partido que sí está listado en la pantalla anterior.

La causa está en la pérdida silenciosa de la identidad del partido al reconstruir el modelo de dominio desde la caché de Room. El dominio `Match` modela el id como `java.util.UUID`. Cuando `MatchEntity.toDomainModel()` reconstruye el `Match`, primero crea `new Match()` (cuyo constructor asigna un `UUID.randomUUID()`) y luego intenta sobrescribir el id con `UUID.fromString(entity.id)` dentro de un `try/catch` que **ignora en silencio** cualquier `IllegalArgumentException`. Si el id almacenado como clave primaria (PK) en la tabla `matches` no es un UUID canónico parseable (por ejemplo, filas persistidas por una versión anterior de la app, o cualquier id no-UUID), el parseo falla y el `Match` conserva el UUID **aleatorio** del constructor.

Ese id aleatorio no coincide con la PK persistida. El mapeador de presentación `MatchUiMapper.toUiModel` lo emite en `MatchUiModel.id` (no vacío, pero incorrecto). La tarjeta abre el Detalle con ese id, `DetallePartidoViewModel.load(matchId)` invoca `GetMatchByIdUseCase` → `MatchRepositoryImpl.getMatchById` → `MatchDao.getMatchById("SELECT ... WHERE id = :matchId")`, no encuentra ninguna fila y devuelve `MATCH_NOT_FOUND`.

El defecto central es que la conversión Entidad → Dominio no preserva la identidad del partido: descarta cualquier id que no sea un UUID canónico y lo reemplaza por uno aleatorio, rompiendo la correspondencia entre el id mostrado en la lista y la PK que busca el Detalle.

Arreglar el bug significa garantizar que la identidad del partido se conserve de extremo a extremo (Entidad → Dominio → `MatchUiModel.id`), de modo que el id que abre el Detalle sea siempre igual a la PK persistida en Room, sin romper la generación determinista de `stableId` ni los tests unitarios existentes.

## Análisis del Bug

### Comportamiento Actual (Defecto)

Cuando el id persistido en Room no es un UUID canónico parseable, la reconstrucción del dominio pierde la identidad y el Detalle no encuentra el partido.

1.1 WHEN una fila de la tabla `matches` tiene un `id` (PK) que no es un UUID canónico parseable y se reconstruye vía `MatchEntity.toDomainModel()` THEN el sistema descarta el id persistido y deja en `Match.id` un `UUID.randomUUID()` distinto de la PK almacenada
1.2 WHEN un `Match` reconstruido con id que no corresponde a la PK se convierte con `MatchUiMapper.toUiModel()` THEN el sistema produce un `MatchUiModel.id` que no coincide con la PK persistida de ese partido
1.3 WHEN el usuario pulsa una tarjeta cuyo `MatchUiModel.id` no coincide con la PK persistida y `DetallePartidoViewModel.load(matchId)` consulta por ese id THEN el sistema no encuentra fila en `MatchDao.getMatchById` y emite `PredictionError.MATCH_NOT_FOUND`

### Comportamiento Esperado (Correcto)

La identidad del partido debe conservarse íntegra desde la caché hasta la UI, de modo que el id de la tarjeta coincida siempre con la PK que busca el Detalle.

2.1 WHEN una fila de la tabla `matches` tiene un `id` (PK) que no es un UUID canónico parseable y se reconstruye vía `MatchEntity.toDomainModel()` THEN el sistema SHALL conservar la identidad del partido de forma que ese `Match` siga siendo localizable por la misma PK almacenada
2.2 WHEN un `Match` reconstruido desde una fila cualquiera de la caché se convierte con `MatchUiMapper.toUiModel()` THEN el sistema SHALL producir un `MatchUiModel.id` no vacío e igual a la PK persistida de ese partido
2.3 WHEN el usuario pulsa una tarjeta de un partido presente en la caché y `DetallePartidoViewModel.load(matchId)` consulta por su id THEN el sistema SHALL encontrar la fila correspondiente en `MatchDao.getMatchById` y NO emitir `PredictionError.MATCH_NOT_FOUND`

### Comportamiento Inalterado (Prevención de Regresiones)

El arreglo no debe cambiar el comportamiento de los partidos cuyo id ya es un UUID canónico, ni la generación determinista de ids, ni la lógica de validación existente.

3.1 WHEN una fila de la tabla `matches` tiene un `id` (PK) que ES un UUID canónico parseable y se reconstruye vía `MatchEntity.toDomainModel()` THEN el sistema SHALL CONTINUAR reconstruyendo el `Match` con ese mismo id y produciendo un `MatchUiModel.id` igual a la PK, tal como ya ocurría
3.2 WHEN se refrescan partidos desde la red con un `externalId` no vacío THEN el sistema SHALL CONTINUAR generando el id determinista (`stableId`) con `UUID.nameUUIDFromBytes(externalId)` y persistiéndolo de forma estable entre refrescos
3.3 WHEN `DetallePartidoViewModel.load(matchId)` recibe un id nulo o vacío THEN el sistema SHALL CONTINUAR emitiendo `PredictionError.MATCH_NOT_FOUND` de inmediato sin consultar la caché
3.4 WHEN se ejecutan los tests unitarios JVM existentes (Prediction_ScorerTest, Season_Stats_CalculatorTest, PredictionRepositoryUpsertResolverTest, ProfileRepositoryChangePasswordTest, ScoreValidatorTest, ProfileValidatorTest) THEN el sistema SHALL CONTINUAR pasándolos sin modificaciones a su comportamiento
