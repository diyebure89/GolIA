# Requirements Document

## Introduction

Esta funcionalidad permite que, al tocar un partido en la lista (tanto en `InicioFragment` como en `PartidosFragment`), el usuario abra un flujo de cuatro pantallas (Activities) para ver el detalle del encuentro y registrar un pronóstico de marcador exacto en la app GolIA (Android nativo, Java, Clean Architecture + MVVM + Hilt + Room, sin backend remoto activo).

El flujo de cuatro pantallas es:

1. **DetallePartidoActivity** ("Ver encuentro"): muestra el detalle del partido (liga y jornada, equipos y logos, fecha/hora en zona local, estado y marcador si aplica) y una sección de estadísticas de temporada. Ofrece un botón "Hacer Pronóstico".
2. **PronosticoActivity** ("Registrar pronóstico"): el usuario ingresa el marcador exacto (goles del local y del visitante).
3. **ConfirmacionActivity** ("Confirmar registro de pronóstico"): confirma que el pronóstico quedó registrado.
4. **HistorialPronosticosActivity** ("Consultar pronósticos guardados"): lista los pronósticos del usuario con su estado.

Decisiones de producto confirmadas:

- **Pronóstico de marcador exacto**: el usuario ingresa goles del local y del visitante; el resultado 1X2 (`predictedOutcome`) se deriva del marcador.
- **Estadísticas de temporada derivadas de la caché local (opción A2)**: `Season_Stats` se calcula a partir de los partidos ya almacenados en `Local_Database` (Room) para cada equipo, SIN llamar a la API ni consumir la cuota de peticiones. Las métricas están limitadas a los partidos que la app ya descargó (no a una temporada completa), y cuando un equipo no tiene partidos suficientes en caché se muestra un estado neutro/placeholder sin error.
- **Se omite la sección "Cuotas" del mock**: las cuotas (`homeOdds`/`drawOdds`/`awayOdds`) están siempre en `0.0` en el flujo real de datos y no se muestran.
- **Navegación por Activities con Intents y extras**, siguiendo el patrón existente (la clase `Constants` ya define extras como `EXTRA_USER_ID`); se añadirá `EXTRA_MATCH_ID` (String, el id estable del partido).

Fuera de alcance de esta funcionalidad:

- Cuotas reales de casas de apuestas (sección "Cuotas" del mock).
- Estadísticas de temporada obtenidas vía API (head-to-head, estadísticas de equipo remotas). En esta entrega solo se usan estadísticas derivadas de la caché local.
- La finalización automática de resultados y el otorgamiento de puntos al terminar un partido: el pronóstico nace como pendiente y el `Historial_UI` solo refleja su estado; la resolución de aciertos/puntos al finalizar el partido no se implementa aquí.

## Glossary

- **Detalle_Partido_UI**: La pantalla `DetallePartidoActivity` que muestra el detalle del partido y la sección de estadísticas de temporada, y ofrece el botón "Hacer Pronóstico". Es una vista delgada que observa un ViewModel y no accede directamente a la base de datos.
- **Pronostico_UI**: La pantalla `PronosticoActivity` donde el usuario ingresa el marcador exacto (goles local y visitante) y confirma el registro del pronóstico.
- **Confirmacion_UI**: La pantalla `ConfirmacionActivity` que confirma que el pronóstico quedó registrado y ofrece acciones para ir al historial o volver.
- **Historial_UI**: La pantalla `HistorialPronosticosActivity` que lista los pronósticos guardados del usuario con su estado.
- **Match**: Modelo de dominio del partido, con `id` (UUID estable derivado de `externalId`), `externalId`, `competitionId`, `competitionName`, `matchday` (jornada), equipos (`homeTeamId`/`homeTeamName`/`homeTeamLogoUrl`, `awayTeamId`/`awayTeamName`/`awayTeamLogoUrl`), `scheduledDateTime` (epoch millis), `status` (`MatchStatus`), `homeScore`/`awayScore` (Integer nullable), `homeOdds`/`drawOdds`/`awayOdds` (siempre `0.0`, no se usan), `elapsedMinute` (solo LIVE), `venueName`, `venueCity`.
- **MatchStatus**: Estado del partido: `SCHEDULED`, `LIVE`, `FINISHED`, `POSTPONED`, `CANCELLED`.
- **Match_Repository**: La interfaz de dominio `MatchRepository` y su implementación de datos, con estrategia cache-first sobre Room. Hoy no expone la obtención de un partido individual; esta funcionalidad añade un método para obtener un `Match` por su `id` desde caché, ejecutado en el executor de I/O, sin acceso a red.
- **Prediction**: Modelo de dominio del pronóstico, con `id` (UUID), `userId` (UUID), `matchId` (UUID), `predictedOutcome` (`PredictionOutcome`), `predictedHomeScore` (Integer), `predictedAwayScore` (Integer), `pointsEarned` (int), `isCorrect` (Boolean; `null` = pendiente), `createdAt`, `finalizedAt`.
- **PredictionOutcome**: Resultado 1X2 del pronóstico: `HOME_WIN`, `DRAW`, `AWAY_WIN`. Se deriva del marcador exacto.
- **Prediction_System**: El conjunto de componentes de dominio/datos que valida, deriva y persiste los pronósticos del usuario en `Local_Database`. Incluye un nuevo `Prediction_Repository`.
- **Prediction_Repository**: Interfaz de dominio (a construir) que expone operaciones de pronóstico sobre `PredictionDao` (obtener el pronóstico del usuario para un partido, guardar/actualizar sin duplicar por usuario+partido, listar los pronósticos del usuario), ejecutando en el executor de I/O y devolviendo `Result`.
- **Season_Stats**: Estadísticas de temporada del partido derivadas exclusivamente de los partidos `FINISHED` presentes en `Local_Database` para cada equipo: victorias (comparativa local vs visitante), goles promedio por partido y forma reciente de los últimos 5 partidos. No consume la cuota de la API.
- **Local_Session**: La sesión local gestionada por `PreferencesManager`. La identidad del usuario actual se obtiene con `getUserId()` validando `isLoggedIn()`.
- **Local_Database**: La base de datos local Room (`GolIADatabase`) con las tablas `matches` (`MatchDao`) y `predictions` (`PredictionDao`).
- **PredictionDao**: DAO existente y provisto por Hilt, con `userHasPredictionForMatch(userId, matchId)`, `getUserPredictionForMatch(userId, matchId)`, `insertPrediction` (REPLACE), `updatePrediction`, `getUserPredictionsSorted(userId)` y contadores/totales.
- **RequestBudgetManager**: Componente que limita las peticiones a la API (100/día, intervalo mínimo de 60 s). El detalle y las estadísticas de esta funcionalidad no deben provocar peticiones a la API.
- **Auth_Error**: Contrato tipado de errores del dominio que la UI traduce a mensajes localizados.
- **EXTRA_MATCH_ID**: Nueva constante de extra de Intent (String) que transporta el `id` estable del partido entre Activities.
- **Prediction_Lock_Threshold**: Margen de bloqueo del pronostico. Un partido admite pronostico solo mientras falten mas de 10 minutos para su inicio; es decir, cuando (`scheduledDateTime` menos 10 minutos) es posterior al instante actual y el `status` es `SCHEDULED`. A partir de ese momento el pronostico queda cerrado.
- **Prediction_Scoring**: Regla de puntuacion de un `Prediction` de marcador exacto, calculada como la suma de los aciertos frente al marcador real del partido finalizado: acertar el ganador 1X2 (`predictedOutcome`) otorga 5 puntos; acertar los goles exactos del equipo local otorga 2 puntos; acertar los goles exactos del equipo visitante otorga 2 puntos; acertar la diferencia de goles (local menos visitante) otorga 1 punto; y acertar el marcador exacto completo otorga 9 puntos adicionales. El maximo posible es 19 puntos (5 + 2 + 2 + 1 + 9). El bono por ser el unico jugador con el marcador exacto queda fuera de alcance por requerir un backend que compare pronosticos entre usuarios.

## Requirements

### Requirement 1: Navegación al detalle desde la lista de partidos

**User Story:** Como usuario, quiero tocar un partido de la lista para abrir su detalle, para revisar el encuentro y decidir si hago un pronóstico.

#### Acceptance Criteria

1. THE `MatchesAdapter` SHALL exponer un listener de click por partido y THE `InicioFragment` y THE `PartidosFragment` SHALL cablearlo para reaccionar al toque de una tarjeta de partido.
2. THE `Match_Id` transportado al detalle SHALL ser exactamente la clave primaria `Match.id` (serializada como String), el mismo valor presente en `MatchUiModel.id`, y NO el `externalId`.
3. WHEN el usuario toca una tarjeta de partido en `InicioFragment` o en `PartidosFragment`, THE aplicación SHALL abrir `Detalle_Partido_UI` mediante un Intent que transporte el `Match_Id` en `EXTRA_MATCH_ID`.
4. THE `Constants` SHALL definir la constante `EXTRA_MATCH_ID` (String) usada para transportar el `Match_Id` entre Activities.
5. WHEN el usuario pulsa el control de "atrás" del sistema o el botón de retroceso del encabezado en `Detalle_Partido_UI`, THE aplicación SHALL regresar a la lista de partidos de origen conservando su estado de vista.
6. IF el Intent que abre `Detalle_Partido_UI` no contiene un `EXTRA_MATCH_ID` no vacío, THEN THE `Detalle_Partido_UI` SHALL mostrar un mensaje de error indicando que el partido no está disponible y SHALL permitir volver a la pantalla anterior sin cargar datos.

### Requirement 2: Carga del detalle del partido desde caché

**User Story:** Como usuario, quiero ver los datos del partido seleccionado, para conocer el encuentro antes de pronosticar.

#### Acceptance Criteria

1. THE `Match_Repository` SHALL exponer un método de dominio para obtener un `Match` por su `Match_Id` que lea desde `Local_Database` (por la clave primaria `matches.id`), ejecutando la lectura en el `@IoExecutor`, devolviendo `Result<Match>` mediante `Callback`, y SIN realizar ninguna petición a la API.
2. WHEN `Detalle_Partido_UI` se hace visible con un `EXTRA_MATCH_ID` válido, THE `Match_Repository` SHALL obtener el `Match` correspondiente por su `Match_Id`.
3. WHILE el `Match_Repository` obtiene el `Match`, THE `Detalle_Partido_UI` SHALL mostrar un indicador de carga.
4. WHEN el `Match_Repository` devuelve el `Match`, THE `Detalle_Partido_UI` SHALL mostrar el nombre de la liga (`competitionName`) y la jornada (`matchday`), el nombre y el logo de cada equipo, y la fecha y hora de inicio (`scheduledDateTime`) formateadas en la zona local del dispositivo.
5. WHEN el `Match` tiene `status == SCHEDULED`, THE `Detalle_Partido_UI` SHALL mostrar la fecha/hora del encuentro sin marcador; WHEN el `Match` tiene `status == LIVE`, THE `Detalle_Partido_UI` SHALL mostrar el marcador actual y el minuto de juego (`elapsedMinute`); WHEN el `Match` tiene `status == FINISHED`, THE `Detalle_Partido_UI` SHALL mostrar el marcador final; en otros estados (`POSTPONED`, `CANCELLED`) SHALL mostrar una etiqueta de estado localizada.
6. THE `Detalle_Partido_UI` SHALL NOT mostrar ninguna sección de cuotas ni valores de `homeOdds`/`drawOdds`/`awayOdds`.
7. IF no existe en `Local_Database` un `Match` con el `Match_Id` recibido, THEN THE `Match_Repository` SHALL devolver un `Result.Error` con `Prediction_Error.MATCH_NOT_FOUND` y THE `Detalle_Partido_UI` SHALL mostrar un mensaje indicando que el partido no está disponible y SHALL NOT mostrar datos parciales.
8. IF ocurre un error técnico al leer el `Match` de `Local_Database`, THEN THE `Detalle_Partido_UI` SHALL ocultar el indicador de carga, mostrar un mensaje de error y SHALL NOT mostrar datos parciales.
9. THE obtención del detalle SHALL NOT incrementar el contador del `RequestBudgetManager` ni provocar llamadas de red.

### Requirement 3: Estadísticas de temporada derivadas de la caché local

**User Story:** Como usuario, quiero ver estadísticas comparativas de los equipos, para valorar el encuentro antes de pronosticar.

#### Acceptance Criteria

1. WHEN `Detalle_Partido_UI` carga un `Match`, THE `Season_Stats` SHALL calcularse exclusivamente a partir de los partidos con `status == FINISHED` presentes en `Local_Database` para el equipo local y el equipo visitante, sin realizar ninguna petición a la API.
2. THE `Season_Stats` SHALL calcular, para cada equipo, el número de victorias contando como victoria todo partido `FINISHED` en el que el equipo terminó con más goles que su rival (a favor del equipo evaluado, sea local o visitante en ese partido).
3. THE `Season_Stats` SHALL calcular, para cada equipo, los goles promedio por partido como el total de goles a favor del equipo en sus partidos `FINISHED` dividido entre el número de esos partidos, redondeado a un decimal.
4. THE `Season_Stats` SHALL calcular, para cada equipo, la forma reciente considerando sus últimos 5 partidos `FINISHED` ordenados del más reciente al más antiguo por `scheduledDateTime`, clasificando cada uno como victoria (V), empate (E) o derrota (D) desde la perspectiva del equipo evaluado.
5. THE `Detalle_Partido_UI` SHALL presentar las métricas de victorias, goles promedio y forma reciente como una comparativa entre el equipo local y el visitante (por ejemplo, barras comparativas), etiquetando cuál valor corresponde a cada equipo.
6. IF un equipo no tiene partidos `FINISHED` en `Local_Database`, THEN THE `Detalle_Partido_UI` SHALL mostrar un estado neutro o placeholder para sus métricas (por ejemplo, 0 victorias, 0.0 goles promedio y forma reciente vacía) y SHALL NOT mostrar un mensaje de error.
7. IF un equipo tiene menos de 5 partidos `FINISHED` en `Local_Database`, THEN THE `Season_Stats` SHALL calcular la forma reciente con los partidos disponibles (menos de 5) sin error.
8. THE cálculo de `Season_Stats` SHALL ejecutarse fuera del hilo principal y SHALL NOT incrementar el contador del `RequestBudgetManager`.

### Requirement 4: Botón "Hacer Pronóstico" en el detalle

**User Story:** Como usuario, quiero iniciar el registro de un pronóstico desde el detalle, para predecir el marcador del partido.

#### Acceptance Criteria

1. THE `Detalle_Partido_UI` SHALL mostrar un botón "Hacer Pronóstico".
2. THE `Detalle_Partido_UI` SHALL habilitar el botón "Hacer Pronóstico" únicamente cuando el `Match` cumple el `Prediction_Lock_Threshold` (`status == SCHEDULED` y `scheduledDateTime - 10 min` es posterior al instante actual); en cualquier otro caso SHALL deshabilitarlo e indicar que el pronóstico está cerrado para ese partido.
3. WHEN `Detalle_Partido_UI` vuelve a primer plano (`onResume`), THE `Detalle_Partido_UI` SHALL reevaluar el `Prediction_Lock_Threshold` y actualizar el estado del botón, de modo que un partido que cruzó el umbral mientras la pantalla estaba abierta quede deshabilitado.
4. WHEN existe un `Prediction` del usuario actual para ese `Match` y el partido aún cumple el `Prediction_Lock_Threshold`, THE `Detalle_Partido_UI` SHALL indicar que ya hay un pronóstico registrado y el botón SHALL permitir editarlo.
5. WHEN existe un `Prediction` del usuario actual para ese `Match` y el partido ya NO cumple el `Prediction_Lock_Threshold`, THE `Detalle_Partido_UI` SHALL permitir ver el pronóstico en modo de solo lectura y SHALL NOT permitir editarlo.
6. WHEN el usuario pulsa el botón "Hacer Pronóstico", THE aplicación SHALL abrir `Pronostico_UI` mediante un Intent que transporte el `Match_Id` en `EXTRA_MATCH_ID`.
7. IF no hay una `Local_Session` válida (`isLoggedIn()` es falso) al pulsar "Hacer Pronóstico", THEN THE aplicación SHALL redirigir al inicio de sesión y SHALL NOT abrir `Pronostico_UI`.

### Requirement 5: Registro del pronóstico de marcador exacto

**User Story:** Como usuario, quiero ingresar el marcador exacto que predigo, para registrar mi pronóstico del partido.

#### Acceptance Criteria

1. WHEN `Pronostico_UI` se hace visible, THE `Pronostico_UI` SHALL presentar dos campos numéricos separados para los goles del equipo local y los goles del equipo visitante, e identificar claramente a qué equipo corresponde cada campo.
2. WHEN ya existe un `Prediction` del usuario actual para ese `Match`, THE `Pronostico_UI` SHALL precargar `predictedHomeScore` y `predictedAwayScore` en los campos correspondientes.
3. IF alguno de los dos campos de marcador está vacío al enviar, THEN THE `Pronostico_UI` SHALL mostrar un mensaje de error indicando que ambos marcadores son obligatorios y SHALL rechazar el guardado.
4. IF alguno de los marcadores ingresados no es un entero entre 0 y 99 (ambos inclusive), THEN THE `Pronostico_UI` SHALL mostrar un mensaje de error de valor inválido y SHALL rechazar el guardado.
5. WHEN los marcadores son válidos, THE `Prediction_System` SHALL derivar el `predictedOutcome` del marcador: `HOME_WIN` si los goles del local son mayores que los del visitante, `DRAW` si son iguales, y `AWAY_WIN` si son menores.
6. IF al enviar el pronóstico no hay una `Local_Session` válida, THEN THE `Prediction_System` SHALL rechazar el guardado, SHALL devolver `Prediction_Error.SESSION_UNAVAILABLE` y THE `Pronostico_UI` SHALL mostrar un mensaje localizado o redirigir al inicio de sesión.
7. IF al enviar el pronóstico el `Match` ya no cumple el `Prediction_Lock_Threshold` (no es `SCHEDULED`, o faltan 10 minutos o menos para el inicio, o el partido ya comenzó), THEN THE `Prediction_System` SHALL rechazar el guardado, SHALL devolver `Prediction_Error.PREDICTION_CLOSED` y THE `Pronostico_UI` SHALL indicar que el pronóstico está cerrado para ese partido. Esta validación SHALL ser la autoritativa, con independencia del estado visual del botón.
8. WHEN el usuario confirma un pronóstico válido, THE `Prediction_Repository` SHALL guardar o actualizar el `Prediction` del usuario para ese `Match` en `Local_Database`, ejecutando la escritura en el `@IoExecutor`, con `user_id == User_Id`, `match_id == Match_Id`, `predictedHomeScore`, `predictedAwayScore` y el `predictedOutcome` derivado, dejando `isCorrect` en `null` (pendiente) y `pointsEarned` en 0.
9. THE `Prediction_Repository` SHALL garantizar que exista como máximo un `Prediction` por combinación (`user_id`, `match_id`): al guardar, SHALL consultar el pronóstico existente del usuario para ese partido (`getUserPredictionForMatch`) y, si existe, SHALL reutilizar su `id` para reemplazarlo (upsert), evitando crear un duplicado.
10. WHILE el `Prediction_System` procesa el guardado, THE `Pronostico_UI` SHALL mostrar un indicador de carga y SHALL deshabilitar el control de envío para evitar envíos duplicados.
11. WHEN el guardado se completa correctamente, THE aplicación SHALL navegar a `Confirmacion_UI` transportando el `Match_Id` y el marcador pronosticado.
12. IF ocurre un error técnico al guardar el `Prediction`, THEN THE `Prediction_System` SHALL devolver `Prediction_Error.PERSISTENCE_ERROR` y THE `Pronostico_UI` SHALL ocultar el indicador de carga, mostrar un mensaje de error y conservar los valores ingresados.

### Requirement 6: Unicidad y migración de esquema de pronósticos

**User Story:** Como sistema, quiero garantizar que no existan pronósticos duplicados por usuario y partido, para mantener la integridad de los datos y del historial.

#### Acceptance Criteria

1. THE `PredictionEntity` SHALL definir un índice ÚNICO sobre la combinación de columnas (`user_id`, `match_id`), de modo que la base de datos impida dos pronósticos del mismo usuario para el mismo partido.
2. THE `Local_Database` (`GolIADatabase`) SHALL incrementar la versión del esquema de Room y registrar una migración que cree el índice único (`user_id`, `match_id`) en la tabla `predictions`, conservando los registros existentes.
3. IF durante la migración existieran filas duplicadas para una misma pareja (`user_id`, `match_id`), THEN la migración SHALL conservar una sola de ellas (por ejemplo la más reciente por `created_at`) antes de crear el índice único, para no fallar la migración.
4. IF una escritura viola el índice único (`user_id`, `match_id`), THEN THE `Prediction_Repository` SHALL resolverla como una actualización del pronóstico existente (upsert reutilizando su `id`) y SHALL NOT dejar el guardado en un estado de error para el usuario.
5. THE proyecto SHALL mantener `exportSchema = true` y exportar/regenerar el JSON de esquema de la nueva versión.

### Requirement 7: Confirmación del pronóstico registrado

**User Story:** Como usuario, quiero ver una confirmación de mi pronóstico, para saber que quedó registrado correctamente.

#### Acceptance Criteria

1. WHEN `Confirmacion_UI` se hace visible tras un guardado exitoso, THE `Confirmacion_UI` SHALL mostrar los nombres de los equipos, el marcador pronosticado (goles local–visitante), la fecha del partido y los puntos posibles del pronóstico (el máximo teórico de `Prediction_Scoring`, 19 puntos).
2. THE `Confirmacion_UI` SHALL ofrecer una acción "Ver historial" que abra `Historial_UI` y una acción "Ir al inicio" que regrese a `MainActivity`.
3. WHEN el usuario pulsa "Ir al inicio", THE aplicación SHALL navegar a `MainActivity` limpiando la pila del flujo de pronóstico (`FLAG_ACTIVITY_CLEAR_TOP` o equivalente), de modo que "atrás" no reingrese a `Pronostico_UI`.
4. WHEN el usuario pulsa "Ver historial", THE aplicación SHALL abrir `Historial_UI` sin dejar `Pronostico_UI` en la pila de retroceso por encima de `Confirmacion_UI`.
5. WHEN el usuario pulsa "atrás" desde `Confirmacion_UI`, THE aplicación SHALL NOT volver a guardar el pronóstico ni reabrir `Pronostico_UI` con un envío pendiente.
6. THE `Confirmacion_UI` SHALL NOT ejecutar ninguna escritura en `Local_Database` (el guardado ya ocurrió en `Pronostico_UI`).

### Requirement 8: Historial de pronósticos guardados

**User Story:** Como usuario, quiero consultar mis pronósticos guardados, para revisar qué he predicho y su estado.

#### Acceptance Criteria

1. WHEN `Historial_UI` se hace visible, THE `Prediction_Repository` SHALL obtener los pronósticos del usuario actual (`getUserPredictionsSorted` con `User_Id`) desde `Local_Database`, ejecutando la lectura en el `@IoExecutor`.
2. WHEN se muestra cada `Prediction`, THE `Historial_UI` SHALL resolver el `Match` asociado por su `match_id` (vía el `Match_Repository`) para mostrar los nombres de los equipos, además del marcador pronosticado (`predictedHomeScore`–`predictedAwayScore`) y el estado del pronóstico.
3. IF el `Match` asociado a un `Prediction` ya no está en `Local_Database` (por ejemplo purgado por limpieza de partidos antiguos), THEN THE `Historial_UI` SHALL mostrar ese pronóstico con un texto placeholder (por ejemplo "Partido no disponible") y su marcador pronosticado, sin romper la lista ni mostrar un error global.
4. THE `Historial_UI` SHALL representar el estado del pronóstico como pendiente cuando `isCorrect` es `null`, acertado cuando `isCorrect` es verdadero y fallido cuando `isCorrect` es falso, y SHALL mostrar los puntos (`pointsEarned`) cuando el pronóstico está resuelto.
5. WHILE el `Match` de un `Prediction` no esté finalizado, THE `Historial_UI` SHALL mostrar el pronóstico como pendiente y SHALL NOT mostrar puntos obtenidos.
6. WHEN el usuario no tiene pronósticos guardados, THE `Historial_UI` SHALL mostrar un estado vacío informativo sin error.
7. IF no hay una `Local_Session` válida al abrir `Historial_UI`, THEN THE `Historial_UI` SHALL redirigir al inicio de sesión y SHALL NOT mostrar pronósticos.
8. IF ocurre un error técnico al leer los pronósticos, THEN THE `Historial_UI` SHALL mostrar un mensaje de error y SHALL NOT mostrar datos parciales.

### Requirement 9: Puntuación y resolución del pronóstico

**User Story:** Como usuario, quiero ganar puntos según qué tan acertado fue mi pronóstico de marcador exacto y que se resuelvan al terminar el partido, para competir en el ranking.

#### Acceptance Criteria

1. WHEN se crea o actualiza un `Prediction`, THE `Prediction_System` SHALL inicializarlo como pendiente (`isCorrect == null`) y con `pointsEarned == 0`.
2. THE `Prediction_System` SHALL NOT calcular puntos usando `homeOdds`/`drawOdds`/`awayOdds`, dado que esos valores son siempre `0.0` en el flujo real de datos.
3. THE `Prediction_Scoring` SHALL calcular los puntos de un `Prediction` frente al marcador real de un `Match` finalizado sumando los aciertos: 5 puntos si el `predictedOutcome` coincide con el ganador real (1X2); 2 puntos si los goles pronosticados del equipo local coinciden exactamente con los reales; 2 puntos si los goles pronosticados del equipo visitante coinciden exactamente con los reales; 1 punto si la diferencia de goles pronosticada (local menos visitante) coincide con la real; y 9 puntos adicionales si el marcador pronosticado coincide exactamente con el real en ambos equipos.
4. THE puntuación máxima de un único `Prediction` SHALL ser 19 puntos (5 + 2 + 2 + 1 + 9), calculada como la suma directa de los componentes acertados.
5. THE `Prediction_System` SHALL NOT otorgar el bono por ser el único jugador con el marcador exacto (fuera de alcance).
6. WHEN el `Prediction_Resolver` procesa un `Match` cuyo `status` pasó a `FINISHED` con `homeScore`/`awayScore` no nulos, THE `Prediction_Resolver` SHALL, para cada `Prediction` pendiente (`isCorrect == null`) de ese partido, calcular los puntos con `Prediction_Scoring`, establecer `isCorrect == true` si obtuvo al menos 1 punto o `isCorrect == false` si obtuvo 0 puntos, persistir `pointsEarned` y `finalizedAt`, ejecutándose en el `@IoExecutor`.
7. THE `Prediction_Resolver` SHALL ser idempotente: un `Prediction` ya resuelto (`isCorrect != null`) SHALL NOT recalcularse ni duplicar puntos si el partido se procesa de nuevo.
8. THE `Prediction_Resolver` SHALL invocarse cuando la app detecte partidos finalizados durante el refresco de datos de partidos (integrándose con el flujo de actualización existente), sin realizar peticiones adicionales a la API por sí mismo.
9. THE `Confirmacion_UI` y THE `Historial_UI` SHALL presentar los puntos posibles o los puntos obtenidos de un `Prediction` de forma coherente con `Prediction_Scoring` (máximo 19).

### Requirement 10: Estados de interfaz, errores tipados, concurrencia y navegación

**User Story:** Como usuario, quiero recibir retroalimentación clara del progreso y los errores, y una navegación coherente entre las pantallas del flujo.

#### Acceptance Criteria

1. THE `Prediction_System` y THE `Match_Repository` SHALL ejecutar todas sus operaciones de lectura/escritura en el `@IoExecutor`, devolver `Result<T>` mediante `Callback` y publicar los resultados en `LiveData` desde el ViewModel (patrón `BaseViewModel` existente), sin bloquear el hilo de UI.
2. WHILE cualquier pantalla del flujo procesa una operación asíncrona (carga del detalle, cálculo de estadísticas, guardado del pronóstico o carga del historial), THE pantalla correspondiente SHALL mostrar un indicador de carga.
3. WHEN una operación asíncrona termina con éxito, THE pantalla correspondiente SHALL ocultar el indicador de carga y mostrar el resultado.
4. IF una operación asíncrona termina con error, THEN THE pantalla correspondiente SHALL ocultar el indicador de carga y mostrar un mensaje de error, sin exponer datos parciales.
5. WHEN el dominio devuelve un `Prediction_Error` tipado, THE capa de presentación SHALL traducirlo a un mensaje localizado, desacoplando la lógica de dominio de los textos de la UI.
6. THE navegación entre `Detalle_Partido_UI`, `Pronostico_UI`, `Confirmacion_UI` e `Historial_UI` SHALL realizarse mediante Intents, transportando el `Match_Id` con `EXTRA_MATCH_ID` cuando la pantalla destino lo requiera.
7. THE control de "atrás" del sistema SHALL comportarse de forma coherente en cada Activity del flujo, regresando a la pantalla anterior sin repetir escrituras en `Local_Database`.

### Requirement 11: Verificación mediante pruebas automatizadas

**User Story:** Como equipo de desarrollo, quiero pruebas automatizadas del cálculo de estadísticas, la validación del marcador, la puntuación y el guardado del pronóstico, para garantizar la corrección y prevenir regresiones.

#### Acceptance Criteria

1. THE proyecto SHALL incluir tests unitarios puros de JVM para el cálculo de `Season_Stats` (victorias totales, goles promedio a un decimal y forma reciente de los últimos 5 partidos con desempate estable), incluyendo los casos de cero partidos y de menos de 5 partidos `FINISHED`.
2. THE proyecto SHALL incluir tests unitarios puros de JVM para la validación del marcador (enteros 0..99, campos requeridos) y para la derivación del `predictedOutcome` a partir del marcador (`HOME_WIN`/`DRAW`/`AWAY_WIN`).
3. THE proyecto SHALL incluir tests unitarios puros de JVM para `Prediction_Scoring` que cubran: acierto solo del ganador (5), acierto de goles de un equipo (2) y de ambos (4), acierto de la diferencia (1), marcador exacto completo (19) y fallo total (0 puntos, `isCorrect == false`).
4. THE proyecto SHALL incluir un test que verifique la idempotencia del `Prediction_Resolver` (un pronóstico ya resuelto no se recalcula).
5. THE proyecto SHALL incluir un test del guardado del pronóstico que verifique la unicidad por (`user_id`, `match_id`): un segundo pronóstico del mismo usuario para el mismo partido reemplaza al anterior (upsert) y no crea un duplicado.
6. THE proyecto SHALL incluir un test de migración de esquema que verifique la creación del índice único (`user_id`, `match_id`) conservando los pronósticos existentes.