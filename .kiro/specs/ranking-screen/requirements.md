# Requirements Document

## Introduction

Esta funcionalidad construye desde cero la pantalla "Ranking" (`RankingFragment`) de la app GolIA (Android nativo, Java, Clean Architecture + MVVM + Hilt + Room, sin backend remoto activo). Hoy `fragment_ranking.xml` es un marcador de posición vacío; el objetivo es materializar la pantalla del mock: un encabezado "Ranking Global", un podio con los tres primeros puestos, un selector de periodo (Semanal / Mensual / Total) y una lista de posiciones con puntos, porcentaje de acierto y racha por usuario.

Los datos del ranking se derivan de los resultados de los pronósticos y se comparan entre los usuarios para generar las posiciones. Como la app es local (Room/SQLite) y no tiene backend ni sincronización entre dispositivos, el ranking se construye sobre una fuente local de participantes: el `Current_User` real (con sus pronósticos guardados) más un conjunto de `Seed_Profiles` (perfiles de ejemplo) que pueblan el podio y la lista de forma consistente con el mock. La pantalla indica que el ranking es local/demo mientras no exista un backend.

Decisiones de producto y de arquitectura confirmadas para esta entrega (Opción A + A1):

- **Fuente del ranking (Opción A)**: participantes = `Current_User` real + `Seed_Profiles` de ejemplo. Los puntos del usuario real se recalculan a partir de sus pronósticos; los de los perfiles seed a partir de sus resultados de muestra deterministas.
- **Motor de puntuación único**: el `Ranking_System` recalcula los puntos en memoria con `Prediction_Scorer` (el mismo componente de dominio ya existente: 5 ganador + 2 goles local + 2 goles visitante + 1 diferencia + 9 marcador exacto; máximo 19 por pronóstico). El ranking NO depende del valor `points_earned` persistido (que proviene de un motor de puntuación antiguo basado en cuotas y puede ser 0), evitando dos fuentes de verdad distintas.
- **Campo temporal del periodo**: los puntos consolidados se filtran por `finalizedAt` del pronóstico (cuándo se resolvió); los `Live_Points` se asocian por `scheduledDateTime` del partido en vivo. Un pronóstico cuyo `Match` ya no está en la caché local se excluye del cálculo de ese periodo.
- **Puntos en vivo (A1)**: mientras un partido asociado a un pronóstico esté `LIVE` con marcador disponible, la pantalla muestra una puntuación provisional ("en vivo") calculada con `Prediction_Scorer` sobre el marcador en vivo; al finalizar el partido, la puntuación se consolida. Los conjuntos "consolidado" (partidos `FINISHED`) y "en vivo" (partidos `LIVE`) son disjuntos, por lo que no hay doble conteo.
- El selector de periodo (Semanal / Mensual / Total) filtra los pronósticos considerados por su fecha.

Fuera de alcance de esta funcionalidad:

- Un ranking global real entre usuarios de distintos dispositivos (requiere backend y sincronización).
- La creación o edición de pronósticos (cubierta por la funcionalidad `match-detail-prediction`).
- La obtención de datos de partidos desde la API (el ranking consume solo la caché local y no gasta cuota).

## Glossary

- **Ranking_UI**: La pantalla `RankingFragment` y su `RankingViewModel`, responsables de mostrar el podio, el selector de periodo y la lista de posiciones. Es una vista delgada que observa `LiveData` y no accede directamente a la base de datos.
- **Ranking_System**: El conjunto de componentes de dominio/datos que calcula las posiciones del ranking a partir de los pronósticos y de los participantes locales, recalculando los puntos con `Prediction_Scorer` en memoria.
- **Ranking_Entry**: Una entrada del ranking para un participante, con: nombre a mostrar, avatar (opcional), puntos del periodo, `Win_Percentage`, `Streak` y posición (1, 2, 3, ...).
- **Ranking_Period**: El periodo seleccionado que filtra los pronósticos considerados: `SEMANAL` (últimos 7 días), `MENSUAL` (últimos 30 días) o `TOTAL` (todo el historial). La ventana se mide contra el instante actual.
- **Current_User**: El usuario autenticado cuya identidad se obtiene de `Local_Session` (`PreferencesManager.getUserId()` validando `isLoggedIn()`). Es el único participante con pronósticos reales en el dispositivo.
- **Display_Name**: Regla de nombre a mostrar reutilizada de Perfil/Inicio (`DisplayName.resolve`): prioriza el nombre de usuario y, si no existe, el nombre completo.
- **Seed_Profiles**: Conjunto determinista de perfiles de ejemplo. Cada perfil define nombre, avatar y una lista de `Seed_Result` (resultados de pronóstico de muestra con marcador pronosticado, marcador real y timestamp), de modo que el filtrado por `Ranking_Period` y el cálculo de puntos/porcentaje/racha se apliquen igual que para el `Current_User`. Residen como constantes de dominio en memoria (no se insertan en Room).
- **Seed_Result**: Un resultado de muestra de un `Seed_Profile`: marcador pronosticado (home/away), marcador real (home/away) y `timestamp` (equivalente a `finalizedAt`).
- **Prediction**: Modelo de dominio del pronóstico, con `userId`, `matchId`, `predictedHomeScore`, `predictedAwayScore`, `predictedOutcome`, `pointsEarned`, `isCorrect` (`null` = pendiente), `createdAt`, `finalizedAt`.
- **Prediction_Scorer**: Componente de dominio ya existente (`domain.model.Prediction_Scorer`) que calcula los puntos de un pronóstico de marcador exacto frente a un marcador real: `score(predictedHome, predictedAway, actualHome, actualAway)` en `0..19` e `isCorrect(score) = score >= 1`. Es la única regla de puntuación usada por el ranking.
- **Live_Points**: Puntuación provisional de un pronóstico cuyo partido está `LIVE` con marcador no nulo, calculada con `Prediction_Scorer` sobre el marcador en vivo. No se persiste. Si el partido `LIVE` no tiene marcador (null), aporta 0 puntos.
- **Match**: Modelo de dominio del partido, con `status` (`MatchStatus`: `SCHEDULED`, `LIVE`, `FINISHED`, `POSTPONED`, `CANCELLED`), `homeScore`/`awayScore` (Integer nullable) y `scheduledDateTime`.
- **Local_Session**: La sesión local gestionada por `PreferencesManager` (`getUserId()`, `isLoggedIn()`).
- **Local_Database**: La base de datos local Room (`GolIADatabase`) con las tablas `users` (`UserDao`), `predictions` (`PredictionDao`) y `matches` (`MatchDao`).
- **PredictionDao**: DAO existente con `getPredictionsByUser(userId)`, `getUserPredictionsSorted(userId)`, contadores y agregados. El ranking lee los pronósticos individuales (para recalcular con `Prediction_Scorer`), no el agregado `getUserTotalPoints`.
- **Win_Percentage**: Porcentaje de acierto de un participante en el periodo = round((pronósticos acertados / pronósticos resueltos) * 100), como entero (`Math.round`); un pronóstico está "resuelto" cuando su partido está `FINISHED`; 0% si no hay pronósticos resueltos en el periodo.
- **Streak**: Racha GLOBAL (independiente del periodo) de aciertos consecutivos más reciente: número de pronósticos resueltos consecutivos con puntuación `>= 1` (según `Prediction_Scorer`) contando desde el más reciente (por `finalizedAt`) hacia atrás; se interrumpe con el primer pronóstico resuelto con 0 puntos.
- **RequestBudgetManager**: Componente que limita las peticiones a la API. El ranking no debe provocar peticiones a la API.

## Requirements

### Requirement 1: Estructura y carga de la pantalla de Ranking

**User Story:** Como usuario, quiero ver la pantalla de ranking con el podio y la lista de posiciones, para conocer mi posición y la de los demás participantes.

#### Acceptance Criteria

1. THE `Ranking_UI` SHALL presentar un encabezado "Ranking Global", un podio con los tres primeros puestos, un selector de periodo (Semanal / Mensual / Total) y una lista de posiciones.
2. WHEN `Ranking_UI` se hace visible, THE `Ranking_System` SHALL calcular las `Ranking_Entry` del periodo seleccionado (por defecto Semanal), recalculando los puntos con `Prediction_Scorer` en memoria a partir de los pronósticos de los participantes y el marcador real/en vivo de sus partidos, ejecutando el cálculo fuera del hilo principal.
3. THE `Ranking_System` SHALL NOT usar el valor persistido `points_earned` como fuente de los puntos mostrados; SHALL recalcularlos con `Prediction_Scorer` para garantizar una única regla de puntuación coherente con el resto de la app.
4. WHILE el `Ranking_System` calcula el ranking, THE `Ranking_UI` SHALL mostrar un indicador de carga.
5. WHEN el `Ranking_System` devuelve las `Ranking_Entry`, THE `Ranking_UI` SHALL mostrar en el podio los puestos 1, 2 y 3 (con la posición 1 destacada al centro) y, en la lista inferior, las posiciones ordenadas por puntos descendentes.
6. THE cálculo del ranking SHALL NOT provocar peticiones a la API ni incrementar el contador del `RequestBudgetManager` (usa solo `Local_Database` y `Seed_Profiles` en memoria).
7. IF no hay una `Local_Session` válida al abrir `Ranking_UI`, THEN THE `Ranking_UI` SHALL redirigir al inicio de sesión y SHALL NOT mostrar el ranking.
8. IF ocurre un error técnico al calcular el ranking, THEN THE `Ranking_UI` SHALL ocultar el indicador de carga y mostrar un mensaje de error, sin mostrar datos parciales.

### Requirement 2: Participantes del ranking (usuario real + perfiles de ejemplo)

**User Story:** Como usuario, quiero que el ranking incluya mi cuenta real y perfiles de ejemplo, para ver una tabla de posiciones completa mientras la app es local.

#### Acceptance Criteria

1. THE `Ranking_System` SHALL incluir como participantes al `Current_User` (con sus pronósticos reales de `Local_Database`) y al conjunto de `Seed_Profiles` de ejemplo.
2. THE `Seed_Profiles` SHALL definirse como constantes de dominio deterministas en memoria (no se insertan en Room), cada uno con nombre, avatar y una lista de `Seed_Result` con marcador pronosticado, marcador real y `timestamp`, de modo que produzcan los mismos puntos, `Win_Percentage` y `Streak` en cada ejecución.
3. THE `Ranking_System` SHALL calcular los puntos, el `Win_Percentage` y la `Streak` de cada `Seed_Profile` aplicando `Prediction_Scorer` y las mismas definiciones de métricas que para el `Current_User`, y filtrando sus `Seed_Result` por `Ranking_Period` mediante su `timestamp`.
4. THE `Ranking_UI` SHALL mostrar, como subtítulo bajo "Ranking Global", una nota localizada fija que indique que el ranking es local/demo mientras no exista un backend.
5. THE `Ranking_System` SHALL marcar la `Ranking_Entry` del `Current_User` (mediante una bandera) y THE `Ranking_UI` SHALL resaltarla visualmente para que el usuario reconozca su posición.
6. THE `Ranking_System` SHALL garantizar, mediante los `Seed_Profiles`, al menos tres participantes para poblar el podio; el manejo de menos de tres participantes (Requisito 3.3) permanece como salvaguarda.
7. THE `Ranking_UI` SHALL resolver el nombre a mostrar del `Current_User` con `Display_Name` (`DisplayName.resolve`), consistente con Perfil e Inicio.
### Requirement 3: Podio de los tres primeros puestos

**User Story:** Como usuario, quiero ver destacados a los tres mejores del ranking, para identificar rápidamente a los líderes.

#### Acceptance Criteria

1. WHEN hay al menos tres participantes, THE `Ranking_UI` SHALL mostrar en el podio al 1.º (centro, destacado), 2.º (izquierda) y 3.º (derecha), cada uno con su nombre, avatar y puntos del periodo.
2. THE `Ranking_UI` SHALL mostrar el número de posición (1, 2, 3) en cada bloque del podio.
3. IF hay menos de tres participantes con puntos en el periodo, THEN THE `Ranking_UI` SHALL mostrar solo los puestos disponibles sin romper el diseño ni mostrar posiciones vacías con datos inventados.
4. WHEN dos o más participantes empatan en puntos del periodo, THE `Ranking_System` SHALL aplicar el siguiente desempate determinista y vinculante, en orden: (a) mayor `Win_Percentage`; (b) si persiste, orden alfabético ascendente del nombre a mostrar comparado sin distinción de mayúsculas/minúsculas (case-insensitive); (c) si aún persiste, orden ascendente del identificador del participante. Este criterio SHALL producir posiciones estables entre aperturas.

### Requirement 4: Selector de periodo (Semanal / Mensual / Total)

**User Story:** Como usuario, quiero filtrar el ranking por periodo, para ver el desempeño reciente o histórico.

#### Acceptance Criteria

1. THE `Ranking_UI` SHALL presentar tres opciones de `Ranking_Period`: Semanal, Mensual y Total, con Semanal seleccionada por defecto.
2. WHEN el usuario selecciona un `Ranking_Period`, THE `Ranking_System` SHALL recalcular el ranking considerando, para los puntos consolidados, únicamente los pronósticos resueltos cuyo `finalizedAt` cae dentro de la ventana del periodo medida contra el instante actual: últimos 7 días (Semanal), últimos 30 días (Mensual) o todo el historial (Total).
3. WHEN se recalcula por cambio de periodo, THE `Ranking_UI` SHALL actualizar el podio y la lista de forma coherente, mostrando un indicador de carga si el cálculo no es inmediato.
4. THE `Ranking_System` SHALL aplicar el mismo criterio de periodo por `finalizedAt` tanto al `Current_User` (usando `finalizedAt` del `Prediction`) como a los `Seed_Profiles` (usando el `timestamp` de cada `Seed_Result`).
5. IF un `Prediction` del `Current_User` está resuelto pero su `Match` ya no existe en la caché local, THEN THE `Ranking_System` SHALL excluir ese pronóstico del cálculo del periodo (no puede recalcularse su puntuación con `Prediction_Scorer` sin el marcador real).
6. WHEN un periodo no tiene pronósticos para un participante, THE `Ranking_System` SHALL considerar 0 puntos, 0% de acierto para ese participante en ese periodo (la `Streak` es global, ver Requisito 5.3).

### Requirement 5: Lista de posiciones con métricas por participante

**User Story:** Como usuario, quiero ver la lista de posiciones con puntos, porcentaje de acierto y racha, para comparar el desempeño de cada participante.

#### Acceptance Criteria

1. THE `Ranking_UI` SHALL mostrar, por cada `Ranking_Entry` de la lista, el nombre del participante, su avatar, su `Win_Percentage`, su `Streak` y sus puntos del periodo, además de un indicador de posición (medalla para el top y número para el resto).
2. THE `Win_Percentage` SHALL calcularse como `Math.round((pronósticos acertados / pronósticos resueltos) * 100)` dentro del periodo, considerando "resuelto" un pronóstico cuyo partido está `FINISHED` y "acertado" cuando `Prediction_Scorer.score >= 1`, mostrándose como entero (por ejemplo "84% acierto"); IF no hay pronósticos resueltos en el periodo, THEN THE valor SHALL ser 0%.
3. THE `Streak` SHALL ser GLOBAL (independiente del `Ranking_Period` seleccionado) y calcularse como el número de pronósticos resueltos consecutivos con `Prediction_Scorer.score >= 1` contando desde el más reciente por `finalizedAt` hacia atrás; se interrumpe con el primer pronóstico resuelto con 0 puntos.
4. THE `Ranking_UI` SHALL formatear los puntos con separador de miles según la configuración regional (por ejemplo "3,420 pts").
5. THE lista SHALL ordenarse por puntos del periodo en orden descendente, aplicando el desempate determinista y vinculante del Requisito 3.4.

### Requirement 6: Puntos en vivo (A1)

**User Story:** Como usuario, quiero que mis puntos reflejen los partidos en vivo, para ver mi puntuación provisional mientras se juega.

#### Acceptance Criteria

1. THE `Ranking_System` SHALL derivar el total de puntos del periodo de dos conjuntos disjuntos por estado del partido: los puntos consolidados (pronósticos cuyo `Match` está `FINISHED`) y los `Live_Points` (pronósticos cuyo `Match` está `LIVE`), evitando cualquier doble conteo.
2. WHEN un `Prediction` está asociado a un `Match` con `status == LIVE` y `homeScore`/`awayScore` no nulos, THE `Ranking_System` SHALL calcular sus `Live_Points` con `Prediction_Scorer` usando el marcador en vivo actual del partido.
3. IF un `Match` está `LIVE` pero su `homeScore` o `awayScore` es `null`, THEN THE `Ranking_System` SHALL asignar 0 `Live_Points` a ese pronóstico sin lanzar error.
4. THE `Ranking_System` SHALL sumar los `Live_Points` provisionales a los puntos consolidados del participante para obtener el total mostrado en el periodo.
5. THE `Ranking_UI` SHALL mostrar un distintivo "en vivo" en la entrada de un participante cuando este tenga al menos un pronóstico con partido `LIVE` que aporte `Live_Points`.
6. THE `Ranking_System` SHALL NOT persistir los `Live_Points`; la consolidación ocurre cuando el partido pasa a `FINISHED` mediante la resolución de pronósticos existente.
7. WHEN `Ranking_UI` vuelve a primer plano (`onResume`) o cuando se actualizan los partidos en vivo mediante el mecanismo de refresco existente, THE `Ranking_UI` SHALL recalcular y reflejar la puntuación provisional actualizada, SHALL NOT realizar peticiones adicionales a la API por sí misma.
8. WHILE no haya partidos `LIVE` asociados a los pronósticos de un participante, THE `Ranking_System` SHALL mostrar únicamente sus puntos consolidados sin distintivo de "en vivo".

### Requirement 7: Estados de interfaz, errores, concurrencia y navegación

**User Story:** Como usuario, quiero retroalimentación clara del progreso y los errores en la pantalla de ranking, para entender su estado.

#### Acceptance Criteria

1. WHILE el `Ranking_System` procesa un cálculo (carga inicial, cambio de periodo o recálculo en vivo), THE `Ranking_UI` SHALL mostrar un indicador de carga.
2. WHEN el cálculo termina con éxito, THE `Ranking_UI` SHALL ocultar el indicador de carga y mostrar el podio y la lista.
3. IF el cálculo termina con error, THEN THE `Ranking_UI` SHALL ocultar el indicador de carga y mostrar un mensaje de error, sin datos parciales.
4. WHEN no hay ningún participante con puntos en el periodo, THE `Ranking_UI` SHALL mostrar un estado vacío informativo sin error.
5. THE `Ranking_System` SHALL ejecutar sus lecturas/cálculos en el executor de I/O del proyecto (`@IoExecutor`), devolver `Result<T>` mediante `Callback` y publicar en `LiveData` desde el `RankingViewModel` (patrón `BaseViewModel`), sin bloquear el hilo de UI.
6. IF llega un nuevo recálculo (por cambio de periodo o refresco en vivo) mientras otro está en curso, THEN THE `Ranking_System` SHALL descartar el resultado del cálculo obsoleto y publicar únicamente el correspondiente a la última selección de periodo (el último gana), evitando parpadeos o resultados inconsistentes.
7. WHEN ocurre un cambio de configuración (rotación), THE `RankingViewModel` SHALL conservar el `Ranking_Period` seleccionado y el estado del ranking sin recalcular innecesariamente ni duplicar cargas.

### Requirement 8: Rendimiento y consistencia del cálculo

**User Story:** Como equipo de desarrollo, quiero que el cálculo del ranking sea eficiente y determinista, para una experiencia fluida y estable.

#### Acceptance Criteria

1. THE `Ranking_System` SHALL calcular el ranking del `Current_User` leyendo sus pronósticos con una consulta acotada (por ejemplo `getPredictionsByUser`) y resolviendo los `Match` necesarios de la caché, sin realizar una consulta por cada pronóstico cuando pueda evitarse. (Con un único usuario real y `Seed_Profiles` en memoria, el volumen es reducido; el criterio previene degradación si aumentan los usuarios locales.)
2. THE `Ranking_System` SHALL producir el mismo orden de posiciones ante los mismos datos de entrada (cálculo determinista), aplicando el desempate del Requisito 3.4.
3. THE `Ranking_System` SHALL resolver el nombre del `Current_User` con `Display_Name` y su avatar desde `Local_Database` / `Local_Session`, y el nombre y avatar de los `Seed_Profiles` desde su definición de muestra.

### Requirement 9: Verificación mediante pruebas automatizadas

**User Story:** Como equipo de desarrollo, quiero pruebas del cálculo del ranking, para garantizar la corrección y prevenir regresiones.

#### Acceptance Criteria

1. THE proyecto SHALL incluir tests unitarios puros de JVM para el cálculo de `Win_Percentage` (incluyendo 0 pronósticos resueltos y el redondeo con `Math.round`) y de `Streak` global (racha interrumpida por un fallo; racha contada desde el más reciente por `finalizedAt`).
2. THE proyecto SHALL incluir tests unitarios puros de JVM para el ordenamiento del ranking y el desempate determinista completo (empate en puntos resuelto por `Win_Percentage`, luego nombre case-insensitive, luego identificador).
3. THE proyecto SHALL incluir tests unitarios puros de JVM para el filtrado por `Ranking_Period` (Semanal / Mensual / Total) por `finalizedAt` sobre un conjunto de pronósticos con fechas variadas, y para la exclusión de un pronóstico cuyo `Match` no está en caché.
4. THE proyecto SHALL incluir tests unitarios puros de JVM para los `Live_Points`: un pronóstico con partido `LIVE` con marcador aporta puntuación provisional según el marcador en vivo; un partido `LIVE` con marcador `null` aporta 0 y no lanza error; y los `Live_Points` no se cuentan como consolidados (conjuntos disjuntos).