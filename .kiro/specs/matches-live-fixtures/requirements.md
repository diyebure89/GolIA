# Requirements Document

## Introduction

Esta funcionalidad dota de contenido real a la pantalla de Partidos (`PartidosFragment`) de la app GolIA, una aplicación Android de predicciones de fútbol construida con Clean Architecture + MVVM + Hilt + Room. La pantalla debe replicar el mockup adjunto: encabezado "GOL-IA" con campana de notificaciones, título "Partidos" con icono de búsqueda, una fila de chips de filtro por horario ("Hoy", "Mañana", "Esta semana") y una lista de tarjetas de partido con nombre de liga y jornada, hora local, logos y nombres de ambos equipos, marcador/cuotas, estado (por ejemplo "Disponible") y ubicación/estadio.

Los datos provienen de la API gratuita API-Football (API-SPORTS). Se muestran partidos del conjunto ampliado de Competiciones_Objetivo, que abarca ligas domésticas (Premier League de Inglaterra, LaLiga / Primera División de España, Serie A de Italia, Bundesliga de Alemania, Ligue 1 de Francia y Colombia Primera A), competiciones de clubes (UEFA Champions League) y competiciones de selecciones (UEFA Nations League y la Clasificación al Mundial por confederación: Europa, Sudamérica/Conmebol, África, Asia, CONCACAF, Oceanía y el Repechaje Intercontinental). El estilo visual se alinea con la paleta oscura azul del login (`blue_dark`, `blue_start`, `blue_end`).

La pantalla observa estado desde un ViewModel `@HiltViewModel` que consume `MatchRepository` (dominio) mediante casos de uso; el fragment no llama directamente a la API. Se reutiliza la infraestructura existente (`Match`, `MatchStatus`, `Competition`, `Team`, `MatchRepository`, `MatchRepositoryImpl`, `FootballApiService`, DAOs de Room, `GolIADatabase`, módulos Hilt), ajustándola para apuntar a API-Football real y al conjunto de Competiciones_Objetivo.

### Fuera de alcance (explícito)

- La lógica de predicciones y apuestas. Las cuotas del mockup quedan fuera de alcance en este spec: no se obtienen ni se muestran cuotas, y realizar apuestas o predicciones no es parte de este spec.
- La autenticación de usuario.
- El contenido de otros fragments (Inicio, Noticias, Perfil, Ranking, Predicciones).
- El mockup muestra "NBA Playoffs" como ejemplo ilustrativo; este spec cubre únicamente fútbol de las Competiciones_Objetivo indicadas.

### Riesgos y compromisos a reflejar

- El plan gratuito de API-Football permite 100 peticiones/día. Esto obliga a una caché local agresiva (Room) y a una política de refresco controlada. El "en vivo" no puede hacer polling cada pocos segundos; se acota el refresco periódico solo mientras haya partidos en vivo visibles.
- La API key no debe almacenarse en el control de versiones; se lee desde un lugar seguro/local (por ejemplo `local.properties` expuesto vía `BuildConfig`).
- Se DECIDIÓ migrar el cliente del esquema football-data.org al de API-Football (API-SPORTS): endpoint `/fixtures`, IDs numéricos de liga/competición (ligas domésticas: 39=Premier League, 140=LaLiga, 135=Serie A, 78=Bundesliga, 61=Ligue 1, 239=Colombia Primera A; clubes: 2=UEFA Champions League; selecciones: 5=UEFA Nations League y Clasificación al Mundial 32=Europa, 34=Sudamérica/Conmebol, 29=África, 30=Asia, 31=CONCACAF, 33=Oceanía, 37=Repechaje Intercontinental), header `x-apisports-key` y respuesta JSON anidada (`response[].fixture/teams/goals/league`). Esto implica reescribir `FootballApiService`, sus DTOs y mapeadores, y deprecar los métodos que no aplican (por ejemplo `getMatchesByDate` estilo football-data.org y `getCompetitions`).
- El `MatchRepositoryImpl` actual usa llamadas Retrofit síncronas (`.execute()`) y ejecuta algunos DAOs en el hilo llamante. Se refactoriza para ejecutar la obtención de datos en un ejecutor de E/S (`@IoExecutor`) y propagar errores de forma tipada con `domain/common/Result`.
- El esquema Room de `MatchEntity` actual NO tiene jornada, estadio ni minuto de juego. Los requisitos que dependen de esos campos implican una migración de Room.

#### Secuenciación recomendada

El trabajo se aborda en este orden de prioridad para evitar retrabajo:

1. Contrato del proveedor API-Football y reescritura del cliente (Requisito 1).
2. Política de cuota (Requisito 7) y polling en vivo (Requisitos 7 y 6).
3. Contrato del Repositorio_Partidos y threading (Requisito 12).

## Glossary

- **App**: La aplicación Android GolIA en su conjunto.
- **Pantalla_Partidos**: La interfaz gestionada por `PartidosFragment` que muestra la lista de partidos, los chips de filtro y el encabezado.
- **ViewModel_Partidos**: El `@HiltViewModel` que provee el estado observable de la Pantalla_Partidos y coordina los casos de uso.
- **Repositorio_Partidos**: La implementación de `MatchRepository` (`MatchRepositoryImpl`) que orquesta datos remotos (API-Football) y locales (Room).
- **Servicio_API_Football**: El cliente Retrofit `FootballApiService` que consume los endpoints de API-Football.
- **Cache_Partidos**: El almacenamiento local en Room (`MatchDao`, `TeamDao`, `CompetitionDao`, `GolIADatabase`) de partidos, equipos y competiciones.
- **Adaptador_Partidos**: El `RecyclerView.Adapter` que renderiza las tarjetas de partido usando `DiffUtil`.
- **Competiciones_Objetivo**: El conjunto exacto de competiciones cuyos partidos se muestran, identificadas por su ID numérico de liga de API-Football. Abarca ligas domésticas (39 Premier League, 140 LaLiga/Primera División, 135 Serie A, 78 Bundesliga, 61 Ligue 1, 239 Colombia Primera A), competiciones de clubes (2 UEFA Champions League) y competiciones de selecciones (5 UEFA Nations League y la Clasificación al Mundial por confederación: 32 Europa, 34 Sudamérica/Conmebol, 29 África, 30 Asia, 31 CONCACAF, 33 Oceanía, 37 Repechaje Intercontinental). Cualquier competición fuera de este conjunto se descarta. (Anteriormente denominado Ligas_Objetivo cuando el conjunto se limitaba a las seis ligas domésticas.)
- **Zona_Local**: La zona horaria configurada en el dispositivo del usuario.
- **Utilidad_Zona_Local**: La única utilidad de cálculo de rangos temporales que, a partir de la Zona_Local, produce el Rango_Hoy, el Rango_Manana y el Rango_Esta_Semana. Es el único punto de verdad para el cálculo de rangos.
- **API_Key**: La clave de autenticación de API-Football enviada en el header `x-apisports-key`.
- **Presupuesto_Peticiones**: El contador diario persistente de peticiones realizadas a API-Football, con reset diario, usado para respetar el límite de 100 peticiones/día del plan gratuito.
- **Minuto_Juego**: El minuto de juego transcurrido de un partido en estado `LIVE`, obtenido de `fixture.status.elapsed` de API-Football.
- **Chip_Horario**: Cada uno de los chips de filtro "Ayer", "Hoy", "Mañana", "Esta semana" del `ChipGroup`.
- **Rango_Ayer**: Intervalo del día anterior al actual en Zona_Local, desde el inicio inclusivo `00:00:00.000` hasta el fin inclusivo `23:59:59.999`.
- **Rango_Hoy**: Intervalo del día actual en Zona_Local, desde el inicio inclusivo `00:00:00.000` hasta el fin inclusivo `23:59:59.999`.
- **Rango_Manana**: Intervalo del día siguiente al actual en Zona_Local, desde el inicio inclusivo `00:00:00.000` hasta el fin inclusivo `23:59:59.999`.
- **Rango_Esta_Semana**: Intervalo desde el inicio inclusivo `00:00:00.000` del día actual hasta el fin inclusivo `23:59:59.999` del domingo de la semana actual en Zona_Local.
- **Estado_UI**: El estado observable de la Pantalla_Partidos, uno de: Cargando, Contenido, Vacío, Error.
- **Grupo_LIVE**: El grupo de partidos de la lista filtrada con `MatchStatus` `LIVE`, ubicado con prioridad sobre el Grupo_No_LIVE en el ordenamiento.
- **Grupo_No_LIVE**: El grupo de partidos de la lista filtrada con `MatchStatus` `SCHEDULED` y demás estados distintos de `LIVE`, ubicado después del Grupo_LIVE en el ordenamiento.
- **Pantalla_Inicio**: La interfaz gestionada por `InicioFragment` (`R.layout.fragment_inicio`) que muestra la bienvenida, las estadísticas y la sección "Próximos partidos" con datos reales de partidos.

## Requirements

### Requisito 1: Contrato del proveedor API-Football

**Historia de Usuario:** Como usuario, quiero ver partidos reales de las Competiciones_Objetivo indicadas, para consultar información actualizada de los encuentros que me interesan.

#### Criterios de Aceptación

1. WHEN la Pantalla_Partidos solicita partidos, THE Repositorio_Partidos SHALL obtener los fixtures de la temporada actual de las Competiciones_Objetivo mediante el Servicio_API_Football usando el endpoint `/fixtures`, priorizando la consulta por fecha (`/fixtures?date={YYYY-MM-DD}`) sobre la consulta por liga (`/fixtures?league={idNum}&season={year}`) para minimizar el número de peticiones.
2. WHEN el Servicio_API_Football realiza una petición a API-Football, THE Servicio_API_Football SHALL incluir la API_Key en el header `x-apisports-key`.
3. THE Servicio_API_Football SHALL mapear cada una de las Competiciones_Objetivo a su identificador numérico de liga de API-Football: ligas domésticas 39 para Premier League, 140 para LaLiga/Primera División, 135 para Serie A, 78 para Bundesliga, 61 para Ligue 1 y 239 para Colombia Primera A; competiciones de clubes 2 para UEFA Champions League; y competiciones de selecciones 5 para UEFA Nations League y la Clasificación al Mundial por confederación 32 para Europa, 34 para Sudamérica/Conmebol, 29 para África, 30 para Asia, 31 para CONCACAF, 33 para Oceanía y 37 para el Repechaje Intercontinental.
4. WHEN API-Football responde con el formato JSON anidado del endpoint de fixtures (`response[].fixture/teams/goals/league`), THE Repositorio_Partidos SHALL mapear la respuesta a objetos de dominio `Match`, `Team` y `Competition`.
5. THE Repositorio_Partidos SHALL limitar los partidos obtenidos a las Competiciones_Objetivo y descartar competiciones fuera de ese conjunto.
6. IF una respuesta de API-Football contiene un partido de una competición fuera de las Competiciones_Objetivo, THEN THE Repositorio_Partidos SHALL excluir ese partido del resultado.

### Requisito 1B: Reescritura del cliente Retrofit al esquema de API-Football

**Historia de Usuario:** Como desarrollador, quiero reemplazar el cliente Retrofit modelado para football-data.org por uno modelado para API-Football, para que la obtención de datos sea coherente con el proveedor decidido.

#### Criterios de Aceptación

1. THE Servicio_API_Football SHALL reescribir la interfaz `FootballApiService`, sus DTOs y sus mapeadores para el esquema de API-Football (endpoint `/fixtures`, IDs numéricos de liga, header `x-apisports-key` y respuesta JSON anidada `response[].fixture/teams/goals/league`).
2. THE Servicio_API_Football SHALL deprecar o eliminar los métodos que no aplican al esquema de API-Football, incluidos `getMatchesByDate` con el estilo de football-data.org y `getCompetitions`.
3. WHEN el Servicio_API_Football recibe una respuesta del endpoint `/fixtures`, THE Servicio_API_Football SHALL exponer los DTOs anidados (`fixture`, `teams`, `goals`, `league`) que los mapeadores transforman a objetos de dominio.
4. THE Servicio_API_Football SHALL NOT depender de los identificadores de competición en formato de cadena de football-data.org (por ejemplo `"PL"`), usando en su lugar los identificadores numéricos de liga de API-Football.

### Requisito 2: Seguridad de la API Key

**Historia de Usuario:** Como desarrollador, quiero mantener la API_Key fuera del control de versiones, para evitar exponer credenciales en el repositorio de código.

#### Criterios de Aceptación

1. THE App SHALL leer la API_Key desde una fuente local no versionada (por ejemplo `local.properties` expuesto vía `BuildConfig`).
2. THE código fuente versionado SHALL NOT contener el valor literal de la API_Key.
3. IF la API_Key no está configurada al iniciar la Pantalla_Partidos, THEN THE ViewModel_Partidos SHALL exponer un Estado_UI de Error con un mensaje que indique que falta la configuración de la clave.

### Requisito 3: Filtros por chips de horario

**Historia de Usuario:** Como usuario, quiero filtrar los partidos por "Ayer", "Hoy", "Mañana" y "Esta semana", para encontrar rápidamente los encuentros dentro del rango temporal que me interesa.

#### Criterios de Aceptación

1. THE Pantalla_Partidos SHALL mostrar un `ChipGroup` con los cuatro Chip_Horario en este orden: "Ayer", "Hoy", "Mañana" y "Esta semana".
2. THE ChipGroup SHALL permitir la selección de un único Chip_Horario a la vez.
3. WHEN la Pantalla_Partidos se abre, THE Pantalla_Partidos SHALL seleccionar por defecto el Chip_Horario "Hoy".
3a. WHEN el usuario selecciona el Chip_Horario "Ayer", THE ViewModel_Partidos SHALL mostrar únicamente los partidos cuyo horario en Zona_Local cae dentro del Rango_Ayer, es decir desde el inicio inclusivo `00:00:00.000` hasta el fin inclusivo `23:59:59.999` del día anterior al actual en Zona_Local.
4. WHEN el usuario selecciona el Chip_Horario "Hoy", THE ViewModel_Partidos SHALL mostrar únicamente los partidos cuyo horario en Zona_Local cae dentro del Rango_Hoy, es decir desde el inicio inclusivo `00:00:00.000` hasta el fin inclusivo `23:59:59.999` del día actual en Zona_Local.
5. WHEN el usuario selecciona el Chip_Horario "Mañana", THE ViewModel_Partidos SHALL mostrar únicamente los partidos cuyo horario en Zona_Local cae dentro del Rango_Manana, es decir desde el inicio inclusivo `00:00:00.000` hasta el fin inclusivo `23:59:59.999` del día siguiente al actual en Zona_Local.
6. WHEN el usuario selecciona el Chip_Horario "Esta semana", THE ViewModel_Partidos SHALL mostrar únicamente los partidos cuyo horario en Zona_Local cae dentro del Rango_Esta_Semana, es decir desde el inicio inclusivo `00:00:00.000` del día actual hasta el fin inclusivo `23:59:59.999` del domingo de la semana actual en Zona_Local.
7. THE ViewModel_Partidos SHALL calcular el Rango_Hoy, el Rango_Manana y el Rango_Esta_Semana mediante la Utilidad_Zona_Local como único punto de verdad para el cálculo de rangos temporales.
8. WHERE un partido cae dentro del Rango_Hoy, THE ViewModel_Partidos SHALL incluir ese partido también dentro del Rango_Esta_Semana (el Rango_Hoy es un subconjunto del Rango_Esta_Semana).
9. WHERE el chip activo está seleccionado, THE Pantalla_Partidos SHALL resaltar ese Chip_Horario con el color `blue_end`.

### Requisito 4: Conversión de zona horaria

**Historia de Usuario:** Como usuario, quiero ver los horarios de los partidos en mi zona horaria, para saber a qué hora local se juega cada encuentro.

#### Criterios de Aceptación

1. WHEN la Pantalla_Partidos muestra la hora de un partido, THE ViewModel_Partidos SHALL convertir el horario recibido de API-Football (UTC/timestamp) a la Zona_Local mediante la Utilidad_Zona_Local.
2. WHEN el ViewModel_Partidos clasifica un partido en un Chip_Horario, THE ViewModel_Partidos SHALL comparar el horario del partido convertido a la Zona_Local contra los límites en milisegundos (inicio inclusivo `00:00:00.000`, fin inclusivo `23:59:59.999`) del rango correspondiente calculado por la Utilidad_Zona_Local.
3. WHEN la Pantalla_Partidos presenta la fecha y hora de un partido, THE Pantalla_Partidos SHALL formatearla de forma localizada (por ejemplo "14 Jun · 20:00").

### Requisito 5: Contenido de la tarjeta de partido

**Historia de Usuario:** Como usuario, quiero ver la información clave de cada partido en una tarjeta, para reconocer de un vistazo los equipos, la competición y el estado del encuentro.

#### Criterios de Aceptación

1. WHEN el Adaptador_Partidos renderiza un partido, THE Adaptador_Partidos SHALL mostrar el nombre de la liga y la jornada del partido, obteniendo la jornada del campo `league.round` de API-Football; este campo requiere ampliar el esquema de `MatchEntity`/`Match` (migración de Room).
2. WHEN el Adaptador_Partidos renderiza un partido, THE Adaptador_Partidos SHALL mostrar el nombre de ambos equipos y su hora local.
3. WHEN el Adaptador_Partidos renderiza un partido, THE Adaptador_Partidos SHALL cargar los logos de ambos equipos desde su URL.
4. IF el logo de un equipo no se puede cargar, THEN THE Adaptador_Partidos SHALL mostrar una imagen placeholder en su lugar.
5. WHERE API-Football provee la ubicación o el estadio del partido en los campos `fixture.venue.name` y `fixture.venue.city`, THE Adaptador_Partidos SHALL mostrar la ubicación/estadio en la tarjeta; estos campos requieren ampliar el esquema de `MatchEntity`/`Match` (migración de Room).

### Requisito 6: Estados de partido y marcador

**Historia de Usuario:** Como usuario, quiero distinguir entre partidos programados, en vivo y finalizados, para saber el estado real de cada encuentro y su marcador.

#### Criterios de Aceptación

1. WHERE un partido tiene estado `SCHEDULED`, THE Adaptador_Partidos SHALL mostrar la hora local del partido.
2. WHERE un partido tiene estado `LIVE`, THE Adaptador_Partidos SHALL mostrar el marcador actual y el Minuto_Juego.
3. WHERE un partido tiene estado `FINISHED`, THE Adaptador_Partidos SHALL mostrar el marcador final.
4. WHEN el estado de un partido en Cache_Partidos cambia de `LIVE` a `FINISHED`, THE Pantalla_Partidos SHALL actualizar la tarjeta correspondiente para reflejar el marcador final.
5. WHEN el Repositorio_Partidos persiste un partido en la Cache_Partidos, THE Repositorio_Partidos SHALL almacenar el marcador (goles local y visitante) y el Minuto_Juego en `MatchEntity`; estos campos requieren ampliar el esquema de `MatchEntity` (migración de Room).

### Requisito 6B: Normalización de estados del proveedor

**Historia de Usuario:** Como desarrollador, quiero traducir los códigos de estado de API-Football al dominio `MatchStatus`, para que la app opere con estados normalizados y predecibles.

#### Criterios de Aceptación

1. WHEN el Repositorio_Partidos mapea un partido de API-Football, THE Repositorio_Partidos SHALL normalizar el código `fixture.status.short` del proveedor al dominio `MatchStatus` según la siguiente tabla de mapeo:

   | `fixture.status.short` (API-Football) | `MatchStatus` (dominio) |
   | --- | --- |
   | `NS` | `SCHEDULED` |
   | `1H`, `HT`, `2H`, `ET`, `BT`, `P`, `LIVE` | `LIVE` |
   | `FT`, `AET`, `PEN` | `FINISHED` |
   | `PST` | `POSTPONED` |
   | `CANC`, `ABD` | `CANCELLED` |

2. IF el código `fixture.status.short` no coincide con ningún valor de la tabla de mapeo, THEN THE Repositorio_Partidos SHALL asignar el `MatchStatus` `SCHEDULED` como valor por defecto y registrar el código no reconocido.
3. WHERE un partido tiene un `MatchStatus` normalizado `LIVE`, THE Repositorio_Partidos SHALL obtener el Minuto_Juego del campo `fixture.status.elapsed`.

### Requisito 7: Resultados en vivo y política de refresco

**Historia de Usuario:** Como usuario, quiero que los partidos en vivo actualicen su marcador, sin que la app agote la cuota diaria de la API, para tener datos frescos de forma sostenible.

#### Criterios de Aceptación

1. WHEN la Pantalla_Partidos se abre, THE Repositorio_Partidos SHALL refrescar los partidos desde API-Football usando el endpoint por-fecha (`/fixtures?date={YYYY-MM-DD}`), de modo que una única petición cubra todas las Competiciones_Objetivo de esa fecha en lugar de una petición por competición.
1a. WHEN el Repositorio_Partidos refresca por fecha, THE Repositorio_Partidos SHALL descargar una ventana de días que comience en el día anterior al actual (para poblar el Chip_Horario "Ayer") y abarque el día actual y los días siguientes hasta cubrir el Rango_Esta_Semana, realizando una petición por día dentro del Presupuesto_Peticiones.
2. WHEN el usuario realiza pull-to-refresh, THE Repositorio_Partidos SHALL refrescar los partidos desde API-Football usando el endpoint por-fecha para minimizar el número de peticiones.
3. WHEN el Repositorio_Partidos realiza una petición a API-Football, THE Repositorio_Partidos SHALL incrementar el Presupuesto_Peticiones, que es un contador diario persistente con reset diario.
4. WHILE cualquier fuente de refresco (apertura de pantalla, pull-to-refresh o polling en vivo) solicita datos, THE ViewModel_Partidos SHALL aplicar un intervalo mínimo de 60 segundos COMPARTIDO entre todas las fuentes de refresco, descartando las solicitudes que lleguen antes de cumplirse ese intervalo desde la última petición.
5. WHILE existan partidos con estado `LIVE` visibles, definidos como los partidos con `MatchStatus` `LIVE` presentes en la lista filtrada actual del Chip_Horario activo, THE ViewModel_Partidos SHALL refrescar los datos en vivo respetando el intervalo mínimo compartido de 60 segundos entre peticiones.
6. WHILE no existan partidos con estado `LIVE` en la lista filtrada actual del Chip_Horario activo, THE ViewModel_Partidos SHALL detener el refresco periódico en vivo.
7. IF el Presupuesto_Peticiones diario se ha agotado, THEN THE ViewModel_Partidos SHALL degradar a modo solo-caché mostrando los datos de la Cache_Partidos junto con un aviso de que se ha alcanzado el límite diario de peticiones, y SHALL NOT realizar más peticiones a API-Football hasta el reset diario.
8. THE Repositorio_Partidos SHALL persistir los partidos, equipos y competiciones obtenidos en la Cache_Partidos.
9. WHEN la Pantalla_Partidos se abre y la Cache_Partidos contiene datos, THE Pantalla_Partidos SHALL mostrar los datos cacheados mientras se completa el refresco remoto.

### Requisito 7B: Ciclo de vida del polling en vivo

**Historia de Usuario:** Como usuario, quiero que la app deje de consumir peticiones cuando no estoy mirando la pantalla, para no agotar la cuota diaria en segundo plano.

#### Criterios de Aceptación

1. WHEN el fragment de la Pantalla_Partidos se pausa (`onPause`) o la App pasa a segundo plano, THE ViewModel_Partidos SHALL detener el polling en vivo.
2. WHEN el fragment de la Pantalla_Partidos vuelve a primer plano (`onResume`), THE ViewModel_Partidos SHALL reanudar el polling en vivo respetando el intervalo mínimo compartido de 60 segundos entre peticiones.

### Requisito 8: Ordenamiento de partidos

**Historia de Usuario:** Como usuario, quiero ver los partidos ordenados de forma predecible, para localizar fácilmente el próximo encuentro relevante.

#### Criterios de Aceptación

1. WHEN la Pantalla_Partidos muestra la lista filtrada, THE ViewModel_Partidos SHALL separar los partidos en dos grupos por prioridad: primero el Grupo_LIVE (partidos con `MatchStatus` `LIVE`) y después el Grupo_No_LIVE (partidos con `MatchStatus` `SCHEDULED` y demás estados distintos de `LIVE`).
2. THE ViewModel_Partidos SHALL ubicar todo el Grupo_LIVE antes de todo el Grupo_No_LIVE, respetando este orden de grupos con independencia del horario de cada partido.
3. WHEN el ViewModel_Partidos ordena los partidos dentro de un mismo grupo, THE ViewModel_Partidos SHALL ordenarlos por horario (kickoff) convertido a Zona_Local en orden ascendente.
4. WHERE dos partidos de un mismo grupo comparten el mismo horario en Zona_Local, THE ViewModel_Partidos SHALL aplicar un desempate estable y determinista ordenando primero por nombre de liga en orden alfabético ascendente y, en caso de coincidir, por nombre del equipo local en orden alfabético ascendente.
5. WHERE un partido del Grupo_LIVE comenzó a una hora anterior a la de un partido del Grupo_No_LIVE, THE ViewModel_Partidos SHALL ubicar igualmente el partido del Grupo_LIVE antes que el del Grupo_No_LIVE, dado que la prioridad de grupo domina sobre el horario entre grupos.

### Requisito 9: Estados de la interfaz

**Historia de Usuario:** Como usuario, quiero recibir retroalimentación clara mientras cargan los datos o cuando algo falla, para entender qué está ocurriendo en la pantalla.

#### Criterios de Aceptación

1. WHILE el Repositorio_Partidos obtiene datos y aún no hay contenido para mostrar, THE Pantalla_Partidos SHALL mostrar un indicador de carga.
2. WHEN el filtro seleccionado no tiene partidos, THE Pantalla_Partidos SHALL mostrar el `empty_view` (id `empty_view`) con un mensaje de lista vacía y ocultar el `recycler_matches`.
3. IF ocurre un fallo de API o de red y no hay datos cacheados que mostrar, THEN THE Pantalla_Partidos SHALL mostrar un Estado_UI de Error con un mensaje y una acción de reintentar.
4. WHEN el usuario activa la acción de reintentar, THE ViewModel_Partidos SHALL volver a solicitar el refresco de partidos.

### Requisito 10: Búsqueda de partidos

**Historia de Usuario:** Como usuario, quiero buscar partidos por nombre de equipo o liga desde el icono de búsqueda, para filtrar rápidamente la lista mostrada.

#### Criterios de Aceptación

1. WHEN el usuario activa el icono de búsqueda del encabezado, THE Pantalla_Partidos SHALL mostrar un campo de entrada de texto para la búsqueda.
2. WHEN el usuario introduce texto en el campo de búsqueda, THE ViewModel_Partidos SHALL aplicar la búsqueda dentro de la lista del Chip_Horario seleccionado, conservando únicamente los partidos cuyo nombre de equipo local, nombre de equipo visitante o nombre de liga contengan el texto introducido.
3. WHEN el usuario introduce texto en el campo de búsqueda, THE ViewModel_Partidos SHALL aplicar un debounce de 300 milisegundos antes de recalcular la lista filtrada, de modo que las pulsaciones rápidas no disparen filtrados intermedios.
4. WHEN el ViewModel_Partidos compara el texto de búsqueda con los nombres de equipo y de liga, THE ViewModel_Partidos SHALL normalizar ambos lados sin distinguir mayúsculas y sin distinguir acentos ni diacríticos, de modo que "Atletico" coincida con "Atlético".
5. WHILE hay texto de búsqueda activo, THE Pantalla_Partidos SHALL mantener el Chip_Horario seleccionado visible y activo, y THE ViewModel_Partidos SHALL operar la búsqueda dentro de la lista de ese Chip_Horario.
6. WHEN el usuario cambia de Chip_Horario mientras hay texto de búsqueda activo, THE ViewModel_Partidos SHALL re-aplicar la búsqueda sobre la lista del nuevo Chip_Horario seleccionado.
7. WHEN el texto de búsqueda no coincide con ningún partido de la lista del Chip_Horario seleccionado, THE Pantalla_Partidos SHALL mostrar un estado vacío específico de búsqueda con un mensaje de "sin resultados de búsqueda", distinto del estado vacío de filtro del Requisito 9.2.
8. WHEN el usuario limpia el texto de búsqueda, THE ViewModel_Partidos SHALL restaurar la lista correspondiente al Chip_Horario seleccionado.

### Requisito 11: Comportamiento offline y manejo de errores

**Historia de Usuario:** Como usuario, quiero seguir viendo información aunque no tenga conexión, para consultar los últimos datos disponibles cuando la red o la API fallen.

#### Criterios de Aceptación

1. IF no hay conexión de red al abrir la Pantalla_Partidos, THEN THE Pantalla_Partidos SHALL mostrar los datos de la Cache_Partidos junto con un aviso de que se muestran datos sin conexión.
2. IF API-Football responde con un error o la cuota diaria se ha agotado, THEN THE Pantalla_Partidos SHALL mostrar un mensaje adecuado y los datos de la Cache_Partidos disponibles.
3. WHEN se restablece la red y el usuario solicita un refresco, THE Repositorio_Partidos SHALL intentar actualizar los datos desde API-Football.

### Requisito 12: Arquitectura y preparación de la pantalla

**Historia de Usuario:** Como desarrollador, quiero que la pantalla siga la arquitectura del proyecto, para mantener la coherencia, la testabilidad y la mantenibilidad.

#### Criterios de Aceptación

1. THE Pantalla_Partidos SHALL observar su estado desde el ViewModel_Partidos mediante `LiveData`.
2. THE Pantalla_Partidos SHALL NOT invocar directamente el Servicio_API_Football ni el Repositorio_Partidos, delegando toda obtención de datos en el ViewModel_Partidos a través de casos de uso.
3. THE ViewModel_Partidos SHALL estar anotado con `@HiltViewModel` y consumir `MatchRepository` mediante casos de uso del dominio.
4. THE Pantalla_Partidos SHALL usar el Adaptador_Partidos con `DiffUtil` para aplicar actualizaciones de la lista de forma eficiente.
5. WHEN el Repositorio_Partidos obtiene datos remotos o accede a la Cache_Partidos, THE Repositorio_Partidos SHALL ejecutar esas operaciones en el `@IoExecutor` y SHALL NOT ejecutarlas en el hilo principal, evitando `NetworkOnMainThreadException`.
6. WHEN el Repositorio_Partidos encuentra un fallo de red, de API o de persistencia, THE Repositorio_Partidos SHALL propagar el fallo de forma tipada mediante `domain/common/Result`, de modo que los criterios 9.3 y 11.2 sean satisfacibles.
7. THE ViewModel_Partidos SHALL exponer el Estado_UI observable mediante `LiveData` a partir de los resultados tipados `domain/common/Result` recibidos del Repositorio_Partidos.

### Requisito 13: Estilo visual coherente con el login

**Historia de Usuario:** Como usuario, quiero que la pantalla de Partidos tenga la misma estética que el resto de la app, para una experiencia visual consistente.

#### Criterios de Aceptación

1. THE Pantalla_Partidos SHALL usar un fondo con la paleta oscura azul del login (`blue_dark`, `blue_start`, `blue_end`).
2. THE Pantalla_Partidos SHALL mostrar el encabezado "GOL-IA" con un icono de campana de notificaciones y el título "Partidos" con el icono de búsqueda, según el mockup.
3. THE Adaptador_Partidos SHALL renderizar cada tarjeta de partido con el estilo del mockup coherente con la paleta del login.

### Requisito 14: Sección "Próximos partidos" en el Inicio

**Historia de Usuario:** Como usuario, quiero ver los próximos partidos reales directamente en la pantalla de Inicio, para consultar de un vistazo los encuentros más cercanos sin entrar a la pantalla de Partidos.

#### Criterios de Aceptación

1. THE Pantalla_Inicio (`InicioFragment`) SHALL mostrar en su sección "Próximos partidos" datos reales obtenidos de la misma fuente que la Pantalla_Partidos (los casos de uso `GetMatchesUseCase` y `RefreshMatchesUseCase` sobre `MatchRepository`), en lugar de tarjetas de ejemplo estáticas.
2. THE Pantalla_Inicio SHALL observar su lista de próximos partidos desde un `InicioViewModel` (`@HiltViewModel`) mediante `LiveData`, sin invocar directamente el Servicio_API_Football ni el Repositorio_Partidos.
3. WHEN el `InicioViewModel` deriva la lista de próximos partidos, THE `InicioViewModel` SHALL incluir los partidos con `MatchStatus` `LIVE` y los partidos con `MatchStatus` `SCHEDULED` cuyo horario sea posterior o igual al momento actual, ordenarlos con los `LIVE` primero y luego por horario ascendente, y limitar la lista a un máximo de 5 encuentros.
4. THE Pantalla_Inicio SHALL renderizar cada próximo partido reutilizando el `Adaptador_Partidos` (`MatchesAdapter`) y el layout `item_match`, para mantener consistencia visual con la Pantalla_Partidos.
5. WHEN el usuario activa el enlace "Ver todos" de la sección de próximos partidos, THE Pantalla_Inicio SHALL navegar a la pestaña Partidos de la `BottomNavigationView` (`R.id.nav_partidos`).
6. THE Pantalla_Inicio SHALL mostrar el título de la sección como "Próximos partidos" (recurso `partidos_proximos`).
7. IF la API_Key no está configurada o el refresco remoto falla, THEN THE Pantalla_Inicio SHALL mostrar los datos cacheados disponibles o una lista vacía, sin exponer estados de error (la sección es secundaria).
